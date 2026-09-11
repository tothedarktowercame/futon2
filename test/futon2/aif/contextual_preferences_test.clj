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
  (is (= #{'derive-binding}
         (set (keys (ns-publics 'futon2.aif.contextual-preferences))))))
