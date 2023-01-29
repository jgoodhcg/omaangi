(ns umeng.app
  (:require
   ["react-native-gesture-handler" :as gh]
   ["color" :as color]
   ["expo" :as ex]
   ["expo-constants" :as expo-constants]
   ["react-native" :as rn]
   ["react" :as react]
   ["@react-navigation/native" :as nav]
   ["@react-navigation/drawer" :as d]
   ["@react-navigation/stack" :as s]
   ["react-native-paper" :as paper]

   [applied-science.js-interop :as j]
   [camel-snake-kebab.core :as csk]
   [camel-snake-kebab.extras :as cske]
   [reagent.core :as r]
   [re-frame.core :refer [dispatch-sync]]
   [shadow.expo :as expo]
   [potpuri.core :as p]

   [umeng.app.fx :refer [!navigation-ref]]
   [umeng.app.handlers]
   [umeng.app.subscriptions]
   [umeng.app.misc :refer [<sub >evt get-theme]]
   [umeng.app.screens.core :refer [screens]]
   [umeng.app.screens.day :as day]
   [umeng.app.screens.settings :as settings]
   [umeng.app.screens.session :as session]
   [umeng.app.screens.reports :as reports]
   [umeng.app.screens.tags :as tags]
   [umeng.app.screens.tag :as tag]
   [umeng.app.screens.templates :as templates]
   [umeng.app.screens.template :as template]
   [umeng.app.screens.session-template :as session-template]
   [umeng.app.screens.backups :as backups]
   [umeng.app.screens.import :as import]
   [umeng.app.tailwind :refer [tw]]
   ))

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

(def stack (s/createStackNavigator))

(defn drawer-navigator [] (-> drawer (j/get :Navigator)))

(defn stack-navigator [] (-> stack (j/get :Navigator)))

(defn drawer-screen [props] [:> (-> drawer (j/get :Screen)) props])

