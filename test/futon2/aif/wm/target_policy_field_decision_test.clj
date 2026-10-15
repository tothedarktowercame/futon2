(ns futon2.aif.wm.target-policy-field-decision-test
  "Q5 step 1 (NOTE-wiring-reading-to-decision §7.5 step 2) at the production
  seam: cascade-decision forms every enumerated target's policy family from
  the assembly's source declaration, loading the pinned graph once, and
  retains every family and every typed failure without selecting from them."
  (:require [cheshire.core :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.pattern-graph-pin :as graph-pin]
            [futon2.aif.target-reading-registry :as reading-registry]
            [futon2.aif.wm.cascade-decision :as wm-cd])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn- temp-dir [prefix]
  (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0))))

(defn- analysis [pattern]
  {:status "analyzed"
   :sentences
   [{:id "s1"
     :fragments [{:start 0 :end 1 :text pattern :intent "act" :target "target"
                  :rationale "fixture fit" :relations ["action"]
                  :pattern_refs [{:id pattern :status "candidate"
                                  :rationale "fixture fit"
                                  :source_sha256 (apply str (repeat 64 "a"))}]}]}]})

(defn- pinned-graph [dir]
  (let [file (io/file dir "graph.json")]
    (spit file (json/generate-string
                {:records 1 :patterns 2 :pattern_ids ["p/a" "p/b"]
                 :summary [{:through "why" :edges 1} {:through "how" :edges 0}]
                 :edges [{:a "p/a" :b "p/b" :kind "why" :evidence []}]}))
    (graph-pin/pin! file)
    file))

(defn- field-fixture
  "Three enumerated targets: one with a current reading, one whose mission
  file has no reading, and a ticket with no source path."
  []
  (let [root (temp-dir "field-decision-reading-")
        current (io/file root "M-current.md")
        unread (io/file root "M-unread.md")]
    (spit current "# Current\n\n## HEAD\nCurrent work.\n")
    (spit unread "# Unread\n\n## HEAD\nNobody has read this yet.\n")
    (let [digest (reading-registry/excerpt-digest current)]
      (reading-registry/publish!
       root {:target-id "M-current" :source-path (.getPath current)
             :excerpt-digest digest
             :request {:task {:target_id "M-current" :excerpt_sha256 digest}}
             :analysis (analysis "p/a") :validator-version 1}))
    {:root root
     :rows [{:target-id "M-current" :source-kind :head :source-path (.getPath current)
             :source-absent nil}
            {:target-id "M-unread" :source-kind :head :source-path (.getPath unread)
             :source-absent nil}
            {:target-id "T-no-path" :source-kind :item-section :source-path nil
             :source-absent :target-source-path-absent}]}))

(defn- assembled [rows]
  ;; The old route's own outcome is not under test: every target is refused
  ;; by assembly, so that route abstains and the field is all that differs.
  {:problems []
   :refusals (mapv #(hash-map :target (:target-id %) :kind :no-constructed-candidate)
                   rows)
   :target-sources rows})

(defn- decide [rows opts]
  (let [loads (atom [])
        load-pinned graph-pin/load-pinned]
    (with-redefs [graph-pin/load-pinned (fn [path] (swap! loads conj path) (load-pinned path))]
      (assoc (wm-cd/cascade-decision (assembled rows) opts) ::loads @loads))))

