(ns app.screens.templates
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
   [app.components.tag-button :as tag-button]
   [app.helpers :refer [<sub >evt get-theme chance]]
   [app.screens.core :refer [screens]]
   [app.tailwind :refer [tw]]
   [potpuri.core :as p]))

(defn screen [props]
  (r/as-element
    [(fn []
       (let [theme     (->> [:theme] <sub get-theme)
             templates (<sub [:templates-list])]

         [:> rn/ScrollView {:style (merge (tw "flex flex-1")
                                          {:background-color (-> theme (j/get :colors) (j/get :background))})}

          [:> rn/StatusBar {:visibility "hidden"}]

          [top-section/component props (:templates screens)]

          [:> rn/View {:style (tw "flex flex-col")}

           [:> rn/View {:style (tw "flex flex-row flex-wrap items-center mb-8")}
            (for [{label              :template/label
                   sesssion-templates :template/session-templates
                   id                 :template/id} templates]
              [:> rn/View {:key id}
               [:> paper/Button {:mode     "flat"
                                 :style    (tw "ml-4 mb-4")
                                 :on-press #(do (>evt [:set-selected-template id])
                                                (>evt [:navigate (:template screens)]))} label]])]

           [:> paper/Button {:mode     "flat"
                             :icon     "plus"
                             :on-press #(>evt [:create-template-from-nothing])}]]]))]))
