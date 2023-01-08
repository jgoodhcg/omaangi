;; # Exercise Insight
(ns umeng.notebooks.2022-12-29-exercise-insight
  {:nextjournal.clerk/error-on-missing-vars :off
   :nextjournal.clerk/toc true
   :nextjournal.clerk/visibility {:code :fold}}
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
            [scicloj.kindly.v3.kind :as kind]
            [nextjournal.clerk :as clerk]))

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
(defn enrich-session-with-logs
  [{log-ids :exercise-session/exercise-log-ids
    :as     session}]
  (-> logs-by-id
      (->> (sp/select [(sp/submap log-ids)
                       sp/MAP-VALS
                       (sp/submap [:xt/id
                                   :exercise-log.interval/beginning
                                   :exercise-log.interval/end
                                   :exercise-log/sets])]))
      (->> (hash-map :exercise-session/exercise-logs))
      (merge session)))

(def sessions-with-logs
  (-> sessions-by-week-of-year
      (->> (sp/transform
            [sp/MAP-VALS sp/ALL]
            enrich-session-with-logs))
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

(defn convert-weight
  [{amount :exercise-log.set.weight/amount
                 unit   :exercise-log.set.weight/unit
                 :as weight}]
              (let [converted-amount (->lb unit amount)]
                (merge weight {:exercise-log.set.weight/converted-amount-lb converted-amount})))

(def sessions-with-weight-conversions
  (-> sessions-with-logs
      (->> (sp/transform
            [sp/MAP-VALS sp/ALL :exercise-log/sets sp/ALL :exercise-log.set/weight]
            convert-weight))))

;; ### Totalling reps, weight, and time under tension
(defn derive-session-totals
  [{logs :exercise-session/exercise-logs
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
                                     (->> (reduce +)))]
                (merge session
                       {:exercise-session.derived-totals/reps                  total-reps
                        :exercise-session.derived-totals/weight-lb             total-weight
                        :exercise-session.derived-totals/seconds-under-tension duration})))

(def derived-totals-per-session
  (-> sessions-with-weight-conversions
      (->> (sp/transform
            [sp/MAP-VALS sp/ALL]
            ;; TODO replace this with derive-session-totals, there is a falttening that needs to be accounted for
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
                       {:exercise-session/derived-totals ;; TODO flatten this
                        {:exercise-session.derived-totals/reps                  total-reps
                         :exercise-session.derived-totals/weight-lb             total-weight
                         :exercise-session.derived-totals/seconds-under-tension duration}})))))))

;; ## Visualizing (clay)
;; This won't be visible when looking at a notebook rendered with clerk
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

;; ## Visualizing (clerk)
;; This won't be visible when looking at a notebook rendered with clay
;; ### First attempt
;; This shows each week as a row with a variable number of sessions
(clerk/plotly
 {:data [{:z (->> derived-totals-per-session
                  (sp/transform
                   #_[sp/MAP-VALS]
                   #_(fn [sessions]
                       (->> sessions
                            (map #(get-in % [:exercise-session/derived-totals
                                             :exercise-session.derived-totals/reps]))
                            (reduce +)))

                   [sp/MAP-VALS
                    sp/ALL]
                   (fn [session]
                     (-> session (get-in [:exercise-session/derived-totals
                                          :exercise-session.derived-totals/reps])))

                   #_[sp/MAP-VALS]
                   #_(fn [sessions]
                     (->> sessions
                          (group-by
                           (fn [{beg :exercise-session.interval/beginning}]
                             (-> beg t/date)))
                          (sp/select [sp/MAP-VALS])
                          (map (fn [day]
                                 (->> day
                                      (map #(get-in % [:exercise-session/derived-totals
                                                       :exercise-session.derived-totals/reps]))
                                      (reduce +)))))))

                  (sp/select [sp/MAP-VALS]))
          :y (->> derived-totals-per-session
                  keys
                  (map str))
          :type "heatmap"}]})

;; ### Second Attempt
;; I want to get a true calendar where each week is a row and each day is a column.
;; For that I need to fill out a data structure and iterate over it looking up the week/day from the derived-totals-per-session.

;; What's the interval?
(def earliest-day (-> sessions-by-day keys first))
(def latest-day (-> sessions-by-day keys last))

;; Let's see the calendar
(def calendar
  (->> (t/range earliest-day latest-day (t/new-period 1 :days))
       (group-by (fn [d] (str (t/year d) "-w" (format "%02d"(week-number-of-year d)))))
       (into (sorted-map))))

