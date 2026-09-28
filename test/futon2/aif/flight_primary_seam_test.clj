(ns futon2.aif.flight-primary-seam-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-problems :as cascade-problems]
            [futon2.aif.flight :as flight]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.outcome-wants :as outcome-wants]
            [futon2.aif.wm.construction-inputs :as construction-inputs])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def fixture-path "test/fixtures/h-interp/primary-wants-seam-v1.edn")

(defn fixture []
  (edn/read-string (slurp fixture-path)))

(defn result
  ([document] (result document (constantly false)))
  ([document observe]
   (flight/source-wants {:kind :primary-seam
                         :document document
                         :observe observe}
                        {:target (:target document)} {})))

(defn outcome [document id]
  (first (filter #(= id (:id %)) (:outcomes document))))

(deftest admitted-mined-graph-flies-and-p0-waits
  (let [document (fixture)
        mined (outcome document :mined-graph)
        expected-token (:token (outcome-wants/outcome-criterion
                                (:target document) mined))
        actual (result document)
        waiting (get-in actual [:source :unlocated])]
    (is (= [expected-token] (:wants actual)))
    (is (= 1 (count waiting)))
    (is (= :no-admitted-locator (:reason (first waiting))))
    (is (= (get-in (outcome document :p0-reconstruction) [:locator :would-be])
           (:would-be (first waiting))))
    (is (= #{:wants :locators :universe :c :source} (set (keys actual))))))

(defn- assembled-fixture [document]
  (let [source-result (result document)
        target (:target document)
        token (first (:wants source-result))
        flight {:target target :wants (:wants source-result)
                :locators (:locators source-result) :universe (:universe source-result)
                :c (:c source-result)}
        input {:targets [target]
               :sources {:universes {target (:universe source-result)}
                         :wants {target (:wants source-result)}
                         :locators {target (:locators source-result)}
                         :interpretations {target {:patterns {:p {:guard {:needs #{} :forbids #{}}
                                                                  :produces #{token}}}}}
                         :candidates {target [{:precedence [:p]
                                              :construction-receipt {:kind :fixture}}]}
                         :horizon-steps 1 :context-of (constantly :WM)
                         :beta-by-context {:WM 1}}}
        assembled (cascade-problems/assemble
                   (construction-inputs/flight-assembly-input flight input))]
    (get-in assembled [:problems 0 :cascade-problem :cascade-spec])))

(deftest primary-seam-c-reaches-assembled-scoring-spec
  (let [document (fixture)
        mined (outcome document :mined-graph)
        token (:token (outcome-wants/outcome-criterion (:target document) mined))
        spec (assembled-fixture document)
        c (:c spec)
        mined-c (first (filter #(= token (:token %)) (:outcomes c)))]
    (is (contains? (:want spec) token))
    (is (= :derived (:status c)))
    (is (= :primary-seam (:source c)))
    (is (= (get-in document [:extractor :weighting]) (:weighting c)))
    (is (= (:source-outcome mined) (:source-outcome mined-c)))))

(deftest missing-extractor-is-distinct-from-extractor-unstated
  (let [c (:c (result (dissoc (fixture) :extractor)))]
    (is (= {:absent :not-in-seam-document} (:weighting c)))
    (is (not= {:status :uniform-declared-constant} c))))

(deftest served-by-is-joined-by-source-outcome
  (let [document (-> (fixture)
                     (assoc-in [:outcomes 0 :source-outcome] :o-mined)
                     (assoc-in [:extractor :served-by]
                               [{:instance :right :serves [{:outcome :o-mined :via :shared}]}
                                {:instance :wrong :serves [{:outcome :o-other :via :other}]}]))
        actual (result document)
        token (first (:wants actual))
        outcome-c (first (filter #(= token (:token %)) (get-in actual [:c :outcomes])))]
    (is (= [:right] (mapv :instance (:served-by outcome-c))))))

(deftest a-exits-flight-assembly-does-not-add-c
  (let [target "M-a-exits"
        assembled (construction-inputs/flight-assembly-input
                   {:target target :wants [:w] :want-source {:kind :a-exits}}
                   {:targets [target] :sources {:wants {target [:w]}}})]
    (is (nil? (get-in assembled [:sources :c target])))))

(deftest injected-observer-supplies-the-mined-graph-value
  (let [actual (result (fixture) (constantly false))
        token (first (:wants actual))]
    (is (false? (get-in actual [:universe token])))))

(deftest unconfirmed-classification-waits-even-with-a-locator
  (let [document (fixture)
        replacement (get-in document [:variants :unconfirmed-classification :replace-steps])
        document (update document :outcomes
                         (fn [outcomes]
                           (mapv #(if (= :mined-graph (:id %))
                                    (assoc-in % [:provenance :steps] replacement)
                                    %)
                                 outcomes)))
        actual (result document)
        mined-token (:token (outcome-wants/outcome-criterion
                             (:target document)
                             (outcome document :mined-graph)))
        mined-wait (first (filter #(= mined-token (:token %))
                                  (get-in actual [:source :unlocated])))]
    (is (= :unconfirmed-classification (:reason mined-wait)))
    (is (not (some #{mined-token} (:wants actual))))))

(deftest outcome-tokens-are-stable-on-rerun
  (let [document (fixture)]
    (is (= (:wants (result document)) (:wants (result document))))
    (is (= (keys (get-in (result document) [:source :criteria-by-token]))
           (keys (get-in (result document) [:source :criteria-by-token]))))))

(deftest criterion-keeps-the-provenance-quote-span
  (let [document (fixture)
        mined (outcome document :mined-graph)
        token (:token (outcome-wants/outcome-criterion
                       (:target document) mined))]
    (is (= [10126 10419]
           (get-in (result document)
                   [:source :criteria-by-token token :provenance :quote :span])))))

(defn- temp-dir []
  (.toFile (Files/createTempDirectory "primary-seam-git" (make-array FileAttribute 0))))

(defn- git! [repo & args]
  (let [result (apply sh/sh "env" "-u" "GIT_DIR" "-u" "GIT_WORK_TREE"
                      "git" "-C" (str repo) args)]
    (when-not (zero? (:exit result))
      (throw (ex-info "test git command failed" {:args args :result result})))
    (str/trim (:out result))))

(def seam-path "holes/missions/seam/M-x.primary-wants.edn")

(defn- git-seam-source [repo]
  {:kind :primary-seam :code-root (str (.getParentFile repo))
   :repo (.getName repo) :path seam-path :observe (constantly false)})

(defn- committed-seam-repo
  ([] (committed-seam-repo identity))
  ([transform]
   (let [repo (temp-dir)]
     (git! repo "init")
     (git! repo "config" "user.name" "Primary Seam Test")
     (git! repo "config" "user.email" "primary-seam@example.invalid")
     (git! repo "commit" "--allow-empty" "-m" "base")
     (let [base (git! repo "rev-parse" "HEAD")
           document (-> (fixture)
                        (assoc :target "M-x")
                        (assoc-in [:source :commit] base)
                        transform)
           file (io/file repo seam-path)]
       (.mkdirs (.getParentFile file))
       (spit file (pr-str document))
       (git! repo "add" "--" seam-path)
       (git! repo "commit" "-m" "add seam")
       {:repo repo :file file :document document
        :head (git! repo "rev-parse" "HEAD")}))))

(defn- git-result [repo]
  (flight/source-wants (git-seam-source repo) {:target "M-x"} {}))

(deftest committed-seam-is-read-at-head-with-byte-receipt
  (let [{:keys [repo file head document]} (committed-seam-repo)
        actual (git-result repo)
        expected (result document)
        bytes (.getBytes (slurp file) "UTF-8")]
    (is (= (:wants expected) (:wants actual)))
    (is (= (get-in expected [:source :unlocated])
           (get-in actual [:source :unlocated])))
    (is (= {:repo (.getName repo) :path seam-path :sha head
            :sha256 (evidence/sha256 bytes)}
           (get-in actual [:source :read])))))

(deftest absent-path-is-a-typed-no-seam-document
  (let [{:keys [repo head]} (committed-seam-repo)
        actual (flight/source-wants
                (assoc (git-seam-source repo) :path "holes/missions/seam/missing.edn")
                {:target "M-x"} {})]
    (is (= [] (:wants actual)))
    (is (= {:kind :primary-seam :absent :no-seam-document
            :repo (.getName repo) :path "holes/missions/seam/missing.edn"
            :read-sha head}
           (:source actual)))))

(deftest wrong-schema-is-typed
  (let [{:keys [repo]} (committed-seam-repo #(assoc % :schema :wrong))
        actual (git-result repo)]
    (is (= [] (:wants actual)))
    (is (= :seam-schema-mismatch (get-in actual [:source :absent])))))

(deftest source-commit-must-be-an-ancestor-of-read-head
  (let [repo (temp-dir)
        _ (git! repo "init")
        _ (git! repo "config" "user.name" "Primary Seam Test")
        _ (git! repo "config" "user.email" "primary-seam@example.invalid")
        _ (git! repo "commit" "--allow-empty" "-m" "base")
        tree (git! repo "rev-parse" "HEAD^{tree}")
        orphan (git! repo "commit-tree" tree "-m" "orphan")
        document (-> (fixture) (assoc :target "M-x")
                     (assoc-in [:source :commit] orphan))
        file (io/file repo seam-path)
        _ (.mkdirs (.getParentFile file))
        _ (spit file (pr-str document))
        _ (git! repo "add" "--" seam-path)
        _ (git! repo "commit" "-m" "add non-ancestor seam")
        actual (git-result repo)]
    (is (= [] (:wants actual)))
    (is (= :seam-source-not-ancestor (get-in actual [:source :absent])))))

(deftest uncommitted-working-tree-edit-is-not-read
  (let [{:keys [repo file document]} (committed-seam-repo)
        committed (git-result repo)
        _ (spit file (pr-str (assoc document :schema :working-tree-only)))
        actual (git-result repo)]
    (is (= (:wants committed) (:wants actual)))
    (is (nil? (get-in actual [:source :absent])))
    (is (= :wm/primary-wants-seam-v1
           (:schema (edn/read-string (git! repo "show" (str "HEAD:" seam-path))))))))
