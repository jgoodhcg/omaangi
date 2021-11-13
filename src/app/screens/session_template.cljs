(ns app.screens.session-template
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]

   [applied-science.js-interop :as j]
   [reagent.core :as r]

   [app.components.label :as label]
   [app.components.tag-related :as tags]
   [app.components.color-override :as color-override]
   [app.components.time-related :as tm]
   [app.components.delete-button :as delete-button]
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

            [tags/tags-component {:add-fn    #(>evt [:add-tag-to-session-template
                                                     {:session-template/id id
                                                      :tag/id              %}])
                                  :remove-fn #(>evt [:remove-tag-from-session-template
                                                     {:session-template/id id
                                                      :tag/id              %}])
                                  :tags      tags}]

            [color-override/component {:update-fn      #(>evt [:update-session-template
                                                               {:session-template/color-hex %
                                                                :session-template/id        id}])
                                       :remove-fn      #(>evt [:update-session-template
                                                               {:session-template/remove-color true
                                                                :session-template/id           id}])
                                       :color          color
                                       :color-override color-override}]

            [delete-button/component {:on-press #(>evt [:delete-session-template session-template])}]
            ]]]))]))
