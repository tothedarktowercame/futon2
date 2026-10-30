(ns futon2.aif.standing-retained-authority-test
  "Witness and control tests for the standing retained medic authority.

  Background (2026-10-31): full_loop_runtime's default authority branch wired
  the raw out-of-band genuine-repair verifier, so retained receipts of the
  cause-reattribution (f04177cd9) and ordinary-author-refusal (1ce1efbea)
  grammars — appended by this runtime itself at ordinals 118 and 125 — were
  rejected by every subsequent ledger load (:medic/authority-rejected). The
  repair composes the existing per-grammar union
  (meta-medic-authority/with-retained-authorities, 5a1de6f3b) instead of
  duplicating any grammar. These tests pin:

  1. the default resolution now exposes the union (behaviour, not identity);
  2. ordinal 118's exact entry admits under the union from a hermetic,
     re-pinned receipt, and NOT under the raw genuine-repair verifier;
  3. tamper/swap/unsupported-grammar controls refuse without any writes;
  4. a read-only live-ledger witness over the exact pre-click (c29c46b3) and
     post-click (f7b5a7aa) ledger bytes: under the union the failing
     medic-repaired ordinals reduce from 13 to exactly the 11 bundle-grammar
     entries (38-44, 46, 48, 50, 71) that no entry-local retained authority
     covers — the recorded STOP blocker. Live bytes are hash-guarded before
     and after; the test skips when the pinned evidence files are absent."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.cause-reattribution :as cause]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-injury-ledger :as ledger]
            [futon2.aif.meta-injury-store :as store]
            [futon2.aif.meta-medic-authority :as authority]
            [futon2.aif.out-of-band-fix :as out-of-band]))

(def fixture-root "test/resources/wm/standing-retained-authority")
(def live-post-click "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22/ledger.edn")
(def live-pre-click "/home/joe/notes/wm-next/criteria-completeness/run001/renewed-click-01b/copy/ledger/ledger.edn")
(def live-post-click-sha "f7b5a7aac642b35302f30e1254c707805e95188ffd3c8158c51ae6bde56a0074")
(def live-pre-click-sha "c29c46b3de676773441b5e0edc618255d79920ecff156bd0b644e7536cc82384")

(def union
  "The exact standing authority composed by the repaired default branch."
  (authority/with-retained-authorities (fn [_] false)))

(def raw-genuine-repair out-of-band/retained-checked-medic-authority)