(defn stack-screen [props] [:> (-> stack (j/get :Screen)) props])

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
        text-color-hex (-> theme (j/get :colors) (j/get :onSurface))]

    (r/as-element
     [:> d/DrawerContentScrollView (merge (js->clj props)
                                          {:style {:backgroundColor
                                                   (-> theme
                                                       (j/get :colors)
                                                       (j/get :surface)
                                                       )}})
       [:> d/DrawerItemList (js->clj props)]
       [:> d/DrawerItem {:label       "Share"
                         :label-style {:color text-color-hex}
                         :icon        (drawer-icon "hamburger" text-color-hex)
                         :on-press    #(tap> "Sharing is caring")}]
       [:> d/DrawerItem {:label       "Contact"
                         :label-style {:color text-color-hex}
                         :icon        (drawer-icon "hamburger" text-color-hex)
                         :on-press    #(tap> "Scotty, come in Scotty!")}]])))

(defn hoc-wrap
  [the-screen]
  (gh/gestureHandlerRootHOC
   (paper/withTheme
    (fn [props]
      (r/as-element [the-screen props])))))

(defn root []
  (let [theme           (->> [:theme] <sub get-theme)
        !route-name-ref (clojure.core/atom {})
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
                           (let [current-route-name (-> @!navigation-ref
                                                        (j/call :getCurrentRoute)
                                                        (j/get :name))
                                 prev-route-name    (->  @!route-name-ref :current)]

                             ;; This is a bit of a hack 😬
                             ;; I needed a way to "deselect" session when going "back" from session to day screen
                             ;; TODO figure out a better way to do this
                             (when (and (-> current-route-name (= (:day screens)))
                                        (-> prev-route-name (= (:session screens))))
                               (>evt [:set-selected-session nil]))

                             ;; same hack but different set of screens
                             (when (and (-> current-route-name (= (:template screens)))
                                        (-> prev-route-name (= (:session-template screens))))
                               (>evt [:set-selected-session-template nil]))

                             (swap! !route-name-ref merge {:current current-route-name})))}

       #_[:> (stack-navigator) {:screen-options {:header-shown false}}
          (stack-screen {:name      "test-dashboard"
                         :options   {}
                         :component (hoc-wrap
                                     #(r/as-element
                                       [:> rn/View {:style (tw "h-full")}
                                        [:> rn/StatusBar {:hidden true}]
                                        [:> paper/Surface {:style (tw "h-full justify-center items-center")}
                                         [:> rn/SafeAreaView
                                          [:> paper/Text {:variant "displayLarge"} "Hello"]]]]))})]

       [:> (drawer-navigator) {:drawer-content     custom-drawer
                               :drawer-style       drawer-style
                               :initial-route-name (:day screens)
                               :screen-options     {:headerShown         false
                                                    :drawerActiveTintColor
                                                    (-> theme
                                                        (j/get :colors)
                                                        (j/get :onSurface))
                                                    :drawerInactiveTintColor
                                                    (-> theme
                                                        (j/get :colors)
                                                        (j/get :onSurfaceVariant))}}
        (drawer-screen {:name      (:day-stack screens)
                        :options   {:drawerIcon (drawer-icon "calendar")}
                        :component #(r/as-element
                                     [:> (stack-navigator) {:initial-route-name (:day screens)}
                                      (stack-screen {:name      (:day screens)
                                                     :component (hoc-wrap day/screen)
                                                     :options   {:headerShown false}})
                                      (stack-screen {:name      (:session screens)
                                                     :options   {:headerTintColor (-> theme
                                                                                      (j/get :colors)
                                                                                      (j/get :onSurface))
                                                                 :headerTitleStyle
                                                                 #js {:display "none"}
                                                                 :headerStyle
                                                                 ;; for some reason the :surface color comes out the same as :background when used on paper/Surface
                                                                 ;; when using :background here it has a weird opacity issue or something
                                                                 #js {:backgroundColor (-> theme
                                                                                           (j/get :colors)
                                                                                           (j/get :surface))}}
                                                     :component (hoc-wrap session/screen)})])})
        (drawer-screen {:name      (:reports screens)
                        :options   {:drawerIcon (drawer-icon "hamburger")}
                        :component (hoc-wrap reports/screen)})
        (drawer-screen {:name      (:tags-stack screens)
                        :options   {:drawerIcon (drawer-icon "hamburger")}
                        :component #(r/as-element
                                     [:> (stack-navigator) {:initial-route-name (:tags screens)}
                                      (stack-screen {:name      (:tags screens)
                                                     :component (hoc-wrap tags/screen)
                                                     :options   {:headerShown false}})
                                      (stack-screen {:name      (:tag screens)
                                                     :options   {:headerTintColor (-> theme
                                                                                      (j/get :colors)
                                                                                      (j/get :text))
                                                                 :headerTitleStyle
                                                                 #js {:display "none"}
                                                                 :headerStyle
                                                                 ;; for some reason the :surface color comes out the same as :background when used on paper/Surface
                                                                 ;; when using :background here it has a weird opacity issue or something
                                                                 #js {:backgroundColor (-> theme
                                                                                           (j/get :colors)
                                                                                           (j/get :surface))}}
                                                     :component (hoc-wrap tag/screen)})])})
        (drawer-screen {:name      (:templates-stack screens)
                        :options   {:drawerIcon (drawer-icon "hamburger")}
                        :component #(r/as-element
                                     [:> (stack-navigator) {:initial-route-name (:templates screens)}
                                      (stack-screen {:name      (:templates screens)
                                                     :component (hoc-wrap templates/screen)
                                                     :options   {:headerShown false}})
                                      (stack-screen {:name      (:template screens)
                                                     :options   {:headerTintColor (-> theme
                                                                                      (j/get :colors)
                                                                                      (j/get :text))
                                                                 :headerTitleStyle
                                                                 #js {:display "none"}
                                                                 :headerStyle
                                                                 #js {:backgroundColor (-> theme
                                                                                           (j/get :colors)
                                                                                           (j/get :background))}}
                                                     :component (hoc-wrap template/screen)})
                                      (stack-screen {:name      (:session-template screens)
                                                     :options   {:headerTintColor (-> theme
                                                                                      (j/get :colors)
                                                                                      (j/get :text))
                                                                 :headerTitleStyle
                                                                 #js {:display "none"}
                                                                 :headerStyle
                                                                 #js {:backgroundColor (-> theme
                                                                                           (j/get :colors)
                                                                                           (j/get :background))}}
                                                     :component (hoc-wrap session-template/screen)})])})
        (drawer-screen {:name      (:settings screens)
                        :options   {:drawerIcon (drawer-icon "tune")}
                        :component (hoc-wrap settings/screen)})
        (drawer-screen {:name      (:backups screens)
                        :options   {:drawerIcon (drawer-icon "tune")}
                        :component (hoc-wrap backups/screen)})
        (drawer-screen {:name      (:import screens)
                        :options   {:drawerIcon (drawer-icon "tune")}
                        :component (hoc-wrap import/screen)})]]]))

(defn start
  {:dev/after-load true}
  []
  (expo/render-root
    (r/as-element
      [root
       ;; this is to force a re-render on every save
       ;; otherwise components nesteded withins screens don't get re-rendered
       {:x (js/Date.now)}])))

(defn init []
  (try
    (dispatch-sync [:initialize-db]) ;; this just keepts the subs from blowing up
    (dispatch-sync [:check-for-saved-db]) ;; load from local file system or default then start ticking
    (dispatch-sync [:create-backups-directory]) ;; create directory for backups if it doesn't exist
    (start)
    (catch js/Object e
      (-> rn/Alert (j/call :alert "Failure on startup" (str e))))))

(comment
  (>evt [:set-theme :light])
  (>evt [:set-theme :dark])
  (->> [:theme] <sub get-theme)
  )
