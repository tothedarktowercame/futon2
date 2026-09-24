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

(deftest a-moved-criterion-refuses
  ;; bad case: the mission was edited after the want was read; the request
  ;; must not cite whatever now sits at the old line
  (let [{:keys [root opts]} (fixture)
        c (assoc (document-criterion) :line 5)]
    (is (= :want/criterion-moved
           (try (wi/request! {:target "M-test" :want (:token c) :criterion c :facts {} :patterns {}}
                             root (assoc opts :retrieve-fn (fn [_] [])))
                nil
                (catch clojure.lang.ExceptionInfo e
                  (:interpretation/refusal (ex-data e))))))))

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
        _ (wi/publish! store (req document) doc-resp doc)
        arg-resp (response :writing-coherence/plain-language-thesis)
        arg (validate argue arg-resp (wi/merge-published s0 store ["M-futon-seams"]))
        rec (wi/publish! store (req argue) arg-resp arg)
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
           (try (wi/publish! store (req argue) bad v) nil
                (catch clojure.lang.ExceptionInfo e (:interpretation/refusal (ex-data e))))))
    (is (nil? (wi/read-published store "M-futon-seams")))))

(deftest a-conflicting-reading-of-a-published-pattern-refuses
  (let [store (temp-store)
        resp (response :writing-coherence/meet-the-reader-where-they-are)
        v (validate document resp (seams-sources))
        _ (wi/publish! store (req document) resp v)
        other (update-in v [:interpretation :writing-coherence/meet-the-reader-where-they-are :guard :needs]
                         conj :exit/h66b2ffcf3e0b)]
    (is (= :want/conflicting-publication
           (try (wi/publish! store (req document) resp other) nil
                (catch clojure.lang.ExceptionInfo e (:interpretation/refusal (ex-data e))))))
    (testing "republishing the same reading is a no-op"
      (is (map? (wi/publish! store (req document) resp v))))))

(deftest a-hand-declaration-wins-over-a-published-reading
  (let [store (temp-store)
        resp (response :writing-coherence/meet-the-reader-where-they-are)
        v (validate document resp (seams-sources))
        _ (wi/publish! store (req document) resp v)
        declared {:guard {:needs #{instantiate} :forbids #{}} :produces #{document :exit/hdeclared}}
        merged (wi/merge-published
                (assoc-in (seams-sources) [:interpretations "M-futon-seams" :patterns
                                           :writing-coherence/meet-the-reader-where-they-are] declared)
                store ["M-futon-seams"])]
    (is (= declared (get-in merged [:interpretations "M-futon-seams" :patterns
                                    :writing-coherence/meet-the-reader-where-they-are])))
    (is (= [] (get-in merged [:machine-interpretations "M-futon-seams"])))))
