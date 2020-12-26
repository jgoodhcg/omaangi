(ns app.db
  (:require
   ["color" :as color]
   [applied-science.js-interop :as j]
   [clojure.spec.alpha :as s]
   [spec-tools.data-spec :as ds]
   [spec-tools.core :as st]
   [tick.alpha.api :as t]
   ))

;; TODO do I need this? (s/def ::uuid-indexed (s/and map? (s/every-kv uuid? some?)))

;; TODO port over period spec for sessions

(def hour-ms
  (->> 1
       (* 60)
       (* 60)
       (* 1000)))

;; (def time-range
;;   (range (.valueOf (-> (js/Date.)))
;;          (.valueOf (end-of-today (make-date) (get-default-timezone)))
;;          hour-ms))

;; (def time-set
;;   (set (->> time-range
;;             (map #(new js/Date %)))))

;; (s/def ::time-point (s/with-gen inst? #(s/gen time-set)))

(defn start-before-stop [{:session/keys [start stop]}]
  (if (and (some? start) (some? stop))
    (> (.valueOf stop) (.valueOf start))
    ;; Passes if it has no time stamps
    true))

(defn generate-session [time-point]
  (let [type-chance (> 0.5 (rand))
        start       (.valueOf time-point)
        stop        (->> start (+ (rand-int (* 2 hour-ms))))
        stamps      {:start (new js/Date start)
                     :stop  (new js/Date stop)}
        type        (if type-chance :session/plan :session/track)]

    (merge stamps
           {:id          (random-uuid)
            :created     time-point
            :last-edited time-point
            :label       "I'm auto generated!"
            :type        type})))

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
