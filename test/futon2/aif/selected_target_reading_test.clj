(ns futon2.aif.selected-target-reading-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.selected-target-reading :as sut]
            [futon2.aif.target-reading-registry :as registry])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- judgement [target source excerpt]
  {:outer-task-selection {:chosen {:id target :source {:path source}}}
   :cascade-problems {:target-sources [{:target-id target :source-path source}]}
   :decision {:status :abstained
              :selection-certificate
              {:target-policy-field
               {:families [{:target-id target
                            :failures [{:kind :no-current-target-reading
                                        :target-id target
                                        :expected-digest excerpt}]}]}}}})

(defn- analysis-for [request]
  {:labeller "wm-author" :reusable_cues [] :rnode_cues []
   :sentences
   (mapv (fn [{:keys [id start text]}]
           (if-let [offset (when-let [i (.indexOf ^String text "OPEN")]
                            (when-not (neg? i) (+ start i)))]
             {:id id :unresolved_reason ""
              :fragments [{:start offset :end (+ offset 4) :text "OPEN"
                           :intent "continue" :target "mission lifecycle"
                           :rationale "The mission explicitly remains open."
                           :relations ["context"]
                           :pattern_refs
                           [{:id "futon-theory/mission-lifecycle"
                             :rationale "The pattern governs explicit mission states."}]
                           :pattern_rejections []
                           :display_cues [{:start offset :end (+ offset 4)
                                           :text "OPEN"}]
                           :no_surface_cue ""}]}
             {:id id :fragments []
              :unresolved_reason "Heading only; no additional claim."}))
         (:sentences request))})

(deftest exact-outer-target-is-published-by-the-designated-author
  (let [work (temp-dir "selected-reading-")
        readings (temp-dir "selected-reading-registry-")
        source (io/file work "M-selected.md")
        _ (spit source "# M-selected\n\n## HEAD\nOPEN work.\n")
        excerpt (registry/excerpt-digest source)
        contacted (atom [])
        consumed (atom nil)
        answer (fn [{:keys [target author request] :as issued}]
                 (swap! contacted conj [target author (:acceptance-contract issued)])
                 (let [analysis (analysis-for request)]
                   {:analysis
                    (if (= 1 (count @contacted))
                      ;; Offset arithmetic is projected deterministically now;
                      ;; use a genuine semantic rejection to exercise correction.
                      (assoc-in analysis [:sentences 1 :fragments 0 :relations]
                                ["not-a-relation"])
                      analysis)}))
        opts {:run-id "run-one" :author "wm-author"
              :answer-fn answer :request-root (str work)
              :target-reading-root (str readings)}
        step (runner/selected-target-reading-step
              opts (judgement "M-selected" (.getPath source) excerpt)
              sut/repair!
              (fn [_]
                (reset! consumed
                        (registry/current-reading (str readings) "M-selected" excerpt))
                {:outer-task-selection {:chosen {:id "M-selected"}}
                 :decision {:status :selected :action {:target "M-selected"}}}))
        result (:record step)]
    (is (= :published (:status result)))
    (is (= 2 (count @contacted))
        "a validator-rejected first answer receives one bounded correction")
    (is (= ["M-selected" "wm-author"] (subvec (first @contacted) 0 2)))
    (is (= #{"action" "condition" "context" "contrast" "dependency" "goal" "rationale"}
           (set (get-in @contacted [0 2 :response :relation_vocabulary])))
        "the prompt contract comes from the validator's template output")
    (is (= "exact-source-slice-at-start-end"
           (get-in @contacted [0 2 :response :fragments :text]))
        "the live handoff carries the validator's exact fragment rule")
    (is (= "dropped-and-counted"
           (get-in @contacted [0 2 :response :rnode_cues :invalid_entries]))
        "non-fatal normalization rules are disclosed too")
    (is (= "M-selected" (:target result)))
    (is (= excerpt (:excerpt-digest result)))
    (is (= (:request-sha256 result)
           (registry/source-file-digest (:request-file result)))
        "the recorded issued request stays byte-identical after validation")
    (let [dir (.getParentFile (io/file (:request-file result)))]
      (is (.isFile (io/file dir "analysis-input-attempt-1.json")))
      (is (.isFile (io/file dir "analysis-input-attempt-2.json")))
      (is (= ["not-a-relation"]
             (get-in (json/parse-string
                      (slurp (io/file dir "analysis-input-attempt-1.json")) true)
                     [:sentences 1 :fragments 0 :relations]))
          "the rejected first answer is retained byte-for-meaning, not overwritten"))
    (is (= :current-candidate (:status @consumed))
        "the resumed inner decision sees the publication in the same step")
    (is (= :current-candidate
           (:status (registry/current-reading (str readings) "M-selected" excerpt))))))

