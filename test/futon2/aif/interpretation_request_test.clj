(ns futon2.aif.interpretation-request-test
  (:require [cheshire.core]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.interpretation-request :as request]
            [futon2.aif.mission-registry :as registry]
            [futon2.report.cascade-lane :as cascade])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]
           [java.time Instant]
           [java.util UUID]))

(def roots (atom []))
(use-fixtures :each
  (fn [f]
    (try (f) (finally
               (doseq [root @roots file (reverse (file-seq root))] (Files/delete (.toPath file)))
               (reset! roots [])))))

(defn fixture [type text]
  (let [root (.toFile (Files/createTempDirectory "interpretation-request" (make-array FileAttribute 0)))
        _ (swap! roots conj root)
        id (if (= type :advance-ticket) "T-fixture" "M-fixture")
        target (io/file root (str id ".md"))
        action (array-map :type type :target id :rationale "keep literal action" :extra [1 2])
        dir (io/file root "cohort" "attempt-001")
        _ (.mkdirs dir)
        start (pr-str {:attempt/id "attempt-001" :cohort/id :cohort :payload {:judgment {:semantic-epoch :test}}})
        _ (spit (io/file dir "001-time-step.edn") start)
        _ (spit target text)
        code (io/file root "code.py") index (io/file root "index.json")
        _ (spit code "# retriever fixture") _ (spit index "[]")
        occurrence (retention/mint-occurrence
                    {:run-id (str (UUID/randomUUID)) :cohort-id "cohort" :attempt-id "attempt-001"
                     :selected-action action :now #(Instant/now) :uuid-fn #(UUID/randomUUID)})
        identity {:occurrence occurrence :semantic-epoch :test :data-root (.getCanonicalPath root)
                  :start-event-sha256 (evidence/sha256 (.getBytes start "UTF-8"))
                  :interpreter-job {:status :none :reason :not-dispatched}
                  :author "fixture" :schema-version 1}]
    {:action action :identity identity :entry {:id id :path (.getCanonicalPath target)}
     :opts {:revision-fn (constantly "fixture-revision") :library-fn (constantly [])
            :retriever-specs (mapv #(assoc % :implementation (.getCanonicalPath code)
                                            :index (.getCanonicalPath index)) request/retrievers)}}))

(defn citations-match? [prepared]
  (let [identity (:identity prepared)
        dir (io/file (:data-root identity) "cohort" "attempt-001" "evidence")
        by-id (into {} (map (juxt :id clojure.core/identity)) (:sources prepared))]
    (every? (fn [c]
              (let [source (get by-id (:source c))
                    bs (Files/readAllBytes (.toPath (io/file dir (:file source))))
                    lines (vec (str/split-lines (String. bs "UTF-8")))
                    [a b] (:lines c)]
                (and (= (:sha256 source) (evidence/sha256 bs))
                     (= (:quote c) (str/join "\n" (subvec lines (dec a) b))))))
            (get-in prepared [:target :citations]))))

(deftest mission-and-ticket-use-cited-tension-without-construction
  (doseq [[type text] [[:advance-mission "# TITLE-NOT-QUERY\n**Status:** OPEN\n## 1. IDENTIFY\nHave a spec; want a tested implementation.\nHowever evidence is missing.\n## MAP\nBuilt but unwired.\n## ARGUE\nNOT-QUERY\n"]
                     [:open-mission "# TITLE-NOT-QUERY\n## DERIVE\nHave a boundary, want a receipt.\n"]
                     [:advance-ticket "# TITLE-NOT-QUERY\n## Problem\nThe gate ignores an error.\n## Evidence\nProbe reproduced it.\n## Fix\nAdditional requirement.\n"]]]
    (let [{:keys [action identity entry opts]} (fixture type text)
          calls (atom []) constructors (atom 0)]
      (with-redefs [registry/load-missions-cached (fn [& _] {:missions [entry]})
                    registry/ticket-entry (fn [_] entry)
                    cascade/cascade-policy-for (fn [& _] (swap! constructors inc))
                    cascade/cascade-lane (fn [& _] (swap! constructors inc))]
        (let [r (request/prepare! action identity
                                 (assoc opts :retrieve-fn
                                        (fn [q] (swap! calls conj q) [{:pattern "family/example" :score 1}])))]
          (is (identical? action (get-in r [:target :action])))
          (is (= (pr-str action) (pr-str (get-in r [:target :action]))))
          (is (citations-match? r))
          (is (not (str/includes? (get-in r [:retrieval :query]) "NOT-QUERY")))
          ;; retriever runs execute concurrently: invocation order is
          ;; nondeterministic; the request's runs stay in configured order
          (is (= ["embedding" "tier0"] (sort (mapv :kind @calls))))
          (is (= [:unjudged :unjudged]
                 (mapv #(get-in % [:candidates 0 :judgment]) (get-in r [:retrieval :runs]))))
          (is (= 0 @constructors))
          (is (not (contains? evidence/schemas (:schema r)))))))))

(defn finding [f] (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))
(deftest no-tension-and-retriever-failures
  (let [{:keys [action identity entry opts]} (fixture :advance-mission "# Title\n**Status:** OPEN\n")
        calls (atom 0)
        failure (finding #(request/prepare! action identity
                                            (assoc opts :resolve-fn (constantly entry)
                                                   :retrieve-fn (fn [_] (swap! calls inc)))))]
    (is (= :interpretation/no-citable-tension (:interpretation/refusal failure)))
    (is (zero? @calls)))
  (doseq [both? [false true]]
    (let [{:keys [action identity entry opts]} (fixture :advance-ticket "## Problem\nHave failing gate; need evidence.\n")
          calls (atom [])
          run #(request/prepare! action identity
                                 (assoc opts :resolve-fn (constantly entry)
                                        :retrieve-fn
                                        (fn [q] (swap! calls conj (:kind q))
                                          (if (or both? (= "embedding" (:kind q)))
                                            (throw (ex-info "controlled retriever failure" {:port (:kind q)}))
                                            [{:pattern "family/result"}]))))
          r (if both? (finding run) (run))
          partial (if both? (:request r) r)]
      (is (= ["embedding" "tier0"] (sort @calls)))
      (is (= 2 (count (get-in partial [:retrieval :runs]))))
      (is (seq (get-in partial [:retrieval :runs 0 :failures])))
      (if both?
        (is (= :interpretation/retrieval-unavailable (:interpretation/refusal r)))
        (is (= "family/result" (get-in r [:retrieval :runs 1 :candidates 0 :pattern])))))))

(deftest pin-is-stable-after-original-file-edit-and-action-drift-refuses
  (let [{:keys [action identity entry opts]} (fixture :advance-mission "## IDENTIFY\nHave original evidence.\n")
        r (request/prepare! action identity
                            (assoc opts :resolve-fn (constantly entry)
                                   :retrieve-fn (fn [_] (spit (:path entry) "changed") [])))]
    (is (citations-match? r))
    (is (= :interpretation/action-mismatch
           (:interpretation/refusal
            (finding #(request/prepare! (assoc action :target "another") identity opts)))))))

(deftest corpus-body-rules-and-metadata
  (doseq [id ["T-fail-agent-not-found" "T-zai-chat-transient-timeout" "T-cx-new-blocks-emacs"]]
    (let [text (slurp (str "../futon3c/holes/tickets/" id ".md"))
          result (request/tension-selection :ticket "source" text)
          query (str/join "\n" (map :quote (:citations result)))]
      (is (= :ticket-body (:tension-rule result)))
      (is (seq (:citations result)))
      (is (not (str/includes? query (first (str/split-lines text)))))))
  (let [text (slurp "holes/missions/M-wm-aif-policy-grain-compliance.md")
        r (request/tension-selection :mission "s" text)
        query (str/join "\n" (map :quote (:citations r)))]
    (is (= :mission-body (:tension-rule r)))
    (is (str/includes? query "The War Machine is the reference implementation"))
    (is (not (str/includes? query "**Cross-references:**")))
    (is (not (str/includes? query "**Status:**"))))
  (let [text "# Mission title\n**Status (triaged):** OPEN\n**Parent:** M-parent\nDate: yesterday\nOwner: person\nDriver: agent\nHome: repo\nLifecycle: draft\nDispatched by lead\nReviewer: peer\n\n## Goal\nHave an interface, want a receipt.\n"
        r (request/tension-selection :mission "s" text)
        query (str/join "\n" (map :quote (:citations r)))]
    (is (= :mission-body (:tension-rule r)))
    (is (str/includes? query "Have an interface"))
    (is (not (re-find #"Mission title|OPEN|M-parent|yesterday|person|agent|repo|draft|lead|peer" query)))))

(deftest real-json-array-and-id-normalisation
  (let [rows (cheshire.core/parse-string "[{\"pattern\":\"p\"},{\"pattern\":\"q\"},{\"pattern_id\":\"full/name\"}]" true)
        r (request/normalize-rows (vec rows) [{:relative "one/p.flexiarg"} {:relative "two/p.flexiarg"}
                                             {:relative "one/q.flexiarg"}])]
    (is (not (vector? rows)))
    (is (= ["one/q" "full/name"] (mapv :pattern (:candidates r))))
    (is (= :interpretation/ambiguous-pattern-id (get-in r [:failures 0 :kind])))
    (is (= {:pattern "p"} (get-in r [:failures 0 :raw])))))

(deftest non-git-symlink-source-remains-pinned
  (let [{:keys [action identity entry opts]} (fixture :advance-mission "## Goal\nHave bytes, want evidence.\n")
        source (io/file (:path entry))
        link (io/file (.getParentFile source) "linked.md")
        _ (Files/createSymbolicLink (.toPath link) (.toPath source) (make-array FileAttribute 0))
        r (request/prepare! action identity
                            (-> opts (dissoc :revision-fn)
                                (assoc :resolve-fn (constantly (assoc entry :path (str link)))
                                       :retrieve-fn (constantly []))))]
    (is (= "untracked:not-in-git-work-tree" (:revision (first (:sources r)))))
    (is (some #(and (= (str link) (:requested-path %)) (= (str source) (:canonical-path %))) (:source-paths r)))
    (is (pos? (:captured-bytes r)))
    (is (citations-match? r))))

(defn- git! [dir & args]
  (let [r (apply shell/sh "git" "-C" (str dir) args)]
    (when-not (zero? (:exit r)) (throw (ex-info "git failed" {:args args :err (:err r)})))
    (str/trim (:out r))))

(deftest library-pinned-once-per-revision
  ;; Acceptance (a): two want requests against the same library revision pin
  ;; the library once; (b): a library commit re-pins and the pins change.
  (let [root (.toFile (Files/createTempDirectory "library-pin-memo" (make-array FileAttribute 0)))
        _ (swap! roots conj root)
        lib (io/file root "lib" "family")
        _ (.mkdirs lib)
        fa (io/file lib "alpha.flexiarg") fb (io/file lib "beta.flexiarg")
        _ (spit fa "pattern alpha") _ (spit fb "pattern beta")
        target (io/file root "M-fixture.md")
        _ (spit target "# M\n\n## IDENTIFY\nHave a boundary; the criterion text.\n")
        code (io/file root "code.py") index (io/file root "index.json")
        _ (spit code "# retriever fixture") _ (spit index "[]")
        _ (git! root "init")
        _ (git! root "add" ".")
        _ (git! root "-c" "user.email=t@t" "-c" "user.name=t" "commit" "-m" "rev1")
        rev1 (git! root "rev-parse" "HEAD")
        lib-paths (mapv #(.getCanonicalPath %) [fa fb])
        pin-counts (atom {})
        real-pin @#'request/pin!
        entry {:id "M-fixture" :path (.getCanonicalPath target)}
        opts {:resolve-fn (constantly entry)
              :retrieve-fn (constantly [])
              :revision-fn @#'request/revision
              :library-fn (constantly lib-paths)
              :retriever-specs (mapv #(assoc % :implementation (.getCanonicalPath code)
                                              :index (.getCanonicalPath index))
                                     request/retrievers)}
        citations (atom 0)
        ask (fn [] (request/prepare-want-proposal!
                    "M-fixture" :mission (io/file root "evidence")
                    (fn [src _text] (let [n (swap! citations inc)]
                                      [{:source src :lines [3 3]
                                        :quote (str "criterion " n)}]))
                    opts))]
    (with-redefs [request/pin! (fn [dir path revision-fn]
                                 (swap! pin-counts update path (fnil inc 0))
                                 (real-pin dir path revision-fn))]
      (let [r1 (ask) r2 (ask)
            lib-pins (fn [r] (get-in r [:retrieval :runs 1 :parameters :library-sources]))]
        ;; (a) library pinned once across two wants; target/code/index pin per request
        (is (= 1 (get @pin-counts (first lib-paths))))
        (is (= 1 (get @pin-counts (second lib-paths))))
        (is (= 2 (get @pin-counts (.getCanonicalPath target))))
        (is (= (lib-pins r1) (lib-pins r2)))
        (is (= 2 (count (lib-pins r1))))
        ;; (b) a library commit re-pins, and the pins reflect the change
        (spit fa "pattern alpha, revised")
        (git! root "add" ".")
        (git! root "-c" "user.email=t@t" "-c" "user.name=t" "commit" "-m" "rev2")
        (let [rev2 (git! root "rev-parse" "HEAD")
              r3 (ask)]
          (is (not= rev1 rev2))
          (is (= 2 (get @pin-counts (first lib-paths))))
          (is (= 2 (get @pin-counts (second lib-paths))))
          (is (not= (lib-pins r1) (lib-pins r3))))))))

(deftest retriever-runs-run-concurrently-in-configured-order
  ;; (a) two 1s retrievers finish in < 1.6s, runs in configured order with
  ;; the same content as sequential; (b) one throwing retriever records its
  ;; own failure and leaves the other run unaffected.
  (let [{:keys [action identity entry opts]} (fixture :advance-mission "## IDENTIFY\nHave a spec; want a tested implementation.\n")
        opts (assoc opts :resolve-fn (constantly entry))
        t0 (System/nanoTime)
        r (request/prepare! action identity
                            (assoc opts :retrieve-fn
                                   (fn [q] (Thread/sleep 1000)
                                     [{:pattern (str "family/" (:kind q)) :score 1}])))
        ms (/ (double (- (System/nanoTime) t0)) 1e6)]
    (is (< ms 1600.0))
    (is (= ["embedding" "tier0"] (mapv :retriever (get-in r [:retrieval :runs]))))
    (is (= ["family/embedding" "family/tier0"]
           (mapv #(get-in % [:candidates 0 :pattern]) (get-in r [:retrieval :runs]))))
    (is (= (mapv :source-id (get-in r [:retrieval :runs 0 :parameters :library-sources] []))
           (mapv :source-id (get-in r [:retrieval :runs 1 :parameters :library-sources] []))))
    (let [t1 (System/nanoTime)
          r2 (request/prepare! action identity
                               (assoc opts :retrieve-fn
                                      (fn [q] (Thread/sleep 1000)
                                        (when (= "embedding" (:kind q))
                                          (throw (ex-info "controlled retriever failure" {})))
                                        [{:pattern "family/ok" :score 1}])))
          ms2 (/ (double (- (System/nanoTime) t1)) 1e6)]
      (is (< ms2 1600.0))
      (is (= :interpretation/retriever-failed
             (get-in r2 [:retrieval :runs 0 :failures 0 :kind])))
      (is (= "controlled retriever failure"
             (get-in r2 [:retrieval :runs 0 :failures 0 :reason])))
      (is (= [] (get-in r2 [:retrieval :runs 1 :failures])))
      (is (= "family/ok" (get-in r2 [:retrieval :runs 1 :candidates 0 :pattern]))))))
