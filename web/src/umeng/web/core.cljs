(ns umeng.web.core
  (:require
   [reagent.dom :as rdom]
   [re-frame.core :as re-frame]
   [umeng.web.events :as events]
   [umeng.web.routes :as routes]
   [umeng.web.views :as views]
   [umeng.web.config :as config]
   ))


(defn dev-setup []
  (when config/debug?
    (println "dev mode")))

(defn ^:dev/after-load mount-root []
  (re-frame/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [views/main-panel] root-el)))

(defn init []
  (routes/start!)
  (re-frame/dispatch-sync [::events/initialize-db])
  (dev-setup)
  (mount-root))
