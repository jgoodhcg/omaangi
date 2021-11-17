(ns app.screens.backups
  (:require
   ["color" :as color]
   ["react-native" :as rn]
   ["react-native-paper" :as paper]

   [applied-science.js-interop :as j]
   [com.rpl.specter :as sp :refer [setval]]
   [reagent.core :as r]

   [app.colors :refer [material-500-hexes]]
   [app.components.generic-top-section :as top-section]
   [app.components.color-picker :as color-picker]
   [app.components.tag-related :as components]
   [app.helpers :refer [<sub >evt get-theme chance]]
   [app.screens.core :refer [screens]]
   [app.tailwind :refer [tw]]
   [potpuri.core :as p]))

(defn screen [props]
  (r/as-element
    [(fn []
       (let [theme (->> [:theme] <sub get-theme)
             ]

         ;; TODO justin 2021-02-07 Do we need safe area view everywhere?
         [:> rn/ScrollView {:style (merge (tw "flex flex-1")
                                          {:background-color (-> theme (j/get :colors) (j/get :background))})}

          [:> rn/StatusBar {:visibility "hidden"}]

          [top-section/component props (:backups screens)]

          [:> rn/View {:style (tw "flex flex-col")}

           [:> rn/View {:style (tw "flex flex-row flex-wrap items-center mb-8")}
            (for [{version :version
                   date    :date} [{:version 1 :date "2021-11-1"} {:version 2 :date "2021-11-2"}]]
              [:> rn/View {:key (str (random-uuid))}
               [:> paper/Text (str "version: " version " date: " date)]])]

           [:> paper/Button {:mode     "outlined"
                             :icon     "plus"
                             :on-press #(>evt [:create-backup])}]]]))]))