(deftest refusal-never-falls-through-to-another-target-or-seat
  (let [source (io/file (temp-dir "selected-reading-refusal-") "M-selected.md")
        _ (spit source "# M-selected\n\n## HEAD\nOPEN work.\n")
        excerpt (registry/excerpt-digest source)
        j (judgement "M-selected" (.getPath source) excerpt)]
    (testing "the selected target alone supplies the need"
      (is (= "M-selected" (:target (sut/selected-need j))))
      (is (= "M-selected"
             (:target (sut/selected-need (dissoc j :outer-task-selection))))
          "the sole target-local family remains usable before checkpoint retention")
      (is (nil? (sut/selected-need
                 (-> j
                     (dissoc :outer-task-selection)
                     (update-in [:decision :selection-certificate
                                 :target-policy-field :families]
                                conj
                                {:target-id "M-other"
                                 :failures [{:kind :no-current-target-reading
                                             :expected-digest excerpt}]}))))
          "several families never permit a first-target fallback")
      (is (nil? (sut/selected-need
                 (assoc-in j [:decision :selection-certificate
                              :target-policy-field :families 0 :target-id]
                           "M-runner-up")))))
    (testing "a conflicting derived source pin cannot override the outer receipt"
      (let [conflicted (assoc-in j [:cascade-problems :target-sources 0 :source-path]
                                  "/different/source.md")
            contacts (atom 0)
            result (sut/repair! {:run-id "run-conflict" :author "wm-author"
                                 :answer-fn (fn [_] (swap! contacts inc))}
                                conflicted)]
        (is (= :selected-target-reading-unpinned (:kind result)))
        (is (zero? @contacts))))
    (testing "an unavailable designated author is a typed no-contact"
      (let [result (sut/repair! {:run-id "run-unavailable" :author "wm-author"
                                 :answer-fn (constantly
                                             {:state :not-contacted
                                              :reason :designated-author-unavailable})
                                 :request-root (str (temp-dir "selected-reading-request-"))}
                                j)]
        (is (= :designated-author-unavailable (:kind result)))
        (is (false? (get-in result [:detail :contacted?])))
        (is (= "M-selected" (:target result)))))))

(deftest unpinnable-source-refuses-before-contact
  (let [contacts (atom 0)
        result (sut/repair!
                {:run-id "run-unpinned" :author "wm-author"
                 :answer-fn (fn [_] (swap! contacts inc))}
                (judgement "M-selected" "/does/not/exist.md"
                           (apply str (repeat 64 "a"))))]
    (is (= :selected-target-reading-unpinned (:kind result)))
    (is (zero? @contacts))))

(deftest author-prompt-names-the-validators-complete-relation-vocabulary
  (let [text (sut/prompt
              {:request {:sentences []}
               :acceptance-contract
               {:id :wm/selected-target-reading-analysis :version 1
                :response {:relation_vocabulary
                           ["action" "condition" "context" "contrast"
                            "dependency" "goal" "rationale"]}
                :validator {:id :wm/session-turn-analysis :version 2}}})]
    (is (.contains text
                   ":relation_vocabulary"))
    (is (.contains text "\"dependency\""))))

(deftest author-prompt-states-machine-derived-offset-contract
  (let [text (sut/prompt
              {:request {:sentences []}
               :acceptance-contract
               {:id :wm/selected-target-reading-analysis :version 1
                :response {:relation_vocabulary ["context"]}
                :validator {:id :wm/session-turn-analysis :version 2}}})]
    (is (.contains text "The machine derives Unicode-codepoint offsets"))
    (is (.contains text "your numeric start/end values are not trusted"))
    (is (.contains text "absent or occurs more than once"))))

