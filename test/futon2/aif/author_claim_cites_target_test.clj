(ns futon2.aif.author-claim-cites-target-test
  "An existing-artifact claim must cite the selected target before a
  reviewer is dispatched.  The negative case is retained from click
  fc2e27e3 (2026-10-30, run 2026-10-30-e5f08d91, M-typed-memories): the
  author claimed futon3c 7aac5ca07, whose message is
  `inbox-zero: promote 1 path(s) for claude-16 / Mission: M-giraffe` and
  whose only file is `holes/excursions/E-argument-game-search.md`.  The
  positive cases are the retained click-14 shape (the claimed commit touches
  the selected mission's own file) and a message that names the target."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.meta-terminal-injury :as terminal]
            [futon2.aif.stop-line :as stop])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def click-50-message
  "inbox-zero: promote 1 path(s) for claude-16\n\nholes/excursions/E-argument-game-search.md\n\nMission: M-giraffe\n")
(def click-50-files ["holes/excursions/E-argument-game-search.md"])
(def click-50-sha "7aac5ca0705e38d49b9c7f105e28ba1a41c8e9dd")

(deftest click-50-claim-does-not-cite-the-selected-target
  (let [citation (runner/claimed-commit-cites-target
                  {:target "M-typed-memories"
                   :source-path "/home/joe/code/futon3c/holes/missions/M-typed-memories.md"
                   :message click-50-message
                   :files click-50-files})]
    (is (false? (:cites? citation)))
    (is (false? (:message-cites-target? citation)))
    (is (nil? (:source-file citation)))
    (is (nil? (:target-path citation)))))

(deftest claim-cites-target-through-the-mission-file
  ;; Click 14's shape: the already-satisfied commit touched the selected
  ;; mission's own record.
  (let [citation (runner/claimed-commit-cites-target
                  {:target "M-recommendation-bindings"
                   :source-path "/home/joe/code/futon5a/holes/missions/M-recommendation-bindings.md"
                   :message "bindings: land :binds-to for THE-STACK and leaf invariants"
                   :files ["holes/missions/M-recommendation-bindings.md"
                           "holes/stories/THE-STACK.aif.edn"]})]
    (is (true? (:cites? citation)))
    (is (= "holes/missions/M-recommendation-bindings.md" (:source-file citation)))))

(deftest claim-cites-target-through-the-message
  (let [citation (runner/claimed-commit-cites-target
                  {:target "M-typed-memories"
                   :source-path "/home/joe/code/futon3c/holes/missions/M-typed-memories.md"
                   :message "memories: typed capture for the APM run\n\nMission: M-typed-memories\n"
                   :files ["src/futon3c/memory/typed.clj"]})]
    (is (true? (:cites? citation)))
    (is (true? (:message-cites-target? citation)))))

(deftest claim-cites-target-through-a-path-carrying-the-id
  (let [citation (runner/claimed-commit-cites-target
                  {:target "M-typed-memories"
                   :source-path nil
                   :message nil
                   :files ["docs/M-typed-memories/notes.md"]})]
    (is (true? (:cites? citation)))
    (is (= "docs/M-typed-memories/notes.md" (:target-path citation)))))

(deftest blank-target-never-cites
  (is (false? (:cites? (runner/claimed-commit-cites-target
                        {:target "" :message "anything" :files ["a"]})))))

