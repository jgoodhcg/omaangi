(ns app.fx
  (:require
   [re-frame.core :refer [reg-fx dispatch]]
   [applied-science.js-interop :as j]))

(def !navigation-ref (clojure.core/atom nil))

(defn navigate [name] ;; no params yet
  ;; TODO implement a check that the navigation component has initialized
  ;; https://reactnavigation.org/docs/navigating-without-navigation-prop#handling-initialization
  ;; The race condition is in my favor if the user has to press a component within the navigation container
  (tap> name)
  (tap> @!navigation-ref)

  (-> @!navigation-ref
      ;; no params yet for second arg
      (j/call :navigate name #js {})))

(reg-fx :navigate navigate)

(reg-fx :some-fx-example
        (fn [x]
          (println x)))
