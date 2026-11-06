(ns futon2.aif.operational-witness-test
  (:require [clojure.edn :as edn]
            [clojure.java.shell :as sh]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.operational-witness :as witness]))

;; --- registration conformance (M-experiment-build-match) ------------------

(def conformance-registration
  {:clean/experiment :slice5-confirmation
   :clean/schema-version 1
   :clean/experiment-design
   {:arms [{:id :aif-full :ablates []}
           {:id :no-risk :ablates [:kl-risk]}]
    :scenarios [:patchy]
    :seeds {:runs-per-cell 2}
    :environment {:size [10 10]}}})

(defn- conformance-artifact
  [& {:as overrides}]
  (merge {:cells [{:scenario :patchy :arm :aif-full
                   :runs [{:food-seed 202609110 :move-seed 202609111 :choice-seed 202659110}
                          {:food-seed 202609112 :move-seed 202609113 :choice-seed 202659111}]}]}
         overrides))

(deftest conformance-witness-holds-for-a-registered-artifact
  (let [observation {:registration conformance-registration
                     :run-artifact (conformance-artifact)}
        report (witness/verify-registration-conformance observation)]
    (is (:verified? report))
    (is (= 2 (count (:witnessed-tuples report))))
    (is (= {:witnessed? true
            :desc "a run artifact conforms to its registration: every (scenario, arm, seed) cell registered, run counts and arm design unchanged"
            :bindings [{:verified? true :registered-cell-count 2 :row-count 2
                        :witnessed-tuples [[:patchy :aif-full {:food-seed 202609110 :move-seed 202609111 :choice-seed 202659110}]
                                           [:patchy :aif-full {:food-seed 202609112 :move-seed 202609113 :choice-seed 202659111}]]}]}
           (select-keys (get (witness/run-register observation) :registration-conformance)
                        [:witnessed? :desc :bindings])))))

(deftest conformance-witness-rejects-unregistered-arm
  (testing "the exact bad case the relation is named for: a run arm absent
            from the registration — what keying on food-seed alone missed"
    (let [artifact {:cells (conj (:cells (conformance-artifact))
                                 {:scenario :patchy :arm :unregistered-arm
                                  :runs [{:food-seed 1 :move-seed 2 :choice-seed 3}]})}
          report (witness/verify-registration-conformance
                  {:registration conformance-registration :run-artifact artifact})]
      (is (false? (:verified? report)))
      (is (= {:key [:patchy :unregistered-arm] :kind :unregistered-tuple
              :run {:food-seed 1 :move-seed 2 :choice-seed 3}}
             (first (:rejections report))))
      (is (empty? (witness/witness-registration-conformance
                   {:registration conformance-registration :run-artifact artifact}))))))

(deftest conformance-witness-rejects-changed-run-count-and-design
  (let [short (conformance-artifact
               :cells [{:scenario :patchy :arm :aif-full
                        :runs [{:food-seed 202609110 :move-seed 202609111 :choice-seed 202659110}]}])
        drifted (conformance-artifact
                 :cells [{:scenario :patchy :arm :aif-full
                          :runs [{:food-seed 202609110 :move-seed 202609111 :choice-seed 202659110
                                  :design {:ablates [:some-other-term]}}
                                 {:food-seed 202609112 :move-seed 202609113 :choice-seed 202659111
                                  :design {:ablates [:some-other-term]}}]}])]
    (is (= {:key [:patchy :aif-full] :kind :cell-run-count :registered 2 :found 1}
           (first (:rejections (witness/verify-registration-conformance
                                {:registration conformance-registration :run-artifact short})))))
    (is (= {:key [:patchy :aif-full] :kind :design-differs
            :registered [] :found [:some-other-term]}
           (first (:rejections (witness/verify-registration-conformance
                                {:registration conformance-registration :run-artifact drifted})))))))



(def ants-split-revision "0b9a7b0a5eea7fe5bb766761e60e7d3629082b62")

(defn- committed-ants-artifact [path]
  (let [{:keys [exit out err]}
        (sh/sh "git" "-C" "../futon2a" "show"
               (str ants-split-revision ":" path))]
    (when-not (zero? exit)
      (throw (ex-info "Pinned ants artifact is unavailable"
                      {:revision ants-split-revision :path path :error err})))
    (edn/read-string out)))

(defn- run-record
  [yield]
  {:food-seed 11 :move-seed 12 :choice-seed 13 :yield yield})

(deftest reproduction-relation-catches-one-cell-mismatch
  (testing "mandatory positive control: the checker demonstrably fails"
    (let [committed {:cells [{:scenario :patchy :arm :a0
                              :runs [(run-record 4.0)]}]}
          rerun (assoc-in committed [:cells 0 :runs 0 :yield] 5.0)
          report (witness/verify-artifact-reproduction committed rerun)]
      (is (false? (:verified? report)))
      (is (= 1 (:committed-row-count report)))
      (is (= 1 (:rerun-row-count report)))
      (is (= [{:key [:patchy :a0
                     {:food-seed 11 :move-seed 12 :choice-seed 13}]
               :kind :different-run-record
               :committed (run-record 4.0)
               :rerun (run-record 5.0)}]
             (:mismatches report))))))

(deftest committed-pilot-artifact-row-counts-reconcile
  (doseq [[path expected]
          [["holes/labs/ants-faithfulness/slice5.edn" 540]
           ["holes/labs/ants-faithfulness/authority.edn" 360]]]
    (let [artifact (committed-ants-artifact path)
          ;; A re-run need not emit cells or runs in the committed order. Reverse
          ;; both to ensure reconciliation is by the full identity, not position.
          rerun (update artifact :cells
                        (fn [cells]
                          (mapv #(update % :runs (comp vec reverse))
                                (reverse cells))))
          report (witness/verify-artifact-reproduction artifact rerun)]
      (is (:verified? report) path)
      (is (= expected (:committed-row-count report)) path)
      (is (= expected (:rerun-row-count report)) path)
      (is (= expected (:matched-row-count report)) path))))
