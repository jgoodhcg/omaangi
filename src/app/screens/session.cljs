(ns app.screens.session
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]
   ["@react-native-community/datetimepicker" :default DateTimePicker]
   [applied-science.js-interop :as j]
   [reagent.core :as r]
   [app.helpers :refer [<sub >evt get-theme]]
   [potpuri.core :as p]
   [tick.alpha.api :as t]))

(def styles
  {:surface
   {:flex            1
    :justify-content "flex-start"}

   :time-stamps-component
   {:button {:margin-right 8
             :margin-top   16}}})

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
         (str "Delete tag: " label)]]]]]))

(defn tag-add-modal []
  (let [all-tags [{:label "tag0" :color "#ff00ff" :id #uuid "732825de-6ffb-4cb7-a02c-04dbeb3500fb"}
                  {:label "tag1" :color "#af0cff" :id #uuid "989b4f81-6f57-4f00-98dd-fedf7a6648fd"}
                  {:label "tag2" :color "#cb2111" :id #uuid "e2100a73-8dd2-45ad-84b2-d7770aa6f7a2"}]

        {:tag-add-modal/keys [visible]} (<sub [:tag-add-modal])]

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

(defn time-stamps-component []
  (let [session-id (random-uuid)
        now        (t/now)
        later      (t/+ now (t/new-duration 5 :hours))
        {:time-stamps/keys [start-date-label
                            start-time-label
                            start-value
                            stop-date-label
                            stop-time-label
                            stop-value]}
        #:time-stamps {:start-date-label (-> now t/date str)
                       :start-time-label (-> now t/time (#(str (t/hour now) "-" (t/minute now))))
                       :start-value      (-> now t/inst)
                       :stop-date-label  (-> later t/date str)
                       :stop-time-label  (-> later t/time (#(str (t/hour later) "-" (t/minute later))))
                       :stop-value       (-> later t/inst)}

        {:date-time-picker/keys [value mode visible field-key]
         picker-session-id      :date-time-picker/session-id} (<sub [:date-time-picker])]

    [:> rn/View {:style {:display        "flex"
                         :flex-direction "column"}}

     (when visible
       [:> DateTimePicker {:value     value :mode mode
                           :on-change #(do (tap> (str "Update " field-key " for " picker-session-id " as "
                                                      (-> %
                                                          (j/get :nativeEvent)
                                                          (j/get :timestamp)
                                                          t/instant)))
                                           (>evt [:set-date-time-picker
                                                  #:date-time-picker
                                                  {:value      nil
                                                   :mode       nil
                                                   :session-id nil
                                                   :field-key  nil
                                                   :visible    false}]))}])

     ;; start
     [:> rn/View {:style {:display        "flex"
                          :flex-direction "row"}}

      [:> paper/Button {:mode     "contained"
                        :style    (-> styles :time-stamps-component :button)
                        :on-press #(>evt [:set-date-time-picker
                                          #:date-time-picker
                                          {:value      start-value
                                           :mode       "date"
                                           :session-id session-id
                                           :field-key  :session/start
                                           :visible    true}])} start-date-label]

      [:> paper/Button {:mode     "contained"
                        :style    (-> styles :time-stamps-component :button)
                        :on-press #(>evt [:set-date-time-picker
                                          #:date-time-picker
                                          {:value      start-value
                                           :mode       "time"
                                           :session-id session-id
                                           :field-key  :session/start
                                           :visible    true}])} start-time-label]]

     ;; end
     [:> rn/View {:style {:display        "flex"
                          :flex-direction "row"}}

      [:> paper/Button {:mode     "contained"
                        :style    (-> styles :time-stamps-component :button)
                        :on-press #(>evt [:set-date-time-picker
                                          #:date-time-picker
                                          {:value      stop-value
                                           :mode       "date"
                                           :session-id session-id
                                           :field-key  :session/stop
                                           :visible    true}])} stop-date-label]
      [:> paper/Button {:mode     "contained"
                        :style    (-> styles :time-stamps-component :button)
                        :on-press #(>evt [:set-date-time-picker
                                          #:date-time-picker
                                          {:value      stop-value
                                           :mode       "time"
                                           :session-id session-id
                                           :field-key  :session/stop
                                           :visible    true}])} stop-time-label]]]))

(defn screen [props]
  (r/as-element
    [(fn [props]
       (let [theme (->> [:theme] <sub get-theme)]

         [:> paper/Surface {:style (-> styles :surface
                                       (merge {:background-color (-> theme (j/get :colors) (j/get :background))}))}

          [:> rn/View {:style {:padding         8
                               :display         "flex"
                               :flex            1
                               :flex-direction  "column"
                               :justify-content "space-around"}}

           [label-component]

           [tags-component]

           [time-stamps-component]

           ]]))]))
