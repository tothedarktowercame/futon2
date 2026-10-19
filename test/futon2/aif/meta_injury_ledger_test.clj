(ns futon2.aif.meta-injury-ledger-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.meta-injury-ledger :as ledger]))

(def campaign-records
  [{:path "data/wm-runs/tick-run-record-2026-10-19-bd1f106d-5bdc-4b9d-8350-5d9c9bd6832f.edn"
    :sha256 "11250189cfd7ac0c36ed237086547453b6bca8f25fbc8eb2ee0430d04eb7f8b7"}
   {:path "data/wm-runs/tick-run-record-2026-10-19-be38a3a9-92bc-4084-b7a6-1c4d2fcfe84a.edn"
    :sha256 "97b4480739c63be9814bc503953f6c4ffb02c28214250ace115253cc823f287d"}
   {:path "data/wm-runs/tick-run-record-2026-10-19-e86d38df-e527-4648-bdfe-cce61133330f.edn"
    :sha256 "d85ac99e9ccd2751cc875f001e6f0e0ddf98569d547ff2489eff66764faae983"}])

(def patch-sha "95145f09395145f09395145f09395145f0939514")
(def review-sha "42bfb4bb542bfb4bb542bfb4bb542bfb4bb542bf")
(def integration-sha "5a5b421bc5a5b421bc5a5b421bc5a5b421bc5a5b")

(defn appended [ledger transition]
  (let [result (ledger/append-transition ledger transition)]
    (is (= :appended (:status result)) (pr-str result))
    (:ledger result)))

(defn opened
  ([] (opened ledger/empty-ledger "injury/2026-10-19/meta-route" (first campaign-records)))
  ([base injury-id cause]
   (appended base {:injury-id injury-id :state :open :attempt 1
                   :cause-record-pin cause :failure-stage :construction
                   :failure-kind :cascade-construction-failed
                   :detected-click "2026-10-19-bd1f106d-5bdc-4b9d-8350-5d9c9bd6832f"})))

(defn claimed [history attempt suffix]
  (appended history {:injury-id "injury/2026-10-19/meta-route"
                     :state :claimed :attempt attempt
                     :repair-id "repair/meta-self-heal-route"
                     :algorithm "META-dedicated-self-heal"
                     :author-session (str "author-session-" suffix)
                     :reviewer-session (str "reviewer-session-" suffix)}))
(defn authored [history attempt]
  (appended history {:injury-id "injury/2026-10-19/meta-route"
                     :state :authored :attempt attempt
                     :repair-id "repair/meta-self-heal-route"
                     :patch-sha patch-sha
                     :worktree "/home/joe/worktrees/meta-self-heal-repair"}))
(defn reviewed [history attempt suffix]
  (appended history {:injury-id "injury/2026-10-19/meta-route"
                     :state :reviewed :attempt attempt
                     :repair-id "repair/meta-self-heal-route"
                     :verdict :approved :reviewer-sha review-sha
                     :reviewer-session (str "reviewer-session-" suffix)}))
(defn enacted [history attempt]
  (appended history {:injury-id "injury/2026-10-19/meta-route"
                     :state :enacted :attempt attempt
                     :repair-id "repair/meta-self-heal-route"
                     :retry-click (str "retry/meta-self-heal/" attempt)
                     :integration-commit integration-sha}))
(defn through-enacted
  ([] (through-enacted (opened) 1 "a"))
  ([history attempt suffix]
   (-> history (claimed attempt suffix) (authored attempt)
       (reviewed attempt suffix) (enacted attempt))))

(deftest complete-valid-transaction-discharges-only-on-attributable-retry
  (let [history (through-enacted)
        unrelated (ledger/append-transition
                   history {:injury-id "injury/2026-10-19/meta-route"
                            :state :discharged :attempt 1
                            :repair-id "repair/meta-self-heal-route"
                            :patch-sha patch-sha :retry-click "unrelated-click"
                            :outcome :success})
        discharged (ledger/append-transition
                    history {:injury-id "injury/2026-10-19/meta-route"
                             :state :discharged :attempt 1
                             :repair-id "repair/meta-self-heal-route"
                             :patch-sha patch-sha
                             :retry-click "retry/meta-self-heal/1"
                             :outcome :grounded-change})]
    ;; A retry is attributable by the repair id, exact authored patch, and the
    ;; retry identity. Merely successful but forged-patch work is refused.
    (is (= :transition/invalid (:reason unrelated)))
    (is (= :appended (:status discharged)))
    (is (empty? (:injuries (ledger/active-injuries (:ledger discharged)))))
    (is (= :transition/invalid
           (:reason (ledger/append-transition
                     history {:injury-id "injury/2026-10-19/meta-route"
                              :state :discharged :attempt 1
                              :repair-id "repair/meta-self-heal-route"
                              :patch-sha review-sha :retry-click "other"
                              :outcome :success}))))))

(deftest arbitrary-nondischarge-appends-never-reduce-active-set
  (let [initial (opened)
        before (set (keys (:injuries (ledger/active-injuries initial))))
        records (take 64
                      (map (fn [n]
                             {:path (str "data/wm-runs/arbitrary-" n ".edn")
                              :sha256 (format "%064x" n)})
                           (iterate inc 1)))
        after-ledger (reduce (fn [history [n pin]]
                               (:ledger (ledger/append-observation
                                         history pin
                                         (if (even? n) :run-record-append
                                             :unrelated-event))))
                             initial (map-indexed vector records))
        after (set (keys (:injuries (ledger/active-injuries after-ledger))))]
    (is (= before after))
    (is (= 65 (:entry-count (ledger/validate-ledger after-ledger))))))

