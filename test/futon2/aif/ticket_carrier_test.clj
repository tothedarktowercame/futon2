(ns futon2.aif.ticket-carrier-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.mission-registry :as missions]
            [futon2.aif.task-completion-surface :as completion]))

(def ticket
  {:id "T-car3-queue-routing-design-note"
   :kind :ticket
   :path "/tmp/futon3c/holes/tickets/T-car3-queue-routing-design-note.md"
   :source {:path "/tmp/futon3c/holes/tickets/T-car3-queue-routing-design-note.md"
            :sha256 "58c4b43bb886c66d0ee3ad2310b37c69e993fdc2ea72965b1ab6e1483a2e62cc"}})

(def mission
  {:id "M-carrier-control"
   :kind :mission
   :path "/tmp/holes/missions/M-carrier-control.md"
   :source {:path "/tmp/holes/missions/M-carrier-control.md"
            :sha256 "mission-source-sha256"}})

(defn- carrier [action]
  (#'runner/mission-for-decision {:action action} (:target action)))

(deftest cascade-ticket-resolves-the-source-pinned-registry-carrier
  (with-redefs [missions/ticket-entry #(when (= (:id ticket) %) ticket)]
    (let [action {:kind :cascade-candidate :target (:id ticket)}
          resolved (carrier action)
          ;; The live outer field builds this completion surface from the same
          ;; registry entry.  Keep the selection and work authorities equal.
          outer-completion (with-redefs [clojure.core/slurp (constantly "# Ticket\n")]
                             (completion/describe ticket))]
      (is (= (select-keys ticket [:id :kind :path :source])
             (select-keys resolved [:id :kind :path :source])))
      (is (= (:source resolved) (:source-pin outer-completion))))))

(deftest mission-and-advance-ticket-resolution-are-unchanged
  (with-redefs [missions/open-missions (constantly [mission])
                missions/ticket-entry #(when (= (:id ticket) %) ticket)]
    (is (= mission (carrier {:kind :cascade-candidate :target (:id mission)})))
    (is (= ticket (carrier {:type :advance-ticket :target (:id ticket)})))))

(deftest absent-ticket-stays-absent-and-completion-source-guard-stays-sharp
  (with-redefs [missions/ticket-entry (constantly nil)]
    (is (nil? (carrier {:kind :cascade-candidate :target "T-not-registered"}))))
  (with-redefs-fn {#'runner/resolve-target-build
                   (fn [_ _ _] {:repo "/tmp/repo" :files []})}
    #(let [failure (try
                     (runner/review-already-satisfied-claim!
                      {} {} {} "T-not-registered" nil {} {}
                      {:grounded-commit {:repo "repo" :sha "commit"}}
                      (fn []) nil)
                     nil
                     (catch clojure.lang.ExceptionInfo e (ex-data e)))]
       (testing "missing registry evidence does not manufacture a source pin"
         (is (= :review-completion-source-unavailable (:failure-kind failure)))))))
