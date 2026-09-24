(ns futon2.aif.increment-attestation
  "Supply the build checkpoint's :increment attestation evidence from a
  registered test-registry warrant (PROOF-wm-works-2026-09-22, step ⟨1⟩7
  part 2). Absent by construction: when the attempt's work registered no
  warrant, no evidence is emitted, and an honest
  :missing [:attested-increment] remains the recorded result. The evidence
  is the wiring of an existing warrant id into the build judgment — never
  a synthesis. Matching is not verification; the route-attestation
  receipt records what was supplied."
  (:require [babashka.http-client :as http]
            [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.load-identity :as load-identity])
  (:import [java.net URLEncoder]
           [java.nio.charset StandardCharsets]
           [java.time Instant]))

(load-identity/register! *ns* *file*)

(def resource "wm/route-attestation-v1.edn")
(def warrant-prefix "test-registry-")

(defn declarations
  "The War Machine's own route declarations, supplied on every click via
  runner/config's :route-attestation."
  []
  (edn/read-string (slurp (io/resource resource))))

(defn- url-encode [s]
  (URLEncoder/encode (str s) StandardCharsets/UTF_8))

(def registration-timeout-ms (* 10 60 1000))

(defn registration-command
  "The command the runner registers: the criterion scopes' test namespaces
  over the repository's :test alias. Derived from the declarations, never
  supplied by the caller — the registry executes what this names and sets
  :warrant? from the run it actually performed."
  [decls]
  (let [tests (into [] (distinct (mapcat #(get-in % [:scope :tests]) (:criteria decls))))]
    ["clojure" "-X:test" ":nses" (pr-str (mapv symbol tests))]))

(defn register-warrant-http
  "Register the attempt's increment warrant through
  POST /api/alpha/test-registry/run and report the typed outcome. The body
  names what to run, never what happened. Every failure mode is visible
  and warrant-less: a refusal (tests failed, scope uncommitted), an
  unreachable registry, a non-200 — all return {:warrant? false ...} with
  the reason, so a reader can tell 'the tests failed' from 'the registry
  was unreachable'."
  [opts {:keys [repo author artifact-dir]}]
  (let [decls (or (:route-attestation opts) (declarations))]
    (try
      (let [body (json/generate-string
                  {:repo-root repo
                   :command (registration-command decls)
                   :author author
                   :artifact-dir artifact-dir
                   :code-paths (or (:warrant-code-paths opts) ["src"])
                   :test-paths (or (:warrant-test-paths opts) ["test"])})
            r (http/post (str (:agency-base opts) "/api/alpha/test-registry/run")
                         {:body body
                          :headers {"content-type" "application/json"}
                          :timeout registration-timeout-ms
                          :throw false})
            parsed (when (string? (:body r))
                     (try (json/parse-string (:body r) true)
                          (catch Exception _ nil)))]
        (cond
          (= "test-registry/refusal" (:record/type parsed))
          {:warrant? false :reason (:reason parsed) :details (:details parsed)
           :at (str (Instant/now))}

          (and (= 200 (:status r)) (contains? parsed :warrant?))
          {:warrant? (true? (:warrant? parsed))
           :evidence/id (:evidence/id parsed)
           :postcheck (:postcheck parsed)
           :at (str (Instant/now))}

          :else
          {:warrant? false :reason :registry-http-error :status (:status r)
           :at (str (Instant/now))}))
      (catch Throwable t
        {:warrant? false :reason :registry-unreachable
         :details {:message (.getMessage t)}
         :at (str (Instant/now))}))))

(defn warrant-entries-http
  "Default warrant port: test-registry entries in the evidence store behind
  the Agency HTTP boundary, filtered server-side by the :test-registry tag
  and by author when given. Returns raw entries; non-200 is an empty list,
  because a store that cannot be read is a store that supplies no warrant —
  the run then attests nothing, which is the fail-closed reading."
  [opts {:keys [author since]}]
  (let [url (str (:agency-base opts) "/api/alpha/evidence?tag=test-registry&limit=200"
                 (when (and author (not (str/blank? (str author))))
                   (str "&author=" (url-encode author)))
                 (when since
                   (str "&since=" (url-encode since))))
        r (http/get url {:timeout 10000 :throw false})]
    (if (= 200 (:status r))
      (or (:entries (json/parse-string (:body r) true)) [])
      [])))

(defn- run-payload
  "The parsed run record of a test-registry evidence entry, or nil when the
  entry is not a completed registry run."
  [entry]
  (let [payload (try (edn/read-string (get-in entry [:evidence/body :payload-edn]))
                     (catch Exception _ nil))]
    (when (and (map? payload)
               (= "test-registry/v1" (:schema payload))
               (= :run (:kind payload)))
      payload)))

(defn- instant-or-nil [x]
  (try (Instant/parse x) (catch Exception _ nil)))

(defn- repo-match?
  "The warrant's repo/root names the criterion's repository: equal, or the
  repository's name as the root's last path segment."
  [scope-repo root]
  (and (string? root)
       (or (= scope-repo root)
           (str/ends-with? root (str "/" scope-repo)))))

(defn- tests-covered?
  "Every test namespace the criterion's scope names appears in the warrant's
  executed command. A warrant over other tests does not attest this
  increment."
  [tests command]
  (let [text (pr-str command)]
    (every? #(str/includes? text %) tests)))

(defn qualifying-warrant
  "The latest warrant that attests this attempt's increment for criterion
  `criterion`, among `entries`: a :warrant? true registry run in the
  criterion's repository, whose command covers the scope's tests, run no
  earlier than `since` when given. Deterministic: ordered by
  [:finished-at :run/id], last taken. Nil when nothing qualifies — the
  usual case, and the honest one."
  [criterion entries {:keys [since]}]
  (let [scope (:scope criterion)
        since-instant (when since (instant-or-nil since))]
    (->> entries
         (keep (fn [entry]
                 (when-let [run (run-payload entry)]
                   (when (and (true? (:warrant? run))
                              (repo-match? (:repo scope) (:repo/root run))
                              (tests-covered? (:tests scope) (:command run))
                              (or (nil? since-instant)
                                  (let [ran (instant-or-nil (:ran-at run))]
                                    (and ran (not (.isBefore ran since-instant))))))
                     {:warrant-id (:evidence/id entry)
                      :finished-at (:finished-at run)
                      :run/id (:run/id run)}))))
         (sort-by (juxt :finished-at :run/id))
         last)))

(defn increment-evidence
  "The :increment evidence map for the build checkpoint judgment, or nil.
  `opts` carries :warrant-lookup-fn (default: the Agency HTTP port) and the
  declarations (default: the wm/route-attestation-v1 resource); `context`
  carries {:keys [repo author since]}. Only increment criteria backed by a
  qualifying registered warrant yield evidence; the warrant's own content
  hash (its registry id suffix) is the evidence digest."
  ([context] (increment-evidence {} context))
  ([opts context]
   (let [lookup (or (:warrant-lookup-fn opts) warrant-entries-http)
         decls (or (:route-attestation opts) (declarations))
         entries (lookup opts (select-keys context [:author :since]))]
     (some (fn [criterion]
             (when (and (= :increment (:kind criterion))
                        (= :registered-test-warrant (:evidence-kind criterion)))
               (when-let [warrant (qualifying-warrant criterion entries context)]
                 (let [warrant-id (:warrant-id warrant)
                       digest (when (and (string? warrant-id)
                                         (str/starts-with? warrant-id warrant-prefix))
                                (subs warrant-id (count warrant-prefix)))]
                   (when (and digest (re-matches #"[0-9a-f]{64}" digest))
                     {:criterion (select-keys criterion [:id :version])
                      :kind :registered-test-warrant
                      :status :present
                      :target (:target criterion)
                      :want (:want criterion)
                      :scope (:scope criterion)
                      :sha256 digest
                      :warrant-id warrant-id
                      :at (:finished-at warrant)})))))
           (:criteria decls)))))
