(ns futon2.aif.receipt-construction-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.set :as set]
            [clojure.java.io :as io]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.evidence-manifest :as manifest]
            [futon2.aif.cascade-policy :as policy]
            [futon2.aif.receipt-construction :as construction]
            [futon2.aif.find-receipt-test :as fixture]
            [futon2.aif.interpretation-evidence :as evidence])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.time Instant]
           [java.util UUID]))

(def absent-score {:status :none :reason :cascade-g-not-computed})
(def authority {:authority :documented-interpretation :source "previous-occurrence/fixture"})
(defn refusal [f] (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))

(deftest reachability-order-and-admissibility-control
  (let [edges #{[:a :b] [:b :c]} order (construction/precedence #{:a :c} edges)]
    (is (= [:c :a] order))
    (is (construction/validate-admissible! order #{:a :c} #{[:a :c]}))
    (is (= :inadmissible-order (:construction/refusal
                               (refusal #(construction/validate-admissible! [:a :c] #{:a :c} #{[:a :c]})))))))

(deftest admitted-closure-node-is-not-added-and-can-fire
  (let [repo {:patterns #{:a :b} :stands-on #{[:a :b]}}
        interpretations {:a {:guard [:fact "ready"] :effect {"done" true}}
                         :b {:guard [:fact "ready"] :effect {"support" true}}}
        d (policy/organise policy/first-attempt-cascade #{:a} repo {:b authority}
                           {:temperament (assoc policy/up-closure-temperament :precedence [:b :a])
                            :acting-order-fn #(construction/acting-order interpretations {"ready" true} (:precedence %))
                            :score-fn (constantly absent-score)})]
    (is (= #{} (:added-by-organise d)))
    (is (= #{:a :b} (:nodes d) (set/difference (:nodes d) (:added-by-organise d))))
    (is (= #{[:a :b]} (:organised-edges d)))
    (is (= [:b :a] (:acting-order-after d)))
    (is (= {:b authority} (get-in d [:provenance :admissions])))))

(deftest first-firing-applies-effects-and-never-repeats
  (let [xs {:a {:guard [:fact "start"] :effect {"next" true "start" false}}
            :b {:guard [:fact "next"] :effect {"start" true}}}]
    (is (= [:a :b] (construction/acting-order xs {"start" true "next" false} [:b :a])))
    (is (= [] (construction/acting-order {:a {:guard [:not [:fact "x"]] :effect {"x" true}}} {"x" :unknown} [:a])))))

(deftest construction-ports-and-negative-controls
  (let [{:keys [record captured id]} (fixture/sample)
        prev {:cascade policy/first-attempt-cascade :admitted {}
              :provenance {:status :none :reason :no-earlier-target-construction}
              :admission-reason :first-attempt-no-admissions}
        run #(construction/construct % captured fixture/root prev nil)
        c (run record)]
    (is (= (get-in record [:target :action]) (:selected-action c)))
    (let [original (get-in record [:identity :occurrence :action/value])
          reordered (into (array-map) (reverse (seq original)))]
      (is (not= (pr-str original) (pr-str reordered)))
      (is (= (pr-str original) (pr-str (:selected-action (run (assoc-in record [:target :action] reordered)))))))
    (is (= (get-in c [:receipted-construction :cascade-diff-sha256])
           (evidence/value-digest (get-in c [:receipted-construction :cascade-diff]))))
    (is (= :F4 (:law (refusal #(construction/construct record captured fixture/root prev #{id})))))
    (let [with-admission (assoc prev :cascade {:nodes #{id} :edges #{} :precedence [id]}
                               :admitted {id authority} :admission-reason :carried-from-previous-occurrence)
          carried (construction/construct record captured fixture/root with-admission nil)]
      (is (= {id authority} (get-in carried [:receipted-construction :cascade-diff :provenance :admissions]))))
    (let [organise policy/organise]
      (with-redefs [policy/organise (fn [& args] (update (apply organise args) :added-by-organise conj id))]
        (is (= :bootstrap-carrier-mismatch (:construction/refusal (refusal #(run record)))))))
    (is (:interpretation-evidence/refusal
         (refusal #(run (-> record (assoc-in [:interpretations 0 :clauses :if :quote] "wrong bytes")
                            (update-in [:interpretations 0] fixture/seal))))))
    (is (= :interpretation/no-relevant-pattern (:interpretation/refusal
                                               (refusal #(run (assoc-in record [:facts 0 :value] false))))))))

(defn history-fixture! [root epoch at closed-at]
  (let [action {:type :advance-mission :target "M-history"}
        occurrence (retention/mint-occurrence {:run-id (str (UUID/randomUUID)) :cohort-id ":fixture"
                                              :attempt-id "attempt-001" :selected-action action
                                              :now #(Instant/parse at) :uuid-fn #(UUID/randomUUID)})
        dir (io/file root "fixture" "attempt-001")
        start-file (io/file dir "001-time-step.edn")
        construction-file (io/file dir "003-construction.edn")
        close-file (io/file dir "007-closed.edn")
        _ (.mkdirs dir)
        _ (spit start-file (pr-str {:payload {:judgment {:semantic-epoch epoch}}}))
        identity {:occurrence occurrence :semantic-epoch epoch :data-root (.getCanonicalPath (io/file root))
                  :start-event-sha256 (evidence/sha256 (Files/readAllBytes (.toPath start-file)))
                  :interpreter-job "prior-job" :author "fixture" :schema-version 1}
        diff (policy/organise policy/first-attempt-cascade #{:a}
                              {:patterns #{:a :b} :stands-on #{[:a :b]}} {:b authority}
                              {:temperament (assoc policy/up-closure-temperament :precedence [:b :a])
                               :acting-order-fn (fn [c] (:precedence c)) :score-fn (constantly absent-score)})
        construction {:cohort/id :fixture :attempt/id "attempt-001"
                      :payload {:judgment {:mission "M-history" :cascade {}
                                           :receipted-construction {:identity identity :cascade-diff diff
                                                                    :cascade-diff-sha256 (evidence/value-digest diff)}}}}
        _ (spit construction-file (pr-str construction))
        m (manifest/build-manifest {:entries (mapv (fn [f] {:evidence/id (.getName f) :source-path (.getCanonicalPath f)
                                                          :admitted-at closed-at}) [start-file construction-file])
                                    :read-bytes #(Files/readAllBytes (.toPath (io/file %)))})
        block (retention/build-retention-block {:occurrence occurrence :state {:status :absent :reason :test}
                                               :model {:status :absent :reason :declared-model-identity-unthreaded} :closed-at closed-at :evidence-cutoff closed-at
                                               :admitted-evidence (mapv :evidence/id (:entries m))})
        close {:recorded-at closed-at :payload {:close-retention block :close-evidence-manifest m}}]
    (spit close-file (pr-str close))
    {:identity identity :close close :close-file close-file :construction-file construction-file}))

(deftest prior-construction-joins-full-occurrence-across-reused-names
  (let [temp (.toFile (Files/createTempDirectory "receipt-history" (make-array FileAttribute 0)))
        r1 (io/file temp "first") r2 (io/file temp "second")]
    (try
      (let [old (history-fixture! r1 :epoch-one "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z")
            newer (history-fixture! r2 :epoch-two "2026-09-15T11:00:00Z" "2026-09-15T11:01:00Z")
            current (assoc (:identity newer) :occurrence
                           (retention/mint-occurrence {:run-id (str (UUID/randomUUID)) :cohort-id ":fixture"
                                                       :attempt-id "attempt-001" :selected-action {:type :advance-mission :target "M-history"}
                                                       :now #(Instant/parse "2026-09-15T12:00:00Z") :uuid-fn #(UUID/randomUUID)}))
            previous (construction/previous! current [r1 r2])]
        (is (= (:identity newer) (get-in previous [:provenance :identity])))
        (is (= {:b authority} (:admitted previous)))
        (is (= [:b :a] (get-in previous [:cascade :precedence])))
        ;; Same attempt/cohort names cannot replace the full occurrence join.
        (spit (:close-file newer) (pr-str (assoc-in (:close newer) [:payload :close-retention :occurrence]
                                                  (get-in old [:identity :occurrence]))))
        (is (= :previous-occurrence-mismatch
               (:construction/refusal (refusal #(construction/previous! current [r1 r2]))))))
      (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))

(deftest legacy-predecessor-resets-with-recorded-provenance
  ;; Real July closes carry no close-retention, and their constructions no
  ;; :receipted-construction (futon2/data/wm-full-loop/wm-outer-loop-41-v1/attempt-043).
  (let [temp (.toFile (Files/createTempDirectory "receipt-legacy" (make-array FileAttribute 0)))
        dir (io/file temp "legacy-cohort" "attempt-001")]
    (try
      (.mkdirs dir)
      (spit (io/file dir "001-time-step.edn") (pr-str {:payload {:judgment {:semantic-epoch :legacy}}}))
      (spit (io/file dir "003-construction.edn")
            (pr-str {:cohort/id :legacy-cohort :attempt/id "attempt-001"
                     :payload {:judgment {:mission "M-history" :cascade {:shown ["family/old"]}}}}))
      (spit (io/file dir "007-closed.edn")
            (pr-str {:recorded-at "2026-07-21T10:08:23Z" :payload {:judgment {:outcome :build-failed}}}))
      (let [current {:occurrence (retention/mint-occurrence
                                  {:run-id (str (UUID/randomUUID)) :cohort-id ":fixture" :attempt-id "attempt-002"
                                   :selected-action {:type :advance-mission :target "M-history"}
                                   :now #(Instant/parse "2026-09-15T12:00:00Z") :uuid-fn #(UUID/randomUUID)})}
            previous (construction/previous! current [temp])]
        (is (= policy/first-attempt-cascade (:cascade previous)))
        (is (= {} (:admitted previous)))
        (is (= :legacy-predecessor-unrepresentable (get-in previous [:provenance :status])))
        (is (= :legacy-predecessor-no-ruled-admissions (:admission-reason previous)))
        (is (= (evidence/sha256 (Files/readAllBytes (.toPath (io/file dir "003-construction.edn"))))
               (get-in previous [:provenance :construction-sha256]))))
      (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))
