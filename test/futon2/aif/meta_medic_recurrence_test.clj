(ns futon2.aif.meta-medic-recurrence-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-terminal-injury :as terminal]))

(defn pin [label digit]
  {:path (str "fixtures/" label ".edn") :sha256 (apply str (repeat 64 digit))})

(def opening-pin (pin "opening" "1"))
(def fresh-pin (pin "fresh" "2"))
(def observation-pin (pin "observation" "3"))
(def medic-record-pin (pin "medic" "4"))
(def authority-pin (pin "authority" "5"))
(def patch-sha "95145f09395145f09395145f09395145f0939514")
(def review-sha "42bfb4bb542bfb4bb542bfb4bb542bfb4bb542bf")
(def integration-sha "5a5b421bc5a5b421bc5a5b421bc5a5b421bc5a5b")

(def failure {:kind :synthetic-recurrence :stage :construction
              :detail {:target "medic-recurrence"}})

(defn classified [run click source-pin]
  (terminal/classify {:run/id run :click/id click :failure failure} source-pin))

(defn append! [history event verifier]
  (let [result (ledger/append-transition history event verifier)]
    (is (= :appended (:status result)) (pr-str result))
    (:ledger result)))

(defn historical-payload [entry]
  {:kind :historical-misclassification
   :injury-id (:injury-id entry)
   :entry-id (str (:ordinal entry))
   :predecessor-entry-id
   (some-> (get-in entry [:injury-predecessor :entry-id])
           (as-> predecessor-id
               (some (fn [candidate]
                       (when (= predecessor-id (:entry-id candidate))
                         (str (:ordinal candidate))))
                     [])))
   :patch-id "patch/classifier"
   :reviewer "reviewer/independent"
   :evaluator "evaluator/independent"
   :evidence-pin "evidence/synthetic"
   :original-record-id (:path opening-pin)
   :original-pin (:sha256 opening-pin)
   :classifier-revision "classifier/revision"
   :replay :producer-qualified-non-injury})

(defn payload-for [history entry]
  (assoc (historical-payload entry)
         :predecessor-entry-id
         (when-let [predecessor-id (get-in entry [:injury-predecessor :entry-id])]
           (some (fn [candidate]
                   (when (= predecessor-id (:entry-id candidate))
                     (str (:ordinal candidate))))
                 (:entries history)))))

(defn operation-for [history entry operation-id]
  (let [payload (payload-for history entry)]
    {:operation-id operation-id
     :expected-ledger-pin (:ledger-pin history)
     :request {:candidate payload :evidence payload}
     :authority-receipt-pin authority-pin}))

(defn synthetic-authority [{:keys [operation candidate evidence current]}]
  (and (= authority-pin (:authority-receipt-pin operation))
       (= candidate evidence)
       (= [(:injury-id candidate) (:entry-id candidate)]
          [(:injury-id current) (:entry-id current)])))

(defn open-ledger
  ([] (open-ledger ledger/empty-ledger))
  ([base]
   (:ledger (terminal/integrate base
                                (classified "opening" "detect" opening-pin)))))

(defn cover-latest [history operation-id]
  (let [entry (last (filter #(= ledger/transition-schema (:schema %))
                            (:entries history)))
        result (ledger/append-medic-repaired
                history medic-record-pin
                (operation-for history entry operation-id)
                synthetic-authority)]
    (is (= :appended (:status result)) (pr-str result))
    result))

(defn claimed-ledger [history attempt]
  (append! history {:injury-id (:injury-id (classified "opening" "detect" opening-pin))
                    :state :claimed :attempt attempt :repair-id "repair/synthetic"
                    :algorithm "synthetic" :author-session (str "author/" attempt)
                    :reviewer-session (str "reviewer/" attempt)} synthetic-authority))

(defn enacted-ledger []
  (let [injury-id (:injury-id (classified "opening" "detect" opening-pin))
        claim (claimed-ledger (open-ledger) 1)
        authored (append! claim {:injury-id injury-id :state :authored :attempt 1
                                 :repair-id "repair/synthetic" :patch-sha patch-sha
                                 :worktree "/tmp/synthetic"} synthetic-authority)
        reviewed (append! authored {:injury-id injury-id :state :reviewed :attempt 1
                                    :repair-id "repair/synthetic" :verdict :approved
                                    :reviewer-sha review-sha
                                    :reviewer-session "reviewer/1"} synthetic-authority)]
    (append! reviewed {:injury-id injury-id :state :enacted :attempt 1
                       :repair-id "repair/synthetic"
                       :integration-commit integration-sha
                       :retry-click "retry/exact"} synthetic-authority)))

