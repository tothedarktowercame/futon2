(ns futon2.aif.wm.family-selection-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.learning-trial-ledger :as ledger]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.target-reading-registry :as reading-registry]
            [futon2.aif.wm.family-selection :as sut])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn- with-empty-learning-ledger [run]
  (let [root (.getPath (.toFile (Files/createTempDirectory
                                 "family-selection-ledger-"
                                 (make-array FileAttribute 0))))]
    (with-redefs [ledger/default-root root]
      (run))))

(use-fixtures :each with-empty-learning-ledger)

(defn- analysis [pattern]
  {:status "analyzed"
   :sentences
   [{:id "s1"
     :fragments [{:start 0 :end 1 :text pattern :intent "act" :target "target"
                  :rationale "fixture fit" :relations ["action"]
                  :pattern_refs [{:id pattern :status "candidate"
                                  :rationale "fixture fit"
                                  :source_sha256 (apply str (repeat 64 "a"))}]}]}]})

(defn- temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- graph-file [dir]
  (let [file (io/file dir "graph.json")]
    (spit file (json/generate-string
                {:records 1 :patterns 2 :pattern_ids ["p/a" "p/b"]
                 :summary [{:through "why" :edges 1}
                           {:through "how" :edges 0}]
                 :edges [{:a "p/a" :b "p/b" :kind "why" :evidence []}]}))
    (graph-pin/pin! file)
    file))

(defn- reading-publication [target source digest]
  {:target-id target :source-path (.getPath source)
   :excerpt-digest digest
   :request {:task {:target_id target :excerpt_sha256 digest}}
   :analysis (analysis "p/a") :validator-version 1})

