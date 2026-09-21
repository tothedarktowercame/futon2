(ns futon2.aif.cascade-proposals-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-proposals :as proposals]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.cascade-sources :as sources]
            [futon2.aif.mission-hole-wants :as wants]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn with-sources [f]
  (let [root (.toFile (Files/createTempDirectory "cascade-proposals" (make-array FileAttribute 0)))
        target (io/file root "M-proposal.md")
        pattern (io/file root "library" "apparatus" "example.flexiarg")
        code (io/file root "retrieve.py") index (io/file root "index.json")]
    (try
      (.mkdirs (.getParentFile pattern))
      (spit target "# Mission\n## IDENTIFY\nHave a model; want an observed result.\n")
      (spit pattern "@title Example\n@violation-signature Missing observed result\n")
      (spit code "# bounded retrieval port") (spit index "[]")
      (f {:root root :target target :pattern pattern
          :options {:resolve-fn (constantly {:id "M-proposal" :path (str target)})
                    :revision-fn (constantly "fixture-revision")
                    :library-fn (constantly [(str pattern)])
                    :retriever-specs (mapv #(hash-map :kind % :implementation (str code) :index (str index) :k 1)
                                          [:embedding :tier0])
                    :retrieve-fn (constantly [{:pattern "apparatus/example" :score 1}])}})
      (finally (doseq [file (reverse (file-seq root))] (Files/delete (.toPath file)))))))

