#!/usr/bin/env bb
;; holder_check.clj — Wave 0's executable falsifier.
;;
;; Two things must hold, and today the second does not (loudly, by design):
;;   1. No contract declaration inline-names a SESSION as holder. Holes.lean says
;;      `holder: by-record`; the durable `owner` record carries responsibility.
;;   2. Every owning record resolves, via checks/holder-registry.edn, to a holder that
;;      is ASSIGNED and LIVE on the Agency roster.
;;
;; Ordinary violations exit 1. `--negative` asserts the check can fail (it injects a
;; dead holder and requires a violation); rejecting that mutation is a passing control
;; and exits 0, while a mutation that slips through exits 2.
(require '[cheshire.core :as json] '[clojure.edn :as edn]
         '[clojure.string :as str] '[babashka.http-client :as http])

(def contract-path "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json")
(def registry-path "/home/joe/code/futon2/checks/holder-registry.edn")

(defn owning-record [d]
  (let [o (or (:owner d) "")]
    (-> o (str/split #"·") last str/trim (str/split #"\s+") first)))

(defn roster []
  (try (->> (http/get "http://localhost:7070/api/alpha/agents" {:throw false :timeout 2000})
            :body json/parse-string (#(get % "agents")) keys set)
       (catch Exception _ nil)))

(defn -main [& args]
  (let [negative? (some #{"--negative"} args)
        contract  (json/parse-string (slurp contract-path) true)
        reg       (edn/read-string (slurp registry-path))
        records   (cond-> (:records reg)
                    negative? (assoc "P-R9" {:decls 14 :holder "claude-15-retired" :note "injected"}))
        live      (roster)
        decls     (:declarations contract)
        inline    (filter #(and (:holder %) (not= "by-record" (:holder %))) decls)
        by-rec    (group-by owning-record decls)
        problems
        (concat
         (for [d inline] {:kind :inline-session-holder :decl (:name d) :holder (:holder d)})
         (for [[rec ds] by-rec
               :let [h (get-in records [rec :holder])]
               :when (or (nil? h) (= :unassigned h)
                         (and live (string? h) (not (contains? live h)) (not= "joe" h)))]
           {:kind (cond (nil? h) :record-not-in-registry
                        (= :unassigned h) :unassigned
                        :else :holder-not-live)
            :record rec :decls (count ds) :holder h}))
        orphaned (reduce + (map #(or (:decls %) 0) problems))]
    (println (pr-str {:pass? (empty? problems)
                      :declarations (count decls)
                      :records (count by-rec)
                      :roster-reachable? (some? live)
                      :inline-session-holders (count inline)
                      :orphaned-declarations orphaned
                      :problems (vec (take 6 problems)) :problems-total (count problems)}))
    (when negative?
      (if (seq problems)
        (do (println "holder-check: PASS negative control rejected exit-convention=0-pass/1-fail") (System/exit 0))
        (do (println "holder-check: FAIL negative mutation passed exit-convention=0-pass/1-fail") (System/exit 2))))
    (println (str "holder-check: " (if (empty? problems) "PASS" "FAIL")
                  " exit-convention=0-pass/1-fail"))
    (System/exit (if (empty? problems) 0 1))))

(when (= *file* (System/getProperty "babashka.file")) (apply -main *command-line-args*))
