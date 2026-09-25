;; H-VALUE-G-D reproduction (PROOF-2 register row AR-40). DISCOVERY ONLY:
;; read-only, no clicks, no writes outside a private temp store, no load-file
;; into a shared JVM.
;;
;;   cd /home/joe/code/futon2
;;   clojure -M holes/labs/wm-contract/proof2/packets/h-value-g-d-repro.clj
;;
;; Reproduces AR-40's four G pairs, decomposes each into the terms the
;; implementation actually has, and checks the closed form those terms obey.
;;
;; The G under test is the lane's own:
;;   futon2.report.war-machine/constructed-candidate-g   (scripts/futon2/report/war_machine.clj:6041)
;; which the report passes to the constructor as :evaluate-g (same file, line
;; 7227). It scores one candidate by running R1->R6->R13->R4->R5 (cascade-lane
;; {:through :R5}) over a family it builds itself, and reading that candidate's
;; :G-efe. For cascade candidates R5 is efe/rank-cascade-actions (efe.clj:1006),
;; whose G is cascade-model-manifest/horizon-g-sparse (cascade_model_manifest.clj:991)
;; -- NOT compute-efe. So the :G-risk/:G-ambiguity/:homeostatic-pressure keys of
;; compute-efe are absent here; the terms are horizon-g-sparse's per-step risk
;; and ambiguity, and at the default all-zero adjudication rates the ambiguity
;; term is identically 0 (docstring: "at zero adjudication rates A is the
;; identity kernel ... so Q(o_t|pi) = q_t and the ambiguity term is identically 0").
;;
;; Every problem scored here is built by the real assembly
;; (assemble-cascade-problems-with-published), so facts, repository, c-schedule,
;; cascade-spec, preference scales and beta are the lane's (cascade_problems.clj:154-168).
(require '[futon2.report.war-machine :as wm]
         '[futon2.aif.interpretation-construction :as ic]
         '[futon2.aif.cascade-model-manifest :as cascade-manifest]
         '[futon2.aif.efe]
         '[clojure.set :as set])
(import '[java.nio.file Files] '[java.nio.file.attribute FileAttribute])

;; private in efe; read here to build the SAME universe rank-cascade-actions
;; builds (efe.clj:1093-1096), so the decomposition is of the same computation.
(def candidate-tokens @#'futon2.aif.efe/cascade-candidate-tokens)

(def target "M-chain")

;; D16's 4-step chain, verbatim from test/futon2/report/war_machine_horizon_test.clj:14-18
(def patterns
  {:chain/one   {:guard {:needs #{:s0} :forbids #{:t1}} :produces #{:t1}}
   :chain/two   {:guard {:needs #{:t1} :forbids #{:t2}} :produces #{:t2}}
   :chain/three {:guard {:needs #{:t2} :forbids #{:t3}} :produces #{:t3}}
   :chain/four  {:guard {:needs #{:t3} :forbids #{:t4}} :produces #{:t4}}})

(def chain [:chain/one :chain/two :chain/three :chain/four])
(def tokens [:s0 :t1 :t2 :t3 :t4])
(def T 4)

(defn- empty-store []
  (.getCanonicalPath (.toFile (Files/createTempDirectory "h-value-g-d" (make-array FileAttribute 0)))))

(defn- sources
  "The D16 sources with one variation: WANTS.

  DECLARED, when given, declares that candidate order in the sources instead of
  letting the constructor build it. It carries a :construction-receipt because
  cascade_problems.clj:53-63 admits only orders that have one. This is used for
  the row whose plan the constructor REFUSES to build (row C, the point at
  issue): the plan still needs a problem to be scored on. G never reads the
  receipt -- constructed-candidate-g takes only :precedence -- so this does not
  affect any number below."
  [wants declared]
  (cond-> {:universes {target (into {:s0 true} (for [t (rest tokens)] [t false]))}
           :wants {target (vec wants)}
           :horizon-steps T
           :locators {target (into {} (for [t tokens]
                                        [t {:class :C4 :repo "futon2" :sha "HEAD" :path "x.md"
                                            :decl (str "- [x] " (name t))}]))}
           :interpretations {target {:patterns patterns
                                     :receipts (into {} (for [k (keys patterns)] [k {:source :test}]))}}
           :beta-by-context {:WM {:beta 1}} :context-of (constantly :WM)}
    declared
    (assoc :candidates
           {target [{:precedence declared
                     :construction-receipt {:kind :declared-for-h-value-g-d-repro
                                            :note "declared so the plan has a problem to be scored on"}}]})
    (not declared)
    (assoc :construction {:construct ic/construct
                          :budget {:max-moves 4 :max-expansions 20000}
                          :move-cost (:value wm/construction-move-cost)
                          :evaluate-g wm/constructed-candidate-g})))

(defn- assembled
  [wants declared]
  (let [r (wm/assemble-cascade-problems-with-published
           (empty-store) {:targets [target] :sources (sources wants declared)})]
    {:refusals (:refusals r)
     :problem (some-> (first (:problems r)) :cascade-problem (dissoc :precedences))}))

(defn- family
  "constructed-candidate-g's family, replicated from war_machine.clj:6056-6060:
  the single-pattern order of every interpretation ENABLED on the problem's true
  facts, plus the candidate's own order. R6 adds the empty cascade C0 itself.
  NOTE what this means: the family -- and therefore the token universe G is
  normalised over -- DEPENDS ON THE CANDIDATE BEING SCORED."
  [problem prec]
  (let [true-facts (set (for [[t v] (:facts problem) :when (true? v)] t))
        enabled? (fn [[_ {:keys [guard]}]]
                   (and (every? true-facts (:needs guard))
                        (not-any? true-facts (:forbids guard))))
        singles (mapv (comp vector key)
                      (sort-by (comp pr-str key) (filter enabled? (:interpretations problem))))]
    (vec (distinct (cond-> singles (seq prec) (conj (vec prec)))))))

(defn- spec-of
  "The preference spec rank-cascade-actions builds from the problem's
  :cascade-spec (efe.clj:1071-1091), replicated so the decomposition scores what
  the lane scored."
  [problem]
  (let [s (:cascade-spec problem)]
    (cond-> {:want (:want s) :evidence (or (:evidence s) #{})
             :lam (or (:lam s) 1) :mu (or (:mu s) 1)
             :zeroed (or (:zeroed s) #{})
             :c (or (:c s) {:status :uniform-declared-constant})}
      (:weights s) (assoc :weights (:weights s))
      (contains? s :c-schedule) (assoc :c-schedule (:c-schedule s)))))

(defn- lane-scoring
  "Run the lane at :through :R5 over the family G uses for PREC, and return both
  the lane's G per candidate and the per-step certificate for each, computed with
  the same q0/universe/rates/spec the lane used. The certificate's :g is checked
  against the lane's :G-efe, so a decomposition is only reported when it is of
  the identical computation."
  [problem prec]
  (let [fam (family problem prec)
        lane (wm/cascade-lane (assoc problem :precedences fam) {:through :R5})]
    (if (:refusal lane)
      {:refusal (:refusal lane) :stopped-at (:stopped-at lane)}
      (let [cands (:candidates lane)
            q0 (cascade-manifest/observed-belief
                (set (for [[t v] (:facts problem) :when (true? v)] t)))
            want (set (:want problem))
            universe (-> (reduce set/union #{} (map #(candidate-tokens (:precedence %)) cands))
                         (into (reduce set/union #{} (keys q0)))
                         (into want))
            rates (zipmap universe (repeat {:false-neg 0 :false-pos 0}))
            spec (spec-of problem)
            id->prec (into {} (map (fn [c] [(:id c) (mapv :id (:precedence c))]) cands))
            g-by-id (into {} (map (fn [e] [(:cascade-id e) (:G-efe e)]) (:ranked lane)))]
        {:universe universe
         :rows (for [c cands
                     :let [{:keys [g certificate]}
                           (cascade-manifest/horizon-g-sparse-cert
                            {:rates rates :q0 q0
                             :precedence-fn (constantly (:precedence c))
                             :horizon T :spec spec :zeta 1 :universe universe})
                           ;; per-step risk/ambiguity records (cascade_model_manifest.clj:1096-1112)
                           steps (:steps certificate)]]
                 {:precedence (get id->prec (:id c))
                  :lane-g (get g-by-id (:id c))
                  :cert-g g
                  :matches? (and (number? g) (number? (get g-by-id (:id c)))
                                 (< (abs (- (double g) (double (get g-by-id (:id c))))) 1e-9))
                  :steps (mapv (fn [s] (select-keys s [:tau :risk :ambiguity :ambiguity-status])) steps)})}))))

;; --- the closed form the numbers obey -------------------------------------
;; With all-zero rates the ambiguity term is identically 0, and the rollout
;; belief of a firing precedence is a point mass, so
;;   risk_tau = -ln c(S_tau) = ln Z - u(S_tau)
;;   G        = sum_{tau=1..T} (ln Z - u(S_tau)) = T*ln Z - sum_tau u(S_tau)
;; with (cascade_model_manifest.clj:588-606, 608-656)
;;   w_v  = lam/|want| for v in want, + mu for v in evidence, 0 otherwise
;;   u(o) = sum_{v in o cap V} w_v
;;   ln Z = sum_{v in V} ln(1 + e^{w_v})
;; V is the COMMON UNIVERSE of the compared family. Every token in V adds
;; ln(1+e^{w_v}) to ln Z; a token that is not wanted has w_v = 0 and so adds
;; exactly ln 2 -- T*ln 2 to every candidate's G in that family.
(defn- ln-Z [want universe lam]
  (let [per (if (seq want) (/ (double lam) (count want)) 0.0)]
    (reduce + 0.0 (map (fn [v] (Math/log1p (Math/exp (if (contains? want v) per 0.0)))) universe))))

(defn- u-of [want state lam]
  (let [per (if (seq want) (/ (double lam) (count want)) 0.0)]
    (reduce + 0.0 (map (fn [v] (if (contains? want v) per 0.0)) state))))

(defn- states-of
  "The point-mass trajectory of PREC from the true facts: each pattern fires in
  order, adding its :produces; once the plan is exhausted the state is held."
  [problem prec]
  (let [start (set (for [[t v] (:facts problem) :when (true? v)] t))]
    (loop [tau 1 s start acc []]
      (if (> tau T) acc
          (let [pat (get patterns (nth prec (dec tau) nil))
                s' (if pat (set/union s (:produces pat)) s)]
            (recur (inc tau) s' (conj acc s')))))))

(defn- closed-form [problem prec universe]
  (let [want (set (:want problem))
        lam (or (:lam (:cascade-spec problem)) 1)
        z (ln-Z want universe lam)
        ss (states-of problem prec)]
    {:ln-Z z :states ss
     :g (- (* T z) (reduce + 0.0 (map #(u-of want % lam) ss)))}))

(defn- fmt [x] (if (number? x) (format "%.4f" (double x)) (pr-str x)))

(defn- row [label wants plan declared expected]
  (println (str "\n=== " label))
  (println "    wants" (pr-str wants) " plan" (pr-str plan) " AR-40 expects:" expected)
  (let [{:keys [refusals problem]} (assembled wants declared)]
    (doseq [rf refusals]
      (println "    assembly refusal:" (:kind rf)
               "| constructor stop-reason"
               (pr-str (get-in rf [:constructor-refusal :construction-receipt :stop-reason]))
               "| g-of-best"
               (fmt (get-in rf [:constructor-refusal :construction-receipt :g-of-best]))
               "| final-evaluation"
               (pr-str (get-in rf [:constructor-refusal :construction-receipt :coverage :final-evaluation]))))
    (when problem
      ;; The two numbers the CONSTRUCTOR compares: two separate evaluate-g calls.
      (let [g-plan (wm/constructed-candidate-g problem {:precedence plan})
            g-empty (wm/constructed-candidate-g problem {:precedence []})]
        (println (format "    G(plan) %s   G(empty) %s   plan taken? %s"
                         (fmt g-plan) (fmt g-empty) (< g-plan g-empty)))
        (doseq [[what prec] [["plan " plan] ["empty" []]]]
          (let [sc (lane-scoring problem prec)]
            (if (:refusal sc)
              (println "    " what "decomposition refused:" (pr-str (:refusal sc)))
              (let [cf (closed-form problem prec (:universe sc))
                    me (first (filter #(= (vec prec) (:precedence %)) (:rows sc)))]
                (println (format "    %s call: universe %s (|V|=%d), lnZ %s"
                                 what (pr-str (sort (:universe sc))) (count (:universe sc)) (fmt (:ln-Z cf))))
                (println (format "         per-step risk %s  sum %s"
                                 (pr-str (mapv #(fmt (:risk %)) (:steps me)))
                                 (fmt (reduce + 0.0 (map #(double (or (:risk %) 0)) (:steps me))))))
                (println (format "         per-step ambiguity %s  status %s"
                                 (pr-str (mapv #(fmt (:ambiguity %)) (:steps me)))
                                 (pr-str (distinct (map :ambiguity-status (:steps me))))))
                (println (format "         lane G %s  cert G %s  same-computation? %s  closed form T*lnZ-sum u(S) %s"
                                 (fmt (:lane-g me)) (fmt (:cert-g me)) (:matches? me) (fmt (:g cf))))))))
        {:label label :g-plan g-plan :g-empty g-empty}))))

(println "H-VALUE-G-D — AR-40 reproduced against the lane's real G")
(println "lower is better; the constructor takes a plan only if G(plan) < G(empty)")

(def results
  (doall (remove nil?
   [(row "A  want t1, plan = 1 step, 0 unwanted intermediates" [:t1] [:chain/one] nil "4.03 vs 8.03 (taken)")
    (row "B  want t2, plan = 2 steps, 1 unwanted intermediate (t1)" [:t2] [:chain/one :chain/two] nil "7.80 vs 10.80 (taken)")
    (row "C  want t4, plan = 4 steps, 3 unwanted intermediates (t1,t2,t3)" [:t4] chain chain "15.34 vs 10.80 (NOT taken)")
    (row "D  want t1..t4, plan = 4 steps, every intermediate wanted" [:t1 :t2 :t3 :t4] chain nil "13.49 vs 15.99 (taken)")])))

;; --- the cross-universe comparison, which is the finding -------------------
(println "\n=== row C: the two G values the constructor compares are taken over DIFFERENT universes")
(let [{:keys [problem]} (assembled [:t4] chain)
      sc-plan (lane-scoring problem chain)
      sc-empty (lane-scoring problem [])
      v-plan (:universe sc-plan)
      v-empty (:universe sc-empty)
      empty-in-plan-family (first (filter #(= [] (:precedence %)) (:rows sc-plan)))
      empty-in-own-family (first (filter #(= [] (:precedence %)) (:rows sc-empty)))
      plan-row (first (filter #(= chain (:precedence %)) (:rows sc-plan)))
      k (count (set/difference v-plan v-empty))]
  (println "  universe when scoring the PLAN :" (pr-str (sort v-plan)))
  (println "  universe when scoring EMPTY   :" (pr-str (sort v-empty)))
  (println "  tokens only in the plan's universe:" (pr-str (sort (set/difference v-plan v-empty))) " k =" k)
  (println (format "  G(empty) in its own (smaller) family : %s   <- the number the constructor compares against"
                   (fmt (:lane-g empty-in-own-family))))
  (println (format "  G(empty) in the PLAN's family        : %s   <- the comparable number"
                   (fmt (:lane-g empty-in-plan-family))))
  (println (format "  difference                           : %s   predicted T*k*ln2 = %s"
                   (fmt (- (:lane-g empty-in-plan-family) (:lane-g empty-in-own-family)))
                   (fmt (* T k (Math/log 2)))))
  (println (format "  G(plan) %s vs G(empty) %s IN ONE UNIVERSE -> plan preferred? %s  (by %s = the final want's lam)"
                   (fmt (:lane-g plan-row)) (fmt (:lane-g empty-in-plan-family))
                   (< (:lane-g plan-row) (:lane-g empty-in-plan-family))
                   (fmt (- (:lane-g empty-in-plan-family) (:lane-g plan-row))))))

;; --- the bad case that must still be declined -----------------------------
;; In one common universe the chain's whole improvement is the final want's
;; utility, lam (t4 held for one step at tau=4): 1.0 here. A move cost c > 0
;; makes a k-move plan cost k*c, so the plan must be declined when k*c >= lam.
;; The lane's move cost is currently 0 (war_machine.clj construction-move-cost),
;; so nothing is declined on cost today; this is the arithmetic the fix has to
;; keep, not a run of it.
(println "\n=== refutation check: is an unwanted token PENALISED as a deviation from preference?")
;; AR-40's candidate cause says the risk term treats instrumental tokens as
;; deviations from preference. Then two predicted states differing ONLY in
;; unwanted tokens would have different preference, hence different risk.
;; utility-weights (cascade_model_manifest.clj:588-606) gives a token outside
;; want and evidence weight 0, so u(o) cannot see it. Checked directly:
(let [want #{:t4}
      spec {:want want :evidence #{} :lam 1 :mu 1 :zeroed #{}}
      universe #{:s0 :t1 :t2 :t3 :t4}
      log-c (cascade-manifest/log-preference-fn spec universe)
      bare #{:s0 :t4}
      with-intermediates #{:s0 :t1 :t2 :t3 :t4}]
  (println (format "  ln c(%s) = %s" (pr-str (sort bare)) (fmt (log-c bare))))
  (println (format "  ln c(%s) = %s" (pr-str (sort with-intermediates)) (fmt (log-c with-intermediates))))
  (println (format "  equal? %s -> three unwanted tokens change the preference of the state by %s"
                   (< (abs (- (double (log-c bare)) (double (log-c with-intermediates)))) 1e-12)
                   (fmt (- (double (log-c with-intermediates)) (double (log-c bare))))))
  (println "  => no per-token deviation penalty exists; what changes G is ln Z over the UNIVERSE,")
  (println "     and every token in the universe (wanted or not) adds ln(1+e^{w_v}) to it at every step."))

(println "\n=== bad case arithmetic (must still be declined after any fix)")
(let [{:keys [problem]} (assembled [:t4] chain)
      sc (lane-scoring problem chain)
      plan-g (:lane-g (first (filter #(= chain (:precedence %)) (:rows sc))))
      empty-g (:lane-g (first (filter #(= [] (:precedence %)) (:rows sc))))
      improvement (- empty-g plan-g)]
  (println (format "  improvement in one universe = %s ; moves = %d ; current move-cost = %s (authority %s)"
                   (fmt improvement) (count chain) (pr-str (:value wm/construction-move-cost))
                   (pr-str (:ruling (:authority wm/construction-move-cost)))))
  (doseq [c [0 0.2 0.25 0.5]]
    (println (format "    move-cost %.2f -> total cost %.2f %s improvement %.4f -> %s"
                     (double c) (* (count chain) (double c))
                     (if (>= (* (count chain) (double c)) improvement) ">=" "<")
                     improvement
                     (if (>= (* (count chain) (double c)) improvement) "DECLINED" "taken")))))

(println "\n=== what the fix would do, and the bad case under it, scored in ONE common universe")
;; The fix: give every evaluate-g call of one problem the SAME universe, namely
;; cascade-problems/problem-tokens (facts + want + every interpreted pattern's
;; guard and produces) -- candidate-independent by construction. Scored here by
;; putting every candidate in ONE family, which is what one common universe means.
;; Three candidates: the chain (reaches the want t4), a 3-step prefix (never
;; reaches it), and the empty cascade.
(let [{:keys [problem]} (assembled [:t4] chain)
      prefix [:chain/one :chain/two :chain/three]
      fam [[:chain/one] prefix chain]
      lane (wm/cascade-lane (assoc problem :precedences fam) {:through :R5})
      cands (:candidates lane)
      id->prec (into {} (map (fn [c] [(:id c) (mapv :id (:precedence c))]) cands))
      g-of (fn [prec] (some (fn [e] (when (= prec (get id->prec (:cascade-id e))) (:G-efe e))) (:ranked lane)))
      g-empty (g-of []) g-chain (g-of chain) g-prefix (g-of prefix)]
  (println "  one universe for the whole family, G from a single lane run:")
  (println (format "    G(empty)                      = %s" (fmt g-empty)))
  (println (format "    G(chain, reaches t4)          = %s  improvement %s -> %s"
                   (fmt g-chain) (fmt (- g-empty g-chain))
                   (if (< g-chain g-empty) "TAKEN (the AR-40 case, now taken)" "declined")))
  (println (format "    G(3-step prefix, never reaches t4) = %s  improvement %s -> %s"
                   (fmt g-prefix) (fmt (- g-empty g-prefix))
                   (if (< g-prefix g-empty) "taken" "DECLINED (bad case: no want reached, no credit)")))
  (println "  so under the fix the want-reaching chain is preferred by exactly lam, and a chain")
  (println "  that reaches no want has improvement 0 and is declined at any move cost >= 0.")
  (println "  A want-reaching chain whose COST exceeds lam needs a positive move cost to be")
  (println "  declined: at lam 1 and 4 moves that is move-cost >= 0.25 (see the arithmetic above).")
  (println (format "  current declared move cost is %s, so on cost alone nothing is declined today."
                   (pr-str (:value wm/construction-move-cost)))))

(println "\n=== summary")
(doseq [{:keys [label g-plan g-empty]} results]
  (println (format "  %-64s G(plan) %8s  G(empty) %8s  taken %s"
                   (subs label 0 (min 64 (count label))) (fmt g-plan) (fmt g-empty) (< g-plan g-empty))))
