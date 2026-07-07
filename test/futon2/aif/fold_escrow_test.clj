(ns futon2.aif.fold-escrow-test
  "E-live-loop-2 gate 2d: the malformed-record rejection test. A deliberately
   malformed deposit must be REJECTED loudly (reason + file, in :rejected and
   on stderr) while a valid deposit still loads and replays on exact sha."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.close-loop :as cl]
            [futon2.aif.fold-escrow :as esc]
            [futon2.aif.fold-llm :as fl])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def mock-cascade ["invariant-coherence/shape-first-identify"])
(def mock-circumstance {:mission "test-d/mission/mock" :psi "WANT: x. HAVE: y."})
(def mock-prose
  (slurp "/home/joe/code/futon3/library/invariant-coherence/shape-first-identify.flexiarg"))
(def mock-prompt
  (fl/fold-prompt mock-cascade mock-circumstance {(first mock-cascade) mock-prose}))

(def mock-answer
  ;; 1 box + 1 hole ⇒ coverage 0.5 ⇒ coverage→rollout ΔG −0.5 (the Leg-2 shape)
  {:boxes        [{:id :b1 :role "concrete reshape step" :fits-pattern "invariant-coherence/shape-first-identify"
                   :addresses-however "mock"}]
   :wires        []
   :terminals    [:b1]
   :policy-holes [{:unfolded-pattern nil :free "the un-grounded remainder" :why "mock hole"}]})

(defn- valid-record []
  {:fold-turn/id "ft-test-001"
   :mission (:mission mock-circumstance)
   :cascade {:psi (:psi mock-circumstance) :pattern-ids mock-cascade}
   :prompt {:sha256 (esc/prompt-sha mock-prompt)}
   :turn {:agent "test-agent" :model "mock" :at "2026-07-05T00:00:00Z"
          :answer mock-answer}
   :arming {:operator "joe" :word "mock arming word — test fixture, not a real arming" :at "2026-07-05T00:00:00Z"
            :scope :one-fold}
   :eval {:delta-g -0.5 :g-grain :coverage}})

(defn- learning-loop-clean []
  (edn/read-string (slurp "/home/joe/code/futon6/holes/clean/M-learning-loop.clean.edn")))

(defn- cycle-clean []
  {:clean/proof "cycle-fixture"
   :clean/seq [:one :two]
   :clean/boxes [{:id :b1 :method :one :text "one"
                  :consumes [:b] :produces :a}
                 {:id :b2 :method :two :text "two"
                  :consumes [:a] :produces :b}]
   :clean/wires [{:from :b1 :to :b2 :carries :a}
                 {:from :b2 :to :b1 :carries :b}]
   :clean/copar []
   :clean/shape {:macro :cycle-fixture
                 :holes-at []
                 :discharges-at []
                 :note "cycle fixture"}})

(defn- tmp-deposit-dir!
  "Write {filename → edn-string|record} into a fresh temp dir; return its path."
  [files]
  (let [dir (.toFile (Files/createTempDirectory "fold-turns-test" (make-array FileAttribute 0)))]
    (doseq [[nm content] files]
      (spit (io/file dir nm) (if (string? content) content (pr-str content))))
    (.getPath dir)))

