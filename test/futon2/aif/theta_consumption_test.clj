(ns futon2.aif.theta-consumption-test
  "PROOF-wm-works ⟨1⟩8 second half: the scorer consumes the recorded theta.
  In-process, on the frozen reference input, through the real
  pattern-theta reader and the real class scorer."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.cascade-policy :as cpol]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.efe :as efe]
            [futon2.aif.focus-receipt :as focus]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.scoring-input-receipts :as ir]))

(def t "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")
(def joe-c {:focused 55/100 :related 35/100 :unrelated 5/100 :stop-the-line 5/100})

(defn- score-with-thetas
  "The joint decision's scoring shape over the frozen reference input, with
  each pattern's theta set by THETAS {pattern-id theta-map}: the real
  loaders, the real qualifier, the real class model, the real rank-actions."
  [thetas]
  (let [sources (cs/with-context-fn (cs/load-declared))
        T4 (or (:horizon-steps sources) 4)
        assembled (cp/assemble {:targets [t] :sources (assoc sources :horizon-steps T4)})
        problems (:problems assembled)
        pats (get-in sources [:interpretations t :patterns])
        tokpats (reduce (fn [acc pm] (reduce conj acc (concat (get-in pm [:guard :needs]) (get-in pm [:guard :forbids]) (:produces pm)))) #{} (vals pats))
        qual (fn [target token] [target token])
        pm2 (fn [id]
             (let [mm (get pats id) g (:guard mm)
                   ft (get thetas id)]
               (-> (cpol/token-interpretation id {:guard {:needs (set (map (partial qual t) (:needs g))) :forbids (set (map (partial qual t) (:forbids g)))} :produces (set (map (partial qual t) (:produces mm)))})
                   (assoc :target t)
                   (cond-> (= :recorded-trials (:status ft))
                     (assoc :theta (:theta ft) :theta-source :recorded-trials)))))
        cands (vec (mapcat (fn [p] (mapv (fn [c] {:kind :cascade-candidate :id (:candidate-id c) :target t :precedence (mapv pm2 (:precedence c))}) (:constructed-candidates p))) problems))
        q0 (:value (ir/initial-belief problems))
        inputs (focus/read-inputs)
        now "2099-01-01T00:00:00Z"
        est (focus/discover inputs "2026-09-22T17:31:44Z" nil)
        focus-info (focus/discover inputs now {:focus (:focus est) :as-of "2026-09-22T17:31:44Z"})
        cls (focus/classify-target inputs focus-info now t
                                    {:ticket-dir "holes/tickets"
                                     :findings-dir "data/wm-repair-obligations/findings"})
        acceptance (reduce (fn [acc p] (reduce conj acc (map (fn [w] [(:target p) w]) (get-in p [:cascade-problem :want])))) #{} problems)
        universe (reduce (fn [acc c] (reduce (fn [a pattern] (reduce conj a (concat (:produces pattern) (mapcat (fn [cl] (concat (:present cl) (:absent cl))) (get-in pattern [:guard :clauses]))))) acc (:precedence c))) (reduce clojure.set/union (clojure.set/union acceptance tokpats) (map (fn [k] (reduce clojure.set/union #{} k)) (keys q0))) cands)
        not-yet :ending/not-yet-evaluated
        model {:schema :wm/observation-model-v1 :backend :exact-enumeration :kind :class-emission
               :universe universe :horizon T4
               :class-universe [:focused :related :unrelated :stop-the-line not-yet]
               :acceptance acceptance
               :target-class {t (get {:focus :focused :associated :related :useful-elsewhere :unrelated} (:class cls) :unknown)}
               :class-preference (into {} (for [tau (range 1 (inc T4))] [tau (if (= tau T4) joe-c {not-yet 1})]))
               :provenance {:status :synthetic :calibrated false :source "test"}}
        ranked (efe/rank-actions {:cascade-belief q0} cands
                                 {:horizon-steps T4 :observation-model model
                                  :prediction-context {:occurrence-id "tc" :tau T4}
                                  :cascade-spec {:want acceptance :evidence #{} :zeroed #{}}})]
    {:ranked ranked :cands cands}))

(deftest empty-ledger-reproduces-todays-numbers
  ;; no recorded thetas: the documented defaults, today's exact numbers
  (let [{:keys [ranked]} (score-with-thetas {})]
    (is (vector? ranked))
    (let [by-id (into {} (map (juxt #(get-in % [:action :id]) identity)) ranked)]
      (is (< (Math/abs (- (:controller-score (by-id :C2)) (Math/log (/ 1 0.55)))) 0.001)
          "G(C2)=ln(1/.55) exactly")
      (is (< (Math/abs (- (:controller-score (by-id :C1)) (Math/log 20))) 0.001)
          "G(C1)=ln20 exactly"))))

(deftest one-recorded-success-changes-the-prediction-in-the-implied-direction
  ;; ONE recorded success for C2's head family, written through the
  ;; runner's own record! seam: theta 3/4 (the b-update rule's posterior).
  ;; With theta < 1 the kernel spreads mass between the produced state and
  ;; the current state, so the predicted outcome changes in the direction
  ;; the update implies (the terminal acceptance token now carries less
  ;; than full mass), and the decision records the theta as
  ;; :recorded-trials with its identities.
  (let [tmp (.toFile (java.nio.file.Files/createTempDirectory
                      "theta-consumption" (make-array java.nio.file.attribute.FileAttribute 0)))
        _ (spit (io/file tmp "attempts.edn") "")
        ;; the runner's own append seam with :after-observation (the honest field)
        ;; The row is written in the shape the PRODUCTION writer produces:
        ;; the parameter key is DERIVED from the trial's own recorded
        ;; precedence and effect (learning-trial-ledger/theta-key), never
        ;; named by the test. The earlier version of this test set
        ;; :learning-family to a pattern-id keyword -- a shape
        ;; attempt-learning/receipt cannot emit, since it digests the trial
        ;; configuration there -- so both sides of the identity were
        ;; authored here and the real write path was never exercised
        ;; (claude-2's review, 2026-09-23).
        _ (ledger/record! (.getPath tmp)
                          {:trials [{:status :admitted-at-attempt-grain
                                     :deduplication {:identity "tc-occ-1"}
                                     :learning-family "a-trial-configuration-digest"
                                     :effect [t :repair/split-declared-valid]
                                     :selected-cascade
                                     {:precedence [{:id :aif/measurement-window-hygiene
                                                    :produces #{[t :repair/held-out-observations-collected]}}
                                                   {:id :aif/declare-the-conditioning
                                                    :produces #{[t :repair/split-declared-valid]}}]}
                                     :after-observation true}]})
        ft (ledger/pattern-theta :aif/declare-the-conditioning (.getPath tmp))
        _ (is (= :recorded-trials (:status ft)) (pr-str ft))
        _ (is (= 3/4 (:theta ft)) "one success from cold: the update rule's posterior")
        _ (is (= ["tc-occ-1"] (:identities ft)))
        ;; the pattern-theta reader drives the scorer's pattern thetas
        {:keys [ranked]} (score-with-thetas {:aif/declare-the-conditioning ft})
        by-id (into {} (map (juxt #(get-in % [:action :id]) identity)) ranked)
        g-with (:controller-score (by-id :C2))
        ;; baseline for comparison
        {ranked0 :ranked} (score-with-thetas {})
        g-without (:controller-score (get-in (first ranked0) [:action :id]))
        g0 (if (= :C2 (get-in (first ranked0) [:action :id]))
             (:controller-score (first ranked0))
             (:controller-score (second ranked0)))]
    ;; the predicted outcome CHANGED in the direction the update implies:
    ;; with theta 3/4 the real kernel's 4-step rollout spreads mass, so the
    ;; terminal state is no longer the pure acceptance point mass — the
    ;; acceptance token's predicted mass is 3/4, not 1
    ;; direct: the real kernel with theta 3/4 vs theta 1 on the head pattern
    (let [pats (get-in (cs/with-context-fn (cs/load-declared)) [:interpretations t :patterns])
          ;; the REAL pattern from the loaded source, with only its theta
          ;; varied -- a hand-built pattern map lacks the interpretation
          ;; fields the manifest requires and the rollout refuses instead
          ;; of running (claude-5, finishing zai-1's job after it died).
          ;; the REAL pattern, interpreted and target-qualified exactly as
          ;; score-with-thetas does it; only its theta varies. A hand-built
          ;; pattern map lacks the interpretation fields the manifest
          ;; requires, and the rollout refuses instead of running
          ;; (claude-5, finishing zai-1's job after it died mid-edit).
          head-m (let [mm (get pats :aif/declare-the-conditioning)
                       g (:guard mm)
                       qual (fn [tok] [t tok])]
                   (-> (cpol/token-interpretation
                        :aif/declare-the-conditioning
                        {:guard {:needs (set (map qual (:needs g)))
                                 :forbids (set (map qual (:forbids g)))}
                         :produces (set (map qual (:produces mm)))})
                       (assoc :target t)))
          mk (fn [th] (assoc head-m :theta th))
          q0 {#{[t :admission/task-stated]} 1}
          r-1 (m/rollout (constantly [(mk 1)]) q0 1)
          r-34 (m/rollout (constantly [(mk 3/4)]) q0 1)
          accept-token (fn [belief]
                         ;; find the mass of the state CONTAINING the produced token
                         (some (fn [[st mass]] (when (contains? st [t :repair/split-declared-valid]) mass)) belief))]
      ;; exact rationals throughout the kernel: the mass is 1, not 1.0
      (is (= 1 (accept-token r-1)) "theta 1: the full mass reaches the produced state")
      (is (= 3/4 (accept-token r-34))
          "theta 3/4: the predicted outcome changes exactly as the update implies"))))

(deftest malformed-ledger-keeps-the-default-and-still-ranks
  ;; a malformed ledger: the reader returns the typed default; the scorer
  ;; still ranks with the documented defaults
  (let [tmp (.toFile (java.nio.file.Files/createTempDirectory
                      "theta-malformed" (make-array java.nio.file.attribute.FileAttribute 0)))
        _ (spit (io/file tmp "attempts.edn") "not-edn-at-all {{{")
        ft (ledger/pattern-theta :aif/declare-the-conditioning (.getPath tmp))
        _ (is (contains? #{:defaulted :no-recorded-trials :recorded-trials} (:status ft)))
        ;; the scorer shape: a defaulted theta maps to the documented default
        thetas (if (= :defaulted (:status ft))
                 {:aif/declare-the-conditioning {:status :no-recorded-trials}}
                 {})
        {:keys [ranked]} (score-with-thetas thetas)]
    (is (vector? ranked) "the decision still ranks")
    (let [by-id (into {} (map (juxt #(get-in % [:action :id]) identity)) ranked)]
      (is (< (Math/abs (- (:controller-score (by-id :C2)) (Math/log (/ 1 0.55)))) 0.001)
          "the documented-default numbers reproduce"))))

(deftest kernel-unchanged-for-explicit-theta
  ;; the kernel keeps taking theta off the pattern; an explicit theta is honoured
  (let [p {:id :x :produces #{:a} :guard {:status :interpreted :clauses []}}
        with-default (m/with-pattern-theta p)
        explicit (m/with-pattern-theta (assoc p :theta 1/2))]
    (is (= 1 (:theta with-default))
        "no explicit theta: the documented default, unchanged")
    (is (= :documented-default (:theta-source with-default)))
    (is (= 1/2 (:theta explicit)))
    (let [k (m/pattern-kernel explicit #{})]
      (is (= 1/2 (get k #{:a})) "the kernel spreads mass per the explicit theta")
      (is (= 1/2 (get k #{})) "and keeps the complementary mass"))))

(deftest banked-ledger-reads-the-recorded-theta
  ;; The assertion that would have caught the identity bug: read the REAL
  ;; banked ledger, no authored constants, no fixture. Before the fix this
  ;; returned :no-recorded-trials for every pattern, because the reader
  ;; filtered on the trial-configuration digest rather than on the pattern
  ;; that declared the effect (claude-2, 2026-09-23).
  (let [root "data/wm-learning-trials"
        holder (ledger/pattern-theta :contracts/holder-states-the-claim root)
        falsifier (ledger/pattern-theta :contracts/every-entry-has-a-falsifier root)
        untried (ledger/pattern-theta :aif/two-layer-calibration root)]
    (is (= :recorded-trials (:status holder)) (pr-str holder))
    (is (= 1/8 (:theta holder)) "three attempts, none accepted")
    (is (= 3 (:trials-count holder)))
    (is (= 0 (:successes holder)))
    ;; provenance a reviewer needs: the 1/8 is three attempts on ONE target
    (is (= 1 (count (:targets holder))) (pr-str (:targets holder)))
    (is (= 3/4 (:theta falsifier)) "one attempt, accepted")
    (is (= :no-recorded-trials (:status untried))
        "a pattern with no trials keeps the documented default")))
