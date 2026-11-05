(ns futon2.aif.medic-discharge-test
  (:require [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.medic-discharge :as discharge]))

(def injury-id "injury/discharge")
(def cause-pin {:path "fixture/cause.edn" :sha256 (apply str (repeat 64 "a"))})
(def authority (constantly true))

(defn- append! [history entry]
  (:ledger (ledger/append-transition history entry authority)))

(defn- enacted-ledger []
  (let [patch "1111111111111111111111111111111111111111"
        opened (append! ledger/empty-ledger
                        {:injury-id injury-id :state :open :attempt 1
                         :cause-record-pin cause-pin :failure-stage :construction
                         :failure-kind :fixture :detected-click "click/fixture"})
        claimed (append! opened {:injury-id injury-id :state :claimed :attempt 1
                                 :repair-id "repair/fixture" :algorithm "fixture"
                                 :author-session "author/fixture"
                                 :reviewer-session "reviewer/fixture"})
        authored (append! claimed {:injury-id injury-id :state :authored :attempt 1
                                   :repair-id "repair/fixture" :patch-sha patch
                                   :worktree "fixture/worktree"})
        reviewed (append! authored {:injury-id injury-id :state :reviewed :attempt 1
                                    :repair-id "repair/fixture" :verdict :approved
                                    :reviewer-sha patch
                                    :reviewer-session "reviewer/fixture"})]
    (append! reviewed {:injury-id injury-id :state :enacted :attempt 1
                       :repair-id "repair/fixture" :patch-sha patch
                       :retry-click "retry/fixture"
                       :integration-commit patch})))

(defn- write-edn! [dir name value]
  (let [path (str dir "/" name)
        bytes (.getBytes (str (pr-str value) "\n") "UTF-8")]
    (java.nio.file.Files/write (.toPath (java.io.File. path)) bytes
                               (make-array java.nio.file.OpenOption 0))
    {:path path :sha256 (load-identity/sha256 bytes)}))

(defn- git-fixture []
  (let [dir (.getCanonicalPath
             (.toFile (java.nio.file.Files/createTempDirectory
                       "medic-discharge-git"
                       (make-array java.nio.file.attribute.FileAttribute 0))))]
    (shell/sh "git" "-C" dir "init" "-q")
    (shell/sh "git" "-C" dir "config" "user.email" "fixture@example.test")
    (shell/sh "git" "-C" dir "config" "user.name" "Fixture")
    (spit (str dir "/tracked") "one\n")
    (shell/sh "git" "-C" dir "add" "tracked")
    (shell/sh "git" "-C" dir "commit" "-qm" "fixture")
    {:dir dir :head (str/trim (:out (shell/sh "git" "-C" dir "rev-parse" "HEAD")))}))

(defn- receipt-chain!
  "Write the retained OutOfBandFix receipt chain the production corrections
  carry: authority receipt -> evaluation receipt -> pre/post/negative replays."
  [dir verdicts]
  (let [{:keys [pre post negative evaluation]
         :or {pre :original-contract-failed post :original-contract-repaired
              negative :negative-control-refused
              evaluation :original-acceptance-repaired}} verdicts
        replay (fn [phase verdict]
                 (write-edn! dir (str (name phase) "-replay.edn")
                             {:schema :wm/meta-medic-genuine-repair-replay-v1
                              :phase phase :verdict verdict}))
        evaluation-pin (write-edn! dir "evaluation-receipt.edn"
                                   {:pre-replay-pin (replay :pre pre)
                                    :post-replay-pin (replay :post post)
                                    :negative-control-pin (replay :negative negative)
                                    :verdict evaluation})]
    (write-edn! dir "authority.edn" {:evaluation-pin evaluation-pin})))

(defn- corrected-ledger
  ([repository commit] (corrected-ledger repository commit {}))
  ([repository commit verdicts]
  (let [history (enacted-ledger)
        current (last (:entries history))
        receipt-pin (receipt-chain! repository verdicts)
        payload {:kind :genuine-repair :injury-id injury-id
                 :entry-id (str (:ordinal current))
                 :predecessor-entry-id (str (dec (:ordinal current)))
                 :cause-record-id (:path cause-pin) :cause-pin (:sha256 cause-pin)
                 :acceptance-contract "contract/fixture" :patch-id commit
                 :source-revision commit :pre-replay-receipt "pre"
                 :post-replay-receipt "post" :negative-control-receipt "negative"
                 :original-acceptance-repaired? true
                 :reviewer "reviewer/other" :evaluator "evaluator/other"
                 :evidence-pin (:sha256 receipt-pin)}
        operation {:operation-id "operation/fixture"
                   :expected-ledger-pin (:ledger-pin history)
                   :request {:candidate payload :evidence payload}
                   :authority-receipt-pin receipt-pin}
        corrected (ledger/append-medic-repaired history receipt-pin operation authority)]
    {:ledger (:ledger corrected) :receipt-pin receipt-pin
     :correction (:entry corrected) :occurrence [injury-id (:ordinal current)]})))

