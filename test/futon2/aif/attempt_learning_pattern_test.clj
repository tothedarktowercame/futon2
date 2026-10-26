(ns futon2.aif.attempt-learning-pattern-test
  "R8b worked example (decision sheet 2026-10-26 item 1, option (a); contract
  v3). The whole chain over attempt-002's retained prediction and fold
  (machinery-150, after revision futon5a 547b191f): the clause now authored
  in req_f7374bcce for occurrence [gauntlet/world-is-hypergraph 0] is an
  artifact locator; the producer evaluates it at the after revision; the
  comparison verdict is :predicted-and-observed; the v3 receipt admits the
  row; the ledger appends it; pattern-theta for gauntlet/world-is-hypergraph
  moves from no trials to 3/4. Occurrence 19, still prose, is held, as all
  27 were before the change."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.attempt-learning :as learning]
            [futon2.aif.completion-check :as completion-check]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.pattern-observation :as pattern-observation]
            [futon2.aif.token-outcome :as token-outcome]
            [futon2.aif.wm.authored.req-f7374bcce :as authored]))

(def fixture
  (edn/read-string
   (slurp "test/resources/wm/retained-25e24-pattern-fold-projection.edn")))

(def zero ["gauntlet/world-is-hypergraph" 0])
(def nineteen ["gauntlet/world-is-hypergraph" 19])
(def pattern "gauntlet/world-is-hypergraph")
(def after (:after fixture))
(def revision {:repo "futon5a" :sha after})
(def source
  {:record-path
   "data/wm-full-loop-machinery-150/wm-contract-machinery-150-v1/attempt-002/003-construction.edn"
   :record-sha256 "15d855b11777c3afae1ec7d523ee91433098cd7b8dc4d9812e15a0f63e25c16c"})
(def occurrence
  {:schema :wm/action-transition-occurrence-v2
   :action/id "action-25e24"
   :action/value-sha256 (apply str (repeat 64 "a"))
   :transition/id "transition-25e24"})

