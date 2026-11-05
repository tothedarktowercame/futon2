(ns futon2.aif.wm.run-brief-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.wm.run-brief :as run-brief]))

(def ledger
  {:schema run-brief/ledger-schema
   :occasions
   [{:schema run-brief/occasion-schema
     :occasion/id "occ-prev" :occasion/seq 1
     :occasion/summary "previous run"}
    {:schema run-brief/occasion-schema
     :occasion/id "occ-latest" :occasion/seq 2
     :occasion/summary "latest run"}]
   :decisions
   [{:schema run-brief/decision-schema
     :decision/id "D1" :occasion/id "occ-latest" :decision/status :settled
     :decision/summary "settled thing" :decision/basis "test"}
    {:schema run-brief/decision-schema
     :decision/id "D2" :occasion/id "occ-latest" :decision/status :newly-opened
     :decision/summary "newly opened thing" :decision/basis "test"}
    {:schema run-brief/decision-schema
     :decision/id "D3" :occasion/id "occ-prev" :decision/status :outstanding
     :decision/summary "still outstanding thing" :decision/basis "test"}]})

(deftest ledger-accepts-well-formed-entries
  (is (= ledger (run-brief/validate-ledger ledger))))

(deftest render-distinguishes-the-three-operator-questions
  (let [brief (run-brief/render-brief ledger)]
    (is (re-find #"decisions since occ-prev \(occasion occ-latest\)" brief))
    (is (re-find #"### Settled since the previous occasion\n- \*\*D1\*\*" brief))
    (is (re-find #"### Newly needing an operator call\n- \*\*D2\*\*" brief))
    (is (re-find #"### Still outstanding\n- \*\*D3\*\*" brief))
    (is (.contains brief "basis: test"))
    ;; the source is the ledger, not a conversation: no chat artifacts appear
    (is (not (.contains brief "conversation")))))

(deftest render-is-deterministic
  (is (= (run-brief/render-brief ledger) (run-brief/render-brief ledger))))

(deftest invalid-status-entry-is-a-typed-error-not-a-drop
  ;; The exact bad case the validator is named for: an entry whose status is
  ;; not one of the three documented statuses must be refused with its entry
  ;; retained, so the ledger cannot silently lose rows.
  (let [bad (assoc-in ledger [:decisions 0 :decision/status] :maybe)
        result (run-brief/validate-ledger bad)]
    (is (= :wm/run-brief-error-v1 (:schema result)))
    (is (= :invalid-entries (:kind result)))
    (is (= "D1" (-> result :decision-errors first :entry :decision/id)))))

(deftest orphan-decision-attachment-is-refused
  (let [bad (assoc-in ledger [:decisions 1 :occasion/id] "occ-unknown")
        result (run-brief/validate-ledger bad)]
    (is (= :decision-occasion-unknown (:kind result)))
    (is (= ["D2"] (mapv :decision/id (:entries result))))))

(deftest duplicate-decision-id-is-refused
  (let [bad (update ledger :decisions conj (assoc (first (:decisions ledger))
                                                  :decision/summary "dupe"))
        result (run-brief/validate-ledger bad)]
    (is (= :duplicate-decision-id (:kind result)))))

(deftest classify-since-scopes-to-the-latest-occasion
  (let [c (run-brief/classify-since ledger)]
    (is (= {:previous "occ-prev" :latest "occ-latest"} (:since-occasion c)))
    (is (= ["D1"] (mapv :decision/id (:settled (:by-status c)))))
    (is (= ["D2"] (mapv :decision/id (:newly-opened (:by-status c)))))
    (is (= [] (vec (:outstanding (:by-status c)))))))