(deftest checked-correction-discharges-enacted-occurrence
  (let [{:keys [dir head]} (git-fixture)
        {:keys [ledger correction occurrence]} (corrected-ledger dir head)
        before (ledger/effective-active-injuries-from-validated-ledger ledger)
        result (discharge/append-medic-discharge ledger occurrence dir authority)
        after (ledger/effective-active-injuries-from-validated-ledger (:ledger result))
        entry (:entry result)]
    (is (= :appended (:status result)) (pr-str result))
    (is (= :discharged (:state entry)))
    (is (= (:entry-pin correction) (:medic-correction-pin entry)))
    (is (= 1 (:raw-active-count before)))
    (is (= 0 (:raw-active-count after)))
    (is (= (:injuries before) (:injuries after)))))

(deftest evidence-failures-append-nothing
  (let [{:keys [dir head]} (git-fixture)
        {:keys [ledger occurrence]} (corrected-ledger dir head)
        stale-ledger (update ledger :entries
                             (fn [entries]
                               (mapv #(if (= :medic-repaired (:observation-kind %))
                                        (assoc-in % [:medic-operation :authority-receipt-pin
                                                     :sha256]
                                                  (apply str (repeat 64 "f"))) %)
                                     entries)))
        stale-ledger (ledger/seal-ledger stale-ledger)
        historical (discharge/append-medic-discharge stale-ledger occurrence dir authority)
        current (discharge/append-medic-discharge ledger occurrence dir authority)
        open-result (discharge/append-medic-discharge ledger [injury-id 1] dir authority)]
    (is (= :historical (:failed-check historical)))
    (is (nil? (:ledger historical)) "historical refusal appends nothing")
    (is (= :appended (:status current)) "fixture HEAD is accepted before the negative case")
    (is (= :not-enacted (:failed-check open-result)))
    (is (nil? (:ledger open-result)) "state refusal appends nothing")
    (testing "a real unrelated commit is rejected"
      (spit (str dir "/other") "other\n")
      (shell/sh "git" "-C" dir "add" "other")
      (shell/sh "git" "-C" dir "commit" "-qm" "other")
      (let [other (str/trim
                   (:out (shell/sh "git" "-C" dir "rev-parse" "HEAD")))
            branch (str dir "-unrelated")]
        (shell/sh "git" "clone" "-q" dir branch)
        (shell/sh "git" "-C" branch "checkout" "-q" "--orphan" "unrelated")
        (spit (str branch "/tracked") "unrelated\n")
        (shell/sh "git" "-C" branch "add" "tracked")
        (shell/sh "git" "-C" branch "-c" "user.email=fixture@example.test"
                  "-c" "user.name=Fixture" "commit" "-qm" "unrelated")
        (let [unrelated (str/trim
                         (:out (shell/sh "git" "-C" branch "rev-parse" "HEAD")))
              changed (update ledger :entries
                              (fn [entries]
                                (mapv #(if (= :medic-repaired (:observation-kind %))
                                         (-> %
                                             (assoc-in [:medic-operation :request :candidate
                                                        :patch-id] unrelated)
                                             (assoc-in [:medic-operation :request :evidence
                                                        :patch-id] unrelated)
                                             ledger/seal-entry) %)
                                      entries)))
              changed (ledger/seal-ledger changed)
              refused (discharge/append-medic-discharge changed occurrence dir authority)]
          (is (not= other unrelated))
          (is (= :current (:failed-check refused)))
          (is (nil? (:ledger refused)) "current refusal appends nothing"))))))

(deftest later-reopening-is-active-again
  (let [{:keys [dir head]} (git-fixture)
        {:keys [ledger occurrence]} (corrected-ledger dir head)
        discharged (:ledger (discharge/append-medic-discharge ledger occurrence dir authority))
        reopened (ledger/seal-entry
                  {:schema ledger/transition-schema
                   :ordinal (inc (count (:entries discharged)))
                   :injury-id injury-id :state :reopened :attempt 2})
        projected (ledger/effective-active-injuries-from-validated-ledger
                   (ledger/seal-ledger (update discharged :entries conj reopened)))]
    (is (= 1 (:raw-active-count projected)))
    (is (= 1 (:effective-active-count projected)))
    (is (= [] (:masked projected)))))

(deftest counterfactual-and-current-verdicts-are-checked
  (testing "a counterfactual that did not fail refuses"
    (let [{:keys [dir head]} (git-fixture)
          {:keys [ledger occurrence]}
          (corrected-ledger dir head {:pre :original-contract-repaired})
          result (discharge/append-medic-discharge ledger occurrence dir authority)]
      (is (= :counterfactual (:failed-check result)))
      (is (nil? (:ledger result)))))
  (testing "a negative control that was not refused refuses"
    (let [{:keys [dir head]} (git-fixture)
          {:keys [ledger occurrence]}
          (corrected-ledger dir head {:negative :negative-control-passed})
          result (discharge/append-medic-discharge ledger occurrence dir authority)]
      (is (= :counterfactual (:failed-check result)))))
  (testing "a post-repair replay that still fails refuses as current"
    (let [{:keys [dir head]} (git-fixture)
          {:keys [ledger occurrence]}
          (corrected-ledger dir head {:post :original-contract-failed})
          result (discharge/append-medic-discharge ledger occurrence dir authority)]
      (is (= :current (:failed-check result)))))
  (testing "an evaluation that did not find the acceptance repaired refuses as historical"
    (let [{:keys [dir head]} (git-fixture)
          {:keys [ledger occurrence]}
          (corrected-ledger dir head {:evaluation :original-acceptance-not-repaired})
          result (discharge/append-medic-discharge ledger occurrence dir authority)]
      (is (= :historical (:failed-check result))))))
