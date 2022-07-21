(ns app.screens.import
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]

   [applied-science.js-interop :as j]
   [reagent.core :as r]

   [app.components.generic-top-section :as top-section]
   [app.misc :refer [<sub >evt get-theme]]
   [app.screens.core :refer [screens]]
   [app.tailwind :refer [tw]]))

(defn screen [props]
  (r/as-element
   [(fn []
      (let [theme (->> [:theme] <sub get-theme)]

        [:> rn/SafeAreaView {:style (merge (tw "flex flex-1")
                                           {:background-color (-> theme (j/get :colors) (j/get :background))})}
         [:> rn/StatusBar {:visibility "hidden"}]

         [top-section/component props (:import screens)]

         [:> rn/View {:style (tw "flex flex-col p-2 items-center h-full justify-center")}

          [:> paper/Button {:mode     "contained"
                            :icon     "content-save"
                            :style    (tw "mb-8")
                            :on-press #(>evt [:import-backup])}
           "Import a backup"]]]))]))
