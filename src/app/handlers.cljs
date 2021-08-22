(ns app.handlers
  (:require
   ["color" :as color]
   [re-frame.core :refer [reg-event-db
                          ->interceptor
                          reg-event-fx
                          dispatch
                          debug]]
   [com.rpl.specter :as sp :refer [select select-one setval transform selected-any?]]
   [clojure.spec.alpha :as s]
   [app.db :as db :refer [default-app-db app-db-spec]]
   [tick.alpha.api :as t]
   [potpuri.core :as p]
   [app.helpers :refer [make-color-if-some]]))

(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db event]
  (when-not (s/valid? spec db)
    (let [explanation (s/explain-str spec db)]
      (throw (str "Spec check failed: " explanation))
      true)))

(defn validate-spec
  [context]
  (let [db     (-> context :effects :db)
        old-db (-> context :coeffects :db)
        event  (-> context :coeffects :event)]

    (if (some? (check-and-throw app-db-spec db event))
      (assoc-in context [:effects :db] old-db)
      ;; put the old db back as the new db when check fails
      ;; otherwise return context unchanged
      context)))

(def spec-validation
  (if goog.DEBUG
    (->interceptor
      :id :spec-validation
      :after validate-spec)
    ->interceptor))

(def base-interceptors  [;; (when ^boolean goog.DEBUG debug) ;; use this for some verbose re-frame logging
                         spec-validation])

(defn initialize-db
  [_ _]
  default-app-db)
(reg-event-db :initialize-db [base-interceptors] initialize-db)

(defn set-theme
  [db [_ theme]]
  (->> db (setval [:app-db.settings/theme] theme)))
(reg-event-db :set-theme [base-interceptors] set-theme)

(defn set-version
  [db [_ version]]
  (->> db (setval [:app-db/version] version)))
(reg-event-db :set-version [base-interceptors] set-version)

(defn navigate
  [cofx [_ screen-name]]
  {:db       (:db cofx)
   :navigate screen-name})
(reg-event-fx :navigate [base-interceptors] navigate)

(defn set-tag-remove-modal
  [db [_ {:tag-remove-modal/keys [id visible label]
          hex-color              :tag-remove-modal/color}]]
  (->> db
       (setval [:app-db.view.tag-remove-modal/id] id)
       (setval [:app-db.view.tag-remove-modal/visible] visible)
       (setval [:app-db.view.tag-remove-modal/label] label)
       (setval [:app-db.view.tag-remove-modal/color] (color hex-color))))
(reg-event-db :set-tag-remove-modal set-tag-remove-modal)

(defn set-tag-add-modal
  [db [_ {:tag-add-modal/keys [visible]}]]
  (->> db (setval [:app-db.view.tag-add-modal/visible] visible)))
(reg-event-db :set-tag-add-modal set-tag-add-modal)

(defn set-date-time-picker
  [db [_ {:date-time-picker/keys [visible
                                  value
                                  mode
                                  session-id
                                  id
                                  field-key]}]]
  (->> db
       (setval [:app-db.view.date-time-picker/field-key] field-key)
       (setval [:app-db.view.date-time-picker/session-id] session-id)
       (setval [:app-db.view.date-time-picker/mode] mode)
       (setval [:app-db.view.date-time-picker/value]
               (cond
                 (t/date? value)    (-> value (t/at (t/time "00:00")) t/inst)
                 (t/instant? value) (-> value t/inst)
                 :else              value))
       (setval [:app-db.view.date-time-picker/visible] visible)
       (setval [:app-db.view.date-time-picker/id] id)))
(reg-event-db :set-date-time-picker set-date-time-picker)

(defn set-color-picker
  [db [_ {:color-picker/keys [visible value]}]]
  (->> db
       (setval [:app-db.view.color-picker/value]
               (if (some? value) (color value) nil))
       (setval [:app-db.view.color-picker/visible] visible)))
(reg-event-db :set-color-picker set-color-picker)

(defn set-selected-day
  [db [_ new-date-inst]]
  (->> db
       (setval [:app-db.selected/day] (-> new-date-inst t/date))))
(reg-event-db :set-selected-day set-selected-day)

(defn set-selected-session
  [db [_ session-id]]
  (->> db
       (setval [:app-db.selected/session] session-id)))
(reg-event-db :set-selected-session set-selected-session)

(defn update-session
  "This is not meant to be used with tags, just label start stop type color"
  [db [_ {:session/keys [id color-hex] :as session}]]
  (let [c       (make-color-if-some color-hex)
        session (-> session
                    (dissoc :session/color-hex)
                    (p/update-if-contains :session/start t/instant)
                    (p/update-if-contains :session/stop t/instant))]
    (tap> (p/map-of color-hex session))
    (->> db
         (transform [:app-db/sessions (sp/keypath id)]
                    #(merge %
                            session
                            (when (some? c) {:session/color c}) )))))
(reg-event-db :update-session update-session)

(defn add-tag-to-session
  [db [_ {session-id :session/id
          tag-id     :tag/id}]]
  (->> db
       (transform [:app-db/sessions (sp/keypath session-id) :session/tags]
                  #(conj % tag-id))))
(reg-event-db :add-tag-to-session add-tag-to-session)

(defn remove-tag-from-session
  [db [_ {session-id :session/id
          tag-id     :tag/id}]]
  (->> db
       (transform [:app-db/sessions (sp/keypath session-id) :session/tags]
                  (fn [tags] (->> tags (remove #(= % tag-id)) vec)))))
(reg-event-db :remove-tag-from-session remove-tag-from-session)

;; TODO this is totally untested
(defn set-initial-timestamp
  [db [_ {:keys      [set-start set-stop]
          session-id :session/id}]]
  (->> db
       (transform [:app-db/sessions (sp/keypath session-id)]
                  (fn [{:session/keys [start stop]
                        :as           session}]
                    (merge session
                           (when (and set-start
                                      (nil? start))
                             (if (some? stop)
                               {:session/start
                                (-> stop
                                    (t/- (t/new-duration 60 :minutes)))}
                               ;; TODO inject now
                               {:session/start (t/now)
                                :session/stop  (-> (t/now) (t/+ 60 :minutes))}))
                           (when (and set-stop
                                      (nil? stop))
                             (if (some? start)
                               {:session/stop
                                (-> start
                                    (t/+ (t/new-duration 60 :minutes)))}
                               ;; TODO inject now
                               {:session/start (t/now)
                                :session/stop  (-> (t/now) (t/+ 60 :minutes))})))))))
(reg-event-db :set-initial-timestamp set-initial-timestamp)
