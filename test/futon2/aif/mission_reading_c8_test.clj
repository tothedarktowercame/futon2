(ns futon2.aif.mission-reading-c8-test
  "A C8 locator through the whole flight cycle (AR-41). The reading names a
  test namespace with no registered run: the locator validates, observes
  false and is published as the want's locator. A run is then registered at
  the current content. A registry id is the digest of its record, so the
  id published before the run can never name it: the published locator stays
  false, and a new locator reading carrying the new id observes true. Until a
  namespace index lands (AR-42), that re-read is how a registration reaches a
  want; a published locator is never edited in place."
  (:require [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.mission-reading :as mr]
            [futon2.aif.observation-checks :as oc]
            [futon2.aif.want-interpretation :as wi])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def roots (atom []))
(use-fixtures :each
  (fn [f] (try (f) (finally
                     (doseq [root @roots file (reverse (file-seq root))] (Files/delete (.toPath file)))
                     (reset! roots [])))))

(defn- store []
  (let [d (.toFile (Files/createTempDirectory "reading-store" (make-array FileAttribute 0)))]
    (swap! roots conj d) (.getCanonicalPath d)))

(def f11-text (slurp "test/fixtures/mission-criteria/M-f11-find-production-successor@futon2-22fa0da9.md"))
(def target "M-f11-find-production-successor")
(def test-ns-name "futon2.aif.mission-reading-c8-test")
(def code-path "src/futon2/aif/mission_reading.clj")
(def test-path "test/futon2/aif/mission_reading_c8_test.clj")

(defn- sha-now [path] (oc/content-sha (java.io.File. (str oc/repo-root "/futon2/" path))))