(defn- temp-mission [target text]
  (let [dir (.toFile (Files/createTempDirectory "claim-cites-" (make-array FileAttribute 0)))
        file (io/file dir (str target ".md"))]
    (spit file text)
    {:id target
     :source {:path (.getPath file)
              :sha256 (#'runner/sha256-bytes (.getBytes text "UTF-8"))}}))

(deftest non-citing-claim-is-refused-before-any-reviewer-dispatch
  (let [target "M-typed-memories"
        mission (temp-mission target "# M-typed-memories\n\nwant: typed memories\n")
        author {:job-id "author-fc2e27e3" :state "done"
                :artifact-ref click-50-sha
                :result (str "FULL_LOOP_FEATURE_CARD: {:built \"already satisfied\" "
                             ":want-coverage \"already satisfied at " click-50-sha "\" "
                             ":matches-intent? true :things-to-try [] "
                             ":proof-ref \"" click-50-sha "\"}\n"
                             "FULL_LOOP_AUTHOR: REFUSE :already-satisfied-at-base-head")}
        classification {:outcome :already-satisfied
                        :grounded-commit {:repo "futon3c" :sha click-50-sha}}
        contacts (atom [])
        charge (atom 0)
        result (runner/review-already-satisfied-claim!
                {:phase-log-fn (fn [_])
                 :resolve-build-fn (fn [commit]
                                     (is (= click-50-sha commit))
                                     {:repo "/nonexistent/futon3c" :files click-50-files})
                 :commit-message-fn (fn [_repo commit]
                                      (is (= click-50-sha commit))
                                      click-50-message)
                 :dispatch-fn (fn [& args] (swap! contacts conj args) {:job-id "must-not-run"})
                 :poll-fn (fn [_ id] {:job-id id :state "done" :result "FULL_LOOP_REVIEW: APPROVE"})}
                {} {:author "wm-author" :reviewer "wm-reviewer"}
                target mission {} author classification #(swap! charge inc))]
    (is (= 0 @charge) "no reviewer seat is charged")
    (is (empty? @contacts) "no reviewer is dispatched")
    (is (= :guardrail-refusal (:outcome result)))
    (is (= :claimed-artifact-not-on-selected-target (:failure-kind result)))
    (is (= :author-wait (:failure-stage result)))
    (is (= click-50-sha (:commit result)))
    (is (true? (:claim-refused-before-review? result)))
    (is (false? (:reviewed-claim-refused? result)))
    (is (false? (get-in result [:claim-citation :cites?])))
    (is (str/includes? (:refusal-reason result) "does not cite the selected target"))
    (is (stop/no-finding-required? result) "the author's answer, not the apparatus, is wrong")
    (is (contains? terminal/explicit-non-injury-kinds (:failure-kind result)))))

(deftest citing-claim-still-reaches-the-reviewer
  (let [target "M-typed-memories"
        mission (temp-mission target "# M-typed-memories\n\nwant: typed memories\n")
        sha "0123456789abcdef0123456789abcdef01234567"
        author {:job-id "author-citing" :state "done" :artifact-ref sha
                :result (str "FULL_LOOP_FEATURE_CARD: {:built \"already satisfied\" "
                             ":want-coverage \"ok\" :matches-intent? true :things-to-try [] "
                             ":proof-ref \"" sha "\"}\n"
                             "FULL_LOOP_AUTHOR: REFUSE :already-satisfied-at-base-head")}
        classification {:outcome :already-satisfied :grounded-commit {:repo "futon3c" :sha sha}}
        contacts (atom [])
        charge (atom 0)
        result (runner/review-already-satisfied-claim!
                {:phase-log-fn (fn [_])
                 :resolve-build-fn (fn [_] {:repo "/nonexistent/futon3c"
                                            :files ["holes/missions/M-typed-memories.md"]})
                 :commit-message-fn (fn [_ _] "memories: close the capture want")
                 :dispatch-fn (fn [_ seat _ t prompt]
                                (swap! contacts conj {:seat seat :target t :prompt prompt})
                                {:job-id "claim-review"})
                 :poll-fn (fn [_ id] {:job-id id :state "done"
                                      :result "FULL_LOOP_REVIEW: REJECT not the selected want"
                                      :execution {:executed true :tool-events 1}})}
                {} {:author "wm-author" :reviewer "wm-reviewer"}
                target mission {} author classification #(swap! charge inc))]
    (is (= 1 @charge))
    (is (= 1 (count @contacts)))
    (is (= "wm-reviewer" (:seat (first @contacts))))
    (is (= :guardrail-refusal (:outcome result)))
    (is (= :reviewed-claim-refused (:failure-kind result)))
    (is (true? (get-in result [:claim-citation :cites?])))
    (is (= "holes/missions/M-typed-memories.md" (get-in result [:claim-citation :source-file])))))
