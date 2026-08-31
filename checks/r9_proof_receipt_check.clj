#!/usr/bin/env bb
(ns checks.r9-proof-receipt-check
  (:require [babashka.process :as process]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def receipt-path "holes/labs/wm-contract/R9-VerdictConsultsChecker-proof-receipt.edn")
(def repo-root "/home/joe/code")

(defn sha256 [path]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream path)]
      (let [buf (byte-array 8192)]
        (loop []
          (let [n (.read in buf)]
            (when (pos? n)
              (.update digest buf 0 n)
              (recur))))))
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn last-sha [{:keys [repo path]}]
  (str/trim (:out (process/shell {:out :string :err :string}
                                 "git" "-C" (str repo-root "/" repo)
                                 "log" "-1" "--format=%H" "--" path))))

(defn proof-receipt-shape? [x]
  (and (= :LeanProofReceipt (:receipt/type x))
       (= 1 (:receipt/version x))
       (string? (:recorded-at x))
       (= "DarkTower.WarMachine.Holes.R9D2.verdictConsultsChecker" (:theorem x))
       (= 64 (count (get-in x [:proof-source :sha256] "")))
       (= ["lake" "env" "lean"] (get-in x [:elaborator :command]))
       (= 0 (get-in x [:result :exit]))
       (vector? (get-in x [:result :axioms]))
       (every? string? (get-in x [:result :axioms]))))

(defn elaborate [receipt]
  (let [source (str repo-root "/" (get-in receipt [:proof-source :repo]) "/"
                    (get-in receipt [:proof-source :path]))
        theorem (last (str/split (:theorem receipt) #"\."))
        text (slurp source)
        augmented (str/replace text
                               "end DarkTower.WarMachine.Holes.R9D2"
                               (str "#print axioms " theorem
                                    "\nend DarkTower.WarMachine.Holes.R9D2"))
        tmp (java.io.File/createTempFile "r9-proof-receipt-" ".lean")]
    (try
      (spit tmp augmented)
      (let [result (process/shell {:out :string :err :string :continue true
                                   :dir (get-in receipt [:elaborator :cwd])}
                                  "lake" "env" "lean" (.getAbsolutePath tmp))
            axiom-line (str (:out result) (:err result))
            axioms (if-let [[_ body] (re-find #"depends on axioms: \[([^]]*)\]" axiom-line)]
                     (if (str/blank? body) [] (mapv str/trim (str/split body #",")))
                     nil)]
        {:exit (:exit result) :axioms axioms :output axiom-line})
      (finally (.delete tmp)))))

(defn validate [receipt]
  (let [proof (:proof-source receipt)
        imported (:imported-module receipt)
        source-path (str repo-root "/" (:repo proof) "/" (:path proof))
        live (when (proof-receipt-shape? receipt) (elaborate receipt))
        failures (cond-> []
                   (not (proof-receipt-shape? receipt)) (conj :shape)
                   (and (proof-receipt-shape? receipt)
                        (not= (:sha256 proof) (sha256 source-path))) (conj :source-content-drift)
                   (and (proof-receipt-shape? receipt)
                        (not= (:git-sha proof) (last-sha proof))) (conj :source-git-drift)
                   (and (proof-receipt-shape? receipt)
                        (not= (:git-sha imported) (last-sha imported))) (conj :import-git-drift)
                   (and live (not= (:result receipt) (select-keys live [:exit :axioms])))
                   (conj :elaboration-drift))]
    {:pass? (empty? failures) :failures failures
     :theorem (:theorem receipt) :axioms (:axioms live)}))

(defn read-receipt [mode]
  (case mode
    "absent" (edn/read-string (slurp (str receipt-path ".absent-control")))
    "tampered" (assoc-in (edn/read-string (slurp receipt-path)) [:result :axioms] [])
    (edn/read-string (slurp receipt-path))))

(defn -main [& [mode]]
  (let [negative? (#{"absent" "tampered"} mode)
        report (try (validate (read-receipt mode))
                    (catch Exception e {:pass? false :failures [:unreadable]
                                        :message (.getMessage e)}))]
    (cond
      (and negative? (not (:pass? report)))
      (println "r9-proof-receipt-check: negative-control PASS (binding evidence rejected) exit-convention=0-pass/1-fail/2-mutation-slipped" (pr-str report))
      negative?
      (do (println "r9-proof-receipt-check: FAIL (mutation slipped) exit-convention=0-pass/1-fail/2-mutation-slipped" (pr-str report))
          (System/exit 2))
      (:pass? report)
      (println "r9-proof-receipt-check: PASS exit-convention=0-pass/1-fail/2-mutation-slipped" (pr-str report))
      :else
      (do (println "r9-proof-receipt-check: FAIL exit-convention=0-pass/1-fail/2-mutation-slipped" (pr-str report))
          (System/exit 1)))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
