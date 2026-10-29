(ns ^{:clj-kondo/config
      '{:lint-as {futon2.aif.wm.injury41-function-repair-test/with-open-error
                  clojure.core/fn}}}
 futon2.aif.wm.injury41-function-repair-test
  "Acceptance for injury 41a00c62 (run fc464172, 2026-10-23, M-stack-geometry,
  held cascade 27ee1f80…, 19 :operational-clause-owed holes).

  The trigger: the author seat answered the held-cascade requisition with
  patch {namespace req-fbad4b8b, var \"clauses\"}.  `clauses` is the bare
  decision map (`def`), not the guarded function `decisions` (`defn`).  A
  Clojure map satisfies `ifn?`, so the production patch-loader and
  `hot-load!` both admitted it; calling the map with the resume context did
  a key lookup and returned nil; D3's retry refused with the typed
  :verified-authoring-product-missing; `resume!` read that as
  :compiled-digest-present? false and returned :resume-protocol-violation,
  dropping the retry's own kind.

  The repair: a patch var must be a FUNCTION of the context map.  A data
  var is refused at hot-load as :hot-load-failed with a bounded typed cause
  :patch-var-not-a-function, before any retry, with the accepted answer and
  the attempt retained in the ledger.

  Two fixture families, labelled in every deftest name:
  * RETAINED: the fc464172 checkpoint's :selected-entry and the two seats'
    reply texts, verbatim projections (see test/resources/wm/injury41/README.md).
  * SYNTHETIC: named string substitutions on the retained replies (var
    \"decisions\"; data vars in futon2.aif.wm.injury41-data-vars; a missing
    var; a wrong source digest) and the packet-D4 synthetic port fixture for
    the injected-loader contract.

  Real caller wiring, hermetic roots: cascade-decision/execute-selected-cascade!
  on the retained entry under temp cascade/journal roots -> the real
  :resume-port retry -> full-loop-runtime/open-cascade-resume-handler with the
  contact fns injected (the Agency transport is the only thing replaced) ->
  open-cascade-resume/handle-error! -> the PRODUCTION patch-loader, which
  `require :reload`s the authored namespace from the classpath and checks its
  source digest.  The retry closure is wrapped with a counter, never replaced."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.handoff-contract :as handoff]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.wm.cascade-decision :as cd]
            [futon2.aif.wm.injury41-data-vars]
            [futon2.aif.wm.open-cascade-resume :as resume])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

;; ---------------------------------------------------------------------------
;; Retained witnesses (verbatim projections)
;; ---------------------------------------------------------------------------

(def ^:private historical-requisition-id
  "fbad4b8be6eb583b0ad5be984aec251139b288492d200c6d7a15c9618dd11f3f")
(def ^:private historical-compiled-digest
  "27ee1f800ba25111680c7c7c93355afc069f89d3a666c8a238a7574477e0b937")
(def ^:private historical-selected-digest
  "c601dc5697a2d5d65c5c8440f08b3905cee1030b9a62dfa03e2ad5e7ab202c27")
(def ^:private authored-namespace "futon2.aif.wm.authored.req-fbad4b8b")
(def ^:private authored-source-digest
  "a5e34ef1cfa0f69c17ccac1fb6dbb7f04bac7cf891c0faa4546aa7eb4ab5813b")

(defn- resource-text
  "test/ is the classpath root under the :test alias, so the fixtures are the
  resource resources/wm/injury41/<name>; the relative file is the fallback
  for a REPL started at the repo root."
  [name]
  (slurp (or (io/resource (str "resources/wm/injury41/" name))
             (let [f (io/file "test/resources/wm/injury41" name)]
               (when (.isFile f) f))
             (throw (ex-info "retained fixture missing" {:name name})))))

(def ^:private retained-entry
  (delay (edn/read-string (resource-text "selected-entry-fc464172.edn"))))
(def ^:private retained-author-reply (delay (resource-text "zai-2-author-reply.txt")))
(def ^:private retained-review-reply (delay (resource-text "zai-3-review-reply.txt")))

