(ns umeng.app.components.color-override
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]
   ["color" :as color]

   [applied-science.js-interop :as j]

   [umeng.app.components.color-picker :as color-picker]
   [umeng.app.misc :refer [>evt]]
   [umeng.app.tailwind :refer [tw]]
   ))

(defn component [{session-ish-color :color
                  color-override    :color-override
                  update-fn         :update-fn
                  remove-fn         :remove-fn}]
  (let [mode  (if (and (some? session-ish-color)
                       color-override) "contained" "flat")
        label (if (some? session-ish-color) session-ish-color "set color")]

    [:> rn/View {:style (tw "flex flex-col mb-8")}
     [:> paper/Button {:mode        mode
                       :icon        "palette"
                       :dark        (-> session-ish-color color (j/call :isDark))
                       :buttonColor session-ish-color
                       :on-press    #(>evt [:set-color-picker
                                            #:color-picker {:visible true
                                                            :value   session-ish-color}])}
      label]

     [color-picker/component {:input-color session-ish-color
                              :update-fn   update-fn
                              :remove-fn   remove-fn}]]))
