(ns futon2.aif.ticket-readiness-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.meta-pipeline-selector :as pipeline]
            [futon2.aif.ticket-readiness :as readiness]))

(defn- temp-dir [] (.toFile (java.nio.file.Files/createTempDirectory "ticket-readiness" (make-array java.nio.file.attribute.FileAttribute 0))))
(defn- write! [dir name value]
  (let [f (io/file dir name)] (spit f (str (pr-str value) "\n")) (.getCanonicalPath f)))
(def source-pin {:path "tickets/T-zai-chat-transient-timeout.md"
                 :sha256 "09e1270000000000000000000000000000000000000000000000000000000000"})

(defn- reviewed-output [dir binding producer reviewer result-text]
  (let [contract-path (write! dir "contract.edn" (merge binding
                                                          {:schema readiness/contract-schema
                                                           :producer producer
                                                           :positive-cases [:transport-timeout]
                                                           :negative-cases [:done-marker-only]}))
        contract-pin (readiness/file-pin contract-path)
        job-id "job-review-1"
        job-path (write! dir "job.edn" {:job-id job-id :seat reviewer})
        result-path (write! dir "result.edn" {:job-id job-id :result result-text
                                               :contract-pin contract-pin
                                               :source-pin (:source-pin binding)})
        review-path (write! dir "review.edn" {:schema readiness/review-schema
                                               :contract-pin contract-pin
                                               :reviewer reviewer :job-id job-id
                                               :result-sha256 (readiness/value-digest result-text)
                                               :verdict :accepted})]
    (merge binding {:contract-pin contract-pin :review-pin (readiness/file-pin review-path)
                    :review-job-pin (readiness/file-pin job-path)
                    :review-result-pin (readiness/file-pin result-path)
                    :authorized-reviewers #{reviewer}})))

(deftest contract-output-needs-content-and-independent-review
  (let [dir (temp-dir)
        binding {:ticket-id "T-x" :source-pin source-pin :increment-id :prepare-h1}
        opts (reviewed-output dir binding "author" "reviewer" "accepted independently")]
    (is (= :qualified (:status (readiness/validate-preparation-output opts))))
    (spit (get-in opts [:review-result-pin :path])
          (str (pr-str {:job-id "job-review-1" :result "changed"}) "\n"))
    (is (= :review-port-binding-mismatch
           (:reason (readiness/validate-preparation-output
                     (assoc opts :review-result-pin
                            (readiness/file-pin (get-in opts [:review-result-pin :path])))))))
    (let [same (reviewed-output dir binding "same" "same" "self review")]
      (is (= :review-not-independent
             (:reason (readiness/validate-preparation-output same)))))))

(deftest semantic-red-is-not-generic-c8-false
  (let [dir (temp-dir)
        log (write! dir "red.log" {:failure :H1-throw-to-value})
        post (write! dir "post.edn" {:status :failed})
        _ (spit log (str (pr-str {:schema readiness/semantic-log-schema
                                  :failures [{:id :H1-throw-to-value}]}) "\n"))
        _ (spit post (str (pr-str {:schema readiness/postcheck-schema :status :failed
                                   :revision "before" :failure-id :H1-throw-to-value}) "\n"))
        cp {:path "code" :sha256 (apply str (repeat 64 "c"))}
        tp {:path "test" :sha256 (apply str (repeat 64 "t"))}
        contract-pin {:path "c" :sha256 (apply str (repeat 64 "a"))}
        registry (write! dir "registry.edn" {:schema readiness/registry-schema
                                              :status :executed :revision "before"
                                              :contract-pin contract-pin :code-pin cp :test-pin tp
                                              :log-pin (readiness/file-pin log)
                                              :postcheck-pin (readiness/file-pin post)})
        base {:expected-revision "before" :contract-pin contract-pin
              :failure-id :H1-throw-to-value :expected-code-pin cp :expected-test-pin tp}]
    (is (= :semantic-negative-not-established
           (:reason (readiness/semantic-negative base))))
    (is (= :established (:status (readiness/semantic-negative
                                  (assoc base :registry-pin (readiness/file-pin registry))))))
    (is (= :before-revision-mismatch
           (:reason (readiness/semantic-negative
                     (assoc base :expected-revision "other"
                            :registry-pin (readiness/file-pin registry))))))
    (spit log (str (pr-str {:schema readiness/semantic-log-schema
                            :failures [{:id :unrelated}]}) "\n"))
    (let [record (edn/read-string (slurp registry))
          registry (write! dir "forged-registry.edn"
                           (assoc record :log-pin (readiness/file-pin log)))]
      (is (= :semantic-negative-not-established
             (:reason (readiness/semantic-negative
                       (assoc base :registry-pin (readiness/file-pin registry)))))))))

(deftest four-state-increment-classification
  (let [common {:source-current? true :operator-allowed? true :owned? false :active? true
                :interpretation-valid? true :root-route :standalone}]
    (is (= {:classification :suitable :increment :preparation
            :implementation-admitted false :global-completeness-claimed false}
           (readiness/classify (assoc common :increment :preparation
                                      :preparation-dependencies-resolved? true))))
    (is (= :operator-gate (:reason (readiness/classify (assoc common :increment :preparation
                                                               :operator-allowed? false)))))
    (is (= :owned (:reason (readiness/classify (assoc common :increment :preparation :owned? true)))))
    (is (= :assessment-stale (:reason (readiness/classify (assoc common :increment :preparation
                                                                  :source-current? false)))))
    (is (= :relevant-dependency-unresolved
           (:reason (readiness/classify
                     (assoc common :increment :implementation
                            :preparation-output-qualified? true
                            :semantic-negative {:status :established}
                            :relevant-dependencies-resolved? false)))))
    (is (= :implementation
           (:increment (readiness/classify
                        (assoc common :increment :implementation
                               :preparation-output-qualified? true
                               :semantic-negative {:status :established}
                               :relevant-dependencies-resolved? true)))))
    (is (= {:classification :completed :increment-completed? true
            :ticket-completed? false :next-disposition :assess-remaining-increments}
           (readiness/classify (assoc common :completion-evidence {:status :validated :revision "after"}
                                      :ticket-completed? false
                                      :expected-after-revision "after"
                                      :after-revision "after"))))
    (is (= :after-revision-mismatch
           (:reason (readiness/classify (assoc common :completion-evidence {:status :validated :revision "wrong"}
                                                :expected-after-revision "after"
                                                :after-revision "wrong")))))))

(deftest one-refresh-is-durable-fair-and-enters-membership
  (let [dir (temp-dir) manifest (.getPath (io/file dir "manifest.edn"))
        tickets [{:id "T-b" :source source-pin}
                 {:id "T-a" :source (assoc source-pin :path "T-a" :sha256 (apply str (repeat 64 "b")))}]
        contacts (atom [])
        inputs (fn [ticket] {:source (:source ticket) :dependency-evidence []
                             :ownership-evidence :unowned
                             :implementation-evidence :absent
                             :operator-evidence :allowed :root-route :standalone})
        opts {:tickets tickets :manifest-path manifest :input-fn inputs
              :prepare-fn (fn [ticket] {:target (:id ticket) :source (:source ticket)})
              :answer-fn (fn [proposal] (swap! contacts conj (:target proposal))
                           {:provenance :synthetic-test-reply :increment :preparation})
              :validate-fn (fn [ticket _ _]
                             {:increment :preparation
                              :increment-want [(:id ticket) :contract-present]
                              :assessment {:classification :suitable}})
              :now-fn (constantly "2026-10-27T00:00:00Z")}
        first-result (readiness/refresh-one! opts)
        first-bytes (slurp manifest)
        second-result (readiness/refresh-one! opts)
        loaded (readiness/read-manifest manifest)
        section (readiness/suitable-section loaded (into {} (map (juxt :id inputs) tickets)))
        snapshot {:schema pipeline/snapshot-schema
                  :summary-source {:path "s" :sha256 (apply str (repeat 64 "c"))}
                  :graph-source {:path "g" :sha256 (apply str (repeat 64 "d"))}
                  :summary {:consistent? true}
                  :graph {:ticket-readiness section
                          :section-status {:lineage {:status :ok} :clusters {:status :ok}
                                           :arrows {:status :ok} :held {:status :ok}
                                           :patterns {:status :ok} :ticket-readiness {:status :ok}}}}]
    (is (= :assessed (:status first-result)))
    (is (= "T-a" (:ticket-id first-result)))
    (is (not= first-bytes (slurp manifest)))
    (is (= ["T-a" "T-b"] @contacts))
    (is (= #{"T-a" "T-b"} (:ids (pipeline/pipeline-membership snapshot))))
    (is (= :synthetic-test-reply
           (:external-reply-provenance
            (edn/read-string (slurp (get-in second-result [:record-pin :path]))))))))

(deftest admitted-root-is-deduplicated
  (is (= {:classification :not-suitable :reason :admitted-root
          :root-observation-retained true}
         (readiness/classify {:increment :preparation :source-current? true
                              :operator-allowed? true :owned? false :active? true
                              :interpretation-valid? true :root-route :admitted}))))

(deftest forged-membership-and-changed-inputs-are-excluded
  (let [dir (temp-dir) assessment {:classification :suitable}
        inputs {:source source-pin :dependency-evidence [:d1]
                :ownership-evidence :unowned :implementation-evidence :absent
                :operator-evidence :allowed :root-route :standalone}
        record (write! dir "assessment.edn"
                       {:schema readiness/schema :ticket-id "T-x" :source-pin source-pin
                        :increment :preparation :increment-want ["T-x" :contract]
                        :assessment assessment :inputs inputs})
        pin (readiness/file-pin record)
        entry {:ticket-id "T-x" :increment :preparation
               :increment-want ["T-x" :contract]
               :source-pin source-pin :assessment assessment
               :assessment-pin pin
               :input-identity (readiness/value-digest
                                {:source source-pin :dependency-evidence [:d1]
                                 :ownership-evidence :unowned :implementation-evidence :absent
                                 :operator-evidence :allowed :root-route :standalone})}
        manifest {:schema readiness/manifest-schema :entries [entry] :attempts {}}]
    (is (= ["T-x"] (mapv :ticket-id
                           (:items (readiness/suitable-section
                                    manifest {"T-x" {:source source-pin :dependency-evidence [:d1]
                                                     :ownership-evidence :unowned
                                                     :implementation-evidence :absent
                                                     :operator-evidence :allowed
                                                     :root-route :standalone}})))))
    (is (empty? (:items (readiness/suitable-section
                         manifest {"T-x" {:source source-pin :dependency-evidence [:changed]
                                          :ownership-evidence :unowned
                                          :implementation-evidence :absent
                                          :operator-evidence :allowed :root-route :standalone}}))))
    (spit record (str (pr-str {:inputs {:source source-pin :dependency-evidence [:d1]
                                        :ownership-evidence :owned
                                        :implementation-evidence :absent
                                        :operator-evidence :allowed :root-route :standalone}}) "\n"))
    (is (empty? (:items (readiness/suitable-section
                         manifest {"T-x" {:source source-pin :dependency-evidence [:d1]
                                          :ownership-evidence :unowned
                                          :implementation-evidence :absent
                                          :operator-evidence :allowed :root-route :standalone}}))))))

(deftest unknown-assessments-retry-fairly-and-input-change-reassesses
  (let [dir (temp-dir) manifest (.getPath (io/file dir "manifest.edn"))
        tickets [{:id "T-a" :source (assoc source-pin :path "a")}
                 {:id "T-b" :source (assoc source-pin :path "b")}]
        deps (atom {"T-a" [:d1] "T-b" [:d1]})
        contacted (atom [])
        tick (atom 0)
        opts {:tickets tickets :manifest-path manifest
              :input-fn (fn [t] {:source (:source t)
                                  :dependency-evidence (get @deps (:id t))
                                  :ownership-evidence :unowned
                                  :implementation-evidence :absent
                                  :operator-evidence :allowed :root-route :standalone})
              :prepare-fn identity
              :answer-fn (fn [t] (swap! contacted conj (:id t)) {:provenance :synthetic-test-reply})
              :validate-fn (fn [_ _ _] {:increment :preparation
                                         :assessment {:classification :unknown}})
              :now-fn #(str (swap! tick inc))}]
    (readiness/refresh-one! opts)
    (readiness/refresh-one! opts)
    (readiness/refresh-one! opts)
    (is (= ["T-a" "T-b" "T-a"] @contacted))
    (swap! deps assoc "T-b" [:d2])
    (readiness/refresh-one! opts)
    (is (= "T-b" (last @contacted)))))

(deftest old-snapshot-without-readiness-remains-complete
  (let [snapshot {:graph {:section-status {:lineage {:status :ok}
                                            :clusters {:status :ok}
                                            :arrows {:status :ok}
                                            :held {:status :ok}
                                            :patterns {:status :ok}}}}]
    (is (= :complete (:status (pipeline/section-availability snapshot))))))

(deftest bounded-preparation-response-is-source-and-proposal-bound
  (let [ticket {:id "T-x" :source source-pin :status-class :live}
        proposal {:target "T-x" :source source-pin}
        inputs {:source source-pin :dependency-evidence {:status :unknown}
                :ownership-evidence {:state :unowned}
                :implementation-evidence {:status :unknown}
                :operator-evidence {:status-class :live}
                :root-route :standalone}
        answer {:schema readiness/response-schema :increment :preparation
                :ticket-id "T-x" :source-pin source-pin
                :proposal-pin (readiness/value-digest proposal)
                :increment-id :prepare-contract
                :increment-want ["T-x" :contract-present]
                :increment-locator {:check :C3 :path "contracts/T-x.edn"}
                :preparation-dependencies :satisfied}
        admitted (readiness/validate-assessment-response
                  ticket proposal {:answer answer :provenance :synthetic-test-reply} inputs)]
    (is (= :suitable (get-in admitted [:assessment :classification])))
    (is (= {:check :C3 :path "contracts/T-x.edn"} (:increment-locator admitted)))
    (is (= :unknown
           (get-in (readiness/validate-assessment-response
                    ticket proposal
                    {:answer (assoc answer :source-pin (assoc source-pin :sha256 "wrong"))
                     :provenance :synthetic-test-reply}
                    inputs)
                   [:assessment :classification])))))
