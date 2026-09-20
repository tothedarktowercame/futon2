(ns futon2.aif.exact-belief-core
  "One exact rational conditioning calculation, shared by the finite-kernel
   adapter and the mathematical manifest API. No runner continuation policy.")

(defn probability? [p]
  (and (or (integer? p) (ratio? p)) (<= 0 p 1)))

(defn distribution? [row]
  (and (map? row) (seq row) (every? probability? (vals row))
       (= 1 (reduce +' 0 (vals row)))))

(defn condition-predicted
  "Condition an already B-pushed distribution using A(o|s) for each state.
   Invalid rational domains are not impossible observations. Zero evidence
   is :refused/:none; only positive evidence yields :ok/:some. Does not
   decide whether a runner discards an observation or reinitializes."
  [predicted likelihoods observation]
  (let [receipt {:predicted-state predicted :likelihoods likelihoods
                 :observation observation}]
    (cond
      (not (distribution? predicted))
      (assoc receipt :status :invalid :kind :invalid-predicted-state)

      (not (and (map? likelihoods) (= (set (keys predicted)) (set (keys likelihoods)))
                (every? probability? (vals likelihoods))))
      (assoc receipt :status :invalid :kind :invalid-observation-likelihood)

      :else
      (let [weights (into {} (map (fn [[s p]] [s (*' p (get likelihoods s))])) predicted)
            evidence (reduce +' 0 (vals weights))
            result (assoc receipt :observation-probability evidence)]
        (if (zero? evidence)
          (assoc result :status :refused :option :none :kind :impossible-observation)
          (assoc result :status :ok :option :some
                 :posterior (update-vals weights #(/ % evidence))))))))
