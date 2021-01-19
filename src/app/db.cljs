(ns app.db
  (:require
   ["color" :as color]
   ["faker" :as faker]
   [applied-science.js-interop :as j]
   [com.rpl.specter :as sp :refer [select select-one setval transform selected-any?]]
   [clojure.spec.alpha :as s]
   [spec-tools.data-spec :as ds]
   [spec-tools.core :as st]
   [tick.alpha.api :as t]
   [clojure.spec.gen.alpha :as gen]
   ;; needed to `gen/sample` or `gen/generate`
   [clojure.test.check.generators]

   [app.colors :refer [material-500-hexes]]
   [app.helpers :refer [touches chance]]))

;;
;; misc
;;

(def reasonable-date-times
  (memoize
    ;; generating this range repeatedly is costly so it's _cached_ with memoize
    ;; not sure if this is actually useful or not
    (fn []
      (t/range (-> (t/yesterday) (t/bounds) (t/beginning))
               (-> (t/tomorrow) (t/bounds) (t/end))
               (t/new-duration 1 :minutes)))))

(def arbitrary-date-times
  (memoize
    ;; generating this range repeatedly is costly so it's _cached_ with memoize
    ;; not sure if this is actually useful or not
    (fn [d]
      (t/range (-> d (t/bounds) (t/beginning))
               (-> d (t/bounds) (t/end))
               (t/new-duration 1 :minutes)))))

(defn start-before-stop [{:session/keys [start stop]}]
  (and (t/instant? start)
       (t/instant? stop)
       (t/< start stop)))

;;
;; independent generators
;;

(defn generate-time-point []
  (->> (reasonable-date-times)
       (rand-nth)
       (t/instant)))

(defn generate-color []
  ;; (-> faker (j/get :internet) (j/call :color) color)
  (-> material-500-hexes rand-nth color))

