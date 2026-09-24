(ns futon2.aif.b-update-carrier-test
  "B-C (PROOF-2 strategy row 34): the concentration carrier recorded at the
   close. Spec: proof2/packets/B-D.md §4 as revised by reviews/B-D-codex-20.md
   §3, §6, §7. Pins are verbatim copies of live records under
   test/fixtures/b-update-carrier/ (each names its source path and raw sha);
   the live records themselves are read read-only when present, to show the
   copies have not drifted. Nothing here establishes PROOF-2 standing; it
   pins what the builder emits over the population the judge actually reads."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.learning-trial-ledger :as ledger]))

(def family :apparatus/done-is-observed-running)
(def pinned-id "8e7d1aaf32d9ef2ead1702b304887c414298ce7ea5b48b0e51e42995963e1403")
(def row-fixture "test/fixtures/b-update-carrier/8e7d1aaf.edn")
(def close-fixture "test/fixtures/b-update-carrier/machinery-71-attempt-002-close-extract.edn")
(def record-fixture "test/fixtures/b-update-carrier/tick-run-record-1790199409-c1-extract.edn")
(def live-ledger "/home/joe/code/futon2/data/wm-learning-trials/attempts.edn")

(defn- temp-root []
  (.toFile (java.nio.file.Files/createTempDirectory
            "b-carrier-" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- fixture-root
  "A ledger holding exactly the pinned row, byte-for-byte."
  []
  (let [root (temp-root)]
    (spit (io/file root "attempts.edn") (slurp row-fixture))
    root))

(defn- read-fixture [path] (edn/read-string {:default tagged-literal} (slurp path)))

(defn- pinned-rows [root]
  (ledger/annotate-close-statuses (ledger/read-trials root) [close-fixture] nil nil))

(defn- carrier [rows] (ledger/concentration-carrier family rows {:layer :none}))

(defn- conc-achieved [c] (get-in c [:posterior-concentrations 0 0]))

(defn- wrong-builder-no-dedup
  "A deliberately wrong builder: counts every row it is handed."
  [rows]
  (+ 1/2 (count (filter :observed (filter #(= family (:theta-key %)) rows)))))

(defn- wrong-builder-accepted-only
  "A deliberately wrong builder: the population a reader would assume from
   the B commit point, i.e. only rows whose close was accepted."
  [rows]
  (carrier (filter #(true? (get-in % [:close-acceptance :accepted?])) rows)))

(deftest pinned-live-row-is-the-population-the-judge-reads
  ;; The pin: row 8e7d1aaf… copied verbatim (with its newline) from
  ;; data/wm-learning-trials/attempts.edn; its close
  ;; machinery-71/attempt-002 recorded :accepted? :refused. The row is in
  ;; the population, with that status on it.
  (let [fixture-row (edn/read-string (slurp row-fixture))
        root (fixture-root)
        rows (pinned-rows root)
        c (carrier rows)
        t (first (:trial-identities c))]
    (when (.exists (io/file live-ledger))
      (testing "the copy has not drifted from the live row (read-only)"
        (let [live (first (filter #(= pinned-id (:identity %)) (ledger/read-trials (.getParent (io/file live-ledger)))))]
          (is (= fixture-row (:row live))))))
    (is (= fixture-row (:row (first rows))) "read-trials returns the row itself")
    (testing "the close's own receipt says the row was appended at comparison"
      (let [receipt-trial (get-in (read-fixture close-fixture)
                                  [:payload :judgment :token-outcome-comparison
                                   :learning-trial-receipt :trials 0])]
        (is (= :admitted-at-attempt-grain (:status receipt-trial)))
        (is (true? (:counted? receipt-trial)))
        (is (= :appended (get-in receipt-trial [:ledger :status])))
        (is (= pinned-id (get-in receipt-trial [:ledger :identity])))))
    (is (= [pinned-id] (mapv :identity (:trial-identities c))))
    (is (= {:identity pinned-id :theta-key family :cell :achieved}
           (select-keys t [:identity :theta-key :cell])))
    (is (= :refused (get-in t [:close-acceptance :accepted?])))
    (is (= :predicate-evaluation-failed (get-in t [:close-acceptance :reason])))
    (is (= [:payload :judgment :accepted-increment] (get-in t [:close-acceptance :source :key-path])))
    (is (= [{:identity pinned-id :outcome [1 0] :state-belief [1]}] (:trial-vectors c)))
    (is (= [[1/2] [1/2]] (:prior-concentrations c)))
    (is (= [[3/2] [1/2]] (:posterior-concentrations c)))
    (testing "posterior = prior + sum of outer(outcome, state-belief), recomputed here"
      (let [outer (fn [o s] (mapv (fn [oi] (mapv #(* oi %) s)) o))
            sum (reduce (fn [acc v] (mapv #(mapv + %1 %2) acc (outer (:outcome v) (:state-belief v))))
                        (:prior-concentrations c) (:trial-vectors c))]
        (is (= sum (:posterior-concentrations c)))))
    (is (= {:axis :outcomes-at-fixed-state :state :singleton :successes 1 :trials 1
            :conc-achieved 3/2 :conc-not 1/2 :theta 3/4}
           (dissoc (:normalization c) :rule)))
    (is (= 3/4 (:theta (ledger/pattern-theta family root)))
        "the carrier's normalized cell is the judge's read over the same rows")
    (is (= [] (get-in c [:dedup :fired])))
    (is (re-matches #"sha256:[0-9a-f]{64}" (:version c)))
    (is (nil? (ledger/carrier-refusal c)))))

(deftest dropping-refused-close-rows-disagrees-with-record-1790199409
  ;; Record 1790199409 scores C1 with theta 3/4 from exactly this identity
  ;; (fixture extract; the live record is cross-checked when present). A
  ;; builder that keeps only accepted-close rows sees no trial and reports
  ;; the prior 1/2: it cannot be the carrier of what the judge consumed.
  ;; Without the population rule this test fails at the `not=` below
  ;; (wrong-builder-accepted-only would BE the builder) and at the
  ;; refused-row assertion in the pinned test above.
  (let [extract (read-fixture record-fixture)
        entry (first (:entries extract))
        root (fixture-root)
        rows (pinned-rows root)
        right (carrier rows)
        wrong (wrong-builder-accepted-only rows)]
    (is (= 1 (count (:entries extract))) "one distinct C1 theta entry in the record")
    (is (= 3/4 (:theta entry)))
    (is (= [pinned-id] (get-in entry [:theta-provenance :identities])))
    (is (= 1 (get-in entry [:theta-provenance :trials-count])))
    (when (.exists (io/file (:source extract)))
      (testing "the live record still says 3/4 for every C1 entry naming this identity (read-only)"
        (let [rec (read-fixture (:source extract))
              thetas (atom [])
              walk (fn walk [x]
                     (cond (map? x)
                           (do (when (and (contains? x :theta-provenance)
                                          (some #{pinned-id} (get-in x [:theta-provenance :identities])))
                                 (swap! thetas conj (:theta x)))
                               (doseq [[_ v] x] (walk v)))
                           (coll? x) (doseq [v x] (walk v))))]
          (walk rec)
          (is (pos? (count @thetas)))
          (is (every? #(= 3/4 %) @thetas)))))
    (is (= 3/4 (get-in right [:normalization :theta])))
    (is (= (:theta entry) (get-in right [:normalization :theta])))
    (is (= [] (:trial-identities wrong)))
    (is (= 1/2 (get-in wrong [:normalization :theta])))
    (is (not= (:theta entry) (get-in wrong [:normalization :theta])))))

(deftest three-dedup-layers-leave-conc-achieved-unchanged
  ;; A duplicate through each layer leaves conc(achieved) at 3/2. Each
  ;; sub-test first shows the deliberately wrong builder moving to 5/2, so
  ;; the layer is doing the work, not the fixture.
  (let [root (fixture-root)
        rows (ledger/read-trials root)
        one (carrier rows)]
    (is (= 3/2 (conc-achieved one)))
    (testing "read-identity layer: the same identity handed twice counts once"
      ;; Without the collapse: 5/2 (wrong-builder-no-dedup shows it).
      (let [dup (carrier (concat rows rows))]
        (is (= 5/2 (wrong-builder-no-dedup (concat rows rows))))
        (is (= 3/2 (conc-achieved dup)))
        (is (= (:posterior-concentrations one) (:posterior-concentrations dup)))
        (is (= (:version one) (:version dup)))
        (is (= [:read-identity] (get-in dup [:dedup :fired])))
        (is (= 1 (get-in dup [:dedup :read-side :collapsed-count])))))
    (testing "ledger-identity layer: record! holds the replayed trial, the snapshot names it"
      ;; Without record!'s identity check the replay would append a second
      ;; row: the ledger would read 2 rows, the receipt would say :appended
      ;; rather than :held, and the snapshot's :dedup :fired would name
      ;; [:read-identity] (the collapse catching a same-identity pair)
      ;; instead of [:ledger-identity]. conc(achieved) alone would not
      ;; move, which is why the layer is pinned by name, not by the number.
      (let [original (:row (first rows))
            replay (ledger/record! root {:contract (:contract original) :trials [(:trial original)]})
            snapshot (ledger/close-b-update {:ledger-root root :close-record-files [close-fixture]
                                             :learning-trial-receipt replay})
            c (get-in snapshot [:families family])]
        (is (= :held (get-in replay [:trials 0 :status])))
        (is (= :duplicate-replay (get-in replay [:trials 0 :reason])))
        (is (= 1 (count (ledger/read-trials root))) "one banked row, not two")
        (is (= :recorded (:status snapshot)))
        (is (= 3/2 (conc-achieved c)))
        (is (= [:ledger-identity] (get-in c [:dedup :fired])))
        (is (= [{:identity pinned-id :reason :duplicate-replay}] (get-in c [:dedup :upstream :held])))
        (is (= :refused (get-in c [:trial-identities 0 :close-acceptance :accepted?])))))
    (testing "update-occurrence layer: b-update over an already-banked occurrence writes nothing"
      ;; Without the occurrence check b-update would count the occurrence
      ;; again: theta (1+1+1/2)/(1+1+1) = 5/6 instead of 3/4.
      (let [occurrence (get-in (:row (first rows)) [:trial :deduplication :inputs :occurrence])
            result (ledger/b-update {:family family :occurrence occurrence
                                     :occurrence-identity "artifact-sha-not-a-dedup-key"
                                     :accepted-verdict {:accepted? true :observed true}
                                     :ledger-root root})
            c (:carrier result)]
        (is (= :already-recorded (:status result)))
        (is (= 3/4 (:theta result)))
        (is (= 3/2 (conc-achieved c)))
        (is (= [:update-occurrence] (get-in c [:dedup :fired])))
        (is (= (:posterior-concentrations one) (:posterior-concentrations c)))
        (is (= (:version one) (:version c)))
        (is (= :close-not-looked-up (get-in c [:trial-identities 0 :close-acceptance :reason]))
            "b-update's carrier does not look closes up; it says so")))))

(deftest invented-token-posterior-is-refused
  ;; X5: a whole-attempt success re-described as a token posterior, or a
  ;; bare scalar, or a tampered array, must fail. Each refusal below is
  ;; recomputed from the carrier's own vectors; without that recomputation
  ;; the tampered posterior and version would pass as-declared.
  (let [c (carrier (ledger/read-trials (fixture-root)))]
    (is (nil? (ledger/carrier-refusal c)))
    (is (= :token-posterior-not-a-trial
           (ledger/carrier-refusal
            (assoc-in c [:trial-identities 0 :token-belief]
                      {["M-f11-find-production-successor" :hole/h9ab212b3281d] 1}))))
    (is (= :token-posterior-not-a-trial
           (ledger/carrier-refusal (assoc-in c [:trial-vectors 0 :state-belief] [2]))))
    (is (= :scalar-without-concentrations
           (ledger/carrier-refusal {:family family :theta 3/4}))
        "3/4 with no concentration array and normalization fails")
    (is (= :trial-vectors-missing (ledger/carrier-refusal (dissoc c :trial-vectors))))
    (is (= :trial-vector-mismatch
           (ledger/carrier-refusal (assoc-in c [:trial-vectors 0 :outcome] [0 1]))))
    (is (= :posterior-mismatch
           (ledger/carrier-refusal (assoc c :posterior-concentrations [[5/2] [1/2]]))))
    (is (= :normalization-mismatch
           (ledger/carrier-refusal (assoc-in c [:normalization :axis] :states-at-fixed-outcome))))
    (is (= :version-mismatch
           (ledger/carrier-refusal (assoc c :version "sha256:0000"))))
    (is (= :inexact-number
           (ledger/carrier-refusal (assoc-in c [:normalization :theta] 0.75))))
    (is (= :carrier-not-a-map (ledger/carrier-refusal 3/4)))))

(deftest no-doubles-anywhere-in-the-carrier
  ;; Without exact-tree? a double slipped in as (double 3/4) = 0.75 would
  ;; survive: the assertion below is the only thing that notices.
  (let [snapshot (ledger/close-b-update {:ledger-root (fixture-root)
                                         :close-record-files [close-fixture]})]
    (is (= :recorded (:status snapshot)))
    (is (ledger/exact-tree? snapshot))
    (is (not (ledger/exact-tree? (assoc-in snapshot [:families family :normalization :theta] 0.75))))
    (when (.exists (io/file live-ledger))
      (testing "the live ledger, read-only, with the live close roots scanned"
        (let [live (ledger/close-b-update {:ledger-root (.getParent (io/file live-ledger))})]
          (is (= :recorded (:status live)))
          (is (ledger/exact-tree? live))
          (is (pos? (:close-files-scanned live)))
          (is (every? nil? (map ledger/carrier-refusal (vals (:families live)))))
          (is (= 3/4 (get-in live [:families family :normalization :theta]))
              "the live population still gives C1's 3/4")
          (is (= :refused (get-in live [:families family :trial-identities 0 :close-acceptance :accepted?]))))))))

(deftest typed-absence-when-rows-are-unavailable
  (let [root (fixture-root)
        j (get-in (read-fixture close-fixture) [:payload :judgment])
        opts {:ledger-root root :close-path "new/007-closed.edn"
              :close-judgment j :close-record-files []}]
    (testing "the close being written supplies its own status, sourced :same-close"
      (let [c (get-in (ledger/close-b-update opts) [:families family])]
        (is (= :refused (get-in c [:trial-identities 0 :close-acceptance :accepted?])))
        (is (= :same-close (get-in c [:trial-identities 0 :close-acceptance :source :placement])))
        (is (nil? (ledger/carrier-refusal c)))))
    (testing "no close anywhere for the occurrence"
      (is (= :close-not-found
             (get-in (ledger/close-b-update (dissoc opts :close-judgment))
                     [:families family :trial-identities 0 :close-acceptance :reason]))))
    (testing "the same occurrence closed twice is ambiguous, not silently first"
      (is (= :ambiguous-close
             (get-in (ledger/close-b-update (assoc opts :close-record-files [close-fixture close-fixture]
                                                       :close-judgment nil))
                     [:families family :trial-identities 0 :close-acceptance :reason]))))
    (testing "no ledger file"
      (is (= {:status :missing :reason :ledger-unavailable}
             (dissoc (ledger/close-b-update (assoc opts :ledger-root (temp-root))) :ledger-path))))
    (testing "an unreadable close record is a typed failure, not a prior"
      (let [r (ledger/close-b-update (assoc opts :close-record-files ["/nonexistent/b-c-close"]))]
        (is (= :missing (:status r)))
        (is (= :ledger-or-close-read-failed (:reason r)))
        (is (nil? (:families r)))))))
