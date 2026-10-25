(ns futon2.aif.meta-injury-ledger-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
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

(defn- click-7-ledger []
  ;; Captured from the live click-7 ledger after wm-click-25cd3682's failed
  ;; author answer, rather than reconstructed as a test-only transition.
  (edn/read-string (slurp "test/futon2/aif/fixtures/click-7-ledger.edn")))

(defn- click-10-abandoned-ledger []
  ;; Byte-identical live ledger snapshot at 2026-10-20 19:24Z, sha256
  ;; 3aaa6dc63bd65be00e3c70a337f1871c27b154167ec889f4815ece37a3249bc6.
  (edn/read-string
   (slurp "test/futon2/aif/fixtures/meta-injury-ledger-2026-10-20-d755c886-abandoned.edn")))

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
        ;; The first campaign record is the opening cause. Replaying the
        ;; external run stream appends it again as an inert observation: cause
        ;; attribution and append-only audit presence are distinct facts.
        result (reduce (fn [h pin]
                         (:ledger (ledger/append-observation h pin :campaign-run-record)))
                       history campaign-records)]
    (is (= #{"injury/2026-10-19/meta-route"}
           (set (keys (:injuries (ledger/active-injuries result))))))
    (is (= (mapv :sha256 campaign-records)
           (mapv #(get-in % [:record-pin :sha256])
                 (filter (comp #{:wm/meta-injury-observation-v1} :schema)
                         (:entries result)))))
    (is (= (first campaign-records)
           (:cause-record-pin (first (:entries result)))))
    (is (= (first campaign-records)
           (:record-pin (second (:entries result)))))
    (is (= 4 (:entry-count (ledger/validate-ledger result))))))

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

