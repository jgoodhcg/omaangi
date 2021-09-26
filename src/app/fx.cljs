(ns app.fx
  (:require
   ["@react-native-async-storage/async-storage" :as async-storage]
   [re-frame.core :refer [reg-fx]]
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

(reg-fx :check-for-saved-db
        (fn [_]
          (try
            (-> async-storage
                (j/get :default)
                (j/call :getItem "@app_db")
                (j/call :then #(tap> (str "get item then " {:thing % :nil? (nil? %)})))
                (j/call :catch #(tap> (str "get item catch " %))))
            (catch js/Object e (tap> (str "error checking for db " e))))))
