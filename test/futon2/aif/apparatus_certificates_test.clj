(ns futon2.aif.apparatus-certificates-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.hierarchical-budget-adapter :as r11]
            [futon2.aif.wm.apparatus-certificates :as apparatus]))

(def r9
  {:schema :wm/r9-independence-admission-v1
   :roles {:author "wm-author" :reviewer "wm-reviewer"}
   :joins {:distinct :passed :artifact :passed}})

(def r10
  {:status :verified-causal-route :production-edge-fired? true})

(def r12
  {:status :admitted :node :R12
   :returned {:artifact {:layer-2/independent-evidence
                         {:layer :R12/layer-2 :status :admitted
                          :independent? true}}}})

(def r15
  {:schema :wm/r15-two-tick-certificate-v1
   :fast-outcome-independently-witnessed? true
   :slow-state-before {:alpha 1 :beta 1}
   :slow-state-after {:alpha 2 :beta 1}
   :next-tick-consumed-slow-state {:alpha 2 :beta 1}})

(deftest ordinary-click-reports-honest-apparatus-state
  (let [r (apparatus/receipt {:data {}} :duree-click-on-demand)]
    (is (= :incomplete (:status r)))
    (is (= {:present 0 :refused 3 :not-applicable 2} (:counts r)))
    (is (= :not-applicable (get-in r [:by-node :R10 :status])))
    (is (= :not-applicable (get-in r [:by-node :R11 :status])))
    (is (= :refused (get-in r [:by-node :R9 :status])))
    (is (= :refused (get-in r [:by-node :R12 :status])))
    (is (= :refused (get-in r [:by-node :R15 :status])))))

(deftest complete-applicable-certificate-packet
  (let [budget (get-in (r11/select-ranked-proposal-fields
                         {:shared-budget 1
                         :fields [{:id :one :budget 1
                                   :proposals [{:id :p :rank 1 :cost 1 :utility 2
                                                :action {:type :demo}}]}]})
                       [:replay/receipt])
        result {:data {:apparatus-evidence
                       {:R9 r9 :R10 r10 :R11 budget :R12 r12 :R15 r15}}}
        r (apparatus/receipt result :scheduled)]
    (is (= :complete (:status r)))
    (is (= {:present 5 :refused 0 :not-applicable 0} (:counts r)))
    (is (every? #(= :present (:status %)) (vals (:by-node r))))))

(deftest mutations-fail-closed
  (testing "same author and reviewer cannot certify R9"
    (is (= :refused
           (get-in (apparatus/receipt
                    {:data {:apparatus-evidence
                            {:R9 (assoc-in r9 [:roles :reviewer] "wm-author")}}}
                    :duree-click-on-demand)
                   [:by-node :R9 :status]))))
  (testing "an empty join set cannot certify R9"
    (is (= :refused
           (get-in (apparatus/receipt
                    {:data {:apparatus-evidence {:R9 (assoc r9 :joins {})}}}
                    :duree-click-on-demand)
                   [:by-node :R9 :status]))))
  (testing "a scheduled fixture route is not a production R10 witness"
    (is (= :refused
           (get-in (apparatus/receipt
                    {:data {:apparatus-evidence
                            {:R10 (assoc r10 :production-edge-fired? false)}}}
                    :scheduled)
                   [:by-node :R10 :status]))))
  (testing "Layer 1 cannot impersonate independent Layer 2"
    (is (= :refused
           (get-in (apparatus/receipt
                    {:data {:apparatus-evidence
                            {:R12 (assoc-in r12
                                           [:returned :artifact :layer-2/independent-evidence
                                            :independent?]
                                           false)}}}
                    :duree-click-on-demand)
                   [:by-node :R12 :status])))))
