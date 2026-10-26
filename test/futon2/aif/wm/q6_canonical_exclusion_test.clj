(ns futon2.aif.wm.q6-canonical-exclusion-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.outer-canonical-g :as canonical-g]
            [futon2.aif.outer-canonical-selection :as canonical-selection]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.q8-frontier :as q8]
            [futon2.aif.selected-outer-cascade-policy :as selected]
            [futon2.aif.selected-outer-cascade-policy-test]
            [futon2.aif.wm.cascade-decision :as cascade-decision]
            [futon2.aif.wm.family-selection :as family-selection]))

(defn- production-carriers []
  ((var-get (ns-resolve 'futon2.aif.selected-outer-cascade-policy-test
                        'production-carriers))))

(defn- fixture []
  (let [{:keys [menu evidence objective]} (production-carriers)]
    {:menu menu
     :evidence evidence
     :objective objective
     :frontier (canonical-g/produce menu)}))

(defn- policy-key [menu policy-id]
  (get-in (first (filter #(= policy-id (:policy-id %)) (:entries menu)))
          [:action :policy-key]))

(defn- predecessor [outcome policy-key input-digest]
  (previous-run/seal
   {:schema previous-run/predecessor-schema
    :status :present
    :previous-run/id "previous"
    :previous {:outcome {:status :present :outcome outcome}
               :choice {:status :present :policy-key policy-key
                        :target "T" :policy-id "P"}
               :input-digest input-digest}}))

(defn- selected-case [pred draw]
  (let [{:keys [menu evidence objective frontier]} (fixture)
        exclusion (previous-run/canonical-exclusion pred menu frontier)
        selection (canonical-selection/evaluate
                   frontier menu draw (:excluded-keys exclusion))
        sealed (selected/seal-canonical menu evidence objective frontier selection
                                        (:carrier exclusion))
        decision (:decision (selected/decision sealed))]
    {:menu menu :frontier frontier :exclusion exclusion :selection selection
     :sealed sealed :decision decision}))

(deftest grounded-progress-is-a-changed-run-outcome
  ;; The retained closed event for run 2026-10-26-8be8ff03… in
  ;; data/wm-runs/run-stream.edn currently records the pre-R22 image:
  ;; {:outcome :invalid, :raw :grounded-progress}.
  (is (= {:status :present
          :outcome :changed
          :raw :grounded-progress}
         (previous-run/typed-outcome
          :grounded-progress nil {:status :selected :action {:target "T"}}))))

(deftest canonical-selection-excludes-the-refused-previous-choice
  (let [{:keys [menu frontier]} (fixture)
        initial (canonical-selection/evaluate frontier menu 0.0)
        refused-id (get-in initial [:action :policy-id])
        refused-key (policy-key menu refused-id)
        digest (previous-run/canonical-selection-inputs-digest menu frontier)
        pred (predecessor :refused refused-key digest)
        {:keys [exclusion selection sealed decision]} (selected-case pred 0.0)
        target (:target-id sealed)
        cascade (q8/cascade decision pred target)]
    (is (= [:excluded :previous-choice-refused-on-same-inputs 1]
           ((juxt :status :reason :removed) (:carrier exclusion))))
    (is (not= refused-id (get-in selection [:action :policy-id])))
    (is (nil? (selected/verify sealed)))
    (is (= (:carrier exclusion)
           (get-in decision [:selection-certificate :q6])))
    (is (previous-run/intact? (get-in decision [:selection-certificate :q6])))
    (is (= :present (:status cascade)))
    (is (= #{refused-key} (:requirement-excluded cascade)))))

(deftest excluding-the-only-policy-is-a-typed-repeat-refusal
  (let [{:keys [menu evidence objective]} (fixture)
        entry (first (:entries menu))
        menu (assoc menu :entries [entry])
        menu (assoc menu :menu-pin (family-selection/sealed-menu-pin menu))
        frontier (canonical-g/produce menu)
        key (get-in entry [:action :policy-key])
        digest (previous-run/canonical-selection-inputs-digest menu frontier)
        pred (predecessor :refused key digest)
        exclusion (previous-run/canonical-exclusion pred menu frontier)
        selection (canonical-selection/evaluate frontier menu nil
                                                (:excluded-keys exclusion))
        refused (selected/seal-canonical menu evidence objective frontier selection
                                         (:carrier exclusion))
        projected (cascade-decision/cascade-decision
                   {} {:selected-outer-cascade-policy refused})]
    (is (= [:no-choice :repeat-choice-after-refusal]
           ((juxt :status :reason) selection)))
    (is (= {:condition :selection-inputs-change
            :input-digest digest
            :excluded-key key}
           (get-in exclusion [:carrier :rearm])))
    (is (= :refused (get-in exclusion [:carrier :status])))
    (is (= (:carrier exclusion) (:refusal exclusion)))
    (is (= (:carrier exclusion)
           (get-in projected [:decision :selection-certificate :q6])))))

(deftest legacy-nil-digest-warms-the-next-terminal
  (let [{:keys [menu]} (fixture)
        key (get-in (first (:entries menu)) [:action :policy-key])
        pred (predecessor :refused key nil)
        {:keys [exclusion selection decision]} (selected-case pred 0.0)
        terminal (previous-run/terminal
                  {:run-id "current" :started-at "2026-10-26T00:00:00Z"
                   :decision decision :outcome :already-satisfied})]
    (is (= [:not-recomputable :previous-input-digest-absent 0]
           ((juxt :status :reason :removed) (:carrier exclusion))))
    (is (= :selected (:status selection)))
    (is (string? (:input-digest terminal)))))

(deftest already-satisfied-predecessor-carries-an-empty-exclusion
  (let [{:keys [menu frontier]} (fixture)
        key (get-in (first (:entries menu)) [:action :policy-key])
        digest (previous-run/canonical-selection-inputs-digest menu frontier)
        pred (predecessor :already-satisfied key digest)
        {:keys [exclusion sealed decision]} (selected-case pred 0.0)
        cascade (q8/cascade decision pred (:target-id sealed))]
    (is (= [:not-triggered :previous-outcome-not-refused 0]
           ((juxt :status :reason :removed) (:carrier exclusion))))
    (is (= :present (:status cascade)))
    (is (= #{} (:requirement-excluded cascade)))))

(deftest canonical-digest-does-not-read-the-tie-draw
  (let [{:keys [menu frontier]} (fixture)
        reversed (assoc menu :entries (vec (rseq (:entries menu))))]
    (is (= (previous-run/canonical-selection-inputs-digest menu frontier)
           (previous-run/canonical-selection-inputs-digest menu frontier)))
    ;; The digest is a function of the key set, not of the sealing process's
    ;; printer: under a REPL's *print-length* every key once printed alike,
    ;; so the order, and the digest, followed menu order.
    (is (= (previous-run/canonical-selection-inputs-digest menu frontier)
           (binding [*print-length* 1 *print-level* 1 *print-namespace-maps* false]
             (previous-run/canonical-selection-inputs-digest reversed frontier))))
    (is (= (canonical-selection/evaluate frontier menu 0.1 #{})
           (canonical-selection/evaluate frontier menu 0.1 nil)))
    (is (not= (canonical-selection/evaluate frontier menu 0.1 nil)
              (canonical-selection/evaluate frontier menu 0.9 nil)))))

(deftest canonical-q6-tampering-is-refused-at-both-seams
  (let [{:keys [menu frontier]} (fixture)
        key (get-in (first (:entries menu)) [:action :policy-key])
        digest (previous-run/canonical-selection-inputs-digest menu frontier)
        pred (predecessor :already-satisfied key digest)
        {:keys [sealed decision]} (selected-case pred 0.0)
        tampered-q6 (assoc (:q6-exclusion sealed) :excluded-keys [key])
        tampered-sealed (-> sealed
                            (assoc :q6-exclusion tampered-q6)
                            (assoc :carrier-pin nil))
        tampered-sealed (assoc tampered-sealed :carrier-pin
                               (selected/carrier-pin tampered-sealed))
        tampered-decision (assoc-in decision [:selection-certificate :q6]
                                    tampered-q6)]
    (is (= :selected-policy-q6-carrier-tampered
           (selected/verify tampered-sealed)))
    (is (= :q6-carrier-absent-or-tampered
           (:reason (q8/cascade tampered-decision pred (:target-id sealed)))))))
