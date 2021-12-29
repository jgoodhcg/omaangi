(ns app.fx
  (:require
   ["@react-native-async-storage/async-storage" :as async-storage]
   ["expo-constants" :as expo-constants]
   ["expo-file-system" :as expo-file-system]
   ["expo-sharing" :as expo-sharing]
   ["react-native" :as rn]

   [applied-science.js-interop :as j]
   [cljs.core.async :refer [go <!]]
   [cljs.core.async.interop :refer [<p!]]
   [re-frame.core :refer [reg-fx]]
   [tick.alpha.api :as t]

   [app.helpers :refer [>evt]]
   [app.db :as db :refer [default-app-db serialize de-serialize]]
   [app.screens.core :refer [screens]]))


(def !navigation-ref (clojure.core/atom nil))

(def ticker-ref (clojure.core/atom nil))

(defn navigate [name] ;; no params yet
  ;; TODO implement a check that the navigation component has initialized
  ;; https://reactnavigation.org/docs/navigating-without-navigation-prop#handling-initialization
  ;; The race condition is in my favor if the user has to press a component within the navigation container
  (-> @!navigation-ref
      ;; no params yet for second arg
      (j/call :navigate name (j/lit {}))))

(reg-fx :navigate navigate)

(reg-fx :some-fx-example
        (fn [x]
          (println x)))

