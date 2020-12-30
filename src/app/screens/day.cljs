(ns app.screens.day
  (:require
   ["expo" :as ex]
   ["expo-constants" :as expo-constants]
   ["react-native" :as rn]
   ["react" :as react]
   ["@react-navigation/native" :as nav]
   ["@react-navigation/drawer" :as drawer]
   ["react-native-paper" :as paper]
   [applied-science.js-interop :as j]
   [reagent.core :as r]
   [app.helpers :refer [style-sheet]]))

(def styles
  (-> {:surface
       {:flex            1
        :justify-content "center"}}
      style-sheet))

(defn screen [props]
  (r/as-element
    [:> paper/Surface {:style (-> styles (j/get :surface))}
     [:> rn/View
      [:> paper/Title "Day Here!"]]]))
