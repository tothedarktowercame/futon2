(ns futon2.aif.meta-medic-authority
  "Pure offline verification of an acyclic medic authority bundle.

  Artifact bytes establish integrity only. Authentication/approval is a
  separate caller-supplied TRUSTED? predicate over exact receipt scope. This
  namespace performs no filesystem, network, store, or ledger mutation."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.medic-disposition :as medic])
  (:import [java.io PushbackReader StringReader]))

(def intent-schema :wm/meta-medic-intent-v1)
(def evaluation-schema :wm/meta-medic-evaluation-v1)
(def review-schema :wm/meta-medic-qualification-review-v1)
(def authorization-schema :wm/meta-medic-authority-v1)

(def required-findings
  {:classification :producer-qualified-already-satisfied
   :empty-history :no-injury
   :attributable-retry :retry-inconclusive
   :unsupported-refusal :injury
   :done-at-base :injury
   :guard-removal :rejected
   :qualifier-removal :rejected})

(def ^:private intent-fields
  #{:schema :scope :disposition-kind :injury-id :ordinal :ledger-entry-id
    :predecessor-entry-id :state :attempt :ledger-root :original-record-pin
    :acceptance-contract-pin :patch-revision :classifier-revision :replay
    :evaluator :reviewer})

(defn digest [value] (identity/digest value))
(defn intent-digest [intent] (digest [:medic-intent intent]))

