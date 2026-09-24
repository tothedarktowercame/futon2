;; P2 gate for :e-cascade-real/turn-rules-v1 files (E-cascade-real packet P2).
;; Fresh process, read-only everywhere:
;;   cd /home/joe/code/futon2 && clojure -M holes/labs/wm-contract/E-cascade-real/rules-from-turns/hist/check.clj
;; Exits non-zero and prints one line per violation.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str]
         '[cheshire.core :as json])

(def dir "holes/labs/wm-contract/E-cascade-real/rules-from-turns/hist")
(def lib-root "/home/joe/code")
(def code-root "/home/joe/code")

(def violations (atom []))
(defn- violate [file msg] (swap! violations conj (str file ": " msg)))

(defn- sha256-file [^java.io.File f]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")
        bytes (java.nio.file.Files/readAllBytes (.toPath f))]
    (.update digest bytes)
    (apply str (map #(format "%02x" %) (.digest digest)))))

(doseq [^java.io.File f (->> (.listFiles (io/file dir))
                             (filter #(str/ends-with? (.getName %) ".edn"))
                             (sort-by #(.getName %)))]
  (let [fname (.getName f)
        data (try (edn/read-string (slurp f))
                  (catch Throwable e
                    (violate fname (str "does not read as EDN: " (.getMessage e)))
                    nil))]
    (when data
      (when-not (= :e-cascade-real/turn-rules-v1 (:schema data))
        (violate fname (str "schema is " (:schema data))))
      (let [tokens (:tokens data)
            src-path (io/file code-root (:source-record data))
            source-text (when (.isFile src-path)
                          (:source_text (json/parse-string (slurp src-path) true)))]
        (when-not source-text
          (violate fname (str "source record unreadable: " src-path)))
        ;; :source_sha256 in the analysis record is the SHA-256 of the
        ;; source_text CONTENT (UTF-8), identical across the .json and
        ;; .analysis.json records — verified 2026-09-24 on this batch.
        (when (and source-text (string? (:source-sha256 data)))
          (let [digest (java.security.MessageDigest/getInstance "SHA-256")
                real-sha (apply str (map #(format "%02x" %)
                                         (.digest digest (.getBytes ^String source-text "UTF-8"))))]
            (when-not (= real-sha (:source-sha256 data))
              (violate fname (str "source-sha256 mismatch: record says "
                                  (:source-sha256 data) ", source_text hashes to " real-sha)))))
        (doseq [[tok {:keys [statement cue]}] tokens]
          (let [[s e] cue]
            (when-not (and (string? statement) (seq statement))
              (violate fname (str "token " tok " has no statement")))
            (when-not (and (integer? s) (integer? e) (<= 0 s) (< s e))
              (violate fname (str "token " tok " cue out of order: " cue)))
            (when (and source-text (integer? s) (integer? e))
              (when-not (<= e (count source-text))
                (violate fname (str "token " tok " cue end " e " beyond source length " (count source-text))))
              (when (and (<= 0 s) (<= e (count source-text)) (< s e)
                         (str/blank? (subs source-text s e)))
                (violate fname (str "token " tok " cue text is empty"))))))
        (when (empty? (:rules data))
          (violate fname "no rules and no not-a-rule entries")
          )
        (doseq [r (:rules data)]
          (let [pat (:pattern r)
                lib-file (io/file lib-root "futon3/library" (str pat ".flexiarg"))]
            (if-not (.isFile lib-file)
              (violate fname (str "pattern file missing: " pat))
              (let [head (slurp lib-file)]
                (when-not (str/includes? head (str "@flexiarg " pat))
                  (violate fname (str "@flexiarg line mismatch in " pat)))
                (let [recorded (get-in r [:receipt :source :sha256])
                      actual (sha256-file lib-file)]
                  (when-not (= recorded actual)
                    (violate fname (str "sha mismatch for " pat ": recorded " recorded " actual " actual)))))))
          (when (empty? (:produces r))
            (violate fname (str "rule for " (:pattern r) " has empty :produces")))
          (doseq [tok (concat (:needs (:guard r)) (:forbids (:guard r)) (:produces r))]
            (when-not (contains? tokens tok)
              (violate fname (str "rule for " (:pattern r) " uses undefined token " tok)))))
        (doseq [n (:not-a-rule data)]
          (when-not (:reason n)
            (violate fname (str "not-a-rule for " (:pattern n) " has no reason"))))))))

(let [v @violations]
  (if (empty? v)
    (println "GATE OK:" (count (->> (.listFiles (io/file dir))
                                     (filter #(str/ends-with? (.getName %) ".edn"))))
             "files, 0 violations")
    (do (println "GATE VIOLATIONS:" (count v))
        (doseq [x v] (println " " x))
        (System/exit 1))))
(shutdown-agents)
