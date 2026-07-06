(ns futon2.aif.fold-escrow-test
  "E-live-loop-2 gate 2d: the malformed-record rejection test. A deliberately
   malformed deposit must be REJECTED loudly (reason + file, in :rejected and
   on stderr) while a valid deposit still loads and replays on exact sha."
  (:require [clojure.test :refer [deftest is testing]]
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
      (is (= ["ft-test-001"] (map :fold-turn/id deposits))))
    (testing "each malformed record is rejected with its own loud reason"
      (is (= {"ft-no-arming.edn"  :missing-arming
              "ft-dg-drift.edn"   :delta-g-mismatch
              "ft-bad-pin.edn"    :prompt-not-reconstructable
              "ft-no-boxes.edn"   :empty-construction
              "ft-unreadable.edn" :unreadable-edn}
             reasons)))
    (testing "rejection messages carry the file (auditable, not just a boolean)"
      (is (every? #(re-find #"fold-turn REJECTED \[" (:message %)) rejected)))))

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
    "ft-bounded-in-flight-state-008"})

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
