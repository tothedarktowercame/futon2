(ns review-probe
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.test :refer [deftest is use-fixtures run-tests]]
            [futon2.aif.receipt-construction :as c]
            [futon2.aif.receipt-construction-test :as fixture]
            [futon2.aif.find-receipt-test :as find-fixture]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.evidence-manifest :as manifest]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.interpretation-job-test :as jobs]
            [futon2.aif.full-loop-runner-test :as runner-fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic])
  (:import [java.nio.file Files] [java.nio.file.attribute FileAttribute]
           [java.time Instant] [java.util UUID]))
(use-fixtures :once hermetic/with-hermetic-stores runner-fixture/with-hermetic-traces)
(def observations (atom {}))
(defn read-file [f] (edn/read-string (slurp f)))
(defn write-file [f x] (spit f (pr-str x)))
(defn record! [k x] (swap! observations assoc k x) x)
(defn current-id []
  {:occurrence (retention/mint-occurrence
                 {:run-id (str (UUID/randomUUID)) :cohort-id ":fixture" :attempt-id "attempt-002"
                  :selected-action {:type :advance-mission :target "M-history"}
                  :now #(Instant/parse "2026-09-15T12:00:00Z") :uuid-fn #(UUID/randomUUID)})})
(defn attempt [f]
  (try {:accepted (f)}
       (catch Exception e {:exception (.getName (class e)) :data (ex-data e) :message (.getMessage e)})))
(defn with-history [f]
  (let [root (.toFile (Files/createTempDirectory "wm-review-history-" (make-array FileAttribute 0)))]
    (try
      (f root (fixture/history-fixture! root :review "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z"))
      (finally (doseq [x (reverse (file-seq root))] (Files/delete (.toPath x)))))))
(deftest established-history-damage-controls
  ;; Characterization assertions expose observed fail-open behavior; passing these
  ;; tests does NOT mean the contract passes. See independent verdict in RECEIPT.
  (doseq [[label mutate expected]
          [[:valid (fn [_] nil) :carried-from-previous-occurrence]
           [:missing-construction #(Files/delete (.toPath (:construction-file %))) :first-attempt-no-admissions]
           [:missing-close #(Files/delete (.toPath (:close-file %))) :first-attempt-no-admissions]
           [:missing-cascade #(write-file (:construction-file %)
                               (update-in (read-file (:construction-file %)) [:payload :judgment] dissoc :cascade))
            :first-attempt-no-admissions]
           [:removed-receipted-carrier #(write-file (:construction-file %)
                                         (update-in (read-file (:construction-file %)) [:payload :judgment]
                                                    dissoc :receipted-construction))
            :legacy-predecessor-no-ruled-admissions]]]
    (with-history
      (fn [root h]
        (is (= :carried-from-previous-occurrence (:admission-reason (c/previous! (current-id) [root]))))
        (mutate h)
        (let [result (record! label (attempt #(c/previous! (current-id) [root])))]
          (is (= expected (get-in result [:accepted :admission-reason])) (str label))))))
  (doseq [[label mutate]
          [[:malformed-construction #(spit (:construction-file %) "{:broken")]
           [:changed-acting-order-unsealed
            #(write-file (:construction-file %)
                         (assoc-in (read-file (:construction-file %))
                                   [:payload :judgment :receipted-construction :cascade-diff :acting-order-after] [:a]))]]]
    (with-history
      (fn [root h]
        (mutate h)
        (let [result (record! label (attempt #(c/previous! (current-id) [root])))]
          (is (:exception result))
          (when (= label :changed-acting-order-unsealed)
            (is (= :previous-manifest-source-mismatch (get-in result [:data :construction/refusal])))))))))
(defn reseal! [h mutate]
  ;; Models a producer retaining the wrong order, not an undetected byte edit.
  (let [f (:construction-file h)
        doc (mutate (read-file f))
        d (get-in doc [:payload :judgment :receipted-construction :cascade-diff])
        doc (assoc-in doc [:payload :judgment :receipted-construction :cascade-diff-sha256]
                      (evidence/value-digest d))
        _ (write-file f doc)
        closed (read-file (:close-file h))
        old (get-in closed [:payload :close-evidence-manifest])
        entries (mapv #(select-keys % [:evidence/id :source-path :admitted-at]) (:entries old))
        m (manifest/build-manifest {:entries entries :read-bytes #(Files/readAllBytes (.toPath (io/file %)))})]
    (write-file (:close-file h) (assoc-in closed [:payload :close-evidence-manifest] m))))
(deftest occurrence-and-order-controls
  (with-history
    (fn [root h]
      (let [correct (c/previous! (current-id) [root])
            {:keys [record captured]} (find-fixture/sample)
            d (get-in (c/construct record captured find-fixture/root
                                  (assoc correct :admitted {}) nil) [:receipted-construction :cascade-diff])]
        (is (= [:b :a] (:acting-order-before d)))
        (record! :positive-order {:identity (get-in correct [:provenance :identity])
                                 :before (:acting-order-before d) :after (:acting-order-after d)}))
      (let [closed (read-file (:close-file h))]
        (write-file (:close-file h)
                    (assoc-in closed [:payload :close-retention :occurrence :run/id] (str (UUID/randomUUID)))))
      (let [result (record! :wrong-occurrence (attempt #(c/previous! (current-id) [root])))]
        (is (= :previous-occurrence-mismatch (get-in result [:data :construction/refusal]))))))
  (with-history
    (fn [root h]
      (reseal! h #(assoc-in % [:payload :judgment :receipted-construction :cascade-diff :acting-order-after]
                            [:a :b]))
      (let [prev (c/previous! (current-id) [root])
            {:keys [record captured]} (find-fixture/sample)
            d (get-in (c/construct record captured find-fixture/root (assoc prev :admitted {}) nil)
                      [:receipted-construction :cascade-diff])]
        (is (= [:a :b] (:acting-order-before d)))
        (record! :wrong-order-resealed {:accepted-before (:acting-order-before d)
                                       :prior-precedence (get-in prev [:cascade :precedence])
                                       :prior-edges (get-in prev [:cascade :edges])
                                       :meaning :producer-supplied-order-not-validated-as-actual-enactment})))))
(deftest interpretation-failure-path
  (doseq [scenario [:invalid :no-relevant :nothing-fires :source-changed :genesis]]
    (let [{:keys [result calls constructors failures organised]} (jobs/run-case :receipt scenario)
          observation {:outcome (:outcome result) :data (:data result)
                       :failure-kind (get-in failures [0 :failure :kind])
                       :constructors constructors :calls calls :organised organised
                       :construction-checkpoint (get-in result [:checkpoints :construction])}]
      (record! scenario observation)
      (is (= :environmental-hold (get-in result [:data :repair-obligation :repair/class])))
      (is (not= :success (:outcome result)))
      (is (= :not-reached-construction (get-in observation [:construction-checkpoint :sorry :kind])))
      (is (zero? constructors))
      (is (every? #{"interpreter"} calls))
      (is (= (:failure-kind observation) (get-in result [:data :failure-kind]))))))
(defn -main [out]
  (let [result (run-tests 'review-probe)]
    (with-open [w (io/writer out)] (binding [*out* w] (pp/pprint {:tests result :observations @observations})))
    (shutdown-agents)
    (System/exit (if (zero? (+ (:fail result) (:error result))) 0 1))))
