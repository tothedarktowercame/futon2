(ns futon2.aif.observation-checks-test
  "Checks against real pinned shas in futon2 and mathlib4."
  (:require [babashka.http-client :as http]
            [clojure.data.json :as json]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
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
  ;; exactly one of :namespace / :command is required (neither refuses, naming
  ;; the rule), and a :config given as blank is malformed
  (is (= :exactly-one-of-namespace-or-command
         (get-in (oc/check-registered-run {:repo "futon2"}) [:data :rule])))
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

(deftest c8-lookup-asks-for-a-small-page-and-a-refusal-says-how-far-it-looked
  ;; 2026-09-25, live: with no limit the endpoint's default page does not
  ;; answer within this client's 5s timeout, so C8 refused :registry-unreadable
  ;; for EVERY namespace, present or absent. The client asks for an explicit
  ;; small page, and a refusal carries the endpoint's own accounting of how
  ;; far it looked.
  (let [seen (atom nil)]
    (with-redefs [http/get (fn [url _]
                             (reset! seen url)
                             {:status 200
                              :body (json/write-str {:latest {:found false
                                                              :reason "scan-window-exhausted"
                                                              :scanned 100
                                                              :registry-entries 3108}})})]
      (let [r (oc/fetch-latest-for-namespace "http://127.0.0.1:7070" "demo-test")]
        (is (str/includes? @seen "namespace=demo-test"))
        (is (str/includes? @seen "&limit=100"))
        (is (= :scan-window-exhausted (get-in r [:data :reason])))
        (is (= 100 (get-in r [:data :scanned])))
        (is (= 3108 (get-in r [:data :registry-entries])))))))

;; ---------------------------------------------------------------------------
;; E-kimi-task-28: C8 resolves a COMMAND, not only a namespace.
;; A gate run (["bb" "scripts/gates.clj"], a shell VERIFY exit, ["lake" "build"
;; <Module>]) names no -n namespace, so a namespace-only consumer refused it
;; before any read. The command lookup is the registry's own (futon3c
;; E-kimi-task-19); this side asks with ?command=<pr-str> and reads the same
;; reply shape.

(deftest c8-resolves-a-command-with-no-config
  (let [{:keys [entry-id entry]} (stub-record (run-record))
        asked (atom [])]
    (binding [oc/*registry-latest* (fn [base locator]
                                     (swap! asked conj [base locator])
                                     {:entry-id entry-id :resolved-by :command-lookup})
              oc/*registry-entry* (fn [_ id] (if (= id entry-id) entry :absent))]
      (let [cmd ["clojure" "-M:test" "-n" "futon2.aif.observation-checks-test"]
            r (oc/check-registered-run {:repo "futon2" :command cmd})]
        (is (true? (:observed r)) (pr-str (:evidence r)))
        (is (= 1 (count @asked)) "the lookup is asked exactly once")
        (is (= {:command cmd} (second (first @asked)))
            "the seam is handed the command locator, not a namespace string")
        (is (= :command-lookup (get-in r [:evidence :resolved-by])))
        (is (= entry-id (get-in r [:evidence :warrant-id])))))))

(deftest c8-command-no-entry-is-false-not-a-refusal
  ;; the registry answered and holds no run for this command: observed false,
  ;; :no-entry, exactly as the namespace path reads a typed absence
  (binding [oc/*registry-latest* (fn [_ _] :absent)]
    (let [r (oc/check-registered-run {:repo "futon2" :command ["bb" "scripts/gates.clj"]})]
      (is (false? (:observed r)))
      (is (nil? (:status r)))
      (is (= :no-entry (get-in r [:evidence :reason])))
      (is (= :command-lookup (get-in r [:evidence :resolved-by]))))))

(deftest c8-command-locator-refusals
  ;; both :namespace and :command refuses, and names the rule
  (let [r (oc/check-registered-run {:repo "futon2" :namespace "futon2.x-test"
                                    :command ["bb" "scripts/gates.clj"]})]
    (is (= :no-locator (:kind r)))
    (is (= :exactly-one-of-namespace-or-command (get-in r [:data :rule]))))
  ;; neither refuses the same way
  (let [r (oc/check-registered-run {:repo "futon2"})]
    (is (= :no-locator (:kind r)))
    (is (= :exactly-one-of-namespace-or-command (get-in r [:data :rule]))))
  ;; an empty vector is not a command
  (is (= :no-locator
         (:kind (oc/check-registered-run {:repo "futon2" :command []})))))

(deftest c8-command-lookup-reader-maps-the-registry-answers
  (let [respond (fn [status body]
                  (with-redefs [http/get (fn [_ _] {:status status :body body})]
                    (oc/fetch-latest-for-command "http://127.0.0.1:7070"
                                                 ["bb" "scripts/gates.clj"])))]
    ;; found: the id, resolved by the command lookup
    (is (= {:entry-id "test-registry-abc" :resolved-by :command-lookup}
           (respond 200 (json/write-str {:latest {:found true :entry-id "test-registry-abc"}}))))
    ;; typed absence: no-run-for-command reads :absent
    (is (= :absent (respond 200 (json/write-str {:latest {:found false
                                                          :reason "no-run-for-command"}}))))
    ;; a 400 is the endpoint refusing the question; its named reason is carried
    (let [r (respond 400 (json/write-str {:record/type "test-registry/refusal"
                                          :reason "invalid-command"}))]
      (is (= :registry-unreadable (:kind r)))
      (is (= :invalid-command (get-in r [:data :reason])))
      (is (= 400 (get-in r [:data :status]))))
    ;; any other not-found reason stays :registry-unreadable
    (let [r (respond 200 (json/write-str {:latest {:found false
                                                   :reason "scan-window-exhausted"}}))]
      (is (= :registry-unreadable (:kind r)))
      (is (= :scan-window-exhausted (get-in r [:data :reason]))))))

(deftest c8-command-lookup-url-encodes-the-pr-str
  ;; bad case watched: a command whose pr-str contains spaces must arrive
  ;; intact through URL encoding — the stub asserts the query it received.
  (let [seen (atom nil)]
    (with-redefs [http/get (fn [url _]
                             (reset! seen url)
                             {:status 200
                              :body (json/write-str {:latest {:found false
                                                              :reason "no-run-for-command"}})})]
      (is (= :absent (oc/fetch-latest-for-command
                      "http://127.0.0.1:7070" ["sh" "-c" "verify exit 0"])))
      (let [query (second (str/split @seen #"\?" 2))
            command-param (first (filter #(str/starts-with? % "command=")
                                         (str/split query #"&")))
            encoded (second (str/split command-param #"=" 2))
            decoded (java.net.URLDecoder/decode encoded "UTF-8")]
        (is (not (str/includes? encoded " ")) "no raw space crosses the wire")
        (is (= "[\"sh\" \"-c\" \"verify exit 0\"]" decoded)
            "the pr-str arrives intact")
        (is (= ["sh" "-c" "verify exit 0"] (edn/read-string decoded))
            "and reads back as the same vector")))))

;; ONE live-pinned case (E-kimi-task-28): the command lookup and the namespace
;; lookup observe the same entry on the live :7070. Pinned 2026-09-25T03:23Z,
;; when the rebuilt ledger's marker read :scanned 3200 = :registry-entries,
;; :complete? true and the newest run of futon3c.test-registry-test was the
;; 03:21:59Z warranted run. If the registry has since seen a newer run of that
;; namespace, the pin — not the lookup — is what moved.
(def ^:private live-pinned-entry-id
  "test-registry-06f03cf1551c33455e13e82c59bd4deff97f40bb6bd6de3bf3a07c972396044e")

(deftest c8-live-command-lookup-observes-the-same-entry-as-the-namespace-lookup
  (let [base (oc/agency-base)
        cmd ["clojure" "-M:test" "-n" "futon3c.test-registry-test"]
        by-command (oc/fetch-latest-for-command base cmd)
        by-namespace (oc/fetch-latest-for-namespace base "futon3c.test-registry-test")]
    (is (= live-pinned-entry-id (:entry-id by-command)) (pr-str by-command))
    (is (= live-pinned-entry-id (:entry-id by-namespace)) (pr-str by-namespace))
    (is (= :command-lookup (:resolved-by by-command)))
    (is (= :namespace-lookup (:resolved-by by-namespace))))
  ;; the marker this pin was taken against: a complete command-keyed build
  (let [ledger-file (io/file "/home/joe/code/futon3c/data/test-registry/namespace-ledger.edn")
        entries (with-open [r (java.io.PushbackReader. (io/reader ledger-file))]
                  (loop [acc []]
                    (let [form (edn/read {:eof ::eof} r)]
                      (if (= ::eof form) acc (recur (conj acc form))))))
        marker (last (filter #(= :namespace-ledger-built (:entry/type %)) entries))]
    (is (some? marker) "the ledger carries a build marker")
    (is (= 3200 (:scanned marker)))
    (is (= 3200 (:registry-entries marker)))
    (is (true? (:complete? marker)))))

(deftest c8-reads-a-run-record-s-counts-by-shape
  ;; review bad case (claude-8, 2026-09-25, of b9eae2d6): the first live gate
  ;; run looked up by command, a green lake build, read :run-recorded-failures
  ;; because the Lean arm records :error-count/:sorry-count and :failures was
  ;; nil. Absence is not a failure: counts are read by shape, and a record
  ;; with neither shape is a typed unknown.
  (is (= :clean (oc/results-verdict {:failures 0 :errors 0})))
  (is (= :clean (oc/results-verdict {:error-count 0 :sorry-count 0 :jobs 8501})))
  (is (= :failed (oc/results-verdict {:failures 1 :errors 0})))
  (is (= :failed (oc/results-verdict {:error-count 0 :sorry-count 2})))
  (is (= :unknown (oc/results-verdict {:exit 0})))
  (is (= :unknown (oc/results-verdict nil)))
  (let [lookup (fn [record]
                 (let [{:keys [entry-id entry]} (stub-record record)]
                   (binding [oc/*registry-latest* (fn [_ _] {:entry-id entry-id :resolved-by :command-lookup})
                             oc/*registry-entry* (fn [_ id] (if (= id entry-id) entry :absent))]
                     (oc/check-registered-run {:repo "futon2" :command (:command record)}))))
        lean (assoc (run-record) :command ["lake" "build" "DarkTower.WarMachine.X"]
                    :results {:exit 0 :error-count 0 :sorry-count 0 :jobs 8501 :duration-ms 3389})]
    ;; a green Lean build observes true and its counts are carried
    (let [r (lookup lean)]
      (is (true? (:observed r)) (pr-str (:evidence r)))
      (is (= {:error-count 0 :sorry-count 0 :jobs 8501} (get-in r [:evidence :run-counts]))))
    ;; a Lean build with a sorry is a recorded failure
    (is (= :run-recorded-failures
           (get-in (lookup (assoc-in lean [:results :sorry-count] 1)) [:evidence :reason])))
    ;; a record with neither shape is a typed unknown, not a failure
    (is (= :results-shape-unknown
           (get-in (lookup (assoc lean :results {:exit 0})) [:evidence :reason])))))

(deftest c8-command-elements-must-be-nonblank-strings
  ;; review bad case (claude-8, 2026-09-25, of d5320918): a command with a
  ;; keyword, a nested vector or a blank element was sent to the registry as
  ;; a key no run can carry and read false :no-entry forever. A malformed
  ;; command is a locator refusal naming the offending elements.
  (binding [oc/*registry-latest* (fn [_ _] (throw (ex-info "registry must not be asked" {})))]
    (doseq [[command offending] [[["lake" :build "X"] [:build]]
                                 [[["bb"] "g.clj"] [["bb"]]]
                                 [["bb" ""] [""]]]]
      (let [r (oc/check-registered-run {:repo "futon3c" :command command})]
        (is (= :no-locator (:kind r)) (pr-str command))
        (is (= :command-elements-must-be-nonblank-strings (get-in r [:data :rule])))
        (is (= offending (get-in r [:data :offending])))))
    ;; a well-formed argv still goes through to the registry
    (binding [oc/*registry-latest* (fn [_ _] :absent)]
      (is (= :no-entry (get-in (oc/check-registered-run {:repo "futon3c" :command ["bb" "g.clj"]})
                               [:evidence :reason]))))))
