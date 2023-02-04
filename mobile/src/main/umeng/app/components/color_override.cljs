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
  ;; Apparently all session-ish items have a color by default ...
  ;; Maybe I should indicate that a default color has been supplied so this button can be displayed with flat or outline mode
  (let [mode  (if (or (some? session-ish-color)
                       color-override) "contained" "flat")
        label (if (some? session-ish-color) session-ish-color "set color")]

    [:> rn/View {:style (tw "flex flex-col mb-8")}
     [:> paper/Button {:mode        mode
                       :icon        "palette"
                       :buttonColor session-ish-color
                       :dark        (-> session-ish-color color (j/call :isDark))
                       :on-press    #(>evt [:set-color-picker
                                            #:color-picker {:visible true
                                                            :value   session-ish-color}])}
      label]

     [color-picker/component {:input-color session-ish-color
                              :update-fn   update-fn
                              :remove-fn   remove-fn}]]))
