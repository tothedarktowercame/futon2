#!/usr/bin/env bb
;; f9_cascade_target_check.bb -- :F9's acceptance test, as a program.
;;
;;   bb f9_cascade_target_check.bb <trace-file-or-dir> ...
;;   bb f9_cascade_target_check.bb --rank1 <trace-file-or-dir> ...
;;   bb f9_cascade_target_check.bb --edn   <trace-file-or-dir> ...
;;
;; THE QUESTION, and it is C475 6.4's, not one invented here: does the cascade
;; the tick CONSTRUCTED belong to the target the tick COMMITTED TO? The record
;; carries both -- `:decision :action :target` (war_machine.clj, `wm-decision`,
;; persisted at `:decision`) and `:cascade-policies` (the lane's own output,
;; persisted beside it) -- so the check is over committed artifacts and needs
;; no live machine.
;;
;; WHY COUNTING IS NOT THE CHECK. Two ways to be green without having wired
;; anything, and both are refused below:
;;   * a `:cascade-policies` entry for the decision target that carries no
;;     constructed cascade (no `:shown`, no `:cascade-score`) -- a target with
;;     a placeholder attached is not a cascade FOR it;
;;   * a tick that constructed NO cascade, which passes any "no entry disagrees
;;     with the decision" formulation vacuously. It is
;;     `:error/no-cascade-constructed` here, and it is the state of all 48
;;     recorded S1b/S2/S4/S5 ticks, which is what "red 24/24 BY CONSTRUCTION"
;;     in the :F9 row names. Note `:target-absent` is a DIFFERENT verdict: it
;;     is a tick that built a cascade for some other target.
;;
;; `--rank1` runs the SAME check against the head of `:ranked-actions` instead
;; of `:decision`. That is the target the pre-:F9 lane built for
;; (`cascade_lane.clj`, `decision-entry`, 1-arity), so the two modes together
;; measure the gap the wiring closes rather than asserting it.
;;
;; Exit 0 when every record checked is `:ok` and at least one record was
;; checked; 1 otherwise. An empty input is a failure, not a pass.

