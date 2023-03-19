(ns user-example
  (:require [scicloj.clay.v2.api :as clay]
            [scicloj.kindly.v3.api :as kindly]
            [scicloj.kindly.v3.kind :as kind]
            [scicloj.kindly-default.v1.api :as kindly-default]
            [nextjournal.clerk :as clerk]))

(comment
  ;; don't evaluate the require and just `cider-eval-last-sexp` on the setup! and start!
  (kindly-default/setup!)
  ;; Wait a few seconds before running start!
  ;; It also helps to have the browser open
  ;; You might have to close the repl and retry a few times
  (clay/start!)
  (clay/show-doc! "src/umeng/notebooks/index.clj")

  ;; call `clerk/show!` explicitly to show a given notebook.
  (clerk/show! "src/umeng/notebooks/index.clj")

  (clerk/serve! {:watch-paths ["src" "data"]})

  (clerk/clear-cache!)

  (clerk/build! {:paths ["src/umeng/notebooks/"]
                 ;; :out-path "resources/clerk_output/"
                 :bundle true
                 ;; :ssr true
                 ;; :compile-css true
                 :browse true})
  )
