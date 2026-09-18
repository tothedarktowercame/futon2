(ns futon2.aif.efe-certificate-test
  "WIRE-1 acceptance: the emission slice. rank-cascade-actions attaches a
  :certificate (Lean DarkTower/AIF/Certificates.lean GCertificate) to every
  ranked candidate, filled from inside the horizon-g-sparse evaluation;
  the scalar G is byte-identical to the pre-slice path; a refused
  computation emits no certificate — the refusal IS the record."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.set :as cset]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.likelihood-precision :as lp]
            [futon2.aif.cascade-selection :as cs]
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
      ;; WIRE-2: F is computed on the tick (not a declared-neutral 0). This
      ;; fixture declares no :evidence, so the observed token set is empty and
      ;; the identity observation of nothing is impossible under every
      ;; rollout: F = ##Inf. Computed and provenance-stamped — and NOT
      ;; attached to the entry, because a non-finite F drives the selection
      ;; posterior to NaN for every candidate (WIRE-2 review fix, 2026-09-18).
      (is (= {:value ##Inf
              :status :computed-not-attached
              :reason :non-finite-under-identity-a
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
  ;; WIRE-4: non-zero rates now SCORE, so the induced refusal here is the
  ;; factorized path's own typed precondition — a non-empty :zeroed under
  ;; non-zero rates. Still a refusal, still no certificate.
  (let [{:keys [q0 universe]} (fixture)
        {:keys [g certificate]}
        (m/horizon-g-sparse-cert {:rates (zipmap universe (repeat {:false-neg 1/8 :false-pos 0}))
                                  :q0 q0
                                  :precedence-fn (constantly [])
                                  :horizon 1
                                  :spec {:want #{["t" "evidence"]}
                                         :evidence #{} :lam 1 :mu 1
                                         :zeroed #{#{["t" "evidence"]}}}
                                  :universe universe})]
    (is (= :missing (:status g)))
    (is (= :zeroed-unsupported-with-rates (:kind g)))
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
    ;; The firing pattern leaves the observed state, so under identity A the
    ;; prediction is contradicted and F = ##Inf. Computed, recorded, and NOT
    ;; attached: a non-finite F makes every score -Inf and the posterior NaN
    ;; for every candidate, which selection then reports as a decision. Three
    ;; states must stay distinguishable — computed-and-attached (finite),
    ;; computed-not-attached (non-finite), not-attached (never computed).
    (is (nil? (:f entry))
        "a non-finite F does not reach the law")
    (is (= ##Inf (:value f-cert))
        "but the computed value is still on the certificate, not discarded")
    (is (= :computed-not-attached (:status f-cert)))
    (is (= :non-finite-under-identity-a (:reason f-cert))
        "and the certificate says WHY it did not reach the law")
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

(deftest finite-f-reaches-the-law-and-non-finite-f-is-refused
  ;; The other half of the WIRE-2 review fix: nothing above should be read as
  ;; "F never reaches selection". A finite F is attached and moves the
  ;; posterior; a non-finite one is a typed refusal rather than a NaN that
  ;; selection reports as a decision.
  (testing "a finite F is attached and changes the posterior"
    (let [post (cs/selection-posterior
                {:beta 1 :candidates [{:id :x :habit 1 :f 0.5 :g 1.0}
                                      {:id :y :habit 1 :f 2.0 :g 1.0}]})]
      (is (= 2 (count post)))
      (is (every? #(and (number? %) (Double/isFinite (double %))) (vals post)))
      (is (> (get post :x) (get post :y))
          "the lower-F candidate carries more posterior mass")))
  (testing "a non-finite F is a typed refusal, not a NaN posterior"
    (doseq [bad [##Inf ##-Inf ##NaN]]
      (let [refusal (try (cs/selection-posterior
                          {:beta 1 :candidates [{:id :a :habit 1 :f bad :g 1.0}]})
                         (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e))))]
        (is (= :invalid-free-energy (:kind refusal))
            (str "F = " bad " must refuse, not produce NaN"))
        (is (= :a (get-in refusal [:detail :id])))))))

;; ===== WIRE-3: which C the scoring used =====
;; The certificate's :c field records derived (with signature and the
;; weights echoed) versus the uniform declared-constant spec, so "C was
;; derived and happened to be near-uniform" is distinguishable from "C was
;; never derived".

(deftest wire-3-derived-and-uniform-c-are-distinguishable-in-the-certificate
  (let [{:keys [q0 spec candidates]} (fixture)
        base-opts {:horizon-steps 1 :cascade-spec spec}
        uniform (efe/rank-cascade-actions {:cascade-belief q0} candidates base-opts)
        want-tok (first (:want spec))
        weighted-spec (-> spec
                          (assoc :weights {want-tok 1}
                                 :c {:status :derived
                                     :source :futon2.aif.live-c/cascade-spec
                                     :signature "wire-3-test-signature"}))
        derived (efe/rank-cascade-actions
                 {:cascade-belief q0} candidates
                 {:horizon-steps 1 :cascade-spec weighted-spec})
        c-uniform (:c (:certificate (first uniform)))
        c-derived (:c (:certificate (first derived)))]
    (is (= :uniform-declared-constant (:status c-uniform))
        "no :c on the spec records the uniform declared-constant spec")
    (is (nil? (:signature c-uniform)))
    (is (= :derived (:status c-derived)))
    (is (= "wire-3-test-signature" (:signature c-derived)))
    (is (= :futon2.aif.live-c/cascade-spec (:source c-derived)))
    (is (= {want-tok 1} (:weights-echo c-derived))
        "the weights that reached the law are echoed — a derived C that
        happens to be near-uniform still reads :derived")
    ;; the two records are structurally distinguishable: no shared status
    (is (not= (:status c-uniform) (:status c-derived)))
    ;; the weighted scoring itself is well-formed: finite G, no NaN, no
    ;; candidate zeroed out by the weights (weights are positive rationals
    ;; over want, so no preference mass can vanish).
    (is (every? number? (map :controller-score derived)))
    (is (every? #(Double/isFinite ^double %) (map :controller-score derived))
        "a positive-rational weight cannot zero preferred mass: every G finite")))

(deftest wire-3-invalid-weights-refuse-the-spec-typed
  ;; A weight on a token outside :want is log-preference-fn's typed
  ;; refusal. At the ranking layer it lands per candidate: G IS the typed
  ;; refusal, controller-score is ##Inf, and NO certificate is fabricated
  ;; for a computation that did not run — selection-posterior then filters
  ;; on finite G and refuses :no-admissible-candidate. The invalid weights
  ;; are never silently dropped.
  (let [{:keys [q0 spec candidates]} (fixture)
        r (efe/rank-cascade-actions
           {:cascade-belief q0} candidates
           {:horizon-steps 1
            :cascade-spec (assoc spec :weights {:not-in-want 1})})]
    (is (vector? r))
    (is (= 2 (count r)))
    (doseq [entry r]
      (is (= :missing (:status (:G-efe entry)))
          "G is the typed refusal itself")
      (is (= :invalid-preference-spec (:kind (:G-efe entry))))
      (is (= :weights (:field (:G-efe entry))))
      (is (= ##Inf (:controller-score entry))
          "a refused G cannot be ranked as finite")
      (is (nil? (:certificate entry))
          "no certificate is fabricated for the refused computation"))))

(defn- r7-setup
  "Self-contained R7 fixture: the universe rank-cascade-actions actually
  constructs (precedence tokens + q0 support + want), so declared rates can
  cover it exactly."
  []
  (let [p (m/interpret-pattern "p" "pattern" pattern-text)
        q0 (m/observed-belief #{"ready"})
        want #{"evidence"}
        spec {:want want :evidence #{} :lam 1 :mu 1/2 :zeroed #{}}
        universe (-> (candidate-tokens [p])
                     (into (reduce cset/union #{} (keys q0)))
                     (into want))]
    {:q0 q0 :spec spec :universe universe :candidates
     [{:kind :cascade-candidate :id :c1 :precedence [p]}]
     :p p}))

(deftest r7-declared-rates-and-zeta-reach-the-tick-path
  ;; R7 (2026-09-18): rank-cascade-actions no longer hardcodes zero rates.
  ;; Absent :adjudication-rates the call is byte-identical to the identity
  ;; path; a declared rates map (covering the scored universe) reaches the
  ;; factorized scorer, and a declared FIXED :zeta tempers it — the effect
  ;; shows in G, the GCertificate and the scoring meta.
  (let [{:keys [q0 spec universe p candidates]} (r7-setup)
        state {:cascade-belief q0}
        opts {:horizon-steps 1 :cascade-spec spec}
        base (efe/rank-actions state candidates opts)
        nz (zipmap universe (repeat {:false-neg 1/8 :false-pos 1/16}))
        scored (efe/rank-actions state candidates (assoc opts :adjudication-rates nz))
        tempered (efe/rank-actions state candidates
                                  (assoc opts :adjudication-rates nz :zeta 3))]
    ;; absent rates: exactly the identity path it always was
    (is (= :zero-adjudication-identity
           (get-in (meta base) [:cascade-scoring :rates])))
    (is (= :identity-A-zero-rates
           (:evaluation (:certificate (first base)))))
    ;; declared rates: the factorized path, meta says so
    (is (= :declared-adjudication-rates
           (get-in (meta scored) [:cascade-scoring :rates])))
    (is (= :factorized-nonzero-rates
           (:evaluation (:certificate (first scored)))))
    ;; declared FIXED zeta: G measurably moves, the certificate records it
    (is (not= (:G-cascade (first scored)) (:G-cascade (first tempered))))
    (is (= 3 (:zeta (:certificate (first tempered)))))
    (is (true? (:zeta-tempered? (:certificate (first tempered)))))
    (is (= 3 (get-in (meta tempered) [:cascade-scoring :zeta])))
    ;; and the tempered score is exactly the sparse scorer at tempered rates
    (is (< (Math/abs (- (:G-cascade (first tempered))
                        (m/horizon-g-sparse
                         {:rates (lp/tempered-rates nz 3)
                          :q0 q0 :precedence-fn (constantly [p])
                          :horizon 1 :spec spec :universe universe})))
           1e-12))))

(deftest r7-partial-rates-refuse-typed-never-projected-to-zero
  ;; a declared rates map missing tokens of the scored universe is the
  ;; typed refusal :invalid-adjudication-rates naming the missing tokens —
  ;; the old hardcode would have silently scored them at zero.
  (let [{:keys [q0 universe candidates]} (r7-setup)
        state {:cascade-belief q0}
        missing-token (first universe)
        partial (zipmap (rest universe) (repeat {:false-neg 1/8 :false-pos 1/16}))
        r (efe/rank-actions state candidates
                            {:horizon-steps 1
                             :cascade-spec {:want #{"evidence"} :lam 1 :mu 1/2}
                             :adjudication-rates partial})]
    (is (= :missing (:status r)))
    (is (= :invalid-adjudication-rates (:kind r)))
    (is (= 1 (count (:missing r))))
    (is (= missing-token (first (:missing r))))))