(defn pin-valid? [pin]
  (and (map? pin) (= #{:path :sha256} (set (keys pin)))
       (string? (:path pin)) (not (str/blank? (:path pin)))
       (string? (:sha256 pin)) (re-matches #"[0-9a-f]{64}" (:sha256 pin))))

(defn resolve-bytes
  "Resolve opaque UTF-8 bytes from the supplied map and verify their pin.
  This performs no decoding and opens no path implicitly."
  [artifacts pin]
  (let [bytes (get artifacts (:path pin))]
    (cond
      (not (pin-valid? pin)) {:status :refused :reason :artifact/invalid-pin}
      (not (string? bytes)) {:status :refused :reason :artifact/missing}
      (not= (:sha256 pin) (load-identity/sha256 (.getBytes bytes "UTF-8")))
      {:status :refused :reason :artifact/hash-drift}
      :else {:status :resolved :bytes bytes})))

(defn resolve-artifact
  "Resolve one strict typed EDN artifact after opaque hash verification."
  [artifacts pin]
  (let [resolved (resolve-bytes artifacts pin)]
    (if-not (= :resolved (:status resolved))
      resolved
      (try
        (with-open [reader (PushbackReader. (StringReader. (:bytes resolved)))]
          (let [eof (Object.)
                value (edn/read {:eof eof} reader)
                trailing (edn/read {:eof eof} reader)]
            (cond
              (identical? eof value) {:status :refused :reason :artifact/empty}
              (not (identical? eof trailing))
              {:status :refused :reason :artifact/multiple-forms}
              :else {:status :resolved :value value})))
        (catch Throwable t
          {:status :refused :reason :artifact/malformed
           :detail (ex-message t)})))))

(defn final-operation-id
  "Digest the final request and receipt pins after the acyclic receipts exist.
  Receipts bind INTENT-DIGEST, never this final digest."
  [intent-digest request evaluation-pin review-pin authorization-pin]
  (digest [:medic-final-operation
           {:intent-digest intent-digest :request request
            :evaluation-pin evaluation-pin :review-pin review-pin
            :authorization-pin authorization-pin}]))

(defn- same-identity? [a b]
  (and (map? a) (= #{:seat :session} (set (keys a)))
       (every? #(and (string? %) (not (str/blank? %))) (vals a))
       (= a b)))

(defn- trusted? [trust kind identity intent pin]
  (try
    (true? (trust {:kind kind :identity identity
                   :intent-digest intent :receipt-pin pin}))
    (catch Throwable _ false)))

(defn verify
  "Verify one fully assembled bundle. TRUST is independent of artifact bytes
  and must authenticate exact receipt scope. Returns typed refusal otherwise."
  [{:keys [operation current ledger-entry intent artifacts evaluation-pin review-pin
           authorization-pin trust]}]
  (try
    (let [request (:request operation)
          candidate (:candidate request)
          evidence (:evidence request)
          intent-id (intent-digest intent)
          evaluation-result (resolve-artifact artifacts evaluation-pin)
          review-result (resolve-artifact artifacts review-pin)
          authorization-result (resolve-artifact artifacts authorization-pin)
          original-pin (:original-record-pin intent)
          contract-result (resolve-bytes artifacts (:acceptance-contract-pin intent))
          original-result (resolve-artifact artifacts original-pin)]
      (cond
        (not (and (map? intent) (= intent-fields (set (keys intent)))
                  (= intent-schema (:schema intent))))
        {:status :refused :reason :intent/malformed}
        (not= candidate evidence) {:status :refused :reason :payload/swap}
        (not (medic/payload-valid? candidate))
        {:status :refused :reason :payload/malformed}
        (= (get-in intent [:evaluator :seat]) (get-in intent [:reviewer :seat]))
        {:status :refused :reason :roles/not-independent}
        (not= (:expected-ledger-pin operation) (:ledger-root intent))
        {:status :refused :reason :intent/root-mismatch}
        (not= [(:injury-id current) (:entry-id current)]
              [(:injury-id intent) (str (:ordinal intent))])
        {:status :refused :reason :intent/occurrence-mismatch}
        (not= [(:injury-id ledger-entry) (:ordinal ledger-entry)
               (:entry-id ledger-entry) (:state ledger-entry) (:attempt ledger-entry)]
              [(:injury-id intent) (:ordinal intent) (:ledger-entry-id intent)
               (:state intent) (:attempt intent)])
        {:status :refused :reason :intent/ledger-entry-mismatch}
        (not= [(:cause-record-id current) (:cause-pin current)]
              [(:path original-pin) (:sha256 original-pin)])
        {:status :refused :reason :intent/cause-mismatch}
        (not= (:patch-id candidate) (:patch-revision intent))
        {:status :refused :reason :intent/patch-mismatch}
        (not= (:classifier-revision candidate) (:classifier-revision intent))
        {:status :refused :reason :intent/classifier-mismatch}
        (not= [(:kind candidate) (:injury-id candidate) (:entry-id candidate)
               (:predecessor-entry-id candidate) (:original-record-id candidate)
               (:original-pin candidate) (:patch-id candidate)
               (:classifier-revision candidate) (:replay candidate)
               (:evaluator candidate) (:reviewer candidate)
               (:evidence-pin candidate)]
              [(:disposition-kind intent) (:injury-id intent) (str (:ordinal intent))
               (:predecessor-entry-id intent) (:path original-pin)
               (:sha256 original-pin) (:patch-revision intent)
               (:classifier-revision intent) (:replay intent)
               (get-in intent [:evaluator :seat]) (get-in intent [:reviewer :seat])
               (:sha256 evaluation-pin)])
        {:status :refused :reason :intent/payload-mismatch}
        (not (pin-valid? (:acceptance-contract-pin intent)))
        {:status :refused :reason :intent/contract-mismatch}
        (not= :resolved (:status original-result)) original-result
        (not= :resolved (:status contract-result)) contract-result
        (not= :resolved (:status evaluation-result)) evaluation-result
        (not= :resolved (:status review-result)) review-result
        (not= :resolved (:status authorization-result)) authorization-result
        :else
        (let [evaluation (:value evaluation-result)
              review (:value review-result)
              authorization (:value authorization-result)
              evaluator (:evaluator intent)
              reviewer (:reviewer intent)
              expected-id (final-operation-id intent-id request evaluation-pin
                                              review-pin authorization-pin)]
          (cond
            (not (and (= evaluation-schema (:schema evaluation))
                      (= intent-id (:intent-digest evaluation))
                      (same-identity? evaluator (:evaluator evaluation))
                      (= required-findings (:findings evaluation))))
            {:status :refused :reason :evaluation/not-qualified}
            (not (and (= review-schema (:schema review))
                      (= intent-id (:intent-digest review))
                      (= evaluation-pin (:evaluation-pin review))
                      (same-identity? reviewer (:reviewer review))
                      (= :historical-misclassification-qualified (:verdict review))))
            {:status :refused :reason :review/not-qualified}
            (not (and (= authorization-schema (:schema authorization))
                      (= intent-id (:intent-digest authorization))
                      (= evaluation-pin (:evaluation-pin authorization))
                      (= review-pin (:review-pin authorization))
                      (= (:scope intent) (:scope authorization))
                      (= :qualified (:verdict authorization))))
            {:status :refused :reason :authorization/not-qualified}
            (not (trusted? trust :evaluation evaluator intent-id evaluation-pin))
            {:status :refused :reason :evaluation/untrusted}
            (not (trusted? trust :review reviewer intent-id review-pin))
            {:status :refused :reason :review/untrusted}
            (not (trusted? trust :authorization (:authorizer authorization)
                           intent-id authorization-pin))
            {:status :refused :reason :authorization/untrusted}
            (not= expected-id (:operation-id operation))
            {:status :refused :reason :operation/final-binding-mismatch}
            (not= authorization-pin (:authority-receipt-pin operation))
            {:status :refused :reason :operation/authority-pin-mismatch}
            :else {:status :verified :intent-digest intent-id
                   :operation-id expected-id}))))
    (catch Throwable t
      {:status :refused :reason :authority/malformed :detail (ex-message t)})))

(defn authority-verifier
  "Adapt a fixed offline BUNDLE to meta-injury-ledger's supplied verifier API."
  [bundle]
  (fn [{:keys [ledger operation current candidate evidence]}]
    (let [ledger-entry (some #(when (and (= (:injury-id current) (:injury-id %))
                                         (= (:entry-id current) (str (:ordinal %)))) %)
                             (:entries ledger))
          checked-operation (assoc operation :request {:candidate candidate
                                                       :evidence evidence})]
      (= :verified (:status (verify (assoc bundle :operation checked-operation
                                           :current current
                                           :ledger-entry ledger-entry)))))))

(defn a9-unapproved-candidate
  "Retained-snapshot manifest only. It intentionally cannot verify."
  []
  {:schema :wm/meta-medic-candidate-manifest-v1
   :status :unapproved
   :injury-id "injury/a9b900a41f704673f9d7f35f545f0482b06135ea"
   :retained-ledger-root "bdb8f7289f527fc29656048ebdefcd49dba17d0ef0963d749ad649db2ead163c"
   :occurrence {:ordinal 34
                :entry-id "2a2db7b787d3ce3f3238fa904a92f2fe699a67396ff9e531d5dabdf10c0e300e"
                :state :open :attempt 1}
   :missing [:complete-original-record-bytes
             :trusted-evaluator-attestation
             :qualification-review-receipt
             :trusted-reviewer-attestation
             :authorization-receipt
             :trusted-authorizer-attestation
             :fresh-ledger-root-and-occurrence]})
