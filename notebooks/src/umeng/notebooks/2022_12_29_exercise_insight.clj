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
            [clojure.pprint :refer [pprint]]
            [tick.alpha.interval :as t.i]
            [scicloj.kindly.v3.kind :as kind]))

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

;; ## Index by id
(def exercises-by-id
  (-> data
      :exercise
      (->> (group-by :xt/id))
      (->> (pot/map-vals first))))

(def logs-by-id
  (-> data
      :exercise-log
      (->> (group-by :xt/id))
      (->> (pot/map-vals first))))

(def sessions-by-id
  (-> data
      :exercise-session
      (->> (group-by :xt/id))
      (->> (pot/map-vals first))))

;; ## Group Sessions
;; ### By day
(def sessions-by-day
  (-> data
      :exercise-session
      (->> (group-by
            (fn [{beg :exercise-session.interval/beginning}]
              (-> beg t/date))))
      (->> (into (sorted-map)))))

;; ### By week
;; What week is it?
(defn days-until-next-week [date]
  (->> date
      t/day-of-week
      t/int
      (- 8)))

(defn week-number-of-month [date]
  (let [year-month (t/year-month date)
        month      (t/int (t/month date))
        day        (t/day-of-month date)]
    (loop [d (t/date (str year-month "-" "01"))
           w 1]
    (let [start-of-next-week (t/>> d (t/new-period (days-until-next-week d) :days))]
      (if (and (= (t/int (t/month start-of-next-week)) month)
               (-> start-of-next-week (t/day-of-month) (< day)))
        (recur start-of-next-week (inc w))
        w)))))

(def sessions-by-week-of-month
 (-> data
      :exercise-session
      (->> (group-by
            (fn [{beg :exercise-session.interval/beginning}]
              (let [year-month (t/year-month beg)
                    w (-> beg t/date week-number-of-month)]
                (str year-month "-w" (format "%02d" w))))))
      (->> (into (sorted-map)))))