(deftest field-families-count-current-stale-and-path-absent
  (let [root (temp-dir "field-reading-root-")
        graph (graph-file (temp-dir "field-graph-"))
        current-file (io/file root "M-current.md")
        stale-file (io/file root "M-stale.md")]
    (spit current-file "# Current\n\n## HEAD\nCurrent work.\n")
    (spit stale-file "# Stale\n\n## HEAD\nEarlier work.\n")
    (let [current-digest (reading-registry/excerpt-digest current-file)
          stale-digest (reading-registry/excerpt-digest stale-file)]
      (reading-registry/publish!
       root (reading-publication "M-current" current-file current-digest))
      (reading-registry/publish!
       root (reading-publication "M-stale" stale-file stale-digest))
      (spit stale-file "# Stale\n\n## HEAD\nChanged work.\n")
      (let [result (sut/families-for-field
                    {:target-sources
                     [{:target-id "M-current" :source-path (.getPath current-file)}
                      {:target-id "M-stale" :source-path (.getPath stale-file)}
                      {:target-id "T-no-path" :source-path nil
                       :source-absent :target-source-path-absent}]
                     :reading-root root :graph-path graph})]
        (is (= [:computed :failed :failed] (mapv :status (:families result))))
        (is (= [:stale-target-reading :target-source-path-absent]
               (mapv #(get-in % [:failures 0 :kind]) (rest (:families result)))))
        (is (= {:targets 3 :current 1 :stale 1 :absent 0
                :source-path-absent 1 :source-unreadable 0
                :source-kind-unsupported 0 :head-template-only 0
                :graph-refused 0}
               (:coverage result)))))))

(deftest template-only-head-is-a-counted-field-failure
  (let [root (temp-dir "field-template-root-")
        graph (graph-file (temp-dir "field-template-graph-"))
        file (io/file root "M-template.md")]
    (spit file (str "# Mission\n\n## HEAD\n\n*"
                    reading-registry/head-template-definition "*\n"))
    (let [result (sut/families-for-field
                  {:target-sources [{:target-id "M-template" :source-kind :head
                                     :source-path (.getPath file)}]
                   :reading-root root :graph-path graph})]
      (is (= :failed (get-in result [:families 0 :status])))
      (is (= :target-head-template-only
             (get-in result [:families 0 :failures 0 :kind])))
      (is (= 1 (get-in result [:coverage :head-template-only]))))))

(deftest source-kinds-not-yet-readable-are-counted
  (let [graph (graph-file (temp-dir "field-source-kind-graph-"))
        result (sut/families-for-field
                {:target-sources [{:target-id "T-item" :source-kind :item-section
                                   :source-path "/not/read-as-head.md" :item-line 8}]
                 :reading-root (temp-dir "field-source-kind-reading-")
                 :graph-path graph})]
    (is (= :failed (get-in result [:families 0 :status])))
    (is (= :target-source-kind-unsupported
           (get-in result [:families 0 :failures 0 :kind])))
    (is (= 1 (get-in result [:coverage :source-kind-unsupported])))))

(deftest graph-refusal-precedes-every-reading-lookup
  (let [root (temp-dir "field-refused-reading-")
        graph (graph-file (temp-dir "field-refused-graph-"))
        calls (atom 0)]
    (spit graph " " :append true)
    (with-redefs [reading-registry/current-reading
                  (fn [& _] (swap! calls inc) (throw (ex-info "must not read" {})))]
      (let [result (sut/families-for-field
                    {:target-sources (mapv #(hash-map :target-id (str "T-" %)
                                                     :source-path "/not/read")
                                          (range 3))
                     :reading-root root :graph-path graph})]
        (is (zero? @calls))
        (is (= 3 (count (:families result))))
        (is (every? #(= :graph-pin-mismatch (get-in % [:failures 0 :kind]))
                    (:families result)))
        (is (= 3 (get-in result [:coverage :graph-refused])))))))

(deftest field-loads-the-pinned-graph-once
  (let [calls (atom 0)]
    (with-redefs [graph-pin/load-pinned
                  (fn [_] (swap! calls inc)
                    {:status :refused :kind :graph-pin-missing :path "/tmp/missing.pin"})]
      (sut/families-for-field
       {:target-sources (mapv #(hash-map :target-id (str "T-" %)
                                        :source-path "/not/read")
                             (range 7))
        :reading-root (temp-dir "field-once-reading-") :graph-path "/missing"})
      (is (= 1 @calls)))))

(deftest unreadable-target-source-is-counted
  (let [graph (graph-file (temp-dir "field-unreadable-graph-"))
        result (sut/families-for-field
                {:target-sources [{:target-id "M-gone"
                                   :source-path "/path/which/does/not/exist.md"}]
                 :reading-root (temp-dir "field-unreadable-reading-")
                 :graph-path graph})]
    (is (= :failed (get-in result [:families 0 :status])))
    (is (= :target-source-unreadable
           (get-in result [:families 0 :failures 0 :kind])))
    (is (= 1 (get-in result [:coverage :source-unreadable])))))

(defn- fixture-policy [target n]
  (let [pattern (str target "/p" n)]
    {:target target :mission target :policy-id (str target "/policy-" n)
     :kind :reading-alternatives :analysis (analysis pattern)
     :cascade {:nodes [{:pattern pattern :fragment-index 0 :roles ["action"]}]
               :edges [] :precedence [pattern]}}))

(defn- computed-family [target n]
  {:status :computed :target-id target
   :policies (mapv #(fixture-policy target %) (range n))
   :distinct-count n :reported-count n :failures [] :failure-count 0})

(defn- failed-family [target kind]
  {:status :failed :target-id target :policies [] :distinct-count 0
   :failures [{:kind kind}] :failure-count 1})

(deftest joint-selector-scores-every-policy-once
  (let [result (sut/select-over-families [(computed-family "T-a" 2)
                                          (computed-family "T-b" 3)]
                                         {:beta 1 :enactment-fold nil
                                          :novelty-inputs {}})
        ranked (:ranked result)
        scores (mapv (fn [entry]
                       {:entry entry
                        ;; beta=1 and the recorded unseen-policy habit is 1,
                        ;; hence ln(E)-F-G = -F-G.
                        :selection-score (- (+ (:f entry) (:controller-score entry)))})
                     ranked)
        best (apply max (map :selection-score scores))
        expected (->> scores
                      (filter #(== best (:selection-score %)))
                      (sort-by #(pr-str (get-in % [:entry :action :precedence
                                                   :co-apply :units 0])))
                      first :entry :action)]
    (is (= :selected (:status result)))
    (is (= 5 (count ranked)))
    (is (= 5 (count (mapcat :policies (:target-policy-families result)))))
    (doseq [entry ranked]
      (is (Double/isFinite (double (:f entry))))
      (is (Double/isFinite (double (:controller-score entry))))
      (is (= :co-application-frontier-theta-v1 (:transition-kernel entry)))
      (is (= sut/neutral-co-apply-habit (:habit-provenance entry))))
    (is (= expected (get-in result [:decision :action]))
        "the chosen policy is the argmax computed from the recorded F/G posterior")))

