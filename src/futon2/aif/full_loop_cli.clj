(ns futon2.aif.full-loop-cli
  "Operator entrypoint for real War Machine ticks and durée clicks."
  (:require [clojure.pprint :as pp]
            [clojure.string :as str]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.morning-brief :as brief]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.trace :as trace]))

(defn- parse-long! [label x]
  (or (parse-long x)
      (throw (ex-info (str label " must be an integer") {label x}))))

(defn- parse-tripwire-action! [value]
  (let [action (keyword value)]
    (when-not (#{:record :stop-line :park-and-summon} action)
      (throw (ex-info "--tripwire-action must be record, stop-line, or park-and-summon"
                      {:tripwire-action value})))
    action))

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
    (:repair-reviewer flags) (assoc :repair-reviewer (:repair-reviewer flags))
    (:batch-id flags) (assoc :batch-id (:batch-id flags))
    (:tripwire-action flags)
    (assoc :tripwire/action (parse-tripwire-action! (:tripwire-action flags)))
    (:window-days flags) (assoc :window-days (parse-long! :window-days
                                                           (:window-days flags)))
    (:agent-budget-seconds flags)
    (assoc :agent-budget-ms
           (* 1000 (parse-long! :agent-budget-seconds
                                (:agent-budget-seconds flags))))))

