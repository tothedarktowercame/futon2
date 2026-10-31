(ns futon2.aif.wm.delivery-failure-evidence
  "Content-addressed retention of delivery-failure evidence.

  Injury adf04df0 (ordinal 130, click wm-click-eaa9bc3a, 2026-10-31) proved
  the recurrence class this namespace closes: check-delivery consumed the
  author's exact patch descriptor, typed a refusal, and the failure
  publication retained only the classified :failure summary — the exact
  check input (the abbreviated commit descriptor) was left in the volatile
  Agency job ledger (/tmp journal; terminal transcripts compact to
  tombstones after a day). The P4-2 diagnosis (click wm-click-023dde8b)
  therefore refused :cause-scope-unretained: no retained carrier exists.

  Contract, deliberately narrow:

  * ONE append-only store of content-addressed EDN records; a record's
    file name is its own sha256, written with CREATE_NEW, so an identical
    replay verifies existing bytes and a mismatch is a typed error — the
    same discipline as futon2.aif.job-text-retention.
  * Records are written at the delivery-failure boundary only (the
    open-cascade resume branch where check-delivery refused). Success
    paths, reviewer paths and selection paths never write here.
  * Reading never repairs, never substitutes a pin, and never rewrites a
    historical record: a tampered or missing file is a typed refusal.
  * This namespace does NOT extend cause-entry-defect. Whether a retained
    supplemental record may serve as a diagnosis input carrier is an
    explicit append-only contract proposal (supplemental-carrier-proposal)
    that a human review admits or rejects; nothing here changes the
    existing rule that input.record-pin must equal the source-record pin."
  (:require [clojure.edn :as edn]
            [clojure.walk :as walk]
            [clojure.java.io :as io]
            [futon2.aif.load-identity :as load-identity])
  (:import [java.nio.charset StandardCharsets]
           [java.nio.file Files StandardOpenOption FileAlreadyExistsException]
           [java.time Instant]))

(load-identity/register! *ns* *file*)

(def schema-version :wm/delivery-failure-evidence-v1)

(def default-store-root
  "Canonical content-addressed store for retained delivery failures."
  "/home/joe/code/futon2/data/wm-delivery-failure-evidence")

(defn- record-bytes [record]
  (.getBytes ^String (pr-str record) StandardCharsets/UTF_8))

(defn- store-file [root sha]
  (io/file root (str sha ".edn")))

(defn- record-address
  "Content address of a record: sha256 over its printed form without the
  volatile :retained-at timestamp, so re-retaining the same evidence at a
  later moment is idempotent at one address instead of minting files per
  second. Every other byte participates."
  [record]
  (load-identity/sha256 (record-bytes (dissoc record :retained-at))))

(defn retain-record!
  "Append one immutable, content-addressed record. Returns the pin
  {:path :sha256 :schema}. An existing identical record is idempotent;
  a byte mismatch at the same address refuses :content-mismatch. Record
  must be a map; retention never overwrites."
  [root record]
  (when-not (map? record)
    (throw (ex-info "delivery-failure evidence records must be maps"
                    {:failure-kind :delivery-evidence-record-not-a-map})))
  (let [sha (record-address record)
        file (store-file root sha)
        path (.toPath file)]
    (io/make-parents file)
    (if (.isFile file)
      (let [existing (try (edn/read-string (slurp file))
                          (catch Exception _
                            (throw (ex-info "retained delivery-failure evidence unreadable"
                                            {:failure-kind :delivery-evidence-unreadable
                                             :path (str file)}))))]
        (when (not= sha (record-address existing))
          (throw (ex-info "retained delivery-failure evidence does not match its content address"
                          {:failure-kind :delivery-evidence-content-mismatch
                           :path (str file)}))))
      (try
        (Files/write path (record-bytes record)
                     (into-array StandardOpenOption
                                 [StandardOpenOption/CREATE_NEW
                                  StandardOpenOption/WRITE]))
        (catch FileAlreadyExistsException _
          (throw (ex-info "retained delivery-failure evidence address raced"
                          {:failure-kind :delivery-evidence-content-mismatch
                           :path (str file)})))))
    {:status :retained :path (.getAbsolutePath file) :sha256 sha
     :schema schema-version}))

