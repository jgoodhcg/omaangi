(ns app.helpers
  (:require [re-frame.core :refer [subscribe dispatch]]
            ["react-native" :as rn]
            [camel-snake-kebab.core :as csk]
            [camel-snake-kebab.extras :as cske]))

(def <sub (comp deref subscribe))

(def >evt dispatch)

(defn style-sheet [s]
  ^js (-> s
          (#(cske/transform-keys csk/->camelCase %))
          (clj->js)
          (rn/StyleSheet.create)))
