(ns futon2.aif.outer-select-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-observation :as outer-observation]
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
   :field [{:target "M-ready" :reach 1/2}
           {:target "M-unknown" :reach 1/2}]
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
  (testing "the chosen cascade can gate everything out"
    (let [makeup {:cascades [{:id :none :patterns ["test"]
                              :steps [{:gate-status :not-ready}]}]}
          result (outer-select/select makeup base-state base-observation)]
      (is (nil? (:selection result)))
      (is (nil? (:prediction result))))))

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

(deftest tie-means-distinct-tied-picks
  (testing "two cascades with the same minimum and the same pick are not a tie"
    (let [makeup {:cascades [{:id :a :patterns ["test"] :steps [{:gate-status :ready}]}
                             {:id :b :patterns ["test"] :steps [:ready-gate]}]}
          result (outer-select/select makeup base-state base-observation)]
      (is (= [:a :b] (:tied-cascades result)))
      (is (false? (:tie? result)))))
  (testing "equal scores with different picks are a tie, and it is recorded"
    (let [observation (assoc base-observation
                             :relation (fn [_ _] :focus)
                             :facts {"M-ready" {:status :ready :unblocks 0 :cone 0}
                                     "M-unknown" {:status :ready :unblocks 0 :cone 0}})
          makeup {:cascades [{:id :a :patterns ["test"] :steps [{:gate #(= "M-ready" (:target %))}]}
                             {:id :b :patterns ["test"] :steps [{:gate #(= "M-unknown" (:target %))}]}]}]
      (is (true? (:tie? (outer-select/select makeup base-state observation)))))))

(defonce ^:private giraffe-loader
  (doto (clojure.lang.DynamicClassLoader.
         (.getContextClassLoader (Thread/currentThread)))
    (.addURL (.toURL (.toURI (io/file "/home/joe/code/futon3c/src"))))))

(defn actual-var [symbol]
  (with-bindings {clojure.lang.Compiler/LOADER giraffe-loader}
    (requiring-resolve symbol)))

(deftest real-giraffe-selection-demonstration
  (let [dir (io/file "/home/joe/code/storage/giraffe/20261028T154151.285Z-b916cd4e")]
    (is (.isDirectory dir) "the retained Giraffe evidence must be present")
    (let [makeup (outer-state/load-makeup)
            state (outer-state/init-state makeup)
            field (binding [outer-observation/*resolve-var* actual-var]
                    (outer-observation/giraffe-field dir makeup))
            observation (assoc field
                               :focus "M-wm-outer-selection"
                               ;; Relation evidence is packet 4b's integration
                               ;; input.  This demonstration holds it constant
                               ;; so the real Giraffe field isolates the menu.
                               :relation (fn [_ _] :focus))
            result (outer-select/select makeup state observation)]
        (println "outer-select real run demonstration"
                 (pr-str {:cascade-id (:cascade-id result)
                          :mission (get-in result [:selection :selected :target])
                          :scores (:cascade-scores result)
                          :tie? (:tie? result)}))
        (is (some? (:selection result)))
        (is (not= "M-repl-wins-over-cli"
                  (get-in result [:selection :selected :target])))
        (is (every? #(and (number? %) (Double/isFinite (double %)))
                    (:cascade-scores result))))))

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
