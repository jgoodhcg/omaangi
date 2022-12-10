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
            [integrant.core :as ig]
            [umeng.shared.specs.exercises :as e-specs]
            [clojure.spec.alpha :as spec]
            [xtdb.api :as xt]
            [potpuri.core :as p]))

(def xtdb-atom (atom nil))

(defn xtdb-submit-tx [tx-data]
  (-> @xtdb-atom (xt/submit-tx tx-data)))

(pco/defresolver test-resolver
  [{input :test-resolver/input}]
  {:test-resolver/output (str input "+ some more")})

(pco/defmutation add-exercises
  [{exercises :umeng/exercises}]
  {::pco/output [:add-exercises/error
                 :exercises]} ;; 2022-12-04 Justin add all keys for exercise or remove this
  (let [invalid-items (->> exercises (remove (fn [exercise] (spec/valid? e-specs/exercise-spec exercise))))]
    (if (empty? invalid-items)
      (do (xtdb-submit-tx (->> exercises
                               (mapv (fn [exercise] [:xtdb.api/put exercise]))))
          {:add-exercises/error nil
           :exercises           exercises})
      {:add-exercises/error (->> exercises
                                 (mapv (fn [exercise]
                                         (-> exercise
                                              (->> (spec/explain-data e-specs/exercise-spec))
                                              (select-keys [:clojure.spec.alpha/problems])
                                              (merge {:item exercise})))))
       :exercises           exercises})))

(def indexes (pci/register [test-resolver add-exercises]))

(defmethod ig/init-key :api/xtdb
  [_ {xtdb-node :xtdb-node}]
  (reset! xtdb-atom xtdb-node))

(comment
  (spec/valid? e-specs/exercise-spec {:xt/id #uuid "5bf4dbb9-5931-4f7c-9095-48e6477bdefc"
                                      :umeng/type :exercise :exercise/label "Pushup"})
  (spec/explain-data e-specs/exercise-spec {:xt/id #uuid "5bf4dbb9-5931-4f7c-9095-48e6477bdefc"
                                       :umeng/type :exercises :exercise/label "Pushup"})
)

(comment
  (-> @xtdb-atom (xt/db) (xt/q '{:find [e] :where [[e :xt/id _]]}))
  (-> @xtdb-atom (xt/submit-tx [[:xtdb.api/put {:xt/id :hello-1 :a "there"}]]))
  )
