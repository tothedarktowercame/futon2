(ns futon2.aif.meta-medic-authority
  "Pure offline verification of an acyclic medic authority bundle.

  Artifact bytes establish integrity only. Authentication/approval is a
  separate caller-supplied TRUSTED? predicate over exact receipt scope. This
  namespace performs no filesystem, network, store, or ledger mutation."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.cause-reattribution :as reattribution]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.medic-disposition :as medic]
            [futon2.aif.out-of-band-fix :as out-of-band])
  (:import [java.io PushbackReader StringReader]))

(def intent-schema :wm/meta-medic-intent-v1)
(def evaluation-schema :wm/meta-medic-evaluation-v1)
(def review-schema :wm/meta-medic-qualification-review-v1)
(def authorization-schema :wm/meta-medic-authority-v1)
(def genuine-intent-schema :wm/meta-medic-genuine-repair-intent-v1)
(def genuine-evaluation-schema :wm/meta-medic-genuine-repair-evaluation-v1)
(def genuine-review-schema :wm/meta-medic-genuine-repair-review-v1)
(def genuine-authorization-schema :wm/meta-medic-genuine-repair-authority-v1)
(def genuine-replay-schema :wm/meta-medic-genuine-repair-replay-v1)
(def recovery-intent-schema :wm/meta-medic-transaction-recovery-intent-v1)
(def recovery-evaluation-schema :wm/meta-medic-transaction-recovery-evaluation-v1)
(def recovery-review-schema :wm/meta-medic-transaction-recovery-review-v1)
(def recovery-authorization-schema :wm/meta-medic-transaction-recovery-authority-v1)
(deftype ^:private AuthorityResolution [identity verifier])

(defn resolved-context? [value]
  (instance? AuthorityResolution value))

(defn resolution-identity [value]
  (when (resolved-context? value) (.-identity ^AuthorityResolution value)))

(defn with-retained-authorities
  "Extend the resolved external medic authority with entry-local authorities
  whose complete evidence is retained by the entry itself. Either verifier
  may accept an entry; the retained one accepts only entries whose own receipt
  re-derives."
  [external]
  (fn [observation]
    (or (try (true? (external observation)) (catch Throwable _ false))
        (out-of-band/retained-checked-medic-authority observation)
        (reattribution/retained-authority observation))))

(defn resolution-verifier [value]
  (when (resolved-context? value)
    (with-retained-authorities (.-verifier ^AuthorityResolution value))))

(def required-findings
  {:producer-qualified-non-injury
   {:classification :producer-qualified-already-satisfied
    :empty-history :no-injury
    :attributable-retry :retry-inconclusive
    :unsupported-refusal :injury
    :done-at-base :injury
    :guard-removal :rejected
    :qualifier-removal :rejected}
   :successful-close-non-injury
   {:classification :successful-task-outcome
    :empty-history :no-injury
    :attributable-retry :discharged
    :unsupported-refusal :injury
    :done-at-base :injury
    :image-removal :injury
    :failure-present :injury}})

(def ^:private intent-fields
  #{:schema :scope :disposition-kind :injury-id :ordinal :ledger-entry-id
    :predecessor-entry-id :state :attempt :ledger-root :original-record-pin
    :acceptance-contract-pin :patch-revision :classifier-revision :replay
    :evaluator :reviewer})

(def ^:private genuine-intent-fields
  #{:schema :scope :disposition-kind :injury-id :ordinal :ledger-entry-id
    :predecessor-entry-id :state :attempt :ledger-root :cause-record-pin
    :acceptance-contract-pin :patch-revision :source-revision
    :pre-replay-pin :post-replay-pin :negative-control-pin :evaluator :reviewer})

(def ^:private recovery-intent-fields
  #{:schema :scope :injury-id :repair-id :attempt :ledger-entry-id
    :ledger-entry-pin :canonical-transaction-path :canonical-transaction-sha256
    :deferred-transaction-path :deferred-transaction-sha256
    :ledger-author-receipt-pin :ledger-author-patch
    :author :evaluator :reviewer})

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

(def ^:private max-artifact-parse-cache-entries 384)

(defonce ^:private !artifact-parse-cache
  (atom {:entries {} :order []}))

(def ^:dynamic *artifact-parse-observer*
  "Test/diagnostic observer invoked with the verified sha before each parse."
  (fn [_] nil))

