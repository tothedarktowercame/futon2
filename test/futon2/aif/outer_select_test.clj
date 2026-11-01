(ns futon2.aif.outer-select-test
  (:require [clojure.edn :as edn]
            [futon2.aif.action-identity :as identity]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-score :as outer-score]
            [futon2.aif.outer-select :as outer-select]
            [futon2.aif.outer-state :as outer-state]))

(def base-makeup
  {:cascades [{:id :first :patterns ["test"] :steps [{:gate-status :ready}]}
              {:id :second :patterns ["test"] :steps [{:gate-status :undetermined}]}]})
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

(deftest blocking-history-target-is-excluded-before-scoring
  (testing "the bad case still selects the target when selection has no
            history view (demonstrates the finding), and is excluded once it does"
    ;; Without the index view the bad case reproduces: M-ready is selectable.
    (let [without-history (outer-select/select
                           base-makeup base-state
                           (assoc base-observation :giraffe/manifest-sha256 manifest-a))]
      (is (contains? (set (map :target (get-in without-history [:selection :candidates])))
                     "M-ready")))
    ;; With the view, the guard's target never reaches scoring.
    (let [result (outer-select/select
                  base-makeup base-state
                  (assoc base-observation
                         :giraffe/manifest-sha256 manifest-a
                         :effective-run-index-rows finding-rows))
          exclusion (some #(when (= "M-ready" (:target %)) %)
                          (:outer-score/excluded result))]
      (is (= ["M-unknown"] (:outer-score/admitted result)))
      (is (= ["2026-10-31-4955b6e6"] (:matched-run-ids exclusion)))
      (is (= :outer-score/duplicate-effective-run (:kind exclusion)))
      (is (= :blocking-effective-run-on-same-giraffe-field (:reason exclusion)))
      (is (not (contains? (set (map :target (get-in result [:selection :candidates])))
                          "M-ready"))))))

(deftest changed-giraffe-field-readmits-the-target
  (let [result (outer-select/select
                base-makeup base-state
                (assoc base-observation
                       :giraffe-manifest-sha256 "a-new-giraffe-run-changed-the-field"
                       :effective-run-index-rows finding-rows))]
    (is (= ["M-ready" "M-unknown"] (:outer-score/admitted result)))
    (is (nil? (some #(= :outer-score/duplicate-effective-run (:kind %))
                    (:outer-score/excluded result))))))

(deftest wholly-blocked-field-is-a-typed-refusal
  (let [observation (assoc base-observation
                           :field [{:target "M-ready" :reach 1/2}]
                           :giraffe/manifest-sha256 manifest-a
                           :effective-run-index-rows finding-rows)
        refusal (try (outer-select/select base-makeup base-state observation)
                     nil (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (= :outer-score/duplicate-effective-run-field-emptied (:kind refusal)))
    (is (= ["M-ready"] (mapv :target (:duplicate-effective-run refusal))))
    (is (= 1 (count (get-in refusal [:duplicate-effective-run 0 :matched-run-ids]))))))

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
      (is (= :pick-not-below-abstention (:kind refusal)))
      (is (= (:G refusal) (:abstention-G refusal)))
      (is (= :no-pick (get-in refusal [:selection-evidence :cascades 0 :pick]))))))

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

