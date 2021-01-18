(ns app.index
  (:require
   ["color" :as color]
   ["expo" :as ex]
   ["expo-constants" :as expo-constants]
   ["react-native" :as rn]
   ["react" :as react]
   ["@react-navigation/native" :as nav]
   ["@react-navigation/drawer" :as d]
   ["react-native-paper" :as paper]
   [applied-science.js-interop :as j]
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [reagent.core :as r]
   [re-frame.core :refer [dispatch-sync]]
   [shadow.expo :as expo]
   [app.fx]
   [app.handlers]
   [app.subscriptions]
   [app.helpers :refer [<sub >evt get-theme]]
   [app.screens.day :as day]
   [app.screens.settings :as settings]
   [app.screens.session :as session]
   [app.screens.reports :as reports]
   [app.screens.tags :as tags]))

;; must use defonce and must refresh full app so metro can fill these in
;; at live-reload time `require` does not exist and will cause errors
;; must use path relative to :output-dir
(defonce splash-img (js/require "../assets/shadow-cljs.png"))

(def styles
  ^js (-> {:surface
           {:flex            1
            :justify-content "center"}

           :theme-switch
           {:flex-direction  "row"
            :justify-content "space-between"}}
          (#(cske/transform-keys csk/->camelCase %))
          (clj->js)
          (rn/StyleSheet.create)))

(def drawer (d/createDrawerNavigator))

(defn navigator [] (-> drawer (j/get :Navigator)))

(defn screen [props] [:> (-> drawer (j/get :Screen)) props])

(defn drawer-icon
  ([icon] (drawer-icon icon nil))
  ([icon text-color-hex]
   (fn [props]
     (r/as-element
       [:> paper/IconButton {:icon  icon
                             :color (if (some? text-color-hex)
                                      text-color-hex
                                      (j/get props :color))}]))))

(defn custom-drawer [props]
  (let [theme          (->> [:theme] <sub get-theme)
        text-color-hex (-> theme (j/get :colors) (j/get :text))
        hidden-screens #{"Session"}
        new-routes     (-> props (j/get :state) (j/get :routes)
                           (->> (remove #(some? (some hidden-screens [(j/get % :name)]))))
                           clj->js)]

    (r/as-element
      [:> d/DrawerContentScrollView (js->clj (-> props (j/assoc-in! [:state :routes] new-routes)))
       [:> d/DrawerItemList (js->clj props)]
       [:> d/DrawerItem {:label       "Share"
                         :label-style {:color text-color-hex}
                         :icon        (drawer-icon "hamburger" text-color-hex)
                         :on-press    #(tap> "Sharing is caring")}]
       [:> d/DrawerItem {:label       "Contact"
                         :label-style {:color (-> theme (j/get :colors) (j/get :text))}
                         :icon        (drawer-icon "hamburger" text-color-hex)
                         :on-press    #(tap> "Scotty, come in Scotty!")}]])))

(defn root []
  (let [theme           (->> [:theme] <sub get-theme)
        !route-name-ref (clojure.core/atom {})
        !navigation-ref (clojure.core/atom {})
        drawer-style    {:background-color (-> theme
                                               (j/get :colors)
                                               (j/get :surface)
                                               color
                                               (j/call :lighten 0.25)
                                               (j/call :hex))}]

    [:> paper/Provider
     {:theme theme}

     [:> nav/NavigationContainer
      {:ref             (fn [el] (reset! !navigation-ref el))
       :on-ready        (fn []
                          (swap! !route-name-ref merge {:current (-> @!navigation-ref
                                                                     (j/call :getCurrentRoute)
                                                                     (j/get :name))}))
       :on-state-change (fn []
                          (let [prev-route-name    (-> @!route-name-ref :current)
                                current-route-name (-> @!navigation-ref
                                                       (j/call :getCurrentRoute)
                                                       (j/get :name))]
                            (when (not= prev-route-name current-route-name)
                              ;; This is where you can do side effecty things like analytics
                              (>evt [:some-fx-example (str "New screen encountered " current-route-name)]))
                            (swap! !route-name-ref merge {:current current-route-name})))}

      [:> (navigator) {:open-by-default        true
                       :drawer-content         custom-drawer
                       :drawer-style           drawer-style
                       :drawer-content-options {:active-tint-color   (-> theme (j/get :colors) (j/get :accent))
                                                :inactive-tint-color (-> theme (j/get :colors) (j/get :text))}}
       (screen {:name      "Day"
                :options   {:drawerIcon (drawer-icon "hamburger")}
                :component (paper/withTheme day/screen)})
       (screen {:name      "Reports"
                :options   {:drawerIcon (drawer-icon "hamburger")}
                :component (paper/withTheme reports/screen)})
       (screen {:name      "Tags"
                :options   {:drawerIcon (drawer-icon "hamburger")}
                :component (paper/withTheme tags/screen)})
       (screen {:name      "Settings"
                :options   {:drawerIcon (drawer-icon "hamburger")}
                :component (paper/withTheme settings/screen)})
       (screen {:name      "Session"
                :options   {:drawerIcon (drawer-icon "hamburger")}
                :component (paper/withTheme session/screen)})]]]))

(defn start
  {:dev/after-load true}
  []
  (expo/render-root
    (r/as-element
      [root
       ;; this is to force a re-render on every save
       ;; otherwise components nesteded withins screens don't get re-rendered
       {:x (js/Date.now)}])))

(def version (-> expo-constants
                 (j/get :default)
                 (j/get :manifest)
                 (j/get :version)))

(defn init []
  (dispatch-sync [:initialize-db])
  (dispatch-sync [:set-version version])
  (start))
