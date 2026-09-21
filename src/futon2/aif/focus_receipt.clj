(ns futon2.aif.focus-receipt
  "Record-only focus discovery from frozen inputs. Never supplies scoring C."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity])
  (:import [java.time Instant]))

(load-identity/register! *ns* *file*)

(defn- absent [reason] {:status :absent :reason reason})
(defn- instant [s] (Instant/parse s))
(defn- at-or-before? [a b] (not (.isAfter (instant a) (instant b))))
(defn read-inputs []
  (try
    (json/parse-string (slurp (io/resource "wm/focus/commit-facets-v1.json")) true)
    (catch Exception _ (absent :discovery-inputs-unavailable))))

(defn- facets [paths]
  (let [wm #"(^|/)(wm-contract|WarMachine|aif)(/|$)|(^|/)(war_machine|wm_|M-war-machine|M-wm-|M-G-wm|M-aif-policy)"
        apm #"(^|/)(apm|apm-lean)(/|$)|(^|/)(M-apm-|apm_|apm-|countdown_manifest)|^problems/"
        result (cond-> #{} (some #(re-find wm %) paths) (conj "WM")
                       (some #(re-find apm %) paths) (conj "APM"))]
    (if (seq result) result #{"other/unattributed"})))

(defn discover
  "Recompute path credit inside the frozen window, with no current git/store reads.
   Previous focus persists: this slice has no completion/transition authority."
  [inputs as-of previous]
  (let [window (last (sort-by :from
                             (filter #(and (at-or-before? (:from %) as-of)
                                           (at-or-before? as-of (:valid-through %))) (:windows inputs))))
        rows (filterv #(and (at-or-before? (:from window) (:at %))
                           (at-or-before? (:at %) (:until window))
                           (at-or-before? (:at %) as-of)) (:commits window))
        credits (reduce (fn [acc row]
                          (let [fs (facets (:paths row))]
                            (reduce #(update %1 %2 (fnil + 0) (/ 1 (count fs))) acc fs))) {} rows)
        best (when (seq credits) (apply max (vals credits)))
        winners (vec (sort (for [[f c] credits :when (= c best)] f)))
        discovered (when (and (= 1 (count winners)) (not= "other/unattributed" (first winners))) (first winners))
        focus (or (:focus previous) discovered)
        edges (filterv #(at-or-before? (:effective-from %) as-of) (:facet-edges inputs))]
    {:status (if (and window focus) :discovered :unknown)
     :reason (cond (nil? window) :discovery-window-unavailable
                   (nil? focus) :no-unique-attributed-focus)
     :focus (when window focus) :as-of as-of
     :window (if window (assoc (dissoc window :commits)
                              :source-until (:until window)
                              :until (if (at-or-before? as-of (:until window)) as-of (:until window)))
                 (absent :discovery-window-unavailable))
     :commit-count (count rows) :facet-credit credits :commits rows
     :previous-focus (or previous (absent :previous-focus-not-retained))
     :completion (absent :completion-authority-not-consumed)
     :transition {:status :held :reason :record-only-no-transition-authority}
     :focus-origin (if (:focus previous) :retained-unfinished-focus :commit-facets)
     :facet-graph {:active (if focus [focus] [])
                   :background (vec (sort (set (keep (fn [e]
                                                      (cond (= focus (:from e)) (:to e)
                                                            (= focus (:to e)) (:from e))) edges))))
                   :edges edges}}))

(defn- classification [inputs discovery as-of candidate]
  (let [id (:id candidate) target (:target id)
        relation (first (filter #(and (= target (:target %)) (at-or-before? (:effective-from %) as-of)) (:relations inputs)))
        facets (set (concat (get-in discovery [:facet-graph :active]) (get-in discovery [:facet-graph :background])))
        eligible (and (= :discovered (:status discovery)) (:source relation)
                      (contains? #{"focus" "associated" "useful-elsewhere"} (:relation relation))
                      (or (= "useful-elsewhere" (:relation relation)) (facets (:facet relation))))
        class (if eligible (keyword (:relation relation)) :unknown)
        node (when (string? target) (subs target (if (.startsWith ^String target "M-") 2 0)))]
    {:candidate-id id :target target :class class
     :relation (if eligible relation (absent (if relation :focus-not-established :relation-not-declared)))
     :embedding (if (some #{node} (get-in inputs [:embedding :nodes]))
                  {:status :present :node node :authority :presence-only}
                  (absent :embedding-node-not-retained))
     :outcome (absent :attested-outcome-not-inferred-from-prediction)}))

(defn build [decision inputs {:keys [as-of previous-focus]}]
  (let [discovery (discover inputs as-of previous-focus)
        candidates (get-in decision [:selection-certificate :candidates])]
    {:schema :wm/focus-receipt-v1 :mode :record-only
     :inputs inputs :inputs-sha256 (identity/digest inputs)
     :context {:as-of as-of :previous-focus previous-focus}
     :rule (:rule inputs) :heads (:heads inputs) :discovery discovery
     :candidates (mapv #(classification inputs discovery as-of %) candidates)
     :global-preference (assoc (:global-preference inputs)
                               :temporal-status (if (and (get-in inputs [:global-preference :effective-from])
                                                         (at-or-before? (get-in inputs [:global-preference :effective-from]) as-of))
                                                  :available :retrospective-ruling))
     :outcome-domain {:id :attested-increment-or-known-typed-failure-v1
                      :map {:focus :attested-focus-increment
                            :associated :attested-associated-increment
                            :useful-elsewhere :attested-useful-elsewhere-increment
                            :known-failure :observed-typed-nondelivery}
                      :unobserved (absent :observation-is-not-a-valued-outcome)
                      :unrepresented-class-mass {:status :held :reason :outcome-kernel-unavailable
                                                 :declared-masses (get-in inputs [:global-preference :masses])}}
     :attestation (absent :attestation-join-not-wired)
     :kernel (absent :predictive-attestation-kernel-not-declared)
     :local-C {:status :held :reason :conditional-outcome-kernel-unavailable}}))

(defn attach
  ([decision] (attach decision (read-inputs) {:as-of (str (Instant/now))}))
  ([decision inputs context]
   (assoc-in decision [:selection-certificate :focus-receipt] (build decision inputs context))))

(defn valid? [decision receipt]
  (try (= receipt (build decision (:inputs receipt) (:context receipt)))
       (catch Exception _ false)))
