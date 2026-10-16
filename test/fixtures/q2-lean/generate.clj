;; Produce Q2 run-fact exports through the real runner (the Q8 route's two
;; clicks on one run stream, select-live in live mode, then cases Q2
;; refuses) and the Lean terms wm_check_run_facts generates from them. Run
;; from the futon2 worktree; Q2_LEAN_OUT names the output directory.
(require 'futon2.aif.wm.q2-construction-route-test '[cheshire.core :as json] 'wm-check-run-facts
         'clojure.string 'clojure.java.io 'futon2.aif.load-identity)
(in-ns 'futon2.aif.wm.q2-construction-route-test)
(require '[cheshire.core :as json] 'wm-check-run-facts)
(defn emit [out label record]
  (let [export (json/parse-string (json/generate-string
                                   (facts/facts-for-record record "r" {})))
        path (str out "/Q2" label ".lean")]
    (spit path (wm-check-run-facts/lean-source export (str "q2" label)))
    (println label (q2-holds? record) path)))
(def out (or (System/getenv "Q2_LEAN_OUT") "test/fixtures/q2-lean"))
(def worktree (str (System/getProperty "user.dir") "/"))
(with-redefs [futon2.aif.load-identity/required-sources
              (into {} (map (fn [[n p]] [n (clojure.string/replace p "/home/joe/code/futon2/" worktree)]))
                    futon2.aif.load-identity/required-sources)]
  (futon2.aif.hermetic-repair-fixture/with-hermetic-stores
   (fn []
     (futon2.test-support.runner-fixture/with-hermetic-traces
      (fn []
        (binding [futon2.aif.full-loop-runner/*wm-status-reporting?* false]
          (route/with-empty-learning-ledger
           (fn []
             (let [fx (route/fixture)
                   dir (route/temp-dir "q2-lean-")
                   one (:record (click! fx dir))
                   two (:record (click! fx dir))
                   changed (route/fixture)
                   _ (spit (clojure.java.io/file (:library changed) "p/b.flexiarg") "edited\n")
                   off (let [a {:type :advance-mission :target "M-off"}]
                         {:task "M-off" :action a
                          :action-id (futon2.aif.interpretation-evidence/value-digest a)})]
               (emit out "First" one)
               (emit out "Repeat" two)
               (emit out "Steiner" (certified-record (route/fixture ["p/a" "p/c"])))
               (emit out "LibraryChanged" (certified-record changed))
               (emit out "ExcludedTaskAction"
                     (reseal-outer two #(update % :actions conj off))))))))))))
