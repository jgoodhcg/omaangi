(ns umeng.app.components.time-indicators
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]

   [applied-science.js-interop :as j]

   [umeng.app.misc :refer [<sub]]
   [umeng.app.tailwind :refer [tw]]))

(defn component [{:keys [theme]}]
  (let [hours (<sub [:hours])]
    [:> rn/View
     (for [{:keys [top val]} hours]
       [:> rn/View {:key   (str (random-uuid) "-time-indicator")
                    :style (-> (tw "absolute w-full ml-1")
                               (merge {:top top}))}
        [:> paper/Divider]
        [:> paper/Text {:style {:color (-> theme
                                           (j/get :colors)
                                           (j/get :onSurfaceDisabled))}}
         val]])]))
