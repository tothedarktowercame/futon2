(ns futon2.aif.terminal-receipt-commit-test
  "PROOF-2b (click 13, tick-run-record-2026-09-30-1790737908): the click
  closed :grounded-change and its terminal action receipt said :commit nil,
  although the author seat committed futon7 891001d. The receipt's commit now
  comes from the record's :grounded-commit (what the close observed — the
  artifact binding's repo and authored commit, which may live outside
  futon2), falls back to a bare {:sha} from the d-task-enactment revision
  pair, and is a typed absence — never nil — when the result names no commit."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.wm.terminal-receipt :as terminal-receipt]))

(def ^:private selected-record
  {:click/id "wm-click-13"
   :selection-event {:event :cascade-selected :target "M-self-documenting-stack"}
   :decision {:selection-law {:per-policy-argmax {:action {:kind :cascade-candidate}}}
              :g-term-decomposition {:schema :wm/g-term-decomposition-v1 :status :missing}}
   :outcome :grounded-change
   :participants {:roles {:reviewer-of-record {:identity "codex-proof2e"}}}})

(def futon7-sha "891001d0aa11bb22cc33dd44ee55ff66aabbccdd")

(deftest a-grounded-commit-in-another-repo-names-repo-and-sha
  (let [receipt (terminal-receipt/terminal-receipt
                 (assoc selected-record :grounded-commit
                        {:repo "futon7" :sha futon7-sha}))]
    (is (= :action-receipt (:kind receipt)))
    (is (= {:repo "futon7" :sha futon7-sha} (:commit receipt)))))

(deftest the-close-observes-the-other-repos-commit
  ;; the derivation persist-run-record! runs: the close map's artifact
  ;; binding, not the (refused) d-task verification, is the commit's source
  (let [result {:outcome :grounded-change
                :data {:commit futon7-sha
                       :artifact-binding {:repo "futon7" :commit futon7-sha}}}]
    (is (= {:repo "futon7" :sha futon7-sha}
           (runner/grounded-commit-for result)))
    (is (= {:repo "futon7" :sha futon7-sha}
           (:commit (terminal-receipt/terminal-receipt
                     (assoc selected-record
                            :grounded-commit (runner/grounded-commit-for result))))))))

(deftest a-revision-pair-alone-is-a-bare-sha
  (let [receipt (terminal-receipt/terminal-receipt
                 (assoc-in selected-record
                           [:d-task-enactment :verification :revision-pair :after]
                           "40762056b2636d94c3b6aa9e416e9c9065705261"))]
    (is (= {:sha "40762056b2636d94c3b6aa9e416e9c9065705261"}
           (:commit receipt)))))

(deftest no-commit-anywhere-is-a-typed-absence-never-nil
  (let [receipt (terminal-receipt/terminal-receipt selected-record)]
    (is (= {:absent :no-grounded-commit} (:commit receipt)))
    (is (nil? (runner/grounded-commit-for {:outcome :grounded-no-change :data {}})))))
