(ns futon2.aif.open-cascade-pattern-search-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.open-cascade-pattern-search :as sut]))

(def h (apply str (repeat 64 "a")))
(defn pin [id] {:path (str "library/fixture/" (name id) ".flexiarg") :revision "r1" :sha256 h})
(defn legacy [id]
  {:if true :route :structured-antecedent :warrant {:file (:path (pin id))}
   :citation {:kind :pattern-text :path (:path (pin id)) :sha256 h :lines [1 1] :quote "authored"}})
(defn repository [ids]
  (let [r {:identity "library" :version "r1"
           :members (mapv (fn [id] {:id id :source-pin (pin id)}) ids)}]
    (assoc r :digest (sut/repository-digest r))))
(defn judgment [id verdict]
  {:pattern id :authority "reviewer-job-1" :verdict verdict
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
            :query {:id "query-1" :digest h} :blocker {:id "blocker-1" :digest h}
            :prior-cascade {:id "cascade-1" :digest h}
            :search-implementation {:id "finder-1" :digest h}
            :judgments [(judgment :fixture/a :admissible) (judgment :fixture/b :rejected)]
            :priority ids :result {:kind :chosen-existing :pattern :fixture/a}
            :repository-global-absence nil}]
     (sut/seal r))))
(defn reseal [r] (sut/seal (dissoc r :receipt-digest :projection)))
(defn refusal [r]
  (try (sut/validate! r) nil
       (catch clojure.lang.ExceptionInfo e (:open-cascade-pattern-search/refusal (ex-data e)))))

(deftest positive-complete-and-bounded
  (doseq [scope [:complete :bounded]]
    (let [v (sut/validate! (base scope))]
      (is (= #{:fixture/a} (set (keys (get-in v [:projection :legacy-receipts])))))
      (is (nil? (get-in v [:projection :repository-global-absence]))))))

(deftest complete-and-subset-falsifiers
  (is (= :complete-domain-not-repository
         (refusal (reseal (assoc-in (base :complete) [:domain :members] [:fixture/a])))))
  (is (= :bounded-domain-outside-repository
         (refusal (reseal (assoc-in (base) [:domain :members] [:fixture/a :fixture/c])))))
  (is (= :repository-digest-mismatch
         (refusal (reseal (assoc-in (base) [:repository :members 0 :source-pin :sha256]
                                      (apply str (repeat 64 "b"))))))))

(deftest judgment-authority-and-priority-falsifiers
  (is (= :judgment-coverage-or-order-mismatch
         (refusal (reseal (update (base) :judgments pop)))))
  (is (= :judgment-coverage-or-order-mismatch
         (refusal (reseal (assoc-in (base) [:judgments 1 :pattern] :fixture/a)))))
  (is (= :judgment-self-authority
         (refusal (reseal (assoc-in (base) [:judgments 0 :authority] "finder-1")))))
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
           (get-in (sut/validate! r) [:projection :repository-global-absence]))))
  (is (= :bounded-global-absence-forbidden
         (refusal (reseal (assoc (base) :repository-global-absence
                                 :no-pattern-addresses-this-tension))))))