(deftest failed-target-is-counted-and-does-not-enter-selection
  (let [failed (assoc (failed-family "T-missing" :no-current-target-reading)
                      ;; A failed family must contribute no placeholder even
                      ;; if malformed input happens to carry one.
                      :policies [(fixture-policy "T-missing" 99)])
        result (sut/select-over-families [failed (computed-family "T-ready" 1)]
                                         {:beta 1 :enactment-fold nil
                                          :novelty-inputs {}})]
    (is (= :selected (:status result)))
    (is (= 1 (count (:ranked result))))
    (is (= "T-ready" (get-in result [:decision :action :target])))
    (is (= 1 (:failure-count result)))
    (is (= {:kind :no-current-target-reading :target-id "T-missing"}
           (first (:failures result))))))

(deftest all-failed-targets-abstain-with-every-failure
  (let [result (sut/select-over-families
                [(failed-family "T-a" :no-current-target-reading)
                 (failed-family "T-b" :stale-target-reading)]
                {:beta 1 :enactment-fold nil :novelty-inputs {}})]
    (is (= :abstained (:status result)))
    (is (= :no-computed-policy-family (get-in result [:decision :kind])))
    (is (nil? (get-in result [:decision :action])))
    (is (empty? (:ranked result)))
    (is (= 2 (:failure-count result)))
    (is (= #{["T-a" :no-current-target-reading]
             ["T-b" :stale-target-reading]}
           (set (map (juxt :target-id :kind) (:failures result)))))))

(deftest cyclic-policy-is-counted-and-never-ranked
  (let [cyclic-policy (assoc (fixture-policy "T-cycle" 0)
                             :cascade {:nodes [{:pattern "p/a"} {:pattern "p/b"}]
                                       :edges [{:from "p/a" :to "p/b" :kind :precedes}
                                               {:from "p/b" :to "p/a" :kind :precedes}]})
        cyclic-family {:status :computed :target-id "T-cycle"
                       :policies [cyclic-policy] :distinct-count 1
                       :failures [] :failure-count 0}
        result (sut/select-over-families [cyclic-family (computed-family "T-ready" 1)]
                                         {:beta 1 :enactment-fold nil
                                          :novelty-inputs {}})]
    (is (= :selected (:status result)))
    (is (= 1 (count (:ranked result))))
    (is (= "T-ready" (get-in result [:decision :action :target])))
    (is (= 1 (:failure-count result)))
    (is (= :cyclic-arrangement (get-in result [:failures 0 :kind])))
    (is (= "T-cycle" (get-in result [:failures 0 :target-id])))
    (is (= (first (get-in result [:failures 0 :cycle]))
           (last (get-in result [:failures 0 :cycle]))))))

(deftest wide-policy-is-counted-while-ordinary-sibling-is-ranked
  (let [wide (assoc (fixture-policy "T-mixed" 0)
                    :policy-id "T-mixed/wide"
                    :cascade {:nodes (mapv #(hash-map :pattern (str "p/" %)) (range 13))
                              :edges []})
        ordinary (assoc (fixture-policy "T-mixed" 1) :policy-id "T-mixed/ordinary")
        family {:status :computed :target-id "T-mixed"
                :policies [wide ordinary] :distinct-count 2
                :failures [] :failure-count 0}
        result (sut/select-over-families [family]
                                         {:beta 1 :enactment-fold nil
                                          :novelty-inputs {}})]
    (is (= :selected (:status result)))
    (is (= ["T-mixed/ordinary"] (mapv :policy-id (:ranked result))))
    (is (= 1 (:failure-count result)))
    (is (= {:target-id "T-mixed"
            :kind :frontier-too-wide-for-exact-enumeration
            :policy-id "T-mixed/wide"}
           (select-keys (first (:failures result)) [:target-id :kind :policy-id])))))
