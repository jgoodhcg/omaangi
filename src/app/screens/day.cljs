(ns app.screens.day
  (:require
   ["color" :as color]
   ["react-native" :as rn]
   ["react-native-gesture-handler" :as g]
   ["react-native-paper" :as paper]
   [applied-science.js-interop :as j]
   [reagent.core :as r]
   [potpuri.core :as p]
   [app.helpers :refer [<sub >evt get-theme]]
   [app.components.menu :as menu]))

(def styles
  {:surface           {:flex 1 :justify-content "flex-start"}
   :date-indicator    {:container {:display         "flex"
                                   :flex-grow       1
                                   :flex-direction  "row"
                                   :justify-content "center"}
                       :text      {:font-weight "bold"
                                   :text-align  "center"
                                   :margin      8}}
   :tracking-sessions {:surface       {:padding       8
                                       :margin-bottom 4}
                       :container     {:width  "100%"
                                       :height 32
                                       :margin 4}
                       :session       {:position "absolute"
                                       :top      0
                                       :left     0
                                       :height   32
                                       :padding  4
                                       :overflow "hidden"}
                       :indicator     {:position "absolute"
                                       :top      0
                                       :width    8
                                       :height   32}
                       :dbl-container {:position       "absolute"
                                       :top            0
                                       :right          16
                                       :display        "flex"
                                       :flex-direction "row"
                                       :align-items    "center"
                                       :width          8
                                       :height         32}
                       :button        {:position "absolute"
                                       :left     0
                                       :top      0
                                       :height   32
                                       :width    "100%"}}
   :top-section       {:outer {:max-height     256
                               :display        "flex"
                               :flex-direction "column"}
                       :inner {:display         "flex"
                               :flex-direction  "row"
                               :align-items     "center"
                               :justify-content "flex-start"}}} )

(defn date-indicator [{:keys [day-of-week
                              day-of-month
                              year
                              month
                              display-year
                              display-month]}]
  [:> rn/View {:style (-> styles :date-indicator :container)}

   (when display-year
     [:> paper/Text {:style (-> styles :date-indicator :text)} year])
   (when display-month
     [:> paper/Text {:style (-> styles :date-indicator :text)} month])
   [:> paper/Text {:style (-> styles :date-indicator :text)} day-of-week]
   [:> paper/Text {:style (-> styles :date-indicator :text)} day-of-month]])

(defn tracking-sessions []
  (let [theme  (->> [:theme] <sub get-theme)
        tracks (<sub [:tracking])]
    [:> g/ScrollView
     [:> paper/Surface {:style (-> styles :tracking-sessions :surface)}

      (for [{:tracking-render/keys
             [relative-width
              color-hex
              text-color-hex
              indicator-color-hex
              indicator-position
              show-indicator
              ripple-color-hex
              label] :as t} tracks]
        ;; container
        [:> rn/View {:key   (random-uuid)
                     :style (-> styles :tracking-sessions :container)}

         ;; session
         [:> rn/View {:style (merge
                               (-> styles :tracking-sessions :session)
                               {:width            relative-width
                                :border-radius    (-> theme (j/get :roundness))
                                :background-color color-hex})}

          ;; intended duration indication
          (when show-indicator
            [:> rn/View {:style (merge
                                  (-> styles :tracking-sessions :indicator)
                                  {:left             indicator-position
                                   :background-color indicator-color-hex})}])

          [:> paper/Text {:style {:color text-color-hex}} label]]

         ;; selection button needs to go "over" everything
         [:> g/RectButton {:on-press       #(println "selected tracking item")
                           :ripple-color   ripple-color-hex
                           :underlay-color ripple-color-hex
                           :active-opacity 0.7
                           :style          (-> styles :tracking-sessions :button
                                               (merge {:border-radius (-> theme (j/get :roundness))}))}]])]]))

(defn top-section [props]
  (let [this-day      (<sub [:this-day])
        theme         (->> [:theme] <sub get-theme)
        menu-color    (-> theme
                          (j/get :colors)
                          (j/get :text))
        toggle-drawer (-> props
                          (j/get :navigation)
                          (j/get :toggleDrawer))]

    [:> rn/View {:style (-> styles :top-section :outer)}

     [:> rn/View {:style (-> styles :top-section :inner)}
      [menu/button {:button-color menu-color
                    :toggle-menu  toggle-drawer}]

      [date-indicator this-day]]

     [tracking-sessions]]))

(defn time-indicators []
  (let [theme (->> [:theme] <sub get-theme)
        hours (<sub [:hours])]
    [:> rn/View
     (for [{:keys [top val]} hours]
       [:> rn/View {:key   (str (random-uuid))
                    :style {:position    "absolute"
                            :top         top
                            :width       "100%"
                            :margin-left 4}}
        [:> paper/Divider]
        [:> paper/Text {:style {:color (-> theme
                                           (j/get :colors)
                                           (j/get :disabled))}}
         val]])]))

(defn sessions-component []
  (let [theme    (->> [:theme] <sub get-theme)
        sessions (<sub [:sessions-for-this-day])]

    [:> rn/View {:style {:margin-left 64}}
     (for [{:session-render/keys [left
                                  top
                                  height
                                  width
                                  elevation
                                  color-hex
                                  ripple-color-hex
                                  text-color-hex
                                  label]
            :as                  s} sessions]

       [:> g/RectButton {:key            (:session/id s)
                         :style          {:position         "absolute"
                                          :top              top
                                          :left             left
                                          :height           height
                                          :width            width
                                          :elevation        elevation
                                          :background-color color-hex
                                          :border-radius    (-> theme (j/get :roundness))}
                         :on-press       #(println "selected session item")
                         :ripple-color   ripple-color-hex
                         :underlay-color ripple-color-hex
                         :active-opacity 0.7}
        [:> rn/View {:style {:height   "100%"
                             :width    "100%"
                             :overflow "hidden"
                             :padding  4}}
         [:> paper/Text {:style {:color text-color-hex}} label]]])]))

(defn screen [props]
  (r/as-element
    (let [theme         (->> [:theme] <sub get-theme)
          zoom          (<sub [:zoom])
          now-indicator (<sub [:now-indicator])]

      [:> rn/SafeAreaView {:style {:display          "flex"
                                   :flex             1
                                   :background-color (-> theme (j/get :colors) (j/get :background))}}
       [:> paper/Surface {:style (-> styles :surface
                                     (merge {:background-color (-> theme (j/get :colors) (j/get :background))}))}
        [:> rn/View
         [:> rn/StatusBar {:visibility "hidden"}]

         [top-section props]

         [:> g/ScrollView
          [:> rn/View {:style {:height        (-> 1440 (* zoom))
                               :margin-bottom 128}}

           [time-indicators]

           [sessions-component]

           ;; now indicator
           (let [{:now-indicator-render/keys
                  [position
                   label
                   display-indicator]} now-indicator]
             (when true ;; TODO display-indicator
               [:> rn/View {:style {:position "absolute"
                                    :top      position
                                    :left     64
                                    :width    "100%"}}
                [:> rn/View {:style {:width            "100%"
                                     :height           2
                                     :background-color (-> theme (j/get :colors) (j/get :text))}}]
                [:> paper/Text label]]))]]]]])))
