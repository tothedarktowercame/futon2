(ns row12-witness
  (:require [clojure.edn :as edn] [clojure.java.io :as io]
            [clojure.pprint :as pp] [futon2.aif.machine-accumulation :as acc])
  (:import [java.io PushbackReader] [java.security MessageDigest]))

(def trace-path "data/wm-trace/wm-trace-2026-09-04.edn")
(def output-path "holes/labs/wm-contract/runs/row-12-accumulation-2026-09-12/witness.edn")
(def entity "arxana/stack/futon-v1/leaf/2/2")
(defn sha256 [path]
  (let [bs (java.nio.file.Files/readAllBytes (.toPath (io/file path)))]
    (apply str (map #(format "%02x" (bit-and 255 %))
                    (.digest (MessageDigest/getInstance "SHA-256") bs)))))
(defn forms [n]
  (with-open [r (PushbackReader. (io/reader trace-path))]
    (vec (repeatedly n #(edn/read {:eof ::eof} r)))))
(defn reference-step [table observation belief observation-support state-support]
  (into {} (for [o observation-support]
             [o (into {} (for [s state-support]
                           [s (+ (get-in table [o s]) (* (observation o) (belief s)))]))])))
(defn max-delta [actual reference observation-support state-support]
  (reduce max 0.0 (for [o observation-support s state-support]
                    (Math/abs (- (double (get-in actual [o s]))
                                 (double (get-in reference [o s])))))))
(defn run! []
  (let [rows (forms 3)
        observation-support (vec (sort (keys (:observation (first rows)))))
        state-support (vec (sort (keys (get-in (first rows) [:mu-post entity]))))
        ids (mapv :timestamp rows)
        ticks (mapv (fn [i row]
                      {:id (ids i) :previous-id (when (pos? i) (ids (dec i)))
                       :observation (:observation row) :belief (get-in row [:mu-post entity])})
                    (range 3) rows)
        initial (acc/initialize observation-support state-support 1.0)
        replay (reduce (fn [{:keys [state reference steps]} tick]
                         (let [next (acc/step state tick)
                               ref (reference-step reference (:observation tick) (:belief tick)
                                                   observation-support state-support)
                               delta (max-delta (:concentrations next) ref observation-support state-support)]
                           {:state next :reference ref
                            :steps (conj steps {:tick-id (:id tick) :previous-id (:previous-id tick)
                                               :production (:concentrations next) :lean-reference ref
                                               :per-coordinate-delta
                                               (into {} (for [o observation-support]
                                                          [o (into {} (for [s state-support]
                                                                        [s (- (double (get-in (:concentrations next) [o s]))
                                                                              (double (get-in ref [o s])))]))]))
                                               :maximum-absolute-delta delta})}))
                       {:state initial :reference (:concentrations initial) :steps []} ticks)
        first-state (acc/step initial (ticks 0))
        recount (acc/step initial (assoc (ticks 2) :previous-id nil))
        dropped (acc/step first-state (assoc (ticks 2) :previous-id (ids 1)))
        mutated (acc/step first-state (update (ticks 1) :belief dissoc (first state-support)))
        witness {:schema :wm/accumulation-witness-v1 :row 12 :status :executed :at "2026-09-12"
                 :trace {:path trace-path :sha256 (sha256 trace-path)
                         :forms (mapv (fn [i id] {:index i :id id :previous-id (when (pos? i) (ids (dec i)))})
                                      (range 3) ids)}
                 :entity entity :initialization {:authority :declared :prior 1.0}
                 :coordinate-contract {:observation observation-support :state state-support}
                 :declared-update "a[t+1,o,s] = a[t,o,s] + observation[t,o] * belief[t,s]"
                 :steps (:steps replay) :compared-coordinates (* 3 (count observation-support) (count state-support))
                 :maximum-absolute-delta (reduce max (map :maximum-absolute-delta (:steps replay)))
                 :float-rule :exact-ieee-operation-order-no-rounding
                 :controls [{:mutation :recount-current-tick-only
                             :passed? (false? (acc/recurrence-valid? first-state (ticks 2) recount))
                             :result :failed-recurrence}
                            {:mutation :dropped-middle-tick :passed? (= :carry-chain-gap (get-in dropped [:refusal :kind]))
                             :refusal (get-in dropped [:refusal :kind])}
                            {:mutation :support-mismatch :passed? (= :support-mismatch (get-in mutated [:refusal :kind]))
                             :refusal (get-in mutated [:refusal :kind])}]
                 :boundary {:live-wiring false :a4a-changed false :registry-changed false}}]
    (when-not (and (zero? (:maximum-absolute-delta witness))
                   (every? :passed? (:controls witness)))
      (throw (ex-info "Row 12 witness failed" witness)))
    (spit output-path (with-out-str (pp/pprint witness)))
    (prn {:status :executed :compared-coordinates (:compared-coordinates witness)
          :maximum-absolute-delta (:maximum-absolute-delta witness)
          :controls (mapv #(select-keys % [:mutation :passed? :refusal :result]) (:controls witness))})))
(run!)
