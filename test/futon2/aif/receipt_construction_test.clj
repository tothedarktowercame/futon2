(ns futon2.aif.receipt-construction-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
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
  (let [fixture-cohort (keyword (str "fixture-" (name epoch)))
        action {:type :advance-mission :target "M-history"}
        occurrence (retention/mint-occurrence {:run-id (str (UUID/randomUUID)) :cohort-id (str fixture-cohort)
                                              :attempt-id "attempt-001" :selected-action action
                                              :now #(Instant/parse at) :uuid-fn #(UUID/randomUUID)})
        dir (io/file root (name fixture-cohort) "attempt-001")
        start-file (io/file dir "001-time-step.edn")
        selection-file (io/file dir "002-selection.edn")
        construction-file (io/file dir "003-construction.edn")
        close-file (io/file dir "007-closed.edn")
        _ (.mkdirs dir)
        event (fn [index checkpoint time payload]
                {:event/schema-version 1 :cohort/id fixture-cohort :attempt/id "attempt-001"
                 :attempt/ordinal 1 :event/sequence index :checkpoint/type checkpoint
                 :recorded-at time :payload payload})
        _ (spit start-file (pr-str (event 1 :time-step at {:judgment {:semantic-epoch epoch} :ground {:kind :fixture-start}})))
        _ (spit selection-file (pr-str (event 2 :selection at {:judgment {:selected-action action :selected-mission "M-history"}
                                                             :ground {:kind :fixture-selection}})))
        identity {:occurrence occurrence :semantic-epoch epoch :data-root (.getCanonicalPath (io/file root))
                  :start-event-sha256 (evidence/sha256 (Files/readAllBytes (.toPath start-file)))
                  :interpreter-job "prior-job" :author "fixture" :schema-version 1}
        diff (policy/organise policy/first-attempt-cascade #{:a}
                              {:patterns #{:a :b} :stands-on #{[:a :b]}} {:b authority}
                              {:temperament (assoc policy/up-closure-temperament :precedence [:b :a])
                               :acting-order-fn (fn [c] (:precedence c)) :score-fn (constantly absent-score)})
        construction (event 3 :construction at
                            {:judgment {:mission "M-history" :cascade {}
                                        :receipted-construction {:identity identity :cascade-diff diff
                                                                 :cascade-diff-sha256 (evidence/value-digest diff)}}
                             :ground {:kind :fixture-construction}})
        _ (spit construction-file (pr-str construction))
        m (manifest/build-manifest {:entries (mapv (fn [f] {:evidence/id (.getName f) :source-path (.getCanonicalPath f)
                                                          :admitted-at closed-at}) [start-file selection-file construction-file])
                                    :read-bytes #(Files/readAllBytes (.toPath (io/file %)))})
        block (retention/build-retention-block {:occurrence occurrence :state {:status :absent :reason :test}
                                               :model {:status :absent :reason :declared-model-identity-unthreaded} :closed-at closed-at :evidence-cutoff closed-at
                                               :admitted-evidence (mapv :evidence/id (:entries m))})
        close (event 7 :closed closed-at {:judgment {:outcome :build-failed}
                                         :ground {:kind :full-loop-outcome :attempt-id "attempt-001"}
                                         :close-retention block :close-evidence-manifest m})]
    (spit close-file (pr-str close))
    {:identity identity :close close :close-file close-file :construction-file construction-file}))

(deftest prior-construction-joins-full-occurrence-across-distinct-cohorts
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
        ;; Distinct cohort names still cannot replace the full occurrence join.
        (spit (:close-file newer) (pr-str (assoc-in (:close newer) [:payload :close-retention :occurrence]
                                                  (get-in old [:identity :occurrence]))))
        (is (= :previous-occurrence-mismatch
               (:construction/refusal (refusal #(construction/previous! current [r1 r2]))))))
      (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))

(deftest old-shaped-predecessor-refuses-current-evidence-gap
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
            (pr-str {:cohort/id :legacy-cohort :attempt/id "attempt-001" :recorded-at "2026-07-21T10:08:23Z" :payload {:judgment {:outcome :build-failed}}}))
      (let [current {:occurrence (retention/mint-occurrence
                                  {:run-id (str (UUID/randomUUID)) :cohort-id ":fixture" :attempt-id "attempt-002"
                                   :selected-action {:type :advance-mission :target "M-history"}
                                   :now #(Instant/parse "2026-09-15T12:00:00Z") :uuid-fn #(UUID/randomUUID)})}
            rejected (refusal #(construction/previous! current [temp]))]
        (is (= :previous-occurrence-unavailable (:construction/refusal rejected)))
        (is (= (str (io/file dir "007-closed.edn")) (:history/close-file rejected))))
      (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))

(deftest before-arm-reuses-the-recorded-acting-order
  ;; The previous precedence names a pattern this receipt never interprets.
  ;; Re-simulating it would refuse (:previous-or-admitted-interpretation-missing);
  ;; the before arm must use the previous attempt's recorded acting order.
  (let [{:keys [record captured id]} (fixture/sample)
        prev {:cascade {:nodes #{:other/pattern} :edges #{} :precedence [:other/pattern]}
              :acting-order [:other/pattern]
              :admitted {}
              :provenance {:status :fixture}
              :admission-reason :carried-from-previous-occurrence}
        c (construction/construct record captured fixture/root prev nil)
        d (get-in c [:receipted-construction :cascade-diff])]
    (is (= [:other/pattern] (:acting-order-before d)))
    (is (= [:other/pattern] (:precedence-before d)))
    (is (= [id] (:acting-order-after d)))))

(defn rewrite-history! [file f]
  (spit file (pr-str (f (edn/read-string (slurp file))))))

(deftest damaged-latest-history-never-resets-or-falls-back
  (doseq [with-older? [false true]
          [label damage expected]
          [[:carrier-removed #(rewrite-history! (:construction-file %) (fn [x] (update-in x [:payload :judgment] dissoc :receipted-construction))) :previous-cascade-carrier-unavailable]
           [:carrier-malformed #(rewrite-history! (:construction-file %) (fn [x] (assoc-in x [:payload :judgment :receipted-construction] {}))) :previous-manifest-source-mismatch]
           [:cascade-removed #(rewrite-history! (:construction-file %) (fn [x] (update-in x [:payload :judgment] dissoc :cascade))) :previous-cascade-unavailable]
           [:construction-missing #(Files/delete (.toPath (:construction-file %))) :previous-cascade-unavailable]
           [:manifest-missing #(rewrite-history! (:close-file %) (fn [x] (update x :payload dissoc :close-evidence-manifest))) :previous-evidence-invalid]
           [:manifest-corrupt #(rewrite-history! (:close-file %) (fn [x] (assoc-in x [:payload :close-evidence-manifest] {}))) :previous-evidence-invalid]
           [:occurrence-missing #(rewrite-history! (:close-file %) (fn [x] (update-in x [:payload :close-retention] dissoc :occurrence))) :previous-occurrence-unavailable]
           [:occurrence-corrupt #(rewrite-history! (:close-file %) (fn [x] (assoc-in x [:payload :close-retention :occurrence] {}))) :previous-evidence-invalid]
           [:target-conflict #(rewrite-history! (:construction-file %) (fn [x] (assoc-in x [:payload :judgment :mission] "M-other"))) :history-discovery-invalid]
           [:order-missing #(rewrite-history! (:close-file %) (fn [x] (dissoc x :recorded-at))) :history-discovery-invalid]
           [:order-conflict #(rewrite-history! (:close-file %) (fn [x] (assoc x :recorded-at "2026-09-16T12:00:00Z"))) :history-discovery-invalid]
           [:order-corrupt #(rewrite-history! (:close-file %) (fn [x] (assoc x :recorded-at "not-a-time"))) :history-discovery-invalid]
           [:unreadable-edn #(spit (:close-file %) "{") :history-discovery-invalid]
           [:multiple-forms #(spit (:close-file %) "{} {}") :history-discovery-invalid]
           [:missing-targets (fn [x]
                               (Files/delete (.toPath (:construction-file x)))
                               (Files/delete (.toPath (io/file (.getParentFile (:construction-file x)) "002-selection.edn")))
                               (rewrite-history! (:close-file x) #(update-in % [:payload :close-retention] dissoc :occurrence))) :history-discovery-invalid]]]
    (testing (str (name label) " older=" with-older?)
      (let [temp (.toFile (Files/createTempDirectory "receipt-damage" (make-array FileAttribute 0)))
            older (io/file temp "older") newer (io/file temp "newer")]
        (try
          (when with-older?
            (history-fixture! older :older "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z"))
          (let [latest (history-fixture! newer :newer "2026-09-15T11:00:00Z" "2026-09-15T11:01:00Z")
                current (assoc-in (:identity latest) [:occurrence :action-at] "2026-09-15T12:00:00Z")]
            (damage latest)
            (let [error (refusal #(construction/previous! current (if with-older? [older newer] [newer])))]
              (is (= expected (:construction/refusal error)))
              (is (or (:file error) (:history/close-file error)))))
          (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))))

(deftest discovery-establishes-absence-and-compares-instants
  (let [temp (.toFile (Files/createTempDirectory "receipt-discovery" (make-array FileAttribute 0)))
        first-root (io/file temp "first") second-root (io/file temp "second")
        current {:occurrence {:action/value {:target "M-history"} :action-at "2026-09-15T12:00:00Z"}}]
    (try
      (is (= :first-attempt-no-admissions (:admission-reason (construction/previous! current [temp]))))
      (is (= :history-discovery-invalid (:construction/refusal (refusal #(construction/previous! current [(io/file temp "absent")])))))
      (is (= :history-discovery-invalid (:construction/refusal (refusal #(construction/previous! current [])))))
      (is (= :history-discovery-invalid (:construction/refusal (refusal #(construction/previous! current [nil])))))
      (spit (io/file temp "not-a-root") "file")
      (is (= :history-discovery-invalid (:construction/refusal (refusal #(construction/previous! current [(io/file temp "not-a-root")])))))
      (history-fixture! first-root :earlier "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z")
      ;; Valid modern evidence proves this history unrelated to a different target.
      (is (= :first-attempt-no-admissions
             (:admission-reason (construction/previous! (assoc-in current [:occurrence :action/value :target] "M-other") [first-root]))))
      ;; Different textual representations of the same instant remain ambiguous.
      (history-fixture! second-root :same-time "2026-09-15T10:00:00Z" "2026-09-15T11:01:00+01:00")
      (is (= :ambiguous-previous-construction
             (:construction/refusal (refusal #(construction/previous! current [first-root second-root])))))
      (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))

(deftest resealed-carriers-reach-carrier-validation
  (doseq [[label mutate expected]
          [[:unchanged identity nil]
           [:removed #(update-in % [:payload :judgment] dissoc :receipted-construction)
            :previous-cascade-carrier-unavailable]
           [:malformed #(assoc-in % [:payload :judgment :receipted-construction] {})
            :previous-evidence-invalid]]]
    (testing (name label)
      (let [temp (.toFile (Files/createTempDirectory "receipt-resealed" (make-array FileAttribute 0)))]
        (try
          (let [latest (history-fixture! temp :sealed "2026-09-15T11:00:00Z" "2026-09-15T11:01:00Z")
                current (assoc-in (:identity latest) [:occurrence :action-at] "2026-09-15T12:00:00Z")]
            (rewrite-history! (:construction-file latest) mutate)
            (rewrite-history! (:close-file latest)
                              (fn [closed]
                                (assoc-in closed [:payload :close-evidence-manifest]
                                          (manifest/build-manifest
                                           {:entries (mapv #(select-keys % [:evidence/id :source-path :admitted-at])
                                                           (get-in closed [:payload :close-evidence-manifest :entries]))
                                            :read-bytes #(Files/readAllBytes (.toPath (io/file %)))}))))
            (if expected
              (let [error (refusal #(construction/previous! current [temp]))]
                (is (= expected (:construction/refusal error)))
                (is (:history/construction-file error))
                (when (= label :malformed)
                  (is (= :shape-invalid (:interpretation-evidence/refusal error)))
                  (is (= [:identity] (:path error)))))
              (is (= :carried-from-previous-occurrence
                     (:admission-reason (construction/previous! current [temp]))))))
          (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))))

(defn non-construction-fixture! [root selected? manifest?]
  ;; Producer-shaped checkpoint envelopes and marker, pinned by revision 3 to
  ;; machinery-52/attempt-002; all values below remain hermetic fixture data.
  (let [dir (io/file root "fixture" "attempt-002")
        _ (.mkdirs dir)
        action {:type :advance-mission :target "M-history"}
        outcome (if selected? :agent-unavailable :no-selection)
        types [:time-step :selection :construction :dispatch :build :adjudication :closed]
        files (mapv #(io/file dir (format "%03d-%s.edn" %1 (name %2))) (range 1 8) types)
        records (mapv (fn [index checkpoint]
                        {:event/schema-version 1 :cohort/id :fixture :attempt/id "attempt-002"
                         :attempt/ordinal 2 :event/sequence index :checkpoint/type checkpoint
                         :recorded-at (format "2026-09-15T11:%02d:00Z" index)
                         :payload (case checkpoint
                                    :time-step {:judgment {} :ground {:kind :fixture-start}}
                                    :selection (if selected?
                                                 {:judgment {:selected-action action :selected-mission "M-history"}
                                                  :ground {:kind :fixture-selection}}
                                                 {:sorry {:kind :no-selection :decision nil}})
                                    :closed {:judgment {:outcome outcome :grounded? false :artifact-only? false :witness nil}
                                             :ground {:kind :full-loop-outcome :attempt-id "attempt-002"}}
                                    {:sorry {:kind (keyword (str "not-reached-" (name checkpoint))) :outcome outcome}})})
                      (range 1 8) types)]
    (doseq [[file record] (map vector files records)] (spit file (pr-str record)))
    (when manifest?
      (let [m (manifest/build-manifest {:entries (mapv (fn [file] {:evidence/id (.getName file)
                                                                :source-path (.getCanonicalPath file)
                                                                :admitted-at "2026-09-15T11:07:00Z"}) (butlast files))
                                        :read-bytes #(Files/readAllBytes (.toPath (io/file %)))})
            occurrence (retention/mint-occurrence {:run-id (str (UUID/randomUUID)) :cohort-id ":fixture"
                                                   :attempt-id "attempt-002" :selected-action action
                                                   :now #(Instant/parse "2026-09-15T11:01:00Z") :uuid-fn #(UUID/randomUUID)})
            block (retention/build-retention-block {:occurrence occurrence :state {:status :absent :reason :test}
                                                     :model {:status :absent :reason :declared-model-identity-unthreaded}
                                                     :closed-at "2026-09-15T11:07:00Z" :evidence-cutoff "2026-09-15T11:07:00Z"
                                                     :admitted-evidence (mapv :evidence/id (:entries m))})]
        (rewrite-history! (last files) #(update % :payload assoc :close-retention block :close-evidence-manifest m))))
    files))

(deftest positive-non-construction-exclusion-retains-provenance
  (doseq [[selected? manifest?] [[true true] [true false] [false false]] with-older? [true false]]
    (let [temp (.toFile (Files/createTempDirectory "receipt-nonconstruction" (make-array FileAttribute 0)))
          older (io/file temp "old") marker (io/file temp "marker")
          current {:occurrence {:action/value {:target "M-history"} :action-at "2026-09-15T12:00:00Z"}}]
      (try
        (when with-older? (history-fixture! older :older "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z"))
        (non-construction-fixture! marker selected? manifest?)
        (let [result (construction/previous! current (if with-older? [older marker] [marker]))
              excluded (get-in result [:provenance :excluded-attempts])]
          (is (= (if with-older? :carried-from-previous-occurrence :first-attempt-no-admissions) (:admission-reason result)))
          (is (= 1 (count excluded)))
          (is (= :producer-not-reached-construction (:reason (first excluded))))
          (is (= 7 (count (:records (first excluded))))))
        (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f))))))))

(deftest non-construction-cannot-hide-missing-or-contradictory-records
  (doseq [[label damage]
          [[:missing #(Files/delete (.toPath (nth % 2)))]
           [:cascade #(rewrite-history! (nth % 2) (fn [x] (assoc-in x [:payload :judgment :cascade] {})))]
           [:carrier #(rewrite-history! (nth % 2) (fn [x] (assoc-in x [:payload :judgment :receipted-construction] {})))]
           [:identity #(rewrite-history! (nth % 2) (fn [x] (assoc x :attempt/id "attempt-999")))]
           [:checkpoint #(rewrite-history! (nth % 2) (fn [x] (assoc x :checkpoint/type :selection)))]
           [:unknown-marker #(rewrite-history! (nth % 2) (fn [x] (assoc-in x [:payload :sorry :kind] :unknown)))]
           [:malformed-marker #(rewrite-history! (nth % 2) (fn [x] (assoc-in x [:payload :sorry] 7)))]
           [:manifest #(rewrite-history! (last %) (fn [x] (assoc-in x [:payload :close-evidence-manifest] {})))]
           [:manifest-source #(spit (first %) (str (slurp (first %)) " "))]
           [:later-construction #(rewrite-history! (nth % 3) (fn [x] (assoc x :payload {:judgment {} :ground {:kind :dispatch}})))]
           [:close-claim #(rewrite-history! (last %) (fn [x] (assoc-in x [:payload :judgment :grounded?] true)))]]]
    (testing (name label)
      (let [temp (.toFile (Files/createTempDirectory "receipt-marker-refusal" (make-array FileAttribute 0)))
            current {:occurrence {:action/value {:target "M-history"} :action-at "2026-09-15T12:00:00Z"}}]
        (try
          (damage (non-construction-fixture! temp true true))
          (let [error (refusal #(construction/previous! current [temp]))]
            (is (:construction/refusal error))
            (is (or (:file error) (:history/construction-file error))))
          (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))))

(deftest excluded-marker-does-not-recover-unsupported-actual-history
  (let [temp (.toFile (Files/createTempDirectory "receipt-marker-no-recovery" (make-array FileAttribute 0)))
        older (io/file temp "actual") marker (io/file temp "marker")
        current {:occurrence {:action/value {:target "M-history"} :action-at "2026-09-15T12:00:00Z"}}]
    (try
      (let [actual (history-fixture! older :actual "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z")]
        (rewrite-history! (:construction-file actual) #(update-in % [:payload :judgment] dissoc :receipted-construction))
        (non-construction-fixture! marker false false)
        (let [error (refusal #(construction/previous! current [older marker]))]
          (is (= :previous-cascade-carrier-unavailable (:construction/refusal error)))
          (is (= 1 (count (:history/excluded-attempts error))))))
      (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))

(defn discovery-history! [root target rejected?]
  (let [files (non-construction-fixture! root true false)
        action {:type :advance-mission :target target}]
    (rewrite-history! (nth files 1)
                      #(assoc % :payload {:judgment {:selected-action action :selected-mission target}
                                          :ground {:kind :fixture-selection}}))
    (rewrite-history! (nth files 2)
                      #(assoc % :payload (if rejected?
                                           {:sorry {:kind :invalid-checkpoint-cell :outcome :incomplete
                                                    :refused-checkpoint :construction :cell-errors [[:invalid-fold-output :fixture]]}}
                                           {:judgment {:mission (str target) :cascade {:construction-kind :selected-policy}}
                                            :ground {:kind :decision-pinned-construction :selected-action action}})))
    (when rejected?
      (rewrite-history! (last files) #(assoc-in % [:payload :judgment :outcome] :incomplete)))
    files))

(deftest rejected-construction-remains-a-predecessor
  (doseq [older? [false true]]
    (let [temp (.toFile (Files/createTempDirectory "receipt-rejection" (make-array FileAttribute 0)))
          old (io/file temp "old") latest (io/file temp "latest")
          current {:occurrence {:action/value {:target "M-history"} :action-at "2026-09-15T12:00:00Z"}}]
      (try
        (when older? (history-fixture! old :older "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z"))
        (let [files (discovery-history! latest "M-history" true)
              error (refusal #(construction/previous! current (if older? [old latest] [latest])))]
          (is (= :previous-construction-rejected (:construction/refusal error)))
          (is (= (str (nth files 2)) (:file error)))
          (is (= :invalid-checkpoint-cell (get-in error [:history/rejection :kind])))
          (is (= [[:invalid-fold-output :fixture]] (get-in error [:history/rejection :cell-errors])))
          (is (= (evidence/sha256 (Files/readAllBytes (.toPath (nth files 2)))) (:history/construction-sha256 error))))
        (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f))))))))

(deftest unrelated-history-needs-relevance-evidence-not-modern-carrier
  (doseq [older? [false true] rejected? [false true]]
    (let [temp (.toFile (Files/createTempDirectory "receipt-unrelated" (make-array FileAttribute 0)))
          old (io/file temp "old") other (io/file temp "other")
          current {:occurrence {:action/value {:target "M-history"} :action-at "2026-09-15T12:00:00Z"}}]
      (try
        (when older? (history-fixture! old :older "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z"))
        (let [files (discovery-history! other "M-other" rejected?)
              roots (if older? [old other] [other])
              result (construction/previous! current roots)
              excluded (first (get-in result [:provenance :excluded-attempts]))]
          (is (= (if older? :carried-from-previous-occurrence :first-attempt-no-admissions) (:admission-reason result)))
          (is (= :producer-recorded-different-target (:reason excluded)))
          (is (seq (:target-evidence excluded)))
          (is (= 3 (count (:records excluded))))
          ;; A present broken integrity binding cannot be discarded as old data.
          (rewrite-history! (last files) #(assoc-in % [:payload :close-evidence-manifest] {}))
          (is (= :history-discovery-invalid (:construction/refusal (refusal #(construction/previous! current roots))))))
        (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f))))))))

(deftest discovery-target-rendering-is-exact-and-provenanced
  (doseq [[mission requested expected]
          [[":ns/target" :ns/target :match]
           [":ns/target" ":ns/target" :match]
           [":other/target" :ns/target :conflict]
           ["ns/target" :ns/target :conflict]
           [42 :ns/target :conflict]]]
    (let [temp (.toFile (Files/createTempDirectory "receipt-target-rendering" (make-array FileAttribute 0)))
          current {:occurrence {:action/value {:target requested} :action-at "2026-09-15T12:00:00Z"}}]
      (try
        (let [files (discovery-history! temp :ns/target false)]
          (rewrite-history! (nth files 2) #(assoc-in % [:payload :judgment :mission] mission))
          (let [error (refusal #(construction/previous! current [temp]))]
            ;; Exact agreement makes this matching OLD construction, which must
            ;; refuse current admission, not disappear as an unrelated target.
            (is (= (if (= expected :match) :previous-occurrence-unavailable :history-discovery-invalid)
                   (:construction/refusal error)))))
        (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))
  (let [temp (.toFile (Files/createTempDirectory "receipt-target-provenance" (make-array FileAttribute 0)))]
    (try
      (discovery-history! temp :ns/target false)
      (let [result (construction/previous! {:occurrence {:action/value {:target :different/target}
                                                        :action-at "2026-09-15T12:00:00Z"}} [temp])
            fields (get-in result [:provenance :excluded-attempts 0 :target-evidence])]
        (is (= #{:keyword :string} (set (map :type fields))))
        (is (some #(= :ns/target (:value %)) fields))
        (is (some #(= ":ns/target" (:value %)) fields))
        (is (every? #(and (:file %) (:path %)) fields))
        (is (= :different/target (get-in result [:provenance :requested-target :value]))))
      (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))

(defn targetless-non-construction! [root action label]
  (let [files (non-construction-fixture! root true false)]
    (rewrite-history! (nth files 1)
                      #(assoc % :payload {:judgment {:selected-action action :selected-mission label}
                                          :ground {:kind :wm-judgement}}))
    files))

(deftest producer-targetless-non-construction-is-excluded
  (doseq [[action label] [[{:type :learn-action-class :target-class :survey-mission
                          :intrinsic-value 0.1 :rationale "no addressable entities for :survey-mission in current substrate"}
                         ":survey-mission"]
                        [{:type :no-op} ":no-op"]]
          older? [false true]]
    (let [temp (.toFile (Files/createTempDirectory "receipt-targetless" (make-array FileAttribute 0)))
          old (io/file temp "old") marker (io/file temp "marker")
          current {:occurrence {:action/value {:target "M-history"} :action-at "2026-09-15T12:00:00Z"}}]
      (try
        (when older? (history-fixture! old :older "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z"))
        (let [files (targetless-non-construction! marker action label)
              result (construction/previous! current (if older? [old marker] [marker]))
              excluded (first (get-in result [:provenance :excluded-attempts]))]
          (is (= (if older? :carried-from-previous-occurrence :first-attempt-no-admissions) (:admission-reason result)))
          (is (= :producer-not-reached-construction (:reason excluded)))
          (is (= 7 (count (:records excluded))))
          (is (= "attempt-002" (get-in excluded [:identity :attempt/id])))
          (is (empty? (:target-evidence excluded)))
          (is (= action (get-in excluded [:selection-evidence :value :selected-action])))
          (is (= action (get-in (edn/read-string (slurp (nth files 1))) [:payload :judgment :selected-action]))))
        (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f))))))))

(deftest targetless-selection-does-not-exempt-damage
  (doseq [[label damage]
          [[:missing-class #(rewrite-history! (nth % 1) (fn [x] (update-in x [:payload :judgment :selected-action] dissoc :target-class)))]
           [:unknown-class #(rewrite-history! (nth % 1) (fn [x] (assoc-in x [:payload :judgment :selected-action :target-class] :invented)))]
           [:string-class #(rewrite-history! (nth % 1) (fn [x] (assoc-in x [:payload :judgment :selected-action :target-class] "survey-mission")))]
           [:unknown-action #(rewrite-history! (nth % 1) (fn [x] (assoc-in x [:payload :judgment :selected-action :type] :invented)))]
           [:nil-target #(rewrite-history! (nth % 1) (fn [x] (assoc-in x [:payload :judgment :selected-action :target] nil)))]
           [:conflicting-target #(rewrite-history! (nth % 1) (fn [x] (assoc-in x [:payload :judgment :selected-action :target] "M-other")))]
           [:conflicting-label #(rewrite-history! (nth % 1) (fn [x] (assoc-in x [:payload :judgment :selected-mission] ":other")))]
           [:nil-label #(rewrite-history! (nth % 1) (fn [x] (assoc-in x [:payload :judgment :selected-mission] nil)))]
           [:malformed-label #(rewrite-history! (nth % 1) (fn [x] (assoc-in x [:payload :judgment :selected-mission] 7)))]
           [:attempt-identity #(rewrite-history! (nth % 1) (fn [x] (assoc x :attempt/id "wrong")))]
           [:constructed-evidence #(rewrite-history! (nth % 2) (fn [x] (assoc-in x [:payload :judgment] {:cascade {}})))]
           [:close-target #(rewrite-history! (last %) (fn [x] (assoc-in x [:payload :judgment :outcome-entity] {:status :present :entity/id "M-other"})))]
           [:integrity #(rewrite-history! (last %) (fn [x] (assoc-in x [:payload :close-evidence-manifest] {})))]]]
    (testing (name label)
      (let [temp (.toFile (Files/createTempDirectory "receipt-targetless-damage" (make-array FileAttribute 0)))]
        (try
          (damage (targetless-non-construction! temp {:type :learn-action-class :target-class :survey-mission} ":survey-mission"))
          (let [error (refusal #(construction/previous! {:occurrence {:action/value {:target "M-history"}
                                                                   :action-at "2026-09-15T12:00:00Z"}} [temp]))]
            (is (= :history-discovery-invalid (:construction/refusal error)))
            (is (string? (:file error))))
          (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))))

(deftest annotated-close-is-not-producer-exclusion-evidence
  ;; Retained attempt-053 form, per K7 B2: no path/hash exemption or recovery.
  (doseq [older? [false true]]
    (let [temp (.toFile (Files/createTempDirectory "receipt-annotated-close" (make-array FileAttribute 0)))
          old (io/file temp "old") marker (io/file temp "marker")]
      (try
        (when older? (history-fixture! old :older "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z"))
        (let [files (non-construction-fixture! marker false false)]
          (rewrite-history! (last files)
                            #(assoc-in % [:payload :ground] {:witness "claude-4" :evidence ["fixture exit 143 SIGTERM" "fixture jstack"]}))
          (let [error (refusal #(construction/previous! {:occurrence {:action/value {:target "M-history"}
                                                                   :action-at "2026-09-15T12:00:00Z"}}
                                                      (if older? [old marker] [marker])))]
            (is (= :history-discovery-invalid (:construction/refusal error)))
            (is (= :non-construction-close-contradiction (:reason error)))
            (is (= (str (last files)) (:file error)))))
        (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f))))))))

(deftest archived-history-shares-ordering-and-admission
  (doseq [damaged? [false true] overlap? [false true]]
    (let [temp (.toFile (Files/createTempDirectory "receipt-archive" (make-array FileAttribute 0)))
          group (io/file temp "archives" "fixture-group")
          current {:occurrence {:action/value {:target "M-history"} :action-at "2026-09-15T12:00:00Z"}}]
      (try
        (history-fixture! temp :old "2026-09-15T09:00:00Z" "2026-09-15T09:01:00Z")
        (let [latest (history-fixture! group :archived "2026-09-15T10:00:00Z" "2026-09-15T10:01:00Z")
              roots (if overlap? [temp group] [temp])]
          (when damaged?
            (rewrite-history! (:construction-file latest) #(update-in % [:payload :judgment] dissoc :receipted-construction)))
          (if damaged?
            (let [error (refusal #(construction/previous! current roots))]
              (is (= :previous-cascade-carrier-unavailable (:construction/refusal error)))
              (is (= (str (:construction-file latest)) (:file error))))
            (let [result (construction/previous! current roots)]
              (is (= :carried-from-previous-occurrence (:admission-reason result)))
              (is (= (str (:construction-file latest)) (get-in result [:provenance :construction-file])))
              (is (some #(= :archive (:layout %)) (get-in result [:provenance :searched-layouts]))))))
        (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f))))))))

(deftest unrelated-archive-and-recognized-open-attempt
  (let [temp (.toFile (Files/createTempDirectory "receipt-archive-other" (make-array FileAttribute 0)))
        group (io/file temp "archives" "fixture-group")]
    (try
      (let [files (discovery-history! group "M-other" false)
            open (io/file temp "fixture" "attempt-003")]
        (.mkdirs open)
        (spit (io/file open "001-time-step.edn") "{:fixture :open}")
        (.mkdirs (io/file open "evidence"))
        (spit (io/file open "evidence" "fixture.source") "immutable fixture")
        (let [result (construction/previous! {:occurrence {:action/value {:target "M-fresh"}
                                                          :action-at "2026-09-15T12:00:00Z"}} [temp])
              excluded (get-in result [:provenance :excluded-attempts])]
          (is (= :first-attempt-no-admissions (:admission-reason result)))
          (is (= 1 (count excluded)))
          (is (= :producer-recorded-different-target (:reason (first excluded))))
          (is (some #(= (str (last files)) (:file %)) (:records (first excluded))))))
      (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))

(deftest unsupported-history-containers-refuse-coverage
  (doseq [path [["extra" "nested" "fixture" "attempt-001"]
                ["archives" "group" "extra" "nested" "fixture" "attempt-001"]
                ["fixture" "attempt-001" "hidden"]]]
    (let [temp (.toFile (Files/createTempDirectory "receipt-layout" (make-array FileAttribute 0)))]
      (try
        (let [dir (apply io/file temp path)]
          (.mkdirs dir)
          (spit (io/file dir "007-closed.edn") "{}")
          (let [error (refusal #(construction/previous! {:occurrence {:action/value {:target "M-fresh"}
                                                                   :action-at "2026-09-15T12:00:00Z"}} [temp]))]
            (is (= :history-discovery-invalid (:construction/refusal error)))
            (is (= :unsupported-history-layout (:reason error)))
            (is (string? (:file error)))))
        (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f))))))))

(deftest history-links-do-not-escape-or-cycle
  (doseq [cycle? [true false]]
    (let [temp (.toFile (Files/createTempDirectory "receipt-links" (make-array FileAttribute 0)))
          outside (.toFile (Files/createTempDirectory "receipt-outside" (make-array FileAttribute 0)))
          link (io/file temp "link")]
      (try
        (Files/createSymbolicLink (.toPath link) (.toPath (if cycle? temp outside)) (make-array FileAttribute 0))
        (let [error (refusal #(construction/previous! {:occurrence {:action/value {:target "M-fresh"}
                                                                 :action-at "2026-09-15T12:00:00Z"}} [temp]))]
          (is (= :history-discovery-invalid (:construction/refusal error)))
          (is (= :unsupported-history-link-or-escape (:reason error)))
          (is (= (str link) (:file error))))
        (finally
          (Files/deleteIfExists (.toPath link))
          (Files/delete (.toPath temp))
          (Files/delete (.toPath outside)))))))

(deftest distinct-path-checkpoint-copies-remain-unresolved
  (doseq [different? [false true]]
    (let [temp (.toFile (Files/createTempDirectory "receipt-collision" (make-array FileAttribute 0)))
          archive (io/file temp "archives" "snapshot")]
      (try
        (let [files (discovery-history! temp "M-history" false)
              copied (mapv #(io/file archive "fixture" "attempt-002" (.getName %)) files)]
          (.mkdirs (.getParentFile (first copied)))
          (doseq [[source dest] (map vector files copied)] (spit dest (slurp source)))
          (when different?
            (rewrite-history! (first copied) #(assoc-in % [:payload :ground :annotation] :different)))
          (doseq [requested ["M-history" "M-fresh"]]
            (let [run #(construction/previous! {:occurrence {:action/value {:target requested}
                                                             :action-at "2026-09-15T12:00:00Z"}} [temp archive])]
              (if (or different? (= requested "M-history"))
                (let [error (refusal run)]
                  (is (= (if different? :history-discovery-invalid :ambiguous-previous-construction)
                         (:construction/refusal error)))
                  (when different? (is (= :history-identity-collision (:reason error))))
                  (is (= 2 (count (:records error))))
                  (is (= (not different?) (:identical-checkpoint-sets? error)))
                  (is (every? #(= 7 (count (:checkpoints %))) (:records error))))
                (let [result (run)]
                  (is (= :first-attempt-no-admissions (:admission-reason result)))
                  (is (= 2 (count (get-in result [:provenance :excluded-attempts])))))))))
        (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f))))))))

(deftest auxiliary-exclusion-requires-complete-coverage
  (doseq [hidden [nil :close :attempt :unreadable]]
    (let [temp (.toFile (Files/createTempDirectory "receipt-aux" (make-array FileAttribute 0)))
          auxiliary (io/file temp "fixture" "arbitrary-name")
          leaf (io/file auxiliary "nested")
          children @#'construction/history-children!
          current {:occurrence {:action/value {:target "M-fresh"} :action-at "2026-09-15T12:00:00Z"}}]
      (try
        (.mkdirs (io/file temp "fixture" "attempt-003"))
        (.mkdirs leaf)
        (spit (io/file auxiliary "notes.txt") "ancillary")
        (doseq [i (range 1 26)]
          (spit (io/file leaf (format "attempt-%03d.edn" i)) "{:projection true}"))
        (case hidden
          :close (spit (io/file leaf "007-closed.edn") "{}")
          :attempt (.mkdirs (io/file leaf "attempt-999"))
          nil)
        (let [run #(construction/previous! current [temp])
              result (if (= :unreadable hidden)
                       (with-redefs-fn {#'construction/history-children!
                                       (fn [dir]
                                         (if (= (str dir) (str leaf))
                                           (throw (ex-info "simulated unreadable directory"
                                                           {:construction/refusal :history-discovery-invalid
                                                            :reason :root-or-directory-unreadable :file (str dir)}))
                                           (children dir)))}
                         #(refusal run))
                       (if hidden (refusal run) (run)))]
          (if hidden
            (do (is (= :history-discovery-invalid (:construction/refusal result)))
                (is (string? (:file result))))
            (let [coverage (first (get-in result [:provenance :auxiliary-exclusions]))]
              (is (= :first-attempt-no-admissions (:admission-reason result)))
              (is (= :complete-no-attempt-no-close-coverage (:reason coverage)))
              (is (= (str auxiliary) (:path coverage)))
              (is (= 28 (count (:visited coverage)))))))
        (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f))))))))