(deftest rejects-malformed-loudly-accepts-valid
  (let [dir (tmp-deposit-dir!
             {"ft-valid.edn"       (valid-record)
              "ft-no-arming.edn"   (dissoc (valid-record) :arming)
              "ft-dg-drift.edn"    (assoc-in (valid-record) [:eval :delta-g] -0.9)
              "ft-bad-pin.edn"     (assoc-in (valid-record) [:prompt :sha256] (apply str (repeat 64 "0")))
              "ft-no-boxes.edn"    (assoc-in (valid-record) [:turn :answer :boxes] [])
              "ft-unreadable.edn"  "{:fold-turn/id \"ft-broken\" :never-closed"
              "not-a-deposit.edn"  {:ignored true}})   ; wrong prefix: not scanned
        {:keys [deposits rejected]} (esc/load-deposits dir)
        reasons (into {} (map (juxt #(-> % :file io/file .getName) :reason)) rejected)]
    (testing "the valid deposit loads"
      (is (= ["ft-test-001"] (map :fold-turn/id deposits)))
      (is (= [:clean-missing] (map :clean/status deposits))))
    (testing "each malformed record is rejected with its own loud reason"
      (is (= {"ft-no-arming.edn"  :missing-arming
              "ft-dg-drift.edn"   :delta-g-mismatch
              "ft-bad-pin.edn"    :prompt-not-reconstructable
              "ft-no-boxes.edn"   :empty-construction
              "ft-unreadable.edn" :unreadable-edn}
             reasons)))
    (testing "rejection messages carry the file (auditable, not just a boolean)"
      (is (every? #(re-find #"fold-turn REJECTED \[" (:message %)) rejected)))))

(deftest clean-block-is-validated-when-present
  (let [dir (tmp-deposit-dir!
             {"ft-clean.edn" (assoc (valid-record) :clean (learning-loop-clean))})
        {:keys [deposits rejected]} (esc/load-deposits dir)]
    (is (empty? rejected) (pr-str rejected))
    (is (= ["ft-test-001"] (map :fold-turn/id deposits)))
    (is (= :clean-pass (-> deposits first :clean/status)))
    (is (re-find #"PASS" (-> deposits first :clean/argcheck-output)))))

(deftest malformed-clean-block-is-rejected-with-gate-reason
  (testing "G5 port mismatch"
    (let [bad-clean (assoc-in (learning-loop-clean)
                              [:clean/wires 0 :carries]
                              :not-produced-or-consumed)
          dir (tmp-deposit-dir! {"ft-bad-clean.edn" (assoc (valid-record) :clean bad-clean)})
          {:keys [deposits rejected]} (esc/load-deposits dir)]
      (is (empty? deposits))
      (is (= [:clean-invalid] (mapv :reason rejected)))
      (is (re-find #"G5" (-> rejected first :message)))))
  (testing "G7 cycle"
    (let [dir (tmp-deposit-dir! {"ft-cycle-clean.edn" (assoc (valid-record) :clean (cycle-clean))})
          {:keys [deposits rejected]} (esc/load-deposits dir)]
      (is (empty? deposits))
      (is (= [:clean-invalid] (mapv :reason rejected)))
      (is (re-find #"G7" (-> rejected first :message))))))

(deftest clean-missing-is-grandfathered-unless-strict
  (let [dir (tmp-deposit-dir! {"ft-no-clean.edn" (valid-record)})
        loose (esc/load-deposits dir)
        strict (esc/load-deposits dir {:strict-clean? true})]
    (is (= ["ft-test-001"] (map :fold-turn/id (:deposits loose))))
    (is (= :clean-missing (-> loose :deposits first :clean/status)))
    (is (empty? (:deposits strict)))
    (is (= [:clean-missing] (mapv :reason (:rejected strict))))))

(deftest replay-only-on-exact-sha
  (let [dir     (tmp-deposit-dir! {"ft-valid.edn" (valid-record)})
        turn-fn (esc/escrow-turn-fn (:deposits (esc/load-deposits dir)))]
    (testing "exact prompt match replays the recorded answer"
      (is (= mock-answer (turn-fn mock-prompt))))
    (testing "any drift in the prompt ⇒ nil ⇒ the fold abstains (pin 1)"
      (is (nil? (turn-fn (str mock-prompt " ")))))))

(deftest missing-dir-is-empty-not-an-error
  (is (= {:deposits [] :rejected []}
         (esc/load-deposits "/nonexistent/fold-turns-nowhere"))))

(def expected-real-deposits
  #{"ft-autoclock-in-001"
    "ft-live-geometric-stack-002"
    "ft-bayesian-structure-learning-003"
    "ft-aif-head-004"
    "ft-action-vocabulary-005"
    "ft-peradam-mechanization-006"
    "ft-first-flights-007"
    "ft-bounded-in-flight-state-008"
    "ft-evaluate-policies-009"
    "ft-aif-faithfulness-001"
    "ft-legacy-sorry-cleanup-001"
    "ft-fold-ansatz-001"
    "ft-operational-vocabulary-001"
    "ft-learning-loop-010"
    "ft-chipwitz-corps-001"
    "ft-futonzero-generative-011"
    "ft-pattern-mining-011"})

(defn- real-prose-fn [pattern-id]
  (slurp (str "/home/joe/code/futon3/library/" pattern-id ".flexiarg")))

(deftest real-fold-turns-load-clean-under-pin-1b
  (let [{:keys [deposits rejected]} (esc/load-deposits)
        ids (set (map :fold-turn/id deposits))]
    (is (empty? rejected) (pr-str rejected))
    (is (= expected-real-deposits ids))))

(deftest first-flights-replays-through-enact-style-seam
  (let [{:keys [deposits rejected]} (esc/load-deposits)
        dep (first (filter #(= "ft-first-flights-007" (:fold-turn/id %)) deposits))
        circumstance {:mission (:mission dep)
                      :psi (get-in dep [:cascade :psi])}
        entry {:mission (:mission dep)
               :shown (get-in dep [:cascade :pattern-ids])
               :F-free-energy 1.0
               :G-rollout nil}]
    (is (empty? rejected) (pr-str rejected))
    (is (some? dep) "007 deposit must be present in the real fold-turn corpus")
    (binding [cl/*escrow-replay?* true
              cl/*classical-fold-dG?* false]
      (let [ag (cl/act-gate-from-lane-entry
                entry circumstance
                {:escrow-turn-fn (esc/escrow-turn-fn deposits)
                 :prose-fn real-prose-fn})]
        (is (= (get-in dep [:eval :delta-g]) (:delta-G ag)))
        (is (= :fold-escrow (:delta-G/source ag)))
        (is (= (get-in dep [:turn :answer :boxes])
               (get-in ag [:fold-escrow :wiring :boxes])))))))
