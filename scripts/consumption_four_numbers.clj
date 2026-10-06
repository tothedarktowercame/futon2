(ns consumption-four-numbers
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [clojure.string :as str]
            [futon2.aif.cascade-model-manifest :as manifest]
            [futon2.aif.cascade-shape-g :as shape-g])
  (:import (java.security MessageDigest)))

(def target "meta-outer-policy-cascade")
(def report-path "holes/labs/wm-contract/REPORT-consumption-four-numbers-2026-10-06.md")
(def proposal-path "holes/labs/wm-contract/proposals/meta-outer-policy-cascade-extended-proposal.edn")
(def pin-paths
  ["holes/labs/wm-contract/NOTE-outer-cascade-as-pasted-blends-2026-10-05.md"
   proposal-path
   "holes/labs/wm-contract/REPORT-diamond-four-numbers-2026-10-05.md"
   "holes/labs/wm-contract/REPORT-extended-diamond-validation-2026-10-06.md"
   "scripts/consumption_four_numbers.clj"])

(def observe "meta/observe-the-meta-field")
(def fill "meta/fill-meta-policy-slots")
(def injury "meta/injury-routes-to-self-heal")
(def minimise "meta/minimise-g-over-filled-meta-policies")
(def alcove "meta/inert-alcove")
(def original-patterns [observe fill injury minimise])
(def original-semantics
  {observe {:needs #{} :produces #{:field-observation :injury-observation}}
   fill {:needs #{:field-observation} :produces #{:filled-candidates :typed-exclusions}}
   injury {:needs #{:field-observation :injury-observation}
           :produces #{:admitted-support :rearm-slot}}
   minimise {:needs #{:filled-candidates :typed-exclusions :admitted-support :rearm-slot}
             :produces #{:selected-meta-policy}}})
(def want #{:selected-meta-policy})
(defn edge [a b] {:from a :to b :kind :precedes})

(defn keyword->pattern [k] (subs (str k) 1))
(defn proposal-data [] (edn/read-string (slurp proposal-path)))
(defn extended-semantics [proposal]
  (into {} (for [[unit semantics] (:tokens proposal)]
             [(keyword->pattern unit) semantics])))

(defn linear-extensions [nodes edges]
  (let [n (count nodes)
        idx (zipmap nodes (range))
        prereq (reduce (fn [v {:keys [from to]}]
                         (update v (idx to) bit-or (bit-shift-left 1 (idx from))))
                       (vec (repeat n 0)) edges)
        full (dec (bit-shift-left 1 n))
        memo (atom {})]
    (letfn [(go [mask]
              (if (= mask full)
                1
                (if-let [hit (find @memo mask)]
                  (val hit)
                  (let [answer
                        (reduce + 0
                                (for [i (range n)
                                      :when (and (zero? (bit-and mask (bit-shift-left 1 i)))
                                                 (= (prereq i) (bit-and mask (prereq i))))]
                                  (go (bit-or mask (bit-shift-left 1 i)))))]
                    (swap! memo assoc mask answer)
                    answer))))]
      (go 0))))

(defn arrangement [id label patterns precedence edges semantics]
  (let [nodes (mapv #(hash-map :pattern %) patterns)]
    {:id id :label label :patterns patterns :semantics semantics
     :cascade {:nodes nodes :precedence precedence :edges edges}
     :linear-extensions (linear-extensions patterns edges)}))

(defn arrangements []
  (let [proposal (proposal-data)
        extended (mapv keyword->pattern (:patterns proposal))
        extended-edges (mapv (fn [[a b]] (edge (keyword->pattern a) (keyword->pattern b)))
                             (:precedence proposal))
        ext-semantics (extended-semantics proposal)
        diamond-edges [(edge observe fill) (edge observe injury)
                       (edge fill minimise) (edge injury minimise)]
        alcove-semantics (assoc original-semantics alcove
                                {:needs #{:field-observation} :produces #{:alcove-token}})]
    [(arrangement "diamond" "diamond" original-patterns original-patterns
                  diamond-edges original-semantics)
     (arrangement "chain-1" "chain-1" original-patterns original-patterns
                  [(edge observe fill) (edge fill injury) (edge injury minimise)]
                  original-semantics)
     (arrangement "chain-2" "chain-2" original-patterns [observe injury fill minimise]
                  [(edge observe injury) (edge injury fill) (edge fill minimise)]
                  original-semantics)
     (arrangement "bag" "bag" original-patterns original-patterns [] original-semantics)
     (arrangement "alcove" "house of alcoves"
                  [observe fill injury alcove minimise]
                  [observe fill injury alcove minimise]
                  (conj diamond-edges (edge observe alcove)) alcove-semantics)
     (arrangement "extended" "extended shape" extended extended extended-edges ext-semantics)
     (arrangement "extended-bag" "extended bag" extended extended [] ext-semantics)
     (arrangement "extended-chain" "extended chain" extended extended
                  (mapv (fn [[a b]] (edge a b)) (partition 2 1 extended)) ext-semantics)]))

(defn done-token [pattern] [target :pattern-done pattern])

(defn fired-series [score patterns]
  (let [candidate (:candidate score)
        transition (constantly (:precedence candidate))
        progress (set (map done-token patterns))
        acceptance (set (map done-token (:terminals score)))]
    (mapv (fn [tau]
            (let [distribution (manifest/rollout transition {#{} 1} tau)]
              {:tau tau
               :expected-count
               (reduce + 0.0 (for [[state mass] distribution]
                               (* (double mass) (count (set/intersection progress state)))))
               :p-want
               (reduce + 0.0 (for [[state mass] distribution
                                   :when (set/subset? acceptance state)]
                               (double mass)))}))
          (range 1 (inc (:horizon score))))))

(defn root-refusal [{:keys [patterns cascade semantics]}]
  (let [non-roots (set (map :to (:edges cascade)))
        roots (remove non-roots patterns)
        invalid (vec (for [unit roots :when (seq (get-in semantics [unit :needs]))]
                       {:unit unit :needs (get-in semantics [unit :needs])}))]
    (when (seq invalid)
      {:status :refused :kind :typed-needs-unsatisfied-at-first-frontier
       :tau 1 :units invalid})))

(defn consumption-series [{:keys [patterns semantics] :as arrangement} horizon]
  (if-let [refusal (root-refusal arrangement)]
    {:refusal refusal}
    (loop [tau 1 held #{} fired #{} consumed #{} out []]
      (if (> tau horizon)
        {:series out}
        (let [ready (set (for [unit patterns
                               :when (and (not (fired unit))
                                          (set/subset? (get-in semantics [unit :needs]) held))]
                           unit))
              produced (reduce set/union #{}
                               (map #(get-in semantics [% :produces]) ready))
              taken (reduce set/union
                            (set/intersection produced want)
                            (map #(get-in semantics [% :needs]) ready))
              consumed' (set/union consumed taken)
              fired' (set/union fired ready)
              completed' (set (for [unit fired'
                                    :when (seq (set/intersection
                                                (get-in semantics [unit :produces]) consumed'))]
                                unit))
              held' (set/union held produced)
              row {:tau tau :count (count completed')
                   :want-met? (set/subset? want held')
                   :fired (sort ready)}]
          (recur (inc tau) held' fired' consumed' (conj out row)))))))

(defn preference-probabilities [n horizon series]
  (let [preference (#'shape-g/progress-preference n horizon)]
    (mapv (fn [{:keys [tau count want-met?]}]
            {:tau tau :outcome [count want-met?]
             :probability (get-in preference [tau [count want-met?]])})
          series)))

(defn measure [arrangement]
  (let [score (shape-g/score-arranged target (:id arrangement) (:cascade arrangement))]
    (when (= :refused (:status score))
      (throw (ex-info "control score-arranged refused" {:arrangement (:id arrangement)
                                                        :refusal score})))
    (let [consumption (consumption-series arrangement (:horizon score))]
      (assoc arrangement :score score
             :fired-series (fired-series score (:patterns arrangement))
             :consumption
             (cond-> consumption
               (:series consumption)
               (assoc :preference-probabilities
                      (preference-probabilities (count (:patterns arrangement))
                                                (:horizon score) (:series consumption))))))))

(defn sha256 [path]
  (let [digest (MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream path)]
      (let [buf (byte-array 8192)]
        (loop []
          (let [n (.read in buf)]
            (when (pos? n) (.update digest buf 0 n) (recur))))))
    (format "%064x" (BigInteger. 1 (.digest digest)))))

(defn fmt [x] (format "%.12f" (double x)))
(defn fmt4 [x] (format "%.4f" (double x)))
(defn log2 [n] (/ (Math/log (double n)) (Math/log 2.0)))
(defn fired-text [rows]
  (str/join " " (for [{:keys [tau expected-count p-want]} rows]
                    (format "τ%d:[%.6f,%.6f]" tau expected-count p-want))))
(defn consumption-text [{:keys [series refusal]}]
  (if refusal
    (str "`" (pr-str refusal) "`")
    (str/join " " (for [{:keys [tau count want-met?]} series]
                    (str "τ" tau ":[" count "," want-met? "]")))))
(defn preference-text [{:keys [preference-probabilities refusal]}]
  (if refusal "—"
      (str/join " " (for [{:keys [tau outcome probability]} preference-probabilities]
                      (str "τ" tau ":C" outcome "=" (fmt probability))))))

(def predictions
  {"diamond" "[0 1 4 4]; G(ii) unavailable"
   "chain-1" "[0 1 4 4]; G(ii) unavailable"
   "chain-2" "[0 1 4 4]; G(ii) unavailable"
   "bag" "refused at τ=1"
   "alcove" "[0 1 4 4 4]; no gain over diamond"
   "extended" "[0 1 6 6 6 6 6]; G(ii) unavailable"
   "extended-bag" "refused at τ=1"
   "extended-chain" "[0 1 6 6 6 6 6]; G(ii) unavailable"})

(defn actual-summary [{:keys [consumption]}]
  (if-let [r (:refusal consumption)]
    (str "refused at τ=" (:tau r))
    (str (mapv :count (:series consumption)) "; G(ii) unavailable")))

(defn render [results pins]
  (str "# Consumption-counted progress: four, alcove, and extended arrangements — 2026-10-06\n\n"
       "Reproduce in a fresh process from `/home/joe/code/futon2`:\n\n"
       "```sh\nclojure -M scripts/consumption_four_numbers.clj\n```\n\n"
       "## Source pins\n\n| input | sha256 |\n|---|---|\n"
       (str/join "\n" (for [[path hash] pins] (str "| `" path "` | `" hash "` |")))
       "\n\n## Step 1: can the existing scorer accept consumption progress as data?\n\n"
       "No. `score-arranged` has only `[target id cascade]` and `[target id cascade ledger-root]` at `src/futon2/aif/cascade_shape_g.clj:211-217`; it constructs `progress-tokens` from every `:pattern-done` token at lines 250-255. The lower-level `rank-cascade-actions` accepts an `:observation-model` option (`cascade_observation_scoring.clj:220-249`), but `progress-outcome` still computes the count as a static intersection of state with that model's `:progress-tokens` (`observation_model.clj:274-280`). There is no function argument for an externally supplied set-by-τ or count-by-τ series. Consequently rule (ii) below records the exact consumption series and evaluates each observed `[count want-met?]` with the scorer's own private `progress-preference`, but it does **not** report that preference probability as G. Consumption G is typed `:unavailable :scorer-has-no-consumption-observation-input`.\n\n"
       "## Measurement rule\n\n"
       "The current-rule column is the scorer's exact stochastic rollout: each entry is `τ:[expected fired-pattern count, P(want met)]`. The consumption-rule column is the maximal no-simultaneity typed trace: a producer becomes complete at the first later step when a firing unit consumes one of its products, or at the production step if the want consumes it. An arrangement is refused when its first arrangement frontier contains a unit whose typed needs are not held. R8(a) assigns no separate cost to inert products.\n\n"
       "| arrangement | fired-pattern series (i) | G (i) | consumption series (ii) | per-step C probability for (ii) | G (ii) | log2 extensions | G(i)+ordering |\n"
       "|---|---|---:|---|---|---|---:|---:|\n"
       (str/join "\n"
                 (for [{:keys [label fired-series score consumption linear-extensions]} results]
                   (str "| " label " | `" (fired-text fired-series) "` | " (fmt (:g score))
                        " | " (consumption-text consumption) " | " (preference-text consumption)
                        " | `:unavailable` | " (fmt (log2 linear-extensions)) " | "
                        (fmt (+ (:g score) (log2 linear-extensions))) " |")))
       "\n\nCounts in the consumption series are completed producers, not raw consumed-token cardinality; this preserves the scorer's outcome shape `[completed-pattern-count want-met?]`. The extended coupling square and the alcove both produce tokens with no consumer, so neither producer completes.\n\n"
       "## Control check\n\n"
       "The existing scorer was called unchanged for every row. The original four-node controls are diamond "
       (fmt4 (get-in (first (filter #(= "diamond" (:id %)) results)) [:score :g]))
       ", bag " (fmt4 (get-in (first (filter #(= "bag" (:id %)) results)) [:score :g]))
       ", and both chains " (fmt4 (get-in (first (filter #(= "chain-1" (:id %)) results)) [:score :g]))
       ", reproducing `REPORT-diamond-four-numbers-2026-10-05.md` (1.2797, 0.3929, 1.5166). The values 1.3105 and 0.2381 named in the measurement brief are the **extended** chain and extended bag controls from the 2026-10-06 report; this run gives "
       (fmt4 (get-in (first (filter #(= "extended-chain" (:id %)) results)) [:score :g]))
       " and " (fmt4 (get-in (first (filter #(= "extended-bag" (:id %)) results)) [:score :g]))
       ", respectively. Thus all three specifically requested checkpoints—1.2797 / 1.3105 / 0.2381—are reproduced without conflating the two reports.\n\n"
       "## Prediction versus result\n\n| arrangement | prediction made before run | result |\n|---|---|---|\n"
       (str/join "\n" (for [{:keys [id label] :as result} results]
                          (str "| " label " | " (predictions id) " | " (actual-summary result) " |")))
       "\n\nThe bag behavior under R8(a) is **refused at τ=1**: `fill`, `injury`, and `minimise` are arrangement roots but have unmet typed needs. The extended bag is refused for the same reason. The alcove's completed-producer series is the diamond's `[0 1 4 4]` padded to its five-step horizon, so its inert output adds no progress. Both chains equal the diamond on consumption progress because their extra ordering edge carries no needed token; only their separately reported ordering term differs.\n\n"
       "## Proposed source change (not applied)\n\n"
       "The smallest honest interface is not a precomputed scalar series: it is an outcome function evaluated on every predicted state, because risk requires the full predictive distribution. A proposed change is:\n\n"
       "```diff\n"
       "--- a/src/futon2/aif/observation_model.clj\n"
       "+++ b/src/futon2/aif/observation_model.clj\n"
       "@@\n"
       "-(defn- progress-outcome [{:keys [progress-tokens want]} state]\n"
       "-  [(count (set/intersection progress-tokens state))\n"
       "-   (set/subset? want state)])\n"
       "+(defn- progress-outcome [{:keys [progress-tokens want progress-outcome-fn] :as model} state tau]\n"
       "+  (if progress-outcome-fn\n"
       "+    (progress-outcome-fn {:model model :state state :tau tau})\n"
       "+    [(count (set/intersection progress-tokens state))\n"
       "+     (set/subset? want state)]))\n"
       "@@\n"
       "-(defn- progress-predictive [model belief]\n"
       "+(defn- progress-predictive [model belief tau]\n"
       "@@\n"
       "-            (update out (progress-outcome model state) (fnil + 0) mass))\n"
       "+            (update out (progress-outcome model state tau) (fnil + 0) mass))\n"
       "--- a/src/futon2/aif/cascade_shape_g.clj\n"
       "+++ b/src/futon2/aif/cascade_shape_g.clj\n"
       "@@\n"
       "+;; Add an options arity and place its :progress-outcome-fn in model.\n"
       "```\n\n"
       "That callback would also require the rollout state to retain consumed-token provenance; today's state is only a held-token set. The proposal therefore names the missing interface without pretending that a static `:progress-tokens` replacement implements R8(a). No file under `src/` was edited.\n"))

(defn assert-controls! [results]
  (let [g (into {} (map (juxt :id #(get-in % [:score :g])) results))
        expected {"diamond" 1.279742336087
                  "chain-1" 1.516585504479
                  "chain-2" 1.516585504479
                  "bag" 0.392913018968
                  "extended-chain" 1.3105
                  "extended-bag" 0.238083059884}]
    (doseq [[id value] expected]
      (when (> (Math/abs (- (double (g id)) value)) 5.0e-5)
        (throw (ex-info "control G did not reproduce" {:id id :expected value :actual (g id)}))))))

(defn -main [& _]
  (let [results (mapv measure (arrangements))]
    (assert-controls! results)
    (spit report-path (render results (mapv (fn [path] [path (sha256 path)]) pin-paths)))
    (println report-path)))

(apply -main *command-line-args*)
