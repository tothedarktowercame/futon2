(ns holes.labs.wm-contract.runs.row-16-r8-policy-f-2026-09-12.generate
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str])
  (:import [java.io PushbackReader]
           [java.security MessageDigest]))

(def source "holes/labs/wm-contract/runs/2026-09-01-s4/wm-trace-s4.edn")
(def source-sha "aaeccaf477dfd16bcc73064aa979f1fefa9753e2eec4ccd053e5e17acf8efdbf")
(def run-dir "holes/labs/wm-contract/runs/row-16-r8-policy-f-2026-09-12")
(def lean-path "/home/joe/code/mathlib4/DarkTower/WarMachine/MachinePolicyFreeEnergyMeasurementWitness.lean")
(def channels [:active-repo-ratio :annotation-health :attack-coverage :consulting-pct
               :coupling-density :depositing-signal :loop-health :mathematics-pct
               :mission-health :portfolio-pct :sorry-count-norm :stack-pct
               :support-coverage :ticks-firing-ratio])

(defn sha256 [path]
  (let [d (MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream path)]
      (let [b (byte-array 8192)]
        (loop [] (let [n (.read in b)] (when (pos? n) (.update d b 0 n) (recur))))))
    (apply str (map #(format "%02x" (bit-and 255 %)) (.digest d)))))

(defn forms [path]
  (with-open [r (PushbackReader. (io/reader path))]
    (loop [v []] (let [x (edn/read {:eof ::eof} r)]
                   (if (= ::eof x) v (recur (conj v x)))))))

(defn exact-ratio [x]
  (let [bits (Double/doubleToRawLongBits (double x))
        sign (if (neg? bits) -1N 1N)
        exp (bit-and 0x7ff (unsigned-bit-shift-right bits 52))
        frac (bit-and bits 0xfffffffffffff)
        pow52 (.shiftLeft java.math.BigInteger/ONE 52)
        mant (if (zero? exp) (bigint frac) (+ (bigint pow52) frac))
        e (- (if (zero? exp) 1 exp) 1023 52)
        n (* sign mant)]
    (if (neg? e)
      [n (bigint (.shiftLeft java.math.BigInteger/ONE (- e)))]
      [(bigint (.shiftLeft (biginteger n) e)) 1N])))

(defn ratio-str [x] (let [[n d] (exact-ratio x)] (str n "/" d)))
(defn lean-rat [x] (let [[n d] (exact-ratio x)] (str "(" n " : ℝ) / " d)))
(defn ident [a]
  [(:type a) (cond (contains? a :target) [:target (:target a)]
                   (contains? a :target-class) [:target-class (:target-class a)]
                   :else [:unscoped nil])])

(defn datum [candidate observation channel]
  (let [mean (get-in candidate [:prediction-mean channel])
        raw-v (get-in candidate [:prediction-variance channel])
        status (get-in candidate [:prediction-variance-status channel :status])
        observed (get observation channel)]
    {:channel channel :mean mean :observation observed
     :residual (- (double observed) (double mean))
     :raw-variance raw-v :variance-status status
     :effective-variance (if (and (zero? (double raw-v)) (= :absent status)) 0.01 raw-v)}))

(defn lean-datum [{:keys [residual raw-variance variance-status]}]
  (str "⟨" (lean-rat residual) ", " (lean-rat raw-variance) ", ."
       (name variance-status) "⟩"))

(defn emit-lean [rows]
  (str "import DarkTower.WarMachine.MachinePolicyFreeEnergy\n\n"
       "namespace DarkTower.WarMachine.MachinePolicyFreeEnergyMeasurementWitness\n\n"
       "open DarkTower.WarMachine.MachinePolicyFreeEnergy\n\n"
       "/- Generated from the pinned S4 consecutive-record fixture. Every decimal input is\n"
       "expanded to the exact rational represented by its retained IEEE-754 binary64 bits. -/\n"
       "noncomputable def pinnedChannels : List (List ChannelDatum) := [\n  "
       (str/join ",\n  " (map #(str "[" (str/join ", " (map lean-datum (:channels %))) "]") rows))
       "\n]\n\n"
       "theorem pinnedCandidateCount : pinnedChannels.length = 145 := by rfl\n\n"
       "/-- Each pinned candidate is governed by the registry's symbolic-log law. -/\n"
       "theorem everyPinnedCandidateUsesMachinePolicyFreeEnergy :\n"
       "    ∀ xs ∈ pinnedChannels,\n"
       "      machinePolicyFreeEnergy xs 0 (1 / 100) .floor =\n"
       "        xs.foldlM (fun total datum =>\n"
       "          return total + (← channelPolicyFreeEnergy datum 0 (1 / 100) .floor)) 0 := by\n"
       "  intro xs _\n  rfl\n\n"
       "#print axioms pinnedCandidateCount\n"
       "#print axioms everyPinnedCandidateUsesMachinePolicyFreeEnergy\n\n"
       "end DarkTower.WarMachine.MachinePolicyFreeEnergyMeasurementWitness\n"))

(defn -main []
  (when-not (= source-sha (sha256 source))
    (throw (ex-info "source pin mismatch" {:actual (sha256 source)})))
  (let [xs (forms source) previous (nth xs 2) current (nth xs 3)
        stored (get-in current [:f-pi-by-candidate-id :by-candidate-id])
        stored-by-identity (into {} (map (juxt :candidate-identity identity) (vals stored)))
        rows (mapv (fn [candidate]
                     (let [rank (:rank candidate) id (str "rank/" rank)]
                       {:candidate-id id :candidate-identity (ident (:action candidate))
                        :retained-result (get stored-by-identity (ident (:action candidate)))
                        :channels (mapv #(datum candidate (:observation current) %) channels)}))
                   (:ranked-actions previous))
        fixture {:schema :wm/row16-r8-policy-f-fixture-v1
                 :subject {:node :R8 :equation :policy-free-energy :quantity :F-pi
                           :declaration "machinePolicyFreeEnergy"}
                 :source {:repo "futon2" :path source :sha256 source-sha
                          :previous-form-index 2 :current-form-index 3
                          :previous-timestamp (:timestamp previous)
                          :current-timestamp (:timestamp current)}
                 :extracted-at "2026-09-12T22:43:00Z"
                 :configuration {:deterministic-tolerance 0.0 :absent-variance :floor
                                 :variance-floor 0.01
                                 :caller "scripts/futon2/report/war_machine.clj:458-564"}
                 :scope :s4-run-2026-09-01-consecutive-records
                 :run-kind :redirected-experimental-run9-machinery
                 :rows rows
                 :real-refusal-control
                 {:form-index 1 :timestamp (:timestamp (nth xs 1))
                  :result (get-in (nth xs 1) [:decision :f-pi-posterior])}}]
    (spit (str run-dir "/fixture.edn") (with-out-str (pp/pprint fixture)))
    (with-open [w (io/writer (str run-dir "/symbolic-input.tsv"))]
      (.write w "candidate\tchannel\tresidual\tvariance\n")
      (doseq [row rows d (:channels row)]
        (.write w (str (subs (:candidate-id row) 5) "\t" (name (:channel d)) "\t"
                       (ratio-str (:residual d)) "\t" (ratio-str (:effective-variance d)) "\n"))))
    (spit lean-path (emit-lean rows))
    (prn {:candidates (count rows) :coordinates (reduce + (map #(count (:channels %)) rows))})))

(-main)
