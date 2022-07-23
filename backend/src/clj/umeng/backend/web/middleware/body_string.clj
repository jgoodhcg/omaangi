(ns umeng.backend.web.middleware.body-string
  (:require [ring.util.request :refer [body-string]]))

(defn wrap-body-string [handler]
  (fn [request]
    (let [body-str (body-string request)]
      (handler (assoc request :body (java.io.StringReader. body-str))))))
