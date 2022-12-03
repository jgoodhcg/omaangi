(ns umeng.tooling.bb.deploy-backend
  (:require [babashka.tasks :refer [shell]]
            [clojure.string :refer [replace]]))

(do
  (let [time-stamp (-> (java.time.LocalDateTime/now)
                       str
                       (replace "T" "-")
                       (replace ":" "-")
                       (replace "." "-"))]
    (shell
     {:dir "../backend"}
     (str "docker build . -t jgoodhcg/umeng:latest -t jgoodhcg/umeng:"
          time-stamp))
    (shell
     {:dir "../backend"}
     "docker push jgoodhcg/umeng:latest")
    (shell
     {:dir "../backend"}
     (str "docker push jgoodhcg/umeng:" time-stamp))
    (shell
     {:dir "../backend"}
     "doctl apps create-deployment 5d984be2-26fc-4e29-8f84-2dc3ffd86067")))
