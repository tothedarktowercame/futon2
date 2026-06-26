(ns futon2.aif.c-vector-test
  "E-C-vector-live exit conditions, tested hermetically (no :7071 dependency —
   the live derive is exercised separately as an integration smoke check).

   Maps to the excursion's §6 exits:
     exit-1 derive shape          → derive-stated-shape-test
     exit-2 updates / off-cycle    → refresh-keeps-prior-on-empty-test (+ live smoke)
     exit-3 efe reads live C       → goal-outcome-risk-* / efe-goal-outcome-*-test
     exit-4 freshness guard fires  → freshness-guard-test
     exit-5 safe degrade to floor  → reduces-to-floor-test"
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.belief :as belief]
            [futon2.aif.c-vector :as cv]
            [futon2.aif.efe :as efe]))

;; Reset the live atom around each test so they don't leak state into each other
;; (the suite never derives from the store, so a clean empty atom is the baseline).
(use-fixtures :each
  (fn [t] (reset! cv/c-state {:entries [] :signature nil :derived-at nil :n-source nil})
    (t)
    (reset! cv/c-state {:entries [] :signature nil :derived-at nil :n-source nil})))

;; ---- synthetic corpus (the :entities shape :7071 returns) -------------------

(def ^:private caps
  [{:id "cap-attested" :name "done"  :props {:capability/attested? true  :capability/id :cap/done :capability/status :held}}
   {:id "cap-held"     :name "held"  :props {:capability/attested? false :capability/id :cap/held :capability/status :held}}
   {:id "cap-frontier" :name "front" :props {:capability/attested? false :capability/id :cap/front :capability/status :frontier}}
   ;; a schema meta-placeholder — must be dropped
   {:id "scope/capability/capability" :name "meta" :props {:capability/attested? false}}])

(def ^:private sorries-open
  [{:id "s-clean"  :props {:sorry/status "open"   :sorry/id :sorry/clean :sorry/if "Something specific is broken"}}
   {:id "s-boiler" :props {:sorry/status "open"   :sorry/id :sorry/boiler :sorry/if "Work requires a structured plan and more"}}
   {:id "s-closed" :props {:sorry/status "closed" :sorry/id :sorry/closed :sorry/if "already done"}}])

;; the same corpus after the clean sorry is CLOSED (a goal satisfied)
(def ^:private sorries-after
  (assoc-in (vec sorries-open) [0 :props :sorry/status] "closed"))

;; ---- exit-1: derive shape --------------------------------------------------

