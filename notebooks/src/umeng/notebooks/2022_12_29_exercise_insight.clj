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
            [tick.alpha.interval :as t.i]))

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
