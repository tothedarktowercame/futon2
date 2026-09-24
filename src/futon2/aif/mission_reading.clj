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
(def checkable #{:C3 :C4 :C5 :C6})
(def class-fields {:C3 [:repo :sha :path] :C4 [:repo :sha :path :decl]
                   :C5 [:repo :sha :bundle-path :entry] :C6 [:repo :sha :path]})

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
   :asks "a checkable locator (C3 path exists, C4 declaration head starts a line, C5 registry entry, C6 witness reference) whose observation decides this criterion, or a typed decline"})

(defn criteria-request [target mission sections-read]
  {:schema :wm/criteria-request-v1 :kind :criteria :target target
   :want {:token :criteria}
   :mission mission :sections-read (vec sections-read)
   :asks "the mission's completion criteria as it states them (goal, done-when, outcome sentences, phase exits), each cued to the exact lines of the mission at HEAD, or a typed decline naming the sections read"})

(defn prompt [issued]
  (let [schema (if (= :locator (:kind issued)) locator-schema criteria-schema)]
    (str "The War Machine asks for a reading (D11 part 5). Request " (:request-id issued) ":\n\n```edn\n"
         (pr-str issued) "\n```\n\nREPLY GRAMMAR: exactly one fenced ```edn block holding "
         (if (= :locator (:kind issued))
           (str "{:schema " schema " :locator {:class :C3|:C4|:C5|:C6 :repo … :sha \"HEAD\" :path … (:decl for C4)} "
                ":cue {:quote \"words of the criterion this locator decides\"} :reading \"why observing it decides the criterion\" :by \"seat\"}")
           (str "{:schema " schema " :criteria [{:statement \"…\" :cue {:lines [first last] :quote \"exact text of those lines\"}} …] "
                ":questions [{:question \"…\" :span {:lines [first last] :quote \"exact text\"} :alternatives [\"reading A\" \"reading B\"]} …] :by \"seat\"} "
                "(criteria where the text is clear, questions anchored to the spans that are not; either may be empty but not both)"))
         " or {:schema " schema " :decline {:reason … :sections-read […]}}; anything else is unparseable.\n")))

;; ---------------------------------------------------------------------------
;; Validation

(defn validate-locator
  "A locator reading for ISSUED is valid when its class is checkable, its
  fields are present, the check runs without refusing (OBSERVE, default
  observation-checks/observe), its cue quotes the criterion's own words and
  its reading is stated. Returns {:status :valid :locator … :observed bool}
  or {:status :rejected :reasons […]}."
  [issued response & [{:keys [observe] :or {observe checks/observe}}]]
  (let [{:keys [locator cue reading]} response
        cls (:class locator)
        missing (remove #(and (string? (get locator %)) (not (str/blank? (get locator %))))
                        (get class-fields cls))
        stated (str (get-in issued [:criterion :stated]))
        static (cond-> []
                 (not (checkable cls)) (conj {:reason :class-not-checkable :class cls})
                 (and (checkable cls) (seq missing)) (conj {:reason :locator-fields-missing :missing (vec missing)})
                 (or (str/blank? (:quote cue)) (not (str/includes? stated (:quote cue))))
                 (conj {:reason :cue-not-in-criterion :quote (:quote cue)})
                 (str/blank? reading) (conj {:reason :reading-not-stated}))]
    (if (seq static)
      {:status :rejected :reasons static}
      (let [token (get-in issued [:want :token])
            r (observe {token locator})]
        (if-let [refused (get-in r [:refused token])]
          {:status :rejected :reasons [{:reason :check-refused :refusal refused}]}
          {:status :valid :locator locator :observed (contains? (:observed r) token)})))))

(defn- at-lines [lines {[a b] :lines}]
  (when (and (integer? a) (integer? b) (<= 1 a b (count lines)))
    (str/join "\n" (subvec lines (dec a) b))))

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
        bad-q (vec (for [{:keys [question span alternatives] :as q} qs
                         :let [at (at-lines lines span)]
                         :when (or (str/blank? question) (nil? at) (not= at (:quote span))
                                   (< (count (remove str/blank? alternatives)) 2))]
                     {:reason (cond (nil? span) :question-without-span
                                    (or (nil? at) (not= at (:quote span))) :question-span-does-not-resolve
                                    (str/blank? question) :question-not-stated
                                    :else :question-without-alternatives)
                      :question (select-keys q [:question])}))
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
