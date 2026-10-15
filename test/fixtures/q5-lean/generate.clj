;; Produce the run-fact exports for an issued selected reading and an
;; unpinned one through the real runner, and the Lean terms wm_check_run_facts
;; generates from them. Run from the futon2 worktree with run_redirected.clj loaded.
(require 'futon2.aif.wm.selected-reading-runner-test '[cheshire.core :as json] 'wm-check-run-facts 'clojure.java.io)
(in-ns 'futon2.aif.wm.selected-reading-runner-test)
(require '[cheshire.core :as json] 'wm-check-run-facts)
(defn emit [label lib ask]
  (let [d (selected-decision (atom 0))
        {:keys [record]} (run-click d lib ask (atom 0))
        export (json/parse-string (json/generate-string (facts/facts-for-record record "r" snap)))
        out (str (System/getProperty "user.home") "/notes/q5/lean/Q5" label ".lean")]
    (spit out (wm-check-run-facts/lean-source export (str "q5" label)))
    (println label (get-in export ["facts" "interpretationOrder"]) out)))
(with-redefs [futon2.aif.learning-trial-ledger/default-root (.getPath (temp-dir "q5-lean-ledger"))]
  (futon2.aif.hermetic-repair-fixture/with-hermetic-stores
   (fn []
     (futon2.test-support.runner-fixture/with-hermetic-traces
      (fn []
        (binding [futon2.aif.full-loop-runner/*wm-status-reporting?* false]
          (let [lib (library)]
            (emit "Issued" lib #(answer % lib "accepted")))
          (let [lib (library)]
            (clojure.java.io/delete-file (clojure.java.io/file lib "p/b.flexiarg"))
            (emit "Unpinned" lib (fn [_] {})))))))))
