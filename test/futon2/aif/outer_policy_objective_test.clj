(ns futon2.aif.outer-policy-objective-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.outer-policy-objective :as sut]
            [futon2.aif.outer-policy-evidence :as policy-evidence]
            [futon2.aif.parameter-novelty :as novelty]
            [futon2.aif.wm.family-selection :as family-selection]
            [futon2.aif.wm.selection-cascade-identity :as identity]))

(use-fixtures :each
  (fn [run]
    (with-redefs [identity/verify-selected-identity
                  (fn [decision _]
                    (if (get-in decision [:action :cascade-digest])
                      {:status :verified :digest (get-in decision [:action :cascade-digest])}
                      {:status :cascade-identity-mismatch}))]
      (run))))
(defn pin [path c] {:path path :sha256 (apply str (repeat 64 c))})

(defn row [id alpha ln-e]
  (let [n (double (reduce + alpha)) q (mapv #(/ (double %) n) alpha)]
    {:policy-id id :target-id "same-target" :cascade-digest (str "digest-" id)
     :posterior-alpha alpha
     :posterior-predictive {:vocabulary [:success :failure]
                            :distribution q :units :probability}
     :eig (novelty/categorical-information [:success :failure] alpha)
     :ln-e ln-e
     :ln-e-components {:resource {:value ln-e :units :nats :source-pin (pin "resource" "1")}
                       :cadence {:value 0.0 :units :nats :source-pin (pin "cadence" "2")}
                       :continuation {:value 0.0 :units :nats :source-pin (pin "continuation" "3")}}}))

(defn sealed [rows]
  (let [body {:schema :wm/sealed-outer-policy-menu-v1
              :field-source-pin {:path "field" :sha256 (apply str (repeat 64 "a"))}
              :family-census {} :refusals [] :failures []
              :entries (mapv (fn [r] {:policy-id (:policy-id r) :target-id (:target-id r)
                                       :cascade-digest (:cascade-digest r)
                                       :canonical-cascade {:digest (:cascade-digest r)}}) rows)}]
    (assoc body :status :sealed :menu-pin (family-selection/sealed-menu-pin body))))

(defn evidence [rows]
  (let [s (sealed rows)
        body {:schema :wm/outer-policy-evidence-v1 :status :computed
              :outcome-vocabulary [:success :failure]
              :preference {:vocabulary [:success :failure]
                           :distribution [0.8 0.2] :units :probability
                           :authority (pin "prefs" "4")}
              :preference-authority (pin "prefs" "4")
              :gamma 1.0 :menu-pin (:menu-pin s) :declaration-pin (pin "declaration" "5")
              :dataset-pin (pin "dataset" "6") :entries rows}]
    (assoc body :evidence-pin (policy-evidence/evidence-pin body))))

(defn reseal-evidence [e] (assoc e :evidence-pin (policy-evidence/evidence-pin e)))

(deftest ambiguity-closed-form
  (let [a [2.0 3.0]
        expected (- (novelty/digamma 6.0)
                    (+ (* 0.4 (novelty/digamma 3.0))
                       (* 0.6 (novelty/digamma 4.0))))]
    (is (< (abs (- expected (sut/ambiguity-nats a))) 1.0e-12))))

(deftest concentration-novelty-and-strict-selection
  (let [rows [(row "low" [1.0 1.0] 0.0) (row "high" [10.0 10.0] 0.0)]
        r (sut/evaluate (evidence rows) (sealed rows))
        [low high] (:policies r)]
    (is (> (get-in low [:terms :novelty]) (get-in high [:terms :novelty])))
    (is (< (get-in low [:terms :G]) (get-in high [:terms :G])))
    (is (= "low" (get-in r [:action :policy-id])))
    (is (< (abs (- 1.0 (reduce + (map :posterior (:policies r))))) 1.0e-12))))

(deftest ln-e-changes-choice-without-changing-g
  (let [rows [(row "a" [2.0 2.0] 0.0) (row "b" [2.0 2.0] 2.0)]
        r (sut/evaluate (evidence rows) (sealed rows))]
    (is (= (get-in r [:policies 0 :terms :G]) (get-in r [:policies 1 :terms :G])))
    (is (= "b" (get-in r [:action :policy-id])))))

(deftest exact-tie-and-atomic-refusals
  (let [rows [(row "a" [2.0 2.0] 0.0) (row "b" [2.0 2.0] 0.0)]
        tied (sut/evaluate (evidence rows) (sealed rows))]
    (is (= :selected (:status tied)))
    (is (= :uniform-over-equal-minima (get-in tied [:draw :law])))
    (is (= "a" (get-in (sut/evaluate (evidence rows) (sealed rows) 0.0) [:action :policy-id])))
    (is (= "b" (get-in (sut/evaluate (evidence rows) (sealed rows) 0.99) [:action :policy-id])))
    (is (= tied (sut/evaluate (evidence rows) (sealed rows) (get-in tied [:draw :u]))))
    (is (= :evidence-not-computed
           (:reason (sut/evaluate (assoc (evidence rows) :status :refused) (sealed rows)))))
    (is (= :preference
           (:reason (sut/evaluate (reseal-evidence
                                   (assoc-in (evidence rows) [:preference :vocabulary] [:x :y]))
                                  (sealed rows)))))
    (is (= :row-identity
           (:reason (sut/evaluate (reseal-evidence
                                   (update (evidence rows) :entries conj {:target-id "T"}))
                                  (sealed rows)))))))

(deftest cross-stage-integrity-refuses-before-selection
  (let [rows [(row "a" [2.0 2.0] 0.0) (row "b" [3.0 3.0] 0.0)]
        s (sealed rows) e (evidence rows)]
    (is (= :evidence-pin-mismatch (:reason (sut/evaluate (update e :entries pop) s))))
    (is (= :menu-pin
           (:reason (sut/evaluate (reseal-evidence (assoc e :menu-pin {:path "other"})) s))))
    (doseq [mutate [(fn [x] (update-in x [:entries 0 :ln-e] inc))
                    (fn [x] (update-in x [:entries 0 :posterior-predictive :distribution 0] + 0.1))
                    (fn [x] (update-in x [:entries 0 :posterior-alpha 0] inc))]]
      (is (= :evidence-pin-mismatch (:reason (sut/evaluate (mutate e) s)))))
    (is (= :ln-e
           (:reason (sut/evaluate (reseal-evidence (update-in e [:entries 0 :ln-e] inc)) s))))
    (is (= :posterior-predictive
           (:reason (sut/evaluate (reseal-evidence
                                   (update-in e [:entries 0 :posterior-predictive :distribution 0] + 0.1)) s))))
    (is (= :posterior-alpha-mismatch
           (:reason (sut/evaluate (reseal-evidence (update-in e [:entries 0 :posterior-alpha 0] inc)) s))))))

(deftest resealed-malformations-are-total-typed-refusals
  (let [rows [(row "a" [2.0 2.0] 0.0)] s (sealed rows) e (evidence rows)
        cases [[:ln-e-components #(update-in % [:entries 0 :ln-e-components] dissoc :resource)]
               [:ln-e-components #(assoc-in % [:entries 0 :ln-e-components :resource :value] ##NaN)]
               [:posterior-alpha #(assoc-in % [:entries 0 :posterior-alpha] [])]
               [:posterior-alpha #(assoc-in % [:entries 0 :posterior-alpha] [1.0])]
               [:posterior-alpha #(assoc-in % [:entries 0 :posterior-alpha] [-1.0 2.0])]
               [:posterior-predictive #(assoc-in % [:entries 0 :posterior-predictive :distribution] [])]
               [:posterior-predictive #(assoc-in % [:entries 0 :posterior-predictive :distribution] [1.0])]
               [:posterior-predictive #(assoc-in % [:entries 0 :posterior-predictive :distribution] [0.8 0.8])]
               [:eig #(assoc-in % [:entries 0 :eig] {:status :absent :reason :x})]
               [:eig #(assoc-in % [:entries 0 :eig :units] :bits)]
               [:preference #(assoc-in % [:preference :units] :nats)]
               [:preference-distribution #(assoc-in % [:preference :distribution] [0.8 0.8])]
               [:preference-authority-mismatch #(assoc-in % [:preference :authority] (pin "other" "7"))]
               [:dataset-pin #(assoc % :dataset-pin {:sha256 (apply str (repeat 64 "8"))})]
               [:empty-policy-evidence #(assoc % :entries [])]]]
    (doseq [[reason mutate] cases]
      (let [result (try (sut/evaluate (reseal-evidence (mutate e)) s)
                        (catch Throwable t t))]
        (is (map? result) (str reason " must not throw"))
        (is (= :refused (:status result)))
        (is (= reason (:reason result)))))))

(deftest unscaled-large-g-and-digest-transport
  (let [rows [(row "a" [1.0 9.0] 0.0) (row "b" [9.0 1.0] 0.0)]
        r (sut/evaluate (evidence rows) (sealed rows))
        gs (mapv #(get-in % [:terms :G]) (:policies r))]
    (is (some #(> % 1.0) gs))
    (is (= (get-in r [:selected :cascade-digest])
           (get-in r [:action :cascade-digest])
           (get-in r [:identity-check :digest])))))
