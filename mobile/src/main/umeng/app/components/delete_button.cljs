(ns umeng.app.components.delete-button
  (:require
   ["react-native-paper" :as paper]

   [umeng.app.tailwind :refer [tw]]))

(defn component
  [{:keys [on-press]}]
  [:> paper/Button {:mode     "flat"
                    :icon     "delete"
                    :style    (tw "mr-4 mt-4 w-28")
                    :on-press on-press}
   "Delete Session"])