(deftest ties-refuse-even-with-the-same-pick
  (doseq [different-picks? [false true]]
    (let [observation (assoc base-observation :relation (fn [_ _] :focus)
                             :facts {"M-ready" {:status :ready :unblocks 0 :cone 0}
                                     "M-unknown" {:status :ready :unblocks 0 :cone 0}})
          makeup {:cascades [{:id :a :patterns ["test"] :steps [:ready-gate]}
                             {:id :b :patterns ["test"]
                              :steps (if different-picks? [{:gate #(= "M-unknown" (:target %))}]
                                         [:ready-gate])}]}
          refusal (try (outer-select/select makeup base-state observation)
                       (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (= :tie-at-minimum (:kind refusal)))
      (is (= [:a :b] (:cascades refusal) (:tied-cascades refusal)))
      (is (= 2 (count (get-in refusal [:selection-evidence :cascades])))))))

(deftest real-giraffe-selection-demonstration
  ;; Fixed pass bytes, not the host/cwd-sensitive historical-run verifier.
  (let [text (slurp "/home/joe/code/storage/giraffe/20261101T044121.422Z-730407c5/pass.edn")
        expected "a9ad27a8cb6fd0c808413ce639019b117aed746fbcb6b2945560cfc71423ff91"]
    (is (= expected (identity/sha256 text)))
    (when-not (= expected (identity/sha256 text)) (throw (ex-info "Pass pin drift" {})))
    (let [rows (:order (edn/read-string text))
          makeup (outer-state/load-makeup)
          state (outer-state/init-state makeup)
          observation {:field (mapv #(hash-map :target (name (:id %)) :reach (:default-reach makeup)
                                               :reach-source :default-reach) rows)
                       :facts (into {} (map #(vector (name (:id %)) %) rows))
                       :focus "WM" :relation (fn [_ _] :associated)}
          result (try (outer-select/select makeup state observation)
                      (catch clojure.lang.ExceptionInfo e (ex-data e)))
          evidence (or (:selection-evidence result) result)]
      (is (= :tie-at-minimum (:kind result)))
      (is (= 3 (count (:cascades evidence))))
      (is (= (:cascade-scores evidence) (mapv :G (:cascades evidence))))
      (is (every? #(= :default-reach (get-in % [:reach :source]))
                  (remove #(= :no-pick (:pick %)) (:cascades evidence))))
      (is (= [:ready-gate :by-cone :by-unblocks] (get-in evidence [:cascades 0 :steps])))
      (is (every? #(and (:sha256 %) (= :present (:status %)))
                  (mapcat :units (:cascades evidence)))))))

(deftest unknown-graph-member-is-excluded-with-a-producer-request
  (let [graph {:status :unknown :reason :prerequisite-unresolved
               :scope :population/missions-excursions :population-sha "frozen"
               :prerequisites [{:raw_target "external prerequisite" :kind "depends-on"
                                :absence :unresolved-prerequisite}]}
        obs (assoc-in base-observation [:facts "M-unknown"]
                      {:status :undetermined :cone nil :unblocks nil :graph-terms graph})
        result (outer-select/select base-makeup base-state obs)
        exclusion (first (:outer-score/excluded result))]
    (is (= "M-ready" (get-in result [:selection :selected :target])))
    (is (= ["M-ready"] (:outer-score/admitted result)))
    (is (= ["M-ready"] (mapv :target (get-in result [:selection :candidates]))))
    (is (= :outer-score/graph-term-unavailable (:kind exclusion)))
    (is (= [:cone :unblocks] (mapv :term (:terms exclusion))))
    (is (= (:prerequisites graph) (get-in exclusion [:terms 0 :prerequisites])))
    (is (= [(:knowledge-producer-request exclusion)] (:knowledge-producer-requests result)))
    (is (nil? (get-in obs [:facts "M-unknown" :cone])))
    (let [all-unknown (assoc obs :field [{:target "M-unknown" :reach 1/2}])
          refusal (try (outer-select/select base-makeup base-state all-unknown)
                       nil (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (= :outer-score/no-known-graph-members (:kind refusal)))
      (is (= 1 (count (:outer-score/excluded refusal)))))
    (is (thrown? clojure.lang.ExceptionInfo
                 (outer-score/apply-selection-step (:facts obs) (:field obs) :by-cone))
        "The numeric scorer still refuses an unknown; the boundary never supplies a zero.")))

(deftest every-policy-retains-terms-prediction-and-separate-stop
  (let [r (outer-select/select base-makeup base-state base-observation)
        input {:ending-counts (:c base-state) :gain-counts (:gain-counts base-state)}]
    (is (= 2 (count (:cascades r))))
    (doseq [c (:cascades r)]
      (let [{:keys [risk ambiguity novelty]} (:terms c)]
        (is (< (Math/abs (- (:G c) (- (+ risk ambiguity) novelty))) 1.0e-9)))
      (is (< (Math/abs (- 1.0 (reduce + (vals (get-in c [:prediction :outcomes]))))) 1.0e-9))
      (is (= :default-reach (get-in c [:reach :source])))
      (is (re-matches #"[0-9a-f]{64}" (:digest c))))
    (is (= (:cascade-scores r) (mapv :G (:cascades r))))
    (is (= (:cascade-picks r) (mapv :pick (:cascades r))))
    (is (= (:prediction r) (:prediction (nth (:cascades r) (:cascade-index r)))))
    (is (= (:abstention r) (outer-score/abstention-score input)))
    (is (< (nth (:cascade-scores r) (:cascade-index r)) (get-in r [:abstention :G])))
    (is (= :pinned (get-in r [:library :status])))))

(deftest single-unknown-relation-pick-cannot-beat-stopping
  (let [r (try (outer-select/select (update base-makeup :cascades #(vec (take 1 %))) base-state
                                    (assoc base-observation :relation (fn [_ _] :unknown)))
               (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (= :pick-not-below-abstention (:kind r)))
    (is (= :unknown (get-in r [:selection-evidence :cascades 0 :relation :class])))
    (is (= (:G r) (:abstention-G r)))))

(deftest absent-relation-receipt-is-not-a-hash-of-the-class
  (let [r (outer-select/select base-makeup base-state base-observation)]
    (is (every? #(= :receipt-unavailable (get-in % [:relation :reason])) (:cascades r)))
    (is (every? #(nil? (get-in % [:relation :receipt-digest])) (:cascades r)))))

(deftest library-and-cascade-identities-are-stable-and-honest
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
