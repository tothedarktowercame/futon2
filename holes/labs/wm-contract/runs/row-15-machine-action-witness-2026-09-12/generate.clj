(ns holes.labs.wm-contract.runs.row-15-machine-action-witness-2026-09-12.generate
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.machine-policy-set :as projection])
  (:import [java.security MessageDigest]))

(def capture-dir "holes/labs/wm-contract/runs/row-15-branches-capture-2026-09-12")
(def lean-path "/home/joe/code/mathlib4/DarkTower/WarMachine/MachineActionBranchesWitness.lean")

(defn sha256 [path]
  (let [d (MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream path)]
      (let [b (byte-array 8192)]
        (loop [] (let [n (.read in b)] (when (pos? n) (.update d b 0 n) (recur))))))
    (apply str (map #(format "%02x" (bit-and 255 %)) (.digest d)))))

(defn read-edn [file] (edn/read-string (slurp (str capture-dir "/" file))))
(defn candidate [{:keys [id score no-op]}]
  (format "⟨%d, %d, %s⟩" id score (if no-op "true" "false")))
(defn candidate-list [name xs]
  (str "def " name " : List Candidate := [\n  "
       (str/join ",\n  " (map candidate xs)) "\n]\n\n"))
(defn project-score [row score]
  (projection/project-candidate (assoc row :controller-score score)))
(defn exact-q [x]
  (let [bits (Double/doubleToRawLongBits (double x))
        sign (if (neg? bits) -1N 1N)
        exp (bit-and 0x7ff (unsigned-bit-shift-right bits 52))
        frac (bit-and bits 0xfffffffffffff)
        mant (+ (bigint (.shiftLeft java.math.BigInteger/ONE 52)) frac)
        power (- exp 1023 52)
        n (* sign mant)
        [n d] (if (neg? power)
                [n (bigint (.shiftLeft java.math.BigInteger/ONE (- power)))]
                [(bigint (.shiftLeft (biginteger n) power)) 1N])]
    (str "((" n " : ℚ) / " d ")")))

(defn -main []
  (let [manifest (read-edn "manifest.edn")
        _ (doseq [{:keys [file] expected-sha :sha256} (:artifacts manifest)]
            (let [actual (sha256 (str capture-dir "/" file))]
              (when-not (= expected-sha actual)
                (throw (ex-info "capture pin mismatch" {:file file :expected expected-sha :actual actual})))))
        natural (read-edn "machinery-capture.edn")
        controller (read-edn "controller-head.edn")
        full (read-edn "full-score-first-max.edn")
        habit (read-edn "habit-last-max.edn")
        abstain (read-edn "no-op-abstain.edn")
        absent (read-edn "requested-posterior-f-pi-absent.edn")
        tie (read-edn "first-max-tie-control.edn")
        base-rows (get-in controller [:input :ranked-actions])
        base-ranked (projection/project-ranked-actions base-rows)
        full-weights (get-in full [:output :softmax-weights])
        full-scored (mapv #(project-score % (get full-weights (:action %))) base-rows)
        weights (get-in habit [:output :softmax-weights])
        habit-scored (mapv #(project-score % (get weights (:action %))) base-rows)
        tie-rows (get-in tie [:input :ranked-actions])
        tie-ranked (projection/project-ranked-actions tie-rows)
        tie-weights (get-in tie [:output :softmax-weights])
        tie-scored (mapv #(project-score % (get tie-weights (:action %))) tie-rows)
        chosen-id (get-in habit [:output :rank])
        chosen-habit (first (filter #(= chosen-id (:id %)) habit-scored))
        selected-full (first (filter #(= (get-in full [:output :rank]) (:id %)) full-scored))
        selected-tie (first tie-scored)
        head (first base-ranked)
        no-op-row (first (filter #(= :no-op (get-in % [:action :type])) base-rows))
        chosen-row (first (filter #(= chosen-id (:rank %)) base-rows))
        eps (get-in abstain [:input :options :abstain-epsilon])
        code (str
              "import DarkTower.WarMachine.MachineAction\n\n"
              "namespace DarkTower.WarMachine.MachineActionBranchesWitness\n\n"
              "open DarkTower.WarMachine.MachineAction\n\n"
              "/- Generated only through futon2.aif.machine-policy-set/project-candidate.\n"
              "The scored carriers use retained positive posterior weights; MachineAction.\n"
              "fullScoreIsPosteriorArgmax licenses their order as the selection-score order. -/\n"
              (candidate-list "baseRanked" base-ranked)
              (candidate-list "fullScoreRanked" full-scored)
              (candidate-list "habitScoreRanked" habit-scored)
              (candidate-list "tieRanked" tie-ranked)
              (candidate-list "tieScored" tie-scored)
              "/-- Natural machinery capture b19ab316...: internal controller head before the distinct reason-bearing override. -/\n"
              "theorem naturalInternalControllerHead :\n  machineAction .strategicRecommendation .controllerHead false true baseRanked [] = some "
              (candidate head) " := by native_decide\n\n"
              "/-- Direct controller record 77586955... -/\n"
              "theorem directControllerHead :\n  machineAction .strategicRecommendation .controllerHead false true baseRanked [] = some "
              (candidate head) " := by native_decide\n\n"
              "/-- Full-score record 5dedbcab... -/\n"
              "theorem fullScoreFirstMax :\n  machineAction .strategicRecommendation .fullScorePosterior true true baseRanked fullScoreRanked = some "
              (candidate selected-full) " := by native_decide\n\n"
              "/-- Habit record 7d98ee2e... -/\n"
              "theorem habitLastMax :\n  machineAction .actuation .controllerHead false true baseRanked habitScoreRanked = some "
              (candidate chosen-habit) " := by native_decide\n\n"
              "/-- Requested posterior, absent F_pi record c86501f2... -/\n"
              "theorem requestedPosteriorAbsentFallsToHead :\n  machineAction .strategicRecommendation .fullScorePosterior false true baseRanked fullScoreRanked = some "
              (candidate head) " := by native_decide\n\n"
              "/-- Commissioned first-max tie record 2ec01de0... -/\n"
              "theorem equalScoreTieKeepsFirst :\n  machineAction .strategicRecommendation .fullScorePosterior true true tieRanked tieScored = some "
              (candidate selected-tie) " := by native_decide\n\n"
              "/-- Abstain record 7aef6171...: exact retained G margin is below the resolved epsilon. -/\n"
              "theorem noOpAbstainComparison : abstains " (exact-q (:controller-score no-op-row)) " "
              (exact-q (:controller-score chosen-row)) " " (exact-q eps) " = true := by\n  norm_num [abstains]\n\n"
              "#print axioms naturalInternalControllerHead\n#print axioms directControllerHead\n"
              "#print axioms fullScoreFirstMax\n#print axioms habitLastMax\n"
              "#print axioms requestedPosteriorAbsentFallsToHead\n"
              "#print axioms equalScoreTieKeepsFirst\n#print axioms noOpAbstainComparison\n\n"
              "end DarkTower.WarMachine.MachineActionBranchesWitness\n")]
    (spit lean-path code)
    (prn {:candidate-count (count base-ranked) :tie-count (count tie-ranked)
          :selected {:head (:id head) :full (:id selected-full) :habit (:id chosen-habit)}
          :abstain {:no-op-G (:controller-score no-op-row)
                    :chosen-G (:controller-score chosen-row) :epsilon eps}})))

(-main)
