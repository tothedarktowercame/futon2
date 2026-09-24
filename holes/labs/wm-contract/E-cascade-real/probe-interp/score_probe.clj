;; P1 scoring — interpretation probe (E-cascade-real D11). Adapted from i2_construct.clj.
;; Run from /home/joe/code/futon2:  clojure -M holes/labs/wm-contract/E-cascade-real/probe-interp/score_probe.clj
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

;; Same discovery choices as I2 (see i2_construct.clj): G = precedence length
;; with the EMPTY family pinned worst; move-cost 1; budget 4 moves / 10000
;; expansions; horizon = the file's declared :horizon-steps, else 4. G decides
;; only take/no-take (E-cascade-real D12); the columns below separate that.
(require '[clojure.edn :as edn])
(def inputs (edn/read-string (slurp "holes/labs/wm-contract/E-cascade-real/probe-interp/inputs.edn")))
(def seats ["seat-A" "seat-B"])

(defn score-target [declared t]
  (let [want (get-in declared [:wants t])
        universe (get-in declared [:universes t])
        patterns (get-in declared [:interpretations t :patterns])
        receipts (get-in declared [:interpretations t :receipts])
        horizon (or (:horizon-steps declared) 4)
        produced (reduce clojure.set/union #{} (map :produces (vals patterns)))
        covered (clojure.set/intersection (set want) produced)]
    (if (empty? patterns)
      {:target t :status :no-file}
      (let [result (try (ctor/construct
                         {:target t :want want :observation universe
                          :interpretations patterns :interpretation-receipts receipts
                          :horizon horizon :move-cost 1
                          :budget {:max-moves 4 :max-expansions 10000}
                          :evaluate-g (fn [c] (if (empty? (:precedence c)) 1.0e9
                                                  (double (count (:precedence c)))))})
                        (catch Throwable e {:status :threw :message (.getMessage e)}))
            cands (:candidates result)
            admitted (vec (for [c cands
                                :let [a (candidate-want-progress
                                         {:facts universe :want want :interpretations patterns
                                          :horizon-steps horizon} (:precedence c))]]
                            {:precedence (:precedence c) :admission (if a (:reason a) :passes)}))]
        {:target t :n-patterns (count patterns) :wants (count want)
         :wants-covered-by-produces (count covered)
         :constructor (:status result) :refusal-kind (:kind result)
         :findings (:findings result) :candidates admitted
         :admitted? (boolean (some #(= :passes (:admission %)) admitted))}))))

(def results
  (into {} (for [seat seats
                 :let [dir (str "holes/labs/wm-contract/E-cascade-real/probe-interp/" seat)
                       declared (try (sources/load-declared dir)
                                     (catch Throwable e {:load-error (.getMessage e) :data (ex-data e)}))]]
             [seat (if (:load-error declared)
                     {:load-error (:load-error declared)}
                     {:rows (mapv #(score-target declared (:target %)) (:targets inputs))})])))

(defn pattern-sets [seat]
  (let [dir (str "holes/labs/wm-contract/E-cascade-real/probe-interp/" seat)
        d (sources/load-declared dir)]
    ;; Seat B wrote pattern ids as symbols, seat A as keywords; load-declared
    ;; accepts both. Normalise to keywords so agreement compares patterns, not spellings.
    (into {} (for [{t :target} (:targets inputs)
                   :let [norm #(keyword (str/replace (str %) #"^:" ""))
                         ps (get-in d [:interpretations t :patterns])]]
               [t {:patterns (set (map norm (keys ps)))
                   :produces (into {} (for [[p i] ps] [(norm p) (:produces i)]))}]))))

(def agreement
  (let [a (pattern-sets "seat-A") b (pattern-sets "seat-B")]
    (vec (for [{t :target} (:targets inputs)
               :let [pa (get-in a [t :patterns]) pb (get-in b [t :patterns])
                     both (clojure.set/intersection pa pb)]]
           {:target t :a (count pa) :b (count pb) :shared (count both)
            :shared-ids (vec (sort both))
            :same-produces-on-shared
            (count (filter #(= (get-in a [t :produces %]) (get-in b [t :produces %])) both))}))))

(pp/pprint {:results results :agreement agreement})
(println "SUMMARY")
(doseq [seat seats :let [rows (get-in results [seat :rows])]]
  (println seat
           "files" (count (remove #(= :no-file (:status %)) rows))
           "constructed" (count (filter #(= :constructed (:constructor %)) rows))
           "admitted" (count (filter :admitted? rows))
           "refusals" (frequencies (keep :refusal-kind rows))))
(shutdown-agents)
