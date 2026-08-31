#!/usr/bin/env bb
(ns checks.model-uncertainty-eig-witness
  (:require [babashka.process :as process]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def mathlib-root "/home/joe/code/mathlib4")
(def source-path "/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean")
(def receipt-path "holes/labs/wm-contract/model-uncertainty-eig-proof-receipt.edn")
(def theorem "DarkTower.WarMachine.Holes.modelUncertaintyAndEIG")
(def pinned-declarations
  ["modelUncertaintyBonus" "parameterInformationGain"
   "expectedInformationGain" "modelUncertaintyAndEIG"])

(defn sha256-text [s]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (.update digest (.getBytes s "UTF-8"))
    (format "%064x" (java.math.BigInteger. 1 (.digest digest)))))

(defn declaration-text [source declaration]
  (let [lines (vec (str/split-lines source))
        start-pattern (re-pattern
                       (str "^(?:private\\s+)?(?:noncomputable\\s+)?(?:def|theorem|structure|inductive|abbrev)\\s+"
                            (java.util.regex.Pattern/quote declaration) "(?:\\s|$)"))
        boundary? #(boolean (re-find #"^(?:/--|private\\s+|structure\\s|inductive\\s|def\\s|theorem\\s|lemma\\s|namespace\\s|end\\s)" %))
        start (first (keep-indexed #(when (re-find start-pattern %2) %1) lines))]
    (when (nil? start) (throw (ex-info "declaration absent" {:declaration declaration})))
    (let [end (or (first (keep-indexed #(when (and (> %1 start) (boundary? %2)) %1) lines))
                  (count lines))]
      (str (str/join "\n" (subvec lines start end)) "\n"))))

(defn basis []
  (let [source (slurp source-path)]
    (mapv (fn [name] {:name name :sha256 (sha256-text (declaration-text source name))})
          pinned-declarations)))

(defn lean-run [body]
  (let [tmp (java.io.File/createTempFile "model-uncertainty-eig-" ".lean")]
    (try
      (spit tmp body)
      (process/shell {:dir mathlib-root :continue true :out :string :err :string}
                     "lake" "env" "lean" (.getAbsolutePath tmp))
      (finally (.delete tmp)))))

(defn positive-run []
  (lean-run (str "import DarkTower.WarMachine.Holes\n#print axioms " theorem "\n")))

(defn collapsed-equality-run []
  (lean-run
   (str "import DarkTower.WarMachine.Holes\n"
        "open DarkTower.WarMachine.Holes\n"
        "example : (modelUncertaintyBonus [⟨1, by norm_num⟩]).value = 0 := by\n"
        "  norm_num [modelUncertaintyBonus]\n")))

(defn validate [receipt]
  (let [live (positive-run)
        source-basis (basis)]
    {:pass? (and (= :LeanProofReceipt (:receipt/type receipt))
                 (= 2 (:receipt/version receipt))
                 (= theorem (:theorem receipt))
                 (= source-basis (get-in receipt [:proof-source :declarations]))
                 (= 0 (:exit live))
                 (= 0 (get-in receipt [:result :exit])))
     :theorem theorem :positive-exit (:exit live)
     :source-basis-matches? (= source-basis (get-in receipt [:proof-source :declarations]))}))

(defn -main [& args]
  (let [mode (first args)]
    (if (= "--basis" mode)
      (println (pr-str (basis)))
      (let [negative? (= "--negative" mode)
            report (if negative?
                     (let [run (collapsed-equality-run)]
                       {:pass? (not= 0 (:exit run)) :collapsed-equality-exit (:exit run)})
                     (validate (edn/read-string (slurp receipt-path))))
            exit (cond (and negative? (:pass? report)) 0
                       negative? 2 (:pass? report) 0 :else 1)]
        (println "model-uncertainty-eig-witness:"
                 (cond (and negative? (:pass? report)) "negative-control PASS"
                       negative? "mutation slipped" (:pass? report) "PASS" :else "FAIL")
                 (pr-str report) "exit-convention=0-pass/1-fail/2-mutation-slipped")
        (System/exit exit)))))

(apply -main *command-line-args*)
