(ns futon2.aif.observation-rates-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.observation-rates :as rates]
            [futon2.aif.cascade-model-manifest :as manifest]
            [futon2.report.war-machine :as wm]))

(def contract
  {:schema :wm/observation-contract-v1
   :classes [{:id :C5 :kind :checkable :token "named entry in the contract bundle"}
             {:id :J :kind :judgement :question "..."}
             {:id :U :kind :judgement :question "..."}
             {:id :V :kind :judgement :question "..."}]})

(def labels
  ;; class :J — 2 subjects, 5 labels. One label has an admitted reference
  ;; but NO recorded verdict: it counts towards coverage only, and towards
  ;; no denominator. Compared admitted-absent cells: 2, of which 1 recorded
  ;; true (raw false-pos 1/2). Compared admitted-present cells: 2, of which
  ;; 1 recorded false (raw false-neg 1/2).
  [{:token-class :J :recorded true :admitted :absent}
   {:token-class :J :recorded false :admitted :absent}
   {:token-class :J :recorded false :admitted :present}
   {:token-class :J :recorded true :admitted :present}
   {:token-class :J :recorded nil :admitted :absent}
   ;; class :K — observed but zero errors: raw rates 0/3 and 0/1, not smoothed.
   {:token-class :K :recorded false :admitted :absent}
   {:token-class :K :recorded false :admitted :absent}
   {:token-class :K :recorded false :admitted :absent}
   {:token-class :K :recorded true :admitted :present}
   ;; class :U — labels but none admitted: wholly unobserved.
   {:token-class :U :recorded true :admitted nil}
   ;; class :V — admitted on one direction only: false-neg cell observed
   ;; (0/1), false-pos cell :unobserved (zero admitted-absent comparisons).
   {:token-class :V :recorded true :admitted :present}])

(deftest raw-counts-and-coverage
  (let [r (rates/rates-by-class labels {:J 2 :K 8 :U 1 :V 4})]
    (is (= {:numerator 1 :denominator 2 :rate 1/2} (get-in r [:J :false-pos])))
    (is (= {:numerator 1 :denominator 2 :rate 1/2} (get-in r [:J :false-neg])))
    (is (= 5/2 (get-in r [:J :coverage])))          ; 5 labels / 2 subjects
    ;; zero errors is raw zero, not a smoothed posterior
    (is (= {:numerator 0 :denominator 3 :rate 0} (get-in r [:K :false-pos])))
    (is (= {:numerator 0 :denominator 1 :rate 0} (get-in r [:K :false-neg])))
    ;; per-cell unobserved: V has no admitted-absent comparison at all
    (is (= :unobserved (get-in r [:V :false-pos :status])))
    (is (= {:numerator 0 :denominator 1 :rate 0} (get-in r [:V :false-neg])))))

(deftest unobserved-class-and-unknown-subjects
  (let [r (rates/rates-by-class labels {:J 2 :K 8 :U 1 :V 4})]
    (is (= :unobserved (get-in r [:U :status])))
    (is (nil? (get-in r [:U :false-pos]))))
  (is (= {:status :missing :kind :unknown-subject-count :class :J}
         (get (rates/rates-by-class labels {:K 8 :U 1 :V 4}) :J))))

(deftest priors-explicit-only
  ;; an unauthorised prior is refused
  (is (= :invalid-prior (:kind (rates/rates-by-class labels {:J 2 :K 8 :U 1 :V 4}
                                                      {:alpha 1 :beta 1}))))
  (is (= :invalid-prior (:kind (rates/rates-by-class labels {:J 2 :K 8 :U 1 :V 4}
                                                      {:alpha 1 :beta 1 :authority "  "}))))
  ;; an authorised prior is recorded next to the raw counts, adds posterior
  ;; means, and changes no raw count
  (let [r (rates/rates-by-class labels {:J 2 :K 8 :U 1 :V 4}
                                {:alpha 1 :beta 1
                                 :authority "pending: Beta(1,1) not approved"})]
    (is (= {:numerator 1 :denominator 2 :rate 1/2 :posterior-mean 2/4}
           (get-in r [:J :false-pos])))
    (is (= {:prior {:alpha 1 :beta 1 :authority "pending: Beta(1,1) not approved"}}
           (select-keys (get r :J) [:prior])))
    (is (= {:numerator 0 :denominator 3 :rate 0 :posterior-mean 1/5}
           (get-in r [:K :false-pos])))
    ;; a prior never turns an unobserved cell into a rate by itself
    (is (= :unobserved (get-in r [:V :false-pos :status])))))

