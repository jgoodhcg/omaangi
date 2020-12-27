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
   [clojure.test.check.generators]
   ))

;; TODO do I need this? (s/def ::uuid-indexed (s/and map? (s/every-kv uuid? some?)))

(defn generate-time-point [_]
  (->> (t/range (-> (t/yesterday) (t/bounds) (t/beginning))
                (-> (t/tomorrow) (t/bounds) (t/end))
                (t/new-duration 1 :minutes))
       (rand-nth)
       (t/instant)))

(s/def ::time-point (s/with-gen t/instant? #(gen/fmap generate-time-point (s/gen int?))))

;; (gen/generate (s/gen ::time-point))

(defn start-before-stop [{:session/keys [start stop]}]
  (and (t/instant? start)
       (t/instant? stop)
       (t/< start stop)))

(defn generate-session [time-point]
  (let [random-minutes (-> (t/new-duration 1 :hours)
                           (t/minutes)
                           (rand-int))]
    (merge
      #:session {:id          (random-uuid)
                 :start       time-point
                 :stop        (-> time-point
                                  (t/+ (t/new-duration random-minutes :minutes)))
                 :created     time-point
                 :last-edited time-point
                 :type        (if (> 0.5 (rand))
                                :session/plan :session/track)}
      (when (> 0.1 (rand))
        #:session {:label (-> faker (j/get :random) (j/call :words))})
      (when (> 0.7 (rand))
        #:session {:color (-> faker (j/get :internet) (j/call :color) color)}))))

(s/def ::color (s/with-gen #(j/contains? % :color)
                 #(gen/fmap
                    (fn [_]
                      (-> faker (j/get :internet) (j/call :color) color))
                    (s/gen int?))))

(def session-data-spec
  (ds/spec {:name ::session-ds
            :spec {(ds/req :session/id)          uuid?
                   (ds/req :session/created)     ::time-point
                   (ds/req :session/last-edited) ::time-point
                   (ds/req :session/start)       ::time-point
                   (ds/req :session/stop)        ::time-point
                   (ds/req :session/type)        (s/spec #{:session/track :session/plan})
                   (ds/opt :session/label)       string?
                   (ds/opt :session/color)       ::color}}))

(s/def ::session (s/with-gen session-data-spec #(gen/fmap generate-session (s/gen ::time-point))))

(s/def ::reasonable-number (s/int-in 1 20))

;; (->> (gen/sample (s/gen ::session)) (map :session/label))
(defn generate-sessions [n]
  (apply merge (->> n
                    range
                    (map #(let [id (random-uuid)]
                            {id (-> (generate-time-point nil)
                                    generate-session
                                    (merge {:session/id id}))})))))

(s/def ::sessions (s/with-gen
                    (s/and map? (s/every-kv uuid? ::session))
                    #(gen/fmap generate-sessions (s/gen ::reasonable-number))))

;; (->> (gen/generate (s/gen ::sessions))
;;      vals
;;      (map :session/label))

;; TODO make a tag spec

(def app-db-spec
  (ds/spec {:spec {:settings {:theme (s/spec #{:light :dark})}
                   :version  string?
                   ;; TODO figure out how this works, probably need to create a separate spec
                   ;;       :calendar (s/every-kv uuid? {:calendar/sessions [uuid?]})
                   ;; TODO sessions and groups
                   }
            :name ::app-db}))

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
     #:tag  {:color (color  "#00ff00")
             :label "my first group"}}

    }
   })

;; (random-uuid)
;; (-> default-app-db (get-in [:sessions #uuid "afa40006-5e4f-4f8a-b4f9-09886bbbaf36"]))