(defn reset-artifact-parse-cache!
  "Clear the bounded memo of successfully parsed artifacts. Intended for tests."
  []
  (reset! !artifact-parse-cache {:entries {} :order []})
  nil)

(defn- cache-artifact! [sha256 result]
  (swap! !artifact-parse-cache
         (fn [{:keys [entries order]}]
           (let [present? (contains? entries sha256)
                 order (cond-> order (not present?) (conj sha256))
                 entries (assoc entries sha256 result)
                 overflow (- (count order) max-artifact-parse-cache-entries)
                 evicted (if (pos? overflow) (subvec order 0 overflow) [])
                 order (if (pos? overflow) (subvec order overflow) order)]
             {:entries (apply dissoc entries evicted) :order order})))
  result)

(defn- parse-artifact [sha256 bytes]
  (*artifact-parse-observer* sha256)
  (try
    (with-open [reader (PushbackReader. (StringReader. bytes))]
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
       :detail (ex-message t)})))

(defn resolve-artifact
  "Resolve one strict typed EDN artifact after opaque hash verification."
  [artifacts pin]
  (let [resolved (resolve-bytes artifacts pin)]
    (if-not (= :resolved (:status resolved))
      resolved
      (let [sha256 (:sha256 pin)]
        (or (get-in @!artifact-parse-cache [:entries sha256])
            (locking !artifact-parse-cache
              (or (get-in @!artifact-parse-cache [:entries sha256])
                  (let [result (parse-artifact sha256 (:bytes resolved))]
                    (if (= :resolved (:status result))
                      (cache-artifact! sha256 result)
                      result)))))))))

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

