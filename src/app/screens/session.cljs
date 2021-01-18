(ns app.screens.session
  (:require
   ["react-native" :as rn]
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
      [:> paper/Title "Edit session here!"]]]))
