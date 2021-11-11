(ns app.screens.session-template
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

   [app.colors :refer [material-500-hexes]]
   [app.components.color-picker :as color-picker]
   [app.components.tag-button :as tag-button]
   [app.components.label :as label]
   [app.helpers :refer [<sub >evt get-theme clear-datetime-picker >evt-sync]]
   [app.tailwind :refer [tw]]))

(defn screen [props]
  (r/as-element
    [(fn [props]
       (let [theme (->> [:theme] <sub get-theme)

             {:session-template/keys
              [id
               start
               stop
               type
               label
               tags
               color-override
               color]
              :as session-template} (<sub [:selected-session-template])]

         [:> rn/ScrollView {:style {:background-color
                                    (-> theme (j/get :colors) (j/get :background))}}
          [:> paper/Surface {:style (-> (tw "flex flex-1")
                                        ;; TODO justin 2020-01-23 Move this to tailwind custom theme
                                        ;; (merge {:background-color (-> theme (j/get :colors) (j/get :background))})
                                        )}

           [:> rn/View {:style (tw "flex p-4 flex-col")}

            [label/component {:label     label
                              :update-fn #(>evt [:update-session-template
                                                 {:session-template/label %
                                                  :session-template/id    id}])}]

            ]]]))]))
