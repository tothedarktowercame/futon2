(ns futon2.aif.mission-reading
  "D11 part 5: what a mission does not state, computed at click time
  through the same ask channel as interpretations; never a refusal for a
  missing list (PROOF-2a 924a6820; Joe, 2026-09-24).

  Two readings:

    LOCATOR  a criterion with no stated verdict gets a checkable locator
             (C3-C6) that decides it, proposed by the answering seat and
             validated here. Its status is then OBSERVED each click by that
             check, like a stated verdict's. The observation contract keeps
             every token mechanically observable (WM-04; class J refused),
             so the reading computes the check, not a met/not-met judgement:
             the judgement would be an unmeasured class-J observation, and a
             check re-read each click cannot go stale.
    CRITERIA a mission with no criteria in a recognised form gets criteria
             extracted from its text, each cued to the lines it came from;
             every cue must be the mission's text at HEAD, or the reply is
             refused whole.

  Both publish into the machine-owned store beside interpretations
  (want-interpretation/default-store), bound to an issued request, with the
  answerer on the receipt. A stated verdict always wins: a locator reading
  is asked only for a criterion with none."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.want-interpretation :as wi])
  (:import [java.security MessageDigest]))

(def default-store wi/default-store)
(def locator-schema :wm/locator-response-v1)
(def criteria-schema :wm/criteria-response-v1)
(def constraints-schema :wm/constraints-response-v1)
(def checkable #{:C3 :C4 :C5 :C6 :C8})
(def class-fields {:C3 [:repo :sha :path] :C4 [:repo :sha :path :decl]
                   :C5 [:repo :sha :bundle-path :entry] :C6 [:repo :sha :path]
                   ;; C8's own check owns its locator rule (exactly one of
                   ;; :namespace or :command; :config only with :namespace);
                   ;; a locator it refuses comes back :check-refused
                   :C8 [:repo]})

(defn text-sha [^String text]
  (wi/content-id :text text))

