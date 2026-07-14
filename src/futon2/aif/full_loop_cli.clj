(ns futon2.aif.full-loop-cli
  "Operator entrypoint for real War Machine ticks and durée clicks."
  (:require [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.morning-brief :as brief]
            [futon2.aif.trace :as trace]))

(defn- parse-long! [label x]
  (or (parse-long x)
      (throw (ex-info (str label " must be an integer") {label x}))))

(defn- option-map [args]
  (loop [xs args out {}]
    (if (empty? xs)
      out
      (let [[flag value & more] xs]
        (when-not (and (str/starts-with? flag "--") value)
          (throw (ex-info "Options require --name value pairs" {:args xs})))
        (recur more (assoc out (keyword (subs flag 2)) value))))))

(defn- runner-opts [trigger flags]
  (cond-> {:trigger trigger}
    (:author flags) (assoc :author (:author flags))
    (:reviewer flags) (assoc :reviewer (:reviewer flags))
    (:window-days flags) (assoc :window-days (parse-long! :window-days
                                                           (:window-days flags)))))

(defn- canary-path []
  (str "/home/joe/code/futon2/data/wm-full-loop-canary/canary-"
       (str/replace (str (java.time.Instant/now)) #"[:.]" "-") ".edn"))

(defn- print-value [x]
  (pp/pprint x)
  x)

(defn- run-once! [trigger flags]
  (print-value (runner/run-opportunity! (runner-opts trigger flags))))

(defn- continuous! [flags]
  (let [interval-seconds (parse-long! :interval-seconds
                                      (get flags :interval-seconds "0"))
        count-limit (some-> (:count flags) (parse-long! :count))]
    (loop [n 0]
      (when (or (nil? count-limit) (< n count-limit))
        (run-once! :duree-click-continuous flags)
        (when (pos? interval-seconds)
          (Thread/sleep (* 1000 interval-seconds)))
        (recur (inc n))))))

(def usage
  (str "War Machine real full-loop runner\n\n"
       "  status\n"
       "  activate\n"
       "  canary [--out PATH] [agent options]\n"
       "  once [--author zai-5 --reviewer codex-7]\n"
       "  tick [--author zai-5 --reviewer codex-7]\n"
       "  continuous [--count N] [--interval-seconds N] [agent options]\n"
       "  brief\n"
       "  qa ATTEMPT-ID ENTITY-ID VERDICT NOTE [REVIEWER]\n\n"
       "once is an on-demand durée click; continuous emits sequential durée "
       "clicks; tick is one wall-clock opportunity. VERDICT is one of approve, "
       "confirmed, request-changes, reject, uncertain."))

(defn- run-command! [args]
  (let [[command & rest] args]
    (case command
      "status" (print-value {:cohort (cohort/ledger)
                              :readiness (runner/readiness)})
      "activate" (print-value {:activation (cohort/activate!)})
      "canary" (let [flags (option-map rest)
                     out (or (:out flags) (canary-path))]
                 (print-value
                  (runner/run-opportunity!
                   (assoc (runner-opts :duree-click-on-demand flags)
                          :cohort? false :canary-out out))))
      "once" (run-once! :duree-click-on-demand (option-map rest))
      "tick" (run-once! :wallclock-cron (option-map rest))
      "continuous" (continuous! (option-map rest))
      "brief" (let [record (trace/latest-trace-record)]
                (print-value {:pending (brief/pending-items)
                              :reviews (brief/reviews)
                              :valid-belief-entity-ids
                              (vec (sort-by str (keys (:mu-post record))))}))
      "qa" (let [[attempt-id entity-id verdict note reviewer & extra] rest]
             (when (or (seq extra) (some nil? [attempt-id entity-id verdict note]))
               (throw (ex-info "qa requires ATTEMPT ENTITY VERDICT NOTE [REVIEWER]"
                               {:args rest})))
             (print-value (brief/review! attempt-id entity-id (keyword verdict)
                                         note (or reviewer "joe"))))
      (do (println usage)
          (when command (throw (ex-info "Unknown command" {:command command})))))))

(defn -main [& args]
  (try
    (run-command! args)
    (finally
      ;; clojure.java.shell uses the non-daemon solo-agent executor. Without
      ;; this, completed one-shot runs visibly linger for its idle timeout.
      (shutdown-agents))))
