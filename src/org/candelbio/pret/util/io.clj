(ns org.candelbio.pret.util.io
  (:require [clojure.java.io :refer [make-parents file delete-file]]
            [clojure.edn :as edn]
            [clojure.java.shell :refer [sh]]
            [clojure.string :as s]
            [org.candelbio.pret.util.text :refer [->pretty-string]]
            [clojure.java.io :as io]))


(defn exists?
  "Predicate to determine if file f (string filename or file) exists."
  [f]
  (-> (if (string? f)
        (file f)
        f)
      (.exists)))

(defn dir?
  "Predicate to determine if file f (string filename or file) exists."
  [f]
  (-> (if (string? f)
        (file f)
        f)
      (.isDirectory)))

(defn empty-dir?
  "Predicate to determine if the file f (string or file) is an empty directory."
  [f]
  (if (and (exists? f) (dir? f))
    (-> (if (string? f)
          (file f)
          f)
        (.listFiles)
        count
        zero?)
    false))

(defn write-edn-file
  "Makes parent folders (if necessary) and spits directly into f is data is a string, otherwise writes
   ->pretty-string of data."
  [f data]
  (make-parents f)
  (spit f (if (string? data)
            data
            (->pretty-string data))))


(defn glob
  "Given a directory and a glob-pattern, returns vector of matched files."
  [glob-dir glob-pattern]
  (let [grammar-matcher
        (-> (java.nio.file.FileSystems/getDefault)
            (.getPathMatcher (str "glob:" glob-pattern)))]
    (->> glob-dir
         clojure.java.io/file
         file-seq
         (filter (fn [f]
                   (and (.isFile f)
                        (let [fname (-> f (.toPath) (.getFileName))]
                          (.matches grammar-matcher fname)))))
         (mapv #(.getAbsolutePath %)))))

(defn unrealized-glob
  "Reads in a glob specification and outputs a map"
  [[dir pattern]]
  {:glob/directory dir
   :glob/pattern pattern})

(defn read-edn-file
  "Reads EDN file, or throws ex-info with info on why EDN file can't be read."
  [f]
  (try
    (let [f-text (slurp f)
          f-edn (edn/read-string {:readers {'glob unrealized-glob}} f-text)]
      f-edn)
    (catch Exception e
      (let [message (.getMessage e)
            ;; this is written as cond and can be extended so that we re-map unclear errors
            ;; as encountered to better ones but let clear enough ones through via else
            cause (cond
                    (= message "EOF while reading")
                    "Unmatched delimiters in EDN file resulted in no closing ),}, or ]."

                    :else
                    message)]
        (throw (ex-info (str "Invalid EDN file: " f)
                        {:file f
                         :cause cause}))))))
