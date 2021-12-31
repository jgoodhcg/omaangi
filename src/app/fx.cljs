(ns app.fx
  (:require
   ["@react-native-async-storage/async-storage" :as async-storage]
   ["expo-constants" :as expo-constants]
   ["expo-file-system" :as expo-file-system]
   ["expo-sharing" :as expo-sharing]
   ["react-native" :as rn]

   [applied-science.js-interop :as j]
   [com.rpl.specter :as sp :refer [select transform]]
   [cljs.core.async :refer [go <!]]
   [cljs.core.async.interop :refer [<p!]]
   [clojure.set :refer [subset?]]
   [re-frame.core :refer [reg-fx]]
   [potpuri.core :as p]
   [tick.alpha.api :as t]

   [app.misc :refer [>evt
                     >evt-sync
                     sessions->min-col
                     smoosh-sessions
                     combine-tag-labels
                     hex-if-some
                     replace-tag-refs-with-objects
                     set-session-ish-color
                     mix-tag-colors]]
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

(reg-fx :check-for-saved-db
        (fn [_]
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
                                                      (->> (merge default-app-db )))]))))
                           (catch js/Object e
                             (-> rn/Alert (j/call :alert "Failure on readAsStringAsync" (str e))))))))))
              (catch js/Object e
                (-> rn/Alert (j/call :alert "Failure on getInfoAsync" (str e))))))       ))

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
                          (str backups-dir (t/date-time timestamp) "--" version ".edn")
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

(defn generate-pie-chart-data
  [{:keys [calendar sessions tags report-interval tag-groups]}]
  (go
    (let [{beg-intrvl :app-db.reports/beginning-date
           end-intrvl :app-db.reports/end-date}
          report-interval
          tag-groups      (->> tag-groups
                               (select [sp/MAP-VALS])
                               (transform [sp/ALL] #(replace-tag-refs-with-objects tags %)))
          days            (vec (t/range beg-intrvl
                                        (t/+ end-intrvl
                                             (t/new-period 1 :days))))
          session-ids     (->> calendar
                               (select [(sp/submap days)
                                        sp/MAP-VALS
                                        :calendar/sessions])
                               flatten
                               set
                               vec)
          sessions-tagged (->> sessions
                               (select [(sp/submap session-ids)
                                        sp/MAP-VALS])
                               (filter #(= :session/track (:session/type %)))
                               (smoosh-sessions)
                               (mapv (partial replace-tag-refs-with-objects tags))
                               (mapv (partial set-session-ish-color {:hex true})))
          tg-matched      (->> sessions-tagged
                               (mapv (fn [{session-tags :session/tags :as session-tagged}]
                                       (let [this-session-tags (->> session-tags
                                                                    (mapv :tag/id)
                                                                    set)
                                             tag-group         (->> tag-groups
                                                                    (some
                                                                      #(let [tg-set
                                                                             (->> %
                                                                                  (select [:tag-group/tags
                                                                                           sp/ALL
                                                                                           :tag/id])
                                                                                  set)
                                                                             strict-match (:tag-group/strict-match %)]
                                                                         (when (and (seq tg-set) ;; not empty
                                                                                    (if strict-match
                                                                                      (= tg-set this-session-tags)
                                                                                      (subset? tg-set this-session-tags) )
                                                                                    ) %))))
                                             tag-group-id      (:tag-group/id tag-group)
                                             match             (when (some? tag-group-id)
                                                                 {:tag-group/id        tag-group-id
                                                                  :tag-group/color     (->> tag-group
                                                                                            :tag-group/tags
                                                                                            (mapv :tag/color)
                                                                                            mix-tag-colors
                                                                                            :mixed-color
                                                                                            hex-if-some)
                                                                  :combined-tag-labels (-> tag-group
                                                                                           :tag-group/tags
                                                                                           combine-tag-labels)})]
                                         (merge session-tagged match)))))
          has-tg?         (fn [{tag-group-id :tag-group/id}] (some? tag-group-id))
          total-time      (-> (t/between
                                (-> beg-intrvl (t/at (t/time "00:00")) (t/instant))
                                (-> end-intrvl (t/at (t/time "23:59")) (t/instant)))
                              (t/minutes))
          other-time      (->> tg-matched
                               (remove has-tg?)
                               (sessions->min-col)
                               (reduce +))
          matched-total   (->> tg-matched
                               (filter has-tg?)
                               (sessions->min-col)
                               (reduce +))
          chart-config    {:legendFontColor "#7f7f7f"
                           :legendFontSize  15}
          results         (->> tg-matched
                               (filter has-tg?)
                               (group-by :tag-group/id)
                               vals
                               (mapv (fn [session-group]
                                       (merge chart-config
                                              {:name  (-> session-group first :combined-tag-labels)
                                               :min   (->> session-group
                                                           (sessions->min-col)
                                                           (reduce +))
                                               :color (-> session-group first :tag-group/color)}))))
          final-results   (-> results
                              (conj (merge chart-config
                                           {:name  "Untracked"
                                            :min   (-> total-time (- other-time) (- matched-total))
                                            :color "#242424"}))
                              (conj (merge chart-config
                                           {:name  "Other"
                                            :min   other-time
                                            :color "#a0a0a0"})))]
      (>evt [:set-pie-chart-data final-results]))))

(reg-fx :generate-pie-chart-data
        (fn [args]
          ;; timeout is a hack to allow for re-render and displaying the loading component
          (-> #(generate-pie-chart-data args)
              (js/setTimeout 500))))

;; Some helpful repl stuff
(comment
  ;; Hard reset app-db
  (do
    (>evt [:stop-ticking])
    (>evt [:initialize-db])
    (>evt [:save-db]))

  )
