(ns app.components.menu
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]
   [reagent.core :as r]))

(defn button [{:keys [button-color toggle-menu]}]
  [:> paper/IconButton {:icon     "menu"
                        :color    button-color
                        :size     20
                        :on-press toggle-menu}])
