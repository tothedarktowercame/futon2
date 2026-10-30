(ns futon2.aif.standing-authority
  "Standing recovered medic authority: persistent, data-driven serving use of
  the exact prior operator-approved adoption bundles, recorded verbatim in the
  provenance-pinned policy resource wm/standing-authority/policy.edn with a
  sha-addressed retained evidence store under wm/standing-authority/evidence/.

  Bounds, deliberately:
  - No new approvals and no blanket identity trust. Trust is the retained
    per-bundle manifest: exactly three pinned tuples per bundle (evaluation,
    review, authorization), matched by full tuple equality against the pins the
    ledger entries themselves carry.
  - Every artifact byte resolves from the evidence store BY SHA256. Original
    pin paths inside intents/tuples are inert provenance required for equality
    against ledger entries; no resolution step reads a mutable source path, and
    nothing here overrides paths generically.
  - Composition goes through meta-medic-authority/resolve-context: an
    observation is accepted only when exactly one bundle verifies it
    (overlapping scopes refuse as ambiguous), re-deriving the full
    intent/operation/injury/occurrence/prefix/evidence bindings through the
    ORIGINAL verifiers (verify-historical / verify-genuine).
  - Failure is typed and fail-closed: missing/corrupt policy, missing or
    hash-drifted evidence, malformed bundles and ambiguous scopes return
    {:status :refused :reason ...} and the resulting verifier accepts nothing."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.load-identity :as li]
            [futon2.aif.meta-medic-authority :as medic-authority]))

(def ^:private policy-resource "wm/standing-authority/policy.edn")
(def ^:private evidence-dir "wm/standing-authority/evidence/")
(def ^:private policy-schema :wm/standing-recovered-medic-authority-policy-v1)

(defn- read-resource [rel]
  (when-let [r (io/resource rel)]
    (slurp r)))

(defn- refused [reason detail]
  {:status :refused :reason reason :detail detail})

(defn- sha256-of [^String s]
  (li/sha256 (.getBytes s "UTF-8")))

(defn- evidence-bytes
  "Resolve one evidence entry BY SHA256 from the retained store. Typed refusal
  on a missing file or a byte hash that no longer matches the recorded pin."
  [{:keys [sha256 path]}]
  (let [rel (str evidence-dir sha256)
        text (read-resource rel)
        digest (when text (sha256-of text))]
    (cond
      (nil? text) (refused :evidence/missing {:sha256 sha256 :pin-path path})
      (not= sha256 digest)
      (refused :evidence/hash-drift {:sha256 sha256 :pin-path path
                                     :actual digest})
      :else {:status :resolved :bytes text})))

(defn- bundle-from-entry
  [{:keys [intent evaluation-pin review-pin authorization-pin trusted-tuples evidence]}]
  (let [resolved (mapv evidence-bytes evidence)]
    (if-let [bad (some #(when (not= :resolved (:status %)) %) resolved)]
      bad
      (let [artifacts (into {}
                            (mapv (fn [e r] [(:path e) (:bytes r)])
                                  evidence resolved))]
        {:status :resolved
         :bundle {:intent intent :artifacts artifacts
                  :evaluation-pin evaluation-pin
                  :review-pin review-pin
                  :authorization-pin authorization-pin
                  :trust (let [tuple-set (set trusted-tuples)]
                           (fn [tuple] (contains? tuple-set tuple)))}}))))

(defn- refused? [x] (and (map? x) (= :refused (:status x))))

(defn- load-context []
  (let [text (read-resource policy-resource)]
    (cond
      (nil? text) (refused :policy/missing {:resource policy-resource})
      :else
      (let [policy (try (edn/read-string text)
                        (catch Throwable t
                          (refused :policy/malformed {:error (ex-message t)})))]
        (if (or (not (map? policy)) (refused? policy))
          (if (map? policy) (refused :policy/malformed {:schema (:schema policy)}) policy)
          (if (not= policy-schema (:schema policy))
            (refused :policy/schema {:expected policy-schema :actual (:schema policy)})
            (if-not (vector? (:bundles policy))
              (refused :policy/malformed {:missing :bundles})
              (let [bundles (mapv bundle-from-entry (:bundles policy))]
                (if-let [bad (some #(when (not= :resolved (:status %)) %) bundles)]
                  bad
                  (let [context (medic-authority/resolve-context
                                 {:bundles (mapv :bundle bundles)})]
                    (if (= :ready (:status context))
                      {:status :ready :identity (:identity context)
                       :resolution (:resolution context)}
                      context)))))))))))


(defonce ^:private !context (atom nil))

(defn standing-context
  "Resolve (and memoize) the standing recovered authority context from the
  policy resource and retained evidence store. Fresh on first use in any JVM;
  deterministic and cwd/JVM-history independent because every input is a
  classpath resource with pinned hashes."
  []
  (swap! !context #(or % (load-context))))

(defn standing-status [] (:status (standing-context)))

(defn standing-verifier
  "Fail-closed observation verifier for the recovered scope. Accepts nothing
  when the context is refused; accepts only operations that exactly one
  retained bundle fully re-verifies."
  []
  (let [context (standing-context)]
    (if (= :ready (:status context))
      (medic-authority/resolution-verifier (:resolution context))
      (fn [_] false))))

(defn reset-context!
  "Test hook only. Never called by production paths."
  []
  (reset! !context nil))

(defn standing-resolution
  "THE single shared medic-authority resolution (brittleness design P1).

  The policy resource wm/standing-authority/policy.edn v1 IS the pin-addressed
  approved-authority record (design P2): content-addressed (every evidence
  artifact is a sha-named file re-hashed at load), scope-exact (grammar id +
  ordinal/entry pins + three receipt pins per bundle), sealed (its own sha256
  is pinned in the recovery report and the dispatch record), append-only (a
  new approval is a new policy version, never an edit of this one).

  Every default/cold-start, diagnosis, and store-loader path composes THIS
  function; there is no other composition site. On a refused standing context
  the retained per-grammar union remains the fail-closed baseline (never a
  permissive fallback) and the typed refusal reason is carried for diagnosis.
  The :identity on a ready resolution is the resolving code/authority
  identity (design P4): load outcomes stamped with it tie a validation result
  to the exact standing policy that produced it."
  []
  (let [context (standing-context)]
    (cond-> {:status (if (= :ready (:status context)) :ready :absent)
             :verifier (if (= :ready (:status context))
                         (medic-authority/resolution-verifier (:resolution context))
                         (medic-authority/with-retained-authorities (fn [_] false)))}
      (= :ready (:status context)) (assoc :identity (:identity context))
      (= :refused (:status context)) (assoc :standing-authority-refusal
                                            (:reason context)))))