(defn- sha1-12 [^String s]
  (let [d (.digest (MessageDigest/getInstance "SHA-1") (.getBytes s "UTF-8"))]
    (subs (apply str (map #(format "%02x" %) d)) 0 12)))

;; ---------------------------------------------------------------------------
;; Requests and prompts

(defn locator-request [target mission criterion]
  {:schema :wm/locator-request-v1 :kind :locator :target target
   :want {:token (:token criterion)}
   :criterion (select-keys criterion [:kind :line :phase :stated])
   :mission mission
   :asks (str "a checkable locator (C3 path exists, C4 declaration head starts a line, C5 registry entry, C6 witness reference, C8 registered passing run) whose observation decides this criterion. "
              "The locator may name evidence that does not exist yet: it then reads false now, and producing that evidence is the flight's work. "
              "The classes observe exactly: C3 that a path exists at a commit; C4 that a declaration head starts a line of a file; C5 that a contract-registry bundle entry exists with its clojure loci; C6 that an EDN witness {:repo :sha :entry} at a path references an existing commit/entry; C8 that the test registry holds a warrant for a named test namespace, or for a named command (a gate: bb, sh, lake build), pinned to the current content of its code and test paths, postcheck matched, no failures or errors. "
              "For a criterion about tests passing, use C8 with the test namespace: {:class :C8 :repo \"<dir under /home/joe/code>\" :namespace \"<test ns>\"}, the registry finding its newest run (add :config \"<registry record id test-registry-<64hex>>\" only to name one record). For a gate or a Lean build, use C8 with the exact command it runs, no :config: {:class :C8 :repo \"<dir>\" :command [\"bb\" \"<script>\" …]} or [\"lake\" \"build\" \"<target>\"]. Exactly one of :namespace or :command. It reads false until a run is registered at current content (no entry, moved content, another namespace or command, failures are all false, not refusals); registering that run is the flight's work. Do not propose a file whose mere existence would read true. "
              "Decline only if no checkable observation could ever decide it; ask questions if the criterion is unclear.")})

(defn criteria-request [target mission sections-read]
  {:schema :wm/criteria-request-v1 :kind :criteria :target target
   :want {:token :criteria}
   :mission mission :sections-read (vec sections-read)
   :asks "the mission's completion criteria as it states them (goal, done-when, outcome sentences, phase exits), each cued to the exact lines of the mission at HEAD, or a typed decline naming the sections read"})

(defn coverage-request
  "Ask whether FOUND (the criteria the reader lifted) cover the mission's
  done-definition: criteria stated elsewhere (Scope, Objective, boundary
  sentences) that never became bullets, scope-outs a faithful closure must
  not require, and anchor data (a file:line, a named artifact) the prose
  gives for a found criterion. Asked once per text even when criteria were
  found (FLIGHT-TARGET-D2 §7.2: an extractor lifts more than bullets)."
  [target mission mission-sha found]
  {:schema :wm/coverage-request-v1 :kind :coverage :target target
   :want {:token :coverage :mission-sha mission-sha}
   :mission mission :found (vec found)
   :asks "criteria the found list misses (cued), scope-outs (cued), anchors for found criteria (cued), or anchored questions; all may be empty"})

(defn constraints-request
  "Ask for the ordering dependencies the mission states in any form among
  its wants and facts: KNOWN is [{:token … :text …}], each want's criterion
  or fact's own line, so an edge names tokens the flight already has."
  [target mission mission-sha known]
  {:schema :wm/constraints-request-v1 :kind :constraints :target target
   :want {:token :constraints :mission-sha mission-sha}
   :mission mission :known (vec known)
   :asks (str "every ordering dependency the mission text states between these tokens (X only after Y, X requires Y, X closes only through Y), "
              "each as {:want X :requires Y :cue {:lines [first last] :quote \"exact mission text\"}}; none is a valid answer; "
              "if a stated dependency is unclear about which token it means, ask anchored questions instead")})

(defn prompt [issued]
  (let [schema (case (:kind issued) :locator locator-schema :constraints constraints-schema criteria-schema)
        coverage? (= :coverage (:kind issued))]
    (str "The War Machine asks for a reading (D11 part 5). Request " (:request-id issued) ":\n\n```edn\n"
         (pr-str issued) "\n```\n\nREPLY GRAMMAR: exactly one fenced ```edn block holding "
         (cond
           (= :constraints (:kind issued))
           (str "{:schema " schema " :constraints [{:want :token :requires :token :cue {:lines [first last] :quote \"exact text\"}} …] "
                ":questions [{:question \"…\" :span {:lines [a b] :quote \"…\"} :alternatives [\"…\" \"…\"]}] :by \"seat\"} (both may be empty)")
           coverage?
           (str "{:schema " schema " :criteria [{:statement \"a criterion the found list misses\" :cue {:lines [a b] :quote \"exact text\"}} …] "
                ":scope-outs [{:statement \"what a faithful closure must not require\" :cue {:lines [a b] :quote \"…\"}} …] "
                ":anchors [{:token :found-token :anchor \"file:line or artifact\" :cue {:lines [a b] :quote \"…\"}} …] "
                ":questions [anchored as usual] :by \"seat\"} (all may be empty: the found criteria cover it)")
           (= :locator (:kind issued))
           (str "{:schema " schema " :locator {:class :C3|:C4|:C5|:C6 :repo … :sha \"HEAD\" :path … (:decl for C4)} or {:class :C8 :repo … :namespace …} or {:class :C8 :repo … :command [argv …]} "
                ":cue {:quote \"words of the criterion this locator decides\"} :reading \"why observing it decides the criterion\" :by \"seat\"}, "
                "or, if the criterion is genuinely unclear, {:schema " schema " :questions [{:question \"…\" :span {:lines [first last] :quote \"exact mission text\"} :alternatives [\"reading A\" \"reading B\"]}] :by \"seat\"}")
           :else
           (str "{:schema " schema " :criteria [{:statement \"…\" :cue {:lines [first last] :quote \"exact text of those lines\"}} …] "
                ":questions [{:question \"…\" :span {:lines [first last] :quote \"exact text\"} :alternatives [\"reading A\" \"reading B\"]} …] :by \"seat\"} "
                "(criteria where the text is clear, questions anchored to the spans that are not; either may be empty but not both)"))
         " or {:schema " schema " :decline {:reason … :sections-read […]}}; anything else is unparseable.\n")))

;; ---------------------------------------------------------------------------
;; Validation

(defn- at-lines [lines {[a b] :lines}]
  (when (and (integer? a) (integer? b) (<= 1 a b (count lines)))
    (str/join "\n" (subvec lines (dec a) b))))

(defn question-reasons
  "Why QS are not good questions about TEXT: each needs a question, a span
  that is exactly the text at its lines, and at least two readings."
  [qs text]
  (let [lines (vec (str/split-lines (str text)))]
    (vec (for [{:keys [question span alternatives] :as q} qs
               :let [at (at-lines lines span)]
               :when (or (str/blank? question) (nil? at) (not= at (:quote span))
                         (< (count (remove str/blank? alternatives)) 2))]
           {:reason (cond (nil? span) :question-without-span
                          (or (nil? at) (not= at (:quote span))) :question-span-does-not-resolve
                          (str/blank? question) :question-not-stated
                          :else :question-without-alternatives)
            :question (select-keys q [:question])}))))

(defn- words
  "Text compared as words: whitespace runs and markdown code backticks do
  not count, so a cue quoting a criterion across its line breaks matches."
  [s]
  (-> (str s) (str/replace "`" "") (str/replace #"\s+" " ") str/trim))

(defn validate-locator
  "A locator reading for ISSUED is valid when its class is checkable, its
  fields are present, the check runs without refusing (OBSERVE, default
  observation-checks/observe), its cue quotes the criterion's own words and
  its reading is stated. Returns {:status :valid :locator … :observed bool}
  or {:status :rejected :reasons […]}."
  [issued response & [{:keys [observe text] :or {observe checks/observe}}]]
  (if (and (seq (:questions response)) (nil? (:locator response)))
    ;; the criterion is unclear: questions instead of a locator (Joe: good
    ;; questions logged, not bad work against a vague specification)
    (let [bad (question-reasons (:questions response) text)]
      (if (seq bad)
        {:status :rejected :reasons bad}
        {:status :questions :questions (mapv #(select-keys % [:question :span :alternatives]) (:questions response))}))
  (let [{:keys [locator cue reading]} response
        cls (:class locator)
        missing (remove #(and (string? (get locator %)) (not (str/blank? (get locator %))))
                        (get class-fields cls))
        stated (str (get-in issued [:criterion :stated]))
        static (cond-> []
                 (not (checkable cls)) (conj {:reason :class-not-checkable :class cls})
                 (and (checkable cls) (seq missing)) (conj {:reason :locator-fields-missing :missing (vec missing)})
                 (or (str/blank? (:quote cue)) (not (str/includes? (words stated) (words (:quote cue)))))
                 (conj {:reason :cue-not-in-criterion :quote (:quote cue)})
                 (str/blank? reading) (conj {:reason :reading-not-stated}))]
    (if (seq static)
      {:status :rejected :reasons static}
      (let [token (get-in issued [:want :token])
            r (observe {token locator})]
        (if-let [refused (get-in r [:refused token])]
          {:status :rejected :reasons [{:reason :check-refused :refusal refused}]}
          {:status :valid :locator locator :observed (contains? (:observed r) token)}))))))

(defn validate-criteria
  "A criteria reading is valid when it lists at least one criterion or one
  question, every criterion's cue is exactly the mission TEXT at its lines,
  and every question is anchored to a span that is exactly the text at its
  lines and states at least two alternative readings (Joe: a genuinely
  unclear mission is not a good target, but should yield good questions,
  not a refusal or bad work against a vague specification). One cue or span
  that does not resolve, or a question without a span, refuses the whole
  reply: a vague question about a vague mission is what this prevents. A
  reply may mix criteria and questions."
  [issued response text]
  (let [lines (vec (str/split-lines (str text)))
        target (:target issued)
        cs (:criteria response)
        qs (:questions response)
        bad-q (question-reasons qs text)
        bad (vec (for [{:keys [statement cue] :as c} cs
                       :let [[a b] (:lines cue)
                             at (when (and (integer? a) (integer? b) (<= 1 a b (count lines)))
                                  (str/join "\n" (subvec lines (dec a) b)))]
                       :when (or (str/blank? statement) (nil? at) (not= at (:quote cue)))]
                   {:reason :cue-does-not-resolve :criterion (select-keys c [:statement]) :lines (:lines cue)}))]
    (cond
      (and (empty? cs) (empty? qs)) {:status :rejected :reasons [{:reason :no-criteria}]}
      (or (seq bad) (seq bad-q)) {:status :rejected :reasons (into bad bad-q)}
      :else
      {:status :valid
       :questions (mapv #(select-keys % [:question :span :alternatives]) qs)
       :criteria (mapv (fn [{:keys [statement cue]}]
                         {:kind :extracted-criterion :line (first (:lines cue))
                          :stated (:quote cue) :statement statement :phase "EXTRACTED"
                          :token (keyword "exit" (str "h" (sha1-12 (str target "\n:extracted\n"
                                                                        (first (str/split-lines (:quote cue)))))))})
                       cs)})))

;; ---------------------------------------------------------------------------
;; Publication (same store, same issued-request binding as interpretations)

(defn- bound! [store request]
  (let [issued (wi/issued-request store (:request-id request))]
    (when-not (and issued (= (:target issued) (:target request)) (= (:want issued) (:want request)))
      (throw (ex-info "the reading is not bound to a request the machine issued"
                      {:interpretation/refusal :reading/request-not-issued :request-id (:request-id request)})))
    issued))

(defn publish-locator!
  [store issued response validated answered-by]
  (bound! store issued)
  (when-not (= :valid (:status validated))
    (throw (ex-info "only a validated reading is published" {:interpretation/refusal :reading/not-validated})))
  (let [target (:target issued) token (get-in issued [:want :token])
        prior (or (wi/read-published store target)
                  {:schema :wm/machine-interpretations-v1 :target target :patterns {} :receipts {} :records {}})
        rec (-> prior
                (assoc-in [:locators token]
                          {:locator (:locator validated)
                           :receipt {:kind :machine-read-locator :request-id (:request-id issued)
                                     :response-id (wi/content-id :response response)
                                     :cue (:cue response) :reading (:reading response)
                                     :answered-by answered-by :validator @wi/validator
                                     :observed-at-validation (:observed validated)}}))]
    (wi/write-atomic! (io/file store (str target ".edn")) rec)
    rec))

(defn publish-criteria!
  [store issued response validated answered-by mission-sha]
  (bound! store issued)
  (when-not (= :valid (:status validated))
    (throw (ex-info "only a validated reading is published" {:interpretation/refusal :reading/not-validated})))
  (let [target (:target issued)
        prior (or (wi/read-published store target)
                  {:schema :wm/machine-interpretations-v1 :target target :patterns {} :receipts {} :records {}})
        rec (assoc prior :criteria
                   {:criteria (:criteria validated) :questions (:questions validated)
                    :mission-sha mission-sha
                    :receipt {:kind :machine-read-criteria :request-id (:request-id issued)
                              :response-id (wi/content-id :response response)
                              :answered-by answered-by :validator @wi/validator}})]
    (wi/write-atomic! (io/file store (str target ".edn")) rec)
    rec))

(defn published-criteria
  "Published extracted criteria for TARGET whose cues still resolve in TEXT
  (a criterion whose lines moved or changed is dropped, and so asked again)."
  [store target text]
  (let [lines (vec (str/split-lines (str text)))]
    (vec (for [c (get-in (wi/read-published store target) [:criteria :criteria])
               :let [n (count (str/split-lines (:stated c)))
                     a (:line c) b (+ a n -1)]
               :when (and (<= 1 a b (count lines)) (= (:stated c) (str/join "\n" (subvec lines (dec a) b))))]
           c))))

(defn published-locators [store target]
  (into {} (for [[t {:keys [locator]}] (get-in (wi/read-published store target) [:locators])] [t locator])))

(defn published-questions [store target]
  (get-in (wi/read-published store target) [:criteria :questions] []))

(defn mission-owner
  "The owner a mission names on an **Owner:** line (first agent-like id), or nil."
  [text]
  (some->> (re-find #"(?m)^\*\*Owner:\*\*\s*(.*)$" (str text)) second
           (re-find #"(?:claude|codex|kimi|zai)-\d+")))

(defn question-prompt [target owner questions]
  (str "The War Machine read " target " and could not tell what it asks for at these spans. "
       "Answering by editing the mission text at each span lets a later reading find the answer there.\n\n```edn\n"
       (pr-str {:target target :owner owner :questions questions}) "\n```\n"))

(defn publish-locator-questions!
  "Record the questions a locator reading raised for ISSUED's criterion."
  [store issued response validated answered-by]
  (bound! store issued)
  (let [target (:target issued) token (get-in issued [:want :token])
        prior (or (wi/read-published store target)
                  {:schema :wm/machine-interpretations-v1 :target target :patterns {} :receipts {} :records {}})
        rec (assoc-in prior [:locator-questions token]
                      {:questions (:questions validated)
                       :receipt {:kind :machine-read-questions :request-id (:request-id issued)
                                 :response-id (wi/content-id :response response) :answered-by answered-by}})]
    (wi/write-atomic! (io/file store (str target ".edn")) rec)
    rec))

(defn published-locator-questions [store target]
  (into {} (for [[t {:keys [questions]}] (get-in (wi/read-published store target) [:locator-questions])] [t questions])))

(defn validate-constraints
  "A constraints reading is valid when every edge joins two distinct tokens
  the request listed, its cue is exactly the mission TEXT at its lines, and
  every question is anchored (question-reasons). One bad edge or question
  refuses the whole reply. An empty reply is valid: the text states none."
  [issued response text]
  (let [lines (vec (str/split-lines (str text)))
        known (set (map :token (:known issued)))
        bad (vec (for [{:keys [want requires cue] :as e} (:constraints response)
                       :let [at (at-lines lines cue)]
                       :when (or (not (known want)) (not (known requires)) (= want requires)
                                 (nil? at) (not= at (:quote cue)))]
                   {:reason (cond (not (and (known want) (known requires))) :edge-token-unknown
                                  (= want requires) :edge-to-itself
                                  :else :cue-does-not-resolve)
                    :edge (select-keys e [:want :requires])}))
        bad-q (question-reasons (:questions response) text)]
    (if (or (seq bad) (seq bad-q))
      {:status :rejected :reasons (into bad bad-q)}
      {:status :valid
       :constraints (mapv (fn [{:keys [want requires cue]}]
                            {:want want :requires requires :by :machine-reading
                             :line (first (:lines cue)) :quote (:quote cue)})
                          (:constraints response))
       :questions (mapv #(select-keys % [:question :span :alternatives]) (:questions response))})))

(defn publish-constraints!
  [store issued response validated answered-by]
  (bound! store issued)
  (when-not (= :valid (:status validated))
    (throw (ex-info "only a validated reading is published" {:interpretation/refusal :reading/not-validated})))
  (let [target (:target issued)
        prior (or (wi/read-published store target)
                  {:schema :wm/machine-interpretations-v1 :target target :patterns {} :receipts {} :records {}})
        rec (assoc prior :constraints-read
                   {:constraints (:constraints validated) :questions (:questions validated)
                    :mission-sha (get-in issued [:want :mission-sha])
                    :receipt {:kind :machine-read-constraints :request-id (:request-id issued)
                              :response-id (wi/content-id :response response)
                              :answered-by answered-by :validator @wi/validator}})]
    (wi/write-atomic! (io/file store (str target ".edn")) rec)
    rec))

(defn published-constraints
  "The constraints reading for TARGET, if it was read from this MISSION-SHA
  (a changed text is read again)."
  [store target mission-sha]
  (let [r (get-in (wi/read-published store target) [:constraints-read])]
    (when (and r (= mission-sha (:mission-sha r))) r)))

(defn validate-coverage
  "A coverage reading is valid when every added criterion, scope-out and
  anchor is cued to exactly the mission TEXT at its lines, every anchor
  names a found token, and every question is anchored. One bad item refuses
  the reply. All-empty is valid: the found criteria cover the mission."
  [issued response text]
  (let [lines (vec (str/split-lines (str text)))
        found (set (map :token (:found issued)))
        cue-bad (fn [kind items]
                  (for [{:keys [cue] :as it} items
                        :let [at (at-lines lines cue)]
                        :when (or (nil? at) (not= at (:quote cue))
                                  (and (= kind :anchor) (not (found (:token it)))))]
                    {:reason (if (and (= kind :anchor) (not (found (:token it)))) :anchor-token-not-found :cue-does-not-resolve)
                     :kind kind :item (select-keys it [:statement :token :anchor])}))
        bad (vec (concat (cue-bad :criterion (:criteria response))
                         (cue-bad :scope-out (:scope-outs response))
                         (cue-bad :anchor (:anchors response))
                         (question-reasons (:questions response) text)))
        target (:target issued)]
    (if (seq bad)
      {:status :rejected :reasons bad}
      {:status :valid
       :criteria (mapv (fn [{:keys [statement cue]}]
                         {:kind :extracted-criterion :line (first (:lines cue)) :stated (:quote cue)
                          :statement statement :phase "EXTRACTED"
                          :token (keyword "exit" (str "h" (sha1-12 (str target "\n:extracted\n"
                                                                        (first (str/split-lines (:quote cue)))))))})
                       (:criteria response))
       :scope-outs (mapv (fn [{:keys [statement cue]}] {:statement statement :line (first (:lines cue)) :quote (:quote cue)
                                                        :reason :scope-out})
                         (:scope-outs response))
       :anchors (into {} (for [{:keys [token anchor cue]} (:anchors response)]
                           [token {:anchor anchor :line (first (:lines cue)) :quote (:quote cue)}]))
       :questions (mapv #(select-keys % [:question :span :alternatives]) (:questions response))})))

(defn publish-coverage!
  [store issued response validated answered-by]
  (bound! store issued)
  (when-not (= :valid (:status validated))
    (throw (ex-info "only a validated reading is published" {:interpretation/refusal :reading/not-validated})))
  (let [target (:target issued)
        prior (or (wi/read-published store target)
                  {:schema :wm/machine-interpretations-v1 :target target :patterns {} :receipts {} :records {}})
        rec (assoc prior :coverage
                   (assoc (select-keys validated [:criteria :scope-outs :anchors :questions])
                          :mission-sha (get-in issued [:want :mission-sha])
                          :receipt {:kind :machine-read-coverage :request-id (:request-id issued)
                                    :response-id (wi/content-id :response response)
                                    :answered-by answered-by :validator @wi/validator}))]
    (wi/write-atomic! (io/file store (str target ".edn")) rec)
    rec))

(defn published-coverage [store target mission-sha]
  (let [r (:coverage (wi/read-published store target))]
    (when (and r (= mission-sha (:mission-sha r))) r)))

;; A locator reading the seat declined, recorded per text so an unchanged
;; mission is not asked again; the criterion stays a want that no check can
;; yet observe (a typed absence after the step ran).
(defn record-locator-decline! [store issued decline answered-by mission-sha]
  (let [target (:target issued)
        prior (or (wi/read-published store target)
                  {:schema :wm/machine-interpretations-v1 :target target :patterns {} :receipts {} :records {}})
        rec (assoc-in prior [:locator-declines (get-in issued [:want :token])]
                      {:decline decline :mission-sha mission-sha :request-id (:request-id issued)
                       :answered-by answered-by})]
    (wi/write-atomic! (io/file store (str target ".edn")) rec)
    rec))

(defn published-locator-declines [store target mission-sha]
  (into {} (for [[t d] (:locator-declines (wi/read-published store target))
                 :when (= mission-sha (:mission-sha d))]
             [t d])))
