;; U6 (worklist.edn :U6) — freeze ONE real recorded zaif v0 decision, and the
;; corpus facts the futon2 test cites, from the SHIPPED controller.
;;
;; This script runs in the FUTON3C jvm, because zaif v0 lives there and futon2
;; does not have futon3c on its classpath (futon2/deps.edn :deps). That is the
;; reason the futon2 test carries a FROZEN fixture rather than calling decide()
;; live: the fixture's numbers come from here, and the futon2 test pins the
;; fixture's SHAPE against the controller source it was produced by.
;;
;;   cd /home/joe/code/futon3c && \
;;     FUTON3C_ZAIF_GAMMA_EDN=/home/joe/code/futon2/holes/labs/M-zaif-harness/b1-gamma-mission.edn \
;;     clojure -M ../futon2/holes/labs/wm-contract/u6_zaif_decision.clj \
;;     > ../futon2/holes/labs/wm-contract/U6-ZAIF-DECISION.txt
;;
;; Pure replay: reads two files, writes stdout, takes no run lock, touches
;; nothing under data/ and never calls persist-decision!.

(require '[futon3c.agents.zaif-controller :as zc]
         '[futon3c.agents.zaif-inputs :as zi]
         '[clojure.edn :as edn]
         '[clojure.pprint :as pp])

(def sessions-path
  "/home/joe/code/futon2/holes/labs/M-zaif-harness/calibration-sessions.edn")

(def session-id
  "The frozen subject. A REAL recorded operator turn: gold-judged, labelled a
   correction, and attributed to the ONE mission whose B1 gamma cell is burned
   in (gamma 0.7071 rather than the uniform 1.0 prior), so the fixture is
   non-degenerate on the gamma channel."
  "e-ce907fcf-c7e1-4272-a4a2-13def7aaaa50")

(def sessions (edn/read-string (slurp sessions-path)))
(def session (first (filter #(= session-id (:id %)) sessions)))

(zi/reset-gamma-cache!)

(defn- hydrate [s] (zi/hydrate-inputs {:context (:context s)}))

(def inputs (hydrate session))
(def decision (zc/decide inputs))

(defn- tied-at-max?
  "Was the chosen arm a TIE at the maximum G-term? `choose-arm`
   (zaif_controller.clj:98-103) sorts by [(- value) tie-rank] and takes the
   first, so a tie is resolved by the fixed arm order :act :retrieve :ask
   :yield and not by anything the decision measured."
  [d]
  (let [vs (vals (:g-terms d))
        m (apply max (map double vs))]
    (> (count (filter #(= m (double %)) vs)) 1)))

(def corpus (mapv (fn [s] (let [i (hydrate s)] {:inputs i :decision (zc/decide i)}))
                  sessions))

(println "=== U6-ZAIF-DECISION: one recorded zaif v0 decision, and its corpus ===")
(println)
(println "controller  futon3c/src/futon3c/agents/zaif_controller.clj")
(println "hydrator    futon3c/src/futon3c/agents/zaif_inputs.clj")
(println "sessions    futon2/holes/labs/M-zaif-harness/calibration-sessions.edn")
(println "gamma       futon2/holes/labs/M-zaif-harness/b1-gamma-mission.edn")
(println)
(println "--- SHIPPED CONSTANTS (zaif_controller.clj:11-20) ---")
(pp/pprint zc/constants)
(println)
(println "--- THE FROZEN SESSION ---")
(pp/pprint (select-keys session [:id :at :is_correction :route :gold_judged]))
(println "context:")
(prn (:context session))
(println)
(println "--- HYDRATED INPUTS (zaif_inputs.clj:177-194) ---")
(pp/pprint inputs)
(println)
(println "--- THE DECISION (zaif_controller.clj:105-141) ---")
(pp/pprint decision)
(println "chosen arm was a tie at the maximum?" (tied-at-max? decision))
(println)
(println "--- THE SAME INPUTS UNDER BOTH Z3a CONSTANTS (zaif_controller.clj:150-171) ---")
(pp/pprint (zc/dual-decide inputs))
(println)
(println "--- CORPUS (all recorded sessions, live hydrator, shipped constants) ---")
(println "sessions                    " (count corpus))
(println "arm distribution            " (frequencies (map #(-> % :decision :arm) corpus)))
(println "chosen by TIE-BREAK, not score"
         (count (filter #(tied-at-max? (:decision %)) corpus))
         "  by arm" (frequencies (map #(-> % :decision :arm)
                                      (filter #(tied-at-max? (:decision %)) corpus))))
(println ":act G-term non-zero on     "
         (count (filter #(not= 0.0 (-> % :decision :g-terms :act)) corpus)))
(println ":task-belief non-empty on   "
         (count (filter #(seq (-> % :inputs :task-belief)) corpus)))
(println "distinct gamma-used         "
         (vec (sort (distinct (map #(-> % :decision :gamma-used) corpus)))))
(println "distinct :ask G-terms       "
         (vec (sort (distinct (map #(-> % :decision :g-terms :ask) corpus)))))
(println ":retrieve G-term range      "
         [(apply min (map #(-> % :decision :g-terms :retrieve) corpus))
          (apply max (map #(-> % :decision :g-terms :retrieve) corpus))])
(println "arm distribution at the Z3a sweep constant 0.15"
         (frequencies (map (fn [s] (-> (zc/dual-decide (hydrate s)) second :decision :arm))
                           sessions)))
(println)
(println "--- EVIDENCE BODY KEYS (zaif_controller.clj:185-218) ---")
(pp/pprint (vec (sort (keys (:evidence/body
                             (zc/decision-evidence-entry
                              {:agent-id "u6" :sid "u6" :turn-id "u6"
                               :decision decision :inputs inputs
                               :constant 0.65 :constant-label :shipped
                               :pairing-key "u6" :round 1}))))))