(deftest open-and-enacted-covered-occurrences-recur-visibly
  (doseq [[history click]
          [[(:ledger (cover-latest (open-ledger) "medic/open")) "fresh/open"]
           [(:ledger (cover-latest (enacted-ledger) "medic/enacted"))
            "fresh/nonmatching-retry"]]]
    (let [fresh (classified "fresh" click fresh-pin)
          result (terminal/integrate history fresh synthetic-authority)
          entry (:entry result)
          effective (ledger/effective-active-injuries
                     (:ledger result) synthetic-authority)]
      (is (= :appended (:status result)) (pr-str result))
      (is (= :reopened (:state entry)))
      (is (= :medic-recurrence (:reason entry)))
      (is (= fresh-pin (:new-cause-pin entry)))
      (is (= (inc (count (:entries history))) (:ordinal entry)))
      (is (= (:entry-id (last (filter #(= ledger/transition-schema (:schema %))
                                      (:entries history))))
             (get-in entry [:injury-predecessor :entry-id])))
      (is (= (:ordinal entry)
             (:ordinal (get-in effective [:injuries (:injury-id fresh)])))))))

(deftest exact-enacted-retry-remains-existing-route
  (let [history (:ledger (cover-latest (enacted-ledger) "medic/enacted-exact"))
        result (terminal/integrate history
                                   (classified "retry" "retry/exact" fresh-pin)
                                   synthetic-authority)]
    (is (= :reopened (get-in result [:entry :state])))
    (is (nil? (get-in result [:entry :reason])))
    (is (= "retry/exact" (get-in result [:entry :retry-click])))
    (is (nil? (get-in result [:entry :recurs-after-medic-operation-id])))))

(deftest exact-enacted-retry-at-limit-remains-ordinary-exhaustion
  (let [history (:ledger (cover-latest
                          (ledger/seal-ledger
                           (assoc (enacted-ledger) :attempt-limit 1))
                          "medic/enacted-limit"))
        result (terminal/integrate history
                                   (classified "retry-limit" "retry/exact" fresh-pin)
                                   synthetic-authority)]
    (is (= :appended (:status result)) (pr-str result))
    (is (= :abandoned (get-in result [:entry :state])))
    (is (= :attempt-limit-exhausted (get-in result [:entry :reason])))
    (is (= "retry/exact" (get-in result [:entry :retry-click])))
    (is (nil? (get-in result [:entry :recurs-after-medic-operation-id])))
    (is (= :valid (:status (ledger/validate-ledger
                            (:ledger result) synthetic-authority))))))

(deftest observation-only-and-old-cause-records-remain-duplicates
  (let [open (open-ledger)
        observed (:ledger (ledger/append-observation
                           open observation-pin :diagnostic-only))
        history (:ledger (cover-latest observed "medic/duplicates"))]
    (doseq [source [opening-pin observation-pin]]
      (let [result (terminal/integrate
                    history (classified "duplicate" "other" source)
                    synthetic-authority)]
        (is (= :duplicate (:status result)))
        (is (= history (:ledger result)))))))

(deftest at-limit-exhaustion-retains-fresh-cause-and-is-replay-inert
  (let [limit-one (ledger/seal-ledger (assoc ledger/empty-ledger :attempt-limit 1))
        history (:ledger (cover-latest (open-ledger limit-one) "medic/exhaust"))
        classification (classified "fresh-limit" "fresh-limit" fresh-pin)
        result (terminal/integrate history classification synthetic-authority)
        entry (:entry result)
        replay (terminal/integrate (:ledger result) classification synthetic-authority)]
    (is (= :abandoned (:state entry)))
    (is (= :attempt-limit-exhausted (:reason entry)))
    (is (= fresh-pin (:new-cause-pin entry)))
    (is (= "medic/exhaust" (:recurs-after-medic-operation-id entry)))
    (is (= :duplicate (:status replay)))
    (is (empty? (:injuries (ledger/effective-active-injuries
                            (:ledger result) synthetic-authority))))))

