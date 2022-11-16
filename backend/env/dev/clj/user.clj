(ns user
  "Userspace functions you can run by default in your local REPL."
  (:require
    [clojure.pprint]
    [clojure.spec.alpha :as s]
    [clojure.tools.namespace.repl :as repl]
    [criterium.core :as c]                                  ;; benchmarking
    [expound.alpha :as expound]
    [integrant.core :as ig]
    [integrant.repl :refer [clear go halt prep init reset reset-all]]
    [integrant.repl.state :as state]
    [kit.api :as kit]
    [lambdaisland.classpath.watch-deps :as watch-deps]      ;; hot loading for deps
    [umeng.backend.core :refer [start-app]]
    [tick.core :as t]
    [tick.alpha.interval :as t.i]
    [clj-jwt.core :refer [str->jwt verify]]
    ))

;; uncomment to enable hot loading for deps
(watch-deps/start! {:aliases [:dev :test]})

(alter-var-root #'s/*explain-out* (constantly expound/printer))

(add-tap (bound-fn* clojure.pprint/pprint))

(defn dev-prep!
  []
  (integrant.repl/set-prep! (fn []
                              (-> (umeng.backend.config/system-config {:profile :dev})
                                  (ig/prep)))))

(defn test-prep!
  []
  (integrant.repl/set-prep! (fn []
                              (-> (umeng.backend.config/system-config {:profile :test})
                                  (ig/prep)))))

;; Can change this to test-prep! if want to run tests as the test profile in your repl
;; You can run tests in the dev profile, too, but there are some differences between
;; the two profiles.
(dev-prep!)

(repl/set-refresh-dirs "src/clj")

(def refresh repl/refresh)

(comment
  (go)
  (reset)
  (require '[xtdb.api :as xt])
  (let [node (-> integrant.repl.state/system :db.xtdb/node)]
    (-> node (xt/submit-tx [[:xtdb.api/put {:xt/id :hello-4 :a "there"}]]))
    (-> node (xt/submit-tx [[:xtdb.api/put {:xt/id       (.toString (java.util.UUID/randomUUID))
                                            :healthcheck true}]]))
    (-> node xt/status)
    (-> node xt/recent-queries)
    (-> node xt/latest-submitted-tx)
    (-> node (xt/db))
    (-> node (xt/db) (xt/q '{:find [e] :where [[e :xt/id _]]}))
    )
  )

(comment
  (require '[tick.core :as t])
  (require '[tick.alpha.interval :as t.i])
  (t/new-duration 1 :minutes)
  (t/new-duration 100 :seconds)
  (t/new-period 2 :months)

  (random-uuid)

  (t.i/new-interval
   (t/now)
   (t/>> (t/now) (t/new-duration 5 :minutes)))
  ;; => #:tick{:beginning #time/instant "2022-10-28T11:30:34.550786Z", :end #time/instant "2022-10-28T11:35:34.550809Z"}

  (t/now) ;; => #time/instant "2022-10-28T11:35:13.631707Z"

  )

(comment
  (go)
  (reset)
  (require '[xtdb.api :as xt])
  (let [node (-> integrant.repl.state/system :db.xtdb/node)]


    (-> node (xt/submit-tx [[:xtdb.api/put
                             {:xt/id           #uuid "99d46f06-502b-4cc8-8c7d-5b2532b371a3"
                              :umeng/type      :exercise
                              :exercise/label  "World's perfect stretch"
                              :exercise/notes  "<Link to roam page>"
                              :exercise/source "<some url to a tiktok>"}]]))

    (-> node
        (xt/submit-tx
         [[:xtdb.api/put
           {:xt/id                         #uuid "ec5b2c43-f7be-4603-b601-a9f6b64fd14b"
            :umeng/type                    :exercise-log
            :exercise/id                   #uuid "99d46f06-502b-4cc8-8c7d-5b2532b371a3"
            :exercise-log/interval         {:tick/beginning #time/instant "2022-10-28T11:35:13.631707Z"
                                            :tick/end       #time/instant "2022-10-28T11:40:11.520342Z"}
            :exercise-log/notes            "Focused on keeping my quads engaged"
            :exercise-log/relativety-score :relativety-score/better
            :exercise-log/data             [{:sets        1
                                             :reps        2
                                             :weight      3
                                             :weight-unit "lbs"
                                             :interval    {:tick/beginning #time/instant "2022-10-28T11:36:13.631707Z"
                                                           :tick/end       #time/instant "2022-10-28T11:38:11.520342Z"}}
                                            ;; could also include any of these keys
                                            ;; semantically only weight and weight-unit would be in either type
                                            {:distance 12 :distance-unit "miles" :elevation-gain "" :elevation-gain-unit ""}
                                            ;; this one is specific to inversion table but shows with a schemaless data type I could throw whatever in here
                                            {:inversion-angle 60}]}]]))

    #_(-> node (xt/submit-tx [[:xtdb.api/put {:xt/id "some other stuff" :hello "there"}]]))
    #_(-> node (xt/db) (xt/q '{:find [e] :where [[e :xt/id _]]}))
    #_(-> node (xt/db) (xt/q '{:find [id label] :where [[id :type :exercise]
                                                        [id :label label]]}))
    (-> node (xt/db) (xt/q '{:find  [data label]
                             :where [[es-id :umeng/type :exercise-log]
                                     [es-id :exercise/id #uuid "99d46f06-502b-4cc8-8c7d-5b2532b371a3"]
                                     [es-id :exercise-log/data data]
                                     [e-id :exercise/label label]]}))
    (-> node (xt/db) (xt/q '{:find  [(pull ?ex-log [:exercise-log/data :exercise-log/interval])
                                     (pull ?ex [:exercise/label])]
                             :where [[?ex-log :umeng/type :exercise-log]
                                     [?ex-log :exercise/id #uuid "99d46f06-502b-4cc8-8c7d-5b2532b371a3"]
                                     [?ex :exercise/label label]]}))
    )

  )

(comment
  (def token "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJhdXRoZW50aWNhdGVkIiwiZXhwIjoxNjY3MjI3ODI3LCJzdWIiOiJhZGM1NzlhMy05OGE4LTQ3NDctYTE4Zi0xY2NlODQ4NTczOGMiLCJlbWFpbCI6Impnb29kaGNnK2xvY2FsdGVzdEBnbWFpbC5jb20iLCJwaG9uZSI6IiIsImFwcF9tZXRhZGF0YSI6eyJwcm92aWRlciI6ImVtYWlsIiwicHJvdmlkZXJzIjpbImVtYWlsIl19LCJ1c2VyX21ldGFkYXRhIjp7fSwicm9sZSI6ImF1dGhlbnRpY2F0ZWQifQ.0xmlLYRNL3aAoQ06ADXebT7zFoZll7hJj73DpDF33Qs"
    )

  (def key "super-secret-jwt-token-with-at-least-32-characters-long")

  (-> token str->jwt (verify key)) ;; does not take into account :exp

  (-> token str->jwt :claims :exp (t/new-duration :seconds) t/instant (t/> (t/now)))
  )
