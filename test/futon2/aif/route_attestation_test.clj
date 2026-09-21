(ns futon2.aif.route-attestation-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.route-attestation :as route]))

(def institution {:id :review-practice :version 1
                  :situation {:condition {:id :wm-work :version 1} :status :holds}
                  :preference {:id :warranted-increments :version 1 :engaged? true}
                  :valence :must})
(def criterion {:id :comparison-implemented :version 1 :institution {:id :review-practice :version 1}
                :target "M-aif-policy-conditioned-eig" :want ["M-aif-policy-conditioned-eig" :comparison-implemented]
                :scope {:repo "futon2" :revision "abc" :tests ["comparison-test"]}
                :kind :increment :evidence-kind :registered-test-warrant})
(def evidence {:criterion {:id :comparison-implemented :version 1} :kind :registered-test-warrant
               :status :present :target (:target criterion) :want (:want criterion) :scope (:scope criterion)
               :sha256 (apply str (repeat 64 "a")) :warrant-id "test-registry-fixture"
               :at "2026-09-21T01:00:00Z"})
(def input {:target (:target criterion)
            :declarations {:institutions [institution] :criteria [criterion]
                           :bindings [{:criterion {:id :comparison-implemented :version 1}
                                       :checkpoint :build :path [:increment]}]}
            :events {:dispatch {:event/sequence 4 :recorded-at "2026-09-21T00:30:00Z" :payload {:judgment {}}}
                     :build {:event/sequence 5 :recorded-at "2026-09-21T01:01:00Z" :payload {:judgment {:increment evidence}}}}})
(defn receipt-binding [x] (first (:bindings (route/receipt x))))

(deftest institutions-are-situational-not-weights
  (is (= :active (get-in (route/receipt input) [:institutions 0 :activation :status])))
  (doseq [[path value reason] [[[:situation :status] :not-holds :situation-not-applicable]
                             [[:preference :engaged?] false :preference-not-engaged]
                             [[:preference :engaged?] nil :applicability-unknown]
                             [[:weight] 1 :institution-is-not-a-weight]]]
    (let [r (route/receipt (update-in input [:declarations :institutions 0] assoc-in path value))]
      (is (= reason (get-in r [:institutions 0 :activation :reason])))
      (is (empty? (:increments r)))))
  (doseq [v [:must :may :may-not]]
    (is (= v (:valence (receipt-binding (assoc-in input [:declarations :institutions 0 :valence] v)))))))

(deftest matching-is-scoped-and-does-not-infer-a-warrant
  (let [b (receipt-binding input)]
    (is (= :matched (:status b)))
    (is (= {:status :present :want (:want criterion) :warrant-id "test-registry-fixture"
            :verification :supplied-not-checked} (:attestation b)))
    (is (= :build (get-in b [:source :checkpoint])))
    (is (= [:payload :judgment :increment] (get-in b [:source :path]))))
  (doseq [[x reason] [[(assoc-in input [:declarations :bindings] []) :binding-absent]
                      [(assoc-in input [:events :build :payload :judgment :increment :scope :revision] "other") :scope-mismatch]
                      [(assoc-in input [:events :build :payload :judgment :increment :target] "M-other") :target-mismatch]
                      [(assoc-in input [:events :build :payload :judgment :increment :kind] :prompt) :evidence-kind-mismatch]
                      [(assoc-in input [:events :build :payload :judgment :increment :kind] :selected-pattern) :evidence-kind-mismatch]
                      [(assoc-in input [:events :build :payload :judgment :increment :want] ["M-other" :done]) :want-mismatch]]]
    (is (= reason (:reason (receipt-binding x))))))

