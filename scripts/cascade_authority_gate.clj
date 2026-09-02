(ns cascade-authority-gate
  "CHANNEL 1 authority gate: does a CASCADE of ants patterns move the forager?
  futon3 worklist row :LA4 (futon3/holes/labs/library-contract/worklist.edn).

  WHAT THE 2026-07-16 GATE MEASURED, AND WHAT IT DID NOT.

  scripts/pattern_authority_gate.clj returned AUTHORITY FAIL -- no real pattern
  differed from `off` by held-out yield sign test at any lambda -- with sound
  controls (the sham tied `off` exactly on every seed).  holes/cascade-ants.edn:
  27-38 records the scope limit at source: that gate's `activate-pattern:52-68`
  sets `[:config :aif :efe :lambda :pattern]` and `:cyber-pattern` DIRECTLY and
  never calls `cyber/attach-config`, so the patterns' `@aif-delta` never reached
  `:aif-config`.  The patterns have two channels and only one was tested:

    channel 1  @aif-delta -> :aif-config, read by ants.aif.core/aif-config:67-71
               on every step.  The patterns' DECLARED PARAMETERS.  NEVER TESTED.
    channel 2  ants.aif.pattern-efe's hard-coded risk/info-gain, reached through
               a singular `:cyber-pattern :id` and gated by lambda.pattern.
               TESTED, FAILED.

  This gate measures channel 1, on a CASCADE rather than on one pattern.

  WHAT IS BORROWED AND WHAT IS NEW.  Borrowed unchanged from the 2026-07-16
  gate: the geometry screen, the paired held-out seeds, `run-cell-with` (the
  same loop, metrics and trace), `summarize` (the same exact two-sided binomial
  sign test), `exact-results?` and the preregistered verdict.  New: one
  activation function, `activate-cascade`, which attaches the whole cascade
  through `cyber/attach-cascade-config`.

  THE RESULT IS STATED IN ADVANCE, per LA1c-restatement.md §6: the honest
  expectation is a REFUSAL, and a refusal is a delivery.  Ants is in this
  programme because its gate has already refused everything once with sound
  controls; a constructor that only ever confirms is caught here.  A PASS
  reported without this test having run is the control discarded, not passed.

  Run:  cd futon2 && clojure -M scripts/cascade_authority_gate.clj [n-seeds] [ticks] [lambdas]"
  (:require [ants.aif.core :as aif]
            [ants.aif.experiment :as experiment]
            [ants.cyber :as cyber]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [pattern-authority-gate :as pattern-gate]))

(def cascade-artifact
  "Written by futon3/checks/construct_ants_cascade.clj.  The cascade is READ, not
  written here, and it carries no `@aif-delta` at all -- the deltas come from the
  same flexiarg files through `cyber/cyber-config`.  That is what makes the
  cascade reviewable BEFORE the run (LA1c-restatement.md §7): the reviewer reads
  an artefact, not a literal buried in this file."
  "../futon3/checks/ants-cascade.edn")

(def report-path "holes/cascade-authority-gate.edn")

(def sham-cascade
  "The control named by :LA4's acceptance: it must tie `off` EXACTLY on every
  seed at every lambda.  `ants/baseline-cyber-ant`'s @aif-delta is literally
  empty -- cascade-ants.edn:48 calls it \"the identity element of the pattern
  algebra\" -- so channel 1 receives {} and channel 2 receives `:cyber/baseline`,
  which is precisely the sham the 2026-07-16 gate used."
  {:id :sham
   :members [:ants/baseline-cyber-ant]
   :authored-order [:ants/baseline-cyber-ant]
   :precedence {:ants/baseline-cyber-ant 1}})

;; ---------------------------------------------------------------------------
;; the activation -- the ONE thing this gate does differently
;; ---------------------------------------------------------------------------

(defn activate-cascade
  "Put a cascade on the board.  Same shape as
  `pattern-authority-gate/activate-pattern:52-68`: set lambda.pattern on the
  world's config and give every initial ant the same pattern identity.  The
  difference is the one line that matters -- `cyber/attach-cascade-config`, which
  folds the members' @aif-delta maps in precedence order into `:aif-config`."
  [world cascade lambda-pattern]
  (-> world
      (assoc-in [:config :aif :efe :lambda :pattern] (double lambda-pattern))
      (update :ants
              (fn [ants]
                (into (empty ants)
                      (map (fn [[id ant]]
                             [id (cyber/attach-cascade-config ant cascade)]))
                      ants)))))