(defn- failure-kinds [census]
  (mapv #(mapv :kind (:failures %)) (:families census)))

(deftest every-target-family-formed-against-one-graph-read
  (let [{:keys [root rows]} (field-fixture)
        graph (pinned-graph (temp-dir "field-decision-graph-"))
        result (decide rows {:pattern-graph-path (str graph) :target-reading-root (str root)})
        field (:target-policy-field result)
        census (get-in result [:decision :selection-certificate :target-policy-field])]
    (testing "three targets, one graph read"
      (is (= [(str graph)] (::loads result))))
    (testing "every target keeps its family, in field order"
      (is (= ["M-current" "M-unread" "T-no-path"] (mapv :target-id (:families field))))
      (is (= [:computed :failed :failed] (mapv :status (:families field))))
      (is (= (mapv :target-id (:families field)) (mapv :target-id (:families census)))))
    (testing "every failure is typed and retained on the recorded census"
      (is (= [[] [:no-current-target-reading] [:target-source-path-absent]]
             (failure-kinds census)))
      (is (= {:targets 3 :current 1 :stale 0 :absent 1 :source-path-absent 1
              :source-unreadable 0 :source-kind-unsupported 0 :head-template-only 0
              :graph-refused 0}
             (:coverage census))))
    (testing "the census is pinned to the graph and names its sources"
      (is (= :loaded (get-in census [:graph :status])))
      (is (= (:sha256 (edn/read-string (slurp (str graph ".pin.edn"))))
             (get-in census [:graph :pin :sha256])))
      (is (= (str root) (:reading-root census)))
      (is (= :wm/target-policy-field-census-v1 (:schema census))))
    (testing "the computed family's policies are on the census by stable id"
      (let [policies (get-in census [:families 0 :policies])]
        (is (seq policies))
        (is (= (mapv :policy-id (get-in field [:families 0 :policies]))
               (mapv :policy-id policies)))
        (is (every? #(re-matches #"M-current/policy-[0-9a-f]{20}" (:policy-id %)) policies))))
    (testing "nothing was selected from the field"
      (is (= :abstained (get-in result [:decision :status]))))))

(deftest family-identities-are-stable-under-the-same-pin
  (let [{:keys [root rows]} (field-fixture)
        graph (pinned-graph (temp-dir "field-decision-stable-"))
        opts {:pattern-graph-path (str graph) :target-reading-root (str root)}
        ids #(mapv :policy-id (get-in % [:decision :selection-certificate
                                         :target-policy-field :families 0 :policies]))
        first-run (decide rows opts)
        second-run (decide (vec (reverse rows)) opts)]
    (is (seq (ids first-run)))
    (is (= (ids first-run)
           (mapv :policy-id
                 (get-in (some #(when (= "M-current" (:target-id %)) %)
                               (get-in second-run [:decision :selection-certificate
                                                   :target-policy-field :families]))
                         [:policies]))))))

(deftest graph-refusals-are-typed-for-every-target
  (let [{:keys [root rows]} (field-fixture)]
    (doseq [[label graph-path expected]
            [[:malformed (let [f (io/file (temp-dir "field-bad-graph-") "graph.json")]
                           (spit f "{not json")
                           (str f))
              :graph-unreadable]
             [:missing-file (str (io/file (temp-dir "field-no-graph-") "graph.json"))
              :graph-unreadable]
             [:unpinned (let [dir (temp-dir "field-unpinned-graph-")
                              f (pinned-graph dir)]
                          (io/delete-file (io/file (str f ".pin.edn")))
                          (str f))
              :graph-pin-missing]
             [:edited-after-pinning (let [f (pinned-graph (temp-dir "field-edited-graph-"))]
                                      (spit f " " :append true)
                                      (str f))
              :graph-pin-mismatch]]]
      (testing (name label)
        (let [result (decide rows {:pattern-graph-path graph-path
                                   :target-reading-root (str root)})
              census (get-in result [:decision :selection-certificate :target-policy-field])]
          (is (= [graph-path] (::loads result)))
          (is (= [:failed :failed :failed] (mapv :status (:families census))))
          (is (every? #(= expected (get-in % [:failures 0 :kind])) (:families census)))
          (is (= 3 (get-in census [:coverage :graph-refused])))
          (is (= :refused (get-in census [:graph :status]))))))
    (testing "no graph path"
      (let [result (decide rows {:target-reading-root (str root)})
            census (get-in result [:decision :selection-certificate :target-policy-field])]
        (is (empty? (::loads result)))
        (is (every? #(= :graph-path-not-supplied (get-in % [:failures 0 :kind]))
                    (:families census)))
        (is (= 3 (get-in census [:coverage :graph-refused])))))))

(deftest no-source-declaration-is-a-typed-absence
  (let [result (wm-cd/cascade-decision {:problems [] :refusals [{:target "T" :kind :no-constructed-candidate}]}
                                       {})]
    (is (= {:status :absent :reason :target-sources-not-supplied}
           (get-in result [:decision :selection-certificate :target-policy-field])))))
