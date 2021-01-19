(ns app.screens.session
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]
   [applied-science.js-interop :as j]
   [reagent.core :as r]
   [app.helpers :refer [<sub >evt get-theme]]))

(def styles
  {:surface
   {:flex            1
    :justify-content "center"}})

(defn screen [props]
  (let [theme (->> [:theme] <sub get-theme)]
    (r/as-element
      [:> paper/Surface {:style (-> styles :surface
                                    (merge {:background-color (-> theme (j/get :colors) (j/get :background))}))}
       [:> rn/View
        [:> paper/Title "Edit session here!"]]])))
