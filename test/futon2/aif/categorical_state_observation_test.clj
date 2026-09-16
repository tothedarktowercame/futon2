(ns futon2.aif.categorical-state-observation-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.categorical-state-observation :as sut]
            [futon2.aif.evidence-manifest :as manifest]
            [futon2.aif.observation-authority-resolver :as authority-resolver])
  (:import (java.nio.charset StandardCharsets)
           (java.nio.file Files)))

(defn edn-bytes [x] (.getBytes (pr-str x) StandardCharsets/UTF_8))
(defn pointer [path bs] {:path (str path) :sha256 (sut/sha256-bytes bs)})
(defn write-record! [dir filename record]
  (let [path (.resolve dir filename) bs (edn-bytes record)]
    (Files/write path bs (make-array java.nio.file.OpenOption 0))
    (pointer path bs)))
(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(def base-point
  {:run/id "run-test-1" :cohort/id :cohort/test :attempt/id "attempt-1"
   :checkpoint/ref "cohort/test/attempt-1/closed"
   :state-point :post-action-pre-disposition-at-close
   :action/started-at "2026-09-12T11:59:00Z"
   :action/completed-at "2026-09-12T12:00:00Z"
   :evidence/cutoff-at "2026-09-12T12:01:00Z"
   :disposition/recorded-at "2026-09-12T12:02:00Z"
   :annotation/created-at "2026-09-13T08:00:00Z"})

(defn observation [claim-ref]
  {:schema sut/schema :observation/id "test/observation-1"
   :subject {:entity/id "entity/test-1"} :point base-point
   :categorical-status {:domain :wm/status-v1 :value :strengthened}
   :observation/method :reviewed-categorical-annotation
   :rubric {:id sut/rubric-id}
   :evidence {:claims [{:claim/ref claim-ref}]}
   :authority {:observer/ref :authority/observer-a :review/ref :authority/review-a}
   :limitations {:retrospective? true :missingness :selected-attempts-only
                 :selection :selection-target-present
                 :method :reviewed-categorical-annotation :rubric sut/rubric-id}})

(defn evidence-claim [assertion]
  {:schema sut/evidence-claim-schema :claim/id "claim/test-1"
   :claim/type :categorical-status-evidence
   :subject {:entity/id "entity/test-1"}
   :point (assoc (select-keys base-point [:run/id :cohort/id :attempt/id :checkpoint/ref])
                 :observed-at "2026-09-12T12:00:30Z")
   :assertion assertion :payload {:kind :operator-note :fact :support-gained}
   :authority/scope :test
   :authority/provenance {:config/id :test/categorical-authority :revision :v1}})

(defn review-for [frozen-subject observer-id]
  {:schema sut/review-schema :role :categorical-state-reviewer
   :reviewer/id "reviewer/test-b" :observer/id observer-id
   :verdict :accepted :rubric/id sut/rubric-id
   :subject frozen-subject :subject/sha256 (sut/acceptance-subject-digest frozen-subject)
   :authority/scope :test
   :authority/provenance {:config/id :test/categorical-authority :revision :v1}
   :reviewed-at "2026-09-13T09:00:00Z"})

(defn fixture
  ([] (fixture (evidence-claim :support-gained)))
  ([claim]
   (let [dir (Files/createTempDirectory "categorical-state-test"
                                         (make-array java.nio.file.attribute.FileAttribute 0))
         claim-ref :evidence/claim-a candidate (observation claim-ref)
         claim-source (write-record! dir "claim.edn" claim)
         observer-record {:schema sut/authority-schema
                          :role :categorical-state-observer
                          :principal/id "observer/test-a"
                          :authority/scope :test
                          :authority/provenance
                          {:config/id :test/categorical-authority :revision :v1}}
         observer-source (write-record! dir "observer.edn" observer-record)
         expected {:subject {:entity/id "entity/test-1"}
                   :point (dissoc base-point :annotation/created-at :state-point)
                   :authority/scope :test
                   :authority/provenance {:config/id :test/categorical-authority
                                          :revision :v1}}
         resolved-claim {:ref claim-ref :claim/id (:claim/id claim)
                         :assertion (:assertion claim)
                         :observed-at (get-in claim [:point :observed-at])
                         :source claim-source :claim claim}
         frozen-subject (sut/acceptance-subject candidate [resolved-claim]
                                                observer-record observer-source expected)
         review-source (write-record! dir "review.edn"
                                      (review-for frozen-subject "observer/test-a"))
         records {[:evidence claim-ref] claim-source
                  [:observer :authority/observer-a] observer-source
                  [:review :authority/review-a] review-source}]
     {:dir dir :candidate candidate :records records
      :observer-record observer-record :observer-source observer-source
      :expected expected :frozen-subject frozen-subject
      :authority {:expected expected :resolver (fn [kind ref] (get records [kind ref]))}})))

(defn indexed-fixture
  "Test-only commissioned index over the existing realistic fixture records."
  []
  (let [{:keys [dir records expected] :as f} (fixture)
        evidence-source (get records [:evidence :evidence/claim-a])
        admitted (manifest/build-manifest
                  {:entries [{:evidence/id "claim/test-1"
                              :source-path (:path evidence-source)
                              :expected-sha256 (:sha256 evidence-source)
                              :admitted-at (get-in expected [:point :evidence/cutoff-at])}]
                   :read-bytes #(Files/readAllBytes (java.nio.file.Path/of
                                                     ^String % (make-array String 0)))})
        index {:schema authority-resolver/index-schema :index/id :test/index-1
               :expected expected :manifest-sha256 (:manifest-sha256 admitted)
               :entries (mapv (fn [[[kind ref] source]]
                                (cond-> {:kind kind :ref ref :source source}
                                  (= kind :evidence) (assoc :evidence/id "claim/test-1")))
                              records)}
        index-source (write-record! dir "index.edn" index)
        inputs {:index-source index-source :manifest admitted :expected expected
                :commission/ref :test/external-commission}]
    (assoc f :index index :resolver-inputs inputs)))

