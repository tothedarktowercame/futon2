(ns futon2.aif.open-cascade-pattern-search-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.open-cascade-pattern-search :as sut]))

(def source-bytes
  {:fixture/a (.getBytes "@flexiarg fixture/a\n+ IF:\n  authored\n" "UTF-8")
   :fixture/b (.getBytes "@flexiarg fixture/b\n+ IF:\n  authored\n" "UTF-8")})
(defn pin [id]
  {:path (str "library/fixture/" (name id) ".flexiarg") :revision "r1"
   :sha256 (evidence/sha256 (source-bytes id))})
(def captured-sources
  (into {} (map (fn [id] [[(:path (pin id)) (:revision (pin id))] (source-bytes id)]))
        (keys source-bytes)))
(defn carrier [id content]
  (let [c {:id id :content content}]
    (assoc c :digest (evidence/value-digest c))))
(def reviewer-job
  (let [j {:job-id "review-job-1" :agent-id "reviewer-1" :state "done"
           :result {:schema :wm/external-pattern-judgments-v1 :patterns [:fixture/a :fixture/b]}}]
    (assoc j :result-digest (sut/agency-result-digest j))))
(def context {:captured-sources captured-sources
              :authority-results {"review-job-1" reviewer-job}})
(def authority {:id "reviewer-1" :job-id "review-job-1"
                :result-digest (:result-digest reviewer-job)})
(defn legacy [id]
  {:if true :route :structured-antecedent :warrant {:file (:path (pin id))}
   :citation {:kind :pattern-text :path (:path (pin id)) :sha256 (:sha256 (pin id))
              :lines [1 1] :quote "authored"}})
(defn repository [ids]
  (let [r {:identity "library" :version "r1"
           :members (mapv (fn [id] {:id id :source-pin (pin id)}) ids)}]
    (assoc r :digest (sut/repository-digest r))))
(defn judgment [id verdict]
  {:pattern id :authority authority :verdict verdict
   :evidence (cond-> {:source-pin (pin id)}
               (= verdict :admissible) (assoc :legacy-receipt (legacy id))
               (= verdict :rejected) (assoc :reason "not applicable"))})
(defn base
  ([] (base :bounded))
  ([scope]
   (let [ids [:fixture/a :fixture/b]
         r {:schema sut/schema :implementation sut/implementation
            :implementation-digest (sut/implementation-digest)
            :repository (repository ids)
            :domain (cond-> {:scope scope :members ids}
                      (= scope :bounded) (assoc :limitation "captured top two"))
            :query (carrier "query-1" {:tension "blocked" :facts [:f1]})
            :blocker (carrier "blocker-1" {:kind :review-refusal :finding "missing evidence"})
            :prior-cascade (carrier "cascade-1" {:patterns [:fixture/original]})
            :search-implementation (carrier "finder-1" {:name "captured-search" :version 1})
            :judgments [(judgment :fixture/a :admissible) (judgment :fixture/b :rejected)]
            :priority ids :result {:kind :chosen-existing :pattern :fixture/a}
            :repository-global-absence nil}]
     (sut/seal r))))
(defn reseal [r] (sut/seal (dissoc r :receipt-digest :projection)))
(defn refusal
  ([r] (refusal r context))
  ([r ctx] (try (sut/validate! r ctx) nil
                (catch clojure.lang.ExceptionInfo e
                  (:open-cascade-pattern-search/refusal (ex-data e))))))

