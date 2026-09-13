(ns futon2.aif.categorical-state-observation-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.categorical-state-observation :as sut])
  (:import (java.nio.charset StandardCharsets)
           (java.nio.file Files)))

(defn edn-bytes [x] (.getBytes (pr-str x) StandardCharsets/UTF_8))
(defn pointer [path bs] {:path (str path) :sha256 (sut/sha256-bytes bs)})

(defn base-observation [evidence-pointer]
  {:schema sut/schema
   :observation/id "test/observation-1"
   :subject {:entity/id "entity/test-1"}
   :point {:run/id "run-test-1" :cohort/id :cohort/test :attempt/id "attempt-1"
           :checkpoint/ref "cohort/test/attempt-1/closed"
           :state-point :post-action-pre-disposition-at-close
           :action/started-at "2026-09-12T11:59:00Z"
           :action/completed-at "2026-09-12T12:00:00Z"
           :evidence/cutoff-at "2026-09-12T12:01:00Z"
           :disposition/recorded-at "2026-09-12T12:02:00Z"
           :annotation/created-at "2026-09-13T08:00:00Z"}
   :categorical-status {:domain :wm/status-v1 :value :strengthened}
   :observation/method :reviewed-categorical-annotation
   :rubric {:id sut/rubric-id :assertions [:support-gained]}
   :evidence {:items [{:kind :operator-note
                       :observed-at "2026-09-12T12:00:30Z"
                       :source evidence-pointer}]}
   :authority {:observer/ref :authority/observer-a :review/ref :authority/review-a}
   :limitations {:retrospective? true :cohort :isolated-production-shaped-test
                 :missingness :not-a-production-observation}})

(defn review-for [observation observer-id]
  {:schema sut/review-schema :role :categorical-state-reviewer
   :reviewer/id "reviewer/test-b" :observer/id observer-id
   :verdict :accepted :rubric/id sut/rubric-id
   :subject (sut/subject observation) :subject/sha256 (sut/subject-digest observation)
   :reviewed-at "2026-09-13T09:00:00Z"})

(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(defn fixture []
  (let [dir (Files/createTempDirectory "categorical-state-test" (make-array java.nio.file.attribute.FileAttribute 0))
        evidence-path (.resolve dir "evidence.edn") evidence-bytes (edn-bytes {:fact :support-gained})
        observation (base-observation (pointer evidence-path evidence-bytes))
        observer-path (.resolve dir "observer.edn")
        observer-bytes (edn-bytes {:schema sut/authority-schema
                               :role :categorical-state-observer
                               :principal/id "observer/test-a"})
        review-path (.resolve dir "review.edn") review-bytes (edn-bytes (review-for observation "observer/test-a"))]
    (Files/write evidence-path evidence-bytes (make-array java.nio.file.OpenOption 0))
    (Files/write observer-path observer-bytes (make-array java.nio.file.OpenOption 0))
    (Files/write review-path review-bytes (make-array java.nio.file.OpenOption 0))
    {:observation observation :review-path review-path :review-bytes review-bytes
     :authority {:expected {:subject (:subject observation)
                            :point (select-keys (:point observation)
                                                [:run/id :cohort/id :attempt/id :checkpoint/ref])}
                 :resolver (fn [kind ref]
                             (case [kind ref]
                               [:observer :authority/observer-a]
                               (pointer observer-path observer-bytes)
                               [:review :authority/review-a] (pointer review-path review-bytes)
                               nil))}}))

(deftest production-shaped-isolated-qualification-test
  (let [{:keys [observation authority]} (fixture)
        result (sut/validate-observation! observation authority)]
    (is (= :qualified (:status result)))
    (is (= :strengthened (get-in result [:observation :categorical-status :value])))
    (is (= :isolated-production-shaped-test (get-in result [:limitations :cohort])))))

(deftest substitution-and-rubric-refusals-test
  (let [{:keys [observation authority]} (fixture)]
    (is (= :derived-state-not-observation
           (refusal #(sut/validate-observation!
                      (assoc observation :observation/method :derived-unique-argmax-of-mu-post)
                      authority))))
    (is (= :state-domain-mismatch
           (refusal #(sut/validate-observation!
                      (assoc-in observation [:categorical-status :value] :live) authority))))
    (is (= :state-domain-mismatch
           (refusal #(sut/validate-observation!
                      (assoc-in observation [:categorical-status :value] :state/strengthened)
                      authority))))
    (is (= :ambiguous-categorical-evidence
           (refusal #(sut/validate-observation!
                      (assoc-in observation [:rubric :assertions]
                                [:support-gained :framing-sharpened]) authority))))
    (is (= :insufficient-categorical-evidence
           (refusal #(sut/validate-observation!
                      (assoc-in observation [:rubric :assertions] [:unknown-assertion])
                      authority))))))

