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
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.interpretation-request :as ireq])
  (:import [java.nio.file Files]))

(defn unproduced-wants
  "Wants that are not true in UNIVERSE and that no pattern in PATTERNS
  produces, in WANTS order. These are the wants a constructor cannot plan
  for, whatever else is admitted."
  [wants universe patterns]
  (let [produced (set (mapcat :produces (vals patterns)))]
    (vec (remove #(or (true? (get universe %)) (contains? produced %)) wants))))

(defn citation-for
  "The citation of CRITERION ({:line n :stated text}) in TEXT, checked
  against the text: the stated span must be the text at those lines, or the
  mission has moved since the want was read and the request refuses."
  [source-id text {:keys [line stated]}]
  (let [lines (vec (str/split-lines text))
        n (count (str/split-lines stated))
        a line b (+ line n -1)
        at (when (<= 1 a b (count lines)) (str/join "\n" (subvec lines (dec a) b)))]
    (when-not (and at (str/starts-with? at stated))
      (throw (ex-info "criterion is not at its recorded lines"
                      {:interpretation/refusal :want/criterion-moved
                       :line line :stated stated :at at})))
    {:source source-id :lines [a b] :quote at}))

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

  Checks, in order, all reported: id canonical (the loader's own rule);
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
      (let [id (try (cs/canonical-pattern-id (:pattern response) :want-response :pattern)
                    (catch clojure.lang.ExceptionInfo _ nil))
            interp {:guard {:needs (set (get-in response [:guard :needs]))
                            :forbids (set (get-in response [:guard :forbids]))}
                    :produces (set (:produces response))}
            patterns (get-in sources [:interpretations target :patterns])
            known (set (concat (keys (get-in sources [:universes target]))
                               (get-in sources [:wants target])
                               (mapcat :produces (vals patterns))))
            static (vec (concat
                         (when-not id [{:reason :invalid-pattern-id :value (:pattern response)}])
                         (when id (receipt-reasons code-root id (:receipt response)))
                         (when-not (contains? (:produces interp) want)
                           [{:reason :does-not-produce-the-want :want want}])
                         (guard-reasons interp known)
                         (constraint-reasons interp constraints)))]
        (if (seq static)
          {:status :rejected :target target :want want :reasons static}
          (let [trial (-> sources
                          (assoc-in [:interpretations target :patterns id] interp)
                          (assoc-in [:interpretations target :receipts id] (:receipt response))
                          (update :candidates dissoc target))
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
