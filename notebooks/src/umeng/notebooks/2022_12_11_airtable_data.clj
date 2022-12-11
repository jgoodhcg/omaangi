(ns umeng.notebooks.2022-12-11-airtable-data
  (:require [clojure.edn :as edn]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]
            [potpuri.core :as pot]))

;; ## Data manipulation

;; #### Exercises
;; Straight forward and don't rely on anything
(def exercises-raw
  (-> "data/2022_12_11__15_16_exercises.edn"
      slurp
      edn/read-string))

;; A raw item example
(-> exercises-raw rand-nth)

;; Simple transformation to `umeng.shared.specs.exercises/exercise` spec
(defn xform-exercise [{:keys [id created-time fields]}]
  (let [{:keys [name
                notes
                distance-unit
                weight-unit
                exercise-log
                source
                log-count
                latest-done]} fields
        new-uuid              (java.util.UUID/randomUUID)]
    {:xt/id                  new-uuid
     :umeng/type             :exercise
     :exercise/label         name
     :exercise/notes         notes
     :exercise/source        source
     :airtable/ported        true
     :airtable/created-time  created-time
     :airtable/distance-unit distance-unit
     :airtable/id            id
     :airtable/exercise-log  exercise-log
     :airtable/weight-unit   weight-unit
     :airtable/log-count     log-count
     :airtable/latest-done   latest-done}))

;; Viola!
(-> exercises-raw
    (->> (mapv #(cske/transform-keys csk/->kebab-case-keyword %)))
    rand-nth
    xform-exercise)

;; #### Exercise Logs
;; This is tricky
;; Sessions are introduced at this level of the data model
;; We can assume every log has a session of just that log
;; Each log has `:exercise-log/data` which has some attributes like `:exercise-log.data/weight-unit`
;; Previously attributes like that were on the exercise item itself
;; Now we will need to look them up in the exercises to determine them for the log data based on some preserved airtable attributes
(def exercise-log-raw (slurp "data/2022_12_11__15_17_exercise_log.edn"))
