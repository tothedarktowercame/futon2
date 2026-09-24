;; I2 — CONSTRUCTOR (E-cascade-real). DISCOVERY ONLY.
;; Run from /home/joe/code/futon2:  clojure -M holes/labs/wm-contract/E-cascade-real/i2_construct.clj
;; Fresh private process; read-only on src/resources/data; no clicks; no shared JVM.
;;
;; Per target: cascade-sources/load-declared → interpretation-construction/construct
;; (budget/horizon/move-cost/evaluate-g as documented below) → the judge's own
;; admission check candidate-want-progress, copied VERBATIM from
;; scripts/futon2/report/war_machine.clj:6549-6586 (the "candidate-want-progress"
;; defn) so this run checks what the judge checks. Verbatim copy begins/ends
;; at the marked comments.
(require '[futon2.aif.cascade-sources :as sources]
         '[futon2.aif.cascade-problems :as problems]
         '[futon2.aif.cascade-policy :as cascade-policy]
         '[futon2.aif.cascade-model-manifest :as cascade-manifest]
         '[futon2.aif.interpretation-construction :as ctor]
         '[clojure.set]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

;;; --- BEGIN verbatim copy: war_machine.clj:6549-6586 (candidate-want-progress)
(defn- candidate-want-progress
  "Use the scorer/constructor's rollout, on this target's fresh true facts.
   The production D initializer currently consumes exactly these facts;
   prospective carry has no consumption authority. Add-only transitions make
   positive terminal probability for an initially absent want a new predicted
   satisfaction. This is a prediction, never an observed discharge."
  [{:keys [facts want interpretations horizon-steps]} precedence]
  (if-not (pos-int? horizon-steps)
    {:reason :candidate-prediction-refused
     :evidence {:horizon horizon-steps
                :refusal {:kind :missing-common-horizon}}}
    (let [initial (set (for [[token value] facts :when (true? value)] token))
          wanted (set want)
          initial-wanted (clojure.set/intersection wanted initial)
          patterns (mapv #(cascade-policy/token-interpretation % (get interpretations %)) precedence)
          terminal (cascade-manifest/rollout
                    (constantly patterns) (cascade-manifest/observed-belief initial) horizon-steps)
          evidence {:horizon horizon-steps :wanted-tokens wanted
                    :initial-facts facts :initial-state initial
                    :initial-wanted-tokens initial-wanted
                    :prediction-source :cascade-model-manifest/rollout
                    :initialization :fresh-target-facts
                    :semantics :positive-terminal-probability-of-initially-absent-want}]
      (if (:status terminal)
        {:reason :candidate-prediction-refused
         :evidence (assoc evidence :refusal terminal)}
        (let [projected (reduce-kv (fn [m state mass]
                                     (if (pos? mass)
                                       (update m (clojure.set/intersection wanted state) (fnil + 0) mass)
                                       m)) {} terminal)
              new-wanted (clojure.set/difference
                          (reduce clojure.set/union #{} (keys projected)) initial-wanted)]
          (when (empty? new-wanted)
            {:reason :no-new-wanted-token
             :evidence (assoc evidence :terminal-wanted-belief projected
                              :new-wanted-tokens new-wanted)}))))))
;;; --- END verbatim copy

(def targets
  ["T-repair-occ-444fb018" ; prefix; resolved against the declared targets below
   "M-wm-08-external-f2"
   "M-f11-find-production-successor"
   "M-aif-policy-conditioned-eig"])

(def declared (sources/load-declared))

(defn resolve-target [prefix]
  (first (filter #(str/starts-with? % prefix) (keys (:wants declared)))))

;; Discovery choices, documented in I2-constructor.md:
;; - :evaluate-g: precedence length, with the EMPTY family pinned worst.
;;   G is injected into the constructor and used only for comparison inside
;;   construction/construct (pragmatic = G(best family) − G(best proposed);
;;   a move is taken only when its value is positive). An earlier run with
;;   plain length as G made M-aif-policy-conditioned-eig refuse
;;   :construction-not-taken — the empty initial family (G 0.0) beat the
;;   constructed one (G 1.0) — which is an artefact of the injected G, not
;;   of the search. The pin below removes that artefact; the judge's own
;;   scorer is still not replayed here, and no conclusion depends on G
;;   beyond the take/no-take sensitivity it exposes.
;; - :move-cost 1; :budget {:max-moves 4 :max-expansions 10000}.
;; - :horizon: the source-declared common horizon (CLICK2-D: 4).
(def horizon (or (:horizon-steps declared) 4))

(doseq [prefix targets]
  (let [t (resolve-target prefix)
        want (get-in declared [:wants t])
        universe (get-in declared [:universes t])
        patterns (get-in declared [:interpretations t :patterns])
        receipts (get-in declared [:interpretations t :receipts])
        observation universe]
    (println "================================================================")
    (println "TARGET" t)
    (pp/pprint {:want want :facts universe :horizon horizon
                :patterns (set (keys patterns))})
    (let [result (try
                   (ctor/construct
                    {:target t
                     :want want
                     :observation observation
                     :interpretations patterns
                     :interpretation-receipts receipts
                     :horizon horizon
                     :move-cost 1
                     :budget {:max-moves 4 :max-expansions 10000}
                     :evaluate-g (fn [c] (if (empty? (:precedence c))
                                           1.0e9
                                           (double (count (:precedence c)))))})
                   (catch Throwable e
                     {:status :threw :message (.getMessage e)
                      :data (ex-data e)}))]
      (if (= :constructed (:status result))
        (do
          (println "CONSTRUCTED" (count (:candidates result)) "candidates")
          (doseq [c (:candidates result)]
            (pp/pprint {:precedence (:precedence c)
                        :need-edges (:need-edges c)
                        :receipt-kind (get-in c [:construction-receipt :kind])})
            (let [admission (candidate-want-progress
                             {:facts universe :want want
                              :interpretations patterns :horizon-steps horizon}
                             (:precedence c))]
              (pp/pprint {:admission (or admission :passes-no-new-wanted-token-check)
                          :reason (:reason admission)})))
          (when (seq (:findings result))
            (println "findings:") (pp/pprint (:findings result))))
        (pp/pprint {:constructor-result (select-keys result [:status :kind :tokens :patterns :expanded :findings :message])})))))

(println "================================================================")
(println "SUBSTRATE TARGETS vs DECLARED INTERPRETATIONS")
(let [substrate (problems/substrate-targets)
        with-interpretations (set (keys (:interpretations declared)))]
  (pp/pprint {:n-substrate-targets (count substrate)
              :substrate-targets substrate
              :with-declared-interpretations with-interpretations
              :covered (clojure.set/intersection (set substrate) with-interpretations)}))
(shutdown-agents)
