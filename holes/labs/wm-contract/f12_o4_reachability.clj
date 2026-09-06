(ns f12-o4-reachability
  "`:F12` slice 3. MEASURES whether O4 is reachable over the 1239-pattern library,
   and reports the six numbers an O4 row would carry if it were.

   THE SLICE THIS WAS ASKED TO BE, and why it is this instead.  `C539` §3 names
   slice 3 as \"a `futon3` construction whose members carry at least two
   play-grain rules, so the precedence change has something to reorder\".  That
   precondition is measured here and it is NOT SUFFICIENT.  O4's acting order is
   read over the gate's PRIMARY rounds -- a round carrying both a transcript and a
   recorded v0 decision whose oracle label the record determines
   (`zaif_cascade_gate.clj:435`, repairs 2 and 3 of that gate's second pre-run
   review).  Two members carrying rules move the acting order only if their rules
   CONTEND on a round inside that denominator.  On the one cohort the library
   scale has, they do not: the two selected rules are both live on rounds 3, 4 and
   7 and on no others, and none of those three rounds is primary.  So a
   construction built to the stated precondition would record `:exercised? true`
   and `:holds? false`, and the failure would be the denominator's and not the
   cascade's.  Building it first and finding that out afterwards is the checkpoint
   this file is instead of.

   AND A SECOND BLOCKER, found while measuring the first: the gate that would have
   to record an O4 row DOES NOT PASS at HEAD.  `require-pass!` aborts with
   `:rule-does-not-encode-an-authored-then` for three of the four rules, because
   the library patterns their `:then-source` spans cite were rewritten on
   2026-09-05 (futon3 `2a91028`, `5704359`) after the table was committed on
   2026-09-02 (futon3c `31260dd4`), and the spans no longer fall inside the
   `+ THEN:` blocks they name.  Recorded, not repaired.

   WHAT IS MEASURED, all of it recomputed and none read off a record:

   1. `:carriage` -- for every run of every recorded cascade that carries runs,
      the members that carry a non-counterfactual rule of the pinned table.  The
      maximum over all of them is ONE, and the two library-scale records carry a
      DIFFERENT one each: `construct-cascade.edn`'s budget arm admits
      `war-machine/ambient-pattern-retrieval`, `zaif-cascade.edn`'s two arms
      select `math-strategy/missing-dependency-protocol`, and no recorded cascade
      carries both.
   2. `:contention` -- per rule, the rounds of the a97J05 cohort on which its
      antecedent holds AND its THEN returns a value; the rounds on which both do;
      and which of those rounds are in each of the three denominators.
   3. `:probe` -- `fo/o4-precedence-governance` applied to the six numbers the
      precedence exchange produces, under BOTH denominators.  This is NOT A
      WITNESS and is labelled so in the output: no constructor selected both
      patterns, so there is no cascade for the row to be of.  It is the
      counterfactual the decision in §4 of the note turns on, and it is computed
      by the gate's own `play` and `fo`'s own predicate rather than by a second
      spelling of either.

   WHAT THIS IS NOT.  No `futon3` file is changed and no `futon3c` file is
   changed; the rule table is read where it lives and the cascade records are read
   where they lie.  No Lean is written -- slice 3 is not a Lean slice.  No
   `:choices` entry and no `:decisions` entry: which denominator O4's acting order
   is read over is a question, not a ruling this file may take.

   Determinism: every collection is sorted before it is written, no wall clock and
   no timestamp; the two repository HEADs are recorded as content, so two runs
   over unchanged trees are byte-identical.

   Run from the `futon3c` checkout, whose classpath the rule table needs:

     clojure -Sdeps '{:paths [\"src\" \"resources\" \"library\" \"scripts\" \".\"
                              \"../futon3/checks\" \"../futon3\"
                              \"../futon2/holes/labs/wm-contract\"]}' \\
       -M -m f12-o4-reachability

   Env overrides, so a NEGATIVE CONTROL can point it at a planted copy and see the
   verdict move: F12_COHORT, F12_CHECKS, F12_OUT.  The rule table is planted by
   putting a copy of `scripts/` ahead of `futon3c`'s on the classpath, which needs
   no override here."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.pprint :as pprint]
            [clojure.set :as set]
            [clojure.string :as str]
            [find-organise :as fo]
            [zaif-cascade-gate :as g]))

