#!/usr/bin/env bb
;; Builds witnesses/outer-selection-conformant-2026-11-01.edn from real pins (claude-2, 2026-11-01).
;; Scoring replicates src/futon2/aif/outer_score.clj (risk + ambiguity - novelty) with evidence-based reach.
(require '[clojure.pprint :as pp])
(def endings [:focused :related :unrelated :stop-the-line :not-yet-evaluated])
(def gains [:none :one :several])
(def outcomes (vec (for [e endings g gains] [e g])))
(defn normalize [m ks] (let [t (reduce + (map #(double (get m % 0)) ks))] (into {} (map (fn [k] [k (/ (double (get m k 0)) t)]) ks))))
(def ending-kit {:focused 55 :related 35 :unrelated 5 :stop-the-line 5 :not-yet-evaluated 0})
(def gain-kit {:none 1 :one 2 :several 3})
(def C (let [e (normalize ending-kit endings) g (normalize gain-kit gains)] (into {} (for [[x y :as o] outcomes] [o (* (e x) (g y))]))))
(defn kernel [landing reach] (let [row0 (zipmap outcomes (repeat 0.0)) stop [:stop-the-line :none] blocked (assoc row0 stop 1.0) doable (if (= landing stop) blocked (assoc row0 landing reach stop (- 1.0 reach)))] {:doable doable :blocked blocked}))
(defn push [q k] (into {} (for [o outcomes] [o (+ (* (:doable q) (get-in k [:doable o])) (* (:blocked q) (get-in k [:blocked o])))])))
(defn risk [p] (reduce + (for [[o m] p :when (pos? m)] (* m (Math/log (/ m (C o)))))))
(defn entropy [row] (- (reduce + (for [[_ m] row :when (pos? m)] (* m (Math/log m))))))
(defn ambiguity [q k] (+ (* (:doable q) (entropy (:doable k))) (* (:blocked q) (entropy (:blocked k)))))
(defn novelty [a b k] (if (zero? a) 0.0 (let [t (+ a b) var (/ (* a b) (* t t (inc t))) fisher (reduce + (for [o outcomes :let [ad (get-in k [:doable o]) ab (get-in k [:blocked o]) pr (/ (+ (* a ad) (* b ab)) t)] :when (pos? pr)] (/ (* (- ad ab) (- ad ab)) pr)))] (* 0.5 var fisher))))
(defn score [{:keys [state-counts landing reach]}]
  (let [[a b] state-counts q {:doable (/ a (+ a b)) :blocked (/ b (+ a b))} k (kernel landing reach) p (push q k) r (risk p) am (ambiguity q k) nv (novelty a b k)]
    {:q q :risk r :ambiguity am :novelty nv :G (- (+ r am) nv) :prediction (into {} (filter (fn [[_ v]] (pos? v)) p))}))
(def lib "futon3/library/task-selection/")
(def pins {"only-what-can-start-now" "c0d2464a682f91d942450151040829a05e439960c651e83f8ec3c3866d5fb475"
           "what-others-wait-on-first" "a582e82e22f66b4bf7be5e7c81d886f137bda923b8e6a481eb9635ee3f9d80be"
           "prepare-the-ground" "16108c59bf0dd84c4ac9d42197a19699423b3e14f96833db3599bfd5d3746f3a"
           "repair-goes-to-the-front" "eaa7021ac0b4693d40a626858239e96a8e014ce03d61fb665a637a7090536897"
           "end-in-something-shown" "3a1af79cd2f48948eb3a18ecb64d0227607c8ed7a57bcd07bc41582a35bedc52"
           "find-out-before-building" "6654bd9b1f346e17e97580fecd11ba176a9b1de660fe6bf930883ff64a53ed28"
           "bound-the-bet" "eedd5c216220e86aa15c0827a309314c3b80e81d518b1db4e428dec77a07a9bf"
           "halfway-house" "fbfd521afb4cb0f10afffbd1249060a2cf04cdec96ddf228c9e3bd37c0a252ae"})
(defn unit [p i] {:pattern (str lib p ".flexiarg") :sha256 (pins p) :index i})
(defn cascade [id readme-row patterns edges] {:id id :readme-cascade readme-row :units (vec (map-indexed (fn [i p] (unit p i)) patterns)) :edges edges})
(def abst (score {:state-counts [0 1] :landing [:stop-the-line :none] :reach 0.0}))
(def c1 (score {:state-counts [3 1] :landing [:related :several] :reach 0.5}))
(def c3 c1)
(def c5 (score {:state-counts [1 1] :landing [:related :none] :reach (/ 609.0 615.0)}))
(def facade (score {:state-counts [3 1] :landing [:stop-the-line :none] :reach 0.5}))
(doseq [[n s] [["abstention" abst] ["C1 giraffe-order" c1] ["C3 repair-click" c3] ["C5 uncertain-design" c5] ["facade click12" facade]]]
  (println (format "%-22s G=%.4f risk=%.4f amb=%.4f nov=%.4f" n (:G s) (:risk s) (:ambiguity s) (:novelty s))))
(def w
 {:schema :wm/outer-selection-witness-v1
  :purpose "Hand-constructed conformant selection run on the real 2026-11-01 field (claude-2). Shows the shape a conforming :outer/selection must have and that it can be built from real data. Not produced by the machine."
  :as-of "2026-11-01T06:02Z"
  :generator "witnesses/build-outer-selection-witness.bb"
  :focus {:status :retained :focus "WM" :producer "futon2.aif.focus-receipt/decision-focus-info"}
  :field
  {:population {:source "mission registry (src/futon2/aif/mission_registry.clj), as retained in run ac5bc477 :world-at-selection :open-tasks"
                :counts {:missions 181 :excursions 255 :tickets 190 :total 626}
                :ids-file "witnesses-open-tasks-2026-11-01.edn" :ids-sha256 "e82f6381fa3e056fff332cdb510662469e353fb120cbb7120d1eb2b46f9333d9"}
   :giraffe {:run "/home/joe/code/storage/giraffe/20261101T044121.422Z-730407c5" :pass-sha256 "a9ad27a8cb6fd0c808413ce639019b117aed746fbcb6b2945560cfc71423ff91" :input-sha256 "730407c514e2fd21905da2b533d92e046a454372c919fbd89089fc953ebf1667"
             :items 609 :status-counts {:ready 500 :not-ready 57 :undetermined 52}}
   :admitted 609
   :excluded [{:reason :giraffe/unread-new :count 17 :evidence "pass.edn :graph-coverage :drift :unread-new"
               :ids ["E-kimi-task-171" "E-kimi-task-172" "E-kimi-task-173" "E-kimi-task-174" "E-kimi-task-175" "E-kimi-task-176" "E-kimi-task-177" "E-kimi-task-178" "E-kimi-task-179" "E-kimi-task-180" "E-kimi-task-181" "E-kimi-task-182" "E-kimi-task-183" "E-kimi-task-184" "E-kimi-task-185" "E-kimi-task-186" "M-peradam-grounding"]}]
   :due-consideration "626 open tasks are the construction field (Q2 as restored in L1: targets = enumeratedTasks for a selection run); 609 carry Giraffe facts and 17 carry a typed in-field exclusion with a pin. No task is missing and none is excluded without a reason (Q1)."}
  :library {:path lib :commit "f3b8b32211e567dadba586db86bd54c8b2ba53c8" :pattern-count 19
            :manifest-sha256 "7dac941136815a55974bd0d0de5e540b0bc474bec5ac7c4addad0d5e148d57d6"
            :slice (vec (sort (keys pins))) :slice-from-whole-library true
            :note "the slice is the 8 patterns the three README cascades are composed from; every unit cites a library file by path and sha"}
  :makeup {:path "futon2/data/wm-learning/outer-makeup.edn" :sha256 "f312583522597c49b4134b08c4a186a013baaaa12a6f1c332fe9875e57f58e3a"
           :used-for "C kit (55/35/5/5/0 x 1/2/3) and state kit only; the cascade menu in that file is not used, a hand-listed menu is a facade"}
  :cascades
  [(assoc (cascade :giraffe-order 1 ["only-what-can-start-now" "prepare-the-ground" "what-others-wait-on-first"] [[0 1 :precedes] [1 2 :precedes]])
          :applied {:steps [:ready-gate :by-cone :by-unblocks] :field-after-gate 500 :pick "M-pattern-retrieval-calibration" :giraffe-facts {:status :ready :cone 2 :unblocks 2 :rank 1}}
          :relation {:class :associated :facet "WM" :producer "focus-receipt/classify-target, run 2026-11-01T06:02Z"}
          :model {:state-counts [3 1] :state-counts-source "state kit for :ready (starter; no classified ending has yet updated it)"
                  :landing [:related :several] :reach 0.5
                  :reach-source {:kind :observed :evidence "4 prior clicks on this target: 2 grounded-progress, 2 guardrail-refusal (runs d5b9b513, c5590f95, e71c7380, f4b95abf)" :counts [2 2]}}
          :score c1)
   (assoc (cascade :repair-click 3 ["repair-goes-to-the-front" "only-what-can-start-now" "end-in-something-shown"] [[0 1 :precedes] [1 2 :precedes]])
          :applied {:steps ["repair obligation open on the path of the next candidate?" :ready-gate "verifiable result required"]
                    :next-candidate "M-pattern-retrieval-calibration" :open-repair-obligations-on-its-path 0
                    :note "22 repaired targets have open obligations (first in field order: M-interim-director-proxy-metric-inventory, rank 4, 6 tickets) but none on the next candidate, so the pattern passes the field through unchanged"
                    :pick "M-pattern-retrieval-calibration" :giraffe-facts {:status :ready :cone 2 :unblocks 2 :rank 1}}
          :relation {:class :associated :facet "WM" :producer "focus-receipt/classify-target"}
          :model {:state-counts [3 1] :landing [:related :several] :reach 0.5 :reach-source {:kind :observed :counts [2 2]}}
          :score c3
          :tie-with :giraffe-order
          :tie-disclosure "same pick, same model, same G as :giraffe-order: a genuine same-pick tie. Under the restored requirements it is disclosed as a tie and resolved by the declared tie law, never by menu position. It is not the minimum here, so it does not decide the click.")
   (assoc (cascade :uncertain-design 5 ["find-out-before-building" "bound-the-bet" "halfway-house"] [[0 1 :precedes] [1 2 :precedes]])
          :applied {:steps [{:gate-status :undetermined} "discovery task ahead of the build" "bounded: one dependency reading" "halfway: read, do not build"]
                    :field-after-gate 52 :pick-target "M-interim-director" :giraffe-facts {:status :undetermined :rank 3 :reason :needs/dependency-reading :cone nil}
                    :pick {:kind :discovery
                           :task "resolve the out-of-scope prerequisite of M-interim-director: read M-interest-network-coupling (futon4) step (b), event vocabulary committed in code, and record whether it landed"
                           :producer :dependency-reader-reviewer
                           :evidence {:path "/home/joe/code/futon7/holes/M-interim-director.md" :line 7 :sha256 "aad85c4b70e1e68ec659f21c0ec07a4d886a4520bad48555d30aca84a3511dd3"}}}
          :relation {:class :associated :facet "WM" :producer "focus-receipt/classify-target (M-interim-director; M-interest-network-coupling also :associated)"}
          :model {:state-counts [1 1] :state-counts-source "state kit for :undetermined" :landing [:related :none] :reach (/ 609.0 615.0)
                  :reach-source {:kind :observed :evidence "dependency readings complete for 609 of 615 members (pass.edn :graph-coverage)" :counts [609 6]}
                  :gain-note ":none, a reading makes nothing ready by itself; it makes M-interim-director determined, which is information gain and is carried by the novelty term"}
          :score c5)]
  :abstention {:model {:state-counts [0 1] :landing [:stop-the-line :none]} :score abst
               :note "G of choosing nothing. Every admitted cascade must beat it strictly; the facade did not."}
  :selection {:minimum :uncertain-design :strict true
              :margins {:over-next (- (:G c1) (:G c5)) :over-abstention (- (:G abst) (:G c5))}
              :tie-law {:declared "ties among non-minimal cascades are disclosed; a tie at the minimum refuses the click with :tie-at-minimum and records the tied cascades and their picks" :tie? false :tied-cascades [:giraffe-order :repair-click]}
              :selected {:kind :discovery :target "M-interim-director" :cascade :uncertain-design}
              :why "lowest G: the field has 52 undetermined items and the top of the ready order is a target already clicked four times with two refusals; a bounded reading that determines a rank-3 item is predicted to land :related with reach 0.99 and carries the largest novelty term because the doable/blocked prior for undetermined items is 1:1"}
  :facade-control {:what "click 12 as recorded: E-comprehension-foundation, relation :unknown (relation-not-declared per classify-target), reach 1/2 default, landing stop-the-line/none" :score facade
                   :reads "G equals abstention to the last digit; under the restored requirements this is three violations before any comparison: unknown relation on the selected path, defaulted reach, pick scoring equal to abstention"}
  :lean-shape {:RunFacts {:openTasks 626 :enumeratedTasks 626 :targetsReachingScoring 609 :libraryPatternCount 19
                          :targetConstruction [{:kind :selection :targets "the 626 enumerated (17 with typed exclusions)" :action :select-task :reading "giraffe pass sha" :library "library manifest sha" :graph "giraffe input sha" :kernel "makeup sha" :slice 8 :pool 8 :sliceFromWholeLibrary true :policyCount 3 :policies "3 cascade digests"}]
                          :comparedPolicies 3 :cascadesWithoutG 0 :policyPreferences "3 records, horizon 1, terminalOnly, rawCumulative, terms risk+ambiguity+informationGain"
                          :interpretationOrder :selectionBeforeInterpretation :pathAbsenceCount 0
                          :previousChoice ["E-comprehension-foundation" :what-others-wait-on-first] :previousOutcome :invalid :currentChoice ["M-interim-director discovery" :uncertain-design]
                          :factorizedPreference "NOT MET by the kit product: completion (gain) and relevance (ending) are independent marginals with no certificate; the one requirement this witness cannot satisfy without an authorized factorization (Q9)"}}})
(spit "outer-selection-conformant-2026-11-01.edn" (with-out-str (pp/pprint w)))
(println "written outer-selection-conformant-2026-11-01.edn")
