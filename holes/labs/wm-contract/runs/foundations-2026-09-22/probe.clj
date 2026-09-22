(ns probe
  "Read-only selection witnesses. Expected failures are data, never suite tests.
   Run from repo root: clojure -M:test holes/labs/wm-contract/runs/foundations-2026-09-22/probe.clj"
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.attempt-learning :as attempt]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.cascade-sources :as sources]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.token-initialization-policy :as initialization]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.cascade-habit-reinforcement :as reinforcement]
            [futon2.aif.cascade-prior :as prior]
            [futon2.aif.efe :as efe]
            [futon2.aif.parameter-novelty :as novelty]
            [futon2.aif.policy :as policy]
            [futon2.aif.ticket-queue :as queue]
            [futon2.aif.token-belief-predecessor :as predecessor]
            [futon2.report.war-machine :as wm]))

(def root (or (System/getenv "FUTON2_RECORD_ROOT") "/home/joe/code/futon2"))
(defn record [run]
  (edn/read-string (slurp (str root "/data/wm-runs/tick-run-record-" run ".edn"))))
(def click1 (record "2026-09-21-1790033693"))
(def click2 (record "2026-09-22-1790037762"))
(def decision (:decision click1))
(def candidates (mapv :id (get-in decision [:selection-certificate :candidates])))
(def model (get-in decision [:selection-certificate :precision-family :model]))