(defn week-number-of-year [date]
  (let [year (t/year date)]
    (loop [d (t/date (str year "-01-01"))
           w 1]
      (let [start-of-next-week (t/>> d (t/new-period (days-until-next-week d) :days))]
      (if (and (= (t/year start-of-next-week) year)
               (-> start-of-next-week
                   (t.i/relation date)
                   ((fn [r] (some #{:precedes :meets :equals} [r])))))
        (recur start-of-next-week (inc w))
        w)))))

(def sessions-by-week-of-year
 (-> data
      :exercise-session
      (->> (group-by
            (fn [{beg :exercise-session.interval/beginning}]
              (let [year (t/year beg)
                    w (-> beg t/date week-number-of-year)]
                (str year "-w" (format "%02d" w))))))
      (->> (into (sorted-map)))))

;; ## Enriching sessions
;; ### Add logs
(def sessions-with-logs
  (-> sessions-by-week-of-year
      (->> (sp/transform
            [sp/MAP-VALS sp/ALL]
            (fn [{log-ids :exercise-session/exercise-log-ids
                 :as     session}]
              (-> logs-by-id
                  (->> (sp/select [(sp/submap log-ids)
                                   sp/MAP-VALS
                                   (sp/submap [:xt/id
                                               :exercise-log.interval/beginning
                                               :exercise-log.interval/end
                                               :exercise-log/sets])]))
                  (->> (hash-map :exercise-session/exercise-logs))
                  (merge session)))))
      #_#_#_#_
      vals
      rand-nth
      rand-nth
      :exercise-session/exercise-logs
      ))

;; ### Weight conversion to lb
;; lb is always correct and should never be lbs
(def weight-conversions
  {:lb {:lb 1
        :lbs 1
        :kgs 0.454
        :kg 0.454}
   :kg {:lb 2.205
        :lbs 2.2025
        :kg 1
        :kgs 1}})

(defn ->lb [unit amount]
  (-> amount (* (-> weight-conversions unit :lb))))

(def sessions-with-weight-conversions
  (-> sessions-with-logs
      (->> (sp/transform
            [sp/MAP-VALS sp/ALL :exercise-log/sets sp/ALL :exercise-log.set/weight]
            (fn [{amount :exercise-log.set.weight/amount
                 unit   :exercise-log.set.weight/unit
                 :as weight}]
              (let [converted-amount (->lb unit amount)]
                (merge weight {:exercise-log.set.weight/converted-amount-lb converted-amount})))))))

;; ### Totalling reps, weight, and time under tension
(def derived-totals-per-session
  (-> sessions-with-weight-conversions
      (->> (sp/transform
            [sp/MAP-VALS sp/ALL]
            (fn [{logs :exercise-session/exercise-logs
                 :as  session}]
              (let [sets         (-> logs (->> (map :exercise-log/sets)) flatten (->> (remove nil?)))
                    total-reps   (-> sets
                                     (->> (map :exercise-log.set/reps))
                                     (->> (remove nil?))
                                     (->> (reduce +)))
                    total-weight (-> sets
                                     (->> (map :exercise-log.set/weight))
                                     (->> (remove nil?))
                                     (->> (map :exercise-log.set.weight/amount))
                                     (->> (reduce +)))
                    duration     (-> logs
                                     (->> (map
                                           (fn [{beg :exercise-log.interval/beginning
                                                end :exercise-log.interval/end}]
                                             (-> beg (t/between end) t/seconds))))
                                     (->> (reduce +)))
                    ]
                (merge session
                       {:exercise-session/derived-totals
                        {:exercise-session.derived-totals/reps                  total-reps
                         :exercise-session.derived-totals/weight-lb             total-weight
                         :exercise-session.derived-totals/seconds-under-tension duration}})))))))

;; ## Visualizing
;; ### Failed group bar chart
;; This bar chart works but it is so wide it is hard to read and breaks the table of contents
(comment
  (def vega-vals
  (-> derived-totals-per-session
      (->> (sp/transform
            [sp/MAP-VALS]
            (fn [sessions]
              (let [totals  (-> sessions (->> (map :exercise-session/derived-totals)))
                    reps    (-> totals
                                (->> (map :exercise-session.derived-totals/reps))
                                (->> (reduce +)))
                    weight  (-> totals
                                (->> (map :exercise-session.derived-totals/weight-lb))
                                (->> (reduce +)))
                    tension (-> totals
                                (->> (map :exercise-session.derived-totals/seconds-under-tension))
                                (->> (reduce +)))]
                {:total-reps                  reps
                 :total-weight-lb             weight
                 :total-minutes-under-tension (-> tension
                                                  (/ 60)
                                                  float
                                                  (->> (format "%.2f"))
                                                  Float/parseFloat)}))))
      (->> (map (fn [[week-num {:keys [total-reps
                                      total-weight-lb
                                      total-minutes-under-tension]}]]
                  [{:week  week-num
                      :group "reps"
                      :value total-reps}
                     {:week  week-num
                      :group "weight-lbs"
                      :value total-weight-lb}
                     {:week  week-num
                      :group "minutes-under-tension"
                      :value total-minutes-under-tension}])))
      flatten))

  (kind/vega {:data     {:values vega-vals}
            :mark     "bar"
            :encoding {:x       {:field "week"}
                       :y       {:field "value" :type "quantitative"}
                       :xOffset {:field "group"}
                       :color   {:field "group"}}}))

;; ### Reps per week line chart
(def vega-vals
  (-> derived-totals-per-session
      (->> (sp/transform
            [sp/MAP-VALS]
            (fn [sessions]
              (let [totals  (-> sessions (->> (map :exercise-session/derived-totals)))
                    reps    (-> totals
                                (->> (map :exercise-session.derived-totals/reps))
                                (->> (reduce +)))
                    weight  (-> totals
                                (->> (map :exercise-session.derived-totals/weight-lb))
                                (->> (reduce +)))
                    tension (-> totals
                                (->> (map :exercise-session.derived-totals/seconds-under-tension))
                                (->> (reduce +)))]
                {:total-reps                  reps
                 :total-weight-lb             weight
                 :total-minutes-under-tension (-> tension
                                                  (/ 60)
                                                  float
                                                  (->> (format "%.2f"))
                                                  Float/parseFloat)}))))
      (->> (map (fn [[week-num {:keys [total-reps
                                      total-weight-lb
                                      total-minutes-under-tension]}]]
                  {:week  week-num
                   :group "reps"
                   :value total-reps})))))

(kind/vega {:data     {:values vega-vals}
            :width    600
            :mark     "line"
            :encoding {:x {:field "week" :axis {:values
                                                (-> vega-vals
                                                    (->> (map :week))
                                                    (->> (partition 5))
                                                    (->> (map first))
                                                    flatten)}}
                       :y {:field "value" :type "quantitative"}}})

;; TODO replace x tick labels with dates
;; TODO Add a background per season
