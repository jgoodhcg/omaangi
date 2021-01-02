(ns app.subscriptions
  (:require
   ["react-native-paper" :as paper]
   [re-frame.core :refer [reg-sub subscribe]]
   [com.rpl.specter :as sp :refer [select
                                   select-one
                                   select-one!]]
   [tick.alpha.api :as t]))

(defn version [db _]
  (->> db
       (select-one! [:version])))

(defn theme-js [db _]
  (let [theme-type (->> db
                        (select-one! [:settings :theme]))]
    (case theme-type
      :light paper/DefaultTheme
      :dark  paper/DarkTheme
      paper/DarkTheme)))

(defn selected-day [db _]
  (->> db (select-one! [:view :view/selected-day])))

(defn calendar [db _]
  (->> db (select-one! [:calendar])))

(defn sessions [db _]
  (->> db (select-one! [:sessions])))

(reg-sub :version version)
(reg-sub :theme-js theme-js)

(reg-sub :selected-day selected-day)
(reg-sub :calendar calendar)
(reg-sub :sessions sessions)

(reg-sub
  :sessions-for-this-day

  :<- [:selected-day]
  :<- [:calendar]
  :<- [:sessions]

  (fn [[selected-day calendar sessions] _]
    (let [this-day (get calendar selected-day)]
      (->> this-day
           :calendar/sessions
           (map #(get sessions %))
           vec))))

(defn abbreviate [x] (->> x str (take 3) (clojure.string/join "")))

(reg-sub
  :this-day

  :<- [:selected-day]

  ;; If there is only one signal sub then the inputs are not in a vector :O
  (fn [selected-day _]
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
       :display-month (not= month (t/month now))})))
