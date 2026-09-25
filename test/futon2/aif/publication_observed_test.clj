(ns futon2.aif.publication-observed-test
  "M-wm-wiring row 10 (H-publish), step 12: observe-publication-fn reads the
  click's run record :repair/publication (the tick's catch-up! results, one
  per repair id) and says whether the chosen action's repair obligation
  published (:status :receipt-committed). It writes the observation; the
  enactment record copies it, and the flight's enactment entry copies the
  record's."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def run-record
  {:repair/publication [{:status :receipt-committed :repair/id "occ-published" :repair/discharged? true}
                        {:status :publication-refused :repair/id "occ-refused" :reason :publication-error}]})

(defn- observe [repair-id & [record]]
  ((fr/observe-publication-fn {:fetch-run-record (fn [_] (or record run-record))
                               :repair-id-fn (constantly repair-id)})
   {:target "T-repair-x"} {:click-id "run-p"}))

(deftest a-committed-receipt-is-observed-with-its-evidence
  (is (= {:observed true :at "run-p"
          :evidence {:status :receipt-committed :repair/id "occ-published" :repair/discharged? true}}
         (observe "occ-published"))))

(deftest not-published-says-what-was-checked
  (is (= {:observed false :checked {:repair/id "occ-refused" :click-id "run-p" :entries 2
                                    :statuses [:publication-refused]}}
         (observe "occ-refused")))
  (is (= [] (get-in (observe "occ-absent") [:checked :statuses])) "no entry for the id: checked, not found"))

(deftest the-absences-are-typed
  (is (= {:absent :no-repair-obligation-for-target :target "T-repair-x"} (observe nil)))
  (is (= :no-publication-observation-source (:absent (observe "occ-published" {:other :keys})))))

(deftest observed-true-without-evidence-is-refused-by-the-writer
  ;; the bad case: a value standing in for an observation
  (is (= {:absent :observation-refused :reason :observed-true-without-evidence}
         ((fr/observe-publication-fn {:observation-fn (constantly {:observed true})})
          {:target "t"} {:click-id "c"}))))

(deftest one-authority-the-record-and-the-flight-carry-the-same-value
  (let [dir (str (.toFile (Files/createTempDirectory "pub" (make-array FileAttribute 0))))
        enact (fr/enact-fn {:dispatch-step! (fn [_] {:commit "c" :produced :t :check {:class :fixture}})
                            :check-fn (constantly {:observed true})
                            :interpretations (constantly {:p/a {:produces #{:t}}})
                            :fetch-run-record (constantly run-record)
                            :repair-id-fn (constantly "occ-published")
                            :record-dir dir})
        f (flight/run! (flight/start {:target "T-repair-x" :chosen-because {:kind :requested}}
                                     {:kind :a-exits :repo "futon2" :path "p" :read-text (fn [& _] "")}
                                     {:id "flight-pub"})
                       {:click-fn (constantly {:click-id "run-p" :chosen {:candidate :cand/p :precedence [:p/a]}})
                        :enact-fn enact
                        :observe-fn (fn [_ _] {})
                        :sources-fn (constantly {})
                        :max-clicks 1})
        entry (first (:enactments f))
        record (edn/read-string (slurp (:record-path entry)))]
    (is (true? (get-in record [:publication-observed :observed])))
    (is (= (:publication-observed record) (:publication-observed entry)))))
