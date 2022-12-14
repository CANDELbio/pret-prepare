(ns org.candelbio.pret.db.schema
  (:require [clojure.java.io :as io]
            [org.candelbio.pret.util.io :as util.io]
            [org.candelbio.pret.db.indexes :as indexes]
            [datomic.api :as d]
            [clojure.set :as set]))

;; hard-coded pret resource based schema file retrievals
(defn base-schema [] (util.io/read-edn-file (io/resource "schema/schema.edn")))
(defn enums [] (util.io/read-edn-file (io/resource "schema/enums.edn")))
(defn metamodel [] (util.io/read-edn-file (io/resource "schema/metamodel.edn")))
(defn pret-meta [] (util.io/read-edn-file (io/resource "schema/pret-meta.edn")))
(defn cached [] (clojure.java.io/resource "cached-schema.edn"))

(def new-ident-q
  '[:find (count ?i)
    :with ?e
    :where [?e :db/ident ?i]])

(def schema-txes
  [{:name :base-schema
    :query new-ident-q
    :tx-data (base-schema)}
   {:name :enums
    :query new-ident-q
    :tx-data (enums)}
   {:name :metamodel-attr
    :query new-ident-q
    :tx-data (first (metamodel))}
   {:name :metamodel-entities
    :query '[:find (count ?k)
             :where [?k :kind/name]]
    :tx-data (second (metamodel))}
   {:name :metamodel-refs
    :query '[:find (count ?p)
             :with ?c
             :where [?p :ref/to ?c]]
    :tx-data (last  (metamodel))}
   {:name :org.candelbio.pret.import.tx-data/metadata
    :query new-ident-q
    :tx-data (pret-meta)}])

(defn cache
  "Write schema to resources (wrapped in vec for eagerness, readability)"
  [schema]
  (binding [*print-length* nil]
    (spit (cached) (vec schema))))

(defn get-all-kind-data
  "Get all the entities representing the kinds in the system"
  [db]
  (flatten (d/q '[:find (pull ?e [*
                                  {:kind/attr [:db/ident]}
                                  {:kind/context-id [:db/ident]}
                                  {:kind/need-uid [:db/ident]}
                                  {:kind/synthetic-attr-name [:db/ident]}])
                  :where [?e :kind/name]] db)))

(defn get-all-schema
  "Query database for installed attributes"
  [db]
  (flatten (d/q '[:find (pull ?e [*
                                  {:db/valueType [:db/ident]}
                                  {:db/cardinality [:db/ident]}
                                  {:db/unique [:db/ident]}])
                  :where [_ :db.install/attribute ?e]] db)))

(defn get-non-attr-idents
  "Returns non-attribute idents.

  Non-attribute idents are assumed to be valid enum idents (this is mostly true, and
  incidental non-enum idents are unlikely to conflict w/user typos.)"
  [db]
  (let [attr-idents (d/q '[:find [?ident ...]
                           :where
                           [_ :db.install/attribute ?a]
                           [?a :db/ident ?ident]]
                         db)
        all-idents (d/q '[:find [?ident ...]
                          :where
                          [?e :db/ident ?ident]]
                        db)]
    (set/difference (set all-idents)
                    (set attr-idents))))

(defn get-metamodel-and-schema
  "Return the schema + metamodel data structure"
  ([db]
   (let [flat-schema (concat (map #(assoc % :db.install/_attribute true)
                                  (get-all-schema db))
                             (get-all-kind-data db))
         core-indexes (indexes/all flat-schema)
         enums (get-non-attr-idents db)
         indexes (assoc core-indexes :index/enum-idents enums)]
     (concat [indexes] flat-schema)))
  ([]
   (util.io/read-edn-file (cached))))

(defn version
  []
  (-> (keep (fn [{:keys [db/ident] :as ent}]
              (when (= ident :candel/schema)
                ent))
            (util.io/read-edn-file (io/resource "schema/enums.edn")))
      (first)
      (:candel.schema/version)))

