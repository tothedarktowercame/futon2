(ns futon2.aif.occurrence-declared-effect-binding
  "Pure construction and verification of the occurrence -> declared-effect
   binding receipt v1 (contract: declared-effect-binding-contract-review.json,
   claude-2 2026-10-22). The occurrence (close-retention v2) is NOT migrated;
   this is a separate versioned receipt joining a valid occurrence, the
   selected mission, and the authoritative eligible declared-effect record
   frozen in the selection cell, with the record's source pin and base
   revision copied in.

   Record authority is INJECTED, never defaulted: build and verify both
   require a record-verification produced by the accepted (corrected) C2
   producer's verify-record. The rejected f90c614fd verify-record
   (digest-only, schema-blind — see declared-effect-record-independent-
   review.json) is neither called nor duplicated here; the binding checks the
   verification's digest against the record and refuses without it. No
   status-only trust: an :eligible record without a matching verification
   does not bind.

   Ordering note (NOT an atomicity claim): mint-then-bind is two steps, not
   an atomic if-and-only-if. Eventual wiring must fail the attempt
   (:declared-effect-binding-failed) and refuse dispatch when binding fails
   or is missing for an ordinary route; this namespace only builds and
   verifies the receipt. Clock/time values (:selection/recorded-at,
   :bound-at) are supplied explicitly by the caller; nothing here reads a
   clock. The action value, action digest and selection/scoring certificate
   are not read beyond :target and are never altered."
  (:require [clojure.string :as str]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.close-retention :as retention]))

(load-identity/register! *ns* *file*)

(def binding-schema :wm/occurrence-declared-effect-binding-v1)
(def occurrence-digest-tag :wm/action-transition-occurrence-digest-v1)
(def binding-digest-tag :wm/occurrence-declared-effect-binding-digest-v1)
(def record-schema :wm/selected-effect-declaration-v1)
(def verification-schema :wm/occurrence-declared-effect-binding-verification-v1)
(def binding-keys
  #{:schema :occurrence/digest :occurrence/key :target :declared-effect/digest
    :declared-effect/source :base-revision :selection/recorded-at :bound-at
    :binding/digest})

(defn- refuse! [code path & [data]]
  (throw (ex-info "Declared-effect binding refused"
                  (merge {:declared-effect-binding/refusal code :path path} data))))

(defn- exact-map! [x ks path]
  (when-not (and (map? x) (= ks (set (keys x))))
    (refuse! :shape-invalid path {:expected ks :actual (some-> x keys set)}))
  x)

