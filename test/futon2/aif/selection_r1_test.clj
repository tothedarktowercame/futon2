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
  (testing "an item-r1 :refused refuses selection, typed, with the target"
    (binding [outer-select/*r1-of* (fn [pick]
                                     (if (= "M-ready" pick)
                                       {:status :refused :reason :no-A-table}
                                       {:status :unlinked}))]
      (let [refusal (try (outer-select/select outer-select-test/base-makeup
                                              outer-select-test/base-state
                                              outer-select-test/base-observation)
                         (catch clojure.lang.ExceptionInfo e (ex-data e)))]
        (is (= :r1-refused (:kind refusal)))
        (is (= "M-ready" (:target refusal)))
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
        (is (nil? (get-in r [:r1 "M-unknown"]))
            "an unlinked item never reaches the scorer; its exclusion row carries the reason")
        (is (= :no-benefit-path (get-in r [:outer-score/excluded 0 :reason])))
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
  (testing "the field filter admits only benefit-linked items: M-ready is
  unlinked (and would have the lower G: its class :related outranks b's
  :unrelated 35/100 vs 5/100), so it leaves the field before any cascade
  picks, and the linked b is chosen"
    (binding [outer-select/*r1-of*
              (fn [pick]
                (if (= "M-ready" pick)
                  {:status :unlinked :why {:reason "stale"}}
                  {:status :linked :below 1 :benefits ["B17"]
                   :log-hash "h" :calibration #{:joe}}))]
      (let [r (outer-select/select admission-makeup
                                   outer-select-test/base-state
                                   admission-observation)
            b (first (filter #(= :b (:id %)) (:cascades r)))]
        (is (= {:status :chosen} (:outcome r)))
        (is (= "M-unknown" (get-in r [:selection :selected :target]))
            "the pick, not just 'no refusal': the linked item is what runs")
        (is (= [{:target "M-ready" :reason :no-benefit-path :why {:reason "stale"}}]
               (:outer-score/excluded r)))
        (is (= ["M-unknown"] (:outer-score/admitted r)))
        (is (nil? (:inadmissible-reason b)))
        (is (empty? (:unlinked-picks r))
            "the backstop list stays empty: the field filter caught it first")))))

(deftest all-unlinked-menu-refuses-typed
  (testing "an all-unlinked field refuses :outer-score/no-admitted-tasks,
  listing each exclusion with its :why and the :no-benefit-path count, so
  the operator sees why the field emptied"
    (binding [outer-select/*r1-of*
              (constantly {:status :unlinked :why {:reason "stale"}})]
      (let [refusal (try (outer-select/select admission-makeup
                                              outer-select-test/base-state
                                              admission-observation)
                         (catch clojure.lang.ExceptionInfo e (ex-data e)))]
        (is (= :outer-score/no-admitted-tasks (:kind refusal)))
        (is (= 2 (:no-benefit-path refusal)))
        (is (= #{"M-ready" "M-unknown"}
               (set (map :target (:outer-score/excluded refusal)))))
        (is (every? #(and (= :no-benefit-path (:reason %))
                          (= {:reason "stale"} (:why %)))
                    (:outer-score/excluded refusal)))))))

(deftest why-and-via-ride-in-r1
  (testing "the :r1 record keeps :via (linked); an unlinked item's :why
  rides in its :outer-score/excluded row (it never reaches the scorer)"
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
        (is (= {:status :linked :below 1 :benefits ["B17"]
                :via [{:via :link :target "B17"}]
                :log-hash "h" :calibration #{:joe}}
               (get-in r [:r1 "M-unknown"])))
        (is (= {:reason "stale"}
               (get-in r [:outer-score/excluded 0 :why])))))))

(deftest mutant-unlinked-admissible-fails
  (testing "PLANTED MUTANT: skip the benefit partition (every candidate
  stays in :field). Run against the source: then
  unlinked-pick-never-chosen-even-at-lower-g fails -- either a is chosen
  on its lower G, or the pick-level backstop fires
  :no-benefit-linked-pick; the test asserts the pick, so both mutant
  outcomes fail it. Here the partition is pinned directly:"
    (binding [outer-select/*r1-of*
              (fn [pick]
                (if (= "M-ready" pick)
                  {:status :unlinked :why {:reason "stale"}}
                  {:status :linked :below 1 :benefits ["B17"]}))]
      (let [r (outer-select/select admission-makeup
                                   outer-select-test/base-state
                                   admission-observation)]
        (is (some #(= :no-benefit-path (:reason %)) (:outer-score/excluded r))
            "under the mutant nothing is excluded for :no-benefit-path")))))

(deftest field-partition-filters-the-field
  (testing "one linked and two unlinked items: the gate-free menu's cascade
  can only ever pick the linked one, and both unlinked items appear in
  :outer-score/excluded with :no-benefit-path and their :why"
    (let [observation {:focus "M-linked"
                       :relation (fn [_ _] :associated)
                       :field [{:target "M-u1" :reach 1/2 :reach-source :default-reach}
                               {:target "M-linked" :reach 1/2 :reach-source :default-reach}
                               {:target "M-u2" :reach 1/2 :reach-source :default-reach}]
                       :facts {"M-u1" {:status :ready :unblocks 5 :cone 5}
                               "M-linked" {:status :ready :unblocks 0 :cone 0}
                               "M-u2" {:status :ready :unblocks 3 :cone 3}}}
          makeup {:cascades [{:id :menu :patterns ["test"] :steps [:ready-gate]}]}]
      (binding [outer-select/*r1-of*
                (fn [pick]
                  (if (= "M-linked" pick)
                    {:status :linked :below 1 :benefits ["B17"]}
                    {:status :unlinked :why {:reason (str "stale-" pick)}}))]
        (let [r (outer-select/select makeup
                                     outer-select-test/base-state
                                     observation)]
          (is (= {:status :chosen} (:outcome r)))
          (is (= "M-linked" (get-in r [:selection :selected :target]))
              "the pick, not just 'no refusal' -- under the skip-partition mutant this is M-u1 (or the backstop refuses)")
          (is (= ["M-linked"] (:outer-score/admitted r)))
          (is (= #{"M-u1" "M-u2"}
                 (set (map :target (:outer-score/excluded r)))))
          (is (every? #(= :no-benefit-path (:reason %))
                      (:outer-score/excluded r)))
          (is (= {:reason "stale-M-u1"}
                 (some #(when (= "M-u1" (:target %)) (:why %))
                       (:outer-score/excluded r)))))))))
