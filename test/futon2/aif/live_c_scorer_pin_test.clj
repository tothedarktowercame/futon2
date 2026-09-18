(ns futon2.aif.live-c-scorer-pin-test
  "WM-13-delivery W13-1: the live-C-to-scorer path, pinned value-level.

  Dispatch: p4ng wm-walkthroughs/item-owners/closure-plans/wm-13/
  PINNED-DISPATCH-v1.md (271da75, amended 6973417/f248155). Three claims this
  namespace certifies, each with its negative:

  1. SPEC-EQUALITY: the spec the cascade scorer consumes -- the seam
     `cascade-model-manifest/horizon-g-sparse-cert` takes as `:spec`, which
     is where `efe/rank-cascade-actions` sends `(:cascade-spec opts)` --
     accepts `live-c/cascade-spec`'s output over injected sources and
     evaluates it exactly (log-preference-fn over the token universe, exact
     rationals, no refusal). Machine: a FRESH TEST JVM -- this certifies the
     composed function, not the serving JVM's loaded spec (zai-35's C-1
     discipline; a serving-JVM claim needs its own control).

  2. NEGATIVES: a stale signature is detectable and refuses at the caller
     (`live-c/stale?` flips); `:no-reachable-want` passes through as the
     lane's typed refusal rather than a default uniform.

  3. CARRIER VOCABULARY (zai-8): the certificate the scorer emits tags C's
     admission `:float-carried` beside q/A's `:exact` -- the
     float-carrier-1 words. CITED AS VOCABULARY, NOT COVERAGE: this is new
     serving-behavior evidence using the accepted carrier contract's terms,
     not a claim the carrier acceptance already established it."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.live-c :as lc]))

(def sources
  "Injected read-sources shape (same fixture discipline as live_c_test)."
  {:wholeness {:path "/w" :sha256 "a"
               :value {:missions [{:mission "M-a" :class :alive :L 5 :T 1 :H 5}
                                  {:mission "M-b" :class :alive :L 3 :T 3 :H 1}]}}
   :missions [{:path "/m1" :sha256 "m1" :mission "M-a" :text "**Status:** OPEN."}
              {:path "/m2" :sha256 "m2" :mission "M-b" :text "**Status:** OPEN."}]
   :stars {:path "/s" :sha256 "s"
           :value {:capabilities {:c1 {:status :held} :c2 {:status :satisfied}}}}})

(defn- derived []
  (lc/derive-live-c sources))

(deftest live-c-spec-reaches-the-scorers-seam-exactly
  (let [d (derived)
        reachable (conj (:want d) :closed/M-b)   ; every token is in-domain here
        live-spec (lc/cascade-spec d reachable)]
    (testing "the live spec validates as the scorer's own PreferenceSpec"
      (let [v (m/preference-spec live-spec)]
        (is (not (:status v)) (pr-str v))
        (is (= (:want live-spec) (set (:want v))))))
    (testing "log-preference-fn evaluates it over the token universe (floats; the exact side is the admitted weights)"
      ;; log-preference-fn works in doubles by design (its docstring: logs
      ;; keep Z finite at thousands of tokens). The EXACT side of the
      ;; float-carrier-1 boundary is the admission, not this arithmetic:
      ;; preference-spec admits :weights only as positive EXACT RATIONALS.
      (let [lp (m/log-preference-fn live-spec (set reachable))]
        (is (fn? lp))
        ;; o is a SUBSET of the universe (pointwise over outcomes), never a
        ;; bare token
        (is (number? (lp (set reachable))))
        (doseq [tok reachable]
          (is (number? (lp #{tok})) (str tok))))
    (testing "C's weights are admitted exact rationals (float-carrier-1's :exact side)"
      (is (every? #(or (ratio? %) (integer? %)) (vals (:weights live-spec))))
      (let [v (m/preference-spec live-spec)]
        (is (not (:status v)))
        (is (every? #(or (ratio? %) (integer? %)) (vals (:weights v)))))))
    (testing "the weights the derivation produced are the spec's weights"
      (let [v (m/preference-spec (assoc live-spec :weights (:weights live-spec)))]
        (is (not (:status v)))
        (is (= (set (keys (:weights live-spec))) (:want live-spec)))))))

(deftest the-scorer-consumes-the-live-spec-value-level
  ;; The composed seam: the exact call rank-cascade-actions makes. One
  ;; candidate producing one want token, horizon 2, identity rates -- the
  ;; live tick's regime (W13-4: an explicit default, not a structural
  ;; ceiling; this test does not claim A is nonzero).
  (let [d (derived)
        live-spec (lc/cascade-spec d (conj (:want d) :closed/M-b))
        spec (m/preference-spec live-spec)
        universe (:universe spec)
        rates (zipmap universe (repeat {:false-neg 0 :false-pos 0}))
        produce (first (:want spec))
        p1 {:id :p1
            :guard {:status :interpreted :operator :and
                    :clauses [{:status :interpreted :present #{} :absent #{}}]}
            :transition {:status :interpreted :operator :union :produces #{produce}}
            :produces #{produce}}
        {:keys [g certificate]}
        (m/horizon-g-sparse-cert {:rates rates
                                  :q0 {#{} 1}
                                  :precedence-fn (constantly [p1])
                                  :horizon 2
                                  :spec spec})]
    (is (number? g) (pr-str g))
    (is (map? certificate))
    (testing "float-carrier-1 vocabulary, not coverage: what THIS seam's certificate records"
      (is (= :constant-spec (:c-form certificate)) "C came from the spec, declared")
      (is (= :identity-A-zero-rates (:evaluation certificate))
          "the live regime today: an explicit default, never a structural ceiling (W13-4)")
      ;; The per-input :admissions tags (:q/:a :exact, :c :float-carried) are
      ;; cascade_g/step-g's records on the OTHER evaluator, covered by
      ;; cascade_g's own tests; cited here as vocabulary, not asserted as
      ;; coverage of this seam (zai-8's caveat verbatim).
      )))

(deftest negatives-stale-and-no-reachable-want
  (testing "a stale signature is detectable and must not score silently"
    (let [d (derived)
          sources-now (assoc-in sources [:wholeness :value :missions 0 :L] 6)]
      (is (:stale? (lc/stale? d sources-now)))
      ;; the caller-side discipline the pin requires: stale? true means
      ;; re-derive before scoring; scoring the STALE derived map is exactly
      ;; what this control forbids, so the refusal is asserted as the
      ;; detectable precondition, not wired here (the lane does not check
      ;; staleness today -- recorded in the dispatch, W13-1's open seam).
      (is (not= (:signature-now (lc/stale? d sources-now))
                (:signature-derived (lc/stale? d sources-now))))))
  (testing ":no-reachable-want passes through as the typed refusal, never a default uniform"
    (let [d (derived)
          r (lc/cascade-spec d #{::outside-domain})]
      (is (= :no-reachable-want (get-in r [:refusal :kind])))
      ;; and the scorer's own validator refuses the empty-want shape it
      ;; would otherwise default to
      (is (= :invalid-preference-spec
             (:kind (m/preference-spec {:want #{} :evidence #{} :lam 1 :mu 1 :zeroed #{}})))))))
