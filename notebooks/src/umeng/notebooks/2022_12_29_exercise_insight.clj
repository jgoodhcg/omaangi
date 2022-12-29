;; # Exercise Insight
(ns umeng.notebooks.2022-12-29-exercise-insight
  (:require [clojure.edn :as edn]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [potpuri.core :as pot]
            [umeng.shared.data-xform.airtable-exercises :refer [xform-exercise]]
            [umeng.shared.specs.exercises :refer [exercise-spec
                                                  exercise-log-spec
                                                  exercise-session-spec]]
            [clojure.spec.alpha :as s]
            [tick.core :as t]
            [com.rpl.specter :as sp]
            [kixi.stats.core :refer [mean]]
            [clojure.pprint :refer [pprint]]))

;; ## Get the data
(def data
  (-> "data/2022_12_11__15_16_exercises_logs_sessions_xformed.edn"
      slurp
      (->> (edn/read-string {:readers {'time/instant t/instant}}))))

;; ## Data examples
;; ### Exercise
(-> data :exercise rand-nth)
;; ### Exercise Log
(-> data :exercise-log rand-nth)
;; ### Exercise Session
(-> data :exercise-session rand-nth)
