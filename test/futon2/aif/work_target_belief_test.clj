(ns futon2.aif.work-target-belief-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.machine-belief :as mb]
            [futon2.aif.machine-model :as model]
            [futon2.aif.work-target-belief :as wt]))

;; Copies of load-missions/load-tickets entries read 2026-09-15. SHA-256
;; fields are acquisition context added by the snapshot caller, not fields
;; claimed to be supplied by the existing loaders. No registry scan in tests.
(def mission
  {:id "M-G-wm-wiring"
   :path "/home/joe/code/futon2/holes/missions/M-G-wm-wiring.md"
   :title "Mission: Wire G Through the War Machine (M-G-wm-wiring)"
   :status-line "HEAD. Campaign chartered by Joe (emacs-repl, 2026-09-15): \"we need to get real evidence that it can be completed… a new M-G-wm-wiring.md campaign that indexes into these checklist items as its (complex) gap, and that develops a strategy for working through them in a reasonable order that will get us to completion.\""
   :status-class :open :open-hole-count 9
   :sha256 "b14492b12a28ea6bee1dd54161c23fc7c282cdad43b0f8c5fd320c204651de53"})

(def ticket
  {:id "T-car3-phase2-impl" :kind :ticket
   :path "/home/joe/code/futon3c/holes/tickets/T-car3-phase2-impl.md"
   :title "T-car3-phase2-impl — durable turn queue (flag-gated, load-dark)"
   :status-line "PARTIAL -- shipped, one contract delta.** Core Phase-2 pieces are at HEAD and the model test passes (8/8, re-run at review). The hard 'default OFF' rollout requirement was flipped: turn_queue.clj:19 documents Default TRUE (2026-06-14) after the queue was proven live -- a dated decision, not a silent breach. Close by amending this rollout section to record the flip. Triage codex-5, reviewed claude-1."
   :status-class :live :parent "M-agency-hardening"
   :sha256 "42ab25071afe61b7a1cc933e25c071ec7cb882e90ba52b78cedb8b48d5fbcc25"})

(def t0 {:timestamp "2026-09-15T17:20:00Z" :tick-id "p1a-first"})
(def t1 {:timestamp "2026-09-15T17:21:00Z" :tick-id "p1a-next"})
(def snapshot {:status :read :entries [mission ticket] :read-at (:timestamp t0)})
(def candidates
  [{:action {:type :advance-mission :target (:id mission)} :G 2.0}
   {:action {:type :advance-ticket :target (:id ticket)} :G 3.0}])
(def nonuniform
  {:spawned 1/10 :refined 1/10 :strengthened 2/5 :addressed 1/10
   :falsified 1/10 :foreclosed 1/10 :reopened 1/10})

(defn kind [result] (get-in result [:refusal :kind]))
(defn introduce [declaration cs]
  (wt/carry-and-introduce declaration {:status :absent-pre-genesis}
                          (wt/admissions cs snapshot t0) t0))
(defn context [state target]
  (assoc (:model-context state) :entity/id target
         :mode :single-entity :policy-entities [target]))

