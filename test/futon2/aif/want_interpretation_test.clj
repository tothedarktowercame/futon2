(ns futon2.aif.want-interpretation-test
  "D11 part 1: which wants need an interpretation, and a request that cites
  the exact criterion and retrieves with it as the query."
  (:require [clojure.edn]
            [clojure.java.io :as io]
            [futon2.aif.interpretation-construction]
            [futon2.aif.observation-checks]
            [futon2.report.war-machine]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.interpretation-request :as ireq]
            [futon2.aif.mission-criteria :as mc]
            [futon2.aif.want-interpretation :as wi])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def roots (atom []))
(use-fixtures :each
  (fn [f]
    (try (f) (finally
               (doseq [root @roots file (reverse (file-seq root))] (Files/delete (.toPath file)))
               (reset! roots [])))))

(def mission-text
  (str/join "\n" ["# M-test" "" "## MAP" ""
                  "**Exit criterion:** every MAP question has a concrete answer. **Met.**" ""
                  "## DOCUMENT" ""
                  "**Exit criterion:** someone browsing the docbook can discover what this"
                  "mission built. **Not started.**" ""
                  "## ARGUE" "" "Other text that must not be the query." ""]))

(defn- fixture []
  (let [root (.toFile (Files/createTempDirectory "want-interp" (make-array FileAttribute 0)))
        _ (swap! roots conj root)
        target (io/file root "M-test.md") code (io/file root "code.py") index (io/file root "index.json")]
    (spit target mission-text) (spit code "# retriever fixture") (spit index "[]")
    {:root root
     :opts {:resolve-fn (fn [_] {:id "M-test" :path (.getCanonicalPath target)})
            :revision-fn (constantly "fixture-revision") :library-fn (constantly [])
            :retriever-specs (mapv #(assoc % :implementation (.getCanonicalPath code)
                                            :index (.getCanonicalPath index)) ireq/retrievers)}}))

