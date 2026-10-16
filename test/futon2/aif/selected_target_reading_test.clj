(ns futon2.aif.selected-target-reading-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runner :as runner]
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
        answer (fn [{:keys [target author request]}]
                 (swap! contacted conj [target author])
                 {:analysis (analysis-for request)})
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
    (is (= [["M-selected" "wm-author"]] @contacted))
    (is (= "M-selected" (:target result)))
    (is (= excerpt (:excerpt-digest result)))
    (is (= (:request-sha256 result)
           (registry/source-file-digest (:request-file result)))
        "the recorded issued request stays byte-identical after validation")
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
