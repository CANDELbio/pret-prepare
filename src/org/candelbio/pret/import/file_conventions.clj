(ns org.candelbio.pret.import.file-conventions
  (:require [clojure.java.io :as io]
            [org.candelbio.pret.util.text :as text]
            [org.candelbio.pret.util.io :as pio]
            [clojure.string :as str]
            [clojure.edn :as edn])
  (:import (java.io File)))

(def ref-file-prefix "pret-ref-")
(def import-cfg-job-file-name "import-job.edn")
(def ignored-filenames #{"import-summary.edn" import-cfg-job-file-name})
(def sep (File/separator))

(defn ->full-path [fname]
  (-> fname
      (io/file)
      (.getCanonicalPath)))

(defn- edn+file-filter
  [dir]
  (->> dir
       (io/file)
       (.list)
       (filter #(str/ends-with? % ".edn"))
       (remove ignored-filenames)))

(defn- tsv+file-filter
  [dir]
  (->> dir
       (io/file)
       (.list)
       (filter #(str/ends-with? % ".tsv"))
       (remove ignored-filenames)))

(defn rm-edn-files
  [dir]
  (let [files (->> dir
                   (io/file)
                   (.list)
                   (filter #(str/ends-with? % ".edn")))]
    (doseq [f files]
      (-> (io/file (str dir sep f))
          (.delete)))))

(defn matrix-folder
  "Return the matrix folder path within a target dir."
  [target-dir]
  (str target-dir sep "matrix-data"))

(defn in-entity-dir [target-dir fname]
  (let [target-full-path (-> target-dir
                             (io/file)
                             (.getCanonicalPath))]
    (str target-full-path sep fname)))


(defn all-entity-filenames
  "Return a list of all filenames of .edn files in the directory 'path' excluding
  the import-summary.edn and import-job.edn files"
  [target-dir]
  (->> (edn+file-filter target-dir)
       (map (partial str target-dir sep))
       (map ->full-path)))

(defn matrix-filenames
  "Given a pret working directory, returns a list of all matrix filenames."
  [target-dir]
  (->> target-dir
       (matrix-folder)
       (tsv+file-filter)))

(defn in-matrix-dir [target-dir fname]
  (let [target-full-path (-> target-dir
                             (matrix-folder)
                             (io/file)
                             (.getCanonicalPath))]
    (str target-full-path sep fname)))

(defn job-entity [target-dir]
  (let [in-path (str target-dir sep "tx-data" sep import-cfg-job-file-name)]
    (edn/read-string (str "[" (slurp in-path) "]"))))

(defn import-name
  "Return the import job name defined by the import job file in the tx-data
   subdir of the given path."
  [target-dir]
  (let [data (job-entity target-dir)
        import-job-name (get-in (ffirst data) [:import/import :import/name])]
    import-job-name))

(defn dataset-name
  "Return the dataset name defined by the import job file in the given path."
  [target-dir]
  (let [data (job-entity target-dir)
        dataset-name (:dataset/name (second data))]
    dataset-name))

(defn dataset-name
  "Return the dataset name (from the import entity tx-data file)"
  [target-dir]
  (let [in-path (str target-dir sep "tx-data" sep import-cfg-job-file-name)
        cfg-file-data (edn/read-string (str "[" (slurp in-path) "]"))]
    (->> cfg-file-data
         (second)
         (filter :dataset/name)
         (first)
         (:dataset/name))))
