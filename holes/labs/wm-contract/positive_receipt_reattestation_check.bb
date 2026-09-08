#!/usr/bin/env bb
;; U71: a changed declaration that a positive receipt pins must be re-attested.
;; The U72 backlog is explicit and hash-specific: a second change is not hidden
;; by an entry for the first stale state.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(def args (apply hash-map *command-line-args*))
(def lab-dir (io/file (or (get args "--lab")
                          (.getParent (io/file *file*)))))
(def code-root (io/file (or (get args "--code-root") "/home/joe/code")))
(def known-path (io/file (or (get args "--known")
                             (str (io/file lab-dir "positive-receipt-known-stale.edn")))))

(defn die [& xs]
  (binding [*out* *err*] (apply println "positive_receipt_reattestation_check: FAIL --" xs))
  (System/exit 1))

(def declaration-start
  #"^(?:private\s+)?(?:noncomputable\s+)?(?:structure|inductive|abbrev|def|theorem|lemma)\s+")

(defn declaration-text [source declaration]
  (let [lines (vec (str/split-lines source))
        start-pattern (re-pattern
                       (str "^(?:private\\s+)?(?:noncomputable\\s+)?"
                            "(?:structure|inductive|abbrev|def|theorem|lemma)\\s+"
                            (java.util.regex.Pattern/quote declaration) "(?:\\s|$)"))
        boundary? #(or (boolean (re-find declaration-start %))
                       (boolean (re-find #"^(?:namespace|end)\s" %)))
        start (first (keep-indexed #(when (re-find start-pattern %2) %1) lines))]
    (when (nil? start)
      (throw (ex-info "declaration absent" {:declaration declaration})))
    (let [end (or (first (keep-indexed #(when (and (> %1 start) (boundary? %2)) %1) lines))
                  (count lines))]
      (str (str/join "\n" (subvec lines start end)) "\n"))))

(defn sha256 [s]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes s "UTF-8"))
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(def receipt-files
  (->> (.listFiles lab-dir)
       (filter #(or (str/ends-with? (.getName %) "-positive-receipt.edn")
                    (str/ends-with? (.getName %) "-proof-receipt.edn")))
       (sort-by #(.getName %))))

(def stale
  (vec
   (for [receipt-file receipt-files
         :let [receipt (edn/read-string (slurp receipt-file))]
         basis (or (:source-basis receipt)
                   (when-let [proof-source (:proof-source receipt)] [proof-source]))
         declaration (:declarations basis)
         :let [source-file (io/file code-root (:repo basis) (:path basis))
               live (try (sha256 (declaration-text (slurp source-file) (:name declaration)))
                         (catch Exception e (str "unreadable:" (.getMessage e))))]
         :when (not= (:sha256 declaration) live)]
     {:receipt (.getName receipt-file)
      :repo (:repo basis)
      :path (:path basis)
      :declaration (:name declaration)
      :pinned-sha256 (:sha256 declaration)
      :live-sha256 live})))

(def known-doc
  (try (edn/read-string (slurp known-path))
       (catch Exception e (die "cannot read known-stale set" (.getPath known-path) (.getMessage e)))))
(when-not (= :positive-receipt-known-stale/v1 (:schema known-doc))
  (die "unexpected known-stale schema in" (.getPath known-path)))
(def known (set (:entries known-doc)))
(def unexpected (remove known stale))

(doseq [entry stale]
  (println "positive_receipt_reattestation_check:"
           (if (known entry) "KNOWN-STALE" "UNATTESTED-DRIFT")
           (pr-str entry)))
(when (seq unexpected)
  (die (count unexpected) "declaration(s) moved without a re-emitted receipt"))
(println "positive_receipt_reattestation_check: PASS --"
         (count receipt-files) "receipts;" (count stale) "known stale; 0 un-attested drift")
