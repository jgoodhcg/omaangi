(ns specs
  (:require [spec-tools.data-spec :as ds]))

(def person
  {::id integer?
   ::age ::age
   :boss boolean?
   (ds/req :name) string?
   (ds/opt :description) string?
   :languages #{keyword?}
   :aliases [(ds/or {:maps {:alias string?}
                     :strings string?})]
   :orders [{:id int?
             :description string?}]
   :address (ds/maybe
              {:street string?
               :zip string?})})

(def exercise-log
  {:xt/id             #uuid "ec5b2c43-f7be-4603-b601-a9f6b64fd14b"
   :type              :exercise-log
   :exercise/id       #uuid "5a31abf1-5af9-4825-afc0-3206b80de0ed"
   :timestamp         #inst "2022-10-24T09:20:27.966-00:00" ;; I'm tempted to use valid time instead
   :duration          #time/duration "PT1M40S"
   :notes             "Focused on keeping my quads engaged"
   :relativety-score  :relativety-score/better
   :exercise-log/data [{:sets 1 :reps 2 :weight 3 :weight-unit "lbs"}
                           ;; could also include any of these keys
                           ;; semantically only weight and weight-unit would be in either type
                       {:distance 12 :distance-unit "miles" :elevation-gain "" :elevation-gain-unit ""}
                           ;; this one is specific to inversion table but with a schemaless I can use whatever
                       {:angle 60}]})

(def exercise-log-spec
  (ds/spec
    {:name ::person
     :spec person}))
