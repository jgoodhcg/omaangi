(ns umeng.backend.pathom.core
  (:require [com.wsscode.pathom3.cache :as p.cache]
            [com.wsscode.pathom3.connect.built-in.resolvers :as pbir]
            [com.wsscode.pathom3.connect.built-in.plugins :as pbip]
            [com.wsscode.pathom3.connect.foreign :as pcf]
            [com.wsscode.pathom3.connect.indexes :as pci]
            [com.wsscode.pathom3.connect.operation :as pco]
            [com.wsscode.pathom3.connect.operation.transit :as pcot]
            [com.wsscode.pathom3.connect.planner :as pcp]
            [com.wsscode.pathom3.connect.runner :as pcr]
            [com.wsscode.pathom3.error :as p.error]
            [com.wsscode.pathom3.format.eql :as pf.eql]
            [com.wsscode.pathom3.interface.async.eql :as p.a.eql]
            [com.wsscode.pathom3.interface.eql :as p.eql]
            [com.wsscode.pathom3.interface.smart-map :as psm]
            [com.wsscode.pathom3.path :as p.path]
            [com.wsscode.pathom3.plugin :as p.plugin]
            [umeng.shared.specs.exercises :as e-specs]
            [clojure.spec.alpha :as spec]))


(pco/defresolver test-resolver
  [{input :test-resolver/input}]
  {:test-resolver/output (str input "+ some more")})

#_(pco/defmutation add-exercises
  [env {exercises :umeng/exercises}]
  )

(def indexes (pci/register [test-resolver]))

(comment
  (spec/valid? e-specs/exercise-spec {:xt/id #uuid "5bf4dbb9-5931-4f7c-9095-48e6477bdefc"
                                      :umeng/type :exercise :exercise/label "Pushup"})
  (spec/explain e-specs/exercise-spec {:xt/id #uuid "5bf4dbb9-5931-4f7c-9095-48e6477bdefc"
                                       :umeng/type :exercise :exercise/label "Pushup"})
  )