;; Figure out the data
(def z-data ;; reps
  (->> calendar
       (map (fn [[week days]]
              (let [sessions-for-this-week (get derived-totals-per-session week)]
                (->> days
                     (map (fn [day]
                            (->> sessions-for-this-week
                                 (filter (fn [{beg :exercise-session.interval/beginning}]
                                           (t/= (t/date beg) (t/date day))))
                                 (map (fn [session]
                                        (get-in session
                                                [:exercise-session/derived-totals
                                                 :exercise-session.derived-totals/reps])))
                                 (reduce +))))))))))

(def x-data ;; day
  ["MON" "TUE" "WED" "THU" "FRI" "SAT" "SUN"])

(def y-data ;; week
  (->> calendar keys))

(clerk/plotly
 {:data [{:z           z-data
          :x           x-data
          :y           y-data
          :type        "heatmap"
          :colorscale  [[0, "#DBD3D8"
                        1, "#CD4631"]]
          :hoverongaps false}]})

;; ### Third attempt

;; Another calendar
(def calendar-3
  (->> (t/range (t/date "2021-01-01") (t/date "2022-12-31") (t/new-period 1 :days))
       (group-by (fn [d] (str (t/day-of-week d))))))

(def z-data-3 ;;reps
  (->> calendar-3
       (map (fn [[_ days]]
              (->> days
                   (map (fn [d]
                          (let [sessions (get sessions-by-day d)]
                            (->> sessions
                                 (map enrich-session-with-logs)
                                 (sp/transform
                                  [sp/ALL :exercise-log/sets sp/ALL :exercise-log.set/weight]
                                  convert-weight)
                                 (map derive-session-totals)
                                 (map :exercise-session.derived-totals/reps)
                                 (reduce +))))))))))

(def x-data-3 ;; date
  (->> calendar-3
       (map (fn [[_ days]]
              (->> days (map str))))))

(def y-data-3 ;; day of week
  (->> calendar-3 keys))

(clerk/plotly
 {:data [{:z           z-data-3
          :x           x-data-3
          :y           y-data-3
          :type        "heatmap"
          :colorscale  [[0, "#DBD3D8"
                         1, "#CD4631"]]}]})

;; ### Forth attempt (Vega lite heatmap)

(def vl-heatmap-data
  (->> calendar-3
       (map (fn [[_ days]]
              (->> days
                   (map (fn [d]
                          (let [sessions (get sessions-by-day d)]
                            {:date       d
                             :dow-int    (t/int (t/day-of-week d))
                             :dow        (str (t/day-of-week d))
                             :week-month (str (week-number-of-year d) "-" (t/month d))
                             :week       (str (t/year d) "-" (format "%02d" (week-number-of-year d)))
                             :reps       (->> sessions
                                            (map enrich-session-with-logs)
                                            (sp/transform
                                             [sp/ALL :exercise-log/sets sp/ALL :exercise-log.set/weight]
                                             convert-weight)
                                            (map derive-session-totals)
                                            (map :exercise-session.derived-totals/reps)
                                            (reduce +))}))))))
       flatten))

(clerk/vl
 {:data   {:values (->> vl-heatmap-data (filter #(= (t/int (t/year (:date %))) 2021)))}
  :mark   "rect"
  :width  650
  :config {:view {:stroke-width 0
                  :step         15}
           :axis {:domain false}}
  :encoding
  {:x     {:field "week"
           :type  "ordinal"
           :axis  {:domain false :labels false :ticks false :title nil}}
   :y     {:field "dow"
           :type  "ordinal"
           :sort  ["MONDAY" "TUESDAY" "WEDNESDAY" "THURSDAY" "FRIDAY" "SATURDAY"]
           :axis  {:title nil}}
   :color {:field     "reps"
           :aggregate "max"
           :type      "quantitative"
           :legend    {:title nil}}}})

(clerk/vl
 {:data   {:values (->> vl-heatmap-data (filter #(= (t/int (t/year (:date %))) 2022)))}
  :mark   "rect"
  :config {:view {:stroke-width 0
                  :step         15}
           :axis {:domain false}}
  :width  650
  :encoding
  {:x     {:field "week"
           :type  "ordinal"
           :axis  {:domain false :labels false :ticks false :title nil}}
   :y     {:field "dow"
           :type  "ordinal"
           :sort  ["MONDAY" "TUESDAY" "WEDNESDAY" "THURSDAY" "FRIDAY" "SATURDAY"]
           :axis  {:title nil}}
   :color {:field     "reps"
           :aggregate "max"
           :type      "quantitative"
           :legend    {:title nil}}}})
