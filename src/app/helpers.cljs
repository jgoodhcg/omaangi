(ns app.helpers
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]
   ["color" :as color]
   ["react-native-gesture-handler" :as g]
   [clojure.string :refer [join split]]
   [applied-science.js-interop :as j]
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [tick.alpha.api :as t]
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]))

(def <sub (comp deref subscribe))

(def >evt dispatch)

(def >evt-sync dispatch-sync)

;; TODO @deprecated
(defn style-sheet [s]
  ^js (-> s
          (#(cske/transform-keys csk/->camelCase %))
          (clj->js)
          (rn/StyleSheet.create)))

(defn get-theme [k] (j/get paper k))

(defn interval? [x] (and (contains? x :tick/beginning)
                         (contains? x :tick/end)))

(defn make-session-interval [x]
  (if (interval? x)
    x ;; just return it if it is an interval
    ;; otherwise make it a valid tick interval
    (let [{:session/keys [start stop]} x]
      (merge x {:tick/beginning start
                :tick/end       stop}))))

(defn touches [a b]
  (if (and (some? a)
           (some? b))
    (not (some? (some #{:precedes :preceded-by} ;; checks that these are not the relation
                      [(t/relation (make-session-interval a)
                                   (make-session-interval b))])))
    false))

(defn chance [p]
  (let [r (rand)]
    (case p
      :low  (-> r (< 0.25))
      :med  (-> r (< 0.51))
      :high (-> r (< 0.90))
      (-> r (< 0.75)))))

(defn prepend-zero [n]
  (if (= 1 (count (str n)))
    (str 0 n)
    (str n)))

(def clear-datetime-picker [:set-date-time-picker
                            #:date-time-picker
                            {:value      nil
                             :mode       nil
                             :session-id nil
                             :field-key  nil
                             :visible    false}] )

(defn drop-keyword-sections [n k]
  (-> k
      str
      (split ".")
      (->> (drop n))
      join
      keyword))

(defn hex-if-some
  [color-object]
  (when (some? color-object)
    (-> color-object (j/call :hex))))

(defn make-color-if-some
  [hex]
  (when (some? hex)
    (-> hex color)))

(defn is-color?
  [c]
  (if (some? c)
    (j/contains? c :color)
    false))

(defn active-gesture? [evt]
  (-> evt
      (j/get :nativeEvent)
      (j/get :state)
      (= (j/get g/State :ACTIVE))))

(defn time-label [instant]
  (str (prepend-zero (t/hour instant))
       "-"
       (prepend-zero (t/minute instant))))
