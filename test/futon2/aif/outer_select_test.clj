(ns futon2.aif.outer-select-test
  (:require [clojure.edn :as edn]
            [clojure.string :as string]
            [futon2.aif.outer-menu :as menu]
            [futon2.aif.outer-menu-fixture :as menu-fixture]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-score :as outer-score]
            [futon2.aif.outer-select :as outer-select]
            [futon2.aif.action-identity :as identity]
            [wm-run-facts :as facts]
            [wm-check-run-facts :as checker]))

(clojure.test/use-fixtures :each menu-fixture/with-fixture-constructor)

(deftest task-selection-library-root-follows-installation-root
  (is (= "/srv/wm/code/futon3/library/task-selection"
         (outer-select/configured-task-selection-library-root
          {"FUTON_CODE_ROOT" "/srv/wm/code/"})))
  (is (= "/srv/patterns/task-selection"
         (outer-select/configured-task-selection-library-root
          {"FUTON_CODE_ROOT" "/ignored/code"
           "FUTON_PATTERN_LIBRARY_ROOT" "/srv/patterns/"})))
  (is (= "/srv/task-selection"
         (outer-select/configured-task-selection-library-root
          {"FUTON_CODE_ROOT" "/ignored/code"
           "FUTON_PATTERN_LIBRARY_ROOT" "/ignored/patterns"
           "FUTON_TASK_SELECTION_LIBRARY_ROOT" "/srv/task-selection"})))
  (is (= (str (System/getProperty "user.home")
              "/code/futon3/library/task-selection")
         (outer-select/configured-task-selection-library-root {}))))

