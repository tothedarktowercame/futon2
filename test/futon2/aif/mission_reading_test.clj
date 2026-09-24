(ns futon2.aif.mission-reading-test
  "D11 part 5: a mission is never refused for a missing list. Criteria with
  no stated verdict get machine locators (observed each click); a mission
  with no criteria in a recognised form gets criteria extracted from its
  text, every cue resolving. The known case is M-f11 at futon2 22fa0da9:
  six Acceptance criteria, none with a verdict."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.mission-reading :as mr]
            [futon2.aif.want-interpretation :as wi])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def roots (atom []))
(use-fixtures :each
  (fn [f] (try (f) (finally
                     (doseq [root @roots file (reverse (file-seq root))] (Files/delete (.toPath file)))
                     (reset! roots [])))))

(defn- store []
  (let [d (.toFile (Files/createTempDirectory "reading-store" (make-array FileAttribute 0)))]
    (swap! roots conj d) (.getCanonicalPath d)))

(def f11-text (slurp "test/fixtures/mission-criteria/M-f11-find-production-successor@futon2-22fa0da9.md"))
(def target "M-f11-find-production-successor")

;; a stubbed check: C4 locators whose :decl starts with "OBSERVED" read true
(defn- observe [locs]
  (reduce-kv (fn [acc t l]
               (if (#{:C3 :C4 :C5 :C6} (:class l))
                 (cond-> (assoc-in acc [:results t] {})
                   (str/starts-with? (str (:decl l)) "OBSERVED") (update :observed conj t))
                 (assoc-in acc [:refused t] {:status :missing :kind :no-mechanical-check})))
             {:observed #{} :results {} :refused {}} locs))

(defn- f11-flight [s]
  (flight/start {:target target :chosen-because {:kind :requested}}
                {:kind :a-exits :repo "futon2" :path "holes/missions/M-f11-find-production-successor.md"
                 :store s :read-text (fn [& _] f11-text)
                 :observe #(contains? (:observed (observe {::t %})) ::t)}
                {:id "flight-f11"}))

(defn- locator-reply [issued]
  (let [stated (get-in issued [:criterion :stated])
        words (subs (str/replace stated #"^- " "") 0 (min 20 (count (str/replace stated #"^- " ""))))]
    (str "```edn\n"
         (pr-str {:schema mr/locator-schema
                  :locator {:class :C4 :repo "futon2" :sha "HEAD" :path "holes/missions/x.md"
                            :decl (str "NOT-YET " (get-in issued [:want :token]))}
                  :cue {:quote words} :reading "the check names the artifact the criterion asks for" :by "kimi-6"})
         "\n```")))

(defn- answer-with [f]
  (let [n (atom 0)]
    (fn [issued] {:seat "kimi-6" :job-id (str "job-" (swap! n inc)) :state "done" :text (f issued)})))

(deftest validate-locator-cases
  (let [issued {:kind :locator :target target :want {:token :exit/hx}
                :criterion {:stated "- Discharge or amend the `find` sorry."}}
        good {:locator {:class :C4 :repo "futon2" :sha "HEAD" :path "p" :decl "OBSERVED x"}
              :cue {:quote "amend the `find` sorry"} :reading "r"}
        v #(mr/validate-locator issued % {:observe observe})
        reasons #(set (map :reason (:reasons (v %))))]
    (is (= {:status :valid :locator (:locator good) :observed true} (v good)))
    (testing "a judgement class is not a checkable locator"
      (is (contains? (reasons (assoc-in good [:locator :class] :J)) :class-not-checkable)))
    (is (contains? (reasons (update good :locator dissoc :decl)) :locator-fields-missing))
    (is (contains? (reasons (assoc-in good [:cue :quote] "words not in the criterion")) :cue-not-in-criterion))
    (is (contains? (reasons (assoc good :reading "")) :reading-not-stated))
    (testing "a check that refuses (an unknown sha, say) is not a locator"
      (is (= [{:reason :check-refused :refusal {:status :missing :kind :unknown-sha}}]
             (:reasons (mr/validate-locator issued good
                                            {:observe (fn [m] {:observed #{} :results {}
                                                               :refused {(ffirst m) {:status :missing :kind :unknown-sha}}})})))))))

(deftest validate-criteria-cases
  (let [text "# M\n\n## Goal\n\nThe docbook lists this mission.\nIt links the page.\n"
        issued {:kind :criteria :target "M"}
        ok {:criteria [{:statement "docbook lists it" :cue {:lines [5 6] :quote "The docbook lists this mission.\nIt links the page."}}]}]
    (is (= :valid (:status (mr/validate-criteria issued ok text))))
    (is (= :extracted-criterion (get-in (mr/validate-criteria issued ok text) [:criteria 0 :kind])))
    (testing "one cue that does not resolve refuses the whole reply"
      (is (= :rejected (:status (mr/validate-criteria issued (update ok :criteria conj {:statement "x" :cue {:lines [3 3] :quote "## Not the text"}}) text)))))
    (is (= [{:reason :no-criteria}] (:reasons (mr/validate-criteria issued {:criteria []} text))))))

(deftest m-f11-is-not-refused-its-criteria-get-locators
  (let [s (store)
        f (f11-flight s)
        before (flight/click-wants f {})
        read ((fr/read-fn {:store s :answer-fn (answer-with locator-reply) :observe observe}) f {})
        after (flight/click-wants f {})]
    (testing "before: six criteria, all unlocated, six locator readings needed"
      (is (= 6 (count (:wants before))))
      (is (= 6 (count (get-in before [:source :readings-needed :locators])))))
    (is (= (repeat 6 :published) (map :outcome (:asked read))))
    (is (empty? (:needs read)))
    (testing "after: every criterion located by a machine locator and observed; nothing left to refuse"
      (is (= [] (get-in after [:source :unlocated])))
      (is (= 6 (count (get-in after [:source :machine-located]))))
      (is (every? false? (map #(get (:universe after) %) (:wants after)))))
    (is (= "kimi-6" (get-in (wi/read-published s target)
                            [:locators (first (:wants after)) :receipt :answered-by :seat])))))

(deftest a-stated-verdict-is-never-asked-for-a-locator
  (let [text (slurp "test/fixtures/mission-criteria/M-futon-seams@futon3c-071dee27.md")
        f (flight/start {:target "M-futon-seams" :chosen-because {:kind :requested}}
                        {:kind :a-exits :repo "futon3c" :path "p" :store (store) :read-text (fn [& _] text)
                         :observe (constantly false)} {:id "f"})]
    (is (= {:criteria? false :locators []}
           (select-keys (get-in (flight/click-wants f {}) [:source :readings-needed]) [:criteria? :locators])))))

(def bare-text
  (str/join "\n" ["# M-bare" "" "## Motivation" "" "We want the docbook to list every mission."
                  "" "## Notes" "" "Other text." ""]))

(deftest a-mission-with-no-recognised-criteria-gets-them-read
  (let [s (store)
        f (flight/start {:target "M-bare" :chosen-because {:kind :requested}}
                        {:kind :a-exits :repo "futon2" :path "p" :store s :read-text (fn [& _] bare-text)
                         :observe #(contains? (:observed (observe {::t %})) ::t)} {:id "f-bare"})
        replies (fn [issued]
                  (if (= :criteria (:kind issued))
                    (str "```edn\n" (pr-str {:schema mr/criteria-schema :by "kimi-6"
                                             :criteria [{:statement "the docbook lists every mission"
                                                         :cue {:lines [5 5] :quote "We want the docbook to list every mission."}}]})
                         "\n```")
                    (locator-reply issued)))
        before (flight/click-wants f {})
        read ((fr/read-fn {:store s :answer-fn (answer-with replies) :observe observe}) f {})
        after (flight/click-wants f {})]
    (is (= {:criteria? true} (select-keys (get-in before [:source :readings-needed]) [:criteria?])))
    (is (= [:criteria :locator] (mapv :kind (:asked read))) "criteria first, then a locator for the extracted one")
    (is (= [:published :published] (mapv :outcome (:asked read))))
    (is (= :machine-reading (get-in after [:source :criteria-from])))
    (is (= 1 (count (:wants after))))
    (is (= [] (get-in after [:source :unlocated])))))

(deftest a-criteria-reply-with-a-bad-cue-publishes-nothing
  (let [s (store)
        f (flight/start {:target "M-bare" :chosen-because {:kind :requested}}
                        {:kind :a-exits :repo "futon2" :path "p" :store s :read-text (fn [& _] bare-text)} {:id "f"})
        bad (constantly (str "```edn\n" (pr-str {:schema mr/criteria-schema
                                                 :criteria [{:statement "x" :cue {:lines [5 5] :quote "Words that are not there."}}]})
                             "\n```"))
        read ((fr/read-fn {:store s :answer-fn (answer-with bad) :observe observe}) f {})]
    (is (= [:rejected] (mapv :outcome (:asked read))))
    (is (= [{:kind :rejected :missing :criteria}] (mapv #(select-keys % [:kind :missing]) (:needs read))))
    (is (nil? (:criteria (wi/read-published s "M-bare"))))))

(deftest the-loop-reads-before-it-reads-the-wants
  (let [order (atom [])
        f (flight/run! (f11-flight (store))
                       {:read-fn (fn [_ _] (swap! order conj :read) {:asked [] :needs []})
                        :ask-fn (fn [_ _ _] (swap! order conj :ask) {:asked [] :needs []})
                        :click-fn (fn [_] (swap! order conj :click) {:click-id "c"})
                        :observe-fn (fn [_ _] {})
                        :sources-fn (constantly {})
                        :max-clicks 1})]
    (is (= [:read :ask :click] @order))
    (is (= 1 (:before-click (first (:readings f)))))))

(deftest a-checkbox-want-carries-its-task-line-as-criterion
  ;; the M-f11 plan showed the repair-024 checkbox want with no criterion, so
  ;; its interpretation request would end :no-criterion
  (let [decl "- [x] Publish the strict successful successor link for repair-024, or retain the typed failure without resolution."
        f (f11-flight (store))
        cw (flight/click-wants f {:wants {target [:hole/h2045faa0e7cc]}
                                  :locators {target {:hole/h2045faa0e7cc {:class :C4 :decl decl}}}})
        c (get-in cw [:source :criteria-by-token :hole/h2045faa0e7cc])]
    (is (= :checkbox-task (:kind c)))
    (is (str/starts-with? (:stated c) "- [ ] Publish the strict"))
    (testing "and the request cites it where it stands in the mission"
      (is (= [102 102] (:lines (wi/citation-for "src" f11-text c)))))))