(deftest indexed-authority-integration-and-late-review-test
  (let [{:keys [candidate resolver-inputs records]} (indexed-fixture)
        authority (authority-resolver/build-resolver! resolver-inputs)
        result (sut/validate-observation! candidate authority)
        review (sut/read-pinned-form! (get records [:review :authority/review-a]))]
    (is (= :qualified (:status result)))
    (is (= :test (:authority/scope result)))
    (is (= [:support-gained] (:derived-rubric-assertions result)))
    (is (.isAfter (java.time.Instant/parse (:reviewed-at review))
                  (java.time.Instant/parse (get-in candidate [:point :evidence/cutoff-at]))))
    (is (= (get records [:evidence :evidence/claim-a])
           (get-in result [:resolved-evidence-claims 0 :source])))
    (is (= :test/external-commission (get-in authority [:identities :commission/ref])))))

(deftest grounded-claim-qualification-test
  (let [{:keys [candidate authority]} (fixture)
        result (sut/validate-observation! candidate authority)]
    (is (= :qualified (:status result)))
    (is (= [:support-gained] (:derived-rubric-assertions result)))
    (is (= :test (:authority/scope result)))
    (is (re-matches #"[0-9a-f]{64}"
                    (get-in result [:resolved-evidence-claims 0 :source :sha256])))
    (is (re-matches #"[0-9a-f]{64}" (get-in result [:review-source :sha256])))
    (is (= true (get-in result [:limitations :retrospective?])))
    (is (= :selected-attempts-only (get-in result [:limitations :missingness])))))

(deftest evidence-claim-is-authoritative-test
  (is (= :evidence-entity-mismatch
         (let [{:keys [candidate authority]}
               (fixture (assoc-in (evidence-claim :support-gained)
                                  [:subject :entity/id] "entity/other"))]
           (refusal #(sut/validate-observation! candidate authority)))))
  (is (= :evidence-point-mismatch
         (let [{:keys [candidate authority]}
               (fixture (assoc-in (evidence-claim :support-gained) [:point :run/id] "run/other"))]
           (refusal #(sut/validate-observation! candidate authority)))))
  (is (= :evidence-after-cutoff
         (let [{:keys [candidate authority]}
               (fixture (assoc-in (evidence-claim :support-gained) [:point :observed-at]
                                  "2026-09-12T12:01:30Z"))]
           (refusal #(sut/validate-observation! candidate authority)))))
  (is (= :forbidden-evidence-source
         (let [{:keys [candidate authority]}
               (fixture (assoc (evidence-claim :support-gained)
                               :payload {:kind :operator-note
                                         :target-disposition :grounded-change}))]
           (refusal #(sut/validate-observation! candidate authority)))))
  (is (= :unsupported-evidence-assertion
         (let [{:keys [candidate authority]} (fixture (evidence-claim :invented-status-proof))]
           (refusal #(sut/validate-observation! candidate authority))))))

(deftest complete-resolved-subject-controls-test
  (let [{:keys [candidate authority dir records observer-record observer-source expected]}
        (fixture)
        replacement (assoc (evidence-claim :support-gained)
                           :payload {:kind :operator-note :fact :different-grounded-evidence})
        replacement-source (write-record! dir "replacement.edn" replacement)
        replaced-records (assoc records [:evidence :evidence/claim-a] replacement-source)
        replaced-authority (assoc authority :resolver #(get replaced-records [%1 %2]))]
    (is (= :review-subject-mismatch
           (refusal #(sut/validate-observation! candidate replaced-authority))))
    (let [resolved {:ref :evidence/claim-a :claim/id (:claim/id replacement)
                    :assertion (:assertion replacement)
                    :observed-at (get-in replacement [:point :observed-at])
                    :source replacement-source :claim replacement}
          frozen (sut/acceptance-subject candidate [resolved]
                                         observer-record observer-source expected)
          review-source (write-record! dir "replacement-review.edn"
                                       (review-for frozen "observer/test-a"))
          rs (assoc replaced-records [:review :authority/review-a] review-source)
          a (assoc authority :resolver #(get rs [%1 %2]))]
      (is (= :qualified (:status (sut/validate-observation! candidate a)))))
    (let [production-expected (-> expected
                                  (assoc :authority/scope :production)
                                  (assoc-in [:authority/provenance :revision] :production-v1))
          production-observer (assoc observer-record :authority/scope :production
                                     :authority/provenance
                                     (:authority/provenance production-expected))
          observer-p (write-record! dir "production-observer.edn" production-observer)
          rs (assoc records [:observer :authority/observer-a] observer-p)
          a {:expected production-expected :resolver #(get rs [%1 %2])}]
      (is (= :evidence-scope-mismatch
             (refusal #(sut/validate-observation! candidate a)))))
    (let [claim (dissoc (evidence-claim :support-gained) :authority/provenance)
          p (write-record! dir "missing-provenance-claim.edn" claim)
          rs (assoc records [:evidence :evidence/claim-a] p)
          a (assoc authority :resolver #(get rs [%1 %2]))]
      (is (= :evidence-provenance-mismatch
             (refusal #(sut/validate-observation! candidate a)))))
    (let [new-start "2026-09-12T11:58:30Z"
          c (assoc-in candidate [:point :action/started-at] new-start)
          a (assoc-in authority [:expected :point :action/started-at] new-start)]
      (is (= :review-subject-mismatch
             (refusal #(sut/validate-observation! c a)))))
    (let [other-observer (assoc observer-record :principal/id "observer/test-c")
          p (write-record! dir "changed-observer.edn" other-observer)
          rs (assoc records [:observer :authority/observer-a] p)
          a (assoc authority :resolver #(get rs [%1 %2]))]
      (is (= :review-subject-mismatch
             (refusal #(sut/validate-observation! candidate a)))))))

(deftest independent-context-binds-time-and-point-test
  (let [{:keys [candidate authority]} (fixture)]
    (is (= :observation-time-mismatch
           (refusal #(sut/validate-observation!
                      (assoc-in candidate [:point :evidence/cutoff-at] "2026-09-12T12:00:45Z")
                      authority))))
    (is (= :observation-entity-mismatch
           (refusal #(sut/validate-observation!
                      candidate (assoc-in authority [:expected :subject :entity/id] "entity/other")))))
    (is (= :observation-identity-mismatch
           (refusal #(sut/validate-observation!
                      candidate (assoc-in authority [:expected :point :attempt/id] "attempt/other")))))
    (is (= :authority-scope-missing
           (refusal #(sut/validate-observation! candidate
                                                (update authority :expected dissoc :authority/scope)))))
    (is (= :authority-provenance-missing
           (refusal #(sut/validate-observation!
                      candidate (update authority :expected dissoc :authority/provenance)))))))

(deftest identities-authority-and-review-binding-test
  (let [{:keys [candidate authority dir records frozen-subject]} (fixture)]
    (is (= :missing-identity
           (refusal #(sut/validate-observation! (assoc candidate :observation/id "") authority))))
    (is (= :candidate-owned-review
           (refusal #(sut/validate-observation! (assoc candidate :review {:verdict :accepted}) authority))))
    (is (= :authority-not-found
           (refusal #(sut/validate-observation!
                      (assoc-in candidate [:authority :review/ref] :review/forged) authority))))
    (let [observer (write-record! dir "blank-observer.edn"
                                  {:schema sut/authority-schema :role :categorical-state-observer
                                   :principal/id "" :authority/scope :test
                                   :authority/provenance {:config/id :test/categorical-authority
                                                          :revision :v1}})
          a (assoc authority :resolver #(get (assoc records [:observer :authority/observer-a] observer)
                                             [%1 %2]))]
      (is (= :observer-unauthorized (refusal #(sut/validate-observation! candidate a)))))
    (let [p (write-record! dir "self-review.edn"
                           (assoc (review-for frozen-subject "observer/test-a")
                                  :reviewer/id "observer/test-a"))
          a (assoc authority :resolver #(get (assoc records [:review :authority/review-a] p) [%1 %2]))]
      (is (= :self-review (refusal #(sut/validate-observation! candidate a)))))
    (let [ref :evidence/other
          p (write-record! dir "other-claim.edn"
                           (assoc (evidence-claim :support-gained) :claim/id "claim/other"))
          changed (assoc-in candidate [:evidence :claims 0 :claim/ref] ref)
          a (assoc authority :resolver #(get (assoc records [:evidence ref] p) [%1 %2]))]
      (is (= :review-subject-mismatch
             (refusal #(sut/validate-observation! changed a)))))))

(deftest limitations-substitution-and-ambiguity-test
  (let [{:keys [candidate authority dir records observer-record observer-source expected]}
        (fixture)]
    (is (= :limitations-missing
           (refusal #(sut/validate-observation! (dissoc candidate :limitations) authority))))
    (is (= :candidate-owned-rubric-assertions
           (refusal #(sut/validate-observation!
                      (assoc-in candidate [:rubric :assertions] [:support-gained]) authority))))
    (is (= :retrospective-flag-mismatch
           (refusal #(sut/validate-observation!
                      (assoc-in candidate [:limitations :retrospective?] false) authority))))
    (is (= :derived-state-not-observation
           (refusal #(sut/validate-observation!
                      (assoc candidate :observation/method :derived-unique-argmax-of-mu-post)
                      authority))))
    (is (= :state-domain-mismatch
           (refusal #(sut/validate-observation!
                      (assoc-in candidate [:categorical-status :value] :live) authority))))
    (let [ref :evidence/claim-b
          claim (assoc (evidence-claim :framing-sharpened) :claim/id "claim/test-2")
          p (write-record! dir "claim-b.edn" claim)
          c (-> candidate
                (update-in [:evidence :claims] conj {:claim/ref ref})
                (assoc-in [:authority :review/ref] :authority/review-b))
          resolved-a (get-in (sut/validate-observation! candidate authority)
                             [:resolved-evidence-claims 0])
          resolved-b {:ref ref :claim/id (:claim/id claim) :assertion (:assertion claim)
                      :observed-at (get-in claim [:point :observed-at])
                      :source p :claim claim}
          frozen (sut/acceptance-subject c [resolved-a resolved-b]
                                         observer-record observer-source expected)
          review-p (write-record! dir "review-b.edn" (review-for frozen "observer/test-a"))
          rs (assoc records [:evidence ref] p [:review :authority/review-b] review-p)
          a (assoc authority :resolver #(get rs [%1 %2]))]
      (is (= :ambiguous-categorical-evidence
             (refusal #(sut/validate-observation! c a)))))))

(deftest strict-source-controls-test
  (let [good (edn-bytes {:a 1})
        bad-utf8 (byte-array [(unchecked-byte 0xc3) (unchecked-byte 0x28)])]
    (is (= :invalid-utf8
           (refusal #(sut/read-pinned-form!
                      {:path "virtual" :sha256 (sut/sha256-bytes bad-utf8)}
                      {:read-bytes (constantly bad-utf8)}))))
    (is (= :multiple-forms
           (let [bs (.getBytes "{:a 1} {:b 2}" StandardCharsets/UTF_8)]
             (refusal #(sut/read-pinned-form!
                        {:path "virtual" :sha256 (sut/sha256-bytes bs)}
                        {:read-bytes (constantly bs)})))))
    (is (= :source-mutated
           (let [calls (atom 0)]
             (refusal #(sut/read-pinned-form!
                        {:path "virtual" :sha256 (sut/sha256-bytes good)}
                        {:read-bytes (fn [_]
                                       (if (= 1 (swap! calls inc)) good
                                           (edn-bytes {:a 2})))})))))))
