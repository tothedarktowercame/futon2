(require '[futon2.aif.machine-slow-feedback-evidence :as e]
 '[futon2.aif.machine-slow-feedback-evidence-test :as t])
(let [cfg (#'t/fixture (fn [rs]
 (let [changed (assoc-in rs [:outcome :terminal/at] "2026-09-13T04:10:00Z")]
  (assoc-in changed [:application-ledger :entries 0 :input/digests :outcome]
   (#'t/vd (:outcome changed))))))
 out (e/verify-feedback cfg)]
 (assert (true? (:replay/identical? out)))
 (prn {:control :changed-terminal-time-under-unchanged-review :replay (:replay/identical? out)}))