(deftest positive-complete-and-bounded
  (doseq [scope [:complete :bounded]]
    (let [v (sut/validate! (base scope) context)]
      (is (= #{:fixture/a} (set (keys (get-in v [:projection :legacy-receipts])))))
      (is (nil? (get-in v [:projection :repository-global-absence]))))))

(deftest externally-resolved-source-pins
  (is (= :complete-domain-not-repository
         (refusal (reseal (assoc-in (base :complete) [:domain :members] [:fixture/a])))))
  (is (= :bounded-domain-outside-repository
         (refusal (reseal (assoc-in (base) [:domain :members] [:fixture/a :fixture/c])))))
  ;; Fully resealed invented SHA still disagrees with captured revision bytes.
  (let [fake (apply str (repeat 64 "b"))
        r (-> (base) (assoc-in [:repository :members 0 :source-pin :sha256] fake))
        r (-> r (assoc :repository (assoc (:repository r) :digest
                                          (sut/repository-digest (:repository r)))) reseal)]
    (is (= :source-pin-stale (refusal r))))
  (let [r (-> (base) (assoc-in [:repository :members 0 :source-pin :revision] "invented")
              (assoc-in [:judgments 0 :evidence :source-pin :revision] "invented"))
        r (-> r (assoc :repository (assoc (:repository r) :digest
                                          (sut/repository-digest (:repository r)))) reseal)]
    (is (= :source-revision-unresolved (refusal r))))
  (let [r (-> (base) (assoc-in [:repository :members 0 :source-pin :path] "library/invented/a.flexiarg")
              (assoc-in [:judgments 0 :evidence :source-pin :path] "library/invented/a.flexiarg"))
        r (-> r (assoc :repository (assoc (:repository r) :digest
                                          (sut/repository-digest (:repository r)))) reseal)]
    (is (= :source-revision-unresolved (refusal r)))))

(deftest retained-identity-carriers-detect-resealed-tampering
  (doseq [k [:query :blocker :prior-cascade :search-implementation]]
    (is (= :identity-digest-mismatch
           (refusal (reseal (assoc-in (base) [k :content :tampered] true)))))))

(deftest external-authority-is-retained-job-result
  (is (= :authority-job-unresolved
         (refusal (reseal (assoc-in (base) [:judgments 0 :authority :job-id] "invented")))))
  (let [tampered (assoc-in reviewer-job [:result :patterns] [:fixture/b])]
    (is (= :authority-result-digest-mismatch
           (refusal (base) (assoc context :authority-results {"review-job-1" tampered})))))
  (let [r (assoc (base) :search-implementation
                 (carrier "reviewer-1" {:name "captured-search" :version 1}))]
    (is (= :judgment-self-authority (refusal (reseal r))))))

(deftest judgment-authority-and-priority-falsifiers
  (is (= :judgment-coverage-or-order-mismatch (refusal (reseal (update (base) :judgments pop)))))
  (is (= :judgment-coverage-or-order-mismatch
         (refusal (reseal (assoc-in (base) [:judgments 1 :pattern] :fixture/a)))))
  (is (= :priority-invalid
         (refusal (reseal (assoc (base) :priority [:fixture/a :fixture/a :fixture/b])))))
  (is (= :chosen-not-first-admissible
         (refusal (reseal (assoc-in (base) [:result :pattern] :fixture/b)))))
  (is (= :chosen-not-first-admissible
         (refusal (reseal (-> (base)
                              (assoc-in [:judgments 0] (judgment :fixture/a :rejected))
                              (assoc-in [:result :pattern] :fixture/a))))))
  (is (= :no-match-has-admissible-member
         (refusal (reseal (assoc (base) :result {:kind :no-admissible-match})))))
  (is (= :legacy-receipt-unprojectable
         (refusal (reseal (assoc-in (base) [:judgments 0 :evidence :legacy-receipt] nil))))))

(deftest no-match-and-bounded-global-absence
  (let [r (-> (base :complete)
              (assoc :judgments [(judgment :fixture/a :rejected) (judgment :fixture/b :rejected)]
                     :result {:kind :no-admissible-match}
                     :repository-global-absence :no-pattern-addresses-this-tension)
              reseal)]
    (is (= :no-pattern-addresses-this-tension
           (get-in (sut/validate! r context) [:projection :repository-global-absence]))))
  (is (= :bounded-global-absence-forbidden
         (refusal (reseal (assoc (base) :repository-global-absence
                                 :no-pattern-addresses-this-tension))))))
