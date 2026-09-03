(ns futon2.aif.survey-mission-value-test
  "U24. What is pinned here is the boundary between what this term MEASURES and
   what it DECLARES, plus the two identities the design rests on: that the
   default path is byte-identical, and that a survey candidate's G-core carries
   no target information at all, so the epistemic leg is the only thing that
   tells two survey candidates apart."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.efe :as efe]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.mission-epistemic-value :as mev]
            [futon2.aif.portfolio-action-proposer :as pap]
            [futon2.aif.survey-mission-value :as smv]))

(def ratchet-doc
  "/home/joe/code/futon0/holes/missions/M-apm-capability-ratchet.md")

(def u23-artifact
  "holes/labs/wm-contract/runs/U23-cascade-catalog/carrier-population.edn")

;; ---------------------------------------------------------------------------
;; (a) the MAP-question carrier
;; ---------------------------------------------------------------------------

(deftest the-map-question-list-is-read-off-the-real-doc
  (testing "the one doc on this field that carries the carrier"
    (when (.exists (io/file ratchet-doc))
      (let [got (smv/map-questions-from-text (slurp ratchet-doc))]
        (is (= 260 (:heading-line got))
            "the heading line C492 section 4c pointed at")
        (is (= 6 (count (:questions got))))
        (is (= [1 2 3 4 5 6] (mapv :ordinal (:questions got))))
        (is (nil? (:absence got)))
        (is (every? #(re-find #"\?$" (:text %)) (:questions got))
            "each item is a question")))))

(deftest a-doc-without-the-carrier-gets-a-typed-absence-not-zero-questions
  (let [got (smv/map-questions-from-text "# M-nothing\n\nSome prose.\n")]
    (is (empty? (:questions got)))
    (is (= :no-map-question-list (get-in got [:absence :reason])))))

(deftest the-near-miss-phrasings-are-deliberately-not-matched
  (testing "widening the regex to collect these is the move U22's first run had to undo"
    (doseq [phrase ["Questions the evidence bundle must answer:"
                    "Questions the audit must answer:"
                    "each with the question MAP must answer. The"
                    "with the question MAP must answer stated as: already done?"]]
      (let [text (str "## Section\n\n" phrase "\n\n1. first\n2. second\n")]
        (is (= :no-map-question-list
               (get-in (smv/map-questions-from-text text) [:absence :reason]))
            (str "must not match: " phrase)))))
  (testing "and each one is on the record with a pointer"
    (is (= 4 (count smv/near-miss-phrasings)))
    (is (every? #(re-find #":\d+$" (:pointer %)) smv/near-miss-phrasings))))

(deftest a-heading-with-no-numbered-items-is-its-own-typed-absence
  (is (= :map-question-heading-with-no-numbered-items
         (get-in (smv/map-questions-from-text "MAP must answer:\n\nprose, not a list\n")
                 [:absence :reason]))))

;; ---------------------------------------------------------------------------
;; (b) the kin-catalog gap
;; ---------------------------------------------------------------------------

(deftest the-carrier-partition-is-u23s-eleven-carriers-and-nothing-invented
  (is (= 11 (count smv/catalog-carriers)))
  (is (= 5 (count smv/answerable-carriers)))
  (is (= 6 (count smv/unanswerable-carriers)))
  (is (empty? (set/intersection (set smv/answerable-carriers)
                                        (set smv/unanswerable-carriers))))
  (testing "against U23's own artifact, if it is present"
    (when (.exists (io/file u23-artifact))
      (let [rec (first (:cascade-records (edn/read-string (slurp u23-artifact))))]
        (is (= (set (map :carrier smv/catalog-carriers))
               (set (keys (:carriers rec))))
            "the partition names exactly the carriers U23 read")))))

(deftest a-carrier-the-catalog-already-read-is-settled-and-worth-nothing
  (let [uncatalogued (smv/survey-questions {:catalog-readings #{}})
        catalogued (smv/survey-questions {:catalog-readings (set smv/answerable-carriers)})]
    (is (= 5 (count (:questions uncatalogued))))
    (is (= 5 (count (:questions catalogued))))
    (is (every? #(= 0.5 (:p %)) (:questions uncatalogued)))
    (is (every? #(= 1.0 (:p %)) (:questions catalogued)))
    (is (< (Math/abs (- (* 5 (Math/log 2.0))
                        (reduce + 0.0 (map :nats (:questions uncatalogued)))))
           1.0e-12))
    (is (zero? (reduce + 0.0 (map :nats (:questions catalogued))))
        "surveying a mission the catalog already holds gains nothing here")))

(deftest the-unanswerable-carriers-are-excluded-and-counted-not-paid
  (let [q (smv/survey-questions {:catalog-readings #{}})]
    (is (= 6 (:excluded-carrier-count q)))
    (is (= (set smv/unanswerable-carriers) (set (:excluded-carriers q))))
    (is (empty? (filter (fn [x] (contains? (set smv/unanswerable-carriers)
                                           (second (:question x))))
                        (:questions q)))
        "paying ln 2 for a carrier no survey could read is the malformed-cross-reference bug")))

;; ---------------------------------------------------------------------------
;; the term
;; ---------------------------------------------------------------------------

(deftest availability-gates-the-nats-and-a-verify-phase-survey-earns-none
  (let [base {:mission-id "M-x" :catalog-readings #{}}
        v (smv/survey-value (assoc base :phase "verify"))
        m (smv/survey-value (assoc base :phase "map"))
        i (smv/survey-value (assoc base :phase "identify"))]
    (is (= :measured (:status v)))
    (is (zero? (:nats v)) "phase-survey-availability verify = 0.0")
    (is (= (* 5 (Math/log 2.0)) (:raw-nats v))
        "the raw nats are still measured and reported; availability is the gate")
    (is (< (:nats i) (:nats m)))
    (is (= 0.6 (:availability i)))))

(deftest an-unreadable-phase-is-a-typed-absence-not-a-measured-zero
  (is (= :phase-unreadable (:status (smv/survey-value {:mission-id "M-x" :phase nil}))))
  (is (= :phase-unreadable (:status (smv/survey-value {:mission-id "M-x" :phase "unknown"}))))
  (is (zero? (:nats (smv/survey-value {:mission-id "M-x" :phase "banana"}))))
  (is (= :phase-unreadable (:status (smv/survey-value {:mission-id "M-x" :phase "banana"}))))
  (testing "and a phase the doability table knows but the availability table does not"
    (is (contains? mev/phase-doability-prior "unknown"))
    (is (not (contains? mev/phase-survey-availability "unknown")))))

(deftest the-availability-table-is-reused-and-not-redeclared
  (is (identical? mev/phase-survey-availability
                  (var-get (resolve 'futon2.aif.mission-epistemic-value/phase-survey-availability)))
      "there is one availability table in the codebase, U22's"))

(deftest every-question-goes-through-the-one-kernel
  (testing "an open latent is exactly ln 2 and a settled one exactly 0, from mev/latent-eig"
    (let [qs (:questions (smv/survey-questions
                          {:catalog-readings #{:clocked-on}
                           :map-questions {:questions [{:ordinal 1 :line 9 :text "q?"}]}}))]
      (is (= 6 (count qs)) "1 MAP question + 5 catalog carriers")
      (doseq [q qs]
        (is (= (mev/latent-eig (:p q)) (:nats q)))))))

(deftest a-map-question-is-open-because-nothing-answers-it-not-because-we-assume-so
  (let [qs (:questions (smv/survey-questions
                        {:catalog-readings (set smv/answerable-carriers)
                         :map-questions {:questions [{:ordinal 1 :line 262 :text "q?"}]}}))
        mq (first (filter #(= :map-question (first (:question %))) qs))]
    (is (= :no-per-question-answer-carrier (get-in mq [:basis :reason])))
    (is (= 262 (get-in mq [:basis :doc-line])) "the absence carries a pointer")))

;; ---------------------------------------------------------------------------
;; the G leg
;; ---------------------------------------------------------------------------

(def demo-state
  "FIXTURE, copied verbatim from the `:observation` of
   `data/wm-trace/wm-trace-2026-09-02.edn` line 2 (run
   4abad68c-5481-4402-8f0e-252add62c54b) -- the tick whose decision selected
   M-zaif-harness-v1, and the field
   `holes/labs/wm-contract/u24_survey_mission.clj` stages. All fourteen
   observation channels, because a three-channel toy observation gives the risk
   and ambiguity legs a different shape and `:no-op` wins on it.

   The belief is empty on purpose: G-risk and G-ambiguity are computed from the
   predicted observation mean and variance only, so every score below is
   identical with the record's 417-entry `:mu-pre` in place. Carrying the empty
   map keeps that independence visible instead of implying the belief was
   consulted."
  {:observation {:mathematics-pct 9.436929852154766E-4
                 :coupling-density 0.4666666666666667
                 :support-coverage 0.6
                 :depositing-signal 0.0
                 :loop-health 0.5168326459030906
                 :mission-health 0.023376623376623377
                 :portfolio-pct 0.002201950298836112
                 :annotation-health 0.9951690821256038
                 :ticks-firing-ratio 0.0
                 :active-repo-ratio 0.75
                 :attack-coverage 0.5
                 :consulting-pct 0.0
                 :sorry-count-norm 0.0
                 :stack-pct 0.9968543567159485}
   :belief {}})

(def demo-opts
  {:risk-mode :kl :ambiguity-mode :gaussian-entropy :goal-outcome-mode :kl
   :structural-pressure-mode :habit-prior
   :predictability-control-mode :telemetry-only
   :homeostatic-control-mode :telemetry-only
   :graph-feasibility-mode :policy-support})

(defn- survey-action [nats]
  (cond-> {:type :survey-mission :target "M-x" :weight 0.3}
    nats (assoc :survey-eig-nats nats)))

(deftest the-default-weight-is-zero-and-the-default-path-is-byte-identical
  (is (zero? efe/default-survey-eig-weight))
  (let [bare (efe/compute-efe demo-state (survey-action nil) demo-opts)
        laden (efe/compute-efe demo-state (survey-action 7.6246) demo-opts)]
    (is (not (contains? (:augmentation-terms laden) :survey-eig)))
    (is (not (contains? laden :survey-eig-contribution)))
    (is (= (:controller-score bare) (:controller-score laden))
        "carrying a payload changes nothing until the weight is declared")))

(deftest the-leg-is-subtracted-in-nats-at-exactly-the-declared-rate
  (let [dark (efe/compute-efe demo-state (survey-action 7.0) demo-opts)
        live (efe/compute-efe demo-state (survey-action 7.0)
                              (assoc demo-opts :survey-eig-weight 0.02))]
    (is (= -0.14 (get-in live [:augmentation-terms :survey-eig])))
    (is (= (:G-core dark) (:G-core live))
        "invariant I3: the epistemic leg is augmentation, not G-core")
    (is (< (Math/abs (- (- (double (:controller-score dark)) 0.14)
                        (double (:controller-score live))))
           1.0e-12))
    (is (= 0.02 (:survey-eig-weight live)))
    (is (= 7.0 (:survey-eig-nats live)))))

(deftest a-candidate-with-no-payload-is-skipped-not-imputed-a-zero
  (let [no-payload (efe/compute-efe demo-state (survey-action nil)
                                    (assoc demo-opts :survey-eig-weight 0.02))]
    (is (not (contains? (:augmentation-terms no-payload) :survey-eig)))
    (is (not (contains? no-payload :survey-eig-nats))
        "an unmeasured candidate must not record a measured-looking 0.0")))

(deftest survey-candidates-carry-no-target-information-at-g-core
  (testing "the identity the whole design rests on: predict-effects ignores :target"
    (let [a (efe/compute-efe demo-state {:type :survey-mission :target "M-a" :weight 0.3} demo-opts)
          b (efe/compute-efe demo-state {:type :survey-mission :target "M-b" :weight 0.3} demo-opts)]
      (is (= (:G-core a) (:G-core b)))
      (is (= (:G-risk a) (:G-risk b)))
      (is (= (:G-ambiguity a) (:G-ambiguity b)))))
  (testing "so the epistemic leg is the only thing that can order two of them"
    (let [w {:survey-eig-weight 0.02}
          a (efe/compute-efe demo-state (assoc (survey-action 7.0) :target "M-a") (merge demo-opts w))
          b (efe/compute-efe demo-state (assoc (survey-action 1.0) :target "M-b") (merge demo-opts w))]
      (is (< (double (:controller-score a)) (double (:controller-score b)))))))

;; ---------------------------------------------------------------------------
;; addressability, dark by default
;; ---------------------------------------------------------------------------

(def demo-missions
  [{:id "M-a" :path "/tmp/does-not-exist-M-a.md" :status-class :open}
   {:id "M-b" :path "/tmp/does-not-exist-M-b.md" :status-class :identify}])

(deftest survey-mission-is-not-addressable-until-the-portfolio-proposer-is-armed
  (let [state {:missions demo-missions}]
    (is (false? (fm/can-propose? state :survey-mission)))
    (is (false? (fm/can-execute? state {:type :survey-mission :target "M-a"})))
    (binding [pap/*portfolio-proposer-active?* true]
      (is (true? (fm/can-propose? state :survey-mission)))
      (is (true? (fm/can-execute? state {:type :survey-mission :target "M-a"})))
      (is (false? (fm/can-execute? state {:type :survey-mission :target "M-not-here"}))))))

(deftest the-enricher-passes-everything-through-when-there-are-no-readings
  (let [cands [{:type :survey-mission :target "M-a"} {:type :advance-mission :target "M-b"}]]
    (is (= cands (smv/enrich-survey-candidates nil cands)))))

(deftest the-enricher-touches-only-survey-candidates
  (let [readings {:missions {"a" {:mission-id "a" :phase "map" :catalog-readings #{}}}}
        cands [{:type :survey-mission :target "M-a"} {:type :advance-mission :target "M-a"}]
        out (smv/enrich-survey-candidates readings cands)]
    (is (contains? (first out) :survey-eig-nats))
    (is (not (contains? (second out) :survey-eig-nats)))
    (is (pos? (:survey-eig-nats (first out))))))

(deftest a-mission-absent-from-the-readings-is-typed-not-scored
  (let [readings {:missions {}}
        [c] (smv/enrich-survey-candidates readings [{:type :survey-mission :target "M-ghost"}])]
    (is (zero? (:survey-eig-nats c)))
    (is (= :mission-absent-from-survey-readings (get-in c [:survey-eig-basis :status])))))

;; ---------------------------------------------------------------------------
;; the planted IDENTIFY demonstration, pinned
;; ---------------------------------------------------------------------------

(deftest an-identify-phase-mission-prefers-surveying-itself-to-advancing-itself
  (testing "U24's acceptance demonstration, on the demonstration's own field"
    (let [reading {:mission-id "planted-identify-demo"
                   :phase "identify"
                   :catalog-readings #{}
                   :map-questions {:questions (mapv (fn [i] {:ordinal i :line (+ 100 i)
                                                             :text (str "planted MAP question " i)})
                                                    [1 2 3])}}
          readings {:missions {"planted-identify-demo" reading}}
          [survey] (smv/enrich-survey-candidates
                    readings
                    [{:type :survey-mission :target "M-planted-identify-demo" :weight 0.3}])
          advance {:type :advance-mission :target "M-planted-identify-demo"
                   :weight 1.0 :open-hole-count 1}
          ranked (efe/rank-actions demo-state [advance survey {:type :no-op}]
                                   (assoc demo-opts :survey-eig-weight 0.02))]
      (is (= 8 (count (:questions (smv/survey-value reading))))
          "3 planted MAP questions + 5 answerable catalog carriers")
      (is (< (Math/abs (- (* 0.6 8 (Math/log 2.0)) (:survey-eig-nats survey))) 1.0e-12))
      (is (= :survey-mission (get-in (first ranked) [:action :type])))
      (is (= "M-planted-identify-demo" (get-in (first ranked) [:action :target])))
      (is (= :advance-mission (get-in (second ranked) [:action :type]))))))

;; ---------------------------------------------------------------------------
;; the finding this row did not go looking for
;; ---------------------------------------------------------------------------

(deftest a-survey-candidate-outranks-an-advance-candidate-on-declared-determinism
  (testing "and NOT on information gain -- the epistemic leg is off here"
    (let [survey (efe/compute-efe demo-state {:type :survey-mission :target "M-x" :weight 0.3}
                                  demo-opts)
          advance (efe/compute-efe demo-state {:type :advance-mission :target "M-x"
                                               :weight 1.0 :mission-value-factor 0.3}
                                   demo-opts)]
      (is (< (double (:G-ambiguity survey)) (double (:G-ambiguity advance)))
          "survey's ambiguity is better")
      (is (< (Math/abs (- 7.915207 (- (double (:G-ambiguity advance))
                                      (double (:G-ambiguity survey)))))
             1.0e-5)
          (str "the advantage is exactly one channel's worth of declared variance: "
               ":advance-mission declares :sorry-count-norm 0.01 and :survey does not, "
               "so :survey takes the 1e-9 floor there. C487 section 5 measured the same "
               "7.915207 nats on the :fire-pattern/:advance-mission pair."))
      (is (not (contains? (:augmentation-terms survey) :survey-eig))
          "no epistemic leg is involved in that advantage")))
  (testing "the two variance profiles that produce it"
    (let [sv (get-in (efe/compute-efe demo-state {:type :survey-mission :target "M-x"} demo-opts)
                     [:prediction :next-observation :variance])
          av (get-in (efe/compute-efe demo-state {:type :advance-mission :target "M-x"} demo-opts)
                     [:prediction :next-observation :variance])]
      (is (= 1 (count (remove (comp zero? val) sv))) ":survey declares variance on one channel")
      (is (= 2 (count (remove (comp zero? val) av))) ":advance-mission declares two"))))

;; ---------------------------------------------------------------------------
;; discharge
;; ---------------------------------------------------------------------------

(deftest the-discharge-shape-is-declared-and-says-why-it-cannot-be-written
  (is (= :mission (:grain smv/discharge-shape)))
  (is (= :declared-not-implemented (:status smv/discharge-shape)))
  (is (= :writer-exists-no-records (get-in smv/discharge-shape [:blocked-by :reason]))
      "U23's measurement, not an opinion: zero *.flight.edn anywhere under ~/code"))
