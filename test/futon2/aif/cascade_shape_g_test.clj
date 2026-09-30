(ns futon2.aif.cascade-shape-g-test
  (:require [clojure.set :as set]
            [clojure.test :refer [deftest is]]
            [futon2.aif.analysis-cascade :as analysis]
            [futon2.aif.cascade-model-manifest :as manifest]
            [futon2.aif.cascade-shape-g :as shape-g]
            [futon2.aif.cascade-observation-scoring :as scorer]
            [futon2.aif.learning-trial-ledger :as ledger]))

(def artifacts "holes/labs/wm-contract/mission-head-cascades-2026-09-30")

(def same-order-a
  {:nodes [{:pattern "p/a"} {:pattern "p/b"} {:pattern "p/c"}]
   :precedence ["p/a" "p/b" "p/c"]
   :edges [{:from "p/a" :to "p/b" :kind :precedes}
           {:from "p/b" :to "p/c" :kind :precedes}]})

(def same-order-b
  {:nodes (:nodes same-order-a)
   :precedence (:precedence same-order-a)
   :edges [{:from "p/a" :to "p/c" :kind :precedes}]})

(deftest arrangement-reaches-the-real-scorer
  (let [a (shape-g/score-arranged "target" "chain" same-order-a)
        b (shape-g/score-arranged "target" "join" same-order-b)]
    (is (= [:computed :computed] [(:status a) (:status b)]))
    (is (not= (:g a) (:g b))
        "same firing list, different edges: compiling only the list makes this fail")
    (doseq [r [a b]]
      (is (every? #(Double/isFinite (double %))
                  ((juxt :g :risk :ambiguity :information-gain) r)))
      (is (= (:horizon r) (count (:preference-at-each-step r)))))))

