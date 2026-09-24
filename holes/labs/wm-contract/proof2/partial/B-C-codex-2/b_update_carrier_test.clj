(ns futon2.aif.b-update-carrier-test
  "B-C negative diagnostics; these fixtures do not establish live PROOF-2 standing."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.learning-trial-ledger :as ledger]))

(def family :apparatus/done-is-observed-running)
(def pinned-id "8e7d1aaf32d9ef2ead1702b304887c414298ce7ea5b48b0e51e42995963e1403")
(def fixture "test/fixtures/b-update-carrier/8e7d1aaf.edn")
(def close-fixture "test/fixtures/b-update-carrier/closed-extract.edn")
(defn temp-root []
  (.toFile (java.nio.file.Files/createTempDirectory
            "b-carrier-" (make-array java.nio.file.attribute.FileAttribute 0))))
(defn fixture-root []
  (let [root (temp-root)]
    (spit (io/file root "attempts.edn") (slurp fixture)) root))
(defn carrier [rows] (ledger/concentration-carrier family rows {:layer :none}))
(defn pinned-rows [root]
  (ledger/annotate-close-statuses (ledger/read-trials root) [close-fixture] nil nil))

(deftest refused-close-trial-is-the-recorded-judge-population
  ;; The fixture row is copied verbatim, not a reconstruction. The close
  ;; extract is labelled with its original path. Record 1790199409 carries
  ;; this identity as C1's single trial and theta=3/4 (see B-D Revision 2).
  (let [root (fixture-root) rows (pinned-rows root) c (carrier rows)
        wrong (carrier (filter #(true? (get-in % [:close-acceptance :accepted?])) rows))]
    (is (= (edn/read-string (slurp fixture)) (:row (first rows))))
    (is (= [pinned-id] (mapv :identity (:trial-identities c))))
    (is (= :refused (get-in c [:trial-identities 0 :close-acceptance :accepted?])))
    (is (= :predicate-evaluation-failed
           (get-in c [:trial-identities 0 :close-acceptance :reason])))
    (is (= [{:identity pinned-id :outcome [1 0] :state-belief [1]}] (:trial-vectors c)))
    (is (= [[3/2] [1/2]] (:posterior-concentrations c)))
    (is (= 3/4 (get-in c [:normalization :theta]) (:theta (ledger/pattern-theta family root))))
    (is (= 1/2 (get-in wrong [:normalization :theta])))
    (is (not= (get-in wrong [:normalization :theta]) (:theta (ledger/pattern-theta family root))))
    (is (nil? (ledger/carrier-refusal c)))))

(deftest three-distinct-dedup-layers
  (let [root (fixture-root) rows (ledger/read-trials root)
        one (carrier rows) dup (carrier (concat rows rows))
        original (:row (first rows))
        replay (ledger/record! root {:contract (:contract original) :trials [(:trial original)]})
        occurrence (get-in original [:trial :deduplication :inputs :occurrence])
        update-result (ledger/b-update {:family family :occurrence occurrence
                                       :occurrence-identity "artifact-not-a-dedup-key"
                                       :accepted-verdict {:accepted? true :observed true}
                                       :ledger-root root})]
    ;; Wrong accumulation of a repeated success gives 5/2, not 3/2.
    (is (not= (+ 1/2 (count (concat rows rows)))
              (get-in dup [:posterior-concentrations 0 0])))
    (is (= (:posterior-concentrations one) (:posterior-concentrations dup)))
    (is (= (:version one) (:version dup)))
    (is (= 1 (get-in dup [:dedup :read-side :collapsed-count])))
    (is (= :duplicate-replay (get-in replay [:trials 0 :reason])))
    (is (= :not-appended (get-in replay [:trials 0 :ledger :status])))
    (is (= 1 (count (ledger/read-trials root))))
    (is (= :already-recorded (:status update-result)))
    (is (= 3/4 (:theta update-result) (:theta (ledger/pattern-theta family root))))))

(deftest vectors-and-normalization-refuse-corruption
  (let [c (carrier (ledger/read-trials (fixture-root)))]
    (is (= :token-posterior-not-a-trial
           (ledger/carrier-refusal
            (assoc-in c [:trial-vectors 0 :state-belief] {["T" :restoration-accepted] 1}))))
    (is (= :token-posterior-not-a-trial
           (ledger/carrier-refusal (assoc-in c [:trial-vectors 0 :state-belief] [2])))))
    (is (= :trial-vectors-missing (ledger/carrier-refusal (dissoc c :trial-vectors))))
    (is (= :trial-vector-mismatch
           (ledger/carrier-refusal (assoc-in c [:trial-vectors 0 :outcome] [0 1])))))
    (is (= :inexact-number
           (ledger/carrier-refusal (assoc-in c [:normalization :theta] 0.75)))))
    (is (= :normalization-mismatch
           (ledger/carrier-refusal (assoc-in c [:normalization :axis] :states-at-fixed-outcome)))))
    (is (not= (:version c) (:version (ledger/concentration-carrier
                                     :other/family
                                     (map #(assoc % :theta-key :other/family)
                                          (ledger/read-trials (fixture-root))) {}))))))

(deftest close-emission-and-typed-absence
  (let [root (fixture-root)
        j (get-in (edn/read-string (slurp close-fixture)) [:payload :judgment])
        opts {:ledger-root root :close-path "new/007-closed.edn"
              :close-judgment j :close-record-files []}
        snapshot (ledger/close-b-update opts)
        c (get-in snapshot [:families family])]
    (is (= :recorded (:status snapshot)))
    (is (= :refused (get-in c [:trial-identities 0 :close-acceptance :accepted?])))
    (is (= :same-close (get-in c [:trial-identities 0 :close-acceptance :source :placement])))
    (is (nil? (ledger/carrier-refusal c)))
    (is (= :close-not-found
           (get-in (ledger/close-b-update (dissoc opts :close-judgment))
                   [:families family :trial-identities 0 :close-acceptance :reason])))
    (is (= {:status :missing :reason :ledger-unavailable}
           (ledger/close-b-update (assoc opts :ledger-root (temp-root)))))
    (is (= :ledger-or-close-read-failed
           (:reason (ledger/close-b-update (assoc opts :close-record-files ["/nonexistent/b-c-close"])))))))

(deftest hash-order-and-last-wins-are-explicit
  (is (= (ledger/carrier-hash {:z #{:a :b} :a 1/2})
         (ledger/carrier-hash (array-map :a 1/2 :z #{:b :a}))))
  (is (not= (ledger/carrier-hash #{:a :b}) (ledger/carrier-hash [:a :b])))
  (let [r (first (ledger/read-trials (fixture-root)))
        c (carrier [r (assoc r :observed false)])]
    (is (= 1/4 (get-in c [:normalization :theta])))
    (is (= [pinned-id] (get-in c [:dedup :read-side :conflicting-identities])))))
