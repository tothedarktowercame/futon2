;; Produce the Q6 run-fact exports through the real runner (two clicks on one
;; run stream: the second repeats after a refusal on the same inputs) and the
;; Lean terms wm_check_run_facts generates from them. Run from the futon2
;; worktree; Q6_LEAN_OUT names the output directory.
(require 'futon2.aif.wm.q6-repeat-after-refusal-test '[cheshire.core :as json] 'wm-check-run-facts)
(in-ns 'futon2.aif.wm.q6-repeat-after-refusal-test)
(require '[cheshire.core :as json] 'wm-check-run-facts 'clojure.string 'futon2.aif.load-identity)
(defn emit [out label run]
  (let [export (json/parse-string (json/generate-string
                                   (facts/facts-for-record (:record run) "r" snap)))
        path (str out "/Q6" label ".lean")]
    (spit path (wm-check-run-facts/lean-source export (str "q6" label)))
    (println label (select-keys (get export "facts")
                                ["previousOutcome" "previousInputDigest" "currentInputDigest"])
             path)))
(def out (or (System/getenv "Q6_LEAN_OUT") "test/fixtures/q6-lean"))
(def worktree (str (System/getProperty "user.dir") "/"))
;; the runner's drift guard compares this checkout against itself
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
                 dir (route/temp-dir "q6-lean-")
                 one (click! fx dir)
                 two (click! fx dir)]
             (emit out "First" one)
             (emit out "Repeat" two))))))))))
