(ns futon2.aif.selection-r1-test
  "SelectionR1 (E4, mathlib4 c14-cascade-outcome 71d54b7421): task
  selection's initial belief reads R1. r1-of is passed explicitly in every
  test; no futon3c is needed.

  Covers: an unlinked menu is byte-identical to before the change (point
  mass); one linked pick at q = 1 gives the unlinked G
  (predEnding_class_q_one); lowering q raises that pick's G when the
  preference ranks its class above :unrelated (risk_certain_below_lt), and
  two q values give two G values; a refused item refuses selection typed;
  the joint refuses at 11 linked picks (:r1-joint-too-large); the selection
  evidence records :r1."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-menu-fixture :as menu-fixture]
            [futon2.aif.outer-select :as outer-select]
            [futon2.aif.outer-select-test :as outer-select-test]
            [futon2.aif.selection-canonical-g :as selection-g]))

(clojure.test/use-fixtures :each menu-fixture/with-fixture-constructor)

(def interps {"p" {:consumes #{:ready} :forbids #{} :produces #{:can-begin}}})

(def entries
  [{:id :c1 :patterns ["p"] :pick "M-a" :relation-class :associated}])

(def facts {"M-a" {:status :ready :unblocks 1 :cone 1}})

(defn- g-of [result id]
  (:G-cascade (get-in result [:by-id id])))

(deftest unlinked-menu-is-byte-identical-to-before
  (testing "an unlinked menu keeps the point mass exactly (the default
  r1-of and the explicit one give the same full result)"
    (is (= (selection-g/score entries facts interps)
           (selection-g/score entries facts interps (constantly {:status :unlinked}))))))

(deftest linked-q-one-gives-the-unlinked-g
  (testing "predEnding_class_q_one: q = 1 on a linked pick is today's G"
    (let [unlinked (selection-g/score entries facts interps
                                      (constantly {:status :unlinked}))
          linked-q1 (selection-g/score entries facts interps
                                       (constantly {:status :linked :below 1
                                                    :benefits ["B01"] :log-hash "h" :calibration #{:joe}}))]
      (is (= (g-of unlinked :c1) (g-of linked-q1 :c1))))))

(deftest lowering-q-raises-g
  (testing "risk_certain_below_lt: the preference ranks :related above
  :unrelated (35/100 vs 5/100), so moving mass off benefit-below raises the
  pick's cascade G; two q values give two G values"
    (let [g-at (fn [q] (g-of (selection-g/score entries facts interps
                                                (constantly {:status :linked :below q
                                                             :benefits ["B01"]}))
                             :c1))
          g1 (g-at 1) g34 (g-at 3/4) g12 (g-at 1/2)]
      (is (< g1 g34 g12))
      (is (not= g34 g12))
      (is (every? #(Double/isFinite (double %)) [g1 g34 g12])))))

(deftest mutant-ignoring-q-fails
  (testing "PLANTED MUTANT: ignore q in r1-belief (always the point mass on
  the with-token state). Run by mutating the source; then
  lowering-q-raises-g fails because both q values score as q = 1. Here the
  belief construction itself is pinned so the mutant cannot pass silently:
  at q = 1/2 the joint has exactly two states, the with-token state at 1/2
  and the without-token state at 1/2, summing to 1"
    (let [belief (:belief (#'selection-g/r1-belief
                           ["M-a"]
                           (constantly {:status :linked :below 1/2 :benefits ["B01"]})
                           #{["M-a" :ready]}))]
      (is (= 2 (count belief)))
      (is (= 1/2 (get belief #{["M-a" :ready] ["M-a" :benefit-below]})))
      (is (= 1/2 (get belief #{["M-a" :ready]})))
      (is (= 1 (reduce + (vals belief))))
      (is (every? ratio? (vals belief))))))

(deftest refused-item-refuses-selection-typed
  (testing "an item-r1 :refused refuses selection, typed, with the item"
    (binding [outer-select/*r1-of* (fn [pick]
                                     (if (= "M-ready" pick)
                                       {:status :refused :reason :no-A-table}
                                       {:status :unlinked}))]
      (let [refusal (try (outer-select/select outer-select-test/base-makeup
                                              outer-select-test/base-state
                                              outer-select-test/base-observation)
                         (catch clojure.lang.ExceptionInfo e (ex-data e)))]
        (is (= :r1-refused (:kind refusal)))
        (is (= "M-ready" (:pick refusal)))
        (is (= :no-A-table (:detail refusal)))))))

(deftest joint-refuses-at-eleven-linked-picks
  (testing ":r1-joint-too-large with the count; never truncated"
    (let [picks (mapv #(str "M-" %) (range 11))
          entries (mapv (fn [p] {:id (keyword (str "c-" p)) :patterns ["p"]
                                 :pick p :relation-class :associated})
                        picks)
          facts (into {} (map (fn [p] [p {:status :ready :unblocks 0 :cone 0}]) picks))
          refusal (try (selection-g/score entries facts interps
                                          (constantly {:status :linked :below 1/2
                                                       :benefits ["B01"]}))
                       (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (= :r1-joint-too-large (:kind refusal)))
      (is (= 11 (:count refusal)))
      (is (= selection-g/max-r1-joint (:bound refusal))))))

(deftest select-records-r1-in-the-evidence
  (testing "the selection evidence carries :r1 per pick, for the stepper's
  [:after :selection-world]"
    (binding [outer-select/*r1-of* (fn [pick]
                                     (if (= "M-ready" pick)
                                       {:status :linked :below 3/4 :benefits ["B17"]
                                        :log-hash "abc" :calibration #{:placeholder}}
                                       {:status :unlinked}))]
      (let [r (outer-select/select outer-select-test/base-makeup
                                   outer-select-test/base-state
                                   outer-select-test/base-observation)]
        (is (= {:status :linked :below 3/4 :benefits ["B17"]
                :log-hash "abc" :calibration #{:placeholder}}
               (get-in r [:r1 "M-ready"])))
        (is (= {:status :unlinked} (get-in r [:r1 "M-unknown"])))
        (is (= {:status :chosen} (:outcome r)))))))

(deftest unavailable-without-explicit-r1-of
  (testing "with no :r1-of anywhere and futon3c off the classpath (futon2's
  own tests) the selection refuses typed :r1-unavailable -- never a silent
  point-mass fallback"
    (binding [outer-select/*r1-of* nil]
      (let [refusal (try (outer-select/select outer-select-test/base-makeup
                                              outer-select-test/base-state
                                              outer-select-test/base-observation)
                         (catch clojure.lang.ExceptionInfo e (ex-data e)))]
        (is (= :r1-unavailable (:kind refusal)))))))

(deftest q-zero-drops-the-with-token-state
  (testing "claude-14 review: at q = 0 no zero-mass state is carried (symmetric with q = 1)"
    (let [r1 (#'futon2.aif.selection-canonical-g/r1-belief
              [:p] (constantly {:status :linked :below 0}) #{[:p :ready]})]
      (is (= {#{[:p :ready]} 1} (:belief r1))))))

(deftest selection-r1-of-resolves-a-var
  (testing "claude-14 review: the live path resolves item-r1 through requiring-resolve, which
  returns a Var; selection-r1-of must hand back the function, not refuse :r1-unavailable"
    (let [item-r1 (fn [_] {:status :unlinked})]
      (with-redefs [requiring-resolve (fn [_] (intern (create-ns 'c14.fake-live-r1) 'item-r1 item-r1))]
        (binding [futon2.aif.outer-select/*r1-of* nil]
          (let [f (futon2.aif.outer-select/selection-r1-of {})]
            (is (fn? f))
            (is (= {:status :unlinked} (f "M-x")))))))))

;;; ## Admission by benefit (Joe 2026-11-27)

(def admission-observation
  {:focus "M-ready"
   :relation (fn [_ target]
               (cond (= "M-ready" target) :associated
                     (= "M-unknown" target) :useful-elsewhere
                     :else :unknown))
   :field [{:target "M-ready" :reach 1/2 :reach-source :default-reach}
           {:target "M-unknown" :reach 1/2 :reach-source :default-reach}]
   :facts {"M-ready" {:status :ready :unblocks 3 :cone 3}
           "M-unknown" {:status :ready :unblocks 0 :cone 0}}})

(def admission-makeup
  {:cascades [{:id :a :patterns ["test"] :steps [{:gate #(= "M-ready" (:target %))}]}
              {:id :b :patterns ["test"] :steps [{:gate #(= "M-unknown" (:target %))}]}]})

(deftest unlinked-pick-never-chosen-even-at-lower-g
  (testing "a's pick is unlinked, b's is linked; a has the lower G (its
  class :related outranks b's :unrelated 35/100 vs 5/100), so without
  benefit admission a would win -- the linked b is chosen instead"
    (binding [outer-select/*r1-of*
              (fn [pick]
                (if (= "M-ready" pick)
                  {:status :unlinked :why {:reason "stale"}}
                  {:status :linked :below 1 :benefits ["B17"]
                   :log-hash "h" :calibration #{:joe}}))]
      (let [r (outer-select/select admission-makeup
                                   outer-select-test/base-state
                                   admission-observation)
            a (first (filter #(= :a (:id %)) (:cascades r)))
            b (first (filter #(= :b (:id %)) (:cascades r)))]
        (is (< (:G a) (:G b)) "fixture check: the unlinked pick really has the lower G")
        (is (= {:status :chosen} (:outcome r)))
        (is (= "M-unknown" (get-in r [:selection :selected :target])))
        (is (= {:kind :no-benefit-path :why {:reason "stale"}}
               (:inadmissible-reason a)))
        (is (nil? (:inadmissible-reason b)))
        (is (= [{:pick "M-ready" :why {:reason "stale"}}]
               (:unlinked-picks r)))))))

(deftest all-unlinked-menu-refuses-typed
  (testing "no admissible class remains -> :no-benefit-linked-pick with
  each pick's :why; never a silent abstain, never an unlinked choice"
    (binding [outer-select/*r1-of*
              (constantly {:status :unlinked :why {:reason "stale"}})]
      (let [refusal (try (outer-select/select admission-makeup
                                              outer-select-test/base-state
                                              admission-observation)
                         (catch clojure.lang.ExceptionInfo e (ex-data e)))]
        (is (= :no-benefit-linked-pick (:kind refusal)))
        (is (= #{"M-ready" "M-unknown"}
               (set (map :pick (:unlinked-picks refusal)))))
        (is (every? #(= {:reason "stale"} (:why %))
                    (:unlinked-picks refusal)))
        (is (every? :inadmissible-reason
                    (get-in refusal [:selection-evidence :cascades])))))))

(deftest why-and-via-ride-in-r1
  (testing "the per-pick :r1 record keeps :why (unlinked) and :via (linked)"
    (binding [outer-select/*r1-of*
              (fn [pick]
                (if (= "M-ready" pick)
                  {:status :unlinked :why {:reason "stale"}}
                  {:status :linked :below 1 :benefits ["B17"]
                   :via [{:via :link :target "B17"}]
                   :log-hash "h" :calibration #{:joe}}))]
      (let [r (outer-select/select admission-makeup
                                   outer-select-test/base-state
                                   admission-observation)]
        (is (= {:status :unlinked :why {:reason "stale"}}
               (get-in r [:r1 "M-ready"])))
        (is (= {:status :linked :below 1 :benefits ["B17"]
                :via [{:via :link :target "B17"}]
                :log-hash "h" :calibration #{:joe}}
               (get-in r [:r1 "M-unknown"])))))))

(deftest mutant-unlinked-admissible-fails
  (testing "PLANTED MUTANT: treat :unlinked as admissible (drop the
  :inadmissible-reason marking). Run against the source: then
  unlinked-pick-never-chosen-even-at-lower-g fails because a wins on its
  lower G. Here the admission check is pinned directly: the class filter
  sees a's :inadmissible-reason"
    (binding [outer-select/*r1-of*
              (fn [pick]
                (if (= "M-ready" pick)
                  {:status :unlinked :why {:reason "stale"}}
                  {:status :linked :below 1 :benefits ["B17"]}))]
      (let [r (outer-select/select admission-makeup
                                   outer-select-test/base-state
                                   admission-observation)]
        ;; if the marking is dropped, this record is absent and a is chosen
        (is (some #(= :no-benefit-path (get-in % [:inadmissible-reason :kind]))
                  (:cascades r))
            "under the mutant no cascade carries :inadmissible-reason, so the filter admits a")))))
