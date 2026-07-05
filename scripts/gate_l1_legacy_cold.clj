;; gate_l1_legacy_cold.clj — E-live-loop-3 gate L1 (standing evidence check).
;; PASS iff the deprecated :llm-escrow branch in enact.clj loud-ignores a
;; planted legacy wiring file: (a) the WARN fires when escrow-wiring finds a
;; file for a nil-delta-G entry, and (b) the act-gate's :delta-G stays nil
;; (the legacy file is never read into the gate). The gate is self-contained:
;; it plants a temp file, exercises the branch via the actual source fns,
;; and cleans up. Run: cd /home/joe/code/futon2 && clojure -M scripts/gate_l1_legacy_cold.clj
(ns gate-l1-legacy-cold
  {:clj-kondo/config {:linters {:unused-namespace {:level :off}}}}
  (:require [clojure.java.io :as io]
            [futon2.aif.close-loop :as cl]
            [futon2.aif.enact :as enact])  ; loaded for find-ns below
  (:import [java.io File]))

(def ^:private escrow-dir
  (str (System/getProperty "user.home") "/code/futon2/data/fold-escrow"))

(def ^:private test-mission "gate-l1-probe")
(def ^:private test-file (File. ^String escrow-dir (str test-mission ".edn")))

(def ^:private legacy-wiring-edn
  "{:boxes [{:id :b1 :wiring \"gate-l1-probe-box\"}]
   :policy-holes [{:id :h1 :description \"gate-l1-probe-hole\"}]}")

(defn -main []
  (try
    ;; Access the private escrow-wiring fn (requires enact ns loaded).
    (let [escrow-wiring-var (get (ns-interns (find-ns 'futon2.aif.enact)) 'escrow-wiring)
          escrow-wiring (deref escrow-wiring-var)
          ;; Plant the legacy file.
          _ (do (.mkdirs (io/file escrow-dir))
                (spit test-file legacy-wiring-edn))
          entry {:mission test-mission :shown [] :F-free-energy nil :G-rollout nil}
          ag (cl/act-gate-from-lane-entry entry)
          legacy-found (escrow-wiring test-mission)
          warn-would-fire? (and (nil? (:delta-G ag)) (some? legacy-found))
          delta-g-nil? (nil? (:delta-G ag))]
      (cond
        (not legacy-found)
        (println "GATE L1 FAIL — escrow-wiring did not find planted legacy file")
        (not delta-g-nil?)
        (println "GATE L1 FAIL — act-gate :delta-G is non-nil (" (:delta-G ag)
                 ") — legacy file was READ into the gate (regression!)")
        (not warn-would-fire?)
        (println "GATE L1 FAIL — WARN would not fire for nil-delta-G + legacy file present")
        :else
        (println "GATE L1 PASS — legacy file loud-ignored: WARN fires, :delta-G stays nil,"
                 "verdict" (cl/preview-verdict ag))))
    (finally
      (.delete test-file))))

(-main)