(deftest pre-dispatch-means-both-event-and-registration-precede-dispatch
  (let [x (-> input (assoc-in [:declarations :criteria 0 :kind] :stamp)
              (assoc-in [:declarations :criteria 0 :evidence-kind] :registration)
              (assoc-in [:declarations :criteria 0 :timing] :before-dispatch)
              (assoc-in [:events :build :payload :judgment :increment :kind] :registration))]
    (is (= :not-recorded-before-dispatch (:reason (receipt-binding x))))
    (is (= :not-recorded-before-dispatch
           (:reason (receipt-binding (assoc-in x [:events :build :payload :judgment :increment :at] "2026-09-21T00:01:00Z")))))
    (is (= :matched (:status (receipt-binding (-> x
                                         (assoc-in [:events :build :recorded-at] "2026-09-21T00:02:00Z")
                                         (assoc-in [:events :build :event/sequence] 3)
                                         (assoc-in [:events :build :payload :judgment :increment :at] "2026-09-21T00:01:00Z"))))))))

(deftest incremental-attestation-never-closes-the-mission-or-overwrites-observation
  (let [reference (edn/read-string (slurp "test/fixtures/run-narrative-1789964661.edn"))
        comparison {:status :compared :tokens (get-in reference [:cascade :outcomes])}
        original (pr-str reference)
        r (route/receipt (assoc input :token-comparison comparison :outcome :grounded-change))]
    (is (= comparison (:token-outcome-comparison r)))
    (is (false? (:observed (last (get-in r [:token-outcome-comparison :tokens])))))
    (is (= :increment (get-in r [:increments 0 :criterion :kind])))
    (is (not (contains? r :mission-closed?)))
    (is (= original (pr-str reference)))
    (is (= (dissoc r :token-outcome-comparison)
           (dissoc (route/receipt (assoc input :outcome :build-failed)) :token-outcome-comparison))))
  (is (empty? (:increments (route/receipt (assoc-in input [:declarations :criteria 0 :kind] :mission-closure))))))

(deftest profile-is-eleven-artifacts-or-gaps
  (let [r (route/receipt input)]
    (is (= route/principles (mapv :principle (:iad-profile r))))
    (is (every? #(= :gap (:status %)) (:iad-profile r))))
  (let [x (-> input (assoc-in [:declarations :iad-profile :4A] {:checkpoint :build :path [:artifact]})
              (assoc-in [:events :build :payload :judgment :artifact]
                        {:kind :artifact :path "test/comparison.clj" :sha256 (:sha256 evidence)}))]
    (is (= :artifact (get-in (route/receipt x) [:iad-profile 5 :status])))
    (is (= :gap (get-in (route/receipt (assoc-in x [:events :build :payload :judgment :artifact :score] 1))
                       [:iad-profile 5 :status])))))

(deftest retained-under-attempt-only-and-idempotent
  (let [root (.toFile (java.nio.file.Files/createTempDirectory "route-receipt-" (make-array java.nio.file.attribute.FileAttribute 0)))
        files #(set (map str (filter (fn [f] (.isFile f)) (file-seq (io/file "/home/joe/code/futon2/data")))))
        before (files)]
    (try
      (let [r (route/receipt input) a (route/retain! root "attempt/route" r)
            b (route/retain! root "attempt/route" r) path (get-in a [:reference :path])]
        (is (= (:reference a) (:reference b)))
        (is (= (get-in a [:reference :sha256])
               (apply str (map #(format "%02x" (bit-and 255 %))
                               (.digest (java.security.MessageDigest/getInstance "SHA-256")
                                        (java.nio.file.Files/readAllBytes (.toPath (io/file path))))))))
        (is (str/ends-with? path "/retained/route-attestation.edn"))
        (is (= r (edn/read-string (slurp path)))))
      (is (= before (files)))
      (finally (doseq [f (reverse (file-seq root))] (io/delete-file f true))))))

(deftest no-declarations-and-narrative
  (is (= :none-declared (:status (route/receipt {})))))

(deftest prohibited-evidence-is-not-an-attested-increment
  (is (empty? (:increments (route/receipt (assoc-in input [:declarations :institutions 0 :valence] :may-not))))))
