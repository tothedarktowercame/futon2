(ns futon2.aif.fact-measurement-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.fact-measurement :as m]
            [futon2.aif.find-receipt-test :as fixture]
            [futon2.aif.interpretation-evidence :as e])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def at "2026-09-15T12:00:00Z")
(defn sample [plan-fn]
  (let [{:keys [record captured]} (fixture/sample)
        plan (plan-fn record)
        bs (.getBytes (pr-str plan) "UTF-8")
        source {:id "reader-plan" :path "/fixture/interpretation-reader-plan.edn"
                :file m/plan-file :revision "fixture" :sha256 (e/sha256 bs)}]
    {:record (update record :sources conj source) :captured (assoc captured m/plan-file bs) :plan plan}))
(defn entry [r i reader parameters]
  {:fact (get-in r [:facts i :id]) :reader reader :parameters parameters
   :citations (get-in r [:facts i :citations])})
(defn plans [r]
  [(entry r 0 :artifact {:repository "futon2" :path "src/component.clj"})
   (entry r 1 :request-status {:record-id "request-1"})
   (entry r 2 :independent-replay {:reviewer "independent" :revision "abc" :command "test replay"})])
(defn refused [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo ex (:measurement/refusal (ex-data ex)))))
(defn with-files [record captured f]
  (let [dir (.toFile (Files/createTempDirectory "p5-measure-" (make-array FileAttribute 0)))]
    (try
      (doseq [[file bs] captured]
        (Files/write (.toPath (io/file dir file)) bs (make-array java.nio.file.OpenOption 0)))
      (spit (io/file dir "interpretation-receipt.edn") (pr-str record))
      (f dir)
      (finally (doseq [file (reverse (file-seq dir))] (Files/delete (.toPath file)))))))

