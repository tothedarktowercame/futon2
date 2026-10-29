(ns futon2.aif.ordinary-author-refusal-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-medic-authority :as authority]
            [futon2.aif.meta-self-heal-production :as production]
            [futon2.aif.ordinary-author-refusal :as refusal]))

(def live-ledger
  "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22/ledger.edn")
(def live-ledger-sha
  "6bfbd39c444c2235153b288ad2d2331ffa3903a91f21d36a4a4e95ff40f828f0")
(def live-record
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-29-39a845a3-496a-4bce-888c-e8b0ca2835ab.edn")
(def live-record-sha
  "591c5505e2a155663e9f5e676e8dccc28c7bacef3bb53cc2a195dd0f846601dd")
(def live-job
  "/home/joe/notes/wm-next/criteria-completeness/run001/author-answer-invalid/job.json")
(def live-job-sha
  "d502f118b58e00523e3f6901457a34b77e6fcfc3b56018c58dd7413443ee49be")

(defn- root []
  (.toFile (java.nio.file.Files/createTempDirectory
            "ordinary-author-refusal-"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- pin! [root name value]
  (let [file (java.io.File. root name)
        bytes (.getBytes (pr-str value) "UTF-8")]
    (spit file (String. bytes "UTF-8"))
    {:path (.getCanonicalPath file) :sha256 (identity/sha256 bytes)}))

(defn- exact-prior-authority [history]
  (let [operations
        (into {}
              (keep (fn [entry]
                      (when (= :medic-repaired (:observation-kind entry))
                        (let [operation (:medic-operation entry)]
                          [(:operation-id operation) operation]))))
              (:entries history))]
    (fn [{:keys [operation current candidate evidence]}]
      (and (= operation (get operations (:operation-id operation)))
           (= candidate evidence (get-in operation [:request :candidate]))
           (= [(:injury-id candidate) (:entry-id candidate)]
              [(:injury-id current) (:entry-id current)])))))

(defn- loaders [ledger-path verifier transaction-path]
  (let [opts {:meta-injury-ledger-path ledger-path
              :meta-repair-transaction-path transaction-path
              :meta-medic-authority-verifier verifier}
        context (production/diagnosis-context opts)]
    {:load-ledger (store/load-ledger ledger-path verifier)
     :load-durable-ledger (store/load-durable-ledger ledger-path verifier)
     :diagnosis-context (:durable-ledger context)
     :production-readiness (production/production-readiness opts context)}))

(defn fixture [failure-kind]
  (let [root (root)
        record-kind (if (= :author-response-unparseable failure-kind)
                      :open-cascade-author-answer-invalid
                      failure-kind)
        job {:job-id "job/author" :state "done"
             :result "verbatim malformed JSON"}
        job-pin (pin! root "job.edn" job)
        record {:run/id "run/author" :click/id "click/author" :traceWritten true
                :registered-run/model-usage {:jobs [{:job-id "job/author"}]}
                :failure {:kind record-kind :stage :cascade-resume}}
        record-pin (pin! root "record.edn" record)
        opened (ledger/append-transition
                ledger/empty-ledger
                {:injury-id "injury/author" :state :open :attempt 1
                 :cause-id "cause/author" :cause-record-pin record-pin
                 :failure-stage :cascade-resume :failure-kind record-kind
                 :detected-click "click/author"})
        entry (:entry opened)
        candidate {:kind :ordinary-author-refusal
                   :injury-id "injury/author" :entry-id (str (:ordinal entry))
                   :predecessor-entry-id nil
                   :patch-id "a8c12af10b588eaea784e288c0fcc6736b0216de"
                   :reviewer "medic-reviewer" :evaluator "medic-evaluator"
                   :evidence-pin (:sha256 job-pin)
                   :original-record-id (:path record-pin) :original-pin (:sha256 record-pin)
                   :author-response-id (:path job-pin) :author-response-pin (:sha256 job-pin)
                   :classifier-revision "a8c12af10b588eaea784e288c0fcc6736b0216de"
                   :replay :ordinary-author-refusal-non-injury}
        receipt {:schema refusal/receipt-schema :verifier refusal/verifier-id
                 :candidate candidate
                 :record-pin record-pin :author-response-pin job-pin
                 :fix-commit (:patch-id candidate)
                 :replay-result :author-response-unparseable}
        receipt-pin (pin! root "receipt.edn" receipt)
        operation {:operation-id "operation/author-refusal"
                   :expected-ledger-pin (get-in opened [:ledger :ledger-pin])
                   :request {:candidate candidate :evidence candidate}
                   :authority-receipt-pin receipt-pin}]
    {:root root :record-pin record-pin :opened opened :operation operation
     :authority (authority/with-retained-authorities (constantly false))}))

(deftest retained-author-refusal-appends-and-revalidates
  (let [{:keys [opened operation record-pin authority]}
        (fixture :author-response-unparseable)
        appended (ledger/append-medic-repaired (:ledger opened) record-pin
                                                operation authority)]
    (is (= :appended (:status appended)) (pr-str appended))
    (is (= :valid (:status (ledger/validate-ledger (:ledger appended) authority))))
    (is (empty? (get-in (ledger/effective-active-injuries
                         (:ledger appended) authority) [:injuries])))))

(deftest apparatus-and-mismatched-response-are-inert
  (let [{:keys [opened operation record-pin authority]} (fixture :hot-load-failed)
        apparatus (ledger/append-medic-repaired (:ledger opened) record-pin
                                                 operation authority)
        good (fixture :author-response-unparseable)
        tampered (assoc-in (:operation good)
                           [:request :candidate :author-response-pin]
                           (apply str (repeat 64 "0")))
        tampered (assoc-in tampered [:request :evidence]
                           (get-in tampered [:request :candidate]))
        mismatch (ledger/append-medic-repaired
                  (get-in good [:opened :ledger]) (:record-pin good)
                  tampered (:authority good))]
    (is (= :medic/authority-rejected (:reason apparatus)))
    (is (= :medic/authority-rejected (:reason mismatch)))))

(deftest retained-ordinal-124-replays-and-loads-through-every-production-path
  ;; This fixture preserves the property under correction: ordinal 124's
  ;; exact retained answer is undecodable, while current terminal policy names
  ;; that exhausted author-format refusal as a non-injury.
  (let [ledger-bytes (java.nio.file.Files/readAllBytes (.toPath (io/file live-ledger)))
        history (edn/read-string (String. ledger-bytes "UTF-8"))
        current (last (:entries history))
        root (root)
        candidate {:kind :ordinary-author-refusal
                   :injury-id (:injury-id current) :entry-id (str (:ordinal current))
                   :predecessor-entry-id (:injury-predecessor current)
                   :patch-id "a8c12af10b588eaea784e288c0fcc6736b0216de"
                   :reviewer "medic-reviewer" :evaluator "medic-evaluator"
                   :evidence-pin live-job-sha
                   :original-record-id live-record :original-pin live-record-sha
                   :author-response-id live-job :author-response-pin live-job-sha
                   :classifier-revision "a8c12af10b588eaea784e288c0fcc6736b0216de"
                   :replay :ordinary-author-refusal-non-injury}
        receipt {:schema refusal/receipt-schema :verifier refusal/verifier-id
                 :candidate candidate
                 :record-pin {:path live-record :sha256 live-record-sha}
                 :author-response-pin {:path live-job :sha256 live-job-sha}
                 :fix-commit (:patch-id candidate)
                 :replay-result :author-response-unparseable}
        receipt-pin (pin! root "ordinary-author-refusal-receipt.edn" receipt)
        operation {:operation-id "operation/ordinal-124-author-refusal"
                   :expected-ledger-pin (:ledger-pin history)
                   :request {:candidate candidate :evidence candidate}
                   :authority-receipt-pin receipt-pin}
        verifier (authority/with-retained-authorities
                  (exact-prior-authority history))
        appended (ledger/append-medic-repaired
                  history {:path live-record :sha256 live-record-sha}
                  operation verifier)
        ledger-path (str (io/file root "ledger.edn"))
        transaction-path (str (io/file root "transaction.edn"))]
    (is (= live-ledger-sha (identity/sha256 ledger-bytes)))
    (is (= 124 (count (:entries history))))
    (is (= :appended (:status appended)) (pr-str appended))
    (is (= (:entries history) (subvec (get-in appended [:ledger :entries]) 0 124)))
    (is (empty? (get-in (ledger/effective-active-injuries
                         (:ledger appended) verifier) [:injuries])))
    (spit ledger-path (str (pr-str (:ledger appended)) "\n"))
    (let [results (loaders ledger-path verifier transaction-path)]
      (is (= #{:loaded}
             (set (map :status
                       ((juxt :load-ledger :load-durable-ledger
                              :diagnosis-context) results))))
          (pr-str results))
      (is (= :ready (get-in results [:production-readiness :status]))
          (pr-str (:production-readiness results))))))