(deftest pinned-proposals-survive-readback-without-admission
  (with-sources
    (fn [{:keys [root options target]}]
      (let [{:keys [path record]} (proposals/generate-retrieval! "M-proposal" :mission root options)
            proposal (first (:proposals record))]
        (is (= :proposed (:status proposal)))
        (is (= :retrieval-proposed (:origin proposal)))
        (is (= 1 (count (:proposals record))))
        (is (empty? (:declines record)))
        (is (every? #(not (contains? proposal %)) [:guard :produces :locators :interpretation-receipts :candidates]))
        (is (nil? (get-in record [:request :identity])))
        (is (nil? (get-in record [:request :target :action])))
        (spit target "Changed original text")
        (is (= (:proposals record) (:proposals (proposals/read-bundle path))))
        (is (= (:proposals record) (:proposals (proposals/load-proposals root))))
        ;; A status edit cannot convert captured retrieval into admission.
        (spit path (pr-str (assoc-in record [:proposals 0 :status] :admitted)))
        (is (= :proposed (get-in (proposals/read-bundle path) [:proposals 0 :status])))))))

(deftest missing-pin-and-missing-tension-decline
  (with-sources
    (fn [{:keys [root options target]}]
      (let [r (:record (proposals/generate-retrieval! "M-proposal" :mission root
                                                    (assoc options :retrieve-fn (constantly [{:pattern "missing/source"}]))))]
        (is (empty? (:proposals r)))
        (is (= :proposal-pattern-source-missing (get-in r [:declines 0 :reason]))))
      (spit target "# Title\n**Status:** OPEN\n")
      (let [calls (atom 0)
            r (:record (proposals/generate-retrieval! "M-proposal" :mission root
                                                    (assoc options :retrieve-fn (fn [_] (swap! calls inc)))))]
        (is (zero? @calls))
        (is (empty? (:proposals r)))
        (is (= :interpretation/no-citable-tension (get-in r [:declines 0 :reason])))))))

(deftest damaged-snapshot-never-supplies-a-proposal
  (with-sources
    (fn [{:keys [root options]}]
      (let [{:keys [path record]} (proposals/generate-retrieval! "M-proposal" :mission root options)
            file (io/file (.getParentFile (io/file path)) "evidence" (get-in record [:request :sources 0 :file]))]
        (spit file "tampered")
        (let [r (proposals/read-bundle path)]
          (is (empty? (:proposals r)))
          (is (= :proposal-evidence-invalid (get-in r [:declines 0 :reason]))))))))

(deftest failed-retrieval-retains-evidence-without-execution-identity
  (with-sources
    (fn [{:keys [root options]}]
      (let [{:keys [path record]} (proposals/generate-retrieval!
                                 "M-proposal" :mission root
                                 (assoc options :retrieve-fn #(throw (ex-info "unavailable" %))))
            stored (edn/read-string (slurp path))]
        (is (= stored record))
        (is (= :wm/cascade-proposal-request-v1 (get-in record [:request :schema])))
        (is (not (contains? (:request record) :identity)))
        (is (not (contains? (get-in record [:request :target]) :action)))
        (is (= :interpretation/retrieval-unavailable (get-in record [:declines 0 :reason])))
        (is (empty? (:proposals (proposals/read-bundle path))))))))

(deftest no-evidence-want-declines-through-real-admission-and-records-proposals
  (with-sources
    (fn [{:keys [root options target]}]
      (let [mission {:id "M-proposal" :path (str target) :status-class :identify
                     :open-holes [{:id "M-proposal#abc" :kind :unchecked-task :text "- [ ] Observed result"}]}
            want (wants/mission-source (str root) mission)
            source {:universes {"M-proposal" (:universe want)}
                    :wants {"M-proposal" (:want want)}
                    :interpretations {"M-proposal" (:interpretation want)}
                    :candidates {"M-proposal" (:candidates want)} :horizon-steps 2}
            assembled (problems/assemble {:targets ["M-proposal"] :sources source})
            no-evidence (proposals/record-supply assembled source {:proposals [] :declines []})
            supply (:record (proposals/generate-retrieval! "M-proposal" :mission root options))
            supplied (proposals/record-supply assembled source supply)
            decision (wm/cascade-decision supplied {})]
        (is (empty? (:candidates want)))
        (is (empty? (get-in want [:interpretation :patterns])))
        (is (some #(= :no-evidenced-proposal (:reason %)) (:dropped-candidates no-evidence)))
        (is (empty? (:problems supplied)))
        (is (= :no-acting-cascade-candidate (get-in decision [:decision :reason])))
        (is (some #(= :proposal-awaiting-agent-admission (:reason %)) (:dropped-candidates decision)))
        (is (= (:proposals supply)
               (get-in decision [:decision :selection-certificate :proposal-supply :proposals])))))))

(deftest agent-authored-proposal-uses-the-existing-declaration-loader
  (with-sources
    (fn [{:keys [root options]}]
      (let [original (edn/read-string (slurp (io/resource "wm/cascade-sources/M-wm-08-external-f2.edn")))
            pattern :cascade-construction/run-it-on-a-real-case
            pattern-file (io/file "/home/joe/code" (get-in original [:interpretation-receipts pattern :source :path]))
            supply (:record (proposals/generate-retrieval!
                             "M-proposal" :mission root
                             (assoc options :library-fn (constantly [(str pattern-file)])
                                    :retrieve-fn (constantly [{:pattern (subs (str pattern) 1)}]))))
            proposal (first (:proposals supply))
            dir (doto (io/file root "declarations") .mkdirs)
            ;; The real hand-admitted declaration structure and source bytes.
            ;; Facts omitted only to isolate document loading from git observations.
            declaration (-> original (assoc :target "M-proposal" :facts [])
                            (assoc-in [:interpretation-receipts pattern :proposal-id] (:proposal-id proposal)))
            file (io/file dir "admission.edn")
            _ (spit file (pr-str declaration))
            loaded (sources/with-context-fn (sources/load-declared (str dir)))
            recorded (proposals/record-supply {:problems []} loaded supply)]
        (is (= (:patterns original) (get-in loaded [:interpretations "M-proposal" :patterns])))
        (is (= (:candidates original) (get-in loaded [:candidates "M-proposal"])))
        (is (= (:proposal-id proposal) (get-in recorded [:proposal-supply :admissions 0 :proposal-id])))
        (is (empty? (:dropped-candidates recorded)))
        (is (thrown? clojure.lang.ExceptionInfo
                     (proposals/record-supply {} loaded (assoc-in supply [:proposals 0 :target] "different-target"))))
        (is (thrown? clojure.lang.ExceptionInfo
                     (proposals/record-supply {} loaded (assoc-in supply [:proposals 0 :evidence :pattern-source :sha256] "different-bytes"))))))))