(deftest author-prompt-states-the-fragment-source-slice-contract
  (let [text (sut/prompt
              {:request {:source_text "A sentence."
                         :sentences [{:id "s1" :start 0 :end 11
                                      :text "A sentence."}]}
               :acceptance-contract
               {:id :wm/selected-target-reading-analysis :version 1
                :response {:relation_vocabulary ["context"]}
                :validator {:id :wm/session-turn-analysis :version 2}}})]
    (is (.contains text
                   "Quote every fragment and display cue exactly"))
    (is (.contains text "including punctuation and whitespace"))
    (is (.contains text "relations must come from its documented vocabulary"))))

(deftest correction-prompt-retains-the-request-and-validator-failure
  (let [text (sut/prompt
              {:request {:source_text "Exact text." :sentences []}
               :validation-feedback "display cue offsets/text do not match"
               :prior-analysis {:sentences [{:id "s1" :fragments []}]}
               :acceptance-contract
               {:id :wm/selected-target-reading-analysis :version 1
                :response {:relation_vocabulary ["context"]}
                :validator {:id :wm/session-turn-analysis :version 2}}})]
    (is (.contains text "Your preceding answer was rejected"))
    (is (.contains text "display cue offsets/text do not match"))
    (is (.contains text "Exact text."))))

(deftest offset-diagnostics-use-unicode-codepoints-and-exact-source-slices
  (let [request {:source_text "α the WM's setpoints, steady"
                 :sentences [{:id "s1" :start 0 :end 28
                              :text "α the WM's setpoints, steady"}]}
        analysis {:sentences
                  [{:id "s1"
                    :fragments
                    [{:start 2 :end 28 :text "the WM's setpoints, steady"
                      :display_cues [{:start 6 :end 24
                                      :text "the WM's setpoints"}]}]}]}
        errors (sut/offset-diagnostics request analysis)]
    (is (= [{:sentence "s1" :field :display-cue
             :fragment-start 2 :fragment-end 28
             :start 6 :end 24 :submitted "the WM's setpoints"
             :expected "WM's setpoints, st"}]
           errors))))

(deftest quoted-text-is-the-authority-for-machine-derived-offsets
  (let [request {:source_text "α OPEN work; OPEN elsewhere."
                 :sentences [{:id "s1" :start 0 :end 11
                              :text "α OPEN work"}
                             {:id "s2" :start 11 :end 28
                              :text "; OPEN elsewhere."}]}
        analysis {:sentences
                  [{:id "s1" :unresolved_reason ""
                    :fragments [{:start 999 :end 1000 :text "OPEN work"
                                 :display_cues [{:start -1 :end 50 :text "OPEN"}]}]}
                   {:id "s2" :unresolved_reason "" :fragments []}]}
        resolved (sut/resolve-analysis-offsets request analysis)]
    (is (= :resolved (:status resolved)))
    (is (= [2 11]
           ((juxt :start :end)
            (get-in resolved [:analysis :sentences 0 :fragments 0]))))
    (is (= [2 6]
           ((juxt :start :end)
            (get-in resolved [:analysis :sentences 0 :fragments 0 :display_cues 0]))))
    (is (= :quoted-text-absent
           (:reason (sut/resolve-analysis-offsets
                     request
                     (assoc-in analysis [:sentences 0 :fragments 0 :text]
                               "not present")))))
    (is (= :quoted-text-ambiguous
           (:reason (sut/resolve-analysis-offsets
                     {:source_text "x OPEN OPEN"
                      :sentences [{:id "s1" :start 0 :end 11 :text "x OPEN OPEN"}]}
                     {:sentences [{:id "s1" :fragments [{:text "OPEN"
                                                         :display_cues []}]}]}))))))

