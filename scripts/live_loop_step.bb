#!/usr/bin/env bb
;; live_loop_step.bb — stepper for E-live-loop-2 (operator design 2026-07-05):
;; microsteps + machine-checkable gates; a step's ACTION may only run when
;; ALL its :needs gates are green. Gates re-run freely (standing evidence
;; checks). This tool never performs actions; it enforces order + records.
;;
;;   bb live_loop_step.bb status
;;   bb live_loop_step.bb gate <id>     ; run one gate, record result
;;   bb live_loop_step.bb gates         ; run all :cmd gates
;;   bb live_loop_step.bb runnable <id> ; may the ACTION be performed?
(require '[clojure.edn :as edn]
         '[clojure.java.shell :as sh]
         '[clojure.string :as str])

(def state-file
  ;; override with LIVE_LOOP_STEPS to drive any loop's board (added for loop-3)
  (or (System/getenv "LIVE_LOOP_STEPS")
      "/home/joe/code/futon2/holes/e-live-loop-2-steps.edn"))

(defn load-state [] (edn/read-string (slurp state-file)))
(defn save-state [st] (spit state-file (with-out-str (clojure.pprint/pprint st))))

(defn step-by-id [st id] (first (filter #(= id (:id %)) (:steps st))))

(defn gate-pass? [step] (= :pass (:status step)))

(defn needs-met [st step]
  (let [unmet (remove #(gate-pass? (step-by-id st %)) (:needs step))]
    {:met? (empty? unmet) :unmet (vec unmet)}))

(defn run-gate [st id]
  (let [step (step-by-id st id)]
    (cond
      (nil? step) (do (println "no such step:" id) st)
      (:manual (:gate step))
      (do (println (format "[%s] gate MANUAL/undefined — %s" id (:manual (:gate step))))
          st)
      :else
      (let [{:keys [exit out err]} (sh/sh "bash" "-c" (:cmd (:gate step)))
            pass (zero? exit)
            entry {:at (str (java.time.Instant/now))
                   :pass pass
                   :out (str/trim (subs (str out err) 0 (min 300 (count (str out err)))))}
            prev (:status step)
            st' (update st :steps
                        (fn [steps]
                          (mapv #(if (= id (:id %))
                                   (-> % (assoc :status (if pass :pass :fail))
                                       (update :history conj entry))
                                   %)
                                steps)))]
        (println (format "[%s] gate %s%s" id (if pass "PASS" "FAIL")
                         (if (and (= prev :pass) (not pass))
                           "  ** REGRESSION: was green — ledger entry required **" "")))
        (when-not pass (println "   " (:out entry)))
        (save-state st')
        st'))))

(defn status [st]
  (println (format "%-4s %-6s %-8s %-9s %s" "id" "armed" "status" "runnable" "title"))
  (doseq [s (:steps st)]
    (let [{:keys [met? unmet]} (needs-met st s)]
      (println (format "%-4s %-6s %-8s %-9s %s%s"
                       (:id s) (if (:armed s) "⚠YES" "-")
                       (name (:status s))
                       (if met? "yes" "no")
                       (:title s)
                       (if met? "" (str "  [needs: " (str/join "," unmet) "]")))))))

(defn runnable [st id]
  (let [step (step-by-id st id)
        {:keys [met? unmet]} (needs-met st step)]
    (cond
      (not met?) (println (format "[%s] NO — unmet gates: %s" id (str/join ", " unmet)))
      (:armed step) (println (format "[%s] gates met, but ⚠ARMED — requires the operator's word, recorded in step history" id))
      :else (println (format "[%s] YES — all prerequisite gates green" id)))))

(let [[cmd id] *command-line-args*
      st (load-state)]
  (case cmd
    "status" (status st)
    "gate" (run-gate st id)
    "gates" (reduce (fn [s step] (if (get-in step [:gate :cmd]) (run-gate s (:id step)) s))
                    st (:steps st))
    "runnable" (runnable st id)
    (println "usage: live_loop_step.bb status | gate <id> | gates | runnable <id>")))
