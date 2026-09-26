(ns futon2.aif.flight-conditioning-step-test
  "F1b-join-I (PROOF-2a-PLAN <2>2d F1; F1c-D futon3c 8cc2d425): the
  conditioning step bound on the flight's enactment entry, from the entry's
  observation and policy key and the click's run record. First-layer wire
  test through run! with a hermetic run record; SPEC-F's bad cases
  assigned to this packet.

  F1a-2c: the run record's [:decision :measured-a] is the record the tick's
  PRODUCER builds, futon2.report.war-machine/measured-a-version, from
  admitted labels through observation-rates/sourced-rates, never a
  hand-built map (the earlier hand-built record carried a :measurement key
  the producer did not write, so the step's test passed against its own
  stub). Bad inputs are that producer's record with a key removed."
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-model-manifest :as manifest]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def target "M-f1b-join")
(def key-a [target [:p/a] {}])

(defn- pattern [theta]
  {:guard {:status :interpreted :clauses [{:present #{} :absent #{}}]}
   :transition {:status :interpreted :produces #{:t}}
   :theta theta})

(defn- labels
  "Admitted labels for CLS: PRESENT established tokens of which MISSED were
  recorded false, ABSENT non-established of which REPORTED were recorded
  true, so false-neg = MISSED/PRESENT and false-pos = REPORTED/ABSENT."
  [cls present missed absent reported]
  (concat (for [i (range present)] {:token-class cls :admitted :present :recorded (>= i missed)})
          (for [i (range absent)] {:token-class cls :admitted :absent :recorded (< i reported)})))

(defn- produced-measured-a
  "The measured-A record as the tick writes it: measured-a-version over one
  problem of TARGET with LOCATORS, from LABELS (default: :t of class :C4,
  false-neg 1/10, false-pos 1/5)."
  [{:keys [locators] :as opts}]
  (let [ls (vec (or (:labels opts) (labels :C4 10 1 5 1)))]
    (wm/measured-a-version [{:target target :cascade-problem {:locators (or locators {:t {:class :C4}})}}]
                           {target {:labels ls
                                    :subjects (frequencies (map :token-class ls))}})))

(defn- run-record [{:keys [theta measured-a] :as opts}]
  {:decision
   {:measured-a (or measured-a (produced-measured-a opts))
    ;; the belief holds :u, a token the step does not check
    :initial-belief-receipt {:value {#{[target :u]} 1}}
    :selection-certificate
    {:token-belief-stage
     {:domain-inputs [{:target target
                       :declaration {:interpretations {:p/a (pattern (or theta 1/2))}}}]}}}})

(defn- fly [rr-opts & {:keys [fetch?] :or {fetch? true}}]
  (let [dir (str (.toFile (Files/createTempDirectory "f1bj" (make-array FileAttribute 0))))
        enact (fr/enact-fn {:dispatch-step! (fn [_] {:commit "c1" :produced :t :check {:class :C4}})
                            :check-fn (constantly {:observed true})
                            :interpretations (constantly {:p/a {:produces #{:t}}})
                            :record-dir dir})
        f (flight/run! (flight/start {:target target :chosen-because {:kind :requested}}
                                     {:kind :operator-declared :wants [:t :u] :declared-by "test"}
                                     {:id "flight-f1bj"})
                       (cond-> {:click-fn (constantly {:click-id "run-1" :chosen {:candidate :cand/a :precedence [:p/a]}})
                                :enact-fn enact
                                :wc-fn (fn [_ _] {:wc {:verdict []} :increment {:record-id "r1" :delta 1 :policy-key key-a}})
                                :observe-fn (fn [_ _] {:t true})
                                :sources-fn (constantly {})
                                :max-clicks 1}
                         fetch? (assoc :fetch-run-record (fn [click-id] (when (= "run-1" click-id) (run-record rr-opts))))))]
    (first (:enactments f))))

(deftest the-step-is-bound-and-its-q-is-exact-update-over-the-restricted-kernel
  (let [step (:step (fly {}))
        V #{:t}
        rates {:t {:false-neg 1/10 :false-pos 1/5}}
        pushed (manifest/rollout (constantly [(assoc (pattern 1/2) :id :p/a)]) {#{:u} 1} 1)
        lik (fn [s o] (manifest/token-likelihood rates (set/intersection s V) o))]
    (is (= :present (:status step)))
    (is (= :wm/conditioning-step-v1 (:schema step)))
    (is (= key-a (:policy-key step)))
    (is (= {:flight "flight-f1bj" :click "run-1"} (:occurrence step)))
    (is (= {#{:u} 1} (get-in step [:s-prev :value])) "sPrev: the decision's belief, marginalised to the target")
    (is (= :initial-belief (get-in step [:s-prev :source])))
    (testing "q is cascade-model-manifest/exact-update over token-likelihood restricted to :checked"
      (is (= (manifest/exact-update lik pushed #{:t}) (:q step)))
      (is (= {#{:u :t} 9/11 #{:u} 2/11} (:q step)))
      (is (contains? (manifest/token-likelihood rates #{:u :t} #{:t}) :status)
          "without the restriction the kernel refuses the unchecked state token :u"))
    (is (= 11/20 (:p-o step)))
    (is (= (- (Math/log (double 11/20))) (:f step)) "f = -ln P(o)")))

(deftest the-producer-writes-what-the-step-reads
  (let [ma (produced-measured-a {})]
    (is (= {[target :t] {:false-neg 1/10 :false-pos 1/5}} (:rates ma)))
    (is (= {[target :t] {:false-neg {:numerator 1 :denominator 10}
                         :false-pos {:numerator 1 :denominator 5}}}
           (update-vals (:measurement ma) #(update-vals % (fn [c] (select-keys c [:numerator :denominator]))))))
    (testing "bad case: the producer's record without :measurement (F1a-2b's record) leaves the step absent"
      (is (= {:status :absent :reason :no-measurement-provenance}
             (select-keys (:step (fly {:measured-a (dissoc ma :measurement)})) [:status :reason]))))))

(deftest unmeasured-a-refuses-the-step
  ;; :t's class (:C4) has no admitted labels while :w's (:C3) does: the
  ;; producer writes a present record whose :t measurement is :absent
  (let [step (:step (fly {:locators {:t {:class :C4} :w {:class :C3}} :labels (labels :C3 4 1 2 0)}))]
    (is (= :refused (:status step)))
    (is (= :unmeasured-class (:reason step)))
    (is (= {:t :C4} (:classes step)))
    (is (not (contains? step :q)) "not a step scored with identity A")))

(deftest a-zero-probability-observation-is-a-contradiction-not-a-number
  ;; theta 0: :t is never produced; zero false-pos: :t is never reported unless established
  (let [step (:step (fly {:theta 0 :labels (labels :C4 10 0 5 0)}))]
    (is (= :contradiction (:f step)))
    (is (= 0 (:p-o step)))
    (is (= :refused (get-in step [:q :status])) "the update refuses: no q")))

(deftest a-run-record-without-the-rates-value-is-an-absence
  (let [ma (dissoc (produced-measured-a {}) :rates)
        step (:step (fly {:measured-a ma}))]
    (is (= {:status :absent :reason :no-rates-value :inputs {:rates-sha (:rates-sha ma)}}
           (select-keys step [:status :reason :inputs])))
    (is (= {:policy-key key-a :occurrence {:flight "flight-f1bj" :click "run-1"}}
           (select-keys step [:policy-key :occurrence]))
        "an absence names its policy and occurrence, so admission can end that prefix at it"))
  (is (= {:status :absent :reason :no-run-record}
         (select-keys (:step (fly {} :fetch? false)) [:status :reason]))
      "no fetcher: the typed absence, not a step"))

(deftest a-later-step-takes-the-chain's-q
  (let [prior {:step {:status :present :policy-key key-a :q {#{:t} 1}}}
        step (flight/conditioning-step
              {:run-record (run-record {}) :target target :flight-id "f" :click-id "run-2"
               :observation {:status :observed :o #{:t} :checked #{:t} :channel {:t :C4}}
               :policy-key key-a :precedence [:p/a] :enactments [prior]})]
    (is (= {:value {#{:t} 1} :source :chain} (:s-prev step)))))
