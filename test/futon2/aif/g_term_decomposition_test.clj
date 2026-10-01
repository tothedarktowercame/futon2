(ns futon2.aif.g-term-decomposition-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.cascade-observation-scoring :as scoring]
            [futon2.aif.efe :as efe]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.g-term-decomposition :as d]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.policy :as policy]
            [futon2.aif.token-belief-carry-test :as carry-fixture]
            [futon2.aif.token-observation-initialization-test :as observation-fixture])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores)

;; Recorded tick-001 inputs, transcribed unchanged from
;; vm-test/futon2/vm/tick_001_s06_r5_test.clj, definitions T through spec.
;; This is an OFFLINE replay, not a new production click.
(def T 3)

(def q0
  (m/observed-belief #{:summary-without-total-repos-throws
                       :active-repo-ratio-absent-default-is-0
                       :coupling-density-reads-same-key-with-default
                       :observe-empty-does-not-throw}))

;; One common universe: q0 ∪ want ∪ every token named by any pattern guard.
(def universe
  #{:summary-without-total-repos-throws
    :active-repo-ratio-absent-default-is-0
    :coupling-density-reads-same-key-with-default
    :observe-empty-does-not-throw
    :summary-without-total-repos-observes-cleanly
    :test-covers-missing-total-repos})

(def rates (zipmap universe (repeat {:false-neg 0 :false-pos 0})))

