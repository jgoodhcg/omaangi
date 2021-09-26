(ns app.fx
  (:require
   [re-frame.core :refer [reg-fx dispatch]]
   [applied-science.js-interop :as j]
   [app.helpers :refer [>evt]]))

(def !navigation-ref (clojure.core/atom nil))

(def ticker-ref (clojure.core/atom nil))

(defn navigate [name] ;; no params yet
  ;; TODO implement a check that the navigation component has initialized
  ;; https://reactnavigation.org/docs/navigating-without-navigation-prop#handling-initialization
  ;; The race condition is in my favor if the user has to press a component within the navigation container
  (-> @!navigation-ref
      ;; no params yet for second arg
      (j/call :navigate name (j/lit {}))))

(reg-fx :navigate navigate)

(reg-fx :some-fx-example
        (fn [x]
          (println x)))

(reg-fx :start-ticking
        (fn [_]
          (reset! ticker-ref (js/setInterval #(>evt [:tick-tock]) 5000))))

(reg-fx :stop-ticking
        (fn [_]
          (let [ticker-ref-id @ticker-ref]
            (tap> (str "clearing interval " ticker-ref-id))
            (-> ticker-ref-id (js/clearInterval)))))
