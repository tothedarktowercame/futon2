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
            [futon2.aif.disposition-risk :as disposition]
            [checks.disposition-kernel :as checkpoint-kernel]
            [futon2.aif.ruled-outcome-c :as ruled]
            [futon2.aif.enumeration-completeness :as ec]
            [futon2.aif.free-energy :as free-energy]
            [futon2.aif.mission-c :as mc]
            [futon2.aif.mission-epistemic-value :as mev]
            [futon2.aif.mission-gauges :as gauges]
            [futon2.aif.observation :as observation]
            [futon2.aif.policy :as policy]
            [futon2.aif.preferences :as pref]
            [futon2.aif.sorry-registry :as sorry-registry]
            [futon2.aif.selection-rationale :as selection-rationale]
            [futon2.aif.trace :as trace]
            [futon2.report.war-machine :as wm])
  (:import (java.io PushbackReader StringReader)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

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
          ;; U44: the index carries the phase off the hyperedge now; it is
          ;; read only when the declared input is on, and the enrichment two
          ;; lines below is the default path, so the factors are unchanged.
          (is (= {"alpha" {:endpoint "repo-d/mission/alpha"
                            :operator-gates []
                            :phase "head"}
                  "beta" {:endpoint "repo-d/mission/beta"
                           :operator-gates []
                           :phase "derive"}}
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

(deftest configured-fold-options-preserve-absence
  (let [base {:time-pressure 0.25 :horizon-steps 3}
        absent (#'wm/configured-fold-efe-opts base {})
        calls (atom [])
        config {:ruled-outcome-c-enabled? true
                :seeded-c ruled/seeded-c
                :disposition-kernel (fn [observation]
                                      (swap! calls conj observation)
                                      (assoc (zipmap (:support ruled/seeded-c)
                                                    (repeat 0.0))
                                             :agent-unavailable 1.0))}
        supplied (#'wm/configured-fold-efe-opts base config)]
    (is (= base absent))
    (is (= config (select-keys supplied (keys config))))
    (is (= (pr-str (efe/compute-efe {} {:type :no-op} base))
           (pr-str (efe/compute-efe {} {:type :no-op} absent))))
    (is (pos? (:G-ruled-outcome-c
               (efe/compute-efe {} {:type :no-op} supplied))))
    (is (= 1 (count @calls)))))

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

(deftest selection-clock-flag-off-still-writes-the-trace-and-the-rationale-test
  ;; RE4 CHANGED THIS CALL SHAPE DELIBERATELY, and this test now pins the new
  ;; one rather than the old. `write-trace-and-clock!` always asks for
  ;; `:return-record? true`, because the rationale is built from the record that
  ;; was written, and it always emits a rationale -- the clock flag guards the
  ;; substrate edge only. What has NOT changed is what lands on disk:
  ;; `:return-record?` alters the RETURN VALUE of `trace/write-trace!` and
  ;; nothing it writes (trace.clj:712-733), which the byte control below
  ;; measures rather than asserts.
  (let [calls (atom [])
        rationales (atom [])
        result {:decision {:action {:type :advance-mission :target "M-next"}}}]
    (with-redefs-fn {#'wm/*clock-selection?* false
                     #'trace/write-trace!
                     (fn [& args]
                       (swap! calls conj args)
                       {:path "/tmp/historical-trace.edn" :record {:written :record}})
                     #'selection-rationale/emit!
                     (fn [record opts]
                       (swap! rationales conj [record opts])
                       "/tmp/traces/rationale/x.edn")
                     #'wm/record-selection-clock!
                     (fn [& _] (throw (ex-info "must not clock" {})))}
      (fn []
        (is (= "/tmp/historical-trace.edn"
               (#'wm/write-trace-and-clock! result "/tmp/traces"))
            "the seam still returns the trace path")
        (is (= [[result :dir "/tmp/traces" :return-record? true]] @calls))
        (is (= [[{:written :record}
                 {:dir "/tmp/traces/rationale"
                  :trace-path "/tmp/historical-trace.edn"}]]
               @rationales)
            "the rationale is emitted from the written record, beside the trace")))))

(deftest return-record-option-does-not-change-the-persisted-trace-bytes-test
  ;; The control for the claim above. Two writes of the same judge output, one
  ;; with the historical argument list and one with RE4's, compared byte for
  ;; byte. Without this the docstring's "persisted bytes are unchanged" would be
  ;; an assertion about code someone read once.
  (let [judge-output {:belief {} :observation {} :free-energy {}
                      :ranked-actions [{:action {:type :no-op}
                                        :controller-score 0.0 :rank 1}]
                      :decision {:action {:type :no-op}}
                      :mode :multiplied}
        dir-a (str (java.nio.file.Files/createTempDirectory
                    "wm-trace-bytes-a" (into-array java.nio.file.attribute.FileAttribute [])))
        dir-b (str (java.nio.file.Files/createTempDirectory
                    "wm-trace-bytes-b" (into-array java.nio.file.attribute.FileAttribute [])))
        fixed-ts "2026-09-04T00:00:00Z"
        stable (fn [path]
                 (-> (slurp path)
                     (clojure.string/replace
                      #":timestamp \"[^\"]+\"" (str ":timestamp \"" fixed-ts "\""))
                     (clojure.string/replace
                      #":at \"[^\"]+\"" (str ":at \"" fixed-ts "\""))))
        path-a (trace/write-trace! judge-output :dir dir-a :date-str "2026-09-04")
        written-b (trace/write-trace! judge-output :dir dir-b :date-str "2026-09-04"
                                      :return-record? true)]
    (is (string? path-a))
    (is (map? written-b))
    (is (= (stable path-a) (stable (:path written-b)))
        "the option changes the return value only")))

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

;; ---------------------------------------------------------------------------
;; I6 (harvested refusal class prediction-error-v1--source-field-missing):
;; `:sorry-count-norm`'s source. The producer's refusal was correct — nothing
;; wrote `[:graph :summary :total-sorrys]`, the path `observation.clj:59`
;; requires — so the caller (the graph scan) is what changed. The census comes
;; from the same registry that already supplies `:address-sorry` candidates.
;; ---------------------------------------------------------------------------

(deftest open-sorry-census-counts-the-live-registry-test
  (testing "the census is the registry's own open count, not a constant"
    (let [n (#'wm/open-sorry-census)]
      (is (nat-int? n))
      (is (= (count (sorry-registry/open-sorrys)) n)
          "same population the :address-sorry proposer enumerates")))
  (testing "an unreadable registry is nil, never a measured zero"
    (with-redefs [sorry-registry/open-sorrys
                  (fn [& _] (throw (ex-info "registry unreadable" {})))]
      (is (nil? (#'wm/open-sorry-census))))))

(deftest graph-summary-total-sorrys-is-present-only-test
  (testing "a census present puts the key in, including a measured zero"
    (is (= 3 (:total-sorrys (#'wm/graph-summary [] [] [] 3))))
    (is (= 0 (:total-sorrys (#'wm/graph-summary [] [] [] 0)))
        "a registry that read and holds no open sorrys is a reading"))
  (testing "a nil census omits the key rather than writing 0"
    (let [s (#'wm/graph-summary [] [] [] nil)]
      (is (not (contains? s :total-sorrys)))
      (is (= #{:total-repos :active-repos :coupling-edges :ticks-firing}
             (set (keys s)))
          "the other four summary keys are unchanged")))
  (testing "the other four keys still count what they counted"
    (let [s (#'wm/graph-summary [{:active? true} {:active? false}]
                                [:e1 :e2 :e3]
                                [{:fired? true} {:fired? false}]
                                1)]
      (is (= 2 (:total-repos s)))
      (is (= 1 (:active-repos s)))
      (is (= 3 (:coupling-edges s)))
      (is (= 1 (:ticks-firing s))))))

(deftest sorry-count-norm-observed-when-the-census-lands-test
  (testing "the harvested refusal is gone once the scan supplies the path"
    (let [scan {:graph {:summary (#'wm/graph-summary [] [] [] 3)}}
          obs (observation/observe scan)
          triple (free-energy/channel-prediction-error
                  obs :sorry-count-norm {:mean 0.25 :variance 0.04})]
      (is (= :observed (:variant (observation/observation-status
                                  obs :sorry-count-norm))))
      (is (= 0.3 (:sorry-count-norm obs)) "3 open sorrys / 10")
      (is (= :present (:status triple)))
      (is (= 0.3 (:observed triple)))
      (is (not (contains? triple :reason)))))
  (testing "a nil census reproduces sweep 132's record exactly"
    (let [scan {:graph {:summary (#'wm/graph-summary [] [] [] nil)}}
          obs (observation/observe scan)
          triple (free-energy/channel-prediction-error
                  obs :sorry-count-norm {:mean 0.25 :variance 0.04})]
      (is (= :absent (:status triple)))
      (is (= :observed (:absent-member triple)))
      (is (= :source-field-missing (:reason triple)))
      (is (= [[:graph :summary :total-sorrys]] (:paths triple)))
      (is (not (contains? triple :observed))
          "no fabricated observation for an unreadable registry"))))

;; ---------------------------------------------------------------------------
;; U11 (d) — the C_mis binding. Records, consults nothing.

(def ^:private u11-ranked
  "Two mission actions and two non-mission ones. The clocked mission's own
   candidate carries the `:mission-path` the readback falls back to when no
   ingest is declared for it — the shape the 2026-09-02 records have."
  [{:rank 1 :action {:type :advance-mission :target "M-clocked"
                     :mission-path "holes/missions/M-zaif-harness-v1.md"}}
   {:rank 2 :action {:type :advance-mission :target "M-other"
                     :mission-path "holes/missions/M-other.md"}}
   {:rank 3 :action {:type :address-sorry :target :sorry/a}}
   {:rank 4 :action {:type :no-op}}])

(def ^:private u11-clocked
  {:endpoint "futon2-d/mission/clocked" :mission-id "M-clocked"
   :clocked-at-ms 1788357629629 :witness-rule "selection-decision"})

(deftest mission-c-flag-off-leaves-the-judgement-byte-identical-test
  (let [selected {:ranked-actions u11-ranked
                  :decision {:action (:action (first u11-ranked))}
                  :active-mission u11-clocked}
        fields {:version 1 :status :measured :risk-mis-per-criterion []}
        off (with-redefs-fn {#'wm/*mission-c?* false}
              (fn [] (#'wm/carry-mission-c selected fields)))
        on (with-redefs-fn {#'wm/*mission-c?* true}
             (fn [] (#'wm/carry-mission-c selected fields)))]
    (is (= selected off) "flag off: the judgement is the same map, key for key")
    (is (not (contains? off :mission-c)))
    (is (= fields (:mission-c on)))
    (is (= (:ranked-actions off) (:ranked-actions on)))
    (is (= (:decision off) (:decision on))
        "the readback is attached after the decision and cannot move it")))

(deftest mission-c-without-a-clocked-mission-is-a-typed-absence-test
  (testing "the S4 focus read's own typed absence is carried through, not flattened"
    (let [r (#'wm/mission-c-readback {:ok false :reason :no-active-clock}
                                     u11-ranked {})]
      (is (= :absent (:status r)))
      (is (= :no-active-clock (:reason r)))
      (is (not (contains? r :risk-mis-per-criterion)))))
  (testing "and a focus read that never ran at all"
    (is (= :no-clocked-mission (:reason (#'wm/mission-c-readback nil u11-ranked {}))))))

(deftest mission-c-records-typed-unmeasurable-criteria-on-a-real-fixture-test
  (testing "the clocked mission resolves to its declared ingest, and every
            criterion in it is unmeasurable — loudly, per criterion"
    (let [clocked (assoc u11-clocked :mission-id "M-zaif-harness-v1")
          ranked (assoc-in u11-ranked [0 :action :target] "M-zaif-harness-v1")
          r (#'wm/mission-c-readback clocked ranked {:sorry-count-norm 0.0})]
      (is (str/ends-with? (:criteria-source r) "S4-identify-ingest.edn")
          "the declared ingest wins over the candidate's :mission-path")
      (is (= :ingest-edn (:criteria-shape r)))
      (is (= 3 (:criterion-count r)))
      (is (= 0 (:measurable-count r)))
      (is (= :absent (:status r)))
      (is (= :no-measurable-criteria (:reason r)))
      (is (not (contains? r :risk-mis-per-criterion))
          "present-only: no key rather than a nil standing in for numbers")
      (is (= 3 (count (:unmeasurable r))))
      (is (every? #(re-find #":\d+$" (:source %)) (:unmeasurable r))
          "each typed record points at the line it came from")
      (is (not (contains? r :per-mission-action))
          "no per-action numbers when there is no number to record"))))

(deftest mission-c-gauge-producers-close-the-u21-gap-test
  (testing "U42: with the gauge producers' measured values merged into the
            observation the READBACK reads, M-zaif-harness-v1's three criteria
            stop at :undeclared-observable and score"
    (let [clocked (assoc u11-clocked :mission-id "M-zaif-harness-v1")
          ranked (assoc-in u11-ranked [0 :action :target] "M-zaif-harness-v1")
          tick-observation {:sorry-count-norm 0.0}
          produced (gauges/reading)
          before (#'wm/mission-c-readback clocked ranked tick-observation)
          after (#'wm/mission-c-readback clocked ranked
                                         (merge tick-observation
                                                (:observables produced)))]
      (is (= [:undeclared-observable :undeclared-observable :undeclared-observable]
             (mapv :reason (:unmeasurable before)))
          "the state U21 measured and this row was minted from")
      (is (= 3 (:measurable-count after)))
      (is (= :measured (:status after)))
      ;; LIVE PIN, and the whole point of the row: this number comes from the
      ;; tracked artifacts the producers read, not from a supplied probe.
      ;; U21's probe reported 3.333378732232873 by ASSUMING
      ;; :registry-gap-list-present is 1.0; the registry declares no gap-list
      ;; pointer, so it measures 0.0 and a second criterion is unsatisfied.
      (is (= [6.666712065566529]
             (vec (distinct (map :risk-mis (:per-mission-action after))))))
      (is (= {:worklist-acceptance-state 1.0
              :reporting-gate-test-result 0.0
              :registry-gap-list-present 0.0}
             (:observables produced)))
      (is (not (contains? tick-observation :worklist-acceptance-state))
          "the tick's own observation is not what was merged into -- the merge
           is local to the readback's argument, so no channel, weight, G term,
           admissibility verdict or selector can see a gauge observable"))))

(deftest mission-c-falls-back-to-the-candidates-own-mission-doc-test
  (let [r (#'wm/mission-c-readback u11-clocked u11-ranked {})]
    (is (= "holes/missions/M-zaif-harness-v1.md" (:criteria-source r))
        "no declared ingest for M-clocked, so the clocked candidate's path answers")
    ;; :markdown-inline-paragraph until futon2 161ac09, which added the
    ;; `## Completion criteria` section J6's ruling asked for; the reader tries
    ;; the section shape first. Same three criteria either way.
    (is (= :markdown-numbered-list (:criteria-shape r)))
    (is (= 3 (:criterion-count r)))
    (is (= 2 (:mission-action-count r)))
    (is (= 2 (:non-mission-action-count r)))
    (is (not (contains? r :declared-gauges))
        "gauges are declared per mission id, and M-clocked has none")))

(def ^:private u12-recorded-source-sha256
  "The two criteria sources U12's replay read, with the digests it recorded:
   `holes/labs/wm-contract/runs/U12-c-mis-falsifier/measurements.edn`
   `:criteria-source-sha256`. These are the expected values U15's :statement
   names. A FAILURE HERE IS NOT A CODE FAULT: it says the criteria file has
   been edited since U12 measured it, so U12's numbers no longer describe the
   file at that path. The repair is to re-measure and update both the artifact
   and this pin together, never this pin alone."
  {"holes/labs/zaif-harness/runs/S4-identify-ingest.edn"
   "b85cb1dade5acecccfcb5188106a82908aa61a70f60b1f4b9f6af46873a5f2a9"
   "holes/missions/M-wm-aif-policy-grain-compliance.md"
   "a770d0005af53aa483f2fe2094d363c502eee3c7842d3663af01cf37353ef08e"})

(deftest mission-c-readback-hashes-the-criteria-source-test
  (testing "U15. U12 clause (a) passed determinism only as stated: the readback
            reproduces from its arguments, but :criteria-source is a PATH into
            a mutable file. The digest is what a replay compares."
    (testing "the declared ingest, replayed against U12's recorded digest"
      (let [clocked (assoc u11-clocked :mission-id "M-zaif-harness-v1")
            ranked (assoc-in u11-ranked [0 :action :target] "M-zaif-harness-v1")
            r (#'wm/mission-c-readback clocked ranked {:sorry-count-norm 0.0})]
        (is (str/ends-with? (:criteria-source r) "S4-identify-ingest.edn"))
        (is (= (get u12-recorded-source-sha256
                    "holes/labs/zaif-harness/runs/S4-identify-ingest.edn")
               (:criteria-source-sha256 r)))))
    (testing "the mission-path fallback, the other source U12 read, and a parse
              that produces NO criteria still records what it read"
      (let [doc "holes/missions/M-wm-aif-policy-grain-compliance.md"
            ranked (assoc-in u11-ranked [0 :action :mission-path] doc)
            r (#'wm/mission-c-readback u11-clocked ranked {})]
        (is (= doc (:criteria-source r)))
        (is (= :no-completion-criteria-section (:criteria-reason r)))
        (is (= (get u12-recorded-source-sha256 doc) (:criteria-source-sha256 r)))))
    (testing "a source that is not there is a typed absence with NO digest --
              the field never carries a nil where a hash belongs"
      (let [ranked (assoc-in u11-ranked [0 :action :mission-path]
                             "holes/missions/M-does-not-exist.md")
            r (#'wm/mission-c-readback u11-clocked ranked {})]
        (is (= :source-not-found (:criteria-reason r)))
        (is (not (contains? r :criteria-source-sha256)))))))

(deftest mission-c-with-no-criteria-source-is-typed-test
  (let [r (#'wm/mission-c-readback u11-clocked
                                   [{:rank 1 :action {:type :no-op}}] {})]
    (is (= :absent (:status r)))
    (is (= :no-criteria-source (:reason r)))
    (is (not (contains? r :criteria-source-sha256))
        "no path was resolved, so there are no bytes to have hashed")))

(deftest mission-c-records-risk-per-mission-action-with-traceable-numbers-test
  (testing "a mission whose criteria DO declare observables: the numeric path,
            end to end, with every number derivable from the record"
    (let [dir (str (System/getProperty "java.io.tmpdir") "/u11-" (System/nanoTime))
          _ (.mkdirs (io/file dir))
          path (str dir "/S4-identify-ingest.edn")
          _ (spit path (pr-str
                        {:ingest/mission "M-planted"
                         :preferences/c
                         [{:criterion :sorries-cleared :observable :sorry-count-norm}
                          {:criterion :mission-healthy :observable :mission-health
                           :spec [0.5 1.0]}
                          {:criterion :prose-only :carrier "a human reads the worklist"}]}))
          ;; DOUBLES throughout, which is what R2's observation vector is.
          ;; Under J6's ruled arm the double 0.0 reaches the unsatisfied pole;
          ;; before the ruling it read as the target, so this fixture had to use
          ;; the long 0 to score an unmet criterion at all.
          observation {:sorry-count-norm 0.0 :mission-health 0.0234}
          r (with-redefs-fn {#'wm/mission-c-criteria-sources {"M-clocked" path}}
              (fn [] (#'wm/mission-c-readback u11-clocked u11-ranked observation)))
          per-criterion (into {} (map (juxt :criterion identity))
                              (:risk-mis-per-criterion r))]
      (is (= :measured (:status r)))
      (is (= 3 (:criterion-count r)))
      (is (= 2 (:measurable-count r)))
      (is (= :uniform-over-measurable (:weight-basis r)))
      (is (= 1 (count (:unmeasurable r)))
          "the prose criterion is still recorded, typed, alongside the numbers")
      (is (= :unresolved-observable (:reason (first (:unmeasurable r)))))
      (testing "every recorded number reproduces from pref/ directly"
        (doseq [[criterion observable spec] [[:sorries-cleared :sorry-count-norm {:becomes 1}]
                                             [:mission-healthy :mission-health [0.5 1.0]]]]
          (let [e (get per-criterion criterion)
                d (pref/c-distribution spec :temperature (:temperature r))
                value (get observation observable)]
            (is (= observable (:observable e)))
            (is (= value (:value e)))
            (is (< (Math/abs (- (:log-c (pref/log-preference-under
                                         d value mc/default-outcome-semantics))
                                (:log-c e)))
                   1e-9))
            (is (< (Math/abs (- 0.5 (:weight e))) 1e-9))
            ;; U17: the recorded contribution is w times the point-mass
            ;; DIVERGENCE, which for :mission-healthy's [0.5 1.0] band differs
            ;; from w times the surprisal by ln Z = -0.5119. :surprisal keeps the
            ;; unshifted per-criterion number on the record.
            (is (< (Math/abs (- (- (:log-c (pref/log-preference-under
                                            d value mc/default-outcome-semantics)))
                                (:surprisal e)))
                   1e-9))
            (is (< (Math/abs (- (* 0.5 (- (- (:log-c (pref/log-preference-under
                                                      d value mc/default-outcome-semantics)))
                                          (pref/point-mass-divergence-shift d)))
                                (:contribution e)))
                   1e-9)))))
      (testing "and the record names the reading that produced the numbers"
        (is (= :declared-binarization (:outcome-semantics r))))
      (testing "one record per MISSION action, scoped, and no record for the others"
        (is (= 2 (count (:per-mission-action r))))
        (is (= 2 (:mission-action-count r)))
        (is (= 2 (:non-mission-action-count r)))
        (is (= [:clocked-mission :other-mission]
               (mapv :scope (:per-mission-action r))))
        (is (= [1 2] (mapv :rank (:per-mission-action r)))))
      (testing "v0's status-quo forward model is action-INSENSITIVE, and the
                record says so rather than leaving it to be re-derived"
        (is (= 1 (get-in r [:action-sensitivity :distinct-risk-values])))
        (is (true? (get-in r [:action-sensitivity :constant?]))))
      (testing "the per-action risk is the sum of the recorded contributions"
        (let [total (reduce + (map :contribution (:risk-mis-per-criterion r)))]
          (is (every? #(< (Math/abs (- total (:risk-mis %))) 1e-9)
                      (:per-mission-action r)))))
      (io/delete-file path true)
      (io/delete-file dir true))))

;; ---------------------------------------------------------------------------
;; U18 (from J6) — the DECLARED GAUGES, and the readback record after the ruling.

(deftest mission-c-declared-gauges-are-one-binding-per-criterion-test
  (testing "the same three criteria under both ids they have: the ingest names
            them, the mission document does not and the reader numbers them"
    (let [g (get wm/mission-c-declared-gauges "M-zaif-harness-v1")]
      (is (= #{:u-rows-green :reporting-gate-holds :honest-gap-list-published
               :criterion-1 :criterion-2 :criterion-3}
             (set (keys g))))
      (is (= 3 (count (distinct (map :observable (vals g)))))
          "six keys, three observables — the pairs are the same criterion twice")
      (is (= [(:observable (:u-rows-green g))
              (:observable (:reporting-gate-holds g))
              (:observable (:honest-gap-list-published g))]
             [(:observable (:criterion-1 g))
              (:observable (:criterion-2 g))
              (:observable (:criterion-3 g))])
          "and the doc's positional order is the ingest's order, which is what
           futon2 161ac09 ported")
      (testing "every gauge declares its observable BINARY, which is what J6's
                arm needs instead of a threshold, and says where it was declared"
        (is (every? #(= {:becomes 1 :observable-kind :binary} (:spec %)) (vals g)))
        (is (every? #(str/includes? (:declared-in %) "mission-c-declared-gauges")
                    (vals g)))
        (is (every? #(string? (:gauge %)) (vals g))))
      (testing "and nothing in the live observation vocabulary supplies them yet,
                which is why they read as a missing PRODUCER and not as prose"
        (let [r (#'wm/mission-c-readback
                 (assoc u11-clocked :mission-id "M-zaif-harness-v1")
                 (assoc-in u11-ranked [0 :action :target] "M-zaif-harness-v1")
                 {:sorry-count-norm 0.0})]
          (is (= 0 (:measurable-count r)))
          (is (= #{:undeclared-observable} (set (map :reason (:unmeasurable r)))))
          (is (= 6 (count (:declared-gauges r)))
              "and the record says which bindings it was read under -- all six
               keys, because the record should show the table as declared and
               not a subset filtered to whichever id vocabulary answered"))))))

(deftest mission-c-reads-the-zaif-criteria-3-of-3-with-the-gauges-supplied-test
  (testing "J6 (d): with the declared gauge observables supplied, the mission
            document's three criteria are measurable and the mission scores"
    (let [observation {:worklist-acceptance-state 0.0
                       :reporting-gate-test-result 0.0
                       :registry-gap-list-present 1.0}
          clocked (assoc u11-clocked :mission-id "M-zaif-harness-v1")
          ranked (-> u11-ranked
                     (assoc-in [0 :action :target] "M-zaif-harness-v1")
                     (assoc-in [0 :action :mission-path]
                               "holes/missions/M-zaif-harness-v1.md"))
          ;; the DOC route: the declared ingest is the criteria authority for
          ;; this mission on a live tick, and it is the doc that carries the
          ;; criteria Joe ported, so this reads the fallback path deliberately.
          r (with-redefs-fn {#'wm/mission-c-criteria-sources {}}
              (fn [] (#'wm/mission-c-readback clocked ranked observation)))]
      (is (= "holes/missions/M-zaif-harness-v1.md" (:criteria-source r)))
      (is (= 3 (:criterion-count r)))
      (is (= 3 (:measurable-count r)))
      (is (= :measured (:status r)))
      (is (= :declared-binarization (:outcome-semantics r)))
      (is (= [] (:unmeasurable r)))
      (is (= [0 0 1] (mapv :outcome (:risk-mis-per-criterion r)))
          "two criteria unmet, the third met — the first mission-grain C reading
           on this machine that is not a typed absence")
      (testing "PROVENANCE is on the record: the bindings these numbers were read
                under are the declared gauges, named on the readback, and not
                anything the mission document itself declared"
        (is (= #{:worklist-acceptance-state :reporting-gate-test-result
                 :registry-gap-list-present}
               (set (vals (:declared-gauges r)))))
        (is (= (set (map :observable (:risk-mis-per-criterion r)))
               (set (vals (:declared-gauges r)))))
        (is (= #{:declared-gauge}
               (set (map :observable-source
                         (:criteria (mc/read-criteria
                                     "holes/missions/M-zaif-harness-v1.md"
                                     :observables observation
                                     :mission "M-zaif-harness-v1"
                                     :gauges (get wm/mission-c-declared-gauges
                                                  "M-zaif-harness-v1"))))))
            "and every criterion row the reader built says so per criterion"))))
  (testing "a reading the ruled arm cannot interpret refuses, and the record
            names which criterion refused rather than only that there is no
            number"
    (let [observation {:worklist-acceptance-state 0.75
                       :reporting-gate-test-result 0.0
                       :registry-gap-list-present 1.0}
          clocked (assoc u11-clocked :mission-id "M-zaif-harness-v1")
          ranked (-> u11-ranked
                     (assoc-in [0 :action :target] "M-zaif-harness-v1")
                     (assoc-in [0 :action :mission-path]
                               "holes/missions/M-zaif-harness-v1.md"))
          r (with-redefs-fn {#'wm/mission-c-criteria-sources {}}
              (fn [] (#'wm/mission-c-readback clocked ranked observation)))]
      (is (= :absent (:status r)))
      (is (= :unread-outcome (:reason r)))
      (is (not (contains? r :risk-mis-per-criterion)))
      (is (= [:criterion-1] (mapv :criterion (:refused-outcomes r))))
      (is (= [:non-binary-value-on-binary-observable]
             (mapv :reason (:refused-outcomes r)))))))

;; ---------------------------------------------------------------------------
;; U28 — the same seam, the other outcome. M-expressions-of-interest's six
;; criteria produced no bindings, and the declaration says so per criterion
;; rather than leaving them looking un-looked-at.

(def ^:private u28-eoi-doc
  "Outside this repo, so its absence skips rather than fails."
  "/home/joe/code/futon5a/holes/missions/M-expressions-of-interest.md")

(deftest mission-c-declared-gauges-carry-the-eoi-producer-bindings-test
  (let [g (get wm/mission-c-declared-gauges "M-expressions-of-interest")]
    (testing "one entry per criterion, keyed the way the markdown reader numbers
              them, because the mission document names none of its criteria"
      (is (= #{:criterion-1 :criterion-2 :criterion-3
               :criterion-4 :criterion-5 :criterion-6}
             (set (keys g)))))
    (testing "each criterion names its U79 producer while retaining the input inventory"
      (is (= 6 (count (set (map :observable (vals g))))))
      (is (every? keyword? (map :observable (vals g))))
      (is (every? #(string? (:would-need (:no-producer %))) (vals g)))
      (is (every? #(string? (:because (:no-producer %))) (vals g)))
      (is (every? #(str/includes? (:because (:no-producer %))
                                  "M-expressions-of-interest.md:")
                  (vals g))
          "each finding carries the file:line it rests on")
      (is (every? #(str/includes? (:declared-in %) "mission-c-declared-gauges")
                  (vals g))))))

(deftest mission-c-reads-the-eoi-criteria-0-of-6-with-the-gauges-supplied-test
  (if-not (.exists (io/file u28-eoi-doc))
    (is true (str "fixture absent, skipped: " u28-eoi-doc))
    (let [clocked (assoc u11-clocked :mission-id "M-expressions-of-interest")
          ranked (-> u11-ranked
                     (assoc-in [0 :action :target] "M-expressions-of-interest")
                     (assoc-in [0 :action :mission-path] u28-eoi-doc))
          r (#'wm/mission-c-readback clocked ranked {:sorry-count-norm 0.0})]
      (is (= 6 (:criterion-count r)))
      (is (= 0 (:measurable-count r))
          "the honest number: declaring that nothing reads a criterion is not a
           reading of it")
      (is (= #{:undeclared-observable} (set (map :reason (:unmeasurable r)))))
      (is (= :absent (:status r)))
      (is (= :no-measurable-criteria (:reason r)))
      (is (not (contains? r :risk-mis-per-criterion)))
      (testing "and the record shows the seam was consulted rather than silent"
        (is (= 6 (count (:declared-gauges r))))
        (is (= 6 (count (set (vals (:declared-gauges r)))))
            "each criterion is bound to its own producer observable")))))

;; ---------------------------------------------------------------------------
;; U21 (from zaif S4) — selection -> clocking, the same-tick half. The focus is
;; a projection of THIS tick's decision; it mints no edge and consults none.

(def ^:private u21-decision
  {:action {:type :advance-mission :target "M-zaif-harness-v1"
            :mission-path "holes/missions/M-zaif-harness-v1.md"}})

(def ^:private u21-stale-clock
  "What the durable read returns on a tick that selects a DIFFERENT mission —
   the previous tick's selection, which is the lag this row repairs. The shape
   is the 2026-09-02 record 4abad68c's own :active-mission."
  {:endpoint "futon2-d/mission/wm-aif-policy-grain-compliance"
   :mission-id "M-wm-aif-policy-grain-compliance"
   :clocked-at-ms 1788356843859 :witness-rule "selection-decision"})

(deftest selection-focus-flag-off-is-the-durable-read-unchanged-test
  (testing "flag off, the readback's input is the S4 focus read, identical"
    (with-redefs-fn {#'wm/*selection-focus?* false}
      (fn []
        (is (= u21-stale-clock
               (#'wm/tick-mission-focus u21-stale-clock u21-decision)))
        (is (nil? (#'wm/tick-mission-focus nil u21-decision)))
        (is (= {:ok false :reason :no-active-clock}
               (#'wm/tick-mission-focus {:ok false :reason :no-active-clock}
                                        u21-decision))))))
  (testing "and the projection adds no field, so the judgement is byte-identical"
    (let [judgement {:ranked-actions u11-ranked :decision u21-decision}
          off (with-redefs-fn {#'wm/*selection-focus?* false}
                (fn [] (#'wm/carry-mission-focus judgement {:mission-id "M-x"})))]
      (is (= judgement off))
      (is (not (contains? off :mission-focus))))))

(deftest selection-focus-makes-this-tick-s-selection-the-focus-test
  (with-redefs-fn {#'wm/*selection-focus?* true}
    (fn []
      (let [focus (#'wm/tick-mission-focus u21-stale-clock u21-decision)]
        (is (= "M-zaif-harness-v1" (:mission-id focus))
            "the mission this tick selected, not the one it read")
        (is (= :this-tick-selection (:origin focus)))
        (is (= "holes/missions/M-zaif-harness-v1.md" (:mission-path focus)))
        (is (= :advance-mission (:action-type focus)))
        (is (false? (:agrees-with-durable? focus))
            "the lag between selection and clock is ON the record")
        (is (= "M-wm-aif-policy-grain-compliance"
               (get-in focus [:durable :mission-id]))
            "and what the durable read said is kept beside it, not overwritten"))
      (testing "a tick whose selection the clock already agrees with says so"
        (let [focus (#'wm/tick-mission-focus
                     (assoc u21-stale-clock :mission-id "M-zaif-harness-v1")
                     u21-decision)]
          (is (true? (:agrees-with-durable? focus))))))))

(deftest selection-focus-on-a-non-mission-decision-falls-back-test
  (testing "no selected mission to focus on: the durable read, tagged, never
            a fabricated focus"
    (with-redefs-fn {#'wm/*selection-focus?* true}
      (fn []
        (let [focus (#'wm/tick-mission-focus
                     u21-stale-clock
                     {:action {:type :address-sorry :target :sorry/a}})]
          (is (= :durable-clock (:origin focus)))
          (is (= "M-wm-aif-policy-grain-compliance" (:mission-id focus))))
        (testing "and a mission action with no target is not a focus either"
          (let [focus (#'wm/tick-mission-focus
                       nil {:action {:type :advance-mission}})]
            (is (= :durable-clock (:origin focus)))
            (is (nil? (:mission-id focus)))))
        (testing "the typed absence of the focus read survives the fallback"
          (let [focus (#'wm/tick-mission-focus
                       {:ok false :reason :no-active-clock}
                       {:action {:type :no-op}})]
            (is (= :no-active-clock (:reason focus)))
            (is (= :no-active-clock
                   (:reason (#'wm/mission-c-readback focus u11-ranked {})))
                "the S4 read's own reason reaches the readback, not the
                 coarser :no-clocked-mission it falls back to")))))))

(deftest selection-focus-reaches-risk-mis-through-the-U18-gauge-path-test
  (testing "the selected mission's criteria are read under the DECLARED gauges,
            and with the gauge observables present a number comes out"
    (with-redefs-fn {#'wm/*selection-focus?* true}
      (fn []
        (let [ranked (assoc-in u11-ranked [0 :action :target] "M-zaif-harness-v1")
              focus (#'wm/tick-mission-focus u21-stale-clock u21-decision)
              observation {:worklist-acceptance-state 1.0
                           :reporting-gate-test-result 0.0
                           :registry-gap-list-present 1.0}
              r (#'wm/mission-c-readback focus ranked observation)]
          (is (= "M-zaif-harness-v1" (:mission r))
              "risk_mis is computed for the mission the tick selected")
          (is (= 3 (:criterion-count r)))
          (is (= 3 (:measurable-count r))
              "all three reach the risk term through the declared gauges")
          (is (= {:criterion-1 :worklist-acceptance-state
                  :criterion-2 :reporting-gate-test-result
                  :criterion-3 :registry-gap-list-present
                  :honest-gap-list-published :registry-gap-list-present
                  :reporting-gate-holds :reporting-gate-test-result
                  :u-rows-green :worklist-acceptance-state}
                 (:declared-gauges r)))
          (is (= :measured (:status r)))
          (is (some? (:risk-mis (first (:per-mission-action r)))))))))
  (testing "with the gauge observables absent — the live state — the same path
            ends in a typed absence naming the missing producer, not a zero"
    (with-redefs-fn {#'wm/*selection-focus?* true}
      (fn []
        (let [ranked (assoc-in u11-ranked [0 :action :target] "M-zaif-harness-v1")
              focus (#'wm/tick-mission-focus u21-stale-clock u21-decision)
              r (#'wm/mission-c-readback focus ranked {:sorry-count-norm 0.0})]
          (is (= :absent (:status r)))
          (is (= :no-measurable-criteria (:reason r)))
          (is (= 0 (:measurable-count r)))
          (is (= [:undeclared-observable :undeclared-observable
                  :undeclared-observable]
                 (mapv :reason (:unmeasurable r)))))))))

;; ---------------------------------------------------------------------------
;; U43 (minted by U21) — reconciling the focus projection with the witnessed
;; clock edge. The row's finding: `:agrees-with-durable?` is one boolean over at
;; least four states, and the state it was minted for — the previous tick's
;; fire-and-forget clock write never landing — is invisible in all of them.

(def ^:private u43-prev-record
  "The previous tick's persisted record. Its identity is what
   `record-selection-clock!` stamps into the edge's `witness.source`, so it is
   what says whether that write landed."
  {:run/id "0a18c4f7-758e-400a-8223-9c52edf07450"
   :timestamp "2026-09-02T06:10:00Z"
   :decision {:action {:type :advance-mission
                       :target "M-wm-aif-policy-grain-compliance"}}})

(defn- u43-focus
  [active decision prev]
  (with-redefs-fn {#'wm/*selection-focus?* true #'wm/*focus-reconcile?* true}
    (fn [] (#'wm/tick-mission-focus active decision prev))))

(deftest focus-reconcile-flag-off-adds-no-field-test
  (testing "U21's record is unchanged when only U21's flag is on"
    (with-redefs-fn {#'wm/*selection-focus?* true #'wm/*focus-reconcile?* false}
      (fn []
        (let [focus (#'wm/tick-mission-focus u21-stale-clock u21-decision
                                             u43-prev-record)]
          (is (not (contains? focus :reconciliation)))
          (is (false? (:agrees-with-durable? focus)))
          (is (= (#'wm/tick-mission-focus u21-stale-clock u21-decision) focus)
              "and the two-argument arity U21 shipped returns the same map")))))
  (testing "and the S4 clock read keeps its four-key shape"
    (let [body (pr-str {:hyperedges
                        [{:hx/endpoints ["agent:war-machine"
                                         "futon4-d/mission/next"]
                          :hx/props {:agent-id "war-machine"
                                     :mission-id "M-next"
                                     :clocked-at-ms 1700
                                     :witness {:rule "selection-decision"
                                               :source "run-9"}}}]})]
      (with-redefs-fn {#'wm/*clock-focus?* true
                       #'wm/*focus-reconcile?* false
                       #'http/get (fn [_ _] {:status 200 :body body})}
        (fn []
          (is (= {:endpoint "futon4-d/mission/next" :mission-id "M-next"
                  :clocked-at-ms 1700 :witness-rule "selection-decision"}
                 (#'wm/load-active-mission))
              "no :witness-source key while U43 is off"))))))

(deftest focus-reconcile-surfaces-the-witness-source-test
  (testing "the edge's witness source — dropped by the S4 read since S4 — is
            the discriminator, so U43's flag keeps it"
    (let [body (pr-str {:hyperedges
                        [{:hx/endpoints ["agent:war-machine"
                                         "futon4-d/mission/next"]
                          :hx/props {:agent-id "war-machine"
                                     :mission-id "M-next"
                                     :clocked-at-ms 1700
                                     :witness {:rule "selection-decision"
                                               :source "run-9"}}}]})]
      (with-redefs-fn {#'wm/*clock-focus?* true
                       #'wm/*focus-reconcile?* true
                       #'http/get (fn [_ _] {:status 200 :body body})}
        (fn []
          (is (= "run-9" (:witness-source (#'wm/load-active-mission))))))))
  (testing "an edge whose witness carries no source reads as nil, not as absent
            — the key is present so the reader can tell the two apart"
    (let [body (pr-str {:hyperedges
                        [{:hx/endpoints ["agent:war-machine"
                                         "futon4-d/mission/next"]
                          :hx/props {:agent-id "war-machine"
                                     :mission-id "M-next"
                                     :clocked-at-ms 1700
                                     :witness {:rule "selection-decision"}}}]})]
      (with-redefs-fn {#'wm/*clock-focus?* true
                       #'wm/*focus-reconcile?* true
                       #'http/get (fn [_ _] {:status 200 :body body})}
        (fn []
          (let [active (#'wm/load-active-mission)]
            (is (contains? active :witness-source))
            (is (nil? (:witness-source active)))))))))

(deftest focus-reconcile-separates-a-landed-lag-from-a-lost-write-test
  (testing "the durable edge names the PREVIOUS trace record: the write landed
            and this is the seam's designed one-tick lag"
    (let [active (assoc u21-stale-clock
                        :witness-source "0a18c4f7-758e-400a-8223-9c52edf07450")
          r (:reconciliation (u43-focus active u21-decision u43-prev-record))]
      (is (= :lagged-write-landed (:case r)))
      (is (true? (:previous-write-landed? r)))
      (is (true? (:comparable? r)))
      (is (= "M-zaif-harness-v1" (:selected-mission-id r)))
      (is (= "M-wm-aif-policy-grain-compliance" (:durable-mission-id r)))
      (is (= "M-wm-aif-policy-grain-compliance"
             (:previous-selected-mission-id r)))))
  (testing "the durable edge names something OLDER: the previous tick's
            fire-and-forget write never landed, which is the failure
            `record-selection-clock!` drops inside its future"
    (let [active (assoc u21-stale-clock :witness-source "some-older-run")
          r (:reconciliation (u43-focus active u21-decision u43-prev-record))]
      (is (= :lagged-write-missing (:case r)))
      (is (false? (:previous-write-landed? r)))
      (is (= "some-older-run" (:durable-witness-source r)))
      (is (= "0a18c4f7-758e-400a-8223-9c52edf07450" (:previous-trace-id r)))))
  (testing "U21's boolean cannot tell those two apart — same false, both times"
    (let [landed (u43-focus (assoc u21-stale-clock :witness-source
                                   "0a18c4f7-758e-400a-8223-9c52edf07450")
                            u21-decision u43-prev-record)
          lost (u43-focus (assoc u21-stale-clock :witness-source "some-older-run")
                          u21-decision u43-prev-record)]
      (is (= false (:agrees-with-durable? landed) (:agrees-with-durable? lost)))
      (is (not= (:case (:reconciliation landed))
                (:case (:reconciliation lost)))))))

(deftest focus-reconcile-types-an-unmeasurable-lag-rather-than-guessing-test
  (testing "no witness source on the edge: unknown, and named as unknown"
    (let [r (:reconciliation (u43-focus u21-stale-clock u21-decision
                                        u43-prev-record))]
      (is (= :lag-unattributable (:case r)))
      (is (= :unknown (:previous-write-landed? r)))
      (is (= :no-witness-source-on-edge (:unknown-reason r)))))
  (testing "no previous trace record to compare against: also unknown, and the
            reason distinguishes it from the missing-source case"
    (let [active (assoc u21-stale-clock :witness-source "run-9")
          r (:reconciliation (u43-focus active u21-decision nil))]
      (is (= :lag-unattributable (:case r)))
      (is (= :unknown (:previous-write-landed? r)))
      (is (= :no-previous-trace-record (:unknown-reason r)))))
  (testing "the previous record's timestamp is the identity when it carries no
            run id — the same rule the writer stamps (`clock-source`)"
    (let [prev {:timestamp "2026-09-02T06:10:00Z"
                :decision {:action {:type :advance-mission :target "M-a"}}}
          active (assoc u21-stale-clock :witness-source "2026-09-02T06:10:00Z")
          r (:reconciliation (u43-focus active u21-decision prev))]
      (is (= :lagged-write-landed (:case r))))))

(deftest focus-reconcile-splits-the-four-states-U21-s-boolean-merged-test
  (testing "clock never read: the comparison is undefined, not failed —
            U21's boolean says `false` here, which reads as disagreement"
    (let [focus (u43-focus nil u21-decision u43-prev-record)]
      (is (false? (:agrees-with-durable? focus)))
      (is (= :clock-not-read (:case (:reconciliation focus))))
      (is (false? (:comparable? (:reconciliation focus))))
      (is (= :clock-focus-flag-off (:unknown-reason (:reconciliation focus))))))
  (testing "the read failed: unmeasured, not absent"
    (let [r (:reconciliation (u43-focus {:ok false :reason :clock-unreadable}
                                        u21-decision u43-prev-record))]
      (is (= :durable-unreadable (:case r)))
      (is (false? (:comparable? r)))))
  (testing "the store holds no edge: nothing to disagree with"
    (let [r (:reconciliation (u43-focus {:ok false :reason :no-active-clock}
                                        u21-decision u43-prev-record))]
      (is (= :no-durable-edge (:case r)))
      (is (false? (:comparable? r)))))
  (testing "no mission selected this tick: there is no projection to compare"
    (let [r (:reconciliation (u43-focus u21-stale-clock
                                        {:action {:type :address-sorry}}
                                        u43-prev-record))]
      (is (= :no-selection-this-tick (:case r)))
      (is (false? (:comparable? r)))))
  (testing "the two sides agree"
    (let [r (:reconciliation
             (u43-focus (assoc u21-stale-clock :mission-id "M-zaif-harness-v1")
                        u21-decision u43-prev-record))]
      (is (= :agrees (:case r)))
      (is (true? (:comparable? r)))))
  (testing "every case produced above is in the declared vocabulary"
    (is (every? wm/focus-reconciliation-cases
                (map #(:case (:reconciliation %))
                     [(u43-focus nil u21-decision u43-prev-record)
                      (u43-focus {:ok false :reason :clock-unreadable}
                                 u21-decision u43-prev-record)
                      (u43-focus {:ok false :reason :no-active-clock}
                                 u21-decision u43-prev-record)
                      (u43-focus u21-stale-clock {:action {:type :address-sorry}}
                                 u43-prev-record)
                      (u43-focus (assoc u21-stale-clock :mission-id
                                        "M-zaif-harness-v1")
                                 u21-decision u43-prev-record)
                      (u43-focus (assoc u21-stale-clock :witness-source
                                        "0a18c4f7-758e-400a-8223-9c52edf07450")
                                 u21-decision u43-prev-record)
                      (u43-focus (assoc u21-stale-clock :witness-source "old")
                                 u21-decision u43-prev-record)
                      (u43-focus u21-stale-clock u21-decision
                                 u43-prev-record)]))))
  (testing "and the reconciliation moves nothing the readback reads: the focus
            the C_mis path receives is the same map with one key added"
    (let [with-r (u43-focus u21-stale-clock u21-decision u43-prev-record)
          without (with-redefs-fn {#'wm/*selection-focus?* true
                                   #'wm/*focus-reconcile?* false}
                    (fn [] (#'wm/tick-mission-focus u21-stale-clock u21-decision
                                                    u43-prev-record)))]
      (is (= without (dissoc with-r :reconciliation))))))

(deftest focus-reconcile-renders-both-fields-on-two-lines-test
  (testing "U43 (b): the durable clock and the tick's focus are two claims and
            get two lines; the durable line is byte-identical to U21's"
    (let [active {:endpoint "futon4-d/mission/next" :mission-id "M-next"
                  :clocked-at-ms 1700 :witness-rule "selection-decision"}
          focus (u43-focus (assoc u21-stale-clock :witness-source "old")
                           u21-decision u43-prev-record)
          md (wm/render-war-machine
              {:now "2026-09-03" :days 7
               :judgement {:active-mission active :mission-focus focus
                           :mode :steady :mode-prior 0.5
                           :free-energy {:controller-score 0.0
                                         :preference-gap-score 0.0
                                         :coverage-uncertainty-pressure 0.0}}})
          lines (->> (str/split-lines md)
                     (filterv #(or (str/starts-with? % "**Active mission:")
                                   (str/starts-with? % "**Tick focus:"))))]
      (is (= 2 (count lines)) "two lines, never collapsed into one")
      (is (= "**Active mission:** futon4-d/mission/next (M-next, clocked 1700, witness selection-decision)"
             (first lines)))
      (is (str/starts-with? (second lines) "**Tick focus:** M-zaif-harness-v1"))
      (is (str/includes? (second lines) "this-tick-selection"))
      (is (str/includes? (second lines)
                         "durable clock: M-wm-aif-policy-grain-compliance"))
      (is (str/includes? (second lines) "agrees: no"))
      (is (str/includes? (second lines) "case: lagged-write-missing"))
      (is (str/includes? (second lines) "previous write landed: NO"))))
  (testing "a judgement with no :mission-focus renders exactly the durable line
            — the default-off report is unchanged"
    (let [md (wm/render-war-machine
              {:now "2026-09-03" :days 7
               :judgement {:active-mission {:ok false :reason :no-active-clock}
                           :mode :steady :mode-prior 0.5
                           :free-energy {:controller-score 0.0
                                         :preference-gap-score 0.0
                                         :coverage-uncertainty-pressure 0.0}}})]
      (is (str/includes? md "**Active mission:** unavailable (no-active-clock)"))
      (is (not (str/includes? md "**Tick focus:**")))))
  (testing "a focus with no reconciliation still renders both fields; the case
            clause is present only when the record carries one"
    (let [focus (with-redefs-fn {#'wm/*selection-focus?* true
                                 #'wm/*focus-reconcile?* false}
                  (fn [] (#'wm/tick-mission-focus u21-stale-clock u21-decision)))
          md (wm/render-war-machine
              {:now "2026-09-03" :days 7
               :judgement {:mission-focus focus :mode :steady :mode-prior 0.5
                           :free-energy {:controller-score 0.0
                                         :preference-gap-score 0.0
                                         :coverage-uncertainty-pressure 0.0}}})]
      (is (str/includes? md "**Tick focus:** M-zaif-harness-v1"))
      (is (str/includes? md "durable clock: M-wm-aif-policy-grain-compliance"))
      (is (not (str/includes? md "case: "))))))

;; ---------------------------------------------------------------------------
;; U22 -- the epistemic term of mission value. Declared input, default off.

(def ^:private u22-mission-docs
  "Three candidates the judge can read, plus the neighbourhood two of them
   declare. M-surveyor is in MAP with ten cross-refs whose phase the judge
   cannot read; M-builder is in INSTANTIATE with the same unread
   neighbourhood; M-mapped is in MAP with nothing left to find."
  (let [dark (mapv #(str "M-dark-" %) (range 10))]
    (into [{:hx/endpoints ["repo-d/mission/surveyor"]
            :hx/props {:mission/id "surveyor" :mission/phase "map"
                       :mission/mtime "2026-01-01"
                       :mission/cross-refs dark}}
           {:hx/endpoints ["repo-d/mission/builder"]
            :hx/props {:mission/id "builder" :mission/phase "instantiate"
                       :mission/mtime "2026-01-01"
                       :mission/cross-refs dark}}
           {:hx/endpoints ["repo-d/mission/mapped"]
            :hx/props {:mission/id "mapped" :mission/phase "map"
                       :mission/mtime "2026-09-03"
                       :mission/cross-refs ["M-builder"]}}]
          (map (fn [id] {:hx/endpoints [(str "repo-d/mission/" id)]
                         :hx/props {"mission/id" (str/replace id "M-" "")
                                    "mission/mtime" "2026-09-03"}}))
          dark)))

(def ^:private u22-declared-weights
  "DECLARED for this test only. The default is :epistemic 0.0; the flip is J."
  {:central 0.20 :strategic 0.30 :doable 0.25 :epistemic 0.25})

(defn- u22-enrich
  [weights]
  (with-redefs-fn
    {#'wm/centrality-joint-map (fn [] {"M-surveyor" 0.0 "M-builder" 0.9
                                       "M-mapped" 1.0})
     #'wm/mission-doc-index (fn [] {"surveyor" {:endpoint "mission/surveyor"}
                                    "builder" {:endpoint "mission/builder"}
                                    "mapped" {:endpoint "mission/mapped"}})
     #'wm/compute-delta-t-mission (fn [_] {:delta-T 0.0})
     #'wm/read-strategy-cascade (fn [_] {:boxes [] :spine [] :terminals []})}
    (fn []
      (wm/enrich-candidates-with-mission-value
       [{:type :advance-mission :target "M-surveyor"}
        {:type :advance-mission :target "M-builder"}
        {:type :advance-mission :target "M-mapped"}]
       nil
       (cond-> {:strategy-cascade-path "unused"
                :epistemic-as-of (java.time.LocalDate/parse "2026-09-03")
                :hyperedges-by-type-fn (fn [_] u22-mission-docs)}
         weights (assoc :mission-value-weights weights))))))

(defn- u22-rank
  [entries id]
  (->> entries
       (sort-by (comp - :mission-value-factor))
       (map :target)
       vec
       (#(inc (.indexOf ^java.util.List % id)))))

(deftest the-epistemic-weight-defaults-to-zero-and-reads-nothing
  (testing "the default weights carry the fourth key at zero"
    (is (= {:central 0.25 :strategic 0.45 :doable 0.30 :epistemic 0.0}
           @#'wm/default-mission-value-weights))
    (is (= 1.0 (reduce + 0.0 (vals @#'wm/default-mission-value-weights)))))
  (testing "a three-key weights map is still accepted, epistemic defaulted"
    (is (= {:central 0.0 :strategic 0.0 :doable 1.0 :epistemic 0.0}
           (#'wm/mission-value-weights
            {:mission-value-weights {:central 0.0 :strategic 0.0 :doable 1.0}}))))
  (testing "off, no epistemic field is attached and the value is the old blend"
    (let [entries (u22-enrich nil)]
      (is (every? #(and (nil? (:epistemic %)) (nil? (:epistemic-basis %)))
                  entries))
      ;; central 1.0 * 0.25 + doable 0.3 * 0.30 = 0.34 for M-mapped
      (is (every? (fn [[entry expected]]
                    (< (Math/abs (- (double (:mission-value-factor entry))
                                    expected))
                       1.0e-9))
                  (map vector entries [0.09 0.315 0.34]))))))

(deftest a-declared-epistemic-weight-moves-a-map-phase-mission-up
  (let [before (u22-enrich nil)
        after (u22-enrich u22-declared-weights)]
    (is (= [3 2 1] (mapv #(u22-rank before %)
                         ["M-surveyor" "M-builder" "M-mapped"]))
        "the exploit-only blend puts the survey mission last")
    (is (= [1 3 2] (mapv #(u22-rank after %)
                         ["M-surveyor" "M-builder" "M-mapped"]))
        "the survey mission takes rank 1 once the term is weighted")
    (testing "for a stated reason, carried in the record"
      (let [basis (->> after
                       (filter #(= "M-surveyor" (:target %)))
                       first
                       :epistemic-basis)]
        (is (= :measured (:status basis)))
        (is (= "map" (:phase basis)))
        (is (= 11 (:question-count basis)) "ten neighbours plus freshness")
        (is (= 11 (:open-question-count basis)))
        (is (true? (:clamped? basis)) "past the declared reference of ten")
        (is (= 1.0 (:availability basis)))
        (is (= :doability-phase-absent (:phase-agreement basis))
            "the doability factor got no phase in this process; the epistemic
             term read one, and the record says so rather than reconciling it")))))

(deftest a-map-phase-with-nothing-unread-does-not-move
  (let [after (u22-enrich u22-declared-weights)
        mapped (first (filter #(= "M-mapped" (:target %)) after))]
    (is (= "map" (get-in mapped [:epistemic-basis :phase])))
    (is (zero? (:epistemic mapped))
        "M-mapped is in MAP too; its neighbourhood is read and its doc is fresh")
    (is (= 2 (u22-rank after "M-mapped"))
        "it keeps its place ahead of M-builder on the exploit factors alone")))


;; ---------------------------------------------------------------------------
;; U44 -- doability liveness. Declared input, default off, and the default is
;; byte-identical rather than merely "close".

(def ^:private u44-mission-idx
  "The index shape the repair reads: `:phase` carried off the mission-doc
   hyperedge. M-instantiated and M-headed have one; M-shipped is marked
   complete, which is what makes the completion gate observable; M-phaseless
   has no phase at all, so its live reading must be a typed absence and not a
   guess."
  {"instantiated" {:endpoint "mission/instantiated" :phase "instantiate"}
   "headed" {:endpoint "mission/headed" :phase "head"}
   "shipped" {:endpoint "mission/shipped" :phase "complete"}
   "phaseless" {:endpoint "mission/phaseless"}})

(defn- u44-enrich
  ([opts] (u44-enrich opts (fn [_] {:delta-T 0.0})))
  ([opts delta-t-fn]
   (with-redefs-fn
     {#'wm/centrality-joint-map (fn [] {})
      #'wm/mission-doc-index (fn [] u44-mission-idx)
      #'wm/compute-delta-t-mission delta-t-fn
      #'wm/read-strategy-cascade (fn [_] {:boxes [] :spine [] :terminals []})}
     (fn []
       (wm/enrich-candidates-with-mission-value
        [{:type :advance-mission :target "M-instantiated"}
         {:type :advance-mission :target "M-headed"}
         {:type :advance-mission :target "M-shipped"}
         {:type :advance-mission :target "M-phaseless"}]
        nil
        (merge {:strategy-cascade-path "unused"
                :mission-value-weights {:central 0.20 :strategic 0.50
                                        :doable 0.30}}
               opts))))))

(defn- u44-by-id [entries]
  (into {} (map (juxt :target identity)) entries))

(deftest the-live-doability-input-defaults-to-off
  (testing "with no declared input the phase is nil and every doable is the
            'unknown' 0.3 -- the inert state C492 section 4b measured"
    (let [entries (u44-enrich {})]
      (is (every? #(nil? (:phase %)) entries))
      (is (= #{0.3} (set (map :doable entries))))
      (is (every? #(not (contains? % :phase-source)) entries)
          "the field the repair adds is absent from the default record")))
  (testing "an explicit false is the same judgement as saying nothing"
    (is (= (u44-enrich {}) (u44-enrich {:live-doability? false})))))

(deftest a-declared-live-doability-reads-the-phase-off-the-hyperedge
  (let [by-id (u44-by-id (u44-enrich {:live-doability? true}))]
    (testing "the fiat table is applied to the phase the index already carried"
      (is (= [1.0 0.1] [(:doable (by-id "M-instantiated"))
                        (:doable (by-id "M-headed"))]))
      (is (= ["instantiate" "head"] [(:phase (by-id "M-instantiated"))
                                     (:phase (by-id "M-headed"))])))
    (testing "and the record says which carrier the number came from"
      (is (= :mission-doc-hyperedge (:phase-source (by-id "M-instantiated")))))
    (testing "a mission with no phase on the hyperedge is typed, not guessed:
              it keeps the 'unknown' 0.3 and says the reading was unreadable"
      (is (= 0.3 (:doable (by-id "M-phaseless"))))
      (is (nil? (:phase (by-id "M-phaseless"))))
      (is (= :unreadable (:phase-source (by-id "M-phaseless")))))))

(deftest the-delta-t-carrier-remains-the-fallback
  (testing "when the hyperedge has no phase and delta-t resolves one, the live
            path takes it and records THAT carrier -- the repair adds a source,
            it does not replace one"
    (let [by-id (u44-by-id (u44-enrich {:live-doability? true}
                                       (fn [_] {:mission-phase "verify"})))]
      (is (= 0.8 (:doable (by-id "M-phaseless"))))
      (is (= :delta-t (:phase-source (by-id "M-phaseless")))))
    (testing "and the hyperedge wins where both carriers have a reading"
      (let [by-id (u44-by-id (u44-enrich {:live-doability? true}
                                         (fn [_] {:mission-phase "verify"})))]
        (is (= "instantiate" (:phase (by-id "M-instantiated"))))
        (is (= :mission-doc-hyperedge
               (:phase-source (by-id "M-instantiated"))))))))

(deftest the-completion-gate-was-inert-for-the-same-reason
  (testing "a mission the field marks complete is ranked like any other while
            the phase is nil: the gate is (= \"complete\" phase) and phase is nil"
    (let [shipped (get (u44-by-id (u44-enrich {})) "M-shipped")]
      (is (= 1.0 (:completion-gate-factor shipped)))
      (is (pos? (double (:mission-value-factor shipped))))))
  (testing "with the phase read, the gate fires and the value is zero"
    (let [shipped (get (u44-by-id (u44-enrich {:live-doability? true}))
                       "M-shipped")]
      (is (= 0.0 (:completion-gate-factor shipped)))
      (is (= 0.0 (:doable shipped)))
      (is (zero? (double (:mission-value-factor shipped)))))))

(deftest the-environment-switch-and-the-opt-are-one-input
  (testing "the var bound on, with no opt passed, is the opt passed true"
    (is (= (with-redefs-fn {#'wm/*live-doability?* true}
             (fn [] (u44-enrich {})))
           (u44-enrich {:live-doability? true}))))
  (testing "and a per-call opt overrides the process-wide switch in both
            directions, because an opt that is present is a declaration"
    (is (= (with-redefs-fn {#'wm/*live-doability?* true}
             (fn [] (u44-enrich {:live-doability? false})))
           (u44-enrich {})))
    (is (= (with-redefs-fn {#'wm/*live-doability?* false}
             (fn [] (u44-enrich {:live-doability? true})))
           (u44-enrich {:live-doability? true})))))

(deftest mission-doc-index-carries-the-phase-it-used-to-discard
  (let [hxs [{:hx/endpoints ["repo/mission/kw"]
              :hx/props {:mission/id "M-kw" :mission/phase "derive"}}
             {:hx/endpoints ["repo/mission/str"]
              :hx/props {"mission/id" "M-str" "mission/phase" "argue"}}
             {:hx/endpoints ["repo/mission/none"]
              :hx/props {:mission/id "M-none"}}]]
    (with-redefs-fn {#'wm/fetch-hyperedges-by-type (constantly hxs)}
      (fn []
        (let [idx (#'wm/mission-doc-index)]
          (testing "both prop shapes the family is served in"
            (is (= "derive" (get-in idx ["kw" :phase])))
            (is (= "argue" (get-in idx ["str" :phase]))))
          (testing "and no phase is no phase"
            (is (nil? (get-in idx ["none" :phase])))))))))

(deftest the-doability-prior-is-pinned-across-the-two-namespaces
  (is (= @#'wm/phase-doability mev/phase-doability-prior)
      "mission-epistemic-value reads its workable partition off this table"))

(deftest weights-that-do-not-sum-to-one-are-refused
  (is (thrown? clojure.lang.ExceptionInfo
               (#'wm/mission-value-weights
                {:mission-value-weights {:central 0.25 :strategic 0.45
                                         :doable 0.30 :epistemic 0.20}})))
  (is (thrown? clojure.lang.ExceptionInfo
               (#'wm/mission-value-weights
                {:mission-value-weights {:central 0.20 :strategic 0.35
                                         :doable 0.25 :epistemic -0.20}}))))

;; ---------------------------------------------------------------------------
;; U37: the enumeration-completeness projection at its call site.

(deftest enumeration-completeness-projection-is-flag-gated
  (testing "flag off, the judgement is untouched -- byte-identical, not merely
            'the key is nil'"
    (let [judgement {:decision {:action {:type :no-op}
                                :controller-ranking
                                [{:action {:type :advance-mission
                                           :target "M-not-on-disk"}}]}}]
      (binding [ec/*enumeration-assert?* false]
        (is (= judgement (#'wm/carry-enumeration-completeness judgement))))))

  (testing "flag on, the tick attaches a real verdict -- the seam is not a
            no-op: a candidate for a mission that is not in the scanned root
            comes back as a phantom, so the scan ran and disagreed"
      (let [root (str (Files/createTempDirectory
                       "u37-wm" (into-array FileAttribute [])))
            _ (let [f (io/file root "repo" "holes" "missions" "M-real.md")]
                (io/make-parents f)
                (spit f "# M-real\n\nStatus: ACTIVE\n"))
            judgement {:decision {:action {:type :no-op}
                                  :controller-ranking
                                  [{:action {:type :advance-mission
                                             :target "M-not-on-disk"}}]}}]
        (with-redefs [ec/default-code-root root]
          (binding [ec/*enumeration-assert?* true]
            (let [out (#'wm/carry-enumeration-completeness judgement)
                  rec (get-in out [:decision :enumeration-completeness])
                  mission (first (filter #(= :mission (:kind %)) (:kinds rec)))]
              (is (= :incomplete (:verdict rec)))
              (is (= ["M-not-on-disk"] (:phantom mission)))
              (is (= ["M-real"] (:missing mission))
                  "and the live mission the tick did not enumerate is named")
              (is (= (dissoc judgement :decision)
                     (dissoc out :decision))
                  "nothing outside the decision moved")))))))

;; ---------------------------------------------------------------------------
;; U52 -- the three-rung ladder at the selection scoring seam
;; ---------------------------------------------------------------------------

(def ^:private u52-candidates
  [{:type :advance-mission :target "M-known"
    :mission-path "/nowhere/M-known.md" :mission-value-factor 0.09}
   {:type :advance-mission :target "M-unknown"
    :mission-path "/nowhere/M-unknown.md" :mission-value-factor 0.09}
   {:type :no-op :target nil}])

(deftest u52-ladder-default-is-the-identity
  (testing "with no declared input the SAME vector object comes back"
    (let [out (wm/apply-task-belief-ladder u52-candidates)]
      (is (identical? u52-candidates (:candidates out)))
      (is (= [] (:refusals out)))
      (is (nil? (:ladder out))
          "and no ladder record is attached, so a default judgement carries no U52 key")))

  (testing "an opt that is present and false is a declaration too"
    (binding [wm/*task-belief-ladder?* true]
      (is (identical? u52-candidates
                      (:candidates (wm/apply-task-belief-ladder
                                    u52-candidates
                                    {:task-belief-ladder? false}))))))

  (testing "the dynamic var alone turns it on"
    (binding [wm/*task-belief-ladder?* true]
      (is (some? (:ladder (wm/apply-task-belief-ladder
                           u52-candidates
                           {:case-history {}})))))))

(deftest u52-ladder-on-rungs-and-refusal
  (let [out (wm/apply-task-belief-ladder
             u52-candidates
             {:task-belief-ladder? true
              :relation :k-doc-xref
              :case-history {[:advance-mission "M-known"] 4}})
        kept (into {} (map (juxt :target identity)) (:candidates out))]
    (is (= {1 1, 3 1, :out-of-scope 1} (:census out))
        "one direct case history, one nothing, one out of scope")
    (is (= 1 (count (:refusals out))))
    (is (= [:advance-mission "M-unknown"]
           (:refusal/action-key (first (:refusals out)))))
    (is (nil? (kept "M-unknown")) "a refused candidate leaves the field")
    (is (= (* 0.09 0.8) (:mission-value-factor (kept "M-known"))))
    (is (= 0.09 (:task-belief/pre-ladder-mission-value-factor (kept "M-known")))
        "the pre-ladder number stays on the record beside the new one")
    (is (= 1 (:history-size (:ladder out)))
        "the ladder record says how many keys the index it read carried")))

(deftest u52-case-history-index-counts-chosen-actions
  (let [dir (str (Files/createTempDirectory "u52-trace" (into-array FileAttribute [])))
        write! (fn [date recs]
                 (spit (io/file dir (str "wm-trace-" date ".edn"))
                       (str/join "\n" (map pr-str recs))))]
    (write! "2026-09-01"
            [{:decision {:action {:type :advance-mission :target "M-a"}}}
             {:decision {:action {:type :advance-mission :target "M-a"}}}
             {:decision {:action {:type :learn-action-class :target nil}}}])
    (write! "2026-09-02"
            [{:decision {:action {:type :advance-mission :target "M-b"}}}])
    ;; a file that is not a daily trace must not be read as one
    (spit (io/file dir "notes.edn") (pr-str {:decision {:action {:type :advance-mission :target "M-c"}}}))
    (let [idx (wm/case-history-index dir)]
      (is (= {[:advance-mission "M-a"] 2
              [:advance-mission "M-b"] 1
              [:learn-action-class nil] 1}
             idx)
          "a typed action with no target is a decision the corpus records and is counted; the ladder's SCOPE rule is what excludes it")
      (is (nil? (get idx [:advance-mission "M-c"]))
          "and a file that is not a daily trace is not corpus"))))

(deftest constant-checkpoint-adapter-through-a3-scorer
  (let [legacy-artifact
        (checkpoint-kernel/fit-kernel
         {:attempts [{:closed? true :checkpoints [:selected :closed]
                      :outcome :grounded-change}]}
         "synthetic-cohort" "test-fixture")
        ;; The checkpoint fitter still records its historical 14-label carrier.
        ;; The A3 scorer contract narrowed to ruled-outcome C's current twelve
        ;; on 2026-09-12; this fixture must exercise that boundary, not disable
        ;; the refusal by feeding the two named historical labels through it.
        support (vec (sort (:support ruled/seeded-c)))
        project (fn [mass] (select-keys mass support))
        artifact (-> legacy-artifact
                     (assoc :support support)
                     (update :outcome-counts project)
                     (update :supported-outcomes
                             #(vec (filter (set support) %)))
                     (update :unsupported-outcomes
                             #(vec (filter (set support) %)))
                     (update :states
                             (fn [states]
                               (mapv #(-> %
                                          (update :counts project)
                                          (update :probability project))
                                     states))))
        adapter (disposition/constant-checkpoint-kernel artifact)
        base {:risk-mode :hinge :ambiguity-mode :variance-sum}
        absent (#'wm/configured-fold-efe-opts base {})
        enabled (#'wm/configured-fold-efe-opts
                 base {:ruled-outcome-c-enabled? true :seeded-c ruled/seeded-c
                       :disposition-kernel adapter})
        action {:type :no-op}
        before (efe/compute-efe {} action base)
        after (efe/compute-efe {} action enabled)]
    (is (= (pr-str before) (pr-str (efe/compute-efe {} action absent))))
    (is (< (Math/abs (- (Math/log 2.0) (:G-ruled-outcome-c after))) 1.0e-12))
    (is (< (Math/abs (- (Math/log 2.0)
                       (- (:controller-score after) (:controller-score before)))) 1.0e-12))))