;; Patterns exactly as R4 built them from 03-R6's interpretations.
(def patt-sov
  {:id :aif/structured-observation-vector
   :produces #{:summary-without-total-repos-observes-cleanly}
   :theta 1
   :guard {:status :interpreted
           :clauses [{:status :interpreted
                      :present #{:summary-without-total-repos-throws}
                      :absent #{:summary-without-total-repos-observes-cleanly}}]}})

(def patt-ph
  {:id :aif/placeholder-is-load-bearing
   :produces #{:summary-without-total-repos-observes-cleanly
               :active-repo-ratio-absent-default-is-0}
   :theta 1
   :guard {:status :interpreted
           :clauses [{:status :interpreted
                      :present #{:coupling-density-reads-same-key-with-default
                                 :summary-without-total-repos-throws}
                      :absent #{:summary-without-total-repos-observes-cleanly}}]}})

(def patt-test
  {:id :test-step-covering-missing-total-repos
   :produces #{:test-covers-missing-total-repos}
   :theta 1
   :guard {:status :interpreted
           :clauses [{:status :interpreted
                      :present #{:summary-without-total-repos-throws}
                      :absent #{:test-covers-missing-total-repos}}]}})

(def cascades
  {:C0-empty []
   :C1-test-first [patt-test patt-sov]
   :C2-fix-first [patt-ph patt-test patt-sov]
   :C3-fix-only [patt-sov]})

(def spec {:want #{:summary-without-total-repos-observes-cleanly
                   :active-repo-ratio-absent-default-is-0
                   :test-covers-missing-total-repos}
           :evidence #{}
           :lam 1
           :mu 1
           :zeroed #{}})


(def expected-g
  {:C0-empty 13.101074797244184 :C1-test-first 11.434408130577516
   :C2-fix-first 11.434408130577516 :C3-fix-only 12.101074797244184})

(defn replay []
  (let [ranked (efe/rank-cascade-actions
                {:cascade-belief q0}
                (mapv (fn [[id precedence]]
                        {:kind :cascade-candidate :id id :precedence precedence})
                      (sort-by key cascades))
                {:horizon-steps T :cascade-spec spec})]
    {:ranked ranked :decision (policy/select-action-cascades ranked {:beta 1})}))

(defn class-model-from-decision []
  (first (filter #(and (map? %) (= :class-emission (:kind %)))
                 (tree-seq coll? seq
                           ((requiring-resolve 'futon2.aif.token-belief-carry-test/decision) nil)))))

(defn- captured-class-q [run!]
  (let [calls (atom [])
        score scoring/rank-cascade-actions]
    (with-redefs [scoring/rank-cascade-actions
                  (fn [state candidates opts]
                    (let [ranked (score state candidates opts)]
                      (swap! calls conj ranked)
                      ranked))]
      (run!)
      (->> @calls
           (mapcat identity)
           (keep #(get-in % [:certificate :consumed-g :Q]))
           (filter #(= :upstream-initialization-conditioning (:form %)))
           last))))

(deftest upstream-initialization-q-is-classified-from-the-record
  (let [open-q (captured-class-q #(carry-fixture/decision nil))
        conditioned-q (captured-class-q
                       #(observation-fixture/with-two-ticks (fn [_] nil)))
        applied (get-in conditioned-q [:conditioning :applied-to])
        vacuous-belief (into {} (map (fn [[state mass]] [(conj state :already-true) mass])) applied)
        vacuous (-> conditioned-q
                    (assoc :initial-belief vacuous-belief)
                    (assoc-in [:conditioning :applied-to] vacuous-belief)
                    (assoc-in [:conditioning :observation-updates]
                              [{:status :updated :token :already-true :observed true}]))
        malformed (assoc-in conditioned-q [:conditioning :observation-updates 0]
                            (dissoc (get-in conditioned-q [:conditioning :observation-updates 0])
                                    :status))]
    (is (= :open-loop-no-conditioning (:reason (d/verdict :Q open-q))))
    (is (= :observation-conditioned (:reason (d/verdict :Q conditioned-q))))
    (is (= :conditioning-vacuous (:reason (d/verdict :Q vacuous))))
    (is (= :q-beliefs-not-recorded
           (:reason (d/verdict :Q (update conditioned-q :conditioning dissoc :applied-to)))))
    (is (= :q-updates-malformed (:reason (d/verdict :Q malformed))))
    (is (= :unsupported-q-form
           (:reason (d/verdict :Q (assoc conditioned-q :form :something-else)))))))

(deftest a-verdict-dispatches-on-the-recorded-likelihood-kind
  (let [identity-a {:x {:false-neg 0 :false-pos 0}}
        noisy-a {:x {:false-neg 1/4 :false-pos 0}}
        model (class-model-from-decision)
        target (ffirst (:acceptance model))
        stochastic (assoc-in model [:target-class target] {:focused 1/2 :related 1/2})]
    (is (= :identity-kernel (:reason (d/verdict :A identity-a))))
    (is (= :non-identity-kernel (:reason (d/verdict :A noisy-a))))
    (is (= {:status :present :value model :verdict :degenerate
            :reason :deterministic-class-emission}
           (d/verdict :A model)))
    (is (= :non-degenerate (:verdict (d/verdict :A stochastic))))
    (is (= :stochastic-class-emission (:reason (d/verdict :A stochastic))))
    (is (= :class-emission-fields-missing
           (:reason (d/verdict :A {:kind :class-emission}))))
    (is (= :invalid-class-emission-row
           (:reason (d/verdict :A (update model :target-class dissoc target))))
        "a target with no declared class is not read as stop-the-line")
    (is (= :missing (:status (d/verdict :A {:kind :something-else}))))
    (is (= :unsupported-a-shape
           (:reason (d/verdict :A {:kind :something-else}))))))

(deftest census-completes-with-a-recorded-class-emission
  (let [model (class-model-from-decision)
        q {#{} 1}
        q-evidence {:initial-belief q :steps [{:tau 1 :belief q}]
                    :observation-updates []}
        ranked [{:action :class-candidate :controller-score 1
                 :certificate {:consumed-g
                               {:A model
                                :C {:form :step-indexed
                                    :steps [{:distribution {:focused 1}}]}
                                :D q :Q q-evidence}}}]
        census (d/census ranked [{:id :class-candidate :habit 1 :f 0}])]
    (is (= :present (:status census)))
    (is (= :deterministic-class-emission
           (get-in census [:policies 0 :terms :A :reason])))))

(deftest census-projects-class-emission-to-the-policy-target
  (let [target "M-daily-scan-multi-axis-queue"
        unrelated "M-formal-patterns"
        model (-> (class-model-from-decision)
                  (assoc :acceptance #{[target :daily-token]
                                       [unrelated :formal-token]})
                  (assoc :target-class {target :related unrelated :unknown}))
        q {#{} 1}
        ranked [{:action {:kind :cascade-candidate :id :C1 :target target}
                 :controller-score 1
                 :certificate {:consumed-g
                               {:A model
                                :C {:form :step-indexed
                                    :steps [{:distribution {:related 1}}]}
                                :D q
                                :Q {:initial-belief q
                                    :steps [{:tau 1 :belief q}]
                                    :observation-updates []}}}}]
        candidate {:id (:action (first ranked)) :habit 1 :f 0}
        census (d/census ranked [candidate])
        retained-a (get-in census [:policies 0 :terms :A :value])]
    (is (= :present (:status census)))
    (is (= #{[target :daily-token]} (:acceptance retained-a)))
    (is (= {target :related} (:target-class retained-a)))
    (is (= :deterministic-class-emission
           (get-in census [:policies 0 :terms :A :reason])))
    (testing "an unknown row remains invalid when it belongs to the policy"
      (let [unknown-census (d/census ranked [(assoc-in candidate [:id :target]
                                                       unrelated)])]
        (is (= :missing (:status unknown-census)))
        (is (= :invalid-class-emission-row
               (get-in unknown-census [:policies 0 :terms :A :reason])))
        (is (= {unrelated {:unknown 1}}
               (get-in unknown-census [:policies 0 :terms :A :invalid-rows])))))))

(deftest each-term-can-distinguish-nondegenerate-input
  (doseq [[term value] {:A {:token {:false-neg 1/4 :false-pos 1/3}}
                       :C {:form :step-indexed
                           :steps [{:distribution {:x 1/4 :y 3/4}}
                                   {:distribution {:x 3/4 :y 1/4}}]}
                       :D {#{:x} 1/4 #{:y} 3/4}
                       :E 2 :F 3/2
                       :Q {:initial-belief {#{:x} 1}
                           :steps [{:tau 1 :belief {#{:x} 1}}]
                           :observation-updates [{:tau 0 :observation #{:x}
                                                  :status :value :consumed true :vacuous false
                                                  :predicted-belief {#{} 1/2 #{:x} 1/2}
                                                  :post-belief {#{:x} 1}}]}}]
    (is (= :non-degenerate (:verdict (d/verdict term value {:all-habits [2 1]}))) (str term)))
  (doseq [term d/terms]
    ;; F-ABS (PROOF-2 packet 27): an absent F is typed :absent (omitted from
    ;; the law), not :missing; every other term's nil case is unchanged.
    (is (= (if (= :F term) :absent :missing) (:status (d/verdict term nil))))
    (is (not (contains? (d/verdict term nil) :verdict)))))

(deftest consumed-f-and-missing-scoring-evidence
  (let [ranked [{:action :a :controller-score 1
                 :certificate {:f {:value ##Inf :status :computed-not-attached
                                   :reason :non-finite-under-identity-a}}}]
        c (get-in (policy/select-action-cascades ranked {:beta 1})
                  [:selection-certificate :g-term-decomposition])
        term (get-in c [:policies 0 :terms :F])]
    (is (= :missing (:status c)))
    (is (= 0 (:value term)))
    (is (= :degenerate (:verdict term)))
    (is (= ##Inf (get-in c [:policies 0 :f-provenance :computed-f :value])))
    (is (= :missing (get-in c [:policies 0 :terms :A :status])))))

(deftest recorded-tick-replay-persists-six-consumed-terms
  (let [{:keys [ranked decision]} (replay)
        dir (.toFile (Files/createTempDirectory "g-term-run-" (make-array FileAttribute 0)))
        before {:repair (count (filter #(.isFile %) (file-seq (io/file hermetic/production-repair-root))))
                :trip (count (filter #(.isFile %) (file-seq (io/file hermetic/production-trip-root))))}
        result {:outcome :offline-selection-replay
                :checkpoints {:selection {:judgment {:controller-decision decision}}}}]
    (try
      (let [written (#'runner/persist-run-record! {:run-record-dir (.getPath dir)
                                                  :scan-render-fn (fn [& _] nil)}
                                                 "offline-tick-001" "2026-09-19T00:00:00Z" result)
            record (edn/read-string (slurp (:run-record written)))
            census (get-in record [:decision :g-term-decomposition])
            ;; Independent probe: use the replay's actual rate input and
            ;; enumerate token observations through A, not the certificate.
            observation (m/token-likelihood rates (ffirst q0) (ffirst q0))
            mismatches (mapv #(m/token-likelihood rates (ffirst q0)
                                                 (if (contains? (ffirst q0) %)
                                                   (disj (ffirst q0) %) (conj (ffirst q0) %)))
                             universe)
            after {:repair (count (filter #(.isFile %) (file-seq (io/file hermetic/production-repair-root))))
                   :trip (count (filter #(.isFile %) (file-seq (io/file hermetic/production-trip-root))))}]
        (is (= before after))
        (is (= 1 observation))
        (is (every? zero? mismatches))
        (is (= :present (:status census)))
        (is (= 4 (count (:policies census))))
        (is (false? (:traceWritten record)))
        (doseq [[entry policy-record] (map vector ranked (:policies census))]
          (is (= rates (get-in policy-record [:terms :A :value])))
          (is (< (Math/abs (- (:G-efe entry) (expected-g (:cascade-id entry)))) 1e-12))
          (is (= (set d/terms) (set (keys (:terms policy-record)))))
          (is (every? #(= :degenerate (:verdict %)) (vals (:terms policy-record))))
          (is (= :identity-kernel (get-in policy-record [:terms :A :reason])))
          (is (= q0 (get-in policy-record [:terms :D :value])))
          (is (= 3 (count (get-in policy-record [:terms :Q :value :steps]))))
          (is (= 0 (get-in policy-record [:terms :F :value])))
          (is (= ##Inf (get-in policy-record [:f-provenance :computed-f :value]))))
        (println "G-TERM-REPLAY-RECEIPT"
                 (pr-str {:scope :offline-selection-replay :source :vm/tick-001
                          :run-record record :store-counts {:before before :after after}
                          :independent-A-probe {:same-state observation :single-token-flips mismatches}
                          :g (into {} (map (juxt :cascade-id :G-efe) ranked))})))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))

(deftest run-without-cascade-selection-records-missing-evidence
  (is (= {:schema :wm/g-term-decomposition-v1 :status :missing
          :reason :no-recorded-cascade-selection :policies []}
         (d/from-result {:outcome :grounded-change}))))

(deftest emitted-kernel-is-the-tempered-kernel-consumed-by-g
  (let [input {:rates {:x {:false-neg 1/4 :false-pos 1/4}}
               :q0 {#{:x} 1} :precedence-fn (constantly [])
               :horizon 2 :spec {:want #{:x} :lam 1 :mu 1} :universe #{:x} :zeta 2}
        {:keys [g certificate]} (m/horizon-g-sparse-cert input)
        kernel (get-in certificate [:consumed-g :A])]
    (is (= g (m/horizon-g-sparse input)))
    ;; Squaring and normalizing [1/4,3/4] gives [1/10,9/10].
    (is (< (Math/abs (- 0.1 (double (get-in kernel [:x :false-neg])))) 1e-12))
    (is (= :non-degenerate (:verdict (d/verdict :A kernel))))
    (is (= [{:tau 1 :belief {#{:x} 1}} {:tau 2 :belief {#{:x} 1}}]
           (get-in certificate [:consumed-g :Q :steps])))))

(deftest existing-selection-decisions-unchanged
  ;; Same baseline as selection-certificate-test/decisions-byte-identical, and
  ;; compared the same way as that twin: the precision metadata added after the
  ;; snapshot is asserted on the baseline, and the later additive law records
  ;; are projected away rather than the bytes re-captured -- re-recording a
  ;; byte-identity fixture to make it pass is how such a test stops meaning
  ;; anything (claude-5, 2026-09-24). The fixture is shared with that twin,
  ;; which needs it to stay the historical snapshot, so it is not touched here.
  (doseq [{:keys [ranked beta decision-bytes]}
          (:cases (edn/read-string (slurp (io/resource "fixtures/selection-certificate/before.edn"))))]
    ;; :gamma, :tau and :tau-source joined the law at 991e27a4 (2026-09-21),
    ;; which is the commit this assertion first failed at; their values are
    ;; asserted rather than projected away.
    (is (= (pr-str (update (edn/read-string decision-bytes) :selection-law
                           assoc :gamma (/ 1.0 beta) :tau beta :tau-source :declared-beta))
           (pr-str (-> (policy/select-action-cascades ranked {:beta beta})
                       (dissoc :selection-certificate)
                       (update :selection-law dissoc
                               ;; :policy-comparison and :near-tie-threshold
                               ;; joined at b701baa3 (2026-09-21), acting-policy
                               ;; odds and action-level flips; :action-comparison
                               ;; at 978c5067 (2026-09-22), the ticket-queue
                               ;; strata. Additive reporting: no winner, no
                               ;; marginal key and no tie-break moves.
                               :policy-comparison :action-comparison :near-tie-threshold
                               ;; :enacted-steps joined at b1979ce2 (2026-09-23),
                               ;; pinned independently in enacted-step-test.
                               :enacted-steps
                               ;; :candidate joined with M-wm-wiring row 9
                               ;; (claude-10, 2026-09-25), the chosen entry's
                               ;; :cascade-id; pinned in
                               ;; selection-law-candidate-test.
                               :candidate
                               ;; :e-source joined with M-wm-wiring step 8 (claude-10,
                               ;; 2026-09-25): where E came from, the enactment fold;
                               ;; pinned in selection-reads-fold-test.
                               :e-source)))))))
