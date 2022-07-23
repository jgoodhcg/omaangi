(ns umeng.app.screens.templates
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]

   [applied-science.js-interop :as j]
   [reagent.core :as r]

   [umeng.app.components.generic-top-section :as top-section]
   [umeng.app.misc :refer [<sub >evt get-theme]]
   [umeng.app.screens.core :refer [screens]]
   [umeng.app.tailwind :refer [tw]]))

(defn screen [props]
  (r/as-element
    [(fn []
       (let [theme     (->> [:theme] <sub get-theme)
             templates (<sub [:templates-list])]

         [:> rn/SafeAreaView {:style (merge (tw "flex flex-1")
                                            {:background-color (-> theme (j/get :colors) (j/get :background))})}

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
                              :on-press #(>evt [:create-template-from-nothing])} "Add new template"]]]]))]))
