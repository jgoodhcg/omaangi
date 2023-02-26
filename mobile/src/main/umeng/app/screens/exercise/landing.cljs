(ns umeng.app.screens.exercise.landing
  (:require
   ["color" :as color]
   ["react" :as react]
   ["react-native" :as rn]
   ["react-native-gesture-handler" :as g]
   ["react-native-modal-datetime-picker" :default DateTimePicker]
   ["react-native-paper" :as paper]

   [applied-science.js-interop :as j]
   [reagent.core :as r]

   [umeng.app.components.menu :as menu]
   [umeng.app.components.time-indicators :as time-indicators]
   [umeng.app.components.screen-wrap :as screen-wrap]
   [umeng.app.components.session-ishes :as session-ishes]
   [umeng.app.misc :refer [<sub
                           >evt
                           get-theme
                           clear-datetime-picker
                           active-gesture?]]
   [umeng.app.screens.core :refer [screens]]
   [umeng.app.tailwind :refer [tw]]
   [tick.core :as t]))

(defn screen [props]
  (let []
     [screen-wrap/basic
      [:> rn/View {:style (tw "h-full")}
       [:> paper/Text {:variant "displayLarge"} "Exercise Landing"]]]))
 