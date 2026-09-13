(ns futon2.aif.machine-slow-feedback-completeness
  "Pure schema/join validator for externally configured E6b completeness
   records. Production is deliberately unavailable in v1."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.machine-slow-feedback-capture :as capture]
            [futon2.aif.machine-slow-feedback-retrospective-projection :as projection])
  (:import (java.io PushbackReader StringReader)
           (java.nio ByteBuffer)
           (java.nio.charset CodingErrorAction StandardCharsets)
           (java.security MessageDigest)
           (java.time Instant)
           (java.util Base64)))

(def ^:private required-roles
  [:capture-artifact :writer-inventory :complete-census :completeness-subject
   :acquisition-boundary :review-commission :review-execution :review-artifact
   :review-origin :acceptance])
(def ^:private hex64 #"[0-9a-f]{64}")
(defn- refuse! [kind data] (throw (ex-info (name kind) (assoc data :refusal kind))))
(defn- sha256 [^bytes bs]
  (apply str (map #(format "%02x" (bit-and 255 %))
                  (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs))))))
(defn- strict-edn [^bytes bs]
  (try
    (let [decoder (doto (.newDecoder StandardCharsets/UTF_8)
                    (.onMalformedInput CodingErrorAction/REPORT)
                    (.onUnmappableCharacter CodingErrorAction/REPORT))
          reader (PushbackReader. (StringReader. (str (.decode decoder (ByteBuffer/wrap bs)))))
          eof (Object.) x (edn/read {:eof eof} reader) tail (edn/read {:eof eof} reader)]
      (when (or (identical? eof x) (not (identical? eof tail)))
        (refuse! :e6b-completeness/invalid-edn-cardinality {}))
      x)
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable e
      (throw (ex-info "invalid UTF-8 EDN" {:refusal :e6b-completeness/invalid-edn} e)))))
(defn- resolve-role [role descriptor]
  (when-not (= #{:bytes/base64 :expected-sha256} (set (keys descriptor)))
    (refuse! :e6b-completeness/role-descriptor-invalid {:role role}))
  (when-not (re-matches hex64 (:expected-sha256 descriptor ""))
    (refuse! :e6b-completeness/role-pin-invalid {:role role}))
  (let [bs (try (.decode (Base64/getDecoder) ^String (:bytes/base64 descriptor))
                (catch Throwable e
                  (throw (ex-info "invalid base64" {:refusal :e6b-completeness/invalid-base64
                                                     :role role} e))))]
    (when-not (= (:expected-sha256 descriptor) (sha256 bs))
      (refuse! :e6b-completeness/role-pin-mismatch {:role role}))
    {:role role :sha256 (:expected-sha256 descriptor) :bytes/base64 (:bytes/base64 descriptor)
     :record (strict-edn bs)}))
(defn- exact! [label keys* record]
  (when-not (and (map? record) (= keys* (set (keys record))))
    (refuse! :e6b-completeness/schema-invalid {:role label}))
  record)
(defn- nonblank? [x] (and (string? x) (not (str/blank? x))))
(defn- instant? [x] (try (Instant/parse x) true (catch Throwable _ false)))
(defn- <=time [& xs]
  (and (every? instant? xs)
       (every? (fn [[a b]] (not (.isAfter (Instant/parse a) (Instant/parse b))))
               (partition 2 1 xs))))
(defn- index-valid? [rows]
  (and (vector? rows) (seq rows)
       (every? #(and (= #{:application/id :feedback/event-id :prior-state/revision
                          :transaction-sha256 :provenance-sha256} (set (keys %)))
                     (every? nonblank? ((juxt :application/id :feedback/event-id
                                               :prior-state/revision) %))
                     (every? (fn [d] (re-matches hex64 (or d "")))
                             ((juxt :transaction-sha256 :provenance-sha256) %))) rows)
       (every? #(= (count rows) (count (distinct (map % rows))))
               [:application/id :feedback/event-id :prior-state/revision
                :transaction-sha256 :provenance-sha256])))

