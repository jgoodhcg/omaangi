(ns app.screens.session
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
   [app.helpers :refer [<sub >evt get-theme clear-datetime-picker]]
   [app.tailwind :refer [tw]]))

(defn label-component
  [{:keys [id label]}]
  [:> paper/TextInput {:label          "Label"
                       :default-value  label
                       :style          (tw "mb-8")
                       :on-change-text #(>evt [:update-session {:session/label %
                                                                :session/id    id}])}])

(defn tag-button
  [{:keys [color id label on-press style]}]
  [:> paper/Button
   (merge
     (when (some? on-press)
       {:on-press on-press})
     (when (some? style)
       {:style style})
     {:mode  (if (some? color) "contained" "outlined")
      :key   id
      :color color})
   label])

(defn tag-remove-modal []
  (let [{:tag-remove-modal/keys [visible id label color]} (<sub [:tag-remove-modal])]

    [:> paper/Portal
     [:> paper/Modal {:visible    visible
                      :on-dismiss #(>evt [:set-tag-remove-modal
                                          #:tag-remove-modal {:visible false
                                                              :id      nil
                                                              :label   nil}])}
      [:> paper/Surface
       [:> rn/View {:style (tw "p-2 flex-col")}
        ;; close button
        [:> paper/IconButton {:icon     "close"
                              :on-press #(>evt [:set-tag-remove-modal
                                                #:tag-remove-modal {:visible false
                                                                    :id      nil
                                                                    :label   nil}])}]

        [:> paper/Paragraph
         {:style (tw "mb-4")}
         "Are you sure you want to remove this tag?"]


        [tag-button (merge {:style (tw "mb-4")}
                           (p/map-of color id label))]

        [:> paper/Button {:icon     "close"
                          :mode     "contained"
                          :style    (tw "mb-4")
                          :color    "red"
                          :on-press #(tap> (str "deleting " id))}
         "remove it"]]]]]))

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

(defn tags-component
  [{:keys [tags id]}]
  (let [there-are-tags (-> tags count (> 0))]

    [:> rn/View {:style (tw "flex flex-row flex-wrap items-center mb-8")}

     (when there-are-tags
       (for [{:tag/keys [label color id]} tags]
         (let [on-press #(>evt [:set-tag-remove-modal
                                #:tag-remove-modal
                                {:visible true
                                 :id      id
                                 :color   color
                                 :label   label}])
               style    (tw "mr-4 mb-4")]
           [tag-button (p/map-of color id label on-press style)])))

     [:> paper/Button {:icon     "plus"
                       :mode     "outlined"
                       :on-press #(>evt [:set-tag-add-modal
                                         #:tag-add-modal
                                         {:visible true}])}
      "Add tag"]

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
                        :style    (tw "mr-4 mt-4 w-40")
                        :on-press #(>evt [:set-date-time-picker
                                          #:date-time-picker
                                          {:value      start-value
                                           :mode       "date"
                                           :session-id session-id
                                           :field-key  :session/start
                                           :visible    true}])} start-date-label]

      [:> paper/Button {:mode     "contained"
                        :icon     "clock"
                        :style    (tw "mr-4 mt-4 w-28")
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
                        :style    (tw "mr-4 mt-4 w-40")
                        :on-press #(>evt [:set-date-time-picker
                                          #:date-time-picker
                                          {:value      stop-value
                                           :mode       "date"
                                           :session-id session-id
                                           :field-key  :session/stop
                                           :visible    true}])} stop-date-label]

      [:> paper/Button {:mode     "contained"
                        :icon     "clock"
                        :style    (tw "mr-4 mt-4 w-28")
                        :on-press #(>evt [:set-date-time-picker
                                          #:date-time-picker
                                          {:value      stop-value
                                           :mode       "time"
                                           :session-id session-id
                                           :field-key  :session/stop
                                           :visible    true}])} stop-time-label]]]))

(def tmp-session-color-state (r/atom nil))

(defn color-override-component []
  (let [{session-color :session/color}
        {:session/color @tmp-session-color-state}

        mode  (if (some? session-color) "contained" "outlined")
        label (if (some? session-color) session-color "set session color")]

    [:> rn/View {:style (tw "flex flex-col mb-8")}
     [:> paper/Button {:mode     mode
                       :icon     "palette"
                       :color    session-color
                       :on-press #(>evt [:set-color-picker
                                         #:color-picker {:visible true
                                                         :value   session-color}])}
      label]

     [color-picker/component {:input-color session-color
                              :update-fn   #(reset! tmp-session-color-state %)}]]))

(defn session-type-component []
(let [{session-type :session/type} {:session/type :session/track}]
  [:> rn/View {:style (tw "flex flex-row mb-8")}
   [:> paper/Button {:style (tw "mr-4 w-24")
                     :icon  "circle-outline"
                     :mode  (case session-type
                              :session/plan "contained"
                              "outlined")}
    "plan"]
   [:> paper/Button {:style (tw "w-24")
                     :icon  "circle-slice-8"
                     :mode  (case session-type
                              :session/track "contained"
                              "outlined")}
    "track"]]))

(defn screen [props]
  (r/as-element
    [(fn [props]
       (let [theme                   (->> [:theme] <sub get-theme)
             {:session/keys [id
                             start
                             stop
                             type
                             label
                             tags
                             color]} (<sub [:selected-session])]
         [:> rn/ScrollView {:style {:background-color
                                    (-> theme (j/get :colors) (j/get :background))}}
          [:> paper/Surface {:style (-> (tw "flex flex-1")
                                        ;; TODO justin 2020-01-23 Move this to tailwind custom theme
                                        ;; (merge {:background-color (-> theme (j/get :colors) (j/get :background))})
                                        )}

           [:> rn/View {:style (tw "flex p-4 flex-col")}

            [label-component (p/map-of id label)]

            [time-stamps-component (p/map-of start stop id)]

            [session-type-component (p/map-of type id)]

            [color-override-component (p/map-of color id)]

            [tags-component (p/map-of tags id)]

            ]]]))]))
