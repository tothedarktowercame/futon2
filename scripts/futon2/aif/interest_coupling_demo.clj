(ns futon2.aif.interest-coupling-demo
  "M-interest-network-coupling — WM-AIF coupling differential demonstration.

   Shows HOW interest-network signals (the real eoi-derived projection) change the
   War Machine's EFE rankings, via Route-1 per-candidate enrichment
   (:intrinsic-value -> G-risk, :structural-pressure-per-action -> G-total).
   Uses the REAL efe (pure fns). Transient JVM only — no serving-JVM / XTDB contact.

   Run:  cd futon2 && clojure -M -m futon2.aif.interest-coupling-demo"
  (:require [futon2.aif.efe :as efe]
            [futon2.aif.belief :as belief]))

;; --- representative WM state (observation shape copied from efe_test/healthy-obs) ---
(def observation
  {:loop-health 0.9 :support-coverage 0.9 :attack-coverage 0.9
   :mission-health 0.7 :stack-pct 0.20 :consulting-pct 0.25
   :portfolio-pct 0.25 :mathematics-pct 0.20 :active-repo-ratio 0.8
   :sorry-count-norm 0.1 :coupling-density 0.2 :ticks-firing-ratio 0.0
   :depositing-signal 0.1})

(def targets [:m-interim-director :m-weird-modernism :sorry-r3d-attribution :p-anamnesis])

(def state {:observation observation
            :belief (belief/initial-belief-state targets)})

;; --- a representative WM cycle's proposed candidate actions ---
(def candidates
  [{:type :no-op}
   {:type :open-mission  :target :m-interim-director}
   {:type :open-mission  :target :m-weird-modernism}
   {:type :address-sorry :target :sorry-r3d-attribution}
   {:type :fire-pattern  :target :p-anamnesis}])

;; --- interest-network adapter (Route 1: per-candidate bias) -----------------
;; Bridge (Q2, explicit for this real-data demo):
;;   :m-interim-director EXPRESSES the livelihood / free-solo through-line
;;   (sharing-vs-commerce, bootstrap-economic-self-financing, work-as-me-vs-hyperreal).
;; Signals (from the real projection interest-network-standing-v1.edn):
;;   evt-close-3 :node/hyperreal-enterprises :strengthened
;;   evt-close-1 scenario-c-vsatlatarium     :strengthened
;;   => mission in-progress (READY-TO-CLOSE, not closed) + worked today (high activation).
(def w-finish 0.15)   ; hand-set (Q4), tunable
(def w-real   1.0)    ; hand-set (Q4), tunable

(def interest-bias
  {:m-interim-director
   {:intrinsic-value                (* w-finish 0.9 1.0)   ; activation 0.9 * unfinished 1
    :structural-pressure-per-action (* w-real   0.8 0.9)   ; latent 0.8 * actualization-deficit 0.9
    :provenance
    {:finish    "in-progress (READY-TO-CLOSE, not closed) + worked today (activation 0.9); evt-close-3 hyperreal :strengthened, evt-close-1 scenario-c :strengthened"
     :make-real "livelihood/free-solo through-line: high corpus-degree, under-actualized (the FUTON frontier)"}}})

(defn enrich [cands bias]
  (mapv (fn [c]
          (if-let [b (get bias (:target c))]
            (assoc c :intrinsic-value (:intrinsic-value b)
                     :structural-pressure-per-action (:structural-pressure-per-action b))
            c))
        cands))

;; --- run + report -----------------------------------------------------------
(defn action-label [a]
  (str (name (:type a)) (when (:target a) (str " " (:target a)))))

(defn print-ranking [title ranked]
  (println (str "\n" title))
  (println (format "  %-4s %-32s %9s %9s %9s %9s %9s"
                   "rank" "action" "G-total" "G-risk" "G-struct" "G-info" "G-ambig"))
  (doseq [r ranked]
    (println (format "  %-4d %-32s %9.4f %9.4f %9.4f %9.4f %9.4f"
                     (:rank r) (action-label (:action r))
                     (double (:G-total r)) (double (:G-risk r))
                     (double (:G-structural-pressure r))
                     (double (:G-info r)) (double (:G-ambiguity r))))))

(defn -main [& _]
  (let [baseline (efe/rank-actions state candidates)
        enriched (efe/rank-actions state (enrich candidates interest-bias))]
    (println "=== Interest-Network -> WM-AIF coupling differential ===")
    (println "Route 1 (per-candidate enrichment). Real efe; transient JVM; no live stack.")
    (println "\nInterest bias applied (with provenance):")
    (doseq [[eid b] interest-bias]
      (println " " eid)
      (println (format "    :intrinsic-value                %.4f  (finish-what-you-started)"
                       (double (:intrinsic-value b))))
      (println (format "    :structural-pressure-per-action %.4f  (make-a-long-standing-interest-real)"
                       (double (:structural-pressure-per-action b))))
      (println "    finish    <-" (get-in b [:provenance :finish]))
      (println "    make-real <-" (get-in b [:provenance :make-real])))
    (print-ranking "BASELINE (no interest coupling):" baseline)
    (print-ranking "ENRICHED (interest-coupled):" enriched)
    (println "\nDIFFERENTIAL (by action):")
    (let [b-by (into {} (map (juxt #(action-label (:action %)) identity)) baseline)
          e-by (into {} (map (juxt #(action-label (:action %)) identity)) enriched)]
      (doseq [k (sort (keys b-by))]
        (let [b (b-by k) e (e-by k)]
          (println (format "  %-32s rank %d->%d   G-total %+.4f->%+.4f  (%+.4f)"
                           k (:rank b) (:rank e)
                           (double (:G-total b)) (double (:G-total e))
                           (double (- (:G-total e) (:G-total b))))))))
    (println "\nReading: the top action may be unchanged; coupling shifts the interest-touched")
    (println "candidate's G-total (intrinsic-value -> G-risk; structural-pressure -> G-total),")
    (println "re-ordering the tail and changing margins -- the forward-model effect, documented.")))