(defn- stub-record
  "A registry run record named by its digest, the way the registry names it."
  [record]
  (let [text (pr-str record)
        digest (#'oc/sha256-hex (.getBytes ^String text "UTF-8"))]
    {:entry-id (str "test-registry-" digest)
     :entry {:evidence/body {:payload-edn text :sha256 digest}}}))

(def registered
  "A :matched, 0-failure run of this namespace pinned to the bytes on disk."
  (delay (stub-record {:schema "test-registry/v1" :kind :run :author "claude-10" :run/id "r-c8-cycle"
                       :command ["clojure" "-M:test" "-n" test-ns-name]
                       :code-files {code-path (sha-now code-path)}
                       :test-files {test-path (sha-now test-path)}
                       :results {:tests 1 :assertions 1 :failures 0 :errors 0 :exit 0}
                       :postcheck {:status :matched} :warrant? true})))

(defn- registry
  "A stubbed registry holding ENTRIES (entry-id -> entry); anything else is absent."
  [entries]
  (fn [_base id] (get entries id :absent)))

;; an id of the registry's shape that no record hashes to: what a reading can
;; name before any run of the namespace exists
(def unregistered-id (str "test-registry-" (apply str (repeat 64 "a"))))

(defn- c8-reply [config]
  (fn [issued]
    (let [stated (str/replace (get-in issued [:criterion :stated]) #"^- " "")]
      (str "```edn\n"
           (pr-str {:schema mr/locator-schema
                    :locator {:class :C8 :repo "futon2" :namespace test-ns-name :config config}
                    :cue {:quote (subs stated 0 (min 20 (count stated)))}
                    :reading "the criterion is decided by this namespace passing at current content"
                    :by "claude-2"})
           "\n```"))))

(defn- answer-with [f]
  (let [n (atom 0)]
    (fn [issued]
      {:seat "claude-2" :job-id (str "job-" (swap! n inc)) :state "done"
       :text (case (:kind issued)
               :constraints (str "```edn\n" (pr-str {:schema mr/constraints-schema :constraints [] :by "claude-2"}) "\n```")
               :coverage (str "```edn\n" (pr-str {:schema mr/criteria-schema :criteria [] :scope-outs [] :anchors [] :by "claude-2"}) "\n```")
               (f issued))})))

(defn- f11-flight [s]
  ;; no :observe: the flight observes through observation-checks, so C8 runs
  ;; for real against the stubbed registry
  (flight/start {:target target :chosen-because {:kind :requested}}
                {:kind :a-exits :repo "futon2" :path "holes/missions/M-f11-find-production-successor.md"
                 :store s :read-text (fn [& _] f11-text)}
                {:id "flight-f11-c8"}))

(deftest c8-locator-full-cycle
  (let [s (store)
        f (f11-flight s)
        {new-id :entry-id new-entry :entry} @registered]
    (testing "no run registered: the C8 locator validates, reads false, and is published"
      (binding [oc/*registry-entry* (registry {})]
        (let [read ((fr/read-fn {:store s :answer-fn (answer-with (c8-reply unregistered-id))}) f {})
              after (flight/click-wants f {})
              published (mr/published-locators s target)]
          (is (= (repeat 6 :published) (map :outcome (filter #(= :locator (:kind %)) (:asked read)))))
          (is (empty? (:needs read)))
          (is (= 6 (count published)))
          (is (every? #(= {:class :C8 :repo "futon2" :namespace test-ns-name :config unregistered-id} %)
                      (vals published)))
          (is (= [] (get-in after [:source :unlocated])))
          (is (= (set (keys published)) (set (get-in after [:source :machine-located]))))
          (is (every? false? (map #(get (:universe after) %) (:wants after))))
          (is (= :no-entry (get-in (oc/check-registered-run (first (vals published))) [:evidence :reason]))))))
    (testing "a run registered at current content: the published locator still reads false"
      (binding [oc/*registry-entry* (registry {new-id new-entry})]
        (let [after (flight/click-wants f {})]
          (is (not= unregistered-id new-id))
          (is (every? false? (map #(get (:universe after) %) (:wants after))))
          (is (true? (:observed (oc/check-registered-run
                                 {:repo "futon2" :namespace test-ns-name :config new-id})))
              "the registered run itself reads true"))))
    (testing "a new locator reading with the new id is published and observes true"
      (binding [oc/*registry-entry* (registry {new-id new-entry})]
        (let [before (mr/published-locators s target)
              token (key (first before))
              criterion (get-in (flight/click-wants f {}) [:source :criteria-by-token token])
              issued (wi/issue! s (mr/locator-request target "M-f11" (assoc criterion :token token)))
              response (read-string (second (re-find #"(?s)```edn\n(.*)\n```" ((c8-reply new-id) issued))))
              v (mr/validate-locator issued response)]
          (is (= {:status :valid :observed true} (select-keys v [:status :observed])))
          (mr/publish-locator! s issued response v "claude-2")
          (let [after (flight/click-wants f {})]
            (is (= new-id (:config (get (mr/published-locators s target) token))))
            (is (true? (get (:universe after) token)))
            (is (= 5 (count (filter false? (map #(get (:universe after) %) (:wants after))))))))))))

;; A gate names a command, not a test namespace (the M-f11 line-37 case:
;; bb, sh and lake gates, declined because C8 then needed a namespace). The
;; locator carries :command and no :config, so the registry's command lookup
;; resolves it each click and no re-read is needed after the run registers.
(def gate ["lake" "build" "DarkTower.WarMachine.F11AppliedConformance"])

(def gate-run
  (delay (stub-record {:schema "test-registry/v1" :kind :run :author "claude-10" :run/id "r-gate"
                       :command gate
                       :code-files {code-path (sha-now code-path)}
                       :test-files {}
                       :results {:error-count 0 :sorry-count 0 :jobs 1 :exit 0}
                       :postcheck {:status :matched} :warrant? true})))

(defn- gate-reply [issued]
  (let [stated (str/replace (get-in issued [:criterion :stated]) #"^- " "")]
    (str "```edn\n"
         (pr-str {:schema mr/locator-schema
                  :locator {:class :C8 :repo "futon2" :command gate}
                  :cue {:quote (subs stated 0 (min 20 (count stated)))}
                  :reading "the criterion is decided by this gate passing at current content"
                  :by "claude-2"})
         "\n```")))

(deftest c8-command-locator-full-cycle
  (let [s (store)
        f (f11-flight s)
        {run-id :entry-id run-entry :entry} @gate-run
        asked (atom [])]
    (testing "no run for the command: the locator validates, reads false :no-entry, and is published"
      (binding [oc/*registry-latest* (fn [_ loc] (swap! asked conj loc) :absent)
                oc/*registry-entry* (registry {})]
        (let [read ((fr/read-fn {:store s :answer-fn (answer-with gate-reply)}) f {})
              after (flight/click-wants f {})
              published (mr/published-locators s target)]
          (is (= (repeat 6 :published) (map :outcome (filter #(= :locator (:kind %)) (:asked read)))))
          (is (empty? (:needs read)))
          (is (every? #(= {:class :C8 :repo "futon2" :command gate} %) (vals published)))
          (is (every? false? (map #(get (:universe after) %) (:wants after))))
          (is (some #(= {:command gate} %) @asked) "asked by command, not by namespace")
          (is (= :no-entry (get-in (oc/check-registered-run (first (vals published))) [:evidence :reason]))))))
    (testing "the gate's run registered at current content: the SAME published locator reads true"
      (binding [oc/*registry-latest* (fn [_ loc] (if (= {:command gate} loc) {:entry-id run-id :resolved-by :command-lookup} :absent))
                oc/*registry-entry* (registry {run-id run-entry})]
        (let [before (mr/published-locators s target)
              after (flight/click-wants f {})]
          (is (= before (mr/published-locators s target)) "nothing re-read or patched")
          (is (every? true? (map #(get (:universe after) %) (:wants after)))))))))

(deftest a-command-locator-with-config-is-the-checks-refusal
  ;; the rule is the check's, not a second copy in the validator: a command
  ;; with :config refuses in the check and comes back :check-refused
  (let [issued {:kind :locator :target target :want {:token :exit/hx}
                :criterion {:stated "- Run the same gates as F12"}}
        v (mr/validate-locator issued {:locator {:class :C8 :repo "futon2" :command gate :config "x"}
                                       :cue {:quote "Run the same gates"} :reading "r"})]
    (is (= :rejected (:status v)))
    (is (= :check-refused (:reason (first (:reasons v)))))))
