(ns app.screens.day
  (:require
   ["react-native" :as rn]
   ["react-native-gesture-handler" :as g]
   ["react-native-paper" :as paper]
   [applied-science.js-interop :as j]
   [reagent.core :as r]
   [app.helpers :refer [<sub >evt]]
   [app.components.menu :as menu]))


(def styles
  {:surface {:flex 1 :justify-content "flex-start"}} )

(defn date-indicator [{:keys [day-of-week
                              day-of-month
                              year
                              month
                              display-year
                              display-month]}]
  [:> rn/View {:style {:width          40
                       :display        "flex"
                       :flex-direction "column"}}

   [:> paper/Text {:style {:font-weight "bold"
                           :text-align  "center"}} (when display-year year)]
   [:> paper/Text {:style {:font-weight "bold"
                           :text-align  "center"}} (when display-month month)]
   [:> paper/Text {:style {:font-weight "bold"
                           :text-align  "center"}} day-of-week]
   [:> paper/Text {:style {:font-weight "bold"
                           :text-align  "center"}} day-of-month]])

(defn screen [props]
  (r/as-element
    (let [theme         (-> props (j/get :theme))
          menu-color    (-> theme
                            (j/get :colors)
                            (j/get :text))
          toggle-drawer (-> props
                            (j/get :navigation)
                            (j/get :toggleDrawer))
          sessions      (<sub [:sessions-for-this-day])
          this-day      (<sub [:this-day])]

      [:> paper/Surface {:style (:surface styles)}
       [:> rn/View
        [:> rn/StatusBar {:visibility "hidden"}]

        [:> rn/View {:style {:display        "flex"
                             :height         145
                             :flex-direction "row"
                             :align-items    "stretch"}}

         [:> rn/View {:style {:flex-direction "column"
                              :align-items    "center"}}
          [menu/button {:button-color menu-color
                        :toggle-menu  toggle-drawer}]

          [date-indicator this-day]]

         [:> g/ScrollView {:content-container-style {:flex-grow       1
                                                     :justify-content "center"}}
          (for [i (range 25)]
            [:> paper/Text {:key i} (str "I'm the " i " th tracking session")])]]

        [:> g/ScrollView
         [:> paper/Title (str (count sessions))]]]])))
