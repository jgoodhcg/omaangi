(ns app.subscriptions
  (:require
   ["color" :as color]
   ["faker" :as faker] ;; TODO remove when tracing is implemented
   ["react-native-paper" :as paper]
   [applied-science.js-interop :as j]
   [re-frame.core :refer [reg-sub subscribe]]
   [com.rpl.specter :as sp :refer [select
                                   select-one
                                   select-one!]]
   [tick.alpha.api :as t]))

(defn version [db _]
  (->> db
       (select-one! [:version])))
(reg-sub :version version)

(defn theme-js [db _]
  ;; TODO inject paper from sub? to make testing easier?
  (let [theme-type (->> db
                        (select-one! [:settings :theme]))]
    (case theme-type
      :light paper/DefaultTheme
      :dark  paper/DarkTheme
      paper/DarkTheme)))
(reg-sub :theme-js theme-js)

(defn selected-day [db _]
  (->> db (select-one! [:view :view/selected-day])))
(reg-sub :selected-day selected-day)

(defn calendar [db _]
  (->> db (select-one! [:calendar])))
(reg-sub :calendar calendar)

(defn sessions [db _]
  (->> db (select-one! [:sessions])))
(reg-sub :sessions sessions)

(defn sessions-for-this-day [[selected-day calendar sessions] _]
  (let [this-day (get calendar selected-day)]
    (->> this-day
         :calendar/sessions
         (map #(get sessions %))
         vec)))
(reg-sub :sessions-for-this-day

         :<- [:selected-day]
         :<- [:calendar]
         :<- [:sessions]

         sessions-for-this-day)

(defn this-day [selected-day _]
  (let [month (t/month selected-day)
        year  (t/year selected-day)
        now   (t/now)]
    {:day-of-week   (->> selected-day
                         t/day-of-week
                         str)
     :day-of-month  (t/day-of-month selected-day)
     :year          (str year)
     :month         (->> month str)
     :display-year  (not= year (t/year now))
     :display-month (not= month (t/month now))}))
(reg-sub :this-day

         :<- [:selected-day]

         this-day)

(defn tracking [db _]
  ;; TODO implement once tick event is in place
  (for [x (-> 8 rand-int (max 1) range)]
    (let [c                 (-> faker (j/get :internet) (j/call :color) color)
          more-than-doubled (-> (rand) (> 0.30))]
      {:session/color-string     (-> c (j/call :hex))
       :session/more-than-double more-than-doubled
       :indicator/color-string   (-> c (j/call :lighten 0.32) (j/call :hex))
       :ripple/color-string      (-> c (j/call :lighten 0.64) (j/call :hex))
       :session/relative-width   (if more-than-doubled
                                   "100%"
                                   (-> (rand) (* 100) (str "%"))) })))
(reg-sub :tracking tracking)
