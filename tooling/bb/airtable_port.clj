(ns umeng.tooling.bb.airtable-port
  (:require [babashka.curl :as curl]
            [cheshire.core :as json]
            [clojure.set :refer [rename-keys]]
            [clojure.pprint :refer [pprint]]))

(def api-key (first *command-line-args*))

(defn get-all-records [base-id table-id api-key]
  (let [url      (str "https://api.airtable.com/v0/" base-id "/" table-id)]
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

(def base-id "appcu3LYk0kLQ2f6A")

(def table-ids {:exercise-log "tblXXCTTcI1jJjdIO"
                :exercises    "tblDLGrkK6jfT2fPU"})

(-> (get-all-records base-id (:exercise-log table-ids) api-key)
    #_(->> (map #(get % "fields")))
    #_(->> (apply merge))
    #_first
    #_(rename-keys {})
    #_(spit "exercise-log.edn")
    #_println
    (#(with-out-str (pprint %)))
    #_(->> (spit "2022_12_11__15_16_exercises.edn"))
    (->> (spit "2022_12_11__15_17_exercise-log.edn"))
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
)
