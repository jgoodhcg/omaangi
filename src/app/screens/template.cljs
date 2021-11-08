(ns app.screens.template
  (:require
   ["react-native" :as rn]
   ["react-native-color-picker" :as c]
   ["react-native-gesture-handler" :as g]
   ["react-native-paper" :as paper]
   ["react-native-modal-datetime-picker" :default DateTimePicker]

   [applied-science.js-interop :as j]
   [potpuri.core :as p]
   [reagent.core :as r]
   [tick.alpha.api :as t]

   [app.helpers :refer [<sub >evt get-theme clear-datetime-picker >evt-sync]]
   [app.components.label :as label]
   [app.components.time-indicators :as time-indicators]
   [app.tailwind :refer [tw]]))

(defn screen [props]
  (r/as-element
    [(fn [props]
       (let [theme                     (->> [:theme] <sub get-theme)
             zoom                      (<sub [:zoom])
             {:template/keys [id
                              label
                              session-templates]
              :as            template} (<sub [:selected-template])]

         [:> rn/ScrollView {:style {:background-color
                                    (-> theme (j/get :colors) (j/get :background))}}
          [:> paper/Surface {:style (-> (tw "flex flex-1")
                                        ;; TODO justin 2020-01-23 Move this to tailwind custom theme
                                        ;; (merge {:background-color (-> theme (j/get :colors) (j/get :background))})
                                        )}

           [:> rn/View {:style (tw "flex p-4 flex-col")}

            [label/component {:label     label
                              :update-fn #(>evt [:update-template {:template/label %
                                                                   :template/id    id}])}]

            [:> rn/View
             {:style {:height        (-> 1440 (* zoom))
                      :margin-bottom 256
                      :margin-top    32}}

             [time-indicators/component]

             ]

            ]]]))]))
