(ns app.screens.settings
  (:require
   ["react-native" :as rn]
   ["react-native-paper" :as paper]

   [applied-science.js-interop :as j]
   [reagent.core :as r]

   [app.components.menu :as menu]
   [app.helpers :refer [<sub >evt get-theme]]
   [app.screens.core :refer [screens]]
   [app.tailwind :refer [tw]]))

(defn top-section [props]
  (let [theme         (->> [:theme] <sub get-theme)
        menu-color    (-> theme
                          (j/get :colors)
                          (j/get :text))
        toggle-drawer (-> props
                          (j/get :navigation)
                          (j/get :toggleDrawer))]

    [:> rn/View {:style (tw "flex flex-row items-center pb-2 pt-2")}
     [menu/button {:button-color menu-color
                   :toggle-menu  toggle-drawer}]
     [:> paper/Title {:style (tw "ml-4")} (:settings screens)]]))

(defn screen [props]
  (r/as-element
    [(fn []
       (let [theme (->> [:theme] <sub get-theme)]

         [:> rn/SafeAreaView {:style (merge (tw "flex flex-1")
                                            {:background-color (-> theme (j/get :colors) (j/get :background))})}

          [:> rn/StatusBar {:visibility "hidden"}]

          [top-section props]

          [:> paper/Surface {:style (merge  (tw "flex flex-1"))}

           ;; switchties
           [:> rn/View {:style (tw "flex p-8 flex-col")}

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
                                :icon     "refresh"} "sync"]]]]]]))]))
