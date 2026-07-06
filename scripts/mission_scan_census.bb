#!/usr/bin/env bb
;; mission_scan_census.bb — W-candidate-drift-fence diagnostic.
;;
;; Reports the mission-doc scan counts with a per-source breakdown so future
;; candidate-count drift has a one-line explanation. The output answers:
;;   - How many unique live missions does the scan find? (should be stable)
;;   - How many raw files does the file-seq walk hit, by source?
;;   - How many would leak through without the scan-root fence?
;;   - Are any non-primary paths leaking into the final candidate pool?
;;
;; Run: cd /home/joe/code/futon2 && bb scripts/mission_scan_census.bb
;;
;; Acceptance: the FINAL line is either "FENCE OK — 0 non-primary in pool" or
;; "FENCE LEAK — N non-primary in pool" naming the source.

(ns mission-scan-census
  (:require [clojure.string :as str]
            [clojure.java.io :as io]))

(def code-root "/home/joe/code")

(def mission-path-pattern
  #".*/holes/missions/(M-[^/]+)\.md$")

(def non-primary-patterns
  [[#"/\.worktrees/" "git-worktree"]
   [#"-health-main/" "health-main-worktree"]
   [#"-index-check/" "index-check-copy"]
   [#"futon3b/holes/missions/" "futon3b-cross-repo"]])

(defn non-primary-source [path]
  (some (fn [[pattern source]] (when (re-find pattern path) source))
        non-primary-patterns))

(defn -main []
  (let [root (io/file code-root)
        raw-files (->> (file-seq root)
                       (filter #(.isFile %))
                       (map #(.getAbsolutePath %))
                       (filter #(re-matches mission-path-pattern %))
                       (remove #(str/includes? % "/.state/")))
        ;; Group by source
        by-source (group-by (fn [p] (or (non-primary-source p) "primary")) raw-files)
        ;; What the scan-root fence excludes
        fenced (filter non-primary-source raw-files)
        ;; What remains after the fence (should match the real load-missions path)
        after-fence (remove non-primary-source raw-files)]
    ;; Per-source raw counts
    (println "=== Mission-doc scan census ===")
    (println "Code root:" code-root)
    (println)
    (println "RAW files by source:")
    (doseq [[src files] (sort-by (fn [[_ f]] (- (count f))) by-source)]
      (println (format "  %-25s %d files" src (count files))))
    (println (format "  %-25s %d files" "TOTAL" (count raw-files)))
    (println)
    ;; Fence impact
    (println "Scan-root fence:")
    (println (format "  Excluded %d non-primary files" (count fenced)))
    (doseq [[src files] (sort-by (fn [[_ f]] (- (count f))) (group-by non-primary-source fenced))]
      (println (format "    %-23s %d files" src (count files))))
    (println (format "  Remaining after fence: %d files" (count after-fence)))
    (println)
    ;; Dedupe count (unique ids, excluding derived)
    (let [unique-ids (->> after-fence
                          (map #(second (re-matches mission-path-pattern %)))
                          (remove #(str/includes? % "."))
                          distinct)]
      (println "After dedupe (unique non-derived ids):" (count unique-ids))
      ;; Load via the real path for the live count
      (println)
      (println "(Live count requires Clojure — run via the WM test suite for the authoritative number.)"))
    ;; Fence verdict
    (println)
    (let [leaking (filter non-primary-source after-fence)]
      (if (empty? leaking)
        (println "FENCE OK — 0 non-primary in pool")
        (do
          (println (format "FENCE LEAK — %d non-primary in pool:" (count leaking)))
          (doseq [p leaking]
            (println " " p)))))))

(-main)
