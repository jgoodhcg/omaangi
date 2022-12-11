(ns umeng.tooling.bb.test-graph-api
  (:require [babashka.curl :as curl]
            [babashka.process :refer [process]]
            [cognitect.transit :as transit]
            [clojure.pprint :refer [pprint]]
            [clojure.edn :as edn]))

(deftype FakeInstant [i])
(defmethod print-method FakeInstant [i writer]
  (doto writer
    (.write "#time/instant ")
    (.write i)))
(set! *data-readers* {'time/instant #(FakeInstant. %)})
(def fake-instant-write-handler (transit/write-handler "time/instant" #(FakeInstant. %)))

(let [[supa-url
       supa-anon-key ;; get by running supabase status
       email
       password
       signup]               *command-line-args*
      command                (str "npx nbb supabase_access_token.cljs"
                                  " \"" supa-url "\""
                                  " \"" supa-anon-key "\""
                                  " \"" email "\""
                                  " \"" password "\""
                                  (when (some? signup) " signup"))
      {:keys [out
              err]}          (-> (process command {:dir "../nbb"} ))
      {:keys [access-token]} (-> out slurp edn/read-string)
      error                  (-> err slurp)
      req-body               [`(umeng.backend.pathom.core/upsert-items
                                {:umeng/items
                                 [{:xt/id      #uuid "5bf4dbb9-5931-4f7c-9095-48e6477bdefc"
                                   :umeng/type :exercise :exercise/label "Pushup" :exercise/notes "upsert-works"}
                                  {:iam :invalid}
                                  {:xt/id      #uuid "b53f9f86-6600-4775-9aa7-f5af13142822"
                                   :umeng/type :exercise-session
                                   :exercise-session.interval/beginning
                                   #time/instant "2022-12-10T23:34:30.076172Z"
                                   :exercise-session.interval/end
                                   #time/instant "2022-12-10T23:34:56.217921Z"}
                                  {:xt/id               #uuid "c15b9704-426b-4196-b82d-286c629e46e1"
                                   :umeng/type          :exercise-log
                                   :exercise-session/id #uuid "b53f9f86-6600-4775-9aa7-f5af13142822"}]})]
      ;;'[{(:>/test {:test-resolver/input "yo"})
      ;;      [:test-resolver/output]}]
      out                    (java.io.ByteArrayOutputStream. 4096)
      writer                 (transit/writer
                              out
                              :json
                              {:handlers fake})
      _                      (transit/write writer {:eql req-body})
      payload                (.toString out)
      _                      (pprint {:request-body req-body
                                      :payload      payload})
      {:keys [body]
       :as   response}       (-> "http://localhost:3000/api/graph"
                                 (curl/post
                                  {:body payload
                                   :headers
                                   {"Accept"        "application/transit+json"
                                    "Content-Type"  "application/transit+json"
                                    "Authorization" (str "Bearer " access-token)}}))
      response-body          body
      in                     (java.io.ByteArrayInputStream.
                              (bytes (byte-array
                                      (map (comp byte int) response-body))))
      reader                 (transit/reader in :json)
      body-decoded           (transit/read reader)]

  (when (some? error) (println error))

  (pprint {:response-body body-decoded}))
