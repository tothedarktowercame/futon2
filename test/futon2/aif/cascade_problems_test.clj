(ns futon2.aif.cascade-problems-test
  "SPEC-flat-removal-and-cascade-decision H2: per-target cascade problem
  assembly. Targets are mission/ticket identities; every target lands in
  exactly one of :problems / :refusals; a missing input is a typed refusal
  in the fixed order (:universe-not-admitted, :no-admitted-interpretation,
  :want-not-declared, :no-constructed-candidate, :beta-not-declared); a
  missing :horizon-steps refuses all targets; a fully supplied target
  assembles a problem the REAL cascade-lane accepts. Tick 1's inputs
  (vm/tick-001/01..07) are the fixture."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.locator-fixtures :as locfix]
            [futon2.aif.wm.construction-inputs :as construction-inputs]
            [futon2.aif.wm.cascade-decision :as wm-cd]))

(defn- assemble*
  "cp/assemble with every token given a fixture C3 locator (P5 locator
  requirement); tests about locators call cp/assemble directly."
  [m]
  (cp/assemble (update m :sources locfix/locate-all)))

(def target :wm-tick-001-observation-crash)

(def universe
  {:summary-without-total-repos-throws true
   :active-repo-ratio-absent-default-is-0 true
   :coupling-density-reads-same-key-with-default true
   :observe-empty-does-not-throw true
   :test-covers-missing-total-repos false
   :summary-without-total-repos-observes-cleanly :unknown})