(defn swap-precedence
  "The O4 arm: exchange the precedence of two members and change NOTHING else.
  `:members` and `:authored-order` are untouched, so the two cascades differ in
  the precedence field alone -- which is what makes the comparison a test of law
  O4 (precedence changed => acting order or score changed) rather than a test of
  two different cascades."
  [cascade a b]
  (let [pa (get-in cascade [:precedence a])
        pb (get-in cascade [:precedence b])]
    (when (or (nil? pa) (nil? pb))
      (throw (ex-info "swap-precedence: a member has no precedence entry"
                      {:type :cascade-gate/bad-swap :a a :b b})))
    (assoc cascade
           :id (keyword (str (name (:id cascade)) "-precedence-swapped"))
           :precedence (assoc (:precedence cascade) a pb b pa))))

;; ---------------------------------------------------------------------------
;; the preconditions -- run BEFORE any arm, so an inert arm cannot produce a null
;; ---------------------------------------------------------------------------
;; README-xeno-loop.md §0 records what happens without these: the old cyberant
;; transfer experiment's `random-wiring` control permuted fields nothing read, so
;; treatment and control were operationally byte-identical and the "null" was a
;; TAUTOLOGY.  A gate on channel 1 has to show the channel is connected before it
;; is allowed to report that nothing moved through it.

(defn efe-of
  "The expected free energy the FIRST ant of a world computes on its first step.
  One number, off one ant, one tick: enough to say whether the knob reaches the
  computation, and deliberately not enough to say anything about the outcome --
  that is what the twenty paired seeds are for."
  [world]
  (let [[_ ant] (first (sort-by key (:ants world)))]
    (:G (aif/aif-step world ant))))

(defn efe-is-connected
  "THE ANTI-TAUTOLOGY PRECONDITION.  README-xeno-loop.md §0 records what a gate
  without one is worth: the old cyberant transfer experiment's `random-wiring`
  control permuted fields nothing read, so treatment and control were
  operationally byte-identical and the resulting 'null' was a TAUTOLOGY -- read
  as refuting the hypothesis when it refuted the apparatus.

  So before any arm runs, take one ant on one board and require that (a) the
  folded config changes the expected free energy at all, and (b) exchanging the
  precedence of the two contending members changes it too.  Neither says the
  cascade HELPS.  Both say a null afterwards is about the ant and not about a
  disconnected wire."
  [cascade swapped seed ticks]
  (let [world (experiment/make-seeded-world pattern-gate/species pattern-gate/food
                                            seed (+ 10000 seed) pattern-gate/size ticks)
        g-off (efe-of (pattern-gate/activate-pattern world nil 0.0))
        g-cascade (efe-of (activate-cascade world cascade 0.0))
        g-swapped (efe-of (activate-cascade world swapped 0.0))]
    {:seed seed
     :g-off g-off
     :g-cascade g-cascade
     :g-precedence-swapped g-swapped
     :folded-config-reaches-the-efe? (not= g-off g-cascade)
     :precedence-reaches-the-efe? (not= g-cascade g-swapped)}))

(defn preconditions
  [cascade swapped]
  (let [delta (cyber/cascade-delta cascade)
        swapped-delta (cyber/cascade-delta swapped)
        sham-delta (cyber/cascade-delta sham-cascade)
        contentions (cyber/cascade-contentions cascade)
        ordered (cyber/ordered-members cascade)]
    {:cascade-id (:id cascade)
     :ordered ordered
     :ordered-agrees-with-artifact? (= ordered (vec (:ordered cascade)))
     :folded-delta delta
     :folded-delta-is-non-empty? (seq delta)
     :every-member-contributes?
     (into (sorted-map)
           (for [p ordered] [p (boolean (seq (:aif-delta (cyber/cyber-config p))))]))
     :contentions contentions
     :swap-changes-the-folded-delta? (not= delta swapped-delta)
     ;; leaf paths, not top-level keys: two members can both write under `:efe`
     ;; and collide at one leaf, and a top-level comparison would call that a
     ;; whole-subtree change.
     :swap-changes-only-the-contended-paths?
     (= (set (map first (remove (fn [[path v]] (= v (get-in swapped-delta path)))
                                (cyber/delta-paths delta))))
        (set (map :path contentions)))
     :sham-delta-is-empty? (= {} sham-delta)
     :channel-2-sees (get-in (cyber/attach-cascade-config {} cascade) [:cyber-pattern :id])
     :channel-2-sees-sham (get-in (cyber/attach-cascade-config {} sham-cascade)
                                  [:cyber-pattern :id])}))

