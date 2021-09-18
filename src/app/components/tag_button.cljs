(ns app.components.tag-button
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]))

(defn component
  [{:keys [color label on-press style]}]
  [:> paper/Button
   (merge
     (when (some? on-press)
       {:on-press on-press})
     (when (some? style)
       {:style style})
     {:mode  (if (some? color) "contained" "outlined")
      :color color})
   label])
