;; Run from futon2: clojure -M test/fixtures/habit-accumulation/verify-regeneration.clj
;; Reads the original committed fixture; writes evidence before any replacement.
(require '[clojure.edn :as edn]
         '[clojure.java.shell :as shell]
         '[clojure.pprint :as pp]
         '[futon2.aif.policy :as policy])

(defn git-output [& args]
  (let [{:keys [exit out err]} (apply shell/sh "git" args)]
    (assert (zero? exit) err)
    out))

(defn differences [path before after]
  (cond
    (= before after) []
    (and (map? before) (map? after))
    (mapcat (fn [k]
              (cond
                (not (contains? before k)) [{:path (conj path k) :change :added :after (get after k)}]
                (not (contains? after k)) [{:path (conj path k) :change :removed :before (get before k)}]
                :else (differences (conj path k) (get before k) (get after k))))
            (sort-by pr-str (into (set (keys before)) (keys after))))
    (and (sequential? before) (sequential? after) (= (count before) (count after)))
    (mapcat #(differences (conj path %) (nth before %) (nth after %)) (range (count before)))
    :else [{:path path :change :changed :before before :after after}]))

(let [fixture "test/fixtures/habit-accumulation/before.edn"
      old-text (git-output "show" (str "4c47b645:" fixture))
      old (edn/read-string old-text)
      cases (:cases old)
      live (mapv #(policy/select-action-cascades (:ranked %) {:beta (:beta %)}) cases)
      diffs (mapv (fn [i c d]
                    {:case (inc i)
                     :differences (vec (differences [] (edn/read-string (:decision-bytes c)) d))})
                  (range) cases live)
      only-scoring? (and (= 6 (count cases))
                         (every? #(= [{:path [:selection-certificate :scoring] :change :added}]
                                     (mapv (fn [d] (select-keys d [:path :change])) (:differences %))) diffs))
      report {:original-commit "4c47b645" :scoring-introduced-by "88419699"
              :compared-head (.trim (git-output "rev-parse" "HEAD"))
              :method "Recursive EDN map/sequence comparison; unequal sets and scalars reported whole. Added map keys reported with their complete value."
              :original-selection-certificate-count (count (re-seq #":selection-certificate" old-text))
              :original-scoring-count (count (re-seq #":scoring" old-text))
              :only-scoring-added? only-scoring? :cases diffs}]
  (spit "test/fixtures/habit-accumulation/structural-diff.edn" (with-out-str (pp/pprint report)))
  (assert only-scoring? "Other differences found: STOP; fixture has not been replaced")
  (spit fixture (str (pr-str (assoc old :cases
                                  (mapv #(assoc %1 :decision-bytes (pr-str %2)) cases live))) "\n"))
  (println "Six decisions: only [:selection-certificate :scoring] added; fixture regenerated."))
