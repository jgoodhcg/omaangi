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
   [tick.alpha.api :as t]))

(defn check-and-throw
  "Throw an exception if db doesn't have a valid spec."
  [spec db event]
  (when-not (s/valid? spec db)
    (let [explanation (s/explain-str spec db)]
      (throw (str "Spec check failed: " explanation))
      true)))

(defn validate-spec [context]
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

(defn initialize-db [_ _]
  default-app-db)
(reg-event-db :initialize-db [base-interceptors] initialize-db)

(defn set-theme [db [_ theme]]
  (->> db
       (setval [:settings :theme] theme)))
(reg-event-db :set-theme [base-interceptors] set-theme)

(defn set-version [db [_ version]]
  (->> db
       (setval [:version] version)))
(reg-event-db :set-version [base-interceptors] set-version)

(defn some-fx-example [cofx [_ x]]
  {:db              (:db cofx)
   :some-fx-example x})
(reg-event-fx :some-fx-example [base-interceptors] some-fx-example)

(defn navigate [cofx [_ screen-name]]
  {:db       (:db cofx)
   :navigate screen-name})
(reg-event-fx :navigate [base-interceptors] navigate)

(defn set-tag-remove-modal [db [_ new-state]]
  (->> db (setval [:view :view/tag-remove-modal] new-state)))
(reg-event-db :set-tag-remove-modal set-tag-remove-modal)

(defn set-tag-add-modal [db [_ new-state]]
  (->> db (setval [:view :view/tag-add-modal] new-state)))
(reg-event-db :set-tag-add-modal set-tag-add-modal)

(defn set-date-time-picker [db [_ new-state]]
  (->> db (setval [:view :view/date-time-picker] new-state)))
(reg-event-db :set-date-time-picker set-date-time-picker)

(defn set-color-picker [db [_ new-state]]
  (tap> new-state)
  (->> db (setval [:view :view/color-picker]
                  (->> new-state (transform [:color-picker/value]
                                            #(if (some? %)
                                               (color %)
                                               nil))))))
(reg-event-db :set-color-picker set-color-picker)

(defn set-selected-day [db [_ new-date-inst]]
  (->> db (setval [:view :view/selected-day] (-> new-date-inst t/date))))
(reg-event-db :set-selected-day set-selected-day)
