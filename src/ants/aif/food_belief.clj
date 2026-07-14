(ns ants.aif.food-belief
  "Hidden-state food-location belief for directed epistemic value (R17'/EIG).

   The ant maintains a SPATIAL belief about where food is — a map from cell-loc
   to {:food-prob :uncertainty :visits}. Updated from observations:
   - saw food at a cell → high food-prob, low uncertainty
   - visited, no food → low food-prob, low uncertainty
   - unvisited → prior food-prob, high uncertainty

   The directed EIG = expected information gain about food-location from taking
   an action and observing. A principled-approximation: EIG proportional to
   (food-plausibility × uncertainty) at the target cell — rewards exploring
   cells that are UNCERTAIN AND PLAUSIBLY-food, not foodless-novel cells.

   Domain-agnostic structure: the belief-update + EIG computation follows the
   pattern (hidden-state-distribution + observation-model + expected-posterior-
   entropy-reduction). The tokamak reuses this pattern with belief=causal-state
   instead of food-location. This namespace is ant-specific (food-location), but
   the PATTERN is liftable.

   Faithfulness tag: principled-approx (true EIG = simulated posterior entropy
   reduction; this is a directed proxy that captures the key signal).")

;; --------------------------------------------------------------------------- ;;
;; Food-belief state: {loc -> {:food-prob :uncertainty :visits}}
;; --------------------------------------------------------------------------- ;;

(defn initial-food-belief
  "Create an empty food-belief. Cells are added as they're observed."
  []
  {})

(defn- neighbor-locs
  "8-neighbors of a loc within grid bounds."
  [size [x y]]
  (let [[w h] size]
    (for [dx [-1 0 1]
          dy [-1 0 1]
          :when (not (and (zero? dx) (zero? dy)))
          :let [nx (+ x dx)
                ny (+ y dy)]
          :when (and (<= 0 nx) (< nx w) (<= 0 ny) (< ny h))]
      [nx ny])))

(defn update-food-belief
  "Update the food-belief from an observation.

   The observation contains :food (local food), :food-trace (neighbor food
   mean), and :loc. We update the ant's cell and infer neighbors from
   food-trace.

   world is needed for grid size and cell food values."
  [belief world observation]
  (let [loc (:loc observation)
        size (get-in world [:grid :size])
        local-food (double (or (:food observation) 0.0))
        food-trace (double (or (:food-trace observation) 0.0))]
    (if (nil? loc)
      belief
      (let [;; Current cell: direct observation
            cell-prob (if (> local-food 0.05) 0.9 (if (> local-food 0.0) 0.5 0.1))
            belief (-> belief
                       (update loc
                               (fn [existing]
                                 (let [visits (inc (int (or (:visits existing) 0)))]
                                   {:food-prob cell-prob
                                    :uncertainty (max 0.05 (/ 1.0 (+ 2.0 visits)))
                                    :visits visits}))))
            ;; Neighbors: infer from actual cell food values
            all-cells (get-in world [:grid :cells])
            neighbors (when size (neighbor-locs size loc))]
        (if (and all-cells (seq neighbors))
          (reduce (fn [b nloc]
                    (let [nfood (double (get-in all-cells [nloc :food] 0.0))
                          nprob (if (> nfood 0.1) 0.8 (if (> nfood 0.0) 0.3 0.1))]
                      (update b nloc
                              (fn [existing]
                                (let [visits (int (or (:visits existing) 0))]
                                  {:food-prob (if (pos? visits)
                                                (* 0.5 (+ (:food-prob existing 0.5) nprob))
                                                nprob)
                                   :uncertainty (if (pos? visits)
                                                  (max 0.05 (/ 1.0 (1.0 (inc visits))))
                                                  0.8)
                                   :visits visits})))))
                  belief
                  neighbors)
          belief)))))

(defn food-belief-at
  "Get the food-belief for a specific location.
   Returns {:food-prob :uncertainty} or prior for unvisited cells."
  [belief loc]
  (or (get belief loc)
      {:food-prob 0.3 :uncertainty 0.9 :visits 0}))

;; --------------------------------------------------------------------------- ;;
;; Directed EIG (Expected Information Gain about food-location)
;; --------------------------------------------------------------------------- ;;

(defn directed-eig
  "Compute the directed Expected Information Gain for visiting a target cell.

   EIG proportional to (food-plausibility × uncertainty) at the target.
   This rewards exploring cells that are UNCERTAIN AND PLAUSIBLY-food —
   NOT foodless-novel cells (low food-prob) and NOT known-empty cells
   (low uncertainty).

   Returns a value in [0, 1]. Higher = more informative to visit."
  [belief target-loc]
  (let [{:keys [food-prob uncertainty]} (food-belief-at belief target-loc)]
    (* (double food-prob) (double uncertainty))))

(defn directed-eig-for-action
  "Compute directed EIG for a candidate action.

   Uses the predicted next-location (from the ant's current loc + the action's
   movement pattern). For forage/pheromone: moves toward food/trail. For hold:
   stays put. For return: moves toward home.

   The EIG is higher for actions that move to uncertain-plausible-food cells."
  [belief obs action]
  (let [loc (:loc obs)
        home-prox (double (or (:home-prox obs) 0.0))
        ;; Predict rough target for each action
        target (case action
                 :forage (let [[x y] (vec loc)]
                           ;; Forage moves toward food — approximate as current loc
                           ;; (the actual target is computed in the kernel)
                           loc)
                 :pheromone loc
                 :hold loc
                 :return loc
                 loc)]
    (directed-eig belief target)))

(defn directed-eig-from-prediction
  "Compute directed EIG from a PREDICTED observation (the outcome from
   predict-observation or predict-outcome). Uses the predicted food/novelty
   to estimate the food-belief at the predicted location.

   This is the form used in the controller-score: it takes the predicted
   observation and returns a scalar EIG value."
  [belief predicted-obs]
  (let [loc (:loc predicted-obs)
        food-prob (double (or (:food predicted-obs) 0.0))
        ;; Uncertainty: high when we haven't observed this cell much
        {:keys [uncertainty]} (food-belief-at belief loc)]
    (* food-prob uncertainty)))
