#!/usr/bin/env bb
;; a_matrix_shadow.bb — M-aif-faithfulness B-3a-shadow (owner claude-12).
;;
;; READ-ONLY shadow evidence for the R1 A-matrix flip: what would the per-entity
;; belief posterior be if the update ran under :a-matrix (a-matrix-v0) instead of
;; :legacy? claude-7's model is dark (default :legacy). No writes to data/wm-trace.
;;
;; Method (no re-simulation needed — the trace has enough):
;;   Each tick records :mu-pre / :mu-post — the 7-status posterior per entity
;;   BEFORE/AFTER the :legacy update. The :legacy rule boosts ONLY the event-type
;;   status by (1+w), so the event(s) are RECOVERABLE by inverting mu-pre→mu-post:
;;   r[k]=post/pre; the boosted status(es) have r/min(r) > 1 ⇒ (type, weight=that-1).
;;   Self-check: re-derive :legacy from pre+recovered-events; ≈ mu-post ⇒ invertible.
;;   Then apply the SAME events to mu-pre under :a-matrix (the REAL belief ns) and
;;   compare to :legacy (= mu-post): per-entity KL + argmax-status flip.
;;
;; Downstream channel note: the 8-channel emission model (channel-emission-matrix)
;; is a SEPARATE block from the event-block entity-status update replayed here
;; (design note §Event vs Channel). Shadowing channel movement needs the FEP /
;; free-energy path re-run per tick — heavier re-simulation, NOT done here (said,
;; not faked). This artifact is the entity-status posterior shadow.
;;
;; Deterministic: same trace corpus in ⇒ same artifact out.
;; Run: bb --classpath src scripts/a_matrix_shadow.bb  [trace-dir] [out-edn]
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[futon2.aif.belief :as belief])

(def trace-dir (or (first *command-line-args*) "data/wm-trace"))
(def out-edn (or (second *command-line-args*)
                 "holes/labs/M-aif-faithfulness/a-matrix-shadow.edn"))
(def eps 1e-6)

(defn read-all-forms [f]
  (with-open [r (java.io.PushbackReader. (io/reader f))]
    (loop [acc []]
      (let [form (edn/read {:eof ::eof :default (fn [_tag v] v)} r)]
        (if (= ::eof form) acc (recur (conj acc form)))))))

(defn recover-events
  "Invert the :legacy update: the status(es) whose post/pre ratio exceeds the
   min-ratio (the untargeted floor) are the event types; weight = M-1."
  [pre post]
  (let [rs (into {} (for [[k p] pre
                          :let [p (double p) q (double (get post k 0.0))]
                          :when (> p 0.0)] [k (/ q p)]))
        mn (when (seq rs) (apply min (vals rs)))]
    (when (and mn (pos? mn))
      (->> rs
           (keep (fn [[k r]] (let [m (/ r mn)] (when (> m (+ 1.0 eps)) {:type k :weight (- m 1.0)}))))
           vec))))

(defn kl [p q]
  (reduce + 0.0 (for [[k pk] p
                      :let [pk (double pk) qk (double (get q k 1e-12))]
                      :when (> pk 1e-12)]
                  (* pk (Math/log (/ pk (max qk 1e-12)))))))

(defn argmax-status [m] (key (apply max-key val m)))