(deftest authority-is-external-and-subject-bound-test
  (let [{:keys [observation authority]} (fixture)]
    (is (= :candidate-owned-review
           (refusal #(sut/validate-observation! (assoc observation :review {:verdict :accepted}) authority))))
    (is (= :authority-not-found
           (refusal #(sut/validate-observation!
                      (assoc-in observation [:authority :review/ref] :authority/forged)
                      authority))))
    (is (= :self-review
           (refusal #(sut/validate-observation!
                      observation
                      (update authority :resolver
                              (fn [resolve]
                                (fn [kind ref]
                                  (let [v (resolve kind ref)]
                                    (if (= kind :review)
                                      (let [record (assoc (review-for observation "observer/test-a")
                                                          :reviewer/id "observer/test-a")
                                            bs (edn-bytes record)
                                            path (Files/createTempFile "self-review" ".edn"
                                                                       (make-array java.nio.file.attribute.FileAttribute 0))]
                                        (Files/write path bs (make-array java.nio.file.OpenOption 0))
                                        (pointer path bs)) v)))))))))
    (is (= :borrowed-review
           (refusal #(sut/validate-observation!
                      observation
                      (update authority :resolver
                              (fn [resolve]
                                (fn [kind ref]
                                  (if (= kind :observer)
                                    (let [record {:schema sut/authority-schema
                                                  :role :categorical-state-observer
                                                  :principal/id "observer/other"}
                                          bs (edn-bytes record)
                                          path (Files/createTempFile "other-observer" ".edn"
                                                                     (make-array java.nio.file.attribute.FileAttribute 0))]
                                      (Files/write path bs (make-array java.nio.file.OpenOption 0))
                                      (pointer path bs))
                                    (resolve kind ref)))))))))
    (is (= :review-subject-mismatch
           (refusal #(sut/validate-observation!
                      (-> observation
                          (assoc-in [:categorical-status :value] :refined)
                          (assoc-in [:rubric :assertions] [:framing-sharpened]))
                      authority))))
    (is (= :review-subject-mismatch
           (refusal #(sut/validate-observation!
                      (assoc-in observation [:evidence :items 0 :kind] :artifact-inspection)
                      authority))))))

(deftest temporal-and-evidence-refusals-test
  (let [{:keys [observation authority]} (fixture)]
    (is (= :temporal-outcome-leakage
           (refusal #(sut/validate-observation!
                      (assoc-in observation [:point :evidence/cutoff-at] "2026-09-12T12:03:00Z")
                      authority))))
    (is (= :forbidden-evidence-source
           (refusal #(sut/validate-observation!
                      (assoc-in observation [:evidence :items 0 :kind] :target-disposition)
                      authority))))
    (is (= :evidence-after-cutoff
           (refusal #(sut/validate-observation!
                      (assoc-in observation [:evidence :items 0 :observed-at]
                                "2026-09-12T12:01:30Z") authority))))
    (is (= :missing-source
           (refusal #(sut/validate-observation!
                      (assoc-in observation [:evidence :items 0 :source :path] "/absent/test.edn")
                      authority))))))

(deftest strict-source-and-mutation-controls-test
  (let [good (edn-bytes {:a 1})
        bad-utf8 (byte-array [(unchecked-byte 0xc3) (unchecked-byte 0x28)])
        ptr {:path "virtual" :sha256 (sut/sha256-bytes bad-utf8)}]
    (is (= :invalid-utf8
           (refusal #(sut/read-pinned-form! ptr {:read-bytes (constantly bad-utf8)}))))
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
                                       (if (= 1 (swap! calls inc)) good (edn-bytes {:a 2})))})))))))

(deftest exact-identity-and-conflict-controls-test
  (let [{:keys [observation authority]} (fixture)]
    (is (= :missing-identity
           (refusal #(sut/validate-observation! (update observation :point dissoc :run/id)
                                                authority))))
    (is (= :observation-entity-mismatch
           (refusal #(sut/validate-observation!
                      observation (assoc-in authority [:expected :subject :entity/id] "entity/other")))))
    (is (= :observation-identity-mismatch
           (refusal #(sut/validate-observation!
                      observation (assoc-in authority [:expected :point :run/id] "run-other")))))
    ;; Each record is independently valid under a resolver that supplies its
    ;; exact review; only the collection-level point conflict refuses.
    (let [other (-> observation
                    (assoc :observation/id "test/observation-2")
                    (assoc-in [:categorical-status :value] :refined)
                    (assoc-in [:rubric :assertions] [:framing-sharpened])
                    (assoc-in [:authority :review/ref] :authority/review-b))
          dir (Files/createTempDirectory "categorical-conflict" (make-array java.nio.file.attribute.FileAttribute 0))
          path (.resolve dir "review.edn") bs (edn-bytes (review-for other "observer/test-a"))
          _ (Files/write path bs (make-array java.nio.file.OpenOption 0))
          resolve0 (:resolver authority)
          authority2 (assoc authority :resolver
                            (fn [kind ref]
                              (if (= [kind ref] [:review :authority/review-b])
                                (pointer path bs) (resolve0 kind ref))))]
      (is (= :categorical-observation-conflict
             (refusal #(sut/validate-observations! [observation other] authority2)))))))
