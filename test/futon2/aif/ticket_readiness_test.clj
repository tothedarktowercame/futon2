(ns futon2.aif.ticket-readiness-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-pipeline-selector :as pipeline]
            [futon2.aif.ticket-readiness :as readiness]))

(defn- git! [dir & args]
  (let [{:keys [exit out err]} (apply shell/sh "git" "-C" (.getPath dir) args)]
    (when-not (zero? exit) (throw (ex-info "git failed" {:args args :err err})))
    (.trim out)))

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
        job {:job-id job-id :seat reviewer :result result-text
             :contract-pin contract-pin :source-pin (:source-pin binding)}
        review-path (write! dir "review.edn" {:schema readiness/review-schema
                                               :contract-pin contract-pin
                                               :reviewer reviewer :job-id job-id
                                               :result-sha256 (readiness/value-digest result-text)
                                               :verdict :accepted})]
    (merge binding {:contract-pin contract-pin :review-pin (readiness/file-pin review-path)
                    :review-job-id job-id :reviewer-policy reviewer
                    :review-resolver (fn [_] job)})))

(deftest contract-output-needs-content-and-independent-review
  (let [dir (temp-dir)
        binding {:ticket-id "T-x" :source-pin source-pin :increment-id :prepare-h1}
        opts (reviewed-output dir binding "author" "reviewer" "accepted independently")]
    (is (= :qualified (:status (readiness/validate-preparation-output opts))))
    (is (= :review-port-binding-mismatch
           (:reason (readiness/validate-preparation-output
                     (assoc opts :review-resolver
                            (fn [_] {:job-id "job-review-1" :seat "reviewer"
                                     :result "changed"}))))))
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
        payload (pr-str {:schema "test-registry/v1" :kind :run :git-head "before"
                         :contract-pin contract-pin :code-pin cp :test-pin tp
                         :semantic-failures [{:id :H1-throw-to-value}]
                         :postcheck {:status :matched}})
        digest (load-identity/sha256 (.getBytes payload "UTF-8"))
        entry-id (str "test-registry-" digest)
        entry {:evidence/body {:payload-edn payload :sha256 digest}}
        base {:expected-revision "before" :contract-pin contract-pin
              :failure-id :H1-throw-to-value :expected-code-pin cp :expected-test-pin tp
              :registry-entry-id entry-id :registry-resolver (fn [_] entry)}]
    (is (= :semantic-negative-not-established
           (:reason (readiness/semantic-negative (dissoc base :registry-resolver)))))
    (is (= :established (:status (readiness/semantic-negative base))))
    (is (= :before-revision-mismatch
           (:reason (readiness/semantic-negative
                     (assoc base :expected-revision "other")))))
    (let [bad-payload (pr-str {:schema "test-registry/v1" :kind :run :git-head "before"
                               :contract-pin contract-pin :code-pin cp :test-pin tp
                               :semantic-failures [{:id :unrelated}]
                               :postcheck {:status :matched}})
          bad-digest (load-identity/sha256 (.getBytes bad-payload "UTF-8"))
          bad-entry {:evidence/body {:payload-edn bad-payload :sha256 bad-digest}}]
      (is (= :semantic-negative-not-established
             (:reason (readiness/semantic-negative
                       (assoc base :registry-entry-id (str "test-registry-" bad-digest)
                              :registry-resolver (fn [_] bad-entry)))))))))

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
           (readiness/classify (assoc common :completion-evidence {:status :validated :revision (apply str (repeat 40 "a"))}
                                      :ticket-completed? false
                                      :expected-after-revision (apply str (repeat 40 "a"))
                                      :after-revision (apply str (repeat 40 "a"))))))
    (is (= :after-revision-mismatch
           (:reason (readiness/classify (assoc common :completion-evidence {:status :validated :revision "wrong"}
                                                :expected-after-revision (apply str (repeat 40 "a"))
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
        jobs (atom {})
        resolver #(get @jobs %)
        opts {:tickets tickets :manifest-path manifest :input-fn inputs
              :contact-resolver resolver
              :prepare-fn (fn [ticket] {:target (:id ticket) :source (:source ticket)})
              :answer-fn (fn [proposal]
                           (swap! contacts conj (:target proposal))
                           (let [job-id (str "job-" (:target proposal))
                                 response {:job-id job-id :seat "test-seat"
                                           :text (str "answer-" (:target proposal))}]
                             (swap! jobs assoc job-id
                                    {:job-id job-id :seat "test-seat" :result (:text response)})
                             (assoc response :result-pin
                                    (readiness/retain-contact-result! manifest response))))
              :validate-fn (fn [ticket _ response]
                             {:increment :preparation
                              :increment-want [(:id ticket) :contract-present]
                              :contact-provenance (select-keys response [:job-id :seat :result-pin])
                              :assessment {:classification :suitable}})
              :now-fn (constantly "2026-10-27T00:00:00Z")}
        first-result (readiness/refresh-one! opts)
        first-bytes (slurp manifest)
        second-result (readiness/refresh-one! opts)
        loaded (readiness/read-manifest manifest)
        section (readiness/suitable-section loaded (into {} (map (juxt :id inputs) tickets))
                                              resolver)
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
    (is (= "job-T-b"
           (get-in (edn/read-string (slurp (get-in second-result [:record-pin :path])))
                   [:contact-provenance :job-id])))))

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
    (is (empty? (:items (readiness/suitable-section
                         manifest {"T-x" {:source source-pin :dependency-evidence [:d1]
                                          :ownership-evidence :unowned
                                          :implementation-evidence :absent
                                          :operator-evidence :allowed
                                          :root-route :standalone}})))
        "a locally coherent assessment without a resolved Agency contact is excluded")
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
  (let [dir (temp-dir)
        ticket {:id "T-x" :source source-pin :status-class :live}
        proposal {:target "T-x" :source source-pin}
        inputs {:source source-pin :dependency-evidence {:status :unknown}
                :ownership-evidence {:state :unowned}
                :implementation-evidence {:status :unknown}
                :operator-evidence {:status-class :live :requisition {:absent :no-requisition}}
                :root-route :standalone}
        answer {:schema readiness/response-schema :increment :preparation
                :ticket-id "T-x" :source-pin source-pin
                :proposal-pin (readiness/value-digest proposal)
                :increment-id :prepare-contract
                :increment-want ["T-x" :contract-present]
                :increment-locator {:check :C3 :repo "futon2"
                                    :sha (apply str (repeat 40 "a"))
                                    :path "holes/labs/wm-contract/ticket-readiness/T-x/prepare-contract.edn"}
                :preparation-dependencies :satisfied}
        response {:answer answer :job-id "job-assess" :seat "test-seat" :text "answer"}
        response (assoc response
                        :result-pin (readiness/retain-contact-result! (.getPath (io/file dir "manifest")) response)
                        :contact-resolver (fn [_] {:job-id "job-assess" :seat "test-seat"
                                                   :result "answer"}))
        admitted (readiness/validate-assessment-response ticket proposal response inputs)]
    (is (= :suitable (get-in admitted [:assessment :classification])))
    (is (= {:check :C3 :class :C3 :repo "futon2" :sha (apply str (repeat 40 "a"))
            :path "holes/labs/wm-contract/ticket-readiness/T-x/prepare-contract.edn"}
           (:increment-locator admitted)))
    (is (= :unknown
           (get-in (readiness/validate-assessment-response
                    ticket proposal
                    (assoc response :answer
                           (assoc answer :source-pin (assoc source-pin :sha256 "wrong")))
                    inputs)
                   [:assessment :classification])))))

(deftest adapter-keeps-operator-ownership-and-provenance-unknowns-typed
  (let [ticket {:id "T-x" :source source-pin}
        proposal {:target "T-x"}
        answer {:schema readiness/response-schema :increment :preparation
                :ticket-id "T-x" :source-pin source-pin
                :proposal-pin (readiness/value-digest proposal)
                :increment-id :prepare-contract :increment-want ["T-x" :contract]
                :increment-locator {:check :C3 :repo "futon2"
                                    :sha (apply str (repeat 40 "a"))
                                    :path "holes/labs/wm-contract/ticket-readiness/T-x/prepare-contract.edn"}
                :preparation-dependencies :satisfied}
        inputs {:source source-pin :dependency-evidence []
                :implementation-evidence {:status :unknown} :root-route :standalone
                :ownership-evidence {:state :unowned}
                :operator-evidence {:status-class :live :requisition {:absent :no-requisition}}}
        no-port {:answer answer :provenance :synthetic-test-reply}]
    (is (= :unknown (get-in (readiness/validate-assessment-response ticket proposal no-port inputs)
                            [:assessment :classification])))
    (is (= :ownership-unknown
           (get-in (readiness/validate-assessment-response
                    ticket proposal no-port (assoc inputs :ownership-evidence {:state :unknown}))
                   [:assessment :reason])))
    (is (= :operator-evidence-unknown
           (get-in (readiness/validate-assessment-response
                    ticket proposal no-port (assoc-in inputs [:operator-evidence :status-class] nil))
                   [:assessment :reason])))
    (is (= :operator-gate
           (get-in (readiness/validate-assessment-response
                    ticket proposal no-port
                    (assoc-in inputs [:operator-evidence :requisition] {:state :in-progress}))
                   [:assessment :reason])))))

(deftest same-clock-different-assessments-have-immutable-distinct-records
  (let [dir (temp-dir) manifest (.getPath (io/file dir "manifest.edn"))
        ticket {:id "T-x" :source source-pin} calls (atom 0)
        inputs (constantly {:source source-pin :dependency-evidence []
                            :ownership-evidence :unowned :implementation-evidence :absent
                            :operator-evidence :allowed :root-route :standalone})
        opts {:tickets [ticket] :manifest-path manifest :input-fn inputs
              :prepare-fn identity :answer-fn (constantly {})
              :validate-fn (fn [_ _ _]
                             {:increment :preparation :increment-id :prepare-contract
                              :assessment {:classification :unknown
                                           :reason (if (= 1 (swap! calls inc)) :first :second)}})
              :now-fn (constantly "same-clock")}
        first-result (readiness/refresh-one! opts)
        first-bytes (slurp (get-in first-result [:record-pin :path]))
        second-result (readiness/refresh-one! opts)]
    (is (not= (get-in first-result [:record-pin :path])
              (get-in second-result [:record-pin :path])))
    (is (= first-bytes (slurp (get-in first-result [:record-pin :path]))))
    (is (not= (get-in first-result [:record-pin :sha256])
              (get-in second-result [:record-pin :sha256])))))

(deftest reviewed-preparation-qualification-reads-the-immutable-commit
  (let [repo (temp-dir) _ (git! repo "init")
        _ (git! repo "config" "user.email" "test@example.invalid")
        _ (git! repo "config" "user.name" "Test")
        path "holes/labs/wm-contract/ticket-readiness/T-x/prepare-contract.edn"
        file (io/file repo path) _ (.mkdirs (.getParentFile file))
        contract {:schema readiness/contract-schema :ticket-id "T-x"
                  :source-pin source-pin :increment-id :prepare-contract
                  :positive-cases [:increment] :negative-cases [:done-only]}
        _ (spit file (str (pr-str contract) "\n")) _ (git! repo "add" path)
        _ (git! repo "commit" "-m" "contract") head (git! repo "rev-parse" "HEAD")
        want ["T-x" :contract-present]
        action {:target "T-x" :want [want]
                :observation-locators {want {:check :C3 :repo "futon2" :sha head :path path}}
                :ticket-readiness {:increment :preparation :increment-id :prepare-contract
                                   :source-pin source-pin}}
        base {:selected-action action :repository (.getPath repo) :after-revision head
              :mission-observation {:status :admitted}
              :selected-want-outcomes {:status :verified
                                       :outcomes [{:classification :reached}]}
              :author-job {:job-id "a" :seat "author"}
              :review-job {:job-id "r" :seat "reviewer"}
              :review-gate {:passed? true}}]
    (is (= :qualified (:status (readiness/qualify-preparation-run base))))
    (is (= :contract-cases-empty
           (:reason (do (spit file (str (pr-str (assoc contract :negative-cases [])) "\n"))
                        (git! repo "add" path) (git! repo "commit" "-m" "empty")
                        (readiness/qualify-preparation-run
                         (assoc base :after-revision (git! repo "rev-parse" "HEAD")))))))
    (is (= :review-not-independent
           (:reason (readiness/qualify-preparation-run
                     (assoc base :review-job {:job-id "r" :seat "author"})))))
    (is (= :preparation-observation-not-admitted
           (:reason (readiness/qualify-preparation-run
                     (assoc base :mission-observation {:status :refused})))))
    (is (= :contract-missing-or-unreadable
           (:reason (readiness/qualify-preparation-run
                     (assoc-in base [:selected-action :observation-locators want :path]
                               "holes/labs/wm-contract/ticket-readiness/T-x/other.edn")))))))
