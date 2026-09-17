(ns futon2.aif.parameter-delivery-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.machine-model :as machine-model]
            [futon2.aif.machine-parameters :as parameters]
            [futon2.aif.parameter-delivery :as delivery]))

;; --- Lean fixture: ExpectedInformationGainWitness.binaryFixture ---
;; one policy "inspect", one outcome :datum of predictive mass 1, uniform
;; prior over [:a :b], point posterior on :a (with :b at mass 0 in the
;; support, exercising the 0·log 0 convention) ⇒ EIG = log 2.
(def fixture-kernels
  {:model {:id "eig-witness" :revision "v1"}
   :theta [:a :b]
   :prior-kernel {"inspect" {:a 1/2 :b 1/2}}
   :posterior-kernel {["inspect" :datum] {:ok true :evidence 1.0
                                          :mass {:a 1.0 :b 0.0}}}
   :posterior-predictive {"inspect" {:datum 1.0}}})

(deftest lean-binary-fixture
  (is (< (Math/abs (- (delivery/expected-information-gain fixture-kernels "inspect")
                      (Math/log 2)))
         1e-12)
      "ExpectedInformationGainWitness.binaryFixture: EIG = log 2"))

(deftest zero-mass-theta-contributes-zero
  ;; posterior support containing a zero-mass θ (0·log 0 := 0) gives the same
  ;; value as a posterior without it.
  (let [with-zero fixture-kernels
        without-zero (assoc-in fixture-kernels [:posterior-kernel ["inspect" :datum] :mass]
                               {:a 1.0})]
    (is (= (delivery/parameter-information-gain with-zero "inspect" :datum)
           (delivery/parameter-information-gain without-zero "inspect" :datum)))))

(deftest refusals
  ;; :zero-evidence-conditioning — posterior entry not :ok
  (let [k (assoc-in fixture-kernels [:posterior-kernel ["inspect" :datum]]
                    {:ok false :refusal {:kind :zero-evidence-conditioning}})]
    (is (= :zero-evidence-conditioning
           (:kind (delivery/parameter-information-gain k "inspect" :datum))))
    ;; a positive predictive mass whose posterior is refused refuses the EIG
    (is (= :zero-evidence-conditioning
           (:kind (delivery/expected-information-gain k "inspect")))))
  ;; :zero-prior-in-posterior-support — Lean's positivePrior hypothesis
  (let [k (-> fixture-kernels
              (assoc-in [:prior-kernel "inspect"] {:a 0.0 :b 1.0})
              (assoc-in [:posterior-kernel ["inspect" :datum] :mass] {:a 1.0}))]
    (is (= :zero-prior-in-posterior-support
           (:kind (delivery/parameter-information-gain k "inspect" :datum)))))
  ;; a zero predictive mass outcome contributes 0 and never consults the
  ;; posterior — even a refused one.
  (let [k {:prior-kernel {"p" {:a 1/2 :b 1/2}}
           :posterior-kernel {["p" :silent] {:ok false
                                             :refusal {:kind :zero-evidence-conditioning}}
                              ["p" :heard] {:ok true :evidence 1.0 :mass {:a 1.0 :b 0.0}}}
           :posterior-predictive {"p" {:silent 0.0 :heard 1.0}}}]
    (is (= (Math/log 2) (delivery/expected-information-gain k "p")))))

;; --- real row-11 kernels, built exactly as machine-parameters-test does ---
(def dir "holes/labs/wm-contract/runs/row-11-parameters")
(def states [:addressed :falsified :foreclosed :refined :reopened :spawned :strengthened])
(def outcomes (:support (machine-model/outcome-authority)))
(defn read-h [file sha]
  (let [r (edn/read-string (slurp (str dir "/" file)))]
    {:id (:id r) :revision (:revision r) :likelihood (:likelihood r)
     :registration {:path (str dir "/" file) :sha256 sha}}))
(def h1 (read-h "identity-transition.edn"
                "f422378a56316306da66a80c2c7a2c9456c38885871534a139a4d28db8d50b18"))
(def h2 (read-h "controlled-transition.edn"
                "7b17469efb3b7b5f4703ddefe7d90c1e53c406ebf6cdc63a078b39cb9bb26f48"))
(def model {:model {:id "wm-parameters" :revision "v1"}
            :state-support states :outcome (machine-model/outcome-authority)})
(def policies [{:id "advance-twice"} {:id "advance-then-cascade"}])
(defn state [s] (assoc (zipmap states (repeat 0)) s 1))
(def parameter-state {:model (:model model) :hypotheses [h1 h2]
                      :prior {"identity-transition" 1/2 "controlled-transition" 1/2}
                      :authority :declared-prior :state-distribution (state :spawned)})

(def real-kernels (parameters/parameter-kernels model parameter-state policies outcomes))