(deftest recomputed-seals-cannot-bypass-prefix-derived-recurrence-bindings
  (let [history (:ledger (cover-latest (open-ledger) "medic/forgery"))
        valid (terminal/integrate history
                                  (classified "fresh-forgery" "fresh" fresh-pin)
                                  synthetic-authority)
        entry (:entry valid)
        forged-entry (ledger/seal-entry
                      (dissoc entry :entry-id :entry-pin
                              :recurs-after-medic-operation-id
                              :recurs-after-medic-occurrence))
        forged (ledger/seal-ledger
                (assoc (:ledger valid) :entries
                       (conj (vec (butlast (:entries (:ledger valid)))) forged-entry)))
        wrong-occurrence-entry
        (ledger/seal-entry
         (assoc (dissoc entry :entry-id :entry-pin)
                :recurs-after-medic-occurrence [(:injury-id entry) 999]))
        wrong-occurrence (ledger/seal-ledger
                          (assoc (:ledger valid) :entries
                                 (conj (vec (butlast (:entries (:ledger valid))))
                                       wrong-occurrence-entry)))
        checked (ledger/validate-ledger forged synthetic-authority)
        wrong-checked (ledger/validate-ledger wrong-occurrence synthetic-authority)]
    (is (= :refused (:status checked)))
    (is (some #{:entry/transition-invalid} (:defects checked)))
    (is (= :refused (:status wrong-checked)))
    (is (some #{:entry/transition-invalid} (:defects wrong-checked)))))

(deftest sealed-above-limit-medic-reopen-is-refused
  (let [history (:ledger
                 (cover-latest
                  (open-ledger (ledger/seal-ledger
                                (assoc ledger/empty-ledger :attempt-limit 1)))
                  "medic/limit-bypass"))
        exhausted (terminal/integrate
                   history (classified "fresh-limit" "fresh-limit" fresh-pin)
                   synthetic-authority)
        forged (-> (:entry exhausted)
                   (dissoc :entry-id :entry-pin)
                   (assoc :state :reopened :reason :medic-recurrence :attempt 2))
        direct (ledger/append-transition history forged synthetic-authority)
        sealed-entry (ledger/seal-entry forged)
        sealed-ledger (ledger/seal-ledger
                       (update history :entries conj sealed-entry))
        checked (ledger/validate-ledger sealed-ledger synthetic-authority)]
    (is (= :refused (:status direct)))
    (is (some #{:medic-recurrence/evidence} (:defects direct)))
    (is (= :refused (:status checked)))
    (is (some #{:entry/transition-invalid} (:defects checked)))))

(deftest recurrence-cause-becomes-current-medic-binding
  (let [history (:ledger (cover-latest (open-ledger) "medic/first"))
        recurred (terminal/integrate history
                                     (classified "fresh-cause" "fresh" fresh-pin)
                                     synthetic-authority)
        claimed (ledger/append-transition
                 (:ledger recurred)
                 {:injury-id (:injury-id (:entry recurred))
                  :state :claimed :attempt 2
                  :repair-id "repair/fresh"
                  :algorithm "algorithm/fresh"
                  :author-session "author/fresh"
                  :reviewer-session "reviewer/fresh"}
                 synthetic-authority)
        current (:entry claimed)
        genuine {:kind :genuine-repair
                 :injury-id (:injury-id current) :entry-id (str (:ordinal current))
                 :predecessor-entry-id
                 (str (:ordinal (:entry recurred)))
                 :patch-id "patch/genuine" :reviewer "reviewer/genuine"
                 :evaluator "evaluator/genuine" :evidence-pin "evidence/genuine"
                 :cause-record-id (:path fresh-pin) :cause-pin (:sha256 fresh-pin)
                 :acceptance-contract "contract/genuine"
                 :pre-replay-receipt "replay/pre" :post-replay-receipt "replay/post"
                 :negative-control-receipt "replay/negative"
                 :source-revision "source/genuine"
                 :original-acceptance-repaired? true}
        operation {:operation-id "medic/fresh-genuine"
                   :expected-ledger-pin (get-in claimed [:ledger :ledger-pin])
                   :request {:candidate genuine :evidence genuine}
                   :authority-receipt-pin authority-pin}
        corrected (ledger/append-medic-repaired
                   (:ledger claimed) medic-record-pin operation synthetic-authority)]
    (is (= :appended (:status claimed)) (pr-str claimed))
    (is (= :appended (:status corrected)) (pr-str corrected))
    (is (empty? (:injuries (ledger/effective-active-injuries
                            (:ledger corrected) synthetic-authority))))))

(deftest supplied-authority-threads-through-ordinary-integrate-branches
  (let [covered (:ledger (cover-latest (open-ledger) "medic/threading"))
        observed (terminal/integrate
                  covered
                  (terminal/classify
                   {:run/id "noninjury" :click/id "other"
                    :failure {:kind :diagnostic-only :stage :selection}}
                   observation-pin)
                  synthetic-authority)
        unrelated (terminal/integrate
                   (:ledger observed)
                   (terminal/classify
                    {:run/id "unrelated" :click/id "other-2"
                     :failure {:kind :other-failure :stage :other-stage
                               :detail {:target "unrelated"}}}
                    {:path "records/unrelated.edn"
                     :sha256 (apply str (repeat 64 "7"))})
                   synthetic-authority)]
    (is (= :appended (:status observed)) (pr-str observed))
    (is (= :appended (:status unrelated)) (pr-str unrelated))
    (is (= :open (get-in unrelated [:entry :state])))))
