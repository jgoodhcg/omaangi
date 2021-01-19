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

(defn label-component []
  [:> paper/TextInput {:label          "Label"
                       :on-change-text #(tap> %)}])

(defn tag-remove-modal []
  (let [{:tag-remove-modal/keys [visible id label]} (<sub [:tag-remove-modal])]

    [:> paper/Portal
     [:> paper/Modal {:visible    visible
                      :on-dismiss #(>evt [:set-tag-remove-modal
                                          #:tag-remove-modal {:visible false
                                                              :id      nil
                                                              :label   nil}])}
      [:> paper/Surface
       [:> rn/View {:style {:padding 8}}
        [:> paper/IconButton {:icon     "close"
                              :on-press #(>evt [:set-tag-remove-modal
                                                #:tag-remove-modal {:visible false
                                                                    :id      nil
                                                                    :label   nil}])}]
        [:> paper/Button {:icon     "delete"
                          :mode     "contained"
                          :color    "red"
                          :on-press #(tap> (str "deleting " id))}
         (str "Delete tag: " label)]]]]]
    ))

(defn tag-add-modal []
  (let [all-tags [{:label "tag0" :color "#ff00ff" :id #uuid "732825de-6ffb-4cb7-a02c-04dbeb3500fb"}
                  {:label "tag1" :color "#af0cff" :id #uuid "989b4f81-6f57-4f00-98dd-fedf7a6648fd"}
                  {:label "tag2" :color "#cb2111" :id #uuid "e2100a73-8dd2-45ad-84b2-d7770aa6f7a2"}]

        {:tag-add-modal/keys [visible] :as tag-add} (<sub [:tag-add-modal])]

    (tap> tag-add)

    [:> paper/Portal
     [:> paper/Modal {:visible    visible
                      :on-dismiss #(>evt [:set-tag-add-modal
                                          #:tag-add-modal {:visible false}])}
      [:> paper/Surface
       [:> rn/View {:style {:padding 8}}
        [:> paper/IconButton {:icon     "close"
                              :on-press #(>evt [:set-tag-add-modal
                                                #:tag-add-modal {:visible false}])}]
        (for [{:keys [label color id]} all-tags]
          [:> paper/Button
           {:mode     "contained"
            :key      id
            :style    {:margin 8}
            :color    color
            :on-press #(tap> "adding tag to session")}
           label])]]]]))

(defn tags-component []
  (let [tags [{:label "tag0" :color "#ff00ff" :id #uuid "732825de-6ffb-4cb7-a02c-04dbeb3500fb"}
              {:label "tag1" :color "#af0cff" :id #uuid "989b4f81-6f57-4f00-98dd-fedf7a6648fd"}
              {:label "tag2" :color "#cb2111" :id #uuid "e2100a73-8dd2-45ad-84b2-d7770aa6f7a2"}]]

    [:> rn/View {:style {:flex-direction "row"
                         :align-items    "center"
                         :margin-top     64}}

     (for [{:keys [label color id]} tags]
       [:> paper/Button
        {:mode     "contained"
         :key      id
         :style    {:margin-right 16}
         :color    color
         :on-press #(>evt [:set-tag-remove-modal
                           #:tag-remove-modal
                           {:visible true
                            :id      id
                            :label   label}])}
        label])

     [:> paper/IconButton {:icon     "plus"
                           :on-press #(>evt [:set-tag-add-modal
                                             #:tag-add-modal
                                             {:visible true}])}]

     [tag-remove-modal]

     [tag-add-modal]]))

(defn screen [props]
  (r/as-element
    [(fn [props]
       (let [theme (->> [:theme] <sub get-theme)]

         [:> paper/Surface {:style (-> styles :surface
                                       (merge {:background-color (-> theme (j/get :colors) (j/get :background))}))}

          [:> rn/View {:style {:padding 8}}

           [label-component]

           [tags-component]

           ]]))]))
