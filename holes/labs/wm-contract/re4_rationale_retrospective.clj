#!/usr/bin/env clojure
;; RE4 -- the retrospective query over the decision-time rationale store.
;;
;; READ-ONLY. Reads the rationale records under
;; runs/RE4-rationale-logging/store/ and writes one artifact,
;; runs/RE4-rationale-logging/retrospective.edn. No tick, no run lock, no
;; substrate call, nothing under data/.
;;
;; WHAT IT SCORES. Each `:recorded :selected` rationale carries the assertion
;; the producer wrote into it: THIS DECISION IS RE-SELECTABLE. That is U39/U40's
;; own regret measure -- U40 reports it as "85/85 re-selections" -- and it is the
;; one a rationale store can adjudicate without re-running the machine.
;;
;;   :held-up     the next later decision whose candidate set CONTAINS the chosen
;;                action chose it again.
;;   :refuted     that decision carried the chosen action among its candidates
;;                and chose something else.
;;   :undecidable no later decision carried it (or the record is a refusal or a
;;                typed absence, each of which is said by name rather than
;;                scored as though it were a selection).
;;
;; THE THIRD VERDICT IS NOT A DUMPING GROUND. Control 4 below is the one that
;; matters: a later decision that did not even carry the chosen action must read
;; :undecidable, never :refuted. Without it "refuted" would silently mean "the
;; candidate was not on offer".
;;
;; DETERMINISM: every field is read from a record; no wall clock is written.
;;
;; Run from the futon2 root:
;;   clojure -M holes/labs/wm-contract/re4_rationale_retrospective.clj
;;
;; RE5: the store and the output directory are overridable, so the same query
;; runs against a RUN'S OWN rationale slice rather than only against RE4's
;; committed shadow store. Both default to RE4's paths, so an invocation with
;; no arguments is the one RE4 ran and writes the same file.
;;
;;   clojure -M holes/labs/wm-contract/re4_rationale_retrospective.clj \
;;     --store holes/labs/wm-contract/runs/2026-09-04-re5/rationale \
;;     --out   holes/labs/wm-contract/runs/2026-09-04-re5

(ns re4-rationale-retrospective
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [futon2.aif.selection-rationale :as sr]))

(def ^:private flags
  "`--k v` pairs. A flag with no value is refused loudly rather than silently
   defaulting, because a mistyped `--store` would otherwise score RE4's store
   and label the report with the run the caller meant."
  (loop [[a v & more] (vec *command-line-args*) acc {}]
    (cond
      (nil? a) acc
      (nil? v) (throw (ex-info (str "re4-retrospective: flag " a " has no value") {:flag a}))
      :else (recur more (assoc acc a v)))))

(def out-dir
  (get flags "--out" "holes/labs/wm-contract/runs/RE4-rationale-logging"))

(def store-dir
  (get flags "--store" (str out-dir "/store")))

(defn chosen-key [r]
  (let [a (get-in r [:rationale/chosen :action])]
    (when (map? a) [(:type a) (:target a)])))

(defn selected? [r]
  (and (= :recorded (:rationale/status r)) (= :selected (:rationale/outcome r))))

