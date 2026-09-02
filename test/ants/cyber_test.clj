(ns ants.cyber-test
  (:require [clojure.test :refer [deftest is testing]]
            [ants.cyber :as cyber]))

(deftest default-pattern-listed
  (let [ids (set (map :id (cyber/available-patterns)))]
    (is (contains? ids cyber/default-pattern-id))))

(deftest attach-config-populates-metadata
  (let [ant {:species :cyber}
        updated (cyber/attach-config ant cyber/default-pattern-id)]
    (is (map? (:aif-config updated)))
    (is (map? (:cyber-pattern updated)))
    (is (= cyber/default-pattern-id (get-in updated [:cyber-pattern :id])))))

(deftest attach-config-falls-back-to-default
  (let [ant {:species :cyber}
        updated (cyber/attach-config ant ::missing-pattern)
        fallback (cyber/cyber-config cyber/default-pattern-id)]
    (is (= (:id fallback) (get-in updated [:cyber-pattern :id])))))

(deftest describe-pattern-smoke
  (let [desc (cyber/describe-pattern cyber/default-pattern-id)]
    (testing "shape"
      (is (= cyber/default-pattern-id (:pattern desc)))
      (is (string? (:title desc)))
      (is (string? (:summary desc))))
    (when-let [excerpt (:excerpt desc)]
      (is (string? excerpt)))))

;; -----------------------------------------------------------------------------
;; Cascade attachment (futon3 worklist row :LA4)
;;
;; The acceptance for :LA4 says the test that PRECEDENCE decides a key collision
;; is the acceptance test for law O4 in this domain.  These are that test, run on
;; the real library rather than on a fixture: futon3/library/ants/
;; pheromone-trail-tuner.flexiarg:11 writes `:efe {:lambda {:info 0.5}}` and
;; white-space-scout.flexiarg:13 writes `:efe {:lambda {:info 0.6}}`, and
;; futon2/holes/cascade-ants.edn:110 records that "under merge-deep the winner is
;; ATTACHMENT ORDER -- not a property of either pattern".  After :LA4 the winner
;; is the cascade's precedence field, which is data on the collection.

(def ^:private cascade-members
  [:ants/cargo-return-discipline
   :ants/hunger-precision-coupling
   :ants/pheromone-trail-tuner
   :ants/white-space-scout])

(defn- cascade-with
  "A cascade over `cascade-members` whose precedence is `order`, most precedent
   first.  `:authored-order` is held FIXED at the constructed order in every
   case, so a difference between two of these cascades is a difference in
   precedence and in nothing else."
  [order]
  {:id :test-cascade
   :members cascade-members
   :authored-order cascade-members
   :precedence (into {} (map-indexed (fn [i p] [p (inc i)])) order)})

(def ^:private tuner-first
  (cascade-with [:ants/cargo-return-discipline :ants/hunger-precision-coupling
                 :ants/pheromone-trail-tuner :ants/white-space-scout]))

(def ^:private scout-first
  (cascade-with [:ants/cargo-return-discipline :ants/hunger-precision-coupling
                 :ants/white-space-scout :ants/pheromone-trail-tuner]))

(deftest the-two-contending-deltas-are-what-the-library-says-they-are
  (testing "the collision is real and read from the flexiargs, not stipulated here"
    (is (= 0.5 (get-in (cyber/cyber-config :ants/pheromone-trail-tuner)
                       [:aif-delta :efe :lambda :info])))
    (is (= 0.6 (get-in (cyber/cyber-config :ants/white-space-scout)
                       [:aif-delta :efe :lambda :info])))))

(deftest precedence-decides-a-key-collision
  (testing "the more precedent member takes the contended key"
    (is (= 0.5 (get-in (cyber/cascade-delta tuner-first) [:efe :lambda :info])))
    (is (= 0.6 (get-in (cyber/cascade-delta scout-first) [:efe :lambda :info]))))
  (testing "and precedence is the ONLY thing that differs between the two"
    (is (= (set (:members tuner-first)) (set (:members scout-first))))
    (is (= (:authored-order tuner-first) (:authored-order scout-first)))
    (is (not= (:precedence tuner-first) (:precedence scout-first))))
  (testing "so the two folds differ in exactly the contended path and nowhere else"
    (let [a (cyber/cascade-delta tuner-first)
          b (cyber/cascade-delta scout-first)]
      (is (not= a b))
      (is (= (assoc-in a [:efe :lambda :info] :X)
             (assoc-in b [:efe :lambda :info] :X))))))

