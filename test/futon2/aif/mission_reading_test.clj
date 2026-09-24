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

(defn- answer-with
  "A stubbed seat answering with F; a constraints request (asked once per
  text) is answered with no edges unless F handles :constraints itself."
  [f & [{:keys [constraints coverage]}]]
  (let [n (atom 0)]
    (fn [issued]
      {:seat "kimi-6" :job-id (str "job-" (swap! n inc)) :state "done"
       :text (cond
               (and (= :constraints (:kind issued)) (not constraints))
               (str "```edn\n" (pr-str {:schema mr/constraints-schema :constraints [] :by "kimi-6"}) "\n```")
               (and (= :coverage (:kind issued)) (not coverage))
               (str "```edn\n" (pr-str {:schema mr/criteria-schema :criteria [] :scope-outs [] :anchors [] :by "kimi-6"}) "\n```")
               :else (f issued))})))

(defn- of-kind [k asked] (filter #(= k (:kind %)) asked))

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
      (is (= :mission-text (get-in before [:source :criteria-from])))
      (is (= [32 33 34 36 37 39]
             (mapv #(get-in before [:source :criteria-by-token % :line])
                   (get-in before [:source :readings-needed :locators])))
          "the Acceptance bullets' lines in M-f11 at 22fa0da9")
      (is (= 6 (count (get-in before [:source :readings-needed :locators])))))
    (is (= (repeat 6 :published) (map :outcome (of-kind :locator (:asked read)))))
    (is (= [:published] (map :outcome (of-kind :constraints (:asked read)))) "the text's dependencies read once")
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
    (is (= [:criteria :locator :constraints] (mapv :kind (:asked read))) "criteria first, then a locator for the extracted one, then the dependencies")
    (is (= [:published :published :published] (mapv :outcome (:asked read))))
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
    (is (= [:rejected] (mapv :outcome (of-kind :criteria (:asked read)))))
    (is (= [{:kind :rejected :missing :criteria}] (mapv #(select-keys % [:kind :missing]) (:needs read))))
    (is (every? #(string? (:job-id %)) (:needs read)) "the need carries the answering job")
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

;; ---------------------------------------------------------------------------
;; Questions (Joe: a genuinely unclear mission yields good questions logged,
;; not a refusal and not bad work against a vague specification)

(def unclear-text
  (str/join "\n" ["# M-unclear" "" "**Owner:** **claude-3** (by assignment)" "" "## Motivation" ""
                  "Make the seam better." "" "## Notes" "" "Other text." ""]))

(def question {:question "Better in what respect?"
               :span {:lines [7 7] :quote "Make the seam better."}
               :alternatives ["fewer call sites cross it" "it is documented" "a second implementation exists"]})

(deftest questions-must-be-anchored-and-offer-readings
  (let [issued {:kind :criteria :target "M-unclear"}
        v #(mr/validate-criteria issued % unclear-text)
        reasons #(set (map :reason (:reasons (v %))))]
    (is (= :valid (:status (v {:questions [question]}))))
    (testing "bad case: a question with no span refuses the whole reply"
      (is (= :rejected (:status (v {:questions [(dissoc question :span)]}))))
      (is (contains? (reasons {:questions [(dissoc question :span)]}) :question-without-span)))
    (is (contains? (reasons {:questions [(assoc-in question [:span :quote] "Not the text.")]}) :question-span-does-not-resolve))
    (is (contains? (reasons {:questions [(assoc question :alternatives ["only one"])]}) :question-without-alternatives))
    (testing "a reply may mix clear criteria and questions"
      (is (= :valid (:status (v {:questions [question]
                                 :criteria [{:statement "notes exist" :cue {:lines [11 11] :quote "Other text."}}]})))))))

(defn- unclear-flight [s]
  (flight/start {:target "M-unclear" :chosen-because {:kind :requested}}
                {:kind :a-exits :repo "futon2" :path "p" :store s :read-text (fn [& _] unclear-text)
                 :observe #(contains? (:observed (observe {::t %})) ::t)} {:id "f-unclear"}))

(defn- criteria-reply [m]
  (constantly (str "```edn\n" (pr-str (merge {:schema mr/criteria-schema :by "kimi-6"} m)) "\n```")))

(deftest an-unclear-mission-ends-not-a-target-yet-with-its-questions
  (let [s (store)
        notified (atom [])
        clicks (atom 0)
        f (flight/run! (unclear-flight s)
                       {:read-fn (fr/read-fn {:store s :answer-fn (answer-with (criteria-reply {:questions [question]}))
                                              :notify! (fn [owner target prompt]
                                                         (swap! notified conj [owner target (str/includes? prompt "Better in what respect?")])
                                                         {:job-id "q-1"})
                                              :caller "joe" :observe observe})
                        :click-fn (fn [_] (swap! clicks inc) {:click-id "c"})
                        :observe-fn (fn [_ _] {})
                        :sources-fn (constantly {})
                        :max-clicks 3})]
    (is (= :not-a-target-yet (:status f)))
    (is (zero? @clicks) "no click is spent on a mission it cannot read")
    (is (= [["claude-3" "M-unclear" true]] @notified) "the owner the mission names is asked")
    (is (= [{:kind :owner-question :to "claude-3" :notified true :question "Better in what respect?"}]
           (mapv #(select-keys % [:kind :to :notified :question]) (:open-questions f))))))

(deftest clear-criteria-fly-and-the-closure-names-the-open-question
  (let [s (store)
        reply (fn [issued]
                (if (= :criteria (:kind issued))
                  ((criteria-reply {:questions [question]
                                    :criteria [{:statement "notes exist" :cue {:lines [11 11] :quote "Other text."}}]}) issued)
                  (str "```edn\n" (pr-str {:schema mr/locator-schema
                                           :locator {:class :C4 :repo "futon2" :sha "HEAD" :path "x" :decl "OBSERVED notes"}
                                           :cue {:quote "Other text"} :reading "r" :by "kimi-6"}) "\n```")))
        f (flight/run! (unclear-flight s)
                       {:read-fn (fr/read-fn {:store s :answer-fn (answer-with reply) :observe observe})
                        :click-fn (fn [_] {:click-id "c"})
                        :observe-fn (fn [_ _] {})
                        :sources-fn (constantly {})
                        :max-clicks 1})]
    (is (not= :not-a-target-yet (:status f)) "a clear criterion is flown")
    (is (some #(= :owner-question (:kind %)) (:needs f)))
    (testing "no owner-less notification: this mission names claude-3, but no notify! was given"
      (is (every? #(false? (:notified %)) (filter #(= :owner-question (:kind %)) (:needs f)))))))

(deftest a-mission-that-names-no-owner-addresses-the-caller
  (let [s (store)
        text (str/replace unclear-text "**Owner:** **claude-3** (by assignment)" "")
        notified (atom 0)
        f (flight/start {:target "M-unclear" :chosen-because {:kind :requested}}
                        {:kind :a-exits :repo "futon2" :path "p" :store s :read-text (fn [& _] text)} {:id "f"})
        q question
        r ((fr/read-fn {:store s :answer-fn (answer-with (criteria-reply {:questions [q]}))
                        :notify! (fn [& _] (swap! notified inc)) :caller "joe" :observe observe}) f {})]
    (is (zero? @notified))
    (is (= [["joe" false]] (mapv (juxt :to :notified) (filter #(= :owner-question (:kind %)) (:needs r)))))))

(deftest a-locator-reading-may-ask-instead
  ;; claude-8 (bell 23944): a locator reply may carry questions; that criterion
  ;; leaves the wants, is named out of view, and its owner is asked
  (let [s (store)
        sorry-span {:lines [36 36] :quote "- Discharge or amend the `find` sorry."}
        reply (fn [issued]
                (if (str/includes? (get-in issued [:criterion :stated]) "`find` sorry")
                  (str "```edn\n" (pr-str {:schema mr/locator-schema :by "kimi-6"
                                           :questions [{:question "Discharge, or amend: which closes it?"
                                                        :span sorry-span
                                                        :alternatives ["the sorry is proved" "the statement is amended and the amendment reviewed"]}]})
                       "\n```")
                  (locator-reply issued)))
        f (f11-flight s)
        read ((fr/read-fn {:store s :answer-fn (answer-with reply) :observe observe}) f {})
        after (flight/click-wants f {})
        sorry-token (some (fn [[t c]] (when (str/includes? (:stated c) "`find` sorry") t))
                          (get-in (flight/click-wants (f11-flight (store)) {}) [:source :criteria-by-token]))]
    (is (= {:published 5 :questions 1} (frequencies (map :outcome (of-kind :locator (:asked read))))))
    (is (= [sorry-token] (mapv :want (filter #(= :owner-question (:kind %)) (:needs read)))))
    (is (= 5 (count (:wants after))) "the questioned criterion is not a want")
    (is (some #(and (= sorry-token (:token %)) (= :owner-question (:reason %))) (get-in after [:source :out-of-view])))
    (testing "bad case: a locator question with no span refuses"
      (is (= :rejected (:status (mr/validate-locator {:criterion {:stated "x"}}
                                                     {:questions [{:question "which?" :alternatives ["a" "b"]}]}
                                                     {:text f11-text})))))))

;; ---------------------------------------------------------------------------
;; Constraints the text states in forms the reader does not recognise
;; (M-f11 line 48: 'Repair-024 is resolved only after this mission's ordinary
;; gates produce strict durable terminal evidence.')

(def line-48 {:lines [48 49] :quote "Repair-024 is resolved only after this mission's ordinary gates produce strict\ndurable terminal evidence. A failure—especially"})
(def checkbox :hole/h2045faa0e7cc)
(def acceptance-box :hole/h9ab212b3281d)

(deftest validate-constraints-cases
  (let [issued {:kind :constraints :known [{:token checkbox} {:token acceptance-box}]}
        v #(mr/validate-constraints issued % f11-text)
        edge {:want checkbox :requires acceptance-box :cue line-48}]
    (is (= [{:want checkbox :requires acceptance-box :by :machine-reading :line 48 :quote (:quote line-48)}]
           (:constraints (v {:constraints [edge]}))))
    (is (= :valid (:status (v {:constraints []}))) "none stated is an answer")
    (testing "one bad edge refuses the reply"
      (is (= :rejected (:status (v {:constraints [edge (assoc edge :requires :exit/hnot-a-known-token)]}))))
      ;; An unlisted token on the :want side refuses too; the edge is
      ;; otherwise good, so only the :want check can reject it.
      (let [r (v {:constraints [edge (assoc edge :want :exit/hnot-a-known-token)]})]
        (is (= :rejected (:status r)))
        (is (= [{:reason :edge-token-unknown
                 :edge {:want :exit/hnot-a-known-token :requires acceptance-box}}]
               (:reasons r))))
      (is (= :rejected (:status (v {:constraints [(assoc edge :requires checkbox)]}))))
      (is (= :rejected (:status (v {:constraints [(assoc-in edge [:cue :quote] "not the text")]})))))))

(deftest the-line-48-dependency-is-read-and-reaches-the-plan
  (let [s (store)
        f (f11-flight s)
        sources {:wants {target [checkbox]}
                 :locators {target {checkbox {:class :C4 :decl "- [x] Publish the strict successful successor link for repair-024, or retain the typed failure without resolution."}
                                    acceptance-box {:class :C4 :decl "- [x] Complete F11's ordinary acceptance and persist its runtime validation evidence."}}}}
        reply (fn [issued]
                (case (:kind issued)
                  :constraints (str "```edn\n" (pr-str {:schema mr/constraints-schema :by "kimi-6"
                                                        :constraints [{:want checkbox :requires acceptance-box :cue line-48}]}) "\n```")
                  (locator-reply issued)))
        before (flight/click-wants f sources)
        _ ((fr/read-fn {:store s :answer-fn (answer-with reply {:constraints true}) :observe observe}) f sources)
        after (flight/click-wants f sources)]
    (is (true? (get-in before [:source :readings-needed :constraints?])))
    (is (some #(= acceptance-box (:token %)) (get-in before [:source :known-tokens])) "facts are joinable, not only wants")
    (is (= [{:want checkbox :requires acceptance-box :by :machine-reading :line 48}]
           (mapv #(select-keys % [:want :requires :by :line]) (get-in after [:source :constraints :requires]))))
    (is (false? (get-in after [:source :readings-needed :constraints?])) "read once for this text")))

(deftest a-cue-may-quote-across-line-breaks-and-backticks
  ;; kimi-6's M-omni-wm-runner locators (jobs 23974/23976/23977) quoted the
  ;; criteria with line breaks collapsed and backticks dropped
  (let [issued {:kind :locator :want {:token :exit/hx}
                :criterion {:stated "- Gates on both: clj-kondo, check-parens, `clojure -X:test` for the\n  touched namespaces."}}
        good {:locator {:class :C4 :repo "futon2" :sha "HEAD" :path "p" :decl "OBSERVED x"}
              :cue {:quote "Gates on both: clj-kondo, check-parens, clojure -X:test for the touched namespaces"}
              :reading "r"}]
    (is (= :valid (:status (mr/validate-locator issued good {:observe observe}))))
    (is (= :rejected (:status (mr/validate-locator issued (assoc-in good [:cue :quote] "Gates on neither") {:observe observe}))))))

;; ---------------------------------------------------------------------------
;; Coverage (FLIGHT-TARGET-D2 §7.2): an extractor lifts more than bullets

(def omni-text (slurp "test/fixtures/mission-criteria/M-omni-wm-runner@futon3c-2114cb99.md"))

(deftest coverage-surfaces-o4-and-o5-on-m-omni-wm-runner
  ;; O4, the mission's actual done-definition, is in Scope and never became
  ;; a bullet; O5 is a scope-out a faithful closure must not require
  (let [s (store)
        f (flight/start {:target "M-omni-wm-runner" :chosen-because {:kind :requested}}
                        {:kind :a-exits :repo "futon3c" :path "holes/missions/M-omni-wm-runner.md" :store s
                         :read-text (fn [& _] omni-text) :observe (constantly false)} {:id "f-omni"})
        o4 {:statement "one durée click runs in-process in the futon3c JVM on a dedicated thread, triggered over HTTP"
            :cue {:lines [29 30] :quote "**In:** one durée click (`once` semantics) runs in-process in the futon3c\nJVM on a dedicated thread; HTTP trigger + status; registry-direct apparatus"}}
        o5 {:statement "continuous mode and the futon0 scan JVM are out of scope"
            :cue {:lines [35 35] :quote "**Out (follow-ups):** `continuous` mode; the transient futon0 scan JVM"}}
        reply (fn [issued]
                (if (= :coverage (:kind issued))
                  (str "```edn\n" (pr-str {:schema mr/criteria-schema :by "kimi-6" :criteria [o4] :scope-outs [o5]}) "\n```")
                  (locator-reply issued)))
        before (flight/click-wants f {})
        read ((fr/read-fn {:store s :answer-fn (answer-with reply {:coverage true}) :observe observe}) f {})
        after (flight/click-wants f {})]
    (is (= 3 (count (:wants before))) "the reader lifts the three Acceptance bullets")
    (is (true? (get-in before [:source :readings-needed :coverage?])))
    (is (= [:published] (map :outcome (of-kind :coverage (:asked read)))))
    (is (= 4 (count (:wants after))) "O4 is now a want")
    (is (some #(= "EXTRACTED" (:phase %)) (vals (get-in after [:source :criteria-by-token]))))
    (is (some #(= :scope-out (:reason %)) (get-in after [:source :out-of-view])) "O5 named out of view")
    (is (false? (get-in after [:source :readings-needed :coverage?])) "read once for this text")))

(deftest coverage-attaches-f4s-anchor-to-its-criterion
  (let [s (store)
        f (f11-flight s)
        sorry-token (some (fn [[t c]] (when (str/includes? (:stated c) "`find` sorry") t))
                          (get-in (flight/click-wants f {}) [:source :criteria-by-token]))
        reply (fn [issued]
                (if (= :coverage (:kind issued))
                  (str "```edn\n" (pr-str {:schema mr/criteria-schema :by "kimi-6"
                                           :anchors [{:token sorry-token :anchor "DarkTower/WarMachine/Holes.lean:264"
                                                      :cue {:lines [15 15] :quote "`find` sorry formerly anchored at `DarkTower/WarMachine/Holes.lean:264`."}}]}) "\n```")
                  (locator-reply issued)))
        _ ((fr/read-fn {:store s :answer-fn (answer-with reply {:coverage true}) :observe observe}) f {})
        c (get-in (flight/click-wants f {}) [:source :criteria-by-token sorry-token])]
    (is (= "DarkTower/WarMachine/Holes.lean:264" (get-in c [:anchor :anchor])))
    (testing "bad case: an anchor for a token the reader did not find refuses the reply"
      (is (= :rejected (:status (mr/validate-coverage {:found [{:token sorry-token}]}
                                                      {:anchors [{:token :exit/hnope :anchor "x" :cue {:lines [15 15] :quote "`find` sorry formerly anchored at `DarkTower/WarMachine/Holes.lean:264`."}}]}
                                                      f11-text)))))))

(deftest a-declined-locator-is-not-asked-again-for-the-same-text
  (let [s (store)
        f (f11-flight s)
        declines (constantly (str "```edn\n" (pr-str {:schema mr/locator-schema :decline {:reason :no-passing-run-class}}) "\n```"))
        asks (atom 0)
        answer (let [a (answer-with declines)] (fn [i] (when (= :locator (:kind i)) (swap! asks inc)) (a i)))
        _ ((fr/read-fn {:store s :answer-fn answer :observe observe}) f {})
        first-asks @asks
        _ ((fr/read-fn {:store s :answer-fn answer :observe observe}) f {})
        cw (flight/click-wants f {})]
    (is (= 6 first-asks))
    (is (= 6 @asks) "the second read asks nothing new for an unchanged text")
    (is (every? #(= :locator-declined (:reason %)) (get-in cw [:source :unlocated])))))
