(ns futon2.aif.machine-accumulation-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.machine-accumulation :as a]
            [futon2.aif.observation :as observation]
            [futon2.aif.belief :as belief]
            [futon2.aif.a4a :as a4a]))
(def os [:o0 :o1]) (def ss [:spawned :refined])
(def t1 {:id 1 :previous-id nil :observation {:o0 1 :o1 0} :belief {:spawned 1/2 :refined 1/2}})
(def t2 {:id 2 :previous-id 1 :observation {:o0 0 :o1 1} :belief {:spawned 1/4 :refined 3/4}})
(deftest recurrence-and-controls
  (let [z (a/initialize os ss 1) x (a/step z t1) y (a/step x t2)]
    (is (:ok z)) (is (:ok x)) (is (:ok y))
    (is (= 3/2 (get-in x [:concentrations :o0 :spawned])))
    (is (= 5/4 (get-in y [:concentrations :o1 :spawned])))
    (is (a/recurrence-valid? x t2 y))
    (is (= :missing-carried-state (get-in (a/step nil t1) [:refusal :kind])))
    (is (= :carry-chain-gap (get-in (a/step x (assoc t2 :previous-id nil)) [:refusal :kind])))
    (is (= :support-mismatch (get-in (a/step x (update t2 :belief dissoc :refined)) [:refusal :kind])))
    (is (= :invalid-increment (get-in (a/step x (assoc-in t2 [:belief :refined] -1)) [:refusal :kind])))
    (let [recount (a/step (a/initialize os ss 1) (assoc t2 :previous-id nil))]
      (is (false? (a/recurrence-valid? x t2 recount))))))

;; The actual Clojure carriers, with their correspondence to Holes.Channel.all
;; and MachineBeliefState.Status.all checked independently below.
(def channels observation/observation-channels)
(def statuses (vec (sort belief/status-set)))
(defn- vector-on [support entries] (merge (zipmap support (repeat 0)) entries))
(defn- tick [id previous-id outcomes states]
  {:id id :previous-id previous-id
   :observation (vector-on channels outcomes) :belief (vector-on statuses states)})
(defn- nonuniform-prior []
  (assoc-in (a/initialize channels statuses 1) [:concentrations :loop-health :spawned] 3/2))
(defn- total [state] (reduce + (mapcat vals (vals (:concentrations state)))))
(defn- changed-cells [before after]
  (set (for [[o row] before [s value] row :when (not= value (get-in after [o s]))] [o s])))
(defn- close? [x y] (< (Math/abs (- (double x) (double y))) 1e-12))

(deftest carriers-match-the-machine-lean-names
  (is (= 14 (count channels)))
  (is (= [:loop-health :support-coverage :attack-coverage :mission-health
          :stack-pct :consulting-pct :portfolio-pct :mathematics-pct
          :active-repo-ratio :sorry-count-norm :coupling-density :ticks-firing-ratio
          :depositing-signal :annotation-health] channels))
  (is (= 7 (count statuses)))
  (is (= (sort [:spawned :refined :strengthened :addressed :falsified :foreclosed :reopened]) statuses)))

