(ns checks.lean-positive-witness
  "Non-vacuous validation for positive Lean witness files."
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.string :as str]))

(def declaration-pattern
  #"(?m)^\s*(?:theorem|lemma|example|def|abbrev|structure|inductive|class|instance)\b")

(def sorry-pattern #"(?m)(?:^|\s)sorry(?:\s|$)")

(defn- without-comments [source]
  ;; Witness sources use ordinary line comments; block comments are removed as
  ;; well so a commented declaration cannot make an empty witness non-vacuous.
  (-> source
      (str/replace #"(?s)/-.*?-/" " ")
      (str/replace #"(?m)--.*$" "")))

(defn source-valid? [source]
  (let [substantive (without-comments source)]
    (and (not (str/blank? substantive))
         (boolean (re-find declaration-pattern substantive))
         (not (re-find sorry-pattern substantive)))))

(defn validate
  "Require a readable, declaration-bearing, sorry-free source and successful
   Lean elaboration. The optional override is a control seam only."
  [mathlib-root relative-path]
  (let [selected (or (System/getenv "FUTON_POSITIVE_LEAN_OVERRIDE") relative-path)
        path (fs/path mathlib-root selected)]
    (try
      (let [source (slurp (str path))
            source-ok? (source-valid? source)
            lean (when source-ok?
                   (process/shell {:dir (str mathlib-root) :continue true
                                   :out :string :err :string}
                                  "lake" "env" "lean" selected))]
        {:pass? (and source-ok? (zero? (or (:exit lean) 1)))
         :path selected
         :source-status (cond
                          (str/blank? (without-comments source)) :empty
                          (not (re-find declaration-pattern (without-comments source))) :no-declarations
                          (re-find sorry-pattern (without-comments source)) :contains-sorry
                          :else :substantive)
         :lean-exit (:exit lean)})
      (catch Throwable t
        {:pass? false :path selected :source-status :unreadable
         :detail (.getMessage t)}))))

(defn pass? [mathlib-root relative-path]
  (:pass? (validate mathlib-root relative-path)))
