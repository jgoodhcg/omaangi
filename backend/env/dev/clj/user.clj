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
  (t/new-duration 1 :minutes)
  (t/new-duration 100 :seconds)
  (t/new-period 2 :months)

  (random-uuid)

  )

(comment
  (go)
  (reset)
  (require '[xtdb.api :as xt])
  (let [node (-> integrant.repl.state/system :db.xtdb/node)]


    (-> node (xt/submit-tx [[:xtdb.api/put
                               {:xt/id  #uuid "99d46f06-502b-4cc8-8c7d-5b2532b371a3"
                                :type   :exercise
                                :label  "World's perfect stretch"
                                :notes  "<Link to roam page>"
                                :source "<some url to a tiktok>"}]]))

    (-> node (xt/submit-tx [[:xtdb.api/put
                               {:xt/id             #uuid "ec5b2c43-f7be-4603-b601-a9f6b64fd14b"
                                :type              :exercise-log
                                :exercise/id       #uuid "99d46f06-502b-4cc8-8c7d-5b2532b371a3"
                                :timestamp         #inst "2022-10-24T09:20:27.966-00:00" ;; I'm tempted to use valid time instead
                                :duration          #time/duration "PT1M40S"
                                :notes             "Focused on keeping my quads engaged"
                                :relativety-score  :relativety-score/better
                                :exercise-log/data [{:sets 1 :reps 2 :weight 3 :weight-unit "lbs"}
                                                    ;; could also include any of these keys
                                                    ;; semantically only weight and weight-unit would be in either type
                                                    {:distance 12 :distance-unit "miles" :elevation-gain "" :elevation-gain-unit ""}
                                                    ;; this one is specific to inversion table but shows with a schemaless data type I could throw whatever in here
                                                    {:angle 60}]}]]))

    #_(-> node (xt/submit-tx [[:xtdb.api/put {:xt/id "some other stuff" :hello "there"}]]))
    #_(-> node (xt/db) (xt/q '{:find [e] :where [[e :xt/id _]]}))
    #_(-> node (xt/db) (xt/q '{:find [id label] :where [[id :type :exercise]
                                                        [id :label label]]}))
    (-> node (xt/db) (xt/q '{:find [data label] :where [[es-id :type :exercise-log]
                                                        [es-id :exercise/id #uuid "99d46f06-502b-4cc8-8c7d-5b2532b371a3"]
                                                        [es-id :exercise-log/data data]
                                                        [e-id :label label]]}))
    )
  )
