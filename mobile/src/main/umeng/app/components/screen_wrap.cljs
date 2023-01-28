(ns umeng.app.components.screen-wrap
(:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]

   [umeng.app.tailwind :refer [tw]]))

(defn basic
  "Full screen no status bar"
  [child-view]
  [:> rn/View {:style (tw "h-full")}
   [:> rn/StatusBar {:hidden true}]
   [:> paper/Surface {:style (tw "h-full")}
    [:> rn/SafeAreaView {:style (tw "h-full")}
     [:> rn/StatusBar {:visibility "hidden"}]
     child-view]]])