(defn validate
  "Validate externally configured immutable completeness records. A positive
   isolated result proves the join mechanism only; production always refuses."
  [{:keys [mode authority-root candidate/id target-application-id expected-review-origin roles]
    :as config}]
  (when-not (= #{:mode :authority-root :candidate/id :target-application-id
                 :expected-review-origin :roles}
               (set (keys config)))
    (refuse! :e6b-completeness/config-schema-invalid {}))
  (when (= :production mode)
    (refuse! :e6b-completeness/production-authority-unavailable {}))
  (when-not (and (= :isolated-test mode) (nonblank? authority-root)
                 (nonblank? id) (nonblank? target-application-id)
                 (= #{:origin/kind :origin/id :origin/owner
                      :origin/provenance-review-sha256}
                    (set (keys expected-review-origin)))
                 (keyword? (:origin/kind expected-review-origin))
                 (every? nonblank? ((juxt :origin/id :origin/owner)
                                    expected-review-origin))
                 (re-matches hex64 (:origin/provenance-review-sha256
                                     expected-review-origin ""))
                 (= (set required-roles) (set (keys roles))))
    (refuse! :e6b-completeness/config-invalid {}))
  (let [resolved (into {} (map (fn [role] [role (resolve-role role (get roles role))]) required-roles))
        record #(get-in resolved [% :record]) pin #(get-in resolved [% :sha256])
        inventory (exact! :writer-inventory
                          #{:schema :scope :authority/owner :authority/root :store/id
                            :owner/generation :boundary/id :writers :covered-writer-roles :issued-at}
                          (record :writer-inventory))
        census (exact! :complete-census
                       #{:schema :scope :authority/owner :authority/root :store/id
                         :owner/generation :boundary/id :writer-inventory/raw-sha256
                         :capture/raw-sha256 :ledger-source/raw-sha256
                         :target/transition-subject :ordered-universe :acquired-at}
                       (record :complete-census))
        boundary (exact! :acquisition-boundary
                         #{:schema :scope :owner/id :authority/root :candidate/id :store/id
                           :owner/generation :boundary/id :writer-inventory/raw-sha256
                           :complete-census/raw-sha256 :intake/status :lifecycle/status
                           :lease/status :closed-at :capture-started-at :capture-finished-at}
                         (record :acquisition-boundary))
        subject (exact! :completeness-subject
                        #{:schema :scope :capture :ledger-source :row-vector :target
                          :ordered-universe :acquisition}
                        (record :completeness-subject))
        _ (exact! :subject-capture
                  #{:bytes/base64 :raw-sha256 :store/id :owner/generation :head-digest
                    :ordered-chain-digests :ordered-universe} (:capture subject))
        _ (exact! :subject-ledger-source #{:bytes/base64 :raw-sha256} (:ledger-source subject))
        _ (exact! :subject-row-vector #{:bytes/base64 :raw-sha256} (:row-vector subject))
        _ (exact! :subject-target
                  #{:application/id :transition/subject :transaction-sha256 :provenance-sha256}
                  (:target subject))
        _ (exact! :subject-acquisition
                  #{:boundary/id :owner/id :owner/generation :writer-inventory-sha256
                    :complete-census/raw-sha256 :closed-at :capture-finished-at}
                  (:acquisition subject))
        commission (exact! :review-commission
                           #{:schema :scope :authority/root :commission/id :reviewer/id
                             :candidate/id :subject/raw-sha256 :boundary/id :issued-at}
                           (record :review-commission))
        execution (exact! :review-execution
                          #{:schema :scope :authority/root :commission/id :reviewer/id :job/id
                            :trace/id :subject/raw-sha256 :review-artifact/raw-sha256 :status
                            :started-at :finished-at}
                          (record :review-execution))
        review (exact! :review-artifact
                       #{:schema :scope :authority/root :reviewer/id :job/id :trace/id
                         :subject/raw-sha256 :boundary/id :outcome :reviewed-at}
                       (record :review-artifact))
        origin (exact! :review-origin
                       #{:schema :scope :authority/root :origin/kind :origin/id :origin/owner
                         :origin/provenance-review-sha256 :reviewer/id :commission/id
                         :commission/raw-sha256 :subject/raw-sha256 :job/id :trace/id
                         :review-artifact/raw-sha256 :artifact/retained-at :terminal/status
                         :finished-at}
                       (record :review-origin))
        acceptance (exact! :acceptance
                           #{:schema :scope :authority/root :authority/owner :reviewer/id :job/id
                             :trace/id :commission/id :subject/raw-sha256
                             :review-artifact/raw-sha256 :boundary/id :outcome :accepted-at}
                           (record :acceptance))
        capture-pin (select-keys (get roles :capture-artifact)
                                 [:bytes/base64 :expected-sha256])
        validated-capture (capture/readback capture-pin)
        capture-universe (get-in validated-capture [:record :application-universe])
        projected (projection/project
                   {:capture-pin capture-pin
                    :target-application-id target-application-id})
        universe (get-in projected [:application-ledger/records])
        capture-index (get-in projected [:target :index-entry])
        transition-subject (get-in projected [:target :transition/subject])
        subject-sha (pin :completeness-subject)
        reviewer (:reviewer/id commission)]
    (when-not
     (and
      (= [:wm/e6b-writer-inventory-v1 :wm/e6b-complete-census-v1
          :wm/e6b-acquisition-boundary-v1 :wm/e6b-completeness-subject-v1
          :wm/e6b-review-commission-v1 :wm/e6b-review-execution-v1
          :wm/e6b-completeness-review-v1 :wm/e6b-review-origin-v1
          :wm/e6b-completeness-acceptance-v1]
         (mapv :schema [inventory census boundary subject commission execution review origin acceptance]))
      (every? #(= mode (:scope %))
              [inventory census boundary subject commission execution review origin acceptance])
      (every? #(= authority-root (:authority/root %))
              [inventory census boundary commission execution review origin acceptance])
      (every? nonblank? [(:authority/owner inventory) (:authority/owner census)
                         (:owner/id boundary) (:authority/owner acceptance)
                         reviewer (:job/id execution) (:trace/id execution)
                         (:boundary/id boundary) (:commission/id commission)])
      (not= id (:authority/owner inventory)) (not= id reviewer)
      (not= (:owner/id boundary) reviewer)
      (not= id (:authority/owner acceptance))
      (not= reviewer (:authority/owner acceptance))
      (= (:authority/owner inventory) (:authority/owner census) (:owner/id boundary))
      (= (:store/id inventory) (:store/id census) (:store/id boundary)
         (get-in subject [:capture :store/id])
         (get-in projected [:capture :store/id]))
      (= (:owner/generation inventory) (:owner/generation census) (:owner/generation boundary)
         (get-in subject [:capture :owner/generation])
         (get-in projected [:capture :generation]))
      (= (:boundary/id inventory) (:boundary/id census) (:boundary/id boundary)
         (:boundary/id commission) (:boundary/id review) (:boundary/id acceptance))
      (= id (:candidate/id boundary) (:candidate/id commission))
      (= (pin :writer-inventory) (:writer-inventory/raw-sha256 census)
         (:writer-inventory/raw-sha256 boundary))
      (= (pin :complete-census) (:complete-census/raw-sha256 boundary)
         (get-in subject [:acquisition :complete-census/raw-sha256]))
      (= (pin :capture-artifact) (:capture/raw-sha256 census)
         (get-in subject [:capture :raw-sha256]) (get-in projected [:capture :raw-sha256]))
      (= (get-in roles [:capture-artifact :bytes/base64])
         (get-in subject [:capture :bytes/base64]))
      (= (get-in projected [:digest-roles :ledger-source/raw])
         (:ledger-source/raw-sha256 census) (get-in subject [:ledger-source :raw-sha256]))
      (= (get-in projected [:digest-roles :ledger/derived]) (get-in subject [:row-vector :raw-sha256]))
      (= (get-in projected [:application-ledger/source :bytes/base64])
         (get-in subject [:ledger-source :bytes/base64]))
      (= (get-in projected [:application-ledger :bytes/base64])
         (get-in subject [:row-vector :bytes/base64]))
      (= transition-subject (:target/transition-subject census)
         (get-in subject [:target :transition/subject]))
      (= target-application-id (get-in subject [:target :application/id])
         (:application/id capture-index))
      (= (:transaction-sha256 capture-index) (get-in subject [:target :transaction-sha256]))
      (= (:provenance-sha256 capture-index) (get-in subject [:target :provenance-sha256]))
      (index-valid? (:ordered-universe census))
      (= (:ordered-universe census) (:ordered-universe subject)
         (get-in subject [:capture :ordered-universe])
         capture-universe)
      (= (mapv #(select-keys % [:application/id :feedback/event-id :prior-state/revision])
               (:ordered-universe census))
         (mapv #(select-keys % [:application/id :feedback/event-id :prior-state/revision]) universe))
      (= (get-in projected [:capture :head-digest]) (get-in subject [:capture :head-digest]))
      (= (get-in projected [:capture :chain-digests])
         (get-in subject [:capture :ordered-chain-digests]))
      (= :closed (:intake/status boundary)) (= :reconciled (:lifecycle/status boundary))
      (= :held-through-capture (:lease/status boundary))
      (vector? (:writers inventory)) (seq (:writers inventory))
      (every? nonblank? (:writers inventory))
      (= (count (:writers inventory)) (count (distinct (:writers inventory))))
      (= [:store-v2-exclusive-owner] (:covered-writer-roles inventory))
      (= subject-sha (:subject/raw-sha256 commission) (:subject/raw-sha256 execution)
         (:subject/raw-sha256 review) (:subject/raw-sha256 acceptance))
      (= reviewer (:reviewer/id execution) (:reviewer/id review) (:reviewer/id acceptance))
      (= (:commission/id commission) (:commission/id execution) (:commission/id acceptance))
      (= (:job/id execution) (:job/id review) (:job/id acceptance))
      (= (:trace/id execution) (:trace/id review) (:trace/id acceptance))
      (= expected-review-origin
         (select-keys origin [:origin/kind :origin/id :origin/owner
                              :origin/provenance-review-sha256]))
      (= reviewer (:reviewer/id origin))
      (= (:commission/id commission) (:commission/id origin))
      (= (pin :review-commission) (:commission/raw-sha256 origin))
      (= subject-sha (:subject/raw-sha256 origin))
      (= (:job/id execution) (:job/id origin))
      (= (:trace/id execution) (:trace/id origin))
      (= (pin :review-artifact) (:review-artifact/raw-sha256 execution)
         (:review-artifact/raw-sha256 origin) (:review-artifact/raw-sha256 acceptance))
      (= :completed (:status execution) (:terminal/status origin))
      (= (:finished-at execution) (:finished-at origin))
      (= :accepted (:outcome review) (:outcome acceptance))
      (<=time (:issued-at inventory) (:closed-at boundary) (:capture-started-at boundary)
              (:capture-finished-at boundary) (:issued-at commission)
              (:started-at execution) (:reviewed-at review)
              (:artifact/retained-at origin) (:finished-at execution)
              (:accepted-at acceptance))
      (= (:capture-finished-at boundary) (:acquired-at census))
      (= {:boundary/id (:boundary/id boundary)
          :owner/id (:owner/id boundary) :owner/generation (:owner/generation boundary)
          :writer-inventory-sha256 (pin :writer-inventory)
          :complete-census/raw-sha256 (pin :complete-census)
          :closed-at (:closed-at boundary)
          :capture-finished-at (:capture-finished-at boundary)}
         (:acquisition subject)))
      (refuse! :e6b-completeness/authority-join-invalid {}))
    {:schema :wm/e6b-completeness-join-result-v1
     :scope :isolated-test :status :join-mechanism-validated
     :evidence/scope :synthetic-independent-role-fixture
     :authority/status :none :subject/raw-sha256 subject-sha
     :target/transition-subject transition-subject
     :retrospective-success? false :restart-authorized? false}))
