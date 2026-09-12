(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.machine-belief :as mb])
(import '[java.security MessageDigest])

(def input (edn/read-string (slurp "holes/labs/wm-contract/runs/row-7-belief-state-2026-09-12/input.edn")))
(def context-base {:model {:id "wm-production" :revision "2026-09-12-row-7"}
                   :state-support mb/state-support :mode :single-entity})

(defn sha256 [path]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256")
                        (java.nio.file.Files/readAllBytes (.toPath (io/file path))))]
    (apply str (map #(format "%02x" (bit-and 255 %)) digest))))

(defn retained-form [{:keys [path form] expected-sha :sha256 :as capture}]
  (when-not (= expected-sha (sha256 path))
    (throw (ex-info "Retained source pin changed" {:capture capture})))
  (with-open [reader (java.io.PushbackReader. (io/reader path))]
    (nth (repeatedly #(edn/read {:eof ::eof} reader)) form)))

(defn measured [capture]
  (let [{:keys [field posterior role] entity :entity/id} capture
        actual-row (get-in (retained-form capture) [field entity])
        context (assoc context-base :entity/id entity :policy-entities [entity])
        result (mb/belief-state-distribution context {entity actual-row})
        output (get-in result [:belief-input :posteriors entity])
        input-sum (reduce + (vals actual-row))]
    {:role role :entity/id entity :ok (:ok result)
     :refusal (:refusal result)
     :coordinates (mapv (fn [s] {:state s :production (get actual-row s)
                                  :lean-reference (get posterior s)
                                  :delta (- (double (get actual-row s))
                                            (double (get posterior s)))})
                        mb/state-support)
     :production-sum input-sum
     :lean-reference-sum (reduce + (vals posterior))
     :packet-9-input (:belief-input result)
     :same-entity (= entity (get-in result [:context :entity/id]))
     :same-model (= (:model context) (:model result))}))

(def positives (mapv measured (:source input)))
(def sample (first (:source input)))
(def eid (:entity/id sample))
(def valid-row (:posterior sample))
(def context (assoc context-base :entity/id eid :policy-entities [eid]))
(def controls
  {:missing (mb/belief-state-distribution context {})
   :deleted (mb/belief-state-distribution context (dissoc {eid valid-row} eid))
   :reordered-support (mb/belief-state-distribution
                       (update context :state-support #(vec (reverse %))) {eid valid-row})
   :invalid-mass (mb/belief-state-distribution context
                                                {eid (assoc valid-row :spawned -1.0)})})
(def report {:schema :wm/belief-state-production-match-v1
             :input input :positive positives :negative-controls controls})
(spit "holes/labs/wm-contract/runs/row-7-belief-state-2026-09-12/readback.edn"
      (pr-str report))
(prn report)
