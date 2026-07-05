(ns futon2.aif.fold-escrow-test
  "E-live-loop-2 gate 2d: the malformed-record rejection test. A deliberately
   malformed deposit must be REJECTED loudly (reason + file, in :rejected and
   on stderr) while a valid deposit still loads and replays on exact sha."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [futon2.aif.fold-escrow :as esc])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def mock-prompt "FOLD TASK — mock prompt for the 2d escrow contract test.")

(def mock-answer
  ;; 1 box + 1 hole ⇒ coverage 0.5 ⇒ coverage→rollout ΔG −0.5 (the Leg-2 shape)
  {:boxes        [{:id :b1 :role "concrete reshape step" :fits-pattern "invariant-coherence/shape-first-identify"
                   :addresses-however "mock"}]
   :wires        []
   :terminals    [:b1]
   :policy-holes [{:unfolded-pattern nil :free "the un-grounded remainder" :why "mock hole"}]})

(defn- valid-record []
  {:fold-turn/id "ft-test-001"
   :mission "test-d/mission/mock"
   :cascade {:psi "WANT: x. HAVE: y." :pattern-ids ["invariant-coherence/shape-first-identify"]}
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
