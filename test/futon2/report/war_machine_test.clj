(ns futon2.report.war-machine-test
  "Tests for War Machine scan logic.

   Tests the pure data transformation functions — arrow-health,
   observation vector, and data shape contracts — without requiring
   live APIs or git repos."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.efe :as efe]
            [futon2.aif.free-energy :as free-energy]
            [futon2.aif.observation :as observation]
            [futon2.aif.policy :as policy]
            [futon2.aif.trace :as trace]
            [futon2.report.war-machine :as wm])
  (:import (java.io PushbackReader StringReader)))

(defn- read-all-forms [source]
  (with-open [reader (PushbackReader. (StringReader. source))]
    (loop [forms []]
      (let [form (read {:eof ::eof} reader)]
        (if (= ::eof form) forms (recur (conj forms form)))))))

(defn- find-binding-expression [form binding-symbol]
  (or (when (vector? form)
        (some (fn [[binding value]]
                (when (= binding-symbol binding) value))
              (partition 2 form)))
      (when (coll? form)
        (some #(find-binding-expression % binding-symbol) form))))

(def ^:private pre-suppression-revision
  "The commit before R5 added :step-portfolio? — e7a9bb6^, PINNED.

   It was `HEAD~`, which broke in the shared checkout when an unrelated commit
   landed between. The fix for that anchored to the parent of the latest commit
   touching war_machine.clj, which survives concurrency but DECAYS: after the
   next edit to this file, that parent already contains R5's change, so the
   control compares R5's default path against itself and the claim it backs —
   \"the default preserves the behaviour that existed before suppression\" —
   silently becomes \"the last commit changed nothing\". A rolling check is
   useful, but it is not this claim, and nobody would notice the substitution.
   A control for a fixed claim gets a fixed anchor (claude-20 review, R5)."
  "e7a9bb64bbe166f8648e47ff0bc1e7baff59a0a2^")

(defn- previous-portfolio-step-fn []
  (let [{show-exit :exit out :out show-err :err}
        (shell/sh "git" "show"
                  (str pre-suppression-revision
                       ":scripts/futon2/report/war_machine.clj"))]
    (when-not (zero? show-exit)
      (throw (ex-info "could not load previous war-machine implementation"
                      {:revision pre-suppression-revision :err show-err})))
    (load-string
     (-> out
         (str/replace-first "(ns futon2.report.war-machine"
                            "(ns futon2.report.war-machine-previous")
         ;; I5 slice (c) deleted `compute-variational-free-energy` from
         ;; futon2.aif.free-energy, and this control compiles a HISTORICAL
         ;; war_machine against TODAY's libraries -- so every revision that
         ;; predates the retirement now fails to compile on a call the control
         ;; does not exercise. The anchor stays pinned (moving it would replace
         ;; the claim, as its own docstring says); the retired call is replaced
         ;; with nil, which is sound here precisely because F is off the
         ;; portfolio-step path this test reads.
         (str/replace "(fe/compute-variational-free-energy prediction-errors)"
                      "nil")))
    (let [judge-form (some #(when (and (seq? %)
                                       (= 'defn (first %))
                                       (= 'judge (second %)))
                              %)
                           (read-all-forms out))
          expression (find-binding-expression judge-form 'portfolio-step)]
      (when-not expression
        (throw (ex-info "HEAD~ judge has no portfolio-step binding" {})))
      (binding [*ns* (the-ns 'futon2.report.war-machine-previous)]
        (eval (list 'fn [] expression))))))

(deftest suppressed-judge-portfolio-step-issues-no-post-test
  (with-redefs [http/post
                (fn [& request]
                  (throw (ex-info "suppressed path issued a POST"
                                  {:request request})))]
    (is (= {:status :absent :reason :portfolio-step-suppressed}
           (#'wm/portfolio-step-for-judge false)))))

(deftest unsuppressed-judge-portfolio-step-matches-head-previous-test
  (let [response {:status 200
                  :body "{\"action\":{\"type\":\"inspect\"},\"recommendation\":\"continue\",\"structure\":{\"adjacent\":[\"A\"]}}"}
        old-step (previous-portfolio-step-fn)]
    (with-redefs [http/post (fn [& _] response)]
      (is (= (old-step) (#'wm/portfolio-step-for-judge true))))))

(deftest suppressed-mission-detail-step-is-explicit-test
  (with-redefs [http/post
                (fn [& request]
                  (throw (ex-info "suppressed path issued a POST"
                                  {:request request})))]
    (is (= {:status :absent
            :reason :mission-detail-portfolio-step-suppressed}
           (:portfolio-step-status
            (wm/scan-mission-detail [{:mission/id "M-test"}] false))))))

(deftest suppressed-invariant-eval-fallback-is-explicit-test
  (with-redefs-fn
    {#'http/post
     (fn [& request]
       (throw (ex-info "suppressed path issued a POST"
                       {:request request})))
     #'wm/http-get-json (constantly nil)
     #'wm/read-edn-file (constantly {:families [] :invariants []})}
    (fn []
      (let [inventory (wm/load-invariant-inventory false)]
        (is (false? (:live-available? inventory)))
        (is (= {:status :absent
                :reason :invariant-eval-fallback-suppressed}
               (:live-status inventory)))))))

(def ^:private real-wm-channels
  ;; Exact channel set from data/wm-trace/wm-trace-2026-07-04.edn.
  [:mathematics-pct :coupling-density :support-coverage :depositing-signal
   :loop-health :mission-health :portfolio-pct :annotation-health
   :ticks-firing-ratio :active-repo-ratio :attack-coverage :consulting-pct
   :sorry-count-norm :stack-pct])

(defn- real-shape-f-pi-fixture []
  (let [mean (zipmap real-wm-channels (repeat 0.5))
        variance (zipmap real-wm-channels
                         (concat [0.02 0.03] (repeat 12 0.0)))
        variance-status
        (into {} (map (fn [channel]
                        [channel {:status :absent
                                  :reason :deterministic-by-action-model}])
                      (drop 2 real-wm-channels)))
        previous-ranked
        (mapv (fn [index]
                {:rank (inc index)
                 :action {:type :open-mission :target (str "M-real-" index)}
                 :prediction-mean mean
                 :prediction-variance variance
                 :prediction-variance-status variance-status})
              (range 110))
        ;; Candidate 109 disappeared; rank/110 now denotes a different action.
        current-ranked (assoc previous-ranked 109
                              {:rank 110
                               :action {:type :open-mission
                                        :target "M-new-this-tick"}})
        ;; Candidate 0 has one prediction-only channel. Its failure must not
        ;; prevent the other 108 shared candidates from being scored.
        previous-ranked (update-in previous-ranked [0 :prediction-mean]
                                   assoc :prediction-only 0.1)
        observation (zipmap real-wm-channels
                            (map #(+ 0.45 (* 0.001 %)) (range 14)))]
    {:previous {:timestamp "2026-07-04T12:00:00Z"
                :effects-mode :target-scaled
                :ranked-actions previous-ranked}
     :current current-ranked
     :observation observation}))

(deftest f-pi-dark-readback-real-wm-shape-test
  (let [{:keys [previous current observation]} (real-shape-f-pi-fixture)
        result (wm/f-pi-dark-readback previous current observation)
        envelope (:f-pi-by-candidate-id result)
        values (:by-candidate-id envelope)]
    (is (= :present (:status envelope)))
    (is (= 110 (count values)))
    (is (= :channel-mismatch (get-in values ["rank/1" :reason])))
    (is (= :candidate-not-in-current-tick
           (get-in values ["rank/110" :reason])))
    (is (= 109 (get-in result [:f-pi-provenance :matched-count])))
    (is (= 1 (get-in result [:f-pi-provenance :unmatched-count])))
    (is (= 108 (get-in result [:f-pi-provenance :scored-count])))
    (is (every? #(= :present (get-in values [(str "rank/" %) :status]))
                (range 2 110)))
    (is (= result (edn/read-string (pr-str result))))))

(deftest f-pi-whole-tick-absence-cannot-be-read-as-values-test
  ;; Both cases used to be bare maps, so `vals` on a whole-tick absence
  ;; returned (:absent :no-previous-trace-record) as though those were F_pi
  ;; numbers. The envelope makes the two shapes distinguishable by :status.
  (let [absent (:f-pi-by-candidate-id (wm/f-pi-dark-readback nil [] {}))
        {:keys [previous current observation]} (real-shape-f-pi-fixture)
        present (:f-pi-by-candidate-id
                 (wm/f-pi-dark-readback previous current observation))]
    (is (= :absent (:status absent)))
    (is (nil? (:by-candidate-id absent)))
    (is (= :present (:status present)))
    (is (map? (:by-candidate-id present)))
    (is (every? #(contains? % :status) [absent present])
        "one envelope, so a consumer reads :status before anything else")))

(deftest f-pi-dark-readback-explicit-whole-tick-absence-test
  (is (= :no-previous-trace-record
         (get-in (wm/f-pi-dark-readback nil [] {})
                 [:f-pi-by-candidate-id :reason])))
  (is (= :previous-trace-has-no-policy-predictions
         (get-in (wm/f-pi-dark-readback
                  {:timestamp "old" :ranked-actions [{:rank 1 :action {:type :no-op}}]}
                  [{:rank 1 :action {:type :no-op}}]
                  {})
                 [:f-pi-by-candidate-id :reason]))))

(deftest beta-dark-carry-solves-over-the-real-shape-field-test
  ;; RUN7 / stage S2. The same 110-candidate fixture the F_pi readback is
  ;; measured on, with this tick's controller scores added -- so the join under
  ;; test is the one a run performs, not a two-candidate toy.
  (let [{:keys [previous current observation]} (real-shape-f-pi-fixture)
        scored (mapv (fn [index action]
                       (assoc action :controller-score (+ 0.25 (* 0.004 index))))
                     (range) current)
        f-pi-fields (wm/f-pi-dark-readback previous scored observation)
        result (wm/beta-dark-carry {} f-pi-fields scored)
        state (:policy-precision-state result)]
    (is (= :present (:status state)))
    (is (= 108 (:f-pi-present-count state))
        "the 108 candidates F_pi scored, and only those, enter the solve")
    (is (= 2 (:f-pi-absent-count state))
        "the channel-mismatch candidate and the one that left the tick are
         counted rather than dropped into a shorter vector")
    (is (= 108 (get-in state [:solve :candidate-count])))
    (is (true? (get-in state [:solve :converged?])))
    (is (true? (get-in state [:solve :bracketed?])))
    (is (= :converged-posterior (:beta-source state)))
    (is (number? (get-in state [:solve :gamma])))
    (is (= state (edn/read-string (pr-str state)))
        "the state survives the EDN round trip it is carried through"))

  (testing "the carry closes through the trace: this tick's beta is next tick's prior"
    (let [{:keys [previous current observation]} (real-shape-f-pi-fixture)
          scored (mapv (fn [index action]
                         (assoc action :controller-score (+ 0.25 (* 0.004 index))))
                       (range) current)
          f-pi-fields (wm/f-pi-dark-readback previous scored observation)
          first-state (:policy-precision-state
                       (wm/beta-dark-carry {} f-pi-fields scored))
          ;; exactly how a run does it: through a persisted trace record
          record (trace/trace-record {:belief {} :observation observation
                                      :policy-precision-state first-state})
          second-state (:policy-precision-state
                        (wm/beta-dark-carry (edn/read-string (pr-str record))
                                            f-pi-fields scored))]
      (is (= (:beta first-state) (get-in second-state [:solve :beta-prior])))
      (is (= 2 (:solved-tick-count second-state))))))

(deftest beta-dark-carry-names-the-reason-it-could-not-solve-test
  (testing "a whole-tick F_pi absence is carried through as its own reason"
    (let [f-pi-fields (wm/f-pi-dark-readback nil [] {})
          state (:policy-precision-state (wm/beta-dark-carry nil f-pi-fields []))]
      (is (= :absent (:status state)))
      (is (= :no-previous-trace-record (:reason state))
          "the F_pi reason propagates, rather than being flattened to a
           generic one that hides which of the three flags was off")
      (is (= 1.0 (:beta state)))
      (is (= :initial (:beta-source state)))
      (is (not (contains? state :solve)))))
  (testing "and the dark beta flag on its own says so rather than reporting nothing"
    (let [state (:policy-precision-state (wm/beta-dark-carry nil nil []))]
      (is (= :no-f-pi-readback (:reason state)))))
  (testing "an F_pi entry whose identity no current candidate carries is absence"
    ;; NOT a test of the Throwable catch in beta-dark-carry: with the join's
    ;; numeric filters in place no input reachable through this seam makes
    ;; converge-beta throw, so that catch is a guard against a future change to
    ;; the solver's validation and is deliberately not claimed as covered.
    (let [fields {:f-pi-by-candidate-id
                  {:status :present
                   :by-candidate-id
                   {"rank/1" {:candidate-identity :c0 :status :present :value -1.0}}}}
          ranked [{:action {:type :open-mission :target "c0"} :controller-score 0.1}]
          state (:policy-precision-state
                 (wm/beta-dark-carry {:policy-precision-state {:beta 1.0}}
                                     fields ranked))]
      ;; the identity fn is the habit-prior policy key, so the readback's :c0
      ;; never matches; the tick records that rather than a number
      (is (= :absent (:status state)))
      (is (= :no-aligned-candidates (:reason state)))
      (is (= 1.0 (:beta state))))))

(deftest beta-dark-carry-consumes-nothing-test
  ;; What makes S2 dark: the state is on the judgement for persistence and no
  ;; selection quantity moves when it changes. Two carries whose only
  ;; difference is the beta they came in with must produce the SAME decision
  ;; inputs -- here checked at the seam, since beta-dark-carry returns only the
  ;; state and touches no ranking, softmax weight or temperature.
  (let [{:keys [previous current observation]} (real-shape-f-pi-fixture)
        scored (mapv (fn [index action]
                       (assoc action :controller-score (+ 0.25 (* 0.004 index))))
                     (range) current)
        f-pi-fields (wm/f-pi-dark-readback previous scored observation)
        low (wm/beta-dark-carry {:policy-precision-state {:beta 0.5}} f-pi-fields scored)
        high (wm/beta-dark-carry {:policy-precision-state {:beta 5.0}} f-pi-fields scored)]
    (is (= [:policy-precision-state] (keys low))
        "one key, and it is not one selection reads")
    (is (= [:policy-precision-state] (keys high)))
    (is (= 0.5 (get-in low [:policy-precision-state :solve :beta-prior])))
    (is (= 5.0 (get-in high [:policy-precision-state :solve :beta-prior])))
    (is (= (:f-pi-present-count (:policy-precision-state low))
           (:f-pi-present-count (:policy-precision-state high)))
        "the carried beta changes the solve and nothing about the field")
    (is (not= (get-in low [:policy-precision-state :solve :gamma])
              (get-in high [:policy-precision-state :solve :gamma]))
        "and the solve does move, so the previous assertion is not vacuous")))

(deftest avoidance-unknown-renders-distinguishably-test
  (let [diagnostics (free-energy/compute-controller-diagnostics
                     (observation/observe {}))
        losses (wm/avoidance-losses :multiplied diagnostics)]
    (is (= 5 (count losses)))
    (is (every? #(= :avoidance-unknown (:type %)) losses))
    (is (every? #(re-find #"avoidance unknown — observation absent"
                           (:summary %))
                losses))))

(deftest scan-frames-retains-unreadable-population-members-test
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "wm-daily-frames-"
                      (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try
      (spit (io/file dir "01-good.edn")
            (pr-str {:frame/id :one :frame/type :daily-scan
                     :frame/cardinal-direction {:depositing 0.2}}))
      (spit (io/file dir "02-broken.edn") "{:frame/id :broken")
      (spit (io/file dir "03-good.edn")
            (pr-str {:frame/id :three :frame/type :daily-scan
                     :frame/cardinal-direction {:depositing 0.6}}))
      (with-redefs-fn {#'wm/frames-dir (.getPath dir)}
        (fn []
          (let [result (wm/scan-frames)
                unreadable (first (:unreadable-frames result))]
            (is (= 3 (:frames-count result)) "the supplied population is retained")
            (is (= 2 (:readable-frames-count result)))
            (is (= 1 (:unreadable-frames-count result)))
            (is (= 2 (:daily-scan-count result)))
            (is (re-find #"02-broken[.]edn$" (:unreadable unreadable)))
            (is (string? (:cause unreadable)))
            (is (= :three (get-in result [:latest-frame :frame/id]))))))
      (finally
        (doseq [file (reverse (file-seq dir))]
          (.delete file))))))

(deftest channel-priorities-exclude-unknown-gaps-before-sorting-test
  (let [result (#'wm/channel-priority-result
                {:per-channel
                 {:known-low {:gap 0.2 :value 0.3 :preferred [0.5 1.0]}
                  :unknown {:gap nil :status :unknown
                            :reason :observation-absent}
                  :known-high {:gap 0.8 :value 0.1 :preferred [0.9 1.0]}}})]
    (is (= [:known-high :known-low] (mapv :id (:priorities result))))
    (is (= [{:type :channel-gap-exclusion
             :id :unknown
             :reason :gap-absent
             :gap-status {:status :unknown :reason :observation-absent}}]
           (:exclusions result)))
    (is (not-any? #(= :unknown (:id %)) (:priorities result)))))

(deftest strategic-selector-accepts-resolved-vars-and-rejects-absence
  (testing "requiring-resolve returns a callable Var, not a value satisfying fn?"
    (is (= {:probe true}
           (#'wm/invoke-strategic-selection
            #'identity {:probe true}))))
  (testing "absence is a system failure, never an additive fallback"
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"requires the shared reason-bearing selector"
         (#'wm/invoke-strategic-selection nil {:probe true})))))

(deftest futon1b-edn-mission-index-enables-strategic-enrichment-test
  (let [body (pr-str
              {:hyperedges
               [{:hx/type "code/v05/mission-doc"
                 :hx/endpoints ["repo-d/mission/alpha"]
                 :hx/props (pr-str {"mission/id" "M-alpha"
                                    "mission/phase" "head"})}
                {:hx/type "code/v05/mission-doc"
                 :hx/ends [{:entity-id "repo-d/mission/beta"}]
                 :hx/props {:mission/id "M-beta"
                            :mission/phase "derive"}}]})
        candidates [{:type :advance-mission :target "M-alpha"
                     :open-hole-count 4}
                    {:type :advance-mission :target "M-beta"
                     :open-hole-count 4}]
        state {:observation {:mission-health 0.3 :sorry-count-norm 0.85}
               :belief {}}]
    (with-redefs-fn
      {#'http/get (fn [_url _opts] {:status 200 :body body})
       #'wm/centrality-joint-map (fn [] {"M-alpha" 0.8 "M-beta" 0.4})
       #'wm/compute-delta-t-mission
       (fn [endpoint]
         {:mission-T (if (= endpoint "repo-d/mission/alpha") 1.0 0.7)
          :mission-phase (if (= endpoint "repo-d/mission/alpha")
                           "head"
                           "derive")})}
      (fn []
        (let [hxs (#'wm/fetch-hyperedges-by-type "code/v05/mission-doc")
              mission-idx (#'wm/mission-doc-index)
              enriched (wm/enrich-candidates-with-mission-value candidates nil)
              factors (mapv :mission-value-factor enriched)
              gs (mapv #(-> (efe/compute-efe state %) :G-efe) enriched)]
          (is (= 2 (count hxs)))
          (is (map? (:hx/props (first hxs)))
              "EDN-string props normalize to a map")
          (is (= ["repo-d/mission/beta"] (:hx/endpoints (second hxs)))
              "structured :hx/ends normalize to string endpoints")
          (is (= {"alpha" {:endpoint "repo-d/mission/alpha"
                            :operator-gates []}
                  "beta" {:endpoint "repo-d/mission/beta"
                           :operator-gates []}}
                 mission-idx))
          (is (every? some? factors))
          (is (apply distinct? factors))
          (is (apply distinct? gs)))))))

(deftest futon1b-edn-r12-apparatus-reader-test
  (let [body (pr-str
              {:hyperedges
               [{:hx/type "code/v05/wm-hyperparameter-update"
                 :hx/endpoints ["wm-class:advance-mission" "run:1"]
                 :hx/props (pr-str {:class :advance-mission
                                    :alpha-post 3.0
                                    :beta-post 2.0
                                    :intrinsic-value-post 0.67
                                    :as-of "2026-07-17T12:00:00Z"})}]})]
    (with-redefs [http/get (fn [_url _opts] {:status 200 :body body})]
      (let [result (wm/scan-r12-apparatus)]
        (is (true? (:available? result)))
        (is (= 1 (:total-records result)))
        (is (= 0.67 (get-in result [:per-class :advance-mission
                                    :intrinsic-value])))))))

(deftest delta-t-wrapper-requests-complete-mission-census-test
  (let [called (atom nil)]
    (with-redefs [clojure.core/requiring-resolve
                  (fn [_]
                    (fn [endpoint opts]
                      (reset! called [endpoint opts])
                      {:mission-phase "instantiate"}))]
      (is (= {:mission-phase "instantiate"}
             (#'wm/compute-delta-t-mission "repo/mission/example")))
      (is (= ["repo/mission/example"
              {:limit 500
               :families ["code/v05/mission-doc"]}]
             @called)))))

(deftest mission-doc-index-parses-zero-one-and-many-operator-gates
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "wm-operator-gates-"
                       (make-array java.nio.file.attribute.FileAttribute 0)))
        paths (mapv #(io/file root (str "M-" % ".md")) ["zero" "one" "many"])
        _ (spit (paths 0) "# Zero\nStatus: MAP\n")
        _ (spit (paths 1) "# One\n**Gate:** operator-acceptance — Joe accepts the view\n")
        _ (spit (paths 2) (str "# Many\n"
                               "**Gate:** operator-acceptance — Joe accepts the view\n"
                               "**Gate:** operator-consent — Joe arms execution\n"
                               "**Gate:** build — this is not an operator gate\n"))
        hxs (mapv (fn [mission path]
                    {:hx/endpoints [(str "repo/mission/" mission)]
                     :hx/props {:mission/id (str "M-" mission)
                                :source-file (str path)}})
                  ["zero" "one" "many"] paths)]
    (with-redefs-fn {#'wm/fetch-hyperedges-by-type (constantly hxs)}
      (fn []
        (let [idx (#'wm/mission-doc-index)]
          (is (= [] (get-in idx ["zero" :operator-gates])))
          (is (= [{:kind "operator-acceptance"
                   :text "Joe accepts the view"}]
                 (get-in idx ["one" :operator-gates])))
          (is (= [{:kind "operator-acceptance"
                   :text "Joe accepts the view"}
                  {:kind "operator-consent"
                   :text "Joe arms execution"}]
                 (get-in idx ["many" :operator-gates]))))))))

(deftest morning-brief-events-use-live-belief-update-and-hold-unknown-entities
  (let [prior {"known" {:spawned (/ 1.0 7) :refined (/ 1.0 7)
                         :strengthened (/ 1.0 7) :addressed (/ 1.0 7)
                         :falsified (/ 1.0 7) :foreclosed (/ 1.0 7)
                         :reopened (/ 1.0 7)}}
        known {:event-id "qa-1" :entity-id "known" :type :strengthened :weight 1.0}
        unknown {:event-id "qa-2" :entity-id "unknown" :type :falsified :weight 1.0}
        result (wm/apply-morning-brief-events prior #{"older"} [known unknown])]
    (is (= known
           (dissoc (first (:applied result)) :evidence/time-provenance)))
    (is (= :predates-field
           (get-in result [:applied 0 :evidence/time-provenance :reason])))
    (is (= :predates-field
           (get-in result [:held 0 :evidence/time-provenance :reason])))
    (is (= unknown
           (dissoc (first (:held result)) :evidence/time-provenance)))
    (is (= #{"older" "qa-1"} (:consumed-ids result)))
    (is (not= prior (:belief result)))))

;; ---------------------------------------------------------------------------
;; arrow-health
;; ---------------------------------------------------------------------------

(deftest arrow-health-test
  (testing "healthy arrow: recent evidence, many entries"
    (let [h (#'wm/arrow-health 15 1 14)]
      (is (> h 0.7) "15 entries, seen yesterday should be healthy")))

  (testing "starved arrow: no evidence at all"
    (is (zero? (#'wm/arrow-health 0 nil 14))
        "zero entries with nil last-seen = zero health"))

  (testing "partial arrow: some evidence but stale"
    (let [h (#'wm/arrow-health 3 10 14)]
      (is (< 0.0 h 0.5) "3 entries, 10 days old should be partial")))

  (testing "edge case: evidence count exceeds normalization ceiling"
    (let [h (#'wm/arrow-health 100 0 14)]
      (is (<= h 1.0) "health capped at 1.0")))

  (testing "edge case: last seen at window boundary"
    (let [h (#'wm/arrow-health 5 14 14)]
      (is (zero? h) "evidence at window boundary means zero freshness"))))

;; ---------------------------------------------------------------------------
;; render-war-machine (markdown output)
;; ---------------------------------------------------------------------------

(def ^:private sample-data
  "Minimal scan data for testing render functions."
  {:self-watch {:available? true
                :issues [{:severity :warning
                          :surface "archaeology"
                          :summary "2 overdue pipeline tracers need close-or-extend decisions"
                          :action "Review tracks: track-1, track-2"
                          :at "2026-05-21T10:00:00Z"}]
                :recoveries [{:severity :info
                              :surface "watchdog"
                              :summary "multi-watcher recovered"
                              :action "No action unless the alert recurs"
                              :at "2026-05-21T09:55:00Z"}]
                :issue-count 1
                :critical-count 0
                :warning-count 1}
   :commit-hygiene {:available? true
                    :queues [{:repo "futon4"
                              :tier :high
                              :pressure 3.14
                              :count 16
                              :max-age-days 22.0
                              :action "Review futon4 for commit/disposition clustering"}]
                    :active-count 1
                    :high-count 1
                    :stop-count 0
                    :clustering-status :not-yet-grouped}
   :loop-health {:overall 0.65
                 :arrows [{:arrow-id :work→proof :health 0.9}
                          {:arrow-id :proof→patterns :health 0.0}]
                 :healthy-count 4
                 :total-count 6
                 :loop-complete? false}
   :support-attack {:support-coverage 0.8
                    :attack-coverage 0.5
                    :claims []}
   :mission-triage {:health 0.4
                    :total 100
                    :active 20
                    :completed 50}
   :graph {:dynamics {:commit-percentages {:stack 0.7
                                           :consulting 0.1
                                           :portfolio 0.15
                                           :mathematics 0.05}
                      :ticks [{:id :hermit-warning :fired? true}
                              {:id :hobby-warning :fired? false}]}
           :summary {:total-repos 16
                     :active-repos 10
                     :total-sorrys 8
                     :coupling-edges 12
                     :ticks-firing 1}}})

(deftest render-war-machine-test
  (testing "produces non-empty markdown"
    (let [md (wm/render-war-machine
              {:self-watch (:self-watch sample-data)
               :commit-hygiene (:commit-hygiene sample-data)
               :loop-health (:loop-health sample-data)
               :support-attack (:support-attack sample-data)
               :mission-triage (:mission-triage sample-data)
               :graph (:graph sample-data)
               :now "2026-04-12" :days 14})]
      (is (string? md))
      (is (pos? (count md)))
      (is (.contains md "War Machine"))
      (is (.contains md "Self-Watch"))
      (is (.contains md "Commit Hygiene"))
      (is (.contains md "Loop Health"))
      (is (.contains md "Holistic Argument"))
      (is (.contains md "Mission Triage")))))

(deftest summarize-self-watch-projects-latest-warning-and-recovery
  (let [entries [{:evidence/at "2026-05-21T10:00:00Z"
                  :evidence/body {:event :family-fired
                                  :family-id :obsolescence-recognition/pipeline-tracer
                                  :outcome :violation
                                  :detail {:obsolete-count 2
                                           :obsolete-artifacts [{:track-id :track-1}
                                                                {:track-id :track-2}]}}}
                 {:evidence/at "2026-05-21T10:01:00Z"
                  :evidence/body {:event "process-alert"
                                  :process-id "multi-watcher"
                                  :kind "stale"
                                  :severity "critical"
                                  :message "last-active age 45000ms"}}
                 {:evidence/at "2026-05-21T10:02:00Z"
                  :evidence/body {:event "process-recovery"
                                  :process-id "drawbridge"
                                  :kind "recovered"
                                  :severity "info"
                                  :message "recovered"}}]
        summary (#'wm/summarize-self-watch entries)]
    (is (= 2 (:issue-count summary)))
    (is (= 1 (:critical-count summary)))
    (is (= 1 (:warning-count summary)))
    (is (= ["multi-watcher stale"
            "2 overdue pipeline tracers need close-or-extend decisions"]
           (mapv :summary (:issues summary))))
    (is (= ["drawbridge recovered"]
           (mapv :summary (:recoveries summary))))))

(deftest summarize-working-tree-hygiene-projects-top-repos-honestly
  (let [summary (#'wm/summarize-working-tree-hygiene
                 {:available? true
                  :max-tier :high
                  :max-pressure 3.25
                  :snapshot-age-minutes 12.0
                  :stale? false
                  :channels [{:channel :working-tree :pressure 3.25 :tier :high}
                             {:channel :active-sessions :pressure 0.66 :tier :silent}]
                  :per-repo [{:repo "futon4" :pressure 3.25 :count 48 :max-age-days 12.4 :bytes 1694674 :tier :high}
                             {:repo "futon3c" :pressure 0.0 :count 0 :max-age-days 0.0 :bytes 0 :tier :silent}]})]
    (is (:available? summary))
    (is (= :not-yet-grouped (:clustering-status summary)))
    (is (= 1 (:active-count summary)))
    (is (= 1 (:high-count summary)))
    (is (= ["futon4"] (mapv :repo (:queues summary))))
    (is (.contains (:action (first (:queues summary))) "commit/disposition clustering"))))

(deftest scan-vsatarcs-status-projects-compact-feed
  (with-redefs-fn {#'wm/vsatarcs-status-script "/tmp"
                   #'clojure.java.shell/sh
                   (fn [& _]
                     {:exit 0
                      :out "{:build {:status :violation} :stories [{:story/id \"leaf-invariants\" :headline \"drift\" :build/status :violation :currency/chains [{:chain :content-drift :outcome :violation}]}] :wm-escalation {:tier :warning}}"
                      :err ""})}
    (fn []
      (let [status (#'wm/scan-vsatarcs-status)]
        (is (:available? status))
        (is (= :violation (get-in status [:build :status])))
        (is (= ["leaf-invariants"] (mapv :story/id (:stories status))))
        (is (= :warning (get-in status [:wm-escalation :tier])))))))

;; ---------------------------------------------------------------------------
;; Data shape contracts
;; ---------------------------------------------------------------------------

(deftest claim-patterns-coverage-test
  (testing "all 9 structural claims are defined"
    (let [patterns #'wm/claim-patterns]
      (is (= 9 (count @patterns)))
      (is (= #{:S1 :S2 :S3 :S4 :S5 :A1 :A2 :A3 :A4}
             (set (keys @patterns)))))))

(deftest loop-arrows-coverage-test
  (testing "all 6 loop arrows are defined"
    (let [arrows #'wm/loop-arrows]
      (is (= 6 (count @arrows)))
      (is (= #{:work→proof :proof→patterns :patterns→coordination
               :coordination→self-rep :self-rep→inference :inference→work}
             (set (map :id @arrows)))))))

;; ---------------------------------------------------------------------------
;; Session replay evidence detection
;; ---------------------------------------------------------------------------

(deftest detect-repos-test
  (testing "repo tags contribute to session replay placement"
    (is (= ["futon3a"]
           (#'wm/detect-repos {:evidence/tags ["invoke" "futon3a"]
                               :evidence/type "coordination"
                               :evidence/body {:text ""}}))))

  (testing "text matches still work and are deduplicated against tags"
    (is (= ["futon0" "futon3c"]
           (#'wm/detect-repos {:evidence/tags ["futon0"]
                               :evidence/type "coordination"
                               :evidence/body {:text "war-machine changes in futon3c"}})))))

(deftest anamnesis-tiebreak-reorders-address-sorry-groups
  (let [ranked [{:rank 1
                 :controller-score -4.2558
                 :action {:type :address-sorry
                          :target :sorry/r3a-likelihood-coupling-density}}
                {:rank 2
                 :controller-score -4.2558
                 :action {:type :address-sorry
                          :target :sorry/r3a-likelihood-ticks-firing-ratio}}
                {:rank 3
                 :controller-score -4.2558
                 :action {:type :address-sorry
                          :target :sorry/r3d-per-entity-attribution}}
                {:rank 4
                 :controller-score -4.2558
                 :action {:type :address-sorry
                          :target :sorry/stub-lifts-pending-aif-edn}}
                {:rank 5
                 :controller-score -4.2558
                 :action {:type :address-sorry
                          :target :sorry/wm-ui-hud-mode-rationale-hardcode}}
                {:rank 6
                 :controller-score -4.2558
                 :action {:type :address-sorry
                          :target :sorry/mission-aif-head-not-served}}
                {:rank 7
                 :controller-score -4.2558
                 :action {:type :address-sorry
                          :target :sorry/handler-closure-route-rebinding}}]
        sorry-idx {"sorry/r3a-likelihood-coupling-density"
                   {:hx/props {:sorry/related-missions ["M-r3a-density"]}}
                   "sorry/r3a-likelihood-ticks-firing-ratio"
                   {:hx/props {:sorry/related-missions ["M-r3a-ticks"]}}
                   "sorry/r3d-per-entity-attribution"
                   {:hx/props {:sorry/related-missions ["M-r3d"]}}
                   "sorry/stub-lifts-pending-aif-edn"
                   {:hx/props {:sorry/related-missions []}}
                   "sorry/wm-ui-hud-mode-rationale-hardcode"
                   {:hx/props {:sorry/related-missions ["M-wm-ui"]}}
                   "sorry/mission-aif-head-not-served"
                   {:hx/props {:sorry/related-missions ["M-head-a" "M-head-b" "M-head-c"]}}
                   "sorry/handler-closure-route-rebinding"
                   {:hx/props {:sorry/related-missions ["M-drawbridge"]}}}
        mission-idx {"r3a-density" "futon3c-d/mission/r3a-density"
                     "r3a-ticks" "futon3c-d/mission/r3a-ticks"
                     "r3d" "futon3c-d/mission/r3d"
                     "wm-ui" "futon3c-d/mission/wm-ui"
                     "head-a" "futon3c-d/mission/head-a"
                     "head-b" "futon3c-d/mission/head-b"
                     "head-c" "futon3c-d/mission/head-c"
                     "drawbridge" "futon3c-d/mission/drawbridge"}
        delta-by-endpoint {"futon3c-d/mission/r3a-density" {:mission-T 1.0}
                           "futon3c-d/mission/r3a-ticks" {:mission-T 1.0}
                           "futon3c-d/mission/r3d" {:mission-T 0.3}
                           "futon3c-d/mission/wm-ui" {:mission-T 0.8}
                           "futon3c-d/mission/head-a" {:mission-T 0.1}
                           "futon3c-d/mission/head-b" {:mission-T 0.3}
                           "futon3c-d/mission/head-c" {:mission-T 0.4}
                           "futon3c-d/mission/drawbridge" {:mission-T 0.0}}]
    (with-redefs-fn {#'wm/sorry-doc-index (fn [] sorry-idx)
                     #'wm/mission-doc-index (fn [] mission-idx)
                     #'wm/compute-delta-t-mission
                     (fn [mission-endpoint]
                       (get delta-by-endpoint mission-endpoint {:delta-T 0.0}))}
      (fn []
        (let [reordered (#'wm/apply-anamnesis-tiebreak ranked)
              targets (mapv #(get-in % [:action :target]) reordered)]
          (is (= [:sorry/mission-aif-head-not-served
                  :sorry/handler-closure-route-rebinding
                  :sorry/r3d-per-entity-attribution
                  :sorry/wm-ui-hud-mode-rationale-hardcode
                  :sorry/r3a-likelihood-coupling-density
                  :sorry/r3a-likelihood-ticks-firing-ratio
                  :sorry/stub-lifts-pending-aif-edn]
                 targets))
          (is (= [1 2 3 4 5 6 7] (mapv :rank reordered)))
          (is (= [:sorry/r3a-likelihood-coupling-density
                  :sorry/r3a-likelihood-ticks-firing-ratio
                  :sorry/stub-lifts-pending-aif-edn]
                 (subvec targets 4 7))
              "legitimate 0.0 concentration ties stay in original order"))))))

(deftest structural-pressure-enrichment-attaches-candidate-local-values
  (let [candidates [{:type :no-op}
                    {:type :address-sorry
                     :target :sorry/r3d-per-entity-attribution}
                    {:type :address-sorry
                     :target :sorry/mission-aif-head-not-served}]
        sorry-idx {"sorry/r3d-per-entity-attribution"
                   {:hx/props {:sorry/related-missions ["M-r3d"]}}
                   "sorry/mission-aif-head-not-served"
                   {:hx/props {:sorry/related-missions ["M-head-a" "M-head-b" "M-head-c"]}}}
        mission-idx {"r3d" "futon3c-d/mission/r3d"
                     "head-a" "futon3c-d/mission/head-a"
                     "head-b" "futon3c-d/mission/head-b"
                     "head-c" "futon3c-d/mission/head-c"}
        delta-by-endpoint {"futon3c-d/mission/r3d" {:mission-T 0.3}
                           "futon3c-d/mission/head-a" {:mission-T 0.1}
                           "futon3c-d/mission/head-b" {:mission-T 0.3}
                           "futon3c-d/mission/head-c" {:mission-T 0.4}}]
    (with-redefs-fn {#'wm/sorry-doc-index (fn [] sorry-idx)
                     #'wm/mission-doc-index (fn [] mission-idx)
                     #'wm/compute-delta-t-mission
                     (fn [mission-endpoint]
                       (get delta-by-endpoint mission-endpoint {:mission-T 0.5}))}
      (fn []
        (let [enriched (#'wm/enrich-candidates-with-structural-pressure candidates)]
          (is (= 0.0 (:structural-pressure-per-action (first enriched))))
          (is (= 0.7 (:structural-pressure-per-action (second enriched))))
          (is (= 2.2 (:structural-pressure-per-action (nth enriched 2)))))))))

(deftest three-factor-mission-value-enrichment-and-non-progress-decay
  (let [candidates [{:type :advance-mission :target "M-spine" :open-hole-count 4}
                    {:type :advance-mission :target "M-head" :open-hole-count 4}
                    {:type :advance-mission :target "M-instantiate" :open-hole-count 4}
                    {:type :advance-mission :target "M-complete" :open-hole-count 4}
                    {:type :fire-pattern :target :pattern/high :retrieval-score 8.0}
                    {:type :fire-pattern :target :pattern/low :retrieval-score 2.0}]
        prev {:decision {:action {:type :advance-mission :target "M-spine"}}
              :mu-pre {"M-spine" {:addressed 0.2}}
              :mu-post {"M-spine" {:addressed 0.2}}
              :outcome :grounded-no-change}
        delta-by-endpoint {"mission/spine" {:mission-phase "identify"}
                           "mission/head" {:mission-phase "head"}
                           "mission/instantiate" {:mission-phase "instantiate"}
                           "mission/complete" {:mission-phase "complete"}}
        cascades {"cascade-a"
                  {:boxes [{:id :spine :mission "M-spine"}
                           {:id :instant :mission "M-instantiate + M-complete"}]
                   :spine [:spine]
                   :terminals [:instant]}
                  "cascade-b"
                  {:boxes [{:id :same :mission "M-spine"}]
                   :terminals [:same]}}
        redefs {#'wm/centrality-joint-map
                (fn [] {"M-spine" 0.0
                        "M-head" 0.5
                        "M-instantiate" 0.5
                        "M-complete" 1.0})
                #'wm/mission-doc-index
                (fn [] {"spine" "mission/spine"
                        "head" "mission/head"
                        "instantiate" "mission/instantiate"
                        "complete" "mission/complete"})
                #'wm/compute-delta-t-mission #(get delta-by-endpoint %)
                #'wm/read-strategy-cascade #(get cascades %)}]
    (with-redefs-fn
      redefs
      (fn []
        (let [[stuck head instantiate complete pattern-high pattern-low]
              (wm/enrich-candidates-with-mission-value
               candidates prev {:strategy-cascade-path "cascade-a"})
              [fresh] (wm/enrich-candidates-with-mission-value
                       [(first candidates)] nil
                       {:strategy-cascade-path "cascade-a"})]
          (is (= {:central 0.0
                  :strategic 1.0
                  :doable 0.2
                  :phase "identify"}
                 (select-keys stuck [:central :strategic :doable :phase])))
          (is (pos? (:mission-value-factor fresh))
              "a zero-centrality spine mission still has additive value")
          (is (= 0.5 (:central instantiate))
              "centrality uses the global cmax, not the candidate batch")
          (is (> (:doable instantiate) (:doable head)))
          (is (> (:mission-value-factor instantiate)
                 (:mission-value-factor head)))
          (is (zero? (:mission-value-factor complete)))
          (is (= 0.5 (:non-progress-decay stuck)))
          (is (true? (:non-progress? stuck)))
          (is (= 1 (:non-progress-count stuck)))
          (is (= (* 0.5 (:mission-value-factor fresh))
                 (:mission-value-factor stuck)))
          (is (< (:mission-value-factor stuck)
                 (:mission-value-factor fresh)))
          (is (= 1.0 (:mission-value-factor pattern-high)))
          (is (= 0.25 (:mission-value-factor pattern-low)))
          (let [[alternate]
                (wm/enrich-candidates-with-mission-value
                 [(first candidates)] nil
                 {:strategy-cascade-path "cascade-b"})]
            (is (not= (:mission-value-factor fresh)
                      (:mission-value-factor alternate))
                "swapping cascades changes the same mission's value"))
          (let [[doable-only]
                (wm/enrich-candidates-with-mission-value
                 [(first candidates)] nil
                 {:strategy-cascade-path "cascade-a"
                  :mission-value-weights {:central 0.0
                                          :strategic 0.0
                                          :doable 1.0}})]
            (is (= 0.2 (:mission-value-factor doable-only))
                "opts override the default three-factor weights"))
          (let [[stuck-again]
                (wm/enrich-candidates-with-mission-value
                 [(first candidates)]
                 {:decision {:action stuck}
                  :mu-pre {"M-spine" {:addressed 0.2}}
                  :mu-post {"M-spine" {:addressed 0.2}}
                  :outcome :grounded-no-change}
                 {:strategy-cascade-path "cascade-a"})]
            (is (= 2 (:non-progress-count stuck-again)))
            (is (= (/ 1.0 3.0) (:non-progress-decay stuck-again))))
          (let [state {:observation {:mission-health 0.3
                                     :sorry-count-norm 0.85}
                       :belief {}}
                stuck-g (:G-efe (efe/compute-efe state stuck))
                instantiate-g (:G-efe (efe/compute-efe state instantiate))]
            (is (not= stuck-g instantiate-g)
                "judge-enriched equal-hole candidates have distinct strategic G")))))))

(deftest operator-gate-is-a-multiplicative-mask-with-visible-components
  (with-redefs-fn
    {#'wm/centrality-joint-map
     (fn [] {"M-gated" 1.0 "M-open" 0.5})
     #'wm/mission-doc-index
     (fn [] {"gated" {:endpoint "mission/gated"
                       :operator-gates
                       [{:kind "operator-acceptance"
                         :text "Joe accepts the result"}]}
             "open" {:endpoint "mission/open"
                     :operator-gates []}})
     #'wm/compute-delta-t-mission
     (fn [_] {:mission-phase "instantiate"})
     #'wm/read-strategy-cascade
     (fn [_] {:boxes [] :spine [] :terminals []})}
    (fn []
      (let [[gated open]
            (wm/enrich-candidates-with-mission-value
             [{:type :advance-mission :target "M-gated"}
              {:type :advance-mission :target "M-open"}]
             [] {:strategy-cascade-path "unused"})]
        (is (= 1.0 (:central gated)))
        (is (= 0.0 (:strategic gated)))
        (is (= 0.0 (:doable gated)))
        (is (true? (:operator-gated gated)))
        (is (= 0.0 (:operator-gate-factor gated)))
        (is (= 1.0 (:completion-gate-factor gated)))
        (is (zero? (:mission-value-factor gated)))
        (is (true? (:operator-gate-top-candidate gated)))
        (is (pos? (:mission-value-factor open)))))))

(deftest non-progress-window-skips-repairs-and-resets-on-grounded-work
  (let [action {:type :advance-mission :target "M-learning-loop"}
        failed {:decision {:action action} :outcome :build-failed}
        repair {:decision {:action {:type :repair-machine-failure
                                    :target "repair-attempt-043-build-failed"}}
                :outcome :grounded-change}
        grounded {:decision {:action action} :outcome :grounded-change}]
    (is (= 2 (#'wm/consecutive-non-progress-count
              action [failed repair failed])))
    (is (= 0 (#'wm/consecutive-non-progress-count
              action [failed repair grounded])))
    (is (= 1 (#'wm/consecutive-non-progress-count
              action [grounded repair failed])))
    (is (= 4 (#'wm/consecutive-non-progress-count
              action (assoc-in failed [:decision :action :non-progress-count] 3)))
        "the single-record API retains its carried-count behavior")
    (is (= 1 (#'wm/consecutive-non-progress-count
              action [failed {:decision {:action {:type :no-op}}} failed]))
        "a targetless record (no-op/abstain) breaks the chain without throwing")))

(deftest live-star-map-efe-opts-adds-conservative-graph-blend
  (testing "live WM opts carry the graph and softened star-map weights when graph loads"
    (let [graph {:capabilities {:goal {:status :held}}
                 :missions {}}]
      (with-redefs-fn {#'wm/capability-star-map (fn [] graph)}
        (fn []
          (let [opts (#'wm/live-star-map-efe-opts
                      {:time-pressure 0.25 :horizon-steps 3})]
            (is (= graph (:capability-graph opts)))
            (is (= :wm-overnight-unsupervised (:pre-registered-goal opts)))
            (is (= 5.0 (:graph-applicability-penalty opts)))
            (is (= 6.0 (:graph-ascent-weight opts)))
            (is (= 3.0 (:graph-body-weight opts)))
            (is (= 0.25 (:time-pressure opts)))
            (is (= 3 (:horizon-steps opts))))))))

  (testing "live WM opts are unchanged if the star-map graph is absent"
    (with-redefs-fn {#'wm/capability-star-map (fn [] nil)}
      (fn []
        (let [base {:time-pressure 0.25 :horizon-steps 3}]
          (is (= base (#'wm/live-star-map-efe-opts base))))))))

(deftest live-gap-view-efe-opts-adds-conservative-gap-blend
  (testing "live WM opts carry only ratified local-capability fold-view gap scores"
    (let [fold-view {:missions [{:mission "M-war-machine-tuning" :gap-score 0.491}
                                {:mission "M-canon-fingerprint-store" :gap-score 0.8}]}
          domain-view {:source "test-ratified"
                       :missions [{:mission "M-war-machine-tuning"
                                   :repo "futon3c"
                                   :domain :local-capability}
                                  {:mission "M-canon-fingerprint-store"
                                   :repo "futon6"
                                   :domain :math}]}]
      (reset! @#'wm/mission-fold-view-cache nil)
      (reset! @#'wm/mission-domain-ratified-cache nil)
      (with-redefs-fn {#'wm/mission-fold-view-path "fold.edn"
                       #'wm/mission-domain-ratified-path "domain.edn"
                       #'wm/read-edn-file (fn [path]
                                            (case path
                                              "fold.edn" fold-view
                                              "domain.edn" domain-view
                                              nil))}
        (fn []
          (let [opts (#'wm/live-gap-view-efe-opts
                      {:time-pressure 0.25 :horizon-steps 3})
                local (efe/gap-control-terms (:mission-gap-view opts)
                                         {:type :open-mission
                                          :target "M-war-machine-tuning"}
                                         {:gap-weight (:gap-weight opts)})
                math (efe/gap-control-terms (:mission-gap-view opts)
                                        {:type :open-mission
                                         :target "M-canon-fingerprint-store"}
                                        {:gap-weight (:gap-weight opts)})]
            (is (= {"M-war-machine-tuning" 0.491}
                   (:mission-gap-view opts)))
            (is (= 6.0 (:gap-weight opts)))
            (is (= 2.9459999999999997 (:gap-exploration-bonus local)))
            (is (= 0.0 (:gap-exploration-bonus math)))
            (is (= 0.25 (:time-pressure opts)))
            (is (= 3 (:horizon-steps opts))))))))

  (testing "live WM opts carry an empty gap view if the ratified domain file is absent"
    (let [fold-view {:missions [{:mission "M-war-machine-tuning" :gap-score 0.491}]}]
      (reset! @#'wm/mission-fold-view-cache nil)
      (reset! @#'wm/mission-domain-ratified-cache nil)
      (with-redefs-fn {#'wm/mission-fold-view-path "fold.edn"
                       #'wm/mission-domain-ratified-path "missing.edn"
                       #'wm/read-edn-file (fn [path]
                                            (case path
                                              "fold.edn" fold-view
                                              "missing.edn" nil
                                              nil))}
      (fn []
        (let [opts (#'wm/live-gap-view-efe-opts
                    {:time-pressure 0.25 :horizon-steps 3})
              local (efe/gap-control-terms (:mission-gap-view opts)
                                       {:type :open-mission
                                        :target "M-war-machine-tuning"}
                                       {:gap-weight (:gap-weight opts)})]
          (is (= {} (:mission-gap-view opts)))
          (is (= 0.0 (:gap-exploration-bonus local)))))))))

(deftest anamnesis-tiebreak-leaves-mixed-or-non-sorry-ties-alone
  (let [ranked [{:rank 1
                 :controller-score -4.2558
                 :action {:type :address-sorry
                          :target :sorry/r3d-per-entity-attribution}}
                {:rank 2
                 :controller-score -4.2558
                 :action {:type :open-mission
                          :target "M-action-cost-modelling"}}
                {:rank 3
                 :controller-score -4.2558
                 :action {:type :open-mission
                          :target "M-mission-wiring"}}]]
    (with-redefs-fn {#'wm/sorry-doc-index (fn [] (throw (ex-info "should not be called" {})))
                     #'wm/mission-doc-index (fn [] (throw (ex-info "should not be called" {})))
                     #'wm/compute-delta-t-mission
                     (fn [_] (throw (ex-info "should not be called" {})))}
      (fn []
        (let [reordered (#'wm/apply-anamnesis-tiebreak ranked)]
          (is (= (mapv #(get-in % [:action :target]) ranked)
                 (mapv #(get-in % [:action :target]) reordered)))
          (is (= [1 2 3] (mapv :rank reordered))))))))

;; ---------------------------------------------------------------------------
;; RUN8 / stage S3 — the FUTON_WM_TAU_MODE parser and the β hand-off.
;;
;; The parser and `policy/effective-temperature`'s closed dispatch have to
;; change together: one throws on any mode the other admits. These tests pin
;; the pair, so a later edit to one alone fails here rather than at tick time.
;; ---------------------------------------------------------------------------

(deftest tau-mode-parser-admits-exactly-what-the-dispatch-accepts-test
  (testing "the REAL parser: which strings map to which mode"
    (is (= :spread (wm/tau-mode-of "spread")))
    (is (= :variational-beta-gamma (wm/tau-mode-of "variational-beta-gamma")))
    (is (= :selection-gain-only (wm/tau-mode-of nil))
        "unset is the live default, flipped by Joe 2026-07-13")
    (doseq [junk ["" "gamma-only" "variational" "VARIATIONAL-BETA-GAMMA" "1"]]
      (is (= :selection-gain-only (wm/tau-mode-of junk))
          (str "unrecognised " (pr-str junk) " falls to the live default"))))
  (testing "and every mode it can produce is one effective-temperature handles
            -- the pair the acceptance says must change together"
    (let [produced (set (map wm/tau-mode-of
                             [nil "spread" "variational-beta-gamma"
                              "" "gamma-only" "typo"]))]
      (is (= #{:spread :selection-gain-only :variational-beta-gamma} produced))
      (doseq [m produced]
        (is (number?
             (policy/effective-temperature
              [0.0 1.0] 1.0 (cond-> {:tau-mode m}
                              (= m :variational-beta-gamma)
                              (assoc :variational-beta 1.25))))
            (str "effective-temperature accepts " m)))))
  (testing "the env read is the parser applied to the env and nothing else"
    (is (= (wm/tau-mode-of (System/getenv "FUTON_WM_TAU_MODE"))
           (#'wm/arena-tau-mode)))))

(deftest variational-tau-preconditions-are-loud-test
  (testing "the variational mode without the flags that compute β holds β₀ on
            every tick and τ ≡ 1.0 — correct, and useless, so it throws"
    (doseq [[beta? fpi? details?] [[false true true]
                                   [true false true]
                                   [true true false]
                                   [false false false]]]
      (with-redefs-fn {#'wm/*beta-dark?* beta?
                       #'wm/*f-pi-dark?* fpi?
                       #'trace/*persist-policy-trace-details?* details?}
        (fn []
          (is (thrown? clojure.lang.ExceptionInfo
                       (wm/variational-tau-preconditions! :variational-beta-gamma))
              (str "missing flags " [beta? fpi? details?] " must throw"))
          ;; and the two selection-gain modes are unaffected by the flags
          (is (nil? (wm/variational-tau-preconditions! :selection-gain-only)))
          (is (nil? (wm/variational-tau-preconditions! :spread))))))
    (with-redefs-fn {#'wm/*beta-dark?* true
                     #'wm/*f-pi-dark?* true
                     #'trace/*persist-policy-trace-details?* true}
      (fn []
        (is (nil? (wm/variational-tau-preconditions! :variational-beta-gamma))
            "all three flags on: no complaint")))))

(deftest variational-temperature-opts-carry-beta-and-its-source-test
  (testing "the two selection-gain modes get the historical bare map; the
            variational mode gets β and the provenance carry-beta assigned it"
    (is (= {:tau-mode :spread}
           (wm/variational-temperature-opts :spread {:policy-precision-state
                                                     {:beta 9.9 :beta-source :converged-posterior}}))
        "β is not smuggled into a mode that does not use it")
    (is (= {:tau-mode :selection-gain-only}
           (wm/variational-temperature-opts :selection-gain-only nil)))
    (is (= {:tau-mode :variational-beta-gamma
            :variational-beta 1.034342317
            :variational-beta-source :converged-posterior}
           (wm/variational-temperature-opts
            :variational-beta-gamma
            {:policy-precision-state {:beta 1.034342317
                                      :beta-source :converged-posterior}})))
    (testing "a HELD tick still supplies a β — the hold happened in the carry,
              and the source is what says so"
      (let [opts (wm/variational-temperature-opts
                  :variational-beta-gamma
                  {:policy-precision-state {:status :absent
                                            :reason :no-f-pi-readback
                                            :beta 1.0
                                            :beta-source :initial}})]
        (is (= 1.0 (:variational-beta opts)))
        (is (= :initial (:variational-beta-source opts)))
        (is (= 1.0 (policy/effective-temperature [0.0 1.0] 4.0 opts))
            "and it is used as τ, NOT crossed to 1/g = 0.25")))
    (testing "no beta state at all reaches effective-temperature's throw
              rather than a silent 1/g"
      (is (thrown? clojure.lang.ExceptionInfo
                   (policy/effective-temperature
                    [0.0 1.0] 4.0
                    (wm/variational-temperature-opts :variational-beta-gamma nil)))))))

;; ---------------------------------------------------------------------------
;; RUN9 / stage S4 — the F_π the live posterior is handed
;; ---------------------------------------------------------------------------

(defn- s4-scored-field []
  ;; The same 110-candidate real-shape fixture the readback and the beta carry
  ;; are measured on: 108 candidates score, one fails on a channel mismatch,
  ;; one left the tick. So it exercises incomplete coverage as a run meets it.
  (let [{:keys [previous current observation]} (real-shape-f-pi-fixture)
        scored (mapv (fn [index action]
                       (assoc action :controller-score (+ 0.25 (* 0.004 index))))
                     (range) current)]
    {:scored scored
     :f-pi-fields (wm/f-pi-dark-readback previous scored observation)}))

(deftest f-pi-posterior-opts-is-off-unless-the-flag-is-on-test
  (let [{:keys [scored f-pi-fields]} (s4-scored-field)]
    (with-redefs-fn {#'wm/*f-pi-posterior?* false}
      (fn []
        (let [opts (wm/f-pi-posterior-opts f-pi-fields scored)]
          (is (false? (:f-pi-policy-posterior? opts)))
          (is (nil? (:f-pi-values opts)))
          (is (= :flag-off (get-in opts [:f-pi-posterior :reason]))))))))

(deftest f-pi-posterior-opts-refuses-incomplete-coverage-test
  (testing "RUN9: F_π here runs ~61.6, so a candidate with no matched previous
            prediction would win the posterior by an artefact of the join.
            Complete-or-off, with the count of what was uncovered."
    (let [{:keys [scored f-pi-fields]} (s4-scored-field)]
      (with-redefs-fn {#'wm/*f-pi-posterior?* true}
        (fn []
          (let [opts (wm/f-pi-posterior-opts f-pi-fields scored)]
            (is (false? (:f-pi-policy-posterior? opts)))
            (is (nil? (:f-pi-values opts))
                "no partial vector escapes: there is nothing to misalign")
            (is (= :incomplete-coverage (get-in opts [:f-pi-posterior :reason])))
            (is (= 2 (get-in opts [:f-pi-posterior :uncovered-count])))
            (is (= 110 (get-in opts [:f-pi-posterior :candidate-count])))))))))

(deftest f-pi-posterior-opts-joins-by-identity-not-by-position-test
  (testing "RUN9: `select-action` receives `wm-admissible`, a can-execute?
            filter of the field the readback ran over, so the values must
            follow the action identity and not the index. Reversing the
            selector's field must reverse the vector."
    (let [{:keys [scored f-pi-fields]} (s4-scored-field)
          covered (filterv (fn [entry]
                             (let [id (#'wm/candidate-identity entry)]
                               (some (fn [result]
                                       (and (= id (:candidate-identity result))
                                            (= :present (:status result))))
                                     (vals (get-in f-pi-fields
                                                   [:f-pi-by-candidate-id
                                                    :by-candidate-id])))))
                           scored)]
      (with-redefs-fn {#'wm/*f-pi-posterior?* true}
        (fn []
          (let [opts (wm/f-pi-posterior-opts f-pi-fields covered)
                reversed (wm/f-pi-posterior-opts f-pi-fields (vec (reverse covered)))]
            (is (= 108 (count covered)))
            (is (true? (:f-pi-policy-posterior? opts)))
            (is (= 108 (count (:f-pi-values opts))))
            (is (every? number? (:f-pi-values opts)))
            (is (= :unscaled (:f-pi-scaling opts))
                "settled by friston2017 eq. 2.7; the live path offers no choice")
            (is (= :complete (get-in opts [:f-pi-posterior :coverage])))
            (is (= 0 (get-in opts [:f-pi-posterior :uncovered-count])))
            (is (= (vec (reverse (:f-pi-values opts))) (:f-pi-values reversed))
                "positional join would have returned the same vector twice")))))))

(deftest f-pi-posterior-opts-counts-an-ambiguous-identity-as-uncovered-test
  (with-redefs-fn {#'wm/*f-pi-posterior?* true}
    (fn []
      (let [fields {:f-pi-by-candidate-id
                    {:status :present
                     :by-candidate-id
                     {"rank/1" {:candidate-identity :c0 :status :present :value 1.0}
                      "rank/2" {:candidate-identity :c0 :status :present :value 2.0}}}}
            ranked [{:action {:type :open-mission :target "M-x"}}]]
        ;; both readback entries claim the same identity; picking one would be
        ;; a coin flip, so the tick declines rather than choosing
        (with-redefs-fn {#'wm/candidate-identity (constantly :c0)}
          (fn []
            (let [opts (wm/f-pi-posterior-opts fields ranked)]
              (is (false? (:f-pi-policy-posterior? opts)))
              (is (= :incomplete-coverage
                     (get-in opts [:f-pi-posterior :reason])))
              (is (= 1 (get-in opts [:f-pi-posterior :uncovered-count]))))))))))

(deftest f-pi-posterior-opts-carries-a-whole-tick-absence-reason-test
  (with-redefs-fn {#'wm/*f-pi-posterior?* true}
    (fn []
      (is (= :no-previous-trace-record
             (get-in (wm/f-pi-posterior-opts (wm/f-pi-dark-readback nil [] {})
                                             [{:action {:type :no-op}}])
                     [:f-pi-posterior :reason])))
      (is (= :no-f-pi-readback
             (get-in (wm/f-pi-posterior-opts nil [{:action {:type :no-op}}])
                     [:f-pi-posterior :reason]))))))

(deftest f-pi-posterior-preconditions-refuse-a-run-that-would-do-nothing-test
  (testing "RUN9: with FUTON_WM_FPI_POSTERIOR=1 and the chain off, every tick
            records :no-f-pi-readback and the posterior is the old one --
            indistinguishable in a record from a tick whose coverage was
            merely incomplete, so it throws at the top of the tick instead"
    (doseq [[fpi? details?] [[false true] [true false] [false false]]]
      (with-redefs-fn {#'wm/*f-pi-posterior?* true
                       #'wm/*f-pi-dark?* fpi?
                       #'trace/*persist-policy-trace-details?* details?}
        (fn []
          (is (thrown? clojure.lang.ExceptionInfo
                       (wm/f-pi-posterior-preconditions! :selection-gain-only))
              (str "missing flags " [fpi? details?] " must throw")))))
    (with-redefs-fn {#'wm/*f-pi-posterior?* true
                     #'wm/*f-pi-dark?* true
                     #'trace/*persist-policy-trace-details?* true}
      (fn []
        (is (nil? (wm/f-pi-posterior-preconditions! :selection-gain-only)))))
    (with-redefs-fn {#'wm/*f-pi-posterior?* true
                     #'wm/*f-pi-dark?* false
                     #'trace/*persist-policy-trace-details?* true}
      (fn []
        (is (nil? (wm/f-pi-posterior-preconditions! :variational-beta-gamma))
            "the tau mode runs the readback itself, so it satisfies the F_π half")))
    (with-redefs-fn {#'wm/*f-pi-posterior?* false
                     #'wm/*f-pi-dark?* false
                     #'trace/*persist-policy-trace-details?* false}
      (fn []
        (is (nil? (wm/f-pi-posterior-preconditions! :selection-gain-only))
            "flag off: no complaint about anything")))))

(deftest selection-clock-flag-off-preserves-the-trace-write-call-test
  (let [calls (atom [])
        result {:decision {:action {:type :advance-mission :target "M-next"}}}]
    (with-redefs-fn {#'wm/*clock-selection?* false
                     #'trace/write-trace!
                     (fn [& args]
                       (swap! calls conj args)
                       "/tmp/historical-trace.edn")
                     #'wm/record-selection-clock!
                     (fn [& _] (throw (ex-info "must not clock" {})))}
      (fn []
        (is (= "/tmp/historical-trace.edn"
               (#'wm/write-trace-and-clock! result "/tmp/traces")))
        (is (= [[result :dir "/tmp/traces"]] @calls)
            "default OFF makes the exact historical trace/write-trace! call")))))

(deftest selection-clock-mission-decision-retracts-old-and-puts-one-new-edge-test
  (let [posts (atom [])
        trace-record {:timestamp "2026-09-02T13:30:00Z" :run/id "run-selection-73"}
        decision {:action {:type :advance-mission :target "M-next"}}
        old-endpoint "futon3c-d/mission/old"
        new-endpoint "futon4-d/mission/next"
        get-stub
        (fn [url _opts]
          (cond
            (str/includes? url "code%2Fv05%2Fmission-doc")
            {:status 200
             :body (pr-str {:hyperedges
                            [{:hx/endpoints [new-endpoint]
                              :hx/props {:mission/id "M-next"}}]})}

            (str/includes? url "clock%2Fclocked-on")
            {:status 200
             :body (pr-str {:hyperedges
                            [{:hx/endpoints ["agent:war-machine" old-endpoint]
                              :hx/props {:agent-id "war-machine"}}]})}

            :else (throw (ex-info "unexpected GET" {:url url}))))
        post-stub
        (fn [url opts]
          (swap! posts conj {:url url :payload (json/parse-string (:body opts))})
          {:status 200 :body "{}"})]
    (with-redefs-fn {#'wm/*clock-selection?* true
                     #'http/get get-stub
                     #'http/post post-stub}
      (fn []
        (is (= {:ok? true :status 200}
               @(#'wm/record-selection-clock! trace-record decision)))
        (is (= 2 (count @posts)) "one retract followed by one put")
        (let [[retract put] (map :payload @posts)
              props (get put "hx/props")
              witness (get props "witness")]
          (is (= "retract" (get retract "hx/op")))
          (is (= ["agent:war-machine" old-endpoint]
                 (get retract "hx/endpoints")))
          (is (= "clock/clocked-on" (get put "hx/type")))
          (is (= ["agent:war-machine" new-endpoint]
                 (get put "hx/endpoints")))
          (is (= "war-machine" (get props "agent-id")))
          (is (integer? (get props "clocked-at-ms")))
          (is (= "selection-decision" (get witness "rule")))
          (is (= "run-selection-73" (get witness "source")))
          (is (= "M-old" (get witness "old-target")))
          (is (= "M-next" (get witness "new-target")))
          (is (= (get put "hx/valid-time")
                 (get props "clocked-at-ms"))))))))

(deftest selection-clock-non-mission-decisions-never-touch-http-test
  (with-redefs-fn {#'wm/*clock-selection?* true
                   #'http/get (fn [& _] (throw (ex-info "GET must not run" {})))
                   #'http/post (fn [& _] (throw (ex-info "POST must not run" {})))}
    (fn []
      (doseq [decision [{:action {:type :fire-pattern :target :pattern/x}}
                        {:action {:type :no-op}}
                        {:action {:type :open-mission}}]]
        (is (nil? (#'wm/record-selection-clock!
                   {:timestamp "2026-09-02T13:30:00Z"} decision)))))))

(deftest selection-clock-substrate-failure-does-not-escape-the-async-task-test
  (with-redefs-fn {#'wm/*clock-selection?* true
                   #'wm/selected-mission-endpoint
                   (constantly "futon4-d/mission/next")
                   #'wm/current-clock-targets (constantly [])
                   #'http/post (fn [& _] (throw (ex-info "substrate down" {})))}
    (fn []
      (is (= {:ok? false :reason :substrate-failure}
             @(#'wm/record-selection-clock!
                {:timestamp "2026-09-02T13:30:00Z"}
                {:action {:type :open-mission :target "M-next"}}))))))

(deftest mission-clock-focus-flag-off-does-no-http-test
  (with-redefs-fn {#'wm/*clock-focus?* false
                   #'wm/read-active-mission-clock
                   (fn [] (throw (ex-info "clock read must not run" {})))}
    (fn []
      (is (nil? (#'wm/load-active-mission))))))

(deftest mission-clock-focus-active-edge-is-typed-and-rendered-test
  (let [body (pr-str
              {:hyperedges
               [{:hx/endpoints ["agent:war-machine" "futon4-d/mission/next"]
                 :hx/props {:agent-id "war-machine"
                            :mission-id "M-next"
                            :clocked-at-ms 1700
                            :witness {:rule "selection-decision"}}}]})]
    (with-redefs-fn {#'wm/*clock-focus?* true
                     #'http/get (fn [_ _] {:status 200 :body body})}
      (fn []
        (let [active (#'wm/load-active-mission)
              markdown (#'wm/render-active-mission-line active)]
          (is (= {:endpoint "futon4-d/mission/next"
                  :mission-id "M-next"
                  :clocked-at-ms 1700
                  :witness-rule "selection-decision"}
                 active))
          (is (str/includes?
               markdown
               "**Active mission:** futon4-d/mission/next (M-next, clocked 1700, witness selection-decision)")))))))

(deftest mission-clock-focus-successful-empty-read-is-typed-absence-test
  (with-redefs-fn {#'wm/*clock-focus?* true
                   #'http/get (fn [_ _]
                                {:status 200 :body (pr-str {:hyperedges []})})}
    (fn []
      (is (= {:ok false :reason :no-active-clock}
             (#'wm/load-active-mission))))))

(deftest mission-clock-focus-unreachable-is-typed-and-nonfatal-test
  (with-redefs-fn {#'wm/*clock-focus?* true
                   #'http/get (fn [& _] (throw (ex-info "offline" {})))}
    (fn []
      (is (= {:ok false :reason :clock-unreadable}
             (#'wm/load-active-mission))))))

(deftest mission-clock-focus-cannot-change-ranked-output-test
  (let [ranked [{:rank 1 :action {:type :fire-pattern :target :pattern/a}}
                {:rank 2 :action {:type :advance-mission :target "M-next"}}]
        selected {:ranked-actions ranked :decision {:action (:action (first ranked))}}
        active {:endpoint "futon4-d/mission/next" :mission-id "M-next"
                :clocked-at-ms 1700 :witness-rule "selection-decision"}
        off (with-redefs-fn {#'wm/*clock-focus?* false}
              (fn [] (#'wm/carry-active-mission selected active)))
        on (with-redefs-fn {#'wm/*clock-focus?* true}
             (fn [] (#'wm/carry-active-mission selected active)))]
    (is (= (:ranked-actions off) (:ranked-actions on)))
    (is (= (:decision off) (:decision on)))
    (is (not (contains? off :active-mission)))
    (is (= active (:active-mission on)))))

(deftest strategic-selection-law-records-controller-rank-of-divergent-choice-test
  (let [head-action {:type :advance-mission :target "M-controller-head"}
        chosen-action {:type :advance-mission :target "M-live-choice"}
        controller-decision
        {:selection-law {:requested :controller-head :applied :controller-head}
         :controller-ranking [{:rank 1 :action head-action}
                              {:rank 124 :action chosen-action}]}
        law (#'wm/strategic-selection-law
             controller-decision {:action chosen-action}
             {:consulted-ranking :scheduler-habit})]
    (is (= :scheduler-habit (:consulted-ranking law)))
    (is (= 124 (:chosen-rank law)))
    (is (= 1 (:controller-head-rank law)))
    (is (true? (:moved-from-controller-head? law)))))

(deftest strategic-selection-law-records-controller-head-honestly-test
  ;; The ranking carries the ENRICHED action map; the selector returns a
  ;; MINIMAL one. Identity is type+target (policy-key), never map equality —
  ;; run 2 of 2026-09-02 stamped the controller head itself as moved because
  ;; the maps differed on enrichment keys.
  (let [enriched-head {:type :advance-mission :target "M-controller-head"
                       :rationale "enriched by the judge" :weight 1.0
                       :mission-value-factor 0.59}
        minimal-head {:type :advance-mission :target "M-controller-head"}
        controller-decision
        {:selection-law {:requested :controller-head :applied :controller-head}
         :controller-ranking [{:rank 1 :action enriched-head}
                              {:rank 2 :action {:type :advance-mission
                                               :target "M-other"}}]}
        law (#'wm/strategic-selection-law
             controller-decision {:action minimal-head}
             {:consulted-ranking :controller})]
    (is (= :controller (:consulted-ranking law)))
    (is (= 1 (:chosen-rank law)))
    (is (false? (:moved-from-controller-head? law)))))

(deftest strategic-selection-law-types-a-choice-outside-the-ranking-test
  (let [controller-decision
        {:selection-law {}
         :controller-ranking [{:rank 1 :action {:type :advance-mission
                                                :target "M-head"}}]}
        law (#'wm/strategic-selection-law
             controller-decision
             {:action {:type :repair-machine-failure :target "R-x"}}
             {:consulted-ranking :scheduler-habit})]
    (is (= :not-in-controller-ranking (:chosen-rank law)))
    (is (true? (:moved-from-controller-head? law)))))