(require '[clojure.java.io :as io]
         '[clojure.edn :as edn]
         '[clojure.string :as str]
         '[clojure.pprint :as pp])

(def ^:private tagged (fn [_tag v] v))

(defn- read-records
  "Every trace record in one file. `:default` keeps a tagged literal readable
   rather than throwing -- the records carry #inst timestamps.

   TWO SHAPES, because the two places a record lives write it differently: a
   `wm-trace-*.edn` is a stream of bare records, and a stepper `delta.edn`
   (`wm_step.sh`) wraps the tick's appended records in one map under
   `:delta/records`. A wrapper is unwrapped; anything else is taken as-is."
  [f]
  (let [forms (with-open [r (java.io.PushbackReader. (io/reader f))]
                (doall (take-while #(not= ::eof %)
                                   (repeatedly #(edn/read {:eof ::eof :default tagged} r)))))]
    (mapcat (fn [form]
              (if (and (map? form) (contains? form :delta/records))
                (:delta/records form)
                [form]))
            forms)))

(defn- trace-files [path]
  (let [f (io/file path)]
    (cond
      (not (.exists f)) []
      (.isDirectory f) (->> (file-seq f)
                            (filter #(and (.isFile ^java.io.File %)
                                          (re-find #"^wm-trace-.*\.edn$" (.getName ^java.io.File %))))
                            (sort-by #(.getPath ^java.io.File %))
                            vec)
      :else [f])))

(defn- constructed?
  "A cascade entry is CONSTRUCTED when the lane actually built one for it: a
   non-empty `:shown` pattern list or a numeric `:cascade-score`. An entry with
   neither is a target the lane named and did not build for."
  [entry]
  (boolean (or (seq (:shown entry)) (number? (:cascade-score entry)))))

(defn cascade-entries
  "The tick's constructed cascades, normalised, from WHICHEVER carrier the
   artifact has.

   TWO CARRIERS, and the difference is not cosmetic. The judge's own output map
   carries `:cascade-policies` (war_machine.clj), but
   `futon2.aif.trace/trace-record` is an explicit projection and does NOT
   include that key (`trace.clj`:513-580) -- established by reading the record
   the wired run wrote, not assumed. What a TRACE record carries is the
   `:apply-cascade` rows the lane's output was lifted into, inside
   `:ranked-actions`. So a checker that reads only `:cascade-policies` reports
   `:no-cascade-policies` on a tick that constructed a cascade, which is what
   this one did before it was pointed at a real record.

   Both are read; each entry says which carrier it came from."
  [rec]
  (vec (concat
        (for [p (:cascade-policies rec)]
          {:mission (:mission p) :shown (:shown p)
           :cascade-score (:cascade-score p) :carrier :cascade-policies})
        (for [e (:ranked-actions rec)
              :when (= :apply-cascade (get-in e [:action :type]))]
          {:mission (get-in e [:action :target])
           :shown (get-in e [:action :cascade :shown])
           :cascade-score (get-in e [:action :act-gate :cascade-score])
           :rank (:rank e)
           :carrier :ranked-actions}))))

(defn check-record
  "The verdict for one trace record. `mode` is :decision (the :F9 wiring) or
   :rank1 (what the pre-:F9 lane built for)."
  [mode rec]
  (let [target (if (= :rank1 mode)
                 (get-in rec [:ranked-actions 0 :action :target])
                 (get-in rec [:decision :action :target]))
        policies (cascade-entries rec)
        missions (mapv :mission policies)
        idx (first (keep-indexed (fn [i m] (when (= m target) i)) missions))
        base {:run-id (get rec :run/id)
              :timestamp (str (:timestamp rec))
              :mode mode
              :target target
              :decision-target (get-in rec [:decision :action :target])
              :rank1-target (get-in rec [:ranked-actions 0 :action :target])
              :cascade-missions missions}]
    (cond
      (nil? target) (assoc base :verdict :error/no-target)
      (empty? policies) (assoc base :verdict :error/no-cascade-constructed)
      (nil? idx) (assoc base :verdict :error/target-absent)
      (not (constructed? (nth policies idx)))
      (assoc base :verdict :error/target-cascade-not-constructed :position idx)
      :else (assoc base :verdict :ok :position idx
                   :carrier (:carrier (nth policies idx))
                   :cascade-score (:cascade-score (nth policies idx))
                   :shown-count (count (:shown (nth policies idx)))))))

(defn -main [& args]
  (let [mode (if (some #{"--rank1"} args) :rank1 :decision)
        edn? (boolean (some #{"--edn"} args))
        paths (remove #(str/starts-with? % "--") args)
        files (mapcat trace-files paths)
        records (mapcat read-records files)
        results (mapv (partial check-record mode) records)
        freqs (frequencies (map :verdict results))
        ok (count (filter #(= :ok (:verdict %)) results))
        pass? (and (pos? (count results)) (= ok (count results)))
        report {:check :f9/cascade-target-equality
                :mode mode
                :paths (vec paths)
                :files (mapv #(.getPath ^java.io.File %) files)
                :record-count (count records)
                :ok-count ok
                :verdicts freqs
                :rank1-equals-decision
                (count (filter #(and (:rank1-target %)
                                     (= (:rank1-target %) (:decision-target %)))
                               results))
                :verdict (if pass? :pass :fail)
                :results results}]
    (if edn?
      (pp/pprint report)
      (do (println (format "f9_cascade_target_check [%s]: %d record(s) in %d file(s); ok %d/%d; %s"
                           (name mode) (count records) (count files) ok (count results)
                           (pr-str freqs)))
          (println (format "  rank1-target = decision-target in %d/%d record(s)"
                           (:rank1-equals-decision report) (count results)))
          (doseq [r (remove #(= :ok (:verdict %)) results)]
            (println (format "  %s %s target=%s decision=%s rank1=%s cascades=%s"
                             (:verdict r) (or (:run-id r) (:timestamp r))
                             (:target r) (:decision-target r) (:rank1-target r)
                             (pr-str (:cascade-missions r)))))
          (println (if pass? "PASS" "FAIL"))))
    (System/exit (if pass? 0 1))))

(apply -main *command-line-args*)
