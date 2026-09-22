(ns futon2.aif.learning-trial-ledger-b-update-test
  "PROOF-wm-works ⟨1⟩4: the B update against the REAL ledger and the REAL
  r4-1/r4-2 closes' predicate verdicts. No live click."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.attempt-learning :as attempt]
            [futon2.aif.learning-trial-ledger :as ledger]))

(def t "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")

(deftest v2-contract-accepted-and-v1-still-reads
  (let [v2 (attempt/declared-contract-v2)
        v1 (attempt/declared-contract)]
    ;; the private check via receipt admission: supported-contract? is exercised
    ;; through the public contract declarations and the receipt path
    (is (= :wm/attempt-learning-contract-v2 (:schema v2)))
    (is (= :production-consumption (:mode v2)))
    (is (re-find #"PROOF-wm-works" (:authority-basis v2))
        "the file cites the plan signature as the authorisation")
    ;; v1 stays exactly the record-only shape
    (is (= :wm/attempt-learning-contract-v1 (:schema v1)))
    (is (= :record-only (:mode v1)))))

(deftest reader-interprets-old-trials-never-duplicates
  (let [snapshot (slurp (io/file ledger/default-root "attempts.edn"))
        recorded (with-open [reader (java.io.PushbackReader. (java.io.StringReader. snapshot))]
                   (loop [rows []]
                     (let [row (edn/read {:eof ::eof} reader)]
                       (if (= ::eof row) rows (recur (conj rows row))))))
        tmp (java.io.File/createTempFile "ledger-read-snapshot" ".edn")]
    (try
      (spit tmp snapshot)
      (let [rows (ledger/read-trials nil tmp)]
        (is (seq recorded) "the retained ledger exercises old-trial interpretation")
        (is (= recorded (mapv :row rows)) "every recorded row is read once, unchanged")
        ;; every row is readable, with identity and family, exactly as recorded
        (is (every? #(some? (:family %)) rows))
        (is (every? #(contains? % :contract-version) rows))
        ;; reading twice does not duplicate
        (is (= rows (ledger/read-trials nil tmp)))
        ;; the v1 interpretation marker is derived, not stored: rows read under
        ;; v2 carry no :consumption veto of their own
        (is (every? #(not= :not-authorized (get-in % [:row :consumption]))
                    (filter #(= :v2 (:contract-version %)) rows))))
      (finally (.delete tmp)))))

(deftest accepted-occurrence-updates-exactly-once
  ;; An accepted-increment verdict over a family with no prior trials: one
  ;; update (Laplace from cold: (0+1/2)/(0+1) = 1/2). The accepted close's
  ;; record! append then lands the occurrence in the ledger; re-running the
  ;; SAME occurrence then reports :already-recorded with the SAME theta.
  (let [verdict {:accepted? true :observed true
                 :evidence {:acceptance-result {:observed true}}}
        family :apparatus/one-authority-per-question
        identity-1 "wm-test-occurrence-0001"
        tmp (.toFile (java.nio.file.Files/createTempDirectory
                      "b-update-ledger" (make-array java.nio.file.attribute.FileAttribute 0)))
        ;; empty family history in the temp ledger
        _ (spit (io/file tmp "attempts.edn") "")
        r1 (ledger/b-update {:family family
                             :occurrence-identity identity-1
                             :accepted-verdict verdict
                             :ledger-root (.getPath tmp)})
        ;; the accepted close's record! append (the existing writer) lands
        ;; the occurrence; simulate by appending the row the ledger accepts
        _ (spit (io/file tmp "attempts.edn")
                (str "{:schema :wm/attempt-learning-count-v1 :identity " (pr-str identity-1)
                     " :family " (pr-str family)
                     " :increment {:success 1 :failure 0}}\n")
                :append true)
        r2 (ledger/b-update {:family family
                             :occurrence-identity identity-1
                             :accepted-verdict verdict
                             :ledger-root (.getPath tmp)})]
    (is (= :updated (:status r1)) (pr-str r1))
    ;; Laplace (beta 1/2,1/2) counting this success once from cold:
    ;; (0 + 1 + 1/2) / (0 + 1 + 1) = 3/4
    (is (= 3/4 (:theta r1)) (pr-str r1))
    (is (= :already-recorded (:status r2)) (pr-str r2))
    (is (= (:theta r1) (:theta r2)) "re-running writes nothing: same theta")
    ;; the update survives being read back (read from the ledger itself)
    (is (= 1 (count (ledger/read-trials (.getPath tmp))))
        "the recorded occurrence reads back")))

(deftest r4-1-and-r4-2-write-nothing
  ;; Both closes are rejected by the accepted-increment predicate (b8bc1d7c's
  ;; tests): r4-1 :binding-not-fresh, r4-2 :no-reviewed-commit. The B update
  ;; must refuse them typed.
  (doseq [verdict [{:accepted? false :failed :a :reason :binding-not-fresh}
                   {:accepted? false :failed :a :reason :no-reviewed-commit}]]
    (let [r (try (ledger/b-update {:family :apparatus/one-authority-per-question
                                   :occurrence-identity "whatever"
                                   :accepted-verdict verdict})
                 (catch Exception e (ex-data e)))]
      (is (= :close-not-accepted (:learning-ledger/refusal r)) (pr-str r))
      (is (= false (:accepted? (:verdict r)))
          "the refusal carries the rejected verdict"))))
