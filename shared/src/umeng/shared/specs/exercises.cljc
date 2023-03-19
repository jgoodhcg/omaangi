(ns umeng.shared.specs.exercises
  (:require [spec-tools.data-spec :as ds]
            [tick.core :as t]
            [clojure.spec.alpha :as s]))

;; 2022-12-03 Justin - should match the data spec symbol
;; but I can't figure out how to enforce that within the data spec right now
;; also I don't know if I __should__ enforce it within the data spec
;; it's kind of a spec selector attribute (somehwat like a multi method)
(s/def ::type (s/spec #{:exercise :exercise-log :exercise-session :exercise-set :exercise-group}))

(def exercise
  {(ds/req :xt/id)               uuid?
   (ds/req :umeng/type)          ::type
   (ds/req :exercise/label)      string?
   :exercise/notes               string?
   :exercise/source              string?
   :exercise/body-areas          [keyword?]
   :exercise/group-ids           [uuid?]
   :exercise/default-weight-unit keyword?
   :airtable/ported              boolean?
   :airtable/created-time        string?
   :airtable/distance-unit       string?
   :airtable/id                  string?
   :airtable/exercise-log        [string?]
   :airtable/weight-unit         string?
   :airtable/log-count           integer?
   :airtable/latest-done         string?})

(def exercise-group
  {(ds/req :xt/id)                uuid?
   (ds/req :umeng/type)           ::type
   (ds/req :exercise-group/label) string?
   :exercise-group/notes          string?
   :exercise-group/exercise-ids   [uuid?]})

(def exercise-session
  {(ds/req :xt/id)                               uuid?
   (ds/req :umeng/type)                          ::type
   (ds/req :exercise-session.interval/beginning) t/instant?
   (ds/req :exercise-session.interval/end)       t/instant?
   (ds/req :exercise-session/exercise-log-ids)   [uuid?]
   :exercise-session/notes                       string?
   :exercise-session/relativety-score            keyword?
   :airtable/ported                              boolean?})

(def exercise-superset
  {(ds/req :xt/id)                              uuid?
   (ds/req :exercise-superset/exercise-log-ids) [uuid?]})

;; Eventually I would like to have a top leve location or "starting location"
;; and then a series of gps coordinates within data
;; using something like https://github.com/Factual/geo
(def exercise-log
  {(ds/req :xt/id)                       uuid?
   (ds/req :umeng/type)                  ::type
   (ds/req :exercise-session/id)         uuid?
   (ds/req :exercise/id)                 uuid?
   :exercise-log.interval/beginning      t/instant?
   :exercise-log.interval/end            t/instant?
   :exercise-log/set-ids                 [uuid?]
   :exercise-log/distance                float?
   :exercise-log/distance-unit           keyword?
   :exercise-log/elevation-gain          float?
   :exercise-log/elevation-gain-unit     keyword?
   :exercise-log/inversion-angle         float?
   :exercise-log/notes                   string?
   :exercise-log/relativety-score        keyword?
   :airtable/exercise-id                 string?
   :airtable/id                          string?
   :airtable/ported                      boolean?
   :airtable/average-duration            boolean?
   :airtable/average-of-average-duration boolean?})

(def exercise-set
  {(ds/req :xt/id)             uuid?
   (ds/req :umeng/type)        type
   (ds/req :exercise-log/id)   uuid?
   (ds/req :exercise-set/reps) integer?
   :exercise-set/beginning     t/instant?
   :exercise-set/end           t/instant?
   :exercise-set/weight-amount float?
   :exercise-set/weight-unit   keyword?})

(def exercise-spec
  (ds/spec
   {:name         ::exercise-spec
    :spec         exercise
    :keys-default ds/opt}))

(def exercise-log-spec
  (ds/spec
   {:name         ::exercise-log-spec
    :spec         exercise-log
    :keys-default ds/opt}))

(def exercise-session-spec
  (ds/spec
   {:name         ::exercise-session-spec
    :spec         exercise-session
    :keys-default ds/opt}))

(def exercise-set-spec
  (ds/spec
   {:name         ::exercise-session-spec
    :spec         exercise-set
    :keys-default ds/opt}))

(def exercise-group-spec
  (ds/spec
   {:name         ::exercise-group-spec
    :spec         exercise-group
    :keys-default ds/opt}))
