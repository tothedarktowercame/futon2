(ns control
  (:require [babashka.process :as process]
            [checks.positive-proof-receipt :as receipt]
            [clojure.data :as data]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(def root "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-vfe-control-1")
(def target "holes/labs/wm-contract/variational-free-energy-positive-receipt.edn")
(def source-path "/home/joe/code/mathlib4/DarkTower/WarMachine/VariationalFreeEnergyWitness.lean")
(defn record! [name x] (spit (str root "/" name) (with-out-str (pp/pprint x))))
(defn replacement [wrapper]
  (let [[_ needle replacement]
        (re-find #"(?s)\(str/replace source\s+(\"(?:\\.|[^\"])*\")\s+(\"(?:\\.|[^\"])*\")" wrapper)]
    (assert needle)
    [(edn/read-string needle) (edn/read-string replacement)]))

(let [old (edn/read-string (slurp (str root "/predecessor.edn")))
      candidate (edn/read-string (slurp (str root "/retained-candidate.edn")))
      source (slurp source-path)
      [needle old-text] (replacement (slurp (str root "/wrapper-before.clj.txt")))
      [new-needle new-text] (replacement (slurp "checks/variational_free_energy_witness.clj"))
      count-matches (count (re-seq (re-pattern (java.util.regex.Pattern/quote needle)) source))
      _ (assert (= needle new-needle))
      _ (assert (= 1 count-matches))
      old-mutant (str/replace source needle old-text)
      mutant (str/replace source needle new-text)
      commands (atom [])
      original-shell process/shell
      logged-shell (fn [& args]
                     (let [result (apply original-shell args)]
                       (swap! commands conj {:arguments args :exit (:exit result)
                                             :out (:out result) :err (:err result)})
                       (record! "protocol-commands.edn" @commands)
                       result))]
  (assert (not= source mutant))
  (record! "replacement.edn" {:needle needle :old old-text :new new-text :substitution-count count-matches})
  (spit (str root "/old-mutated-source.txt") old-mutant)
  (spit (str root "/mutated-source.txt") mutant)
  (with-redefs [process/shell logged-shell]
    (doseq [[label text expected] [["old" old-mutant 1] ["corrected" mutant 0]]]
      (let [tmp (java.io.File/createTempFile "vfe-mutation-" ".lean")]
        (try
          (spit tmp text)
          (let [r (process/shell {:dir "/home/joe/code/mathlib4" :out :string :err :string :continue true}
                                 "lake" "env" "lean" (.getAbsolutePath tmp))]
            (record! (str label "-elaboration.edn") (select-keys r [:exit :out :err]))
            (assert (= expected (:exit r))))
          (finally (.delete tmp)))))
    (let [fresh (assoc (receipt/basis-record candidate) :recorded-at (str (java.time.Instant/now)))
          valid (receipt/validate fresh)
          overridden (receipt/validate fresh {["mathlib4" "DarkTower/WarMachine/VariationalFreeEnergyWitness.lean"] mutant})
          drift (vec (for [spec (:source-basis fresh)
                           :when (= "DarkTower/WarMachine/VariationalFreeEnergyWitness.lean" (:path spec))
                           d (:declarations spec)
                           :let [live (receipt/sha256-text (receipt/declaration-text mutant (:name d))) ]
                           :when (not= (:sha256 d) live)]
                       {:name (:name d) :recorded (:sha256 d) :mutated live}))]
      (assert (= (:adapter old) (:adapter fresh)))
      (assert (= (:dependency-closure old) (:dependency-closure fresh)))
      (assert (= (:fixture old) (:fixture fresh)))
      (assert (:pass? valid))
      (assert (= [:positive-source-drift] (:failures overridden)))
      (assert (= ["constantGaussianReference"] (mapv :name drift)))
      (record! "fresh-successor.edn" fresh)
      (record! "candidate-to-fresh-diff.edn" (data/diff candidate fresh))
      (record! "predecessor-to-fresh-diff.edn" (data/diff old fresh))
      (record! "unmutated-validation.edn" valid)
      (record! "weakened-validation.edn" overridden)
      (record! "drift-location.edn" drift)
      (io/copy (io/file (str root "/fresh-successor.edn")) (io/file target))
      (let [canonical (receipt/validate (edn/read-string (slurp target)))]
        (record! "canonical-validation.edn" canonical)
        (assert (:pass? canonical)))
      (println "Exactly one substitution; old mutation exit 1; corrected mutation exit 0; canonical valid; weakened source refused only at constantGaussianReference."))))
