(ns futon2.aif.occurrence-square-registry
  "Content-bound, occurrence-keyed square records consumed before G."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.three-halves-square :as square])
  (:import [java.nio.file Files StandardCopyOption]))

(def schema :wm/occurrence-square-registry-v1)
(def default-root "/home/joe/code/storage/wm-occurrence-squares")
(def default-library-root "/home/joe/code/futon3/library")

(defn- key-file [root target analysis-digest]
  (io/file (or root default-root) (evidence/value-digest (str target))
           (str analysis-digest ".edn")))

(defn library-resolver
  "Return a pure-at-call-site pattern-text resolver rooted in LIBRARY-ROOT.
  The registry still checks the carried source pin; this only supplies the
  bytes against which the authored four map labels are checked."
  ([] (library-resolver default-library-root))
  ([library-root]
   (fn [pattern]
     (let [relative (str (if (keyword? pattern)
                           (subs (str pattern) 1)
                           pattern)
                         ".flexiarg")
           file (io/file library-root relative)]
       (when (.isFile file) (slurp file))))))

(defn- sealed [record]
  (assoc record :digest (evidence/value-digest (dissoc record :digest))))

(defn validate-record
  "Validate identity, every square, its pattern pin and its written parts."
  [record pattern-texts]
  (let [entries (:entries record)
        pattern-text (fn [pattern]
                       (if (fn? pattern-texts)
                         (pattern-texts pattern)
                         (get pattern-texts pattern)))
        defects
        (if-not (map? entries)
          []
          (vec
         (mapcat
          (fn [[oid {:keys [pattern pattern-sha256 square] :as entry}]]
            (let [occurrence-id? (and (vector? oid) (= 2 (count oid)))]
              (cond-> []
              (not occurrence-id?)
              (conj {:kind :occurrence-id-invalid :occurrence-id oid})
              (not= oid (:occurrence-id entry))
              (conj {:kind :occurrence-id-unbound :occurrence-id oid})
              (and occurrence-id? (not= pattern (first oid)))
              (conj {:kind :occurrence-pattern-mismatch :occurrence-id oid})
              (not= :valid (:status (square/validate square)))
              (conj {:kind :square-invalid :occurrence-id oid})
              (not (every? #(= pattern-sha256 (get-in % [:source :sha256]))
                           (vals (:objects square))))
              (conj {:kind :square-pattern-pin-mismatch :occurrence-id oid})
              (not= :valid (:status (square/parts-check square
                                                        (pattern-text pattern))))
              (conj {:kind :square-parts-mismatch :occurrence-id oid}))))
          entries)))]
    (cond
      (not= schema (:schema record)) {:status :refused :kind :registry-schema-invalid}
      (not (map? entries)) {:status :refused :kind :registry-entries-invalid}
      (not= (:digest record) (evidence/value-digest (dissoc record :digest)))
      {:status :refused :kind :registry-seal-broken}
      (seq defects) {:status :refused :kind :occurrence-squares-invalid
                     :defects defects}
      :else {:status :valid :count (count entries)})))

(defn publish!
  [root record pattern-texts]
  (let [record (sealed (assoc record :schema schema))
        checked (validate-record record pattern-texts)]
    (if-not (= :valid (:status checked))
      checked
      (let [file (key-file root (:target-id record) (:analysis-digest record))
            _ (.mkdirs (.getParentFile file))
            tmp (io/file (.getParentFile file) (str "." (.getName file) ".tmp"))]
        (spit tmp (str (pr-str record) "\n"))
        (Files/move (.toPath tmp) (.toPath file)
                    (into-array StandardCopyOption
                                [StandardCopyOption/ATOMIC_MOVE
                                 StandardCopyOption/REPLACE_EXISTING]))
        record))))

(defn current
  ([target analysis-digest pattern-texts]
   (current default-root target analysis-digest pattern-texts))
  ([root target analysis-digest pattern-texts]
   (let [file (key-file root target analysis-digest)]
     (if-not (.isFile file)
       {:status :absent :kind :occurrence-squares-not-authored
        :target-id target :analysis-digest analysis-digest}
       (let [record (edn/read-string (slurp file))
             checked (validate-record record pattern-texts)]
         (if (= :valid (:status checked)) record checked))))))

(defn squares [record]
  (into {} (map (fn [[oid entry]] [oid (:square entry)])) (:entries record)))
