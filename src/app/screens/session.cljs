(ns app.screens.session
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]
   [applied-science.js-interop :as j]
   [reagent.core :as r]
   [app.helpers :refer [<sub >evt get-theme]]
   [potpuri.core :as p]))

(def styles
  {:surface
   {:flex            1
    :justify-content "flex-start"}})

(defn screen [props]
  (r/as-element
    [(fn [props]
       (let [theme (->> [:theme] <sub get-theme)
             tags  [{:label "tag0" :color "#ff00ff" :id #uuid "732825de-6ffb-4cb7-a02c-04dbeb3500fb"}
                    {:label "tag1" :color "#af0cff" :id #uuid "989b4f81-6f57-4f00-98dd-fedf7a6648fd"}
                    {:label "tag2" :color "#cb2111" :id #uuid "e2100a73-8dd2-45ad-84b2-d7770aa6f7a2"}]

             {tag-removal-visible :tag-removal/visible
              tag-removal-id      :tag-removal/id
              tag-removal-label   :tag-removal/label}
             (<sub [:tag-removal])]

         [:> paper/Surface {:style (-> styles :surface
                                       (merge {:background-color (-> theme (j/get :colors) (j/get :background))}))}

          [:> rn/View {:style {:padding 8}}
           [:> paper/TextInput {:label          "Label"
                                :on-change-text #(tap> %)}]

           [:> rn/View {:style {:flex-direction "row"
                                :align-items    "center"
                                :margin-top     64}}

            (for [{:keys [label color id]} tags]
              [:> paper/Button
               {:mode     "contained"
                :key      id
                :style    {:margin-right 16}
                :color    color
                :on-press #(>evt [:set-tag-removal #:tag-removal {:visible true
                                                                  :id      id
                                                                  :label   label}])}
               label])

            [:> paper/IconButton {:icon     "plus"
                                  :on-press #(tap> "adding a tag")}]

            [:> paper/Portal
             [:> paper/Modal {:visible    tag-removal-visible
                              :on-dismiss #(>evt [:set-tag-removal
                                                  #:tag-removal {:visible false
                                                                 :id      nil
                                                                 :label   nil}])}
              [:> paper/Surface
               [:> rn/View {:style {:padding 8}}
                [:> paper/IconButton {:icon     "close"
                                      :on-press #(>evt [:set-tag-removal
                                                        #:tag-removal {:visible false
                                                                       :id      nil
                                                                       :label   nil}])}]
                [:> paper/Button {:icon     "delete"
                                  :mode     "contained"
                                  :color    "red"
                                  :on-press #(tap> (str "deleting " tag-removal-id))}
                 (str "Delete tag: " tag-removal-label)]]]]]]]]))]))