(defn condition-for [conditions occurrence-id]
  (first (filter #(= occurrence-id (get-in % [:witness :occurrence-id])) conditions)))

(defn with-check
  "attempt-002's recorded fold with one occurrence's authored completion check
  replaced. The compiler hands an authored :completion-check through unchanged
  (cascade_enactment_compiler/assemble-inputs), so this is the fold the next
  construction of the same reading produces from the changed clause."
  [occurrence-id check]
  (mapv #(if (= occurrence-id (get-in % [:witness :occurrence-id]))
           (assoc-in % [:witness :authored-clause :completion-check] check)
           %)
        (:fold-conditions fixture)))

(def authored-check (get-in authored/clauses [zero :completion-check]))

(defn temp-root []
  (str (.toFile (java.nio.file.Files/createTempDirectory
                 "r8b-worked-example"
                 (make-array java.nio.file.attribute.FileAttribute 0)))))

(defn run-chain
  "supply -> compare-supplied-outcomes -> pattern-receipt -> record! -> pattern-theta,
  the order retain-token-outcome! runs them, over a private ledger root."
  [root conditions]
  (let [supplied (pattern-observation/supply
                  {:prediction (:prediction fixture) :fold-conditions conditions
                   :revision revision :source source})
        comparison (token-outcome/compare-supplied-outcomes
                    (:prediction fixture) supplied after)
        receipt (learning/pattern-receipt
                 {:pattern-comparison comparison :occurrence occurrence
                  :route :fresh-author})
        recorded (ledger/record! root receipt)]
    {:supplied supplied :comparison comparison :receipt receipt
     :recorded recorded :theta (ledger/pattern-theta pattern root)}))

(defn trial-for [recorded occurrence-id]
  (first (filter #(= occurrence-id (:pattern-occurrence %)) (:trials recorded))))

(deftest the-authored-clause-is-the-executable-form
  (is (completion-check/executable? authored-check))
  (is (= [:artifact-locator :C4 "futon5a" "holes/missions/M-war-machine-wiring.md"]
         [(first authored-check) (:class (second authored-check))
          (:repo (second authored-check)) (:path (second authored-check))]))
  ;; the recorded attempt-002 clause for the same occurrence was prose
  (let [recorded (get-in (condition-for (:fold-conditions fixture) zero)
                         [:witness :authored-clause :completion-check])]
    (is (= :title-names-bridge (first recorded)))
    (is (string? (second recorded)))
    (is (not (completion-check/executable? recorded)))))

(deftest one-authored-locator-moves-theta
  (let [root (temp-root)
        before (ledger/pattern-theta pattern root)
        {:keys [supplied comparison recorded theta]}
        (run-chain root (with-check zero authored-check))
        row-0 (trial-for recorded zero)
        row-19 (trial-for recorded nineteen)
        token-0 (first (filter #(= zero (nth (:token %) 2)) (:tokens comparison)))
        token-19 (first (filter #(= nineteen (nth (:token %) 2)) (:tokens comparison)))]
    (is (= :no-recorded-trials (:status before)))
    ;; producer: the C4 head is present at the after revision
    (is (= true (:observed (first (filter #(= zero (:occurrence %)) (:rows supplied))))))
    (is (= after (get-in (first (filter #(= zero (:occurrence %)) (:rows supplied)))
                         [:authority-evidence :evidence :resolved-sha])))
    ;; comparison: predicted 1023/1024, observed true
    (is (= :predicted-and-observed (:verdict token-0)))
    (is (= 1023/1024 (:predicted token-0)))
    (is (= :observation-missing (:verdict token-19)))
    (is (= :completion-check-not-executable (:reason token-19)))
    ;; receipt + ledger: occurrence 0 admitted and appended, 19 held
    (is (= :record-only (:status recorded)))
    (is (= :admitted-at-attempt-grain (:status row-0)))
    (is (= pattern (:pattern-key row-0)))
    (is (= true (:after-observation row-0)))
    (is (true? (:counted? row-0)))
    (is (= :appended (get-in row-0 [:ledger :status])))
    (is (= :held (:status row-19)))
    (is (= :completion-check-not-executable (:reason row-19)))
    (is (false? (:counted? row-19)))
    ;; theta: (1 + 1/2) / (1 + 1)
    (is (= {:status :recorded-trials :theta 3/4 :trials-count 1 :successes 1
            :targets ["M-war-machine-wiring"]}
           (select-keys theta [:status :theta :trials-count :successes :targets])))
    (is (= [:v3] (mapv :contract-version (ledger/read-trials root))))
    (is (= [pattern] (mapv :theta-key (ledger/read-trials root))))
    ;; a replay of the same comparison is held by the ledger, theta unchanged
    (let [again (run-chain root (with-check zero authored-check))]
      (is (= :duplicate-replay (:reason (trial-for (:recorded again) zero))))
      (is (= 3/4 (:theta (:theta again))))
      (is (= 1 (count (ledger/read-trials root)))))))

(deftest the-recorded-prose-fold-never-updates
  ;; the bad case: attempt-002 exactly as retained, every check prose
  (let [root (temp-root)
        {:keys [recorded theta]} (run-chain root (:fold-conditions fixture))]
    (is (every? #(= :held (:status %)) (:trials recorded)))
    (is (= [:completion-check-not-executable :completion-check-not-executable]
           (mapv :reason (:trials recorded))))
    (is (= :no-recorded-trials (:status theta)))
    (is (not (.exists (io/file root "attempts.edn"))))))

(deftest a-false-reading-is-a-failure-update-not-an-absence
  (let [root (temp-root)
        absent [:artifact-locator {:class :C3 :repo "futon5a"
                                   :path "holes/missions/no-such-mission.md"}]
        {:keys [comparison recorded theta]} (run-chain root (with-check zero absent))
        token-0 (first (filter #(= zero (nth (:token %) 2)) (:tokens comparison)))]
    (is (= :predicted-not-observed (:verdict token-0)))
    (is (= :admitted-at-attempt-grain (:status (trial-for recorded zero))))
    (is (= false (:after-observation (trial-for recorded zero))))
    (is (= {:theta 1/4 :trials-count 1 :successes 0}
           (select-keys theta [:theta :trials-count :successes])))))

(deftest a-locator-on-an-occurrence-the-prediction-does-not-want-is-not-a-trial
  ;; occurrence 19 is wanted with predicted 0: an executable check there
  ;; observes, but the row is held :effect-not-predicted, as the mission
  ;; domain holds its own unpredicted effects.
  (let [root (temp-root)
        {:keys [recorded theta]} (run-chain root (with-check nineteen authored-check))]
    (is (= :effect-not-predicted (:reason (trial-for recorded nineteen))))
    (is (= :no-recorded-trials (:status theta)))))

(deftest the-v2-contract-does-not-consume-pattern-rows
  (let [supplied (pattern-observation/supply
                  {:prediction (:prediction fixture)
                   :fold-conditions (with-check zero authored-check)
                   :revision revision :source source})
        comparison (token-outcome/compare-supplied-outcomes
                    (:prediction fixture) supplied after)
        receipt (learning/pattern-receipt
                 {:pattern-comparison comparison :occurrence occurrence
                  :route :fresh-author :contract (learning/declared-contract-v2)})]
    (is (= :held (:status receipt)))
    (is (every? #(= :attempt-contract-invalid (:reason %)) (:trials receipt)))))

(deftest the-mission-domain-receipt-is-untouched-by-v3
  ;; v3 is v2 in the mission domain: the v2 reader shape is a subset of v3's
  (let [v2 (learning/declared-contract-v2) v3 (learning/declared-contract-v3)]
    (is (= :wm/attempt-learning-contract-v2 (:supersedes v3)))
    (is (= (select-keys v2 [:trial-grain :occurrence-schema :observation-schema
                            :placement :selection-condition :consumption :calibration])
           (select-keys v3 [:trial-grain :occurrence-schema :observation-schema
                            :placement :selection-condition :consumption :calibration])))
    (is (= :pattern-occurrence-attempt (:pattern-trial-grain v3)))
    (is (= :wm/pattern-outcome-comparison-v1 (:pattern-comparison-schema v3)))))