(def interpretations
  {:patterns
   {:aif/structured-observation-vector
    {:guard {:needs #{:summary-without-total-repos-throws}
             :forbids #{:summary-without-total-repos-observes-cleanly}}
     :produces #{:summary-without-total-repos-observes-cleanly}}
   :aif/placeholder-is-load-bearing
    {:guard {:needs #{:coupling-density-reads-same-key-with-default
                      :summary-without-total-repos-throws}
             :forbids #{:summary-without-total-repos-observes-cleanly}}
     :produces #{:summary-without-total-repos-observes-cleanly
                 :active-repo-ratio-absent-default-is-0}}
   :test-step-covering-missing-total-repos
    {:guard {:needs #{:summary-without-total-repos-throws}
             :forbids #{:test-covers-missing-total-repos}}
     :produces #{:test-covers-missing-total-repos}}}})

(def want
  [:summary-without-total-repos-observes-cleanly
   :active-repo-ratio-absent-default-is-0
   :test-covers-missing-total-repos])

(def receipt
  {:kind :construction-receipt
   :moves [:interpret :order]
   :family-searched 3
   :coverage 1})

(def candidates
  [{:precedence [:test-step-covering-missing-total-repos
                 :aif/structured-observation-vector]
    :construction-receipt receipt}
   {:precedence [:aif/placeholder-is-load-bearing
                 :test-step-covering-missing-total-repos
                 :aif/structured-observation-vector]
    :construction-receipt receipt}
   {:precedence [:aif/structured-observation-vector]
    :construction-receipt receipt}])

(def full-sources
  {:universes {target universe}
   :interpretations {target interpretations}
   :wants {target want}
   :candidates {target candidates}
   :horizon-steps 3
   :beta-by-context {:tick-1 {:beta 1}}
   :context-of (fn [_] :tick-1)})

(deftest assembled-field-carries-one-source-row-per-target
  (let [m1 "M-one"
        m2 "M-two"
        ticket "T-three"
        targets [m1 m2 ticket]
        rows (construction-inputs/target-source-declarations
              targets
              {:loaded-missions {:missions [{:id m1 :path "/repo/M-one.md"}
                                             {:id m2 :path "/repo/M-two.md"}]}
               :loaded-tickets {:tickets [{:id ticket :path "/repo/T-three.md"
                                           :item-line 17 :status-class :live}]}
               :ticket-targets [ticket]})
        assembled (construction-inputs/assemble-cascade-problems
                   {:targets targets :target-sources rows
                    :sources {:horizon-steps 1}})
        landed (concat (:problems assembled) (:refusals assembled))]
    (is (= [{:target-id m1 :source-kind :head :source-path "/repo/M-one.md"
             :source-absent nil}
            {:target-id m2 :source-kind :head :source-path "/repo/M-two.md"
             :source-absent nil}
            {:target-id ticket :source-kind :item-section :source-path "/repo/T-three.md"
             :item-line 17 :source-absent nil}]
           (:target-sources assembled)))
    (is (= (frequencies targets)
           (frequencies (map :target-id (:target-sources assembled))))
        "every enumerated target has exactly one source row")
    (is (= (set (map :target landed))
           (set (map :target-id (:target-sources assembled))))
        "problem and refusal targets are covered by the source declaration")))

(deftest mission-source-without-path-is-a-typed-absence
  ;; Bad case: the mission id resembles a file name, but no filename
  ;; convention is authority for a source path.
  (is (= [{:target-id "M-no-path" :source-kind :head :source-path nil
           :source-absent :target-source-path-absent}]
         (construction-inputs/target-source-declarations
          ["M-no-path"] {:loaded-missions {:missions [{:id "M-no-path"}]}}))))

(deftest non-mission-sources-retain-the-records-already-read
  (let [ticket "T-ticket"
        proposal "T-repair"
        declared "M-declared"
        rows (construction-inputs/target-source-declarations
              [ticket proposal declared]
              {:loaded-tickets {:tickets [{:id ticket :path "/repo/T-ticket.md"
                                           :item-line 23 :status-class :live}]}
               :ticket-targets [ticket]
               :proposals [{:target proposal
                            :evidence {:finding-source {:path "/store/finding.edn"
                                                       :sha256 "finding-pin"}}}]
               :proposal-targets [proposal]
               :declared-files [{:target declared :path "/resources/declared.edn"
                                 :sha256 "declared-pin"}]
               :declared-targets [declared]})]
    (is (= [{:source-kind :item-section :source-path "/repo/T-ticket.md"
             :item-line 23 :target-id ticket :source-absent nil}
            {:source-kind :inline-bytes :source-path "/store/finding.edn"
             :source-sha256 "finding-pin" :source-origin :repair-proposal
             :target-id proposal :source-absent nil}
            {:source-kind :inline-bytes :source-path "/resources/declared.edn"
             :source-sha256 "declared-pin" :source-origin :declared
             :target-id declared :source-absent nil}]
           rows))))

(deftest conflicting-source-claims-are-retained-and-refused
  (let [rows (construction-inputs/target-source-declarations
              ["T-conflict"]
              {:loaded-tickets
               {:tickets [{:id "T-conflict" :path "/repo/first.md" :item-line 3
                            :status-class :live}
                           {:id "T-conflict" :path "/repo/second.md" :item-line 4
                            :status-class :live}]}
               :ticket-targets ["T-conflict"]})
        row (first rows)]
    (is (= :target-source-conflict (:source-absent row)))
    (is (= #{"/repo/first.md" "/repo/second.md"}
           (set (map :source-path (:claims row)))))))

(deftest ticket-without-an-item-line-is-not-given-one
  (is (= :target-item-line-absent
         (-> (construction-inputs/target-source-declarations
              ["T-no-line"]
              {:loaded-tickets {:tickets [{:id "T-no-line" :path "/repo/T-no-line.md"
                                           :source-kind :item-section
                                           :status-class :live}]}
               :ticket-targets ["T-no-line"]})
             first :source-absent))))

(deftest ticket-own-file-without-item-line-is-a-head-source
  (is (= {:source-kind :head :source-path "/repo/T-own.md"
          :target-id "T-own" :source-absent nil}
         (first (construction-inputs/target-source-declarations
                 ["T-own"]
                 {:loaded-tickets {:tickets [{:id "T-own" :path "/repo/T-own.md"
                                              :status-class :live}]}
                  :ticket-targets ["T-own"]})))))

(deftest ticket-with-parent-item-line-remains-an-item-section
  (is (= {:source-kind :item-section :source-path "/repo/parent.md"
          :item-line 42 :target-id "T-item" :source-absent nil}
         (first (construction-inputs/target-source-declarations
                 ["T-item"]
                 {:loaded-tickets {:tickets [{:id "T-item" :path "/repo/parent.md"
                                              :item-line 42 :status-class :live}]}
                  :ticket-targets ["T-item"]})))))

(defn- kinds
  [result]
  (mapv :kind (:refusals result)))

(deftest h2-assemble
  ;; --- a fully supplied target assembles a problem the REAL cascade-lane
  ;; accepts (tick 1's inputs; route complete, no refusal).
  (let [{:keys [problems refusals]} (assemble* {:targets [target]
                                                  :sources full-sources})]
    (is (and (= 1 (count problems)) (= [] refusals))
        "a fully supplied target lands in :problems, not :refusals")
    (let [problem (first problems)]
      (is (= target (:target problem)))
      (is (= [receipt receipt receipt] (mapv :construction-receipt (:constructed-candidates problem)))
          "each candidate's construction receipt travels with the problem")
      (let [lane (wm-cd/cascade-lane (:cascade-problem problem))]
        (is (and (nil? (:refusal lane)) (nil? (:stopped-at lane)))
            "the real cascade-lane accepts the assembled problem end-to-end")
        (is (= [:R1 :R6 :R13 :R4 :R5 :R14 :R16 :R9] (mapv :node (:route lane)))
            "the lane runs the full node sequence on the assembled problem")))
    (is (= (mapv :precedence candidates)
           (get-in (first problems) [:cascade-problem :precedences]))
        "only the real constructed orders enter the family")))

(deftest h2-refusals-in-order
  ;; 1 :universe-not-admitted — no universe at all (horizon still declared,
  ;; so the per-target order is exercised, not the refuse-all rule)
  (is (= [:universe-not-admitted]
         (kinds (assemble* {:targets [target]
                              :sources (dissoc full-sources :universes)}))))
  (is (= :universes (:missing (first (:refusals
                                       (assemble* {:targets [target]
                                                     :sources (dissoc full-sources
                                                                      :universes)})))))
      "the refusal records which source was absent")
  ;; 2 :no-query-time-slice — without either interpretation operators or
  ;; a retrieved library slice, assembly names the input now required.
  (let [r (assemble* {:targets [target]
                      :sources (assoc-in full-sources
                                         [:interpretations target :patterns] {})})]
    (is (= [:no-query-time-slice] (kinds r)))
    (is (= :query-time-slices (get-in r [:refusals 0 :missing]))))
  ;; 2 also fires for a candidate pattern with no admitted interpretation
  (is (= [:no-admitted-interpretation]
         (kinds (assemble*
                 {:targets [target]
                  :sources (assoc-in full-sources
                                     [:candidates target]
                                     (conj candidates
                                           {:precedence [:aif/pattern-never-interpreted]
                                            :construction-receipt receipt}))}))))
  ;; 3 :want-not-declared
  (is (= [:want-not-declared]
         (kinds (assemble* {:targets [target]
                              :sources (dissoc full-sources :wants)}))))
  ;; 4 :no-constructed-candidate — no candidates at all
  (is (= [:no-constructed-candidate]
         (kinds (assemble* {:targets [target]
                              :sources (dissoc full-sources :candidates)}))))
  ;; 4 — non-empty precedences without construction receipts are proposals,
  ;; not constructed cascades
  (is (= [:no-constructed-candidate]
         (kinds (assemble*
                 {:targets [target]
                  :sources (assoc-in full-sources [:candidates target]
                                     (mapv #(dissoc % :construction-receipt)
                                           candidates))}))))
  (is (= :construction-receipt
         (:missing (first (:refusals
                           (assemble*
                            {:targets [target]
                             :sources (assoc-in full-sources [:candidates target]
                                                (mapv #(dissoc % :construction-receipt)
                                                      candidates))})))))
      "a receipt-less candidate refuses with :missing :construction-receipt"))
  ;; 5 :beta-not-declared — no β for the target's context
  (is (= [:beta-not-declared]
         (kinds (assemble* {:targets [target]
                              :sources (dissoc full-sources :beta-by-context)}))))
  ;; every target lands in exactly one bucket
  (let [r (assemble* {:targets [target :M-other]
                        :sources full-sources})]
    (is (= 2 (+ (count (:problems r)) (count (:refusals r))))
        "every target lands in exactly one of :problems / :refusals")
    (is (= [:universe-not-admitted]
           (mapv :kind (filter (fn [x] (= :M-other (:target x))) (:refusals r))))))

(deftest h2-missing-horizon-refuses-all
  (let [r (assemble* {:targets [target :M-other :T-other]
                        :sources (dissoc full-sources :horizon-steps)})]
    (is (= [] (:problems r))
        "nothing is assembled without a declared horizon")
    (is (= [:horizon-not-declared :horizon-not-declared :horizon-not-declared]
           (kinds r))
        "a missing :horizon-steps refuses ALL targets")
    (is (every? #(= :horizon-steps (:missing %)) (:refusals r))
        "each refusal records which source was absent")))

(deftest h2-empty-sources-refuse-everything
  (let [targets [:M-foo :T-bar]
        r (assemble* {:targets targets :sources {}})]
    (is (= [] (:problems r))
        "with empty sources nothing is assembled")
    (is (= 2 (count (:refusals r)))
        "every target is refused (fixture list; the real list works the same)")
    (is (every? :kind (:refusals r))
        "each refusal is typed — the empty-sources tick abstains with the list")))

(deftest locators-are-required
  ;; Joe 2026-09-17: no per-pass blinded study, so every token must be
  ;; mechanically observable. A token with no checkable locator refuses the
  ;; universe and names the token.
  (let [r (cp/assemble {:targets [target] :sources full-sources})
        refusal (first (:refusals r))]
    (is (empty? (:problems r)))
    (is (= :universe-not-admitted (:kind refusal)))
    (is (= :locators (:missing refusal)))
    (is (seq (:tokens-without-checkable-locator refusal))))
  (let [located (locfix/locate-all full-sources)
        some-token (first (keys (get-in located [:locators target])))
        judged (assoc-in located [:locators target some-token] {:class :J})
        r (cp/assemble {:targets [target] :sources judged})]
    (is (= [some-token] (:tokens-without-checkable-locator (first (:refusals r)))))
    (is (= 1 (count (:problems (cp/assemble {:targets [target] :sources located})))))))


(deftest filtered-empty-proposals-are-recorded-without-renumbering-real-ones
  (let [r (assemble* {:targets [target]
                      :sources (assoc-in full-sources [:candidates target]
                                         [{:precedence [] :construction-receipt receipt}
                                          (first candidates)])})
        p (first (:problems r))]
    (is (= [:C2] (mapv :candidate-id (:constructed-candidates p))))
    (is (= [(get-in candidates [0 :precedence])] (get-in p [:cascade-problem :precedences])))
    (is (= [{:target target :candidate :C1 :stage :construction-admission
             :reason :unconstructed-proposal :missing-evidence [:nonempty-precedence]}]
           (:dropped-candidates r)))))

;; Construction at assembly: a target with admitted interpretations and no
;; declared candidate gets candidates from the constructor when the sources
;; supply :construction; without it the old refusal stands.
(def construction
  {:construct (requiring-resolve 'futon2.aif.interpretation-construction/construct)
   :budget {:max-moves 4 :max-expansions 2000} :move-cost 1
   ;; the empty cascade reaches no want, so it scores worst, as the live G does
   :evaluate-g (fn [_problem c] (if (empty? (:precedence c)) 1.0e9 (double (count (:precedence c)))))})

(defn- receipts-for [patterns]
  (into {} (for [k (keys patterns)] [k {:source :test}])))

(deftest constructs-candidates-when-none-declared
  (let [srcs (-> full-sources
                 (update :candidates dissoc target)
                 (assoc-in [:interpretations target :receipts] (receipts-for (:patterns interpretations)))
                 (assoc :construction construction))
        {:keys [problems refusals]} (assemble* {:targets [target] :sources srcs})
        p (first problems)]
    (is (empty? refusals) (pr-str refusals))
    (is (= target (:target p)))
    (is (seq (:constructed-candidates p)))
    (is (every? #(= :machine-constructed (get-in % [:construction-receipt :kind])) (:constructed-candidates p)))
    ;; the want token held :unknown is what the plan produces
    (is (every? #(some #{:summary-without-total-repos-observes-cleanly}
                       (get-in % [:construction-receipt :unknown-read-as-not-established]))
                (:constructed-candidates p)))))

(deftest no-construction-without-construction-source
  (let [srcs (update full-sources :candidates dissoc target)
        {:keys [refusals]} (assemble* {:targets [target] :sources srcs})]
    (is (= :no-constructed-candidate (:kind (first refusals))))))

(deftest constructor-refusal-is-carried
  ;; the bad case: a pattern needs a token no pattern produces and the
  ;; universe does not establish, so no plan reaches the want
  (let [bad (assoc-in interpretations [:patterns :test-step-covering-missing-total-repos :guard :needs]
                      #{:test-covers-missing-total-repos-precondition})
        srcs (-> full-sources
                 (update :candidates dissoc target)
                 (assoc :interpretations {target (assoc bad :receipts (receipts-for (:patterns bad)))})
                 (assoc-in [:universes target :test-covers-missing-total-repos-precondition] false)
                 (assoc :construction construction))
        {:keys [problems refusals]} (assemble* {:targets [target] :sources srcs})]
    (is (empty? problems))
    (is (= :no-constructed-candidate (:kind (first refusals))))
    (is (some? (:constructor-refusal (first refusals))))))

(deftest base-problem-extraction-preserves-assembly
  ;; Captured assembly shape, updated by S2 only for the typed missing-slice refusal.
  ;; Includes competing missing-candidate / missing-beta refusals.
  (let [s (locfix/locate-all full-sources)
        variants [s (dissoc s :universes) (dissoc s :interpretations)
                  (dissoc s :wants) (dissoc s :candidates)
                  (dissoc s :beta-by-context) (dissoc s :candidates :beta-by-context)]
        printed (pr-str (mapv #(cp/assemble {:sources % :targets [target]}) variants))
        digest (.digest (java.security.MessageDigest/getInstance "SHA-256")
                        (.getBytes printed "UTF-8"))]
    (is (= "163ebd2771963294e582feb81266feffb990804fe8f1f07cfc5c22195158047c"
           (apply str (map #(format "%02x" %) digest))))
    (is (= (dissoc (get-in (cp/assemble {:sources s :targets [target]})
                           [:problems 0 :cascade-problem]) :precedences)
           (cp/base-problem s 3 target)))
    (is (= [:universe-not-admitted :no-query-time-slice :want-not-declared
            :beta-not-declared]
           (mapv #(:kind (cp/base-problem (dissoc s %) 3 target))
                 [:universes :interpretations :wants :beta-by-context])))))


(deftest query-time-slices-survive-assembly-before-interpretation
  (let [targets (mapv #(keyword (str "slice-target-" %)) (range 5))
        slice-for (fn [t]
                    {:schema :wm/query-time-library-slice-v1
                     :target t :query "close target"
                     :candidates [{:pattern :library/inspect :slice-rank 1
                                   :retriever "embedding" :retriever-rank 1
                                   :provenance {:source :fixture}
                                   :judgment :unjudged}]
                     :failures [] :slice-size 1 :library-size 1415})
        sources (-> full-sources
                    (assoc :universes (into {} (map (fn [t] [t universe]) targets))
                           :interpretations (into {} (map (fn [t] [t {:patterns {}}]) targets))
                           :query-time-slices (into {} (map (fn [t] [t (slice-for t)]) targets))
                           :wants (into {} (map (fn [t] [t want]) targets))
                           :candidates {}
                           :context-of (constantly :tick-1)))
        assembled (assemble* {:targets targets :sources sources})]
    (is (empty? (:refusals assembled)))
    (is (= targets (mapv :target (:problems assembled))))
    (is (= (repeat 5 1) (map :slice-size (:problems assembled))))
    (is (= (repeat 5 1415) (map :library-size (:problems assembled))))
    (is (every? #(= :interpretation-owed-after-selection
                    (get-in % [:cascade-problem :pattern-operators :reason]))
                (:problems assembled)))
    (is (every? #(= [:library/inspect]
                    (mapv :pattern (get-in % [:cascade-problem :pattern-pool])))
                (:problems assembled)))
    (is (every? #(= {:source :fixture}
                    (get-in % [:cascade-problem :pattern-pool 0 :provenance]))
                (:problems assembled)))))

(deftest interpretation-backed-assembly-is-unchanged-by-slice-support
  (let [before (assemble* {:targets [target] :sources full-sources})
        with-unread-slice
        (assemble* {:targets [target]
                    :sources (assoc-in full-sources [:query-time-slices target]
                                       {:schema :wm/query-time-library-slice-v1
                                        :target target :query "unused"
                                        :candidates [{:pattern :library/not-admitted}]
                                        :failures [] :slice-size 1 :library-size 1})})]
    (is (= before with-unread-slice))))
