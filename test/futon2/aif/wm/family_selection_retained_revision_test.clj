(ns futon2.aif.wm.family-selection-retained-revision-test
  "DIFFICULTY-REVISES-CASCADE packet 2, reading-policy route (production):
  the menu a click scores comes from families-for-field ->
  target-policy-family/policy-family over the current reading, NOT from
  interpretation-construction. These tests drive that real entry: run A
  deposits a real packet-1 :revised cascade into a cascade-feedback store;
  run B loads the same store as the judge does (:pattern-feedback) and the
  field is formed, scored and selected through the production functions.
  Refused and no-op revisions must never appear in what selection reads."
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-feedback :as feedback]
            [futon2.aif.cascade-revision :as revision]
            [futon2.aif.cascade-revision-producer :as producer]
            [futon2.aif.cascade-revision-producer-test :as p1]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.reading-policy-class-free-energy :as class-f]
            [futon2.aif.target-reading-registry :as reading-registry]
            [futon2.aif.wm.family-selection :as sut])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn- with-empty-learning-ledger [run]
  (let [root (.getPath (.toFile (Files/createTempDirectory
                                 "retained-revision-ledger-"
                                 (make-array FileAttribute 0))))]
    (with-redefs [ledger/default-root root
                  class-f/history-context (fn [_]
                                            {:status :complete :window 0 :sources []})]
      (run))))

(use-fixtures :each with-empty-learning-ledger)

(def target p1/target)
(def pattern-id "gauntlet/placenta-transfer")

(defn- temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- graph-file [dir]
  (let [file (io/file dir "graph.json")]
    (spit file (json/generate-string
                {:records 1 :patterns 1 :pattern_ids [pattern-id]
                 :summary [{:through "why" :edges 0}]
                 :edges []}))
    (graph-pin/pin! file)
    file))

(defn- analysis-with [pattern]
  {:status "analyzed"
   :sentences
   [{:id "s1"
     :fragments [{:start 0 :end 1 :text pattern :intent "act" :target target
                  :rationale "fixture fit" :relations ["action"]
                  :pattern_refs [{:id pattern :status "candidate"
                                  :rationale "fixture fit"
                                  :source_sha256 (apply str (repeat 64 "a"))}]}]}]})

(defn- reading-root-with-current-reading []
  (let [root (temp-dir "retained-revision-reading-")
        file (io/file root "M-hand.md")]
    (spit file "# M-hand\n\n## HEAD\nCurrent work.\n")
    (reading-registry/publish!
     root {:target-id target :source-path (.getPath file)
           :excerpt-digest (reading-registry/excerpt-digest file)
           :request {:task {:target_id target}}
           :analysis (analysis-with pattern-id) :validator-version 1})
    [root (.getPath file)]))

(defn- run-a-revision []
  (let [result ((producer/make-proposals-fn
                 {:sources-fn (fn [_ _] p1/sources)
                  :request-fn (fn [{:keys [target want]}]
                                {:schema :wm/want-interpretation-request-v1
                                 :target target :want {:token want}})
                  :code-root p1/library-root})
                (assoc p1/context
                       :ask! (constantly
                              (str "```edn\n" (pr-str p1/response) "\n```"))))]
    (revision/revise
     {:original (:original p1/context)
      :head-context (revision/head-seed (:original p1/context))
      :whole-context (dissoc (:whole-mission p1/context) :content)
      :blocker (:blocker p1/context)
      :proposals (:candidates result)})))

(defn- deposit!
  [store rev]
  (let [receipt (feedback/receipt
                 {:run-id "2026-11-01-runA" :target target
                  :selected-action (:original rev)
                  :outcome :grounded-progress
                  :cascade-revision rev})]
    (is (= :recorded (:status (feedback/record! store receipt))))))

(defn- field-for
  "What the judge forms: the field over the reading root with run A's
  store loaded exactly as war_machine.clj loads :pattern-feedback."
  [store root path]
  (sut/families-for-field
   {:target-sources [{:target-id target :source-path path}]
    :reading-root root
    :graph-path (graph-file (temp-dir "retained-revision-graph-"))
    :pattern-feedback (feedback/load-construction-metadata store)}))

(deftest run-b-field-and-selection-carry-the-retained-revision
  (let [store (str (temp-dir "retained-revision-store-") "/events.edn")
        rev (run-a-revision)]
    (is (= :revised (:status rev)))
    (is (= [:gauntlet/placenta-transfer] (:added (:delta rev))))
    (deposit! store rev)
    (let [[root path] (reading-root-with-current-reading)
          field (field-for store root path)
          family (first (:families field))]
      (is (= :computed (:status family)) (pr-str family))
      ;; the family's accounting says the revision was considered: one
      ;; candidate, either attached to its structural twin or entering as
      ;; its own :retained-revision policy, nothing refused
      (is (= {:candidates 1 :refused 0}
             (select-keys (:retained-revisions family) [:candidates :refused]))
          (pr-str family))
      (is (= 1 (+ (:as-policy (:retained-revisions family))
                  (:provenance-attached (:retained-revisions family)))))
      ;; the policy carrying it is the natively-read one (structural twin),
      ;; now with the revision's provenance
      (let [carrier (some #(when (:retained-revision %) %) (:policies family))]
        (is carrier (pr-str (mapv :policy-id (:policies family))))
        (is (= "2026-11-01-runA"
               (get-in carrier [:retained-revision :provenance :run/id])))
        (is (= [pattern-id]
               (get-in carrier [:retained-revision :precedence]))))
      ;; WHAT SELECTION READS: drive the production selector over the field
      (let [result (sut/select-over-families
                    (:families field)
                    {:beta 1 :enactment-fold nil :novelty-inputs {}})
            ranked (:ranked result)
            entry (some :retained-revision ranked)]
        (is (= :selected (:status result)) (pr-str (dissoc result :ranked)))
        (is entry (pr-str (mapv :policy-id ranked)))
        (is (= "2026-11-01-runA" (get-in entry [:provenance :run/id])))
        ;; and the selected action IS the policy carrying the revision
        (is (= (:policy-id (some #(when (:retained-revision %) %) ranked))
               (get-in result [:decision :action :id]))
            "the selected action is the policy carrying the revision")))))

(deftest refused-and-noop-revisions-never-reach-the-selection-menu
  (let [store (str (temp-dir "retained-revision-bad-store-") "/events.edn")
        rev (run-a-revision)
        refused {:schema revision/revision-schema :status :refused
                 :kind :revision-construction-inputs-unavailable
                 :target target
                 :original {:identity "sha" :patterns []}
                 :blocker (:blocker rev)
                 :whole-context (dissoc (:whole-mission p1/context) :content)
                 :repair-evidence {:status :present
                                   :reason :whole-mission-skim-found-no-extension}}
        noop (assoc rev
                    :delta {:added [] :removed [] :retained []}
                    :history [{:ordinal 0 :identity "p" :patterns []}
                              {:ordinal 1 :identity "c" :patterns []}]
                    :revised (assoc (:revised rev) :precedence []))]
    (deposit! store refused)
    (deposit! store noop)
    (let [[root path] (reading-root-with-current-reading)
          field (field-for store root path)
          family (first (:families field))]
      (is (= :computed (:status family)))
      (is (= {:candidates 0 :as-policy 0 :provenance-attached 0 :refused 0}
             (:retained-revisions family)))
      (is (nil? (some :retained-revision (:policies family))))
      (let [result (sut/select-over-families
                    (:families field)
                    {:beta 1 :enactment-fold nil :novelty-inputs {}})]
        (is (= :selected (:status result)))
        (is (nil? (some :retained-revision (:ranked result))))))))
