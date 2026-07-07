(ns futon2.aif.actuator-a3-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.actuator-a3 :as a3])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(deftest extracts-all-a3-corpus-deposits
  (let [by-id (a3/deposits-by-id)
        packages (mapv #(a3/extract-build-package (get by-id %)) a3/a3-corpus-ids)]
    (is (= 4 (count packages)))
    (is (every? :ok? packages)
        (pr-str (map #(select-keys % [:fold-turn/id :missing]) packages)))
    (is (every? #(seq (:policy-holes %)) packages))
    (is (every? #(get-in % [:structure-spec :wiring :nodes]) packages))
    (is (= a3/a3-corpus-ids (mapv :fold-turn/id packages)))))

(deftest reports-spec-quality-when-typespec-or-structure-missing
  (testing "missing typespec is reported, not silently skipped"
    (let [pkg (a3/extract-build-package
               {:fold-turn/id "ft-weak"
                :mission "futonx-d/mission/weak"
                :wiring {:nodes [{:id :b1}]
                         :terminals [{:id :b1 :discharges :want-signature-missing}]}
                :turn {:answer {:policy-holes []}}})]
      (is (false? (:ok? pkg)))
      (is (= :missing-typespec (-> pkg :missing first :reason)))))
  (testing "missing structure is reported, not silently skipped"
    (let [pkg (a3/extract-build-package
               {:fold-turn/id "ft-weak"
                :mission "futonx-d/mission/weak"
                :arrow-candidate {:have "h" :want "w"}
                :turn {:answer {:policy-holes []}}})]
      (is (false? (:ok? pkg)))
      (is (= :missing-structure (-> pkg :missing first :reason))))))

(defn- tmp-doc! [text]
  (let [dir (.toFile (Files/createTempDirectory "a3-doc-test"
                                                (make-array FileAttribute 0)))
        f (io/file dir "M-fixture.md")]
    (spit f text)
    (.getPath f)))

(def marker-a "build the capability vocabulary")
(def marker-b "wire typed capability observations")

(defn closure
  ([hole-id marker] (closure hole-id marker true))
  ([hole-id marker resolved?]
   {:hole-id hole-id
    :evidence-ref {:kind :file-exists :path (str "/tmp/" (name hole-id))}
    :closes-mission-hole marker
    :resolved? resolved?}))

(defn resolver-from-closure-flag [ref]
  (boolean (:resolved? ref)))

(deftest advance-mission-doc-closes-only-on-resolving-witness-and-present-hole
  (let [doc (tmp-doc! (str "# Fixture\n\n- [ ] " marker-a "\n- [ ] " marker-b "\n"))
        r (a3/review-partial
           {:mission-doc-path doc
            :closures [(closure :h1 marker-a true)]}
           {:resolver (fn [_] true)})]
    (is (true? (:dial-moved? r)))
    (is (= 2 (:before-open-hole-count r)))
    (is (= 1 (:after-open-hole-count r)))
    (is (= [marker-a] (mapv :closes-mission-hole (:holes-closed r))))
    (is (str/includes? (slurp doc) (str "- [x] " marker-a)))))

(deftest resolving-witness-with-absent-hole-does-not-move-dial
  (let [doc (tmp-doc! (str "# Fixture\n\n- [ ] " marker-a "\n"))
        r (a3/review-partial
           {:mission-doc-path doc
            :closures [(closure :h1 "not present in doc" true)]}
           {:resolver (fn [_] true)})]
    (is (false? (:dial-moved? r)))
    (is (= 1 (:before-open-hole-count r)))
    (is (= 1 (:after-open-hole-count r)))
    (is (= :hole-text-not-in-doc (-> r :rejected first :reason)))
    (is (str/includes? (slurp doc) (str "- [ ] " marker-a)))))

(deftest missing-or-unresolved-witness-with-present-hole-does-not-move-dial
  (testing "missing evidence"
    (let [doc (tmp-doc! (str "# Fixture\n\n- [ ] " marker-a "\n"))
          r (a3/review-partial
             {:mission-doc-path doc
              :closures [{:hole-id :h1 :closes-mission-hole marker-a}]}
             {:resolver (fn [_] true)})]
      (is (false? (:dial-moved? r)))
      (is (= :missing-evidence-ref (-> r :rejected first :reason)))
      (is (str/includes? (slurp doc) (str "- [ ] " marker-a)))))
  (testing "unresolved evidence"
    (let [doc (tmp-doc! (str "# Fixture\n\n- [ ] " marker-a "\n"))
          r (a3/review-partial
             {:mission-doc-path doc
              :closures [(closure :h1 marker-a false)]}
             {:resolver (fn [_] false)})]
      (is (false? (:dial-moved? r)))
      (is (= :unresolved-evidence-ref (-> r :rejected first :reason)))
      (is (str/includes? (slurp doc) (str "- [ ] " marker-a))))))

(deftest advance-mission-doc-is-idempotent
  (let [doc (tmp-doc! (str "# Fixture\n\n- [ ] " marker-a "\n"))
        c [(closure :h1 marker-a true)]
        first-run (a3/review-partial {:mission-doc-path doc :closures c}
                                     {:resolver (fn [_] true)})
        second-run (a3/review-partial {:mission-doc-path doc :closures c}
                                      {:resolver (fn [_] true)})]
    (is (true? (:dial-moved? first-run)))
    (is (false? (:dial-moved? second-run)))
    (is (= 0 (:after-open-hole-count first-run)))
    (is (= 0 (:after-open-hole-count second-run)))
    (is (= :hole-already-closed (-> second-run :rejected first :reason)))))

(deftest review-partial-breaks-down-mixed-set
  (let [doc (tmp-doc! (str "# Fixture\n\n- [ ] " marker-a "\n- [ ] " marker-b "\n"))
        r (a3/review-partial
           {:mission-doc-path doc
            :closures [(closure :h1 marker-a true)
                       (closure :h2 "absent marker" true)
                       (closure :h3 marker-b false)]}
           {:resolver (fn [ref] (not= "/tmp/h3" (:path ref)))})]
    (is (true? (:dial-moved? r)))
    (is (= [marker-a] (mapv :closes-mission-hole (:resolved r))))
    (is (= [:hole-text-not-in-doc :unresolved-evidence-ref]
           (mapv :reason (:rejected r))))
    (is (= [(str "- [ ] " marker-b)] (:holes-remaining r)))
    (is (str/includes? (:next-feedback r) ":hole-text-not-in-doc"))
    (is (str/includes? (:next-feedback r) marker-b))))

(deftest dry-run-over-corpus-is-deterministic
  (let [a (with-out-str
            (doseq [pkg (:packages (a3/run-a3! {:dry-run? true :all-corpus? true}))]
              (println (a3/render-package pkg))))
        b (with-out-str
            (doseq [pkg (:packages (a3/run-a3! {:dry-run? true :all-corpus? true}))]
              (println (a3/render-package pkg))))]
    (is (= a b))
    (is (str/includes? a "ft-learning-loop-010"))
    (is (str/includes? a "ft-pattern-mining-011"))))

(deftest result-truthy-rejects-empty-witness-values
  ;; the Drawbridge gaming-vector fix: a query that "ran fine" but found nothing
  ;; (count 0, empty seq, nil, false, blank) must NOT count as a resolved witness.
  (testing "falsy / empty results do not witness existence"
    (is (false? (a3/result-truthy? nil)))
    (is (false? (a3/result-truthy? false)))
    (is (false? (a3/result-truthy? 0)))
    (is (false? (a3/result-truthy? [])))
    (is (false? (a3/result-truthy? "")))
    (is (false? (a3/result-truthy? "  "))))
  (testing "genuine existence results witness"
    (is (true? (a3/result-truthy? 1)))
    (is (true? (a3/result-truthy? true)))
    (is (true? (a3/result-truthy? [:x])))
    (is (true? (a3/result-truthy? "found")))))
