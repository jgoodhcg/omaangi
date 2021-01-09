(ns app.helpers
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]
   [applied-science.js-interop :as j]
   [re-frame.core :refer [subscribe dispatch]]
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]))

(def <sub (comp deref subscribe))

(def >evt dispatch)

;; TODO @deprecated
(defn style-sheet [s]
  ^js (-> s
          (#(cske/transform-keys csk/->camelCase %))
          (clj->js)
          (rn/StyleSheet.create)))

(defn get-theme [k] (j/get paper k))
