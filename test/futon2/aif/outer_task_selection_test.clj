(ns futon2.aif.outer-task-selection-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-task-selection :as sut]
            [futon2.aif.task-requisition :as requisition]))

(def tasks
  [{:id "M-alpha" :kind :mission :status-class :open :open-hole-count 2}
   {:id "E-beta" :kind :excursion :status-class :active}
   {:id "T-gamma" :kind :ticket :status-class :live}])

(deftest inner-cascade-material-cannot-affect-task-selection
  (let [baseline (sut/select-task {:tasks tasks :seed 20261002})
        poisoned (mapv #(assoc %
                              :constructed-candidates [{:precedence [:wrong/order]}]
                              :interpretations {:secret {:produces #{:x}}}
                              :g -1.0e300
                              :g-terms {:risk -1.0e300})
                       tasks)
        changed (sut/select-task {:tasks poisoned :seed 20261002})]
    (is (= baseline changed))
    (is (not-any? #(some (set (keys %))
                         [:constructed-candidates :interpretations :g :g-terms])
                  (:support changed)))
    (is (= #{:id :kind :status-class :open-hole-count :priority
             :requisition :source}
           (set (get-in changed [:policy :uses]))))))

(deftest selection-is-recomputable-and-yields-a-task-action
  (let [a (sut/select-task {:tasks tasks :seed 7})
        b (sut/select-task {:tasks (reverse tasks) :seed 7})]
    (is (= a b) "input order does not alter the sorted support or draw")
    (is (= (get-in a [:chosen :id]) (get-in a [:action :target])))
    (is (contains? #{:advance-mission :advance-excursion :advance-ticket}
                   (get-in a [:action :type])))))

(deftest absent-support-and-seed-are-typed
  (testing "no valid M/E/T/A task"
    (is (= {:absent :no-selectable-task}
           (:chosen (sut/select-task {:tasks [{:id "X" :kind :unknown}] :seed 1})))))
  (testing "selection cannot smuggle in a clock-derived default seed"
    (is (= {:absent :no-seed}
           (:chosen (sut/select-task {:tasks tasks}))))))

(defn- sha256 [text]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                        (.getBytes text "UTF-8"))]
    (apply str (map #(format "%02x" %) digest))))

(deftest exact-completed-kimi-requisition-is-retained-but-not-selectable
  (let [path "holes/excursions/E-kimi-task-10.md"
        text (slurp path)
        source {:path (str (java.io.File. path)) :sha256 (sha256 text)}
        task {:id "E-kimi-task-10" :kind :excursion :status-class :unknown
              :requisition (requisition/read-state text) :source source}
        pending {:id "M-pending" :kind :mission :status-class :open
                 :requisition {:absent :no-requisition}
                 :source {:path "holes/missions/M-pending.md"
                          :sha256 (apply str (repeat 64 "a"))}}
        result (sut/select-task {:tasks [task pending] :seed 9})
        excluded (first (:excluded result))]
    (is (= "5a9982ebc67644b9a9e3d1d848690565b9183dd2ecbd6095244b62e38ab01f9c"
           (:sha256 source)))
    (is (= {:state :completed
            :text "2026-09-25T01:15:44Z, job invoke-1790298250720-24084-538acf12, state done"}
           (:requisition task)))
    (is (= ["M-pending"] (mapv :id (:support result))))
    (is (= "E-kimi-task-10" (:id excluded)))
    (is (= :requisition/completed (:ineligible-reason excluded)))
    (is (= {:requisition (:requisition task) :source source}
           (:ineligibility-evidence excluded)))))

(deftest requisition-eligibility-is-kind-general
  (let [pinned (fn [id kind requisition]
                 {:id id :kind kind :status-class :open
                  :requisition requisition
                  :source {:path (str id ".md")
                           :sha256 (apply str (repeat 64 (first (name kind))))}})
        blocked [(pinned "M-done" :mission {:state :completed})
                 (pinned "E-busy" :excursion {:state :in-progress})
                 (pinned "T-done" :ticket {:state :completed})]
        pending [(pinned "M-new" :mission {:absent :no-requisition})
                 (pinned "E-new" :excursion {:malformed "**Requisition:** waiting"})
                 (pinned "T-new" :ticket {:absent :no-requisition})]
        result (sut/select-task {:tasks (concat blocked pending) :seed 2})]
    (is (= #{"M-new" "E-new" "T-new"} (set (map :id (:support result)))))
    (is (= #{"M-done" "E-busy" "T-done"} (set (map :id (:excluded result)))))
    (is (= #{:requisition/completed :requisition/in-progress}
           (set (map :ineligible-reason (:excluded result)))))
    (is (every? #(get-in % [:ineligibility-evidence :source :sha256])
                (:excluded result)))))
