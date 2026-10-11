(ns futon2.aif.three-halves-policy-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.active-horizon-g :as ah]
            [futon2.aif.cascade-policy :as policy]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.interpretation-construction :as construction]
            [futon2.aif.three-halves-policy :as sut]
            [futon2.aif.wm.cascade-decision :as decision])
  (:import (java.math BigInteger)
           (java.security MessageDigest)))

(defn sha [s]
  (format "%064x" (BigInteger. 1 (.digest (doto (MessageDigest/getInstance "SHA-256")
                                            (.update (.getBytes s "UTF-8")))))))
(def square (edn/read-string (slurp "test/fixtures/three-halves-square/publication-cadence.edn")))
(def captured (atom {}))
(defn with-pin [name declaration]
  (let [path (str "controlled/" name ".edn") revision "sut-r1"
        body (sut/sealed-source declaration)
        pin {:path path :revision revision :sha256 (sha body)}]
    (swap! captured assoc [path revision] body)
    (assoc declaration :source pin)))
(defn sq [id candidate-id pattern-id position]
  (with-pin (name id) {:square-id id
                       :occurrence {:candidate-id candidate-id :pattern-id pattern-id
                                    :position position}
                       :receipt square}))
(defn gl [id from to]
  (with-pin (name id)
    {:gluing-id id :from {:square-id from :role :G} :to {:square-id to :role :G}
     :iso #{[0 0]} :inverse #{[0 0]}
     :axiom-carriage {:forward #{} :inverse #{}}}))
(defn triangle
  ([] (triangle {:candidate-id :controlled-candidate :precedence [:P :Q :R]}))
  ([{:keys [candidate-id precedence]}]
  (reset! captured {})
  (with-pin
    "policy"
    {:schema sut/schema
     :projection {:target "controlled-3halves"
                  :candidates [{:candidate-id candidate-id :precedence precedence
                                :squares [:s0 :s1 :s2]}]}
     :squares [(sq :s0 candidate-id :P 0) (sq :s1 candidate-id :Q 1)
               (sq :s2 candidate-id :R 2)]
     :gluings [(gl :g01 :s0 :s1) (gl :g12 :s1 :s2) (gl :g02 :s0 :s2)]
     :cycle-obligations [{:edge :g02
                          :forest-path [{:gluing-id :g01 :reversed? false}
                                        {:gluing-id :g12 :reversed? false}]}]})))
(defn validate [p] (sut/validate p {:captured-sources @captured}))
(defn reseal-policy [p] (with-pin "policy-resealed" (dissoc p :source)))

(deftest coherent-multi-square-policy-derives-the-lean-cycle-basis
  (let [r (validate (triangle))]
    (is (= :valid (:status r)))
    (is (= [:g01 :g12] (get-in r [:derived :typed-forest])))
    (is (= [:g02] (get-in r [:derived :required-cycle-edges]))))
  (testing "two independent cycles require two obligations"
    (let [p (triangle)
          cid (get-in p [:projection :candidates 0 :candidate-id])
          p (-> p
                (update :squares conj (sq :s3 cid :S 3))
                (assoc-in [:projection :candidates 0 :precedence] [:P :Q :R :S])
                (update-in [:projection :candidates 0 :squares] conj :s3)
                (update :gluings into [(gl :g23 :s2 :s3) (gl :g13 :s1 :s3)]))
          canonical (conj (:cycle-obligations p)
                          {:edge :g13 :forest-path [{:gluing-id :g12 :reversed? false}
                                                    {:gluing-id :g23 :reversed? false}]})]
      (is (= :valid (:status (validate (with-pin "policy-4"
                                         (assoc (dissoc p :source)
                                                :cycle-obligations canonical))))))
      (is (= :cycle-obligation-population
             (first (:missing-evidence (validate (reseal-policy p)))))))))

(deftest malformed-or-incoherent-policies-refuse-before-g
  (doseq [[label damage reseal? expected]
          [[:omitted #(assoc % :cycle-obligations []) true :cycle-obligation-population]
           [:duplicate #(update % :cycle-obligations into (:cycle-obligations %)) true
            :cycle-obligation-population]
           [:incoherent #(assoc-in % [:cycle-obligations 0 :forest-path 0 :reversed?] true) true
            :cycle-transport-or-canonical-path]
           [:partial #(assoc-in % [:gluings 0 :iso] #{}) true :gluing-invalid]
           [:self-loop #(assoc-in % [:gluings 0 :to :square-id] :s0) true :gluing-invalid]
           [:unknown-role #(assoc-in % [:gluings 0 :from :role] :X) true :gluing-invalid]
           [:role-mismatch #(assoc-in % [:cycle-obligations 0 :forest-path 1 :reversed?] true) true
            :cycle-transport-or-canonical-path]
           [:bad-pin #(assoc-in % [:source :sha256] (apply str (repeat 64 "0"))) false
            :policy-source-pin]]]
    (testing (name label)
      (let [changed (damage (triangle))
            changed (if reseal? (reseal-policy changed) changed)]
        (is (some #{expected} (:missing-evidence (validate changed))))))))

(def interpretations
  {:P {:guard {:needs #{} :forbids #{}} :produces #{["controlled-3halves" :q]}}
   :Q {:guard {:needs #{["controlled-3halves" :q]} :forbids #{}}
       :produces #{["controlled-3halves" :r]}}
   :R {:guard {:needs #{["controlled-3halves" :r]} :forbids #{}}
       :produces #{["controlled-3halves" :w]}}})
(def receipts (zipmap (keys interpretations)
                      (repeat {:kind :fixture :blend-square square})))
(defn score [{:keys [patterns precedence]}]
  (let [by-id (into {} (map (juxt :id identity)) patterns)]
    (ah/active-horizon-g
     {:q0 {#{} 1}
      :precedence-fn (constantly
                      (mapv #(policy/token-interpretation % (get by-id %)) precedence))
      :horizon 3 :universe #{["controlled-3halves" :q]
                             ["controlled-3halves" :r]
                             ["controlled-3halves" :w]} :masked #{} :cap 0
      :rates (zipmap [["controlled-3halves" :q] ["controlled-3halves" :r]
                      ["controlled-3halves" :w]]
                     (repeat {:false-pos 0 :false-neg 0}))
      :c-fn-pointwise (fn [_ o] (/ (if (contains? o ["controlled-3halves" :w]) 3 1) 16))})))
(def input {:target "controlled-3halves" :want [["controlled-3halves" :w]]
            :observation {["controlled-3halves" :q] false
                          ["controlled-3halves" :r] false
                          ["controlled-3halves" :w] false}
            :interpretations interpretations :interpretation-receipts receipts
            :budget {:max-moves 2 :max-expansions 40} :horizon 3
            :move-cost 0 :evaluate-g score})

(defn real-problem []
  (let [candidates (:candidates (construction/construct input))]
    (first (:problems
            (problems/assemble
             {:targets [(:target input)]
              :sources {:universes {(:target input) (:observation input)}
                        :wants {(:target input) (:want input)}
                        :interpretations {(:target input) {:patterns interpretations :receipts receipts}}
                        :locators {(:target input) (zipmap (keys (:observation input))
                                                          (repeat {:class :C4}))}
                        :candidates {(:target input) candidates}
                        :horizon-steps 3 :beta-by-context {:WM 1}
                        :context-of (constantly :WM)}})))))

(deftest controlled-entrypoint-is-a-real-pre-g-gate
  (let [problem (real-problem)
        candidate (first (:constructed-candidates problem))
        policy (triangle candidate)
        result (decision/controlled-three-halves-admission
                {:mode :controlled-sut :policy policy :captured-sources @captured}
                problem)]
    (is (= :ready-for-g (:status result)))
    (is (= (:constructed-identity result) (:admitted-identity result)
           {:target "controlled-3halves"
            :candidates [{:candidate-id :C1 :precedence [:P :Q :R]}]}))
    (is (seq (get-in result [:admission :problem :constructed-candidates]))))
  (is (= :controlled-authority-absent
         (:reason (decision/controlled-three-halves-admission {} (real-problem)))))
  (is (= :three-halves-policy-absent
         (:reason (decision/controlled-three-halves-admission
                   {:mode :controlled-sut} (real-problem))))))

(deftest source-and-candidate-population-joins-are-not-self-exempting
  (let [problem (real-problem)
        candidate (first (:constructed-candidates problem))
        policy (triangle candidate)
        run (fn [p]
              (decision/controlled-three-halves-admission
               {:mode :controlled-sut :policy p :captured-sources @captured} problem))]
    (testing "payload substitution under unchanged authoritative bytes"
      (doseq [changed [(assoc-in policy [:squares 0 :receipt :auxiliary :b1] true)
                       (assoc-in policy [:gluings 0 :iso] #{})
                       (assoc-in policy [:gluings 0 :to :square-id] :s2)
                       (assoc-in policy [:cycle-obligations 0 :forest-path 0 :reversed?] true)]]
        (is (= :three-halves-policy-invalid (:reason (run changed))))))
    (testing "an honestly resealed but unrelated projection cannot authorize the problem"
      (let [unrelated (-> policy
                          (assoc-in [:projection :candidates 0 :candidate-id] :foreign)
                          (update :squares
                                  (fn [squares]
                                    (mapv (fn [s]
                                            (with-pin
                                              (str "foreign-" (name (:square-id s)))
                                              (assoc-in (dissoc s :source)
                                                        [:occurrence :candidate-id] :foreign)))
                                          squares)))
                          reseal-policy)]
        (is (= :three-halves-policy-projection-mismatch (:reason (run unrelated))))))
    (testing "swapped occurrence, omitted candidate, and reordered population refuse"
      (doseq [projection [(assoc-in (:projection policy)
                                    [:candidates 0 :precedence] [:Q :P :R])
                          (assoc (:projection policy) :candidates [])
                          (update-in (:projection policy) [:candidates 0 :precedence]
                                     #(vec (reverse %)))]]
        (let [changed (reseal-policy (assoc policy :projection projection))]
          (is (not= :ready-for-g (:status (run changed)))))))
    (testing "swapping square-to-pattern occurrence bindings is invalid"
      (let [a (get-in policy [:squares 0 :occurrence])
            b (get-in policy [:squares 1 :occurrence])
            squares (-> (:squares policy)
                        (assoc-in [0 :occurrence] b)
                        (assoc-in [1 :occurrence] a)
                        (update 0 #(with-pin "swapped-s0" (dissoc % :source)))
                        (update 1 #(with-pin "swapped-s1" (dissoc % :source))))
            changed (reseal-policy (assoc policy :squares squares))]
        (is (= :three-halves-policy-invalid (:reason (run changed))))))))
