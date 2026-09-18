(ns futon2.aif.efe-certificate-test
  "WIRE-1 acceptance: the emission slice. rank-cascade-actions attaches a
  :certificate (Lean DarkTower/AIF/Certificates.lean GCertificate) to every
  ranked candidate, filled from inside the horizon-g-sparse evaluation;
  the scalar G is byte-identical to the pre-slice path; a refused
  computation emits no certificate — the refusal IS the record."
  (:require [clojure.test :refer [deftest is]]
            [clojure.set :as cset]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.efe :as efe]))

(def pattern-text "  + IF: ready and not blocked\n  + HOWEVER: stalled\n  + THEN: produce evidence\n  + BECAUSE: test\n")
(def produce-z-text "  + IF: ready and not blocked\n  + HOWEVER: stalled\n  + THEN: produce zed\n  + BECAUSE: test\n")

(defn- fixture
  []
  (let [p (m/interpret-pattern "p" "pattern" pattern-text)
        q0 (m/observed-belief #{"ready"})
        want #{["t" "evidence"]}
        spec {:want want :evidence #{} :lam 1 :mu 1 :zeroed #{}}
        universe (set (concat want #{"ready"} #{"evidence"} #{"stalled"}))
        candidates [{:kind :cascade-candidate :id :c1 :precedence [p]}
                    {:kind :cascade-candidate :id :c0 :precedence []}]]
    {:q0 q0 :spec spec :universe universe :candidates candidates :p p}))

(defn- candidate-tokens
  "Mirror of efe's private cascade-candidate-tokens, so the ground truth
  scores over the SAME universe rank-cascade-actions constructs (a different
  universe shifts G by Z, and the byte-identity claim would be vacuous)."
  [precedence]
  (reduce
   (fn [acc pattern]
     (reduce conj acc
             (concat (:produces pattern)
                     (mapcat (fn [clause] (concat (:present clause) (:absent clause)))
                             (get-in pattern [:guard :clauses])))))
   #{}
   precedence))

(defn- ground-truth-g
  "The pre-slice path, called directly: the same arguments rank-cascade-actions
  constructs (zero rates over the same universe), through horizon-g-sparse."
  [{:keys [q0 spec universe p]} T]
  (m/horizon-g-sparse {:rates (zipmap universe (repeat {:false-neg 0 :false-pos 0}))
                       :q0 q0
                       :precedence-fn (constantly [p])
                       :horizon T
                       :spec spec
                       :universe universe}))

(deftest certificate-present-and-arithmetic-exact
  (let [{:keys [q0 spec candidates]} (fixture)
        T 3
        want (:want spec)
        ranked (efe/rank-cascade-actions {:cascade-belief q0}
                                         candidates
                                         {:horizon-steps T
                                          :cascade-spec spec})
        expected-universe (into (candidate-tokens (mapcat :precedence candidates))
                                (concat (apply cset/union (keys q0)) want))]
    (is (vector? ranked))
    (is (= 2 (count ranked)))
    (doseq [entry ranked
            :let [cert (:certificate entry)]]
      (is (map? cert) "every ranked candidate carries a certificate")
      (is (= T (:horizon cert)))
      (is (= T (count (:steps cert))) "steps count equals the horizon (Certificates.lean shape)")
      (is (= (range 1 (inc T)) (mapv :tau (:steps cert))) "taus are 1..T, Lean's step indexing")
      (is (every? #(= :computed (:risk-status %)) (:steps cert)))
      (is (every? #(= 0 (:ambiguity %)) (:steps cert)))
      (is (every? #(= :reduced-identically-zero (:ambiguity-status %)) (:steps cert)))
      (is (every? #(= "identity-A-zero-rates" (:reduction %)) (:steps cert)))
      (is (= (:G-efe entry) (:total cert)) "certificate total is the emitted G")
      (is (= (:total cert)
             (reduce + 0.0 (map :risk (:steps cert))))
          "total is the exact sum of the recorded step risks (GCertificate.valid arithmetic)")
      (is (= :constant-spec (:c-form cert)))
      (is (true? (:rates-all-zero cert)) "read off the actual rates map of the run")
      (is (= (count expected-universe) (:universe-size cert)))
      (is (= {:value nil :status :not-in-scoring-opts} (:beta-declared cert))
          "beta is not in R5 scoring opts today; the absence is recorded, not defaulted")
      (is (= {:value 1 :status :declared-neutral} (:habit cert)))
      ;; WIRE-2: F is now computed on the tick (not a declared-neutral 0).
      ;; This fixture declares no :evidence, so the observed token set is
      ;; empty and the identity observation of nothing is impossible under
      ;; every rollout: F = ##Inf — COMPUTED and provenance-stamped.
      (is (= {:value ##Inf
              :status :computed
              :source :cascade-free-energy/policy-free-energy
              :tau T
              :observed-tokens #{}}
             (:f cert))))))

(deftest g-byte-identical-to-pre-slice
  (let [{:keys [q0 spec candidates p] :as fx} (fixture)
        T 3
        ranked (efe/rank-cascade-actions {:cascade-belief q0}
                                         candidates
                                         {:horizon-steps T
                                          :cascade-spec spec})
        expected (ground-truth-g (assoc fx
                                        :universe (into (candidate-tokens [p])
                                                        (concat (apply cset/union (keys q0))
                                                                (:want spec))))
                                 T)
        c1 (first (filter #(= :c1 (:cascade-id %)) ranked))]
    (is (some? c1) "the c1 entry is found")
    (is (= expected (:G-efe c1)) ":G-efe equals the pre-slice scalar exactly")
    (is (= expected (:G-cascade c1)))
    (is (= expected (:controller-score c1)))))

(deftest beta-echoed-when-declared-on-opts
  (let [{:keys [q0 spec candidates]} (fixture)
        ranked (efe/rank-cascade-actions {:cascade-belief q0}
                                         candidates
                                         {:horizon-steps 1
                                          :cascade-spec spec
                                          :beta 2.0})]
    (doseq [entry ranked]
      (is (= {:value 2.0 :status :declared}
             (:beta-declared (:certificate entry)))))))

(deftest refusal-emits-no-certificate
  ;; Non-zero rates: horizon-g-sparse-cert returns the typed refusal with
  ;; :certificate nil — no certificate is fabricated for a computation that
  ;; did not run.
  (let [{:keys [q0 spec universe]} (fixture)
        {:keys [g certificate]}
        (m/horizon-g-sparse-cert {:rates (zipmap universe (repeat {:false-neg 1/8 :false-pos 0}))
                                  :q0 q0
                                  :precedence-fn (constantly [])
                                  :horizon 1
                                  :spec spec
                                  :universe universe})]
    (is (= :missing (:status g)))
    (is (= :judgement-rates-not-supported-at-scale (:kind g)))
    (is (nil? certificate) "the refusal IS the record"))
  ;; At the ranking layer a refusal (here: missing want) returns the refusal
  ;; itself — no ranked entries, so no certificates anywhere.
  (let [r (efe/rank-cascade-actions {:cascade-belief {#{} 1}}
                                    [{:kind :cascade-candidate :id :c :precedence []}]
                                    {:horizon-steps 1
                                     :cascade-spec {:want #{}}})]
    (is (= :missing (:status r)))
    (is (= :missing-cascade-want (:kind r)))))

(deftest infinite-risk-certifies-the-infinite-step
  ;; A pattern producing a zeroed outcome: risk is :infinite at that step and
  ;; the certificate records the step and the :infinite total (GCertificate
  ;; over EReal, horizonEFE_eq_top_iff) instead of a fabricated finite sum.
  (let [p (m/interpret-pattern "pz" "pattern" produce-z-text)
        q0 (m/observed-belief #{"ready" "stalled"})
        spec {:want #{["t" "evidence"]} :evidence #{"ready" "stalled"} :lam 1 :mu 1
              :zeroed #{#{"ready" "stalled" "zed" "produce"}}}
        universe #{"ready" "evidence" "zed" "stalled" "produce"}
        {:keys [g certificate]}
        (m/horizon-g-sparse-cert {:rates (zipmap universe (repeat {:false-neg 0 :false-pos 0}))
                                  :q0 q0
                                  :precedence-fn (constantly [p])
                                  :horizon 2
                                  :spec spec
                                  :universe universe})]
    (is (= :infinite g))
    (is (= :infinite (:total certificate)))
    (is (= [{:tau 1 :risk :infinite :risk-status :computed
             :ambiguity 0 :ambiguity-status :reduced-identically-zero
             :reduction "identity-A-zero-rates"}]
           (:steps certificate))
        "iteration stopped at the infinite step; no later steps are fabricated")))

;; ===== WIRE-2: per-policy F on the tick =====
;; F_π comes from cascade-free-energy/policy-free-energy at the same q0,
;; tau, rates and universe G was scored over; the observed tokens are the
;; cascade decision's own evidence set. A computed F reaches the ranked
;; entry as :f and the certificate records WHERE it came from; a refused F
;; excludes the candidate with a recorded reason — never a silent 0.

(def wire-pattern
  {:id :p :produces #{"clean"}
   :guard {:status :interpreted
           :clauses [{:status :interpreted
                      :present #{"ready"}
                      :absent #{"clean"}}]}})

(deftest computed-f-is-attached-with-provenance-and-distinct-from-absence
  (let [q0 (m/observed-belief #{"ready"})
        spec {:want #{["t" "evidence"]} :evidence #{"ready"} :lam 1 :mu 1 :zeroed #{}}
        candidates [{:kind :cascade-candidate :id :c1 :precedence [wire-pattern]}]
        ranked (efe/rank-cascade-actions {:cascade-belief q0}
                                         candidates
                                         {:horizon-steps 1 :cascade-spec spec})
        entry (first ranked)
        f-cert (:f (:certificate entry))]
    ;; the firing pattern leaves the observed state: F is computed ##Inf
    ;; (a contradicted prediction at theta = 1), NOT defaulted.
    (is (= ##Inf (:f entry)) ":f on the entry is the computed value")
    (is (= ##Inf (:value f-cert)))
    (is (= :computed (:status f-cert)) "computed is stated in the certificate")
    (is (= :cascade-free-energy/policy-free-energy (:source f-cert))
        "the certificate names WHERE F came from")
    ;; Requirement 3, the acceptance point: a computed 0.0 and an absent F
    ;; are distinguishable — the shapes share no status value. A candidate
    ;; whose F was NOT computed (see the exclusion test) carries
    ;; :status :not-attached with a reason, never {:value 0 :status ...}.
    (is (not= :declared-neutral (:status f-cert)))
    (is (contains? f-cert :source) "absence-shaped records also carry :source")
    ;; and the honest zero case: the empty cascade stays on the observation,
    ;; so its F is computed 0.0 — visible as :computed, not as a default.
    (let [ranked0 (efe/rank-cascade-actions {:cascade-belief q0}
                                            [{:kind :cascade-candidate :id :c0
                                              :precedence []}]
                                            {:horizon-steps 1 :cascade-spec spec})
          f0 (:f (:certificate (first ranked0)))]
      (is (= 0.0 (:value f0)))
      (is (= :computed (:status f0))
          "F computed = 0.0 is distinguishable from F defaulted to 0: the status differs"))))

(deftest refused-f-excludes-the-candidate-with-a-recorded-reason
  ;; A pattern with no interpretation makes policy-free-energy's rollout
  ;; refuse FOR THAT CANDIDATE only. The candidate is excluded from the
  ;; ranking (never scored with F = 0) and the reason is recorded in the
  ;; result meta; the computable sibling is unaffected.
  (let [q0 (m/observed-belief #{"ready"})
        spec {:want #{["t" "evidence"]} :evidence #{"ready"} :lam 1 :mu 1 :zeroed #{}}
        candidates [{:kind :cascade-candidate :id :cbad
                     :precedence [{:id :missing :status :missing}]}
                    {:kind :cascade-candidate :id :c0 :precedence []}]
        ranked (efe/rank-cascade-actions {:cascade-belief q0}
                                         candidates
                                         {:horizon-steps 1 :cascade-spec spec})
        exclusions (:f-exclusions (meta ranked))]
    (is (= [:c0] (mapv :cascade-id ranked))
        "the refused candidate is not in the ranking")
    (is (= 1 (count exclusions)))
    (is (= :cbad (:cascade-id (first exclusions))))
    (is (= :cascade-free-energy/policy-free-energy (:source (first exclusions))))
    (is (= :missing (:status (:reason (first exclusions))))
        "the typed refusal itself is the recorded reason")
    (is (= :missing-pattern-interpretation (:kind (:reason (first exclusions))))
        "the refusal kind names the missing interpretation")
    ;; the whole run's F computation is visible in the scoring meta
    (is (= :computed (get-in (meta ranked) [:cascade-scoring :free-energy :status])))))

(deftest global-f-refusal-refuses-the-family-never-a-silent-zero
  ;; A malformed candidate list (non-vector precedence) makes the producer
  ;; refuse for the whole family: no candidate is ranked with a defaulted
  ;; F — the refusal is the record.
  (let [q0 (m/observed-belief #{"ready"})
        spec {:want #{["t" "evidence"]} :evidence #{"ready"} :lam 1 :mu 1 :zeroed #{}}
        ranked (efe/rank-cascade-actions
                {:cascade-belief q0}
                [{:kind :cascade-candidate :id :cbad :precedence (seq [wire-pattern])}
                 {:kind :cascade-candidate :id :c0 :precedence []}]
                {:horizon-steps 1 :cascade-spec spec})
        meta' (meta ranked)]
    (is (= [] ranked) "no candidate is ranked with a defaulted F")
    (is (= 2 (count (:f-exclusions meta'))))
    (is (every? #(= :invalid-candidates (:kind (:reason %)))
                (:f-exclusions meta')))
    (is (= :refused (get-in meta' [:cascade-scoring :free-energy :status])))))
