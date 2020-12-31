(ns app.helpers
  (:require
   ["react-native" :as rn]
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