;; ---------------------------------------------------------------------------
;; Synthetic variants: one named substitution each, and it must match
;; ---------------------------------------------------------------------------

(defn- substitute
  "Replace FROM with TO in TEXT; a substitution that matches nothing is a
  broken fixture, not a silent no-op."
  [text from to]
  (let [out (str/replace text from to)]
    (when (= out text)
      (throw (ex-info "variant substitution matched nothing" {:from from})))
    out))

(defn- author-variant
  "The retained author reply with its patch descriptor rewritten."
  [{:keys [var namespace source-digest]}]
  (cond-> @retained-author-reply
    var (substitute "\"var\": \"clauses\"" (str "\"var\": \"" var "\""))
    namespace (substitute (str "\"namespace\": \"" authored-namespace "\"")
                          (str "\"namespace\": \"" namespace "\""))
    source-digest (substitute authored-source-digest source-digest)))

(defn- review-variant [{:keys [source-digest]}]
  (cond-> @retained-review-reply
    source-digest (substitute authored-source-digest source-digest)))

(def ^:private data-vars-namespace "futon2.aif.wm.injury41-data-vars")

(defn- data-vars-source-digest []
  (load-identity/sha256
   (load-identity/read-bytes
    (io/resource "futon2/aif/wm/injury41_data_vars.clj"))))

;; ---------------------------------------------------------------------------
;; Hermetic roots and the real D3 open error
;; ---------------------------------------------------------------------------

(defn- temp-root []
  (str (Files/createTempDirectory "injury41-function-repair-"
                                  (make-array FileAttribute 0))))

(defn- delete-tree! [root]
  (doseq [f (reverse (file-seq (io/file root)))]
    (io/delete-file f true)))

(defn- journal-ordering-defect?
  "The pre-existing execution-journal defect this packet does not touch
  (injury a173047e, ledger ordinal 29; Lean click17_childWitness):
  recorded times are compared as strings, and `Instant` trims trailing
  zero nanos, so about 2% of 19-entry batches throw
  :journal-recorded-time-not-increasing.  Recognized here only so the test
  can restart on a fresh root instead of flaking."
  [t]
  (= :journal-recorded-time-not-increasing (:kind (ex-data t))))

(def ^:private journal-restarts
  "How many fresh-root restarts the journal defect forced in this run; the
  receipt reports it."
  (atom 0))

(defn- execute-retained-entry! [root]
  (cd/execute-selected-cascade!
   @retained-entry
   {:executable-cascade-root (str root "/cascades")
    :execution-journal-root (str root "/journals")}))

(defn- open-error
  "D3 under current source on the retained entry: the held open-cascade
  error whose :resume-port retry is the real one.  Restarts on a fresh root
  (at most three times) only when the journal ordering defect is hit."
  [root]
  (loop [root root restarts 0]
    (let [outcome (try (execute-retained-entry! root)
                       (catch clojure.lang.ExceptionInfo t
                         (if (and (journal-ordering-defect? t) (< restarts 3))
                           ::restart
                           (throw t))))]
      (if (= ::restart outcome)
        (do (swap! journal-restarts inc)
            (delete-tree! root)
            (let [fresh (temp-root)]
              (recur fresh (inc restarts))))
        outcome))))

(defn- counted
  "Wrap (not replace) the real retry so the test can prove whether the held
  phase was retried at all."
  [error calls]
  (update-in error [:resume-port :retry]
             (fn [retry] (fn [ctx] (swap! calls inc) (retry ctx)))))