(deftest arrangement-changes-the-co-application-transition
  (let [chain (shape-g/arranged->candidate "target" "chain" same-order-a)
        independent (shape-g/arranged->candidate
                     "target" "independent" (assoc same-order-a :edges []))
        chain-distribution (manifest/rollout (constantly (:precedence chain)) {#{} 1} 1)
        independent-distribution (manifest/rollout
                                  (constantly (:precedence independent)) {#{} 1} 1)
        all-done #{["target" :pattern-done "p/a"]
                   ["target" :pattern-done "p/b"]
                   ["target" :pattern-done "p/c"]}
        chain-score (shape-g/score-arranged "target" "chain" same-order-a)
        independent-score (shape-g/score-arranged
                           "target" "independent" (assoc same-order-a :edges []))]
    (is (= 0 (get chain-distribution all-done 0)))
    (is (= 1/8 (get independent-distribution all-done)))
    (is (not= chain-distribution independent-distribution)
        "plain first-enabled precedence makes this assertion fail")
    (is (not= (:g chain-score) (:g independent-score)))
    (is (not= (get-in chain-score [:scorer-result 0 :certificate :g-terms])
              (get-in independent-score [:scorer-result 0 :certificate :g-terms])))
    (is (= :co-application-frontier-theta-v1
           (get-in chain-score
                   [:scorer-result 0 :certificate :node-evaluations 0 :model :semantics])))))

(deftest overlap-is-a-shared-advance-token
  (let [c (assoc same-order-a :edges [{:from "p/a" :to "p/b" :kind :overlap}])
        candidate (shape-g/arranged->candidate "t" "overlap" c)
        {:keys [units patterns]} (get-in candidate [:precedence :co-apply])
        pa (patterns (first units))
        pb (patterns (second units))]
    (is (= 1 (count (set/intersection (:produces pa) (:produces pb)))))
    (is (empty? (get-in pb [:guard :clauses 0 :present]))
        "overlap co-advances shared state; it is not an enabling edge")))

(declare fit-analysis)

(deftest repeated-pattern-citations-remain-ordered-occurrences
  (let [a (fit-analysis ["p/a" "p/b" "p/a"])
        cascade (first (:cascades (analysis/analysis->cascades a)))
        score (with-redefs [ledger/pattern-theta
                            (fn [_] {:status :no-recorded-trials})]
                (shape-g/score-arranged "t" "repeated" cascade))
        co (get-in score [:candidate :precedence :co-apply])
        raw-info (get-in score [:scorer-result 0 :certificate :g-terms
                                :raw :expected-information-gain])
        roots (remove (set (map second (:descent co))) (:units co))]
    (is (= [["p/a" 0] ["p/b" 1] ["p/a" 2]] (:units co)))
    (is (= [[["p/a" 0] ["p/b" 1]]
            [["p/b" 1] ["p/a" 2]]]
           (:descent co)))
    (is (= [["p/a" 0]] (vec roots)))
    (is (= 3 (count (:patterns co))))
    (is (Double/isFinite (double (:g score))))
    (is (== 2.0 (/ raw-info (:information-gain score)))
        "parameter information is normalized by two distinct pattern ids")))

(deftest cyclic-arrangement-is-refused-before-scoring
  (let [cascade {:nodes [{:pattern "p/a"} {:pattern "p/b"}]
                 :edges [{:from "p/a" :to "p/b" :kind :precedes}
                         {:from "p/b" :to "p/a" :kind :precedes}]}
        result (shape-g/score-arranged "t" "cycle" cascade)]
    (is (= :refused (:status result)))
    (is (= :cyclic-arrangement (:kind result)))
    (is (= (first (:cycle result)) (last (:cycle result))))))

(deftest wide-frontier-is-refused-before-exact-enumeration
  (let [cascade {:nodes (mapv #(str "p/" %) (range 13)) :edges []}
        started (System/nanoTime)
        result (with-redefs [scorer/rank-cascade-actions
                             (fn [& _] (throw (ex-info "scorer must not run" {})))
                             manifest/rollout
                             (fn [& _] (throw (ex-info "enumeration must not run" {})))]
                 (shape-g/score-arranged "t" "wide" cascade))
        elapsed-ms (/ (- (System/nanoTime) started) 1.0e6)]
    (is (= {:status :refused
            :kind :frontier-too-wide-for-exact-enumeration
            :policy-id "wide" :target "t" :units 13 :roots 13
            :bound 13 :limit shape-g/exact-enumeration-frontier-limit}
           result))
    (is (< elapsed-ms 1000.0))))

(deftest five-independent-units-remain-exactly-scorable
  (let [cascade {:nodes (mapv #(str "p/" %) (range 5)) :edges []}
        result (shape-g/score-arranged "t" "five" cascade)]
    (is (= :computed (:status result)))
    (is (Double/isFinite (double (:g result))))))

(defn- fit-analysis [accepted-patterns]
  {:sentences [{:fragments (mapv (fn [i p]
                                   {:start i :end (inc i) :text p :relations ["action"]
                                    :pattern_refs [{:id p :status "candidate"
                                                    :rationale "fixture fit"
                                                    :source_sha256 (apply str (repeat 64 "a"))}]})
                                 (range) accepted-patterns)}]})

(deftest unknown-connector-has-higher-f-and-g
  (let [cascade {:nodes [{:pattern "p/a"} {:pattern "p/b"}]
                 :edges [{:from "p/a" :to "p/b" :kind :precedes}]}
        policy {:target "t" :policy-id "same-shape" :cascade cascade}
        fitted (shape-g/score-policy (assoc policy :analysis (fit-analysis ["p/a" "p/b"])))
        unknown (shape-g/score-policy (assoc policy :analysis (fit-analysis ["p/a"])))]
    (is (< (:f fitted) (:f unknown)))
    (is (< (:g fitted) (:g unknown)))
    (is (= :computed (:f-status fitted) (:f-status unknown)))
    (is (= :sigma-log-E-minus-F-minus-gamma-G
           (get-in unknown [:selection-law :law])))
    (is (= ["p/b"] (get-in unknown [:fit :interpretation-owed])))))

(deftest unexplored-parameter-lowers-g
  (let [cascade {:nodes [{:pattern "p/a"}] :edges []}
        unexplored (with-redefs [ledger/pattern-theta
                                (fn [_] {:status :no-recorded-trials})]
                     (shape-g/score-arranged "t" "unexplored" cascade))
        explored (with-redefs [ledger/pattern-theta
                              (fn [_] {:status :recorded-trials :theta 1/2
                                       :trials-count 20 :successes 10})]
                   (shape-g/score-arranged "t" "explored" cascade))]
    (is (< (:g unexplored) (:g explored))
        "dropping the information subtraction makes this fail")
    (is (> (:information-gain unexplored) (:information-gain explored)))))

(defn- progress-fixture [id first-produces]
  {:kind :cascade-candidate :id id :target "t"
   :precedence [{:id :first :pattern-id :first :target "t"
                 :guard {:status :interpreted :clauses [{:present #{} :absent #{}}]}
                 :produces first-produces :theta 1
                 :theta-record {:status :recorded-trials :trials-count 1 :successes 1}}
                {:id :last :pattern-id :last :target "t"
                 :guard {:status :interpreted :clauses [{:present first-produces :absent #{}}]}
                 :produces #{:p1 :p2 :want} :theta 1
                 :theta-record {:status :recorded-trials :trials-count 1 :successes 1}}]})

(deftest progress-is-preferred-at-every-step-without-token-truncation
  (let [tokens (set (concat [:p1 :p2 :want] (map #(keyword (str "extra-" %)) (range 9))))
        weights (into {} (for [n (range 4) met? [false true]]
                           [[n met?] (/ (* (bit-shift-left 1 n) (if met? 2 1)) 45)]))
        model {:schema :wm/observation-model-v1 :backend :exact-enumeration
               :kind :progress-count :universe tokens :horizon 2
               :progress-tokens #{:p1 :p2 :want} :want #{:want}
               :progress-preference {1 weights 2 weights}
               :provenance {:status :synthetic :calibrated false :source "progress fixture"}}
        opts {:horizon-steps 2 :observation-model model
              :prediction-context {:occurrence-id "progress" :tau 2}
              :observation {:status :observed :occurrence-id "progress" :tau 2
                            :present #{:want} :absent #{}}
              :g-normalization :per-step-capacity-and-pattern
              :cascade-spec {:want #{:want} :evidence #{} :zeroed #{}
                             :c {:source :graded-progress}
                             :c-schedule [1 2]}}
        early (first (scorer/rank-cascade-actions
                      {:cascade-belief {#{} 1}}
                      [(progress-fixture "early" #{:p1 :p2})] opts))
        late (first (scorer/rank-cascade-actions
                     {:cascade-belief {#{} 1}}
                     [(progress-fixture "late" #{:p1})] opts))]
    (is (< (:controller-score early) (:controller-score late))
        "restoring a not-yet placeholder makes early progress invisible")
    (is (= #{:p1 :p2 :want} (first (keys (get-in early [:prediction :belief]))))
        "both policies reach the same terminal progress state")
    (is (= 12 (count (:universe model)))
        "the compact count observation admits more than the ten-token powerset bound")
    (is (every? (fn [step]
                  (every? vector? (keys (:distribution step))))
                (get-in early [:certificate :consumed-g :C :steps]))
        "no step uses ending/not-yet-evaluated as its preferred outcome")))

(defn- chain-cascade [n]
  (let [patterns (mapv #(str "p/" %) (range n))]
    {:nodes (mapv #(hash-map :pattern %) patterns)
     :edges (mapv (fn [[a b]] {:from a :to b :kind :precedes})
                  (partition 2 1 patterns))}))

(defn- fit-analysis-with-status [n status]
  {:sentences
   [{:fragments
     (mapv (fn [i]
             (let [p (str "p/" i)]
               (cond-> {:start i :end (inc i) :text p :relations ["action"]}
                 (= :accepted status)
                 (assoc :pattern_refs [{:id p :rationale "stated good fit"}])
                 (= :rejected status)
                 (assoc :pattern_rejections [{:id p :reason "stated poor fit"
                                              :query p}]))))
           (range n))}]})

(deftest cross-mission-size-control-prefers-fit-and-steady-progress
  (let [poor-short (shape-g/score-policy
                    {:target "t" :policy-id "poor-3" :cascade (chain-cascade 3)
                     :analysis (fit-analysis-with-status 3 :rejected)})
        good-long (shape-g/score-policy
                   {:target "t" :policy-id "good-9" :cascade (chain-cascade 9)
                    :analysis (fit-analysis-with-status 9 :accepted)})]
    (is (< (+ (:f good-long) (:g good-long))
           (+ (:f poor-short) (:g poor-short)))
        "a short but poorly fitting policy must not win merely because it is short")))

(deftest ^:slow all-recorded-head-policies-have-g
  (let [policies (shape-g/materialize-policies artifacts)
        results (mapv shape-g/score-policy policies)]
    ;; S20: the WebArxana artifact read a HEAD-template definition as work;
    ;; its 12 reported / 6 distinct policies are no longer admissible input.
    (is (= {:reported-count 69 :distinct-count 27} (meta policies)))
    (is (= 27 (count policies)))
    ;; The fixed lab retractions predate S18's authored-direction conversion;
    ;; their widest recorded partial order is 7, so all 27 remain admissible.
    ;; The current graph/provider census separately has three width-13 refusals.
    (is (= 27 (count (filter #(= :computed (:status %)) results))))
    (is (zero? (count (filter #(= :frontier-too-wide-for-exact-enumeration
                                  (:kind %)) results))))
    (is (zero? (count (remove #(Double/isFinite (double (:g %))) results))))
    (is (every? #(< (Math/abs (- (:g %) (+ (:risk %) (:ambiguity %)
                                             (- (:information-gain %)))))
                    1.0e-12)
                results))
    (is (every? #(and (= :computed (:f-status %))
                      (Double/isFinite (double (:f %)))) results))
    (is (every? #(= (:horizon %) (count (:preference-at-each-step %))) results))))
