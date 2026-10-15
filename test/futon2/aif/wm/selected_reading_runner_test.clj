(ns futon2.aif.wm.selected-reading-runner-test
  "Q5 step 3a at the production runner seam: one G selection, then a reading
  asked for only the selected policy's owed occurrences, validated, and the
  same immutable policy made eligible for enactment. Enactment itself stops
  at a typed refusal: reading-policy execution semantics are not ruled."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.policy :as policy]
            [futon2.aif.reading-interpretation-debt :as debt]
            [futon2.aif.selected-reading :as selected-reading]
            [futon2.aif.wm.reading-family-decision-test :as rfd]
            [futon2.test-support.runner-fixture :as fixture]
            [wm-run-facts :as facts])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each
  (fn [run]
    (binding [runner/*wm-status-reporting?* false]
      (with-redefs [ledger/default-root
                    (.getPath (.toFile (Files/createTempDirectory
                                        "selected-reading-ledger-"
                                        (make-array FileAttribute 0))))]
        (run)))))

(defn- temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- library
  "A library root holding the three fixture patterns' files."
  []
  (let [root (temp-dir "selected-reading-library-")]
    (doseq [p ["p/a" "p/b" "p/c"]]
      (let [f (io/file root (str p ".flexiarg"))]
        (.mkdirs (.getParentFile f))
        (spit f (str "@flexiarg " p "\n! conclusion: fixture pattern " p "\n"))))
    root))

(defn- sha [root pattern]
  (load-identity/sha256 (Files/readAllBytes (.toPath (io/file root (str pattern ".flexiarg"))))))

(defn- selected-decision
  "A real production decision: the reading names p/a and p/c, so the chosen
  retraction policy owes a reading of its p/b occurrence."
  [selector-calls]
  (let [fx (@#'rfd/fixture ["p/a" "p/c"])
        select policy/select-action-cascades]
    (with-redefs [policy/select-action-cascades
                  (fn [& args] (swap! selector-calls inc) (apply select args))]
      (:decision (#'rfd/decide fx [(get-in fx [:rows :current])] {"M-current" 1})))))

(defn- answer [request lib verdict]
  {:request-id (:request-id request)
   :by "stub-reader"
   :verdicts (mapv (fn [{:keys [occurrence-id pattern]}]
                     {:occurrence-id occurrence-id :pattern pattern
                      :verdict verdict :reason (str pattern " " verdict " for this excerpt")
                      :source_sha256 (sha lib pattern)})
                   (:owed request))})

(defn- answer-with-pins
  "An accepted answer citing the pins the request issued."
  [request]
  {:request-id (:request-id request) :by "stub-reader"
   :verdicts (mapv (fn [{:keys [occurrence-id pattern]}]
                     {:occurrence-id occurrence-id :pattern pattern :verdict "accepted"
                      :reason "fits" :source_sha256 (get-in request [:library-pins pattern])})
                   (:owed request))})

(defn- judgement-for [decision]
  {:decision decision :belief {} :belief-pre {} :observation {} :free-energy {}
   :prediction-errors {} :precision-state {} :micro-step-trace [] :mode :maintain})

(defn- run-click [decision lib ask-fn selector-calls]
  (let [select policy/select-action-cascades
        result (with-redefs [policy/select-action-cascades
                             (fn [& args] (swap! selector-calls inc) (apply select args))]
                 (runner/run-opportunity!
                  (merge (fixture/isolated-runner-opts)
                         {:judge-fn (fn [_] {:judgement (judgement-for decision)})
                          :selected-reading-ask-fn ask-fn
                          :pattern-library-root (str lib)
                          :interpretation-ask-fn (fn [& _] (throw (ex-info "old ask reached" {})))
                          :repair-system-record-fn (fn [m] {:repair/id "repair-test"
                                                            :repair/class (:repair-class m)})
                          :dispatch-fn (fn [& _] (throw (ex-info "Unexpected dispatch" {})))})))]
    {:result result :record (edn/read-string (slurp (:run-record result)))}))

(defn- without-discharge [decision]
  (update decision :selection-certificate dissoc :interpretation-discharge))

(def snap {:open-missions #{"M-current"} :open-excursions #{} :open-tickets #{}
           :patterns #{"p/a" "p/b" "p/c"} :seats #{"zai-5"} :library-pattern-count 3})

(deftest selected-only-reading-discharges-the-same-policy
  (let [selector-calls (atom 0)
        lib (library)
        decision (selected-decision selector-calls)
        requests (atom [])
        {:keys [record result]} (run-click decision lib
                                           (fn [request] (swap! requests conj request)
                                             (answer request lib "accepted"))
                                           selector-calls)
        ;; enactment stops before a judgment cell: the decision the runner
        ;; carried rides on the selection checkpoint's :no-selection cell
        cell (get-in result [:checkpoints :selection :sorry])
        recorded (:decision cell)
        discharge (get-in recorded [:selection-certificate :interpretation-discharge])
        request (first @requests)
        chosen-id (get-in decision [:action :id])
        ;; a certificate row's :id is the candidate's action map; the
        ;; policy id is that action's :id
        losing-ids (vec (remove #{chosen-id}
                                (map (comp :id :id)
                                     (get-in decision [:selection-certificate :candidates]))))]
    (testing "one G selection in all, and the runner asked once"
      (is (= 1 @selector-calls))
      (is (= 1 (count @requests))))
    (testing "the request names only the selected policy's owed occurrences"
      (is (= (:id (:action decision)) (:policy-id request)))
      (is (= (get-in decision [:action :interpretation-debt :owed]) (:owed request)))
      (is (= ["p/b"] (mapv :pattern (:owed request))))
      (is (empty? (select-keys request [:retrieval :want :wants :candidates :policies])))
      (is (seq losing-ids))
      (is (every? #(and (string? %) (str/starts-with? % "M-current/policy-")) losing-ids))
      (is (= chosen-id (:policy-id request)))
      (is (not-any? #{(:policy-id request)} losing-ids))
      (let [in-scope? (fn [id] (and (= chosen-id id) (not-any? #{id} losing-ids)))]
        (is (in-scope? (:policy-id request)))
        (is (not (in-scope? (first losing-ids))) "a losing id fails the scope predicate")))
    (testing "events are strictly ordered: selection, request, validation, eligibility"
      (let [events (:events discharge)]
        (is (= [:selection-completed :interpretation-requested :interpretation-received
                :interpretation-validated :enactment-eligibility-checked]
               (mapv :event events)))
        (is (every? (fn [[a b]] (.isBefore (java.time.Instant/parse (:at a))
                                           (java.time.Instant/parse (:at b))))
                    (partition 2 1 events)))
        (is (= (:at (first events)) (get-in record [:world-at-selection :selection-ended-at])))
        (is (= (:at (second events)) (get-in record [:world-at-selection :interpretation-issued-at])))
        (is (= "selectionBeforeInterpretation"
               (get-in (facts/facts-for-record record "r" snap nil nil)
                       [:facts "interpretationOrder"])))))
    (testing "the policy and its selection certificate are unchanged but for the discharge"
      (is (= (without-discharge decision) (without-discharge recorded))))
    (testing "the discharged policy is eligible, and enactment stops at its typed refusal"
      (is (= :validated (get-in discharge [:validation :status])))
      (is (= :discharged (:status (runner/reading-interpretation-status recorded))))
      (is (= (:action decision) (:action (#'runner/selected-entry {:decision recorded}))))
      (is (= :reading-policy-execution-semantics-owed
             (get-in cell [:downstream-refusal :kind]))))))

(deftest invalid-reading-leaves-the-debt-open-through-the-runner
  (let [selector-calls (atom 0)
        lib (library)
        decision (selected-decision selector-calls)
        {:keys [result]} (run-click decision lib
                                    (fn [request]
                                      (assoc-in (answer request lib "accepted")
                                                [:verdicts 0 :source_sha256]
                                                (apply str (repeat 64 "0"))))
                                    selector-calls)
        recorded (get-in result [:checkpoints :selection :sorry :decision])]
    (is (= 1 @selector-calls))
    (is (= :interpretation-validation-refused
           (get-in recorded [:selection-certificate :interpretation-discharge :validation :status])))
    (is (= :library-pin-mismatch
           (get-in recorded [:selection-certificate :interpretation-discharge
                             :validation :reasons 0 :reason])))
    (is (= :interpretation-owed-after-selection
           (:status (runner/reading-interpretation-status recorded))))
    (is (nil? (#'runner/selected-entry {:decision recorded})))
    (is (= (without-discharge decision) (without-discharge recorded)))))

(defn- phase
  "The runner's own selected-reading phase on DECISION with ASK."
  [decision lib ask]
  (let [clock (#'runner/monotonic-clock)
        at (clock)]
    (:decision (:judgement (runner/selected-reading-phase
                            {:selected-reading-ask-fn ask :pattern-library-root (str lib)}
                            {:decision decision} clock at)))))

(defn- reseal [r]
  (let [r (dissoc r :digest)]
    (assoc r :digest (debt/seal debt/receipt-schema r))))

(deftest verdicts-and-adversarial-responses
  (let [lib (library)
        decision (selected-decision (atom 0))
        status #(:status (runner/reading-interpretation-status %))
        validation #(get-in % [:selection-certificate :interpretation-discharge :validation])]
    (testing "an accepted and a rejected verdict both discharge the coverage debt"
      (is (= :discharged (status (phase decision lib #(answer % lib "accepted")))))
      (is (= :discharged (status (phase decision lib #(answer % lib "rejected"))))))
    (doseq [[label respond reason]
            [[:empty (fn [r] (assoc (answer r lib "accepted") :verdicts [])) :no-verdicts]
             [:foreign-request (fn [r] (assoc (answer r lib "accepted") :request-id "another"))
              :response-for-another-request]
             [:stale-debt (fn [r] (answer (assoc r :request-id "old-request") lib "accepted"))
              :response-for-another-request]
             [:wrong-occurrence (fn [r] (assoc-in (answer r lib "accepted")
                                                  [:verdicts 0 :occurrence-id] "elsewhere"))
              :verdict-for-unowed-occurrence]
             [:another-pattern (fn [r] (update (answer r lib "accepted") :verdicts conj
                                               {:occurrence-id "x" :pattern "p/a" :verdict "accepted"
                                                :reason "not asked" :source_sha256 (sha lib "p/a")}))
              :verdict-for-unowed-occurrence]
             [:wrong-library-pin (fn [r] (assoc-in (answer r lib "accepted")
                                                   [:verdicts 0 :source_sha256] (sha lib "p/a")))
              :library-pin-mismatch]
             [:malformed-verdict (fn [r] (assoc-in (answer r lib "accepted") [:verdicts 0 :verdict] "maybe"))
              :verdict-not-accepted-or-rejected]
             [:no-reason (fn [r] (assoc-in (answer r lib "accepted") [:verdicts 0 :reason] " "))
              :verdict-without-reason]
             [:malformed-response (fn [_] "not a map") :malformed-response]
             [:ask-throws (fn [_] (throw (ex-info "seat down" {}))) :selected-reading-ask-threw]]]
      (testing (name label)
        (let [after (phase decision lib respond)]
          (is (= :interpretation-validation-refused (:status (validation after))))
          (is (= reason (get-in (validation after) [:reasons 0 :reason])))
          (is (= :interpretation-owed-after-selection (status after)))
          (is (empty? (get-in after [:selection-certificate :interpretation-discharge :receipts])))
          (is (nil? (#'runner/selected-entry {:decision after})))
          (is (= (without-discharge decision) (without-discharge after))))))
    (testing "a receipt cannot stand for a different occurrence of the same pattern"
      (let [after (phase decision lib #(answer % lib "accepted"))
            moved (update-in after [:selection-certificate :interpretation-discharge :receipts 0]
                             #(reseal (assoc % :occurrence-id "p/b#other")))]
        (is (= :discharged (status after)))
        (is (= :receipt-for-unowed-occurrence
               (:reason (runner/reading-interpretation-status moved))))))
    (testing "nothing owed, nothing asked"
      (let [asked (atom 0)
            zero (:decision (#'rfd/decide (@#'rfd/fixture) [(get-in (@#'rfd/fixture) [:rows :current])]
                                          {"M-current" 1}))
            after (phase zero lib (fn [r] (swap! asked inc) (answer r lib "accepted")))]
        (is (= 0 @asked))
        (is (= zero after))))
    (testing "the request is for the selected debt only, with the library pinned at issuance"
      (let [request (selected-reading/request decision {:library-root (str lib)})]
        (is (= (get-in decision [:action :interpretation-debt :digest]) (:debt-digest request)))
        (is (= (get-in decision [:action :policy-key]) (:policy-key request)))
        (is (= {"p/b" (sha lib "p/b")} (:library-pins request)))))))

(deftest receipts-stand-only-as-sealed-by-the-validator
  (let [lib (library)
        decision (selected-decision (atom 0))
        after (phase decision lib #(answer % lib "accepted"))
        section [:selection-certificate :interpretation-discharge]
        receipt (get-in after (conj section :receipts 0))
        with-receipt (fn [r] (assoc-in after (conj section :receipts) [r]))
        reason #(:reason (runner/reading-interpretation-status %))]
    (is (= :discharged (:status (runner/reading-interpretation-status after))))
    (testing "a forged minimal receipt"
      (is (= :receipt-unsealed
             (reason (with-receipt (merge (first (get-in decision [:action :interpretation-debt :owed]))
                                          {:policy-id (get-in decision [:action :id])
                                           :debt-digest (get-in decision [:action :interpretation-debt :digest])
                                           :status :validated}))))))
    (testing "a tampered receipt"
      (is (= :receipt-seal-broken (reason (with-receipt (assoc receipt :verdict :rejected))))))
    (testing "tampered and resealed against another pin, request, reading or source"
      (is (= :receipt-library-pin-mismatch
             (reason (with-receipt (reseal (assoc receipt :source-sha256 (sha lib "p/a")
                                                  :library-pin (sha lib "p/a")))))))
      (is (= :receipt-for-another-request
             (reason (with-receipt (reseal (assoc-in receipt [:reading :request-id]
                                                     (apply str (repeat 64 "c"))))))))
      (is (= :receipt-source-identity-malformed
             (reason (with-receipt (reseal (assoc-in receipt [:reading :request-id] "another"))))))
      (is (= :receipt-source-identity-malformed
             (reason (with-receipt (reseal (update receipt :reading dissoc :response-digest))))))
      (is (= :receipt-source-identity-malformed
             (reason (with-receipt (reseal (assoc-in receipt [:reading :response-digest] 7))))))
      (is (= :receipt-reading-mismatch
             (reason (with-receipt (reseal (assoc-in receipt [:reading :analysis-digest] "other"))))))
      (is (= :receipt-without-source-identity
             (reason (with-receipt (reseal (assoc-in receipt [:reading :by] " "))))))
      (is (= :receipt-policy-key-mismatch
             (reason (with-receipt (reseal (assoc receipt :policy-key-digest "other"))))))
      (is (= :receipt-not-validated
             (reason (with-receipt (reseal (assoc receipt :validation {:validator :other :result :valid}))))))
      (is (= :receipt-not-validated
             (reason (with-receipt (reseal (dissoc receipt :validation)))))))
    (testing "the issued request cannot be edited under its id"
      (is (= :receipt-request-missing-or-broken
             (reason (assoc-in after (conj section :request :library-pins "p/b") (sha lib "p/a")))))
      (is (= :receipt-request-missing-or-broken
             (reason (update-in after section dissoc :request)))))))

(deftest library-pinned-at-issuance-and-source-named
  (let [lib (library)
        decision (selected-decision (atom 0))
        validation #(get-in % [:selection-certificate :interpretation-discharge :validation])
        status #(:status (runner/reading-interpretation-status %))]
    (testing "the library changing between request and response refuses"
      (let [after (phase decision lib
                         (fn [request]
                           (spit (io/file lib "p/b.flexiarg") "@flexiarg p/b\n! edited after the request\n")
                           (assoc-in (answer request lib "accepted")
                                     [:verdicts 0 :source_sha256] (sha lib "p/b"))))]
        (is (= :library-source-changed (get-in (validation after) [:reasons 0 :reason])))
        (is (= :interpretation-owed-after-selection (status after)))))
    (testing "a library file absent at issuance refuses, and the debt is never asked about"
      (let [lib (library)
            _ (io/delete-file (io/file lib "p/b.flexiarg"))
            asked (atom 0)
            after (phase decision lib (fn [request] (swap! asked inc) (answer-with-pins request)))]
        (is (= 0 @asked))
        (is (= :selected-reading-source-unpinned (get-in (validation after) [:reasons 0 :reason])))
        (is (= (get-in decision [:action :interpretation-debt :owed])
               (get-in (validation after) [:reasons 0 :missing])))
        (is (= :interpretation-owed-after-selection (status after)))
        (testing "no contact is recorded: no request event, no issued instant"
          (let [events (get-in after [:selection-certificate :interpretation-discharge :events])]
            (is (= [:selection-completed :interpretation-not-requested :enactment-eligibility-checked]
                   (mapv :event events)))
            (is (nil? (get-in after [:selection-certificate :interpretation-discharge :request])))
            (let [clock (#'runner/monotonic-clock)
                  record (:record (runner/selected-reading-phase
                                   {:selected-reading-ask-fn (fn [_] (swap! asked inc))
                                    :pattern-library-root (str lib)}
                                   {:decision decision} clock (clock)))]
              (is (= :absent (:status record)))
              (is (not (contains? record :issued-at)))
              (is (= 0 @asked)))))
        (testing "no request with an unpinned library source is ever issued"
          (let [issued (selected-reading/request decision {:library-root (str lib)})]
            (is (= :refused (:status issued)))
            (is (= :selected-reading-source-unpinned (:kind issued)))
            (is (not (contains? issued :library-pins)))
            (is (not (contains? issued :request-id)))))))
    (testing "an answer without a named source refuses"
      (let [after (phase decision lib #(assoc (answer % lib "accepted") :by " "))]
        (is (= :missing-source-identity (get-in (validation after) [:reasons 0 :reason])))
        (is (= :interpretation-owed-after-selection (status after)))))))
