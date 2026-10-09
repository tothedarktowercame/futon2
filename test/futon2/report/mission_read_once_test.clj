(ns futon2.report.mission-read-once-test
  "WM-MISSION-READ-ONCE-I: one selection reads the mission registry once.
  judge read it for the declared sources and again, through
  cascade-problems/substrate-targets, for the cascade's targets: two reads of
  the same registry, one substrate read apart, in one let. The loaded doc is
  now bound once and passed to both. judge is stopped right after the
  targets are computed (assemble-cascade-problems-with-published captures
  them and throws), with every store it reads before that pointed at an
  empty temp dir. The pre-fix judge is materialised from git (e3bdcfdb)
  under a renamed namespace, never loaded over the real one."
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.mission-hole-wants :as holes]
            [futon2.aif.mission-registry :as mr]
            [futon2.aif.outer-task-selection :as outer-task-selection]
            [futon2.aif.ticket-queue :as ticket-queue]
            [futon2.report.war-machine :as wm]))

(def missions {:missions [{:id "M-live" :status-class :active}
                          {:id "M-done" :status-class :complete}]})

(defn- tmp-dir []
  (str (java.nio.file.Files/createTempDirectory
        "mission-read-once" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- one-selection
  "judge in NS (the war-machine namespace, or its pre-fix copy) with
  JUDGE-OPTS, stopped once the targets exist: {:reads n :targets [...]}."
  ([ns judge-opts] (one-selection ns judge-opts missions))
  ([ns judge-opts mission-doc]
  (let [reads (atom 0)
        load-args (atom [])
        targets (atom nil)
        tmp (tmp-dir)]
    (with-redefs-fn {#'mr/load-missions (fn [& args]
                                         (swap! reads inc)
                                         (swap! load-args conj args)
                                         (if (fn? mission-doc)
                                           (apply mission-doc args)
                                           mission-doc))
                     #'mr/load-tickets (fn [& _] {:tickets []})
                     #'mr/load-excursions (fn [& _]
                                            {:excursions (vec (::excursions judge-opts))})
                     (ns-resolve ns 'assemble-cascade-problems-with-published)
                     (fn [_ input & _]
                       (reset! targets (:targets input))
                       (throw (ex-info "stop: targets computed" {::stop true})))}
      #(try ((ns-resolve ns 'judge) {} (merge {:cascade-sources-dir tmp
                                                :cascade-proposals-dir tmp
                                                :repair-obligations-root tmp
                                                :machine-interpretations-dir tmp
                                                :ticket-queue ticket-queue/empty-declaration}
                                               judge-opts))
            (catch clojure.lang.ExceptionInfo e
              (when-not (::stop (ex-data e)) (throw e)))))
    {:reads @reads :load-args @load-args :targets @targets})))

