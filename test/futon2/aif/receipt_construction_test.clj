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
            (pr-str {:recorded-at "2026-07-21T10:08:23Z" :payload {:judgment {:outcome :build-failed}}}))
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
