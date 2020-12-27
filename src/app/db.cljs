(ns app.db
  (:require
   ["color" :as color]
   ["faker" :as faker]
   [applied-science.js-interop :as j]
   [clojure.spec.alpha :as s]
   [spec-tools.data-spec :as ds]
   [spec-tools.core :as st]
   [tick.alpha.api :as t]
   [clojure.spec.gen.alpha :as gen]
   ;; needed to `gen/sample` or `gen/generate`
   [clojure.test.check.generators]))

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

(defn chance [p]
  (let [r (rand)]
    (case p
      :low  (-> r (< 0.2))
      :med  (-> r (< 0.51))
      :high (-> r (< 0.90))
      (-> r (< 0.75)))))

(defn start-before-stop [{:session/keys [start stop]}]
  (and (t/instant? start)
       (t/instant? stop)
       (t/< start stop)))

;;
;; independent generator fns
;;

(defn generate-uuid-keyed [n gen-fn]
  (apply merge
         (->> n
              range
              (map #(let [id (random-uuid)]
                      {id (gen-fn id)})))))

(defn generate-time-point []
  (->> (reasonable-date-times)
       (rand-nth)
       (t/instant)))

(defn generate-color []
  (-> faker (j/get :internet) (j/call :color) color))

(defn generate-session []
  (let [time-point     (generate-time-point)
        random-minutes (-> (t/new-duration 1 :hours)
                           (t/minutes)
                           (rand-int))]
    (merge
      #:session {:id          (random-uuid)
                 :start       time-point
                 :stop        (-> time-point
                                  (t/+ (t/new-duration random-minutes :minutes)))
                 :created     time-point
                 :last-edited time-point
                 :type        (if (chance :med)
                                :session/plan :session/track)}
      (when (chance :med)
        #:session {:label (-> faker (j/get :random) (j/call :words))})
      (when (chance :low)
        #:session {:color (-> faker (j/get :internet) (j/call :color) color)}))))

(defn generate-sessions [n]
  (generate-uuid-keyed
    n
    (fn [id] (-> generate-session
                 (merge {:session/id id})))))

(defn generate-tag []
  (merge #:tag {:id (random-uuid)}
         (when (chance :high)
           #:tag {:color (generate-color)})))

(defn generate-tags [n]
  (generate-uuid-keyed
    n
    (fn [id] (-> (generate-tag)
                 (merge {:tag/id id})))))

(defn generate-app-db []
  ;; make tags
  ;; make sessions
  ;; randomly associate sessions / tags
  ;; place sessions on calendar
  nil
  )

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

(s/def ::session (s/with-gen session-data-spec #(gen/fmap generate-session (s/gen ::time-point))))

(s/def ::sessions (s/with-gen
                    (s/and map? (s/every-kv uuid? ::session))
                    #(gen/fmap generate-sessions (s/gen ::reasonable-number))))

(def tag-data-spec
  (ds/spec {:name ::tag-ds
            :spec {:tag/id             uuid?
                   (ds/opt :tag/color) ::color
                   (ds/opt :tag/label) string?}}))

(s/def ::tag (s/with-gen tag-data-spec #(gen/fmap generate-tag (s/gen uuid?))))

(s/def ::tags (s/with-gen
                (s/and map? (s/every-kv uuid? ::tag))
                #(gen/fmap generate-tags (s/gen ::reasonable-number))))

(def app-db-spec
  (ds/spec {:spec {:settings {:theme (s/spec #{:light :dark})}
                   :version  string?
                   ;; TODO figure out how this works, probably need to create a separate spec
                   ;;       :calendar (s/every-kv uuid? {:calendar/sessions [uuid?]})
                   ;; TODO sessions and groups
                   }
            :name ::app-db}))

;;
;; data
;;

(def default-app-db
  {:settings {:theme :dark}
   :version  "version-not-set"

   :calendar
   {#inst "2020-12-21T13:19:25.742-00:00"
    {:calendar/sessions [#uuid "df8788c4-67db-4fb1-980b-b5f1ab5bb4ac"
                         #uuid "afa40006-5e4f-4f8a-b4f9-09886bbbaf36"]}}

   :sessions
   {#uuid "df8788c4-67db-4fb1-980b-b5f1ab5bb4ac"
    #:session {:id          #uuid "df8788c4-67db-4fb1-980b-b5f1ab5bb4ac"
               :start       (t/instant #inst "2020-12-21T13:19:25.742-00:00")
               :stop        (t/instant #inst "2020-12-21T14:20:25.742-00:00")
               :label       "my frist track"
               :color       (color "#ff00ff")                              ;; this is an ovveride
               :tags        [#uuid "26c24deb-c0b5-4b7a-8ef9-8dcf540f80d8"] ;; otherwise the color is derived from mixing colors of tags
               :type        :session/track
               :copied-from #uuid "afa40006-5e4f-4f8a-b4f9-09886bbbaf36"
               }

    #uuid "afa40006-5e4f-4f8a-b4f9-09886bbbaf36"
    #:session {:id    #uuid "afa40006-5e4f-4f8a-b4f9-09886bbbaf36"
               :start (t/instant #inst "2020-12-21T13:19:20.742-00:00")
               :stop  (t/instant #inst "2020-12-21T13:21:25.742-00:00")
               :label "my first plan"
               :tags  [#uuid "26c24deb-c0b5-4b7a-8ef9-8dcf540f80d8"]
               :type  :session/plan}

    :tags
    {#uuid "26c24deb-c0b5-4b7a-8ef9-8dcf540f80d8"
     #:tag  {:id    #uuid "26c24deb-c0b5-4b7a-8ef9-8dcf540f80d8"
             :color (color  "#00ff00")
             :label "my first group"}}

    }
   })

;; (random-uuid)
;; (-> default-app-db (get-in [:sessions #uuid "afa40006-5e4f-4f8a-b4f9-09886bbbaf36"]))
