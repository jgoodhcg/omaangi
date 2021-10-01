(ns app.fx
  (:require
   ["@react-native-async-storage/async-storage" :as async-storage]
   ["expo-constants" :as expo-constants]
   ["react-native" :as rn]
   [re-frame.core :refer [reg-fx]]
   [applied-science.js-interop :as j]
   [app.helpers :refer [>evt]]
   [app.db :as db :refer [default-app-db serialize de-serialize]]
   [cljs.reader :refer [read-string]] ;; TODO justin 2021-09-26 is this a security threat?
   ))

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

(def app-db-key "@app_db")

(reg-fx :check-for-saved-db
        (fn [_]
          (try
            (-> async-storage
                (j/get :default)
                (j/call :getItem app-db-key)
                (j/call :then (fn [local-store-value]
                                (if (some? local-store-value)
                                  (>evt [:load-db (-> local-store-value de-serialize)])
                                  (do
                                    (-> rn/Alert (j/call :alert "no local store data found"))
                                    (>evt [:load-db default-app-db])))))
                (j/call :catch #(do
                                  (tap> (str "get item catch " %))
                                  (-> rn/Alert (j/call :alert "js catch on get item " (str %))))))
            (catch js/Object e
              (tap> (str "error checking for db " e))
              (-> rn/Alert (j/call :alert "cljs catch on get Item " (str e)))))))

(reg-fx :save-db
        (fn [app-db]
          (try
            (-> async-storage
                (j/get :default)
                (j/call :setItem app-db-key (serialize app-db)))
            (catch js/Object e (tap> (str "error saving db " e))))))

(def version (-> expo-constants
                 (j/get :default)
                 (j/get :manifest)
                 (j/get :version)))

(reg-fx :post-load-db
        (fn [_]
          (>evt [:set-version version])
          ;; The tick rate is rather slow (5 sec as of 2021-10-01) because faster rates interfere with buttons
          ;; Because of that we want to tick when app state changes
          (-> rn/AppState
              (j/call :addEventListener
                      "change"
                      #(>evt [:tick-tock])))))
