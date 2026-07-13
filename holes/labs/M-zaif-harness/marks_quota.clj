#!/usr/bin/env bb
;; marks_quota.clj — the operator's mark-quota view (C-futon1b-features).
;;
;; GAMIFICATION GUARD (feedback memory, honored by construction): every
;; target below traces to a REAL gate constant, never fun-tuning —
;;   20  = M-marks-to-labels §L3 preregistration (mark-labels needed before
;;         the measurement gate runs);
;;   5   = policy_precision.clj default-min-history (γ burn-in per mission
;;         cell — below it the cell sits at the uniform prior).
;; Second-string types get COUNTS, NO TARGETS: no promotion threshold has
;; been declared, so none is invented; usage argues promotion.
;;
;; Store truth only (laptop :7073; the marks corpus is SPLIT across stores —
;; lucy's first ✓ is grandfathered via b1-live-marks.edn, shown separately).
;;
;; Usage (any cwd):  bb marks_quota.clj [--since ISO] [--edn]
(require '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[babashka.fs :as fs])

(def lab (str (fs/parent (fs/absolutize *file*))))
(load-file (str lab "/z1_views.clj"))

(def l0-live "2026-07-12T09:40:00Z")  ; L0 recognizer hot-loaded (futon3c 6c7ff4d+71ed872)
(def l3-target 20)                     ; M-marks-to-labels §L3 prereg
(def burn-in 5)                        ; futon2.aif.policy-precision default-min-history

(def core-tags #{:correction :approval})
(def second-string [:guidance :tactics :concern :fact :encouragement
                    :request :extension :procedural :hinge])

(defn -main [& args]
  (let [since (or (second (drop-while #(not= "--since" %) args)) l0-live)
        turns (:results (z1-views/operator-turns {:since since :limit 3000}))
        ;; L0 stamps :marks into the body; the Z1 view carries :marks + :tags
        ;; through since 2026-07-12. Count EVENTS only (mentions excluded).
        marked (for [t turns
                     m (or (:marks t) [])
                     :when (= "event" (name (or (:verdict m) "event")))]
                 (assoc m :turn-id (:id t) :mission (:clocked-mission t)))
        by-tag (frequencies (map keyword (mapcat :tags turns)))
        core-events (filter #(core-tags (keyword (or (:type %) :none))) marked)
        ref-bearing (filter :ref core-events)
        deposit-refs (filter #(some-> (:ref %) (str/starts-with? "ft-")) ref-bearing)
        pre-l0 (edn/read-string (slurp (str lab "/b1-live-marks.edn")))
        per-mission (frequencies (keep :mission core-events))
        second-counts (into {} (for [t second-string
                                     :let [n (get by-tag t 0)]
                                     :when (pos? n)] [t n]))
        report {:since since
                :store "laptop :7073 (lucy NOT counted; split-store fact)"
                :label-mint {:ref-bearing-core-marks (count ref-bearing)
                             :deposit-refs (count deposit-refs)
                             :target l3-target
                             :unlocks "M-marks-to-labels L3 measurement gate"}
                :gamma-burn-in {:per-mission per-mission
                                :target burn-in
                                :unlocks "γ(mission) leaves uniform prior (R14)"}
                :second-string {:counts second-counts
                                :note "no targets declared — usage argues promotion"}
                :pre-l0-grandfathered (count (:events pre-l0))}]
    (if (some #{"--edn"} args)
      (prn report)
      (do
        (println "MARKS QUOTA — store truth, laptop :7073, since" since)
        (println (format "  label-mint : %d/%d ref-bearing ✓/✘ (%d on ft-deposits) → unlocks L3"
                         (count ref-bearing) l3-target (count deposit-refs)))
        (if (seq per-mission)
          (doseq [[m n] (sort-by (comp - val) per-mission)]
            (println (format "  γ burn-in  : %s %d/%d" m n burn-in)))
          (println (format "  γ burn-in  : no core marks on clocked missions yet (need %d/mission)" burn-in)))
        (if (seq second-counts)
          (println "  2nd string :" (str/join " · " (map (fn [[t n]] (str (name t) " " n)) second-counts)))
          (println "  2nd string : none yet (counts only — no targets by design)"))
        (println (format "  (pre-L0 grandfathered events: %d — see b1-live-marks.edn)"
                         (count (:events pre-l0))))))))

(apply -main *command-line-args*)
