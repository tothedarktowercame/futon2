(ns futon2.aif.observation-checks-test
  "Checks against real pinned shas in futon2 and mathlib4."
  (:require [babashka.http-client :as http]
            [clojure.data.json :as json]
            [clojure.test :refer [deftest is]]
            [futon2.aif.observation-checks :as oc]))

(def futon2-sha "b81afd97")   ; H7c-1 construction commit
(def mathlib-sha "3b19f6225e") ; EpistemicValue.lean

(deftest c3-path
  (is (true? (:observed (oc/check-path-exists {:repo "futon2" :sha futon2-sha :path "src/futon2/aif/construction.clj"}))))
  (is (false? (:observed (oc/check-path-exists {:repo "futon2" :sha futon2-sha :path "src/futon2/aif/no_such_file.clj"}))))
  ;; the file did not exist before its commit: observation is at the sha, not the working tree
  (is (false? (:observed (oc/check-path-exists {:repo "futon2" :sha "77cf311a" :path "src/futon2/aif/construction.clj"})))))

(deftest c4-decl
  (is (true? (:observed (oc/check-decl-in-file {:repo "mathlib4" :sha mathlib-sha
                                                 :path "DarkTower/WarMachine/EpistemicValue.lean"
                                                 :decl "theorem fixture_check_strictly_better"}))))
  (is (false? (:observed (oc/check-decl-in-file {:repo "mathlib4" :sha mathlib-sha
                                                  :path "DarkTower/WarMachine/EpistemicValue.lean"
                                                  :decl "theorem no_such_theorem"})))))

(deftest c5-registry
  (let [base {:repo "mathlib4" :sha "3726659d84"
              :bundle-path "DarkTower/WarMachine/machine-contracts-2026-09-17-r14/machine-contracts.json"}]
    (is (true? (:observed (oc/check-registry-entry (assoc base :entry "wm-machine-observe")))))
    (is (false? (:observed (oc/check-registry-entry (assoc base :entry "no-such-contract")))))))