(deftest first-introduction-from-pinned-declaration
  (let [d (wt/read-declaration)
        admitted (wt/admissions candidates snapshot t0)
        state (introduce d candidates)]
    (is (:ok d))
    (is (:ok state))
    (is (= #{(:id mission) (:id ticket)} (set (keys (:belief state)))))
    (doseq [id [(:id mission) (:id ticket)]]
      (let [row (get-in state [:belief id])
            lineage (get-in state [:lineage id])]
        (is (= (get-in d [:declaration :initial-distribution :masses]) row))
        (is (= (zipmap mb/state-support (repeat 1/7)) row))
        (is (every? ratio? (vals row)))
        (is (= :exact (model/row-sum-admission row)))
        (is (= {:introduced-at (:timestamp t0)
                :admission (get-in admitted [:admitted id])
                :D {:name (get-in d [:declaration :initial-distribution :name])
                    :revision (get-in d [:declaration :revision])
                    :authority :declared-prior
                    :declaration-sha256 wt/declaration-sha256}
                :interpretation-revision (get-in d [:declaration :revision])
                :decision-ref (get-in d [:declaration :decision :bell])
                :updates :no-admitted-observations
                :information-cutoff (:timestamp t0)} lineage))))))

(deftest departure-and-readmission-never-rewrite-carry
  (let [d (wt/read-declaration)
        id (:id mission)
        ;; A deliberately different retained distribution detects a reset to D.
        previous (assoc-in (introduce d candidates) [:belief id] nonuniform)]
    (doseq [cs [[] [(second candidates)] candidates]]
      (let [next-state (wt/carry-and-introduce
                        d {:status :present :state previous}
                        (wt/admissions cs snapshot t1) t1)]
        (is (:ok next-state))
        (is (= (:belief previous) (:belief next-state)))
        (is (= (:lineage previous) (:lineage next-state)))
        (is (identical? nonuniform (get-in next-state [:belief id])))
        (is (identical? (get-in previous [:lineage id])
                        (get-in next-state [:lineage id])))
        (is (= (:timestamp t0) (get-in next-state [:lineage id :information-cutoff])))
        (is (every? #(= :no-admitted-observations (:updates %))
                    (vals (:lineage next-state))))))))

(deftest unusable-predecessor-is-not-genesis
  (let [d (wt/read-declaration) a (wt/admissions candidates snapshot t0)]
    (doseq [p [{:status :missing-after-genesis}
               {:status :unreadable :reason :bad-edn}
               {:status :present :state nil}]]
      (let [r (wt/carry-and-introduce d p a t0)]
        (is (= :carry-missing (kind r)))
        (is (not (contains? r :belief)))
        (is (not (contains? r :lineage)))))))

(deftest later-introduction-does-not-change-earlier-target
  (let [d (wt/read-declaration)
        previous (introduce d [(first candidates)])
        next-state (wt/carry-and-introduce
                    d {:status :present :state previous}
                    (wt/admissions [(second candidates)] snapshot t1) t1)]
    (is (= #{(:id mission) (:id ticket)} (set (keys (:belief next-state)))))
    (is (= (get-in previous [:belief (:id mission)])
           (get-in next-state [:belief (:id mission)])))
    (is (= (get-in previous [:lineage (:id mission)])
           (get-in next-state [:lineage (:id mission)])))
    (is (= (:timestamp t1) (get-in next-state [:lineage (:id ticket) :introduced-at])))
    (is (= (:timestamp t1) (get-in next-state [:lineage (:id ticket) :information-cutoff])))
    (is (= :exact (model/row-sum-admission (get-in next-state [:belief (:id ticket)]))))))

(deftest half-present-target-is-never-reintroduced-even-after-departure
  (let [d (wt/read-declaration) id (:id mission)
        initial (introduce d candidates)]
    (doseq [part [:belief :lineage]
            cs [candidates []]]
      (let [damaged (update initial part dissoc id)
            result (wt/carry-and-introduce d {:status :present :state damaged}
                                          (wt/admissions cs snapshot t1) t1)]
        (is (= :carry-missing (kind (get-in result [:target-refusals id]))))
        (is (= (:belief damaged) (:belief result)))
        (is (= (:lineage damaged) (:lineage result)))
        (is (= :carry-missing
               (kind (wt/target-belief-input (context result id) result #{id} id))))))))

(deftest four-way-adapter-and-unchanged-row-seven
  (let [d (wt/read-declaration) id (:id mission) never (:id ticket)
        unknown "X-p1a-not-a-registry-id"
        state (assoc-in (introduce d [(first candidates)]) [:belief id] nonuniform)
        ids #{id never}
        ctx (context state id)
        plain (mb/belief-state-distribution ctx (:belief state))
        readable (wt/target-belief-input ctx state ids id)]
    (is (= :entity-outside-registry
           (kind (wt/target-belief-input (context state unknown) state ids unknown))))
    (is (= :registered-not-admitted
           (kind (wt/target-belief-input (context state never) state ids never))))
    (doseq [absent [unknown never]]
      (is (= :missing-entity
             (kind (mb/belief-state-distribution (context state absent) (:belief state))))))
    (is (= :carry-missing
           (kind (wt/target-belief-input ctx (update state :belief dissoc id) ids id))))
    (is (:ok readable))
    (is (= plain (dissoc readable :lineage)))
    (is (= (get-in state [:lineage id]) (:lineage readable)))
    (is (identical? nonuniform (get-in readable [:belief-input :posteriors id])))
    (is (= :joint-construction-required
           (kind (wt/target-belief-input (assoc ctx :policy-entities [id never]) state ids id))))
    (is (= :entity-context-mismatch
           (kind (wt/target-belief-input (context state never) state ids id))))))

(deftest admission-filters-exact-targets-and-preserves-source-context
  (let [endpoint "futon4-d/mission/G-wm-wiring"
        cs (concat candidates
                   [{:type :advance-mission :target endpoint}
                    {:type :advance-ticket :target (:id ticket)}
                    {:type :fire-pattern :target (:id mission)}
                    {:action {:type :open-mission :target endpoint}}
                    {:type :advance-mission :target "M-does-not-exist"}
                    {:type :advance-ticket :target (:id mission)}
                    {:type :advance-mission :target (keyword (:id mission))}])
        result (wt/admissions cs snapshot t0)]
    (is (:ok result))
    (is (= #{(:id mission) (:id ticket) endpoint} (set (keys (:admitted result)))))
    (is (= {:type :advance-mission :registry-id (:id mission)
            :registry-pin (select-keys mission [:path :sha256 :status-class])
            :admitted-at (:timestamp t0)}
           (get-in result [:admitted endpoint])))
    (is (= {:not-a-work-target-type 2 :not-registry-eligible 3}
           (frequencies (map :reason (:not-admitted result)))))
    (is (= {} (:admitted (wt/admissions [] snapshot t1))))
    (is (= :registry-pin-missing
           (kind (wt/admissions candidates (update snapshot :entries
                                                  #(mapv (fn [e] (dissoc e :sha256)) %)) t0))))
    (let [r (wt/admissions candidates {:status :unreadable :reason :permission-denied} t0)]
      (is (= :registry-unreadable (kind r)))
      (is (not (contains? r :admitted)))
      (is (= r (wt/carry-and-introduce (wt/read-declaration)
                                      {:status :absent-pre-genesis} r t0))))))

(deftest declaration-pin-and-model-separation
  (let [d (wt/read-declaration) a (wt/admissions candidates snapshot t0)
        prior {:status :absent-pre-genesis}]
    (testing "the loader refuses changed bytes, including a harmless newline"
      (let [file (java.io.File/createTempFile "p1a-declaration-" ".edn")]
        (try
          (spit file (str (:text d) "\n"))
          (is (= :declaration-hash-mismatch (kind (wt/read-declaration (.getPath file)))))
          (finally (.delete file)))))
    (is (= :declaration-hash-mismatch
           (kind (wt/carry-and-introduce (update d :text str "\n") prior a t0))))
    (is (= :declaration-content-mismatch
           (kind (wt/carry-and-introduce
                  (assoc-in d [:declaration :initial-distribution :masses] nonuniform) prior a t0))))
    (is (= :model-context-mismatch
           (kind (wt/carry-and-introduce
                  d {:status :present :state (assoc (introduce d candidates)
                                                    :model-context {:model :strategic-mu-post})}
                  a t1))))))

(deftest public-api-has-no-observation-update-operation
  (is (= '#{declaration-path declaration-sha256 read-declaration admissions
            carry-and-introduce target-belief-input}
         (set (keys (ns-publics 'futon2.aif.work-target-belief)))))
  (let [d (wt/read-declaration)
        state (assoc-in (introduce d candidates)
                        [:lineage (:id mission) :updates] :registry-transition)
        result (wt/carry-and-introduce d {:status :present :state state}
                                      (wt/admissions candidates snapshot t1) t1)]
    (is (= :unsupported-update-lineage (kind result)))
    (is (not (contains? result :belief)))))
