(ns futon2.report.new-wanted-token-admission-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.scoring-input-receipts :as receipts]
            [futon2.aif.wm.cascade-decision :as wm-cd]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.test-support.reading-route :as route]))

(use-fixtures :once hermetic/with-hermetic-stores)
(use-fixtures :each route/with-empty-learning-ledger)

(def blend-square
  (edn/read-string
   (slurp "test/fixtures/three-halves-square/publication-cadence.edn")))

(defn- with-square [problem]
  (update problem :interpretation-receipts
          #(update-vals % (fn [receipt]
                            (assoc receipt :blend-square blend-square)))))

(defn fixture []
  (update (edn/read-string
           (slurp (io/resource "fixtures/new-wanted-token/1789964661.edn")))
          :problems #(mapv with-square %)))

(defn admit [p] (#'wm-cd/admit-cascade-problem p))

(defn candidate-decline [r]
  (first (filter #(= :candidate-admission (:stage %)) (:declines r))))

(deftest recorded-no-op-is-refused
  (let [{:keys [problems recorded-D]} (fixture)
        results (mapv admit problems)
        f2 (last results)
        drop (candidate-decline f2)]
    (is (= recorded-D (:value (receipts/initial-belief problems))))
    (is (= ["M-aif-policy-conditioned-eig" "M-f11-find-production-successor"]
           (mapv #(get-in % [:problem :target]) (take 2 results))))
    (is (nil? (:problem f2)))
    (is (= :no-new-wanted-token (:reason drop)))
    (is (= #{:route-a-rehearsal-reported} (get-in drop [:evidence :initial-wanted-tokens])))
    (is (= {#{:route-a-rehearsal-reported} 1} (get-in drop [:evidence :terminal-wanted-belief])))
    (is (= #{} (get-in drop [:evidence :new-wanted-tokens])))
    (is (= 2 (get-in drop [:evidence :horizon])))))

(defn problem [horizon]
  (with-square
    {:target "M-test"
     :constructed-candidates [{:candidate-id :C1 :precedence [:finish :prepare]
                               :construction-receipt {:source :test}}]
     :interpretation-receipts {:finish {:source :test} :prepare {:source :test}}
     :cascade-problem
     {:facts {:ready true :bridge false :done false} :want [:done] :beta 1
      :horizon-steps horizon
      :interpretations
      {:prepare {:guard {:needs #{:ready} :forbids #{:bridge}} :produces #{:bridge}}
       :finish {:guard {:needs #{:bridge} :forbids #{:done}} :produces #{:done}}}}}))

(deftest horizon-is-a-real-boundary
  (let [short (admit (problem 1)) long (admit (problem 2))]
    (is (= :no-new-wanted-token (:reason (candidate-decline short))))
    (is (= {#{} 1} (get-in (candidate-decline short) [:evidence :terminal-wanted-belief])))
    (is (some? (:problem long)))
    (is (empty? (:declines long)))))

(deftest adding-one-wanted-token-is-enough
  (let [p (-> (problem 1)
              (assoc-in [:cascade-problem :facts :bridge] true)
              (assoc-in [:cascade-problem :want] [:done :other]))]
    (is (some? (:problem (admit p))))))

(deftest missing-horizon-is-not-a-no-progress-finding
  (let [drop (candidate-decline (admit (problem nil)))]
    (is (= :candidate-prediction-refused (:reason drop)))
    (is (= :missing-common-horizon (get-in drop [:evidence :refusal :kind])))))

;; Migrated 2026-10-15 (Q5 route ledger). The interpreted admission above
;; (admit-cascade-problem, still used by the flight ask) keeps its own
;; controls. The decision no longer admits interpreted problems: on the
;; reading-family route the claims below are re-proved over policy families.

(deftest all-refused-uses-existing-abstention
  ;; a field in which no target yields a scored policy uses the existing
  ;; gated abstention, with the selector never called
  (let [r (route/decide-field (route/field {"M-a" nil}) {"M-a" 1} {})]
    (is (= :abstained (get-in r [:decision :status])))
    (is (= :no-acting-cascade-candidate (get-in r [:decision :reason])))
    (is (= :no-policy-family (get-in r [:decision :refusals 0 :kind])))
    (is (empty? (:lanes r)))
    (is (= 0 (:selector-calls r)))))

(deftest rejection-cannot-hide-an-incommensurable-family
  ;; a target whose family fails cannot mask differing temperatures between
  ;; the targets that do have families
  (let [fld (route/field {"M-a" ["p/a" "p/b"] "M-b" ["p/b" "p/c"] "M-none" nil})
        r (route/decide-field fld {"M-a" 1 "M-b" 3 "M-none" 1} {})]
    (is (= :incommensurable-family (get-in r [:decision :refusals 0 :kind])))
    (is (= 0 (:selector-calls r)))))
