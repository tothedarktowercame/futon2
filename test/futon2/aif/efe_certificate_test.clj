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
      (is (= {:value 0 :status :declared-neutral} (:f cert))))))

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