(def pre-fix-ns
  (delay
    (let [{:keys [exit out err]} (sh/sh "git" "show" "e3bdcfdb:scripts/futon2/report/war_machine.clj")
          renamed 'futon2.report.war-machine-at-e3bdcfdb]
      (when-not (zero? exit) (throw (ex-info "git show failed" {:err err})))
      ;; Compiler/load with a source path: the file registers its load
      ;; identity from *file*, which load-string leaves nil
      (clojure.lang.Compiler/load
       (java.io.StringReader. (str/replace-first out "(ns futon2.report.war-machine" (str "(ns " renamed)))
       "futon2/report/war_machine_at_e3bdcfdb.clj" "war_machine_at_e3bdcfdb.clj")
      (the-ns renamed))))

(deftest one-selection-reads-the-registry-once
  (let [{:keys [reads load-args targets]}
        (one-selection (:ns (meta #'wm/judge)) {})]
    (is (= 1 reads))
    (is (= [[mr/default-code-root]] load-args)
        "selection reads the same pinned checkout authority as run facts")
    (is (= ["M-live"] targets))))

(deftest stale-substrate-cannot-replace-the-pinned-registry-population
  (let [seen (atom nil)
        loader (fn
                 ([] {:missions [{:id "M-stale-substrate-only"
                                  :status-class :unknown}]})
                 ([_] {:missions [{:id "M-current-registry"
                                   :status-class :active}]}))
        selector (fn [{:keys [tasks]}]
                   (reset! seen (mapv :id tasks))
                   {:schema :wm/outer-task-selection-v1
                    :policy {:kind :test}
                    :support (mapv outer-task-selection/task-view tasks)
                    :excluded []
                    :draw {:absent :test}
                    :chosen (outer-task-selection/task-view (first tasks))
                    :action {:type :advance-mission
                             :target (:id (first tasks))}})]
    (one-selection (:ns (meta #'wm/judge))
                   {:outer-task-selection-fn selector :cascade-sources {}}
                   loader)
    (is (= ["M-current-registry"] @seen))
    (is (not (some #{"M-stale-substrate-only"} @seen)))))

(deftest with-declared-sources-the-targets-are-still-the-live-missions
  ;; site 1 is guarded out; the unconditional binding still feeds the targets
  (let [{:keys [reads targets]} (one-selection (:ns (meta #'wm/judge))
                                               {:cascade-sources {}})]
    (is (= ["M-live"] targets))
    (is (= 1 reads))))

(deftest outer-task-ranking-does-not-gate-the-cascade-field
  (let [mission-doc {:missions [{:id "M-a" :status-class :active}
                                {:id "M-b" :status-class :active}]}
        seed 17
        expected (get-in (outer-task-selection/select-task
                          {:tasks [{:id "M-a" :kind :mission :status-class :active}
                                   {:id "M-b" :kind :mission :status-class :active}]
                           :seed seed})
                         [:chosen :id])
        rich-a {:universes {"M-a" {:x false}}
                :interpretations {"M-a" {:patterns {:p/a {:produces #{:x}}}}}
                :candidates {"M-a" [{:precedence [:p/a]
                                      :construction-receipt {:kind :fixture}}]}}
        rich-b {:universes {"M-b" {:y false}}
                :interpretations {"M-b" {:patterns {:p/b {:produces #{:y}}}}}
                :candidates {"M-b" [{:precedence [:p/b]
                                      :construction-receipt {:kind :fixture}}]}}
        first-run (one-selection (:ns (meta #'wm/judge))
                                 {:outer-task-seed seed :cascade-sources rich-a}
                                 mission-doc)
        second-run (one-selection (:ns (meta #'wm/judge))
                                  {:outer-task-seed seed :cascade-sources rich-b}
                                  mission-doc)]
    (is (contains? (set (:targets first-run)) expected))
    (is (= ["M-a" "M-b"] (:targets first-run)))
    (is (= ["M-a" "M-b"] (:targets second-run))
        "prepared cascade material cannot gate the enumerated field")))

(deftest configured-meta-outer-selector-ranks-but-does-not-gate-inner-targets
  (let [mission-doc {:missions [{:id "M-a" :status-class :active}
                                {:id "M-b" :status-class :active}]}
        calls (atom [])
        meta-selector
        (fn [{:keys [tasks seed]}]
          (swap! calls conj {:ids (mapv :id tasks) :seed seed})
          {:schema :wm/outer-task-selection-v1
           :policy {:kind :meta-pipeline-task-state}
           :support (mapv outer-task-selection/task-view tasks)
           :excluded []
           :draw {:absent :meta-policy-does-not-draw}
           :chosen (outer-task-selection/task-view
                    (first (filter #(= "M-b" (:id %)) tasks)))
           :action {:type :advance-mission :target "M-b"}})
        rich-a {:universes {"M-a" {:x false}}
                :interpretations {"M-a" {:patterns {:p/a {:produces #{:x}}}}}}
        run (one-selection (:ns (meta #'wm/judge))
                           {:outer-task-selection-fn meta-selector
                            :cascade-sources rich-a}
                           mission-doc)]
    (is (= ["M-a" "M-b"] (:targets run))
        "META ranking cannot remove an enumerated target")
    (is (= 1 (count @calls)))
    (is (= #{"M-a" "M-b"} (set (:ids (first @calls)))))))

(deftest excursion-documents-enter-the-current-head-want-reader
  (let [captured (atom nil)
        excursion {:id "E-readable" :kind :excursion :status-class :open
                   :path "/fixture/repo/holes/E-readable.md"
                   :text "# E-readable\n\n- [ ] report the measured result\n"}]
    (with-redefs [holes/merge-into-sources
                  (fn [declared _ rows _]
                    (reset! captured (mapv :id rows))
                    declared)]
      (one-selection (:ns (meta #'wm/judge))
                     {:outer-task-selection-fn
                      (fn [{:keys [tasks]}]
                        {:schema :wm/outer-task-selection-v1
                         :policy {:kind :test} :support tasks :excluded []
                         :draw {:absent :test} :chosen (first tasks)
                         :action {:type :advance-excursion :target "E-readable"}})
                      ::excursions [excursion]}
                     {:missions []}))
    (is (= ["E-readable"] @captured))))

(deftest meta-is-fail-closed-and-seeded-baseline-is-explicit
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo #"META outer selector is not configured"
       (wm/select-outer-task {:outer-task-policy :meta}
                             [{:id "M-a" :kind :mission}] 1)))
  (is (= "M-a"
         (get-in (wm/select-outer-task
                  {:outer-task-policy :seeded-baseline}
                  [{:id "M-a" :kind :mission}] 1)
                 [:chosen :id]))))

(deftest the-pre-fix-selection-read-twice
  ;; the bad case: judge at e3bdcfdb, no pass-through
  (let [{:keys [reads targets]} (one-selection @pre-fix-ns {})]
    (is (= 2 reads))
    (is (= ["M-live"] targets) "the same targets: only the count tells them apart")))

(deftest the-zero-arity-still-reads-for-itself
  ;; work_target_predictor_input.clj's caller is unchanged
  (let [reads (atom 0)]
    (with-redefs [mr/load-missions (fn [& _] (swap! reads inc) missions)
                  mr/load-tickets (fn [& _] {:tickets []})
                  mr/load-excursions (fn [& _] {:excursions []})]
      (is (= ["M-live"] (cp/substrate-targets)))
      (is (= 1 @reads)))))
