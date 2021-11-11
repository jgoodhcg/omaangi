(ns app.components.tag-related
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]

   [potpuri.core :as p]

   [app.helpers :refer [<sub]]
   [app.tailwind :refer [tw]]))

(defn tag-button
  [{:keys [color label on-press style]}]
  [:> paper/Button
   (merge
     (when (some? on-press)
       {:on-press on-press})
     (when (some? style)
       {:style style})
     {:mode  (if (some? color) "contained" "outlined")
      :color color})
   label])

(defn tag-remove-modal
  "`remove-fn` is wrapped in partial and given tag-id"
  [{:keys [close-fn remove-fn]}]
  (let [{:tag-remove-modal/keys [visible id label color]}
        (<sub [:tag-remove-modal])]

    [:> paper/Portal
     [:> paper/Modal {:visible    visible
                      :on-dismiss close-fn}
      [:> paper/Surface
       [:> rn/View {:style (tw "p-2 flex-col")}
        ;; close button
        [:> paper/IconButton {:icon     "close"
                              :on-press close-fn}]

        [:> paper/Paragraph
         {:style (tw "mb-4")}
         "Are you sure you want to remove this tag?"]


        [tag-button
         (merge {:style (tw "mb-4")}
                (p/map-of color id label))]

        [:> paper/Button {:icon     "close"
                          :mode     "contained"
                          :style    (tw "mb-4")
                          :color    "red"
                          :on-press (partial remove-fn id)}
         "remove it"]]]]]))
