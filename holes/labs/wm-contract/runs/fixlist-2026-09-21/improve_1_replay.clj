(ns improve-1-replay
  "Read-only inventory and counterfactual; no production parameter updates."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.efe :as efe]
            [futon2.aif.policy :as policy]))

(def root (or (first *command-line-args*) "/home/joe/code"))
(defn read-edn [f] (edn/read-string (slurp f)))
(defn sha [f]
  (format "%064x" (java.math.BigInteger. 1 (.digest (java.security.MessageDigest/getInstance "SHA-256")
                                                  (java.nio.file.Files/readAllBytes (.toPath (io/file f)))))))
(defn edn-files [path]
  (sort-by str (filter #(and (.isFile %) (.endsWith (.getName %) ".edn")) (file-seq (io/file path)))))
(defn close! [x y] (assert (< (abs (- (double x) (double y))) 1e-6) [x y]))
(defn entropy [q] (- (reduce + 0.0 (for [p (vals q) :when (pos? p)] (* (double p) (Math/log (double p)))))))

(defn inventory [f]
  (let [r (read-edn f) d (:dispatch r)
        expected {:occurrence (:occurrence d) :carry-occurrence-id (:carry-occurrence-id d)
                  :universe (:universe d) :declaration-pins (mapv #(select-keys % [:path :sha256]) (:declarations d))}
        jobs (into {} (map (juxt :job-id identity)) [(:author-job r) (:review-job r)])
        v (task/verify-observations-v2 r expected jobs)
        observations (mapv (fn [[token row]]
                             {:token token :observed (get-in row [:artifact-observation :observed])
                              :schedule (:schedule row) :meaning-sha256 (:meaning-sha256 row)
                              :evidence-sha256 (get-in row [:artifact-observation :evidence-sha256])})
                           (sort-by (comp pr-str key) (:observations v)))]
    {:path (str f) :sha256 (sha f) :run (get-in d [:occurrence :run/id])
     :action-id (get-in d [:occurrence :action/id])
     :selected-target (get-in d [:occurrence :action/value :target])
     :selected-patterns (mapv :id (get-in d [:occurrence :action/value :precedence]))
     :revision-pair (:revision-pair r) :causal-attribution (:causal-attribution v)
     :verification (dissoc (select-keys v [:status :kind :reason :detail]) :observations)
     :raw-measurements (count (:after-token-evidence r))
     :boolean-by-target (into (sorted-map)
                              (for [[target rows] (group-by (comp first :token) observations)]
                                [target (frequencies (map :observed (filter #(boolean? (:observed %)) rows)))]))
     :selected-effect-observations
     (filterv #(contains? (into #{} (mapcat :produces) (get-in d [:occurrence :action/value :precedence])) (:token %)) observations)
     :observations observations}))

(def reference-file (str root "/futon2/data/wm-runs/tick-run-record-2026-09-21-1789964661.edn"))
(def record (read-edn reference-file))
(def candidates (get-in record [:decision :selection-certificate :candidates]))
(def terms (get-in record [:decision :g-term-decomposition :policies 0 :terms]))
(def c (get-in terms [:C :value]))
(def terminal (:distribution (last (:steps c))))
(def spec {:want (set (keys (:weights terminal))) :weights (:weights terminal) :lam 1 :mu 0
           :evidence #{} :zeroed (:zeroed terminal) :c-schedule (:schedule c)})
(def q0 (get-in terms [:D :value]))
(def rates (get-in terms [:A :value]))
(def horizon (count (:steps c)))
(def prior [9 1])
(defn posterior-mean [observed]
  (assert (boolean? observed) "A missing observation is not a failed trial")
  (let [[a b] prior]
    (/ (+ a (if observed 1 0)) (+ a b 1))))
(defn run-inventory [f]
  (let [r (read-edn f)
        schemas #{:wm/d-task-token-observations-v2 :wm/token-belief-input-v3 :wm/token-outcome-comparison-v1}]
    {:path (str f) :sha256 (sha f)
     :retained-new-schemas (frequencies (keep #(when (and (map? %) (schemas (:schema %))) (:schema %))
                                            (tree-seq coll? seq r)))}))
(defn trace-inventory [path]
  (with-open [r (java.io.PushbackReader. (io/reader path))]
    (loop [rows []]
      (let [v (edn/read {:eof ::eof} r)]
        (if (= ::eof v)
          {:path path :sha256 (sha path) :forms rows}
          (recur (conj rows {:run (:run/id v)
                            :observation-keys (vec (sort (keys (:observation v))))
                            :accumulation-present? (contains? v :accumulation-state)})))))))
(defn trial [label thetas]
  (let [actions (mapv (fn [candidate theta]
                        (update (:id candidate) :precedence
                                #(mapv (fn [p] (assoc p :theta theta :theta-source :discovery-beta-mean)) %))) candidates thetas)
        ranked (efe/rank-cascade-actions {:cascade-belief q0} actions
                                        {:horizon-steps horizon :cascade-spec spec :adjudication-rates rates})
        rank-g (into {} (map (juxt :action :G-efe)) ranked)
        certs (mapv #(model/horizon-g-sparse-cert
                      {:rates rates :q0 q0 :horizon horizon :spec spec :universe (:universe terminal)
                       :precedence-fn (constantly (:precedence %))}) actions)
        ;; Stable frozen policy identities: only G changes, not habit/F/action projection.
        cs (mapv #(assoc %1 :g (:g %2)) candidates certs)
        posterior (selection/selection-posterior {:beta 1 :candidates cs})
        action-of (into {} (map (fn [c] [(:id c) (#'policy/cascade-first-action (:id c))]) candidates))
        choice (selection/bayes-choice posterior action-of)
        diagnostics (selection/selection-comparisons
                     {:beta 1 :candidates cs :posterior posterior :action-of action-of :choice choice
                      :near-tie-threshold {:status :declared :value 0.01}})]
    (doseq [[a cert] (map vector actions certs)] (close! (:g cert) (get rank-g a)))
    {:label label :theta thetas
     :rows (mapv (fn [candidate cert]
                   (let [steps (get-in cert [:certificate :consumed-g :Q :steps])
                         outputs (into #{} (mapcat :produces) (get-in candidate [:id :precedence]))]
                     {:target (get-in candidate [:id :target]) :g (:g cert)
                      :posterior (get posterior (:id candidate))
                      :steps (mapv (fn [{:keys [tau belief]}]
                                     {:tau tau :entropy (entropy belief)
                                      :output-marginals (into {} (for [token outputs]
                                                                  [token (reduce + 0 (for [[s p] belief :when (contains? s token)] p))]))}) steps)
                      :risk-and-ambiguity (mapv #(select-keys % [:tau :risk :ambiguity]) (get-in cert [:certificate :steps]))})) candidates certs)
     :choice choice
     :policy-comparison (select-keys (:policy-comparison diagnostics) [:contributions :decided-by :near-tie?])
     :action-comparison (:action-comparison diagnostics)}))

(let [inv (mapv inventory (mapcat #(edn-files (str root "/" % "/data/wm-d-task-enactment")) ["futon2" "futon3c"]))
      observed (->> inv
                    (filter #(= "2026-09-21-1789964661" (:run %)))
                    first :selected-effect-observations first :observed)
      _ (assert (false? observed) "Reference selected effect must be observed false")
      ;; Sensitivity only: the real row is held, not admitted as a per-step B trial.
      trials (mapv (fn [[label theta]] (trial label theta))
                   [[:recorded [1 1 1]]
                    [:declared-beta-9-1-prior [9/10 9/10 9/10]]
                    [:one-hypothetical-aif-failure [(posterior-mean observed) 9/10 9/10]]
                    [:one-hypothetical-aif-success [(posterior-mean (not observed)) 9/10 9/10]]
                    [:only-aif-switched-prior [9/10 1 1]]
                    [:only-aif-switched-failure [9/11 1 1]]])]
  (doseq [[candidate row] (map vector candidates (:rows (first trials)))]
    (close! (:g candidate) (:g row))
    (close! (get-in record [:decision :selection-law :posterior (:id candidate)]) (:posterior row)))
  (assert (not= (get-in trials [1 :rows 0 :steps]) (get-in trials [2 :rows 0 :steps])))
  (assert (not= (get-in trials [2 :choice :action]) (get-in trials [3 :choice :action])))
  (assert (= #{:G} (get-in trials [2 :action-comparison :decided-by])))
  (pp/pprint {:reference-sha256 (sha reference-file) :horizon horizon
              :inventory inv :run-inventory (mapv run-inventory (edn-files (str root "/futon2/data/wm-runs")))
              :trace-inventory (trace-inventory (str root "/futon2/data/wm-trace/wm-trace-2026-09-21.edn"))
              :declared-counterfactual-prior prior :trials trials
              :validation :recorded-G-and-posteriors-match-and-record-change-alters-Q-and-G-flip-witness}))
(shutdown-agents)
