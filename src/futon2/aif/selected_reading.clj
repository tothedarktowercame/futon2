(ns futon2.aif.selected-reading
  "Q5 step 3a: the 象 reading a selected reading policy still owes, asked for
  and validated for exactly that policy (codex-68 ruling, option (a)).

  A reading policy owes a reading verdict on every arrangement occurrence its
  target's reading did not read (reading-interpretation-debt). The request
  names only those occurrences, bound to the selected policy's immutable key,
  id, arrangement and debt digest and to the reading's excerpt and analysis
  digests. It carries no retrieval query, no unmet want and no candidate
  menu: the responder judges the occurrences it is given and nothing else.

  A response gives, per owed occurrence, \"accepted\" or \"rejected\", a
  reason, and the sha256 of the pattern's library file. It is validated here,
  against the request and the library bytes only; no constructor, admission,
  scorer or selector runs. A published whole-excerpt reading is never
  rewritten: these receipts are a separate record."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.reading-interpretation-debt :as debt])
  (:import [java.nio.file Files]))

(load-identity/register! *ns* *file*)

(def request-schema :wm/selected-reading-request-v1)
(def receipt-schema :wm/selected-reading-receipt-v1)
(def validator-id :wm/selected-reading-validator-v1)
(def default-library-root "/home/joe/code/futon3/library")

(defn- digest [tag value]
  (action-identity/sha256
   (action-identity/printed false [tag (action-identity/canonical value)])))

(defn request
  "The selected-only reading request for DECISION's chosen reading policy,
  or nil when it owes nothing (or is not a reading policy)."
  [decision]
  (let [action (:action decision)
        carrier (:interpretation-debt action)]
    (when (and (map? carrier) (seq (:owed carrier)))
      (let [body {:schema request-schema
                  :target (:target action)
                  :policy-id (:id action)
                  :policy-key (:policy-key action)
                  :debt-digest (:digest carrier)
                  :arrangement-digest (:arrangement-digest carrier)
                  :order (:order carrier)
                  :excerpt-digest (:excerpt-digest carrier)
                  :analysis-digest (:analysis-digest carrier)
                  :owed (:owed carrier)
                  :asks {:what (str "For each owed occurrence only: does its pattern "
                                    "apply to this target's excerpt? Answer accepted "
                                    "or rejected, with a reason and the sha256 of the "
                                    "pattern's library file.")
                         :answer-shape {:request-id "this request's :request-id"
                                        :verdicts [{:occurrence-id "as given"
                                                    :pattern "as given"
                                                    :verdict "accepted | rejected"
                                                    :reason "why, for this excerpt"
                                                    :source_sha256 "sha256 of the library file"}]
                                        :by "answering agent id"}}}]
        (assoc body :request-id (digest request-schema body))))))

(defn- library-sha256 [library-root pattern]
  (let [f (io/file library-root (str pattern ".flexiarg"))]
    (when (.isFile f)
      (load-identity/sha256 (Files/readAllBytes (.toPath f))))))

(defn- verdict-problem
  "Why one VERDICT does not stand for REQUEST, or nil."
  [request library-root verdict]
  (let [owed (set (:owed request))
        occurrence (select-keys verdict [:occurrence-id :pattern])]
    (cond
      (not (map? verdict)) :malformed-verdict
      (not (contains? owed occurrence)) :verdict-for-unowed-occurrence
      (not (#{"accepted" "rejected"} (:verdict verdict))) :verdict-not-accepted-or-rejected
      (not (and (string? (:reason verdict)) (not (str/blank? (:reason verdict)))))
      :verdict-without-reason
      (not (and (string? (:source_sha256 verdict))
                (re-matches #"[0-9a-f]{64}" (:source_sha256 verdict))))
      :malformed-library-pin
      (not= (library-sha256 library-root (:pattern verdict)) (:source_sha256 verdict))
      :library-pin-mismatch
      :else nil)))

(defn validate
  "Validate RESPONSE to REQUEST. Every verdict must name an owed occurrence of
  this request (no duplicates), say accepted or rejected with a reason, and
  pin the pattern's library file bytes under LIBRARY-ROOT. Any failing
  verdict refuses the whole response: {:status :interpretation-validation-
  refused :reasons [...]}, and no receipt is written. Otherwise {:status
  :validated :receipts [...] :remaining [...]}; REMAINING owed occurrences
  without a verdict stay open."
  [request response {:keys [library-root] :or {library-root default-library-root}}]
  (let [verdicts (:verdicts response)
        refuse (fn [reasons] {:status :interpretation-validation-refused
                              :request-id (:request-id request) :reasons reasons
                              :rearm {:condition :all-owed-nodes-validated-for-this-identity
                                      :debt-digest (:debt-digest request)}})]
    (cond
      (not (map? response)) (refuse [{:reason :malformed-response}])
      (not= (:request-id request) (:request-id response))
      (refuse [{:reason :response-for-another-request}])
      (not (and (sequential? verdicts) (seq verdicts))) (refuse [{:reason :no-verdicts}])
      (not= (count verdicts)
            (count (distinct (map #(select-keys % [:occurrence-id :pattern]) verdicts))))
      (refuse [{:reason :duplicate-verdict}])
      :else
      (let [problems (vec (keep (fn [v] (when-let [r (verdict-problem request library-root v)]
                                          {:reason r :verdict v}))
                                verdicts))]
        (if (seq problems)
          (refuse problems)
          (let [response-digest (digest :wm/selected-reading-response-v1 response)
                receipts (mapv (fn [v]
                                 {:schema receipt-schema
                                  :status :validated
                                  :policy-id (:policy-id request)
                                  :policy-key-digest (digest :wm/reading-policy-key (:policy-key request))
                                  :debt-digest (:debt-digest request)
                                  :occurrence-id (:occurrence-id v)
                                  :pattern (:pattern v)
                                  :verdict (keyword (:verdict v))
                                  :reason (:reason v)
                                  :source-sha256 (:source_sha256 v)
                                  :reading {:request-id (:request-id request)
                                            :response-digest response-digest
                                            :excerpt-digest (:excerpt-digest request)
                                            :analysis-digest (:analysis-digest request)
                                            :by (:by response)}
                                  :validation {:validator validator-id :result :valid}})
                               verdicts)
                answered (set (map #(select-keys % [:occurrence-id :pattern]) verdicts))]
            {:status :validated :request-id (:request-id request)
             :receipts receipts
             :remaining (vec (remove answered (:owed request)))}))))))

(defn discharge-status
  "reading-interpretation-debt/enactment-status for DECISION's action with
  the receipts already appended at [:selection-certificate
  :interpretation-discharge :receipts] and the decision's recorded digest."
  [decision recorded-digest]
  (debt/enactment-status (:action decision) recorded-digest
                         (get-in decision [:selection-certificate
                                           :interpretation-discharge :receipts])))
