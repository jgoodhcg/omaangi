(ns umeng.notebooks.index
  (:require [scicloj.clay.v2.api :as clay]
            [scicloj.kindly.v3.api :as kindly]
            [scicloj.kindly.v3.kind :as kind]
            [scicloj.kindly-default.v1.api :as kindly-default]
            ))

(kindly-default/setup!)

(clay/start!)

(comment
  (clay/show-doc! "src/umeng/notebooks/index.clj")

  (clay/show-doc! "src/umeng/notebooks/2022_12_11_airtable_data.clj")
  (clay/show-doc-and-write-html! "src/umeng/notebooks/2022_12_11_airtable_data.clj" {})
  )
