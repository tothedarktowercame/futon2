(ns futon2.aif.wm.q10-arrangement-census-route-test
  "Requirements Q10 on the production route. The runner seals a :q10 census
  of the selected task's construction: every pair of constructed policies
  over the same pattern set, typed as a compared control or refused, bound
  to the verified Q2 carrier, the Q6/Q8 stages and the compared G rows.
  The exporter reads only a verified census. The runs are the Q8 route
  test's production clicks."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.policy-prefix-admission :as prefix-admission]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.q10-arrangement-census :as q10]
            [futon2.aif.q2-construction :as q2]
            [futon2.aif.q8-frontier :as q8]
            [futon2.aif.q9-factorization :as q9]
            [futon2.aif.wm.q8-frontier-route-test :as q8-route]
            [futon2.test-support.reading-route :as route]
            [futon2.test-support.runner-fixture :as fixture]
            [wm-check-run-facts :as lean-term]
            [wm-report-card :as card]
            [wm-run-facts :as facts]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each
  (fn [run]
    (binding [runner/*wm-status-reporting?* false]
      (route/with-empty-learning-ledger run))))

(def click! @#'q8-route/click!)

(def fields ["differentArrangementPairs" "arrangementPairsDistinguishedByG"])

(defn- export [record]
  (json/parse-string (json/generate-string (facts/facts-for-record record "r" {}))))

(defn- q10-term [record]
  (re-find #"q10 := .*" (lean-term/lean-source (export record) "q10")))

(defn- verdict [record]
  (:status (first (filter #(= :Q10 (:requirement %))
                          (card/verdict-rows (facts/facts-for-record record "r" {}))))))

(defn- nr? [v] (and (map? v) (contains? v "not-recomputable")))

(defn- not-recomputable?
  "Every Q10 field of RECORD's export is not recomputable, and the Lean term
  says so."
  [record]
  (let [f (get (export record) "facts")]
    (and (every? #(nr? (get f %)) fields)
         (some? (re-find #"q10 := \.nr" (q10-term record)))
         (= :not-recomputable (verdict record)))))

(defn- counts [record] [(get-in (export record) ["facts" "differentArrangementPairs"])
                        (get-in (export record) ["facts" "arrangementPairsDistinguishedByG"])])

(defn- reseal
  "RECORD with its :q10 census recomputed after F rewrites the record, as a
  consistent forger would."
  [record f]
  (let [r (f record)]
    (assoc r :q10 (q10/certificate (dissoc r :q10)))))

(defn- reseal-census
  "Rewrite the decision's sealed Q2 construction census with F, reseal it,
  the Q2 carrier and the Q10 census."
  [record f]
  (reseal record
          (fn [r]
            (let [r (update-in r [:decision :selection-certificate :q2-construction]
                               #(previous-run/seal (f (dissoc % :sha256))))]
              (assoc r :q2 (q2/certificate (q2/record-inputs r)))))))

(defn- task [record] (get-in record [:decision :chosen :target]))
(defn- stages-path [record k]
  [:decision :selection-certificate :policy-stages (task record) k])

(defn- two-clicks
  ([] (two-clicks ["p/a" "p/b"]))
  ([read]
   (let [fx (route/fixture read) dir (route/temp-dir "q10-stream-")]
     [(:record (click! fx dir)) (:record (click! fx dir))])))

;; -- the arrangement, read from the key ---------------------------------------------

(deftest arrangements-compare-up-to-occurrence-names
  (let [k (fn [id s] [:wm/reading-policy-v1 "T" id s])
        precedes (k "T/policy-1" {:nodes [[["p/a" 0] "p/a"] [["p/b" 1] "p/b"]]
                                  :edges [[["p/a" 0] ["p/b" 1] :precedes]]
                                  :order [["p/a" 0] ["p/b" 1]]})
        renamed (k "T/policy-2" {:nodes [["p/a" "p/a"] ["p/b" "p/b"]]
                                 :edges [["p/a" "p/b" :precedes]] :order ["p/a" "p/b"]})
        overlap (k "T/policy-3" {:nodes [["p/a" "p/a"] ["p/b" "p/b"]]
                                 :edges [["p/a" "p/b" :overlap]] :order ["p/a" "p/b"]})
        twice (k "T/policy-4" {:nodes [[["p/a" 0] "p/a"] [["p/a" 1] "p/a"] [["p/b" 2] "p/b"]]
                               :edges [] :order [["p/a" 0] ["p/a" 1] ["p/b" 2]]})]
    (is (= ["p/a" "p/b"] (q10/pattern-set precedes) (q10/pattern-set twice)))
    (is (= (q10/arrangement precedes) (q10/arrangement renamed)))
    (is (not= (q10/arrangement precedes) (q10/arrangement overlap)))
    (is (= :occurrence (:by (q10/arrangement twice))))))

;; -- the production route ---------------------------------------------------------

(deftest the-first-click-compares-one-control
  (let [[one] (two-clicks)
        c (:q10 one)
        q2c (:q2 one)
        entry (first (:entries q2c))]
    (is (= {:status :verified :carrier c} (q10/verify-record one)))
    (is (= :counted (:status c)))
    (is (= (:run/id one) (:run/id c)))
    (testing "the population is Q2's construction, every member typed"
      (is (= (set (:policies entry)) (set (map :key (:population c)))))
      (is (= #{:compared} (set (map :status (:population c)))))
      (is (= (assoc (select-keys entry [:reading-id :graph-id :library-id :kernel-id])
                    :q2 (:sha256 q2c))
             (:pins c))))
    (testing "one control: the ordered and the overlapping arrangement of p/a, p/b"
      (let [[pair & more] (:pairs c)
            gs (into {} (map (fn [r] [(nth (:policy-key (:id r)) 2) (:g r)]))
                     (get-in one [:decision :selection-certificate :candidates]))]
        (is (empty? more))
        (is (= ["p/a" "p/b"] (:patterns pair)))
        (is (= :control (:status pair)))
        (is (true? (:distinguished pair)))
        (is (= (mapv #(gs (nth % 2)) (:members pair)) (:g pair)) "each member's own G row")
        (is (true? (:g-differs pair)))
        (is (= #{:precedes :overlap}
               (set (for [m (:population c) [_ _ kind] (get-in m [:arrangement :edges])] kind))))
        (is (true? (:selected-in-control c)))))
    (testing "export, Lean and the report card: Q10 holds"
      (is (= [1 1] (counts one)))
      (is (= "q10 := .known { Requirements.click20 with differentArrangementPairs := 1, arrangementPairsDistinguishedByG := 1 }"
             (q10-term one)))
      (is (= :pass (verdict one)))
      (is (true? (q10/lean-q10 c))))
    (testing "Q2 and Q9 still verify beside it"
      (is (= :verified (:status (q2/verify-record one))))
      (is (= :verified (:status (q9/verify-record one)))))))

(deftest a-repeat-after-refusal-leaves-no-compared-control
  (let [[_ two] (two-clicks)
        c (:q10 two)
        excluded (get-in two (stages-path two :admitted))
        [pair] (:pairs c)]
    (is (= :verified (:status (q10/verify-record two))))
    (testing "Q6 removed the previous choice, which the census types, not drops"
      (is (= 2 (count (:population c))))
      (is (= #{:compared :requirement-excluded} (set (map :status (:population c)))))
      (is (= #{:previous-choice-refused-on-same-inputs}
             (set (keep :reason (:population c)))))
      (is (= 2 (count excluded))))
    (testing "the same pair is refused: one member was not compared"
      (is (= :refused (:status pair)))
      (is (= :member-not-compared (:reason pair)))
      (is (= [:requirement-excluded] (mapv :status (:members-not-compared pair)))))
    (testing "the Q8 carrier holds the same stages"
      (is (= :verified (:status (q8/verify-record two)))))
    (is (= [0 0] (counts two)))
    (is (= :fail (verdict two)))
    (is (false? (q10/lean-q10 c)))))

(deftest different-pattern-sets-are-not-controls
  (let [[one] (two-clicks ["p/a" "p/c"])
        c (:q10 one)]
    (is (= :verified (:status (q10/verify-record one))))
    (is (= #{["p/a" "p/c"] ["p/a" "p/b" "p/c"]} (set (map :patterns (:population c)))))
    (is (empty? (:pairs c)))
    (is (= [0 0] (counts one)))
    (is (= :fail (verdict one)))))

;; -- a reading whose arrangements share a root: controls survive Q6 -----------------

(defn- verdict-of [q record]
  (:status (first (filter #(= q (:requirement %))
                          (card/verdict-rows (facts/facts-for-record record "r" {}))))))

(deftest a-shared-root-reading-keeps-a-control-through-q6
  ;; p/a, then p/a and p/b over one span: three arrangements of {p/a, p/b}
  ;; (the reading's chain, its overlap reading, the retraction's overlap)
  ;; and a chain over {p/a}. Their co-apply roots coincide, so the gate must
  ;; pool them as the selector does, or the click is refused.
  (let [fx (route/fixture [["p/a"] ["p/a" "p/b"]])
        dir (route/temp-dir "q10-shared-root-")
        clicks (vec (repeatedly 3 #(click! fx dir)))
        [one two three] (mapv :record clicks)
        excluded (fn [r] (filter #(= :requirement-excluded (:status %)) (get-in r [:q10 :population])))]
    (testing "every click selects once, through the gate, and records the selection"
      (is (= [1 1 1] (mapv :calls clicks)))
      (doseq [r [one two three]]
        (is (map? (get-in r [:decision :chosen])))
        (is (= :cascade-selected (get-in r [:selection-event :event])))
        (is (= (get-in r [:decision :chosen :target]) (get-in r [:selection-event :target])))
        (is (= :pass (verdict-of :Q7 r)) "no :selection-not-reached absence for a reached selection")
        (is (= {:status :verified :carrier (:q10 r)} (q10/verify-record r)))))
    (testing "run 1: four policies compared, three controls over {p/a, p/b}"
      (is (= [["p/a"] ["p/a" "p/b"] ["p/a" "p/b"] ["p/a" "p/b"]]
             (sort (map :patterns (get-in one [:q10 :population])))))
      (is (= [3 3] (counts one)))
      (is (= :pass (verdict one))))
    (testing "the census's selected policy is the chosen one, not the posterior's mode"
      ;; three {p/a, p/b} policies share p/a's root with the {p/a} chain, so
      ;; the pooled marginal chooses the chain while the mode is an overlap
      (let [chosen (fn [r] (nth (get-in r [:q10 :selected]) 2))]
        (is (= (get-in one [:decision :chosen :id]) (chosen one)))
        (is (not= (chosen one)
                  (get-in one [:decision :selection-law :per-policy-argmax :action :id]))
            "the fixture really separates the chosen policy from the mode")
        (doseq [r [one two three]]
          (is (= (get-in r [:decision :chosen :id]) (chosen r)))
          (is (= (chosen r) (nth (get-in r [:q9 :selected]) 2))))))
    (testing "run 2: Q6 removed run 1's choice; the three controls remain, beside Q8"
      (is (= [(get-in one [:q10 :selected])] (map :key (excluded two))))
      (is (= :verified (:status (q8/verify-record two))))
      (is (= [3 3] (counts two)))
      (is (= :pass (verdict two))))
    (testing "run 3: Q6 removed a control member; its pairs are refused, one control holds"
      (let [[gone] (excluded three)
            pairs (get-in three [:q10 :pairs])]
        (is (= (get-in two [:q10 :selected]) (:key gone)))
        (is (= ["p/a" "p/b"] (:patterns gone)))
        (is (= {:refused 2 :control 1} (frequencies (map :status pairs))))
        (is (every? #(= :member-not-compared (:reason %)) (filter #(= :refused (:status %)) pairs)))
        (is (= :verified (:status (q8/verify-record three))))
        (is (= [1 1] (counts three)))
        (is (= :pass (verdict three)))))))

;; -- what the census refuses to count ----------------------------------------------

(deftest refusals-are-typed-and-not-counted
  (let [[one] (two-clicks)
        other (first (remove #{(get-in one [:q10 :selected])} (map :key (get-in one [:q10 :population]))))]
    (testing "a member the scorer refused, with its failure, is :not-scored"
      (let [r (reseal one (fn [r]
                            (-> r
                                (update-in (stages-path r :scored) (partial filterv #(not= other %)))
                                (update-in (stages-path r :admitted) (partial filterv #(not= other %)))
                                (update-in [:decision :selection-law :posterior] dissoc
                                           (some #(when (= other (prefix-admission/candidate-key %)) %)
                                                 (keys (get-in r [:decision :selection-law :posterior]))))
                                (update-in [:decision :selection-certificate :family-failures]
                                           conj {:kind :policy-scoring-refused :policy-id (nth other 2)}))))
            [pair] (get-in r [:q10 :pairs])]
        (is (= :counted (get-in r [:q10 :status])))
        (is (= [{:key other :status :not-scored :reason :policy-scoring-refused}]
               (:members-not-compared pair)))
        (is (= [0 0] (counts r)))))
    (testing "a compared member without its own numeric G is counted, not distinguished"
      (let [r (reseal one #(update-in % [:decision :selection-certificate :candidates]
                                      (fn [rows] (mapv (fn [row] (if (= other (prefix-admission/candidate-key (:id row)))
                                                                   (dissoc row :g) row))
                                                       rows))))
            [pair] (get-in r [:q10 :pairs])]
        (is (= [:control false :g-not-numeric] ((juxt :status :distinguished :reason) pair)))
        (is (= [1 0] (counts r)))
        (is (= :fail (verdict r)))))
    (testing "a G row disagreeing with the family summary is not that policy's own"
      (let [r (reseal one #(update-in % [:decision :selection-certificate :candidates]
                                      (partial mapv (fn [row] (update row :g + 1.0)))))]
        (is (= :g-not-the-family-summary's (:reason (first (get-in r [:q10 :pairs])))))
        (is (= [1 0] (counts r)))))))

;; -- planted controls: omission, duplication, foreign, stale, hidden, mismatch -----

(deftest forged-populations-are-not-recomputable
  (let [[one two] (two-clicks)
        sel (get-in one [:q10 :selected])
        other (first (remove #{sel} (map :key (get-in one [:q10 :population]))))
        foreign [:wm/reading-policy-v1 "M-current" "M-current/policy-foreign"
                 {:nodes [["p/a" "p/a"] ["p/b" "p/b"]] :edges [["p/b" "p/a" :precedes]]
                  :order ["p/b" "p/a"]}]]
    (doseq [[label rec reason]
            [[:omitted-from-stages
              (reseal one #(update-in % (stages-path % :constructed) (partial filterv #{sel})))
              :constructed-population-not-q2's]
             [:duplicated-in-stages
              (reseal one #(update-in % (stages-path % :constructed) (fn [ks] (conj (vec ks) (first ks)))))
              :constructed-policy-duplicated]
             [:foreign-compared
              (reseal one #(assoc-in % [:decision :selection-law :posterior
                                        (assoc (key (first (get-in % [:decision :selection-law :posterior])))
                                               :id "M-current/policy-foreign"
                                               :policy-key foreign)]
                                     0.0))
              :foreign-compared-policy]
             [:hidden-refusal
              (reseal one (fn [r]
                            (-> r
                                (update-in (stages-path r :scored) (partial filterv #(not= other %)))
                                (update-in [:decision :selection-law :posterior] dissoc
                                           (some #(when (= other (prefix-admission/candidate-key %)) %)
                                                 (keys (get-in r [:decision :selection-law :posterior])))))))
              :hidden-refusal]
             [:compared-outside-stages
              (reseal one #(update-in % (stages-path % :scored) (partial filterv #{sel})))
              :compared-not-scored-minus-q6-excluded]
             [:omitted-from-q2-census
              (reseal-census one (fn [census] (update-in census [:entries 0 :policies] (comp vec rest))))
              :construction-policy-omitted]
             [:stale-reading-pin
              (reseal-census one (fn [census] (assoc-in census [:entries 0 :reading :analysis-digest] "stale")))
              :construction-reading-not-the-field's]
             [:stale-graph-pin
              (reseal-census one (fn [census] (assoc-in census [:entries 0 :graph-digest] "stale")))
              :construction-graph-not-the-field's]
             [:q2-carrier-forged
              (reseal one #(update-in % [:q2 :entries 0 :policies] (comp vec rest)))
              :q2-carrier-tampered]
             [:q2-carrier-resealed
              (reseal one #(update % :q2 (fn [c] (previous-run/seal (-> (dissoc c :sha256)
                                                                        (update-in [:entries 0 :policies] (comp vec rest)))))))
              :q2-carrier-not-bound-to-record]
             [:q8-stages-mismatched
              (reseal two #(update % :q8 (fn [c] (previous-run/seal (-> (dissoc c :sha256)
                                                                        (assoc-in [:cascade :requirement-excluded] #{}))))))
              :q8-carrier-not-bound-to-record]
             [:q8-carrier-absent (reseal two #(dissoc % :q8)) :q8-carrier-absent]
             [:q6-carrier-edited
              (reseal two #(assoc-in % [:decision :selection-certificate :q6 :reason] :because))
              :q6-carrier-absent-or-tampered]
             [:selected-not-compared
              ;; run two's choice set to run one's, which Q6 excluded; Q2
              ;; reads the choice, refuses it, and Q10 carries Q2's reason
              (reseal two (fn [r]
                            (let [r (assoc-in r [:decision :chosen] (get-in one [:decision :chosen]))]
                              (assoc r :q2 (q2/certificate (q2/record-inputs r))))))
              :chosen-action-not-compared]
             [:other-route
              (reseal one #(update-in % [:decision :selection-certificate] dissoc :selection-route))
              :not-the-reading-family-route]]]
      (testing (name label)
        (is (= :not-recomputable (get-in rec [:q10 :status])))
        (is (= reason (get-in rec [:q10 :reason])))
        (is (= reason (:reason (q10/verify-record rec))))
        (when-not (= :other-route label)
          (is (not-recomputable? rec)))))))

(deftest absent-tampered-and-foreign-censuses-are-not-recomputable
  (let [[one two] (two-clicks)]
    (doseq [[label rec reason]
            [[:absent (dissoc one :q10) :q10-carrier-absent]
             [:tampered (assoc-in one [:q10 :counts :distinguished-by-g] 2) :q10-carrier-tampered]
             [:counts-resealed
              (update one :q10 #(q10/seal (assoc-in (dissoc % :sha256) [:counts :different-arrangement-pairs] 2)))
              :q10-carrier-not-bound-to-record]
             [:pair-dropped
              (update one :q10 #(q10/seal (-> (dissoc % :sha256) (assoc :pairs [])
                                              (assoc :counts {:different-arrangement-pairs 0
                                                              :distinguished-by-g 0}))))
              :q10-carrier-not-bound-to-record]
             [:pins-changed
              (update one :q10 #(q10/seal (assoc-in (dissoc % :sha256) [:pins :reading-id] "stale")))
              :q10-carrier-not-bound-to-record]
             [:foreign (assoc two :q10 (:q10 one)) :q10-carrier-not-bound-to-record]
             [:wrong-schema (update one :q10 #(q10/seal (assoc (dissoc % :sha256) :schema :wm/q10-v0)))
              :q10-carrier-tampered]]]
      (testing (name label)
        (is (= reason (:reason (q10/verify-record rec))))
        (is (not-recomputable? rec))))))

(deftest an-abstention-counts-nothing
  (let [fx (route/fixture)]
    (spit (io/file (:root fx) (str route/target ".md")) "# Current\n\n## HEAD\nChanged work.\n")
    (let [rec (:record (click! fx (route/temp-dir "q10-stream-")))]
      (is (= :abstained (get-in rec [:decision :abstention :status])))
      (is (= :not-recomputable (get-in rec [:q10 :status])))
      (is (not= :verified (:status (q10/verify-record rec))))
      (is (not-recomputable? rec)))))