(deftest projection-normalizes-declared-legacy-relations-and-cue-density
  (let [source "One two three four five six seven eight nine ten"
        request {:source_text source
                 :sentences [{:id "s1" :start 0 :end (count source) :text source}]}
        analysis {:sentences
                  [{:id "s1" :unresolved_reason ""
                    :fragments
                    [{:text source :start 0 :end 1
                      :relations ["evidence" "context"]
                      :display_cues [{:text "One two three" :start 0 :end 1}
                                     {:text "four five six" :start 0 :end 1}]
                      :no_surface_cue ""}]}]}
        result (sut/resolve-analysis-offsets request analysis)
        fragment (get-in result [:analysis :sentences 0 :fragments 0])]
    (is (= :resolved (:status result)))
    (is (= ["rationale" "context"] (:relations fragment)))
    (is (= ["One two three"] (mapv :text (:display_cues fragment))))))

(deftest durable-agency-result-is-the-validated-answer
  (let [payload {:labeller "wm-author" :reusable_cues [] :rnode_cues []
                 :sentences []}
        answer {:state "done" :job-id "job-1"
                :result (json/generate-string payload)}]
    (is (= payload (handoff/response-map answer)))))

(deftest runner-resumes-inner-construction-once-for-the-same-outer-choice
  (let [target "M-selected"
        j (judgement target "/source.md" (apply str (repeat 64 "a")))
        repairs (atom [])
        redecisions (atom 0)
        result
        (runner/selected-target-reading-step
         {:author "wm-author"} j
         (fn [_ observed]
           (swap! repairs conj (get-in observed [:outer-task-selection :chosen :id]))
           {:schema :wm/selected-target-reading-request-v1
            :status :published :published true :target target
            :author "wm-author"})
         (fn [record]
           (swap! redecisions inc)
           {:outer-task-selection (:outer-task-selection j)
            :decision {:status :selected :action {:target (:target record)}}}))]
    (is (= [target] @repairs))
    (is (= 1 @redecisions))
    (is (= target (get-in result [:judgement :outer-task-selection :chosen :id])))
    (is (= target (get-in result [:judgement :decision :action :target])))
    (is (= "wm-author"
           (get-in result [:judgement :selected-target-reading-request :author])))
    (is (= (get-in result [:judgement :selected-target-reading-request])
           (get-in result [:judgement :decision :selection-certificate
                           :selected-target-reading-request])))))

(deftest refused-reading-contact-survives-the-terminal-run-record
  (let [dir (temp-dir "selected-reading-record-")
        refusal {:schema :wm/selected-target-reading-request-v1
                 :status :refused :published false
                 :kind :selected-target-reading-validation-refused
                 :target "M-selected"
                 :detail {:stderr "relations must name a documented role"}}
        written (#'runner/persist-run-record!
                 {:run-record-dir (.getPath dir)
                  :scan-render-fn (fn [& _] nil)}
                 "selected-reading-refusal" "2026-10-16T00:00:00Z"
                 {:outcome :abstained
                  :wm/route [{:fromNode "RUNNER" :toNode "FULL_LOOP_CLOSE"
                              :via :abstained}]
                  :checkpoints
                  {:selection
                   {:sorry {:decision {:status :abstained
                                       :reason :no-acting-cascade-candidate}
                            :selected-target-reading-request refusal}}}})
        record (edn/read-string (slurp (:run-record written)))]
    (is (= refusal (:selected-target-reading record)))
    (is (= :selected-target-reading-validation-refused
           (get-in record [:selected-target-reading :kind])))))

(deftest retained-reading-refusal-is-not-reported-as-not-required
  (let [refusal {:schema :wm/selected-target-reading-request-v1
                 :status :refused :published false
                 :kind :selected-target-reading-validation-refused
                 :target "M-selected"
                 :detail {:stderr "display cues exceed the sentence budget"}}
        cell (cond-> {:sorry {:decision {:status :abstained}}}
               refusal
               (assoc-in [:judgment :selected-target-reading-request] refusal))]
    (is (= refusal
           (#'runner/retained-selected-target-reading
            {:checkpoints {:selection cell}})))
    (is (not= :selected-target-reading-not-required
              (:reason (#'runner/retained-selected-target-reading
                        {:checkpoints {:selection cell}}))))))
