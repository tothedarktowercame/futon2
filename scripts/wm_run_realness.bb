#!/usr/bin/env bb
;; wm_run_realness.bb — is this run REAL, on its own records?
;;
;; Joe's ruling, 2026-09-19 (T-wm-excessive-guardrails-19092026 addendum):
;; guards that stop runs are not wanted while the machine is tuned; what IS
;; wanted is validation that a run is a real AIF run rather than a facade —
;; something that claims to be AIF but computes none of its quantities.
;;
;; This reads the channel-lane trace records (data/wm-trace/wm-trace-DATE.edn)
;; and reports, per tick, which AIF quantities were actually computed and
;; with what provenance. It VETOES NOTHING — it is evidence, run after the
;; fact or over history.
;;
;;   bb scripts/wm_run_realness.bb                 # newest trace file
;;   bb scripts/wm_run_realness.bb 2026-09-12      # that date
;;   bb scripts/wm_run_realness.bb path/to/file.edn
;;
;; v1 limitation, stated: this covers the channel lane. The cascade lane's
;; decision certificates (:c :status :derived / :derived-no-overlap,
;; :rates-provenance — WIRE-3/WIRE-5) live on their own records and are the
;; natural v2.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.string :as str])

(def trace-dir (str (System/getProperty "user.home") "/code/futon2/data/wm-trace"))

(defn resolve-file [arg]
  (cond
    (nil? arg)
    (->> (.listFiles (io/file trace-dir))
         (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
         (sort-by #(.getName %))
         last)

    (re-matches #"\d{4}-\d{2}-\d{2}" arg)
    (io/file trace-dir (str "wm-trace-" arg ".edn"))

    :else (io/file arg)))

(defn read-records [file]
  (edn/read-string {:default (fn [_tag value] value)}
                   (str "[" (slurp file) "]")))

;; Each check: [label present? provenance-string]. A quantity is LIVE when the
;; record carries it non-empty; absence is reported, never punished.
(defn checks [r]
  (let [d (:decision r)
        ec (:enumeration-completeness d)]
    [["free-energy"        (some? (:free-energy r))
      nil]
     ["prediction-errors"  (and (coll? (:prediction-errors r))
                                (seq (:prediction-errors r)))
      nil]
     ["precision-state"    (some? (:precision-state r))
      nil]
     ["policy-precision"   (some? (:policy-precision-state r))
      nil]
     ["F_pi"               (and (map? (:f-pi-by-candidate-id r))
                                (seq (:f-pi-by-candidate-id r)))
      (some-> (:f-pi-provenance r) pr-str)]
     ["observation"        (some? (:observation r))
      nil]
     ["ranked-actions"     (and (coll? (:ranked-actions r))
                                (seq (:ranked-actions r)))
      (str (count (:ranked-actions r)) " candidates")]
     ["tau"                (some? (:tau d))
      (some-> (:tau-source d) pr-str)]
     ["enumeration (U37)"  (some? ec)
      (if ec
        (pr-str (:verdict ec))
        ;; trace.clj's reader rule: no key must be distinguishable from
        ;; "checked and complete" — absent means the flag never put one on.
        "absent: flag off or pre-U37 record")]]))

(defn report! [file]
  (let [recs (read-records file)]
    (println (str "== " (.getName (io/file file)) " — " (count recs) " tick(s)"))
    (doseq [[i r] (map-indexed vector recs)]
      (let [cs (checks r)
            live (filter second cs)
            absent (remove second cs)]
        (let [wm (:wm-version r)
              route (map :node (:wm/route r))]
          (println (format "\n-- tick %d @ %s (sha %s, author %s, trigger %s)"
                           i (:timestamp r)
                           (some-> (:git-sha wm) (subs 0 8))
                           (:author wm) (:trigger wm)))
          (println (str "   route: " (str/join " -> " (map name route)))))
        (println (format "   live %d/%d%s" (count live) (count cs)
                         (if (seq absent)
                           (str " — ABSENT: " (str/join ", " (map first absent)))
                           " — all quantities computed")))
        (doseq [[label ok? prov] cs
                :when (and ok? prov)]
          (println (format "   %-18s %s" label prov)))))
    (let [all (mapcat checks recs)
          by-label (group-by first all)]
      (println "\n== summary")
      (doseq [[label rows] by-label
              :let [n (count (filter second rows))]]
        (println (format "   %-18s live in %d/%d ticks" label n (count rows)))))))

(let [file (resolve-file (first *command-line-args*))]
  (if (and file (.exists file))
    (report! file)
    (binding [*out* *err*]
      (println "no trace file found for" (pr-str (first *command-line-args*)))
      (System/exit 1))))
