(ns futon2.aif.categorical-state-close-attachment-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.categorical-state-close-attachment :as sut]
            [futon2.aif.categorical-state-observation :as observation]
            [futon2.aif.categorical-state-observation-test :as fixture]))

(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(defn attachment-fixture
  ([] (attachment-fixture :strengthened :support-gained "a"))
  ([status assertion suffix]
   (let [{:keys [dir candidate records observer-record observer-source expected]} (fixture/fixture)
         candidate (-> candidate
                       (assoc :observation/id (str "test/attachment-" suffix))
                       (assoc-in [:categorical-status :value] status))
         claim (assoc (fixture/evidence-claim assertion) :claim/id (str "claim/" suffix))
         claim-source (fixture/write-record! dir (str "attachment-claim-" suffix ".edn") claim)
         claim-ref (keyword "attachment" (str "claim-" suffix))
         candidate (assoc-in candidate [:evidence :claims] [{:claim/ref claim-ref}])
         annotation-ref (keyword "attachment" (str "annotation-" suffix))
         candidate (assoc-in candidate [:authority :review/ref]
                             (keyword "attachment" (str "review-" suffix)))
         annotation-source (fixture/write-record! dir (str "annotation-" suffix ".edn") candidate)
         expected (update expected :point assoc :event/sequence 7)
         resolved {:ref claim-ref :claim/id (:claim/id claim) :assertion assertion
                   :observed-at (get-in claim [:point :observed-at])
                   :source claim-source :claim claim}
         frozen (observation/acceptance-subject candidate [resolved]
                                                 observer-record observer-source expected)
         review-ref (get-in candidate [:authority :review/ref])
         review-source (fixture/write-record! dir (str "attachment-review-" suffix ".edn")
                                              (fixture/review-for frozen "observer/test-a"))
         close-ref (keyword "attachment" (str "close-" suffix))
         close {:event/schema-version 1 :cohort/id :cohort/test :attempt/id "attempt-1"
                :event/sequence 7 :checkpoint/type :closed
                :recorded-at "2026-09-12T12:02:00Z"
                :payload {:judgment
                          {:outcome :incomplete
                           :outcome-entity {:status :present :entity/id "entity/test-1"}
                           :entity-state-at-close
                           {:entity/id "entity/test-1"
                            :belief-source {:run/id "run-test-1"}}}}}
         close-source (fixture/write-record! dir (str "close-" suffix ".edn") close)
         context-ref (keyword "attachment" (str "context-" suffix))
         context {:schema sut/context-schema :context/id (str "context/" suffix)
                  :context/ref context-ref :subject (:subject expected) :point (:point expected)
                  :authority/scope :test :authority/provenance (:authority/provenance expected)
                  :close/ref close-ref :close/source close-source
                  :annotation/ref annotation-ref :annotation/source annotation-source}
         context-source (fixture/write-record! dir (str "context-" suffix ".edn") context)
         rs (assoc records
                   [:evidence claim-ref] claim-source
                   [:review review-ref] review-source
                   [:annotation annotation-ref] annotation-source
                   [:close close-ref] close-source
                   [:context context-ref] context-source)]
     {:dir dir :candidate candidate :claim claim :close close :context context
      :context-ref context-ref :close-ref close-ref :annotation-ref annotation-ref
      :records rs :authority {:resolver #(get rs [%1 %2])}})))

(defn replace-context-record [fx context]
  (let [p (fixture/write-record! (:dir fx) "replacement-context.edn" context)
        rs (assoc (:records fx) [:context (:context-ref fx)] p)]
    (assoc fx :context context :records rs :authority {:resolver #(get rs [%1 %2])})))

(defn replace-close-record [fx close]
  (let [p (fixture/write-record! (:dir fx) "replacement-close.edn" close)
        rs (assoc (:records fx) [:close (:close-ref fx)] p)
        context (assoc (:context fx) :close/source p)
        fx (assoc fx :records rs)]
    (replace-context-record fx context)))

(deftest exact-offline-join-test
  (let [{:keys [context-ref authority]} (attachment-fixture)
        joined (sut/attach-from-context! context-ref authority)]
    (is (= :joined (:status joined)))
    (is (= :incomplete (:disposition joined)))
    (is (= :test (get-in joined [:acquisition :authority/scope])))
    (is (= true (get-in joined [:acquisition :retrospective?])))
    (is (= :selected-attempts-only (get-in joined [:acquisition :missingness])))
    (is (= (:close-record joined)
           (observation/read-pinned-form! (:close/source joined))))))

(deftest exact-close-and-context-refusals-test
  (let [fx (attachment-fixture)]
    (doseq [[reason path value]
            [[:close-entity-mismatch [:payload :judgment :outcome-entity :entity/id] "other"]
             [:close-attempt-mismatch [:attempt/id] "attempt-other"]
             [:close-run-mismatch [:payload :judgment :entity-state-at-close :belief-source :run/id]
              "run-other"]
             [:close-time-mismatch [:recorded-at] "2026-09-12T12:02:30Z"]]]
      (let [changed (assoc-in (:close fx) path value)
            f (replace-close-record fx changed)]
        (is (= reason (refusal #(sut/attach-from-context! (:context-ref f) (:authority f)))))))
    (let [f (replace-context-record fx (assoc-in (:context fx) [:subject :entity/id] "other"))]
      (is (= :observation-entity-mismatch
             (refusal #(sut/attach-from-context! (:context-ref f) (:authority f))))))
    (is (= :attachment-authority-not-found
           (refusal #(sut/attach-from-context! {:status :qualified} (:authority fx)))))))

(deftest bytes-scope-and-duplicate-controls-test
  (let [fx (attachment-fixture)
        stale-pointer (assoc (get (:records fx) [:close (:close-ref fx)])
                             :sha256 (apply str (repeat 64 "0")))
        context (assoc (:context fx) :close/source stale-pointer)
        rs (assoc (:records fx) [:close (:close-ref fx)] stale-pointer)
        stale (replace-context-record (assoc fx :records rs) context)]
    (is (= :source-digest-mismatch
           (refusal #(sut/attach-from-context! (:context-ref stale) (:authority stale)))))
    (let [production (replace-context-record
                      fx (-> (:context fx)
                             (assoc :authority/scope :production)
                             (assoc-in [:authority/provenance :revision] :production-v1)))]
      (is (= :evidence-scope-mismatch
             (refusal #(sut/attach-from-context! (:context-ref production)
                                                 (:authority production))))))
    (is (= :duplicate-close-annotation
           (refusal #(sut/attach-all! [(:context-ref fx) (:context-ref fx)] (:authority fx)))))))

(deftest conflicting-annotations-refuse-test
  (let [a (attachment-fixture :strengthened :support-gained "conflict-a")
        {:keys [dir records context candidate]} a
        claim-ref :attachment/claim-conflict-b
        claim (assoc (fixture/evidence-claim :framing-sharpened)
                     :claim/id "claim/conflict-b")
        claim-source (fixture/write-record! dir "conflict-claim-b.edn" claim)
        annotation-ref :attachment/annotation-conflict-b
        review-ref :attachment/review-conflict-b
        candidate (-> candidate
                      (assoc :observation/id "test/attachment-conflict-b")
                      (assoc-in [:categorical-status :value] :refined)
                      (assoc-in [:evidence :claims] [{:claim/ref claim-ref}])
                      (assoc-in [:authority :review/ref] review-ref))
        annotation-source (fixture/write-record! dir "conflict-annotation-b.edn" candidate)
        observer-source (get records [:observer :authority/observer-a])
        observer-record (observation/read-pinned-form! observer-source)
        expected (select-keys context [:subject :point :authority/scope :authority/provenance])
        resolved {:ref claim-ref :claim/id (:claim/id claim) :assertion (:assertion claim)
                  :observed-at (get-in claim [:point :observed-at])
                  :source claim-source :claim claim}
        frozen (observation/acceptance-subject candidate [resolved]
                                               observer-record observer-source expected)
        review-source (fixture/write-record! dir "conflict-review-b.edn"
                                             (fixture/review-for frozen "observer/test-a"))
        context-ref :attachment/context-conflict-b
        context-b (assoc context :context/id "context/conflict-b" :context/ref context-ref
                         :annotation/ref annotation-ref :annotation/source annotation-source)
        context-source (fixture/write-record! dir "conflict-context-b.edn" context-b)
        rs (assoc records
                  [:evidence claim-ref] claim-source
                  [:annotation annotation-ref] annotation-source
                  [:review review-ref] review-source
                  [:context context-ref] context-source)
        authority {:resolver #(get rs [%1 %2])}]
    (is (= :conflicting-close-annotations
           (refusal #(sut/attach-all! [(:context-ref a) context-ref] authority))))))