(def home (System/getProperty "user.home"))

(def cohort-path
  (or (System/getenv "F12_COHORT") (str home "/code/futon3c/" g/cohort-path)))

(def checks-dir
  (or (System/getenv "F12_CHECKS") (str home "/code/futon3/checks")))

(def out-path
  (or (System/getenv "F12_OUT")
      (str home "/code/futon2/holes/labs/wm-contract/runs/F12-organise/02-o4-reachability.edn")))

(def record-files
  "The nine recorded cascades slice 1 surveyed (`C539` §1, F12-c), named here so a
   record added later is a visible edit rather than a silent change of scope."
  ["construct-cascade.edn" "zaif-cascade.edn" "ants-cascade.edn"
   "alfworld-cascade.edn" "snatch-cascade.edn" "open-cascade.edn"
   "open-cascade-short-cue.edn" "retrodiction-cascade.edn"
   "retrodiction-cascade-per-clause.edn"])

(defn gate-status
  "THE SECOND BLOCKER, and it is not this file's.  `zaif_cascade_gate.clj`'s
   `then-correspondence` re-reads each rule's cited `+ THEN:` span from the
   library on every run, and `require-pass!` (`:579-582`) treats a span that does
   not lie strictly inside the THEN block as a hard failure.  Three of the four
   spans no longer do: the table was committed 2026-09-02 (futon3c 31260dd4) and
   the patterns it cites were rewritten 2026-09-05 by the L8 and L10 rationale
   backfills (futon3 2a91028, 5704359), which moved the blocks.  So the gate that
   would have to record an O4 row does not pass at HEAD, whatever cascade it is
   given.  Recorded, not repaired: moving a citation is an edit to a pre-run
   rule table, which is the one thing that table may not have done to it here.

   The gate's OWN contention count is recorded beside this file's, because the two
   answer different questions and the difference is the point: `coverage`'s
   contention is filtered to rules the cascade CONTAINS (`:646-647`), so it is
   empty today and would stay empty however the rounds fell."
  []
  (let [tc (g/then-correspondence)
        failures (vec (for [t tc
                            :when (not (and (:file-exists? t) (:pattern-id-matches-path? t)
                                            (:span-inside-the-then-block? t) (:span-is-non-empty? t)))]
                        (sorted-map :rule (:rule t)
                                    :then-source (:then-source t)
                                    :then-block (:then-block t)
                                    :span-inside-the-then-block? (:span-inside-the-then-block? t))))
        own (try (get-in (g/coverage cohort-path)
                         [:contention :rounds-where-more-than-one-cascade-rule-fires])
                 (catch Exception e {:unreadable (.getMessage e)}))
        abort (try (g/require-pass! (g/report cohort-path)) :passed
                   (catch clojure.lang.ExceptionInfo e (:finding (ex-data e)))
                   (catch Exception e {:threw (.getMessage e)}))]
    (sorted-map
     :then-correspondence-failures failures
     :then-correspondence-failure-count (count failures)
     :require-pass-verdict abort
     :gate-own-contention-over-cascade-members own
     :note "the gate's coverage filters contention to rules the cascade contains, so it is empty while only one member carries a rule; this file's :contention drops that filter deliberately, because the question is what a cascade carrying both WOULD do")))

(defn- head-of [repo]
  (let [{:keys [exit out]} (shell/sh "bash" "-c" (format "git -C %s/code/%s rev-parse --short HEAD" home repo))]
    (if (zero? exit) (str/trim out) :not-a-git-checkout)))

;; ---------------------------------------------------------------------------
;; 1. rule carriage: which recorded cascade members carry a play-grain rule
;; ---------------------------------------------------------------------------

(def selected-rules
  "The non-counterfactual rules of the pinned table.  The two counterfactual ones
   are excluded for the same reason `zaif_cascade_gate.clj:412` excludes them:
   their patterns are not in any cascade, so they cannot be a member's rule."
  (vec (remove :counterfactual? g/rule-table)))

(def rule-ids (into (sorted-set) (map :id) selected-rules))