(deftest onehot-and-empty-trial
  (let [prior (nonuniform-prior)
        t (tick :one nil {:loop-health 1} {:spawned 1})
        post (a/step prior t)]
    (is (= prior (reduce a/step prior [])))
    (is (:ok post))
    (is (= 5/2 (get-in post [:concentrations :loop-health :spawned])))
    (is (= #{[:loop-health :spawned]} (changed-cells (:concentrations prior) (:concentrations post))))
    (doseq [o channels s statuses]
      (is (= (+ (get-in prior [:concentrations o s]) (if (= [o s] [:loop-health :spawned]) 1 0))
             (get-in post [:concentrations o s]))))))

(def soft-one (tick :one nil {:loop-health 1/4 :support-coverage 3/4} {:spawned 1/3 :refined 2/3}))
(def soft-two (tick :two :one {:loop-health 2/3 :support-coverage 1/3} {:spawned 3/5 :refined 2/5}))

(deftest posterior-becomes-prior-and-append-agrees
  (let [prior (nonuniform-prior) first-post (a/step prior soft-one)
        second-post (a/step first-post soft-two)
        batch (reduce a/step prior [soft-one soft-two])]
    (is (= batch second-post))
    (is (a/recurrence-valid? prior soft-one first-post))
    (is (a/recurrence-valid? first-post soft-two second-post))
    (is (= (+ 3/2 1/12 2/5) (get-in batch [:concentrations :loop-health :spawned])))
    (is (= (+ 1 1/2 2/15) (get-in batch [:concentrations :support-coverage :refined])))
    (is (not (a/recurrence-valid? first-post soft-two
                                (a/step prior (assoc soft-two :previous-id nil)))))))

(deftest ^{:doc "Live channel maps are not normalized: +T requires normalized o and s; otherwise growth is sum of the products of their masses."}
  total-growth-is-conditioned-on-normalization
  ;; Live channel values are not normalized across channels. +T applies ONLY
  ;; under the theorem's two normalization hypotheses; never normalize for it.
  (let [prior (nonuniform-prior) ticks [soft-one soft-two]
        post (reduce a/step prior ticks)
        unnormalized (tick :wide nil {:loop-health 1 :support-coverage 1} {:spawned 1 :refined 2})
        wide (a/step prior unnormalized)]
    (doseq [t ticks]
      (is (= 1 (reduce + (vals (:observation t)))))
      (is (= 1 (reduce + (vals (:belief t))))))
    (is (= 197/2 (total prior)))
    (is (= 201/2 (total post)))
    (is (= (count ticks) (- (total post) (total prior))))
    (is (= 6 (* (reduce + (vals (:observation unnormalized)))
                (reduce + (vals (:belief unnormalized))))
           (- (total wide) (total prior))))
    (is (not= 1 (- (total wide) (total prior))))))

(deftest two-channel-update-is-not-a-single-corpus-record
  (let [prior (nonuniform-prior)
        post (a/step prior (tick :soft nil {:loop-health 1/2 :support-coverage 1/2} {:spawned 1}))
        ;; Seed the outcome axis with another capability so the real recount's
        ;; coordinate set stays fixed when ONE additional record is appended.
        corpus {:capabilities ["cap-a" "cap-b"] :edges [["cap-b" "mission"]]}
        before (a4a/corpus->concentration corpus)
        after (a4a/corpus->concentration (update corpus :edges conj ["cap-a" "mission"]))
        differences (for [[cap row] (:concentrations before) [i v] (map-indexed vector row)
                          :when (not= v (get-in after [:concentrations cap i]))]
                      [cap i (- (get-in after [:concentrations cap i]) v)])]
    (is (= #{[:loop-health :spawned] [:support-coverage :spawned]}
           (changed-cells (:concentrations prior) (:concentrations post))))
    (is (= 2 (get-in post [:concentrations :loop-health :spawned])))
    (is (= 3/2 (get-in post [:concentrations :support-coverage :spawned])))
    (is (= (:outcomes before) (:outcomes after)))
    (is (= 1 (count differences)))
    (is (= ["cap-a" 0] (vec (take 2 (first differences)))))
    ;; Only a4a's double arithmetic uses tolerance (absolute error < 1e-12).
    (is (close? 1 (nth (first differences) 2)))
    (is (close? 1/10 (get-in before [:concentrations "cap-a" 0])))
    (is (close? 11/10 (get-in after [:concentrations "cap-a" 0])))))

(deftest recount-ignores-history-but-step-retains-the-prior-difference
  (let [corpus {:capabilities ["cap"] :edges [["cap" "mission"]]}
        recount (a4a/corpus->concentration corpus)
        a-prior (a/initialize channels statuses 1)
        b-prior (a/initialize channels statuses 3)
        a-post (a/step a-prior soft-one) b-post (a/step b-prior soft-one)]
    (is (= recount (a4a/corpus->concentration corpus)))
    ;; Actual public signature, not a fictional wrapper that drops a prior.
    (is (= [1] (mapv count (:arglists (meta #'a4a/corpus->concentration)))))
    (is (not= (:concentrations a-post) (:concentrations b-post)))
    (doseq [o channels s statuses]
      (is (= 2 (- (get-in b-post [:concentrations o s]) (get-in a-post [:concentrations o s])))))))

(deftest positivity-and-trial-refusals
  (doseq [prior [0 0.0 -1 -1/2]]
    (is (= {:ok false :refusal {:kind :prior-not-positive :path [:initialization :prior]}}
           (a/initialize channels statuses prior))))
  (let [prior (nonuniform-prior)]
    (is (:ok (a/initialize channels statuses 1.0)))
    (is (= :invalid-increment (get-in (a/step prior (assoc-in soft-one [:observation :loop-health] -1/2)) [:refusal :kind])))
    (is (= :support-mismatch (get-in (a/step prior (update soft-one :observation dissoc :loop-health)) [:refusal :kind])))
    (is (= :carry-chain-gap (get-in (a/step prior (assoc soft-one :previous-id :missing)) [:refusal :kind])))))