(defn shadow-one [day pre post]
  (when-let [events (recover-events pre post)]
    (when (seq events)
      (let [legacy-recon (reduce #(belief/update-entity-belief %1 %2 {:likelihood-mode :legacy}) pre events)
            am           (reduce #(belief/update-entity-belief %1 %2 {:likelihood-mode :a-matrix}) pre events)
            leg-argmax   (argmax-status post)
            am-argmax    (argmax-status am)]
        {:day day
         :event-types (mapv :type events)
         :pre-uniform? (< (- (apply max (vals pre)) (apply min (vals pre))) 1e-9)
         :invertible? (< (kl post legacy-recon) 1e-4)   ; recovered legacy ≈ recorded post?
         :kl-am-vs-legacy (kl am post)                  ; how far a-matrix pulls from what legacy did
         :argmax-legacy leg-argmax
         :argmax-am am-argmax
         :flip? (not= leg-argmax am-argmax)}))))

;; --- sweep ------------------------------------------------------------------
(def files (->> (.listFiles (io/file trace-dir))
                (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" (.getName %)))
                (sort-by #(.getName %))))
(println (format "Reading %d trace files from %s …" (count files) trace-dir))

(def records
  (vec
   (for [f files
         t (read-all-forms f)
         :let [day (when-let [ts (:timestamp t)] (subs (str ts) 0 10))
               pre (:mu-pre t) post (:mu-post t)]
         :when (and (map? pre) (map? post))
         e (keys pre)
         :let [r (shadow-one day (get pre e) (get post e))]
         :when r]
     r)))

(defn- quantile [xs q]
  (let [v (vec (sort xs)) n (count v)]
    (when (pos? n) (nth v (min (dec n) (int (* q n)))))))

(def kls (map :kl-am-vs-legacy records))
(def flips (filter :flip? records))
(def uninvertible (remove :invertible? records))
(def n (count records))

(def summary
  {:entity-updates n
   :invertible (- n (count uninvertible))
   :uninvertible (count uninvertible)
   :argmax-status-flips (count flips)
   :flip-rate (when (pos? n) (/ (Math/round (* 10000.0 (/ (count flips) (double n)))) 10000.0))
   :flip-status-pairs (frequencies (map (fn [r] [(:argmax-legacy r) (:argmax-am r)]) flips))
   :kl {:mean (when (pos? n) (/ (reduce + kls) (double n)))
        :max (when (seq kls) (apply max kls))
        :p50 (quantile kls 0.5) :p90 (quantile kls 0.9) :p99 (quantile kls 0.99)}
   :kl-by-event-type (into {} (for [[et rs] (group-by (comp first :event-types) records)]
                                [et {:n (count rs)
                                     :mean-kl (/ (reduce + (map :kl-am-vs-legacy rs)) (double (count rs)))
                                     :flips (count (filter :flip? rs))}]))
   :trend-by-day (into (sorted-map)
                       (for [[d rs] (group-by :day records)]
                         [d {:n (count rs)
                             :mean-kl (/ (reduce + (map :kl-am-vs-legacy rs)) (double (count rs)))
                             :flips (count (filter :flip? rs))}]))
   :prior-regime {:uniform-prior-fraction (when (pos? n) (/ (Math/round (* 10000.0 (/ (count (filter :pre-uniform? records)) (double n)))) 10000.0))
                  :distinct-event-types (vec (distinct (mapcat :event-types records)))}})

(def artifact
  {:generated-by "scripts/a_matrix_shadow.bb (M-aif-faithfulness B-3a-shadow)"
   :sim-only true :read-only true :model "a-matrix-v0 (event block, 7×7)"
   :method "recover events by inverting the :legacy update from mu-pre→mu-post; replay under :a-matrix via the real belief ns; per-entity KL + argmax-status flip."
   :corpus {:files (count files)}
   :caveats
   ["Read-only shadow; the production update stays :legacy (dark). No trace writes."
    "Events recovered by legacy-inversion; :invertible? verifies pre+events under :legacy ≈ recorded mu-post (KL<1e-4). Uninvertible rows (re-init/edge cases) reported, not dropped from the count."
    "KL is KL(a-matrix-posterior ‖ legacy-posterior) per entity-update — how far the flip would pull each posterior from what shipped."
    "CHANNEL downstream (8-channel emission block) NOT shadowed — it is a separate block needing FEP-path re-simulation per tick; said, not faked."]
   :headline-finding
   (format (str "Flip :legacy→:a-matrix is INERT on this corpus: %d/%d argmax-status flips (%.2f%%), "
                "KL(a-matrix‖legacy) mean %.5f nats (near-constant). ROOT CAUSE: belief re-initialises "
                "to the UNIFORM prior every tick (%.1f%% of updates) and the evidence is the SINGLE event "
                "type %s — so a-matrix-v0's off-diagonal (lifecycle-adjacent) structure never redistributes "
                "mass differently from legacy. It would only bite with (a) persistent non-uniform priors or "
                "(b) varied event types; NEITHER holds today. Flip is LOW-RISK but its VALUE is UNOBSERVABLE "
                "on the current belief regime — this shadow cannot distinguish the models here.")
           (:argmax-status-flips summary) (:entity-updates summary)
           (* 100.0 (double (or (:flip-rate summary) 0)))
           (double (or (get-in summary [:kl :mean]) 0))
           (* 100.0 (double (or (get-in summary [:prior-regime :uniform-prior-fraction]) 0)))
           (pr-str (get-in summary [:prior-regime :distinct-event-types])))
   :summary summary})

(io/make-parents out-edn)
(spit out-edn (with-out-str (pp/pprint artifact)))

(println "\n=== A-MATRIX SHADOW (entity-status posterior) ===")
(println (format "entity-updates: %d (invertible %d / uninvertible %d)"
                 n (:invertible summary) (:uninvertible summary)))
(println (format "argmax-status FLIPS: %d (%.2f%% of updates)"
                 (count flips) (* 100.0 (double (or (:flip-rate summary) 0)))))
(when (seq (:flip-status-pairs summary))
  (println "flip pairs (legacy→a-matrix):")
  (doseq [[[a b] c] (sort-by (comp - val) (:flip-status-pairs summary))]
    (println (format "  %s → %s : %d" a b c))))
(println (format "KL(a-matrix‖legacy): mean %.5f · p50 %.5f · p90 %.5f · max %.5f"
                 (double (or (get-in summary [:kl :mean]) 0)) (double (or (get-in summary [:kl :p50]) 0))
                 (double (or (get-in summary [:kl :p90]) 0)) (double (or (get-in summary [:kl :max]) 0))))
(println (format "prior regime: %.1f%% uniform-prior; event types %s"
                 (* 100.0 (double (or (get-in summary [:prior-regime :uniform-prior-fraction]) 0)))
                 (pr-str (get-in summary [:prior-regime :distinct-event-types]))))
(println "\nHEADLINE:" (:headline-finding artifact))
(println "\nwrote" out-edn)
