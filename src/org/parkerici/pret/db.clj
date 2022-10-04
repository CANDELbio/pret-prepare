(ns org.parkerici.pret.db
  (:require [datomic.api :as d]
            [clojure.core.async :as a]
            [org.parkerici.pret.db.query :as dq]
            [clojure.tools.logging :as log]
            [org.parkerici.pret.db.schema :as schema]
            [clojure.string :as str]))


(defn tx-effect?
  "Given a connection and map with tx-data and query, determines whether or not transacting
  the data in tx-data would change the results of query. Note: work is done on `conn`, so
  be wary of races and don't rely on this outside of e.g. constrained additive schema."
  [conn {:keys [tx-data query name]}]
  (let [db (d/db conn)
        {:keys [db-after]} (d/with db tx-data)]
      (not= (d/q query db)
            (d/q query db-after))))

(defn apply-schema [datomic-uri]
  (let [conn (d/connect datomic-uri)]
    (doseq [raw-tx schema/schema-txes]
      ;; always index attributes (avoid needing to set this in CANDEL schema edn files).
      (let [tx (update-in raw-tx [:tx-data]
                          (fn [tx-data]
                            (mapv (fn [schema-ent]
                                    (if (and (:db/valueType schema-ent)
                                             (not (:db/unique schema-ent)))
                                       (assoc schema-ent :db/index true)
                                       schema-ent))
                                 tx-data)))]
        (if (tx-effect? conn tx)
          (do (log/info ::schema (:name tx) " not in database, transacting.")
              (d/transact conn (:tx-data tx)))
          (log/info ::schema "Skipping schema install for: " (:name tx)))))))

(defn init
  "Loads all base schema, enums, and metamodel into database if necessary."
  [datomic-uri]
  (let [_created? (d/create-database datomic-uri)
        ;; Just here to ensure we can connect before progressing
        conn (d/connect datomic-uri)]
    (apply-schema datomic-uri)
    datomic-uri))

