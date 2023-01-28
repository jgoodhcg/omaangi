(ns umeng.app.components.delete-button
  (:require
   ["react-native-paper" :as paper]

   [umeng.app.tailwind :refer [tw]]))

(defn component
  [{:keys [on-press]}]
  [:> paper/Button {:mode     "flat"
                    :icon     "delete"
                    :style    (tw "m-4")
                    :on-press on-press}
   "Delete Session"])
