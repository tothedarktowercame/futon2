;; Produce Q4 run-fact exports through the real runner and the Lean terms
;; wm_check_run_facts generates from them: a single run, a second run with
;; Q6's exclusion, a mixed-horizon run, and the second run's record with one
;; receipt's information-gain term removed and the carrier recomputed (the
;; violation witness). Run from the futon2 worktree; Q4_LEAN_OUT names the
;; output directory.
(require 'futon2.aif.wm.q4-preference-route-test '[cheshire.core :as json] 'wm-check-run-facts
         'clojure.string 'futon2.aif.load-identity)
(in-ns 'futon2.aif.wm.q4-preference-route-test)
(require '[cheshire.core :as json] 'wm-check-run-facts)
(defn emit [out label record]
  (let [export (json/parse-string (json/generate-string (facts/facts-for-record record "r" {})))
        path (str out "/Q4" label ".lean")]
    (spit path (wm-check-run-facts/lean-source export (str "q4" label)))
    (println label (holds? record) path)))
(def out (or (System/getenv "Q4_LEAN_OUT") "test/fixtures/q4-lean"))
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
                   dir (route/temp-dir "q4-lean-")
                   one (:record (click! fx dir))
                   two (:record (click! fx dir))
                   mixed (:record (click! (route/fixture ["p/a" "p/c"]) (route/temp-dir "q4-lean-")))]
               (emit out "Single" one)
               (emit out "Excluded" two)
               (emit out "MixedHorizons" mixed)
               (emit out "TermMissing"
                     (reseal two #(assoc-in (vec %) [0 :g-terms :information-gain] nil))))))))))))
