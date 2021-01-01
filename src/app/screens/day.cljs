(ns app.screens.day
  (:require
   ["react-native" :as rn]
   ["react-native-gesture-handler" :as g]
   ["react-native-paper" :as paper]
   [applied-science.js-interop :as j]
   [reagent.core :as r]
   [potpuri.core :as p]
   [app.helpers :refer [<sub >evt]]
   [app.components.menu :as menu]))

(def styles
  {:surface           {:flex 1 :justify-content "flex-start"}
   :date-indicator    {:container {:width          40
                                   :display        "flex"
                                   :flex-direction "column"}
                       :text      {:font-weight "bold"
                                   :text-align  "center"}}
   :tracking-sessions {:container {:display         "flex"
                                   :flex-grow       1
                                   :justify-content "center"}}
   :top-section       {:outer {:height         140
                               :display        "flex"
                               :flex-direction "row"}
                       :inner {:display        "flex"
                               :flex-direction "column"
                               :align-items    "center"}}} )

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
  [:> g/ScrollView {:content-container-style
                    (-> styles :tracking-sessions :container)}
   (for [i (range 50)]
     [:> paper/Text {:key i} (str "I'm the " i " th tracking session")])])

(defn top-section [{:keys [menu-color toggle-drawer this-day]}]
  [:> rn/View {:style (-> styles :top-section :outer)}

   [:> rn/View {:style (-> styles :top-section :inner)}
    [menu/button {:button-color menu-color
                  :toggle-menu  toggle-drawer}]

    [date-indicator this-day]]

   [tracking-sessions]])

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

      [:> paper/Surface {:style (-> styles :surface)}
       [:> rn/View
        [:> rn/StatusBar {:visibility "hidden"}]

        [top-section (p/map-of menu-color toggle-drawer this-day)]

        [:> g/ScrollView
         [:> paper/Title (str (count sessions))]]]])))
