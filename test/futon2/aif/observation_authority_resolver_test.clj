(ns futon2.aif.observation-authority-resolver-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.categorical-state-observation :as observation]
            [futon2.aif.categorical-state-observation-test :as fixture]
            [futon2.aif.evidence-manifest :as manifest]
            [futon2.aif.observation-authority-resolver :as sut])
  (:import (java.nio.file Files Path)))

(defn refusal [f]
  (try (f) nil
       (catch clojure.lang.ExceptionInfo e
         (or (:refusal (ex-data e)) (:evidence-manifest/refusal (ex-data e))))))

(defn with-index [f index]
  (assoc (:resolver-inputs f) :index-source
         (fixture/write-record! (:dir f) "changed-index.edn" index)))

(defn change-evidence [index update-entry]
  (update index :entries
          #(mapv (fn [entry] (if (= :evidence (:kind entry)) (update-entry entry) entry)) %)))

(defn qualify [f inputs]
  (observation/validate-observation! (:candidate f) (sut/build-resolver! inputs)))

(deftest fixed-inputs-and-retained-identities-test
  (let [{:keys [resolver-inputs index records expected] :as f} (fixture/indexed-fixture)
        authority (sut/build-resolver! resolver-inputs)]
    (is (= :qualified (:status (qualify f resolver-inputs))))
    (doseq [[[kind ref] source] records]
      (is (= source ((:resolver authority) kind ref))))
    (is (= {:commission/ref :test/external-commission :index/id (:index/id index)
            :index-source (:index-source resolver-inputs)
            :manifest-sha256 (get-in resolver-inputs [:manifest :manifest-sha256])
            :subject (:subject expected) :point (:point expected)
            :authority/scope :test :authority/provenance (:authority/provenance expected)}
           (:identities authority)))
    (testing "no index mutation after construction changes the captured mapping"
      (spit (get-in resolver-inputs [:index-source :path]) "{}")
      (is (= (get records [:observer :authority/observer-a])
             ((:resolver authority) :observer :authority/observer-a)))
      (is (= :source-digest-mismatch
             (refusal #(sut/build-resolver! resolver-inputs)))))))

(deftest exact-reference-and-unique-binding-test
  (let [{:keys [resolver-inputs index] :as f} (fixture/indexed-fixture)
        resolve! (:resolver (sut/build-resolver! resolver-inputs))]
    (is (= :unsupported-authority-kind (refusal #(resolve! :registry :authority/observer-a))))
    (is (= :authority-not-found (refusal #(resolve! :review :authority/observer-a))))
    (is (= :authority-not-found (refusal #(resolve! :observer :authority/unknown))))
    (is (= :invalid-authority-reference (refusal #(resolve! :observer {:principal/id "self"}))))
    (doseq [kind [:observer :review :evidence]]
      (let [entry (first (filter #(= kind (:kind %)) (:entries index)))]
        (is (= :ambiguous-authority-binding
               (refusal #(sut/build-resolver! (with-index f (update index :entries conj entry))))))))
    (is (= :authority-not-found
           (refusal #(qualify f (with-index f (assoc index :entries []))))))
    (is (= :unsupported-authority-kind
           (refusal #(sut/build-resolver!
                       (with-index f (update index :entries conj
                                             {:kind :registry :ref :arbitrary
                                              :source (:index-source resolver-inputs)}))))))))

(deftest external-authority-required-test
  (let [{:keys [resolver-inputs index] :as f} (fixture/indexed-fixture)]
    (is (= :commission-required (refusal #(sut/build-resolver! (dissoc resolver-inputs :commission/ref)))))
    (is (= :missing-source-pointer (refusal #(sut/build-resolver! (dissoc resolver-inputs :index-source)))))
    (is (= :shape-invalid (refusal #(sut/build-resolver! (dissoc resolver-inputs :manifest)))))
    (is (= :resolver-input-invalid
           (refusal #(sut/build-resolver! (assoc resolver-inputs :candidate (:candidate f))))))
    (is (= :authority-index-invalid
           (refusal #(sut/build-resolver! (with-index f (assoc index :commission/ref :self-commission))))))
    (is (= :authority-manifest-mismatch
           (refusal #(sut/build-resolver! (assoc resolver-inputs :manifest
                                                (manifest/build-manifest {:entries [] :read-bytes identity}))))))))

(deftest manifest-identity-path-hash-and-cutoff-test
  (let [{:keys [index resolver-inputs] :as f} (fixture/indexed-fixture)]
    (is (= :evidence-not-admitted
           (refusal #(sut/build-resolver!
                       (with-index f (change-evidence index (fn [e] (assoc e :evidence/id "not-admitted"))))))))
    (doseq [[field value] [[:path "/different/evidence.edn"] [:sha256 (apply str (repeat 64 "0"))]]]
      (is (= :evidence-admission-mismatch
             (refusal #(sut/build-resolver!
                         (with-index f (change-evidence index (fn [e] (assoc-in e [:source field] value)))))))))
    (testing "a fresh valid manifest hash cannot conceal a late admission"
      (let [entry (first (get-in resolver-inputs [:manifest :entries]))
            late (manifest/build-manifest
                   {:entries [(-> entry (dissoc :sha256)
                                  (assoc :admitted-at "2026-09-12T12:01:01Z"))]
                    :read-bytes #(Files/readAllBytes (Path/of ^String % (make-array String 0)))})
            inputs (assoc (with-index f (assoc index :manifest-sha256 (:manifest-sha256 late)))
                          :manifest late)]
        (is (= :evidence-admitted-after-cutoff (refusal #(sut/build-resolver! inputs))))))
    (testing "manifest structural and digest validation is not trusted to the caller"
      (is (= :manifest-sha256-mismatch
             (refusal #(sut/build-resolver!
                         (assoc-in resolver-inputs [:manifest :manifest-sha256]
                                   (apply str (repeat 64 "0"))))))))))

(deftest exact-external-context-test
  (let [{:keys [resolver-inputs index] :as f} (fixture/indexed-fixture)]
    (doseq [[path value] [[[:subject :entity/id] "entity/other"]
                         [[:point :run/id] "run/other"]
                         [[:point :cohort/id] :cohort/other]
                         [[:point :attempt/id] "attempt/other"]
                         [[:point :checkpoint/ref] "checkpoint/other"]
                         [[:point :evidence/cutoff-at] "2026-09-12T12:01:01Z"]]]
      (is (= :authority-context-mismatch
             (refusal #(sut/build-resolver!
                         (assoc resolver-inputs :expected (assoc-in (:expected f) path value))))))
      (is (= :authority-context-mismatch
             (refusal #(sut/build-resolver!
                         (with-index f (update index :expected assoc-in path value)))))))
    (is (= :observation-entity-mismatch
           (refusal #(qualify (assoc-in f [:candidate :subject :entity/id] "entity/other") resolver-inputs))))
    (is (= :observation-identity-mismatch
           (refusal #(qualify (assoc-in f [:candidate :point :attempt/id] "attempt/other") resolver-inputs))))))

(deftest record-byte-and-pointer-integrity-stays-in-validator-test
  (doseq [kind [:evidence :observer :review]]
    (let [{:keys [resolver-inputs index records] :as f} (fixture/indexed-fixture)
          entry (first (filter #(= kind (:kind %)) (:entries index)))
          source (get records [kind (:ref entry)])
          authority (sut/build-resolver! resolver-inputs)]
      (spit (:path source) "{:tampered true}")
      (is (= :source-digest-mismatch
             (refusal #(observation/validate-observation! (:candidate f) authority))))))
  (doseq [kind [:observer :review]
          [field value reason] [[:sha256 (apply str (repeat 64 "0")) :source-digest-mismatch]
                                [:path "/missing/authority-record.edn" :missing-source]]]
    (let [{:keys [index] :as f} (fixture/indexed-fixture)
          changed (update index :entries
                          #(mapv (fn [entry] (if (= kind (:kind entry))
                                               (assoc-in entry [:source field] value) entry)) %))]
      (is (= reason (refusal #(qualify f (with-index f changed))))))))

(deftest candidate-cannot-supply-authority-or-review-test
  (let [{:keys [resolver-inputs index candidate] :as f} (fixture/indexed-fixture)]
    (is (= :candidate-owned-review
           (refusal #(qualify (assoc f :candidate (assoc candidate :review {:verdict :accepted})) resolver-inputs))))
    (is (= :invalid-authority-reference
           (refusal #(qualify (assoc-in f [:candidate :authority :observer/ref]
                                        {:role :categorical-state-observer :principal/id "self"}) resolver-inputs))))
    (is (= :invalid-authority-reference
           (refusal #(qualify (assoc-in f [:candidate :authority :review/ref]
                                        {:role :categorical-state-reviewer :verdict :accepted}) resolver-inputs))))
    (testing "candidate-attached index/manifest/resolver cannot replace missing external bindings"
      (let [forged (update f :candidate assoc
                           :index index :manifest (:manifest resolver-inputs)
                           :resolver (fn [& _] (:index-source resolver-inputs)))
            no-observer (update index :entries #(filterv (fn [e] (not= :observer (:kind e))) %))]
        (is (= :authority-not-found (refusal #(qualify forged (with-index f no-observer)))))))))

(deftest exact-review-subject-and-role-remain-required-test
  (doseq [[change reason] [[#(assoc % :subject [:different-subject]) :review-subject-mismatch]
                           [#(assoc % :subject/sha256 (apply str (repeat 64 "0"))) :review-subject-mismatch]
                           [#(assoc % :reviewer/id "observer/test-a") :self-review]
                           [#(assoc % :authority/scope :production) :review-unauthorized]]]
    (let [{:keys [records dir index] :as f} (fixture/indexed-fixture)
          review (observation/read-pinned-form! (get records [:review :authority/review-a]))
          source (fixture/write-record! dir "different-review.edn" (change review))
          changed (update index :entries
                          #(mapv (fn [e] (if (= :review (:kind e)) (assoc e :source source) e)) %))]
      (is (= reason (refusal #(qualify f (with-index f changed))))))))
