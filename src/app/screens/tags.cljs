(ns app.screens.tags
  (:require
   ["color" :as color]
   ["react-native" :as rn]
   ["react-native-paper" :as paper]

   [applied-science.js-interop :as j]
   [com.rpl.specter :as sp :refer [setval]]
   [reagent.core :as r]

   [app.colors :refer [material-500-hexes]]
   [app.components.generic-top-section :as top-section]
   [app.components.color-picker :as color-picker]
   [app.helpers :refer [<sub >evt get-theme chance]]
   [app.screens.core :refer [screens]]
   [app.tailwind :refer [tw]]))

(def tmp-tags
  (r/atom
    (for [i (range 10)]
      #:tag {:label (str "tag " i)
             :color (if (chance :med)
                      nil
                      (-> material-500-hexes rand-nth))

             :tmp-update-fn
             #(swap! tmp-tags (fn [tags] (->> tags (setval [i :color] %))))

             :id (random-uuid)})))

(defn screen [props]
  (r/as-element
    [(fn []
       (let [theme (->> [:theme] <sub get-theme)
             tags  @tmp-tags]

         ;; TODO justin 2021-02-07 Do we need safe area view everywhere?
         [:> rn/ScrollView {:style (merge (tw "flex flex-1")
                                          {:background-color (-> theme (j/get :colors) (j/get :background))})}

          [:> rn/StatusBar {:visibility "hidden"}]

          [top-section/component props (:tags screens)]

          (for [{tag-color :tag/color
                 tag-label :tag/label
                 tag-id    :tag/id} tags]
            [:> rn/View {:style (tw "flex flex-row mb-4 items-center")
                         :key   tag-id}

             [color-picker/component {:input-color tag-color
                                      :update-fn   #(tap> (str "set tag color " %))}]

             [:> paper/IconButton {:style    (merge (tw "mr-4")
                                                    {:background-color tag-color})
                                   :color    (if-some [c tag-color]
                                               (if (-> c color (j/call :isLight))
                                                 "black" "white")
                                               (-> theme (j/get :colors) (j/get :disabled)))
                                   :icon     "palette"
                                   :on-press #(>evt [:set-color-picker
                                                     #:color-picker {:visible true
                                                                     :value   tag-color}])}]

             ;; TODO justin 2021-02-07 Should this toggle an input or be an input all of the time?
             [:> paper/TextInput {:style          (tw "w-2/3")
                                  :label          "Label"
                                  :value          tag-label
                                  :on-change-text #(tap> %)}]

             ;; TODO justin 2021-02-07 Add ¿generic? delete modal
             [:> paper/IconButton {:icon "delete-forever"}]])]))]))