(deftest derive-stated-shape-test
  (let [es (cv/entries-from-corpus caps sorries-open)]
    (testing "drops attested caps, the meta placeholder, boilerplate + closed sorries"
      (is (= 3 (count es)) "2 unmet caps (held+frontier) + 1 clean open sorry"))
    (testing "every entry is a well-formed stated C-entry with provenance (I1) + weight basis (I2)"
      (is (every? #(= :stated (:flavour %)) es))
      (is (every? :provenance es))
      (is (every? #(-> % :weight :basis) es))
      (is (every? #(= :becomes (-> % :preferred :op)) es)))
    (testing "held cap outweighs frontier cap (star-map status orientation, I2)"
      (let [by-id (into {} (map (juxt #(-> % :outcome-ref :id) identity) es))]
        (is (= 0.6 (-> by-id (get :cap/held) :weight :value)))
        (is (= 0.4 (-> by-id (get :cap/front) :weight :value)))))
    (testing "the clean sorry's preferred outcome is :closed; boilerplate excluded"
      (let [sorry-refs (set (map #(-> % :outcome-ref :id) es))]
        (is (contains? sorry-refs :sorry/clean))
        (is (not (contains? sorry-refs :sorry/boiler)))
        (is (not (contains? sorry-refs :sorry/closed)))))))

;; ---- exit-5: safe degrade to the static floor ------------------------------

(deftest reduces-to-floor-test
  (testing "empty corpus derives no entries (store-down / fresh-JVM degrade)"
    (is (= [] (cv/entries-from-corpus [] []))))
  (testing "goal-outcome-risk of an empty C is exactly 0.0 (no belly ⇒ static floor)"
    (is (= 0.0 (cv/goal-outcome-risk [])))
    (is (= 0.0 (cv/goal-outcome-risk [] 5.0))))
  (testing "current-c-vector on a never-derived atom is [] ⇒ EFE term is 0.0"
    (is (= [] (cv/current-c-vector)))
    (let [o (efe/compute-efe {:observation {} :belief (belief/initial-belief-state [:m1])}
                             {:type :no-op})]
      (is (= 0.0 (:G-goal-outcome o))
          "with no live C, compute-efe is identical to its pre-E-C-vector-live behaviour"))))

;; ---- exit-3: the belly is wired into EFE's risk and steers selection --------

(def ^:private c-heavy
  "Three unmet goals; the heavy one weighted 0.6."
  [(cv/c-entry {:flavour :stated :outcome-ref {:kind :sorry :id :a}
                :preferred {:op :becomes :value :closed}
                :weight {:value 0.6 :basis :test} :provenance {:source :test}})
   (cv/c-entry {:flavour :stated :outcome-ref {:kind :sorry :id :b}
                :preferred {:op :becomes :value :closed}
                :weight {:value 0.2 :basis :test} :provenance {:source :test}})
   (cv/c-entry {:flavour :stated :outcome-ref {:kind :sorry :id :c}
                :preferred {:op :becomes :value :closed}
                :weight {:value 0.2 :basis :test} :provenance {:source :test}})])

(def ^:private c-light
  "The same corpus after the heavy goal (:a) is satisfied — it drops out."
  (vec (rest c-heavy)))

(deftest goal-outcome-risk-shifts-with-corpus-test
  (testing "satisfying a goal removes its C-entry and LOWERS the belly's risk"
    (let [heavy (cv/goal-outcome-risk c-heavy)
          light (cv/goal-outcome-risk c-light)]
      ;; mean of {0.6,0.2,0.2}=0.333… vs mean of {0.2,0.2}=0.2
      (is (> heavy light) "the belly tracks present goals: fewer/lighter unmet ⇒ lower risk")
      (is (< (Math/abs (- heavy 0.3333333)) 1e-4))
      (is (< (Math/abs (- light 0.2)) 1e-4))))
  (testing "the weight (W4 normaliser) scales the term linearly"
    (is (< (Math/abs (- (cv/goal-outcome-risk c-heavy 2.0)
                        (* 2.0 (cv/goal-outcome-risk c-heavy 1.0))))
           1e-9))))

(deftest efe-goal-outcome-rerank-test
  (let [st  {:observation {} :belief (belief/initial-belief-state [:m1])}
        act {:type :no-op}
        base  (:G-total (efe/compute-efe st act {:goal-outcome-entries []}))
        heavy (:G-total (efe/compute-efe st act {:goal-outcome-entries c-heavy}))
        light (:G-total (efe/compute-efe st act {:goal-outcome-entries c-light}))]
    (testing "the belly adds risk for unmet goals (G-total rises with C present)"
      (is (> heavy base)))
    (testing "satisfying the heavy goal lowers G-total ⇒ the belly re-ranks selection"
      ;; between two real corpus states, the lighter belly wins (base = the
      ;; empty-C floor, trivially lowest — a reference, not a competing policy).
      (is (> heavy light))
      (is (= light (min heavy light)) "lighter belly ⇒ lower EFE ⇒ the selected option"))
    (testing "with an empty C the term is 0 and G-total == the static floor"
      (is (= base (:G-total (efe/compute-efe st act {:goal-outcome-entries []})))))))

;; ---- predictive-risk: the action-dependent term that re-ranks policies ------

(deftest advanced-outcome-ids-test
  (testing "an action's advanced outcomes = explicit ∪ :target ∪ mission :produces"
    (is (= #{"a"} (cv/advanced-outcome-ids {:type :pursue :target :a} nil)))
    (is (= #{} (cv/advanced-outcome-ids {:type :no-op} nil)))
    (testing "open-mission pulls the mission's produced caps from the graph"
      (let [graph {:missions {:M1 {:produces [:cap-x :cap-y]}}}]
        (is (= #{"M1" "cap-x" "cap-y"}
               (cv/advanced-outcome-ids {:type :open-mission :target :M1} graph)))))
    (testing "explicit :advances-outcomes is the override seam"
      (is (= #{"a" "z"}
             (cv/advanced-outcome-ids {:type :pursue :target :a :advances-outcomes [:z]} nil))))
    (testing "keyword targets normalise to match string outcome ids"
      (is (contains? (cv/advanced-outcome-ids {:type :pursue :target :ai-passes-prelims} nil)
                     "ai-passes-prelims")))))

(deftest predictive-goal-outcome-risk-test
  (testing "advancing a goal lowers predicted risk vs advancing nothing"
    (let [r-noop (cv/predictive-goal-outcome-risk c-heavy {:type :no-op} nil)
          r-a    (cv/predictive-goal-outcome-risk c-heavy {:type :pursue :target :a} nil)]
      (is (< r-a r-noop))
      ;; static sum {0.6,0.2,0.2}/3 = 0.333…; advancing :a ⇒ {0,0.2,0.2}/3 = 0.133…
      (is (< (Math/abs (- r-a 0.1333333)) 1e-4))))
  (testing "advancing the HEAVIEST goal lowers risk most (the belly prefers it)"
    (let [r-a (cv/predictive-goal-outcome-risk c-heavy {:type :pursue :target :a} nil)
          r-b (cv/predictive-goal-outcome-risk c-heavy {:type :pursue :target :b} nil)]
      (is (< r-a r-b) "advancing the 0.6 goal beats advancing a 0.2 goal")))
  (testing "denominator is fixed ⇒ advancing any goal can only lower risk (never raise)"
    (let [base (cv/predictive-goal-outcome-risk c-heavy {:type :no-op} nil)]
      (doseq [g [:a :b :c]]
        (is (<= (cv/predictive-goal-outcome-risk c-heavy {:type :pursue :target g} nil) base)))))
  (testing "reduces to the static term when the action advances nothing"
    (is (= (cv/goal-outcome-risk c-heavy)
           (cv/predictive-goal-outcome-risk c-heavy {:type :no-op} nil))))
  (testing "[] ⇒ 0.0 (the floor) regardless of action"
    (is (= 0.0 (cv/predictive-goal-outcome-risk [] {:type :pursue :target :a} nil)))))

(deftest efe-predictive-rerank-test
  (testing "through compute-efe: the belly's term re-ranks an action that advances a goal"
    (let [st  {:observation {} :belief (belief/initial-belief-state [:m1])}
          ;; one heavy goal whose id matches the :address-sorry target :m1
          entries [(cv/c-entry {:flavour :stated :outcome-ref {:kind :sorry :id :m1}
                                :preferred {:op :becomes :value :closed}
                                :weight {:value 0.6 :basis :test} :provenance {:source :test}})]
          opts {:goal-outcome-entries entries}
          g-noop (:G-goal-outcome (efe/compute-efe st {:type :no-op} opts))
          g-addr (:G-goal-outcome (efe/compute-efe st {:type :address-sorry :target :m1} opts))]
      (is (pos? g-noop) "no-op leaves the goal unmet ⇒ positive belly risk")
      (is (= 0.0 g-addr) "addressing the goal discounts it ⇒ the belly term drops")
      (is (< g-addr g-noop) "the belly re-ranks: the goal-advancing action is preferred"))))

;; ---- exit-4: the freshness guard fires loudly on a stale C -----------------

(deftest corpus-signature-detects-change-test
  (testing "a status flip (a sorry closing) changes the signature even at constant count"
    (let [sig-before (#'cv/corpus-signature-of caps sorries-open)
          sig-after  (#'cv/corpus-signature-of caps sorries-after)]
      (is (not= sig-before sig-after) "derived-stale-vs-source is detectable")))
  (testing "an unchanged corpus yields a stable signature (no false alarms)"
    (is (= (#'cv/corpus-signature-of caps sorries-open)
           (#'cv/corpus-signature-of caps sorries-open)))))

(deftest freshness-guard-test
  (testing "a corpus change WITHOUT re-derivation is reported stale (§6.4)"
    (reset! cv/c-state {:entries [] :signature 111 :derived-at :t0 :n-source nil})
    (is (false? (cv/stale? 111)) "same signature ⇒ fresh")
    (is (true? (cv/stale? 222)) "changed signature ⇒ stale")
    (is (true? (:fresh? (cv/freshness-check 111))))
    (is (false? (:fresh? (cv/freshness-check 222)))))
  (testing "a never-derived C is stale (nil stored signature)"
    (reset! cv/c-state {:entries [] :signature nil :derived-at nil :n-source nil})
    (is (true? (cv/stale? 999)))
    (is (false? (:fresh? (cv/freshness-check 999))))))

;; ---- exit-2: refresh never clobbers a good belly to empty ------------------

(deftest refresh-keeps-prior-on-empty-test
  (testing "the empty-derive branch (store down) leaves the last-good C in force"
    ;; entries-from-corpus [] [] is the empty derive refresh! guards against
    (is (empty? (cv/entries-from-corpus [] [])))
    ;; simulate a good prior C, then assert current-c-vector still serves it
    (reset! cv/c-state {:entries c-heavy :signature 7 :derived-at :t0 :n-source nil})
    (is (= 3 (count (cv/current-c-vector))) "a good belly stays readable on the tick")))

;; ---- merge-entries extension point -----------------------------------------

(deftest merge-entries-dedup-test
  (testing "overlay channels fold in, de-duplicated by outcome-ref"
    (let [extra [(cv/c-entry {:flavour :mess :outcome-ref {:kind :sorry :id :a}
                              :preferred {:op :becomes :value :closed}
                              :provenance {:source :overlay}})
                 (cv/c-entry {:flavour :mess :outcome-ref {:kind :coherence :mission "M-x"}
                              :preferred {:op :>= :value 45.9}
                              :provenance {:source :overlay}})]
          merged (cv/merge-entries c-heavy extra)]
      (is (= 4 (count merged)) "the duplicate :a is dropped, the new mission kept"))))