(deftest real-nine-fact-carrier-and-three-planned-readers
  (let [{:keys [record captured plan]} (sample plans)
        raw {:artifact {:repository "futon2" :commit "abc" :paths ["src/component.clj"]}
             :receipts [{:kind :request-status :record-id "request-1" :recorded? true}
                        {:kind :independent-replay :reviewer "independent" :revision "abc"
                         :command "test replay" :executed? true :passed? true}]}
        measured (m/measure plan raw)]
    (is (= 9 (count (:facts record))))
    (is (= plan (m/validate-plan! record captured)))
    (is (every? true? (map :value (vals measured))))
    (with-files record captured
      (fn [dir]
        (doseq [[n receipt] (map-indexed vector (:receipts raw))]
          (spit (io/file dir (str "reader-receipt-" n ".source")) (pr-str receipt)))
        (let [state (m/begin! dir {:receipted-construction {:cascade-diff {:acting-order-after []}}} (constantly at))
              end (m/finish! state (dissoc raw :receipts) (constantly at))]
          (is (= (:facts record) (get-in state [:pre :facts])))
          (is (= 3 (count (filter #(true? (:value %)) (:facts end)))))
          (is (= 6 (count (filter #(= :unknown (:value %)) (:facts end)))))
          (is (= end (e/validate-record end)))
          (is (= end (edn/read-string (slurp (io/file dir "fact-end.edn"))))))))))

(deftest strict-plan-controls
  (doseq [[reason mutate] [[:unknown-reader #(assoc-in % [0 :reader] :invented)]
                          [:forbidden-input #(assoc-in % [0 :parameters :outcome] :success)]
                          [:unknown-parameters #(assoc-in % [0 :parameters :typo] "x")]
                          [:undeclared-fact #(assoc-in % [0 :fact] "invented")]
                          [:citation-mismatch #(assoc-in % [0 :citations 0 :quote] "invented")]]]
    (let [{:keys [record captured]} (sample #(mutate (plans %)))]
      (is (= reason (refused #(m/validate-plan! record captured))))))
  (let [{:keys [record captured]} (sample plans)]
    (is (= :plan-digest-mismatch
           (refused #(m/validate-plan! record (assoc captured m/plan-file (.getBytes "[]" "UTF-8"))))))))

(deftest unknown-and-independent-input-controls
  (let [{:keys [plan]} (sample plans) fact (:fact (first plan))]
    (is (= :unknown (get-in (m/measure plan {}) [fact :value])))
    (with-redefs [m/readers {}]
      (is (= :unknown (get-in (m/measure plan {}) [fact :value]))))
    (with-redefs [m/readers {:artifact (fn [_ _] (throw (ex-info "reader failed" {})))}]
      (is (= :reader-failed (get-in (m/measure plan {}) [fact :reason]))))
    (doseq [key m/forbidden]
      (is (= :forbidden-input (refused #(m/require-input! {key true} key)))))
    (let [raw {:outcome :grounded-change :grounded? true :predicted-effects {"x" true}
               :wiring {:fold-output {:outcome :incomplete :predicted-effects {"x" true}}}
               :receipts [{:kind :gate-receipt :revision "x" :outcome :success}]}]
      (is (= (m/reader-input raw) (m/reader-input (assoc raw :outcome :incomplete))))
      (is (not-any? m/forbidden (tree-seq coll? seq (m/reader-input raw)))))))

(deftest exposure-controls
  (let [record {:facts [{:id "a" :value true} {:id "b" :value false} {:id "c" :value false}]
                :interpretations [{:pattern "test/move" :sha256 "digest"
                                   :guard [:fact "a"] :effect {"a" true "b" true}}]}
        result (m/exposures record [:test/move] [{:id "a" :value true} {:id "b" :value true} {:id "c" :value true}])]
    (is (= true (get-in result [:moves 0 :guard-value])))
    (is (= :already-true (get-in result [:moves 0 :effects "a" :status])))
    (is (= :newly-established (get-in result [:moves 0 :effects "b" :status])))
    (is (= ["c"] (:unexplained result)))))

(deftest literal-document-and-qualifying-receipts
  (let [p [{:fact "d" :reader :document-span :parameters {:source "s" :lines [1 1] :predicate :not-contains :literal "open"}}]]
    (is (false? (get-in (m/measure p {:source-bytes {"s" (.getBytes "open" "UTF-8")}}) ["d" :value])))
    (is (= :unknown (get-in (m/measure p {}) ["d" :value]))))
  (let [p [{:fact "g" :reader :gate-receipt :parameters {:gate-id "gate" :revision "abc"}}]]
    (is (= :unknown (get-in (m/measure p {:receipts [{:approved? true}]}) ["g" :value])))
    (is (false? (get-in (m/measure p {:receipts [{:kind :gate-receipt :gate-id "gate" :revision "abc" :passed? false}]}) ["g" :value])))))

(deftest labels-cannot-change-observation-bytes
  (let [{:keys [record captured]} (sample plans)]
    (with-files record captured
      (fn [dir]
        (let [state (m/begin! dir {} (constantly at))
              a (m/finish! state {:outcome :grounded-change :grounded? true} (constantly at))
              files ["fact-end-measurements.source" "fact-end-target.source" "fact-exposures.source" "fact-end.edn"]]
          ;; Test-only removal of our own outputs permits identical paths/identity/time
          ;; for the counterfactual label arm; production writes are CREATE_NEW.
          (doseq [n files :let [f (io/file dir n)] :when (.exists f)] (Files/delete (.toPath f)))
          (is (= (pr-str a) (pr-str (m/finish! state {:outcome :build-failed :grounded? false} (constantly at))))))))))

(deftest replay-reader-cannot-be-the-build-author
  (let [{:keys [record captured]} (sample plans)]
    (is (= :replay-parameters
           (refused #(m/validate-plan! record captured "independent"))))))

(deftest contradictory-gate-receipts-are-unknown
  (let [p [{:fact "g" :reader :gate-receipt :parameters {:gate-id "gate" :revision "abc"}}]
        rows (mapv #(hash-map :kind :gate-receipt :gate-id "gate" :revision "abc" :passed? %) [true false])]
    (is (= :unknown (get-in (m/measure p {:receipts rows}) ["g" :value])))
    (is (= :unknown (get-in (m/measure p {:receipts (reverse rows)}) ["g" :value])))))
