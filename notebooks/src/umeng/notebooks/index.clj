(ns umeng.notebooks.index
  (:require [scicloj.clay.v2.api :as clay]
            [scicloj.kindly.v3.api :as kindly]
            [scicloj.kindly.v3.kind :as kind]
            [scicloj.kindly-default.v1.api :as kindly-default]
            ))

(defn start [opts]
  (println "starting kindly ...")
  (kindly-default/setup!)
  (println "starting clay ...")
  (clay/start!)
  (println "Showing airtable doc")
  (Thread/sleep 10000) ;; this is necessary, without it the browser just hangs on the result of clay/start!
  (clay/show-doc! "src/umeng/notebooks/2022_12_11_airtable_data.clj")
  )

(comment
  (clay/show-doc! "src/umeng/notebooks/index.clj")

  (clay/show-doc! "src/umeng/notebooks/2022_12_11_airtable_data.clj")
  (clay/show-doc-and-write-html! "src/umeng/notebooks/2022_12_11_airtable_data.clj" {})
  )
