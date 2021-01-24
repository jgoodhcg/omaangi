(ns app.screens.session
  (:require
   ["react-native" :as rn]
   ["react-native-color-picker" :as c]
   ["react-native-paper" :as paper]
   ["react-native-modal-datetime-picker" :default DateTimePicker]

   [applied-science.js-interop :as j]
   [potpuri.core :as p]
   [reagent.core :as r]
   [tick.alpha.api :as t]

   [app.colors :refer [material-500-hexes]]
   [app.helpers :refer [<sub >evt get-theme]]
   [app.tailwind :refer [tw]]))

(defn label-component []
  [:> paper/TextInput {:label          "Label"
                       :style          (tw "mb-8")
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
        [:> paper/Button {:icon     "close"
                          :mode     "contained"
                          :color    "red"
                          :on-press #(tap> (str "deleting " id))}
         (str "remove tag: " label)]]]]]))

(defn tag-add-modal []
  (let [all-tags (for [i (range 25)]
                   {:label (str "tag " i)
                    :color (-> material-500-hexes rand-nth)
                    :id    (random-uuid)})

        {:tag-add-modal/keys [visible]} (<sub [:tag-add-modal])]

    [:> paper/Portal
     [:> paper/Modal {:visible    visible
                      :on-dismiss #(>evt [:set-tag-add-modal
                                          #:tag-add-modal {:visible false}])}
      [:> paper/Surface {:style (tw "m-1")}
       [:> rn/ScrollView
        [:> paper/IconButton {:icon     "close"
                              :on-press #(>evt [:set-tag-add-modal
                                                #:tag-add-modal {:visible false}])}]

        [:> rn/View {:style (tw "flex flex-row flex-wrap items-center p-4")}
         (for [{:keys [label color id]} all-tags]
           [:> paper/Button
            {:mode     "contained"
             :key      id
             :style    (tw "m-2")
             :color    color
             :on-press #(tap> "adding tag to session")}
            label])]]]]]))

(defn tags-component []
  (let [tags (for [i (range 4)]
               {:label (str "tag " i)
                :color (-> material-500-hexes rand-nth)
                :id    (random-uuid)}) ]

    [:> rn/View {:style (tw "flex flex-row flex-wrap items-center mb-8")}

     (for [{:keys [label color id]} tags]
       [:> paper/Button
        {:mode     "contained"
         :key      id
         :style    (tw "mr-4 mb-4")
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
         picker-session-id      :date-time-picker/session-id} (<sub [:date-time-picker])

        clear-datetime-picker [:set-date-time-picker
                               #:date-time-picker
                               {:value      nil
                                :mode       nil
                                :session-id nil
                                :field-key  nil
                                :visible    false}]]

    [:> rn/View {:style (tw "flex flex-col mb-8")}

     [:> DateTimePicker {:is-visible           visible
                         :is-dark-mode-enabled true
                         :value                value
                         :mode                 mode
                         :on-hide              #(do (tap> "hidden")
                                                    (>evt clear-datetime-picker))
                         :on-cancel            #(do (tap> "cancelled")
                                                    (>evt clear-datetime-picker))
                         :on-confirm           #(do (tap> (str "Update " field-key " for " picker-session-id " as " (-> % t/instant)))
                                                    (>evt clear-datetime-picker))}]

     ;; start
     [:> rn/View {:style (tw "flex flex-row")}

      [:> paper/Button {:mode     "contained"
                        :icon     "calendar"
                        :style    (tw "mr-4 mt-4")
                        :on-press #(>evt [:set-date-time-picker
                                          #:date-time-picker
                                          {:value      start-value
                                           :mode       "date"
                                           :session-id session-id
                                           :field-key  :session/start
                                           :visible    true}])} start-date-label]

      [:> paper/Button {:mode     "contained"
                        :icon     "clock"
                        :style    (tw "mr-4 mt-4")
                        :on-press #(>evt [:set-date-time-picker
                                          #:date-time-picker
                                          {:value      start-value
                                           :mode       "time"
                                           :session-id session-id
                                           :field-key  :session/start
                                           :visible    true}])} start-time-label]]

     ;; end
     [:> rn/View {:style (tw "flex flex-row")}

      [:> paper/Button {:mode     "contained"
                        :icon     "calendar"
                        :style    (tw "mr-4 mt-4")
                        :on-press #(>evt [:set-date-time-picker
                                          #:date-time-picker
                                          {:value      stop-value
                                           :mode       "date"
                                           :session-id session-id
                                           :field-key  :session/stop
                                           :visible    true}])} stop-date-label]

      [:> paper/Button {:mode     "contained"
                        :icon     "clock"
                        :style    (tw "mr-4 mt-4")
                        :on-press #(>evt [:set-date-time-picker
                                          #:date-time-picker
                                          {:value      stop-value
                                           :mode       "time"
                                           :session-id session-id
                                           :field-key  :session/stop
                                           :visible    true}])} stop-time-label]]]))

(defn color-override-component []
  (let [{session-color :session/color}
        {:session/color nil}

        mode  (if (some? session-color) "contained" "outlined")
        label (if (some? session-color) session-color "set session color")]

    [:> rn/View {:style (tw "flex flex-col mb-8")}
     [:> paper/Button {:mode mode :icon "palette"} label]

     [:> paper/Portal
      [:> paper/Modal {:visible    true
                       :on-dismiss #(tap> "dismis color picker")}
       [:> paper/Surface {:style (tw "m-8")}
        [:> rn/View {:style (tw "h-full w-full")}
         [:> paper/IconButton {:icon     "close"
                               :on-press #(tap> "dismiss pls")}]
         [:> c/ColorPicker {:on-color-selected #(tap> %)
                            :style             (tw "flex flex-1")}]
         ]]]]]))

(defn screen [props]
  (r/as-element
    [(fn [props]
       (let [theme (->> [:theme] <sub get-theme)]

         [:> paper/Surface {:style (-> (tw "flex flex-1")
                                       ;; TODO justin 2020-01-23 Move this to tailwind custom theme
                                       (merge {:background-color (-> theme (j/get :colors) (j/get :background))}))}

          [:> rn/View {:style (tw "flex p-4 flex-col")}

           [label-component]

           [time-stamps-component]

           [color-override-component]

           [tags-component]

           ]]))]))
