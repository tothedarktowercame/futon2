#!/usr/bin/env clojure
;; :B2 -- the refused-set consequence of the adoptions, shown by REPLAY.
;;
;;   clojure -M holes/labs/wm-contract/b2_refused_set_replay.clj [outdir]
;;
;; The two missions this row adopted holes into were both at
;; :open-hole-count 0, and `task-belief-ladder/classify` overrides every
;; support rung with the typed rung-3 :no-open-holes refusal at zero
;; (task_belief_ladder.clj:262-285). This replays that classification for an
;; :advance-mission candidate at each mission's count BEFORE the adoption (0,
;; the value the registry reported at 2026-09-05 before the edit) and AT the
;; count the registry reports now, over an empty case history so the only thing
;; that can move the verdict is the hole count.
;;
;; No live run, no tick, no run lock. The counts are read from the registry the
;; scheduler reads; everything else is pure.

(require '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[futon2.aif.mission-registry :as registry]
         '[futon2.aif.task-belief-ladder :as ladder])

(def missions ["M-wm-aif-policy-grain-compliance" "M-aif-policy-conditioned-eig"])

(defn- candidate [mission-id h]
  {:type :advance-mission :target mission-id :open-hole-count h})

(defn- verdict [ctx mission-id h]
  (let [c (ladder/classify ctx (candidate mission-id h))]
    {:open-hole-count h
     :rung (:task-belief/rung c)
     :factor (:task-belief/factor c)
     :rule (get-in c [:task-belief/derivation :rule])
     :refused? (= :no-open-holes (get-in c [:task-belief/derivation :rule]))}))

(def ctx
  "Empty case history: the only thing that can move a verdict is the hole count."
  (ladder/field-context (mapv #(candidate % 0) missions) {} {}))

(def ctx-with-one-decision
  "The same field with ONE persisted decision against each candidate's own key.
   Without it every candidate is rung 3 for the ordinary reason -- construction
   exhausted -- and the hole rule's effect is invisible; with it the rung-1
   support the hole rule was overriding is present, so the adoption's arithmetic
   consequence (factor scaled by h/(h+1) instead of zeroed) is readable."
  (ladder/field-context (mapv #(candidate % 0) missions)
                        (into {} (map (fn [m] [(ladder/action-key (candidate m 0)) 1])) missions)
                        {}))

(def record
  {:schema :wm/b2-refused-set-replay-v1
   :row :B2
   :at "2026-09-05"
   :what-this-is
   "The refusal that the adopted holes lift, replayed. Every verdict is read off
    a call to the same classify the field build calls."
   :case-history-sizes {:empty (count (:history ctx))
                        :one-per-candidate (count (:history ctx-with-one-decision))}
   :missions
   (into (sorted-map)
         (map (fn [m]
                (let [now (:open-hole-count (registry/mission-status m))]
                  [m {:registry-open-hole-count-now now
                      :empty-case-history
                      {:before-adoption (verdict ctx m 0)
                       :after-adoption (verdict ctx m now)}
                      :one-persisted-decision
                      {:before-adoption (verdict ctx-with-one-decision m 0)
                       :after-adoption (verdict ctx-with-one-decision m now)}}])))
         missions)})

(let [dir (io/file (or (first *command-line-args*)
                       "holes/labs/wm-contract/runs/B2-strawman"))
      f (io/file dir "03-refused-set-replay.edn")]
  (.mkdirs dir)
  (spit f (with-out-str (pp/pprint record)))
  (println "wrote" (str f))
  (pp/pprint record))
