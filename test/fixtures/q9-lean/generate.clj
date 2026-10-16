;; Produce Q9 run-fact exports through the real runner (the Q8 route's two
;; clicks on one run stream) and the Lean terms wm_check_run_facts generates
;; from them: the production C (Q9 violated), and the carrier missing,
;; tampered or around a self-asserted factorized carrier (Q9 not
;; recomputable). Run from the futon2 worktree; Q9_LEAN_OUT names the
;; output directory.
(require 'futon2.aif.wm.q9-factorization-route-test 'clojure.string 'futon2.aif.load-identity)
(in-ns 'futon2.aif.wm.q9-factorization-route-test)
(require 'wm-check-run-facts)
(defn emit [out label record]
  (let [export (json/parse-string (json/generate-string
                                   (facts/facts-for-record record "r" {})))
        path (str out "/Q9" label ".lean")]
    (spit path (wm-check-run-facts/lean-source export (str "q9" label)))
    (println label (:status (q9/verify-record record)) path)))
(def out (or (System/getenv "Q9_LEAN_OUT") "test/fixtures/q9-lean"))
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
                   dir (route/temp-dir "q9-lean-")
                   one (:record (click! fx dir))
                   two (:record (click! fx dir))]
               (emit out "First" one)
               (emit out "Repeat" two)
               (emit out "Missing" (dissoc two :q9))
               (emit out "Tampered" (update-in two [:q9 :reasons] pop))
               (emit out "SelfAsserted"
                     (reseal two #(assoc-in % [:decision :factorized-preference]
                                            positive-carrier))))))))))))