(defn- document-criterion []
  (first (filter #(= "DOCUMENT" (:phase %)) (mc/criteria "M-test" mission-text))))

(deftest unproduced-wants-are-open-and-unproduced
  (is (= [:c] (wi/unproduced-wants [:a :b :c] {:a true :b false :c false}
                                   {:p/x {:produces #{:b}}})))
  (is (= [] (wi/unproduced-wants [:a] {:a true} {}))))

(deftest request-carries-token-criterion-and-context
  (let [c (document-criterion)
        r (wi/request {:target "M-test" :want (:token c) :criterion c
                       :facts {(:token c) false} :patterns {:p/x {:guard {:needs #{} :forbids #{}}
                                                              :produces #{:y} :receipt {}}}})]
    (is (= :wm/want-interpretation-request-v1 (:schema r)))
    (is (= (:token c) (get-in r [:want :token])))
    (is (str/starts-with? (get-in r [:want :criterion :stated]) "**Exit criterion:** someone browsing"))
    (is (= {:p/x {:guard {:needs #{} :forbids #{}} :produces #{:y}}}
           (get-in r [:context :interpretations])) "receipts are not copied into the request")))

(deftest request!-retrieves-with-the-criterion-as-the-query
  (let [{:keys [root opts]} (fixture)
        c (document-criterion)
        queries (atom [])
        r (wi/request! {:target "M-test" :want (:token c) :criterion c :facts {} :patterns {}}
                       root (assoc opts :retrieve-fn (fn [q] (swap! queries conj (:query q))
                                                       [{:pattern "family/example" :score 1}])))]
    (is (= 2 (count @queries)))
    (testing "the query is the criterion at its lines, not the mission's other text"
      (is (every? #(str/starts-with? % "**Exit criterion:** someone browsing the docbook") @queries))
      (is (not-any? #(str/includes? % "must not be the query") @queries)))
    (is (= [9 10] (get-in r [:retrieval :target :citations 0 :lines])))
    (is (= :want-criterion (get-in r [:retrieval :target :tension-rule])))))

(defn- refusal-of [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:interpretation/refusal (ex-data e)))))

(deftest a-criterion-moved-by-an-edit-above-it-is-relocated
  ;; kimi-2's read of e08d0832: an insertion above the criterion moved it
  ;; without changing it, and the request refused; it now re-locates by text
  (let [c (document-criterion)
        shifted (str "# M-test\n\nAn inserted paragraph.\nAnd another line.\n"
                     (subs mission-text (count "# M-test\n")))
        cit (wi/citation-for "src" shifted c)]
    (is (= [(+ 3 (:line c)) (+ 4 (:line c))] (:lines cit)) "three lines inserted above")
    (is (= (:line c) (:relocated-from cit)))
    (is (str/starts-with? (:quote cit) "**Exit criterion:** someone browsing"))))

(deftest a-criterion-that-is-gone-or-duplicated-refuses
  (let [c (document-criterion)]
    (testing "altered: the stated text no longer occurs"
      (is (= :want/criterion-absent
             (refusal-of #(wi/citation-for "src" (str/replace mission-text "browsing the docbook" "reading the docbook") c)))))
    (testing "duplicated: two occurrences cannot say which was meant"
      (is (= :want/criterion-ambiguous
             (refusal-of #(wi/citation-for "src" (str mission-text "\n" (:stated c) "\n") c)))))
    (testing "through request! as well"
      (let [{:keys [root opts]} (fixture)]
        (is (= :want/criterion-ambiguous
               (refusal-of #(let [f (io/file (.getPath root) "M-test.md")]
                              (spit f (str mission-text "\n" (:stated c) "\n"))
                              (wi/request! {:target "M-test" :want (:token c) :criterion c
                                            :facts {} :patterns {}}
                                           root (assoc opts :retrieve-fn (fn [_] [])))))))
        (is (= :want/criterion-absent
               (refusal-of #(wi/request! {:target "M-test" :want (:token c)
                                          :criterion (assoc c :stated "**Exit criterion:** not in the text")
                                          :facts {} :patterns {}}
                                         root (assoc opts :retrieve-fn (fn [_] []))))))))))

;; ---------------------------------------------------------------------------
;; Part 2: validation, on the first flight's real case. Mission text: the
;; M-futon-seams pin at futon3c d05cb755; responses: kimi-6's proposals
;; (futon2 66a1779e); library files and admission are the real ones.

(def seams-text (slurp "test/fixtures/mission-criteria/M-futon-seams@futon3c-d05cb755.md"))
(def proposals (clojure.edn/read-string
                (slurp "test/fixtures/want-interp-library/M-futon-seams-interpretations@futon2-78439f58.edn")))

(defn- seams-sources []
  (let [w (mc/wants (mc/criteria "M-futon-seams" seams-text)
                    {:repo "futon3c" :path "holes/missions/M-futon-seams.md"
                     :observe #(futon2.aif.observation-checks/decl-present? seams-text (:decl %))})]
    {:universes {"M-futon-seams" (:universe w)}
     :wants {"M-futon-seams" (:wants w)}
     :locators {"M-futon-seams" (:locators w)}
     :interpretations {"M-futon-seams" {:patterns {} :receipts {}}}
     :horizon-steps 4
     :beta-by-context {:WM {:beta 1}} :context-of (constantly :WM)
     :construction {:construct futon2.aif.interpretation-construction/construct
                    :budget {:max-moves 4 :max-expansions 20000} :move-cost 0
                    :evaluate-g futon2.report.war-machine/constructed-candidate-g}}))

(def library-root
  "Pinned copies of the library files the first flight's readings cite
  (futon3/library/writing-coherence/{meet-the-reader-where-they-are,
  plain-language-thesis}.flexiarg), laid out under futon3/library/ so the
  receipts' paths resolve unchanged."
  (.getCanonicalPath (io/file "test/fixtures/want-interp-library")))

(def argue :exit/hac75428b9c97)
(def document :exit/h54d16050a9dc)
(def instantiate :exit/h4ef5c183bc55)
(def owner-constraint {:want argue :requires document :by "claude-1"
                       :reason "ARGUE closes only through DOCUMENT's outsider account; its negative finding is retained"})

(defn- response [id]
  ;; the pinned proposal (futon2 78439f58) carries no :forces and validates
  ;; as recorded: :forces is an optional note, never required
  (merge {:pattern id :receipt (get-in proposals [:interpretation-receipts id])}
         (get-in proposals [:patterns id])))

(defn- req [want] {:target "M-futon-seams" :want {:token want}})

(defn- validate [want resp sources]
  (wi/validate-response (req want) resp
                        {:sources sources :constraints [owner-constraint]
                         :admit #'futon2.report.war-machine/admit-cascade-problem
                         ;; the two library files' bytes, pinned: the receipt
                         ;; sha check must not depend on live futon3
                         :code-root library-root}))

(defn- admit-into [sources {:keys [interpretation receipt]}]
  (let [[id interp] (first interpretation)]
    (-> sources
        (assoc-in [:interpretations "M-futon-seams" :patterns id] interp)
        (assoc-in [:interpretations "M-futon-seams" :receipts id] receipt))))

(deftest the-first-flights-two-interpretations-validate-in-order
  (let [s0 (seams-sources)
        doc (validate document (response :writing-coherence/meet-the-reader-where-they-are) s0)]
    (is (= :valid (:status doc)) (pr-str (:reasons doc)))
    (is (= [:writing-coherence/meet-the-reader-where-they-are] (get-in doc [:candidate :precedence])))
    (testing "ARGUE validates once DOCUMENT's interpretation is admitted"
      (let [arg (validate argue (response :writing-coherence/plain-language-thesis) (admit-into s0 doc))]
        (is (= :valid (:status arg)) (pr-str (:reasons arg)))
        (is (= [:writing-coherence/meet-the-reader-where-they-are :writing-coherence/plain-language-thesis]
               (get-in arg [:candidate :precedence])))))))

(deftest argue-before-document-has-no-route
  (let [arg (validate argue (response :writing-coherence/plain-language-thesis) (seams-sources))]
    (is (= :rejected (:status arg)))))

(deftest bad-responses-are-rejected-with-reasons
  (let [s0 (seams-sources)
        good (response :writing-coherence/plain-language-thesis)
        reasons (fn [resp want] (set (map :reason (:reasons (validate want resp s0)))))]
    (testing "claude-1's condition: ARGUE by any route other than through DOCUMENT"
      (is (contains? (reasons (assoc-in good [:guard :needs] #{instantiate}) argue)
                     :owner-constraint-violated)))
    (is (contains? (reasons (assoc good :produces #{document}) argue) :does-not-produce-the-want))
    (is (contains? (reasons (assoc-in good [:receipt :source :sha256] "0000") argue) :source-sha-mismatch))
    (is (contains? (reasons (assoc good :pattern :writing-coherence/no-such-pattern) argue)
                   :source-not-the-pattern-file))
    (is (contains? (reasons (assoc-in good [:guard :needs] #{document :exit/hunknown}) argue)
                   :guard-token-unknown))
    (is (contains? (reasons (assoc good :pattern "no-namespace") argue) :invalid-pattern-id))
    (is (contains? (reasons (update good :receipt dissoc :scope-limit) argue) :receipt-incomplete))))

(deftest a-decline-is-recorded-not-rejected
  (is (= :declined (:status (validate argue {:decline {:reason :no-library-pattern}} (seams-sources))))))

;; ---------------------------------------------------------------------------
;; H-INTERP-D gaps 1 and 2: one grammar — hand-unit keys normalised at
;; intake; :forces optional (carried as a note when supplied, never required).

(def placenta-unit
  "claude-1's gauntlet/placenta-transfer hand unit, quoted verbatim from
  futon3c holes/labs/M-futon-seams/proto/instance-4.edn (the receipt's
  sha256 is the live library file's; its bytes are pinned under
  test/fixtures/want-interp-library/futon3/library/gauntlet/)."
  {:guard {:needs #{:sites-enumerated :one-producer} :forbids #{}}
   :produces #{:caller-converted}
   :receipt {:source {:path "futon3/library/gauntlet/placenta-transfer.flexiarg"
                      :sha256 "9771eca50e93c42de6b1ea22e188c770635d62830ca190f5ae4ca18056a069cd"}
             :reading "Its conclusion IS the conversion move: identify which functions the human is performing as a surrogate for missing infrastructure and transfer them one at a time to the system. The function here is resolving which seat plays a role, and the mission records Joe performing it by hand four times in one evening. Converting a caller is that transfer, once."
             :scope "The pattern's wider list of AIF functions the operator carries does not transfer; only the identify-and-move-one discipline is used."
             :author "claude-1"}
   :forces "The human is currently performing multiple AIF functions simultaneously, as a surrogate for infrastructure that does not exist yet."})

(defn- hand-sources []
  {:universes {"M-hand" {:sites-enumerated true :one-producer true :caller-converted false}}
   :wants {"M-hand" [:caller-converted]}
   :locators {"M-hand" {:sites-enumerated {:class :C3 :stated "sites enumerated"}
                        :one-producer {:class :C3 :stated "one producer"}
                        :caller-converted {:class :C3 :stated "the caller is converted"}}}
   :interpretations {"M-hand" {:patterns {} :receipts {}}}
   :horizon-steps 4
   :beta-by-context {:WM {:beta 1}} :context-of (constantly :WM)
   :construction {:construct futon2.aif.interpretation-construction/construct
                  :budget {:max-moves 4 :max-expansions 20000} :move-cost 0
                  :evaluate-g futon2.report.war-machine/constructed-candidate-g}})

(defn- validate-hand [resp]
  (wi/validate-response {:target "M-hand" :want {:token :caller-converted}} resp
                        {:sources (hand-sources) :constraints []
                         :admit #'futon2.report.war-machine/admit-cascade-problem
                         :code-root library-root}))

(deftest a-hand-unit-validates-with-its-keys-normalised
  (let [v (validate-hand (assoc placenta-unit :pattern :gauntlet/placenta-transfer))]
    (is (= :valid (:status v)) (pr-str (:reasons v)))
    (is (= (:scope (:receipt placenta-unit)) (get-in v [:receipt :scope-limit]))
        ":scope-limit populated from :scope")
    (is (= "claude-1" (get-in v [:receipt :by])) ":by populated from :author")
    (is (nil? (get-in v [:receipt :scope])) "the alias spelling is gone")
    (is (= (:forces placenta-unit)
           (get-in v [:interpretation :gauntlet/placenta-transfer :forces]))
        "the interpretation record carries the forces")))

(deftest a-reply-without-forces-validates
  ;; Joe, 2026-09-25 (relayed by claude-10): a required field that is only
  ;; tested non-blank is red tape; the receipt's :reading and source sha are
  ;; the application statement and its pin. Nothing is refused for a
  ;; missing :forces.
  (let [v (validate-hand (-> placenta-unit (dissoc :forces)
                             (assoc :pattern :gauntlet/placenta-transfer)))]
    (is (= :valid (:status v)) (pr-str (:reasons v)))
    (is (not-any? #(= :forces-required (:reason %)) (:reasons v)))
    (is (nil? (get-in v [:interpretation :gauntlet/placenta-transfer :forces])))))

(deftest conflicting-key-spellings-are-refused-not-merged
  (let [v (validate-hand (-> placenta-unit
                             (assoc :pattern :gauntlet/placenta-transfer)
                             (assoc-in [:receipt :scope-limit] "a different limit")))]
    (is (= :rejected (:status v)))
    (is (some #(= :receipt-key-conflict (:reason %)) (:reasons v)))
    (is (not-any? #(= :receipt-incomplete (:reason %)) (:reasons v))
        "the conflict is reported as a conflict, not as a missing canonical key")))

;; ---------------------------------------------------------------------------
;; Part 3: publication, and the next tick constructing from it

(defn- temp-store []
  (let [d (.toFile (Files/createTempDirectory "wm-interp-store" (make-array FileAttribute 0)))]
    (swap! roots conj d) (.getCanonicalPath d)))

(deftest published-interpretations-construct-on-the-next-tick
  (let [store (temp-store)
        s0 (seams-sources)
        doc-resp (response :writing-coherence/meet-the-reader-where-they-are)
        doc (validate document doc-resp s0)
        _ (wi/publish! store (wi/issue! store (req document)) doc-resp doc)
        arg-resp (response :writing-coherence/plain-language-thesis)
        arg (validate argue arg-resp (wi/merge-published s0 store ["M-futon-seams"]))
        rec (wi/publish! store (wi/issue! store (req argue)) arg-resp arg)
        {:keys [problems refusals]}
        (futon2.report.war-machine/assemble-cascade-problems-with-published
         store {:targets ["M-futon-seams"] :sources s0})]
    (is (= :valid (:status arg)) "ARGUE validates against the published DOCUMENT reading")
    (is (= #{:writing-coherence/meet-the-reader-where-they-are :writing-coherence/plain-language-thesis}
           (set (keys (:patterns rec)))))
    (is (= :machine-requested (get-in rec [:receipts :writing-coherence/plain-language-thesis :kind])))
    (is (= 4 (count (:records rec))) "two requests and two responses kept whole")
    (is (empty? refusals) (pr-str refusals))
    (is (= [[:writing-coherence/meet-the-reader-where-they-are :writing-coherence/plain-language-thesis]]
           (get-in (first problems) [:cascade-problem :precedences])))))

(deftest only-validated-responses-publish
  (let [store (temp-store)
        bad (assoc (response :writing-coherence/plain-language-thesis) :produces #{document})
        v (validate argue bad (seams-sources))]
    (is (= :want/not-validated
           (try (wi/publish! store (wi/issue! store (req argue)) bad v) nil
                (catch clojure.lang.ExceptionInfo e (:interpretation/refusal (ex-data e))))))
    (is (nil? (wi/read-published store "M-futon-seams")))))

(deftest a-conflicting-reading-of-a-published-pattern-refuses
  (let [store (temp-store)
        resp (response :writing-coherence/meet-the-reader-where-they-are)
        v (validate document resp (seams-sources))
        _ (wi/publish! store (wi/issue! store (req document)) resp v)
        other (update-in v [:interpretation :writing-coherence/meet-the-reader-where-they-are :guard :needs]
                         conj :exit/h66b2ffcf3e0b)]
    (is (= :want/conflicting-publication
           (try (wi/publish! store (wi/issue! store (req document)) resp other) nil
                (catch clojure.lang.ExceptionInfo e (:interpretation/refusal (ex-data e))))))
    (testing "republishing the same reading is a no-op"
      (is (map? (wi/publish! store (wi/issue! store (req document)) resp v))))))

(deftest a-hand-declaration-wins-over-a-published-reading
  (let [store (temp-store)
        resp (response :writing-coherence/meet-the-reader-where-they-are)
        v (validate document resp (seams-sources))
        _ (wi/publish! store (wi/issue! store (req document)) resp v)
        declared {:guard {:needs #{instantiate} :forbids #{}} :produces #{document :exit/hdeclared}}
        merged (wi/merge-published
                (assoc-in (seams-sources) [:interpretations "M-futon-seams" :patterns
                                           :writing-coherence/meet-the-reader-where-they-are] declared)
                store ["M-futon-seams"])]
    (is (= declared (get-in merged [:interpretations "M-futon-seams" :patterns
                                    :writing-coherence/meet-the-reader-where-they-are])))
    (is (= [] (get-in merged [:machine-interpretations "M-futon-seams"])))
    (testing "the overruled published reading stays visible (AR-39)"
      (let [[o] (get-in merged [:machine-interpretations-overridden "M-futon-seams"])]
        (is (= :writing-coherence/meet-the-reader-where-they-are (:id o)))
        (is (not= (:published-sha o) (:declared-sha o)))
        (is (string? (:request-id o)))))
    (testing "a hand declaration identical to the published reading is not an override"
      (let [same (get-in (wi/read-published store "M-futon-seams")
                         [:patterns :writing-coherence/meet-the-reader-where-they-are])
            m (wi/merge-published
               (assoc-in (seams-sources) [:interpretations "M-futon-seams" :patterns
                                          :writing-coherence/meet-the-reader-where-they-are] same)
               store ["M-futon-seams"])]
        (is (nil? (get-in m [:machine-interpretations-overridden "M-futon-seams"])))))))

(deftest a-string-spelled-id-canonicalises
  ;; D17 sibling of the un-namespaced refusal: the same proposal with its
  ;; id as a namespaced string validates under the canonical keyword
  (let [resp (assoc (response :writing-coherence/meet-the-reader-where-they-are)
                    :pattern "writing-coherence/meet-the-reader-where-they-are")
        v (validate document resp (seams-sources))]
    (is (= :valid (:status v)) (pr-str (:reasons v)))
    (is (= [:writing-coherence/meet-the-reader-where-they-are] (keys (:interpretation v))))))

(deftest only-an-answer-to-an-issued-request-publishes
  (let [store (temp-store)
        resp (response :writing-coherence/meet-the-reader-where-they-are)
        v (validate document resp (seams-sources))
        refusal (fn [request] (try (wi/publish! store request resp v) nil
                                   (catch clojure.lang.ExceptionInfo e (:interpretation/refusal (ex-data e)))))]
    (testing "a request the machine never issued"
      (is (= :want/request-not-issued (refusal (assoc (req document) :request-id "request-0000")))))
    (testing "an issued request for a different want"
      (is (= :want/request-not-issued (refusal (wi/issue! store (req argue))))))
    (is (nil? (wi/read-published store "M-futon-seams")))
    (testing "the issued request for this want publishes, and the receipt names the validator"
      (let [rec (wi/publish! store (wi/issue! store (req document)) resp v)
            r (get-in rec [:receipts :writing-coherence/meet-the-reader-where-they-are])]
        (is (= "futon2.aif.want-interpretation" (get-in r [:validator :ns])))
        (is (re-matches #"[0-9a-f]{64}" (str (get-in r [:validator :source-sha256]))))
        (is (string? (:request-id r)))))))

;; ---------------------------------------------------------------------------
;; Part 4: the prompt, and agent-search retrieval in the response (H-INTERP-D
;; §3 gap 3: the pattern a reviewer chose was outside what the seat was
;; shown; the seat may now search the library and append its own runs).

(deftest the-prompt-offers-agent-search-over-the-library
  (let [{:keys [root opts]} (fixture)
        c (document-criterion)
        store (temp-store)
        r (wi/request! {:target "M-test" :want (:token c) :criterion c :facts {} :patterns {}}
                       root (assoc opts :retrieve-fn (fn [_] [{:pattern "family/example" :score 1}])))
        issued (wi/issue! store r)
        text (wi/prompt issued {:library-root library-root})]
    (is (str/includes? text "agent-search"))
    (is (str/includes? text library-root))
    (is (str/includes? text "outside futon3/library/"))))

;; the bytes are a pinned copy of futon3/library/gauntlet/placenta-transfer
;; .flexiarg; the sha256 is of the live file's bytes at capture time
(def placenta-sha256 "9771eca50e93c42de6b1ea22e188c770635d62830ca190f5ae4ca18056a069cd")

(defn- agent-search-runs [candidates]
  {:runs [{:retriever "agent-search" :candidates candidates}]})

(deftest an-agent-search-candidate-naming-a-real-library-file-validates
  ;; the unit claude-1 chose for instance 4 was outside the machine's 48
  ;; retrieval candidates; appended this way the response validates
  (let [resp (assoc (response :writing-coherence/meet-the-reader-where-they-are)
                    :retrieval (agent-search-runs
                                [{:pattern "gauntlet/placenta-transfer"
                                  :source {:path "futon3/library/gauntlet/placenta-transfer.flexiarg"
                                           :sha256 placenta-sha256}}]))
        v (validate document resp (seams-sources))]
    (is (= :valid (:status v)) (pr-str (:reasons v)))))

(deftest bad-appended-runs-are-rejected-with-typed-reasons
  (let [base (response :writing-coherence/meet-the-reader-where-they-are)
        reasons (fn [retrieval]
                  (set (map :reason (:reasons (validate document (assoc base :retrieval retrieval)
                                                        (seams-sources))))))]
    (testing "any other retriever name"
      (is (contains? (reasons {:runs [{:retriever "grep" :candidates []}]})
                     :appended-run-not-agent-search)))
    (testing "a candidate path outside the library root"
      (is (contains? (reasons (agent-search-runs
                               [{:pattern "gauntlet/placenta-transfer"
                                 :source {:path "futon3/not-the-library/placenta-transfer.flexiarg"
                                          :sha256 placenta-sha256}}]))
                     :appended-candidate-outside-library))
      (is (contains? (reasons (agent-search-runs
                               [{:pattern "gauntlet/placenta-transfer"
                                 :source {:path "futon3/library/../../../etc/passwd"}}]))
                     :appended-candidate-outside-library)))
    (testing "a declared sha256 that is not the file's bytes"
      (is (contains? (reasons (agent-search-runs
                               [{:pattern "gauntlet/placenta-transfer"
                                 :source {:path "futon3/library/gauntlet/placenta-transfer.flexiarg"
                                          :sha256 "0000"}}]))
                     :appended-source-sha-mismatch)))
    (testing "a candidate that locates nothing"
      (is (contains? (reasons (agent-search-runs [{:score 3}]))
                     :appended-candidate-unlocatable)))
    ;; review bad cases (claude-8, 2026-09-25): each passed before the fix
    (testing "a sibling directory sharing the library's name as a prefix is outside it"
      (is (contains? (reasons (agent-search-runs
                               [{:pattern "x" :source {:path "futon3/library-old/x.flexiarg"}}]))
                     :appended-candidate-outside-library)))
    (testing "a pattern id whose library file does not exist locates nothing"
      (is (contains? (reasons (agent-search-runs [{:pattern "gauntlet/no-such-pattern"}]))
                     :appended-candidate-unlocatable))
      (is (contains? (reasons (agent-search-runs
                               [{:pattern "gauntlet/x"
                                 :source {:path "futon3/library/gauntlet/nope.flexiarg" :sha256 "00"}}]))
                     :appended-candidate-unlocatable)))
    (testing "an absolute path refuses rather than throwing"
      (is (contains? (reasons (agent-search-runs [{:pattern "x" :source {:path "/etc/passwd"}}]))
                     :appended-candidate-outside-library)))))
