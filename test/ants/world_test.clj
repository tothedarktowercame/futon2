(ns ants.world-test
  (:require [clojure.test :refer :all]
            [ants.aif.core :as core]
            [ants.aif.observe :as observe]
            [ants.aif.policy :as policy]))

;; ---------- Minimal world + helpers for tests --------------------------

(defn seed-world
  ([] (seed-world {:w 50 :h 50}))
  ([{:keys [w h]}]
   {:grid {:size [w h]
           :max-food 5.0
           :max-pher 5.0
           :cells (into {}
                        (for [x (range w) y (range h)]
                          [[x y] {:food 0.0 :pher 0.0 :home nil :ant nil}]))}
    :homes {:aif [20 20] :classic [30 30]}
    :config {:aif core/default-aif-config}}))

(defn place-food [world loc v]
  (assoc-in world [:grid :cells loc :food] (double v)))

(defn set-home [world species loc]
  (-> world
      (assoc-in [:homes species] loc)
      (assoc-in [:grid :cells loc :home] species)))

(defn zero-home-food [world]
  (reduce (fn [w [species home]]
            (assoc-in w [:grid :cells home :food] 0.0))
          world
          (get-in world [:homes])))

(defn world-with
  "Convenience builder:
   {:food-at-home x, :food-at loc->v, :home [x y]}"
  [{:keys [food-at-home food-at home]}]
  (let [home (or home [10 10])
        world (-> (seed-world)
                   (set-home :aif home)
                   (cond-> (some? food-at-home)
                     (place-food home food-at-home)))]
    (cond
      (nil? food-at) world
      (map? food-at) (reduce (fn [w [loc v]] (place-food w loc v)) world food-at)
      (vector? food-at) (let [[loc v] food-at]
                          (place-food world loc v))
      :else world)))

(defn home-of [world species] (get-in world [:homes species]))

(defn predict-outcome [mu obs act]
  ((requiring-resolve 'ants.aif.policy/predict-outcome) mu obs act))

(defn g-observe [world ant]
  (observe/g-observe world ant)) ; normalizes & includes friendly-home/cargo. :contentReference[oaicite:4]{index=4}

(defn aif-step [world ant] ((requiring-resolve 'ants.aif.core/aif-step) world ant))

;; --- [3] “Return @ home with nothing” attractor ------------------------

(defn simulate-actions
  "Run N ticks for a single AIF ant; return chosen actions in a vector."
  [{:keys [ticks] :or {ticks 5}}
   {:keys [home-food aif-cargo] :or {home-food 0.0 aif-cargo 0.0}}]
  (let [home [20 20]
        world (-> (seed-world) (set-home :aif home) (place-food home home-food))
        ant   {:species :aif :loc home :home home :cargo aif-cargo}]
    (loop [w world a ant n ticks acc []]
      (if (zero? n)
        acc
        (let [{:keys [action ant]} (aif-step w a)]
          (recur w (assoc ant :loc (:loc ant)) (dec n) (conj acc action)))))))

(deftest no-empty-return-at-home
  (let [acts (simulate-actions {:ticks 5} {:home-food 0.0 :aif-cargo 0.0})]
    (is (not-any? #{:return} acts))))

;; --- [4] Ingest mapping sanity -----------------------------------------
;; We use policy/predict-outcome as the 'observation predictor'.
;; It already treats :forage differently at/away from home, and propagates :friendly-home. :contentReference[oaicite:5]{index=5}

(deftest ingest-only-on-real-forage
  (let [w   (-> (world-with {:food-at-home 0.0})
                (place-food [12 12] 4.0))
        ant {:species :aif :home (home-of w :aif) :loc (home-of w :aif)}
        obs-home (g-observe w ant)
        obs-away (g-observe w (assoc ant :loc [12 12]))
        out-forage-home (predict-outcome {:h 0.5} obs-home :forage)
        out-forage-away (predict-outcome {:h 0.5} obs-away :forage)
        out-return-home (predict-outcome {:h 0.5} obs-home :return)]
    ;; At home: forage shouldn't imply ingest (target is ~0.05 when friendly-home). :contentReference[oaicite:6]{index=6}
    (is (<= (:ingest out-forage-home) 0.2))
    ;; Away from home + real food: forage should increase ingest proxy.
    (is (> (:ingest out-forage-away) 0.4))
    ;; Return shouldn't create 'ingest' at home.
    (is (<= (:ingest out-return-home) 0.4))))

;; --- [5] Home cell food clamp ------------------------------------------

(deftest home-food-is-zero
  (let [w0 (-> (seed-world)
               (set-home :aif [8 8])
               (place-food [8 8] 0.9))
        w1 (zero-home-food w0)]
    (is (= 0.0 (get-in w1 [:grid :cells [8 8] :food])))))

;; --- [6] “Eat what you’re standing on” guard (non-home) ----------------
;; Define a tiny predicate that says: if there is significant food where you stand,
;; and you're not on your home cell, prefer foraging.

(defn must-forage? [{:keys [loc home cargo capacity] :as ant} world]
  (let [cell (get-in world [:grid :cells loc] {:food 0.0 :home nil})
        on-home (= (:home cell) :aif)
        food (double (or (:food cell) 0.0))
        cap  (double (or capacity 0.7))
        load (double (or cargo 0.0))]
    (and (not on-home)
         (> food 0.3)
         (< load cap))))

(deftest must-forage-here-when-food
  (let [w (-> (seed-world)
              (set-home :aif [0 0])
              (place-food [10 10] 0.6))
        ant {:loc [10 10] :home [0 0] :cargo 0.0 :capacity 0.7}]
    (is (true? (must-forage? ant w)))
    (is (false? (must-forage? (assoc ant :loc [0 0]) w))))) ; not at home
