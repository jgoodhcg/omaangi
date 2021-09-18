(ns app.screens.tag  (:require
                      ["color" :as color]
                      ["react-native" :as rn]
                      ["react-native-paper" :as paper]

                      [applied-science.js-interop :as j]
                      [com.rpl.specter :as sp :refer [setval]]
                      [reagent.core :as r]

                      [app.colors :refer [material-500-hexes]]
                      [app.components.generic-top-section :as top-section]
                      [app.components.color-picker :as color-picker]
                      [app.components.tag-button :as tag-button]
                      [app.helpers :refer [<sub >evt get-theme chance]]
                      [app.screens.core :refer [screens]]
                      [app.tailwind :refer [tw]]
                      [potpuri.core :as p]))

(defn screen [props]
  (r/as-element
    [(fn []
       (let [theme                        (->> [:theme] <sub get-theme)
             {:tag/keys [color id label]} (<sub [:selected-tag])
             mode                         (if (some? color) "contained" "outlined")]

         ;; TODO justin 2021-02-07 Do we need safe area view everywhere?
         [:> rn/ScrollView {:style (merge (tw "flex flex-1")
                                          {:background-color (-> theme (j/get :colors) (j/get :background))})}

          [:> rn/StatusBar {:visibility "hidden"}]


          [:> rn/View {:style (tw "flex p-4 flex-col")}

           [:> paper/TextInput {:style          (tw "mb-8")
                                :label          "label"
                                :default-value  label
                                :on-change-text #(>evt [:update-tag {:tag/id    id
                                                                     :tag/label %}])}]
           [:> rn/View {:style (tw "flex flex-col mb-8")}
            [:> paper/Button {:mode     mode
                              :icon     "palette"
                              :color    color
                              :on-press #(>evt [:set-color-picker
                                                #:color-picker {:visible true
                                                                :value   color}])}
             label]

            [color-picker/component {:input-color color
                                     :update-fn   #(>evt [:update-tag {:tag/id        id
                                                                       :tag/color-hex %}])
                                     :remove-fn   #(>evt [:update-tag {:tag/id           id
                                                                       :tag/remove-color true}])}]]

           ;; TODO justin 2021-09-18 Add ¿generic? delete modal
           [:> paper/Button {:mode     "outlined"
                             :icon     "delete"
                             :style    (tw "mr-4 mt-4 w-28")
                             :on-press #(>evt [:delete-tag {:tag/id id}])}
            "Delete Tag"]]]))]))
