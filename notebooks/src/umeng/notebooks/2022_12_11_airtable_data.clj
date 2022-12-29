(ns umeng.notebooks.2022-12-11-airtable-data
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

;; # Airtable -> Umeng/types

;; ## Exercises
;; Straight forward and don't rely on anything
(def exercises-raw
  (-> "data/2022_12_11__15_16_exercises.edn"
      slurp
      edn/read-string))

;; ### Example of a raw exercise
(-> exercises-raw rand-nth)

(def exercises
  (-> exercises-raw
      (->> (mapv #(cske/transform-keys csk/->kebab-case-keyword %)))
      (->> (remove (fn [{:keys [fields]}] (-> fields :name (#(or (nil? %) (empty? %)))))))
      (->> (mapv xform-exercise))))

;; ### Example of xform exercise
(-> exercises rand-nth)

;; ### All valid exercise xforms?
(->> exercises
     (mapv #(s/explain-data exercise-spec %))
     (filter some?)
     empty?)

;; ### Indexing exercises for log processing
(def exercises-indexed
  (-> exercises
      (->> (group-by :airtable/id))
      (->> (pot/map-vals first))))

;; ## Exercise Logs
;; This is tricky
;; ######
;; Sessions are introduced at this level of the data model.
;; ######
;; We can assume every log has a session of just that log.
;; ######
;; Logs can have attributes like `:exercise-log.set/weight-unit`.
;; ######
;; Previously attributes like that were on the exercise item itself.
;; ######
;; Now we will need to look them up in the exercises to determine them for the log data based on some preserved airtable attributes.
(def exercise-logs-raw
  (-> "data/2022_12_11__15_17_exercise_log.edn"
      slurp
      edn/read-string))

;; ### Example of raw log
(-> exercise-logs-raw rand-nth)

(defn xform-exercise-log
  [{:keys [id fields]}]
  (let [{:keys
         [timestamp
          exercise
          duration
          angle
          reps
          weight
          distance
          notes
          better-than-normal-set
          worse-than-normal-set]
         is-average         :airtable/average-duration
         is-average-average :airtable/average-of-average-duration} fields

        end           (t/instant timestamp)
        beginning     (-> end (t/<< (t/new-duration duration :seconds)))
        airtable-e-id (first exercise)
        exercise-item (get exercises-indexed airtable-e-id)
        e-id          (get exercise-item :xt/id) ;; desctructing these throws an error for some weird reason
        e-weight-unit (get exercise-item :airtable/weight-unit)
        relativety    (if (some? better-than-normal-set)
                        :better
                        (if (some? worse-than-normal-set)
                          :worse
                          nil))]

    (-> {:xt/id                           (java.util.UUID/randomUUID)
         :umeng/type                      :exercise-log
         :exercise-session/id             (java.util.UUID/randomUUID)
         :exercise/id                     e-id
         :exercise-log.interval/beginning beginning
         :exercise-log.interval/end       end
         :airtable/id                     id
         :airtable/exercise-id            airtable-e-id
         :airtable/ported                 true}

        (merge (when (some? reps)
                 {:exercise-log/sets [(-> {:exercise-log.set/reps reps}
                                          (merge (when (some? weight)
                                                   {:exercise-log.set/weight
                                                    {:exercise-log.set.weight/amount      (float weight)
                                                     :exercise-log.set.weight/weight-unit (or (keyword e-weight-unit)
                                                                                              :lbs)}})))]}))
        (merge (when (some? distance)           {:exercise-log/distance      (float distance)
                                                 :exercise-log/distance-unit :miles}))
        (merge (when (some? angle)              {:exercise-log/inversion-angle (float angle)}))
        (merge (when (some? notes)              {:exercise-log/notes notes}))
        (merge (when (some? relativety)         {:exercise-log/relativety-score (keyword relativety)}))
        (merge (when (some? is-average)         {:airtable/average-duration true}))
        (merge (when (some? is-average-average) {:airtable/average-of-average-duration true})))))

(def exercise-logs-with-durations
  (-> exercise-logs-raw
      (->> (mapv #(cske/transform-keys csk/->kebab-case-keyword %))) ;; 3930
      (->> (remove #(-> % :fields :exercise nil?))) ;; 3867
      (->> (remove #(-> % :fields :timestamp nil?))) ;; 3867
      (->> (remove #(-> % :fields :duration nil?))) ;; 1776 (these are still valid though)
      (->> (mapv xform-exercise-log))))

;; ### Example of log xform
(-> exercise-logs-with-durations rand-nth)

(def average-durations
  (-> exercise-logs-with-durations
      (->> (group-by :airtable/exercise-id))
      (->> (pot/map-vals
            (fn [[& e-logs]]
              (->> e-logs
                   (transduce
                    (map (fn [e-log]
                           (let [beg (get-in e-log [:exercise-log.interval/beginning])
                                 end (get-in e-log [:exercise-log.interval/end])]
                             (-> (t/between beg end) t/seconds))))
                    mean)))))))

;; Instead of just taking the average we could categorize and take the average of similar exercises
;; This is way lazier though
(def average-of-average-durations
  (transduce identity mean (vals average-durations)))

(def exercise-logs-without-durations
  (-> exercise-logs-raw
      (->> (mapv #(cske/transform-keys csk/->kebab-case-keyword %)))
      (->> (remove #(-> % :fields :exercise nil?)))
      (->> (remove #(-> % :fields :timestamp nil?)))
      (->> (filter #(-> % :fields :duration nil?)))
      (->> (sp/transform [sp/ALL :fields]
                         (fn [{:keys [exercise] :as fields}]
                           (let [a-e-id  (first exercise)
                                 average (get average-durations a-e-id)]
                             (-> fields
                                 (merge (when (some? average) {:duration average
                                                               :airtable/average-duration true}))
                                 (merge (when (nil? average) {:duration average-of-average-durations
                                                              :airtable/average-of-average-duration true})))))))

      (->> (mapv xform-exercise-log))))

;; ### Example of log xform no durations
(-> exercise-logs-without-durations rand-nth)

(def invalid-exercise-logs
  (-> exercise-logs-raw
      (->> (mapv #(cske/transform-keys csk/->kebab-case-keyword %)))
      (->> (filter #(or (-> % :fields :timestamp nil?)
                        (-> % :fields :exercise nil?))))))

(def final-exercise-logs
  (-> [exercise-logs-with-durations exercise-logs-without-durations] flatten))

;; ### All valid log xforms?
(->> final-exercise-logs
     (mapv #(s/explain-data exercise-log-spec %))
     (filter some?)
     empty?)

;; ### Does everything add up?
(count exercise-logs-raw)
(count exercise-logs-without-durations)
(count exercise-logs-with-durations)
(count invalid-exercise-logs)
(= (count exercise-logs-raw)
   (+ (count exercise-logs-without-durations)
      (count exercise-logs-with-durations)
      (count invalid-exercise-logs))
   (+ (count final-exercise-logs)
      (count invalid-exercise-logs)))

;; ## Exercise Sessions
;; These are derived from the logs. There is no concept of session in airtable.

(defn exercise-log->session [{xt-id :exercise-session/id
                              beg   :exercise-log.interval/beginning
                              end   :exercise-log.interval/end
                              el-id :xt/id}]

  {:xt/id                               xt-id
   :umeng/type                          :exercise-session
   :exercise-session.interval/beginning beg
   :exercise-session.interval/end       end
   :exercise-session/exercise-log-ids   [el-id]
   :airtable/ported                     true})

(def exercise-sessions
  (-> final-exercise-logs
    (->> (map exercise-log->session))))

;; ### All valid xforms?
(->> exercise-sessions
     (mapv #(s/explain-data exercise-session-spec %))
     (filter some?)
     empty?)

;; ## Put the data into an edn file
(comment
  (->> {:exercise-session exercise-sessions
        :exercise         exercises
        :exercise-log     final-exercise-logs}
       (#(with-out-str (pprint %)))
       (spit "data/2022_12_11__15_16_exercises_logs_sessions_xformed.edn")))