(defn- sha-text! [x path]
  (when-not (and (string? x) (re-matches #"[0-9a-f]{64}" x))
    (refuse! :digest-invalid path))
  x)

(defn- rev-text!
  "A git object name (40 hex) or an extended sha256 (64 hex)."
  [x path]
  (when-not (and (string? x) (re-matches #"[0-9a-f]{40}|[0-9a-f]{64}" x))
    (refuse! :revision-invalid path))
  x)

(defn- text! [x path]
  (when-not (and (string? x) (not (str/blank? x)))
    (refuse! :identity-invalid path))
  x)

(defn- instant! [x path]
  (text! x path)
  (try (java.time.Instant/parse x)
       (catch Throwable _ (refuse! :timestamp-invalid path)))
  x)

(defn occurrence-digest
  "Domain-separated sha256 over the canonical occurrence bytes under the
   occurrence-digest tag. NOT action-identity/digest: that tags the bytes as
   an action identity; this tags them as an occurrence being bound."
  [occurrence]
  (identity/sha256
   (identity/printed false [occurrence-digest-tag (identity/canonical occurrence)])))

(defn- body-digest
  "sha256 over the canonical binding BODY (every key except :binding/digest)
   under the binding-digest tag."
  [binding]
  (identity/sha256
   (identity/printed false
                     [binding-digest-tag
                      (identity/canonical (dissoc binding :binding/digest))])))

(defn binding-digest
  "Re-derive the binding's self-digest from its body."
  [binding]
  (sha-text! (:binding/digest binding) [:binding/digest])
  (body-digest binding))

(defn occurrence-key
  "The learning-dedup triple, selected out of the occurrence
   (attempt_learning.clj / learning_trial_ledger.clj key)."
  [occurrence]
  (select-keys occurrence [:action/id :action/value-sha256 :transition/id]))

(defn- record!
  "Structural check of the record the selection cell froze. Semantics
   (observations, admission computation, digest authority) belong to the
   producer; here only the frozen shape the binding joins to."
  [record]
  (when-not (and (map? record) (contains? record :record/digest))
    (refuse! :record-shape-invalid [:record]))
  (when-not (= record-schema (:schema record))
    (refuse! :record-schema-mismatch [:record :schema]))
  (sha-text! (:record/digest record) [:record :record/digest])
  (let [{:keys [declaration base-revision admission]} record]
    (when-not (and (map? declaration) (= :pinned (:status declaration)))
      (refuse! :record-declaration-not-pinned [:record :declaration]))
    (exact-map! declaration #{:status :path :sha256} [:record :declaration])
    (text! (:path declaration) [:record :declaration :path])
    (sha-text! (:sha256 declaration) [:record :declaration :sha256])
    (when-not (and (map? base-revision)
                   (= #{:repo :sha :resolved-sha} (set (keys base-revision))))
      (refuse! :record-base-revision-shape [:record :base-revision]))
    (doseq [k [:sha :resolved-sha]]
      (rev-text! (get base-revision k) [:record :base-revision k]))
    (text! (:repo base-revision) [:record :base-revision :repo])
    (when-not (and (map? admission) (boolean? (:eligible admission)))
      (refuse! :record-admission-shape [:record :admission]))
    record))

(defn- record-verification!
  "The injected authority result: {:status :verified :record/digest sha} from
   the accepted producer's verify-record, or its typed refusal. The binding
   never computes record-digest authority itself; a refusal or a digest
   mismatch fails the join — :admission :eligible true is not trusted
   without it."
  [record record-verification]
  (when-not (and (map? record-verification) (contains? record-verification :status))
    (refuse! :record-verification-missing [:record-verification]))
  (when-not (= :verified (:status record-verification))
    (refuse! :record-verification-refused [:record-verification]
             {:verification record-verification}))
  (when-not (= (:record/digest record)
               (sha-text! (:record/digest record-verification)
                          [:record-verification :record/digest]))
    (refuse! :record-verification-digest-mismatch [:record-verification]))
  record-verification)

(defn build-binding
  "Build the v1 binding from an exact input map
   {:occurrence :selected-mission :record :record-verification
    :recorded-at :bound-at}. OCCURRENCE must be a validated close-retention
   v2 occurrence; RECORD the record frozen in the selection cell;
   RECORD-VERIFICATION the accepted producer's verify-record result;
   RECORDED-AT (selection checkpoint time) and BOUND-AT supplied explicitly.
   Throws typed refusals; returns the exact-ten-key binding receipt with its
   domain-separated self-digest."
  [{:keys [occurrence selected-mission record record-verification
           recorded-at bound-at] :as inputs}]
  (exact-map! inputs
              #{:occurrence :selected-mission :record :record-verification
                :recorded-at :bound-at}
              [:binding-input])
  (when-not (= retention/occurrence-schema (:schema occurrence))
    (refuse! :occurrence-v2-required [:occurrence :schema]))
  (retention/validate-occurrence occurrence)
  (record! record)
  (record-verification! record record-verification)
  (when-not (true? (get-in record [:admission :eligible]))
    (refuse! :record-not-eligible [:record :admission :eligible]
             {:refusals (get-in record [:admission :refusals])}))
  (instant! recorded-at [:selection/recorded-at])
  (instant! bound-at [:bound-at])
  (let [target (get-in occurrence [:action/value :target])]
    (when-not (and (= target selected-mission) (= target (:target record)))
      (refuse! :target-mismatch [:target]
               {:occurrence target :selected-mission selected-mission
                :record (:target record)}))
    (let [body {:schema binding-schema
                :occurrence/digest (occurrence-digest occurrence)
                :occurrence/key (occurrence-key occurrence)
                :target target
                :declared-effect/digest (:record/digest record)
                :declared-effect/source (select-keys (:declaration record)
                                                     [:path :sha256])
                :base-revision (select-keys (:base-revision record)
                                            [:repo :sha :resolved-sha])
                :selection/recorded-at recorded-at
                :bound-at bound-at}
          binding (assoc body :binding/digest (body-digest body))]
      (exact-map! binding binding-keys [:binding])
      binding)))

(defn- first-refusal
  "Run steps left-to-right; the first non-nil typed refusal wins."
  [steps]
  (reduce (fn [_ step] (if-let [r (step)] (reduced r) nil)) nil steps))

(defn verify-binding
  "The one shared verifier every eventual site (dispatch, build, close,
   falsifier) calls; none re-implements a partial check. Re-derives the
   occurrence digest and the binding digest, re-checks the injected record
   verification's digest against the record, and checks every equality of
   the binding invariant (occurrence digest, key triple, target across
   occurrence/binding/record, declared-effect digest, source pin, base
   revision, binding self-digest, timestamps). Returns {:status :verified
   …} or a typed {:status :refused :reason …}; never a default, never a
   throw."
  [{:keys [occurrence binding record record-verification] :as inputs}]
  (let [refused (fn [reason path & [data]]
                  (cond-> {:status :refused :reason reason :path path
                           :schema verification-schema}
                    data (assoc :details data)))]
    (or (first-refusal
         [(fn [] (when-not (map? inputs)
                   (refused :verify-input-invalid [:verify-input])))
          (fn [] (when-not (= #{:occurrence :binding :record :record-verification}
                              (set (keys inputs)))
                   (refused :verify-input-invalid [:verify-input]
                            {:expected #{:occurrence :binding :record
                                         :record-verification}})))
          (fn [] (when-not (map? binding) (refused :binding-not-a-map [:binding])))
          (fn [] (when-not (= binding-keys (set (keys binding)))
                   (refused :binding-shape-invalid [:binding]
                            {:expected binding-keys :actual (set (keys binding))})))
          (fn [] (when-not (= binding-schema (:schema binding))
                   (refused :binding-schema-mismatch [:binding :schema])))
          (fn [] (when-not (= retention/occurrence-schema (:schema occurrence))
                   (refused :occurrence-v2-required [:occurrence :schema])))
          (fn [] (try (retention/validate-occurrence occurrence) nil
                      (catch clojure.lang.ExceptionInfo e
                        (refused :occurrence-invalid [:occurrence]
                                 {:refusal (ex-data e)}))))
          (fn [] (try (record! record) nil
                      (catch clojure.lang.ExceptionInfo e
                        (refused :record-invalid [:record]
                                 {:refusal (ex-data e)}))))
          (fn [] (try (record-verification! record record-verification) nil
                      (catch clojure.lang.ExceptionInfo e
                        (refused :record-verification-refused [:record-verification]
                                 {:refusal (ex-data e)}))))
          (fn [] (when-not (true? (get-in record [:admission :eligible]))
                   (refused :record-not-eligible [:record :admission :eligible]
                            {:refusals (get-in record [:admission :refusals])})))
          ;; self-integrity before field equality: a tampered binding is
          ;; rejected as tampered before its fields are compared
          (fn [] (try (binding-digest binding) nil
                      (catch clojure.lang.ExceptionInfo e
                        (refused :binding-digest-invalid [:binding :binding/digest]
                                 {:refusal (ex-data e)}))))
          (fn [] (when-not (= (:binding/digest binding) (binding-digest binding))
                   (refused :binding-digest-mismatch [:binding :binding/digest])))
          (fn [] (when-not (= (:occurrence/digest binding)
                              (occurrence-digest occurrence))
                   (refused :occurrence-digest-mismatch
                            [:binding :occurrence/digest])))
          (fn [] (when-not (= (:occurrence/key binding) (occurrence-key occurrence))
                   (refused :occurrence-key-mismatch [:binding :occurrence/key])))
          (fn [] (let [target (get-in occurrence [:action/value :target])]
                   (when-not (and (= target (:target binding))
                                  (= target (:target record)))
                     (refused :target-mismatch [:binding :target]
                              {:occurrence target :binding (:target binding)
                               :record (:target record)}))))
          (fn [] (when-not (= (:declared-effect/digest binding)
                              (:record/digest record))
                   (refused :record-digest-mismatch
                            [:binding :declared-effect/digest])))
          (fn [] (when-not (= (:declared-effect/source binding)
                              (select-keys (:declaration record) [:path :sha256]))
                   (refused :declaration-source-mismatch
                            [:binding :declared-effect/source])))
          (fn [] (when-not (= (:base-revision binding)
                              (select-keys (:base-revision record)
                                           [:repo :sha :resolved-sha]))
                   (refused :base-revision-mismatch [:binding :base-revision])))
          (fn [] (try (instant! (:selection/recorded-at binding)
                                [:binding :selection/recorded-at])
                      (instant! (:bound-at binding) [:binding :bound-at])
                      nil
                      (catch clojure.lang.ExceptionInfo e
                        (refused :timestamp-invalid [:binding]
                                 {:refusal (ex-data e)}))))])
        {:status :verified
         :schema verification-schema
         :occurrence/digest (:occurrence/digest binding)
         :declared-effect/digest (:declared-effect/digest binding)
         :binding/digest (:binding/digest binding)})))
