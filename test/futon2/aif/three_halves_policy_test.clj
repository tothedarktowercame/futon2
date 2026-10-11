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
(defn pin [name]
  (let [path (str "controlled/" name ".edn") revision "sut-r1" body (str "source:" name)]
    (swap! captured assoc [path revision] body)
    {:path path :revision revision :sha256 (sha body)}))
(defn sq [id] {:square-id id :source (pin (name id)) :receipt square})
(defn gl [id from to]
  {:gluing-id id :from {:square-id from :role :G} :to {:square-id to :role :G}
   :iso #{[0 0]} :inverse #{[0 0]} :source (pin (name id))
   :axiom-carriage {:forward #{} :inverse #{}}})
(defn triangle []
  (reset! captured {})
  {:schema sut/schema :source (pin "policy")
   :squares [(sq :s0) (sq :s1) (sq :s2)]
   :gluings [(gl :g01 :s0 :s1) (gl :g12 :s1 :s2) (gl :g02 :s0 :s2)]
   :cycle-obligations [{:edge :g02
                        :forest-path [{:gluing-id :g01 :reversed? false}
                                      {:gluing-id :g12 :reversed? false}]}]})
(defn validate [p] (sut/validate p {:captured-sources @captured}))

(deftest coherent-multi-square-policy-derives-the-lean-cycle-basis
  (let [r (validate (triangle))]
    (is (= :valid (:status r)))
    (is (= [:g01 :g12] (get-in r [:derived :typed-forest])))
    (is (= [:g02] (get-in r [:derived :required-cycle-edges]))))
  (testing "two independent cycles require two obligations"
    (let [p (triangle)
          p (-> p
                (update :squares conj (sq :s3))
                (update :gluings into [(gl :g23 :s2 :s3) (gl :g13 :s1 :s3)]))
          canonical (conj (:cycle-obligations p)
                          {:edge :g13 :forest-path [{:gluing-id :g12 :reversed? false}
                                                    {:gluing-id :g23 :reversed? false}]})]
      (is (= :valid (:status (validate (assoc p :cycle-obligations canonical)))))
      (is (= :cycle-obligation-population
             (first (:missing-evidence (validate p))))))))

(deftest malformed-or-incoherent-policies-refuse-before-g
  (doseq [[label damage expected]
          [[:omitted #(assoc % :cycle-obligations []) :cycle-obligation-population]
           [:duplicate #(update % :cycle-obligations into (:cycle-obligations %))
            :cycle-obligation-population]
           [:incoherent #(assoc-in % [:cycle-obligations 0 :forest-path 0 :reversed?] true)
            :cycle-transport-or-canonical-path]
           [:partial #(assoc-in % [:gluings 0 :iso] #{}) :gluing-invalid]
           [:self-loop #(assoc-in % [:gluings 0 :to :square-id] :s0) :gluing-invalid]
           [:unknown-role #(assoc-in % [:gluings 0 :from :role] :X) :gluing-invalid]
           [:role-mismatch #(assoc-in % [:cycle-obligations 0 :forest-path 1 :reversed?] true)
            :cycle-transport-or-canonical-path]
           [:bad-pin #(assoc-in % [:source :sha256] (apply str (repeat 64 "0"))) :policy-source-pin]]]
    (testing (name label)
      (is (some #{expected} (:missing-evidence (validate (damage (triangle)))))))))

(def interpretations
  {:P {:guard {:needs #{} :forbids #{}} :produces #{:q}}
   :Q {:guard {:needs #{:q} :forbids #{}} :produces #{:w}}
   :R {:guard {:needs #{} :forbids #{}} :produces #{:r}}})
(def receipts (zipmap (keys interpretations)
                      (repeat {:kind :fixture :blend-square square})))
(defn score [{:keys [patterns precedence]}]
  (let [by-id (into {} (map (juxt :id identity)) patterns)]
    (ah/active-horizon-g
     {:q0 {#{} 1}
      :precedence-fn (constantly
                      (mapv #(policy/token-interpretation % (get by-id %)) precedence))
      :horizon 2 :universe #{:q :w :r} :masked #{} :cap 0
      :rates (zipmap [:q :w :r] (repeat {:false-pos 0 :false-neg 0}))
      :c-fn-pointwise (fn [_ o] (/ (if (contains? o :w) 3 1) 16))})))
(def input {:target "controlled-3halves" :want [:w] :observation {:q false :w false :r false}
            :interpretations interpretations :interpretation-receipts receipts
            :budget {:max-moves 1 :max-expansions 20} :horizon 2
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
                        :horizon-steps 2 :beta-by-context {:WM 1}
                        :context-of (constantly :WM)}})))))

(deftest controlled-entrypoint-is-a-real-pre-g-gate
  (let [policy (triangle)
        result (decision/controlled-three-halves-admission
                {:mode :controlled-sut :policy policy :captured-sources @captured}
                (real-problem))]
    (is (= :ready-for-g (:status result)))
    (is (= "controlled-3halves" (:constructed-identity result)
           (:admitted-identity result)))
    (is (seq (get-in result [:admission :problem :constructed-candidates]))))
  (is (= :controlled-authority-absent
         (:reason (decision/controlled-three-halves-admission {} (real-problem)))))
  (is (= :three-halves-policy-absent
         (:reason (decision/controlled-three-halves-admission
                   {:mode :controlled-sut} (real-problem))))))
