(ns app.screens.reports
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]
   ["react-native-chart-kit" :as charts]
   [applied-science.js-interop :as j]
   [reagent.core :as r]
   [app.tailwind :refer [tw]]))


(def tmp-contribution-data
  [{ :date "2017-01-02" :count 1 }
   { :date "2017-01-03" :count 2 }
   { :date "2017-01-04" :count 3 }
   { :date "2017-01-05" :count 4 }
   { :date "2017-01-06" :count 5 }
   { :date "2017-01-30" :count 2 }
   { :date "2017-01-31" :count 3 }
   { :date "2017-03-01" :count 2 }
   { :date "2017-04-02" :count 4 }
   { :date "2017-03-05" :count 2 }
   { :date "2017-02-30" :count 4 }])

(def tmp-stacked-data
  {:labels    ["mon" "tue" "wed" "thu" "fri" "sat" "sun"]
   :legend    ["p & t totals" "plan tracked" "alignment"]
   :data      [
               [30 40 90]
               [20 40 90]
               [10 40 90]
               [30 90 20]
               [50 20 10]
               [50 50 90]
               [30 40 90]
               ]
   :barColors ["#ff0000" "#00ff00" "#0000ff"]})

(defn color-opacity-based
  ([] (color-opacity-based 1))
  ([opacity]
   (str "rgba(26, 255, 146, " opacity ")")))

(def chart-config
  {:backgroundGradientFrom        "#1E2923"
   :backgroundGradientFromOpacity 0,
   :backgroundGradientTo          "#08130D"
   :backgroundGradientToOpacity   0.5
   :color                         color-opacity-based
   :strokeWidth                   2
   :barPercentage                 0.5
   :useShadowColorFromDataset     false})

(defn screen [props]
  (r/as-element
    ;; TODO justin 2021-05-01 add safe area view, heading, and pattern
    [:> paper/Surface {:style (tw "flex flex-1 justify-start")}
     [:> rn/View

      [:> charts/ContributionGraph {:values      (j/lit tmp-contribution-data)
                                    :endDate     (js/Date. "2017-04-01")
                                    :numDays     105
                                    :width       400
                                    :height      220
                                    :chartConfig (j/lit chart-config)}]

      ;; TODO justin 2021-05-01 Add pattern component

      [:> charts/StackedBarChart {:data        (j/lit tmp-stacked-data)
                                  :width       400
                                  :height      220
                                  :chartConfig (j/lit chart-config)}]]]))
