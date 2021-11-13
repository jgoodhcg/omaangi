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
   [re-frame.core :refer [subscribe dispatch dispatch-sync]]
   [potpuri.core :as p]))

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

(defn make-session-ish-interval
  [x]
  (let [start (or (:session/start x) (:session-template/start x))
        stop  (or (:session/stop x) (:session-template/stop x))]
    (merge x {:tick/beginning start
              :tick/end       stop})))

(defn touches
  "Works on both sessions and session-templates.
  Merges in `:tick/beginning` and `:tick/end` to make it a valid interval."
  [a b]
  (if (and (some? a)
           (some? b))
    (not (some? (some #{:precedes :preceded-by} ;; checks that these are not the relation
                      [(t/relation (make-session-ish-interval a)
                                   (make-session-ish-interval b))])))
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
                            {:value               nil
                             :mode                nil
                             :session-id          nil
                             :session-template-id nil
                             :field-key           nil
                             :visible             false}] )

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

(defn time-label [instant-or-time]
  (str (prepend-zero (t/hour instant-or-time))
       "-"
       (prepend-zero (t/minute instant-or-time))))

(defn native-event->time
  [{:keys [event zoom]}]
  (-> event
      (j/get :y)
      (/ zoom)
      (int)
      (t/new-duration :minutes)
      (->> (t/+ (t/time "00:00")))))

(defn native-event->type
  [{:keys [event width]}]
  (let [right-side? (-> event
                        (j/get :x)
                        (> (-> width (/ 2))))]
    (if right-side?
      :session/track
      :session/plan)))
