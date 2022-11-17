(ns org.candelbio.pret.cli
  (:require [clojure.tools.logging :as log]
            [clojure.tools.cli :as tools.cli]
            [clojure.string :as str]
            [org.candelbio.pret.import :as import]
            [org.candelbio.pret.db.schema :as schema]
            [org.candelbio.pret.util.io :as util.io]
            [org.candelbio.pret.cli.error-handling :as cli.error-handling :refer [exit]]
            [org.candelbio.pret.util.release :as release])
  (:gen-class)
  (:import (java.util Date)))


(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread ex]
      (log/error ex "Uncaught exception on" (.getName thread))
      (cli.error-handling/report-and-exit ex))))


(defn usage [options-summary]
  (->> ["\nUsage: pret options import-cfg-file target-dir"
        "\nOptions:"
        options-summary]
       (str/join \newline)))

(def cli-options
  [[nil "--continue-on-error" "If set, pret will continue when encountering errors and report all errors at end."]
   ["-h" "--help"]])

(defn prepare
  [{:keys [import-cfg-file target-dir continue-on-error] :as ctx}]
  (when continue-on-error
    (println "Set to attempt to continue when prepare encounters errors. Will report all errors at end and in logs."))
  (when-not (and import-cfg-file
                 (util.io/exists? import-cfg-file))
    (exit 1 (str "ERROR: Import config not specified or does not exist: " import-cfg-file)))
  (when-not target-dir
    (exit 1 "ERROR: Must pass working-directory argument to prepare."))
  (when (and (util.io/exists? target-dir)
             (not (util.io/empty-dir? target-dir)))
    (exit 1 (str "Specified working directory: " target-dir " exists and is non-empty.")))
  (import/prepare ctx))

(defn process-args
  "Validate command line arguments. Signal early termination if required."
  [args]
  (let [{:keys [options arguments errors summary] :as arg-map}
        (tools.cli/parse-opts args cli-options)]
    (cond
      (:help options)
      (exit 0 (usage summary))

      errors
      (exit 1 errors)

      ;; pret path/to/importconfig.edn my-working-dir/
      (= 2 (count arguments))
      (let [[import-cfg target-dir] arguments]
          (assoc arg-map :import-cfg-file import-cfg
                         :target-dir target-dir))

      :else
      (exit 0 (usage summary)))))

(defn- elapsed [start end]
  (let [diff (- (.getTime end) (.getTime start))]
    (/ diff 1000.0)))


(defn -main [& args]
  (try
    (println "Pret version:" (release/version))
    (println "CANDEL schema version: " (schema/version))
    (let [arg-map (process-args args)
          {:keys [exit-message ok?]} arg-map
          start (Date.)
          task-results (prepare arg-map)
          done (Date.)]
      (println "Took" (elapsed start done) "seconds")
      (if (:errors task-results)
        (exit 1 "Prepare failed ")
        (exit 0 "Prepare completed.")))
    (catch Throwable t
      (cli.error-handling/report-and-exit t))))

(comment
  (-main "test/resources/matrix/config.edn" "/Users/bkamphaus/scratch/pret-temp"))
