(ns umeng.shared.data-xform.airtable-exercises)

(defn xform-exercise [{:keys [id created-time fields]}]
  (let [{:keys [name
                notes
                distance-unit
                weight-unit
                exercise-log
                source
                log-count
                latest-done]} fields
        new-uuid              #?(:clj  (java.util.UUID/randomUUID)
                                 :cljs (random-uuid))]
    {:xt/id                  new-uuid
     :umeng/type             :exercise
     :exercise/label         name
     :exercise/notes         notes
     :exercise/source        source
     :airtable/ported        true
     :airtable/created-time  created-time
     :airtable/distance-unit distance-unit
     :airtable/id            id
     :airtable/exercise-log  exercise-log
     :airtable/weight-unit   weight-unit
     :airtable/log-count     log-count
     :airtable/latest-done   latest-done}))