(reg-fx :start-ticking
        (fn [_]
          (reset! ticker-ref (js/setInterval #(>evt [:tick-tock]) 5000))))

(reg-fx :stop-ticking
        (fn [_]
          (let [ticker-ref-id @ticker-ref]
            (tap> (str "clearing interval " ticker-ref-id))
            (-> ticker-ref-id (js/clearInterval)))))

(def app-db-key "@app_db") ;; TODO remove

(def dd (-> expo-file-system (j/get :documentDirectory)))

(def app-db-file-path (str dd "app_db.edn"))

(def backups-dir (str dd "backups/"))

(defn <load-app-db-from-file
  []
  (go
    (try
      (-> expo-file-system (j/call :getInfoAsync app-db-file-path)
          <p!
          ((fn [info-result]
             (if (-> info-result (j/get :exists) (= false))
               ;; file does NOT exist
               (do
                 (-> rn/Alert (j/call :alert "No app-db file exists"))
                 (>evt [:load-db default-app-db]))
               ;; file exists load db
               (go
                 (try
                   (-> expo-file-system (j/call :readAsStringAsync app-db-file-path)
                       <p!
                       ((fn [app-db-str]
                          (>evt [:load-db (-> app-db-str de-serialize
                                              ;; this merge handles accretion to the db spec
                                              (->> (merge default-app-db)))]))))
                   (catch js/Object e
                     (-> rn/Alert (j/call :alert "Failure on readAsStringAsync" (str e))))))))))
      (catch js/Object e
        (-> rn/Alert (j/call :alert "Failure on getInfoAsync" (str e)))))))

(reg-fx :check-for-saved-db
        (fn [_]
          (go
            (try
              ;; TODO temporary migration code replace with load-app-db-from-file
              ;; check async-storage first
              (-> async-storage
                  (j/get :default)
                  (j/call :getItem app-db-key)
                  <p!
                  ((fn [local-store-value]
                     (if (some? local-store-value)
                       (do
                         ;; if an async storage item is present then load it
                         (>evt [:load-db (-> local-store-value de-serialize
                                             ;; this merge handles accretion to the db spec
                                             (->> (merge default-app-db)))])
                         ;; and then get rid of it
                         (go
                           (try
                             (-> async-storage
                                 (j/get :default)
                                 (j/call :removeItem app-db-key)
                                 <p!)
                             (catch js/Object e
                               (tap> (str "error deleting old async storage backup " e))
                               (-> rn/Alert (j/call :alert "error deleting old async storage backup " (str e)))))))

                       ;; if there is no async storage item then just load from file (new process)
                       (go (<! (<load-app-db-from-file)))))))
              (catch js/Object e
                (tap> (str "error checking for db " e))
                (-> rn/Alert (j/call :alert "cljs catch on get Item " (str e))))))))

(reg-fx :save-db
        (fn [app-db]
          (try
            (-> expo-file-system
                (j/call :writeAsStringAsync app-db-file-path (serialize app-db)))
            (catch js/Object e (tap> (str "error saving db to file " e))))))

(def version (-> expo-constants
                 (j/get :default)
                 (j/get :manifest)
                 (j/get :version)))

(reg-fx :post-load-db
        (fn [_]
          (>evt [:set-version version])
          (>evt [:set-selected-day (t/now)])
          (>evt [:load-backup-keys])
          ;; The tick rate is rather slow (5 sec as of 2021-10-01) because faster rates interfere with buttons
          ;; Because of that we want to tick when app state changes -- so the user doesn't have to wait 5 seconds after opening
          (-> rn/AppState
              (j/call :addEventListener
                      "change"
                      #(>evt [:tick-tock])))))

(reg-fx :load-backup-keys
        (fn [_]
          (go
            (try
              (-> expo-file-system
                  (j/call :readDirectoryAsync backups-dir)
                  <p!
                  js->clj
                  vec
                  (#(>evt [:set-backup-keys %])))
              (catch js/Object e
                (tap> (str "error getting all backup file names " e))
                (-> rn/Alert (j/call :alert "error getting all backup file names " (str e))))))))

(reg-fx :create-backup
        (fn [{version   :app-db/version
              timestamp :app-db/current-time
              :as       app-db}]
          (go
            (try
              (-> expo-file-system
                  (j/call :writeAsStringAsync
                          (str backups-dir (t/date-time timestamp) "--" version)
                          (serialize app-db)))
              (>evt [:load-backup-keys])
              (catch js/Object e
                (tap> (str "error creating backup " e))
                (-> rn/Alert (j/call :alert "error creating backup " (str e))))))))

(reg-fx :delete-backup
        (fn [k]
          (go
            (try
              (-> expo-file-system
                  (j/call :deleteAsync (str backups-dir k))
                  <p!
                  (#(>evt [:load-backup-keys])))
              (catch js/Object e
                (tap> (str "error deleting backup " e))
                (-> rn/Alert (j/call :alert "error deleting backup " (str e))))))))

(reg-fx :restore-backup
        (fn [k]
          (go
            (try
              (-> expo-file-system
                  (j/call :readAsStringAsync (str backups-dir k))
                  <p!
                  ((fn [local-store-value]
                     (if (some? local-store-value)
                       (do
                         (>evt [:load-db (-> local-store-value de-serialize
                                             ;; this merge handles accretion to the db spec
                                             (->> (merge default-app-db)))])
                         (>evt [:navigate (:day screens)]))
                       (-> rn/Alert (j/call :alert "Unable to restore backup"))))))
              (catch js/Object e
                (tap> (str "error restoring backup " e))
                (-> rn/Alert (j/call :alert "error restoring backup " (str e))))))))

(reg-fx :export-backup
        (fn [k]
          (go
            (try
              (-> expo-sharing
                  (j/call :shareAsync (str backups-dir k))
                  <p!)
              (catch js/Object e
                (tap> (str "error exporting backup " e))
                (-> rn/Alert (j/call :alert "error exporting backup " (str e))))))))

;; All backup functions require the backups directory to exist or they will throw
(reg-fx :create-backups-directory
        (fn [_]
          (try
            (go
              (-> expo-file-system
                  (j/call :getInfoAsync backups-dir)
                  <p!
                  ((fn [info-result]
                     (when (-> info-result (j/get :exists) (= false))
                       (go
                         (-> expo-file-system
                             (j/call :makeDirectoryAsync backups-dir)
                             <p!)))))))
            (catch js/Object e
              (tap> (str "error creating backup directory " e))
              (-> rn/Alert (j/call :alert "error creating backup directory " (str e)))))))

;; Some helpful repl stuff
(comment
  ;; Hard reset app-db
  (do
    (>evt [:stop-ticking])
    (>evt [:initialize-db])
    (>evt [:save-db]))

  )
