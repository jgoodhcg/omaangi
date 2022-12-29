;; # Life Cycles
(ns umeng.notebooks.2022-12-27-life-cycles
  (:import [org.shredzone.commons.suncalc MoonPhase SunTimes MoonPosition MoonIllumination])
  (:require [scicloj.kindly.v3.kind :as kind]
            [tick.core :as t]
            [tick.alpha.interval :as t.i]
            [potpuri.core :as pot]))

;; ## Examples of libary interop and usage

(-> SunTimes
    (. compute)
    (. on 2020 5 1)
    (. at 40.712778 -74.005833)
    (. execute))

(-> MoonPhase
    (. compute)
    (. phase 270.0)
    (. at 40.712778 -74.005833)
    (. execute))

(-> MoonPhase
    (. compute)
    (. at 40.712778 -74.005833)
    (. execute))

(-> MoonPosition
    (. compute)
    (. on 2023 1 24)
    (. at 40.712778 -74.005833)
    (. execute))

;; ### Moon phase given date and location

(-> MoonIllumination
    (. compute)
    (. on 2023 1 24)
    (. at 40.712778 -74.005833)
    (. execute)
    (. getClosestPhase)
    (. toString))

;; ## Looking at a lifespan

;; ### Solar Years weeks days
(def years 90)
(def days-per-year 365.2422)
(def weeks-per-year 52)
(def days-per-week 7)

(defn render-week [_]
  [:div {:style {:width            8
                 :height           8
                 :background-color "#63B995"
                 :border-radius    2
                 :margin-top       2
                 :margin-left      2}}])

(defn render-year [_]
  (into
   [:div {:style {:display        "flex"
                  :flex-direction "row"}}]
   (for [w (-> weeks-per-year
               (+ 1)
               (->> (range 1)))]
     (render-week w))))

(def life-chart-hiccup
  (into
   [:div {:style {:display        "flex"
                  :flex-direction "column"}}]
   (for [y (-> years
               (+ 1)
               (->> (range 1)))]
     (render-year y))))

;; ### Every week of my life

(kind/hiccup life-chart-hiccup)

;; ### Lunar periods and lunar years

;; Synodic moon period in days
(def synodic-moon-period 29.530588853)

(def moon-periods-in-life-span
  (-> years (* days-per-year) (/ synodic-moon-period) int))

;; A lunar year is 12 synodic months
(def lunar-months-per-lunar-year 12)
(def lunar-years-in-life-span
 (-> moon-periods-in-life-span
    (/ lunar-months-per-lunar-year)
    int))

(def lunar-life-chart-hiccup
  (into
   [:div {:style {:display        "flex"
                  :flex-direction "column"}}]
   (for [_ (-> lunar-years-in-life-span range)]
     (into
      [:div {:style {:display        "flex"
                     :flex-direction "row"}}]
      (for [_ (-> lunar-months-per-lunar-year range)]
        [:div {:style {:width            8
                       :height           8
                       :background-color "#4464AD"
                       :border-radius    2
                       :margin-top       2
                       :margin-left      2}}])))))

;; ### Every moon period of my life

(kind/hiccup lunar-life-chart-hiccup)

;; ### Solar vs Lunar life charts
;; Each row is a respective year
;; Each box in the solar calendar is a week
;; Each box in the lunar calendar is a lunar month

(kind/hiccup
 [:div {:style {:display        "flex"
                :flex-direction "row"
                :justify-content "space-around"}}
  life-chart-hiccup
  lunar-life-chart-hiccup])

;; ### Beef up the lunar chart
;; Can we make these feel similarly sized? Could each box in the lunar calendar be a phase?

(def phases-per-period 4)

(def lunar-life-chart-beefy-hiccup
  (into
   [:div {:style {:display        "flex"
                  :flex-direction "column"}}]
   (for [_ (-> lunar-years-in-life-span range)]
     (into
      [:div {:style {:display        "flex"
                     :flex-direction "row"}}]
      (for [_ (-> lunar-months-per-lunar-year range)]
        (into
         [:div {:style {:display "flex"
                        :flex-direction "row"}}]
         (for [_ (-> phases-per-period range)]
           [:div {:style {:width            8
                          :height           8
                          :background-color "#4464AD"
                          :border-radius    2
                          :margin-top       2
                          :margin-left      2}}])))))))

;; ### Lunar life chart by phases

(kind/hiccup lunar-life-chart-beefy-hiccup)

;; ### Solar vs Lunar (phase) life charts
;; Each row is a respective year
;; Each box in the solar calendar is a week
;; Each box in the lunar calendar is a lunar phase

(kind/hiccup
 [:div {:style {:display        "flex"
                :flex-direction "row"
                :justify-content "space-around"}}
  life-chart-hiccup
  lunar-life-chart-beefy-hiccup])

;; ## Getting more specific
(def birthday (t/instant "1991-09-16T00:00:00.000Z"))


;; ### Moon phase given an instant
(defn moon-phase [instant]
  (-> MoonIllumination
    (. compute)
    (. on instant)
    (. execute)
    (. getClosestPhase)
    (. toString)))

;; Moon phase on my birthday
(moon-phase birthday)

(defn bg-color [relation has-full-moon]
  (cond
    has-full-moon                  "#ACD7EC"
    ;; in the past
    (or (= relation :precedes )
        (= relation :meets))       "#847996"
    ;; in the future
    (or (= relation :met-by)
        (= relation :preceded-by)) "#939F5C"
    ;; happening
    :else "#153B50"))

(def life-chart-enhanced
  (into [:div {:display "flex" :flex-direction "column"}]
        (-> years
            (* days-per-year)
            (range)
            (->> (map
                  (fn [d]
                    (let [date (t/>> birthday (t/new-duration d :days))]
                      {:date       date
                       :moon-phase (moon-phase date)}))))
            (->> (partition days-per-week))
            (->> (map (fn [week]
                        (let [interval        (t.i/new-interval (-> week first :date)
                                                                (-> week last :date))
                              relation        (t.i/relation
                                               (pot/map-vals t/instant (t.i/bounds (t/today)))
                                               interval)
                              has-a-full-moon (->> week
                                                   (map :moon-phase)
                                                   set
                                                   (some #{"FULL_MOON"})
                                                   some?)
                              c               (bg-color relation has-a-full-moon)]
                          [:div {:style {:width            8
                                         :height           8
                                         :background-color c
                                         :border-radius    2
                                         :margin-top       2
                                         :margin-left      2}}]))))
            (->> (partition weeks-per-year))
            (->> (map (fn [y] (into [:div {:style {:display "flex" :flex-direction "row"}}] y)))))))

;; ### More ehanced life chart
;; This is a solar based chart. Every box is a week. It shows weeks that "have a full moon". But this isn't exact.
;; It's really showing weeks that have a day where the closest phase is a full moon.
;; This chart also shows how much of my life is in the future/past.
(kind/hiccup life-chart-enhanced)

(defn moon-phase-number [instant]
  (-> MoonIllumination
    (. compute)
    (. on instant)
    (. execute)
    (. getPhase)))

(moon-phase-number (t/now))

(into [:div {:display "flex" :flex-direction "column"}]
        (-> years
            (* days-per-year)
            (range)
            (->> (map
                  (fn [d]
                    (let [date (t/>> birthday (t/new-duration d :days))
                          mp   (moon-phase-number date)]
                      {:date         date
                       :moon-phase   mp
                       :is-full-moon (and (> mp -10)
                                          (< mp 10))}))))
            (->> (filter :is-full-moon))))