(defn- temp-root []
  (.toFile (java.nio.file.Files/createTempDirectory
            "standing-retained-authority-"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- pin [path]
  {:path path
   :sha256 (identity/sha256
            (java.nio.file.Files/readAllBytes (.toPath (io/file path))))})

(defn- file-sha [path]
  (identity/sha256 (java.nio.file.Files/readAllBytes (.toPath (io/file path)))))

(defn- exact-file [src-url dest-name]
  (let [dest (io/file (temp-root) dest-name)]
    (java.nio.file.Files/copy (.toPath (io/file src-url)) (.toPath dest)
                              (into-array java.nio.file.CopyOption
                                          [java.nio.file.StandardCopyOption/REPLACE_EXISTING]))
    {:file dest :pin (pin (str dest))}))

;; ---------------------------------------------------------------------------
;; Hermetic ordinal-118 fixture: exact ledger entry + exact receipt bytes,
;; receipt re-pinned into the temp root with the operation identity recomputed
;; the same way the append did (cause.checked-operation).
;; ---------------------------------------------------------------------------

(defn- entry-118 []
  (edn/read-string (slurp (str fixture-root "/entry-118-f7b5a7aa.edn"))))

(def ^:private operation-0 (:medic-operation (entry-118)))

(defn- rehomed-118 [receipt-pin]
  (let [entry (entry-118)
        operation (-> operation-0
                      (assoc :authority-receipt-pin receipt-pin)
                      (assoc :operation-id
                             (action-identity/digest
                              [:cause-reattribution
                               {:ledger-pin (:expected-ledger-pin operation-0)
                                :request (:request operation-0)
                                :authority-receipt-pin receipt-pin}])))]
    {:entry (ledger/seal-entry (assoc entry :medic-operation operation))
     :operation operation}))

(defn- context-118 [receipt-pin]
  (let [{:keys [operation]} (rehomed-118 receipt-pin)
        candidate (get-in operation [:request :candidate])]
    {:ledger {:ledger-pin (:expected-ledger-pin operation)}
     :operation operation
     :current {:injury-id (:injury-id candidate) :entry-id (:entry-id candidate)}
     :candidate candidate
     :evidence candidate}))

(deftest default-resolution-exposes-the-retained-union
  (let [resolve-fn (var-get #'runtime/resolve-medic-authority)
        resolved (resolve-fn {})
        probe (context-118 (:authority-receipt-pin operation-0))]
    ;; Since the standing recovered authority landed (operator-authorized
    ;; 2026-10-31) the default resolution is :ready with the standing context;
    ;; before it, and whenever the standing policy refuses, it degrades
    ;; fail-closed to :absent with the retained union only.
    (is (contains? #{:ready :absent} (:status resolved)))
    (is (ifn? (:verifier resolved)))
    ;; Behavioural identity with the union: same admission, same refusals.
    (is (= (boolean (union probe)) (boolean ((:verifier resolved) probe))))
    (is (true? ((:verifier resolved) probe)))))

(deftest ordinal-118-admits-under-union-and-not-under-raw-genuine-repair
  (let [{:keys [pin file]} (exact-file (io/file fixture-root "receipt-118-a184c0ca.edn")
                                       "receipt-118.edn")
        before (file-sha (str file))
        ctx (context-118 pin)]
    (testing "the cause-reattribution grammar admits its own re-derived receipt"
      (is (true? (cause/retained-authority ctx))))
    (testing "the composed union admits it; the raw genuine-repair verifier does not"
      (is (true? (union ctx)))
      (is (false? (raw-genuine-repair ctx))))
    (testing "verifiers never write: receipt bytes unchanged"
      (is (= before (file-sha (str file)))))))

(deftest ordinal-118-controls-refuse-fail-closed
  (let [{:keys [pin file]} (exact-file (io/file fixture-root "receipt-118-a184c0ca.edn")
                                       "receipt-118.edn")
        good (context-118 pin)]
    (testing "wrong injury on current: candidate no longer binds the occurrence"
      (is (false? (union (assoc-in good [:current :injury-id] "injury/other")))))
    (testing "candidate/evidence swap refused"
      (is (false? (union (assoc good :evidence (assoc (:candidate good) :kind :genuine-repair))))))
    (testing "unsupported grammar receipt refused"
      (let [bogus (io/file (.getParentFile file) "bogus.edn")]
        (spit bogus (pr-str {:schema :wm/some-other-receipt-v1 :status :checked}))
        (is (false? (union (context-118 (pin (str bogus))))))))
    (testing "altered receipt bytes with stale pin sha refused"
      (let [tampered (io/file (.getParentFile file) "tampered.edn")
            text (slurp file)]
        (spit tampered (str/replace-first text ":checked" ":unchecked"))
        (is (false? (union (context-118 (assoc (pin (str tampered)) :sha256 (:sha256 pin))))))))
    (testing "missing receipt file refused"
      (is (false? (union (context-118 {:path "/nonexistent/receipt.edn" :sha256 "0"})))))))

;; ---------------------------------------------------------------------------
;; Read-only live-ledger witness. Skips (rather than fails) when the pinned
;; evidence is absent so hermetic CI environments stay green; when present it
;; asserts the exact typed outcome of the STOP blocker.
;; ---------------------------------------------------------------------------

(defn- live-ledger-value [path expected-sha]
  (when (.isFile (io/file path))
    (let [bytes (java.nio.file.Files/readAllBytes (.toPath (io/file path)))]
      (when (= expected-sha (identity/sha256 bytes))
        (:value (store/read-one-form (String. bytes "UTF-8")))))))

(defn- localize [parsed verifier]
  (loop [prefix (ledger/seal-ledger (assoc (select-keys parsed [:schema :attempt-limit :retry-limit])
                                           :entries []))
         rem (:entries parsed)
         bad []]
    (if-let [e (first rem)]
      (let [obs (= :wm/meta-injury-observation-v1 (:schema e))
            d (if obs
                (try ((var-get (resolve 'futon2.aif.meta-injury-ledger/observation-defects))
                      prefix e verifier)
                     (catch Throwable t [:threw (ex-message t)]))
                [])
            p2 (ledger/seal-ledger (update prefix :entries conj e))]
        (recur p2 (rest rem)
               (if (and (vector? d) (seq d)) (conj bad (:ordinal e)) bad)))
      bad)))

(deftest live-ledger-witness-exact-remaining-blocker
  (let [post (live-ledger-value live-post-click live-post-click-sha)
        pre (live-ledger-value live-pre-click live-pre-click-sha)]
    (if-not (and post pre)
      (is true "pinned live evidence absent in this environment; witness skipped")
      (do
        (testing "post-click ledger: union reduces the failing medic ordinals to the 11 bundle-grammar entries"
          (is (= [38 39 40 41 42 43 44 46 48 50 71] (localize post union)))
          (is (= [38 39 40 41 42 43 44 46 48 50 71 116 118 120 122 125]
                 (localize post (fn [_] false)))
              "deny still fails all 16; the fix grants nothing to the deny path"))
        (testing "pre-click ledger shows the identical remaining blocker"
          (is (= [38 39 40 41 42 43 44 46 48 50 71] (localize pre union))))
        (testing "explicit union/deny verifiers still refuse the pinned pre-standing ledger with the exact typed defect"
          (let [refusals [(store/load-durable-ledger live-post-click union)
                          (store/load-ledger live-post-click union)
                          (store/load-durable-ledger live-post-click (fn [_] false))
                          (store/load-ledger live-post-click (fn [_] false))]]
            (is (every? #(= [:refused :store/tampered-ledger]
                            ((juxt :status :reason) %)) refusals))
            (is (every? #(= [:entry/transition-invalid] (:defects %)) refusals))))
        (testing "default (no-opts) loads now resolve through the standing resolver (P1) and load this ledger"
          (let [defaults [(store/load-durable-ledger live-post-click)
                          (store/load-ledger live-post-click)]]
            (is (every? #(= :loaded (:status %)) defaults))
            (is (every? #(contains? % :authority-identity) defaults)
                "each outcome names the resolving authority (P4)")))
        (testing "live ledger bytes untouched by the witness"
          (is (= live-post-click-sha (file-sha live-post-click))))))))
