(ns futon2.aif.wm.q6-repeat-after-refusal-test
  "Requirements Q6 on the production reading-family route: when the previous
  started run in the run stream closed refused and its selection-input
  digest equals this run's, the previous structural reading-policy key is
  removed before the one G selector. The previous run is found by recorded
  start instant, the identity compared is the structural key, and the
  exporter reads only the run's own sealed :q6 carrier."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.previous-run :as previous-run]
            [futon2.test-support.reading-route :as route]
            [futon2.test-support.runner-fixture :as fixture]
            [wm-run-facts :as facts]))

(use-fixtures :once hermetic/with-hermetic-stores fixture/with-hermetic-traces)
(use-fixtures :each
  (fn [run]
    (binding [runner/*wm-status-reporting?* false]
      (route/with-empty-learning-ledger run))))

(def snap {:open-missions #{route/target} :open-excursions #{} :open-tickets #{}
           :patterns #{"p/a" "p/b" "p/c"} :seats #{"zai-5"} :library-pattern-count 3})

(defn- judgement-for [decision]
  {:decision decision :belief {} :belief-pre {} :observation {} :free-energy {}
   :prediction-errors {} :precision-state {} :micro-step-trace [] :mode :maintain})

(defn- click!
  "One production run over FX in run-record DIR: the runner reads the Q6
  predecessor from DIR's stream and binds it; the judge is the real reading
  family decision. Returns the result, the persisted record and the
  selector call count."
  ([fx dir] (click! fx dir {}))
  ([fx dir extra]
   (let [calls (atom 0)
         result (runner/run-opportunity!
                 (merge (fixture/isolated-runner-opts)
                        {:run-record-dir (str dir)
                         :judge-fn (fn [_]
                                     (let [r (route/decide fx 1 extra)]
                                       (swap! calls + (:selector-calls r))
                                       {:judgement (judgement-for (:decision r))}))
                         :repair-system-record-fn (fn [m] {:repair/id "repair-test"
                                                           :repair/class (:repair-class m)})
                         :dispatch-fn (fn [& _] (throw (ex-info "Unexpected dispatch" {})))}))
         record (edn/read-string {:default tagged-literal} (slurp (:run-record result)))]
     {:result result :record record :calls @calls
      :decision (or (get-in result [:checkpoints :selection :judgment :decision])
                    (get-in result [:checkpoints :selection :sorry :decision]))})))

(defn- q6 [run] (get-in run [:decision :selection-certificate :q6]))
(defn- chosen-key [run] (get-in run [:record :q6 :terminal :choice :policy-key]))
(defn- exported [run] (facts/q6-facts (:record run)))

(defn- lean-q6
  "Requirements.Q6 over exported facts: :nr when any field is not
  recomputable, else the implication's truth value."
  [f]
  (if (some #(and (map? %) (contains? % "not-recomputable")) (vals f))
    :nr
    (or (not (and (= "refused" (f "previousOutcome"))
                  (= (f "previousInputDigest") (f "currentInputDigest"))))
        (not= (f "previousChoice") (f "currentChoice")))))

(deftest repeat-after-refusal-on-same-inputs-selects-a-distinct-policy
  (let [fx (route/fixture)
        dir (route/temp-dir "q6-stream-")
        one (click! fx dir)
        two (click! fx dir)
        k1 (chosen-key one)]
    (testing "run one: no earlier run, nothing excluded, refused after selection"
      (is (= 1 (:calls one)))
      (is (= [:not-recomputable :no-earlier-started-run] ((juxt :status :reason) (q6 one))))
      (is (= {:status :present :outcome :refused :kind :reading-policy-execution-semantics-owed}
             (get-in one [:record :q6 :terminal :outcome])))
      (is (previous-run/reading-key? k1))
      (is (= :nr (lean-q6 (exported one)))))
    (testing "run two: the predecessor is run one, read by the runner"
      (is (= (get-in one [:record :run/id])
             (get-in two [:record :q6 :predecessor :previous-run/id])))
      (is (= (get-in one [:record :q6 :terminal])
             (get-in two [:record :q6 :predecessor :previous]))))
    (testing "the same inputs: exactly run one's key is removed before the one selector"
      (is (= 1 (:calls two)))
      (is (= (get-in one [:record :q6 :terminal :input-digest])
             (get-in two [:record :q6 :terminal :input-digest])))
      (is (= :excluded (:status (q6 two))))
      (is (= [k1] (:excluded-keys (q6 two))))
      (is (= [2 1] ((juxt :menu :removed) (q6 two))))
      (is (previous-run/reading-key? (chosen-key two)))
      (is (not= k1 (chosen-key two)))
      (is (not-any? #(= k1 (get-in % [:id :policy-key]))
                    (get-in two [:decision :selection-certificate :candidates]))
          "the excluded policy never reached the selector's menu"))
    (testing "the exporter reads the carrier and Q6 holds"
      (let [f (exported two)]
        (is (= "refused" (f "previousOutcome")))
        (is (= (f "previousInputDigest") (f "currentInputDigest")))
        (is (= true (lean-q6 f)))))))

(deftest only-candidate-refuses-typed-until-inputs-change
  (let [fx (route/fixture ["p/a"])
        dir (route/temp-dir "q6-stream-")
        one (click! fx dir)
        two (click! fx dir)
        three (click! fx dir)]
    (is (= 1 (:calls one)))
    (testing "the one policy was refused on the same inputs: no selection"
      (is (= 0 (:calls two)))
      (is (= :abstained (get-in two [:decision :status])))
      (is (= :repeat-choice-after-refusal (get-in two [:decision :refusals 0 :kind])))
      (is (= {:condition :selection-inputs-change
              :input-digest (get-in one [:record :q6 :terminal :input-digest])
              :excluded-key (chosen-key one)}
             (get-in two [:decision :refusals 0 :rearm])))
      (is (= {:status :absent :reason :chose-nothing}
             (get-in two [:record :q6 :terminal :choice])))
      (is (= :refused (get-in two [:record :q6 :terminal :outcome :outcome]))))
    (testing "the refusal holds on the next run while the inputs are unchanged"
      (is (= 0 (:calls three)))
      (is (= [:refused :repeat-refusal-rearm-not-met]
             ((juxt :status :reason) (q6 three)))))
    (testing "changed inputs re-arm selection of the same policy"
      (let [four (with-redefs [ledger/pattern-theta
                               (fn [_] {:status :recorded-trials :theta 13/16 :trials-count 7
                                        :successes 6 :identities [] :targets [route/target]})]
                   (click! fx dir))]
        (is (= 1 (:calls four)))
        ;; run three chose nothing, so Q6 itself has no previous choice to
        ;; compare: not recomputable, and nothing is excluded
        (is (= [:not-recomputable :previous-choice-absent 0]
               ((juxt :status :reason :removed) (q6 four))))
        (is (= (chosen-key one) (chosen-key four)))))))

(deftest changed-inputs-allow-the-same-policy
  (let [fx (route/fixture)
        dir (route/temp-dir "q6-stream-")
        one (click! fx dir)
        two (with-redefs [ledger/pattern-theta
                          (fn [_] {:status :recorded-trials :theta 13/16 :trials-count 7
                                   :successes 6 :identities [] :targets [route/target]})]
              (click! fx dir))]
    (is (not= (get-in one [:record :q6 :terminal :input-digest])
              (get-in two [:record :q6 :terminal :input-digest])))
    (is (= [:not-triggered :selection-inputs-changed 0]
           ((juxt :status :reason :removed) (q6 two))))
    (is (= true (lean-q6 (exported two))))))

;; -- the predecessor, built from a real stream ---------------------------------

(defn- decision [fx] (:decision (route/decide fx)))

(defn- closed-stream!
  "DIR's stream holding runs [id started-at terminal-opts] each started and
  closed, in the order given."
  [dir runs]
  (doseq [[id at t] runs]
    (previous-run/start! dir id at)
    (when t
      (previous-run/close! dir (previous-run/terminal (merge {:run-id id :started-at at} t))
                           (str "tick-run-record-" id ".edn"))))
  dir)

(defn- decide-after [fx predecessor]
  (binding [previous-run/*predecessor* predecessor]
    (route/decide fx)))

(deftest same-day-runs-are-ordered-by-recorded-start-not-by-name
  (let [fx (route/fixture)
        d (decision fx)
        refused {:decision d :downstream {:kind :reading-policy-execution-semantics-owed}}
        ok {:decision d :outcome :grounded-change}
        ;; ids sort opposite to their start instants
        dir (closed-stream! (route/temp-dir "q6-order-")
                            [["2026-10-15-ffff" "2026-10-15T10:00:00Z" refused]
                             ["2026-10-15-0000" "2026-10-15T11:00:00Z" ok]
                             ["2026-10-15-8888" "2026-10-15T13:00:00Z" refused]])
        p (previous-run/predecessor dir "2026-10-15-cccc" "2026-10-15T12:00:00Z")]
    (is (= :present (:status p)))
    (is (= "2026-10-15-0000" (:previous-run/id p))
        "the latest start before this run's, not the name that sorts last")
    (is (= :changed (get-in p [:previous :outcome :outcome])) "its own close")
    (is (= [:not-triggered :previous-outcome-not-refused]
           ((juxt :status :reason) (get-in (decide-after fx p) [:decision :selection-certificate :q6]))))
    (testing "a successful previous outcome excludes nothing"
      (is (= 0 (get-in (decide-after fx p) [:decision :selection-certificate :q6 :removed]))))))

(defn- carrier-of [result] (get-in result [:decision :selection-certificate :q6]))

(deftest absent-unreadable-and-tampered-predecessors-never-certify
  (let [fx (route/fixture)
        d (decision fx)
        refused {:decision d :downstream {:kind :reading-policy-execution-semantics-owed}}
        now "2026-10-15T12:00:00Z"
        stream (fn [runs] (closed-stream! (route/temp-dir "q6-bad-") runs))
        cases
        {:no-stream [(previous-run/predecessor (route/temp-dir "q6-none-") "c" now)
                     :no-earlier-started-run]
         :not-closed [(previous-run/predecessor (stream [["a" "2026-10-15T10:00:00Z" nil]]) "c" now)
                      :previous-run-not-closed]
         :unreadable-line
         [(let [dir (stream [["a" "2026-10-15T10:00:00Z" refused]])]
            (spit (previous-run/stream-file dir) "{:event :started :run/id\n" :append true)
            (previous-run/predecessor dir "c" now))
          :run-stream-unreadable]
         :tampered-close
         [(let [dir (stream [["a" "2026-10-15T10:00:00Z" refused]])
                f (previous-run/stream-file dir)]
            (spit f (.replace ^String (slurp f) ":refused" ":changed"))
            (previous-run/predecessor dir "c" now))
          :previous-terminal-carrier-tampered]
         :tampered-predecessor
         [(assoc-in (previous-run/predecessor (stream [["a" "2026-10-15T10:00:00Z" refused]]) "c" now)
                    [:previous :outcome :outcome] :changed)
          :predecessor-carrier-tampered]
         :not-supplied [nil :predecessor-carrier-not-supplied]
         :foreign-choice
         [(previous-run/predecessor
           (stream [["a" "2026-10-15T10:00:00Z"
                     (assoc refused :decision {:action {:target route/target
                                                        :precedence [{:id "p/a"}]}})]])
           "c" now)
          :previous-choice-foreign]}]
    (doseq [[label [p reason]] cases]
      (testing (name label)
        (let [r (decide-after fx p)
              c (carrier-of r)]
          (is (= [:not-recomputable reason] ((juxt :status :reason) c)))
          (is (= 0 (:removed c)) "nothing excluded")
          (is (= 1 (:selector-calls r)))
          (let [f (facts/q6-facts {:run/id "c"
                                   :q6 {:predecessor p
                                        :terminal (previous-run/terminal
                                                   {:run-id "c" :started-at now
                                                    :decision (:decision r)
                                                    :downstream {:kind :x}})}})]
            (is (= :nr (lean-q6 f)) "missing evidence is not recomputable, never a pass")
            (is (contains? (f "previousChoice") "not-recomputable"))))))))

(deftest only-the-exact-structural-identity-is-excluded
  (let [fx (route/fixture)
        d (decision fx)
        p0 (previous-run/predecessor
            (closed-stream! (route/temp-dir "q6-id-")
                            [["a" "2026-10-15T10:00:00Z"
                              {:decision d :downstream {:kind :reading-policy-execution-semantics-owed}}]])
            "c" "2026-10-15T12:00:00Z")
        k (get-in p0 [:previous :choice :policy-key])
        [tag target id structure] k
        reseal (fn [key]
                 (let [prev (previous-run/seal (-> (:previous p0) (dissoc :sha256)
                                                   (assoc-in [:choice :policy-key] key)))]
                   (previous-run/seal (-> p0 (dissoc :sha256) (assoc :previous prev)))))]
    (testing "the same key, same inputs: excluded"
      (is (= [:excluded 1] ((juxt :status :removed) (carrier-of (decide-after fx p0))))))
    (doseq [[label key] {:arrangement [tag target id (update structure :edges conj ["p/a" "p/a" :overlap])]
                         :target [tag "M-elsewhere" id structure]
                         :policy [tag target (str target "/policy-0000000000000000ffff") structure]
                         :precedence-vector [target [{:id "p/a"} {:id "p/b"}]]}]
      (testing (name label)
        (let [r (decide-after fx (reseal key))]
          (is (= 0 (:removed (carrier-of r))) "a different identity removes nothing")
          (is (= 2 (count (get-in r [:decision :selection-certificate :candidates])))))))))

(deftest exporter-reads-only-the-run-carrier
  (let [fx (route/fixture)
        dir (route/temp-dir "q6-stream-")
        _one (click! fx dir)
        two (click! fx dir)
        record (:record two)]
    (is (= true (lean-q6 (facts/q6-facts record))))
    (testing "without the carrier nothing is searched for"
      (is (= :nr (lean-q6 (facts/q6-facts (dissoc record :q6)))))
      (is (= :nr (lean-q6 (facts/q6-facts (update-in record [:q6] dissoc :predecessor))))))
    (testing "a tampered terminal is not recomputable"
      (is (= :nr (lean-q6 (facts/q6-facts
                           (assoc-in record [:q6 :terminal :input-digest] "0000"))))))
    (testing "the full export carries the same five fields"
      (let [f (:facts (facts/facts-for-record record "r" snap))]
        (is (= (facts/q6-facts record)
               (select-keys f ["previousChoice" "previousOutcome" "previousInputDigest"
                               "currentChoice" "currentInputDigest"])))))
    (is (.exists (io/file dir previous-run/stream-name)))))
