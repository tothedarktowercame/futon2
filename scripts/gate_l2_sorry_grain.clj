;; gate_l2_sorry_grain.clj — E-live-loop-3 gate L2 (standing evidence check).
;; PART A (pass/fail): a synthetic known-held mission produces sorry-grain ψ
;; through the real mission->psi path. PART B (informational only, never
;; failing): prints the current live-lane intersection count and names.
;; Run: cd /home/joe/code/futon2 && clojure -M scripts/gate_l2_sorry_grain.clj
(ns gate-l2-sorry-grain
  {:clj-kondo/config {:linters {:unused-namespace {:level :off}}}}
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.report.cascade-lane :as lane]))

(def ^:private test-mission "M-live-geometric-stack")

(defn- live-lane-missions
  "Read the latest WM trace's :ranked-actions open-mission targets."
  []
  (try
    (let [trace-dir (io/file (str (System/getProperty "user.home") "/code/futon2/data/wm-trace"))
          trace-files (sort-by #(.getName %) (filter #(re-matches #".*\.edn" (.getName %)) (.listFiles trace-dir)))
          latest (last trace-files)]
      (when latest
        (let [trace (edn/read-string (slurp latest))]
          (->> (:ranked-actions trace)
               (filter #(#{:open-mission "open-mission"} (get-in % [:action :type])))
               (map #(get-in % [:action :target]))
               distinct))))
    (catch Throwable _ [])))

(defn -main []
  ;; Clear the psi cache so we get fresh values from the new code.
  (let [cache-var (get (ns-interns (find-ns 'futon2.report.cascade-lane)) '!psi-cache)]
    (when cache-var (swap! @cache-var (fn [_] {}))))
  ;; PART A: sorry-grain psi for a known-held mission.
  (let [psi (lane/mission->psi test-mission)
        sorry? (boolean (re-find #"WANT:" (str psi)))]
    (if (not sorry?)
      (do (println "GATE L2 FAIL —" test-mission "did not produce sorry-grain ψ (no WANT: found)")
          (println "  psi was:" (pr-str psi))
          (System/exit 1))
      ;; PART A also checks banner fallback for a non-held mission.
      (let [cf-psi (lane/mission->psi "M-canon-fingerprint-store")
            banner? (boolean (re-find #"want:" (str cf-psi)))]
        (if (not banner?)
          (do (println "GATE L2 FAIL — M-canon-fingerprint-store did not fall back to banner ψ")
              (System/exit 1))
          (println "GATE L2 PART A PASS —" test-mission "-> sorry-grain ψ (WANT: visible),"
                   "M-canon-fingerprint-store -> banner ψ (want: visible)"))))
    ;; PART B: informational — live-lane intersection count (never fails).
    (let [lane-missions (live-lane-missions)
          held-count (count (filter identity (map #(when (re-find #"WANT:" (str (lane/mission->psi %))) %) lane-missions)))]
      (println "GATE L2 INFO — live-lane intersection:"
               held-count "/" (count lane-missions) "missions with held-work items."
               (when (seq lane-missions) (str "Lane:" (pr-str lane-missions)))))
    (System/exit 0)))

(-main)
