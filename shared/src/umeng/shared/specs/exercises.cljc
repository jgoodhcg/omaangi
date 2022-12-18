(ns umeng.shared.specs.exercises
  (:require [spec-tools.data-spec :as ds]
            [tick.core :as t]
            [clojure.spec.alpha :as s]))

;; 2022-12-03 Justin should match the data spec symbol
;; but I can't figure out how to enforce that within the data spec right now
;; also I don't know if I __should__ enforce it within the data spec
;; it's kind of a spec selector attribute (somehwat like a multi method)
(s/def ::type (s/spec #{:exercise :exercise-log :exercise-session}))

(def exercise
  {(ds/req :xt/id)          uuid?
   (ds/req :umeng/type)     ::type
   (ds/req :exercise/label) string?
   :exercise/notes          string?
   :exercise/source         string?
   :exercise/body-areas     [keyword?]})

(def exercise-session
  {(ds/req :xt/id)                               uuid?
   (ds/req :umeng/type)                          ::type
   (ds/req :exercise-session.interval/beginning) t/instant?
   (ds/req :exercise-session.interval/end)       t/instant?
   :exercise-session/notes                       string?
   :exercise-session/relativety-score            keyword?})

(def exercise-log
  {(ds/req :xt/id)                  uuid?
   (ds/req :umeng/type)             ::type
   ;; Eventually I would like to have a top leve location or "starting location"
   ;; and then a series of gps coordinates within data
   ;; using something like https://github.com/Factual/geo
   (ds/req :exercise-session/id)    uuid?
   :exercise-log.interval/beginning t/instant?
   :exercise-log.interval/end       t/instant?
   :exercise-log/notes              string?
   :exercise-log/relativety-score   keyword?
   (ds/req :exercise-log/data)      [{(ds/req :exercise/id )                 uuid?
                                      :exercise-log.data.interval/beginning  t/instant?
                                      :exercise-log.data.interval/end        t/instant?
                                      :exercise-log.data/sets                integer?
                                      :exercise-log.data/reps                integer?
                                      :exercise-log.data/weight              float?
                                      :exercise-log.data/weight-unit         keyword?
                                      :exercise-log.data/distance            float?
                                      :exercise-log.data/distance-unit       keyword?
                                      :exercise-log.data/elevation-gain      float?
                                      :exercise-log.data/elevation-gain-unit keyword?
                                      :exercise-log.data/inversion-angle     float?
                                      :exercise-log.data/notes               string?
                                      :exercise-log.data/relativety-score    keyword?
                                      }]})

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