(deftest refusals
  (is (= :no-locator (:kind (oc/check-path-exists {:repo "futon2" :sha futon2-sha}))))
  (is (= :unknown-sha (:kind (oc/check-path-exists {:repo "futon2" :sha "0000000000" :path "x"}))))
  (let [r (oc/observe {:t-path {:class :C3 :repo "futon2" :sha futon2-sha :path "src/futon2/aif/construction.clj"}
                       :t-judgement {:class :J}
                       :t-unknown {:class :C9}})]
    (is (= #{:t-path} (:observed r)))
    (is (= #{:t-judgement :t-unknown} (set (keys (:refused r)))))
    (is (every? #(= :no-mechanical-check (:kind %)) (vals (:refused r))))))

;; claude-7's applicability reading (2026-09-17): anchored declaration heads
(deftest c4-anchored-decl
  (let [text "theorem foo_bar : True := trivial\n-- theorem foo in a comment\n  theorem foo (x : Nat) : True := trivial\n"]
    (is (true? (oc/decl-present? text "theorem foo")))
    (is (false? (oc/decl-present? "theorem foo_bar : True := trivial\n" "theorem foo")))
    (is (false? (oc/decl-present? "-- theorem foo here\n" "theorem foo")))
    (is (true? (oc/decl-present? "(defn check-test-warrant\n  [x])" "(defn check-test-warrant")))))

(deftest c5-locus-resolves
  (let [base {:repo "mathlib4" :sha "52d6516922"
              :bundle-path "DarkTower/WarMachine/machine-contracts-2026-09-17-r15/machine-contracts.json"}
        r (oc/check-registry-entry (assoc base :entry "wm-token-observation"))]
    ;; every declared clojure-locus resolves at its repo's HEAD
    (is (true? (:observed r)) (pr-str (:evidence r)))
    (is (seq (get-in r [:evidence :clojure-loci])))))

;; ---------------------------------------------------------------------------
;; C8: a test namespace passed at the CURRENT content (AR-41).
;;
;; The registry is stubbed, not live: a record is named by the digest of its
;; own EDN, so a stub that hashes the same way is the real shape. The pinned
;; shas are taken from files that really exist in this checkout, so "current
;; content" means the bytes on disk while the test runs.

(def ^:private code-path "src/futon2/aif/observation_checks.clj")
(def ^:private test-path "test/futon2/aif/observation_checks_test.clj")
(def ^:private repo-dir (str oc/repo-root "/futon2"))

(defn- sha-now [path] (oc/content-sha (str repo-dir "/" path)))

(defn- stub-record
  "A registry run record, named by its digest the way append-record! names it."
  [record]
  (let [text (pr-str record)
        digest (#'oc/sha256-hex (.getBytes ^String text "UTF-8"))]
    {:entry-id (str "test-registry-" digest)
     :entry {:evidence/body {:payload-edn text :sha256 digest}}}))

(defn- run-record
  "A :matched, 0-failure run of THIS namespace pinned to the bytes on disk."
  [& {:keys [namespace code-files test-files postcheck results warrant?]
      :or {namespace "futon2.aif.observation-checks-test"
           code-files {code-path (sha-now code-path)}
           test-files {test-path (sha-now test-path)}
           postcheck {:status :matched}
           results {:tests 9 :assertions 40 :failures 0 :errors 0 :exit 0}
           warrant? true}}]
  {:schema "test-registry/v1" :kind :run :author "claude-9" :run/id "r-c8"
   :command ["clojure" "-M:test" "-n" namespace]
   :code-files code-files :test-files test-files
   :results results :postcheck postcheck :warrant? warrant?
   :execution/stable? true})

(defn- observe-c8
  "Run C8 against a stubbed registry holding RECORD (or nothing, for :absent)."
  [record & {:keys [namespace] :or {namespace "futon2.aif.observation-checks-test"}}]
  (let [{:keys [entry-id entry]} (stub-record (or record {}))]
    (binding [oc/*registry-entry* (fn [_base id]
                                    (if (and record (= id entry-id)) entry :absent))]
      (oc/check-registered-run {:repo "futon2" :namespace namespace :config entry-id}))))

(deftest c8-current-matched-warrant-is-observed
  ;; (v) a matched, current, 0-failure warrant reads true
  (let [r (observe-c8 (run-record))]
    (is (true? (:observed r)) (pr-str (:evidence r)))
    (is (= :C8 (:check r)))
    (is (= {:tests 9 :assertions 40 :failures 0 :errors 0} (get-in r [:evidence :run-counts])))
    (is (= [] (get-in r [:evidence :moved-paths])))
    ;; the evidence carries the pinned AND current sha of every declared path
    (is (every? (fn [p] (and (:pinned p) (= (:pinned p) (:current p))))
                (get-in r [:evidence :paths])))))

(deftest c8-warrant-pinned-to-older-content-is-false
  ;; (i) pinned to content that has since moved, with the differing path named
  (let [stale (apply str (repeat 64 "0"))
        r (observe-c8 (run-record :code-files {code-path stale}))]
    (is (false? (:observed r)))
    (is (= :content-moved (get-in r [:evidence :reason])))
    (is (= [code-path] (get-in r [:evidence :moved-paths])))
    (let [p (first (filter #(= code-path (:path %)) (get-in r [:evidence :paths])))]
      (is (= stale (:pinned p)))
      (is (= (sha-now code-path) (:current p)))
      (is (false? (:matched? p))))))

(deftest c8-warrant-for-another-namespace-is-false
  ;; (ii) the record is a fine warrant — for something else
  (let [r (observe-c8 (run-record :namespace "futon2.aif.some-other-test"))]
    (is (false? (:observed r)))
    (is (= :namespace-mismatch (get-in r [:evidence :reason])))
    (is (= "futon2.aif.some-other-test" (get-in r [:evidence :recorded-namespace])))))

(deftest c8-recorded-failure-is-false
  ;; (iii) matched and current, but the run itself recorded a failure
  (let [r (observe-c8 (run-record :results {:tests 9 :assertions 40 :failures 1 :errors 0 :exit 1}))]
    (is (false? (:observed r)))
    (is (= :run-recorded-failures (get-in r [:evidence :reason])))
    (is (= 1 (get-in r [:evidence :run-counts :failures]))))
  ;; an error counts the same way, and so does a count the registry could not read
  (is (= :run-recorded-failures
         (get-in (observe-c8 (run-record :results {:tests 9 :failures 0 :errors 2}))
                 [:evidence :reason])))
  (is (= :run-recorded-failures
         (get-in (observe-c8 (run-record :results {:failures {:record/type :none} :errors 0}))
                 [:evidence :reason]))))

(deftest c8-no-entry-is-false-not-a-refusal
  ;; (iv) the registry answered and holds no such record
  (let [r (observe-c8 nil)]
    (is (false? (:observed r)))
    (is (nil? (:status r)))
    (is (= :no-entry (get-in r [:evidence :reason])))))

(deftest c8-postcheck-not-matched-is-false
  (let [r (observe-c8 (run-record :postcheck {:record/type :test-registry/refusal
                                              :reason :inputs-changed-during-run}))]
    (is (false? (:observed r)))
    (is (= :postcheck-not-matched (get-in r [:evidence :reason])))))

(deftest c8-refusals
  ;; (vi) a locator missing :namespace refuses, and names what is missing
  (let [r (oc/check-registered-run {:repo "futon2" :config "test-registry-x"})]
    (is (= :no-locator (:kind r)))
    (is (= [:namespace] (get-in r [:data :missing]))))
  ;; a :config that names no record is a malformed locator, not a false reading
  (is (= :no-record-id (:kind (oc/check-registered-run
                               {:repo "futon2" :namespace "futon2.x-test"
                                :config "not-an-entry-id"}))))
  ;; a registry that cannot be reached refuses; it does not read false. The
  ;; real reader is exercised here (no stub), against a port nothing serves.
  (with-redefs [oc/agency-base (constantly "http://127.0.0.1:1")]
    (let [r (oc/check-registered-run
             {:repo "futon2" :namespace "futon2.x-test"
              :config (str "test-registry-" (apply str (repeat 64 "a")))})]
      (is (= :registry-unreadable (:kind r)))
      (is (nil? (:observed r)))))
  ;; a body whose text does not hash to the id it was fetched under is an
  ;; unreadable registry, not an observation about tests
  (binding [oc/*registry-entry*
            (fn [_ _] {:evidence/body {:payload-edn (pr-str (run-record)) :sha256 "deadbeef"}})]
    (is (= :registry-unreadable
           (:kind (oc/check-registered-run
                   {:repo "futon2" :namespace "futon2.aif.observation-checks-test"
                    :config (str "test-registry-" (apply str (repeat 64 "b")))}))))))

(deftest c8-is-registered-in-the-checks-map
  (is (= oc/check-registered-run (:C8 oc/checks)))
  ;; and an unknown class is still refused rather than observed absent
  (let [r (oc/observe {:t-unknown {:class :C9}})]
    (is (= :no-mechanical-check (:kind (get-in r [:refused :t-unknown]))))))

;; ---------------------------------------------------------------------------
;; AR-42: C8 resolves :namespace on its own, through futon3c's lookup.
;; The rule for "newest run" lives in the registry; this side only asks.

(deftest c8-resolves-a-namespace-with-no-config
  (let [{:keys [entry-id entry]} (stub-record (run-record))
        asked (atom [])]
    (binding [oc/*registry-latest* (fn [base ns]
                                     (swap! asked conj [base ns])
                                     {:entry-id entry-id :resolved-by :namespace-lookup})
              oc/*registry-entry* (fn [_ id] (if (= id entry-id) entry :absent))]
      (let [r (oc/check-registered-run {:repo "futon2"
                                        :namespace "futon2.aif.observation-checks-test"})]
        (is (true? (:observed r)) (pr-str (:evidence r)))
        (is (= 1 (count @asked)) "the lookup is asked exactly once")
        (is (= "futon2.aif.observation-checks-test" (second (first @asked))))
        (is (= :namespace-lookup (get-in r [:evidence :resolved-by])))
        (is (= entry-id (get-in r [:evidence :warrant-id])))))))

(deftest c8-still-judges-the-record-the-lookup-returned
  ;; the lookup finds the newest run whether or not it passed, so C8 must judge
  ;; it: a namespace whose newest run failed reads FALSE, not true
  (let [{:keys [entry-id entry]} (stub-record
                                  (run-record :results {:tests 9 :failures 2 :errors 0}))]
    (binding [oc/*registry-latest* (fn [_ _] {:entry-id entry-id :resolved-by :namespace-lookup})
              oc/*registry-entry* (fn [_ _] entry)]
      (let [r (oc/check-registered-run {:repo "futon2"
                                        :namespace "futon2.aif.observation-checks-test"})]
        (is (false? (:observed r)))
        (is (= :run-recorded-failures (get-in r [:evidence :reason])))))))

(deftest c8-verifies-the-namespace-it-asked-for
  ;; defence in depth: the lookup promises a record for this namespace, and C8
  ;; checks the record's own command anyway
  (let [{:keys [entry-id entry]} (stub-record (run-record :namespace "futon2.aif.other-test"))]
    (binding [oc/*registry-latest* (fn [_ _] {:entry-id entry-id :resolved-by :namespace-lookup})
              oc/*registry-entry* (fn [_ _] entry)]
      (is (= :namespace-mismatch
             (get-in (oc/check-registered-run {:repo "futon2"
                                               :namespace "futon2.aif.observation-checks-test"})
                     [:evidence :reason]))))))

(deftest c8-reads-a-typed-none-as-false-and-an-unreachable-lookup-as-a-refusal
  ;; the registry answered and holds no run for this namespace
  (binding [oc/*registry-latest* (fn [_ _] :absent)]
    (let [r (oc/check-registered-run {:repo "futon2" :namespace "futon2.aif.nothing-test"})]
      (is (false? (:observed r)))
      (is (nil? (:status r)))
      (is (= :no-entry (get-in r [:evidence :reason])))
      (is (= :namespace-lookup (get-in r [:evidence :resolved-by])))))
  ;; the lookup could not be reached: nothing was observed about any registry
  (binding [oc/*registry-latest* (fn [_ _] {:status :missing :kind :registry-unreadable
                                            :data {:check :C8}})]
    (let [r (oc/check-registered-run {:repo "futon2" :namespace "futon2.aif.nothing-test"})]
      (is (= :registry-unreadable (:kind r)))
      (is (nil? (:observed r)))))
  ;; :namespace is still required, and a :config given as blank is malformed
  (is (= [:namespace] (get-in (oc/check-registered-run {:repo "futon2"}) [:data :missing])))
  (is (= [:config] (get-in (oc/check-registered-run {:repo "futon2" :namespace "n" :config ""})
                           [:data :missing]))))

(deftest c8-lookup-reader-maps-the-registry-answers
  (let [respond (fn [status body]
                  (with-redefs [http/get (fn [_ _] {:status status :body body})]
                    (oc/fetch-latest-for-namespace "http://127.0.0.1:7070" "demo-test")))]
    ;; found: the id, for this side to read and verify itself
    (is (= {:entry-id "test-registry-abc" :resolved-by :namespace-lookup}
           (respond 200 (json/write-str {:latest {:found true :entry-id "test-registry-abc"}}))))
    ;; absence established by the registry
    (is (= :absent (respond 200 (json/write-str {:latest {:found false
                                                          :reason "no-run-for-namespace"}}))))
    ;; absence NOT established: the scan filled its window. This is not an
    ;; observation that nothing is registered, so it refuses.
    (let [r (respond 200 (json/write-str {:latest {:found false
                                                   :reason "scan-window-exhausted"}}))]
      (is (= :registry-unreadable (:kind r)))
      (is (= :scan-window-exhausted (get-in r [:data :reason]))))
    ;; the endpoint is not there yet, or answered rubbish
    (is (= :registry-unreadable (:kind (respond 404 ""))))
    (is (= :registry-unreadable (:kind (respond 200 "not json"))))
    (is (= :unparseable-response (get-in (respond 200 "not json") [:data :reason])))))