(deftest c2-exclusion-is-accepted-and-unknown-reason-is-refused
  (let [partition {:field [{:target "M-c2"} {:target "M-other"}]
                   :excluded [] :requests []}
        field (:field partition)
        exclusion {:target "M-c2" :reason :c2-legitimate-question
                   :pin "evidence-pin"}]
    (is (= ["M-other"]
           (mapv :target (:field (outer-select/apply-c1-exclusions
                                  partition field [exclusion])))))
    (is (= :invalid-c1-exclusions
           (try
             (outer-select/apply-c1-exclusions
              partition field [(assoc exclusion :reason :unknown)])
             nil
             (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))))

(deftest selection-why-names-the-served-ways-and-action
  (let [rendered (outer-select/render-selection-why
                  :class
                  {:cascades [:serves-a-way]
                   :pick "M-work"
                   :steps [{:step :placed-gate :in 3 :out 1}]
                   :placement {:ways ["way-alpha" "way-beta" "way-gamma"]
                               :action "work"}
                   :source {:header {:date :absent :status :absent :timebox :absent}}
                   :terms {} :G 1.0})]
    (is (string/includes? rendered
                          "Serves way-alpha, also way-beta (work)."))))

(def base-makeup
  ;; Real task-selection patterns for the two gate roles, so the canonical
  ;; selection G interprets each by its own @guard / @predicts.
  {:cascades [{:id :first :patterns ["only-what-can-start-now"] :steps [{:gate-status :ready}]}
              {:id :second :patterns ["find-out-before-building"] :steps [{:gate-status :undetermined}]}]})
(def base-state
  {:c {:focused 55 :related 35 :unrelated 5 :stop-the-line 5
       :not-yet-evaluated 0}
   :state-counts {:ready {:doable 3 :blocked 1}
                  :not-ready {:doable 1 :blocked 3}
                  :undetermined {:doable 1 :blocked 1}}
   :gain-counts {:none 1 :one 2 :several 3}})
(def base-observation
  {:focus "M-focus"
   :relation (fn [_ target] (if (= target "M-ready") :focus :associated))
   :field [{:target "M-ready" :reach 1/2 :reach-source :default-reach}
           {:target "M-unknown" :reach 1/2 :reach-source :default-reach}]
   :facts {"M-ready" {:status :ready :unblocks 1 :cone 2}
           "M-unknown" {:status :undetermined :unblocks 0 :cone 1}}})

(deftest chosen-outer-cascade-retains-typed-enactment-pin
  (let [discovery-makeup
        {:cascades [{:id :find-out-before-building :patterns ["test"]
                     :steps [:ready-gate]}
                    {:id :discovery-companion :patterns ["test"]
                     :steps [{:gate-status :undetermined}]}]
         :enactment-modes {:find-out-before-building :discovery
                           :discovery-companion :discovery}}
        discovery (outer-select/select discovery-makeup base-state base-observation)
        implementation (outer-select/select
                        (assoc base-makeup
                               :enactment-modes {:first :implementation
                                                 :second :implementation})
                        base-state base-observation)]
    (is (= {:mode :discovery
            :outer-cascade-id :find-out-before-building
            :selection-sha (outer-select/enactment-selection-sha discovery)}
           (:enactment discovery)))
    (is (= :implementation (get-in implementation [:enactment :mode])))
    (is (= (:cascade-id implementation)
           (get-in implementation [:enactment :outer-cascade-id])))
    (is (= (:enactment discovery)
           (outer-select/validate-enactment-pin! discovery)))
    (is (= (:enactment discovery)
           (outer-select/validate-enactment-pin!
            (update discovery :selection dissoc :candidates))))
    (doseq [id [:giraffe-order :serves-a-way]]
      (let [selected (outer-select/select
                      {:cascades [{:id id :patterns ["test"] :steps [:ready-gate]}
                                  {:id :companion :patterns ["test"]
                                   :steps [{:gate-status :undetermined}]}]
                       :enactment-modes {id :implementation
                                        :companion :implementation}}
                      base-state base-observation)]
        (is (= :implementation (get-in selected [:enactment :mode])))
        (is (= id (get-in selected [:enactment :outer-cascade-id])))))))

(deftest outer-enactment-mode-never-defaults-or-merges-conflicts
  (let [same-policies {:cascades [{:id :a :patterns ["test"] :steps [:ready-gate]}
                                  {:id :b :patterns ["test"] :steps [:ready-gate]}
                                  {:id :c :patterns ["test"]
                                   :steps [{:gate-status :undetermined}]}]}
        failure (fn [makeup]
                  (try (outer-select/select makeup base-state base-observation)
                       nil
                       (catch clojure.lang.ExceptionInfo e (ex-data e))))
        unmapped (failure (assoc same-policies :enactment-modes {:a :implementation}))
        conflict (failure (assoc same-policies
                                 :enactment-modes {:a :implementation :b :discovery}))]
    (is (= :outer-selection/enactment-mode-unmapped (:kind unmapped)))
    (is (= [:b] (:cascades unmapped)))
    (is (= :outer-selection/enactment-mode-conflict (:kind conflict)))
    (is (= #{:implementation :discovery} (:modes conflict)))))

(deftest click-12-trace-is-over-the-admitted-scoring-field
  (let [record (edn/read-string
                (slurp "/home/joe/code/futon2/data/wm-runs/selection-record-2026-11-01-wm-click-7d89656e-b180-4f9e-bfb0-780d225b41de.edn"))
        selection (:outer/selection record)
        field (mapv (fn [target] {:target target}) (:outer-score/admitted selection))
        facts (get-in selection [:field-facts :admitted])
        cascades (into {} (map (juxt :id :steps) (:cascades selection)))
        traced #(outer-score/apply-selection-cascade-traced (cascades %) facts field)
        find-trace (traced :find-out-before-building)
        giraffe-trace (traced :giraffe-order)]
    (is (= "24defce4d95cf3be18cbd71e0a9537ec9c22cc2e4cd1661fe9201c8d951f8e60"
           (get-in selection [:field-facts :giraffe/pass-sha256])))
    (is (= 591 (count field)))
    (is (= [{:step {:status-gate :undetermined} :in 591 :out 47}
            {:step :by-rank :pick-value 1
             :runner-up {:target "M-apm-capability-ratchet" :value 10} :margin 9.0}]
           (:trace find-trace)))
    (is (= [{:step :ready-gate :in 591 :out 494}
            {:step :by-cone :pick-value 9
             :runner-up {:target "M-autoclock-in" :value 4} :margin 5.0}
            {:step :by-unblocks :pick-value 1
             :runner-up {:target "E-memory-whitepaper-v2-programme" :value 1} :margin 0.0}]
           (:trace giraffe-trace)))
    (doseq [[id steps] cascades]
      (is (= (outer-score/cascade-pick steps facts field)
             (first (:field (traced id))))))))

;; ---------------------------------------------------------------------------
;; Pre-selection duplicate exclusion (DUPLICATE-TARGET-STILL-SELECTED, 2026-10-31)
;; ---------------------------------------------------------------------------

(def manifest-a "5cf3904e9906a5ac881337606d0efb34198da8ec6d9fab26c63a725cc06e4cbc")

(defn- index-row
  [target manifest finished-work? run-id]
  {:schema :wm/outer-refusal-index-entry-v1 :status :indexed
   :run-id run-id :target target :finished-work? finished-work?
   :satisfaction {:schema :wm/satisfaction-evidence-v1
                  :evidence :claim-mode-observation
                  :wants-measured finished-work?}
   :failure {:kind (if finished-work? :already-satisfied
                     :outer/duplicate-effective-run)}
   :effective-run-identity
   {:schema :wm/effective-run-identity-v1 :target target
    :giraffe-manifest-sha256 manifest
    :policy-id (str target "/policy-x")
    :compiled-check-set-sha256 "4f53cda18c2baa0c0354bb5f9a3ecbe5ed12ab4d8e11ba873c2f11161202b945"
    :mission-source-sha256 "fa9566fbf6c1adc5299530ef3d0c9caf50f4831cb154ed11b059a06229d8044a"
    :base-head "d5adbceb05068ce518c1d7f59fb7a84e0cc2aa98"}})

;; The exact shape of the finding: run 0ba3370c selected M-typed-memories
;; although 4955b6e6's blocking row already recorded it on the same Giraffe
;; field; the duplicate guard refused only ~40 s later. Here the blocking row
;; is EARLIER than a newer non-blocking row for the same target -- the guard
;; refuses on any blocking row, so admission must too.
(def finding-rows
  [(index-row "M-ready" manifest-a true "2026-10-31-4955b6e6")
   (index-row "M-ready" manifest-a false "2026-10-31-0ba3370c")])

(deftest completed-inner-work-does-not-remove-open-tasks
  (let [plain (outer-select/select base-makeup base-state base-observation)
        with-history (outer-select/select base-makeup base-state
                      (assoc base-observation :giraffe/manifest-sha256 manifest-a
                             :effective-run-index-rows finding-rows))]
    (is (= (:outer-score/admitted plain) (:outer-score/admitted with-history)))
    (is (= (:cascade-picks plain) (:cascade-picks with-history)))
    (is (empty? (:outer-score/excluded with-history)))))

(deftest chosen-score-is-minimal
  (testing "Lean chosenCascade_score_le"
    (let [result (outer-select/select base-makeup base-state base-observation)
          chosen (nth (:cascade-scores result) (:cascade-index result))]
      (is (every? #(<= chosen %) (:cascade-scores result))))))

(deftest selection-is-from-field-or-nil
  (testing "Lean step_selection_some"
    (let [result (outer-select/select base-makeup base-state base-observation)]
      (is (some #{(get-in result [:selection :selected])} (:field base-observation)))
      (is (= (:field base-observation) (get-in result [:selection :candidates])))))
  (testing "a no-pick cascade cannot beat explicit abstention"
    (let [makeup {:cascades [{:id :none :patterns ["test"]
                              :steps [{:gate-status :not-ready}]}]}
          refusal (try (outer-select/select makeup base-state base-observation)
                       (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (= {:status :abstained :case :all-policies-predict-stopping} (:outcome refusal)))
      (is (empty? (:classes refusal)))
      (is (= :no-pick (get-in refusal [:cascades 0 :pick]))))))

(deftest singleton-non-stopping-posterior-abstains
  (let [makeup {:cascades [{:id :ready-a :patterns ["test"] :steps [:ready-gate]}
                           {:id :ready-b :patterns ["test"] :steps [:ready-gate]}
                           {:id :stopping-a :patterns ["test"]
                            :steps [{:gate-status :not-ready}]}
                           {:id :stopping-b :patterns ["test"]
                            :steps [{:gate-status :not-ready}]}]}
        result (outer-select/select makeup base-state base-observation)]
    (is (= {:status :abstained :case :insufficient-comparison}
           (:outcome result)))
    (is (nil? (:selection result)))
    (is (= 1 (count (:classes result))))
    (is (= #{:ready-a :ready-b}
           (set (:members (first (:classes result))))))
    (is (= 1 (count (:stopping-equivalent result))))
    (is (= #{:stopping-a :stopping-b}
           (set (:members (first (:stopping-equivalent result))))))
    (is (= 4 (count (get-in result [:construction :cascades])))))
  (let [result (outer-select/select base-makeup base-state base-observation)]
    (is (= :chosen (get-in result [:outcome :status])))
    (is (= 2 (count (:classes result))))))

(deftest chosen-prediction-is-the-chosen-model-push
  (let [result (outer-select/select base-makeup base-state base-observation)
        chosen (nth (:cascades base-makeup) (:cascade-index result))
        model (outer-score/outer-model
               (merge (select-keys base-observation [:focus :relation :facts :field])
                      {:state-counts (:state-counts base-state)
                       :cascade (:steps chosen)}))
        prediction (:prediction result)]
    (is (= (outer-score/push model) (:outcomes prediction)))
    (is (= (select-keys model [:q :status :landing])
           (select-keys prediction [:q :status :landing])))
    (is (< (Math/abs (- 1.0 (reduce + (vals (:outcomes prediction)))))
           1.0e-12))))

(deftest identical-predictions-are-one-policy-and-not-a-comparison
  (let [observation (assoc base-observation :relation (fn [_ _] :focus))
        makeup {:cascades [{:id :a :patterns ["test"] :steps [:ready-gate]}
                           {:id :b :patterns ["test"] :steps [:ready-gate]}]}
        r (outer-select/select makeup base-state observation)]
    (is (= 1 (count (:classes r))))
    (is (= [:a :b] (:members (first (:classes r)))))
    (is (= {:status :abstained :case :insufficient-comparison} (:outcome r)))
    (is (nil? (:cascade-id r)))
    (is (= 2 (count (:cascades r))))))

(deftest distinct-predictions-at-equal-g-refuse
  (let [observation (assoc base-observation :relation (fn [_ _] :focus)
                           :facts {"M-ready" {:status :ready :unblocks 0 :cone 0}
                                   "M-unknown" {:status :ready :unblocks 0 :cone 0}})
        ;; One task per cascade: a ready-gate over both ready tasks would add the
        ;; chain M-ready then M-unknown, a third policy below the tie.
        makeup {:cascades [{:id :a :patterns ["test"] :steps [{:gate #(= "M-ready" (:target %))}]}
                           {:id :b :patterns ["test"] :steps [{:gate #(= "M-unknown" (:target %))}]}]}
        original outer-score/score-model
        counter (atom 0)
        refusal (with-redefs [outer-score/score-model
                             (fn [input model]
                               (let [r (original input model)]
                                 (if (= 2 (swap! counter inc))
                                   (assoc-in r [:prediction :outcomes] {[:related :one] 1.0}) r)))]
                  (try (outer-select/select makeup base-state observation)
                       (catch clojure.lang.ExceptionInfo e (ex-data e))))]
    (is (= :tie-at-minimum (:kind refusal)))
    (is (= 2 (count (:classes refusal))))))

(deftest unknown-graph-member-stays-in-the-field
  (let [obs (assoc-in base-observation [:facts "M-unknown" :cone] nil)
        partition (outer-select/known-term-field obs)
        error (try (outer-select/select
                    {:cascades [{:id :unknown :steps [{:gate-status :undetermined} :by-cone]}]}
                    base-state obs)
                   (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (= (:field obs) (:field partition)))
    (is (empty? (:excluded partition)))
    (is (= :outer-score/graph-term-unavailable (:kind error)))
    (is (= "M-unknown" (:target error)))))

(deftest every-policy-retains-terms-prediction-and-separate-stop
  (let [r (outer-select/select base-makeup base-state base-observation)
        input {:ending-counts (:c base-state) :gain-counts (:gain-counts base-state)}]
    (is (= 2 (count (:cascades r))))
    (doseq [c (:cascades r)]
      (let [{:keys [risk ambiguity novelty altitude]} (:terms c)]
        (is (< (Math/abs (- (:G c) (+ (- (+ risk ambiguity) novelty) (or altitude 0)))) 1.0e-9)))
      (is (< (Math/abs (- 1.0 (reduce + (vals (get-in c [:prediction :outcomes]))))) 1.0e-9))
      (is (= :default-reach (get-in c [:reach :source])))
      (is (re-matches #"[0-9a-f]{64}" (:digest c))))
    (is (= (:cascade-scores r) (mapv :G (:cascades r))))
    (is (= (:cascade-picks r) (mapv :pick (:cascades r))))
    (is (= (:prediction r) (:prediction (nth (:cascades r) (:cascade-index r)))))
    ;; G over cascades (Joe, 2026-11-13): stopping is the empty policy scored by
    ;; the canonical cascade G; the pick-only abstention terms are retained.
    (is (= :selection-cascade-grain-g (get-in r [:abstention :g-law])))
    (is (= (:terms (outer-score/abstention-score input))
           (get-in r [:abstention :pick-only-terms])))
    (is (< (nth (:cascade-scores r) (:cascade-index r)) (get-in r [:abstention :G])))
    (is (= :pinned (get-in r [:library :status])))))

(deftest single-unknown-relation-field-refuses-with-typed-exclusions
  (let [r (try (outer-select/select (update base-makeup :cascades #(vec (take 1 %))) base-state
                                    (assoc base-observation :relation (fn [_ _] :unknown)))
               (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (= :outer-score/no-admitted-tasks (:kind r)))
    (is (= [] (:outer-score/admitted r)))
    (is (= ["M-ready" "M-unknown"]
           (mapv :target (:outer-score/excluded r))))
    (is (every? #(= :relation-unrecognised (:reason %))
                (:outer-score/excluded r)))))

(deftest absent-relation-receipt-is-not-a-hash-of-the-class
  (let [r (outer-select/select base-makeup base-state base-observation)]
    (is (every? #(= :receipt-unavailable (get-in % [:relation :reason])) (:cascades r)))
    (is (every? #(nil? (get-in % [:relation :receipt-digest])) (:cascades r)))))

(deftest legacy-fixture-identities-are-stable
  (let [m {:cascades [{:id :custom :patterns ["only-what-can-start-now"]
                       :steps [{:gate #(= "M-ready" (:target %))}]}]}
        a (outer-select/select m base-state base-observation)
        b (outer-select/select m base-state base-observation)
        c (first (:cascades a))]
    (is (= (get-in a [:library :sha256]) (get-in b [:library :sha256])))
    (is (= (:digest c) (get-in b [:cascades 0 :digest])))
    (is (= [{:named "custom/step-0"}] (:steps c)))
    (is (= :absent (:lineage-status c)))
    (is (nil? (:readme-cascade c)))
    (is (nil? (:edges c)))
    (is (= (get-in a [:library :patterns "only-what-can-start-now"])
           (get-in c [:units 0 :sha256])))
    (let [missing (outer-select/select (assoc m :task-selection-library-root "/nonexistent/l5-4a-library")
                                      base-state base-observation)]
      (is (= :absent (get-in missing [:library :status])))
      (is (= :library-root-absent (get-in missing [:library :reason]))))))

(deftest typed-field-applies-function-steps-only-once
  (let [calls (atom 0)
        obs (assoc base-observation :field-facts
                   {:enumerated ["M-ready" "M-unknown"] :admitted (:facts base-observation) :excluded []})
        result (outer-select/select
                {:cascades [{:id :one-pass :steps [{:gate (fn [_] (swap! calls inc) true)}]}]}
                base-state obs)]
    (is (= 2 @calls))
    (is (= {:status :abstained :case :insufficient-comparison}
           (:outcome result)))))

(deftest forbidden-picks-stay-scored-but-cannot-win
  (let [before (outer-select/select base-makeup base-state base-observation)
        target (get-in before [:selection :selected :target])
        row {:target target :reason :reader-refused :pin "refusal-pin"}
        after (outer-select/select base-makeup base-state
                                   (assoc base-observation :forbidden #{target} :forbidden-evidence [row]))]
    (is (not= target (get-in after [:selection :selected :target])))
    (is (= (:cascades before) (:cascades after)))
    (is (= (:outer-score/admitted before) (:outer-score/admitted after)))
    (is (= [row] (:forbidden after)))
    (let [rows (mapv #(hash-map :target % :reason :reader-refused :pin "p") (:cascade-picks before))
          failure (try (outer-select/select base-makeup base-state
                                            (assoc base-observation :forbidden (set (:cascade-picks before))
                                                   :forbidden-evidence rows))
                       (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (= :no-admissible-pick (:kind failure)))
      (is (= (:cascades before) (get-in failure [:selection-evidence :cascades]))))))

(deftest forbidden-evidence-must-bind-the-forbidden-targets
  (is (thrown? clojure.lang.ExceptionInfo
               (outer-select/select base-makeup base-state
                                    (assoc base-observation :forbidden #{"M-ready"})))))

(deftest stopping-equivalent-is-retained-at-altitude-but-not-compared
  (let [obs (assoc base-observation :relation (fn [_ target]
                                              (if (= target "M-ready") :focus :unknown)))
        r (outer-select/select base-makeup base-state obs)]
    (is (= 1 (count (:classes r))))
    (is (= 1 (get-in r [:construction :policy-count])))
    (is (= [:second] (:members (first (:stopping-equivalent r)))))
    (is (nil? (some :relation-basis (:classes r))))
    (is (= {:status :abstained :case :insufficient-comparison} (:outcome r)))
    (is (nil? (:cascade-id r)))
    (is (= ["M-ready"] (:outer-score/admitted r)))
    (is (= ["M-unknown"] (mapv :target (:outer-score/excluded r))))
    (is (= 2 (count (:cascades r))))
    (is (< (get-in r [:classes 0 :G]) (get-in r [:abstention :G])))
    (is (every? #(not= (get-in % [:prediction :outcomes])
                       (get-in r [:abstention :prediction :outcomes])) (:classes r)))))


(deftest comparison-projects-progressing-policy-and-altitude
  (let [obs (assoc base-observation :relation (fn [_ t] (if (= t "M-ready") :focus :unknown))
                  :field-facts {:enumerated ["M-ready" "M-unknown"]
                                :admitted (:facts base-observation) :excluded []}
                  :previous-selection {:status :present :chosen-class "previous" :pick "M-old"
                                       :outcome :changed :choice-input-digest "old-input"})
        r (assoc (outer-select/select base-makeup base-state obs)
                 :q6 {:refusal-history []})
        export (facts/selection-facts-for-record {:outer/selection r} "fixture" {} {})
        f (:facts export)
        old (facts/selection-facts-for-record
             {:outer/selection (dissoc r :stopping-equivalent)} "fixture" {} {})]
    (is (= (f "comparedPolicies") (f "constructedCascades")
           (get-in f ["targetConstruction" 0 "policies"])))
    (is (= 1 (count (f "policyG")) (count (f "policyPrediction"))))
    (is (every? #(not= (get % "digest") (f "abstentionPrediction")) (f "policyPrediction")))
    (is (= (identity/digest (get-in r [:abstention :prediction :outcomes])) (f "abstentionPrediction")))
    (is (= (:stopping-equivalent r) (get-in export [:diagnostics :stoppingEquivalent])))
    (is (= (get-in r [:abstention :terms]) (get-in export [:diagnostics :abstentionTerms :terms])))
    (is (.startsWith (checker/evidence f :q11) ".known"))
    (testing "Q11 reads each compared class's predicted parameter posterior"
      (is (every? #(contains? (:prediction %) :parameters) (:cascades r)))
      (is (= (mapv (fn [c] {"policy" (:id c) "digest" (:parameters-digest c)}) (:classes r))
             (f "policyParameters")))
      (is (every? #(string? (get % "digest")) (f "policyParameters")))
      (is (re-find #"policyParameters := \[\(" (checker/evidence f :q11))))
    (is (= 2 (count (get-in old [:facts "constructedCascades"])))
        "Historical projection uses all retained arrangement classes")))

(deftest source-header-reads-pinned-files-and-types-every-absence
  ;; Live pin: the interim-director mission file as the click-12 record pins it.
  (let [path "/home/joe/code/futon7/holes/M-interim-director.md"
        pinned (outer-select/source-header
                {:path path
                 :sha256 "aad85c4b70e1e68ec659f21c0ec07a4d886a4520bad48555d30aca84a3511dd3"})]
    (is (= "2026-05-14" (get-in pinned [:header :date])))
    ;; The live source may close after the retained pin; drift is reported
    ;; separately below, while the header parser must still return its value.
    (is (seq (get-in pinned [:header :status])))
    (is (string/starts-with? (get-in pinned [:header :timebox]) "2 weeks"))
    (is (contains? #{:matches :drifted} (:pin pinned)))
    ;; a wrong pin is :drifted, never silently :matches
    (is (= :drifted (:pin (outer-select/source-header {:path path :sha256 "00"}))))
    ;; no path at all
    (is (= {:path :absent :sha256 :absent :pin :absent
            :header {:date :absent :status :absent :timebox :absent}}
           (outer-select/source-header {})))
    ;; a pinned file that no longer exists must not throw out of the selection
    (is (= :unreadable (:pin (outer-select/source-header
                              {:path "/nonexistent/M-gone.md" :sha256 "00"}))))
    ;; a file without the three header lines
    (let [f (java.io.File/createTempFile "no-header" ".md")]
      (spit f "# Nothing here\n\nprose only\n")
      (is (= {:date :absent :status :absent :timebox :absent}
             (:header (outer-select/source-header {:path (.getPath f) :sha256 "00"})))))))

(deftest per-task-ratings-are-retained-without-changing-the-choice
  (let [s (outer-select/select base-makeup base-state base-observation)
        rows (:per-task-rating s)
        by-id (into {} (map (juxt :target identity)) rows)]
    (is (= (count (:field base-observation)) (count rows)))
    (is (= (:top-ten s) (outer-select/rating-top-ten rows)))
    (is (= (dissoc s :per-task-rating :top-ten)
           (dissoc (with-redefs [outer-select/per-task-ratings (constantly [])]
                     (outer-select/select base-makeup base-state base-observation))
                   :per-task-rating :top-ten)))
    ;; Ratings are pick-level and stay the pick-only G of each class's
    ;; representative; the class G is the cascade-level canonical G and need not
    ;; equal them (G over cascades, Joe 2026-11-13).
    (doseq [c (:classes s)
            :let [rep (first (filter #(= (:id %) (:representative c)) (:cascades s)))
                  {:keys [risk ambiguity novelty altitude]} (:pick-only-terms rep)]]
      (is (= :selection-cascade-grain-g (:g-law rep)))
      (is (< (Math/abs (- (+ (- (+ risk ambiguity) novelty) (or altitude 0))
                          (:G (by-id (:pick c))))) 1.0e-9)))))

(deftest two-ready-tasks-give-a-chain-in-giraffe-order
  ;; Lucy 2026-11-13 clicks 2-3 abstained :insufficient-comparison on ladder 08
  ;; and 10. The field arrives with the lower-ranked task first; Giraffe's rank
  ;; puts T-08 first, and the chain T-08 then T-10 is the second policy.
  (let [makeup {:cascades [{:id :start :patterns ["only-what-can-start-now"] :steps [:ready-gate]}
                           {:id :discover :patterns ["find-out-before-building"]
                            :steps [{:gate-status :undetermined}]}]}
        obs {:focus "M-focus"
             :relation (fn [_ _] :associated)
             :field [{:target "T-10" :reach 1/2 :reach-source :default-reach}
                     {:target "T-08" :reach 1/2 :reach-source :default-reach}]
             :facts {"T-08" {:status :ready :unblocks 0 :cone 0 :rank 1}
                     "T-10" {:status :ready :unblocks 0 :cone 0 :rank 3}}}
        r (outer-select/select makeup base-state obs)
        by-id (into {} (map (juxt :id identity)) (:cascades r))]
    (is (= ["T-08" "T-10"] (get-in by-id [:start--chain-2 :chain :picks])))
    (is (= "T-08" (get-in by-id [:start :pick]) (get-in by-id [:start--chain-2 :pick])))
    (is (= 2 (count (:classes r))) "the chain and its first pick are two policies")
    (is (= {:status :chosen} (:outcome r)))
    (is (= :start--chain-2 (:cascade-id r)))
    (is (= "T-08" (get-in r [:selection :selected :target])))
    (is (not= (get-in by-id [:start :prediction :parameters])
              (get-in by-id [:start--chain-2 :prediction :parameters])))
    ;; wm-click-loop-repair-bb1de665: every cascade digest, chain variants
    ;; included, must be recomputable from its carrier, because
    ;; outer-state/chosen-cascade replays exactly that carrier.
    (doseq [c (vals by-id)]
      (is (= (:digest c) (menu/cascade-digest c))
          (str "digest not carrier-recomputable: " (:id c))))))

