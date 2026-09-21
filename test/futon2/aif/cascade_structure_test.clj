(ns futon2.aif.cascade-structure-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [futon2.aif.cascade-structure :as structure]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.cascade-prior :as prior]
            [futon2.aif.interpretation-construction :as constructor]
            [futon2.aif.interpretation-construction-test :as pq]
            [futon2.aif.fold-cascade :as fold]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.cascade-policy :as policy]))

(def singleton
  {:kind :cascade-candidate :id :C1 :target "M-aif-policy-conditioned-eig"
   :construction-receipt {:kind :hand-admitted}
   :precedence [(policy/token-interpretation
                 :apparatus/one-authority-per-question
                 {:guard {:needs #{["M-aif-policy-conditioned-eig" :admission/task-stated]}
                          :forbids #{["M-aif-policy-conditioned-eig" :hole/h6378c65a4012]}}
                  :produces #{["M-aif-policy-conditioned-eig" :hole/h6378c65a4012]}})]})

(deftest selected-constructor-replaces-literal-with-receipt
  (let [c (runner/construct-selected-action {:action singleton})]
    (is (not (contains? c :semilattice)) "cascade candidates never return the literal semilattice slot")
    (is (= :singleton (get-in c [:cascade-structure :shape])))
    (is (= :declared-need-support-v1 (get-in c [:cascade-structure :basis])))))

(defn action [rows]
  {:kind :cascade-candidate :target "M-fixture" :construction-receipt {:kind :fixture}
   :precedence (mapv (fn [[id needs produces]]
                       (policy/token-interpretation id {:guard {:needs needs :forbids #{}}
                                                        :produces produces})) rows)})

(deftest seven-family-controls
  (doseq [[family shape overlap closed components]
          [[#{#{:a}} :singleton false true 1]
           [#{#{:a} #{:a :b} #{:a :b :c}} :chain false true 1]
           [#{#{:a} #{:b}} :antichain false false 2]
           [#{#{:a} #{:b} #{:a :b}} :tree false false 1]
           [#{#{:a} #{:a :b} #{:a :c} #{:a :b :c :d}} :semilattice true true 1]
           [#{#{:a :b} #{:b :c}} :unclassified true false 1]
           [#{} :empty-family false false 0]]]
    (let [r (structure/classify-family family)]
      (is (= [shape overlap closed components (= 1 components)]
             ((juxt :shape :overlap? :intersection-closed? :component-count :connected?) r)))
      (is (= family (:family r)))))
  (is (= #{#{:b}} (:missing-intersections (structure/classify-family #{#{:a :b} #{:b :c}})))))

(deftest dependencies-derive-supports-not-precedence-arrows
  (let [built (first (:candidates (constructor/construct pq/input)))
        a (action (mapv (fn [id] [id (get-in pq/interpretations [id :guard :needs])
                                  (get-in pq/interpretations [id :produces])]) (:precedence built)))
        r (structure/receipt a)]
    (is (= :chain (:shape r)))
    (is (= {:P #{:P} :Q #{:P :Q}} (:supports r)))
    (is (= #{{:from :P :to :Q :tokens #{:q}}} (:need-edges r)))
    (is (= #{[:P :Q]} (:need-edges built))))
  (let [eoi (action [[:O #{:refused} #{:resolved}] [:B #{} #{:bound}] [:S #{} #{:refused}]])
        alternate (update eoi :precedence #(vec (map % [0 2 1])))
        a (structure/receipt eoi) b (structure/receipt alternate)]
    (is (= :tree (:shape a)))
    (is (= 2 (:component-count a)))
    (is (false? (:connected? a)))
    (is (= #{#{:S} #{:O :S} #{:B}} (:family a)))
    (is (= (:supports a) (:supports b)))
    (is (= #{{:from :S :to :O :tokens #{:refused}}} (:need-edges a))))
  (let [r (structure/receipt (action [[:A #{} #{:a}] [:B #{:a} #{:b}]
                                     [:C #{:a} #{:c}] [:D #{:b :c} #{:d}]]))]
    (is (= :semilattice (:shape r)))
    (is (true? (:overlap? r)))
    (is (true? (:intersection-closed? r)))))

(deftest typed-invalid-inputs-do-not-manufacture-structure
  (doseq [[a reason]
          [[(dissoc singleton :precedence) :missing-or-invalid-precedence]
           [(update singleton :precedence #(conj % (first %))) :duplicate-pattern-ids]
           [(assoc-in singleton [:precedence 0 :guard :operator] :or) :unsupported-pattern-interpretation]
           [(assoc singleton :precedence [:uninterpreted]) :unsupported-pattern-interpretation]
           [(action [[:P #{:w} #{:q}] [:Q #{:q} #{:w}]]) :cyclic-needs]
           [(action [[:P #{:q} #{:q}]]) :cyclic-needs]]]
    (let [r (structure/receipt a)]
      (is (= :unavailable (:status r)))
      (is (= :unclassified (:shape r)))
      (is (= reason (get-in r [:findings 0 :kind])))))
  (is (= :empty-family (:shape (structure/receipt (assoc singleton :precedence []))))))

(deftest receipt-is-record-only-and-roundtrips
  (let [old {:mission (:target singleton) :psi "enact cascade :C1" :construction-kind :selected-cascade
             :selected-action singleton :precedence (:precedence singleton)
             :construction-receipt (:construction-receipt singleton) :interpretation-receipts nil
             :shown [":apparatus/one-authority-per-question"] :semilattice [] :policy-holes []}
        c (runner/construct-selected-action {:action singleton}) r (:cascade-structure c)
        identity-bytes #(pr-str (prior/policy-key (habit/policy-view %)))]
    (is (= (dissoc old :semilattice) (dissoc c :cascade-structure)))
    (is (= singleton (:selected-action c)))
    (is (= (identity-bytes singleton) (identity-bytes (assoc singleton :cascade-structure r))
           (identity-bytes (:selected-action c))))
    (is (= r (edn/read-string (pr-str r))))
    (is (= {:status :unavailable :reason :authority-graph-not-retained} (:authority-evidence r)))
    (is (re-matches #"[0-9a-f]{64}" (:input-sha256 r)))
    (is (= (:input-sha256 r) (:input-sha256 (structure/receipt singleton))))
    ;; Empty locators: the real fold produces an unchanged explicit hole, no IO sources.
    (is (= (fold/realize old) (fold/realize c)))))

(deftest recorded-reference-singleton
  (let [record (edn/read-string (slurp "test/fixtures/run-narrative-1789964661.edn"))
        a (assoc singleton :target (get-in record [:cascade :target])
                 :precedence (get-in record [:cascade :patterns]))
        r (:cascade-structure (runner/construct-selected-action {:action a}))]
    (is (= :computed (:status r)))
    (is (= :singleton (:shape r)))
    (is (= #{:apparatus/one-authority-per-question} (:carrier r)))
    (is (= #{} (:need-edges r)))
    (is (= #{#{:apparatus/one-authority-per-question}} (:family r)))))
