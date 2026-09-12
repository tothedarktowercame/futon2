(ns holes.labs.wm-contract.runs.row-16-r6-policy-set-2026-09-12.generate
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.machine-policy-set :as policy-set])
  (:import [java.io PushbackReader]
           [java.security MessageDigest]))

(def source-path "data/wm-trace/wm-trace-2026-09-12.edn.pre-migration-backup")
(def source-sha "25a0a1e2257396c2d1db889375773c9cbed99b2ad26c57b4674e02ca1660d686")
(def form-index 3)
(def expected-timestamp "2026-09-12T17:28:09.498448087Z")
(def run-dir "holes/labs/wm-contract/runs/row-16-r6-policy-set-2026-09-12")
(def bounded-path (str run-dir "/wm-trace-policy-set.edn"))
(def fixture-path (str run-dir "/fixture.edn"))
(def lean-path "/home/joe/code/mathlib4/DarkTower/WarMachine/MachinePolicySetMeasurementWitness.lean")

(defn sha256 [path]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream path)]
      (let [buf (byte-array 8192)]
        (loop []
          (let [n (.read in buf)]
            (when (pos? n) (.update digest buf 0 n) (recur))))))
    (apply str (map #(format "%02x" (bit-and 255 %)) (.digest digest)))))

(defn read-forms [path]
  (with-open [r (PushbackReader. (io/reader path))]
    (loop [out []]
      (let [x (edn/read {:eof ::eof} r)]
        (if (= ::eof x) out (recur (conj out x)))))))

(defn lean-bool [x] (if x "true" "false"))
(defn lean-candidate [{:keys [id score no-op]}]
  (format "⟨%d, %d, %s⟩" id score (lean-bool no-op)))

(defn emit-lean [projected]
  (str "import DarkTower.WarMachine.MachinePolicySet\n\n"
       "namespace DarkTower.WarMachine.MachinePolicySetMeasurementWitness\n\n"
       "open DarkTower.WarMachine.MachineAction\n"
       "open DarkTower.WarMachine.MachinePolicySet\n\n"
       "/- Generated from futon2's bounded form-3 projection. Candidate.score is\n"
       "the exact signed binary64 bit pattern; machinePolicySet makes no score-order claim. -/\n"
       "def pinnedRanked : List Candidate := [\n  "
       (str/join ",\n  " (map lean-candidate projected))
       "\n]\n\n"
       "theorem pinnedCandidateCount : pinnedRanked.length = 148 := by rfl\n\n"
       "theorem everyProjectedCandidateIsMember :\n"
       "    ∀ c, c ∈ pinnedRanked → c ∈ machinePolicySet pinnedRanked := by\n"
       "  intro c hc\n  exact hc\n\n"
       "theorem exactProjectedSet :\n"
       "    machinePolicySet pinnedRanked = {c | c ∈ pinnedRanked} := by rfl\n\n"
       "#print axioms pinnedCandidateCount\n"
       "#print axioms everyProjectedCandidateIsMember\n"
       "#print axioms exactProjectedSet\n\n"
       "end DarkTower.WarMachine.MachinePolicySetMeasurementWitness\n"))

(defn -main []
  (when-not (= source-sha (sha256 source-path))
    (throw (ex-info "source pin mismatch" {:expected source-sha :actual (sha256 source-path)})))
  (let [record (nth (read-forms source-path) form-index)
        _ (when-not (= expected-timestamp (:timestamp record))
            (throw (ex-info "form timestamp mismatch" {:timestamp (:timestamp record)})))
        ranked (mapv (fn [row]
                       {:rank (:rank row)
                        :controller-score (:controller-score row)
                        :action (select-keys (:action row) [:type])})
                     (:ranked-actions record))
        bounded {:timestamp (:timestamp record)
                 :wm-version (:wm-version record)
                 :ranked-actions ranked}
        projected (policy-set/project-ranked-actions ranked)
        fixture {:schema :wm/row16-r6-policy-set-fixture-v1
                 :subject {:node :R6 :equation :policy-set :quantity :pi
                           :declaration "machinePolicySet"}
                 :source {:repo "futon2" :path source-path :sha256 source-sha
                          :form-index form-index :timestamp expected-timestamp
                          :trace-schema (get-in record [:wm-version :trace-schema-version])}
                 :extracted-at "2026-09-12T21:25:00Z"
                 :projection {:id :retained-rank
                              :score :signed-ieee-754-raw-bits
                              :no-op :action-type-equals-no-op}
                 :candidate-count (count projected)
                 :projected projected}]
    (spit bounded-path (with-out-str (pp/pprint bounded)))
    (spit fixture-path (with-out-str (pp/pprint fixture)))
    (spit lean-path (emit-lean projected))
    (prn {:bounded bounded-path :fixture fixture-path :lean lean-path
          :candidate-count (count projected)})))

(-main)
