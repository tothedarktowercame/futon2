(ns futon2.aif.close-terminal-retention-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runner-test :as fixture]
            [futon2.aif.hermetic-repair-fixture :as hermetic]))

(def historical-errors
  [[:missing-judgment-key :duration-ms] [:missing-judgment-key :resource-use]])

(defn- isolated [f]
  (hermetic/with-hermetic-stores #(fixture/with-hermetic-traces f)))

(deftest production-shaped-close-retains-the-typed-failure-once
  (isolated
   (fn []
     (let [{:keys [root] :as c} (#'fixture/retention-cohort "typed-close-")
           base (#'fixture/retention-success-opts c)
           findings (atom []) calls (atom 0) refusal (atom nil)
           close cohort/close-attempt!
           opts (assoc base
                       :tripwire/cohort-history [] :tripwire/a-matrix-events []
                       :tripwire/grounding-witnesses []
                       :repair-system-record-fn
                       (fn [finding]
                         (let [r (assoc finding :repair/id "isolated-close-finding")]
                           (swap! findings conj r) r))
                       :author-artifact-observer-fn
                       (fn [repo before job]
                         (assoc (fixture/synthetic-artifact-binding repo before job) :repo repo))
                       :poll-fn (fn [o id]
                                  (cond-> ((:poll-fn base) o id)
                                    (= id "retention-author") (assoc :feature-card fixture/feature-card-claim)))
                       :ground-fn (fn [& _] {:before {:id "before"} :after {:id "after"}
                                             :resolved? true :dial-moved? true
                                             :implementation-id "after" :discharge-id "discharge"}))]
       (try
         (let [result (with-redefs [cohort/close-attempt!
                                   (fn [& args]
                                     (let [n (swap! calls inc)
                                           args (if (= 1 n)
                                                  (update (vec args) (dec (count args))
                                                          update :judgment dissoc :duration-ms :resource-use)
                                                  args)]
                                       (try (apply close args)
                                            (catch clojure.lang.ExceptionInfo e
                                              (reset! refusal (ex-data e)) (throw e)))))]
                        (binding [runner/*wm-status-reporting?* false]
                          (runner/run-opportunity! opts)))
               events (cohort/attempt-events (io/file root "test-cohort-exhaustion" "attempt-001"))
               closed (filter #(= :closed (:checkpoint/type %)) events)]
           (is (= historical-errors (:errors @refusal)))
           (is (= :invalid-close-outcome (:failure-kind @refusal)))
           (is (= 2 @calls) "one rejected malformed write, one accepted terminal")
           (is (= 1 (count closed)))
           (is (= 1 (count @findings)))
           (is (= :invalid-close-outcome (:failure-kind (first @findings))))
           (is (= :build-failed (:outcome result)))
           (is (= historical-errors (get-in (first closed) [:payload :judgment :refusal-data :errors])))
           (is (not-any? #(= :initialization (:failure-stage %)) @findings)))
         (finally
           (doseq [dir [root (:repair-root opts)] f (reverse (file-seq (io/file dir)))]
             (io/delete-file f true))))))))

(deftest initialization-preserves-typed-close-data-but-contains-unknown-failure
  (isolated
   (fn []
     (doseq [[data expected] [[{:failure-kind :invalid-close-outcome :failure-stage :close
                               :outcome :build-failed :errors historical-errors}
                              :invalid-close-outcome]
                             [{} :initialization-failed]]]
       (let [findings (atom [])
             opts (assoc (fixture/isolated-runner-opts)
                         :repair-system-record-fn (fn [f] (swap! findings conj f) (assoc f :repair/id "fixture")))
             result (with-redefs-fn {#'runner/run-opportunity-core!
                                    (fn [_] (throw (ex-info "invalid close outcome" data)))}
                      #(binding [runner/*wm-status-reporting?* false] (runner/run-opportunity! opts)))]
         (is (= 1 (count @findings)))
         (is (= expected (:failure-kind (first @findings))))
         (is (= expected (get-in result [:data :failure-kind])))
         (is (= data (:failure-data (first @findings)))))))))
