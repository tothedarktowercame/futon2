(ns futon2.aif.contextual-preferences-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.contextual-preferences :as preferences]))

;; Frozen episode shape: F revision 2, task T, author A and owner O.
;; SHA-256 of the exact UTF-8 view "abc" is a known independent test vector.
(def binding-fixture
  {:fixture? true :instance-id "caption-2" :task "T" :feedback "F" :revision 2
   :membership {:status :established :revision 1 :establisher "fixture-reviewer"
                :recipients [{:id "A" :role "author" :reason "authored T"
                              :evidence "fixture:task-author"}
                             {:id "O" :role "owner" :reason "owns T"
                              :evidence "fixture:task-owner"}]}
   :applicability {:status :established :scope "T" :evidence "fixture:team-task"}
   :authority {:warrant "fixture:adoption" :actor "fixture-reviewer"
               :instance-id "caption-2"}
   :payload {:finding "caption needs review" :reason "feedback F"
             :response-route "fixture:reply" :view "abc"
             :digest "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"}
   :receipt-standard :authorized-inbox
   :inbox-adapter {:status :verifiable :evidence "fixture:inbox-adapter"}
   :claims-timeliness? true :deadline 10})

(deftest frozen-obligations
  (let [result (preferences/derive-binding binding-fixture)]
    (is (= :derived (:status result)))
    (is (= binding-fixture (:binding result)))
    (is (= {:required 2 :received 0} (:coverage result)))
    (is (= #{"A" "O"} (set (keys (:obligations result)))))
    (doseq [obligation (vals (:obligations result))]
      (is (= {:state :pending :revision 2
              :digest (get-in binding-fixture [:payload :digest])
              :receipt-standard :authorized-inbox} obligation)))
    (is (false? (:delivery-complete result)))
    (is (every? #{:unobserved}
                ((juxt :consideration :revision-acceptance :subsequent-use) result)))
    (is (= result (preferences/derive-binding binding-fixture)))))

(deftest missing-and-unknown-prerequisites-refuse
  (doseq [path [[:membership] [:authority] [:applicability]
                [:payload :finding] [:payload :reason] [:payload :response-route]
                [:payload :view] [:payload :digest] [:deadline]
                [:membership :establisher] [:membership :revision]
                [:membership :recipients 0 :reason]
                [:membership :recipients 1 :evidence]]]
    (testing (str path)
      (let [result (preferences/derive-binding (assoc-in binding-fixture path nil))]
        (is (= :refused (:status result)))
        (is (= :missing-prerequisite (:reason result))))))
  (doseq [path [[:membership :status] [:applicability :status]]]
    (is (= :invalid-prerequisite
           (:reason (preferences/derive-binding
                     (assoc-in binding-fixture path :unknown)))))))

(deftest binding-integrity
  (doseq [[path value reason]
          [[[:fixture?] false :invalid-prerequisite]
           [[:revision] "2" :invalid-prerequisite]
           [[:membership :recipients] {} :invalid-prerequisite]
           [[:membership :recipients 1 :id] "A" :duplicate-recipient]
           [[:authority :instance-id] "other" :authority-mismatch]
           [[:applicability :scope] "other" :scope-mismatch]
           [[:receipt-standard] :transport-acceptance-only :invalid-prerequisite]
           [[:inbox-adapter :status] :missing :adapter-gap]
           [[:payload :view] "changed" :digest-mismatch]
           [[:payload :digest] "wrong" :digest-mismatch]
           [[:deadline] -1 :invalid-prerequisite]]]
    (let [result (preferences/derive-binding (assoc-in binding-fixture path value))]
      (is (= :refused (:status result)))
      (is (= reason (:reason result)))
      (is (nil? (:obligations result)))))
  (doseq [invalid [nil [] "binding" 12]]
    (is (= :invalid-binding (:reason (preferences/derive-binding invalid))))))

(deftest scope-and-vacuity
  (let [solo (assoc-in binding-fixture [:membership :recipients]
                       [(first (get-in binding-fixture [:membership :recipients]))])
        empty-roster (assoc-in binding-fixture [:membership :recipients] [])
        result (preferences/derive-binding empty-roster)]
    (is (= #{"A"} (set (keys (:obligations (preferences/derive-binding solo))))))
    (is (= {:required 0 :received 0} (:coverage result)))
    (is (true? (:vacuous? result)))
    (is (= [] (:delivery-evidence result)))
    (is (false? (:delivery-complete result))))
  (is (= :not-applicable
         (:status (preferences/derive-binding
                   (assoc-in binding-fixture [:applicability :status] :not-applicable)))))
  (is (= :derived
         (:status (preferences/derive-binding
                   (dissoc (assoc binding-fixture :claims-timeliness? false) :deadline))))))

(deftest derivation-only-api
  ;; Pure derivation only: binding derivation plus pure event derivation.
  ;; No mark-done, mutation, or adoption operation is exposed.
  (is (= #{'derive-binding 'apply-event 'replay-episode}
         (set (keys (ns-publics 'futon2.aif.contextual-preferences))))))

;; --- Frozen fixture episode replay (caption-review feedback, events 1-11) ---

(def digest-d (get-in binding-fixture [:payload :digest]))

(defn- episode []
  (let [opened (preferences/apply-event (preferences/derive-binding binding-fixture)
                                        {:event-id "e1" :actor "fixture-reviewer"
                                         :type :feedback-created})]
    opened))

(deftest episode-transport-then-receipt
  (let [s0 (episode)
        s1 (preferences/apply-event (:state s0)
                                    {:event-id "e2" :actor "fixture-transport"
                                     :type :delivery-attempted :recipient "A"})
        s2 (preferences/apply-event (:state s1)
                                    {:event-id "e3" :actor "A" :type :inbox-receipt
                                     :recipient "A" :digest digest-d :revision 2})]
    (is (= :applied (:status s1)))
    ;; 2. Transport acceptance is not inbox receipt.
    (is (= {:required 2 :received 0} (get-in s1 [:result :coverage])))
    (is (= :transport (-> s1 :state :delivery-evidence last :acceptance)))
    ;; 3. Authorized inbox receipt for A: coverage 1/2.
    (is (= :applied (:status s2)))
    (is (= {:required 2 :received 1} (get-in s2 [:result :coverage])))
    (is (= :receipted (get-in s2 [:state :obligations "A" :state])))
    (is (= :pending (get-in s2 [:state :obligations "O" :state])))
    (is (false? (:delivery-complete (:state s2))))
    ;; A's receipt never discharges O's obligation.
    (is (= 1 (count (filter #(= :receipted (:state (val %)))
                            (:obligations (:state s2))))))
    ;; 4. Exact duplicate replay is an idempotent no-op.
    (let [dup (preferences/apply-event (:state s2)
                                       {:event-id "e3" :actor "A" :type :inbox-receipt
                                        :recipient "A" :digest digest-d :revision 2})]
      (is (= :applied (:status dup)))
      (is (true? (:replayed? dup)))
      (is (= {:required 2 :received 1} (get-in dup [:result :coverage]))))
    ;; 4b. Conflicting reuse of the event id is a typed refusal.
    (let [conflict (preferences/apply-event (:state s2)
                                            {:event-id "e3" :actor "A"
                                             :type :inbox-receipt
                                             :recipient "A" :digest digest-d
                                             :revision 2 :extra "conflict"})]
      (is (= :refused (:status conflict)))
      (is (= :conflicting-event-reuse (:reason conflict))))
    ;; 5. Wrong digest refuses; coverage unchanged.
    (let [wrong (preferences/apply-event (:state s2)
                                         {:event-id "e5" :actor "A"
                                          :type :inbox-receipt
                                          :recipient "A" :digest
                                          "deadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeefdeadbeef0"
                                          :revision 2})]
      (is (= :refused (:status wrong)))
      (is (= :digest-mismatch (:reason wrong))))
    ;; 6. Wrong revision refuses.
    (let [rev1 (preferences/apply-event (:state s2)
                                        {:event-id "e6" :actor "A"
                                         :type :inbox-receipt
                                         :recipient "A" :digest digest-d
                                         :revision 1})]
      (is (= :refused (:status rev1)))
      (is (= :revision-mismatch (:reason rev1))))))

(deftest episode-registry-deadline-dispute-completion
  (let [s0 (episode)
        s1 (preferences/apply-event (:state s0)
                                    {:event-id "e2" :actor "fixture-transport"
                                     :type :delivery-attempted :recipient "A"})
        s2 (preferences/apply-event (:state s1)
                                    {:event-id "e3" :actor "A" :type :inbox-receipt
                                     :recipient "A" :digest digest-d :revision 2})
        ;; 7. O leaves the registry: denominator stays 2, obligation outstanding.
        s3 (preferences/apply-event (:state s2)
                                    {:event-id "e7" :actor "registry"
                                     :type :registry-exit :recipient "O"})]
    (is (= :applied (:status s3)))
    (is (= {:required 2 :received 1} (get-in s3 [:result :coverage])))
    (is (= :pending (get-in s3 [:state :obligations "O" :state])))
    (is (some #(= :absent (:registry-presence %)) (:route-evidence (:state s3))))
    ;; 8. Deadline reached: O overdue, never auto-satisfied.
    (let [s4 (preferences/apply-event (:state s3)
                                      {:event-id "e8" :actor "clock"
                                       :type :deadline-reached :tick 10})]
      (is (= :applied (:status s4)))
      (is (= ["O"] (get-in s4 [:result :overdue])))
      (is (= :overdue (get-in s4 [:state :obligations "O" :state])))
      (is (= :receipted (get-in s4 [:state :obligations "A" :state])))
      (is (false? (:delivery-complete (:state s4))))
      ;; 9. A disputes: still receipted; a separate dispute opens.
      (let [s5 (preferences/apply-event (:state s4)
                                        {:event-id "e9" :actor "A"
                                         :type :disputed :recipient "A"
                                         :claim "caption was correct"})]
        (is (= :applied (:status s5)))
        (is (= :receipted (get-in s5 [:state :obligations "A" :state])))
        (is (= "caption was correct"
               (get-in s5 [:state :disputes "A" :claim])))
        (is (= {:required 2 :received 1} (get-in s5 [:result :coverage])))
        ;; 10. O's correct receipt completes delivery 2/2; downstream outcomes
        ;; remain unobserved (delivery, consideration, acceptance, use are
        ;; separate).
        (let [s6 (preferences/apply-event (:state s5)
                                          {:event-id "e10" :actor "O"
                                           :type :inbox-receipt
                                           :recipient "O" :digest digest-d
                                           :revision 2})]
          (is (= :applied (:status s6)))
          (is (= {:required 2 :received 2} (get-in s6 [:result :coverage])))
          (is (true? (:delivery-complete (:state s6))))
          (is (every? #{:unobserved}
                      ((juxt :consideration :revision-acceptance :subsequent-use)
                       (:state s6))))
          ;; 11. Unauthorized amendment refuses; obligations intact.
          (let [refused (preferences/apply-event (:state s6)
                                                 {:event-id "e11" :actor "A"
                                                  :type :amendment
                                                  :revision 3})]
            (is (= :refused (:status refused)))
            (is (= :unauthorized-actor (:reason refused)))
            (is (= {:required 2 :received 2}
                   (get-in s6 [:state :coverage])))
            (is (true? (:delivery-complete (:state s6))))
            ;; 11b. Even the authorized actor finds no amendment pathway here.
            (let [auth (preferences/apply-event (:state s6)
                                                {:event-id "e11b"
                                                 :actor "fixture-reviewer"
                                                 :type :amendment :revision 3})]
              (is (= :refused (:status auth)))
              (is (= :amendment-unsupported (:reason auth))))))))))

(deftest episode-typed-refusals
  (let [s0 (episode)]
    ;; Wrong recipient.
    (doseq [ev [{:event-id "x1" :actor "X" :type :inbox-receipt
                 :recipient "X" :digest digest-d :revision 2}
                {:event-id "x2" :actor "X" :type :delivery-attempted
                 :recipient "X"}
                {:event-id "x3" :actor "X" :type :disputed :recipient "X"
                 :claim "c"}]]
      (let [r (preferences/apply-event (:state s0) ev)]
        (is (= :refused (:status r)))
        (is (= :unknown-recipient (:reason r)))))
    ;; Missing prerequisite fields.
    (doseq [ev [{:actor "A" :type :inbox-receipt :recipient "A"}
                {:event-id "x4" :type :inbox-receipt :recipient "A"}
                {:event-id "x5" :actor "A" :type :inbox-receipt
                 :recipient "A" :digest digest-d}]]
      (let [r (preferences/apply-event (:state s0) ev)]
        (is (= :refused (:status r)))
        (is (= :missing-prerequisite (:reason r)))))
    ;; Unauthorized feedback-created.
    (let [fresh (preferences/derive-binding binding-fixture)
          r (preferences/apply-event fresh
                                     {:event-id "y1" :actor "A"
                                      :type :feedback-created})]
      (is (= :refused (:status r)))
      (is (= :unauthorized-actor (:reason r))))
    ;; Vacuous episode earns no delivery evidence.
    (let [empty-roster (assoc-in binding-fixture [:membership :recipients] [])
          vac (preferences/derive-binding empty-roster)
          r (preferences/apply-event vac
                                     {:event-id "z1" :actor "fixture-reviewer"
                                      :type :feedback-created})]
      (is (= :refused (:status r)))
      (is (= :vacuous-episode (:reason r))))
    ;; Not-applicable binding refuses events.
    (let [na (preferences/derive-binding
              (assoc-in binding-fixture [:applicability :status] :not-applicable))
          r (preferences/apply-event na
                                     {:event-id "z2" :actor "fixture-reviewer"
                                      :type :feedback-created})]
      (is (= :refused (:status r)))
      (is (= :episode-not-applicable (:reason r))))
    ;; Second distinct feedback-created refuses.
    (let [r (preferences/apply-event (:state s0)
                                     {:event-id "z3" :actor "fixture-reviewer"
                                      :type :feedback-created})]
      (is (= :refused (:status r)))
      (is (= :episode-already-opened (:reason r))))))

(deftest episode-batch-replay
  (let [digest-d (get-in binding-fixture [:payload :digest])
        complete (preferences/replay-episode
                  (preferences/derive-binding binding-fixture)
                  [{:event-id "e1" :actor "fixture-reviewer" :type :feedback-created}
                   {:event-id "e2" :actor "fixture-transport" :type :delivery-attempted
                    :recipient "A"}
                   {:event-id "e3" :actor "A" :type :inbox-receipt
                    :recipient "A" :digest digest-d :revision 2}
                   {:event-id "e7" :actor "registry" :type :registry-exit
                    :recipient "O"}
                   {:event-id "e8" :actor "clock" :type :deadline-reached :tick 10}
                   {:event-id "e9" :actor "A" :type :disputed :recipient "A"
                    :claim "caption was correct"}
                   {:event-id "e10" :actor "O" :type :inbox-receipt
                    :recipient "O" :digest digest-d :revision 2}])]
    (is (= :complete (:status complete)))
    (is (= ["e1" "e2" "e3" "e7" "e8" "e9" "e10"] (:applied complete)))
    (is (= {:required 2 :received 2} (get-in complete [:state :coverage])))
    (is (true? (get-in complete [:state :delivery-complete])))
    (is (= :overdue-free
           (if (some #(= :overdue (:state (val %))) (:obligations (:state complete)))
             :still-overdue :overdue-free)))
    (is (= "caption was correct" (get-in complete [:state :disputes "A" :claim])))
    ;; Batch replay stops at the first typed refusal with prior applied ids.
    (let [refused (preferences/replay-episode
                   (preferences/derive-binding binding-fixture)
                   [{:event-id "f1" :actor "fixture-reviewer" :type :feedback-created}
                    {:event-id "f2" :actor "A" :type :inbox-receipt
                     :recipient "A" :digest "wrong" :revision 2}])]
      (is (= :refused (:status refused)))
      (is (= ["f1"] (:applied refused)))
      (is (= :digest-mismatch (get-in refused [:refusal :reason]))))))

