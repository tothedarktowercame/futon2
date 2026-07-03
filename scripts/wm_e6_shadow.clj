(ns wm-e6-shadow
  "M-evaluate-policies E6 — the risk/ambiguity MODE shadow comparison
   (INSTANTIATE §12; the evidence a future D5-flip decision rides on).

   SIM-ONLY, one-shot: runs the production judge ONCE (read-only against the
   stores, exactly the scans the hourly tick performs; no trace persisted, no
   enactment), then re-ranks the judge's OWN stashed inputs
   (`wm/!last-wm-inputs` — identical state + enriched candidates) under four
   configs:

     A0 = :hinge + :variance-sum        (production; also the re-rank sanity)
     B  = :kl    + :variance-sum        (D5a risk functional)
     C  = :hinge + :gaussian-entropy    (D5c — the intrinsic-audibility test)
     D  = :kl    + :gaussian-entropy    (the canonical-core preview)

   Opts fidelity: :time-pressure/:horizon-steps are read back from the
   production ranking's own entries; the star-map/gap-view opt enrichers are
   the judge's own (private fns reached by var — introspection, not surgery).
   Cascade placeholder rows are excluded from metrics (unscored by design).

   Usage: clojure -M -m wm-e6-shadow [days]   (default 14, the runner's own)
   Writes: holes/labs/M-evaluate-policies/e6-shadow.edn"
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.set :as cset]
            [futon2.aif.efe :as efe]
            [futon2.report.war-machine :as wm]))

(def out-file "holes/labs/M-evaluate-policies/e6-shadow.edn")

(defn- akey [e] [(get-in e [:action :type]) (get-in e [:action :target])])

(defn- ordinary [ranked] (vec (filter :G-risk ranked)))

(defn- spearman [xs ys]
  (let [ranks (fn [v]
                (let [order (sort-by second (map-indexed vector v))]
                  (persistent!
                   (reduce (fn [m [r [i _]]] (assoc! m i (double (inc r))))
                           (transient {}) (map-indexed vector order)))))
        rx (ranks xs) ry (ranks ys) n (count xs)
        mx (/ (reduce + (vals rx)) n) my (/ (reduce + (vals ry)) n)
        num (reduce + (map #(* (- (rx %) mx) (- (ry %) my)) (range n)))
        dx (Math/sqrt (reduce + (map #(Math/pow (- (rx %) mx) 2) (range n))))
        dy (Math/sqrt (reduce + (map #(Math/pow (- (ry %) my) 2) (range n))))]
    (if (and (pos? dx) (pos? dy)) (/ num (* dx dy)) ##NaN)))

(defn- sd [xs]
  (let [n (count xs) mu (/ (reduce + xs) n)]
    (Math/sqrt (/ (reduce + (map #(Math/pow (- % mu) 2) xs)) n))))

(defn- compare-config [base ranked]
  (let [bk (mapv akey base) rk-order (into {} (map-indexed (fn [i e] [(akey e) i]) ranked))
        shared (filterv rk-order bk)
        base-idx (into {} (map-indexed (fn [i e] [(akey e) i]) base))
        xs (mapv base-idx shared) ys (mapv rk-order shared)
        top10 (fn [r] (set (map akey (take 10 r))))]
    {:winner (akey (first ranked))
     :winner-changed? (not= (akey (first base)) (akey (first ranked)))
     :spearman-vs-A0 (when (seq shared) (spearman xs ys))
     :top10-jaccard (let [a (top10 base) b (top10 ranked)]
                      (/ (double (count (cset/intersection a b)))
                         (count (cset/union a b))))
     :n-rank-moves (count (remove #(= (base-idx %) (rk-order %)) shared))}))

(defn -main [& args]
  (let [days (if (seq args) (Integer/parseInt (first args)) 14)
        _ (println "E6 shadow: one read-only production judge run, days =" days "...")
        {:keys [judgement]} (wm/generate-war-machine days)
        {:keys [wm-state candidates]} @wm/!last-wm-inputs
        prod (ordinary (:ranked-actions judgement))
        tp (or (:time-pressure (first prod)) 0.0)
        hs (:horizon-steps (first prod))
        base-opts ((var-get #'wm/live-star-map-efe-opts)
                   ((var-get #'wm/live-gap-view-efe-opts)
                    {:time-pressure tp :horizon-steps hs}))
        rank (fn [extra] (ordinary (efe/rank-actions wm-state candidates
                                                     (merge base-opts extra))))
        a0 (rank {})
        b (rank {:risk-mode :kl})
        c (rank {:ambiguity-mode :gaussian-entropy})
        d (rank {:risk-mode :kl :ambiguity-mode :gaussian-entropy})
        ;; D5c audibility: within-tick dispersion of the ambiguity term
        amb-sd-varsum (sd (map :G-ambiguity a0))
        amb-sd-gauss (sd (map :G-ambiguity c))
        risk-sd-hinge (sd (map :G-risk a0))
        risk-sd-kl (sd (map :G-risk b))
        result
        {:generated-by "scripts/wm_e6_shadow.clj (M-evaluate-policies E6)"
         :sim-only true :days days
         :n-candidates (count a0)
         :sanity {:judge-winner (akey (first prod))
                  :a0-winner (akey (first a0))
                  :note "judge output passes anamnesis-tiebreak + open-mission filter after ranking; A0 is the raw rank — same scoring path, so B/C/D isolate the mode"}
         :configs {:B-kl-risk (compare-config a0 b)
                   :C-gaussian-ambiguity (compare-config a0 c)
                   :D-both (compare-config a0 d)}
         :dispersion {:ambiguity {:variance-sum amb-sd-varsum
                                  :gaussian-entropy amb-sd-gauss
                                  :audibility-ratio (when (pos? amb-sd-varsum)
                                                      (/ amb-sd-gauss amb-sd-varsum))}
                      :risk {:hinge risk-sd-hinge :kl risk-sd-kl
                             :ratio (when (pos? risk-sd-hinge)
                                      (/ risk-sd-kl risk-sd-hinge))}
                      :note "within-tick sd — the argmin-relevant unit (E4 reviewer note)"}
         :cascade-rows-excluded (- (count (:ranked-actions judgement)) (count prod))}]
    (io/make-parents out-file)
    (spit out-file (with-out-str (pp/pprint result)))
    (println "\n=== E6 SHADOW SUMMARY ===")
    (pp/pprint (dissoc result :generated-by))
    (println "\nWrote" out-file)))
