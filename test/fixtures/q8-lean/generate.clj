;; Produce Q8 run-fact exports through the real runner (two clicks on one run
;; stream, the outer selection by select-live in live mode; the second click
;; refreshes against the first's frontier and carries Q6's exclusion) and the
;; Lean terms wm_check_run_facts generates from them. Run from the futon2
;; worktree; Q8_LEAN_OUT names the output directory.
(require 'futon2.aif.wm.q8-frontier-route-test '[cheshire.core :as json] 'wm-check-run-facts
         'clojure.string 'futon2.aif.load-identity)
(in-ns 'futon2.aif.wm.q8-frontier-route-test)
(require '[cheshire.core :as json] 'wm-check-run-facts)
(defn emit [out label run]
  (let [export (json/parse-string (json/generate-string
                                   (facts/facts-for-record (:record run) "r" {})))
        path (str out "/Q8" label ".lean")]
    (spit path (wm-check-run-facts/lean-source export (str "q8" label)))
    (println label (q8-holds? (:record run)) path)))
(def out (or (System/getenv "Q8_LEAN_OUT") "test/fixtures/q8-lean"))
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
                   dir (route/temp-dir "q8-lean-")
                   one (click! fx dir)
                   two (click! fx dir)
                   changed (click! fx dir (assoc base-tasks "M-third" 6) base-map)]
               (emit out "First" one)
               (emit out "Repeat" two)
               (emit out "Changed" changed))))))))))
