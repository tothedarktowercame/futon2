(ns futon2.aif.uniform-run-record-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.enumeration-completeness :as enumeration]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.locator-fixtures :as locfix]
            [futon2.aif.policy :as policy]
            [futon2.report.cascade-decision-test :as tick]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn- temp-dir []
  (.toFile (Files/createTempDirectory "uniform-record-" (make-array FileAttribute 0))))

(defn- counts []
  (into {} (for [[k root] [[:repair hermetic/production-repair-root]
                          [:trip hermetic/production-trip-root]]]
             [k (count (filter #(.isFile %) (file-seq (io/file root))))])))

(def live-sources
  ;; The existing live-C producer's read-sources shape, from live_c_test.
  ;; Mission-grain wants intentionally do not overlap tick-001's qualified
  ;; cascade outcomes. The producer, not this test, records the fallback.
  {:wholeness {:path "/fixture/w" :sha256 "w"
               :value {:missions [{:mission "M-a" :class :alive :L 5 :T 1 :H 5}]}}
   :missions [{:path "/fixture/m" :sha256 "m" :mission "M-a" :text "**Status:** OPEN."}]
   :stars {:path "/fixture/s" :sha256 "s" :value {:capabilities {}}}})

(deftest scoring-provenance-keeps-every-candidates-value
  (let [entries [{:action :a :controller-score 1 :f 0
                  :certificate {:c {:status :derived :signature "a"}
                                :rates-provenance {:status :identity-default}}}
                 {:action :b :controller-score 2 :f 0
                  :certificate {:c {:status :derived-no-overlap :signature "b"}
                                :rates-provenance {:status :declared-in-opts}}}]
        decision (policy/select-action-cascades entries {:beta 1})
        scoring (get-in decision [:selection-certificate :scoring])]
    (doseq [[i entry] (map-indexed vector entries)]
      (is (= (:action entry) (get-in scoring [i :id])))
      (is (= (:certificate entry) (dissoc (get scoring i) :id))))))

(deftest offline-tick-record-is-valid-with-flags-preserved
  (let [dir (temp-dir)
        before (counts)
        mission (io/file dir "corpus/repo/holes/missions/M-present.md")]
    (try
      (io/make-parents mission)
      (spit mission "# M-present\n\nStatus: ACTIVE\n")
      (let [assembled (cp/assemble {:targets [tick/tick-1-target]
                                    :sources (locfix/locate-all tick/tick-1-sources)})
            result (wm/cascade-decision assembled
                                        {:live-c {:sources live-sources :sources-now live-sources}})
            ;; Use the same U37 producer/carry as judge, against an isolated
            ;; corpus. It must preserve its membership diff, not invent a
            ;; complete verdict to make a validity check green.
            projected (with-redefs [enumeration/default-code-root (str (io/file dir "corpus"))]
                        (binding [enumeration/*enumeration-assert?* true]
                          (#'wm/carry-enumeration-completeness result)))
            decision (:decision projected)
            written (#'runner/persist-run-record!
                     {:run-record-dir (str (io/file dir "records"))}
                     "offline-tick-001-validity" "2026-09-19T00:00:00Z"
                     {:outcome :offline-selection-replay
                      :checkpoints {:selection {:judgment {:controller-decision decision}}}})
            record (edn/read-string (slurp (:run-record written)))
            carried (:decision record)
            check (shell/sh "bb" "scripts/wm_run_validity.bb" (:run-record written))
            lines (filter #(re-find #"^\s+(c-source|rates-provenance|posterior|u37|g-terms)\s" %)
                          (str/split-lines (:out check)))
            after (counts)]
        (is (= before after))
        (is (false? (:traceWritten record)))
        (is (not (contains? record :g-term-decomposition)))
        (is (= (get-in decision [:selection-certificate :g-term-decomposition])
               (:g-term-decomposition carried)))
        (doseq [k [:selection-law :selection-certificate :enumeration-completeness]]
          (is (= (get decision k) (get carried k)) (str k)))
        (is (seq (get-in carried [:enumeration-completeness :kinds])))
        (is (= :incomplete (get-in carried [:enumeration-completeness :verdict])))
        (is (some #(= ["M-present"] (:missing %))
                  (get-in carried [:enumeration-completeness :kinds])))
        (is (every? #(= :computed-not-attached (:f-status %))
                    (get-in carried [:selection-certificate :policies])))
        (is (every? #(= :derived-no-overlap (get-in % [:c :status]))
                    (vals (get-in carried [:selection-certificate :scoring]))))
        (is (zero? (:exit check)) (pr-str check))
        (is (str/includes? (:out check) "SELFTEST PASS"))
        (is (str/includes? (:out check) "VALID (3/5 ok)") (:out check))
        (is (= 5 (count lines)))
        (doseq [line lines]
          (is (str/includes? line "[:decision") line)
          (is (not (re-find #"\s(missing|bad)\s" line)) line))
        (println "UNIFORM-RUN-RECORD-RECEIPT"
                 (pr-str {:scope :offline-selection-replay :source :vm/tick-001
                          :injected-sources [:live-c :enumeration-corpus :locators]
                          :run-record record :checker check
                          :store-counts {:before before :after after}})))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))

(deftest no-selection-does-not-borrow-historical-quantities
  (let [dir (temp-dir)]
    (try
      (let [written (#'runner/persist-run-record!
                     {:run-record-dir (str dir)} "offline-no-selection" "2026-09-19T00:00:00Z"
                     {:outcome :offline-no-selection
                      :backtrace {:decision {:selection-law {:posterior {:a 1/2 :b 1/2}}
                                             :c {:status :derived}}}})
            record (edn/read-string (slurp (:run-record written)))
            check (shell/sh "bb" "scripts/wm_run_validity.bb" (:run-record written))]
        (is (= #{:g-term-decomposition} (set (keys (:decision record)))))
        (is (= :missing (get-in record [:decision :g-term-decomposition :status])))
        (is (str/includes? (:out check) "INVALID"))
        (is (zero? (:exit check)) "Invalidity reports; it does not gate the run."))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))
