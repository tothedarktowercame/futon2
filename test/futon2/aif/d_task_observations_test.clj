(ns futon2.aif.d-task-observations-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.d-predecessor-task-authority-test :as fixture]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.action-identity :as identity]))

(def fixture-options
  {:universe #{["target" :artifact] ["target" :missing-file]
               ["target" :missing-decl] ["target" :missing-revision]
               ["target" :historical]}
   :locators {:missing-file {:class :C3 :repo "repo" :sha "HEAD" :path "absent.clj"}
              :missing-decl {:class :C4 :repo "repo" :sha "HEAD"
                             :path "created.clj" :decl "(defn absent"}
              :missing-revision {:class :C4 :repo "repo" :sha "not-a-revision"
                                 :path "created.clj" :decl "(ns created)"}
              :historical {:class :C3 :repo "repo" :sha "HEAD~1" :path "created.clj"}}})

(deftest signed-observations-do-not-change-v1
  (fixture/with-artifact fixture-options
    (fn [{:keys [inputs expected jobs]}]
      (let [record (task/claim inputs)
            old (task/verify record expected jobs)
            old-bytes (pr-str old)
            new (task/verify-observations-v2 record expected jobs)
            observed #(get-in new [:observations ["target" %] :artifact-observation :observed])]
        (is (= :admitted (:status old) (:status new)))
        (is (= old-bytes (pr-str (task/verify record expected jobs))))
        (is (= old (:execution-verification new)))
        (is (= #{["target" :missing-file] ["target" :missing-decl]} (:unknown old)))
        (is (= #{} (:absent old)))
        (is (false? (observed :missing-file)))
        (is (false? (observed :missing-decl)))
        (is (true? (observed :missing-revision)))
        (is (= :unknown-sha
               (get-in new [:observations ["target" :missing-revision]
                            :declared-revision-observation :observed :kind])))
        (is (true? (observed :historical)))
        (is (false? (get-in new [:observations ["target" :historical]
                                :declared-revision-observation :observed])))
        (doseq [[_ row] (:observations new)]
          (is (= {:status :held :reason :observation-placement-not-declared} (:schedule row)))
          (is (= (:meaning-sha256 row) (evidence/value-digest (:meaning row))))
          (is (= :not-authorized (:consumption row))))
        (is (= :independent-check-required (:causal-attribution new)))
        (is (= :not-authorized (:consumption new)))))))

(deftest unchanged-execution-gates-and-bound-domain
  (fixture/with-artifact fixture-options
    (fn [{:keys [inputs expected jobs]}]
      (let [record (task/claim inputs)
            check #(task/verify-observations-v2 record % jobs)]
        (is (= :carry-domain-changed (:kind (check (assoc expected :universe #{})))))
        (is (= :carry-occurrence-mismatch
               (:kind (check (assoc expected :carry-occurrence-id "other")))))
        (is (= :occurrence-mismatch
               (:kind (check (assoc-in expected [:occurrence :run/id] "other")))))
        (is (= :declaration-pins-mismatch
               (:kind (check (assoc expected :declaration-pins [])))))
        (is (= :independent-jobs-unestablished
               (:kind (task/verify-observations-v2 record expected
                                                  (assoc-in jobs ["review-job" :agent-id] "author")))))
        (is (= :after-token-evidence-mismatch
               (:kind (task/verify-observations-v2
                       (assoc-in record [:after-token-evidence 0 :result :observed] :forged)
                       expected jobs))))
        (is (= :deferred-artifact-not-fresh-execution
               (:kind (task/verify-observations-v2 (assoc record :route :recovery) expected jobs))))))))

(deftest all-negative-evidence-does-not-relax-execution-authority
  (fixture/with-artifact {:locator-path "absent.clj"}
    (fn [{:keys [inputs expected jobs]}]
      (is (= :after-token-evidence-unavailable
             (:kind (task/verify-observations-v2 (task/claim inputs) expected jobs)))))))

(deftest unbound-token-refuses-even-if-caller-and-dispatch-agree
  (fixture/with-artifact {:universe #{["target" :artifact] ["target" :invented]}}
    (fn [{:keys [inputs expected jobs]}]
      (let [record (task/claim inputs)]
        (is (= :admitted (:status (task/verify record expected jobs))))
        (is (= :observation-token-unbound
               (:kind (task/verify-observations-v2 record expected jobs))))))))

(deftest declared-schedule-and-unavailable-check-are-not-invented-observations
  (let [schedule {:tau {:value 2 :status :declared}}]
    (fixture/with-artifact
      {:observation-schedule schedule
       :universe #{["target" :artifact] ["target" :unsupported]}
       :locators {:unsupported {:class :C6 :repo "repo" :sha "HEAD" :path "created.clj"}}}
      (fn [{:keys [inputs expected jobs]}]
        (let [record (task/claim inputs)
              result (task/verify-observations-v2 record expected jobs)
              row (get-in result [:observations ["target" :unsupported]])]
          (is (= :admitted (:status result)))
          (is (= schedule (:schedule row)))
          (is (= (evidence/value-digest schedule) (:schedule-sha256 row)))
          (is (= {:status :missing :kind :revision-pair-reader-unavailable}
                 (get-in row [:artifact-observation :observed])))
          (is (= :revision-pair-invalid
                 (:kind (task/verify-observations-v2
                         (assoc-in record [:revision-pair :after] "other") expected jobs)))))))))


(deftest legacy-d-task-replays-the-matching-printer-without-caller-bindings
  (doseq [writer-mode [false true]]
    (identity/with-printer
     writer-mode
     #(fixture/with-artifact
       {:action {:kind :cascade-candidate :id :C0 :target "target"
                 :receipts {:example/receipt {:reading "bound"}}
                 :precedence [{:id :make-file :produces #{["target" :artifact]}}]}
        :occurrence-fn (fn [o] (assoc o :schema :wm/action-transition-occurrence-v1
                                     :action/value-sha256
                                     (identity/sha256 (identity/printed writer-mode (:action/value o)))))}
       (fn [{:keys [inputs expected jobs]}]
         (let [record (task/claim inputs)
               old (task/verify record expected jobs)
               result (identity/with-printer (not writer-mode)
                        (fn [] (task/verify-observations-v2 record expected jobs)))]
           (is (= :admitted (:status result)))
           (is (= old (:execution-verification result)))
           (is (= writer-mode
                  (get-in result [:occurrence-identity-verification :execution-print-namespace-maps])))
           (is (true? (get-in result [:observations ["target" :artifact] :artifact-observation :observed])))
           (is (= :independent-jobs-unestablished
                  (:kind (task/verify-observations-v2
                          record expected (assoc-in jobs ["review-job" :agent-id] "author")))))))))))
