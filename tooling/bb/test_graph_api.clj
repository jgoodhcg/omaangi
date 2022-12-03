(ns umeng.tooling.bb.test-graph-api
  (:require [babashka.curl :as curl]
            [babashka.process :refer [process]]
            [cognitect.transit :as transit]
            [clojure.pprint :refer [pprint]]
            [clojure.edn :as edn]))

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
      req-body               '[{(:>/test {:test-resolver/input "yo"})
                                [:test-resolver/output]}]
      out                    (java.io.ByteArrayOutputStream. 4096)
      writer                 (transit/writer
                              out
                              :json)
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
