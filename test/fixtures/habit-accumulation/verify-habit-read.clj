;; clojure -Sdeps '{:paths ["src" "resources" "scripts" "test"]}' -M test/fixtures/habit-accumulation/verify-habit-read.clj
;; Writes the proposed fixture to /tmp ONLY. Commit the diff before copying it.
(require '[clojure.edn :as edn]
         '[clojure.java.shell :as shell]
         '[clojure.pprint :as pp]
         '[futon2.aif.cascade-habit-store :as habit]
         '[futon2.aif.policy :as policy]
         '[futon2.report.cascade-habit-read-test :as replay])

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

(defn intended-path? [path]
  (or (= [:chosen-action-mass] path)
      (= :softmax-weights (first path))
      (and (= :selection-law (first path))
           (contains? #{:posterior :softmax-weights} (second path)))
      (and (= :selection-certificate (first path))
           (case (second path)
             :candidates (or (contains? #{:habit :habit-status :habit-provenance} (nth path 3 nil))
                             (= [:inputs :habit] (subvec path 3 (min 5 (count path)))))
             :policies (contains? #{:habit :habit-status} (nth path 3 nil))
             :g-term-decomposition (= [:terms :E] (subvec path 4 (min 6 (count path))))
             false))))

(let [{:keys [exit out]} (shell/sh "git" "show" "9265a89b:test/fixtures/habit-accumulation/before.edn")
      _ (assert (zero? exit))
      original (edn/read-string out)
      proposed (replay/with-store
                (fn [path]
                  (assoc original :cases
                         (mapv (fn [c]
                                 (let [d (policy/select-action-cascades (:ranked c) {:beta (:beta c) :cascade-habit-path path})]
                                   (habit/record-selection! path d)
                                   (assoc c :decision-bytes (pr-str d)))) (:cases original)))))
      diffs (mapv (fn [i old new]
                    (let [a (edn/read-string (:decision-bytes old))
                          b (edn/read-string (:decision-bytes new))]
                      {:case (inc i)
                       :counts (mapv #(get-in % [:habit-provenance :count])
                                     (get-in b [:selection-certificate :candidates]))
                       :selected-action-unchanged? (= (:action a) (:action b))
                       :differences (vec (differences [] a b))}))
                  (range) (:cases original) (:cases proposed))
      intended? (every? #(every? (comp intended-path? :path) (:differences %)) diffs)
      report {:original-commit "9265a89b" :reason :WIRE-habit-read
              :method "Recursive EDN map/sequence walk; no normalization or omitted keys. Each tick reads accumulated previous selections."
              :only-intended-paths? intended?
              :interpretation "Tick 1 is normalization only. Later unequal masses reflect the recorded counts. All six selected actions remain unchanged; posterior masses change."
              :cases diffs
              :discriminating-comparison (replay/with-store replay/evidence)}]
  (spit "test/fixtures/habit-accumulation/habit-read-structural-diff.edn" (with-out-str (pp/pprint report)))
  (assert (and (= 6 (count diffs)) intended?) "Unexpected structural changes: STOP")
  (spit "/tmp/wm-habit-read-proposed.edn" (str (pr-str proposed) "\n"))
  (println "Six decisions: only intended E/provenance/census/posterior paths changed. Proposed fixture remains in /tmp."))
(shutdown-agents)
