(ns umeng.backend.web.controllers.graph
  (:require [ring.util.http-response :as http-response]
            [com.wsscode.pathom3.interface.eql :as p.eql]
            [umeng.backend.pathom.core :refer [indexes]]))

(defn api [{:keys [body-params]}]
  (http-response/ok {:eql (p.eql/process indexes {} (:eql body-params))}))
