(ns app.screens.day
  (:require
   ["react-native" :as rn]
   ["react-native-gesture-handler" :as g]
   ["react-native-paper" :as paper]
   [applied-science.js-interop :as j]
   [reagent.core :as r]
   [app.helpers :refer [<sub >evt]]))


(def styles
  {:surface {:flex 1 :justify-content "flex-start"}} )

(defn menu-button [{:keys [button-color toggle-menu]}]
  [:> paper/IconButton {:icon     "menu"
                        :color    button-color
                        :size     20
                        :on-press toggle-menu}])

(defn screen [props]
  (r/as-element
    (let [theme                   (-> props (j/get :theme))
          toggle-menu             (-> props
                                      (j/get :navigation)
                                      (j/get :toggleDrawer))
          sessions                (<sub [:sessions-for-this-day])
          {:keys [day-of-week
                  day-of-month
                  year
                  month
                  display-year
                  display-month]} (<sub [:this-day])]

      [:> paper/Surface {:style (:surface styles)}
       [:> rn/View
        [:> rn/StatusBar {:visibility "hidden"}]
        [:> g/ScrollView
         [menu-button {:button-color (-> theme
                                         (j/get :colors)
                                         (j/get :text))
                       :toggle-menu  toggle-menu}]
         [:> paper/Title (str
                           (when display-year (str year " "))
                           (when display-month (str month " "))
                           day-of-week " "
                           day-of-month)]]
        [:> g/ScrollView
         [:> paper/Title (str (count sessions))]]]])))
