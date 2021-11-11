(ns app.screens.session-template
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]

   [applied-science.js-interop :as j]
   [reagent.core :as r]

   [app.components.label :as label]
   [app.helpers :refer [<sub >evt get-theme]]
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
