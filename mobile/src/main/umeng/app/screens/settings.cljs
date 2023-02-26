(ns umeng.app.screens.settings
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]

   [applied-science.js-interop :as j]
   [reagent.core :as r]

   [umeng.app.components.generic-top-section :as top-section]
   [umeng.app.components.screen-wrap :as screen-wrap]
   [umeng.app.misc :refer [<sub >evt get-theme]]
   [umeng.app.screens.core :refer [screens]]
   [umeng.app.tailwind :refer [tw]]))

(defn screen [props]
  (let [theme   (->> [:theme] <sub get-theme)
        version (<sub [:version])]

    [screen-wrap/basic
     [top-section/component props (:settings screens)]
     [:> rn/View {:style (tw "flex p-8 flex-col")}
      [:> rn/View {:style (tw "flex flex-col mb-8")}
       [:> paper/Caption {} version]]]]))

(comment
  ;; switchties
  [:> rn/View {:style (tw "flex flex-row justify-between mb-8")}
             [:> paper/Text "Dark mode enabled"]
   [:> paper/Switch {:value true :on-value-change #(tap> "switchity switch")}]]

  [:> rn/View {:style (tw "flex flex-row justify-between mb-8")}
             [:> paper/Text "Lock sessions by default"]
   [:> paper/Switch {:value false :on-value-change #(tap> "switchity switch")}]]

  [:> paper/Divider {:style (tw "mb-8")}]

            ;; export
  [:> rn/View {:style (tw "flex flex-col mb-8")}
   [:> paper/Text {:style (tw "mb-4")} "Export"]
   [:> rn/View {:style (tw "flex flex-row")}
    [:> paper/Button {:style (tw "mr-4 w-24")
                      :mode  "contained"
                      :icon  "code-parentheses"} "edn"]
    [:> paper/Button {:style (tw "mr-4 w-24")
                      :mode  "contained"
                      :icon  "code-braces"} "json"]
    [:> paper/Button {:style (tw "mr-4 w-24")
                      :mode  "contained"
                      :icon  "code-brackets"} "csv"]]]

  [:> paper/Divider {:style (tw "mb-8")}]

            ;; airtable
  [:> rn/View {:style (tw "flex flex-col mb-8")}
   [:> paper/Text {:style (tw "mb-4")} "Airt Table Integration"]
   [:> rn/View {:style (tw "flex flex-row")}
    [:> paper/Button {:style    (tw "mr-4 w-32")
                      :disabled true
                      :mode     "outlined" ;; contained when key is added
                      :icon     "key"} "add key"] ;; "remove key" when key is added
    [:> paper/Button {:style    (tw "mr-4 w-32")
                      :disabled true
                      :mode     "outlined" ;; contained when key is added
                      :icon     "refresh"} "sync"]]]

)