(defn assembled-from-record [r]
  (let [d (:decision r)
        cs (mapv :id (get-in d [:selection-certificate :candidates]))
        inputs (get-in d [:selection-certificate :token-belief-stage :domain-inputs])
        preference (get-in d [:selection-certificate :precision-family :model :preference-spec])
        sources
        (reduce (fn [s {:keys [target declaration]}]
                  (let [actions (filter #(= target (:target %)) cs)]
                    (-> s
                        (assoc-in [:universes target] (:facts declaration))
                        (assoc-in [:wants target] (:want declaration))
                        (assoc-in [:interpretations target]
                                  {:patterns (:interpretations declaration)
                                   :receipts (:interpretation-receipts (first actions))})
                        (assoc-in [:locators target]
                                  (into {} (map (fn [[[_ token] locator]] [token locator]))
                                        (:observation-locators (first actions))))
                        (assoc-in [:candidates target]
                                  (mapv #(hash-map :precedence (mapv :id (:precedence %))
                                                  :construction-receipt (:construction-receipt %)) actions))
                        (assoc-in [:preference-scales target]
                                  {:lam {:value (:lam preference) :status :declared}
                                   :mu {:value (:mu preference) :status :declared}})
                        (assoc-in [:preference-schedules target] (:c-schedule preference)))))
                {:horizon-steps (get-in d [:selection-certificate :precision-family :model :horizon])
                 :context-of (constantly :probe) :beta-by-context {:probe {:beta 1}}} inputs)]
    (cp/assemble {:targets (mapv :target inputs) :sources sources})))

(def assembled (assembled-from-record click1))
(def live-c
  (let [weights (:weights (:preference-spec model))]
    {:want (set (map #(keyword "alive" (str (:target %))) candidates))
     :weights (into {} (for [a candidates]
                         [(keyword "alive" (str (:target a)))
                          (reduce + 0 (for [[[target _] w] weights :when (= target (:target a))] w))]))
     :lam 1 :entries [] :gaps [] :refusals nil :signature "retained-click1-projected-masses"}))
(def empty-inputs {:contract (attempt/declared-contract)
                   :prior {:schema :wm/learning-trial-prior-v1 :authority :illustrative
                           :mode :record-only :alpha 9 :beta 1}
                   :ledger {:status :present :sha256 "in-memory-empty" :records []}})
(def opts {:live-c {:derived live-c} :ticket-queue queue/empty-declaration
           :novelty-inputs empty-inputs})
(defn snapshot [state] {:state state :receipt {:status :present :path "in-memory://probe" :state state}})
(def ^:dynamic *execution* {:status :refused :kind :probe-no-predecessor})
(def ^:dynamic *observations* nil)
(defn select
  ([a o] (select a o (prior/initial-state)))
  ([a o state]
   ;; Only external reads are replaced. Assembly, admission, ranking, selector,
   ;; habit arithmetic and decision gate are the production functions.
   (let [rank efe/rank-actions seen (atom nil)]
     (with-redefs [habit/read-snapshot (fn [_] (snapshot state))
                   predecessor/production-authority (fn [_] *execution*)
                   predecessor/observation-authority (fn [_] *observations*)
                   efe/rank-actions (fn [s actions options]
                                      (let [r (rank s actions options)]
                                        (reset! seen {:state s :actions actions :options options :ranked r}) r))]
       (assoc (wm/cascade-decision a o) :scorer @seen)))))
(defn posterior [r] (get-in r [:decision :selection-law :posterior]))
(defn choice [r] (select-keys (get-in r [:decision :action]) [:target :id]))
(defn scores [r] (mapv :g (get-in r [:decision :selection-certificate :candidates])))
(defn delta [a b]
  (reduce max 0.0 (for [[k p] (posterior a)] (abs (- p (get (posterior b) k 0))))))
(defn result [part pass facts] (merge {:part part :result (if pass :PASS :FAIL)} facts))

(def baseline (select assembled opts))
(assert (= 2 (count (posterior baseline))) "Sensitivity fixture must have two admitted real candidates")
(assert (= (sort (scores baseline))
           (sort (map :g (get-in decision [:selection-certificate :candidates]))))
        "Reassembled fixture must reproduce the recorded G values exactly")
(def ranked (get-in baseline [:scorer :ranked]))
(def actual-actions (mapv :action ranked))
(defn inputs-for [entries]
  (assoc empty-inputs :models
         (into {} (for [e entries :let [a (:action e)]]
                    [a {:schema :wm/attempt-endpoint-parameter-model-v1
                        :authority :illustrative :source "foundation witness, not live authority"
                        :route {:author :probe-author :reviewer :probe-reviewer}
                        :meanings (zipmap (mapcat :produces (:precedence a)) (repeat "pinned-probe-meaning"))
                        :observation {:schema :wm/perfect-attempt-endpoint-v1
                                      :placement :post-build-artifact-revision}}]))))
(def inputs (inputs-for ranked))
(defn trial-rows [entry x n-success n-failure]
  (let [family (get-in (novelty/policy-receipt entry x) [:endpoints 0 :family])]
    (assert family "The real scorer must supply an eligible endpoint")
    (mapv (fn [i observed]
            {:schema :wm/attempt-learning-count-v1 :mode :record-only
             :identity (str "probe-trial-" i) :family (identity/digest family)
             :meaning-sha256 "pinned-probe-meaning" :contract (:contract x)
             :observed observed :increment (if observed {:success 1 :failure 0} {:success 0 :failure 1})})
          (range) (concat (repeat n-success true) (repeat n-failure false)))))
(defn with-rows [x rows]
  (assoc x :ledger {:status :present :sha256 (identity/digest rows) :records rows}))

(defn a-witness []
  (let [universe (keys (:rates model))
        rates (zipmap universe (repeat {:false-neg 1/2 :false-pos 1/2}))
        changed (select assembled (assoc opts :adjudication-rates rates))
        {:keys [state actions options]} (:scorer baseline)
        control (efe/rank-actions state actions (assoc options :adjudication-rates rates))
        control-decision (with-redefs [habit/read-snapshot (fn [_] (snapshot (prior/initial-state)))]
                           (policy/select-action-cascades control {:beta 1 :novelty-inputs empty-inputs}))]
    (assert (> (delta baseline {:decision control-decision}) 0.0001)
            "The declared non-identity A must change the real scorer/selector control")
    (result :A (> (delta baseline changed) 0.0001)
            {:required-posterior-delta 0.0001 :actual-delta (delta baseline changed)
             :baseline-posterior (mapv val (posterior baseline))
             :rates-reached-joint-scorer? (contains? (get-in changed [:scorer :options]) :adjudication-rates)
             :scorer-control-posterior-delta (delta baseline {:decision control-decision})
             :scorer-control-G (mapv :controller-score control)
             :scorer-control-posterior (mapv val (get-in control-decision [:selection-law :posterior]))
             :note "Existing scorer option requested at cascade-decision boundary; no forwarding exists."})))

(defn b-witness []
  (let [before (select assembled (assoc opts :novelty-inputs inputs))
        x (with-rows inputs (trial-rows (first ranked) inputs 0 1))
        after (select assembled (assoc opts :novelty-inputs x))
        q #(mapv (fn [entry] (get-in entry [:certificate :consumed-g :Q]))
                 (get-in % [:scorer :ranked]))
        receipts #(get-in % [:decision :selection-certificate :parameter-novelty])]
    (assert (every? some? (q before)) "The comparison must contain actual planned Q receipts")
    (assert (= 2 (get-in (novelty/policy-receipt (first ranked) x) [:endpoints 0 :prior :beta]))
            "The supplied recorded failure must actually be admitted by the count consumer")
    (result :B (and (not= (q before) (q after)) (not= (scores before) (scores after))
                            (not= (choice before) (choice after)))
            {:learning-receipt-changed? (not= (receipts before) (receipts after))
             :planned-Q-changed? (not= (q before) (q after))
             :G-before (scores before) :G-after (scores after)
             :choice-before (choice before) :choice-after (choice after)
             :note "An admitted-shaped count snapshot at the existing ledger read seam; no ledger written."})))

(defn c-witness []
  (let [gs (scores baseline) spread (- (apply max gs) (apply min gs))
        winner (get-in baseline [:decision :action])
        loser (first (remove #(= % winner) actual-actions))
        state (prior/observe-policy (prior/initial-state) (habit/policy-view loser))
        opposed (select assembled opts state)]
    (result :C (and (> spread (Math/log 2)) (= (choice baseline) (choice opposed)))
            {:G-spread spread :habit-log-odds (Math/log 2)
             :uniform-choice (choice baseline) :opposing-habit-choice (choice opposed)
             :declared-classes {:focus 55 :associated 35 :elsewhere 5 :typed-failure 5}
             :consumed-preference-domain :target-qualified-wanted-tokens
             :note "Real click1 candidates with retained projected C; class-domain predictive bridge is absent, not replaced by token weights."})))

(defn g-witness []
  ;; Matched counterfactual policies on one real problem: same produced token,
  ;; guard, horizon, C and habit. Only illustrative parameter precision differs.
  (let [p (first (:problems assembled))
        original (first (get-in p [:constructed-candidates 0 :precedence]))
        twin :foundations/precision-control
        a (assoc assembled :problems
                 [(-> p
                      (assoc-in [:cascade-problem :interpretations twin]
                                (get-in p [:cascade-problem :interpretations original]))
                      (assoc-in [:interpretation-receipts twin] (get-in p [:interpretation-receipts original]))
                      (assoc :constructed-candidates
                             [(assoc (first (:constructed-candidates p)) :candidate-id :C0 :precedence [original])
                              (assoc (first (:constructed-candidates p)) :candidate-id :C1 :precedence [twin])])
                      (assoc-in [:cascade-problem :precedences] [[original] [twin]]))])
        base (select a opts)
        es (get-in base [:scorer :ranked]) x (inputs-for es)
        winning (get-in base [:decision :action])
        experienced (first (filter #(= winning (:action %)) es))
        x (with-rows x (trial-rows experienced x 81 9))
        after (select a (assoc opts :novelty-inputs x))
        rs (get-in after [:decision :selection-certificate :parameter-novelty])]
    (assert (apply = (scores after)) "Matched policies must tie on serving G")
    (assert (= #{9/10} (set (map #(let [{:keys [alpha beta]} (get-in % [:endpoints 0 :prior])]
                                  (/ alpha (+ alpha beta))) rs))))
    (assert (= 2 (count (set (map #(get-in % [:expected-kl :nats]) rs))))
            "The matched policies must differ in computed information")
    (result :G (not= (choice base) (choice after))
            {:G (scores after) :before (choice base) :after (choice after)
             :information-nats (mapv #(get-in % [:expected-kl :nats]) rs)
             :means (mapv #(let [{:keys [alpha beta]} (get-in % [:endpoints 0 :prior])] (/ alpha (+ alpha beta))) rs)
             :consumed? (mapv #(get-in % [:terms :novelty-consumed-in-G?]) rs)
             :note "Synthetic matched policy pair, not a claim that this twin is admitted live."})))

(defn e-witness []
  (let [loser (first (remove #(= % (get-in baseline [:decision :action])) actual-actions))
        token (first (mapcat :produces (:precedence loser)))
        comparison {:schema :wm/token-outcome-comparison-v1 :status :compared
                    :prediction {:status :frozen :target (:target loser) :action loser
                                 :wanted [{:token token :predicted 1}]}
                    :tokens [{:token token :predicted 1 :observed true :verdict :predicted-and-observed}]}
        rule (reinforcement/evaluate {:action loser} :grounded-change comparison)
        n 3
        state (reduce (fn [s _] (if (= :increment (:reinforcement rule))
                                  (prior/observe-policy s (habit/policy-view loser)) s))
                      (prior/initial-state) (range n))
        after (select assembled opts state)]
    (result :E (and (= :increment (:reinforcement rule)) (not= (choice baseline) (choice after)))
            {:N n :rule (:rule/id rule) :before (choice baseline) :after (choice after)
             :habit-ratio 4 :scope "Outcome rule + pure accumulation + production snapshot consumer; persistence not exercised."})))

(defn update-witness []
  ;; In-memory output of the independently verified close-observation read port.
  ;; This tests consumption and its guards, NOT signature issuance or close IO.
  (let [p (first (:problems assembled)) target (:target p)
        token (first (get-in p [:cascade-problem :want])) qualified [target token]
        locator (get-in p [:cascade-problem :locators token])
        revision (apply str (repeat 40 "a"))
        declaration-sha "fixture-declaration" schedule {:placement :next-selection}
        meaning {:token qualified :declaration-sha256 declaration-sha :locator locator}
        check {:check (:class locator) :observed true :evidence {:resolved-sha revision}}
        observed {:observed true :artifact-sha revision :evidence-sha256 (evidence/value-digest check)
                  :measurement {:token qualified :declaration-sha256 declaration-sha
                                :declared-locator locator :result check}}
        row {:meaning meaning :meaning-sha256 (evidence/value-digest meaning)
             :schedule schedule :schedule-sha256 (evidence/value-digest schedule)
             :artifact-observation observed}
        context {:policy (assoc initialization/disabled :enabled true)
                 :declaration-sha256 declaration-sha :locators {token locator}
                 :schedule schedule :observations {token {:status :missing :kind :probe-reader-unavailable}}}
        a (assoc-in assembled [:problems 0 :cascade-problem :token-initialization] context)
        first-tick (select a opts)
        carry (get-in first-tick [:decision :selection-certificate :token-belief-stage :prospective-carry])
        carry (assoc carry :occurrence-id "probe-closed-attempt")
        expected {:occurrence {:id "probe-occurrence"} :carry-occurrence-id (:occurrence-id carry)
                  :universe (:universe carry)}
        execution {:authority task/authority :scope task/scope :status :admitted
                   :occurrence (:occurrence expected) :carry-occurrence-id (:occurrence-id carry)
                   :candidate-to-minted-join :not-established :enactment-grain :task
                   :b-authority :declared-kernel-of-verified-macro-action
                   :record-sha256 "fixture-record" :source {:sha256 "fixture-source"}}
        signed {:schema :wm/d-task-token-observations-v2 :authority task/observation-authority
                :scope task/observation-scope :status :admitted
                :execution-verification execution :source (:source execution)
                :occurrence (:occurrence expected) :carry-occurrence-id (:occurrence-id carry)
                :consumption :not-authorized :causal-attribution :independent-check-required
                :universe (:universe carry) :observations {qualified row}}
        next-opts (assoc opts :prospective-token-carry carry
                             :token-belief-predecessor-trace {:d-task-context expected})
        run-next (fn [a] (binding [*execution* execution *observations* signed]
                           (select a next-opts)))
        off (run-next (assoc-in a [:problems 0 :cascade-problem :token-initialization :policy :enabled] false))
        on (run-next a)
        q0 #(get-in % [:decision :selection-certificate :precision-family :model :q0])
        input (get-in on [:decision :selection-certificate :token-belief-input])
        tampered (binding [*execution* execution
                          *observations* (assoc-in signed [:observations qualified :meaning-sha256] "wrong")]
                   (select a next-opts))]
    (result :update-at-close (and (not= (q0 off) (q0 on))
                                 (= (q0 off) (q0 tampered))
                                 (= :observed-initialization (:conditioning-status input)))
            {:switch-off-input-changed? (not= (q0 first-tick) (q0 off))
             :switch-on-input-changed? (not= (q0 off) (q0 on))
             :tampered-meaning-refused? (= (q0 off) (q0 tampered))
             :conditioning-status (:conditioning-status input)
             :updated-tokens (mapv #(select-keys % [:token :status :kind :observed]) (:observation-updates input))
             :scope "Fixture at verified-close observation read port; no closed attempt created or independently reverified."})))

(defn declared-source-census []
  (try
    (let [s (sources/with-context-fn (sources/load-declared))
          a (cp/assemble {:targets (vec (sort (keys (:universes s))))
                          :sources (assoc s :horizon-steps 2)})
          admissions (mapv #'wm/admit-cascade-problem (:problems a))]
      {:declaration-files (:files s)
       :declared-targets (count (:universes s))
       :assembled-targets (count (:problems a))
       :admitted-targets (count (keep :problem admissions))
       :admitted-candidates (reduce + (map #(count (get-in % [:problem :constructed-candidates])) admissions))
       :declines (frequencies (map :reason (mapcat :declines admissions)))
       :assembly-refusals (frequencies (map :kind (:refusals a)))
       :enabled-initialization-targets (vec (sort (for [[t c] (:token-initialization s) :when (get-in c [:policy :enabled])] t)))
       :scope "Current declared resources only; excludes generated/proposal/repair targets and ticket eligibility."})
    (catch Exception e {:status :refused :message (.getMessage e) :data (ex-data e)})))

(defn precondition []
  (let [counts (mapv #(count (get-in % [:decision :selection-certificate :candidates])) [click1 click2])]
    (result :admission (every? #(>= % 2) counts)
            {:current-declared-source-census (declared-source-census)
             :run-ids (mapv :run/id [click1 click2]) :scored-candidates counts
             :at-least-two-frequency {:numerator (count (filter #(>= % 2) counts)) :denominator 2}
             :scope "Two retained clicks, not a frequency estimate for all future source states."})))

(defn files []
  (into {} (for [dir [(str root "/data/wm-repair-obligations")
                       (str root "/data/wm-habit") (str root "/data/wm-learning-trials")]
                 f (file-seq (io/file dir)) :when (.isFile f)]
             [(.getPath f) [(.length f) (.lastModified f)]])))
(let [before (files)
      results (mapv (fn [f] (f)) [a-witness b-witness c-witness g-witness e-witness update-witness precondition])]
  (doseq [r results] (prn r))
  (assert (= before (files)) "Probe must not change store files")
  (prn {:read-only-store-check :passed :baseline-choice (choice baseline) :baseline-G (scores baseline)}))
;; Mechanical Git observations use shell/sh futures; this standalone process
;; owns their executor and must release it after the read-only census.
(shutdown-agents)
