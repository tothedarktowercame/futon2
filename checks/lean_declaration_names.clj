(ns checks.lean-declaration-names
  "Lexical namespace context for the source census, never Lean elaboration."
  (:require [clojure.string :as str]))

(defn namespace-prefixes
  "One prefix per comment-stripped source line. Sections do not add a namespace;
   their end only closes that section. The census handles one command per line."
  [lines]
  (:prefixes
   (reduce (fn [{:keys [stack prefixes]} line]
             (let [line (str/trim line)
                   namespace (second (re-find #"^namespace\s+([A-Za-z_][A-Za-z0-9_.']*)\s*$" line))
                   section? (re-matches #"(?:noncomputable\s+)?section(?:\s+\S+)?" line)
                   end? (re-matches #"end(?:\s+\S+)?" line)
                   stack (cond namespace (conj stack namespace)
                               section? (conj stack nil)
                               end? (if (seq stack) (pop stack) stack)
                               :else stack)]
               {:stack stack :prefixes (conj prefixes (str/join "." (remove nil? stack)))}))
           {:stack [] :prefixes []} lines)))

(defn aliases [declaration]
  (let [{:keys [name qualified-name]} declaration]
    (distinct (remove str/blank?
                      [name qualified-name
                       (when qualified-name
                         (str/replace qualified-name #"^DarkTower\.WarMachine\." ""))]))))

(defn index-declarations [declarations]
  ;; Keep the historical bare-name first-site join; add qualified aliases.
  ;; This does not turn a lexical lookup into a proof or change any binding.
  (reduce (fn [index declaration]
            (reduce (fn [m alias]
                      (if (contains? m alias) m
                          (assoc m alias (select-keys declaration [:path :line :kind]))))
                    index (aliases declaration))) {} declarations))
