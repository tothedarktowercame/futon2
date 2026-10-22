(require '[futon2.aif.wm.frozen-cascade-steps :as s]
         '[clojure.pprint :as pp])
(let [base "holes/labs/wm-contract/runs/"
      old (s/history (str base "frozen-cascade-steps-2026-10-22"))
      root (str base "frozen-cascade-verified-2026-10-22")
      done (s/history root)
      wired (:payload (nth done 4))
      prepared (:payload (last done))
      again (s/advance! root)
      result {:schema :wm/frozen-sections-verification-v1
              :prefix-unchanged (= (subvec old 0 3) (subvec done 0 3))
              :stages (mapv :stage done)
              :digests (mapv :digest done)
              :wiring-status (:status wired)
              :box-count (count (get-in wired [:wiring :boxes]))
              :policy-hole-count (count (get-in wired [:fold-output :policy-holes]))
              :operation (get-in prepared [:requests 0 :clause :operation])
              :pattern (get-in prepared [:requests 0 :pattern])
              :completion-status (get-in wired [:wiring :boxes 0 :conditions 0 :status])
              :completed-restart-reused (:reused again)
              :task-executed (:task-executed prepared)
              :tests [{:namespace 'futon2.aif.wm.compiled-cascade-construction-test :tests 2 :assertions 23 :failures 0 :errors 0}
                      {:namespace 'futon2.aif.wm.cascade-continuation-test :tests 5 :assertions 17 :failures 0 :errors 0}
                      {:namespace 'futon2.aif.wm.cascade-runner-seam-test :tests 7 :assertions 70 :failures 0 :errors 0}]}]
  (assert (:prefix-unchanged result))
  (assert (= :wired (:wiring-status result)))
  (assert (= 6 (count done)))
  (assert (= 1 (:box-count result)))
  (assert (= 0 (:policy-hole-count result)))
  (assert (true? (:completed-restart-reused result)))
  (assert (false? (:task-executed result)))
  (spit (str root "/verification.edn") (with-out-str (pp/pprint result)))
  (prn (dissoc result :digests :tests)))
