(ns app.components.time-related
  (:require
   ["react-native-paper" :as paper]

   [app.helpers :refer [>evt >evt-sync]]
   [app.tailwind :refer [tw]]))

(defn date-button
  [{:keys [value id label field-key]}]
  [:> paper/Button {:mode     "flat"
                    :icon     "calendar"
                    :style    (tw "mr-4 mt-4 w-40")
                    :on-press #(>evt [:set-date-time-picker
                                      #:date-time-picker
                                      {:value      value
                                       :mode       "date"
                                       :id         :session
                                       :session-id id
                                       :field-key  field-key
                                       :visible    true}])} label])

(defn time-button
  [{:keys [value id label field-key]}]
  [:> paper/Button {:mode     "flat"
                    :icon     "clock"
                    :style    (tw "mr-4 mt-4 w-28")
                    :on-press #(>evt [:set-date-time-picker
                                      #:date-time-picker
                                      {:value      value
                                       :mode       "time"
                                       :id         :session
                                       :session-id id
                                       :field-key  field-key
                                       :visible    true}])} label])

(defn no-stamp-button
  [{:keys [id set-start set-stop]
    :or   {set-start false
           set-stop  false}}]
  [:> paper/Button {:mode     "outlined"
                    :icon     "calendar"
                    :style    (tw "mr-4 mt-4 w-40")
                    :on-press #(>evt-sync
                                 [:set-initial-timestamp
                                  {:set-start  set-start
                                   :set-stop   set-stop
                                   :session/id id}])  } "not set"])
