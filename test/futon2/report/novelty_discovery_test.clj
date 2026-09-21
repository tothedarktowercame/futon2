(ns futon2.report.novelty-discovery-test
  "Offline parameter-novelty sensitivity, explicitly not a production model."
  (:require [clojure.edn :as edn] [clojure.java.io :as io] [clojure.set :as set]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.policy :as policy])
  (:import [java.util.zip GZIPInputStream]))
(def evidence-dir "holes/labs/wm-contract/runs/fixlist-2026-09-21/improve-5-evidence/")
(defn fixture [run]
  (with-open [in (GZIPInputStream. (io/input-stream (str evidence-dir run ".edn.gz")))]
    (edn/read-string (slurp in))))
(defn harmonic [n] (reduce + 0 (map #(/ 1 %) (range 1 (inc n)))))
(defn h [p] (- (+ (* p (Math/log p)) (* (- 1 p) (Math/log (- 1 p))))))
(defn novelty [[a b]]
  (let [n (+ a b) p (double (/ a n))]
    (+ (h p) (* p (double (- (harmonic a) (harmonic n))))
       (* (- 1 p) (double (- (harmonic b) (harmonic n)))))))
(defn close? [a b] (< (abs (- (double a) (double b))) 1e-8))
(defn label [a] (when a (select-keys a [:id :target :type])))
(defn prepare [f]
  (let [terms (:terms f) c (get-in terms [:C :value])
        terminal (:distribution (last (:steps c)))
        spec {:want (set (keys (:weights terminal))) :weights (:weights terminal) :lam 1 :mu 0
              :evidence #{} :zeroed (:zeroed terminal) :c-schedule (:schedule c)}
        q0 (get-in terms [:D :value]) horizon (count (:steps c))]
    (assert (= 1 (count q0)))
    (assert (= 2 horizon))
    (assert (empty? (get-in c [:steps 0 :distribution :weights])))
    (assoc f :q0 q0 :horizon horizon :spec spec :terminal terminal
           :rows
           (mapv (fn [candidate]
                   (let [a (:id candidate)
                         cert (model/horizon-g-sparse-cert
                               {:rates (get-in terms [:A :value]) :q0 q0 :horizon horizon
                                :spec spec :universe (:universe terminal) :precedence-fn (constantly (:precedence a))})
                         q (model/rollout (constantly (:precedence a)) q0 horizon)
                         state (ffirst q)
                         outputs (into #{} (mapcat :produces) (:precedence a))
                         effects (set/intersection outputs (set (:want spec))
                                                   (set/difference state (ffirst q0)))]
                     (assert (= #{1} (set (vals q))))
                     (assert (close? (:g candidate) (:g cert)))
                     {:candidate candidate :state state :effects effects :g (:g cert)})) (:candidates f)))))

(defn endpoint-q [state effects p]
  (reduce (fn [q token]
            (reduce-kv (fn [out s mass]
                         (-> out (assoc s (* mass (- 1 p))) (assoc (conj s token) (* mass p)))) {} q))
          {(set/difference state effects) 1.0} effects))
(defn endpoint-g [f row prior]
  (let [[a b] prior p (double (/ a (+ a b)))
        q (endpoint-q (:state row) (:effects row) p)
        member (model/preference-member (:spec f) (get-in f [:terminal :universe]) 2 2)
        logc (model/member-log-probability member)
        earlier (* (count (get-in f [:terminal :universe])) (Math/log 2.0))]
    (+ earlier (reduce-kv (fn [v s mass] (+ v (* mass (- (Math/log mass) (logc s))))) 0.0 q))))

(defn evaluate [f priors arm k]
  (let [rows (mapv (fn [row prior]
                     (let [i (* (count (:effects row)) (novelty prior))
                           g0 (if (= arm :fixed-recorded-G) (:g row) (endpoint-g f row prior))]
                       (assoc row :prior prior :novelty i :g-base g0 :g (- g0 (* k i))))) (:rows f) priors)
        cs (mapv #(assoc (:candidate %) :g (:g %)) rows)
        beta (get-in f [:law :beta])
        posterior (selection/selection-posterior {:beta beta :candidates cs})
        action-of (into {} (map (fn [c] [(:id c) (#'policy/cascade-first-action (:id c))]) cs))
        active (into {} (filter (fn [[id _]] (some? (get action-of id)))) posterior)
        choice (selection/bayes-choice active action-of)
        diag (selection/selection-comparisons {:beta beta :candidates cs :posterior posterior :action-of action-of :choice choice})]
    {:arm arm :kappa k :choice (update choice :action label)
     :policy-comparison (select-keys (:policy-comparison diag) [:contributions :decided-by :near-tie?])
     :action-comparison (-> (:action-comparison diag)
                            (update :winner #(update % :action label))
                            (update :runner-up #(if (map? %) (update % :action label) %))
                            (update :flips #(into {} (for [[term x] %] [term (update-in x [:winner :action] label)]))))
     :rows (mapv (fn [r] (let [c (:candidate r)]
                          (merge (label (:id c)) (select-keys r [:effects :prior :novelty :g-base :g])
                                 {:habit (:habit c) :f (:f c) :posterior (get posterior (:id c))}))) rows)}))

(defn report [f]
  (let [n (count (:rows f))
        cases (vec (for [prior [[1 1] [9 1] [90 10]] arm [:fixed-recorded-G :factorized-attempt-endpoint] k [0 1]]
                     (assoc (evaluate f (repeat n prior) arm k) :prior prior)))
        heterogeneous (when (= "1789964661" (:run f))
                        (mapv #(evaluate f [[90 10] [9 1] [90 10]] :factorized-attempt-endpoint %) [0 1]))
        entropy-controls (when (= "1789964661" (:run f))
                           (mapv #(evaluate f [% [9 1] [9 1]] :factorized-attempt-endpoint 1) [[9 1] [9 2] [10 1]]))]
    {:run (:run f) :source-sha256 (:sha256 f) :cases cases
     :heterogeneous-equal-mean-control heterogeneous :attempt-entropy-controls entropy-controls}))

(deftest beta-closed-form-controls
  (is (close? (novelty [1 1]) (- (Math/log 2) 0.5)))
  (is (< 0 (novelty [90 10]) (novelty [9 1]) (novelty [1 1])))
  (is (close? (h 0.9) (h (/ 90.0 100))))
  ;; Direct posterior KL expectation, using digamma differences H_n-H_m.
  (doseq [[a b :as prior] [[1 1] [9 1] [90 10]]]
    (let [n (+ a b) p (/ a n)
          kl1 (+ (Math/log (/ n (double a))) (double (- (harmonic a) (harmonic n))))
          kl0 (+ (Math/log (/ n (double b))) (double (- (harmonic b) (harmonic n))))]
      (is (close? (novelty prior) (+ (* p kl1) (* (- 1 p) kl0)))))))

(deftest frozen-runs-and-novelty-ablation
  (doseq [run ["1789964661" "1789952479"]]
    (let [f (prepare (fixture run)) r (report f)
          baseline (evaluate f (repeat (count (:rows f)) [9 1]) :fixed-recorded-G 0)]
      (doseq [[c row] (map vector (:candidates f) (:rows baseline))]
        (is (close? (get-in f [:law :posterior (:id c)]) (:posterior row))))
      (doseq [c (:cases r)]
        (is (close? 1 (reduce + (map :posterior (:rows c)))))
        (doseq [row (:rows c) :when (empty? (:effects row))]
          (is (zero? (:novelty row)))))
      (doseq [[off on] (partition 2 (:cases r))]
        (is (= (:choice off) (assoc (:choice on) :mass (get-in off [:choice :mass])))))
      (when-let [[off on] (:heterogeneous-equal-mean-control r)]
        (is (not= (get-in off [:choice :action]) (get-in on [:choice :action]))))
      (prn r))))
