(ns futon2.aif.rollout
  "Policy rollout search over meme-arrow transition leaves."
  (:require [clojure.edn :as edn]
            [meme.step :as meme-step]))

(def default-move-set-path
  "/home/joe/code/futon6/data/diffsub-moves-stub.edn")

(defn load-move-set
  ([] (load-move-set default-move-set-path))
  ([path]
   (edn/read-string (slurp path))))

(defn moves
  ([] (moves (load-move-set)))
  ([move-set]
   (vec (:moves move-set))))

(defn constructed-reachable
  [state]
  (->> (:arrows state)
       vals
       (filter #(= :constructed (:status %)))
       (map :want)
       set))

(defn normalize-state
  [state]
  (update state :reachable
          (fn [reachable]
            (set (concat reachable (constructed-reachable state))))))

(defn reachable? [state move]
  (contains? (:reachable state) (:have move)))

(defn frontier-no-path? [state move]
  (let [cap-id (:advances-cap move)
        cap (when cap-id (get-in state [:cap-overlay cap-id]))]
    (and cap
         (meme-step/cap-frontier? cap)
         (not (reachable? state move)))))

(defn reachable-moves
  "Apply the moving reachable mask for a single node."
  [state moves]
  (let [state (normalize-state state)]
    (->> moves
         (remove #(frontier-no-path? state %))
         (filter #(reachable? state %))
         vec)))

(defn renormalize-priors
  "R1: recompute :prior as softmax(:score) over this node's survivors."
  [moves]
  (let [weights (mapv #(Math/exp (double (or (:score %) 0.0))) moves)
        total (reduce + 0.0 weights)]
    (mapv (fn [move weight]
            (assoc move :prior (if (pos? total) (/ weight total) 0.0)))
          moves
          weights)))

(defn ranked-survivors
  [state moves & {:keys [top-k]
                  :or {top-k 5}}]
  (->> (reachable-moves state moves)
       renormalize-priors
       (sort-by (juxt (comp - double :prior) :rank :move/id))
       (take top-k)
       vec))

(defn move-cost
  "Local g(s_t) proxy from the locked stub.

   Negative :delta-g is a benefit. Already-satisfied capability steps get zero
   pragmatic credit so a rollout cannot farm an already closed cap."
  [state move]
  (let [cap-id (:advances-cap move)
        cap (when cap-id (get-in state [:cap-overlay cap-id]))
        status (get-in cap [:props :capability/status])]
    (cond
      (:truncated? state) 0.0
      (= :satisfied status) 0.0
      :else (double (or (:delta-g move) (- (double (or (:score move) 0.0))))))))

(defn apply-move
  [state move]
  (let [state (normalize-state state)
        state' (meme-step/step state (assoc move :to-state :constructed))]
    (update state' :write-count (fnil + 0) 0)))

(defn project-policy
  "Port of ukrn's path accumulator shape: G(pi)=sum gamma^t g(s_t).
   :truncated is sticky: terminal moves carry their local cost and stop expansion."
  [state policy & {:keys [gamma]
                   :or {gamma 0.9}}]
  (loop [state (normalize-state state)
         remaining (seq policy)
         discount 1.0
         total 0.0
         steps []]
    (if (or (nil? remaining) (:truncated? state))
      {:G total
       :final-state state
       :steps (vec steps)
       :policy (mapv #(select-keys % [:move/id :move/class :have :want
                                      :advances-cap :prior :delta-g
                                      :move/terminal?])
                     policy)
       :truncated? (boolean (:truncated? state))}
      (let [move (first remaining)
            g (move-cost state move)
            state' (apply-move state move)]
        (recur state'
               (next remaining)
               (* discount gamma)
               (+ total (* discount g))
               (conj steps {:move/id (:move/id move)
                            :g g
                            :discount discount
                            :discounted-g (* discount g)
                            :prior (:prior move)
                            :truncated? (:truncated? state')}))))))

(defn expand-policies
  [state moves & {:keys [depth top-k]
                  :or {depth 2 top-k 5}}]
  (letfn [(expand [state prefix remaining-depth]
            (if (zero? remaining-depth)
              [{:state state :policy prefix}]
              (let [survivors (ranked-survivors state moves :top-k top-k)]
                (if (empty? survivors)
                  [{:state state :policy prefix}]
                  (mapcat
                   (fn [move]
                     (let [state' (apply-move state move)
                           prefix' (conj prefix move)]
                       (if (or (:truncated? state') (:move/terminal? move))
                         [{:state state' :policy prefix'}]
                         (expand state' prefix' (dec remaining-depth)))))
                   survivors)))))]
    (vec (expand (normalize-state state) [] depth))))

(defn score-policies
  [state moves & {:keys [depth top-k gamma]
                  :or {depth 2 top-k 5 gamma 0.9}}]
  (->> (expand-policies state moves :depth depth :top-k top-k)
       (mapv (fn [{:keys [policy]}]
               (project-policy state policy :gamma gamma)))
       (sort-by :G)
       vec))

(defn greedy-one-step
  [state moves & {:keys [top-k gamma]
                  :or {top-k 20 gamma 0.9}}]
  (first (score-policies state moves :depth 1 :top-k top-k :gamma gamma)))

(defn best-rollout
  [state moves & {:keys [depth top-k gamma]
                  :or {depth 2 top-k 5 gamma 0.9}}]
  (first (score-policies state moves :depth depth :top-k top-k :gamma gamma)))

(defn softmax
  [scores tau]
  (let [tau (double (or tau 1.0))
        weights (mapv #(Math/exp (/ (- (double (:G %))) tau)) scores)
        total (reduce + 0.0 weights)]
    (mapv (fn [score weight]
            (assoc score :selection/probability (if (pos? total) (/ weight total) 0.0)))
          scores
          weights)))

(defn select-policy
  "Argmin G with WM-I4 abstain on flat fields."
  [scored & {:keys [tau abstain-epsilon]
             :or {tau 1.0 abstain-epsilon 1.0e-6}}]
  (let [ranked (vec (sort-by :G scored))
        [best second-best] ranked
        ranked' (softmax ranked tau)]
    (if (and best second-best
             (< (Math/abs (- (double (:G second-best)) (double (:G best))))
                abstain-epsilon))
      {:decision :abstain
       :ranked ranked'}
      {:decision :select
       :selected (first ranked')
       :ranked ranked'})))
