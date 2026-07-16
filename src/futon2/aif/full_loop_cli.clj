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

(declare attempt-brief render-attempt-brief)

(defn- run-once! [trigger flags]
  (let [result (runner/run-opportunity! (runner-opts trigger flags))]
    (print-value result)
    (when (:morning-brief-ref result)
      (println)
      (println (render-attempt-brief (attempt-brief (:attempt-id result)))))
    result))

(defn- selected-target [result]
  (get-in result [:checkpoints :selection :judgment :selected-mission]))

(defn- normalize-brief-item [item]
  (assoc item
         :achievement
         (or (:achievement item)
             {:tier :legacy-unverified
              :summary
              "Legacy item: no structured achievement summary was recorded"})
         :repair-history
         (repair/obligation-history (:attempt-id item))))

(defn- brief-item-view [item]
  (-> (select-keys item [:attempt-id :selected-target :outcome
                         :author :reviewer :commit :queued-at
                         :selection-review :achievement :failure
                         :repair-history :qa-targets :pending-objectives])
      (assoc :witness
             (select-keys (:witness item)
                          [:resolved? :dial-moved?
                           :implementation-id :discharge-id]))))

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
        items (->> (brief/items)
                   (filter #(= batch-id (:batch-id %)))
                   (sort-by :queued-at)
                   (mapv #(brief/with-pending-objectives % all-reviews))
                   (mapv normalize-brief-item)
                   vec)
        attempt-ids (set (map :attempt-id items))
        reviews (->> all-reviews
                     (filter #(contains? attempt-ids (:attempt-id %)))
                     (sort-by :reviewed-at)
                     vec)
        item-view brief-item-view]
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

(defn attempt-brief
  "Build the operator QA view for one attempt. The raw queue item remains the
  append-only source of truth; this view deliberately contains only evidence
  needed to make the pending judgments."
  [attempt-id]
  (let [all-reviews (brief/reviews)
        item (some #(when (= attempt-id (:attempt-id %)) %)
                   (brief/items))]
    (when-not item
      (throw (ex-info "Unknown Morning Brief attempt"
                      {:attempt-id attempt-id})))
    {:morning-brief/schema-version 4
     :attempt (-> item
                  (brief/with-pending-objectives all-reviews)
                  normalize-brief-item
                  brief-item-view)
     :reviews (->> all-reviews
                   (filter #(= attempt-id (:attempt-id %)))
                   (sort-by :reviewed-at)
                   vec)}))

(defn- fmt [x]
  (if (nil? x) "—" (str x)))

(defn- selected-rank [selection]
  (let [selected (:selected-action selection)]
    (or (some (fn [{:keys [rank action]}]
                (when (= selected action) rank))
              (:ranked-candidates selection))
        (get-in selection [:selection-reasons :rank]))))

(defn- candidate-label [{:keys [rank action G-efe]}]
  (str "#" rank " " (or (:target action) (:type action))
       " (G=" (fmt G-efe) ")"))

(defn- artifact-repository [item]
  (or (get-in item [:achievement :adjudication :after
                    :implementation-entity :props
                    :implementation/repository])
      (get-in item [:achievement :adjudication :after
                    :implementation-entity :props :repository])))

(defn- inspection-lines [item objective]
  (let [selection (:selection-review item)
        candidates (:ranked-candidates selection)
        rank (selected-rank selection)
        repo (artifact-repository item)
        commit (:commit item)
        artifacts (get-in item [:achievement :build :artifacts])
        validation (get-in item [:achievement :build :validation])
        witness (:witness item)]
    (case objective
      :selection-quality
      [(str "Evidence: selected rank " (fmt rank) " of " (count candidates)
            "; selected " (fmt (:selected-target item)) ".")
       (str "Compare: "
            (if (seq candidates)
              (str/join "; " (map candidate-label (take 3 candidates)))
              "no ranked alternatives were recorded"))
       "Look for: whether the chosen mission was a defensible use of this click, especially if a lower-G alternative ranked above it; distinguish justified exploration from accidental or stale preference."
       (str "Mission record: "
            (fmt (get-in selection [:selected-action :mission-path])))]

      :substantive-achievement
      [(str "Inspect: git -C " (fmt repo) " show --stat --oneline " (fmt commit))
       (str "Inspect the patch: git -C " (fmt repo) " show --format=fuller "
            (fmt commit) " -- " (str/join " " artifacts))
       (str "Changed artifacts: " (if (seq artifacts)
                                     (str/join ", " artifacts)
                                     "none recorded"))
       "Look for: an observable behavior or capability change in the selected mission, not merely prose, generated cargo, or a claim that work could be done. Check that the mission record describes the same boundary as the code."
       (str "Recorded result: " (fmt (get-in item [:achievement :summary])))]

      :evidence-sufficiency
      [(str "Review evidence: job " (fmt (:review-job validation))
            ", approved=" (fmt (:approved? validation))
            ", author/reviewer=" (fmt (:author item)) "/" (fmt (:reviewer item)) ".")
       (str "Artifact binding: repository=" (fmt repo)
            ", commit=" (fmt commit)
            ", fresh=" (fmt (get-in validation [:artifact-binding :fresh-author?]))
            ", descendant=" (fmt (get-in validation [:artifact-binding :descendant?]))
            ".")
       (str "Grounding: resolved=" (fmt (:resolved? witness))
            ", dial-moved=" (fmt (:dial-moved? witness))
            ", implementation=" (fmt (:implementation-id witness)) ".")
       "Look for: meaningful tests of the changed behavior, independent review by someone other than the author, the observed commit in the intended repository, and a grounding witness that names the same implementation. Do not accept a green label without those links."
       "If validation claims cannot be reproduced from the commit or review job, answer insufficient even though the loop completed." ]

      :machine-response
      [(str "Failure: " (fmt (get-in item [:failure :kind]))
            " at " (fmt (get-in item [:failure :stage])) ".")
       (str "Repair obligation: " (fmt (get-in item [:failure :repair-id]))
            "; requires "
            (fmt (get-in item [:failure :discharge-contract :requires])) ".")
       "Look for: fail-closed behavior, an append-only remembered obligation, a causal backtrace, and a discharge contract that would prove the fault repaired."
       "Answer incorrect if ordinary mission work could proceed ahead of an unresolved blocking repair."]
      [])))

(defn render-attempt-brief
  "Render one attempt as a readable operator QA document."
  [{:keys [attempt reviews]}]
  (let [lines (atom [(str "MORNING BRIEF QA — " (:attempt-id attempt))
                     (str "Target: " (fmt (:selected-target attempt)))
                     (str "Outcome: " (fmt (:outcome attempt))
                          " | achievement: "
                          (fmt (get-in attempt [:achievement :tier])))
                     (str "Author/reviewer: " (fmt (:author attempt))
                          " / " (fmt (:reviewer attempt)))
                     (str "Commit: " (fmt (:commit attempt)))
                     (str "Queued: " (fmt (:queued-at attempt)))
                     ""])
        add! #(swap! lines conj %)
        reviewed-by-objective (into {} (map (juxt :objective identity) reviews))]
    (add! "WHAT HAPPENED")
    (add! (str "  Expected: "
               (fmt (get-in attempt [:selection-review :selected-action
                                     :rationale]))))
    (add! (str "  Actual: " (fmt (get-in attempt [:achievement :summary]))))
    (add! "")
    (add! "OPERATOR QA")
    (doseq [objective (brief/item-objectives attempt)
            :let [spec (get brief/objective-specs objective)
                  prior (get reviewed-by-objective objective)]]
      (add! (str "  " (name objective)))
      (add! (str "    Question: " (:question spec)))
      (doseq [line (inspection-lines attempt objective)]
        (add! (str "    " line)))
      (if prior
        (do
          (add! (str "    ANSWERED " (:answer prior) " by " (:reviewer prior)))
          (add! (str "    Note: " (:note prior))))
        (do
          (add! (str "    Answers: " (vec (sort (:answers spec)))))
          (add! (str "    Submit here: clojure -M:wm-full-loop review "
                     (:attempt-id attempt) " joe"))))
      (add! ""))
    (add! "SUBMISSION")
    (if (seq (:pending-objectives attempt))
      (add! (str "  Run `clojure -M:wm-full-loop review "
                 (:attempt-id attempt)
                 " joe` for a guided questionnaire. Answers are appended "
                 "to the immutable Morning Brief review store."))
      (add! "  All applicable objectives have been answered."))
    (str/join "\n" @lines)))

(defn review-interactively!
  "Prompt for every still-pending objective and append validated answers."
  [attempt-id reviewer]
  (let [{:keys [attempt]} (attempt-brief attempt-id)
        objectives (:pending-objectives attempt)]
    (if (empty? objectives)
      (println "All applicable QA objectives have already been answered.")
      (doseq [objective objectives
              :let [spec (get brief/objective-specs objective)]]
        (println)
        (println (str (name objective) " — " (:question spec)))
        (doseq [line (inspection-lines attempt objective)]
          (println (str "  " line)))
        (println (str "Allowed answers: " (vec (sort (:answers spec)))))
        (print "Answer: ")
        (flush)
        (let [answer (some-> (read-line) str/trim keyword)]
          (when-not (contains? (:answers spec) answer)
            (throw (ex-info "Unknown Morning Brief answer"
                            {:objective objective :answer answer
                             :allowed (:answers spec)})))
          (print "Evidence note: ")
          (flush)
          (let [note (some-> (read-line) str/trim)]
            (when (str/blank? note)
              (throw (ex-info "Morning Brief evidence note must not be blank"
                              {:objective objective})))
            (brief/review! attempt-id objective answer note reviewer)
            (println "Recorded.")))))))

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
       "  brief [--attempt-id ID | --batch-id ID] [--format text|edn]\n"
       "  review ATTEMPT-ID [REVIEWER]\n"
       "  qa ATTEMPT-ID OBJECTIVE ANSWER NOTE [REVIEWER]\n\n"
       "once is an on-demand durée click; continuous emits sequential durée "
       "clicks; tick is one wall-clock opportunity. Agent options also include "
       "--agent-budget-seconds. Machine-repair reviews route to the visible "
       "--repair-reviewer role (default codex-1 Ground Control), while ordinary "
       "work uses --reviewer. Trip escalation is an explicit operator choice: "
       "--tripwire-action record|stop-line|park-and-summon (default: record). "
       "An attempt brief explains what to inspect and `review` provides the "
       "guided answer-entry surface. `qa` is the non-interactive primitive."))

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
                (when (and (:attempt-id flags) (:batch-id flags))
                  (throw (ex-info "brief accepts either --attempt-id or --batch-id"
                                  {:flags flags})))
                (cond
                  (:attempt-id flags)
                  (let [report (attempt-brief (:attempt-id flags))]
                    (if (= "edn" (:format flags))
                      (print-value report)
                      (println (render-attempt-brief report))))

                  (:batch-id flags)
                  (let [report (batch-brief (:batch-id flags))]
                    (if (= "edn" (:format flags))
                      (print-value report)
                      (println (render-batch-brief report))))

                  :else
                  (let [record (trace/latest-trace-record)]
                    (print-value {:pending (brief/pending-items)
                                  :reviews (brief/reviews)
                                  :valid-belief-entity-ids
                                  (vec (sort-by str (keys (:mu-post record))))}))))
      "review" (let [[attempt-id reviewer & extra] rest]
                 (when (or (seq extra) (nil? attempt-id))
                   (throw (ex-info "review requires ATTEMPT-ID [REVIEWER]"
                                   {:args rest})))
                 (review-interactively! attempt-id (or reviewer "joe")))
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
