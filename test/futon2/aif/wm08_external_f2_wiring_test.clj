(ns futon2.aif.wm08-external-f2-wiring-test
  "EV-find-expectations caller integration (2026-09-18): the ordinary
  construction seam — receipt-construction/construct!, the function the
  live full-loop runner calls (full_loop_runner.clj :construct! callback) —
  now validates its find result against the REQUIRED external F2
  expectation artifact when the run carries one, and RECORDS when it does
  not. The wiring is exercised on the Route-A frozen occurrence's real
  record and companion bytes (a rehearsal corpus; ordinary-run credit is
  NOT claimed here — that needs a commissioned live occurrence through the
  runner, ORDINARY-RUN-SCOPE §5 D-F)."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.find-expectations :as fx]
            [futon2.aif.receipt-construction :as construction]
            [futon2.aif.wm08-route-a-test :as route-a])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- empty-history-root []
  (-> (Files/createTempDirectory "wm08-f2-wiring" (make-array FileAttribute 0))
      .toFile .getAbsolutePath))

(defn- construct-with
  "Run the ordinary seam over the Route-A record with a private empty
  history (first attempt) and the given :external-expectations cfg."
  [cfg]
  (let [{:keys [record read-bytes]} (route-a/load-from-disk)]
    (construction/construct! record read-bytes
                             {:interpretation-library-root route-a/library-root
                              :interpretation-history-roots [(empty-history-root)]
                              :external-expectations cfg})))

(defn- known-correct-artifact
  "A correct external artifact for the wiring test: rows are the receipts'
  own four fields over the SAME frozen occurrence (the wiring needs a
  known-correct input; the artifact's independence is the producer's
  business, tested in find-expectations-test)."
  []
  (let [found (:find-result
               (:receipted-construction
                (construct-with nil)))
        occ (route-a/frozen-occurrence)
        ;; rows' :as-of must be receipt-shaped: the find result's own
        ;; value-digest as-of (the two-digest rule means this is the VALUE
        ;; digest, never the occurrence binding's frozen INDEX digest).
        rows (into (sorted-map)
                   (map (fn [id]
                          [id (-> (select-keys (get-in found [:receipts id])
                                               fx/comparison-fields)
                                  (assoc :must-fire? true))]))
                   (:selected found))]
    {:schema fx/schema-id
     :author {:id "wiring-test-producer" :role :external-expectation-producer}
     :occurrence occ
     :expected rows}))

(deftest ordinary-seam-requires-and-validates-external-expectations
  (testing "absent cfg is RECORDED, never silent"
    (let [retained (:receipted-construction (construct-with nil))]
      (is (= {:status :not-supplied}
             (:external-expectations retained)))))
  (testing "a correct artifact validates and the record says so"
    (let [artifact (known-correct-artifact)
          retained (:receipted-construction (construct-with artifact))]
      (is (= {:status :validated
              :author {:id "wiring-test-producer"
                       :role :external-expectation-producer}
              :expected 7}
             (:external-expectations retained)))))
  (testing "an artifact the receipts contradict REFUSES the construction — the refusal is not swallowed"
    (let [artifact (known-correct-artifact)
          id (first (keys (:expected artifact)))
          wrong (assoc-in artifact [:expected id :acknowledged-clause :text]
                          "a clause the producer preferred")
          d (try (construct-with wrong) nil
                 (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (map? d) "construct! THREW — no path returns a contradicted result")
      (is (= :expectation-mismatch (:reason d)))
      (is (= :F2 (:law d)))))
  (testing "an artifact bound to a different occurrence refuses"
    ;; cfg supplies the LIVE occurrence (frozen-occurrence); the artifact
    ;; claims a different pinned-at — the binding must refuse.
    (let [artifact (assoc-in (known-correct-artifact)
                             [:occurrence :pinned-at] "1999-01-01T00:00:00Z")
          d (try (construct-with {:artifact artifact
                                  :occurrence (route-a/frozen-occurrence)})
                 nil
                 (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (= :occurrence-binding-mismatch (:reason d)))))
  (testing "a self-supplied author refuses"
    (let [artifact (assoc-in (known-correct-artifact)
                             [:author :role] :interpreter)
          d (try (construct-with artifact) nil
                 (catch clojure.lang.ExceptionInfo e (ex-data e)))]
      (is (= :self-supplied-expectations (:reason d)))))
  (testing "a path artifact goes through the EDN entry point"
    (let [tmp (.toFile (Files/createTempDirectory "wm08-f2-artifact"
                                                  (make-array FileAttribute 0)))
          path (.getAbsolutePath (io/file tmp "expectations.edn"))
          _ (spit path (pr-str (known-correct-artifact)))
          retained (:receipted-construction
                    (construct-with {:artifact path
                                     :occurrence (route-a/frozen-occurrence)}))]
      (is (= :validated (get-in retained [:external-expectations :status])))
      (Files/delete (.toPath (io/file path)))
      (Files/delete (.toPath tmp)))))
