(ns futon2.aif.want-interpretation
  "D11 / H-interp: the machine asks for the interpretation a want lacks.

  A flight's want that is not yet true and that no admitted interpretation
  produces cannot be constructed for; before this seam the target was
  refused (:no-admitted-interpretation, or the constructor's :no-producer).
  The seam has three parts, each reviewable on its own (FIELD-D's split):

    1. REQUEST (this namespace, `unproduced-wants`, `request`, `request!`):
       for one want, the exit token and the criterion text it was read from,
       the target's current facts and existing interpretations, and a pinned
       library retrieval whose query is that criterion.
    2. VALIDATION of a response (loader + constructor + admission).
    3. PUBLICATION into the sources the tick reads.

  Parts 2 and 3 are separate commits. Nothing here interprets: retrieval
  candidates are unjudged, and the response is the answerer's."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.interpretation-request :as ireq])
  (:import [java.nio.file Files StandardCopyOption]
           [java.time Instant]))

(defn unproduced-wants
  "Wants that are not true in UNIVERSE and that no pattern in PATTERNS
  produces, in WANTS order. These are the wants a constructor cannot plan
  for, whatever else is admitted."
  [wants universe patterns]
  (let [produced (set (mapcat :produces (vals patterns)))]
    (vec (remove #(or (true? (get universe %)) (contains? produced %)) wants))))

(defn citation-for
  "The citation of CRITERION ({:line n :stated text}) in TEXT, located by its
  stated text rather than its recorded line: an edit above the criterion
  moves it without changing it, and must not refuse. Refuses
  :want/criterion-absent when the stated text no longer occurs, and
  :want/criterion-ambiguous when it occurs more than once (the want token is
  keyed on that text, so two occurrences cannot say which was meant). A
  relocation is recorded with the line the want was read at."
  [source-id text {:keys [line stated]}]
  (let [starts (loop [from 0 acc []]
                 (let [k (str/index-of text stated from)]
                   (if (nil? k) acc (recur (inc k) (conj acc k)))))
        refuse (fn [kind] (throw (ex-info "criterion not located by its text"
                                          {:interpretation/refusal kind :line line :stated stated
                                           :occurrences (count starts)})))]
    (case (count starts)
      0 (refuse :want/criterion-absent)
      1 (let [a (inc (count (filter #{\newline} (subs text 0 (first starts)))))
              b (+ a (count (str/split-lines stated)) -1)
              lines (vec (str/split-lines text))]
          (cond-> {:source source-id :lines [a b]
                   :quote (str/join "\n" (subvec lines (dec a) b))}
            (and line (not= a line)) (assoc :relocated-from line)))
      (refuse :want/criterion-ambiguous))))

(defn request
  "The request for one WANT of TARGET. CRITERION is the reader's record of
  it ({:kind :line :phase :stated}); FACTS the target's current universe;
  PATTERNS the admitted interpretations. RETRIEVAL, when given, is the
  pinned library retrieval (from `request!`)."
  [{:keys [target want criterion facts patterns retrieval]}]
  (cond-> {:schema :wm/want-interpretation-request-v1
           :target target
           :want {:token want
                  :observed (get facts want)
                  :criterion (select-keys criterion [:kind :line :phase :stated])}
           :context {:facts facts
                     :interpretations (into (sorted-map-by #(compare (str %1) (str %2)))
                                            (for [[id p] patterns]
                                              [id (select-keys p [:guard :produces])]))}
           :asks {:what "one interpretation of a futon3/library pattern whose :produces contains the want token"
                  :answer-shape {:pattern "family/name, a file under futon3/library"
                                 :guard {:needs "#{token} from the facts or other interpretations' :produces"
                                         :forbids "#{token}"}
                                 :produces (str "#{" want " ...}")
                                 :receipt {:source {:path "futon3/library/<family>/<name>.flexiarg"
                                                    :sha256 "of the bytes read"}
                                           :reading "how the pattern applies to this criterion"
                                           :scope-limit "what of the pattern does not transfer"
                                           :by "answering agent id"}}
                  :or "a typed decline naming why no library pattern produces this want"}}
    retrieval (assoc :retrieval retrieval)))

(defn request!
  "`request` plus a pinned retrieval over the library whose query is the
  criterion's text at its recorded lines (interpretation-request's pinned
  path; hermetic in tests through OPTIONS). ROOT holds the evidence."
  [{:keys [target criterion] :as m} root options]
  (let [r (ireq/prepare-want-proposal! target :mission root
                                       (fn [src text] [(citation-for src text criterion)])
                                       options)]
    (request (assoc m :retrieval (select-keys r [:target :sources :retrieval])))))

;; ---------------------------------------------------------------------------
;; Part 2: validating a response

(def receipt-keys [:reading :scope-limit :by])

(def receipt-key-aliases
  "claude-1's hand units spell the receipt keys :scope and :author; the reply
  grammar's canonical keys are :scope-limit and :by (H-INTERP-D gap 1)."
  {:scope :scope-limit :author :by})

(defn normalise-receipt
  "RECEIPT with the hand-unit spellings (:scope, :author) renamed to the
  canonical keys (:scope-limit, :by), so a hand unit and a seat reply are
  the same record. A key present under both spellings with different values
  is a conflict, not a merge: returns {:conflicts [...]}."
  [receipt]
  (reduce-kv (fn [r alias canonical]
               (if (contains? r alias)
                 (let [a (get r alias) c (get r canonical)]
                   (if (and (some? c) (not= a c))
                     (reduced {:conflicts [{:reason :receipt-key-conflict
                                            :alias alias :canonical canonical
                                            :alias-value a :canonical-value c}]})
                     (-> r (dissoc alias) (assoc canonical a))))
                 r))
             (or receipt {}) receipt-key-aliases))

(defn- library-file [code-root path] (io/file code-root path))

(defn- receipt-reasons
  "Why RECEIPT does not warrant pattern ID: the source must be the library
  file for ID, present, with the sha256 of its bytes."
  [code-root id {:keys [source] :as receipt}]
  (let [path (:path source)
        expected (str "futon3/library/" (namespace id) "/" (name id) ".flexiarg")
        f (when (string? path) (library-file code-root path))]
    (cond-> []
      (some #(str/blank? (str (get receipt %))) receipt-keys)
      (conj {:reason :receipt-incomplete :missing (vec (filter #(str/blank? (str (get receipt %))) receipt-keys))})
      (not= expected path)
      (conj {:reason :source-not-the-pattern-file :expected expected :path path})
      (and f (not (.isFile f)))
      (conj {:reason :source-missing :path path})
      (and f (.isFile f) (not= (:sha256 source) (evidence/sha256 (Files/readAllBytes (.toPath f)))))
      (conj {:reason :source-sha-mismatch :path path :declared (:sha256 source)}))))

(def agent-search-retriever "agent-search")

(defn- appended-run-reasons
  "Why the response's appended retrieval runs do not hold. A response may
  append runs the answerer found by searching the library itself (the
  interpretation job's agent-search convention): every appended run must be
  named \"agent-search\", and every candidate must locate a file inside the
  library — an explicit :source :path, or the pattern id's own library file.
  The canonical path is compared against the library directory under
  CODE-ROOT, so \"..\" segments and other roots refuse. When the candidate
  declares the bytes' sha256 and the file is present, the bytes must match."
  [code-root response]
  (let [lib (.getCanonicalFile (io/file code-root "futon3" "library"))]
    (vec
     (for [run (get-in response [:retrieval :runs])
           reason (concat
                   (when (not= agent-search-retriever (:retriever run))
                     [{:reason :appended-run-not-agent-search :retriever (:retriever run)}])
                   (mapcat
                    (fn [c]
                      (let [path (or (get-in c [:source :path])
                                     (when-let [p (:pattern c)]
                                       (str "futon3/library/" p ".flexiarg")))]
                        (cond
                          (not (string? path))
                          [{:reason :appended-candidate-unlocatable :candidate c}]
                          ;; io/file throws on an absolute child path; a seat
                          ;; naming one is outside the library, not a crash
                          ;; (claude-8 review, 2026-09-25)
                          (.isAbsolute (io/file path))
                          [{:reason :appended-candidate-outside-library :path path}]
                          :else
                          (let [f (.getCanonicalFile (library-file code-root path))
                                ;; the separator keeps a sibling such as
                                ;; futon3/library-old/ from passing as a prefix
                                inside? (str/starts-with? (str f) (str lib java.io.File/separator))]
                            (cond-> []
                              (not inside?)
                              (conj {:reason :appended-candidate-outside-library :path path})
                              ;; the rule is that a candidate LOCATES a file:
                              ;; a path naming nothing is unlocatable, not a pass
                              (and inside? (not (.isFile f)))
                              (conj {:reason :appended-candidate-unlocatable :path path})
                              (and (.isFile f) (get-in c [:source :sha256])
                                   (not= (get-in c [:source :sha256])
                                         (evidence/sha256 (Files/readAllBytes (.toPath f)))))
                              (conj {:reason :appended-source-sha-mismatch :path path
                                     :declared (get-in c [:source :sha256])}))))))
                    (:candidates run)))]
       reason))))

(defn- guard-reasons
  "Guard tokens must be ones the target already knows: its facts, its wants,
  or another interpretation's products. A new token would have no locator,
  so no check could ever observe it."
  [interp known]
  (let [tokens (set (concat (get-in interp [:guard :needs]) (get-in interp [:guard :forbids])))
        unknown (set/difference tokens known)]
    (cond-> [] (seq unknown) (conj {:reason :guard-token-unknown :tokens (vec (sort-by str unknown))}))))

(defn- constraint-reasons
  "Owner constraints {:want w :requires r :by … :reason …}: an interpretation
  producing W must need R, so W is reachable only through R."
  [interp constraints]
  (vec (for [{:keys [want requires] :as c} constraints
             :when (and (contains? (set (:produces interp)) want)
                        (not (contains? (set (get-in interp [:guard :needs])) requires)))]
         {:reason :owner-constraint-violated :constraint c})))

(defn validate-response
  "Validate RESPONSE to REQUEST against the target's SOURCES (the tick's
  sources map, with :construction supplied). RESPONSE is
  {:pattern id :guard … :produces … :receipt …} or {:decline {:reason …}}.

  Checks, in order, all reported: receipt keys normalised (:scope →
  :scope-limit, :author → :by; both spellings with different values is a
  conflict, never a merge); id canonical (the loader's own rule);
  receipt names the pattern's library file with matching sha256 and states
  reading, scope-limit and by; :produces contains the requested want; guard
  tokens are known; owner CONSTRAINTS hold; the constructor, with this
  interpretation added, builds a candidate using it that reaches the want;
  ADMIT (the tick's admission, injected) accepts that problem.

  Returns {:status :valid :interpretation {id {...}} :receipt … :candidate …},
  {:status :declined …} or {:status :rejected :reasons [...]}."
  [request response {:keys [code-root sources constraints admit]
                     :or {code-root "/home/joe/code"}}]
  (let [target (:target request)
        want (get-in request [:want :token])]
    (if-let [decline (:decline response)]
      {:status :declined :target target :want want :decline decline}
      (let [norm-receipt (normalise-receipt (:receipt response))
            response (if (:conflicts norm-receipt)
                       response
                       (assoc response :receipt norm-receipt))
            id (try (cs/canonical-pattern-id (:pattern response) :want-response :pattern)
                    (catch clojure.lang.ExceptionInfo _ nil))
            interp (cond-> {:guard {:needs (set (get-in response [:guard :needs]))
                                    :forbids (set (get-in response [:guard :forbids]))}
                            :produces (set (:produces response))}
                     ;; :forces, when a reply supplies it, is carried as an
                     ;; unchecked note. It is NOT required: a required
                     ;; field that is only tested non-blank proves nothing
                     ;; about the reading (Joe, 2026-09-25, relayed by
                     ;; claude-10); :reading and the source sha are the
                     ;; application statement and its pin.
                     (some? (:forces response)) (assoc :forces (:forces response)))
            patterns (get-in sources [:interpretations target :patterns])
            known (set (concat (keys (get-in sources [:universes target]))
                               (get-in sources [:wants target])
                               (mapcat :produces (vals patterns))))
            static (vec (concat
                         (:conflicts norm-receipt)
                         (when-not id [{:reason :invalid-pattern-id :value (:pattern response)}])
                         (when (and id (not (:conflicts norm-receipt)))
                           (receipt-reasons code-root id (:receipt response)))
                         (when-not (contains? (:produces interp) want)
                           [{:reason :does-not-produce-the-want :want want}])
                         (guard-reasons interp known)
                         (constraint-reasons interp constraints)
                         (when (:retrieval response)
                           (appended-run-reasons code-root response))))]
        (if (seq static)
          {:status :rejected :target target :want want :reasons static}
          (let [trial (-> sources
                          (assoc-in [:interpretations target :patterns id] interp)
                          (assoc-in [:interpretations target :receipts id] (:receipt response))
                          (update :candidates dissoc target)
                          ;; no declared horizon: count the interpretations
                          ;; with this one, as the tick's resolution does
                          (as-> t (cond-> t (nil? (:horizon-steps t))
                                    (assoc :horizon-steps
                                           (max 1 (count (get-in t [:interpretations target :patterns])))))))
                {:keys [problems refusals]} (cp/assemble {:targets [target] :sources trial})
                problem (first problems)
                using (first (filter #(and (some #{id} (:precedence %))
                                           (not-any? (fn [u] (= want (:token u)))
                                                     (get-in % [:construction-receipt :unreached-wants])))
                                     (:constructed-candidates problem)))
                admitted (when (and using admit) (admit problem))]
            (cond
              (nil? problem)
              {:status :rejected :target target :want want
               :reasons [{:reason :construction-refused :refusal (first refusals)}]}
              (nil? using)
              {:status :rejected :target target :want want
               :reasons [{:reason :no-candidate-reaches-the-want-through-it
                          :candidates (mapv #(select-keys % [:precedence]) (:constructed-candidates problem))}]}
              (and admit (:refusal admitted))
              {:status :rejected :target target :want want
               :reasons [{:reason :admission-refused :refusal (:refusal admitted)}]}
              :else
              {:status :valid :target target :want want
               :interpretation {id interp}
               :receipt (:receipt response)
               :candidate (select-keys using [:precedence :construction-receipt])})))))))

;; ---------------------------------------------------------------------------
;; Part 3: publication into the sources the tick reads

(def default-store
  "Machine-published interpretations, one file per target. Kept apart from
  resources/wm/cascade-sources (hand-written declarations), which win on any
  pattern they both name."
  "/home/joe/code/futon2/data/wm-interpretations")

(defn content-id [kind x]
  (str (name kind) "-" (subs (evidence/sha256 (.getBytes (pr-str x) "UTF-8")) 0 16)))

(defn- target-file [store target] (io/file store (str target ".edn")))

(defn read-published
  "The machine-published interpretations for TARGET, or nil."
  [store target]
  (let [f (target-file store target)]
    (when (.isFile f) (edn/read-string (slurp f)))))

(defn write-atomic! [^java.io.File f x]
  (.mkdirs (.getParentFile f))
  (let [tmp (io/file (.getParentFile f) (str "." (.getName f) "." (System/nanoTime) ".tmp"))]
    (spit tmp (with-out-str (pp/pprint x)))
    (Files/move (.toPath tmp) (.toPath f)
                (into-array StandardCopyOption [StandardCopyOption/ATOMIC_MOVE StandardCopyOption/REPLACE_EXISTING]))))

(defn issue!
  "Record REQUEST in STORE as issued by the machine; returns it with its
  :request-id. Only an issued request can have its answer published: the
  issued record is what binds a response to something the machine asked."
  [store request]
  (let [id (content-id :request (dissoc request :retrieval))
        f (io/file store "requests" (str id ".edn"))
        issued (assoc request :request-id id)]
    (when-not (.isFile f) (write-atomic! f issued))
    issued))

(defn issued-request [store request-id]
  (let [f (io/file store "requests" (str request-id ".edn"))]
    (when (and (string? request-id) (.isFile f)) (edn/read-string (slurp f)))))

(def validator
  "The validator's identity for published receipts: this namespace and the
  sha256 of its source as loaded."
  (delay {:ns "futon2.aif.want-interpretation"
          :source-sha256 (some-> (io/resource "futon2/aif/want_interpretation.clj")
                                 slurp (.getBytes "UTF-8") evidence/sha256)}))

;; ---------------------------------------------------------------------------
;; Part 4 support: the prompt and the reply grammar

(def response-schema :wm/want-interpretation-response-v1)

(defn prompt
  "What the machine sends the answering seat for an ISSUED request. OPTIONS:
  :library-root names the library the seat may search itself (default
  interpretation-request's pinned root) — the want-interpretation sibling of
  the interpretation job's agent-search affordance, so a reviewed pattern
  the retriever did not surface is still reachable (H-INTERP-D §3, gap 3)."
  ([issued] (prompt issued nil))
  ([issued {:keys [library-root] :or {library-root ireq/library-root}}]
   (str "The War Machine asks for one interpretation (D11). Request "
        (:request-id issued) ":\n\n```edn\n" (with-out-str (pp/pprint (dissoc issued :retrieval)))
        "```\n\nLibrary retrieval candidates (unjudged): "
        (pr-str (vec (for [run (get-in issued [:retrieval :retrieval :runs])
                           c (:candidates run)]
                       (or (:pattern c) (:id c) c))))
        "\n\nWhole-section retrieval can be weak: you may search the captured library under "
        library-root " and append conformant runs named \"agent-search\" to your response under "
        ":retrieval {:runs [...]}; each run is {:retriever \"agent-search\" :candidates "
        "[{:pattern \"family/name\" :source {:path \"futon3/library/<family>/<name>.flexiarg\" "
        ":sha256 \"of its bytes\"}}]}. A run with any other retriever name, or a candidate "
        "path outside futon3/library/, refuses with a typed reason. "
        "\n\nREPLY GRAMMAR: your reply must contain exactly one fenced ```edn block holding "
        "{:schema " response-schema " :pattern … :guard {:needs #{…} :forbids #{…}} :produces #{…} "
        ":receipt {:source {:path … :sha256 …} :reading … :scope-limit … :by …}} "
        "(:retrieval {:runs [...]} optional, for agent-search runs as above) or "
        "{:schema " response-schema " :decline {:reason …}}; anything else is unparseable.\n")))

(defn parse-reply
  "The single response form in reply TEXT: {:response m}, {:decline d}, or
  {:unparseable-response {:forms n …}} when there is not exactly one fenced
  EDN form of the response SCHEMA (default: the interpretation response).
  Never a decline by default."
  ([text] (parse-reply response-schema text))
  ([schema text]
  (let [blocks (map second (re-seq #"(?s)```(?:edn|clojure)?\s*\n(.*?)```" (str text)))
        forms (keep (fn [b] (try (let [x (edn/read-string b)]
                                   (when (and (map? x) (= schema (:schema x))) x))
                                 (catch Exception _ nil)))
                    blocks)]
    (if (= 1 (count forms))
      (let [m (first forms)]
        (if (:decline m) {:decline (:decline m)} {:response (dissoc m :schema)}))
      {:unparseable-response {:forms (count forms) :fenced-blocks (count blocks)}}))))

(defn publish!
  "Publish a VALIDATED result (validate-response :status :valid) for the
  issued REQUEST (from `issue!`, carrying :request-id) and RESPONSE into
  STORE. Refuses a request id that does not resolve to a request the machine
  issued, or whose target and want differ from the validated result's. The request and response are kept whole under
  content ids; the interpretation's receipt carries both ids, the checks it
  passed and the answerer's own receipt. Refuses anything not :valid, and a
  pattern id already published for this target with a different reading
  (republishing the same one is a no-op). Returns the target's record."
  [store request response validated & [{:keys [now answered-by] :or {now #(str (Instant/now))}}]]
  (when-not (= :valid (:status validated))
    (throw (ex-info "only a validated response is published"
                    {:interpretation/refusal :want/not-validated :status (:status validated)})))
  (let [request-id (:request-id request)
        issued (issued-request store request-id)
        _ (when-not (and issued (= (:target issued) (:target validated))
                         (= (get-in issued [:want :token]) (:want validated)))
            (throw (ex-info "the response is not bound to a request the machine issued"
                            {:interpretation/refusal :want/request-not-issued
                             :request-id request-id :target (:target validated) :want (:want validated)})))
        request issued
        target (:target request)
        [id interp] (first (:interpretation validated))
        response-id (content-id :response response)
        prior (or (read-published store target)
                  {:schema :wm/machine-interpretations-v1 :target target
                   :patterns {} :receipts {} :records {}})
        existing (get-in prior [:patterns id])]
    (when (and existing (not= existing interp))
      (throw (ex-info "a different interpretation of this pattern is already published"
                      {:interpretation/refusal :want/conflicting-publication :pattern id
                       :published existing :offered interp})))
    (let [record (-> prior
                     (assoc-in [:patterns id] interp)
                     (assoc-in [:receipts id]
                               (assoc (:receipt validated)
                                      :kind :machine-requested
                                      :validator @validator
                                      :answered-by answered-by
                                      :request-id request-id :response-id response-id
                                      :want (:want validated)
                                      :validated {:checks [:canonical-id :library-source-sha :produces-want
                                                           :guard-tokens-known :owner-constraints
                                                           :constructs-through-it :admitted]
                                                  :candidate (:candidate validated)
                                                  :at (now)}))
                     (assoc-in [:records request-id] (dissoc request :request-id))
                     (assoc-in [:records response-id] response))]
      (when-not existing (write-atomic! (target-file store target) record))
      record)))

(defn merge-published
  "SOURCES with each of TARGETS' machine-published interpretations merged
  in. A hand-written declaration wins on any pattern id it names. Records which ids came from
  the store under :machine-interpretations."
  [sources store targets]
  (reduce (fn [srcs target]
            (if-let [{:keys [patterns receipts]} (read-published store target)]
              (let [declared-map (get-in srcs [:interpretations target :patterns])
                    declared (set (keys declared-map))
                    fresh (remove (comp declared key) patterns)
                    reading-sha #(evidence/sha256 (.getBytes (pr-str (select-keys % [:guard :produces])) "UTF-8"))
                    ;; hand wins, but a published reading it overrode stays
                    ;; visible on the merged sources (AR-39)
                    overridden (vec (for [[id p] patterns
                                          :let [d (get declared-map id)]
                                          :when (and d (not= (reading-sha d) (reading-sha p)))]
                                      {:id id :published-sha (reading-sha p) :declared-sha (reading-sha d)
                                       :request-id (get-in receipts [id :request-id])}))]
                (-> srcs
                    (cond-> (seq overridden)
                      (assoc-in [:machine-interpretations-overridden target] overridden))
                    (update-in [:interpretations target :patterns] merge (into {} fresh))
                    (update-in [:interpretations target :receipts] merge
                               (select-keys receipts (map key fresh)))
                    (assoc-in [:machine-interpretations target] (vec (sort-by str (map key fresh))))))
              srcs))
          sources
          targets))
