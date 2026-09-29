(ns futon2.aif.scan-model-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]
            [futon2.aif.observation :as observation]
            [futon2.aif.scan-model :as scan-model]
            [futon2.aif.trace :as trace]
            [futon2.report.war-machine :as wm]))

(def prior {:alpha 1 :beta 1 :authority "scan-model-test"})
(def statuses (vec (sort belief/status-set)))

(defn- mu [& pairs]
  (merge (zipmap statuses (repeat 0)) (into {} pairs)))

(defn- record [belief-row support attack]
  {:mu-pre {"entity" belief-row}
   ;; Deliberately poisonous: the learner must never read this posterior.
   :mu-post {"entity" (mu [:foreclosed 1])}
   :scan-exposures {:support support :attack attack}})

(defn- hand-set []
  {:support (get belief/channel-emission-matrix :support-coverage)
   :attack (get belief/channel-emission-matrix :attack-coverage)})

(deftest scan-exposures-are-exact-and-persisted
  (let [entries [{:evidence/at "2099-01-01"
                  :evidence/body {:text "evidence AIF commercial complexity"}}]
        scan (wm/scan-support-attack 30 entries)
        exposures (:scan-exposures scan)
        traced (trace/trace-record {:belief {} :belief-pre {}
                                    :observation {} :decision nil
                                    :scan-exposures exposures})]
    (is (= {:support {:covered 3 :claims 5}
            :attack {:covered 1 :claims 4}}
           exposures))
    (is (= exposures (:scan-exposures traced))))
  (with-redefs-fn {#'wm/claim-patterns {}}
    (fn []
      (let [scan (wm/scan-support-attack 30 [])]
        (is (= {:support {:status :absent :reason :no-claims}
                :attack {:status :absent :reason :no-claims}}
               (:scan-exposures scan)))
        ;; Existing numeric consumers retain their old zero projection.
        (is (= 0.0 (:support-coverage scan) (:attack-coverage scan)))))))

(deftest graph-scan-exposures-are-exact-and-persisted
  (let [repos [{:workstream :stack :commits 3 :active? true}
               {:workstream :consulting :commits 2 :active? true}
               {:workstream :portfolio :commits 0 :active? false}
               {:workstream :mathematics :commits 1 :active? true}]
        coupling [{:from :a :to :b} {:from :b :to :c}]
        graph (with-redefs-fn {#'wm/repo-nodes (fn [_] repos)
                               #'wm/sorry-nodes (constantly [])
                               #'wm/workstream-nodes (constantly [])
                               #'wm/mission-nodes (constantly [])
                               #'wm/temporal-coupling-edges (fn [_] coupling)
                               #'wm/workstream-dependency-edges (constantly [])
                               #'wm/evidence-flow-edges (fn [_] [])
                               #'wm/open-sorry-census (constantly 7)}
                #(wm/scan-graph 30 []))
        support-attack {:support {:covered 3 :claims 5}
                        :attack {:covered 1 :claims 4}}
        exposures (merge support-attack (:scan-exposures graph))
        traced (trace/trace-record {:belief {} :belief-pre {}
                                    :observation {} :decision nil
                                    :scan-exposures exposures})]
    (is (= {:workstream-commits
            {:counts {:stack 3 :consulting 2 :portfolio 0 :mathematics 1} :total 6}
            :active-repos {:active 3 :repositories 4}
            :coupling {:edges 2 :possible 6}
            :ticks {:status :absent :reason :no-tick-results}
            :sorrys {:count 7}}
           (:scan-exposures graph)))
    (is (= exposures (:scan-exposures traced)))
    (is (= support-attack (select-keys (:scan-exposures traced) [:support :attack])))
    (is (= (observation/observe {:graph graph})
           (observation/observe {:graph (dissoc graph :scan-exposures)}))
        "the exact receipt is write-only for the observation vector")))

(deftest graph-scan-exposure-refusals-are-typed
  (let [exposures #'wm/graph-scan-exposures]
    (testing "zero commits keeps the old max-one ratios separate"
      (is (= {:status :absent :reason :no-workstream-commits}
             (:workstream-commits (exposures {} [{:active? false}] [] [] 0))))
      (is (= {:status :refused :reason :undeclared-workstream :workstreams [:teaching]}
             (:workstream-commits (exposures {:stack 3 :teaching 2} [{:active? true}] [] [] 0))))
      (let [graph (with-redefs-fn {#'wm/repo-nodes (fn [_] [{:workstream :stack :commits 0 :active? false}])
                                     #'wm/sorry-nodes (constantly [])
                                     #'wm/workstream-nodes (constantly [])
                                     #'wm/mission-nodes (constantly [])
                                     #'wm/temporal-coupling-edges (fn [_] [])
                                     #'wm/workstream-dependency-edges (constantly [])
                                     #'wm/evidence-flow-edges (fn [_] [])
                                     #'wm/open-sorry-census (constantly 0)}
                    #(wm/scan-graph 30 []))]
        (is (= {:stack 0.0 :consulting 0.0 :portfolio 0.0 :mathematics 0.0}
               (get-in graph [:dynamics :commit-percentages])))))
    (is (= {:status :absent :reason :no-repositories}
           (:active-repos (exposures {} [] [] [] 0))))
    (is (= {:status :absent :reason :fewer-than-two-repositories}
           (:coupling (exposures {} [{:active? true}] [] [] 0))))
    (is (= {:status :refused :reason :edges-exceed-possible :edges 2 :possible 1}
           (:coupling (exposures {} [{:active? true} {:active? true}] [:e1 :e2] [] 0))))
    (is (= {:status :absent :reason :no-tick-results}
           (:ticks (exposures {} [{:active? true}] [] [] 0))))
    (is (= {:fired 1 :eligible 2}
           (:ticks (exposures {} [{:active? true}] [] [{:fired? true} {:fired? false}] 0))))
    (is (= {:status :absent :reason :sorry-registry-unreadable}
           (:sorrys (exposures {} [{:active? true}] [] [] nil))))))

(defn- annotation-scan [anomalies sections]
  (with-redefs [clojure.core/slurp
                (constantly (pr-str {:lift-anomalies (range anomalies)
                                     :sections (range sections)}))]
    (wm/scan-annotation-graph)))

(deftest annotation-scan-exposures-are-exact-and-persisted
  (let [scan (annotation-scan 3 10)
        other {:support {:covered 3 :claims 5}
               :attack {:covered 1 :claims 4}
               :workstream-commits {:counts {:stack 1 :consulting 0
                                              :portfolio 0 :mathematics 0}
                                    :total 1}}
        exposures (merge other (:scan-exposures scan))
        traced (trace/trace-record {:belief {} :belief-pre {}
                                    :observation {} :decision nil
                                    :scan-exposures exposures})]
    (is (= {:annotation {:anomalies 3 :sections 10}}
           (:scan-exposures scan)))
    (is (= exposures (:scan-exposures traced)))
    (is (= other (dissoc (:scan-exposures traced) :annotation)))
    (is (= {:health 0.7 :anomaly-count 3 :section-count 10}
           (select-keys scan [:health :anomaly-count :section-count])))
    (is (= (observation/observe {:annotation-graph scan})
           (observation/observe {:annotation-graph (dissoc scan :scan-exposures)}))
        "the exact receipt does not change the observation vector")))

(deftest annotation-exposures-preserve-unprojected-counts-and-typed-absence
  (testing "anomalies are not clipped to sections"
    (let [scan (annotation-scan 12 10)]
      (is (= {:anomalies 12 :sections 10}
             (get-in scan [:scan-exposures :annotation])))
      (is (= {:health 0.0 :anomaly-count 12 :section-count 10}
             (select-keys scan [:health :anomaly-count :section-count])))))
  (testing "zero sections keeps the legacy fields and names the absent carrier"
    (let [scan (annotation-scan 3 0)]
      (is (= {:status :absent :reason :no-sections}
             (get-in scan [:scan-exposures :annotation])))
      (is (= {:health 0.0 :anomaly-count 3 :section-count 0}
             (select-keys scan [:health :anomaly-count :section-count])))))
  (testing "unreadable source keeps the catch arm's legacy zero projection"
    (let [scan (with-redefs [clojure.core/slurp
                             (fn [& _] (throw (java.io.IOException. "annotation fixture unreadable")))]
                 (wm/scan-annotation-graph))]
      (is (= {:status :absent
              :reason :annotation-source-unreadable
              :error {:class "java.io.IOException"
                      :message "annotation fixture unreadable"}}
             (get-in scan [:scan-exposures :annotation])))
      (is (= {:health 0.0 :anomaly-count 0 :section-count 0}
             (select-keys scan [:health :anomaly-count :section-count]))))))

(deftest scalar-bin-exposures-are-persisted-without-changing-observation
  (let [loop-scan (wm/scan-loop-health 30 [])
        mission-scan (wm/scan-mission-triage
                       30
                       [{:mission/status "complete" :mission/repo "test"}]
                       [])
        frame-dir (.toFile (java.nio.file.Files/createTempDirectory
                             "wm-scan-bins" (make-array java.nio.file.attribute.FileAttribute 0)))
        frame-file (java.io.File. frame-dir "2099-01-01.edn")]
    (spit frame-file
          (pr-str {:frame/id "frame-1"
                   :frame/type :daily-scan
                   :frame/cardinal-direction {:depositing 0.8}}))
    (let [frame-scan (with-redefs-fn {#'wm/frames-dir (.getPath frame-dir)}
                       wm/scan-frames)
          other {:support {:covered 1 :claims 2}}
          exposures (merge other
                           (:scan-exposures loop-scan)
                           (:scan-exposures mission-scan)
                           (:scan-exposures frame-scan))
          scan-data {:loop-health loop-scan
                     :mission-triage mission-scan
                     :frames frame-scan}
          traced (trace/trace-record {:belief {} :belief-pre {}
                                      :observation (observation/observe scan-data)
                                      :decision nil
                                      :scan-exposures exposures})]
      (is (= (:overall loop-scan)
             (get-in exposures [:loop-health :value])))
      (is (= 0 (get-in exposures [:loop-health :bin])))
      (is (= :wm/scan-bins-unit-5-v1
             (get-in exposures [:loop-health :schema])))
      (is (= {:schema :wm/scan-bins-unit-5-v1 :bin 4 :value 1.0
              :components {:total 1 :completed 1 :blocked 0 :abandoned 0}}
             (:mission-health exposures)))
      (is (= {:schema :wm/scan-bins-unit-5-v1 :bin 4 :value 0.8}
             (:depositing-signal exposures)))
      (is (= exposures (:scan-exposures traced)))
      (is (= other (select-keys (:scan-exposures traced) [:support])))
      (is (= (observation/observe scan-data)
             (observation/observe
               (update-vals scan-data #(dissoc % :scan-exposures))))
          "the binned carriers are write-only for the observation vector"))))

(deftest scalar-bin-missing-sources-are-typed-absences
  (let [loop-scan (wm/scan-loop-health 30 nil)
        mission-scan (wm/scan-mission-triage 30 [] [])
        frame-dir (.toFile (java.nio.file.Files/createTempDirectory
                             "wm-scan-bins-empty" (make-array java.nio.file.attribute.FileAttribute 0)))
        frame-scan (with-redefs-fn {#'wm/frames-dir (.getPath frame-dir)}
                     wm/scan-frames)
        scan-data {:loop-health loop-scan
                   :mission-triage mission-scan
                   :frames frame-scan}]
    (is (= {:status :absent :reason :loop-evidence-unavailable}
           (get-in loop-scan [:scan-exposures :loop-health])))
    (is (= {:status :absent :reason :no-missions
            :components {:total 0 :completed 0 :blocked 0 :abandoned 0}}
           (get-in mission-scan [:scan-exposures :mission-health])))
    (is (= {:status :absent :reason :no-depositing-signal}
           (get-in frame-scan [:scan-exposures :depositing-signal])))
    (is (= {:loop-health (:overall loop-scan)
            :mission-health 0.0 :depositing-signal 0.0}
           (select-keys (observation/observe scan-data)
                        [:loop-health :mission-health :depositing-signal])))))

(deftest learn-uses-mu-pre-and-fractional-counts
  (let [records [(record (mu [:spawned 1/2] [:refined 1/2])
                         {:covered 2 :claims 5}
                         {:status :absent :reason :no-claims})
                 (record (mu [:spawned 1/4] [:refined 3/4])
                         {:covered 1 :claims 4}
                         {:covered 3 :claims 4})]
        learned (scan-model/learn records "entity" prior)]
    (is (= {:alpha 9/4 :beta 13/4 :success 5/4 :failure 9/4}
           (get-in learned [:posterior :support :spawned])))
    (is (= {:alpha 11/4 :beta 19/4 :success 7/4 :failure 15/4}
           (get-in learned [:posterior :support :refined])))
    (is (= {:alpha 13/4 :beta 7/4 :success 9/4 :failure 3/4}
           (get-in learned [:posterior :attack :refined])))
    (is (= [{:record 0 :channel :attack :reason :no-claims}]
           (:excluded learned)))))

(deftest tied-reduction-has-registry-sign
  (testing "equal status rates favour tying"
    (let [records (mapv #(record (mu [% 1]) {:covered 5 :claims 10}
                                {:status :absent :reason :no-claims}) statuses)
          score (scan-model/score (scan-model/learn records "entity" prior)
                                  prior (hand-set))]
      (is (neg? (get-in score [:reductions :support :tied :delta-f])))
      (is (= false (:applied score)))
      (is (= -3.0 (:threshold score)))
      (is (= :wm/scan-model-v1 (:schema score)))))
  (testing "strongly different status rates disfavour tying"
    (let [[a b] statuses
          records [(record (mu [a 1]) {:covered 100 :claims 100}
                           {:status :absent :reason :no-claims})
                   (record (mu [b 1]) {:covered 0 :claims 100}
                           {:status :absent :reason :no-claims})]
          score (scan-model/score (scan-model/learn records "entity" prior)
                                  prior (hand-set))]
      (is (pos? (get-in score [:reductions :support :tied :delta-f]))))))

(deftest hand-set-reduction-is-computed-or-typed
  (let [learned (scan-model/learn
                 [(record (mu [:strengthened 1]) {:covered 5 :claims 5}
                          {:covered 4 :claims 4})]
                 "entity" prior)
        score (scan-model/score learned prior (hand-set))]
    ;; theta = 1.0 at :strengthened, 5/5 covered: reduced log evidence 0,
    ;; full log B(6,1) - log B(1,1) = log(1/6)
    (is (< (Math/abs (- (Math/log (/ 1.0 6))
                        (get-in score [:reductions :support :hand-set :delta-f])))
           1.0e-9))
    ;; one uncovered claim at theta = 1.0 is impossible, not an infinite delta-F
    (is (true? (get-in (scan-model/score
                        (scan-model/learn
                         [(record (mu [:strengthened 1]) {:covered 4 :claims 5}
                                  {:covered 4 :claims 4})]
                         "entity" prior)
                        prior (hand-set))
                       [:reductions :support :hand-set :impossible-under-reduced])))
    (is (= :hand-set-not-comparable
           (get-in (scan-model/score learned prior {})
                   [:reductions :support :hand-set :status])))))

(deftest invalid-prior-is-a-typed-refusal
  (doseq [bad [{:alpha 1 :beta 1} {:alpha 0 :beta 1 :authority "bad"}]]
    (is (= :invalid-prior (:kind (scan-model/learn [] "entity" bad))))
    (is (= :invalid-prior (:kind (scan-model/score {} bad {}))))))
