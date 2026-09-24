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
                (slurp "holes/labs/wm-contract/proof2/proposals/M-futon-seams-interpretations.edn")))

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

(def argue :exit/hac75428b9c97)
(def document :exit/h54d16050a9dc)
(def instantiate :exit/h4ef5c183bc55)
(def owner-constraint {:want argue :requires document :by "claude-1"
                       :reason "ARGUE closes only through DOCUMENT's outsider account; its negative finding is retained"})

(defn- response [id]
  (merge {:pattern id :receipt (get-in proposals [:interpretation-receipts id])}
         (get-in proposals [:patterns id])))

(defn- req [want] {:target "M-futon-seams" :want {:token want}})

(defn- validate [want resp sources]
  (wi/validate-response (req want) resp
                        {:sources sources :constraints [owner-constraint]
                         :admit #'futon2.report.war-machine/admit-cascade-problem}))

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