(deftest assembly-checkable-zero-and-refusals
  (let [r (rates/rates-by-class labels {:J 2 :K 8 :U 1 :V 4})
        m (rates/token-likelihood-rates r contract {:t1 :C5 :t2 :J :t3 :J})]
    ;; H-A-CONSUMER-I: the zero kernel is the unmeasured default, recorded
    ;; as such; an estimated kernel carries the counts it came from.
    (is (= {:false-neg 0 :false-pos 0 :basis :checkable :measurement :absent} (:t1 m)))
    (is (= {:false-neg 1/2 :false-pos 1/2 :basis :estimated}
           (select-keys (:t2 m) [:false-neg :false-pos :basis])))
    (is (= {:false-neg {:numerator 1 :denominator 2}
            :false-pos {:numerator 1 :denominator 2}}
           (:counts (:t2 m))))
    ;; unobserved judgement class: typed refusal, never a default
    (is (= {:status :missing :kind :unsupported-class :class :U :token :u}
           (rates/token-likelihood-rates r contract {:u :U})))
    ;; partially unobserved class (V) is refused too
    (is (= {:status :missing :kind :unsupported-class :class :V :token :v}
           (rates/token-likelihood-rates r contract {:v :V})))
    ;; class absent from the contract
    (is (= {:status :missing :kind :unknown-class :class :Z :token :z}
           (rates/token-likelihood-rates r contract {:z :Z})))))

