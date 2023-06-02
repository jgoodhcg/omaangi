;; # Looking at time logs
(ns umeng.notebooks.2023-03-19-time-logs
  {:nextjournal.clerk/toc true
   :nextjournal.clerk/error-on-missing-vars :off}
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
            [tick.alpha.interval :as t.i]
            [com.rpl.specter :as sp]
            [kixi.stats.core :refer [mean]]
            [clojure.pprint :refer [pprint]]
            [umeng.shared.misc :refer [timestamp-for-filename]]
            [clojure.set :as set]
            [nextjournal.clerk :as clerk]
            [taoensso.tufte :as tufte :refer [defnp p profile]]))

;; ## Load data
{::clerk/visibility {:code :fold :result :show}}
(def raw-data
  (->> "data/2023-03-19T17_26_16.492--0.0.29.edn"
       slurp
       (edn/read-string
        {:readers {'time/date    t/date
                  ;; 'uuid         uuid
                   'time/instant t/instant
                   'time/time    t/time
                   'time/zone    t/zone}})))

;; ## Time span of data
{::clerk/visibility {:code :fold :result :hide}}
(def sorted-sessions
  (->> raw-data
       (sp/select [:app-db/sessions sp/MAP-VALS])
       (sort-by :session/start)))
(def earliest-session (first sorted-sessions))
(def latest-session (last sorted-sessions))
(def earliest-date (-> earliest-session :session/start (t/in "US/Eastern")))
(def latest-date (-> latest-session :session/start (t/in "US/Eastern")))
{::clerk/visibility {:code :fold :result :show}}
(clerk/html
 [:div
  [:p "Earliest date: " (t/format (t/formatter "yyyy-MMM-dd") earliest-date)]
  [:p "Latest date: " (t/format (t/formatter "yyyy-MMM-dd") latest-date)]])

;; ## Days with session count
{::clerk/visibility {:code :fold :result :hide}}
(def time-span-with-count
  (->> (t/range earliest-date latest-date (t/new-period 1 :days))
       (map (fn [zdt]
              (-> zdt
                  t/date
                  ((fn [d]
                     (let [{sessions :calendar/sessions}
                           (get-in raw-data [:app-db/calendar d])]
                       {:date        (t/format (t/formatter "yyyy-MMM-dd") d)
                        :day-of-week (str (t/day-of-week d))
                        :year        (str (t/year d))
                        :count       (count sessions)}))))))))
(def time-span-count-heatmap
  {:data  {:values time-span-with-count}
   :facet {:row {:field "year"}}
   :spec  {:mark   "rect"
           :config {:view {:stroke-width 0
                           :step         15}}
           :width  650
           :encoding
           {:x     {:field    "date"
                    :type     "temporal"
                    :timeUnit "week"
                    :axis     {:labelExpr  "monthAbbrevFormat(month(datum.value))"
                               :labelAlign "middle"
                               :title      nil}}
            :y     {:field "day-of-week"
                    :type  "ordinal"
                    :sort  ["MONDAY" "TUESDAY" "WEDNESDAY" "THURSDAY" "FRIDAY" "SATURDAY"]
                    :axis  {:title nil}}
            :color {:field     "count"
                    :aggregate "max"
                    :type      "quantitative"
                    :legend    {:title nil}}}}})
{::clerk/visibility {:code :fold :result :show}}
(clerk/vl time-span-count-heatmap)

;; ## Time logged per tag
{::clerk/visibility {:code :fold :result :hide}}
(defn group-by-tags [maps]
  (reduce
   (fn [acc m]
     (reduce
      (fn [acc tag]
        (assoc acc tag (conj (get acc tag []) m)))
      acc (:session/tags m)))
   {}
   maps))
(def tag-cumulative-time-data
  (-> raw-data
      (->> (sp/select [:app-db/sessions sp/MAP-VALS]))
      (->> (map (fn [{start :session/start stop :session/stop
                      :as   session}]
                  (merge session
                         {:session/duration (t/seconds (t/between start stop))}))))
      group-by-tags
      (->> (map (fn [[tag sessions]]
                  {:duration (-> sessions
                                 (->> (map :session/duration))
                                 (->> (apply +))
                                 (/ 3600))
                   :label    (get-in raw-data [:app-db/tags tag :tag/label])
                   :color    (or (get-in raw-data [:app-db/tags tag :tag/color])
                                 "#e2e2e2")})))
      (->> (sort-by :duration))
      reverse))
{::clerk/visibility {:code :hide :result :show}}
(clerk/vl
 {:data  {:values tag-cumulative-time-data}
  :mark  "bar"
  :width 700
  :encoding
  {:x     {:field "duration"
           :type  "quantitative"
           :axis  {:title "Hours"}}
   :y     {:field "label"
           :type  "nominal"
           :axis  {:title nil}
           :sort  {:field "duration" :op "sum" :order "descending"}
           :scale {:rangeStep 20}}
   :color {:field     "color"
           :condition {:test  "datum.color !== null"
                       :value {:expr "datum.color"}}
           :legend    nil}}})

;; ## Day pattern for a tag
{::clerk/visibility {:code :hide :result :hide}}
(def sessions-by-tag
  (-> raw-data
      (->> (sp/select [:app-db/sessions sp/MAP-VALS]))
      (->> (map (fn [{start :session/start stop :session/stop
                      :as   session}]
                  (merge session
                         {:session/duration (t/seconds (t/between start stop))}))))
      group-by-tags))

{::clerk/visibility {:code :hide :result :show}}
(def rand-tag (get-in raw-data [:app-db/tags (rand-nth (keys sessions-by-tag))]))

{::clerk/visibility {:code :hide :result :hide}}
(def rand-tag-sessions (get sessions-by-tag (:tag/id rand-tag)))

(defn increment-counter [counter hour]
  (update counter hour (fnil inc 0)))

(defn session-hours [session]
  (let [{:keys [session/start session/stop]} session]
    (->> (t/range start stop (t/new-duration 1 :hours))
         (map t/hour)
         set
         seq)))

(defn session-overlap-counter [sessions]
  (reduce (fn [counter session]
            (let [hours (session-hours session)]
              (reduce increment-counter counter hours)))
          (zipmap (range 0 24) (repeat 0))
          sessions))

{::clerk/visibility {:code :hide :result :show}}
(clerk/vl
 {:data     {:values (mapcat (fn [[hour count]]
                               (repeat count {:hour hour :count 1}))
                             (session-overlap-counter rand-tag-sessions))}
  :mark     "bar"
  :encoding {:x {:field "hour"
                 :type  "ordinal"
                 :axis  {:title "Hour of the Day"}}
             :y {:aggregate "count"
                 :field     "count"
                 :type      "quantitative"
                 :axis      {:title "Number of Overlaps"}}}})

{::clerk/visibility {:code :hide :result :hide}}
(defn session-overlap-heatmap [sessions]
  {:data     {:values (mapcat (fn [[hour count]] (repeat count {:hour hour}))
                              (session-overlap-counter sessions))}
   :mark     "rect"
   :width    700
   :encoding {:x     {:field "hour"
                      :type  "ordinal"
                      :axis  {:title "Hour of the Day"}}
              :y     {:aggregate "count"
                      :field     "hour"
                      :type      "quantitative"
                      :axis      {:title "Number of Overlaps"}}
              :color {:field     "hour"
                      :aggregate "count"
                      :type      "quantitative"
                      :legend    {:title "Number of Overlaps"}}}})

{::clerk/visibility {:code :hide :result :show}}
(clerk/vl (session-overlap-heatmap rand-tag-sessions))

;; ## Day pattern for all tags
{::clerk/visibility {:code :hide :result :hide}}
;; Data is essentially a big list of `{:hour x :tag/id #uuid "y" :tag/label "z"}`
(def day-pattern-data
  (-> sessions-by-tag
      (->> (map (fn [[tag-id sessions]]
                  (let [tag-label (get-in raw-data [:app-db/tags tag-id :tag/label])]
                (->> (session-overlap-counter sessions)
                     (mapcat (fn [[hour count]]
                               (repeat count {:hour   hour
                                              :tag-id tag-id
                                              :label  tag-label}))))))))
       flatten))

{::clerk/visibility {:code :hide :result :show}}
(clerk/vl
 {:data     {:values day-pattern-data}
  :mark     "rect"
  :encoding {:x     {:field "hour"
                     :type  "ordinal"
                     :axis  {:title "Hour of the Day"}}
             :y     {:field "label"
                     :type  "nominal"
                     :axis  {:title "Tag"}}
             :color {:field     "hour"
                     :aggregate "count"
                     :type      "quantitative"
                     :legend    {:title "Number of Overlaps"}}}})
