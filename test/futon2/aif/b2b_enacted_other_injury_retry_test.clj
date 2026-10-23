(ns futon2.aif.b2b-enacted-other-injury-retry-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-ledger-test :as fixture]
            [futon2.aif.meta-terminal-injury :as terminal]))

;; Exact classification from b2a-replay.edn, derived from retained run 57cd4dc4.
(def recorded (edn/read-string (slurp "test/resources/b2b/classification.edn")))
(def injury-a (:injury-id recorded))
(def injury-b "injury/cfbcbad062bde07d539c7e2abd97a6a189f910f4")
(def retry-a "retry/fixture-a")
(def retry-b (:click-id recorded))
(def pin {:path "fixtures/b2b-retry.edn" :sha256 (apply str (repeat 64 "a"))})

(defn classification [click]
  (#'terminal/seal-classification
   (-> recorded (dissoc :classification-pin)
       (assoc :click-id click :record-pin pin))))

(defn enact [history injury retry]
  (reduce (fn [h event]
            (fixture/appended h (merge {:injury-id injury :attempt 1
                                       :repair-id (str "repair/" injury)} event)))
          history
          [{:state :open :cause-record-pin (assoc pin :path (str "fixtures/" injury))
            :failure-kind :reviewer-falsifier-failed :failure-stage :reviewer-wait
            :detected-click (str "initial/" injury)}
           {:state :claimed :algorithm "META" :author-session (str "author/" injury) :reviewer-session (str "reviewer/" injury)}
           {:state :authored :patch-sha fixture/patch-sha :worktree "/home/joe/worktrees/futon2/b2b-fixture"}
           {:state :reviewed :verdict :approved :reviewer-sha fixture/review-sha :reviewer-session (str "reviewer/" injury)}
           {:state :enacted :integration-commit fixture/integration-sha :retry-click retry}]))

(defn history [] (-> ledger/empty-ledger (enact injury-a retry-a) (enact injury-b retry-b)))

(deftest other-enacted-injury-retains-both-attributions
  (let [before (history)
        c (classification retry-b)
        result (terminal/integrate before c)
        after (:ledger result)]
    (is (= :valid (:status (terminal/verify-classification recorded))))
    (is (= :appended (:status result)))
    (is (some? (ledger/pending-requeue after injury-b)))
    (when after
      (is (= [{:observation-kind :repeated-active-injury :injury-id injury-a}
              {:observation-kind :retry-inconclusive :injury-id injury-b :retry-click retry-b}]
             (mapv #(select-keys % [:observation-kind :injury-id :retry-click])
                   (drop (count (:entries before)) (:entries after)))))
      (is (= :valid (:status (ledger/validate-ledger after))))
      (is (= :duplicate (:status (terminal/integrate after c)))))))

(deftest wrong-unowned-retry-still-refuses
  (let [result (terminal/integrate (history) (classification "retry/unowned"))]
    (is (= :injury/retry-click-mismatch (:reason result)))
    (is (= retry-a (:expected result)))
    (is (= "retry/unowned" (:actual result)))))

(deftest same-injury-retry-still-reopens
  (let [result (terminal/integrate (history) (classification retry-a))]
    (is (= :appended (:status result)))
    (is (= :reopened (:transition result)))
    (is (= injury-a (get-in result [:entry :injury-id])))
    (is (= :valid (:status (ledger/validate-ledger (:ledger result)))))))