(defn- verify-historical
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
              expected (get required-findings (:replay intent))
              expected-id (final-operation-id intent-id request evaluation-pin
                                              review-pin authorization-pin)]
          (cond
            (not (and (= evaluation-schema (:schema evaluation))
                      (= intent-id (:intent-digest evaluation))
                      (same-identity? evaluator (:evaluator evaluation))
                      (map? expected)
                      (= expected (:findings evaluation))))
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

(defn- exact-map? [m expected] (and (map? m) (= expected (set (keys m)))))

(defn- genuine-replay?
  [receipt contract-pin phase verdict]
  (and (exact-map? receipt #{:schema :acceptance-contract-pin :phase :verdict})
       (= genuine-replay-schema (:schema receipt))
       (= contract-pin (:acceptance-contract-pin receipt))
       (= phase (:phase receipt))
       (= verdict (:verdict receipt))))

(defn- genuine-predecessor-bound?
  "Bind the model's ordinal predecessor to both representations in the ledger:
  the earlier same-injury transition and this entry's sealed raw predecessor.
  Raw entries deliberately store only the prior sealed entry ID/pin; the
  ordinal exists in the projected medic state, not on the raw entry."
  [ledger ledger-entry intent]
  (let [prior (last (filter #(and (= ledger/transition-schema (:schema %))
                                  (= (:injury-id ledger-entry) (:injury-id %))
                                  (< (:ordinal %) (:ordinal ledger-entry)))
                            (:entries ledger)))
        raw (:injury-predecessor ledger-entry)]
    (if prior
      (and (= (str (:ordinal prior)) (:predecessor-entry-id intent))
           (= (:entry-id prior) (:entry-id raw))
           (= (:entry-pin prior) (:entry-pin raw)))
      (and (nil? (:predecessor-entry-id intent)) (nil? raw)))))

(defn- verify-genuine
  "Verify the separate authority grammar for Lean's GenuineRepairPayload.
  Receipt hashes establish bytes; TRUST independently authenticates the three
  exact principal/pin bindings.  This is not a claim that a live row was
  evaluated or approved."
  [{:keys [operation current ledger ledger-entry intent artifacts evaluation-pin review-pin
           authorization-pin trust]}]
  (try
    (let [request (:request operation)
          candidate (:candidate request)
          evidence (:evidence request)
          intent-id (intent-digest intent)
          cause-pin (:cause-record-pin intent)
          contract-pin (:acceptance-contract-pin intent)
          pre-pin (:pre-replay-pin intent)
          post-pin (:post-replay-pin intent)
          negative-pin (:negative-control-pin intent)
          resolved (into {}
                         (for [[k pin] [[:cause cause-pin] [:pre pre-pin]
                                         [:post post-pin] [:negative negative-pin]
                                         [:evaluation evaluation-pin] [:review review-pin]
                                         [:authorization authorization-pin]]]
                           [k (resolve-artifact artifacts pin)]))
          contract-result (resolve-bytes artifacts contract-pin)]
      (cond
        (not (and (exact-map? intent genuine-intent-fields)
                  (= genuine-intent-schema (:schema intent))))
        {:status :refused :reason :genuine-intent/malformed}
        (not= candidate evidence) {:status :refused :reason :payload/swap}
        (not (medic/payload-valid? candidate))
        {:status :refused :reason :payload/malformed}
        (not= :genuine-repair (:kind candidate))
        {:status :refused :reason :genuine-intent/kind-mismatch}
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
        (not (genuine-predecessor-bound? ledger ledger-entry intent))
        {:status :refused :reason :intent/predecessor-mismatch}
        (not= [(:cause-record-id current) (:cause-pin current)]
              [(:path cause-pin) (:sha256 cause-pin)])
        {:status :refused :reason :intent/cause-mismatch}
        (not= [(:kind candidate) (:injury-id candidate) (:entry-id candidate)
               (:predecessor-entry-id candidate) (:cause-record-id candidate)
               (:cause-pin candidate) (:acceptance-contract candidate)
               (:pre-replay-receipt candidate) (:post-replay-receipt candidate)
               (:negative-control-receipt candidate) (:patch-id candidate)
               (:source-revision candidate) (:evaluator candidate) (:reviewer candidate)
               (:evidence-pin candidate) (:original-acceptance-repaired? candidate)]
              [:genuine-repair (:injury-id intent) (str (:ordinal intent))
               (:predecessor-entry-id intent) (:path cause-pin) (:sha256 cause-pin)
               (:sha256 contract-pin) (:sha256 pre-pin) (:sha256 post-pin)
               (:sha256 negative-pin) (:patch-revision intent) (:source-revision intent)
               (get-in intent [:evaluator :seat]) (get-in intent [:reviewer :seat])
               (:sha256 evaluation-pin) true])
        {:status :refused :reason :intent/payload-mismatch}
        (not (every? pin-valid? [cause-pin contract-pin pre-pin post-pin negative-pin
                                 evaluation-pin review-pin authorization-pin]))
        {:status :refused :reason :intent/invalid-pin}
        (not= :resolved (:status contract-result)) contract-result
        (some #(not= :resolved (:status %)) (vals resolved))
        (first (filter #(not= :resolved (:status %)) (vals resolved)))
        :else
        (let [evaluation (get-in resolved [:evaluation :value])
              review (get-in resolved [:review :value])
              authorization (get-in resolved [:authorization :value])
              expected-id (final-operation-id intent-id request evaluation-pin review-pin
                                              authorization-pin)]
          (cond
            (not (and (exact-map? evaluation #{:schema :intent-digest :evaluator
                                                :acceptance-contract-pin :pre-replay-pin
                                                :post-replay-pin :negative-control-pin :verdict})
                      (= genuine-evaluation-schema (:schema evaluation))
                      (= intent-id (:intent-digest evaluation))
                      (same-identity? (:evaluator intent) (:evaluator evaluation))
                      (= contract-pin (:acceptance-contract-pin evaluation))
                      (= pre-pin (:pre-replay-pin evaluation))
                      (= post-pin (:post-replay-pin evaluation))
                      (= negative-pin (:negative-control-pin evaluation))
                      (= :original-acceptance-repaired (:verdict evaluation))))
            {:status :refused :reason :evaluation/not-qualified}
            (not (and (genuine-replay? (get-in resolved [:pre :value]) contract-pin
                                        :pre :original-contract-failed)
                      (genuine-replay? (get-in resolved [:post :value]) contract-pin
                                        :post :original-contract-repaired)
                      (genuine-replay? (get-in resolved [:negative :value]) contract-pin
                                        :negative :negative-control-refused)))
            {:status :refused :reason :replay/not-qualified}
            (not (and (exact-map? review #{:schema :intent-digest :evaluation-pin :reviewer :verdict})
                      (= genuine-review-schema (:schema review))
                      (= intent-id (:intent-digest review))
                      (= evaluation-pin (:evaluation-pin review))
                      (same-identity? (:reviewer intent) (:reviewer review))
                      (= :genuine-repair-qualified (:verdict review))))
            {:status :refused :reason :review/not-qualified}
            (not (and (exact-map? authorization #{:schema :intent-digest :evaluation-pin
                                                   :review-pin :scope :authorizer :verdict})
                      (= genuine-authorization-schema (:schema authorization))
                      (= intent-id (:intent-digest authorization))
                      (= evaluation-pin (:evaluation-pin authorization))
                      (= review-pin (:review-pin authorization))
                      (= (:scope intent) (:scope authorization))
                      (= :qualified (:verdict authorization))))
            {:status :refused :reason :authorization/not-qualified}
            (not (trusted? trust :evaluation (:evaluator intent) intent-id evaluation-pin))
            {:status :refused :reason :evaluation/untrusted}
            (not (trusted? trust :review (:reviewer intent) intent-id review-pin))
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

(def ^:private recovery-candidate-fields
  #{:injury-id :repair-id :attempt :ledger-entry-id :ledger-entry-pin
    :canonical-transaction-path :canonical-transaction-sha256
    :deferred-transaction-path :deferred-transaction-sha256
    :ledger-author-receipt-pin :ledger-author-patch})

(defn- verify-recovery
  [{:keys [operation current ledger intent artifacts evaluation-pin review-pin
           authorization-pin trust]}]
  (try
    (let [request (:request operation)
          candidate (:candidate request)
          evidence (:evidence request)
          intent-id (intent-digest intent)
          evaluation-result (resolve-artifact artifacts evaluation-pin)
          review-result (resolve-artifact artifacts review-pin)
          authorization-result (resolve-artifact artifacts authorization-pin)
          latest (last (filter #(and (= ledger/transition-schema (:schema %))
                                     (= (:injury-id intent) (:injury-id %)))
                               (:entries ledger)))
          author (:author intent)]
      (cond
        (not (and (exact-map? intent recovery-intent-fields)
                  (= recovery-intent-schema (:schema intent))))
        {:status :refused :reason :recovery-intent/malformed}
        (not= candidate evidence) {:status :refused :reason :payload/swap}
        (not (exact-map? candidate recovery-candidate-fields))
        {:status :refused :reason :recovery-payload/malformed}
        (not= (select-keys intent recovery-candidate-fields) candidate)
        {:status :refused :reason :recovery-intent/payload-mismatch}
        (not= [(:entry-id latest) (:entry-pin latest)]
              [(:ledger-entry-id intent) (:ledger-entry-pin intent)])
        {:status :refused :reason :recovery-intent/root-mismatch}
        (not= [(:entry-id current) (:entry-pin current)]
              [(:ledger-entry-id intent) (:ledger-entry-pin intent)])
        {:status :refused :reason :recovery-intent/current-mismatch}
        (not (every? pin-valid? [(:ledger-entry-pin intent)
                                 (:ledger-author-receipt-pin intent)
                                 evaluation-pin review-pin authorization-pin]))
        {:status :refused :reason :recovery-intent/invalid-pin}
        (some #(not= :resolved (:status %))
              [evaluation-result review-result authorization-result])
        (first (filter #(not= :resolved (:status %))
                       [evaluation-result review-result authorization-result]))
        :else
        (let [evaluation (:value evaluation-result)
              review (:value review-result)
              authorization (:value authorization-result)
              evaluator (:evaluator intent)
              reviewer (:reviewer intent)
              authorizer (:authorizer authorization)
              expected-id (final-operation-id intent-id request evaluation-pin
                                              review-pin authorization-pin)]
          (cond
            (not (and (exact-map? evaluation #{:schema :intent-digest :evaluator :verdict})
                      (= recovery-evaluation-schema (:schema evaluation))
                      (= intent-id (:intent-digest evaluation))
                      (same-identity? evaluator (:evaluator evaluation))
                      (= :transaction-recovery-qualified (:verdict evaluation))))
            {:status :refused :reason :evaluation/not-qualified}
            (not (and (exact-map? review #{:schema :intent-digest :evaluation-pin
                                           :reviewer :verdict})
                      (= recovery-review-schema (:schema review))
                      (= intent-id (:intent-digest review))
                      (= evaluation-pin (:evaluation-pin review))
                      (same-identity? reviewer (:reviewer review))
                      (= :transaction-recovery-qualified (:verdict review))))
            {:status :refused :reason :review/not-qualified}
            (not (and (exact-map? authorization #{:schema :intent-digest :evaluation-pin
                                                   :review-pin :scope :authorizer :verdict})
                      (= recovery-authorization-schema (:schema authorization))
                      (= intent-id (:intent-digest authorization))
                      (= evaluation-pin (:evaluation-pin authorization))
                      (= review-pin (:review-pin authorization))
                      (= (:scope intent) (:scope authorization))
                      (= :qualified (:verdict authorization))))
            {:status :refused :reason :authorization/not-qualified}
            (or (same-identity? author evaluator)
                (same-identity? author reviewer)
                (same-identity? author authorizer)
                (same-identity? evaluator reviewer))
            {:status :refused :reason :roles/not-independent}
            (not (trusted? trust :evaluation evaluator intent-id evaluation-pin))
            {:status :refused :reason :evaluation/untrusted}
            (not (trusted? trust :review reviewer intent-id review-pin))
            {:status :refused :reason :review/untrusted}
            (not (trusted? trust :authorization authorizer intent-id authorization-pin))
            {:status :refused :reason :authorization/untrusted}
            (not= expected-id (:operation-id operation))
            {:status :refused :reason :operation/final-binding-mismatch}
            (not= authorization-pin (:authority-receipt-pin operation))
            {:status :refused :reason :operation/authority-pin-mismatch}
            :else {:status :verified :intent-digest intent-id
                   :operation-id expected-id}))))
    (catch Throwable t
      {:status :refused :reason :authority/malformed :detail (ex-message t)})))

(defn verify
  "Verify one of the three distinct medic authority grammars. Existing
  historical and genuine-repair requests never fall through to recovery."
  [bundle]
  (case (get-in bundle [:intent :schema])
    :wm/meta-medic-intent-v1 (verify-historical bundle)
    :wm/meta-medic-genuine-repair-intent-v1 (verify-genuine bundle)
    :wm/meta-medic-transaction-recovery-intent-v1 (verify-recovery bundle)
    {:status :refused :reason :intent/malformed}))

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
                                           :ledger ledger
                                           :current current
                                           :ledger-entry ledger-entry)))))))

