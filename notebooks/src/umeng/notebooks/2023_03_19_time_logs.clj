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
(comment
  (tufte/add-basic-println-handler! {})
(defnp make-session-ish-interval
  [x]
  (let [start (or (:session/start x) (:session-template/start x))
        stop  (or (:session/stop x) (:session-template/stop x))]
    (merge x {:tick/beginning start
              :tick/end       stop})))
(defnp touches
  [a b]
  (if (and (some? a)
           (some? b))
    (not (some? (some #{:precedes :preceded-by} ;; checks that these are not the relation
                      [(t.i/relation (make-session-ish-interval a)
                                     (make-session-ish-interval b))])))
    false))
(defnp session-ish-overlaps-collision-group?
  [session-ish c-group]
  (some? (->> c-group
              (some #(touches session-ish %)))))
(defnp insert-into-collision-group
  [collision-groups session-ish]
  (let [collision-groups-with-trailing-empty
        (if (empty? (last collision-groups))
          collision-groups
          (conj collision-groups []))]

    (sp/setval
      (sp/cond-path
        ;;put the session in the first group that collides
        [(sp/subselect sp/ALL (partial session-ish-overlaps-collision-group? session-ish)) sp/FIRST]
        [(sp/subselect sp/ALL (partial session-ish-overlaps-collision-group? session-ish)) sp/FIRST sp/AFTER-ELEM]

        ;; otherwise put it in the first empty
        [(sp/subselect sp/ALL empty?) sp/FIRST]
        [(sp/subselect sp/ALL empty?) sp/FIRST sp/AFTER-ELEM])

      session-ish
      collision-groups-with-trailing-empty)))
(defnp get-collision-groups
  [session-ishes]
  (->> session-ishes
       (reduce insert-into-collision-group [[]])
       (remove empty?)
       vec))
(defnp smoosh-sessions
  [sessions]
  (->> sessions
       get-collision-groups
       (mapv (fn [cg]
               (loop [time-stamps    (->> cg
                                          (map #(list (:session/start %) (:session/stop %)))
                                          flatten
                                          distinct
                                          sort
                                          rest
                                          vec)
                      acc            []
                      cur-time-stamp (->> cg (sort-by :session/start) first :session/start)]
                 (if (empty? time-stamps)
                   acc
                   (recur (->> time-stamps rest vec)
                          (conj acc {:session/start cur-time-stamp :session/stop (first time-stamps)})
                          (->> time-stamps first))))))
       flatten
       (mapv (fn [smooshed-session]
               (merge smooshed-session
                      {:session/tags (->> sessions
                                          (filter #(touches % smooshed-session))
                                          (mapv :session/tags)
                                          (apply concat)
                                          distinct
                                          vec)}))))))
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
                                 "#e2e2e2")}
                  )))
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
