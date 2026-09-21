(ns reproduce
  "Read-only inventory of durable WM close records for improve-8 discovery."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.security MessageDigest]))

(def default-root "/home/joe/code/futon2/data")

(defn sha256 [file]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream file)]
      (let [buf (byte-array 8192)]
        (loop []
          (let [n (.read in buf)]
            (when (pos? n)
              (.update digest buf 0 n)
              (recur))))))
    (apply str (map #(format "%02x" (bit-and 0xff %)) (.digest digest)))))

(defn read-if-present [file]
  (when (.isFile file) (edn/read-string (slurp file))))

(defn close-files [root]
  (->> (file-seq (io/file root))
       (filter #(and (.isFile %) (= "007-closed.edn" (.getName %))))
       (sort-by #(.getPath %))))

(defn increment-evidence [attempt-dir]
  (let [receipt (read-if-present (io/file attempt-dir "retained/route-attestation.edn"))]
    (first (filter #(and (= :matched (:status %))
                         (= :increment (get-in % [:criterion :kind]))
                         (not= :may-not (:valence %))
                         (= :present (get-in % [:attestation :status])))
                   (:increments receipt)))))

(defn focus-class [attempt-dir target]
  (let [selection (read-if-present (io/file attempt-dir "002-selection.edn"))
        receipt (get-in selection [:payload :judgment :selection-certificate :focus-receipt])]
    (some #(when (= target (:target %)) (:class %)) (:candidates receipt))))

(defn classify [file root]
  (let [close (edn/read-string (slurp file))
        j (get-in close [:payload :judgment])
        attempt-dir (.getParentFile file)
        increment (increment-evidence attempt-dir)
        facet (when increment (focus-class attempt-dir (get-in increment [:criterion :target])))
        typed-failure? (and (false? (:grounded? j))
                            (false? (:artifact-only? j))
                            (keyword? (:failure-kind j)))
        class (cond
                (and increment (= :focus facet)) :focus-increment
                (and increment (= :associated facet)) :associated-increment
                (and increment (= :useful-elsewhere facet)) :elsewhere-useful
                typed-failure? :known-typed-failure
                :else :unknown)
        missing (when (= :unknown class)
                  (cond
                    (nil? increment) [:attested-increment]
                    (nil? facet) [:commit-facets-v1-relation]
                    :else [:supported-run-ending-class]))]
    {:path (str/replace (.getPath file) (str (.getCanonicalPath (io/file root)) "/") "")
     :cohort (:cohort/id close)
     :attempt (:attempt/id close)
     :close-outcome (:outcome j)
     :failure-kind (:failure-kind j)
     :class class
     :missing missing
     :evidence (cond
                 typed-failure? (str "close sha256=" (sha256 file) "; :failure-kind=" (:failure-kind j))
                 increment (str "route increment=" (get-in increment [:criterion :id]) "; facet=" facet)
                 :else (str "close sha256=" (sha256 file) "; no retained route attestation"))}))

(defn render-row [{:keys [path close-outcome failure-kind class missing evidence]}]
  (str "| `" path "` | `" close-outcome "` | "
       (if failure-kind (str "`" failure-kind "`") "—") " | `" class "` | "
       (if missing (str "missing `" (str/join "`, `" missing) "`") evidence) " |"))

(defn -main [& [root]]
  (let [root (or root default-root)
        rows (mapv #(classify % root) (close-files root))]
    (println "| durable close record | close outcome | typed failure | proposed class | evidence / missing |")
    (println "|---|---|---|---|---|")
    (doseq [row rows] (println (render-row row)))
    (binding [*out* *err*]
      (prn {:root (.getCanonicalPath (io/file root))
            :records (count rows)
            :classes (frequencies (map :class rows))
            :close-outcomes (frequencies (map :close-outcome rows))
            :retained-directories (count (filter #(.isDirectory (io/file (.getParentFile %) "retained"))
                                                 (close-files root)))}))))

(apply -main *command-line-args*)