;; ---------------------------------------------------------------------------
;; the run
;; ---------------------------------------------------------------------------

(defn- print-row
  [{:keys [arm lambda delta-yield wins informative sign-p ties live-actuator?]}]
  (println (format "  %-26s %6.1f %+10.3f %3d/%-3d %9.5f %5d  %s"
                   arm lambda delta-yield wins informative sign-p ties
                   (if live-actuator? "DIFFERS — LIVE" "no detected difference"))))

(defn- parse-lambdas
  [raw]
  (if (str/blank? raw)
    pattern-gate/default-lambdas
    (mapv #(Double/parseDouble %) (str/split raw #","))))

(defn -main
  [& args]
  (let [n (Integer/parseInt (or (first args) "20"))
        ticks (Integer/parseInt (or (second args) "300"))
        lambdas (parse-lambdas (nth args 2 nil))
        artifact (io/file cascade-artifact)
        _ (when-not (.exists artifact)
            (throw (ex-info "cascade artefact missing; run futon3 checks/construct_ants_cascade.clj first"
                            {:type :cascade-gate/no-cascade :path cascade-artifact})))
        report (edn/read-string (slurp artifact))
        ;; :widen-to-the-marginal-gain-floor is the arm gated.  It is the cascade
        ;; the constructor STOPPED at, and it is the four real patterns: the
        ;; budget arm differs from it only by admitting the sham, whose delta is
        ;; {} and which therefore folds to the same :aif-config.  That the two
        ;; temperaments are indistinguishable through channel 1 is recorded below
        ;; rather than used to pick the convenient one.
        cascade (get-in report [:runs :widen-to-the-marginal-gain-floor :cascade])
        budget-cascade (get-in report [:runs :widen-to-a-budget :cascade])
        _ (when-not (seq (:members cascade))
            (throw (ex-info "cascade artefact has no members"
                            {:type :cascade-gate/empty-cascade})))
        contentions (cyber/cascade-contentions cascade)
        _ (when-not (seq contentions)
            (throw (ex-info "no member of this cascade contends a key with another; the O4 arm would be vacuous"
                            {:type :cascade-gate/no-contention})))
        [swap-a swap-b] (mapv first (:writers (first contentions)))
        swapped (swap-precedence cascade swap-a swap-b)

        screened (pattern-gate/screen-seeds (range 1 10000) n ticks)
        _ (when (< (count screened) n)
            (throw (ex-info "not enough geometry-screened boards"
                            {:type :cascade-gate/insufficient-boards
                             :wanted n :found (count screened)})))
        seeds (mapv first screened)
        pre (assoc (preconditions cascade swapped)
                   :efe-connection (efe-is-connected cascade swapped (first seeds) ticks))]

    (println "=== CASCADE ACTUATOR AUTHORITY GATE (CHANNEL 1) ===")
    (println "  PREREGISTERED: live actuator iff paired sign test differs from off at p<.05")
    (println "  PRESTATED (LA1c-restatement.md §6): the honest expectation is a REFUSAL.")
    (println (format "  %d held-out paired seeds x %d ticks; patchy %dx%d; nearest food <= %.1f"
                     n ticks (first pattern-gate/size) (second pattern-gate/size)
                     pattern-gate/nearest-max))
    (println "  held-out seeds (screened on geometry only, never selected against):" seeds)
    (println "  lambda.pattern sweep:" lambdas)
    (println (format "  cascade %s from %s" (:id cascade) cascade-artifact))
    (println (format "    ordered (most precedent first): %s" (pr-str (:ordered pre))))
    (println (format "    folded :aif-config delta: %s" (pr-str (:folded-delta pre))))
    (doseq [c contentions]
      (println (format "    CONTENTION %s written by %s -> %s wins, by precedence"
                       (pr-str (:path c)) (pr-str (:writers c)) (pr-str (:won c)))))
    (println (format "    O4 arm: precedence of %s and %s exchanged, nothing else"
                     swap-a swap-b))

    (println)
    (println "  PRECONDITIONS (an inert arm cannot be allowed to produce a null)")
    (doseq [k [:folded-delta-is-non-empty? :ordered-agrees-with-artifact?
               :swap-changes-the-folded-delta? :swap-changes-only-the-contended-paths?
               :sham-delta-is-empty?]]
      (println (format "    %-42s %s" (name k) (boolean (get pre k)))))
    (println (format "    %-42s %s" "every member contributes a non-empty delta"
                     (pr-str (:every-member-contributes? pre))))
    (println (format "    %-42s %s / sham %s" "channel 2 is shown"
                     (:channel-2-sees pre) (:channel-2-sees-sham pre)))
    (let [c (:efe-connection pre)]
      (println (format "    %-42s %s  (off %.6f / cascade %.6f / swapped %.6f, seed %d)"
                       "the knob reaches the computation"
                       [(:folded-config-reaches-the-efe? c) (:precedence-reaches-the-efe? c)]
                       (:g-off c) (:g-cascade c) (:g-precedence-swapped c) (:seed c))))
    (when-not (and (:folded-delta-is-non-empty? pre)
                   (:ordered-agrees-with-artifact? pre)
                   (:swap-changes-the-folded-delta? pre)
                   (:swap-changes-only-the-contended-paths? pre)
                   (:sham-delta-is-empty? pre)
                   (get-in pre [:efe-connection :folded-config-reaches-the-efe?])
                   (get-in pre [:efe-connection :precedence-reaches-the-efe?]))
      (throw (ex-info "cascade gate preconditions failed; no arm was run"
                      {:type :cascade-gate/inert-instrument :preconditions pre})))

    (let [off-results (pattern-gate/run-arm nil 0.0 seeds ticks)
          ;; The controls are completed before any treatment arm, exactly as the
          ;; 2026-07-16 gate does it.
          sham-controls (into {} (for [lambda lambdas]
                                   [lambda (pattern-gate/run-arm-with
                                            #(activate-cascade % sham-cascade lambda)
                                            seeds ticks)]))
          sham-zero (pattern-gate/run-arm-with
                     #(activate-cascade % sham-cascade 0.0) seeds ticks)
          sham-ok? (every? #(pattern-gate/exact-results? off-results (get sham-controls %))
                           lambdas)
          sham-zero-ok? (pattern-gate/exact-results? off-results sham-zero)]
      (println)
      (println "  CONTROL AUDIT")
      (doseq [lambda lambdas]
        (println (format "    sham cascade lambda=%-4.1f exact per seed: %s"
                         lambda (if (pattern-gate/exact-results?
                                     off-results (get sham-controls lambda))
                                  "YES" "NO"))))
      (println (format "    sham cascade lambda=0    exact per seed: %s"
                       (if sham-zero-ok? "YES" "NO")))
      ;; NOT a control here, and said so rather than quietly dropped.  The
      ;; 2026-07-16 gate's second control was "lambda=0 is exact for every
      ;; pattern id", and it is a CHANNEL 2 control: lambda.pattern gates channel
      ;; 2 alone.  Channel 1 acts at every lambda including zero, so requiring
      ;; that control of a non-empty cascade would require the channel under test
      ;; to be dead.  The lambda=0 cell of the real cascade is therefore run as a
      ;; MEASUREMENT -- channel 1 in isolation -- and not as a control.
      (println "    lambda=0 for the REAL cascade is a MEASUREMENT (channel 1 alone), not a control:")
      (println "      lambda.pattern gates channel 2 only, so requiring exactness there")
      (println "      would require the channel under test to be dead.")
      (when-not (and sham-ok? sham-zero-ok?)
        (throw (ex-info "cascade authority instrument failed its exact controls; real arms not run"
                        {:type :cascade-gate/broken-instrument
                         :sham-exact? sham-ok? :sham-zero-exact? sham-zero-ok?})))

      (let [all-lambdas (into [0.0] lambdas)
            real-results (into {} (for [lambda all-lambdas]
                                    [lambda (pattern-gate/run-arm-with
                                             #(activate-cascade % cascade lambda)
                                             seeds ticks)]))
            swapped-results (into {} (for [lambda all-lambdas]
                                       [lambda (pattern-gate/run-arm-with
                                                #(activate-cascade % swapped lambda)
                                                seeds ticks)]))
            sham-summaries (mapv (fn [lambda]
                                   (pattern-gate/summarize "sham-cascade" lambda
                                                           (get sham-controls lambda)
                                                           off-results))
                                 lambdas)
            real-summaries (mapv (fn [lambda]
                                   (pattern-gate/summarize (name (:id cascade)) lambda
                                                           (get real-results lambda)
                                                           off-results))
                                 all-lambdas)
            swapped-summaries (mapv (fn [lambda]
                                      (pattern-gate/summarize
                                       (name (:id swapped)) lambda
                                       (get swapped-results lambda) off-results))
                                    all-lambdas)
            summaries (vec (concat sham-summaries real-summaries swapped-summaries))
            movers (filter :live-actuator? real-summaries)
            ;; O4 as `find_organise.clj:523` states it: precedence changed =>
            ;; acting order or score changed.  The numbers are reported here; the
            ;; predicate is applied by futon3/checks/construct_ants_cascade.clj,
            ;; so the law has one spelling.
            o4 {:precedence-before (into (sorted-map) (:precedence cascade))
                :precedence-after (into (sorted-map) (:precedence swapped))
                :acting-order-before (cyber/ordered-members cascade)
                :acting-order-after (cyber/ordered-members swapped)
                :score-before (pattern-gate/mean (map :yield (get real-results 0.0)))
                :score-after (pattern-gate/mean (map :yield (get swapped-results 0.0)))
                :score-is "mean held-out yield at lambda.pattern 0 (channel 1 alone)"
                :per-lambda
                (into (sorted-map)
                      (for [lambda all-lambdas]
                        [lambda {:before (pattern-gate/mean (map :yield (get real-results lambda)))
                                 :after (pattern-gate/mean (map :yield (get swapped-results lambda)))
                                 :traces-differ?
                                 (not (pattern-gate/exact-results?
                                       (mapv :behavior-trace (get real-results lambda))
                                       (mapv :behavior-trace (get swapped-results lambda))))}]))}
            result {:gate :cascade-authority
                    :channel 1
                    :preregistered-verdict {:test :paired-sign :alpha 0.05
                                            :claim :differs-from-off
                                            :prestated-expectation :refusal}
                    :cascade-artifact cascade-artifact
                    :cascade (select-keys cascade [:id :members :precedence
                                                   :authored-order :ordered :stop])
                    :budget-cascade-folds-to-the-same-config?
                    (= (cyber/cascade-delta cascade) (cyber/cascade-delta budget-cascade))
                    :preconditions pre
                    :screen {:kind :geometry
                             :nearest-food-max pattern-gate/nearest-max
                             :boards screened :held-out? true}
                    :seeds seeds
                    :ticks ticks
                    :lambdas lambdas
                    :off-yields (mapv :yield off-results)
                    :controls {:sham-exact-per-seed? sham-ok?
                               :sham-zero-lambda-exact-per-seed? sham-zero-ok?
                               :zero-lambda-for-the-real-cascade :measurement-not-a-control}
                    :cells summaries
                    :live-actuators (mapv (juxt :arm :lambda) movers)
                    :authority-pass? (boolean (seq movers))
                    :o4 o4}]
        (println)
        (println (format "  %-26s %6s %10s %7s %9s %5s  %s"
                         "arm" "lambda" "delta" "wins/n" "sign-p" "ties" "verdict"))
        (doseq [row summaries] (print-row row))
        (println)
        (println "  BEHAVIOR AUDIT (cells whose action trace differs from off):")
        (let [behavior-cells (filter #(pos? (:behavior-seeds %))
                                     (concat real-summaries swapped-summaries))]
          (if (seq behavior-cells)
            (doseq [{:keys [arm lambda behavior-seeds behavior-delta]} behavior-cells]
              (println (format "    %-26s lambda %.1f traces %d/%d; mean action deltas %s"
                               arm lambda behavior-seeds n (pr-str behavior-delta))))
            (println "    none — every cascade action trace equals off")))
        (println)
        (println "  O4 (precedence changed => acting order or score changed), numbers only;")
        (println "  the predicate is applied by futon3 checks/construct_ants_cascade.clj:")
        (doseq [[lambda row] (:per-lambda o4)]
          (println (format "    lambda %.1f  before %.4f  after %.4f  traces differ %s"
                           lambda (:before row) (:after row) (:traces-differ? row))))
        (println)
        (println (if (seq movers)
                   "  CASCADE AUTHORITY PASS — the constructed cascade is a live actuator on channel 1. Authority is not merit."
                   "  CASCADE AUTHORITY FAIL — the constructed cascade does not differ from off. This was the prestated expectation; it is a delivery, not a defeat."))
        (spit report-path (pr-str result))
        (spit "/tmp/cascade-authority-gate.edn" (pr-str result))
        (println (format "  wrote %s" report-path))
        (println "cascade-authority-gate: exit-convention=0-ran/1-instrument-failed")
        (shutdown-agents)
        (System/exit 0)))))
