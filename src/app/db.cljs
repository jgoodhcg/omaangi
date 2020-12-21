(ns app.db
  (:require
   [clojure.spec.alpha :as s]
   [spec-tools.data-spec :as ds]
   [spec-tools.core :as st]))

(def app-db-spec
  (ds/spec {:spec {:settings {:theme (s/spec #{:light :dark})}
                   :version  string?}
            :name ::app-db}))

(def default-app-db
  {:settings {:theme :dark}
   :version  "version-not-set"

   :calendar
   {#inst "2020-12-21T13:19:25.742-00:00"
    {:calendar/tracks [#uuid "df8788c4-67db-4fb1-980b-b5f1ab5bb4ac"]
     :calendar/plans  [#uuid "afa40006-5e4f-4f8a-b4f9-09886bbbaf36"]}}

   :tracks {#uuid "df8788c4-67db-4fb1-980b-b5f1ab5bb4ac" {:track/start  #inst "2020-12-21T13:19:25.742-00:00"
                                                          :track/stop   #inst "2020-12-21T14:20:25.742-00:00"
                                                          :track/label  "my frist track"
                                                          :track/color  "#ff00ff" ;; when present this value takes precedent
                                                          :track/groups [#uuid "26c24deb-c0b5-4b7a-8ef9-8dcf540f80d8"] ;; otherwise the color is derived from mixing colors of groups
                                                          }}

   :plans {#uuid "afa40006-5e4f-4f8a-b4f9-09886bbbaf36" {:plan/start #inst "2020-12-21T13:19:20.742-00:00"
                                                         :plan/stop  #inst "2020-12-21T13:21:25.742-00:00"
                                                         :plan/label "my first plan"}}

   :groups {#uuid "26c24deb-c0b5-4b7a-8ef9-8dcf540f80d8" {:group/color "#00ff00"}}
   })

(random-uuid)
(-> default-app-db (get-in [:plans #uuid "afa40006-5e4f-4f8a-b4f9-09886bbbaf36"]))
