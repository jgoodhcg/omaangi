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
  [:> paper/Text
   {:style {:width       45
            :text-align  "center"
            :font-weight "bold"}}
   (str
     (when display-year (str year " "))
     (when display-month (str month " "))
     day-of-week " "
     day-of-month)])

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
        [:> g/ScrollView
         [menu/button {:button-color menu-color
                       :toggle-menu  toggle-drawer}]

         [date-indicator this-day]]

        [:> g/ScrollView
         [:> paper/Title (str (count sessions))]]]])))
