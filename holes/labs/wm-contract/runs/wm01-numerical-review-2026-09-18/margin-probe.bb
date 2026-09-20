;; Independent WM-01 ordering-margin measurement (zai-8, read-only probe).
;; Reconstructs the committed fixture (futon2 6826c98d) and measures
;; enclosure widths vs score gaps directly.
(require '[futon2.aif.cascade-g :as cg]
         '[futon2.aif.cascade-model-manifest :as m]
         '[futon2.aif.efe :as efe])

(def rates {"t0" {:false-neg 1/10 :false-pos 1/20}
            "t1" {:false-neg 1/5 :false-pos 1/10}})
(def states [#{} #{"t0"} #{"t1"} #{"t0" "t1"}])
(def outcomes states)
(def spec (m/preference-spec {:want #{"t0" "t1"} :evidence #{} :lam 1 :mu 0 :zeroed #{}}))
(def c-float (m/preference-distribution spec #{"t0" "t1"}))
(def model-id {:id "wm01-numerical-operations" :revision "v1"})

(def produce-t0
  {:id :produce-t0
   :guard {:status :interpreted :operator :and
           :clauses [{:status :interpreted :present #{} :absent #{}}]}
   :transition {:status :interpreted :operator :union :produces #{"t0"}}
   :produces #{"t0"}})

(def produce-t1
  {:id :produce-t1
   :guard {:status :interpreted :operator :and
           :clauses [{:status :interpreted :present #{} :absent #{}}]}
   :transition {:status :interpreted :operator :union :produces #{"t1"}}
   :produces #{"t1"}})

(def a
  {:model model-id :state-support states :outcome-support outcomes
   :authority :observed-estimate
   :rows (into {}
               (map (fn [s] [s {:support outcomes
                                :mass (m/observation-distribution rates s)}]))
               states)})

(defn cg-step [q-tau a point]
  (let [q-support (vec (sort-by count (keys q-tau)))]
    (cg/step-g
     {:q {:model model-id :state-support q-support :mass q-tau
          :authority :declared-prior}
      :a (assoc a :state-support q-support
                :rows (select-keys (:rows a) q-support))
      :c {:model model-id :support outcomes :mass c-float
          :provenance {:producer "cascade-model-manifest/preference-distribution"}}
      :context {:policy/id :probe :occurrence/id :wm01 :point point}})))

(def q0 (m/observed-belief #{}))
(def horizons 2)

(defn enclosure [precedence]
  (let [prec (constantly precedence)
        steps (mapv (fn [tau]
                      (:inputs (cg-step (m/rollout prec q0 tau) a tau)))
                    (range 1 (inc horizons)))
        g (cg/total-g {:schedule (vec (range 1 (inc horizons))) :steps steps})]
    (:g g)))

(def cands [[:none []] [:to-t0 [produce-t0]] [:to-t1 [produce-t1]]])

(def with-g
  (mapv (fn [pair]
          (let [id (first pair)
                enc (enclosure (second pair))
                lo (double (first enc))
                hi (double (second enc))]
            {:id id :lo lo :hi hi :w (- hi lo)
             :exact-width (with-precision 10 (- (second enc) (first enc)))
             :exact-enc enc}))
        cands))

(def ranked
  (efe/rank-actions
   {:cascade-belief q0}
   (map (fn [pair] {:kind :cascade-candidate :id (first pair)
                    :precedence (second pair)}) cands)
   {:horizon-steps horizons :cascade-spec spec :adjudication-rates rates}))

(println "float order:" (mapv :cascade-id ranked))
(doseq [e with-g]
  (printf "  %s lo=%.15g hi=%.15g width=%.3e exact-width=%s\n"
          (:id e) (:lo e) (:hi e) (:w e) (:exact-width e)))
(doseq [pair (partition 2 1 (sort-by :lo with-g))]
  (let [x (first pair) y (second pair)
        gap (- (:lo y) (:hi x))
        width (max (:w x) (:w y) 1e-300)]
    (printf "GAP %s->%s gap=%.6g width=%.3e ratio=%.3g\n"
            (:id x) (:id y) gap width (/ gap width))))
