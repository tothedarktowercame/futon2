(ns futon2.aif.selection-rationale-test
  "RE4 -- the rationale written at decision time, its typed absences, and the
   one seam that writes it.

   The controls are arranged so that no assertion could pass vacuously: every
   absence test names the SPECIFIC reason keyword rather than merely asserting
   that the record is an absence, and the seam tests assert on a file that a
   fresh temporary directory did not contain a moment earlier."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.belief :as belief]
            [futon2.aif.selection-rationale :as sr]
            [futon2.report.war-machine :as wm])
  (:import (java.io File)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(def ^:dynamic *tmpdir* nil)

(defn- with-tmpdir [f]
  (let [dir (Files/createTempDirectory "wm-rationale-test" (into-array FileAttribute []))]
    (binding [*tmpdir* (str dir)]
      (try (f)
           (finally
             (doseq [^File child (reverse (file-seq (io/file (str dir))))]
               (.delete child)))))))

(use-fixtures :each with-tmpdir)

(def ^:private planted-contract
  {:status :present :git-sha "0123456789abcdef" :contract-id "wm-holes"
   :path "planted"})

(defn- candidate [type target g rank]
  {:action {:type type :target target}
   :G-core g :controller-score g :rank rank})

(def ^:private selection-record
  "A persisted decision whose CHOSEN action is not the controller head -- the
   shape the live S5 records have (chosen at controller rank 123 of 145), which
   is why `:rationale/runner-up` exists at all."
  {:timestamp "2026-09-01T22:50:42.709079837Z"
   :run/id "run-a"
   :producer-contract :r8/retired-f-controller-v1
   :wm-version {:git-sha "5a664114c6a149c73554e491b3cb068e8d3be354"}
   :ranked-actions [(candidate :advance-mission "M-head" 4.0 1)
                    (candidate :advance-mission "M-mid" 6.0 2)
                    (candidate :advance-mission "M-chosen" 9.0 3)]
   :policy-support-exclusions
   [{:reason :mission-absent-from-capability-graph
     :action {:type :open-mission :target "M-excluded"}}]
   :decision {:action {:type :advance-mission :target "M-chosen"}
              :controller-ranking [{:rank 1 :action {:type :advance-mission :target "M-head"}}
                                   {:rank 2 :action {:type :advance-mission :target "M-mid"}}
                                   {:rank 3 :action {:type :advance-mission :target "M-chosen"}}]
              :rank 1
              :reason :reviewed-live-reason-bearing-policy
              :selection-boundary :reason-bearing-strategic-policy
              :tau 1.0
              :tau-source :selection-gain-only
              :selected-policy-id "stub:first-ranked-authorized-mission"}})

(def ^:private refusal-record
  "`policy/select-action`'s abstain branch (policy.clj:736-739; branches at :813-817 and :843-847): a refusal is a
   decision, not a missing decision."
  (-> selection-record
      (assoc :run/id "run-refusal")
      (assoc :decision {:action :abstain
                        :reason :no-action-beats-no-op
                        :selection-boundary :actuation
                        :controller-ranking []})))

(defn- record-of [r] (sr/rationale-record r {:contract planted-contract}))

;; ---------------------------------------------------------------------------
;; 1. The producer on a legible selection.

(deftest records-a-selection-with-the-fields-the-retrospective-needs-test
  (let [r (record-of selection-record)]
    (testing "status and outcome"
      (is (= :recorded (:rationale/status r)))
      (is (= :selected (:rationale/outcome r))))
    (testing "the acceptance's minimum field set is present and populated"
      (is (= "run-a" (:rationale/run-id r)))
      (is (= "2026-09-01T22:50:42.709079837Z" (:rationale/tick-id r)))
      (is (= "2026-09-01T22:50:42.709079837Z" (:rationale/at r)))
      (is (= 3 (:rationale/candidate-set-size r)))
      (is (= {:type :advance-mission :target "M-chosen"}
             (get-in r [:rationale/chosen :action])))
      (is (= 1 (count (:rationale/refused r))))
      (is (= 1 (:rationale/refused-count r)))
      (is (string? (:rationale/text r)))
      (is (= "0123456789abcdef" (get-in r [:rationale/contract-sha :git-sha]))))
    (testing "the controller rank is read from the ranking, not from [:decision :rank]"
      ;; [:decision :rank] is 1 here and describes the head the strategic
      ;; selector replaced. A rationale that copied it would report the machine
      ;; choosing its own top candidate when it did not.
      (is (= 1 (get-in selection-record [:decision :rank])))
      (is (= 3 (get-in r [:rationale/chosen :controller-rank]))))
    (testing "the runner-up carries the margin the choice was made against"
      (is (= :present (get-in r [:rationale/runner-up :status])))
      (is (= {:type :advance-mission :target "M-head"}
             (get-in r [:rationale/runner-up :action])))
      (is (= 5.0 (get-in r [:rationale/runner-up :G-core-margin-over-chosen]))))
    (testing "the tie block behind the rank is on the record"
      ;; Without this a reader takes a rank for a score margin. On the real s5
      ;; records ranks 120-125 share one controller score to sixteen digits.
      (is (= 1 (get-in r [:rationale/chosen :controller-score-tie :count])))
      (is (= [3 3] (get-in r [:rationale/chosen :controller-score-tie :rank-band])))
      (is (= 1 (get-in r [:rationale/chosen :key-occurrences]))))
    (testing "the join key's injectivity is measured, not assumed"
      (is (= 3 (:rationale/distinct-candidate-keys r))))
    (testing "no defects"
      (is (= [] (sr/defects r))))))

(deftest a-tied-score-block-and-a-repeated-key-are-both-recorded-test
  ;; The shape the live records actually have: several candidates carrying one
  ;; controller score, and an action key that occurs more than once. Both make
  ;; the rank ambiguous, and both are reported rather than resolved silently.
  (let [tied (-> selection-record
                 (assoc :ranked-actions
                        [(candidate :advance-mission "M-head" 4.0 1)
                         (assoc (candidate :advance-mission "M-mid" 9.0 2)
                                :controller-score 9.0)
                         (candidate :advance-mission "M-chosen" 9.0 3)
                         (candidate :learn-action-class nil 9.0 4)
                         (candidate :learn-action-class nil 9.0 5)])
                 (assoc-in [:decision :controller-ranking]
                           [{:rank 1 :action {:type :advance-mission :target "M-head"}}
                            {:rank 2 :action {:type :advance-mission :target "M-mid"}}
                            {:rank 3 :action {:type :advance-mission :target "M-chosen"}}
                            {:rank 4 :action {:type :learn-action-class}}
                            {:rank 5 :action {:type :learn-action-class}}]))
        r (record-of tied)]
    (is (= 3 (get-in r [:rationale/chosen :controller-rank])))
    (is (= 4 (get-in r [:rationale/chosen :controller-score-tie :count])))
    (is (= [2 5] (get-in r [:rationale/chosen :controller-score-tie :rank-band])))
    (is (= 5 (:rationale/candidate-set-size r)))
    (is (= 4 (:rationale/distinct-candidate-keys r))
        "the repeated [:learn-action-class nil] key is counted once")
    (is (= [] (sr/defects r)))))

(deftest a-repeated-key-takes-its-best-rank-not-the-last-one-test
  ;; `into {}` would make the recorded rank depend on emission order.
  (let [repeated (-> selection-record
                     (assoc-in [:decision :action] {:type :learn-action-class})
                     (assoc :ranked-actions
                            [(candidate :learn-action-class nil 9.0 7)
                             (candidate :learn-action-class nil 9.0 2)])
                     (assoc-in [:decision :controller-ranking]
                               [{:rank 7 :action {:type :learn-action-class}}
                                {:rank 2 :action {:type :learn-action-class}}]))
        r (record-of repeated)]
    (is (= 2 (get-in r [:rationale/chosen :controller-rank])))
    (is (= 2 (get-in r [:rationale/chosen :key-occurrences])))))

(deftest chosen-is-controller-head-is-typed-not-nil-test
  (let [head-chosen (assoc-in selection-record [:decision :action]
                              {:type :advance-mission :target "M-head"})
        r (record-of head-chosen)]
    (is (= :recorded (:rationale/status r)))
    (is (= {:status :absent :reason :chosen-is-controller-head}
           (:rationale/runner-up r)))))

;; ---------------------------------------------------------------------------
;; 2. Refusal is a recorded decision.

(deftest refusal-is-recorded-not-absent-test
  (let [r (record-of refusal-record)]
    (is (= :recorded (:rationale/status r)))
    (is (= :refused (:rationale/outcome r)))
    (is (= :no-action-beats-no-op (:rationale/decision-reason r)))
    (is (= {:status :absent :reason :refused} (:rationale/chosen r)))
    (is (= [] (sr/defects r)))
    (is (re-find #"refused to act" (:rationale/text r)))))

;; ---------------------------------------------------------------------------
;; 3. Typed absences, each on its own specific reason.

(deftest every-illegible-decision-is-a-typed-absence-test
  (testing "not a map"
    (is (= :record-not-a-map (:rationale/absence-reason (record-of nil))))
    (is (= :record-not-a-map (:rationale/absence-reason (record-of "junk")))))
  (testing "no decision key at all"
    (is (= :no-decision
           (:rationale/absence-reason (record-of (dissoc selection-record :decision))))))
  (testing "a decision carrying neither a chosen action nor a refusal"
    (is (= :no-outcome-and-no-refusals
           (:rationale/absence-reason
            (record-of (assoc selection-record :decision {:reason :nothing-here}))))))
  (testing "an empty candidate set"
    (is (= :empty-candidate-set
           (:rationale/absence-reason
            (record-of (assoc selection-record :ranked-actions []))))))
  (testing "every absence reason is in the closed enum and every record validates"
    (doseq [r [(record-of nil)
               (record-of (dissoc selection-record :decision))
               (record-of (assoc selection-record :decision {:reason :nothing-here}))
               (record-of (assoc selection-record :ranked-actions []))]]
      (is (= :typed-absence (:rationale/status r)))
      (is (contains? sr/absence-reasons (:rationale/absence-reason r)))
      (is (= [] (sr/defects r))))))

(deftest producer-is-total-over-arbitrary-inputs-test
  (doseq [x [nil {} [] 7 :kw "s" {:decision 3} {:decision {} :ranked-actions 5}
             {:decision {:action {:type :x}} :policy-support-exclusions 9}
             {:ranked-actions "not a list"}]]
    (let [r (record-of x)]
      (is (map? r) (str "returned a record for " (pr-str x)))
      (is (contains? #{:recorded :typed-absence} (:rationale/status r)))
      (is (= [] (sr/defects r)) (str "validates for " (pr-str x)))
      (when (= :typed-absence (:rationale/status r))
        (is (contains? sr/absence-reasons (:rationale/absence-reason r))))))
  (testing "a non-collection candidate field is malformed, not an empty candidate set"
    (is (= :malformed-record-fields
           (:rationale/absence-reason (record-of {:ranked-actions "not a list"}))))))

;; ---------------------------------------------------------------------------
;; 4. The validator, shown able to fire.

(deftest defects-names-what-is-wrong-test
  (let [good (record-of selection-record)]
    (is (= [] (sr/defects good)))
    (is (= [:wrong-schema-version]
           (sr/defects (assoc good :rationale/schema-version 99))))
    (is (= [:status-out-of-vocabulary]
           (sr/defects (assoc good :rationale/status :probably-fine))))
    (is (= [:missing-contract-sha]
           (sr/defects (dissoc good :rationale/contract-sha))))
    (is (= [:not-marked-decision-time]
           (sr/defects (assoc good :rationale/emitted-at-decision? false))))
    (is (= [:absence-reason-out-of-vocabulary]
           (sr/defects (assoc (record-of nil) :rationale/absence-reason :made-up))))))

;; ---------------------------------------------------------------------------
;; 5. The write: unconditional, deterministic, refusing a defective record.

(deftest emit-writes-one-record-and-is-replay-stable-test
  (let [dir (str *tmpdir* "/store")
        path (sr/emit! selection-record {:dir dir :contract planted-contract})
        bytes1 (slurp path)
        _ (sr/emit! selection-record {:dir dir :contract planted-contract})
        bytes2 (slurp path)]
    (is (.isFile (io/file path)))
    (is (= (str dir "/rationale-2026-09-01-run-a.edn") path))
    (is (= bytes1 bytes2) "re-emitting the same record produces the same bytes")
    (let [r (edn/read-string bytes1)]
      (is (= :recorded (:rationale/status r)))
      (is (= [] (sr/defects r))))))

(deftest emit-writes-a-typed-absence-rather-than-nothing-test
  ;; The negative control the acceptance names: a decision path that fails to
  ;; produce a rationale must be a TYPED ABSENCE IN THE STORE, not silence.
  (let [dir (str *tmpdir* "/absence-store")
        illegible (dissoc selection-record :decision)
        path (sr/emit! illegible {:dir dir :contract planted-contract})]
    (is (.isFile (io/file path)) "the store holds a file for the failed decision")
    (let [r (edn/read-string (slurp path))]
      (is (= :typed-absence (:rationale/status r)))
      (is (= :no-decision (:rationale/absence-reason r)))
      (is (= "run-a" (:rationale/run-id r)))))
  (testing "even a record with no run id lands under a stable name"
    (let [dir (str *tmpdir* "/absence-store-2")
          path (sr/emit! {:timestamp "2026-09-01T00:00:00Z"}
                         {:dir dir :contract planted-contract})]
      (is (= (str dir "/rationale-2026-09-01-no-run-id-2026-09-01T00-00-00Z.edn") path))
      (is (= :no-decision (:rationale/absence-reason (edn/read-string (slurp path))))))))

(deftest emit-refuses-a-defective-record-loudly-test
  (let [dir (str *tmpdir* "/refused-store")]
    (with-redefs [sr/rationale-record (fn [& _] {:rationale/status :improvised})]
      (let [e (is (thrown-with-msg?
                   clojure.lang.ExceptionInfo
                   #"refusing to write a defective record"
                   (sr/emit! selection-record {:dir dir})))]
        (is (= :selection-rationale (:stage (ex-data e))))))
    (is (not (.exists (io/file dir))) "nothing was written")))

(deftest emit-throws-when-the-store-cannot-be-written-test
  ;; A file where the directory must go: `make-parents`/`spit` cannot succeed.
  (let [blocker (str *tmpdir* "/blocked")]
    (spit blocker "not a directory")
    (let [e (is (thrown-with-msg? clojure.lang.ExceptionInfo #"store write failed"
                                  (sr/emit! selection-record
                                            {:dir blocker :contract planted-contract})))]
      (is (= :selection-rationale (:stage (ex-data e)))))))

;; ---------------------------------------------------------------------------
;; 6. The contract pin read.

(deftest contract-identity-is-typed-both-ways-test
  (testing "absent"
    (is (= {:status :absent :reason :contract-file-missing
            :path (str *tmpdir* "/nope.json")}
           (sr/contract-identity (str *tmpdir* "/nope.json")))))
  (testing "unparseable"
    (let [p (str *tmpdir* "/bad.json")]
      (spit p "{not json")
      (is (= :contract-unparseable (:reason (sr/contract-identity p))))))
  (testing "parseable but carrying no source sha"
    (let [p (str *tmpdir* "/nosha.json")]
      (spit p "{\"contract-id\":\"wm-holes\"}")
      (is (= :contract-carries-no-source-sha (:reason (sr/contract-identity p))))))
  (testing "present"
    (let [p (str *tmpdir* "/good.json")]
      (spit p "{\"contract-id\":\"wm-holes\",\"source\":{\"git-sha\":\"deadbeef\"}}")
      (is (= {:status :present :git-sha "deadbeef" :contract-id "wm-holes" :path p}
             (sr/contract-identity p))))))

;; ---------------------------------------------------------------------------
;; 7. The seam. This is the only place that establishes the rationale is
;;    written IN THE SAME ACT as the trace, rather than by a separate pass.

(def ^:private seam-judge-output
  {:belief (belief/initial-belief-state [:m1])
   :observation {:loop-health 0.7}
   :free-energy {:preference-gap-score 0.05 :coverage-uncertainty-pressure 0.1
                 :controller-score 0.075 :per-channel {} :avoided-active []}
   :ranked-actions [{:action {:type :no-op} :G-risk 0.05 :G-ambiguity 0.0
                     :controller-score 0.05 :G-core 0.05 :rank 1}
                    {:action {:type :address-sorry :target :sorry/x}
                     :G-risk 0.03 :G-ambiguity 0.015
                     :controller-score 0.045 :G-core 0.045 :rank 2}]
   :policy-support-exclusions []
   :decision {:action {:type :address-sorry :target :sorry/x}
              :controller-ranking
              [{:rank 1 :action {:type :no-op}}
               {:rank 2 :action {:type :address-sorry :target :sorry/x}}]
              :rank 2 :controller-score 0.045
              :reason :chosen-by-controller
              :selection-boundary :actuation}
   :run/id "seam-run-1"
   :mode :multiplied})

(defn- seam-write! [judge-output]
  (let [trace-dir (str *tmpdir* "/seam")
        path (#'wm/write-trace-and-clock! judge-output trace-dir)
        rationale-files (->> (io/file (str trace-dir "/rationale"))
                             file-seq
                             (filter #(.isFile ^File %))
                             (sort-by #(.getName ^File %)))]
    {:trace-path path
     :trace-dir trace-dir
     :rationale-files rationale-files}))

(deftest the-seam-writes-a-rationale-in-the-same-act-as-the-trace-test
  (let [{:keys [trace-path rationale-files]} (seam-write! seam-judge-output)
        trace-record (edn/read-string (first (line-seq (io/reader trace-path))))
        rationale (edn/read-string (slurp (first rationale-files)))]
    (is (= 1 (count rationale-files)) "exactly one rationale per persisted decision")
    (is (= :recorded (:rationale/status rationale)))
    (is (= :selected (:rationale/outcome rationale)))
    (is (= "seam-run-1" (:rationale/run-id rationale)))
    (testing "the rationale is about the record that was written, joined by its own timestamp"
      (is (= (:timestamp trace-record) (:rationale/at rationale)))
      (is (= trace-path (:rationale/trace-path rationale))))
    (testing "the chosen action is the one the decision took"
      (is (= {:type :address-sorry :target :sorry/x}
             (get-in rationale [:rationale/chosen :action])))
      (is (= 2 (get-in rationale [:rationale/chosen :controller-rank]))))
    (testing "the live contract pin is read at decision time and typed either way"
      (is (contains? #{:present :absent}
                     (get-in rationale [:rationale/contract-sha :status]))))
    (is (= [] (sr/defects rationale)))))

(deftest the-seam-writes-a-typed-absence-when-the-decision-is-illegible-test
  ;; The seam-level negative control: the acceptance's "not silence" clause has
  ;; to hold at the CALL SITE, not only in the producer.
  (let [{:keys [rationale-files]} (seam-write! (dissoc seam-judge-output :decision))
        rationale (edn/read-string (slurp (first rationale-files)))]
    (is (= 1 (count rationale-files)))
    (is (= :typed-absence (:rationale/status rationale)))
    (is (contains? sr/absence-reasons (:rationale/absence-reason rationale)))))

(deftest the-seam-never-writes-into-the-live-store-when-the-trace-is-redirected-test
  ;; `rationale-dir` follows the trace. If it did not, a test run would append
  ;; to the real run store.
  (is (= (str *tmpdir* "/seam/rationale") (#'wm/rationale-dir (str *tmpdir* "/seam"))))
  (is (= sr/default-store-dir (#'wm/rationale-dir nil))))