(deftest campaign-record-appends-cannot-erase-injury
  (let [history (opened)
        result (reduce (fn [h pin]
                         (:ledger (ledger/append-observation h pin :campaign-run-record)))
                       history (rest campaign-records))]
    (is (= #{"injury/2026-10-19/meta-route"}
           (set (keys (:injuries (ledger/active-injuries result))))))
    (is (= (mapv :sha256 (rest campaign-records))
           (mapv #(get-in % [:record-pin :sha256])
                 (filter (comp #{:wm/meta-injury-observation-v1} :schema)
                         (:entries result)))))))

(deftest illegal-order-identity-substitution-and-session-reuse-refuse-typedly
  (let [open (opened)
        claim (claimed open 1 "fresh")]
    (doseq [[proposal defect]
            [[{:injury-id "injury/2026-10-19/meta-route" :state :reviewed
               :attempt 1 :repair-id "repair/meta-self-heal-route"
               :verdict :approved :reviewer-sha review-sha
               :reviewer-session "reviewer-session-fresh"}
              :transition/illegal-order]
             [{:injury-id "injury/2026-10-19/meta-route" :state :authored
               :attempt 1 :repair-id "repair/substituted" :patch-sha patch-sha
               :worktree "/tmp/scoped"}
              :repair/id-substitution]]]
      (let [result (ledger/append-transition claim proposal)]
        (is (= :transition/invalid (:reason result)))
        (is (some #{defect} (:defects result)))))
    (let [reopened (-> claim (authored 1) (reviewed 1 "fresh") (enacted 1)
                       (ledger/append-retry-result
                        {:injury-id "injury/2026-10-19/meta-route"
                         :repair-id "repair/meta-self-heal-route"
                         :patch-sha patch-sha :retry-click "retry/meta-self-heal/1"
                         :new-cause-pin (second campaign-records)}))
          result (ledger/append-transition
                  (:ledger reopened)
                  {:injury-id "injury/2026-10-19/meta-route" :state :claimed
                   :attempt 2 :repair-id "repair/meta-self-heal-route"
                   :algorithm "META-dedicated-self-heal"
                   :author-session "author-session-fresh"
                   :reviewer-session "reviewer-session-new"})]
      (is (some #{:claim/session-not-fresh} (:defects result))))))

(deftest replay-and-tampering-are-total-typed-refusals
  (let [history (opened)
        event (first (:entries history))
        replay (ledger/append-transition history event)
        tampered (assoc-in history [:entries 0 :failure-kind] :substituted)
        malformed-values [nil {} [] {:schema ledger/ledger-schema
                                     :attempt-limit 3 :entries :not-a-vector}]]
    (is (= :transition/duplicate-replay (:reason replay)))
    (is (= :refused (:status (ledger/validate-ledger tampered))))
    (doseq [value malformed-values]
      (is (= :refused (:status (ledger/validate-ledger value))))
      (is (= :refused (:status (ledger/append-transition value {}))))
      (is (= :refused (:status (ledger/append-retry-result value {})))))))

(deftest reopen-increments-attempt-and-limit-exhaustion-abandons
  (let [attempt1 (through-enacted)
        reopen1 (ledger/append-retry-result
                 attempt1 {:injury-id "injury/2026-10-19/meta-route"
                           :repair-id "repair/meta-self-heal-route"
                           :patch-sha patch-sha :retry-click "retry/meta-self-heal/1"
                           :new-cause-pin (second campaign-records)})
        attempt2 (through-enacted (:ledger reopen1) 2 "b")
        reopen2 (ledger/append-retry-result
                 attempt2 {:injury-id "injury/2026-10-19/meta-route"
                           :repair-id "repair/meta-self-heal-route"
                           :patch-sha patch-sha :retry-click "retry/meta-self-heal/2"
                           :new-cause-pin (nth campaign-records 2)})
        attempt3 (through-enacted (:ledger reopen2) 3 "c")
        exhausted (ledger/append-retry-result
                   attempt3 {:injury-id "injury/2026-10-19/meta-route"
                             :repair-id "repair/meta-self-heal-route"
                             :patch-sha patch-sha :retry-click "retry/meta-self-heal/3"
                             :new-cause-pin (first campaign-records)})]
    (is (= :reopened (:state (:entry reopen1))))
    (is (= 2 (:attempt (:entry reopen1))))
    (is (= :reopened (:state (:entry reopen2))))
    (is (= :abandoned (:state (:entry exhausted))))
    (is (= :attempt-limit-exhausted (:reason (:entry exhausted))))
    (is (empty? (:injuries (ledger/active-injuries (:ledger exhausted)))))))

(deftest open-injuries-cannot-be-downgraded-or-silently-removed
  (let [history (opened)]
    (doseq [state [:open :authored :reviewed :enacted :discharged :abandoned]]
      (let [result (ledger/append-transition
                    history {:injury-id "injury/2026-10-19/meta-route"
                             :state state :attempt 1})]
        (is (= :refused (:status result)))))
    (is (= #{"injury/2026-10-19/meta-route"}
           (set (keys (:injuries (ledger/active-injuries history))))))))