(deftest real-row-11-kernels
  (is (:ok real-kernels))
  (doseq [{pid :id} policies]
    (let [eig (delivery/expected-information-gain real-kernels pid)]
      (is (number? eig))
      (is (>= eig 0)))))

(deftest zero-eig-when-posterior-equals-prior
  ;; a hypothesis set whose likelihood rows agree (the outcome is
  ;; uninformative about θ): every posterior equals the prior, so every KL is
  ;; 0 and the EIG is exactly 0.
  (let [prior {:same-a 1/2 :same-b 1/2}
        agreeing {:model {:id "agreeing" :revision "v1"}
                  :theta [:same-a :same-b]
                  :prior-kernel {"p" prior}
                  :posterior-kernel
                  {["p" :o1] {:ok true :evidence 1/2 :mass prior}
                   ["p" :o2] {:ok true :evidence 1/2 :mass prior}}
                  :posterior-predictive {"p" {:o1 1/2 :o2 1/2}}}]
    (is (zero? (delivery/expected-information-gain agreeing "p")))))

(deftest delivery-requires-a-identity-and-observations
  (is (= :a-identity-required
         (:kind (delivery/delivery fixture-kernels {:observations {:o :datum}}))))
  (is (= :observations-required
         (:kind (delivery/delivery fixture-kernels {:a-identity "A-7"}))))
  (let [d (delivery/delivery fixture-kernels {:a-identity "A-7"
                                              :observations {:o :datum}})]
    (is (= :wm/parameter-delivery-v1 (:schema d)))
    (is (= "A-7" (:a-identity d)))
    (is (= {:o :datum} (:observations d)))
    (is (= (:posterior-kernel fixture-kernels) (:posterior-kernel d)))
    (is (= (:prior-kernel fixture-kernels) (:prior-kernel d)))
    (is (number? (get-in d [:expected-information-gain "inspect"])))))

(deftest staleness
  (let [d (delivery/delivery fixture-kernels {:a-identity "A-7"
                                              :observations {:o :datum}})
        current {:model (:model fixture-kernels)}]
    (is (nil? (delivery/stale? d {:a-identity "A-7" :model current})))
    (is (= :a-changed (get-in (delivery/stale? d {:a-identity "A-8" :model current}) [:because])))
    (is (= :model-revision-changed
           (get-in (delivery/stale? d {:a-identity "A-7"
                                       :model {:model {:id "eig-witness" :revision "v2"}}})
                   [:because])))))

(deftest refresh-recomputes-only-when-stale
  (let [d (delivery/delivery fixture-kernels {:a-identity "A-7"
                                              :observations {:o :datum}})
        current {:model (:model fixture-kernels)}]
    ;; not stale: unchanged, flagged :not-required
    (is (= :not-required
           (:refreshed (delivery/refresh d {:model current :parameter-state parameter-state
                                            :policies policies :outcome-support outcomes
                                            :a-identity "A-7" :observations {:o :datum}}))))
    ;; stale on a-identity: recompute through parameter-kernels (which
    ;; re-hashes every registration pin) and deliver under the NEW a-identity.
    (let [r (delivery/refresh d {:model model :parameter-state parameter-state
                                 :policies policies :outcome-support outcomes
                                 :a-identity "A-8" :observations {:o :datum}})]
      (is (= :wm/parameter-delivery-v1 (:schema r)))
      (is (= "A-8" (:a-identity r)))
      (is (every? number? (vals (:expected-information-gain r)))))
    ;; a registration refusal from parameter-kernels passes through untouched
    (let [r (delivery/refresh d {:model model
                                 :parameter-state (assoc-in parameter-state
                                                            [:hypotheses 0 :registration :sha256]
                                                            (apply str (repeat 64 "0")))
                                 :policies policies :outcome-support outcomes
                                 :a-identity "A-8" :observations {:o :datum}})]
      (is (= :registration-pin-mismatch (get-in r [:refusal :kind]))))))

(deftest g-with-information-gain-leaves-g-alone
  (let [d (delivery/delivery fixture-kernels {:a-identity "A-7"
                                              :observations {:o :datum}})
        g-fn (fn [input] {:risk 0.25 :input input})
        result (delivery/g-with-information-gain
                {:g-fn g-fn :g-input {:policy "inspect"} :delivery d
                 :policy-id "inspect"})]
    ;; the information gain must NOT be added to G
    (is (= (g-fn {:policy "inspect"}) (:g result)))
    (is (= (get-in d [:expected-information-gain "inspect"])
           (:expected-information-gain result)))
    (is (= "DarkTower.WarMachine.Holes.expectedInformationGain"
           (get-in result [:basis :lean])))
    (is (= :wm/parameter-delivery-v1 (get-in result [:basis :delivery-schema])))))
