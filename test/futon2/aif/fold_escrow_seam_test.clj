(ns futon2.aif.fold-escrow-seam-test
  "E-live-loop-2 gate 2e: the replay seam, dark. K2 both sides —
   (a) flag ON + injected turn-fn: a MOCK escrowed fold-turn replays through
       `act-gate-from-lane-entry`, its ΔG re-asserted against the deposit;
   (b) flag OFF (default): output key-for-key identical to the pre-seam shape,
       escrow never consulted (a throwing turn-fn proves it)."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [futon2.aif.close-loop :as cl]
            [futon2.aif.fold-escrow :as esc]
            [futon2.aif.fold-llm :as fl])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

;; A pattern the classical rule-table abstains on (contentful-library case) —
;; the exact honest-nothing regime the escrow exists to repair.
(def cascade ["invariant-coherence/state-snapshot-witness"])
(def circumstance {:mission "test-d/mission/mock" :psi "WANT: w. HAVE: h."})
(defn prose-fn [pattern-id]
  (slurp (str "/home/joe/code/futon3/library/" pattern-id ".flexiarg")))

(def mock-answer
  {:boxes        [{:id :b1 :role "mock construction step"
                   :fits-pattern (first cascade) :addresses-however "mock"}]
   :wires        []
   :terminals    [:b1]
   :policy-holes [{:unfolded-pattern nil :free "remainder" :why "mock"}]})

(defn- deposit-for
  "Build the ft record whose prompt-sha binds to EXACTLY this cascade ×
   circumstance × prose (pin 1) — the same prompt llm-fold will reconstruct."
  []
  (let [proses (into {} (for [p cascade] [p (prose-fn p)]))
        prompt (fl/fold-prompt cascade circumstance proses)]
    {:fold-turn/id "ft-seam-test-001"
     :mission (:mission circumstance)
     :cascade {:psi (:psi circumstance) :pattern-ids (vec cascade)}
     :prompt {:sha256 (esc/prompt-sha prompt)}
     :turn {:agent "test-agent" :model "mock" :at "2026-07-05T00:00:00Z"
            :answer mock-answer}
     :arming {:operator "joe" :word "mock arming word — seam test fixture" :at "2026-07-05T00:00:00Z"
              :scope :one-fold}
     :eval {:coverage-score-delta -0.5 :g-grain :coverage}}))

(defn- loaded-deposits []
  (let [dir (.toFile (Files/createTempDirectory "seam-test" (make-array FileAttribute 0)))]
    (spit (io/file dir "ft-seam-test-001.edn") (pr-str (deposit-for)))
    (let [{:keys [deposits rejected]} (esc/load-deposits (.getPath dir))]
      (is (empty? rejected) "seam fixture must load clean (pin 3 re-asserts ΔG)")
      deposits)))

(def entry {:mission "test-d/mission/mock" :cascade-score 0.1 :policy-rollout-score nil
            :shown cascade})

(deftest flag-on-mock-replay-with-dg-reasserted
  (binding [cl/*escrow-replay?* true]
    (let [deposits (loaded-deposits)
          ag (cl/act-gate-from-lane-entry
              entry circumstance
              {:escrow-turn-fn (esc/escrow-turn-fn deposits) :prose-fn prose-fn})]
      (testing "the escrowed ΔG leg fires with escrow provenance"
        (is (= -0.5 (:coverage-score-delta ag)))
        (is (= :fold-escrow (:coverage-score/source ag)))
        (is (= (get-in (first deposits) [:eval :coverage-score-delta]) (:coverage-score-delta ag))
            "replayed ΔG re-asserted against the deposit's stored leg"))
      (testing "the replayed construction rides :fold-escrow, holes preserved"
        (is (= (:policy-holes mock-answer)
               (get-in ag [:fold-escrow :policy-holes]))))
      (testing "prompt drift ⇒ abstain (pin 1): different prose, same deposit"
        (let [ag' (cl/act-gate-from-lane-entry
                   entry circumstance
                   {:escrow-turn-fn (esc/escrow-turn-fn deposits)
                    :prose-fn (constantly "DIFFERENT PROSE")})]
          (is (nil? (:coverage-score-delta ag')))
          (is (not (contains? ag' :fold-escrow))))))))

(deftest flag-default-on-flag-off-restores-pre-seam
  ;; 2g (operator-armed 2026-07-05): the seam is POWERED by default. The
  ;; pre-seam contract is preserved two ways, both asserted here: bound off,
  ;; the escrow is never consulted even when injected; and on-but-uninjected,
  ;; the nil turn-fn short-circuits — which is what keeps the default flip
  ;; byte-identical for callers (e.g. the scheduled runner) that pass no opts.
  (testing "2g: the seam is powered by default"
    (is (true? cl/*escrow-replay?*)))
  (binding [cl/*escrow-replay?* false]
    (let [throwing-turn-fn (fn [_] (throw (ex-info "escrow consulted with flag OFF" {})))
          with-seam (cl/act-gate-from-lane-entry
                     entry circumstance {:escrow-turn-fn throwing-turn-fn :prose-fn prose-fn})
          pre-seam  (cl/act-gate-from-lane-entry entry circumstance)]
      (testing "flag bound off: escrow never consulted, even when injected"
        (is (= pre-seam with-seam) "key-for-key identical to the 2-arity path"))
      (testing "output shape is the pre-seam contract exactly"
        (is (= #{:cascade-score :coverage-score-delta :coverage-score/source :fold} (set (keys with-seam))))
        (is (nil? (:coverage-score-delta with-seam)) "classical abstains on a contentful pattern; no escrow ⇒ abstain"))))
  (testing "flag ON, no turn-fn injected: seam inert, pre-seam shape exactly"
    (let [ag (cl/act-gate-from-lane-entry entry circumstance)]
      (is (= #{:cascade-score :coverage-score-delta :coverage-score/source :fold} (set (keys ag))))
      (is (nil? (:coverage-score-delta ag)) "no injection ⇒ no escrow leg, honest abstain"))))