(defn members-of-run
  "The members of one recorded run.  `zaif-cascade.edn`, `ants-cascade.edn` and
   `alfworld-cascade.edn` carry the finished CascadeState under `:cascade`;
   `construct-cascade.edn` does not, and for it the members are recomputed as
   O1's own union -- `find`'s selected plus the run's admitted -- and the count
   is cross-checked against the `:members` count the run records, so a record
   whose two accounts disagree is a failure and not a silent preference for one."
  [record run]
  (if-let [ms (get-in run [:cascade :members])]
    {:members (into (sorted-set) ms) :basis :cascade-members}
    {:members (into (sorted-set)
                    (set/union (set (get-in record [:find :selected]))
                               (set (:admitted run))))
     :basis :selected-union-admitted}))

(defn carriage []
  (into (sorted-map)
        (for [f record-files
              :let [file (io/file checks-dir f)]]
          [f (if-not (.exists file)
               (sorted-map :read? false :reason :file-not-found)
               (let [text (slurp file)
                     record (edn/read-string text)
                     runs (:runs record)]
                 (if-not (map? runs)
                   (sorted-map :read? true :runs :none
                               :o4-recorded (:o4 record)
                               :rule-ids-occurring-anywhere-in-the-file
                               (vec (sort (filter #(str/includes? text (subs (str %) 1)) rule-ids))))
                   (sorted-map
                    :read? true
                    :o4-recorded (:o4 record)
                    :runs
                    (into (sorted-map)
                          (for [[id run] runs
                                :let [{:keys [members basis]} (members-of-run record run)
                                      recorded (:members run)
                                      carrying (into (sorted-set) (set/intersection members (set rule-ids)))]]
                            [id (sorted-map
                                 :members (count members)
                                 :members-basis basis
                                 :members-count-recorded (if (number? recorded) recorded (count recorded))
                                 :members-count-agrees?
                                 (= (count members) (if (number? recorded) recorded (count recorded)))
                                 :rule-carrying-members (vec carrying)
                                 :rule-carrying-count (count carrying)
                                 :o4-recorded (:o4 run))]))))))])))

;; ---------------------------------------------------------------------------
;; 2. contention over the cohort
;; ---------------------------------------------------------------------------

(defn live?
  "A rule is LIVE on a situation when its antecedent holds AND its THEN returns a
   value.  `fo/fire` (find_organise.clj:406) takes the first rule that is live in
   precedence order, so liveness -- not the antecedent alone -- is what a
   precedence exchange can reorder."
  [rule s]
  (and (fo/fires? rule s) (some? ((:then rule) s))))

(defn primary?
  "The denominator every headline number of the gate is over, and the one its O4
   arm reads the acting order across (`zaif_cascade_gate.clj:435`, `:559-562`)."
  [s]
  (and (g/paired? s) (not (g/oracle-uncertain? s))))

(defn contention [sits]
  (let [live-rounds (fn [r] (vec (map :round (filter #(live? r %) sits))))
        both (filter (fn [s] (= (count selected-rules) (count (filter #(live? % s) selected-rules)))) sits)]
    (sorted-map
     :rounds (count sits)
     :denominators (sorted-map
                    :with-a-transcript (count (filter :has-transcript? sits))
                    :with-a-v0-decision (count (filter :has-v0? sits))
                    :paired (count (filter g/paired? sits))
                    :primary (count (filter primary? sits)))
     :primary-rounds (vec (map :round (filter primary? sits)))
     :live-rounds (into (sorted-map) (for [r selected-rules] [(:id r) (live-rounds r)]))
     :live-round-counts (into (sorted-map) (for [r selected-rules] [(:id r) (count (live-rounds r))]))
     :contending-rounds (vec (map :round both))
     :contending-rounds-in-primary (vec (map :round (filter primary? both)))
     :contending-rounds-that-are-paired (vec (map :round (filter g/paired? both)))
     :contending-round-detail
     (vec (for [s both]
            (sorted-map :round (:round s)
                        :has-transcript? (:has-transcript? s)
                        :has-v0? (:has-v0? s)
                        :paired? (g/paired? s)
                        :oracle-uncertain? (g/oracle-uncertain? s)
                        :primary? (primary? s)
                        :calls (:calls s)
                        :thens (into (sorted-map) (for [r selected-rules] [(:id r) ((:then r) s)])))))
     :the-two-rules-emit-different-arms-on
     (vec (for [s both
                :when (apply not= (map (fn [r] ((:then r) s)) selected-rules))]
            (:round s))))))

;; ---------------------------------------------------------------------------
;; 3. the probe: what the precedence exchange does, under each denominator
;; ---------------------------------------------------------------------------

(def acting-order
  "`zaif_cascade_gate.clj:451`, which spells it as a local `fn` and so cannot be
   called from here.  The one line is re-spelled rather than the arm; every other
   term of the six comes from the gate or from `fo`."
  (fn [plays] (vec (keep :fired plays))))

(defn- agree-count
  "`zaif-cascade-gate/agreement`'s `:agree`, through the private var rather than a
   second spelling of the comparison."
  [plays sits keep?]
  (:agree (#'g/agreement plays sits keep?)))

(defn probe-arm
  "The six numbers of an O4 row, over the rounds `keep?` admits, for the exchange
   of the two rules' precedence.  The rules are ordered 1,2 and then 2,1: that IS
   the exchange `zaif_cascade_gate.clj:444-450` performs, and with exactly two
   rules it needs no cascade to be well defined."
  [sits keep? label]
  (let [[a b] (map :id selected-rules)
        before {a 1 b 2}
        after {a 2 b 1}
        plays-before (g/play selected-rules before sits)
        plays-after (g/play selected-rules after sits)
        kept (fn [ps] (keep-indexed (fn [i p] (when (keep? (nth sits i)) p)) ps))
        row {:precedence-before (mapv before [a b])
             :precedence-after (mapv after [a b])
             :acting-order-before (acting-order (kept plays-before))
             :acting-order-after (acting-order (kept plays-after))
             :score-before (agree-count plays-before sits keep?)
             :score-after (agree-count plays-after sits keep?)}]
    (sorted-map
     :denominator label
     :rounds (count (filter keep? sits))
     :row (into (sorted-map) row)
     :precedence-changed? (not= (:precedence-before row) (:precedence-after row))
     :acting-order-changed? (not= (:acting-order-before row) (:acting-order-after row))
     :score-changed? (not= (:score-before row) (:score-after row))
     :rounds-on-which-the-fired-rule-differs
     (vec (for [[pb pa s] (map vector plays-before plays-after sits)
                :when (and (keep? s) (not= (:fired pb) (:fired pa)))]
            (:round s)))
     :deferred-rounds
     (vec (for [[pb s] (map vector plays-before sits) :when (and (keep? s) (:deferred? pb))]
            (:round s)))
     :o4-holds? (fo/o4-precedence-governance row))))

(defn probe [sits]
  (sorted-map
   :what "fo/o4-precedence-governance over the six numbers the precedence exchange of the two selected rules produces"
   :not-a-witness-because
   "no recorded cascade carries both rule-bearing patterns (see :carriage), so there is no constructed cascade for this row to be OF; these are the numbers a construction that carried both would record, not a law witnessed on one"
   :rules (mapv :id selected-rules)
   :arms (vec (for [[label keep?] [[:primary primary?]
                                   [:paired g/paired?]
                                   [:with-a-transcript :has-transcript?]]]
                (probe-arm sits keep? label)))))

;; ---------------------------------------------------------------------------
;; 4. the controls this file can run on itself
;; ---------------------------------------------------------------------------

(defn positive-control-identity-exchange
  "O4's first disjunct, exercised deliberately: with the precedence NOT exchanged,
   `precedence-before = precedence-after` and the predicate must hold whatever the
   acting order does.  Without this the probe's `:o4-holds? false` under the
   primary denominator could be a broken predicate rather than an unmoved acting
   order."
  [sits]
  (let [[a b] (map :id selected-rules)
        p {a 1 b 2}
        plays (g/play selected-rules p sits)
        kept (keep-indexed (fn [i x] (when (primary? (nth sits i)) x)) plays)
        row {:precedence-before (mapv p [a b])
             :precedence-after (mapv p [a b])
             :acting-order-before (acting-order kept)
             :acting-order-after (acting-order kept)
             :score-before (agree-count plays sits primary?)
             :score-after (agree-count plays sits primary?)}]
    (sorted-map :o4-holds? (fo/o4-precedence-governance row)
                :by-which-disjunct :precedence-unchanged
                :expected true)))

(defn control-problems
  "Failures, not findings.  Each is a condition under which nothing downstream is
   interpretable."
  [result]
  (concat
   (for [[f v] (:carriage result)
         :when (map? (:runs v))
         [rid r] (:runs v)
         :when (not (:members-count-agrees? r))]
     {:finding :recorded-member-count-disagrees-with-the-recomputed-union :record f :run rid})
   (when-not (:o4-holds? (:positive-control-identity-exchange result))
     [{:finding :o4-fails-under-an-unchanged-precedence
       :note "the predicate's first disjunct does not hold; the probe's verdicts are not readable"}])
   (for [r selected-rules
         :when (empty? (get-in result [:contention :live-rounds (:id r)]))]
     {:finding :a-selected-rule-is-live-on-no-round :rule (:id r)
      :note "with a dead rule the exchange is vacuous and the probe measures nothing"})))

;; ---------------------------------------------------------------------------

(defn report []
  (let [cohort (edn/read-string (slurp cohort-path))
        sits (g/situations cohort)
        c (carriage)
        result (sorted-map
                :as-of (sorted-map
                        :cohort cohort-path
                        :cohort-turn (get-in cohort [:as-of :turn-id])
                        :cohort-agent (get-in cohort [:as-of :agent])
                        :checks-dir checks-dir
                        :futon3-head (head-of "futon3")
                        :futon3c-head (head-of "futon3c")
                        :rule-table "futon3c/scripts/zaif_cascade_gate.clj:205 -- read through the namespace, not copied")
                :rule-table (sorted-map
                             :selected (vec rule-ids)
                             :counterfactual (vec (sort (map :id (filter :counterfactual? g/rule-table))))
                             :then-correspondence (g/then-correspondence))
                :carriage c
                :max-rule-carrying-members-on-any-recorded-run
                (reduce max 0 (for [[_ v] c :when (map? (:runs v)) [_ r] (:runs v)]
                                (:rule-carrying-count r)))
                :gate-status (gate-status)
                :contention (contention sits)
                :probe (probe sits)
                :positive-control-identity-exchange (positive-control-identity-exchange sits))]
    (assoc result :problems (vec (control-problems result)))))

(defn -main [& _]
  (let [result (report)]
    (io/make-parents out-path)
    (spit out-path (with-out-str (pprint/pprint result)))
    (println (format "f12 O4 reachability: max rule-carrying members on any recorded run = %d"
                     (:max-rule-carrying-members-on-any-recorded-run result)))
    (doseq [[f v] (:carriage result) :when (map? (:runs v)) [rid r] (:runs v)]
      (println (format "  %s %s: %d members, %d carry a rule %s"
                       f rid (:members r) (:rule-carrying-count r)
                       (pr-str (:rule-carrying-members r)))))
    (println (format "  gate at HEAD: then-correspondence failures %d %s; require-pass! verdict %s; gate's own contention %s"
                     (get-in result [:gate-status :then-correspondence-failure-count])
                     (pr-str (mapv :rule (get-in result [:gate-status :then-correspondence-failures])))
                     (pr-str (get-in result [:gate-status :require-pass-verdict]))
                     (pr-str (get-in result [:gate-status :gate-own-contention-over-cascade-members]))))
    (println (format "  contention: rounds %s; live %s; both live on %s; of those, primary %s"
                     (pr-str (get-in result [:contention :denominators]))
                     (pr-str (get-in result [:contention :live-round-counts]))
                     (pr-str (get-in result [:contention :contending-rounds]))
                     (pr-str (get-in result [:contention :contending-rounds-in-primary]))))
    (doseq [arm (get-in result [:probe :arms])]
      (println (format "  probe over %s (%d rounds): precedence %s -> %s, acting order changed? %s, score %s -> %s, O4 %s"
                       (name (:denominator arm)) (:rounds arm)
                       (pr-str (get-in arm [:row :precedence-before]))
                       (pr-str (get-in arm [:row :precedence-after]))
                       (:acting-order-changed? arm)
                       (get-in arm [:row :score-before]) (get-in arm [:row :score-after])
                       (:o4-holds? arm))))
    (println "  wrote" out-path)
    (if (seq (:problems result))
      (do (println "f12 O4 reachability: FAIL")
          (doseq [p (:problems result)] (println "   " (pr-str p)))
          (shutdown-agents)
          (System/exit 1))
      (do (println "f12 O4 reachability: PASS exit-convention=0-pass/1-fail")
          (shutdown-agents)
          (System/exit 0)))))