(defmacro with-open-error
  "Bind SYM to a fresh real open error under temp roots, removed afterwards
  (the error's :saved path names the root it was actually built under)."
  [[sym] & body]
  `(let [root# (temp-root)
         ~sym (open-error root#)]
     (try
       (is (= :wm/open-cascade-execution-error (:kind ~sym)))
       ~@body
       (finally
         (delete-tree! root#)
         (some-> (:saved ~sym) io/file .getParentFile .getParentFile delete-tree!)))))

;; ---------------------------------------------------------------------------
;; Driving the production handler with the retained replies
;; ---------------------------------------------------------------------------

(defn- drive-handler
  "The production composition with only the Agency transport replaced: the
  injected contact fns return the reply texts as a done job's :result, so
  handoff/response-map does the real decode; the patch-loader is the
  production one (not injected)."
  [error author-reply review-reply]
  (let [clicks (atom 3)
        selections (atom 1)
        calls (atom 0)
        handler (runtime/open-cascade-resume-handler
                 {:author "zai-2" :reviewer "zai-3"
                  :run-id "injury41-function-repair"
                  :open-cascade-author-answer-fn
                  (fn [_req] {:state "done" :result author-reply})
                  :open-cascade-reviewer-answer-fn
                  (fn [_] {:state "done" :result review-reply})
                  :click-budget clicks
                  :selection-counter selections})
        result (handler (counted error calls))]
    {:result result
     :retry-calls @calls
     :clicks @clicks
     :selections @selections}))

(defn- refused-before-retry?
  "The repair's contract for a non-function patch var: typed :hot-load-failed
  with the bounded cause, never :resume-protocol-violation, no retry, no
  click, no reselection."
  [{:keys [result retry-calls clicks selections]} expected-value-type-suffix]
  (and (is (= :error (:status result)))
       (is (= :hot-load-failed (:kind result)))
       (is (not= :resume-protocol-violation (:kind result)))
       (is (= :patch-var-not-a-function (get-in result [:cause :kind])))
       (is (string? (get-in result [:cause :value-type])))
       (is (str/ends-with? (str (get-in result [:cause :value-type]))
                           expected-value-type-suffix))
       (is (= 0 retry-calls) "refused before the held phase was retried")
       (is (= 3 clicks))
       (is (= 1 selections))))

;; ---------------------------------------------------------------------------
;; Tests
;; ---------------------------------------------------------------------------

(deftest retained-checkpoint-rebuilds-the-historical-binding
  (with-open-error [error]
    (let [ctx (resume/recognize error)]
      (is (= :held (:status ctx)))
      (is (= :compile (:phase ctx)))
      (is (= 19 (count (:holes ctx))))
      (is (= historical-compiled-digest (:cascade-digest ctx)))
      (is (= historical-selected-digest (:selected-cascade-digest ctx)))
      (is (= historical-requisition-id (resume/requisition-id ctx))
          "the fresh requisition id equals the one the retained answer names"))))

(deftest retained-descriptor-through-the-production-loader-alone
  ;; The production loader's own contract, independent of hot-load!'s guard
  ;; (mutating either guard back to ifn? must be caught by its own test):
  ;; the retained descriptor (var "clauses") throws the typed cause instead
  ;; of returning the map; var "decisions" yields a function.
  (let [loader (var-get #'runtime/patch-loader)
        descriptor {:namespace authored-namespace :var "clauses"
                    :source-digest authored-source-digest}
        thrown (try (loader descriptor) nil
                    (catch clojure.lang.ExceptionInfo t t))]
    (is (some? thrown) "the loader refused instead of returning the data map")
    (is (= :patch-var-not-a-function (:kind (ex-data thrown))))
    (is (= {:namespace authored-namespace :var "clauses"}
           (select-keys (ex-data thrown) [:namespace :var])))
    (is (str/ends-with? (str (:value-type (ex-data thrown))) "Map"))
    (is (fn? (loader (assoc descriptor :var "decisions"))))))

(deftest retained-enactment-clauses-without-registration-has-a-distinct-cause
  (let [source-file (io/file "/home/joe/code/futon3c/src/futon3c/wm/enactment_clauses.clj")
        source (slurp source-file)
        digest (load-identity/sha256 (.getBytes source "UTF-8"))
        namespace "futon3c.wm.enactment-clauses"
        n (symbol namespace)
        loader (var-get #'runtime/patch-loader)
        descriptor {:namespace namespace :var "enact-clauses"
                    :source-digest digest}
        thrown (with-redefs [clojure.core/require (fn [& _] (load-string source))
                             load-identity/registry (atom {})]
                 (try (loader descriptor) nil
                      (catch clojure.lang.ExceptionInfo t t)))]
    (is (= "bba7869aecef4049c0033d67fd44255d92febe39a90c4a57f8a899fb4b0257ff"
           digest)
        "the fixture is the retained enactment_clauses.clj bytes")
    (is (= :patch-namespace-not-registered (:kind (ex-data thrown))))
    (is (= namespace (:namespace (ex-data thrown))))
    (when (find-ns n) (remove-ns n))))

(deftest retained-resume-path-reports-the-unregistered-enactment-namespace
  (with-open-error [error]
    (let [source (slurp "/home/joe/code/futon3c/src/futon3c/wm/enactment_clauses.clj")
          digest (load-identity/sha256 (.getBytes source "UTF-8"))
          namespace "futon3c.wm.enactment-clauses"
          n (symbol namespace)
          driven (with-redefs [clojure.core/require (fn [& _] (load-string source))
                               load-identity/registry (atom {})]
                   (drive-handler
                    error
                    (author-variant {:namespace namespace :var "enact-clauses"
                                     :source-digest digest})
                    (review-variant {:source-digest digest})))]
      (is (= :hot-load-failed (get-in driven [:result :kind])))
      (is (= :patch-namespace-not-registered
             (get-in driven [:result :cause :kind])))
      (is (= namespace (get-in driven [:result :cause :namespace])))
      (is (= 0 (:retry-calls driven)))
      (when (find-ns n) (remove-ns n)))))

(deftest retained-answer-naming-the-data-var-refuses-at-hot-load-before-retry
  ;; THE REPAIR.  Red on the source that ran fc464172: there the map passed
  ;; ifn?, the retry refused :verified-authoring-product-missing and resume!
  ;; returned :resume-protocol-violation.
  (with-open-error [error]
    (let [driven (drive-handler error @retained-author-reply @retained-review-reply)]
      (is (= "clauses"
             (get-in (handoff/response-map {:state "done" :result @retained-author-reply})
                     [:patch :var]))
          "the retained reply really names the data var")
      (refused-before-retry? driven "Map")
      (is (= authored-namespace (get-in driven [:result :cause :namespace])))
      (is (= "clauses" (get-in driven [:result :cause :var]))))))

(deftest synthetic-variant-naming-the-function-var-resumes-without-click-or-reselection
  ;; Positive regression (the same cascade resumed this way on 10-22 and
  ;; 10-23 05:52): the guard must not refuse the guarded function.
  (with-open-error [error]
    (let [{:keys [result retry-calls clicks selections]}
          (loop [restarts 0]
            (let [driven (drive-handler error (author-variant {:var "decisions"})
                                        @retained-review-reply)
                  journal-defect? (and (= :resume-retry-failed (:kind (:result driven)))
                                       (= "journal recorded time must increase"
                                          (:error (:result driven))))]
              (if (and journal-defect? (< restarts 3))
                (do (swap! journal-restarts inc) (recur (inc restarts)))
                driven)))]
      (is (= :resumed (:status result)))
      (is (= :cascade-ready (get-in result [:result :status])))
      (is (= historical-selected-digest (get-in result [:result :selected-cascade-digest])))
      (is (= 0 (:clicks-spent result)))
      (is (= 0 (:selections-run result)))
      (is (= 1 retry-calls) "the held phase was retried exactly once")
      (is (= 3 clicks))
      (is (= 1 selections)))))

(deftest synthetic-data-vars-of-every-callable-shape-refuse-at-hot-load
  ;; A map, a set and a keyword all satisfy ifn? (one-argument lookup).
  ;; None is a function of the context; each must be refused the same way.
  (with-open-error [error]
    (let [digest (data-vars-source-digest)
          review (review-variant {:source-digest digest})]
      (doseq [[var suffix] [["clauses-map" "Map"]
                            ["clauses-set" "Set"]
                            ["clauses-keyword" "Keyword"]]]
        (testing var
          (let [driven (drive-handler error
                                      (author-variant {:var var
                                                       :namespace data-vars-namespace
                                                       :source-digest digest})
                                      review)]
            (refused-before-retry? driven suffix)
            (is (= var (get-in driven [:result :cause :var])))))))))

(deftest synthetic-missing-var-and-source-digest-mismatch-keep-their-earlier-kinds
  ;; Controls that were already typed before the repair and must stay so:
  ;; the function-valued check is reached only after resolution and the
  ;; digest check, and the missing-var kind stays distinct from the
  ;; non-function kind.
  (with-open-error [error]
    (testing "missing var"
      (let [{:keys [result retry-calls]}
            (drive-handler error (author-variant {:var "no-such-var"}) @retained-review-reply)]
        (is (= :hot-load-failed (:kind result)))
        (is (= :patch-var-unavailable (get-in result [:cause :kind])))
        (is (not= :patch-var-not-a-function (get-in result [:cause :kind])))
        (is (= 0 retry-calls))))
    (testing "source digest mismatch"
      (let [wrong (apply str (repeat 64 "1"))
            {:keys [result retry-calls]}
            (drive-handler error
                           (author-variant {:var "decisions" :source-digest wrong})
                           (review-variant {:source-digest wrong}))]
        (is (= :hot-load-failed (:kind result)))
        (is (= :patch-source-digest-mismatch (get-in result [:cause :kind])))
        (is (= authored-source-digest (get-in result [:cause :loaded])))
        (is (= 0 retry-calls))))))

(deftest retained-answer-direct-handle-error-retains-answer-and-bounded-cause-in-the-ledger
  ;; The same call the handler makes, with the ledger in hand: the accepted
  ;; answer is kept raw, the failed attempt names the patch and the bounded
  ;; cause, and no retry was recorded.
  (with-open-error [error]
    (let [author (handoff/response-map {:state "done" :result @retained-author-reply})
          reviewed (handoff/response-map {:state "done" :result @retained-review-reply})
          accepted-answer (assoc author :accepted-by (:accepted-by reviewed))
          ledger (resume/new-ledger "injury41-direct")
          calls (atom 0)
          result (resume/handle-error! ledger (counted error calls) "zai-2" "zai-3"
                                       accepted-answer
                                       {:click-budget (atom 3)
                                        :selection-counter (atom 1)
                                        :patch-loader (var-get #'runtime/patch-loader)})
          attempt (first (:attempts @ledger))
          answer (first (:answers @ledger))]
      (is (= :hot-load-failed (:kind result)))
      (is (= :patch-var-not-a-function (get-in result [:cause :kind])))
      (is (= 0 @calls))
      (is (empty? (:retries @ledger)) "no retry was attempted or recorded")
      (is (= :accepted (:status answer)))
      (is (= "clauses" (get-in answer [:raw :patch :var]))
          "the original accepted answer is retained whole")
      (is (= :failed (:outcome attempt)))
      (is (= {:namespace authored-namespace :var "clauses"
              :source-digest authored-source-digest}
             (:patch attempt)))
      (is (= (:cause result) (:cause attempt)))
      (is (every? (fn [[_ v]] (or (string? v) (keyword? v) (symbol? v)))
                  (:cause attempt))
          "the cause is bounded scalars, not an exception dump")
      (is (= :verified (:status (resume/validate-ledger ledger)))))))

;; ---------------------------------------------------------------------------
;; The injected-loader contract in hot-load! (SYNTHETIC packet-D4 port)
;; ---------------------------------------------------------------------------

(def ^:private synthetic-digest (apply str (repeat 64 "d")))

(defn- synthetic-accepted []
  (let [ctx (resume/recognize
             {:kind :wm/open-cascade-execution-error
              :cascade-digest synthetic-digest
              :holes [{:hole-id :h/one :owner "x" :question "q"}]
              :phase :enact
              :attempts []
              :resume-port {:retry (fn [_] {:cascade-digest synthetic-digest})}})
        req (resume/authoring-requisition ctx "a" "b")
        accepted (resume/validate-answer
                  req {:requisition-id (:requisition-id req)
                       :cascade-digest synthetic-digest
                       :by "a" :accepted-by "b"
                       :decisions {:h/one "d"}
                       :patch {:namespace "synthetic.ns" :var "v"
                               :source-digest synthetic-digest}})]
    (is (= :accepted (:status accepted)))
    [req accepted]))

(deftest synthetic-injected-loader-must-yield-a-function
  (let [[req accepted] (synthetic-accepted)]
    (testing "a loader that yields a map is refused as a non-function"
      (let [ledger (resume/new-ledger "synthetic")
            loaded (resume/hot-load! ledger req accepted (constantly {:h/one "d"}))]
        (is (= :hot-load-failed (:kind loaded)))
        (is (= :patch-var-not-a-function (get-in loaded [:cause :kind])))
        (is (= "synthetic.ns" (get-in loaded [:cause :namespace])))
        (is (= "v" (get-in loaded [:cause :var])))
        (is (= (:cause loaded) (:cause (first (:attempts @ledger)))))))
    (testing "a loader that yields nothing is a distinct unresolved kind"
      (let [ledger (resume/new-ledger "synthetic")
            loaded (resume/hot-load! ledger req accepted (constantly nil))]
        (is (= :hot-load-failed (:kind loaded)))
        (is (= :patch-descriptor-unresolved (get-in loaded [:cause :kind])))))
    (testing "a loader that yields a function is loaded and called with the binding"
      (let [ledger (resume/new-ledger "synthetic")
            seen (atom nil)
            loaded (resume/hot-load! ledger req accepted
                                     (constantly (fn [ctx] (reset! seen ctx) {:h/one "d"})))]
        (is (= :loaded (:outcome loaded)))
        (is (= {:cascade-digest synthetic-digest :phase :enact} @seen))))
    (testing "a patch that throws still carries its own typed cause when it has one"
      (let [ledger (resume/new-ledger "synthetic")
            loaded (resume/hot-load! ledger req accepted
                                     (constantly (fn [_] (throw (ex-info "boom" {:kind :patch-boom})))))]
        (is (= :hot-load-failed (:kind loaded)))
        (is (= "boom" (:error loaded)))
        (is (= :patch-boom (get-in loaded [:cause :kind])))))))

;; ---------------------------------------------------------------------------
;; The prompt contract
;; ---------------------------------------------------------------------------

(deftest prompts-state-the-function-of-context-contract
  (let [req {:schema :wm/open-cascade-authoring-requisition-v1
             :requisition-id historical-requisition-id
             :cascade-digest historical-compiled-digest
             :phase :compile
             :holes [{:hole-id ["futon-theory/mission-lifecycle" 1]
                      :owner :wm/enactment-compiler
                      :question :operational-clause-owed}]
             :author "zai-2" :reviewer "zai-3"}
        author-prompt (#'runtime/open-cascade-author-prompt req)
        review-prompt (#'runtime/open-cascade-review-prompt
                       {:requisition req
                        :author-answer {:patch {:namespace authored-namespace
                                                :var "clauses"
                                                :source-digest authored-source-digest}}})]
    (doseq [[who prompt] [["author" author-prompt] ["reviewer" review-prompt]]]
      (testing who
        (is (str/includes? prompt "function"))
        (is (str/includes? prompt "{:cascade-digest :phase}"))
        (is (str/includes? prompt "data var"))
        (is (str/includes? prompt ":patch-var-not-a-function"))))
    (is (str/includes? author-prompt "load-identity/register!"))
    (is (str/includes? author-prompt ":patch-namespace-not-registered"))))

(deftest zz-report-journal-restarts
  ;; Not an assertion about the repair: a visible count of how often the
  ;; pre-existing journal ordering defect forced a fresh-root restart.
  (println "injury41-function-repair-test: journal ordering restarts =" @journal-restarts)
  (is (<= 0 @journal-restarts)))
