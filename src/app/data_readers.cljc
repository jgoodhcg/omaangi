(ns app.data-readers
  (:require #?(:cljs ["color" :as color])))

(defn read-color [c]
  #?(:cljs (list 'color c)))