(defn carries? [r k]
  (boolean (some #(= k %) (:rationale/candidate-keys r))))

(defn verdict
  "The typed verdict for record R against the LATER records, in order."
  [r later]
  (cond
    (= :typed-absence (:rationale/status r))
    {:verdict :undecidable
     :reason :typed-absence
     :absence-reason (:rationale/absence-reason r)}

    (= :refused (:rationale/outcome r))
    {:verdict :undecidable
     :reason :refusal-carries-no-re-selection-claim
     :decision-reason (:rationale/decision-reason r)}

    (not (selected? r))
    {:verdict :undecidable :reason :not-a-selection}

    :else
    (let [k (chosen-key r)
          adjudicator (first (filter #(carries? % k) later))]
      (if-not adjudicator
        {:verdict :undecidable
         ;; The two ways of having no adjudicator are DIFFERENT and are named
         ;; apart: the store simply ends here, versus the machine went on
         ;; deciding without this candidate ever being on offer again.
         :reason (if (empty? later)
                   :no-later-decision-in-the-store
                   :no-later-decision-carried-the-chosen-action)
         :later-decisions-examined (count later)}
        (let [their-choice (chosen-key adjudicator)]
          (merge
           {:adjudicated-by (:rationale/run-id adjudicator)
            :adjudicator-at (:rationale/at adjudicator)
            :later-decisions-examined (count later)}
           (if (= k their-choice)
             {:verdict :held-up
              :re-selected-as-rank (get-in adjudicator [:rationale/chosen :controller-rank])}
             {:verdict :refuted
              :chosen-instead their-choice
              :chosen-instead-rank (get-in adjudicator [:rationale/chosen :controller-rank])})))))))

(defn score-store [records]
  (vec
   (map-indexed
    (fn [i r]
      (merge {:run-id (:rationale/run-id r)
              :at (:rationale/at r)
              :status (:rationale/status r)
              :outcome (:rationale/outcome r)
              :chosen (chosen-key r)
              :chosen-controller-rank (get-in r [:rationale/chosen :controller-rank])
              :candidate-set-size (:rationale/candidate-set-size r)
              :contract-sha (get-in r [:rationale/contract-sha :git-sha])}
             (verdict r (subvec (vec records) (inc i)))))
    records)))

;; ---------------------------------------------------------------------------
;; Controls. Planted stores, so each verdict is shown able to occur and the two
;; ways of being undecidable are shown to be distinguished.

(defn- planted [run-id at chosen candidates]
  {:rationale/schema-version sr/schema-version
   :rationale/status :recorded
   :rationale/outcome :selected
   :rationale/run-id run-id
   :rationale/at at
   :rationale/chosen {:action {:type :advance-mission :target chosen}
                      :controller-rank 1}
   :rationale/candidate-keys (mapv (fn [t] [:advance-mission t]) candidates)
   :rationale/candidate-set-size (count candidates)
   :rationale/contract-sha {:status :present :git-sha "planted"}
   :rationale/emitted-at-decision? true})

(def controls
  (let [c1 (score-store [(planted "p1" "2026-01-01T00:00:00Z" "A" ["A" "B"])
                         (planted "p2" "2026-01-01T00:01:00Z" "A" ["A" "B"])])
        c2 (score-store [(planted "p1" "2026-01-01T00:00:00Z" "A" ["A" "B"])
                         (planted "p2" "2026-01-01T00:01:00Z" "B" ["A" "B"])])
        c3 (score-store [(planted "p1" "2026-01-01T00:00:00Z" "A" ["A" "B"])])
        c4 (score-store [(planted "p1" "2026-01-01T00:00:00Z" "A" ["A" "B"])
                         (planted "p2" "2026-01-01T00:01:00Z" "C" ["C" "D"])])
        c5 (score-store [(-> (planted "p1" "2026-01-01T00:00:00Z" "A" ["A"])
                             (assoc :rationale/outcome :refused
                                    :rationale/decision-reason :no-action-beats-no-op
                                    :rationale/chosen {:status :absent :reason :refused}))
                         (planted "p2" "2026-01-01T00:01:00Z" "A" ["A"])])
        c6 (score-store [{:rationale/schema-version sr/schema-version
                          :rationale/status :typed-absence
                          :rationale/absence-reason :no-decision
                          :rationale/run-id "p1"
                          :rationale/at "2026-01-01T00:00:00Z"
                          :rationale/contract-sha {:status :present :git-sha "planted"}
                          :rationale/emitted-at-decision? true}
                         (planted "p2" "2026-01-01T00:01:00Z" "A" ["A"])])]
    {:c1-held-up-can-occur
     {:claim "a re-selected choice scores :held-up"
      :verdict (:verdict (first c1))
      :pass? (= :held-up (:verdict (first c1)))}
     :c2-refuted-can-occur
     {:claim "a choice the next carrying decision passed over scores :refuted"
      :verdict (:verdict (first c2))
      :chosen-instead (:chosen-instead (first c2))
      :pass? (and (= :refuted (:verdict (first c2)))
                  (= [:advance-mission "B"] (:chosen-instead (first c2))))}
     :c3-no-later-decision-is-undecidable
     {:claim "the last record in a store is :undecidable, not :held-up"
      :verdict (:verdict (first c3))
      :reason (:reason (first c3))
      :pass? (and (= :undecidable (:verdict (first c3)))
                  (= :no-later-decision-in-the-store (:reason (first c3))))}
     :c4-unavailable-candidate-is-not-refutation
     {:claim "a later decision that never carried the chosen action is :undecidable, NOT :refuted, and is distinguished from an empty tail"
      :verdict (:verdict (first c4))
      :reason (:reason (first c4))
      :later-decisions-examined (:later-decisions-examined (first c4))
      :pass? (and (= :undecidable (:verdict (first c4)))
                  (= :no-later-decision-carried-the-chosen-action (:reason (first c4)))
                  (= 1 (:later-decisions-examined (first c4))))}
     :c5-refusal-is-named-not-scored
     {:claim "a refusal is :undecidable for a stated reason rather than scored as a selection"
      :verdict (:verdict (first c5))
      :reason (:reason (first c5))
      :pass? (and (= :undecidable (:verdict (first c5)))
                  (= :refusal-carries-no-re-selection-claim (:reason (first c5))))}
     :c6-typed-absence-is-named-not-scored
     {:claim "a typed absence is :undecidable carrying its own absence reason"
      :verdict (:verdict (first c6))
      :reason (:reason (first c6))
      :absence-reason (:absence-reason (first c6))
      :pass? (and (= :undecidable (:verdict (first c6)))
                  (= :typed-absence (:reason (first c6)))
                  (= :no-decision (:absence-reason (first c6))))}}))

;; ---------------------------------------------------------------------------

(def records (sr/read-store store-dir))
(def scored (score-store records))
(def all-pass? (every? :pass? (vals controls)))

(def report
  {:re4/store store-dir
   :re4/records (count records)
   :re4/verdicts (frequencies (map :verdict scored))
   :re4/scored scored
   :re4/controls controls
   :re4/all-controls-pass? all-pass?})

(io/make-parents (str out-dir "/x"))
(spit (str out-dir "/retrospective.edn") (with-out-str (pp/pprint report)))

(pp/pprint report)
(println)
(println "RE4 retrospective:" (pr-str (:re4/verdicts report))
         (if all-pass? "-- ALL CONTROLS PASS" "-- A CONTROL FAILED"))
(flush)
(System/exit (if all-pass? 0 1))