(deftest accepted-by-real-token-likelihood
  (let [r (rates/rates-by-class labels {:J 2 :K 8 :U 1 :V 4})
        m (rates/token-likelihood-rates r contract {:t1 :C5 :t2 :J :t3 :J})
        dist (manifest/observation-distribution m #{:t2})]
    ;; column sums to exactly 1 (tokenLikelihood_colsum)
    (is (= 1 (reduce + (vals dist))))
    ;; checkable token keeps the identity factor in the kernel
    (is (= 1/4 (get dist #{:t2})))))                ; only t2 flips, prob 1/2 each way

;; ------------------------------------------------------------------------
;; H-A-CONSUMER-I (PROOF-2a hole H-A, consumer side): a measured rate for a
;; :checkable class reaches the kernel; the zero kernel is what an
;; UNMEASURED checkable class assumes. Reproduction inverted from
;; holes/labs/wm-contract/proof2/packets/H-A-CONSUMER-D.md §1.1.

(def prod-contract
  "The S-1 observation contract as shipped: every production class,
  :C4 included, is :kind :checkable."
  (edn/read-string (slurp (io/resource "wm/observation-contract.edn"))))

(def ^:private c4-tokens #{:t/observed :t/other :t/wanted})

(defn- c4-locators []
  (into {} (map (fn [t] [t {:class :C4 :repo "futon2" :sha "fixture"
                            :path (str "fixture/" (name t))}]))
        c4-tokens))

(def ^:private repro-labels
  "H-A-CONSUMER-D §1.1: one established token reported, one missed, one
  absent token not reported, one absent token reported."
  [{:token-class :C4 :admitted :present :recorded true}
   {:token-class :C4 :admitted :present :recorded false}
   {:token-class :C4 :admitted :absent :recorded false}
   {:token-class :C4 :admitted :absent :recorded true}])

(deftest checkable-class-precondition
  (is (= :checkable (:kind (first (filter #(= :C4 (:id %)) (:classes prod-contract)))))))

(deftest row1-measured-checkable-rates-reach-the-kernel
  (let [measured (rates/sourced-rates repro-labels {:C4 4} nil (c4-locators) prod-contract)
        unmeasured (rates/sourced-rates nil nil nil (c4-locators) prod-contract)]
    (is (= :sourced (:status measured)))
    (doseq [t c4-tokens]
      (is (= {:false-neg 1/2 :false-pos 1/2} (get-in measured [:rates t])))
      (is (= :estimated (get-in measured [:basis t])))
      (is (= {:false-neg {:numerator 1 :denominator 2}
              :false-pos {:numerator 1 :denominator 2}}
             (get-in measured [:measurement t]))))
    (is (not= measured unmeasured) "labels in, the same zeros out, was the defect")
    (doseq [t c4-tokens]
      (is (= {:false-neg 0 :false-pos 0} (get-in unmeasured [:rates t])))
      (is (= :checkable (get-in unmeasured [:basis t])))
      (is (= :absent (get-in unmeasured [:measurement t]))))))

(deftest row2-checkable-class-under-an-authorised-prior
  (let [prior {:alpha 1/2 :beta 1/2 :authority "test: Jeffreys Beta(1/2,1/2)"}
        r (rates/rates-by-class repro-labels {:C4 4} prior)
        m (rates/token-likelihood-rates r prod-contract {:t/wanted :C4})]
    ;; posterior mean (k + 1/2)/(n + 1) with k = 1, n = 2
    (is (= {:false-neg 1/2 :false-pos 1/2 :basis :prior}
           (select-keys (:t/wanted m) [:false-neg :false-pos :basis])))
    (is (= (/ (+ 1 1/2) (+ 2 1/2 1/2))
           (get-in r [:C4 :false-neg :posterior-mean])
           (get-in m [:t/wanted :false-neg])))
    (let [skew [{:token-class :C4 :admitted :present :recorded false}
                {:token-class :C4 :admitted :present :recorded true}
                {:token-class :C4 :admitted :present :recorded true}
                {:token-class :C4 :admitted :absent :recorded false}]
          m2 (rates/token-likelihood-rates (rates/rates-by-class skew {:C4 4} prior)
                                           prod-contract {:t/wanted :C4})]
      ;; fn: (1 + 1/2)/(3 + 1) = 3/8; fp: (0 + 1/2)/(1 + 1) = 1/4
      (is (= 3/8 (get-in m2 [:t/wanted :false-neg])))
      (is (= 1/4 (get-in m2 [:t/wanted :false-pos])))
      (is (= :prior (get-in m2 [:t/wanted :basis]))))))

(deftest row3-partial-measurement-refuses-never-zero
  (let [present-only [{:token-class :C4 :admitted :present :recorded true}
                      {:token-class :C4 :admitted :present :recorded false}]
        r (rates/rates-by-class present-only {:C4 2})]
    (is (= :unobserved (get-in r [:C4 :false-pos :status])))
    (is (= {:status :missing :kind :unsupported-class :class :C4 :token :t/wanted}
           (rates/token-likelihood-rates r prod-contract {:t/wanted :C4})))
    (is (= :unsupported-class
           (:kind (rates/sourced-rates present-only {:C4 2} nil (c4-locators) prod-contract))))))

(deftest row3b-admitted-nothing-is-unmeasured
  ;; labels but none admitted: the class entry is wholly :unobserved (with
  ;; its :coverage), which is no measurement, so the checkable default holds.
  (let [r (rates/rates-by-class [{:token-class :C4 :recorded true :admitted nil}] {:C4 1})]
    (is (= :unobserved (get-in r [:C4 :status])))
    (is (= {:false-neg 0 :false-pos 0 :basis :checkable :measurement :absent}
           (:t/wanted (rates/token-likelihood-rates r prod-contract {:t/wanted :C4}))))))

(deftest row4-judgement-class-unchanged
  (let [r (rates/rates-by-class labels {:J 2 :K 8 :U 1 :V 4})]
    (is (= {:false-neg 1/2 :false-pos 1/2 :basis :estimated}
           (select-keys (:t2 (rates/token-likelihood-rates r contract {:t2 :J}))
                        [:false-neg :false-pos :basis])))
    (is (= {:status :missing :kind :unsupported-class :class :U :token :u}
           (rates/token-likelihood-rates r contract {:u :U})))
    (is (= {:status :missing :kind :unsupported-class :class :J :token :j}
           (rates/token-likelihood-rates {} contract {:j :J})))))

(deftest invalid-prior-is-not-read-as-unmeasured
  (is (= :invalid-prior
         (:kind (rates/sourced-rates repro-labels {:C4 4} {:alpha 1 :beta 1}
                                     (c4-locators) prod-contract)))))

;; Row 5. G moves. The lowest function that takes the rate map is
;; futon2.aif.cascade-model-manifest/horizon-g-sparse: cascade-lane's R5
;; passes sourced-rates' :rates as :adjudication-rates to efe/rank-actions,
;; whose rank-cascade-actions hands them as :rates to horizon-g-sparse-cert,
;; which returns horizon-g-sparse's :g. The model map for the repro's
;; minimal problem is captured from one lane run by wrapping
;; horizon-g-sparse-cert (it still runs; only its argument is kept); the
;; function under test, horizon-g-sparse, is then called directly, once with
;; the zero kernel and once with rates SOURCED from labels that measure
;; fn 1/6 and fp 3/10 for :C4. Values at futon2 6a1540e6: zero kernel
;; 3.3991120972762268, measured 3.7324454306095607 (see the assertions for
;; how the zero value relates to H-A-CONSUMER-D's 2.012817736156336).

(defn- lane-problem []
  {:facts {:t/observed true :t/other false}
   :want [:t/wanted]
   :interpretations {:p/appears {:guard {:needs #{:t/observed} :forbids #{}}
                                 :produces #{:t/wanted}}}
   :repository {:patterns #{:p/appears} :stands-on #{}}
   :precedences [[:p/appears]]
   :horizon-steps 2
   :cascade-spec {:want #{:t/wanted}}
   :beta 1
   :locators (c4-locators)})

(defn- captured-models []
  (let [seen (atom [])
        cert manifest/horizon-g-sparse-cert
        lane (with-redefs [manifest/horizon-g-sparse-cert
                           (fn [m] (swap! seen conj m) (cert m))]
               (wm/cascade-lane (lane-problem)))]
    {:lane lane :models @seen}))

(def ^:private measuring-labels
  "fn = 1/6 (six admitted-present, one recorded false);
  fp = 3/10 (ten admitted-absent, three recorded true)."
  (vec (concat [{:token-class :C4 :admitted :present :recorded false}]
               (repeat 5 {:token-class :C4 :admitted :present :recorded true})
               (repeat 3 {:token-class :C4 :admitted :absent :recorded true})
               (repeat 7 {:token-class :C4 :admitted :absent :recorded false}))))

(defn- close? [a b] (< (Math/abs (- (double a) (double b))) 1e-9))

(deftest row5-a-measured-checkable-rate-moves-g
  (let [{:keys [lane models]} (captured-models)
        by-id (zipmap (map :cascade-id (:ranked lane)) (map :G-efe (:ranked lane)))
        c1-model (first (filter #(= [:p/appears] (mapv :id ((:precedence-fn %)))) models))
        sourced (rates/sourced-rates measuring-labels {:C4 16} nil (c4-locators) prod-contract)
        zero (rates/sourced-rates nil nil nil (c4-locators) prod-contract)
        g-zero (manifest/horizon-g-sparse (assoc c1-model :rates (:rates zero)))
        g-measured (manifest/horizon-g-sparse (assoc c1-model :rates (:rates sourced)))]
    (testing "the sourced rates are the measured ones"
      (is (= {:false-neg 1/6 :false-pos 3/10} (get-in sourced [:rates :t/wanted])))
      (is (= :estimated (get-in sourced [:basis :t/wanted]))))
    (testing "the captured model is the lane's own scoring of :C1 at production rates"
      (is (some? c1-model))
      (is (= (:rates zero) (:rates c1-model)))
      (is (close? g-zero (get by-id :C1))))
    (testing "against H-A-CONSUMER-D E3's committed values (futon2 0358d4c5)"
      ;; measured: E3's design-(a) value for :C1, reproduced
      (is (close? 3.7324454306095607 g-measured))
      ;; zero kernel: E3 read 2.012817736156336 before a98f5879 (H-VALUE-G-D)
      ;; made R5 score over the problem's whole token universe, which adds
      ;; :t/other; at zero rates that adds ln 2 per step, T = 2
      (is (close? 3.3991120972762268 g-zero))
      (is (close? (* 2 (Math/log 2)) (- g-zero 2.012817736156336))))
    (is (not (close? g-zero g-measured)))))

;; Row 6. REACH LIMIT, recorded not fixed: a cell carrying a :rate but no
;; :numerator/:denominator (a declared number wearing a measured shape, A-S
;; §5's falsifier) is still read by usable-rate as a measurement. The kernel
;; records empty :counts beside it, so the absence of counts is visible on the
;; record; refusing it is a separate packet.
(deftest row6-declared-number-in-measured-shape-reach-limit
  (let [declared {:C4 {:false-neg {:rate 1/6} :false-pos {:rate 3/10}}}
        m (rates/token-likelihood-rates declared prod-contract {:t/wanted :C4})]
    (is (= {:false-neg 1/6 :false-pos 3/10 :basis :estimated
            :counts {:false-neg {} :false-pos {}}}
           (:t/wanted m))
        "reach limit: accepted today, with empty :counts")))
