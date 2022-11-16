(ns umeng.tooling.bb.airtable-port
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.set :refer [rename-keys]]))

(def api-key (first *command-line-args*))

(defn get-all-records [db-id table-id api-key]
  (let [url      (str "https://api.airtable.com/v0/" db-id "/" table-id)]
    (loop [response (-> (curl/get
                         url
                         {:headers {"Authorization" (str "Bearer " api-key)}})
                        (:body)
                        (json/parse-string))
           records []]
      (let [offset (-> response (get "offset"))]
        (if (nil? offset)
          (concat records (-> response (get "records")))
          (recur
           (-> (curl/get
                (str url "?offset=" offset)
                {:headers {"Authorization" (str "Bearer " api-key)}})
               (:body)
               (json/parse-string))
           (concat records (-> response (get "records")))))))))

(-> (get-all-records "appcu3LYk0kLQ2f6A" "tblXXCTTcI1jJjdIO" api-key)
    #_(->> (map #(get % "fields")))
    #_(->> (apply merge))
    first
    #_(rename-keys {})
    #_(spit "exercise-log.edn")
    println
    )

(comment
  ;; all fields for exercise-log
  '("total-weight"
    "Angle"
    "exercise"
    "reps"
    "timestamp"
    "source (from exercise)"
    "duration minutes"
    "distance"
    "better than normal set"
    "duration"
    "worse than normal set"
    "notes"
    "day"
    "weight")

  ;; shape of documents incoming
  {"id"          "rec00CSCBqhgUJtxF"
   "createdTime" "2022-02-28T20:18:52.000Z"
   "fields"      {"total-weight"           ""
                  "Angle"                  ""
                  "exercise"               ""
                  "reps"                   ""
                  "timestamp"              ""
                  "source (from exercise)" ""
                  "duration minutes"       ""
                  "distance"               ""
                  "better than normal set" ""
                  "duration"               ""
                  "worse than normal set"  ""
                  "notes"                  ""
                  "day"                    ""
                  "weight"                 ""}}

  ;; shape of documents output
  {:xt/id             #uuid "ec5b2c43-f7be-4603-b601-a9f6b64fd14b"
   :type              :exercise-log
   :exercise/id       #uuid "5a31abf1-5af9-4825-afc0-3206b80de0ed"
   :timestamp         #inst "2022-10-24T09:20:27.966-00:00" ;; I'm tempted to use valid time instead
   :duration          #time/duration "PT1M40S"
   :notes             "Focused on keeping my quads engaged"
   :relativety-score  :relativety-score/better
   :exercise-log/data [{:sets 1 :reps 2 :weight 3 :weight-unit "lbs" :duration #time/duration "PT1M40S"}
                           ;; could also include any of these keys
                           ;; semantically only weight and weight-unit would be in either type
                       {:distance 12 :distance-unit "miles" :elevation-gain "" :elevation-gain-unit "" :duration #time/duration "PT1M40S"}
                           ;; this one is specific to inversion table but with a schemaless I can use whatever
                       {:angle 60 :duration #time/duration "PT1M40S"}]}
)