(deftest cascade-contentions-names-the-collision-and-its-winner
  (let [c (cyber/cascade-contentions tuner-first)]
    (is (= 1 (count c)) "exactly one path is written by more than one member")
    (is (= [:efe :lambda :info] (:path (first c))))
    (is (= [[:ants/pheromone-trail-tuner 0.5] [:ants/white-space-scout 0.6]]
           (:writers (first c))))
    (is (= 0.5 (:won (first c))))
    (is (= :ants/pheromone-trail-tuner (:won-by (first c)))))
  (testing "reversing the precedence reverses the winner"
    (let [c (cyber/cascade-contentions scout-first)]
      (is (= 0.6 (:won (first c))))
      (is (= :ants/white-space-scout (:won-by (first c)))))))

(deftest ordered-members-reads-precedence-least-number-first
  (is (= [:ants/cargo-return-discipline :ants/hunger-precision-coupling
          :ants/pheromone-trail-tuner :ants/white-space-scout]
         (cyber/ordered-members tuner-first)))
  (is (= [:ants/cargo-return-discipline :ants/hunger-precision-coupling
          :ants/white-space-scout :ants/pheromone-trail-tuner]
         (cyber/ordered-members scout-first)))
  (testing "a member with no precedence entry sorts last, it does not vanish"
    (let [c (update tuner-first :precedence dissoc :ants/white-space-scout)]
      (is (= 4 (count (cyber/ordered-members c))))
      (is (= :ants/white-space-scout (last (cyber/ordered-members c)))))))

(deftest the-empty-cascade-and-the-baseline-are-both-the-identity
  (testing "an empty cascade folds to the empty delta"
    (is (= {} (cyber/cascade-delta {:members [] :precedence {} :authored-order []}))))
  (testing "so does the cascade holding only the identity element"
    (is (= {} (cyber/cascade-delta {:id :sham
                                    :members [:ants/baseline-cyber-ant]
                                    :authored-order [:ants/baseline-cyber-ant]
                                    :precedence {:ants/baseline-cyber-ant 1}}))))
  (testing "and attaching it leaves :aif-config untouched"
    (let [ant {:species :aif :aif-config {:precision {:tau-cap 1.5}}}]
      (is (= (:aif-config ant)
             (:aif-config (cyber/attach-cascade-config
                           ant {:id :sham
                                :members [:ants/baseline-cyber-ant]
                                :authored-order [:ants/baseline-cyber-ant]
                                :precedence {:ants/baseline-cyber-ant 1}})))))))

(deftest attach-cascade-config-sets-both-channels
  (let [ant (cyber/attach-cascade-config {:species :aif} tuner-first)]
    (testing "channel 1: the folded delta reaches :aif-config"
      (is (= 0.5 (get-in ant [:aif-config :efe :lambda :info])))
      (is (= 1.2 (get-in ant [:aif-config :precision :tau-cap])))
      (is (= 0.55 (get-in ant [:aif-config :modes :cargo-high]))))
    (testing "channel 2 is singular, so it is shown the most precedent member,
              under the :cyber/* alias its own case tables dispatch on"
      (is (= :cyber/cargo-return (get-in ant [:cyber-pattern :id])))
      (is (= :ants/cargo-return-discipline (get-in ant [:cyber-pattern :library-id])))
      (is (= :cyber/cargo-return
             (cyber/library-key->pattern-key :ants/cargo-return-discipline))))
    (testing "and the whole cascade is recorded beside it"
      (is (= (cyber/ordered-members tuner-first) (get-in ant [:cyber-pattern :ordered])))
      (is (= :test-cascade (get-in ant [:cyber-pattern :cascade])))
      (is (= (cyber/cascade-delta tuner-first) (get-in ant [:cyber-pattern :delta]))))))

(deftest a-cascade-is-not-the-same-as-its-most-precedent-member
  (testing "the whole point: composing four patterns is not attaching one of them"
    (let [whole (cyber/attach-cascade-config {:species :aif} tuner-first)
          single (cyber/attach-config {:species :aif} :ants/cargo-return-discipline)]
      (is (= :ants/cargo-return-discipline (get-in single [:cyber-pattern :id])))
      (is (= :ants/cargo-return-discipline (get-in whole [:cyber-pattern :library-id])))
      (is (not= (:aif-config whole) (:aif-config single))))))

(deftest three-patterns-shape-tau-through-different-keys
  (testing "cascade-ants.edn:120-126 -- a merge cannot see this contention, so
            cascade-contentions does not report it, and the question it leaves
            open (is the cap above the floor?) is answered here rather than by
            the fold"
    (let [d (cyber/cascade-delta tuner-first)]
      (is (empty? (filter #(= [:precision :tau-cap] (:path %))
                          (cyber/cascade-contentions tuner-first))))
      (is (= 1.2 (get-in d [:precision :tau-cap])))
      (is (= 0.12 (get-in d [:precision :tau-floor])))
      (is (> (get-in d [:precision :tau-cap]) (get-in d [:precision :tau-floor]))))))
