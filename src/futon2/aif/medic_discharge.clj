(ns futon2.aif.medic-discharge
  "Evidence-bearing `:enacted -> :discharged` for an exact occurrence that a
  checked medic correction already covers.

  SelfHealRoute.lean:2169-2176 admits `:discharged` only from `:enacted`;
  OutOfBandFix.lean:52-58,95-98 requires the historical, current and
  counterfactual checks together.  The checks read retained receipts and git,
  so they live here rather than in the pure ledger module."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.medic-disposition :as medic]
            [futon2.aif.meta-injury-ledger :as ledger]))

(defn- pin? [x]
  (and (map? x) (string? (:path x)) (string? (:sha256 x))))

(defn read-checked-pin
  "The EDN value at PIN when its bytes still hash to the pinned sha256."
  [pin]
  (when (pin? pin)
    (try
      (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file (:path pin))))]
        (when (= (:sha256 pin) (load-identity/sha256 bytes))
          (edn/read-string (String. bytes "UTF-8"))))
      (catch Throwable _ nil))))

(defn- ancestor-of-head? [repository commit]
  (and (string? commit)
       (boolean (re-matches #"[0-9a-f]{40}" commit))
       (zero? (:exit (shell/sh "git" "-C" repository
                               "merge-base" "--is-ancestor" commit "HEAD")))))

(defn evidence-check
  "First failing OutOfBandFix check for RECEIPT-PIN, or nil when all hold.
  :historical  the authority and evaluation receipts still hash to their pins
               and the evaluation verdict is :original-acceptance-repaired;
  :current     the repair commit is an ancestor of REPOSITORY HEAD and the
               post-repair replay is :original-contract-repaired;
  :counterfactual the pre-repair replay is :original-contract-failed and the
               negative control was refused."
  [receipt-pin commit repository]
  (let [receipt (read-checked-pin receipt-pin)
        evaluation (some-> receipt :evaluation-pin read-checked-pin)
        replay (fn [k] (some-> evaluation k read-checked-pin :verdict))]
    (cond
      (not (and receipt evaluation
                (= :original-acceptance-repaired (:verdict evaluation))))
      :historical

      (not (and (ancestor-of-head? repository commit)
                (= :original-contract-repaired (replay :post-replay-pin))))
      :current

      (not (and (= :original-contract-failed (replay :pre-replay-pin))
                (= :negative-control-refused (replay :negative-control-pin))))
      :counterfactual

      :else nil)))

(defn- refusal [reason details]
  (merge {:status :refused :reason reason} details))

(defn append-medic-discharge
  "Append `:discharged` for OCCURRENCE [injury-id ordinal] when its latest
  transition is that exact `:enacted` occurrence, a `:medic-repaired`
  observation covers it, and every evidence-check holds.  A refusal returns
  no ledger."
  [ledger-value occurrence repository authority-verifier]
  (let [[injury-id occurrence-ordinal] occurrence
        current (last (filter #(and (ledger/transition? %) (= injury-id (:injury-id %)))
                              (:entries ledger-value)))
        correction (some (fn [entry]
                           (when (and (ledger/observation? entry)
                                      (= :medic-repaired (:observation-kind entry))
                                      (= [injury-id (str occurrence-ordinal)]
                                         (medic/occurrence
                                          (get-in entry [:medic-operation :request
                                                         :candidate]))))
                             entry))
                         (:entries ledger-value))
        candidate (get-in correction [:medic-operation :request :candidate])
        receipt-pin (get-in correction [:medic-operation :authority-receipt-pin])
        commit (:patch-id candidate)]
    (cond
      (or (nil? current)
          (not= (str occurrence-ordinal) (str (:ordinal current)))
          (not= :enacted (:state current)))
      (refusal :medic-discharge/not-enacted
               {:failed-check :not-enacted :occurrence occurrence})

      (nil? correction)
      (refusal :medic-discharge/correction-missing
               {:failed-check :correction :occurrence occurrence})

      :else
      (if-let [failed (evidence-check receipt-pin commit repository)]
        (refusal :medic-discharge/evidence-refused
                 {:failed-check failed :occurrence occurrence
                  :evidence-pin receipt-pin :repair-commit commit})
        (ledger/append-transition
         ledger-value {:injury-id injury-id :state :discharged
                       :attempt (:attempt current) :repair-id (:repair-id current)
                       :outcome :success :reason :medic-correction
                       :medic-correction-pin (:entry-pin correction)
                       :medic-receipt-pin receipt-pin
                       :medic-evidence-pin (:evidence-pin candidate)
                       :repair-commit commit}
         authority-verifier)))))
