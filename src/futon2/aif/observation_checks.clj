(ns futon2.aif.observation-checks
  "Mechanical observation checks for the checkable token classes of the WM-04
  observation contract (futon2 resources/wm/observation-contract.edn, classes
  C3-C6), plus C8, which reads the test registry rather than a git object.
  Each check reads a repository at a pinned sha and returns either
  {:observed true|false :check … :evidence …} or a typed refusal
  {:status :missing :kind …}.

  These observe exactly the fact each check defines: that a path exists, that
  a declaration head is present in a file, or that a contract entry exists
  with a clojure locus. A true result is not evidence that the artifact is
  correct. Under P5 these channels have zero adjudication rates by construction
  (TokenObservation.tokenLikelihood_checkable). Any claim needing judgement is
  class J, which has no measured rate and is refused at assembly.

  Warrant checks are outside the WM observation contract."
  (:require [babashka.http-client :as http]
            [clojure.edn :as edn]
            [clojure.data.json :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str])
  (:import (java.net URLEncoder)
           (java.security MessageDigest)))

(def repo-root "/home/joe/code")

(defn- refuse [kind data] {:status :missing :kind kind :data data})

(defn- git [repo & args]
  (apply sh/sh "git" "-C" (str repo-root "/" repo) args))

(defn- locator-refusal
  "A check needs every locator field as a non-blank string."
  [check m ks]
  (when-let [missing (seq (remove #(and (string? (get m %)) (not (str/blank? (get m %)))) ks))]
    (refuse :no-locator {:check check :missing (vec missing)})))

(defn- resolve-reference [repo reference]
  (let [{:keys [exit out]} (git repo "rev-parse" "--verify" "--end-of-options"
                               (str reference "^{commit}"))]
    (if (zero? exit)
      {:repo repo :sha reference :resolved-sha (str/trim out)}
      (refuse :unknown-sha {:repo repo :sha reference}))))

(defn check-path-exists
  "C3: resolve the declared reference once; check the file at that commit."
  [{:keys [repo sha path] :as m}]
  (or (locator-refusal :C3 m [:repo :sha :path])
      (let [reference (resolve-reference repo sha)]
        (if (:status reference) reference
            (let [{:keys [exit]} (git repo "cat-file" "-e"
                                     (str (:resolved-sha reference) ":" path))]
              {:observed (zero? exit) :check :C3
               :evidence (assoc reference :path path)})))))

(defn decl-present?
  "The declaration head DECL starts a line of TEXT (after optional leading
  whitespace), and the token after it ends at whitespace, `:`, `(`, `{`, `[`
  or end of line. So `theorem foo` does not match `theorem foo_bar`, a
  comment or a docstring (claude-7's applicability reading, 2026-09-17)."
  [text decl]
  (boolean
   (re-find (re-pattern (str "(?m)^\\s*" (java.util.regex.Pattern/quote decl) "(?=[\\s:({\\[]|$)"))
            text)))

(defn check-decl-in-file
  "C4: check the anchored declaration head at the resolved commit."
  [{:keys [repo sha path decl] :as m}]
  (or (locator-refusal :C4 m [:repo :sha :path :decl])
      (let [reference (resolve-reference repo sha)]
        (if (:status reference) reference
            (let [{:keys [exit out]} (git repo "show" (str (:resolved-sha reference) ":" path))]
              {:observed (and (zero? exit) (decl-present? out decl)) :check :C4
               :evidence (assoc reference :path path :decl decl :file-present (zero? exit))})))))

(defn- observe-locus [locus]
  (if-let [[_ repo path line] (re-matches #"([^/]+)/(.+):(\d+)" (str locus))]
    (let [reference (resolve-reference repo "HEAD")]
      (if (:status reference)
        (update reference :data assoc :locus locus)
        (let [{:keys [exit out]} (git repo "show" (str (:resolved-sha reference) ":" path))]
          {:observed (boolean (and (zero? exit)
                                   (<= (Long/parseLong line) (count (str/split-lines out)))))
           :evidence (assoc reference :path path :line (Long/parseLong line))})))
    {:observed false :reason :invalid-clojure-locus :locus locus}))

(defn check-registry-entry
  "C5: resolve the bundle reference and each locus repository HEAD separately.
   Every content check uses its recorded resolved commit."
  [{:keys [repo sha bundle-path entry] :as m}]
  (or (locator-refusal :C5 m [:repo :sha :bundle-path :entry])
      (let [reference (resolve-reference repo sha)]
        (if (:status reference) reference
          (let [{:keys [exit out]} (git repo "show" (str (:resolved-sha reference) ":" bundle-path))]
            (if-not (zero? exit)
              (refuse :bundle-not-found (assoc reference :bundle-path bundle-path))
              (let [contract (some #(when (= entry (get % "contract-id")) %)
                                   (get (json/read-str out) "contracts"))
                    loci (vec (keep #(get % "clojure-locus") (get contract "declarations")))
                    observations (into {} (map (fn [l] [l (observe-locus l)])) loci)
                    refusal (some #(when (:status %) %) (vals observations))]
                (if refusal
                  (assoc refusal :evidence (assoc reference :bundle-path bundle-path
                                                 :entry entry :locus-evidence observations))
                  (let [resolved (into {} (map (fn [[l o]] [l (boolean (:observed o))])) observations)]
                    {:observed (boolean (and contract (seq loci) (every? true? (vals resolved))))
                     :check :C5
                     :evidence (assoc reference :bundle-path bundle-path :entry entry
                                      :contract-found (boolean contract) :clojure-loci resolved
                                      :locus-evidence observations)})))))))))

(defn check-witness-reference
  "C6: an EDN witness contains {:repo string :sha string}, optionally
  :entry (a repository path at that commit). Observe only that the referenced
  commit or entry exists; no claim about its authorship or correctness.
  Missing witness is false. Malformed or unresolved references refuse."
  [{:keys [repo sha path] :as locator}]
  (or (locator-refusal :C6 locator [:repo :sha :path])
      (let [reference (resolve-reference repo sha)]
        (if (:status reference) reference
          (let [evidence (assoc reference :path path)
                {:keys [exit out]} (git repo "show" (str (:resolved-sha reference) ":" path))]
            (if-not (zero? exit)
              {:observed false :check :C6 :evidence (assoc evidence :witness-present false)}
              (let [witness (try (edn/read-string out)
                                 (catch Exception _ ::malformed))
                    bad (when (map? witness)
                          (or (locator-refusal :C6 witness [:repo :sha])
                              (when (or (:require-entry locator) (contains? witness :entry))
                                (locator-refusal :C6 witness [:entry]))))]
                (cond
                  (not (map? witness))
                  (assoc (refuse :invalid-witness {:check :C6}) :evidence evidence)
                  bad (assoc bad :evidence evidence)
                  :else
                  (let [resolved (resolve-reference (:repo witness) (:sha witness))]
                    (if (:status resolved)
                      (assoc resolved :evidence (assoc evidence :witness witness))
                      (let [entry (:entry witness)
                            present? (or (nil? entry)
                                         (zero? (:exit (git (:repo witness) "cat-file" "-e"
                                                            (str (:resolved-sha resolved) ":" entry)))))]
                        {:observed (boolean present?) :check :C6
                         :evidence (assoc evidence :witness-present true
                                          :reference (cond-> resolved entry (assoc :entry entry)))})))))))))))

;; ---------------------------------------------------------------------------
;; C8: a test namespace passed AT THE CURRENT CONTENT (AR-41).
;;
;; C6 observes that a witness references an existing commit, so a receipt
;; recording FAILING gates reads true under it (claude-10, first live read on
;; M-omni-wm-runner, 2026-09-24). Every criterion of the form "tests stay
;; green" therefore had no checkable class and could only be declined.
;;
;; The registry already records the needed fact: futon3c's test-registry pins
;; each run to the SHA-256 of every declared code and test file, records a
;; postcheck that those inputs did not move during the run, and records the
;; run's counts. C8 observes that such a record exists for the namespace AND
;; that its pinned bytes are still the bytes on disk. It does not rerun tests
;; and makes no claim that the tests are adequate.

(def ^:private registry-entry-id-pattern #"^test-registry-[0-9a-f]{64}$")

(defn agency-base
  "Evidence API base, resolved as the rest of the stack resolves it
  (futon2.aif.pattern-registry/configured-evidence-base): FUTON3C_EVIDENCE_BASE,
  FUTON3C_SERVER, then IPv4 loopback on FUTON3C_PORT. The listener is
  IPv4-bound, so the loopback is 127.0.0.1 and not localhost."
  ([] (agency-base (System/getenv)))
  ([env]
   (or (not-empty (get env "FUTON3C_EVIDENCE_BASE"))
       (not-empty (get env "FUTON3C_SERVER"))
       (str "http://127.0.0.1:" (or (not-empty (get env "FUTON3C_PORT")) "7070")))))

(defn- sha256-hex [^bytes bs]
  (format "%064x" (BigInteger. 1 (.digest (doto (MessageDigest/getInstance "SHA-256")
                                            (.update bs))))))

(defn content-sha
  "SHA-256 of FILE's current bytes, or nil when it is not there. Byte-for-byte
  the registry's own hash (futon3c.test-registry/file-sha), so a pinned sha and
  a current sha are comparable values and not two spellings."
  [file]
  (let [f (io/file file)]
    (when (.isFile f)
      (sha256-hex (java.nio.file.Files/readAllBytes (.toPath f))))))

(defn fetch-registry-entry
  "Read one registry record through the evidence API.

  Three outcomes, and C8 turns on telling them apart: the entry map; :absent
  when the store answers and holds no such record (an observation, false);
  a refusal when the store could not be asked or did not answer (unreadable)."
  [base entry-id]
  (let [url (str (str/replace base #"/$" "") "/api/alpha/evidence/" entry-id)
        {:keys [status body]} (try (http/get url {:timeout 5000 :throw false})
                                   (catch Exception e
                                     {:status :unreachable :body (.getMessage e)}))]
    (cond
      (= 404 status) :absent
      (not= 200 status) (refuse :registry-unreadable {:check :C8 :url url :status status})
      :else (let [parsed (try (json/read-str body :key-fn keyword)
                              (catch Exception _ nil))]
              (cond
                (nil? parsed) (refuse :registry-unreadable {:check :C8 :url url
                                                            :reason :unparseable-response})
                (nil? (:entry parsed)) :absent
                :else (:entry parsed))))))

(def ^:dynamic *registry-entry*
  "The seam C8 reads the registry through: (fn [base entry-id] -> entry |
  :absent | refusal). Bound by tests to a stubbed registry, so C8's own tests
  neither need a live agency nor load anything into the serving JVM."
  fetch-registry-entry)

(defn- fetch-latest-for
  "GET /api/alpha/test-registry/latest?<param>=<value>&limit=100 — the shared
  reader for the namespace and command forms. The registry's own typed
  absence is \"no-run-for-<param>\"; a 400 is the endpoint refusing the
  question (a malformed command vector, say), and its named reason is
  carried, not flattened into a bare status."
  [base param value]
  (let [url (str (str/replace base #"/$" "") "/api/alpha/test-registry/latest"
                 "?" param "=" (URLEncoder/encode (str value) "UTF-8")
                 "&limit=100")
        {:keys [status body]} (try (http/get url {:timeout 5000 :throw false})
                                   (catch Exception e
                                     {:status :unreachable :body (.getMessage e)}))]
    (if (not= 200 status)
      (let [parsed (try (json/read-str (str body) :key-fn keyword)
                        (catch Exception _ nil))]
        (refuse :registry-unreadable
                (cond-> {:check :C8 :url url :status status}
                  (:reason parsed) (assoc :reason (keyword (:reason parsed))))))
      (let [latest (:latest (try (json/read-str body :key-fn keyword)
                                 (catch Exception _ nil)))]
        (cond
          (nil? latest)
          (refuse :registry-unreadable {:check :C8 :url url :reason :unparseable-response})

          (true? (:found latest))
          {:entry-id (:entry-id latest) :resolved-by (keyword (str param "-lookup"))}

          (= (str "no-run-for-" param) (:reason latest)) :absent

          :else
          (refuse :registry-unreadable
                  (merge {:check :C8 :url url
                          :reason (keyword (or (:reason latest)
                                               "lookup-inconclusive"))}
                         ;; how far the lookup looked, when it said
                         (into {} (filter (comp some? val))
                               (select-keys latest [:scanned :held :registry-entries])))))))))

(defn fetch-latest-for-command
  "Ask the registry which record covers an exact logical COMMAND (E-kimi-task-19:
  gate runs — bb, sh, lake build — name no -n namespace, so the namespace
  lookup can never find them). VALUE sent is (pr-str command); the reply
  shape and the absence rule are the namespace lookup's own."
  [base command]
  (fetch-latest-for base "command" (pr-str (vec command))))

(defn fetch-latest-for-namespace
  "Ask the registry WHICH record covers NAMESPACE — AR-42's
  GET /api/alpha/test-registry/latest. One rule, held in futon3c: this check
  does not re-derive \"newest run\" for itself, because two implementations of
  that rule would disagree the first time one of them was wrong.

  Returns {:entry-id …}; :absent when the registry establishes that it holds no
  run for the namespace; a refusal when the lookup cannot be reached, when it
  refuses the question itself (a 400 names its own reason), or when it
  reports that its scan filled its window — a scan that ran out of room did not
  establish absence, and reading it as \"no tests are registered\" would be the
  substituted value this class exists to refuse.

  The page is asked for explicitly (&limit=100): with no limit the endpoint's
  default page over a several-thousand-entry registry does not answer within
  this client's 5s timeout (2026-09-25, live), which would refuse EVERY
  namespace, present or absent. A refusal carries the endpoint's own :scanned
  and :registry-entries when it reported them, so the refusal says how far the
  lookup looked rather than just that it refused."
  [base namespace]
  (fetch-latest-for base "namespace" (str namespace)))

(defn fetch-latest
  "The *registry-latest* seam's default: dispatch on the locator. A string is a
  namespace; {:command [...]} asks by command."
  [base locator]
  (if (string? locator)
    (fetch-latest-for-namespace base locator)
    (fetch-latest-for-command base (:command locator))))

(def ^:dynamic *registry-latest*
  "The seam C8 resolves a locator through: (fn [base locator] -> {:entry-id …}
  | :absent | refusal), where the locator is a namespace string or
  {:command [...]}. Bound by tests, so no test needs the lookup endpoint to
  be live."
  fetch-latest)

(defn- decode-record
  "The record is EDN inside the entry body, named by its own digest. Verify
  that naming before reading it: a body whose text does not hash to the id it
  was fetched under is an unreadable registry, not an observation about tests."
  [entry-id entry]
  (let [body (:evidence/body entry)
        text (:payload-edn body)]
    (if-not (string? text)
      (refuse :registry-unreadable {:check :C8 :entry-id entry-id :reason :no-payload})
      (let [digest (sha256-hex (.getBytes ^String text "UTF-8"))]
        (if-not (and (= digest (:sha256 body))
                     (= entry-id (str "test-registry-" digest)))
          (refuse :registry-unreadable {:check :C8 :entry-id entry-id
                                        :reason :record-digest-mismatch})
          (try (edn/read-string text)
               (catch Exception _
                 (refuse :registry-unreadable {:check :C8 :entry-id entry-id
                                               :reason :unreadable-record}))))))))

(defn- recorded-namespace
  "The namespace the registry actually ran: the argument after -n in the
  recorded command. Absent for a command of another shape (a Lean build), and
  then no namespace is claimed."
  [command]
  (when (sequential? command)
    (second (drop-while #(not= "-n" %) command))))

(defn- locate-record
  "The locator's :config is the registry's own name for a record — an entry id
  test-registry-<sha256> — or a path to the EDN config carrying :entry-id.
  A :config that names no record is a malformed locator, not a false reading:
  nothing was observed about any registry."
  [config]
  (cond
    (re-matches registry-entry-id-pattern config) {:entry-id config}
    (.isFile (io/file config))
    (let [cfg (try (edn/read-string (slurp config)) (catch Exception _ nil))
          id (:entry-id cfg)]
      (if (and (string? id) (re-matches registry-entry-id-pattern id))
        {:entry-id id :base (:agency-url cfg)}
        (refuse :no-record-id {:check :C8 :config config})))
    :else (refuse :no-record-id {:check :C8 :config config})))

(defn- path-comparison
  "Every declared path, its pinned sha and the sha of the bytes there now."
  [root files kind]
  (mapv (fn [[path pinned]]
          (let [current (content-sha (io/file root path))]
            {:path path :kind kind :pinned pinned :current current
             :matched? (= pinned current)}))
        (sort-by key files)))

(defn- c8-command-present?
  "A locator :command is present when it is a non-empty sequential (a vector of
  argv strings). Anything else is not a command this check can ask by."
  [command]
  (boolean (and (sequential? command) (seq command))))

(defn check-registered-run
  "C8: the registry holds a warrant for NAMESPACE whose pinned code-path and
  test-path shas are the shas of those files NOW, whose postcheck matched, and
  whose run recorded no failures and no errors.

  False (an observation, not a refusal) when there is no such record, when the
  record is for another namespace, when any pinned path has moved, when the
  postcheck did not match, or when the run recorded failures. Refuses only on a
  malformed locator or a registry that cannot be read.

  The locator resolves the record one of two ways: :namespace <string> asks
  the registry's namespace lookup, or :command <argv vector> asks its command
  lookup (a gate run — bb, sh, lake build — names no -n namespace, so without
  the command form it can never be observed). Exactly one of the two; both or
  neither is a locator refusal naming the rule.

  A true reading says those tests passed over exactly these bytes. It says
  nothing about whether the tests are worth passing."
  [{:keys [repo namespace command config] :as m}]
  (or (locator-refusal :C8 m [:repo])
      (when (contains? m :config) (locator-refusal :C8 m [:config]))
      ;; A :config locator names the record directly; it still answers for a
      ;; namespace (the judgement below compares it), never for a command.
      (when (contains? m :config) (locator-refusal :C8 m [:namespace]))
      (when-not (contains? m :config)
        (let [has-ns (boolean (and (string? namespace) (not (str/blank? namespace))))
              has-cmd (c8-command-present? command)]
          (when (or (and has-ns has-cmd) (not (or has-ns has-cmd)))
            (refuse :no-locator {:check :C8 :rule :exactly-one-of-namespace-or-command
                                 :namespace? has-ns :command? has-cmd}))))
      (let [by-command? (and (not (contains? m :config)) (c8-command-present? command))
            lookup (when-not (contains? m :config)
                     (if by-command? {:command (vec command)} namespace))
            located (if (contains? m :config)
                      (locate-record config)
                      ;; No :config: ask the registry which record covers this
                      ;; namespace or command (AR-42). The answer is still judged
                      ;; below — the lookup finds the newest run regardless of
                      ;; whether it passed, so a later failing run cannot hide
                      ;; behind an earlier green one.
                      (*registry-latest* (agency-base) lookup))
            resolved-by (if by-command? :command-lookup :namespace-lookup)]
        (if (:status located) located
          (if (= :absent located)
            ;; the registry answered and holds no run for this locator
            {:observed false :check :C8
             :evidence (cond-> {:repo repo :root (str repo-root "/" repo)
                                :resolved-by resolved-by :reason :no-entry}
                         (and (string? namespace) (not (str/blank? namespace)))
                         (assoc :namespace namespace)
                         by-command? (assoc :command (vec command)))}
            (let [entry-id (:entry-id located)
                  root (str repo-root "/" repo)
                  base (or (:base located) (agency-base))
                  entry (*registry-entry* base entry-id)
                  evidence (cond-> {:repo repo :root root
                                    :warrant-id entry-id}
                             (and (string? namespace) (not (str/blank? namespace)))
                             (assoc :namespace namespace)
                             by-command? (assoc :command (vec command))
                             (:resolved-by located) (assoc :resolved-by (:resolved-by located)))]
              (cond
                (:status entry) (assoc entry :evidence evidence)
                (= :absent entry) {:observed false :check :C8
                                   :evidence (assoc evidence :reason :no-entry)}
                :else
                (let [record (decode-record entry-id entry)]
                  (if (:status record) (assoc record :evidence evidence)
                      (let [ran (recorded-namespace (:command record))
                            {:keys [failures errors] :as results} (:results record)
                            paths (into (path-comparison root (:code-files record) :code)
                                        (path-comparison root (:test-files record) :test))
                            moved (filterv (complement :matched?) paths)
                            evidence (assoc evidence
                                            :recorded-namespace ran
                                            :postcheck (:postcheck record)
                                            :warrant? (:warrant? record)
                                            :run-counts (select-keys results
                                                                     [:tests :assertions :failures :errors])
                                            :paths paths
                                            :moved-paths (mapv :path moved))
                            reason (cond
                                     (not= :run (:kind record)) :not-a-run-record
                                     (and (not by-command?) (not= namespace ran))
                                     :namespace-mismatch
                                     (and by-command?
                                          (not= (vec command) (vec (:command record))))
                                     :command-mismatch
                                     (not (true? (:warrant? record))) :not-a-warrant
                                     (not= :matched (get-in record [:postcheck :status])) :postcheck-not-matched
                                     (not (and (number? failures) (zero? failures)
                                               (number? errors) (zero? errors))) :run-recorded-failures
                                     (seq moved) :content-moved)]
                        {:observed (nil? reason) :check :C8
                         :evidence (cond-> evidence reason (assoc :reason reason))}))))))))))

(def checks
  {:C3 check-path-exists
   :C4 check-decl-in-file
   :C5 check-registry-entry
   :C6 check-witness-reference
   :C8 check-registered-run})

(defn- observe* [tokens]
  (reduce-kv
   (fn [acc token {:keys [class] :as locator}]
     (let [f (get checks class)
           r (if f (f locator) (refuse :no-mechanical-check {:class class}))]
       (if (contains? r :status)
         (assoc-in acc [:refused token] r)
         (cond-> (assoc-in acc [:results token] r)
           (:observed r) (update :observed conj token)))))
   {:observed #{} :results {} :refused {}}
   tokens))

(defn observe
  "Observe located tokens through C3/C4/C5/C6/C8. Unknown classes are refused,
   never observed absent. No warrant service is consulted."
  [tokens]
  (observe* tokens))