(defn- canary-path []
  (str "/home/joe/code/futon2/data/wm-full-loop-canary/canary-"
       (str/replace (str (java.time.Instant/now)) #"[:.]" "-") ".edn"))

(defn- print-value [x]
  (pp/pprint x)
  x)

(defn- run-once! [trigger flags]
  (print-value (runner/run-opportunity! (runner-opts trigger flags))))

(defn- selected-target [result]
  (get-in result [:checkpoints :selection :judgment :selected-mission]))

(defn- continuous! [flags]
  (let [interval-seconds (parse-long! :interval-seconds
                                      (get flags :interval-seconds "0"))
        count-limit (when-let [count-value (:count flags)]
                      (parse-long! :count count-value))]
    (loop [n 0 previous-target nil]
      (when (or (nil? count-limit) (< n count-limit))
        (let [result (run-once! :duree-click-continuous flags)]
          (when-not (= :grounded-change (:outcome result))
            (throw (ex-info "Continuous full loop stopped after non-grounded outcome"
                            {:outcome :continuous-stopped
                             :completed-clicks (inc n)
                             :last-result result})))
          (let [target (selected-target result)]
            (when (and target (= previous-target target))
              (throw (ex-info "Continuous full loop stopped after repeated selection"
                              {:outcome :continuous-stopped
                               :reason :repeated-selection
                               :completed-clicks (inc n)
                               :repeated-target target
                               :last-result result})))
          (when (and (pos? interval-seconds)
                     (or (nil? count-limit) (< (inc n) count-limit)))
            (Thread/sleep (* 1000 interval-seconds)))
            (recur (inc n) target)))))))

(defn- batch-brief [batch-id]
  (let [all-reviews (brief/reviews)
        normalize-item
        (fn [item]
          (assoc item
                 :achievement
                 (or (:achievement item)
                     {:tier :legacy-unverified
                      :summary
                      "Legacy item: no structured achievement summary was recorded"})
                 :repair-history
                 (repair/obligation-history (:attempt-id item))))
        items (->> (brief/items)
                   (filter #(= batch-id (:batch-id %)))
                   (sort-by :queued-at)
                   (mapv #(brief/with-pending-objectives % all-reviews))
                   (mapv normalize-item)
                   vec)
        attempt-ids (set (map :attempt-id items))
        reviews (->> all-reviews
                     (filter #(contains? attempt-ids (:attempt-id %)))
                     (sort-by :reviewed-at)
                     vec)
        item-view (fn [item]
                    (-> (select-keys item [:attempt-id :selected-target :outcome
                                           :author :reviewer :commit :queued-at
                                           :selection-review :achievement :failure
                                           :repair-history
                                           :qa-targets :pending-objectives])
                        (assoc :witness
                               (select-keys (:witness item)
                                            [:resolved? :dial-moved?
                                             :implementation-id :discharge-id]))))]
    {:morning-brief/schema-version 3
     :batch-id batch-id
     :judgment-order (mapv :attempt-id items)
     :attempt-count (count items)
     :fully-grounded-count (count (filter #(= :fully-grounded
                                               (get-in % [:achievement :tier]))
                                          items))
     :partial-authored-count (count (filter #(= :partial-authored
                                                (get-in % [:achievement :tier]))
                                           items))
     :no-achievement-count (count (filter #(= :none
                                              (get-in % [:achievement :tier]))
                                         items))
     :pending-count (reduce + 0 (map #(count (:pending-objectives %)) items))
     :items (mapv item-view items)
     :reviews reviews}))

(defn- fmt [x]
  (if (nil? x) "—" (str x)))

(defn render-batch-brief [report]
  (let [lines
        (atom
         [(str "MORNING BRIEF — " (:batch-id report))
          (str "Attempts: " (:attempt-count report)
               " | fully grounded: " (:fully-grounded-count report)
               " | partial authored: " (:partial-authored-count report)
               " | no achievement: " (:no-achievement-count report))
          (str "Pending QA objectives: " (:pending-count report))
          ""])
        add! #(swap! lines conj %)]
    (doseq [[index item] (map-indexed vector (:items report))]
      (let [selection (:selection-review item)
            achievement (:achievement item)
            failure (:failure item)]
        (add! (str (inc index) ". " (:attempt-id item)
                   " — " (fmt (:selected-target item))))
        (add! (str "   Outcome: " (fmt (:outcome item))
                   " | achievement: " (fmt (:tier achievement))))
        (add! (str "   Expected: "
                   (fmt (get-in selection [:selected-action :rationale]))))
        (add! (str "   Actual: " (fmt (:summary achievement))))
        (add! (str "   Author/reviewer: " (fmt (:author item))
                   " / " (fmt (:reviewer item))
                   " | commit: " (fmt (:commit item))))
        (when failure
          (add! (str "   STOP THE LINE: " (fmt (:kind failure))
                     " at " (fmt (:stage failure))))
          (add! (str "   Error: " (fmt (:error failure))))
          (add! (str "   Repair: " (fmt (:repair-id failure))
                     " requires "
                     (fmt (get-in failure [:discharge-contract :requires])))))
        (doseq [obligation (:repair-history item)]
          (add! (str "   Repair history: " (:repair/id obligation)
                     " [" (name (:repair/class obligation)) "]"
                     (if (:repair/resolution obligation)
                       " resolved"
                       (if (:repair/implementation obligation)
                         " awaiting production validation"
                         " open"))))
          (add! (str "   Cause: " (fmt (:failure-kind obligation))
                     " at " (fmt (:failure-stage obligation))))
          (when-let [commit (or (get-in obligation
                                        [:repair/resolution :replacement-commit])
                                (get-in obligation
                                        [:repair/implementation
                                         :replacement-commit]))]
            (add! (str "   Recovered/replacement commit: " commit))))
        (add! "   QA objectives:")
        (doseq [objective (:pending-objectives item)
                :let [spec (get brief/objective-specs objective)]]
          (add! (str "     - " (name objective) ": " (:question spec)))
          (add! (str "       answers " (vec (sort (:answers spec)))))
          (add! (str "       use: " (:use spec)))
          (add! (str "       submit: clojure -M:wm-full-loop qa "
                     (:attempt-id item) " " (name objective)
                     " ANSWER \"NOTE\" joe")))
        (add! "")))
    (str/join "\n" @lines)))

(def usage
  (str "War Machine real full-loop runner\n\n"
       "  status\n"
       "  activate\n"
       "  canary [--out PATH] [agent options]\n"
       "  once [--author zai-5 --reviewer codex-7 --repair-reviewer codex-1]\n"
       "  tick [--author zai-5 --reviewer codex-7 --repair-reviewer codex-1]\n"
       "  continuous [--count N] [--interval-seconds N] [agent options]\n"
       "  brief [--batch-id ID] [--format text|edn]\n"
       "  qa ATTEMPT-ID OBJECTIVE ANSWER NOTE [REVIEWER]\n\n"
       "once is an on-demand durée click; continuous emits sequential durée "
       "clicks; tick is one wall-clock opportunity. Agent options also include "
       "--agent-budget-seconds. Machine-repair reviews route to the visible "
       "--repair-reviewer role (default codex-1 Ground Control), while ordinary "
       "work uses --reviewer. Trip escalation is an explicit operator choice: "
       "--tripwire-action record|stop-line|park-and-summon (default: record). "
       "The brief lists each objective's allowed answers."))

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
      "brief" (let [flags (option-map rest)]
                (if-let [batch-id (:batch-id flags)]
                  (let [report (batch-brief batch-id)]
                    (if (= "edn" (:format flags))
                      (print-value report)
                      (println (render-batch-brief report))))
                  (let [record (trace/latest-trace-record)]
                    (print-value {:pending (brief/pending-items)
                                  :reviews (brief/reviews)
                                  :valid-belief-entity-ids
                                  (vec (sort-by str (keys (:mu-post record))))}))))
      "qa" (let [[attempt-id objective answer note reviewer & extra] rest]
             (when (or (seq extra) (some nil? [attempt-id objective answer note]))
               (throw (ex-info "qa requires ATTEMPT OBJECTIVE ANSWER NOTE [REVIEWER]"
                               {:args rest})))
             (print-value (brief/review! attempt-id (keyword objective) (keyword answer)
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
