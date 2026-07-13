#!/usr/bin/env bb
;;; text-sidecar-flight-witness.clj — zai-11 cross-lane flight witness for
;;; ft-text-sidecar-001 and ft-text-sidecar-002 (zai-12's folds).
;;;
;;; Per-obligation ledger: witnesses each claim against actual repos/files.
;;; Does NOT run the live sidecar (the store may be down); witnesses the
;;; CODE and ARTIFACTS on disk that implement each claim.
;;;
;;; Usage: bb text-sidecar-flight-witness.clj
(require '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[babashka.fs :as fs])

(def repo-root (or (System/getenv "REPO_ROOT") "."))

(defn file-exists? [rel]
  (fs/exists? (fs/path repo-root rel)))

(defn file-lines [rel]
  (if (file-exists? rel)
    (count (str/split-lines (slurp (str (fs/path repo-root rel)))))
    :not-found))

(defn grep [rel pattern]
  (if (file-exists? rel)
    (let [content (slurp (str (fs/path repo-root rel)))
          lines (str/split-lines content)
          matches (filter #(re-find (re-pattern pattern) %) lines)]
      {:found (count matches) :sample (first matches)})
    :not-found))

(defn read-edn [rel]
  (if (file-exists? rel)
    (edn/read-string {:default (fn [_ v] v)} (slurp (str (fs/path repo-root rel))))
    :not-found))

(println "=== TEXT-SIDECAR FLIGHT WITNESS (zai-11 flying zai-12's deposits) ===")
(println "")

;; ---- D1 core artifacts ----
(println "--- D1 CORE ARTIFACTS (existence witness) ---")
(println "futon1b/futon1b_text.clj:" (file-exists? "futon1b/futon1b_text.clj")
         "lines:" (file-lines "futon1b/futon1b_text.clj"))
(println "futon1b/fts_oracle.clj:" (file-exists? "futon1b/fts_oracle.clj")
         "lines:" (file-lines "futon1b/fts_oracle.clj"))
(println "futon1b/futon1b_server.clj text-search-route:"
         (:found (grep "futon1b/futon1b_server.clj" "text-search-route")))
(println "futon1b/zai_memory_1b.clj :text filter:"
         (:found (grep "futon1b/zai_memory_1b.clj" "memory-search-text")))
(println "")

;; ---- Key claims witnessed in source ----
(println "--- KEY CLAIMS WITNESSED IN SOURCE ---")
(println "WAL mode (PRAGMA journal_mode=WAL):"
         (:found (grep "futon1b/futon1b_text.clj" "journal_mode=WAL")))
(println "unicode61 tokenizer (no stemming):"
         (:found (grep "futon1b/futon1b_text.clj" "unicode61")))
(println "Compound (at, id) keyset checkpoint:"
         (:found (grep "futon1b/futon1b_text.clj" "last-at")))
(println "FTS5 pre-filter + XTDB re-check (search fn):"
         (:found (grep "futon1b/futon1b_text.clj" "FTS5 pre-filter")))
(println "on-append! hook (append-path refresh):"
         (:found (grep "futon1b/futon1b_text.clj" "on-append!")))
(println "BM25 ranking:"
         (:found (grep "futon1b/futon1b_text.clj" "bm25")))
(println "")

;; ---- P1 divergence evidence ----
(println "--- P1 DIVERGENCE EVIDENCE (measured outputs) ---")
(let [div (read-edn "futon1b/textprobe/divergence-full.edn")]
  (if (= div :not-found)
    (println "divergence-full.edn: NOT FOUND")
    (let [ef (:entity-full (:bands div))
          ev (:evidence-full (:bands div))]
      (println "Entity inflation:" (:posting-inflation ef) "(claim: 1.028)")
      (println "Evidence inflation:" (:posting-inflation ev) "(claim: 1.000)")
      (println "Entity docs:" (:docs ef) "multi-version:" (:multi-version ef)
               "text-changed:" (:text-changed ef))
      (println "Evidence docs:" (:docs ev) "multi-version:" (:multi-version ev)
               "text-changed:" (:text-changed ev)))))
(println "")

;; ---- Oracle structure ----
(println "--- ORACLE STRUCTURE (fts_oracle.clj) ---")
(let [oracle-src (if (file-exists? "futon1b/fts_oracle.clj")
                   (slurp (str (fs/path repo-root "futon1b/fts_oracle.clj")))
                   "")]
  (println "Query count (def queries lines):"
           (count (filter #(re-find #"\[\".*\" :" %) (str/split-lines oracle-src))))
  (println "Exhaustive scan mention:"
           (if (re-find #"exhaustive" oracle-src) "YES" "NO"))
  (println "AGREE/DISAGREE check:"
           (if (re-find #"AGREE" oracle-src) "YES" "NO")))
(println "")

;; ---- Migration store ----
(println "--- MIGRATION STORE (deep storage) ---")
(println "futon1b/migration-store/ exists:" (fs/directory? (fs/path repo-root "futon1b/migration-store")))
(println "")

;; ---- Textprobe scripts ----
(println "--- PROBE SCRIPTS ---")
(doseq [f ["textprobe_census.clj" "textprobe_updates.clj"
           "textprobe_sample.clj" "textprobe_divergence.clj"
           "textprobe_tokens.clj"]]
  (println (str "futon1b/" f ":") (file-exists? (str "futon1b/" f))))
(println "")

;; ---- Mission doc claims ----
(println "--- MISSION DOC D1 ACCEPTANCE STATUS ---")
(let [mission (if (file-exists? "futon2/holes/M-text-sidecar.md")
                (slurp (str (fs/path repo-root "futon2/holes/M-text-sidecar.md")))
                "")]
  (println "D1 ACCEPTANCE — VERIFIED:"
           (if (re-find #"D1 ACCEPTANCE — VERIFIED" mission) "YES" "NO"))
  (println "94,430 rows claim:"
           (if (re-find #"94,430" mission) "YES" "NO"))
  (println "P1 ANSWERED:"
           (if (re-find #"P1.*ANSWERED" mission) "YES" "NO"))
  (println "P2 DECIDED FTS5:"
           (if (re-find #"P2 — DECIDED.*FTS5" mission) "YES" "NO"))
  (println "D2 Joe-gated (outward ob):"
           (if (re-find #"D2.*Joe" mission) "YES" "NO")))
(println "")
(println "=== WITNESS COMPLETE ===")