(defn resolve-context
  "Validate and resolve one or more exact-scope authority bundles.

  This is the shared process-local composition boundary.  The returned
  verifier accepts an operation only when exactly one bundle verifies it;
  overlapping approvals are refused as ambiguous.  IDENTITY contains only
  stable digests and receipt pins, never artifact bytes or executable trust."
  [context]
  (let [bundles (cond
                  (and (map? context) (vector? (:bundles context))) (:bundles context)
                  (map? context) [context]
                  :else nil)]
    (if-not (seq bundles)
      {:status :refused :reason :meta-self-heal/medic-authority-unavailable}
      (let [valid? (fn [bundle]
                     (and (map? bundle) (ifn? (:trust bundle))
                          (contains? #{intent-schema genuine-intent-schema
                                       recovery-intent-schema}
                                     (get-in bundle [:intent :schema]))
                          (map? (:artifacts bundle)) (seq (:artifacts bundle))
                          (every? pin-valid? ((juxt :evaluation-pin :review-pin
                                                   :authorization-pin) bundle))))]
        (if-not (every? valid? bundles)
          {:status :refused :reason :meta-self-heal/medic-authority-unavailable}
          (let [scopes (mapv (fn [bundle]
                               {:intent-digest (intent-digest (:intent bundle))
                                :evaluation-pin (:evaluation-pin bundle)
                                :review-pin (:review-pin bundle)
                                :authorization-pin (:authorization-pin bundle)})
                             bundles)]
            (if-not (= (count scopes) (count (distinct scopes)))
              {:status :refused :reason :meta-self-heal/medic-authority-ambiguous}
              (let [verifiers (mapv authority-verifier bundles)
                    identity {:schema :wm/meta-medic-authority-context-identity-v1
                              :scopes scopes}
                    verifier (fn [observation]
                               (= 1 (count (filter true?
                                                   (map #(% observation) verifiers)))))]
                {:status :ready
                 :identity identity
                 :resolution (AuthorityResolution. identity verifier)}))))))))

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