(defn generate-session
  "By default will make a session that is contained within a day.
  The `:day` option allows you to choose the day.
  When `:within` option is set to _false_ then there is a chance for `:session/start` and/or `:session/stop` to be on the prev or next day respectively."
  ([] (generate-session {:day    (t/date (generate-time-point))
                         :within true}))
  ([{:keys [day within] :or {day    (t/date (generate-time-point))
                             within true}}]
   (let [time-point     (-> day arbitrary-date-times rand-nth t/instant)
         random-minutes (-> (t/new-duration 4 :hours)
                            (t/minutes)
                            (rand-int)
                            (t/new-duration :minutes))
         start          (if within
                          time-point
                          (t/- time-point random-minutes))
         stop           (if within
                          (-> time-point
                              (t/+ random-minutes)
                              (t/min (-> day (t/bounds) (t/end) (t/instant))))
                          (-> time-point
                              (t/+ random-minutes)))]
     (merge
       #:session {:id          (random-uuid)
                  :start       start
                  :stop        stop
                  :created     start
                  :last-edited stop
                  :type        (if (chance :med)
                                 :session/plan :session/track)}
       (when (chance :med)
         #:session {:label (-> faker (j/get :random) (j/call :words))})
       (when (chance :low)
         #:session {:color (-> faker (j/get :internet) (j/call :color) color)})))))

(comment (-> {:day    (t/date (generate-time-point))
              :within false}
             generate-session
             (select-keys [:session/start :session/stop])))

(defn generate-tag []
  (merge #:tag {:id (random-uuid)}
         (when (chance :high)
           #:tag {:color (generate-color)})))

(defn generate-calendar-val []
  {:calendar/sessions []
   :calendar/date     (t/date (generate-time-point))})

;;
;; coll generators
;;

(defn generate-indexed [n index-fn gen-fn]
  (apply merge
         (->> n
              range
              (map #(let [index (index-fn)]
                      {index (gen-fn index)})))))

(defn generate-sessions [n]
  (generate-indexed
    n
    random-uuid
    (fn [id] (-> (generate-session)
                 (merge {:session/id id})))))

(defn generate-tags [n]
  (generate-indexed
    n
    random-uuid
    (fn [id] (-> (generate-tag)
                 (merge {:tag/id id})))))

(defn generate-calendar [n]
  (generate-indexed
    n
    (fn [] (t/date (generate-time-point)))
    (fn [d] {:calendar/sessions []
             :calendar/date     (t/date d)})))

;;
;; relational generators
;;

(defn generate-calendar-tag-sessions []
  ;; TODO specmonstah would make this so much cleaner
  (let [tags     (generate-tags 2)
        sessions (generate-sessions 70)

        sessions-with-tags
        (->> sessions
             (transform
               [sp/MAP-VALS]
               (fn [session]
                 (merge session
                        {:session/tags
                         (->> tags
                              (random-sample 0.5)
                              (select [sp/MAP-VALS :tag/id]))}))))

        days
        (->> sessions
             (select [sp/MAP-VALS
                      (sp/transformed
                        []
                        (fn [{:session/keys [start stop]}]
                          [(t/date start) (t/date stop)]))])
             flatten
             set
             seq)

        empty-calendar
        (->> days count range
             (map #(hash-map))
             (interleave days)
             (apply hash-map))

        calendar
        (->> empty-calendar
             (transform
               [sp/ALL]
               (fn [[day obj]]
                 [day (merge
                        obj
                        {:calendar/date day
                         :calendar/sessions
                         (->> sessions-with-tags
                              (select [sp/MAP-VALS
                                       (fn [{:session/keys [start stop]}]
                                         (touches (->> (t/bounds day)
                                                       (transform [sp/MAP-VALS] t/instant))
                                                  {:tick/beginning start
                                                   :tick/end       stop}))
                                       :session/id]))})])))]

    {:calendar calendar
     :sessions sessions-with-tags
     :tags     tags}))

;;
;; specs with gen
;;

(s/def ::reasonable-number (s/int-in 1 20))

(s/def ::time-point (s/with-gen t/instant? #(gen/fmap generate-time-point (s/gen int?))))

(s/def ::color (s/with-gen #(j/contains? % :color)
                 #(gen/fmap
                    generate-color
                    (s/gen int?))))

(def session-data-spec
  (ds/spec {:name ::session-ds
            :spec {:session/id             uuid?
                   :session/created        ::time-point
                   :session/last-edited    ::time-point
                   :session/start          ::time-point
                   :session/stop           ::time-point
                   :session/type           (s/spec #{:session/track :session/plan})
                   (ds/opt :session/tags)  [uuid?]
                   (ds/opt :session/label) string?
                   (ds/opt :session/color) ::color}}))

(s/def ::session (s/with-gen session-data-spec #(gen/fmap generate-session (s/gen int?))))

(s/def ::sessions (s/with-gen
                    (s/and map? (s/every-kv uuid? ::session))
                    #(gen/fmap generate-sessions (s/gen ::reasonable-number))))

(def tag-data-spec
  (ds/spec {:name ::tag-ds
            :spec {:tag/id             uuid?
                   (ds/opt :tag/color) ::color
                   (ds/opt :tag/label) string?}}))

(s/def ::tag (s/with-gen tag-data-spec #(gen/fmap generate-tag (s/gen int?))))

(s/def ::tags (s/with-gen
                (s/and map? (s/every-kv uuid? ::tag))
                #(gen/fmap generate-tags (s/gen ::reasonable-number))))

(def calendar-val-data-spec
  (ds/spec {:name ::calendar-ds
            :spec {:calendar/date     t/date?
                   :calendar/sessions [uuid?]}}))

(s/def ::calendar-val (s/with-gen calendar-val-data-spec #(gen/fmap generate-calendar-val (s/gen int?))))

(s/def ::calendar (s/with-gen
                    (s/and map? (s/every-kv t/date? ::calendar-val))
                    #(gen/fmap generate-calendar (s/gen ::reasonable-number))))

(s/def ::zoom (s/with-gen
                (s/and float? pos?)
                #(gen/fmap (fn [n] (* n 0.1)) (s/gen ::reasonable-number))))

(def app-db-spec
  (ds/spec {:name ::app-db
            :spec {:settings {:theme (s/spec #{:light :dark})}
                   :version  string?
                   :tracking [uuid?]
                   :calendar ::calendar
                   :sessions ::sessions
                   :tags     ::tags
                   :view     {:view/selected-day     t/date?
                              :view/zoom             ::zoom
                              :view/tag-remove-modal {:tag-remove-modal/id      (ds/maybe uuid?)
                                                      :tag-remove-modal/visible boolean?
                                                      :tag-remove-modal/label   (ds/maybe string?)}
                              :view/tag-add-modal    {:tag-add-modal/visible boolean?}}}}))

(comment
  (s/explain app-db-spec (merge {:settings {:theme :dark}
                                 :version  "version-not-set"}
                                (generate-calendar-tag-sessions))))

(comment (gen/generate (s/gen app-db-spec)))

;;
;; data
;;

(def default-app-db
  (let [cal-tag-sessions (generate-calendar-tag-sessions)]
    (merge
      cal-tag-sessions
      {:settings {:theme :dark}
       :version  "version-not-set"
       :tracking []
       :view     {:view/selected-day     (->> cal-tag-sessions
                                              :calendar
                                              keys
                                              rand-nth)
                  :view/zoom             1.25
                  :view/tag-remove-modal {:tag-remove-modal/id      nil
                                          :tag-remove-modal/visible false
                                          :tag-remove-modal/label   nil}
                  :view/tag-add-modal    {:tag-add-modal/visible false}}})))
