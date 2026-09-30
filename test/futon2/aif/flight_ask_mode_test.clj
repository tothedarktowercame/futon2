(ns futon2.aif.flight-ask-mode-test
  "Answer-only asks must dispatch in mode brief. The defect (clicks 15 and
  17, jobs invoke-1790743384970-28309-49fea8eb and
  invoke-1790746850452-28531-894dee3f): interpretation asks were belled in
  mode work, and an interpretation answer is a reply with no tool use, so
  Agency's work-mode no-execution gate (futon3c transport/http.clj
  codex-task-no-execution?, which fires only for work mode) failed the job
  with terminal-code no-execution-evidence.

  Mode choice justification, futon3c/src/futon3c/transport/http.clj:
    (def ^:private invoke-job-modes #{\"work\" \"brief\"})
    ;; invoke-job-mode: the DEFAULT classification is \"brief\" —
    (if (or (re-find #\"(?i)\\bmode:\\s*task\\b\" p) ...) \"work\" \"brief\")
    ;; codex-task-no-execution?: gated on work-mode? —
    (and enforced? ... work-mode? ... (not executed) (zero? tool-events) ...)

  so \"brief\" is Agency's answer-only mode: the execution gate cannot fire."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight-runner :as fr]))

(defn- ask-with
  "Run one agency-answer-fn ask against a capturing stub dispatch; returns
  the opts the stub received and the prompt."
  [& {:keys [kind]}]
  (let [seen (atom nil)
        answer (fr/agency-answer-fn
                {:seat "claude-5"
                 :dispatch! (fn [opts seat caller mission prompt]
                              (reset! seen {:opts opts :seat seat
                                            :caller caller :mission mission
                                            :prompt prompt})
                              {:job-id "job-ask-1"})
                 :prompt-fn (constantly "the want-interpretation prompt")
                 :poll! (constantly {:state "done" :result "a reading"})})]
    (answer {:target "M-futon-seams" :request-id "req-17" :kind (or kind :interpretation)})
    @seen))

(deftest interpretation-ask-dispatches-in-mode-brief
  (let [seen (ask-with)]
    (is (= "brief" (:invoke-mode (:opts seen)))
        "the ask is answer-only work: Agency's brief mode, whose replies are
         not subject to the no-execution-evidence gate")
    (is (str/starts-with? (:prompt seen)
                          "Requisition: M-futon-seams — War Machine interpretation request req-17")
        "the requisition line is unchanged")
    (is (= "wm-flight" (:caller seen)))
    (is (= "claude-5" (:seat seen)))))

(deftest reading-ask-also-dispatches-in-mode-brief
  (let [seen (ask-with :kind :reading)]
    (is (= "brief" (:invoke-mode (:opts seen))))))

(deftest enactment-step-dispatch-stays-mode-work
  (testing "agency-dispatch-step! passes opts through unchanged, so
            full-loop-runner/dispatch! defaults :mode to \"work\""
    (let [seen (atom nil)
          clock (atom 0)]
      ((fr/agency-dispatch-step!
        {:seat "claude-5" :opts {:agency-base "http://fixture"}
        :roster-fn (constantly {:claude-5 {:status "idle" :invoke-ready? true}})
        :dispatch! (fn [opts seat _ mission prompt]
                     (reset! seen {:opts opts :seat seat :mission mission :prompt prompt})
                     {:job-id "job-step-1"})
        :read-job! (constantly {:state "done"
                                :result "```edn\n{:schema :wm/enactment-step-response-v1 :commit \"abc1234\" :produced :t :check {:class :fixture}}\n```"})
         :now-ms (fn [] @clock) :sleep! (fn [ms] (swap! clock + ms)) :poll-ms 1000})
       {:target "M-autoclock-in" :candidate :C1 :pattern :p/a :n 1 :phase :commit
        :interpretation {:produces #{:t}}})
      (let [sent @seen]
        (is (nil? (:invoke-mode (:opts sent)))
            "no :invoke-mode on the enactment path — runner/dispatch! sends
             :mode \"work\" (full_loop_runner.clj: (or invoke-mode \"work\"))")
        (is (str/starts-with? (:prompt sent)
                              "Requisition: M-autoclock-in — War Machine enactment step 1"))))))

(deftest plant-work-mode-ask-fails-the-mode-assertion
  ;; PLANT: the pre-fix call site passed opts through UNCHANGED (mode work
  ;; upstream, full_loop_runner/dispatch! hardcoding :mode \"work\"). Shown
  ;; here directly: the old call shape carries no :invoke-mode, so the
  ;; acceptance assertion (= \"brief\" (:invoke-mode opts)) fails against
  ;; it — if interpretation-ask-dispatches-in-mode-brief ever goes red,
  ;; the assoc in agency-answer-fn was lost.
  (let [seen (atom nil)
        dispatch! (fn [opts _seat _caller _mission _prompt]
                    (reset! seen opts) {:job-id "j"})]
    ;; the OLD call: opts untouched, exactly as before 2026-09-30
    (dispatch! {:agency-base "http://fixture"} "claude-5" "wm-flight"
               "M-futon-seams" "Requisition: …\n\nprompt")
    (is (not= "brief" (:invoke-mode @seen))
        "the old shape is mode work — this is the failing case the fix removes")))
