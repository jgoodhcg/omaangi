(ns app.handlers
  (:require
   [re-frame.core :refer [reg-event-db
                          ->interceptor
                          reg-event-fx
                          dispatch
                          debug]]
   [com.rpl.specter :as sp :refer [select select-one setval transform selected-any?]]
   [clojure.spec.alpha :as s]
   [app.db :as db :refer [default-app-db app-db-spec]]))

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

(defn set-tag-removal [db [_ tag-removal-state]]
  (tap> {:sub                        "handler"
         :tag-removal-state-incoming tag-removal-state
         :db-state-b4                (->> db (select-one [:view :view/tag-removal]))
         :db-state-after             (->> (->> db (setval [:view :view/tag-removal] tag-removal-state))
                                          (select-one [:view :view/tag-removal]))})
  (->> db (setval [:view :view/tag-removal] tag-removal-state)))
(reg-event-db :set-tag-removal set-tag-removal)
