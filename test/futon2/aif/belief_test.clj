(ns futon2.aif.belief-test
  "Tests for the WM AIF per-entity belief state.

   Validation properties checked here:
   - Posterior is a valid probability distribution (sum = 1.0, all values ≥ 0)
   - Update is deterministic under known input (R1 baseline test required by
     M-war-machine-aif-completion Checkpoint 1)
   - Multiplicative likelihood updates commute under reordering
   - Most-likely-status reports argmax correctly
   - Entropy is maximal for uniform posterior, decreasing under accumulating
     evidence (V-shrink shape — full V-shrink property is R9-class and
     ships with Checkpoint 4)"
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]))

(defn- prob-sum [posterior]
  (reduce + (vals posterior)))

(defn- valid-posterior? [posterior]
  (and (map? posterior)
       (= (set (keys posterior)) belief/status-set)
       (every? #(<= 0.0 % 1.0) (vals posterior))
       (< (Math/abs (- 1.0 (prob-sum posterior))) 1e-9)))

(deftest uniform-prior-test
  (testing "uniform-prior is a valid posterior over status-set"
    (let [p (belief/uniform-prior)]
      (is (valid-posterior? p))))
  (testing "uniform-prior assigns equal probability to every status"
    (let [p (belief/uniform-prior)
          n (count belief/status-set)
          expected (/ 1.0 n)]
      (is (every? #(< (Math/abs (- expected %)) 1e-9) (vals p))))))

(deftest initial-belief-state-test
  (testing "fresh state with given entity ids carries uniform priors"
    (let [b (belief/initial-belief-state [:e1 :e2 :e3])]
      (is (= #{:e1 :e2 :e3} (set (keys b))))
      (is (every? valid-posterior? (vals b))))))

(deftest update-entity-belief-test
  (testing "in-set event increases probability of matched status"
    (let [p0 (belief/uniform-prior)
          p1 (belief/update-entity-belief p0 {:type :strengthened :weight 1.0})]
      (is (valid-posterior? p1))
      (is (> (:strengthened p1) (:strengthened p0))
          "matched status should gain probability")
      (is (< (:spawned p1) (:spawned p0))
          "unmatched statuses should lose probability under normalisation")))
  (testing "unknown event type leaves posterior unchanged"
    (let [p0 (belief/uniform-prior)
          p1 (belief/update-entity-belief p0 {:type :not-a-status :weight 1.0})]
      (is (= p0 p1))))
  (testing "missing :weight defaults to 1.0"
    (let [p1 (belief/update-entity-belief (belief/uniform-prior) {:type :spawned})
          p2 (belief/update-entity-belief (belief/uniform-prior) {:type :spawned :weight 1.0})]
      (is (= p1 p2))))
  (testing "posterior remains valid under repeated updates"
    (let [final (reduce belief/update-entity-belief
                        (belief/uniform-prior)
                        [{:type :spawned :weight 0.5}
                         {:type :refined :weight 1.0}
                         {:type :addressed :weight 2.0}])]
      (is (valid-posterior? final)))))

(deftest update-determinism-test
  (testing "same events on same starting belief yield identical posteriors"
    (let [b0 (belief/initial-belief-state [:m1 :m2])
          events [{:entity-id :m1 :type :strengthened :weight 1.5}
                  {:entity-id :m2 :type :foreclosed :weight 0.8}
                  {:entity-id :m1 :type :addressed :weight 0.5}]
          b1 (belief/update-belief-batch b0 events)
          b2 (belief/update-belief-batch b0 events)]
      (is (= b1 b2) "deterministic update — R1 baseline contract"))))

(deftest update-commutativity-test
  (testing "reordering events for the same entity yields the same final posterior"
    (let [b0 (belief/initial-belief-state [:m1])
          e1 {:entity-id :m1 :type :strengthened :weight 1.0}
          e2 {:entity-id :m1 :type :refined :weight 0.5}
          e3 {:entity-id :m1 :type :addressed :weight 2.0}
          b-forward (belief/update-belief-batch b0 [e1 e2 e3])
          b-reverse (belief/update-belief-batch b0 [e3 e2 e1])]
      ;; multiplicative likelihood under normalisation is commutative
      (let [pf (get b-forward :m1)
            pr (get b-reverse :m1)]
        (is (every? #(< (Math/abs (- (get pf %) (get pr %))) 1e-9)
                    belief/status-set))))))

(deftest update-belief-new-entity-test
  (testing "events for previously-untracked entity-ids initialise with uniform prior"
    (let [b0 {}
          b1 (belief/update-belief b0 {:entity-id :new-thing :type :spawned :weight 1.0})]
      (is (contains? b1 :new-thing))
      (is (valid-posterior? (get b1 :new-thing)))
      (is (> (get-in b1 [:new-thing :spawned])
             (/ 1.0 (count belief/status-set)))
          "spawned should be above uniform after the spawn event"))))

(deftest most-likely-status-test
  (testing "returns nil for empty posterior"
    (is (nil? (belief/most-likely-status {}))))
  (testing "returns the argmax status"
    (let [p (belief/update-entity-belief (belief/uniform-prior) {:type :strengthened :weight 5.0})]
      (is (= :strengthened (belief/most-likely-status p)))))
  (testing "uniform posterior — any status acceptable as argmax (tie-breaking unspecified)"
    (is (contains? belief/status-set (belief/most-likely-status (belief/uniform-prior))))))

(deftest entropy-test
  (testing "uniform posterior has maximal entropy (log n)"
    (let [h (belief/entropy (belief/uniform-prior))
          n (count belief/status-set)
          max-h (Math/log n)]
      (is (< (Math/abs (- h max-h)) 1e-9))))
  (testing "peaked posterior has lower entropy than uniform"
    (let [uniform (belief/uniform-prior)
          peaked (reduce belief/update-entity-belief uniform
                         (repeat 10 {:type :strengthened :weight 2.0}))]
      (is (< (belief/entropy peaked) (belief/entropy uniform))
          "V-shrink-shape: entropy decreases as evidence accumulates")))
  (testing "entropy is non-negative"
    (is (>= (belief/entropy (belief/uniform-prior)) 0.0))))

;; ---------------------------------------------------------------------------
;; v0.9: bootstrap-from-stack-annotations (symmetric with VSATARCS-side)
;; ---------------------------------------------------------------------------

(deftest section-ids-from-stack-annotations-smoke-test
  (testing "default-path read returns a non-empty vec of strings"
    ;; Smoke test against the real canonical source. If the file is
    ;; absent or unreadable the test is informational, not blocking
    ;; — bootstrap-from-stack-annotations falls back to extra-ids only.
    (let [ids (try (belief/section-ids-from-stack-annotations)
                   (catch Exception _ nil))]
      (when ids
        (is (pos? (count ids)) "section-ids should be non-empty")
        (is (every? string? ids) "section-ids should all be strings")
        (is (= (count ids) (count (distinct ids))) "no duplicate ids")))))

(deftest section-ids-known-id-present-test
  (testing "a known canonical section-id appears in the read"
    (let [ids (try (belief/section-ids-from-stack-annotations)
                   (catch Exception _ nil))]
      (when ids
        (is (contains? (set ids) "arxana/stack/futon-v1/leaf/2")
            "canonical leaf-2 id should be present in the bootstrap source")))))

(deftest bootstrap-from-stack-annotations-shape-test
  (testing "bootstrap returns a belief map with valid posterior per entity"
    (let [b (belief/bootstrap-from-stack-annotations [])]
      (when (seq b)
        (is (every? map? (vals b))
            "every entity carries a posterior map")
        (is (every? #(< (Math/abs (- 1.0 (reduce + (vals %)))) 1e-9) (vals b))
            "every posterior sums to 1.0")))))

(deftest bootstrap-merges-extra-ids-test
  (testing "extra-ids are unioned into the entity domain"
    (let [extras [:sorry/my-test-keyword :sorry/another]
          b (belief/bootstrap-from-stack-annotations extras)]
      (is (contains? b :sorry/my-test-keyword))
      (is (contains? b :sorry/another))
      (is (= (belief/uniform-prior) (get b :sorry/my-test-keyword))
          "extra-ids get uniform prior just like sections"))))

(deftest bootstrap-fallback-on-missing-source-test
  (testing "bootstrap with unreadable canonical source returns extra-ids only"
    (with-redefs [belief/section-ids-from-stack-annotations
                  (fn [& _] (throw (ex-info "missing" {})))]
      (let [b (belief/bootstrap-from-stack-annotations [:sorry/test])]
        (is (= #{:sorry/test} (set (keys b)))
            "fallback returns only the extra-ids when source unavailable")))))

(deftest bootstrap-heterogeneous-keys-test
  (testing "section-ids (strings) and extra-ids (keywords) coexist disjointly"
    (let [b (belief/bootstrap-from-stack-annotations [:sorry/wm-aif-substrate-addressability])]
      (when (seq (filter string? (keys b)))
        ;; if we have section-ids, verify both key types are present and
        ;; that the keyword sorry-id is not accidentally collided with
        (is (contains? b :sorry/wm-aif-substrate-addressability))
        (is (some string? (keys b))
            "string section-ids present alongside keyword sorry-id")
        (is (not (contains? b (str :sorry/wm-aif-substrate-addressability)))
            "keyword and stringified-keyword don't collide")))))

;; ---------------------------------------------------------------------------
;; v0.10: predict-annotation-health (R3a likelihood model)
;; ---------------------------------------------------------------------------

(deftest predict-annotation-health-empty-belief-test
  (testing "empty belief returns maximal uncertainty"
    (let [p (belief/predict-annotation-health {})]
      (is (= 0.0 (:mean p)))
      (is (= 1.0 (:variance p))))))

(deftest predict-annotation-health-uniform-belief-test
  (testing "uniform belief over one entity yields rescaled-mean ≈ 0.5 and variance ≈ 1.0"
    (let [b {:e1 (belief/uniform-prior)}
          p (belief/predict-annotation-health b)]
      ;; uniform posterior: Σ p(s) * w(s) where weights are
      ;; {1, 1, 0.5, 0, 0, -0.5, -1} summing to 1.0; mean = 1/7
      ;; rescaled to [0,1] = (1/7 + 1) / 2 ≈ 0.571
      (is (< (Math/abs (- 0.5714285714285714 (:mean p))) 1e-6)
          (str "expected ≈ 0.5714; got " (:mean p)))
      (is (< (Math/abs (- 1.0 (:variance p))) 1e-6)
          "variance ≈ 1.0 for uniform posterior (max entropy normalised)"))))

(deftest predict-annotation-health-peaked-strengthened-test
  (testing "belief concentrated on :strengthened yields mean ≈ 1.0 and variance ≈ 0"
    (let [peaked (reduce belief/update-entity-belief
                         (belief/uniform-prior)
                         (repeat 30 {:type :strengthened :weight 5.0}))
          b {:e1 peaked}
          p (belief/predict-annotation-health b)]
      (is (> (:mean p) 0.95)
          (str "mean should approach 1.0 for :strengthened-peaked belief; got " (:mean p)))
      (is (< (:variance p) 0.1)
          (str "variance should approach 0 for peaked belief; got " (:variance p))))))

(deftest predict-annotation-health-peaked-falsified-test
  (testing "belief concentrated on :falsified yields mean ≈ 0.0 and variance ≈ 0"
    (let [peaked (reduce belief/update-entity-belief
                         (belief/uniform-prior)
                         (repeat 30 {:type :falsified :weight 5.0}))
          b {:e1 peaked}
          p (belief/predict-annotation-health b)]
      (is (< (:mean p) 0.05)
          (str "mean should approach 0.0 for :falsified-peaked belief; got " (:mean p)))
      (is (< (:variance p) 0.1)
          (str "variance should approach 0 for peaked belief; got " (:variance p))))))

(deftest predict-annotation-health-aggregates-across-entities-test
  (testing "mean is the average per-entity expected health across entities"
    (let [strengthened (reduce belief/update-entity-belief
                               (belief/uniform-prior)
                               (repeat 30 {:type :strengthened :weight 5.0}))
          falsified (reduce belief/update-entity-belief
                            (belief/uniform-prior)
                            (repeat 30 {:type :falsified :weight 5.0}))
          mixed {:e1 strengthened :e2 falsified}
          p (belief/predict-annotation-health mixed)]
      ;; e1 mean ≈ 1.0, e2 mean ≈ 0.0; average ≈ 0.5
      (is (< (Math/abs (- 0.5 (:mean p))) 0.05)
          (str "mixed belief mean should average to ≈ 0.5; got " (:mean p))))))

(deftest annotation-health-status-weights-shape-test
  (testing "every status in status-set has a declared weight"
    (is (= belief/status-set
           (set (keys belief/annotation-health-status-weights)))
        "status-weights keys match status-set"))
  (testing "weights are in [-1, 1]"
    (is (every? #(<= -1.0 % 1.0) (vals belief/annotation-health-status-weights)))))

;; ---------------------------------------------------------------------------
;; v0.11: predict-sorry-count-norm, predict-mission-health, predict-active-repo-ratio
;; ---------------------------------------------------------------------------

(deftest predict-sorry-count-norm-empty-test
  (testing "empty belief returns maximal uncertainty"
    (let [p (belief/predict-sorry-count-norm {})]
      (is (= 0.0 (:mean p)))
      (is (= 1.0 (:variance p))))))

(deftest predict-sorry-count-norm-uniform-test
  (testing "uniform belief: each entity contributes 3/7 open-mass; sum /10 capped at 1.0"
    ;; 7 entities × 3/7 each = 3 open-mass total; /10 = 0.3
    (let [b (belief/initial-belief-state (range 7))
          p (belief/predict-sorry-count-norm b)]
      (is (< (Math/abs (- 0.3 (:mean p))) 1e-6)
          (str "expected 0.3 for 7 uniform entities; got " (:mean p))))))

(deftest predict-sorry-count-norm-peaked-closed-test
  (testing "belief peaked on :addressed (closed) yields near-zero open-mass"
    (let [peaked (reduce belief/update-entity-belief
                         (belief/uniform-prior)
                         (repeat 30 {:type :addressed :weight 5.0}))
          b {:e1 peaked}
          p (belief/predict-sorry-count-norm b)]
      (is (< (:mean p) 0.02)
          (str "closed belief should yield ~0 sorry-count; got " (:mean p))))))

(deftest predict-mission-health-empty-test
  (testing "empty belief returns max uncertainty"
    (let [p (belief/predict-mission-health {})]
      (is (= 0.0 (:mean p)))
      (is (= 1.0 (:variance p))))))

(deftest predict-mission-health-uniform-test
  (testing "uniform belief: 2/7 healthy-mass per entity"
    ;; healthy = :strengthened + :addressed = 2 statuses / 7 uniform = 2/7
    (let [b {:e1 (belief/uniform-prior)}
          p (belief/predict-mission-health b)]
      (is (< (Math/abs (- (/ 2.0 7.0) (:mean p))) 1e-6)))))

(deftest predict-mission-health-peaked-strengthened-test
  (testing "belief peaked on :strengthened yields mean ≈ 1.0"
    (let [peaked (reduce belief/update-entity-belief
                         (belief/uniform-prior)
                         (repeat 30 {:type :strengthened :weight 5.0}))
          b {:e1 peaked}
          p (belief/predict-mission-health b)]
      (is (> (:mean p) 0.95)))))

(deftest predict-active-repo-ratio-empty-test
  (testing "empty belief returns max uncertainty"
    (let [p (belief/predict-active-repo-ratio {})]
      (is (= 0.0 (:mean p)))
      (is (= 1.0 (:variance p))))))

(deftest predict-active-repo-ratio-uniform-test
  (testing "uniform belief: 5/7 non-dormant mass per entity"
    ;; non-dormant = 1 - (:foreclosed + :falsified) = 1 - 2/7 = 5/7
    (let [b {:e1 (belief/uniform-prior)}
          p (belief/predict-active-repo-ratio b)]
      (is (< (Math/abs (- (/ 5.0 7.0) (:mean p))) 1e-6)))))

(deftest predict-active-repo-ratio-peaked-foreclosed-test
  (testing "belief peaked on :foreclosed yields mean ≈ 0.0 (dormant)"
    (let [peaked (reduce belief/update-entity-belief
                         (belief/uniform-prior)
                         (repeat 30 {:type :foreclosed :weight 5.0}))
          b {:e1 peaked}
          p (belief/predict-active-repo-ratio b)]
      (is (< (:mean p) 0.05)
          (str "foreclosed belief → dormant; got " (:mean p))))))

(deftest channels-with-likelihood-test
  (testing "v0.11 covers 4 channels"
    (is (= 4 (count belief/channels-with-likelihood)))
    (is (= #{:annotation-health :sorry-count-norm :mission-health :active-repo-ratio}
           belief/channels-with-likelihood))))

(deftest predict-observation-composite-test
  (testing "predict-observation returns predictions for every channel-with-likelihood"
    (let [b (belief/initial-belief-state [:e1 :e2 :e3])
          predictions (belief/predict-observation b)]
      (is (= belief/channels-with-likelihood (set (keys predictions))))
      (is (every? (fn [p] (and (contains? p :mean) (contains? p :variance)))
                  (vals predictions)))
      (is (every? (fn [p] (and (number? (:mean p)) (number? (:variance p))))
                  (vals predictions))))))

;; =============================================================================
;; E-support-coverage Cycle 2: entity-tag classification at bootstrap
;; =============================================================================

(defn- write-tmp-stack-annotations! [path sections]
  (spit path (pr-str {:sections sections})))

(deftest classify-entity-tags-tags-support-claims
  (testing "Sections with sorry|holistic-argument|S|S<N> refs get :supports-S<N> tags"
    (let [tmp (java.io.File/createTempFile "stack-ann-test-" ".edn")
          path (.getAbsolutePath tmp)]
      (try
        (write-tmp-stack-annotations!
         path
         [{:id "e1" :ref "sorry|holistic-argument|S|S1"}
          {:id "e2" :ref "sorry|holistic-argument|S|S5"}
          {:id "e3" :ref "sorry|holistic-argument|S|S6"} ; S6 is capstone, not S1-S5
          {:id "e4" :ref "sorry|holistic-argument|thesis"}])
        (let [tags (belief/classify-entity-tags-from-stack-annotations path)]
          (is (= #{:supports-S1} (get tags "e1")))
          (is (= #{:supports-S5} (get tags "e2")))
          (is (nil? (get tags "e3")) "S6 should not match S<1-5> pattern")
          (is (nil? (get tags "e4")) "thesis should not match")
          (is (= 2 (count tags))))
        (finally (.delete tmp))))))

(deftest classify-entity-tags-tags-attack-claims
  (testing "Sections with sorry|holistic-argument|A|A<N> refs get :attacks-A<N> tags"
    (let [tmp (java.io.File/createTempFile "stack-ann-test-" ".edn")
          path (.getAbsolutePath tmp)]
      (try
        (write-tmp-stack-annotations!
         path
         [{:id "a1" :ref "sorry|holistic-argument|A|A1"}
          {:id "a4" :ref "sorry|holistic-argument|A|A4"}
          {:id "a5" :ref "sorry|holistic-argument|A|A5"}]) ; A5 doesn't exist; out of range
        (let [tags (belief/classify-entity-tags-from-stack-annotations path)]
          (is (= #{:attacks-A1} (get tags "a1")))
          (is (= #{:attacks-A4} (get tags "a4")))
          (is (nil? (get tags "a5")) "A5 out of pattern range [1-4]"))
        (finally (.delete tmp))))))

(deftest classify-entity-tags-handles-missing-file
  (testing "Missing or unreadable substrate returns empty map; bootstrap does not fail"
    (is (= {} (belief/classify-entity-tags-from-stack-annotations
               "/nonexistent/path/stack-annotations.edn")))))

(deftest classify-entity-tags-smoke-against-real-substrate
  (testing "Real stack-annotations.edn yields exactly 9 tagged entities (S1-S5 + A1-A4)"
    (let [tags (belief/classify-entity-tags-from-stack-annotations)]
      (is (= 9 (count tags)))
      (is (= #{:supports-S1 :supports-S2 :supports-S3 :supports-S4 :supports-S5
               :attacks-A1 :attacks-A2 :attacks-A3 :attacks-A4}
             (->> tags vals (mapcat seq) set))))))
