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

(deftest contract-output-needs-content-and-independent-review
  (let [dir (temp-dir)
        binding {:ticket-id "T-x" :source-pin source-pin :increment-id :prepare-h1}
        contract-path (write! dir "contract.edn" (merge binding
                                                          {:schema readiness/contract-schema
                                                           :producer "author"
                                                           :positive-cases [:transport-timeout]
                                                           :negative-cases [:done-marker-only]}))
        contract-pin (readiness/file-pin contract-path)
        review-path (write! dir "review.edn" {:schema readiness/review-schema
                                               :contract-pin contract-pin
                                               :reviewer "reviewer" :verdict :accepted})
        opts (assoc binding :contract-pin contract-pin :review-pin (readiness/file-pin review-path))]
    (is (= :qualified (:status (readiness/validate-preparation-output opts))))
    (spit contract-path "{}\n")
    (is (= :contract-missing-or-unreadable
           (:reason (readiness/validate-preparation-output opts))))
    (let [contract-path (write! dir "contract-2.edn" (merge binding
                                                             {:schema readiness/contract-schema
                                                              :producer "same"
                                                              :positive-cases [:p] :negative-cases [:n]}))
          contract-pin (readiness/file-pin contract-path)
          review-path (write! dir "review-2.edn" {:schema readiness/review-schema
                                                   :contract-pin contract-pin
                                                   :reviewer "same" :verdict :accepted})]
      (is (= :review-not-independent
             (:reason (readiness/validate-preparation-output
                       (assoc binding :contract-pin contract-pin
                              :review-pin (readiness/file-pin review-path)))))))))

(deftest semantic-red-is-not-generic-c8-false
  (let [dir (temp-dir)
        log (write! dir "red.log" {:failure :H1-throw-to-value})
        post (write! dir "post.edn" {:status :failed})
        base {:expected-revision "before" :contract-pin {:path "c" :sha256 (apply str (repeat 64 "a"))}
              :failure-id :H1-throw-to-value}
        good {:status :executed :revision "before" :contract-pin (:contract-pin base)
              :failure-id :H1-throw-to-value :log-pin (readiness/file-pin log)
              :postcheck-pin (readiness/file-pin post)}]
    (is (= :semantic-negative-not-established
           (:reason (readiness/semantic-negative base))))
    (is (= :established (:status (readiness/semantic-negative (assoc base :record good)))))
    (is (= :before-revision-mismatch
           (:reason (readiness/semantic-negative
                     (assoc base :record (assoc good :revision "stale"))))))
    (is (= :semantic-negative-not-established
           (:reason (readiness/semantic-negative
                     (assoc base :record (assoc good :failure-id :unrelated))))))))

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
           (readiness/classify (assoc common :increment-effect? true
                                      :ticket-completed? false
                                      :expected-after-revision "after"
                                      :after-revision "after"))))
    (is (= :after-revision-mismatch
           (:reason (readiness/classify (assoc common :increment-effect? true
                                                :expected-after-revision "after"
                                                :after-revision "wrong")))))))

(deftest one-refresh-is-durable-fair-and-enters-membership
  (let [dir (temp-dir) manifest (.getPath (io/file dir "manifest.edn"))
        tickets [{:id "T-b" :source source-pin}
                 {:id "T-a" :source (assoc source-pin :path "T-a" :sha256 (apply str (repeat 64 "b")))}]
        contacts (atom [])
        opts {:tickets tickets :manifest-path manifest
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
        section (readiness/suitable-section loaded)
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
