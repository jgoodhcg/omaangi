(ns specs
  (:require [spec-tools.data-spec :as ds]
            [tick.core :as t]))

#_(def person
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

#_(def exercise-log-spec
  (ds/spec
    {:name ::person
     :spec person}))

(def exercise
  {:xt/id               uuid?
   :umeng/type          keyword?
   :exercise/label      string?
   :exercise/notes      string?
   :exercise/source     string?
   :exercise/body-areas [keyword?]
   })

(def exercise-log
  {:xt/id                           uuid?
   :umeng/type                      keyword?
   ;; :exercise/id                   uuid? ;; this should go in each data element
   ;; :exercise-log/location ;; use something like https://github.com/Factual/geo
   ;; Eventually I could see tracking GPS coordinates and creating something like a geohash or circle containing all points
   :exercise-session/id             uuid?
   :exercise-log.interval/beginning t/instant?
   :exercise-log.interval/end       t/instant?
   :exercise-log/notes              string?
   :exercise-log/relativety-score   keyword?
   :exercise-log/data               [{:exercise/id                                    uuid?
                                      (ds/opt :exercise-log.data.interval/beginning)  t/instant?
                                      (ds/opt :exercise-log.data.interval/end)        t/instant?
                                      (ds/opt :exercise-log.data/sets)                integer?
                                      (ds/opt :exercise-log.data/reps)                integer?
                                      (ds/opt :exercise-log.data/weight)              float?
                                      (ds/opt :exercise-log.data/weight-unit)         keyword?
                                      (ds/opt :exercise-log.data/distance)            float?
                                      (ds/opt :exercise-log.data/distance-unit)       keyword?
                                      (ds/opt :exercise-log.data/elevation-gain)      float?
                                      (ds/opt :exercise-log.data/elevation-gain-unit) keyword?
                                      (ds/opt :exercise-log.data/inversion-angle)     float?
                                      (ds/opt :exercise-log.data/notes)               string?}]})
