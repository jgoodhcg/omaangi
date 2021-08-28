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
   [app.helpers :refer [<sub >evt get-theme clear-datetime-picker >evt-sync]]
   [app.tailwind :refer [tw]]))

(defn label-component
  [{:keys [id label]}]
  [:> paper/TextInput {:label          "Label"
                       :default-value  label
                       :style          (tw "mb-8")
                       :on-change-text #(>evt [:update-session {:session/label %
                                                                :session/id    id}])}])

(defn tag-button
  [{:keys [color label on-press style]}]
  [:> paper/Button
   (merge
     (when (some? on-press)
       {:on-press on-press})
     (when (some? style)
       {:style style})
     {:mode  (if (some? color) "contained" "outlined")
      :color color})
   label])

(defn tag-remove-modal [{:keys [session-id]}]
  (let [{:tag-remove-modal/keys [visible id label color]}
        (<sub [:tag-remove-modal])]

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
                          :on-press #(do (>evt [:set-tag-remove-modal
                                                #:tag-remove-modal
                                                {:visible false
                                                 :id      nil
                                                 :label   nil}])
                                         (>evt [:remove-tag-from-session
                                                {:session/id session-id
                                                 :tag/id     id}]))}
         "remove it"]]]]]))

(defn tag-add-modal [{:keys [session-id]}]
  (let [all-tags                        (<sub [:tags-not-on-selected-session])
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
         (for [{:tag/keys [label color id]} all-tags]
           [tag-button (merge {:key      id
                               :style    (tw "m-2")
                               :on-press #(do (>evt [:add-tag-to-session
                                                     {:session/id session-id
                                                      :tag/id     id}])
                                              (>evt [:set-tag-add-modal
                                                     #:tag-add-modal {:visible false}]))}
                              (p/map-of label color id))])]]]]]))

(defn tags-component
  [{:keys      [tags]
    session-id :id}]
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
           [tag-button (merge {:key id}
                              (p/map-of color id label on-press style))])))

     [:> paper/Button {:icon     "plus"
                       :mode     "outlined"
                       :on-press #(>evt [:set-tag-add-modal
                                         #:tag-add-modal
                                         {:visible true}])}
      "Add tag"]

     [tag-remove-modal (p/map-of session-id)]

     [tag-add-modal (p/map-of session-id)]]))

(defn date-button
  [{:keys [value id label field-key]}]
  [:> paper/Button {:mode     "contained"
                    :icon     "calendar"
                    :style    (tw "mr-4 mt-4 w-40")
                    :on-press #(>evt [:set-date-time-picker
                                      #:date-time-picker
                                      {:value      value
                                       :mode       "date"
                                       :id         :session
                                       :session-id id
                                       :field-key  field-key
                                       :visible    true}])} label])

(defn time-button
  [{:keys [value id label field-key]}]
  [:> paper/Button {:mode     "contained"
                    :icon     "clock"
                    :style    (tw "mr-4 mt-4 w-28")
                    :on-press #(>evt [:set-date-time-picker
                                      #:date-time-picker
                                      {:value      value
                                       :mode       "time"
                                       :id         :session
                                       :session-id id
                                       :field-key  field-key
                                       :visible    true}])} label])

(defn no-stamp-button
  [{:keys [id set-start set-stop]
    :or   {set-start false
           set-stop  false}}]
  [:> paper/Button {:mode     "outlined"
                    :icon     "calendar"
                    :style    (tw "mr-4 mt-4 w-40")
                    :on-press #(>evt-sync
                                 [:set-initial-timestamp
                                  {:set-start  set-start
                                   :set-stop   set-stop
                                   :session/id id}])  } "not set"])

(defn time-stamps-component []
  (let [{:session/keys [id
                        start-date-label
                        start-time-label
                        start-value
                        start-set
                        stop-date-label
                        stop-time-label
                        stop-value
                        stop-set]}
        (<sub [:selected-session])

        {:date-time-picker/keys [value mode visible field-key]
         dtp-id                 :date-time-picker/id
         picker-session-id      :date-time-picker/session-id}
        (<sub [:date-time-picker])]

    [:> rn/View {:style (tw "flex flex-col mb-8")}

     (when (and (some? value)
                (= dtp-id :session))
       [:> DateTimePicker
        {:is-visible           visible
         :is-dark-mode-enabled true
         :date                 value
         :mode                 mode

         :on-hide    #(do
                        (tap> "hidden")
                        (>evt clear-datetime-picker))
         :on-cancel  #(do
                        (tap> "cancelled")
                        (>evt clear-datetime-picker))
         :on-confirm #(do
                        (tap> (str "Update " field-key
                                   " for " picker-session-id
                                   " as " (-> % t/instant)))
                        (>evt [:update-session
                               {field-key   %
                                :session/id picker-session-id}])
                        (>evt clear-datetime-picker))}])

     (if start-set
       [:> rn/View {:style (tw "flex flex-row")}
        [date-button {:value     start-value
                      :id        id
                      :label     start-date-label
                      :field-key :session/start}]
        [time-button {:value     start-value
                      :id        id
                      :label     start-time-label
                      :field-key :session/start}]]

       [:> rn/View {:style (tw "flex flex-row")}
        [no-stamp-button {:set-start true
                          :id        id}]])

     (if stop-set
       [:> rn/View {:style (tw "flex flex-row")}
        [date-button {:value     stop-value
                      :id        id
                      :label     stop-date-label
                      :field-key :session/stop}]
        [time-button {:value     stop-value
                      :id        id
                      :label     stop-time-label
                      :field-key :session/stop}]]

       [:> rn/View {:style (tw "flex flex-row")}
        [no-stamp-button {:set-stop true
                          :id       id}]])]))

(defn color-override-component [{session-color  :color
                                 color-override :color-override
                                 session-id     :id}]
  (let [mode  (if (and (some? session-color)
                       color-override) "contained" "outlined")
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
                              :update-fn
                              #(>evt [:update-session
                                      {:session/color-hex %
                                       :session/id        session-id}])
                              :remove-fn
                              #(>evt [:update-session
                                      {:session/remove-color true
                                       :session/id           session-id}])}]]))

(defn session-type-component [{:keys [type id]}]
  [:> rn/View {:style (tw "flex flex-row mb-8")}
   [:> paper/Button {:style    (tw "mr-4 w-24")
                     :icon     "circle-outline"
                     :mode     (case type
                                 :session/plan "contained"
                                 "outlined")
                     :on-press #(>evt [:update-session
                                       {:session/type :session/plan
                                        :session/id   id}])}
    "plan"]
   [:> paper/Button {:style    (tw "w-24")
                     :icon     "circle-slice-8"
                     :mode     (case type
                                 :session/track "contained"
                                 "outlined")
                     :on-press #(>evt [:update-session
                                       {:session/type :session/track
                                        :session/id   id}])}
    "track"]])

(defn screen [props]
  (r/as-element
    [(fn [props]
       (let [theme (->> [:theme] <sub get-theme)

             {:session/keys [id
                             start
                             stop
                             type
                             label
                             tags
                             color-override
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

            [color-override-component (p/map-of color id color-override)]

            [tags-component (p/map-of tags id)]

            ]]]))]))
