(ns futon2.report.war-machine-test
  "Tests for War Machine scan logic.

   Tests the pure data transformation functions — arrow-health,
   observation vector, and data shape contracts — without requiring
   live APIs or git repos."
  (:require [babashka.http-client :as http]
            [clojure.java.shell]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.efe :as efe]
            [futon2.report.war-machine :as wm]))

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
         {:mission-T (if (= endpoint "repo-d/mission/alpha") 1.0 0.7)})}
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
          (is (= {"alpha" "repo-d/mission/alpha"
                  "beta" "repo-d/mission/beta"}
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

(deftest morning-brief-events-use-live-belief-update-and-hold-unknown-entities
  (let [prior {"known" {:spawned (/ 1.0 7) :refined (/ 1.0 7)
                         :strengthened (/ 1.0 7) :addressed (/ 1.0 7)
                         :falsified (/ 1.0 7) :foreclosed (/ 1.0 7)
                         :reopened (/ 1.0 7)}}
        known {:event-id "qa-1" :entity-id "known" :type :strengthened :weight 1.0}
        unknown {:event-id "qa-2" :entity-id "unknown" :type :falsified :weight 1.0}
        result (wm/apply-morning-brief-events prior #{"older"} [known unknown])]
    (is (= [known] (:applied result)))
    (is (= [unknown] (:held result)))
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

(deftest mission-value-enrichment-normalizes-and-decays-non-progress
  (let [candidates [{:type :advance-mission :target "M-a" :open-hole-count 4}
                    {:type :advance-mission :target "M-b" :open-hole-count 4}
                    {:type :fire-pattern :target :pattern/high :retrieval-score 8.0}
                    {:type :fire-pattern :target :pattern/low :retrieval-score 2.0}]
        prev {:decision {:action {:type :advance-mission :target "M-a"}}
              :mu-pre {"M-a" {:addressed 0.2}}
              :mu-post {"M-a" {:addressed 0.2}}
              :outcome :grounded-no-change}
        delta-by-endpoint {"mission/a" {:mission-T 1.0}
                           "mission/b" {:mission-T 0.5}}
        redefs {#'wm/centrality-joint-map (fn [] {"M-a" 0.8 "M-b" 0.4})
                #'wm/mission-doc-index (fn [] {"a" "mission/a" "b" "mission/b"})
                #'wm/compute-delta-t-mission #(get delta-by-endpoint %)}]
    (with-redefs-fn
      redefs
      (fn []
        (let [[stuck other pattern-high pattern-low]
              (wm/enrich-candidates-with-mission-value candidates prev)
              [fresh] (wm/enrich-candidates-with-mission-value
                       [(first candidates)] nil)]
          (is (= 1.0 (:tension stuck)))
          (is (= 0.8 (:centrality stuck)))
          (is (= 0.5 (:non-progress-decay stuck)))
          (is (true? (:non-progress? stuck)))
          (is (= 1 (:non-progress-count stuck)))
          (is (= 0.5 (:mission-value-factor stuck)))
          (is (= 0.25 (:mission-value-factor other)))
          (is (= 1.0 (:mission-value-factor fresh)))
          (is (< (:mission-value-factor stuck)
                 (:mission-value-factor fresh)))
          (is (= 1.0 (:mission-value-factor pattern-high)))
          (is (= 0.25 (:mission-value-factor pattern-low)))
          (let [[stuck-again]
                (wm/enrich-candidates-with-mission-value
                 [(first candidates)]
                 {:decision {:action stuck}
                  :mu-pre {"M-a" {:addressed 0.2}}
                  :mu-post {"M-a" {:addressed 0.2}}
                  :outcome :grounded-no-change})]
            (is (= 2 (:non-progress-count stuck-again)))
            (is (= (/ 1.0 3.0) (:non-progress-decay stuck-again))))
          (let [state {:observation {:mission-health 0.3
                                     :sorry-count-norm 0.85}
                       :belief {}}
                stuck-g (:G-efe (efe/compute-efe state stuck))
                other-g (:G-efe (efe/compute-efe state other))]
            (is (not= stuck-g other-g)
                "judge-enriched equal-hole candidates have distinct strategic G")))))))

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
