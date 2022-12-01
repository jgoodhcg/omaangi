(ns umeng.backend.web.routes.api
  (:require
    [umeng.backend.web.controllers.health :as health]
    [umeng.backend.web.controllers.graph :as graph]
    [umeng.backend.web.middleware.exception :as exception]
    [umeng.backend.web.middleware.formats :as formats]
    [umeng.backend.web.middleware.body-string :as body-string]
    [integrant.core :as ig]
    [reitit.coercion.malli :as malli]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.parameters :as parameters]
    [reitit.swagger :as swagger]
    [ring.middleware.jwt :as jwt]))

;; TODO move this to middleware namespace
#_(defn access-token [handler secret]
  (fn [{:keys [headers] :as request}]
    ;; get access token
    ;; validate
    ;; check exp
    ;; take out claims and put in request
    ;; call handlers
    (let [access-token ()])
    ))

;; Routes
(defn api-routes [{:keys [xtdb-node auth-secret] :as _opts}]
  (if (some? auth-secret)
    [["/swagger.json"
      {:get {:no-doc  true
             :swagger {:info {:title "umeng.backend API"}}
             :handler (swagger/create-swagger-handler)}}]
     ["/health"
      {:get (partial health/healthcheck! xtdb-node)}]
     ["/graph"
      {:post       {:parameters {:body map?}}
       :responses  {200 {:body map?}}
       :handler    graph/api
       :middleware [#_[access-token auth-secret]
                    [jwt/wrap-jwt {:issuers {:no-issuer {:alg    :HS256
                                                         :secret auth-secret}}}]]}]]
    (throw (Exception. "forgot to set SUPA_SCRET envvar"))))

(defn route-data
  [opts]
  (merge
    opts
    {:coercion   malli/coercion
     :muuntaja   formats/instance
     :swagger    {:id ::api}
     :middleware [;; query-params & form-params
                  parameters/parameters-middleware
                  ;; content-negotiation
                  muuntaja/format-negotiate-middleware
                  ;; encoding response body
                  muuntaja/format-response-middleware
                  ;; exception handling
                  coercion/coerce-exceptions-middleware
                  ;; decoding request body
                  muuntaja/format-request-middleware
                  ;; coercing response bodys
                  coercion/coerce-response-middleware
                  ;; coercing request parameters
                  coercion/coerce-request-middleware
                  ;; exception handling
                  exception/wrap-exception
                  ;; make req body stream a string
                  #_body-string/wrap-body-string]}))

(derive :reitit.routes/api :reitit/routes)

(defmethod ig/init-key :reitit.routes/api
  [_ {:keys [base-path]
      :or   {base-path ""}
      :as   opts}]
  [base-path (route-data opts) (api-routes opts)])
