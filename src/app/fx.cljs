(ns app.fx
  (:require
   ["@react-native-async-storage/async-storage" :as async-storage]
   ["expo-constants" :as expo-constants]
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

(def app-db-key "@app_db")

(reg-fx :check-for-saved-db
        ;; TODO justin 2021-11-17 convert to core.async
        (fn [_]
          (try
            (-> async-storage
                (j/get :default)
                (j/call :getItem app-db-key)
                (j/call :then (fn [local-store-value]
                                (if (some? local-store-value)
                                  (>evt [:load-db (-> local-store-value de-serialize
                                                      ;; this merge handles accretion to the db spec
                                                      (->> (merge default-app-db)))])
                                  (do
                                    (-> rn/Alert (j/call :alert "no local store data found"))
                                    (>evt [:load-db default-app-db])))))
                (j/call :catch #(do
                                  (tap> (str "get item catch " %))
                                  (-> rn/Alert (j/call :alert "js catch on get item " (str %))))))
            (catch js/Object e
              (tap> (str "error checking for db " e))
              (-> rn/Alert (j/call :alert "cljs catch on get Item " (str e)))))))

(reg-fx :save-db
        (fn [app-db]
          (try
            (-> async-storage
                (j/get :default)
                (j/call :setItem app-db-key (serialize app-db)))
            (catch js/Object e (tap> (str "error saving db " e))))))

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
              (-> async-storage
                  (j/get :default)
                  (j/call :getAllKeys)
                  <p!
                  js->clj
                  (->> (remove (fn [k] (= app-db-key k))))
                  vec
                  (#(>evt [:set-backup-keys %])))
              (catch js/Object e
                (tap> (str "error getting all async storage keys " e))
                (-> rn/Alert (j/call :alert "error getting all async storage keys " (str e))))))
          ))

(reg-fx :create-backup
        (fn [{version   :app-db/version
              timestamp :app-db/current-time
              :as       app-db}]
          (go
            (try
              (-> async-storage
                  (j/get :default)
                  (j/call :setItem (str "@-" (t/date-time timestamp)  "--" version) (serialize app-db)))
              (>evt [:load-backup-keys])
              (catch js/Object e
                (tap> (str "error creating backup " e))
                (-> rn/Alert (j/call :alert "error creating backup " (str e))))))))

(reg-fx :delete-backup
        (fn [k]
          (go
            (try
              (-> async-storage
                  (j/get :default)
                  (j/call :removeItem k)
                  <p!
                  (#(>evt [:load-backup-keys])))
              (catch js/Object e
                (tap> (str "error deleting backup " e))
                (-> rn/Alert (j/call :alert "error deleting backup " (str e))))))))

(reg-fx :restore-backup
        (fn [k]
          (go
            (try
              (-> async-storage
                  (j/get :default)
                  (j/call :getItem k)
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
              (-> async-storage
                  (j/get :default)
                  (j/call :getItem k)
                  <p!
                  ((fn [local-store-value]
                     (if (some? local-store-value)
                       (-> rn/Share (j/call :share #js {:message local-store-value :title k}))
                       (-> rn/Alert (j/call :alert "Unable to export backup")))
                     )))
              (catch js/Object e
                (tap> (str "error exporting backup " e))
                (-> rn/Alert (j/call :alert "error exporting backup " (str e))))))))

;; Some helpful repl stuff
(comment
  ;; Hard reset app-db
  (do
    (>evt [:stop-ticking])
    (>evt [:initialize-db])
    (>evt [:save-db]))

  )