(deftest requeue-keeps-attempt-and-changes-only-retry-identity
  ;; Pins requeue_consumesNoAttempt and the exact +1 requeue law: deleting
  ;; either comparison from :requeue/evidence makes the forged transition valid.
  (let [history (through-enacted)
        forged (ledger/validate-transition
                history {:injury-id "injury/2026-10-19/meta-route"
                         :state :enacted :attempt 2
                         :repair-id "repair/meta-self-heal-route"
                         :patch-sha patch-sha :requeues 1
                         :retry-click "retry/meta-self-heal/requeued"
                         :integration-commit integration-sha})
        reused (ledger/append-requeue
                history "injury/2026-10-19/meta-route"
                "retry/meta-self-heal/1")]
    (is (= :transition/invalid (:reason forged)))
    (is (some #{:attempt/changed-outside-reopen} (:defects forged)))
    (is (= :requeue/retry-click-reused (:reason reused)))))

(deftest retry-budget-abandons-without-consuming-an-attempt
  ;; Pins inconclusive_abandonsWhenSpent and the independent budget: changing
  ;; the boundary to attempt-limit or incrementing attempt breaks these checks.
  (let [history (through-enacted)
        r1 (ledger/append-requeue history "injury/2026-10-19/meta-route" "retry/r1")
        r2 (ledger/append-requeue (:ledger r1) "injury/2026-10-19/meta-route" "retry/r2")
        r3 (ledger/append-requeue (:ledger r2) "injury/2026-10-19/meta-route" "retry/r3")
        spent (ledger/append-requeue (:ledger r3) "injury/2026-10-19/meta-route" "retry/r4")]
    (is (= [1 2 3] (mapv #(get-in % [:entry :requeues]) [r1 r2 r3])))
    (is (= :abandoned (get-in spent [:entry :state])))
    (is (= :retry-budget-spent (get-in spent [:entry :reason])))
    (is (= 1 (get-in spent [:entry :attempt])))
    (is (= 3 (get-in spent [:entry :requeues])))
    (is (= :valid (:status (ledger/validate-ledger (:ledger spent)))))))

(deftest requeue-requires-latest-enacted-entry
  ;; Pins the re-queue source state. Broadening it to active/terminal entries
  ;; makes at least one of these typed refusals append.
  (let [open (opened)
        authored-history (-> open (claimed 1 "not-enacted") (authored 1))
        enacted-history (through-enacted)
        discharged (:ledger
                    (ledger/append-transition
                     enacted-history
                     {:injury-id "injury/2026-10-19/meta-route"
                      :state :discharged :attempt 1
                      :repair-id "repair/meta-self-heal-route"
                      :patch-sha patch-sha :retry-click "retry/meta-self-heal/1"
                      :outcome :success}))]
    (doseq [history [open authored-history discharged]]
      (is (= :requeue/not-enacted
             (:reason (ledger/append-requeue
                       history "injury/2026-10-19/meta-route" "retry/new")))))))

(deftest retry-limit-is-sealed-ledger-authority
  ;; Pins presence, positivity and seal-prefix propagation of :retry-limit.
  (doseq [invalid [(ledger/seal-ledger (dissoc ledger/empty-ledger :retry-limit
                                                :ledger-pin))
                   (ledger/seal-ledger (assoc ledger/empty-ledger :retry-limit 0))
                   (ledger/seal-ledger (assoc ledger/empty-ledger :retry-limit -1))]]
    (let [checked (ledger/validate-ledger invalid)]
      (is (= :refused (:status checked)))
      (is (some #{:ledger/retry-limit} (:defects checked))))))

(deftest requeues-round-trip-to-newest-attributable-retry
  ;; Pins the newest enacted entry as active state and as the sole predecessor
  ;; of discharge; removing retry-click/requeue carry breaks this round trip.
  (let [history (through-enacted)
        r1 (ledger/append-requeue history "injury/2026-10-19/meta-route" "retry/fresh-1")
        latest1 (get-in (ledger/active-injuries (:ledger r1))
                        [:injuries "injury/2026-10-19/meta-route"])
        r2 (ledger/append-requeue (:ledger r1) "injury/2026-10-19/meta-route"
                                  "retry/fresh-2")
        latest2 (get-in (ledger/active-injuries (:ledger r2))
                        [:injuries "injury/2026-10-19/meta-route"])
        discharged (ledger/append-transition
                    (:ledger r2)
                    {:injury-id "injury/2026-10-19/meta-route"
                     :state :discharged :attempt 1
                     :repair-id "repair/meta-self-heal-route"
                     :patch-sha patch-sha :retry-click "retry/fresh-2"
                     :outcome :grounded-change})]
    (is (= "retry/fresh-1" (:retry-click latest1)))
    (is (= 1 (:requeues latest1)))
    (is (= "retry/fresh-2" (:retry-click latest2)))
    (is (= 2 (:requeues latest2)))
    (is (= :appended (:status discharged)))
    (is (= :valid (:status (ledger/validate-ledger (:ledger discharged)))))))

(deftest retry-inconclusive-observation-requires-both-attribution-fields
  ;; Pins :observation/retry-attribution independently for missing injury,
  ;; missing retry click, and a present-but-blank retry click.
  (let [history (through-enacted)
        appended (ledger/append-observation
                  history (second campaign-records) :retry-inconclusive
                  {:injury-id "injury/2026-10-19/meta-route"
                   :retry-click "retry/meta-self-heal/1"})
        index (dec (count (get-in appended [:ledger :entries])))
        entry (:entry appended)
        forged [(dissoc entry :injury-id)
                (dissoc entry :retry-click)
                (assoc entry :retry-click "  ")]]
    (doseq [candidate forged]
      (let [tampered (-> (:ledger appended)
                         (assoc-in [:entries index] (ledger/seal-entry candidate))
                         ledger/seal-ledger)
            checked (ledger/validate-ledger tampered)]
        (is (= :refused (:status checked)))
        (is (some #{:observation/retry-attribution} (:defects checked)))))))

(deftest unattributed-ordinary-observation-remains-valid
  ;; Pins backward compatibility for the live ledger's bare ordinary records.
  (let [result (ledger/append-observation ledger/empty-ledger
                                          (first campaign-records)
                                          :ordinary-selection-abstention)]
    (is (= :appended (:status result)))
    (is (nil? (get-in result [:entry :injury-id])))
    (is (= :valid (:status (ledger/validate-ledger (:ledger result)))))))

(deftest pending-requeue-reads-only-the-latest-enacted-retry-attribution
  ;; Pins all four reader exclusions: matching latest enactment returns;
  ;; another injury, a superseded retry, and terminal state do not.
  (let [injury-id "injury/2026-10-19/meta-route"
        enacted (through-enacted)
        attributed (ledger/append-observation
                    enacted (second campaign-records) :retry-inconclusive
                    {:injury-id injury-id :retry-click "retry/meta-self-heal/1"})
        other (ledger/append-observation
               enacted (second campaign-records) :retry-inconclusive
               {:injury-id "injury/other" :retry-click "retry/meta-self-heal/1"})
        requeued (ledger/append-requeue (:ledger attributed) injury-id "retry/fresh")
        discharged (ledger/append-transition
                    (:ledger attributed)
                    {:injury-id injury-id :state :discharged :attempt 1
                     :repair-id "repair/meta-self-heal-route"
                     :patch-sha patch-sha :retry-click "retry/meta-self-heal/1"
                     :outcome :success})]
    (is (= (:entry attributed)
           (ledger/pending-requeue (:ledger attributed) injury-id)))
    (is (nil? (ledger/pending-requeue (:ledger other) injury-id)))
    (is (nil? (ledger/pending-requeue (:ledger requeued) injury-id)))
    (is (nil? (ledger/pending-requeue (:ledger discharged) injury-id)))
    (is (nil? (ledger/pending-requeue ledger/empty-ledger injury-id)))))

(deftest open-injuries-cannot-be-downgraded-or-silently-removed
  (let [history (opened)]
    (doseq [state [:open :authored :reviewed :enacted :discharged :abandoned]]
      (let [result (ledger/append-transition
                    history {:injury-id "injury/2026-10-19/meta-route"
                             :state state :attempt 1})]
        (is (= :refused (:status result)))))
    (is (= #{"injury/2026-10-19/meta-route"}
           (set (keys (:injuries (ledger/active-injuries history))))))))

(deftest forged-requeue-entries-are-refused-by-the-validator
  ;; A hand-built enacted->enacted entry reaches the ledger through
  ;; append-transition, not append-requeue, so the validator's
  ;; :requeue/evidence conjuncts are the authority. Each forged entry differs
  ;; from the valid re-queue in one field; deleting the conjunct for that
  ;; field lets that forged entry append.
  (let [injury "injury/2026-10-19/meta-route"
        history (through-enacted)
        valid {:injury-id injury
               :state :enacted :attempt 1
               :repair-id "repair/meta-self-heal-route"
               :patch-sha patch-sha :requeues 1
               :retry-click "retry/meta-self-heal/requeued"
               :integration-commit integration-sha}
        spent (reduce (fn [l click] (:ledger (ledger/append-requeue l injury click)))
                      history ["retry/r1" "retry/r2" "retry/r3"])
        forged {:retry-click-blank [history (assoc valid :retry-click " ")]
                :retry-click-reused [history (assoc valid :retry-click
                                                    "retry/meta-self-heal/1")]
                :attempt-changed [history (assoc valid :attempt 2)]
                :requeues-not-incremented [history (assoc valid :requeues 0)]
                :requeues-skipped [history (assoc valid :requeues 2)]
                :requeues-over-limit [spent (assoc valid :requeues 4
                                                   :retry-click "retry/r4")]
                :patch-substituted [history (assoc valid :patch-sha review-sha)]
                :integration-dropped [history (dissoc valid :integration-commit)]
                :reload-receipt-added [history (assoc valid :reload-receipt
                                                      (first campaign-records))]}]
    (is (= :appended (:status (ledger/append-transition history valid))))
    (is (= 3 (get-in (ledger/active-injuries spent) [:injuries injury :requeues])))
    (doseq [[label [base entry]] forged]
      (let [result (ledger/append-transition base entry)]
        (is (= :transition/invalid (:reason result)) (pr-str label result))
        (is (some #{:requeue/evidence} (:defects result)) (pr-str label result))))))

(deftest abandonment-reasons-do-not-accept-each-other
  ;; :attempt-limit-exhausted needs attempt = :attempt-limit and
  ;; :retry-budget-spent needs :requeues = :retry-limit. Neither shape
  ;; validates under the other reason, a short budget does not validate, and
  ;; an unknown reason validates under neither.
  (let [injury "injury/2026-10-19/meta-route"
        base {:injury-id injury :state :abandoned
              :repair-id "repair/meta-self-heal-route"}
        spent (reduce (fn [l click] (:ledger (ledger/append-requeue l injury click)))
                      (through-enacted) ["retry/r1" "retry/r2" "retry/r3"])
        reopen (fn [history attempt pin]
                 (:ledger (ledger/append-retry-result
                           history {:injury-id injury
                                    :repair-id "repair/meta-self-heal-route"
                                    :patch-sha patch-sha
                                    :retry-click (str "retry/meta-self-heal/" attempt)
                                    :new-cause-pin pin})))
        at-limit (-> (through-enacted)
                     (reopen 1 (second campaign-records))
                     (through-enacted 2 "b")
                     (reopen 2 (nth campaign-records 2))
                     (through-enacted 3 "c"))
        refused? (fn [history entry]
                   (let [result (ledger/append-transition history entry)]
                     (and (= :transition/invalid (:reason result))
                          (boolean (some #{:abandonment/not-exhausted}
                                         (:defects result))))))]
    (is (= :appended (:status (ledger/append-transition
                               spent (assoc base :attempt 1 :requeues 3
                                            :reason :retry-budget-spent)))))
    (is (= :appended (:status (ledger/append-transition
                               at-limit (assoc base :attempt 3
                                               :reason :attempt-limit-exhausted)))))
    (is (refused? spent (assoc base :attempt 1 :requeues 3
                               :reason :attempt-limit-exhausted)))
    (is (refused? at-limit (assoc base :attempt 3 :reason :retry-budget-spent)))
    (is (refused? spent (assoc base :attempt 1 :requeues 2
                               :reason :retry-budget-spent)))
    (is (refused? spent (assoc base :attempt 1 :requeues 3 :reason :gave-up)))))

(deftest click-7-claimed-author-failure-reopens-with-sealed-port-evidence
  (let [history (click-7-ledger)
        injury "injury/d755c886a22d6fc8c04db7fada64089e07157f62"
        claimed (last (filter #(= injury (:injury-id %)) (:entries history)))
        pin {:path "meta-self-heal/author/click-7-failed.edn"
             :sha256 (apply str (repeat 64 "f"))}
        proposal {:injury-id injury :repair-id (:repair-id claimed)
                  :reason :author-failed :port-receipt-pin pin}
        reopened (ledger/append-retry-result history proposal)
        prior-projected (last (get-in (#'ledger/medic-state history)
                                      [:state :history]))
        extra-valid (ledger/append-transition
                     history (assoc proposal :state :reopened :attempt 2
                                    :new-cause-pin (second campaign-records)))
        extra-malformed (ledger/append-transition
                         history (assoc proposal :state :reopened :attempt 2
                                        :new-cause-pin {:path "" :sha256 "bad"}))
        invalid (ledger/append-transition
                 history (assoc proposal :state :reopened :attempt 2
                                :port-receipt-pin nil))
        enacted-unexpected (ledger/append-transition
                            (through-enacted)
                            {:injury-id "injury/2026-10-19/meta-route"
                             :state :reopened :attempt 2
                             :repair-id "repair/meta-self-heal-route"
                             :reason :author-failed
                             :port-receipt-pin pin})]
    (is (= :claimed (:state claimed)) "the fixture pins click 7's live hold")
    (is (= :reopened (get-in reopened [:entry :state])))
    (is (= 2 (get-in reopened [:entry :attempt])))
    (is (= pin (get-in reopened [:entry :port-receipt-pin])))
    (is (= :valid (:status (ledger/validate-ledger (:ledger reopened)))))
    (doseq [result [extra-valid extra-malformed]]
      (is (= :appended (:status result)) (pr-str result))
      (is (= (select-keys prior-projected [:cause-record-id :cause-pin])
             (select-keys (last (get-in (#'ledger/medic-state (:ledger result))
                                        [:state :history]))
                          [:cause-record-id :cause-pin]))))
    (is (some #{:reopen/author-failure-evidence} (:defects invalid)))
    (is (some #{:reopen/reason-unexpected} (:defects enacted-unexpected)))))

(deftest third-claimed-author-failure-abandons-at-the-existing-attempt-limit
  (let [injury "injury/2026-10-19/meta-route"
        pin {:path "meta-self-heal/author/failed.edn"
             :sha256 (apply str (repeat 64 "f"))}
        retry (fn [history]
                (:ledger (ledger/append-retry-result
                          history {:injury-id injury
                                   :repair-id "repair/meta-self-heal-route"
                                   :reason :author-failed :port-receipt-pin pin})))
        attempt1 (claimed (opened) 1 "a")
        attempt2 (claimed (retry attempt1) 2 "b")
        attempt3 (claimed (retry attempt2) 3 "c")
        result (ledger/append-retry-result
                attempt3 {:injury-id injury
                          :repair-id "repair/meta-self-heal-route"
                          :reason :author-failed :port-receipt-pin pin})]
    (is (= :abandoned (get-in result [:entry :state])))
    (is (= :attempt-limit-exhausted (get-in result [:entry :reason])))
    (is (= pin (get-in result [:entry :port-receipt-pin])))
    (is (= :valid (:status (ledger/validate-ledger (:ledger result)))))))

(deftest corrected-port-refusals-reopen-an-abandoned-live-injury
  (let [history (click-10-abandoned-ledger)
        injury "injury/d755c886a22d6fc8c04db7fada64089e07157f62"
        repair "repair/239cf98836afc977cd589141c224ec3f4bca83dc"
        by-ordinal (into {} (map (juxt :ordinal identity)) (:entries history))
        pin9 (:port-receipt-pin (by-ordinal 9))
        pin11 (:port-receipt-pin (by-ordinal 11))
        correction {:injury-id injury :repair-id repair
                    :state :reopened :attempt 4
                    :reason :port-refusal-corrected
                    :correction-commit "f63a3da99bb19f4c2217e20ed6f47572e996ccbc"
                    :corrected-port-receipt-pins [pin9 pin11]}
        result (ledger/append-transition history correction)
        prior-projected (last (get-in (#'ledger/medic-state history)
                                      [:state :history]))
        extra-valid (ledger/append-transition
                     history (assoc correction :new-cause-pin (second campaign-records)))
        extra-malformed (ledger/append-transition
                         history (assoc correction
                                        :new-cause-pin {:path "" :sha256 "bad"}))]
    (is (= :valid (:status (ledger/validate-ledger history))))
    (is (= :appended (:status result)) (pr-str result))
    (is (= 4 (get-in result [:entry :attempt])))
    (is (= :reopened
           (get-in (ledger/active-injuries (:ledger result))
                   [:injuries injury :state])))
    (is (= :valid (:status (ledger/validate-ledger (:ledger result)))))
    (doseq [extra [extra-valid extra-malformed]]
      (is (= :appended (:status extra)) (pr-str extra))
      (is (= (select-keys prior-projected [:cause-record-id :cause-pin])
             (select-keys (last (get-in (#'ledger/medic-state (:ledger extra))
                                        [:state :history]))
                          [:cause-record-id :cause-pin]))))

    (doseq [[label proposed expected-defect]
            [[:wrong-reason (assoc correction :reason :author-failed)
              :reopen/correction-evidence]
             [:missing-commit (dissoc correction :correction-commit)
              :reopen/correction-evidence]
             [:empty-pins (assoc correction :corrected-port-receipt-pins [])
              :reopen/correction-evidence]
             [:skipped-attempt (assoc correction :attempt 5)
              :attempt/not-incremented]
             ;; Review (claude-5, F35): shape-only pins let a correction raise
             ;; the attempt budget by any number. A pin nobody refused, and
             ;; the same refusal cited twice, are refused.
             [:unknown-pins (assoc correction :corrected-port-receipt-pins
                                   [{:path "meta-repair-port-receipts/fake/a.edn"
                                     :sha256 (apply str (repeat 64 "a"))}
                                    {:path "meta-repair-port-receipts/fake/b.edn"
                                     :sha256 (apply str (repeat 64 "b"))}
                                    {:path "meta-repair-port-receipts/fake/c.edn"
                                     :sha256 (apply str (repeat 64 "c"))}])
              :reopen/correction-pins-unknown]
             [:repeated-pin (assoc correction :corrected-port-receipt-pins [pin9 pin9])
              :reopen/correction-pins-unknown]]]
      (let [refused (ledger/append-transition history proposed)]
        (is (= :transition/invalid (:reason refused)) (name label))
        (is (some #{expected-defect} (:defects refused)) (name label))))))

(deftest corrected-port-refusals-extend-the-effective-attempt-limit
  (let [history (click-10-abandoned-ledger)
        injury "injury/d755c886a22d6fc8c04db7fada64089e07157f62"
        repair "repair/239cf98836afc977cd589141c224ec3f4bca83dc"
        by-ordinal (into {} (map (juxt :ordinal identity)) (:entries history))
        pins [(:port-receipt-pin (by-ordinal 9))
              (:port-receipt-pin (by-ordinal 11))]
        correction (appended history
                              {:injury-id injury :repair-id repair
                               :state :reopened :attempt 4
                               :reason :port-refusal-corrected
                               :correction-commit "f63a3da99bb19f4c2217e20ed6f47572e996ccbc"
                               :corrected-port-receipt-pins pins})
        claim (fn [ledger attempt suffix]
                (appended ledger
                          {:injury-id injury :repair-id repair
                           :state :claimed :attempt attempt
                           :algorithm "META-dedicated-self-heal"
                           :author-session (str "corrected-author-" suffix)
                           :reviewer-session (str "corrected-reviewer-" suffix)
                           :work-order-pin (first pins)}))
        failed-pin {:path "meta-repair-port-receipts/corrected/author.edn"
                    :sha256 (apply str (repeat 64 "e"))}
        attempt4 (claim correction 4 "four")
        reopened5 (ledger/append-retry-result
                   attempt4 {:injury-id injury :repair-id repair
                             :reason :author-failed
                             :port-receipt-pin failed-pin})
        attempt5 (claim (:ledger reopened5) 5 "five")
        abandoned (ledger/append-retry-result
                   attempt5 {:injury-id injury :repair-id repair
                             :reason :author-failed
                             :port-receipt-pin failed-pin})]
    (is (= :reopened (get-in reopened5 [:entry :state])))
    (is (= 5 (get-in reopened5 [:entry :attempt])))
    (is (= :abandoned (get-in abandoned [:entry :state])))
    (is (= 5 (get-in abandoned [:entry :attempt])))
    (is (= :attempt-limit-exhausted (get-in abandoned [:entry :reason])))
    (is (= :valid (:status (ledger/validate-ledger (:ledger abandoned)))))))
