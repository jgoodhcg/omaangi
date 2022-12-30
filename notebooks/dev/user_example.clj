(ns user-example
  (:require [scicloj.clay.v2.api :as clay]
            [scicloj.kindly.v3.api :as kindly]
            [scicloj.kindly.v3.kind :as kind]
            [scicloj.kindly-default.v1.api :as kindly-default]))

(comment
  ;; don't evaluate the require and just `cider-eval-last-sexp` on the setup! and start!
  (kindly-default/setup!)
  ;; Wait a few seconds before running start!
  ;; It also helps to have the browser open
  ;; You might have to kill the repl and retry a few times
  (clay/start!)
  (clay/show-doc! "src/umeng/notebooks/index.clj"))
