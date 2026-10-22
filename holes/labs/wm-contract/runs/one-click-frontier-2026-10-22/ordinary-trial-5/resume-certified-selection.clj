(require 'futon2.aif.wm.debugger 'futon2.aif.selected-outer-cascade-policy
         'futon2.aif.meta-live-outer-selector 'futon2.aif.full-loop-runner)
;; Operational continuation of the exact held computation, not a new selection.
;; Belief status is inferred only from the recorded legacy refusal branch:
;; learning-certificate checked :produced before emitting shadow-not-consumed.
(let [rid "2026-10-22-02587641-d615-4a5b-8bc8-a3df48e2ab45"
      s (first (filter #(= rid (:run-id %)) (futon2.aif.wm.debugger/stopped)))
      _ (assert (= :selection (:phase s)))
      _ (assert (= :wm/outer-learning-not-consumed (get-in s [:condition :kind])))
      j (get-in s [:ex-data :judgement])
      old (get-in j [:outer-task-selection :outer-learning])
      _ (assert (= :refused (:status old)))
      _ (assert (= :outer-learning-shadow-not-consumed (:reason old)))
      _ (assert (= :live (:mode old)))
      p (get-in j [:outer-task-selection :policy])
      c (:selected-outer-cascade-policy p)
      _ (assert (= "M-stack-geometry/policy-c7d7518e1e185c2e2318" (:policy-id c)))
      _ (assert (= "5dd8de3b28b5654bc0d16fe8f85e1fad33676cbee75379ac8dbf7ae41bac1da7" (get-in c [:carrier-pin :sha256])))
      _ (assert (nil? (futon2.aif.selected-outer-cascade-policy/verify c)))
      cert (futon2.aif.meta-live-outer-selector/ordinary-learning-certificate
            {:outer-learning {:mode (:mode old) :belief {:status :produced}
                              :controller {:re-arm-condition (:re-arm-condition old)}}
             :carrier c :policy-evidence (:policy-evidence p)
             :policy-objective (:policy-objective p)
             :provider-source-pin (:evidence-provider-source-pin p)})
      _ (assert (= :ordinary-provider-consumed (:status cert)))
      fixed (assoc-in j [:outer-task-selection :outer-learning] cert)
      _ (assert (= j (assoc-in fixed [:outer-task-selection :outer-learning] old)))
      _ (assert (= (:decision j) (:decision fixed)))
      condition (futon2.aif.full-loop-runner/selection-terminal-condition :selection fixed)
      _ (assert (or (nil? condition) (= :wm/outer-selection-complete (:kind (ex-data condition)))))
      receipt {:run-id rid :old-certificate old :certificate cert
               :unchanged-except-certificate true :carrier-verified true
               :terminal-condition (some-> condition ex-data :kind)
               :authority-projection {:belief-status :produced
                                      :evidence :legacy-refusal-branch-order}
               :replay-source :exact-held-in-memory-judgement
               :at (str (java.time.Instant/now))}]
  (spit "/home/joe/code/futon2/holes/labs/wm-contract/runs/one-click-frontier-2026-10-22/ordinary-trial-5/certified-selection-resume-receipt.edn" (pr-str receipt))
  (futon2.aif.wm.debugger/continue! rid [:use-value fixed]))
