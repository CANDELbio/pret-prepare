(ns org.parkerici.pret.import
  (:require [org.parkerici.pret.util.io :as util.io]
            [cognitect.anomalies :as anomalies]
            [clojure.tools.logging :as log]
            [org.parkerici.pret.db.schema :as db.schema]
            [org.parkerici.pret.import.engine :as engine]
            [org.parkerici.pret.util.text :refer [->pretty-string folder-of]]))


(defn prepare
  "Create the txn data files from an import-config-file, datomic-config, and target-dir."
  [{:keys [target-dir
           import-cfg-file
           tx-batch-size
           resume
           continue-on-error]}]
  (let [import-config (util.io/read-edn-file import-cfg-file)
        config-root-dir (str (folder-of import-cfg-file) "/")
        schema (db.schema/get-metamodel-and-schema)
        import-result (engine/create-entity-data schema
                                                 import-config
                                                 config-root-dir
                                                 target-dir
                                                 resume
                                                 continue-on-error)]
    (log/info (str "Entity data files prepared: \n" (->> import-result
                                                         (map #(get-in % [:job :pret/input-file]))
                                                         (->pretty-string))))
    (if-let [errors (seq (filter ::anomalies/category import-result))]
      {:errors errors}
      import-result)))
