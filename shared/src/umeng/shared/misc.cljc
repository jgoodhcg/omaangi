(ns umeng.shared.misc
  (:require [clojure.string :as str]))

(defn timestamp-for-filename []
  (-> #?(:clj  (java.time.LocalDateTime/now)
         :cljs (js/Date.))
      str
      (str/replace "-" "_")
      (str/replace "T" "__")
      (str/replace ":" "_")
      (str/replace "." "_")))
