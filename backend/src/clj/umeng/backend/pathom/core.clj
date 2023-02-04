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
            [potpuri.core :as pot]))

(def xtdb-atom (atom nil))

(defn xtdb-submit-tx [tx-data]
  (-> @xtdb-atom (xt/submit-tx tx-data)))

(pco/defresolver test-resolver
  [{input :test-resolver/input}]
  {:test-resolver/output (str input "+ some more")})

(defn- explain-item-xform [item item-spec]
  (-> item
      (->> (spec/explain-data item-spec))
      (select-keys [:clojure.spec.alpha/problems])
      (merge (pot/map-of item))))

(defmulti  valid-item? :umeng/type)
(defmethod valid-item? :exercise         [item] (spec/valid? e-specs/exercise-spec item))
(defmethod valid-item? :exercise-log     [item] (spec/valid? e-specs/exercise-log-spec item))
(defmethod valid-item? :exercise-session [item] (spec/valid? e-specs/exercise-session-spec item))
(defmethod valid-item? nil [_] false)
(defmethod valid-item? :default [_] false)

(defmulti  explain-item :umeng/type)
(defmethod explain-item :exercise         [item] (explain-item-xform item e-specs/exercise))
(defmethod explain-item :exercise-log     [item] (explain-item-xform item e-specs/exercise-log))
(defmethod explain-item :exercise-session [item] (explain-item-xform item e-specs/exercise-session))
(defmethod explain-item nil [item] (str "No umeng/type specified for " item))
(defmethod explain-item :default [item] (str "not a valid umeng/type for " item))

(pco/defmutation upsert-items
  [{items :umeng/items}]
  {::pco/output [:add-items/errors
                 :umeng/items]}
  (let [invalid-items (->> items (remove valid-item?))]
    (if (empty? invalid-items)
      (do (xtdb-submit-tx (->> items
                               (mapv (fn [item] [:xtdb.api/put item]))))
          {:add-items/error nil
           :umeng/items     items})
      {:add-items/error (->> invalid-items
                             (mapv explain-item))
       :umeng/items     items})))

(def indexes (pci/register [test-resolver upsert-items]))

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
