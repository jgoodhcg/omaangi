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
   [com.rpl.specter :as sp :refer [transform]]
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
  ;; TODO add question mark
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

(defn combine-tag-labels
  [tags]
  (->> tags
       (mapv :tag/label)
       (join " ")))

(defn sessions->min-col
  [sessions]
  (->> sessions
       (mapv #(-> (t/between
                    (:session/start %)
                    (:session/stop %))
                  (t/minutes)))))

(defn collides?
  "Works on both sessions and session-templates.
  Merges in `:tick/beginning` and `:tick/end` to make it a valid interval."
  [a b]
  (if (and (some? a)
           (some? b))
    (not (some? (some #{:precedes :preceded-by :meets :met-by} ;; checks that these are not the relation
                      [(t/relation (make-session-ish-interval a)
                                   (make-session-ish-interval b))])))
    false))

(defn combinations
  "Yanked from https://stackoverflow.com/a/6450455"
  [n coll]
  (if (= 1 n)
    (map list coll)
    (lazy-seq
      (when-let [[head & tail] (seq coll)]
        (concat (for [x (combinations (dec n) tail)]
                  (cons head x))
                (combinations n tail))))))

(defn tmp-session
  [start stop tags]
  {:session/start start
   :session/stop  stop
   :session/tags  tags
   :session/id    (random-uuid)
   :session/tmp   true})

(defn smoosh-sessions
  [sessions]
  (->> sessions
       (combinations 2)
       (mapv
         (fn [[{a-start :session/start
                a-stop  :session/stop
                a-tags  :session/tags
                :as     a}
               {b-start :session/start
                b-stop  :session/stop
                b-tags  :session/tags
                :as     b}]]
           (let [combined-tags (vec (concat a-tags b-tags))]
             (case (t/relation
                     (make-session-ish-interval a)
                     (make-session-ish-interval b))
               :overlaps     [(tmp-session a-start b-start a-tags)
                              (tmp-session b-start a-stop combined-tags)
                              (tmp-session a-stop b-stop b-tags)]
               :overlaped-by [(tmp-session b-start a-start b-tags)
                              (tmp-session a-start b-stop combined-tags)
                              (tmp-session b-stop a-stop a-tags)]
               :starts       [(tmp-session a-start a-stop combined-tags)
                              (tmp-session a-stop b-stop b-tags)]
               :started-by   [(tmp-session b-start b-stop combined-tags)
                              (tmp-session b-stop a-stop a-tags)]
               :during       [(tmp-session b-start a-start b-tags)
                              (tmp-session a-start a-stop combined-tags)
                              (tmp-session a-stop b-stop b-tags)]
               :contains     [(tmp-session a-start b-start a-tags)
                              (tmp-session b-start b-stop combined-tags)
                              (tmp-session b-stop a-stop a-tags)]
               :finishes     [(tmp-session b-start a-start b-tags)
                              (tmp-session a-start b-stop combined-tags)]
               :finished-by  [(tmp-session a-start b-start a-tags)
                              (tmp-session b-start a-stop combined-tags)]
               ;; else
               [a b]))))
       (flatten)
       (distinct)
       (vec)))

(comment
  (time
    (->> [
          {:session/label "A" :session/tags [:a] :session/id #uuid "27cab9e7-df48-4218-b227-2ea382939b1d", :session/start #time/instant "2021-12-20T17:31:00Z", :session/stop #time/instant "2021-12-20T19:23:00Z", :session/created #time/instant "2021-12-20T17:31:00Z", :session/last-edited #time/instant "2021-12-20T19:23:00Z", :session/type :session/plan}
          {:session/label "B" :session/tags [:b :c] :session/id #uuid "f8671edc-54cd-474b-b581-73231c963caf", :session/start #time/instant "2021-12-18T11:09:00Z", :session/stop #time/instant "2021-12-18T23:47:00Z", :session/created #time/instant "2021-12-18T11:09:00Z", :session/last-edited #time/instant "2021-12-18T12:47:00Z", :session/type :session/track}
          {:session/label "C" :session/tags [:d] :session/id #uuid "425e67f1-b47e-4589-b958-23a51235eb57", :session/start #time/instant "2021-12-18T18:46:00Z", :session/stop #time/instant "2021-12-18T21:43:00Z", :session/created #time/instant "2021-12-18T18:46:00Z", :session/last-edited #time/instant "2021-12-18T21:43:00Z", :session/type :session/track}
          ]
         (smoosh-sessions)
         ;; (transform [sp/ALL] :session/tags)
         ))
  )
