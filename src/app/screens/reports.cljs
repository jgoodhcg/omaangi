(ns app.screens.reports
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]
   ["react-native-chart-kit" :as charts]

   [applied-science.js-interop :as j]
   [reagent.core :as r]

   [app.helpers :refer [<sub >evt get-theme]]
   [app.tailwind :refer [tw]]
   [app.db :refer [generate-color]]))


(def tmp-pie-chart-data
  [{:name            "Tag 1"
    :min             1000
    :color           (-> (generate-color) (j/call :hex))
    :legendFontColor "#7f7f7f"
    :legendFontSize  15}
   {:name            "Tag 2"
    :min             10300
    :color           (-> (generate-color) (j/call :hex))
    :legendFontColor "#7f7f7f"
    :legendFontSize  15}
   {:name            "Tag 3"
    :min             13000
    :color           (-> (generate-color) (j/call :hex))
    :legendFontColor "#7f7f7f"
    :legendFontSize  15}
   {:name            "Tag 4"
    :min             5000
    :color           (-> (generate-color) (j/call :hex))
    :legendFontColor "#7f7f7f"
    :legendFontSize  15}
   {:name            "Tag 5"
    :min             11100
    :color           (-> (generate-color) (j/call :hex))
    :legendFontColor "#7f7f7f"
    :legendFontSize  15}])

(def tmp-stacked-data
  {:labels    ["mon" "tue" "wed" "thu" "fri" "sat" "sun"]
   :legend    ["time logged" "plan executed" "alignment"]
   :data      [
               [30 40 90]
               [20 40 90]
               [10 40 90]
               [30 90 20]
               [50 20 10]
               [50 50 90]
               [30 40 90]
               ]
   :barColors ["#8d8d8d" "#bdbdbd" "#ab47bc"]})

(def tmp-pattern-colors (->> 6 range (map #(-> (generate-color) (j/call :hex)))))
(def tmp-pattern-day (->> 23
                          range
                          (map #(nth tmp-pattern-colors (mod % 6)))
                          sort
                          (split-at (rand-int 23))
                          ((fn [[before after]]
                             (vec (concat before [(j/call (generate-color) :hex)] after))))))
(def tmp-pattern-days ["mon" "tue" "wed" "thu" "fri" "sat" "sun"])
(def tmp-pattern-data
  (->> 7
       range
       (map (fn [i]
              {:hours (->> tmp-pattern-day cycle (drop (rand-int 7)) (take 24))
               :day   (nth tmp-pattern-days i)}))))

(defn color-opacity-based
  ([] (color-opacity-based 1))
  ([opacity]
   (str "rgba(255, 255, 255, " opacity ")")))

(def chart-config
{:backgroundGradientFrom        "#1E2923"
 :backgroundGradientFromOpacity 0,
 :backgroundGradientTo          "#08130D"
 :backgroundGradientToOpacity   0.5
 :color                         color-opacity-based
 :strokeWidth                   2
 :barPercentage                 0.5
 :useShadowColorFromDataset     false})

(defn pattern-graph []
  [:> rn/View (tw "p-2")
   [:> rn/View (tw "flex flex-col")
    (for [{:keys [day hours]} tmp-pattern-data]
      [:> rn/View {:style (tw "flex flex-row justify-between pb-1")
                   :key   (str (random-uuid))}
       [:> rn/View (tw "w-9")
        [:> paper/Text  day]]
       (for [c hours]
         [:> rn/View {:style (merge (tw "w-3 h-6")
                                    {:background-color c})
                      :key   (str (random-uuid))}])])]])

(defn pie-chart []
  [:> rn/View (tw "p-2")
   [:> charts/PieChart
    {:data            (j/lit tmp-pie-chart-data)
     :width           400
     :height          250
     :chartConfig     (j/lit chart-config)
     :accessor        "min"
     :backgroundColor "transparent"
     :paddingLeft     "15"}]])

(defn stacked-bar-chart []
  [:> rn/View (tw "p-2")
   [:> charts/StackedBarChart {:data        (j/lit tmp-stacked-data)
                               :width       400
                               :height      220
                               :chartConfig (j/lit chart-config)}]])

(defn interval-buttons []
  [:> rn/View (tw "flex flex-row justify-around py-4")
   [:> paper/Button
    {:mode     "flat"
     :on-press #(tap> "hello")}
    "2021-06-20"]
   [:> paper/Button
    {:mode     "flat"
     :on-press #(tap> "hello")}
    "2021-06-27"]])

(defn screen [props]
  (r/as-element
    [(fn []
       (let [theme (->> [:theme] <sub get-theme)]
         ;; TODO justin 2021-05-01 add safe area view, heading, and pattern
         ;;
         [:> rn/SafeAreaView {:style (merge (tw "flex flex-1")
                                            {:background-color (-> theme (j/get :colors) (j/get :background))})}
          [:> paper/Surface {:style (-> (tw "flex flex-1")
                                        (merge {:background-color (-> theme (j/get :colors) (j/get :background))}))}
           [:> rn/ScrollView
            [:> rn/View
             [:> rn/StatusBar {:visibility "hidden"}]

             [interval-buttons]

             [pie-chart]

             [pattern-graph]

             [stacked-bar-chart]]]]]))]))
