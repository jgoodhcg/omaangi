(ns app.screens.reports
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]
   ["react-native-chart-kit" :as charts]
   ["react-native-modal-datetime-picker" :default DateTimePicker]

   [applied-science.js-interop :as j]
   [reagent.core :as r]

   [app.components.time-related :as tm]
   [app.screens.core :refer [screens]]
   [app.components.generic-top-section :as top-section]
   [app.helpers :refer [<sub >evt get-theme clear-datetime-picker]]
   [app.tailwind :refer [tw]]
   [app.db :refer [generate-color]]))

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
  [:> rn/View (tw "p-2 my-6")
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
  (let [data (<sub [:pie-chart-data])]
    [:> rn/View (tw "p-2")
     [:> charts/PieChart
      {:data            (j/lit data)
       :width           400
       :height          250
       :chartConfig     (j/lit chart-config)
       :accessor        "min"
       :backgroundColor "transparent"
       :paddingLeft     "15"}]]))

(defn stacked-bar-chart []
  [:> rn/View (tw "p-2 my-6")
   [:> charts/StackedBarChart {:data        (j/lit tmp-stacked-data)
                               :width       400
                               :height      220
                               :chartConfig (j/lit chart-config)}]])

(defn interval-buttons []
  (let [{:keys [beginning-value
                beginning-label
                end-label
                end-value]} (<sub [:report-interval])
        {:date-time-picker/keys [value mode visible field-key]
         dtp-id                 :date-time-picker/id}
        (<sub [:date-time-picker])]

    [:> rn/View (tw "flex py-4 px-2 items-center")
     (when (and (some? value)
                (= dtp-id :report))
       [:> DateTimePicker
        {:is-visible           visible
         :is-dark-mode-enabled true
         :date                 value
         :mode                 mode

         :on-hide    #(do
                        (tap> "hidden")
                        (>evt clear-datetime-picker))
         :on-cancel  #(do
                        (tap> "cancelled")
                        (>evt clear-datetime-picker))
         :on-confirm #(do
                        (tap> (str "Update " field-key
                                   " as " %))
                        (>evt [:set-report-interval
                               {field-key %}])
                        (>evt clear-datetime-picker))}])

     [:> paper/Subheading "Report interval"]
     [:> rn/View (tw "flex flex-row justify-around")
      [tm/date-button {:value     beginning-value
                       :label     beginning-label
                       :dtp-id    :report
                       :field-key :beginning}]
      [tm/date-button {:value     end-value
                       :label     end-label
                       :dtp-id    :report
                       :field-key :end}]]]))

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

             [top-section/component props (:settings screens)]

             [interval-buttons]

             [:> paper/Divider]

             [:> paper/Paragraph "Cumulative time of tracked sessions with the same set of tags"]

             [pie-chart]

             [:> paper/Divider]

             [pattern-graph]

             [:> paper/Divider]

             [stacked-bar-chart]]]]]))]))
