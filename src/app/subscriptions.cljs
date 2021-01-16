(ns app.subscriptions
  (:require
   ["color" :as color]
   ["faker" :as faker] ;; TODO remove when tracking is implemented
   [applied-science.js-interop :as j]
   [re-frame.core :refer [reg-sub subscribe]]
   [com.rpl.specter :as sp :refer [select
                                   setval
                                   transform
                                   select-one
                                   select-one!]]
   [tick.alpha.api :as t]
   [app.colors :refer [material-500-hexes]]
   [app.helpers :refer [touches]]))

(defn version [db _]
  (->> db (select-one! [:version])))
(reg-sub :version version)

(defn theme [db _]
  ;; TODO inject paper from sub? to make testing easier?
  (let [theme-type (->> db
                        (select-one! [:settings :theme]))]
    (case theme-type
      :light :DefaultTheme
      :dark  :DarkTheme
      :DarkTheme)))
(reg-sub :theme theme)

(defn zoom [db _]
  (->> db (select-one! [:view :view/zoom])))
(reg-sub :zoom zoom)

(defn selected-day [db _]
  (->> db (select-one! [:view :view/selected-day])))
(reg-sub :selected-day selected-day)

(defn calendar [db _]
  (->> db (select-one! [:calendar])))
(reg-sub :calendar calendar)

(defn sessions [db _]
  (->> db (select-one! [:sessions])))
(reg-sub :sessions sessions)

(defn truncate-session [day session]
  (let [{:tick/keys [beginning end]} (t/bounds day)
        beginning                    (t/instant beginning)
        end                          (t/instant end)
        {:session/keys [start stop]} session]
    (merge
      session
      {:session/start-truncated (if (-> start (t/< (t/instant beginning)))
                                  beginning
                                  start)
       :session/stop-truncated  (if (-> stop (t/> (t/instant end)))
                                  end
                                  stop)})))

(defn session-overlaps-collision-group? [session c-group]
  (some? (->> c-group
              (some #(touches session %)))))

(defn insert-into-collision-group [collision-groups session]
  (let [collision-groups-with-trailing-empty
        (if (empty? (last collision-groups))
          collision-groups
          (conj collision-groups []))]

    (setval

      (sp/cond-path
        ;;put the session in the first group that collides
        [(sp/subselect sp/ALL (partial session-overlaps-collision-group? session)) sp/FIRST]
        [(sp/subselect sp/ALL (partial session-overlaps-collision-group? session)) sp/FIRST sp/AFTER-ELEM]

        ;; otherwise put it in the first empty
        [(sp/subselect sp/ALL empty?) sp/FIRST]
        [(sp/subselect sp/ALL empty?) sp/FIRST sp/AFTER-ELEM])

      session
      collision-groups-with-trailing-empty)))

(defn get-collision-groups [sessions]
  (->> sessions
       (reduce insert-into-collision-group [[]])
       (remove empty?)))

(defn set-render-props [zoom
                        [collision-index
                         {:session/keys [type
                                         start
                                         stop
                                         label]
                          session-color :session/color
                          :as           session}]]

  (let [type-offset      (case type
                           :session/plan  0
                           :session/track 50
                           10)
        collision-offset (-> collision-index (* 5))
        left             (str (-> type-offset
                                  (+ collision-offset))
                              "%")
        elevation        (-> collision-index (* 2)) ;; pulled from old code idk why it works
        top              (-> start
                             t/date
                             t/bounds
                             t/beginning
                             (#(t/duration {:tick/beginning (t/date-time %)
                                            :tick/end       (t/date-time start)}))
                             t/minutes
                             (* zoom))
        height           (-> (t/duration {:tick/beginning (t/date-time start)
                                          :tick/end       (t/date-time stop)})
                             t/minutes
                             (* zoom))
        session-color    (-> material-500-hexes rand-nth color)]

    (tap> (merge session {:session-render/elevation        elevation
                          :session-render/left             left
                          :session-render/top              top
                          :session-render/height           height
                          :session-render/label            label
                          ;; TODO finish when tags can be injected
                          :session-render/color-hex        (-> session-color (j/call :hex))
                          :session-render/ripple-color-hex (-> session-color (j/call :lighten 0.64) (j/call :hex))
                          }))

    [collision-index
     (merge session {:session-render/elevation        elevation
                     :session-render/left             left
                     :session-render/top              top
                     :session-render/height           height
                     :session-render/label            label
                     ;; TODO finish when tags can be injected
                     :session-render/color-hex        (-> session-color (j/call :hex))
                     :session-render/ripple-color-hex (-> session-color (j/call :lighten 0.64) (j/call :hex))
                     })]))

(defn sessions-for-this-day [[selected-day calendar sessions zoom] _]
  ;; TODO needs to return this structure
  (comment [;; collision groups are an intermediate grouping not in sub result
            #:session-render {:left             0         ;; collision group position and type
                              :top              0         ;; start
                              :elevation        1         ;; collision group position
                              :height           10        ;; duration
                              :color-hex        "#ff00ff" ;; tags mix or :session/color
                              :ripple-color-hex "#ff00ff" ;; tags mix or :session/color lightened
                              :label            "label"   ;; session label and tags depending on settings
                              }])

  (let [this-day (get calendar selected-day)]
    (->> this-day
         :calendar/sessions
         (mapv #(get sessions %))
         (mapv #(truncate-session (:calendar/date this-day) %))
         (sort-by (fn [s] (->> s
                               :session/start
                               (t/new-interval (t/epoch))
                               t/duration
                               t/millis)))
         get-collision-groups
         (transform [sp/ALL sp/INDEXED-VALS] (partial set-render-props zoom))
         flatten)))
(reg-sub :sessions-for-this-day

         :<- [:selected-day]
         :<- [:calendar]
         :<- [:sessions]
         :<- [:zoom]

         sessions-for-this-day)

(defn this-day [selected-day _]
  (let [month (t/month selected-day)
        year  (t/year selected-day)
        ;; TODO move this to injection form sub call or interceptor
        now   (t/now)]
    {:day-of-week   (->> selected-day
                         t/day-of-week
                         str)
     :day-of-month  (str (t/day-of-month selected-day))
     :year          (str year)
     :month         (->> month str)
     :display-year  (not= year (t/year now))
     :display-month (or (not= year (t/year now))
                        (not= month (t/month now)))}))
(reg-sub :this-day

         :<- [:selected-day]

         this-day)

(defn tracking [db _]
  ;; TODO implement once tick and track event is in place
  (for [x (-> 4 rand-int (max 1) range)]
    (let [c                 (-> material-500-hexes rand-nth color)
          more-than-doubled (-> (rand) (> 0.50))]
      {:session/color-hex        (-> c (j/call :hex))
       :session/more-than-double more-than-doubled
       :indicator/color-hex      (-> c (j/call :lighten 0.32) (j/call :hex))
       :ripple/color-hex         (-> c (j/call :lighten 0.64) (j/call :hex))
       :session/relative-width   (if more-than-doubled
                                   "100%"
                                   (-> (rand) (* 100) (str "%"))) })))
(reg-sub :tracking tracking)

(defn hours [[selected-day zoom] _]
  (->> (let [intvl (t/bounds selected-day)]
         (t/range
           (t/beginning intvl)
           (t/end intvl)
           (t/new-duration 1 :hours)))
       (map (fn [h]
              (let [hour (t/hour h)
                    ;; TODO move zoom offset to sub graph
                    y    (-> hour (* 60) (* zoom))]
                {:top y
                 :val hour})))))

(reg-sub :hours

         :<- [:selected-day]
         :<- [:zoom]

         hours)