(defn read-pin
  "Read and verify one pinned record. Missing file, unreadable EDN, wrong
  schema and sha mismatch are typed refusals; nothing is ever repaired."
  [{:keys [path sha256]}]
  (let [file (io/file path)]
    (cond
      (not (and (string? path) (string? sha256) (seq path)
                (re-matches #"[0-9a-f]{64}" sha256)))
      {:status :refused :reason :delivery-evidence-pin-malformed}
      (not (.isFile file))
      {:status :refused :reason :delivery-evidence-missing :path path}
      :else
      (let [record (try (edn/read-string
                         {:default (fn [_ v] v)}
                         (String. (Files/readAllBytes (.toPath file))
                                  StandardCharsets/UTF_8))
                        (catch Exception _
                          ::unreadable))]
        (cond
          (= ::unreadable record)
          {:status :refused :reason :delivery-evidence-unreadable :path path}
          (not= schema-version (:schema record))
          {:status :refused :reason :delivery-evidence-schema-unknown
           :path path :schema (:schema record)}
          (not= sha256 (record-address record))
          {:status :refused :reason :delivery-evidence-tampered
           :path path :sha256-expected sha256
           :sha256-actual (record-address record)}
          :else {:status :present :path path :sha256 sha256 :record record})))))

(defn- printable
  "Postwalk any value into one whose pr-str reads back with clojure.edn
  (check-delivery results carry java.io.File objects, whose #object
  printing is unreadable and broke content addressing)."
  [x]
  (walk/postwalk #(if (instance? java.io.File %) (str %) %) x))

(defn delivery-failure-record
  "Build (but do not write) the retained record for one delivery failure.
  INPUT is the author's exact answer as consumed by check-delivery;
  CHECK is check-delivery's exact result map; LINKAGE carries the
  response/job identity and requisition identity that bind the evidence
  to its run and injury. No field is invented: absent values stay absent."
  [{:keys [requisition-id cascade-digest author-job answer check]}]
  {:schema schema-version
   :retained-at (str (Instant/now))
   :linkage {:requisition-id requisition-id
             :cascade-digest cascade-digest
             :author-job author-job}
   ;; The exact check input, verbatim. check-delivery reads (:patch answer);
   ;; the descriptor object is retained whole, including a malformed commit.
   :check-input {:requisition-id (:requisition-id answer)
                 :cascade-digest (:cascade-digest answer)
                 :patch (get answer :patch)}
   ;; The exact typed result, with File objects stringified so the record
   ;; stays readable and content-addressable.
   :typed-result (printable check)})

(defn retain-delivery-failure!
  "Retain one delivery failure at the boundary that produced it. Never
  throws into the caller's failure path: retention trouble is reported in
  the returned {:delivery-evidence ...} map, typed, and the caller's own
  result is untouched."
  [root {:keys [requisition-id cascade-digest author-job answer check]}]
  (if (nil? root)
    {:status :disabled :reason :no-store-root}
    (try
      (let [record (delivery-failure-record {:requisition-id requisition-id
                                             :cascade-digest cascade-digest
                                             :author-job author-job
                                             :answer answer
                                             :check check})]
        {:status :retained
         :pin (dissoc (retain-record! root record) :status)})
      (catch Exception e
        {:status :refused
         :reason (or (:failure-kind (ex-data e)) :delivery-evidence-retention-failed)
         :error (ex-message e)}))))

(defn evidence-readiness
  "Read-only witness for one injury's diagnosis evidence, BEFORE any paid
  recovery opportunity. Distinguishes:

    :callable-ports     vars/pins that resolve and can be exercised now
    :retained-input     verified retained evidence usable as check input
    :missing            named routes whose evidence is absent or corrupt

  Never charges, never contacts a producer, never mutates. IN-RECORD-FAILURE
  is the injury record's :failure value; SOURCE-PIN the pinned cause record;
  CANDIDATE-PINS are retained-evidence pins (any subset) to verify. A
  JOB-LOOKUP fn, when supplied, is asked (fn [job-id]) -> {:status :present
  :reply-sha256 ...} so the volatile job-ledger route can be reported as
  present-or-compacted without this namespace reading Agency state."
  [{:keys [source-pin candidate-pins job-lookup requisition-id cascade-digest]}]
  (let [source (when source-pin
                 (let [f (io/file (:path source-pin))]
                   (if (.isFile f)
                     (let [sha (load-identity/sha256 (Files/readAllBytes (.toPath f)))]
                       (if (= (:sha256 source-pin) sha)
                         {:status :present :sha256 sha}
                         {:status :corrupt :sha256-expected (:sha256 source-pin)
                          :sha256-actual sha}))
                     {:status :missing})))
        pins (mapv read-pin (or candidate-pins []))
        job (when (ifn? job-lookup)
              (try (job-lookup)
                   (catch Exception e {:status :refused :error (ex-message e)})))]
    {:schema :wm/evidence-readiness-v1
     :source-record source
     :retained-input (mapv (fn [p] (if (= :present (:status p))
                                     {:status :usable
                                      :pin (select-keys p [:path :sha256])}
                                     {:status (:status p) :reason (:reason p)}))
                           pins)
     :job-ledger-route (when job (select-keys job [:status]))
     ;; Explicit, honest accounting of what this witness can NOT promise:
     ;; diagnosis itself still needs a callable boundary and an admitted
     ;; carrier contract; readiness does not imply a free or successful
     ;; diagnosis.
     :limits (cond-> [:readiness-is-not-diagnosis
                     :carrier-admission-requires-review]
              (and requisition-id cascade-digest
                   (not (some #(= :present (:status %)) pins)))
              (conj :no-retained-supplemental-input))}))

(defn supplemental-carrier-proposal
  "EXPLICIT append-only contract proposal — not a bypass. Builds the record
  a review would admit if a retained delivery-failure record is to become an
  admissible diagnosis input carrier. Nothing here changes
  cause-entry-defect, which today requires input.record-pin to equal the
  source-record pin; admitting this proposal is a reviewed contract change
  (e.g. a new typed :cause-scope/input-supplemental-record defect arm), not
  a silent substitution."
  [{:keys [injury-id source-record-pin retained-pin boundary-var boundary-file]}]
  {:schema :wm/supplemental-carrier-proposal-v1
   :injury-id injury-id
   :existing-contract :cause-scope/input-pin-not-source-record
   :proposal {:cause-entry/input
              {:record-pin source-record-pin
               :supplemental-record retained-pin
               :kind :wm/delivery-failure-evidence-v1}
              :boundary {:var boundary-var :file boundary-file}
              :admission-rule [:append-only :review-required
                               :no-substitution-of-source-pin]
              :explicitly-not [:contract-change :automatic-admission
                               :healthy-predicate-fabrication]}})
