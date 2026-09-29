(ns futon2.report.war-machine-accumulation-test
  (:require [clojure.repl]
            [clojure.test :refer [deftest is use-fixtures]]
            [clojure.edn :as edn] [clojure.java.io :as io]
            [futon2.aif.trace :as trace]
            [futon2.aif.belief :as belief]
            [futon2.aif.scan-bmr :as scan-bmr]
            [futon2.aif.scan-learn :as scan-learn]
            [futon2.aif.scan-shadow :as scan-shadow]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.run-tick-once :as tick]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files] [java.nio.file.attribute FileAttribute]))
(def entity "e")
(def obs {:c0 1.0 :c1 0.0})
(def envelope {:channels (update-vals obs #(hash-map :variant :observed :value %))})
(def beliefs {entity {:spawned 1.0 :refined 0.0}})
(def init {:authority :declared :prior 1.0 :model/revision "v1"})
(defn step [previous id]
  (wm/accumulation-step-for-tick {:previous-record previous :tick-id id :entity-id entity
                                  :observation obs :observation-envelope envelope :belief-pre beliefs :belief-post beliefs
                                  :initialization init}))
(defn record [id result]
  {:run/id id :accumulation-state (:state result) :observation obs :mu-post beliefs
   :accumulation-update-input (:update-input result)})
(defn refusal [f] (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))
(deftest three-tick-chain-and-refusals
  (let [a (step nil "t1") ar (record "t1" a) b (step ar "t2") br (record "t2" b)
        c (step br "t3") cr (record "t3" c)]
    (is (= [nil "t1" "t2"] (mapv #(get-in % [:accumulation-update-input :previous-id]) [ar br cr])))
    (doseq [r [ar br cr]]
      (is (= (:observation r) (get-in r [:accumulation-update-input :observation])))
      (is (= (get-in r [:mu-post entity]) (get-in r [:accumulation-update-input :belief-post]))))
    (is (= 4.0 (get-in cr [:accumulation-state :concentrations :c0 :spawned])))
    (is (= :accumulation-migration-required (refusal #(step (dissoc br :accumulation-state) "bad"))))
    (is (= :carry-chain-gap (refusal #(step (assoc-in ar [:accumulation-state :last-tick] "wrong") "bad"))))
    (is (= :support-mismatch
           (refusal #(wm/accumulation-step-for-tick
                      {:previous-record ar :tick-id "bad" :entity-id entity
                       :observation {:c0 1.0} :observation-envelope {:channels (select-keys (:channels envelope) [:c0])} :belief-pre beliefs :belief-post beliefs
                       :initialization init}))))))

(def ^:dynamic *dir* nil)
(use-fixtures :each
  (fn [f]
    (let [d (.toFile (Files/createTempDirectory "accumulation-receipt-" (make-array FileAttribute 0)))]
      (try (binding [*dir* d] (f))
           (finally (doseq [x (reverse (file-seq d))] (io/delete-file x true)))))))
(def decision {:action {:kind :cascade-candidate :target :fixture} :chosen-action-mass 1})
(defn inputs []
  {:enabled? true :trace-dir (str *dir*) :tick-id "next" :entity-id entity
   :observation obs :observation-envelope envelope :belief-pre beliefs :belief-post beliefs :initialization init})
(defn judged [input]
  (let [r (wm/accumulation-outcome-for-tick input)]
    (wm/with-accumulation-receipt
     {:run/id (:tick-id input) :decision decision :observation obs
      :belief beliefs :belief-pre beliefs
      :accumulation-state (:state r) :accumulation-update-input (:update-input r)
      :accumulation-initialization (:initialization r)} r)))
(defn publish [j]
  (:record (trace/write-trace! j :dir (str *dir*) :date-str "2026-09-26" :return-record? true)))
(defn same-selection [a b]
  (is (= (pr-str (dissoc (:decision a) :accumulation :accumulation-bmr))
         (pr-str (dissoc (:decision b) :accumulation :accumulation-bmr)))))
(defn assert-absent [kind input]
  (let [off (judged (assoc input :enabled? false)) j (judged input)
        saved (#'runner/persist-run-record!
               {:run-record-dir (str (io/file *dir* "runs"))
                   :scan-render-fn (fn [& _] nil)}
               "refused" "2026-09-26T00:00:00Z"
               {:outcome :offline-no-selection
                :checkpoints {:selection {:judgment {:decision (:decision j)}}}})
        disk (edn/read-string (slurp (:run-record saved)))
        r (trace/trace-record j)]
    (is (= kind (get-in j [:decision :accumulation :reason])
           (get-in disk [:decision :accumulation :reason])))
    (is (= (:accumulation-receipt j) (get-in r [:decision :accumulation])
           (:accumulation-receipt r)))
    (is (not (contains? r :accumulation-state)))
    (same-selection j off)
    r))

(deftest healthy-chain-hashes-the-persisted-state
  (let [first-record (publish (judged (assoc (inputs) :tick-id "first")))
        next-j (judged (inputs))]
    (is (true? (get-in first-record [:accumulation-receipt :initialization?])))
    (publish next-j)
    (let [r (peek (:records (trace/read-history-strict 12 :dir (str *dir*))))
          receipt (:accumulation-receipt r)]
      (is (= :accumulated (:status receipt)))
      (is (= "first" (:previous-id receipt)))
      (is (= "next" (:tick-id receipt)))
      (is (= entity (:entity receipt)))
      (is (= "v1" (:model/revision receipt)))
      (is (false? (:initialization? receipt)))
      (is (= (identity/sha256 (.getBytes (pr-str (:accumulation-state r)) "UTF-8"))
             (:state-sha256 receipt)))
      (is (= 3.0 (get-in r [:accumulation-state :concentrations :c0 :spawned])))
      (is (= receipt (get-in r [:decision :accumulation]))))))

(deftest missing-state-is-recorded-not-reinitialized
  (publish {:run/id "old" :observation obs :belief beliefs :decision decision})
  (assert-absent :accumulation-migration-required (inputs)))

(deftest strict-failures-do-not-call-the-adapter
  (let [file (io/file *dir* "wm-trace-2026-09-25.edn")]
    (.mkdir file)
    (with-redefs [wm/accumulation-step-for-tick
                  (fn [_] (throw (AssertionError. "Strict failure must not step or initialize")))]
      (assert-absent :trace-read-failed (inputs)))
    (.delete file)
    (spit file "{:run/id \"earlier\"}\n{:broken")
    (with-redefs [wm/accumulation-step-for-tick
                  (fn [_] (throw (AssertionError. "Malformed history must not step or initialize")))]
      (let [r (assert-absent :malformed-trace-record (inputs))]
        (is (= 2 (get-in r [:accumulation-receipt :index])))))
  (is (= [{:run/id "earlier"}] (trace/read-trace :dir (str *dir*) :date-str "2026-09-25")))))

(deftest adapter-refusals-retain-their-kind
  (doseq [[kind modify] [[:accumulation-identity-missing #(dissoc % :tick-id)]
                         [:single-entity-belief-missing #(assoc % :entity-id "missing")]
                         [:accumulation-initialization-required #(dissoc % :initialization)]
                         [:prior-not-positive #(assoc-in % [:initialization :prior] 0)]
                         [:accumulation-observation-unavailable #(assoc-in % [:observation :c0] -1)]]]
    (is (= kind (get-in (judged (modify (inputs))) [:decision :accumulation :reason]))))
  (let [r (step nil "old")]
    (publish {:run/id "different" :observation obs :belief beliefs :accumulation-state (:state r)})
    (is (= :carry-chain-gap (get-in (judged (inputs)) [:decision :accumulation :reason])))))

(deftest run-receipts-retain-the-same-outcome
  (let [j (judged (assoc (inputs) :enabled? false)) receipt (:accumulation-receipt j)
        once (#'tick/tick-run-record "one" "start" {} {:entries-read 0 :entries-limit 0} j "offline" false)
        saved (#'runner/persist-run-record!
               {:run-record-dir (str (io/file *dir* "runs"))
                   :scan-render-fn (fn [& _] nil)}
               "flight-offline" "2026-09-26T00:00:00Z"
               {:outcome :offline-no-selection
                :checkpoints {:selection {:judgment {:decision (:decision j)}}}})
        disk (edn/read-string (slurp (:run-record saved)))]
    (is (= {:status :absent :reason :accumulation-not-configured} receipt))
    (is (= receipt (get-in once [:decision :accumulation]) (get-in disk [:decision :accumulation])))))

(deftest scheduled-and-one-shot-share-the-id-rule
  (let [at (java.time.Instant/parse "2026-09-26T00:00:00Z")
        a (tick/mint-run-id at) b (tick/mint-run-id (.plusSeconds at 1))]
    (is (re-matches #"2026-09-26-[0-9]+" a))
    (is (= "2026-09-26-1790380800" a))
    (is (= "2026-09-26-1790380801" b))
    (is (not= a b))))

(deftest origin-and-lineage-survive-and-cannot-be-rebound
  (let [first-record (record "first" (step nil "first"))]
    (is (= {:authority :declared :prior 1.0}
           (get-in (step first-record "next") [:state :initialization])))
    (doseq [input [(assoc (inputs) :entity-id "another"
                        :belief-pre {"another" (beliefs entity)} :belief-post {"another" (beliefs entity)})
                   (assoc-in (inputs) [:initialization :model/revision] "v2")]]
      (is (= :accumulation-lineage-mismatch
             (refusal #(wm/accumulation-step-for-tick (assoc input :previous-record first-record))))))))

(deftest two-publishers-record-the-losing-update-without-stopping
  ;; Both computations read the same disk predecessor. Both runs can publish,
  ;; but only the first may claim a new accumulated state.
  (let [a (judged (assoc (inputs) :tick-id "a"))
        b (judged (assoc (inputs) :tick-id "b"))]
    (publish a)
    (let [saved (publish b)
          receipt (:accumulation-receipt saved)
          returned (trace/reconcile-accumulation b saved)]
      (is (= :accumulation-stale-predecessor (:reason receipt)))
      (is (= nil (:expected receipt)))
      (is (= "a" (:actual receipt)))
      (is (= receipt (get-in saved [:decision :accumulation])
             (:accumulation-receipt returned) (get-in returned [:decision :accumulation])))
      (doseq [record [saved returned] k [:accumulation-state :accumulation-update-input :accumulation-initialization]]
        (is (not (contains? record k))))
      (same-selection b saved)
      (is (= ["a" "b"] (mapv :run/id (:records (trace/read-history-strict 12 :dir (str *dir*)))))))))

(deftest receipt-only-tail-does-not-reinitialize-or-skip
  (publish (judged (assoc (inputs) :tick-id "a")))
  (publish (judged (assoc (inputs) :tick-id "b" :enabled? false)))
  (is (= :accumulation-migration-required
         (get-in (judged (assoc (inputs) :tick-id "c")) [:accumulation-receipt :reason]))))

(deftest default-flight-judge-forwards-configuration-with-one-publisher
  (let [captured (atom nil)]
    (with-redefs [wm/accumulation-config (constantly {:accumulation-entity-id entity
                                                    :accumulation-initialization init})
                  wm/generate-war-machine (fn [days opts] (reset! captured [days opts]))]
      ((:judge-fn (runtime/production-defaults
                   {:trace-dir (str *dir*) :run-id "flight" :flight {:target "m"}})) 3)
      (is (= 3 (first @captured)))
      (is (= {:accumulation-entity-id entity :accumulation-initialization init
              :trace-dir (str *dir*) :run-id "flight" :flight {:target "m"}
              :trace? false :include-advisory-lanes? false :defer-render? true}
             (second @captured))))))

(deftest unreadable-configuration-is-a-recorded-absence
  (with-redefs-fn {#'wm/accumulation-config-path (str (io/file *dir* "no-config.edn"))}
    (fn []
      (let [config (wm/accumulation-config)
            refusal (:accumulation-configuration-refusal config)]
        (is (= :accumulation-configuration-invalid (:reason refusal)))
        (is (= {:receipt refusal}
               (wm/accumulation-outcome-for-tick
                (assoc (inputs) :configuration-refusal refusal))))))))

(deftest returned-tick-and-flight-receipts-use-the-published-refusal
  (let [a (judged (assoc (inputs) :tick-id "a"))
        b (judged (assoc (inputs) :tick-id "b"))]
    (publish a)
    (let [publication (#'wm/write-trace-and-clock! b (str *dir*) true)
          returned (:judgement publication)
          receipt (get-in publication [:record :accumulation-receipt])
          once (#'tick/tick-run-record "b" "start" {} {:entries-read 0 :entries-limit 0}
                returned "offline" true)]
      (is (= :accumulation-stale-predecessor (:reason receipt)))
      (is (= receipt (get-in once [:decision :accumulation])))
      (is (nil? (:trace-write-failed returned)))
      (same-selection b returned)
      (let [flight (#'runner/publish-selection-trace! {:trace-dir (str *dir*)}
                    (assoc b :run/id "flight"))
            cell (#'runner/reconcile-selection-publication
                  {:judgment {:controller-decision (:decision b)} :ground {:decision (:decision b)}}
                  (:record flight))
            receipt (get-in flight [:record :accumulation-receipt])
            saved (#'runner/persist-run-record!
                   {:run-record-dir (str (io/file *dir* "runs"))
                   :scan-render-fn (fn [& _] nil)} "flight" "2026-09-27T00:00:00Z"
                   {:outcome :offline-no-selection :trace-path (:path flight)
                    :checkpoints {:selection cell}})
            disk (edn/read-string (slurp (:run-record saved)))]
        (is (= :accumulation-stale-predecessor (:reason receipt)))
        (is (= receipt (get-in flight [:judgement :decision :accumulation])
               (get-in cell [:judgment :controller-decision :accumulation])
               (get-in cell [:ground :decision :accumulation])
               (get-in disk [:decision :accumulation])))
        (is (not (contains? (:judgement flight) :accumulation-state)))
        (same-selection b (:judgement flight))))))

(deftest corrupt-corpus-does-not-claim-coherent-publication
  ;; A stale predecessor is recoverable. A corrupt authoritative corpus still
  ;; cannot yield the exact futility index; do not hide that independent error.
  (let [pending (judged (inputs))]
    (spit (io/file *dir* "wm-trace-2026-09-25.edn") "{:broken")
    (is (thrown-with-msg? RuntimeException #"EOF" (publish pending)))
    (is (not (.exists (io/file *dir* "wm-trace-2026-09-26.edn"))))))

;; ---------------------------------------------------------------------
;; ITEM6B SCAN-LIVE-CARRY-I: write-only raw-scan learner carry.

(def scan-a {:run/id "scan-a"
             :scan-exposures {:support {:covered 5 :claims 5}}})
(def scan-b {:run/id "scan-b"
             :scan-exposures {:support {:covered 0 :claims 5}}})
(def scan-c {:run/id "scan-c"
             :scan-exposures {:attack {:covered 4 :claims 4}}})

(defn scan-outcome [tick]
  (wm/scan-learn-outcome-for-tick
    {:trace-dir (str *dir*)
     :run/id (:run/id tick)
     :scan-exposures (:scan-exposures tick)}))

(defn scan-judgement [tick outcome]
  (wm/with-scan-learn-outcome
    {:run/id (:run/id tick)
     :scan-exposures (:scan-exposures tick)
     :decision decision :observation {} :belief {} :belief-pre {}
     }
    outcome))

(defn publish-scan [tick outcome]
  (:record (trace/write-trace! (scan-judgement tick outcome)
                               :dir (str *dir*) :date-str "2026-09-29"
                               :return-record? true)))

(deftest scan-learner-carries-through-two-publications
  (let [first-record (publish-scan scan-a (scan-outcome scan-a))
        second-record (publish-scan scan-b (scan-outcome scan-b))
        expected (:state (scan-learn/fold [scan-a scan-b]))]
    (is (= (:scan-learn-state first-record)
           (:state (scan-learn/fold [scan-a]))))
    (is (= (identity/sha256
             (.getBytes (pr-str (:scan-learn-state first-record)) "UTF-8"))
           (get-in first-record [:scan-learn-receipt :state-sha256])))
    (is (= expected (:scan-learn-state second-record)))
    (is (= (:q expected) (get-in second-record [:scan-learn-state :q])))
    (is (= (:concentrations expected)
           (get-in second-record [:scan-learn-state :concentrations])))
    (is (= "scan-a" (get-in second-record [:scan-learn-receipt :previous-id])))
    (is (= (:scan-learn-state second-record)
           (get-in (scan-outcome scan-c) [:predecessor :state])))
    (is (= (get-in second-record [:scan-learn-receipt :bmr])
           (get-in (scan-outcome scan-c) [:predecessor :bmr])))
    (is (not (get-in second-record [:scan-learn-receipt :recovered-by-fold])))
    (is (false? (get-in second-record [:scan-learn-receipt :bmr :applied])))
    (is (= (get-in second-record [:scan-learn-state :channel-ticks :support])
           (get-in second-record
                   [:scan-learn-receipt :bmr :channels :support
                    :n-admitted-ticks])
           2))
    (is (every? #(not (contains? % :counts))
                (vals (get-in second-record [:scan-learn-receipt :bmr :channels]))))))

(deftest scan-bmr-refuses-a-mismatched-carried-prior-without-losing-state
  (let [mismatched (assoc (scan-learn/prior-state) :kappa 99)]
    (trace/write-trace! (scan-judgement
                          scan-a
                          {:state mismatched
                           :receipt {:run/id "scan-a" :previous-id nil}})
                        :dir (str *dir*) :date-str "2026-09-29")
    (let [outcome (scan-outcome scan-b)]
      (is (map? (:state outcome)))
      (is (= 99 (get-in outcome [:state :kappa])))
      (is (= {:status :refused :reason :prior-mismatch :fields [:kappa]}
             (get-in outcome [:receipt :bmr]))))))

(deftest scan-bmr-exceptions-do-not-remove-the-learned-state
  (with-redefs [scan-bmr/score (fn [& _] (throw (ex-info "bmr fixture" {})))]
    (let [outcome (scan-outcome scan-a)]
      (is (map? (:state outcome)))
      (is (= :scan-bmr-unavailable (get-in outcome [:receipt :bmr :reason])))
      (is (= "clojure.lang.ExceptionInfo"
             (get-in outcome [:receipt :bmr :error :class]))))))

(deftest scan-learner-recovers-a-gap-by-full-fold
  (publish-scan scan-a (scan-outcome scan-a))
  (trace/write-trace! {:run/id "pre-carrier-gap" :decision decision
                       :observation {} :belief {} :belief-pre {}}
                      :dir (str *dir*) :date-str "2026-09-29")
  (let [outcome (scan-outcome scan-b)
        saved (publish-scan scan-b outcome)
        expected (:state (scan-learn/fold [scan-a scan-b]))]
    (is (true? (get-in outcome [:receipt :recovered-by-fold])))
    (is (not (contains? outcome :predecessor)))
    (is (= 1 (get-in outcome [:receipt :recovered-admitted])))
    (is (= expected (:scan-learn-state saved)))))

(deftest scan-learner-stale-publication-drops-state-and-next-tick-recovers
  (let [a (scan-outcome scan-a)
        b (scan-outcome scan-b)]
    (publish-scan scan-a a)
    (let [stale (publish-scan scan-b b)]
      (is (not (contains? stale :scan-learn-state)))
      (is (= {:status :absent :reason :scan-learn-stale-predecessor
              :expected nil :actual "scan-a"}
             (:scan-learn-receipt stale))))
    (let [outcome (scan-outcome scan-c)
          saved (publish-scan scan-c outcome)
          expected (:state (scan-learn/fold [scan-a scan-b scan-c]))]
      (is (true? (get-in outcome [:receipt :recovered-by-fold])))
      (is (= 2 (get-in outcome [:receipt :recovered-admitted])))
      (is (= expected (:scan-learn-state saved))))))

(deftest scan-learner-exceptions-are-recorded-not-thrown
  (with-redefs [scan-learn/step (fn [& _] (throw (ex-info "fixture" {:kind :fixture})))]
    (let [outcome (scan-outcome scan-a)]
      (is (nil? (:state outcome)))
      (is (= :absent (get-in outcome [:receipt :status])))
      (is (= :scan-learn-unavailable (get-in outcome [:receipt :reason])))
      (is (= "clojure.lang.ExceptionInfo" (get-in outcome [:receipt :error :class]))))))

(deftest scan-learner-attachment-is-write-only
  (let [control {:decision decision
                 :belief {"entity" {:spawned 1.0}}
                 :observation {:loop-health 0.5}
                 :accumulation-state {:control :same}}
        outcome {:state {:learned :state} :receipt {:status :learned}}
        attached (wm/with-scan-learn-outcome control outcome)]
    (is (= (select-keys control [:decision :belief :observation :accumulation-state])
           (select-keys attached [:decision :belief :observation :accumulation-state])))
    (is (= {:learned :state} (:scan-learn-state attached)))
    (is (= {:status :learned} (:scan-learn-receipt attached)))))

(deftest scan-shadow-wiring-is-write-only-and-reruns-only-for-exclusions
  (let [proxy "proxy"
        mu (belief/uniform-prior)
        learner-state (scan-learn/prior-state)
        base {:decision decision :belief {proxy mu} :observation {:x 1}}
        no-adoption {:state {:next true}
                     :receipt {:previous-id "previous"}
                     :predecessor {:state learner-state :bmr {:channels {}}}}
        no-rerun (wm/scan-shadow-for-tick
                  {:entity-id proxy :terminal-belief (:belief base)
                   :scan-learning no-adoption :scan-exposures {}
                   :r3-input {:same :arguments}})]
    (is (= mu (:mu-shadow no-rerun)))
    (is (not (contains? no-rerun :mu-excl)))
    (is (= "previous" (:previous-id no-rerun)))
    (let [captured (atom nil)
          rerun-row (assoc mu :spawned 0.2 :refined 0.0)
          expected-shadow {:shadow true}
          r3-input {:initial-belief (:belief base) :exclude-channels #{}}
          adopted (assoc-in no-adoption
                            [:predecessor :bmr :channels :support]
                            {:eligible true :chosen-model :learned})
          result (with-redefs [wm/r3-inner-loop
                               (fn [args]
                                 (reset! captured args)
                                 {:belief {proxy rerun-row}})
                               scan-shadow/shadow-row
                               (fn [args]
                                 (is (= rerun-row (:mu-excl args)))
                                 expected-shadow)]
                   (wm/scan-shadow-for-tick
                    {:entity-id proxy :terminal-belief (:belief base)
                     :scan-learning adopted
                     :scan-exposures {:support {:covered 1 :claims 2}}
                     :r3-input r3-input}))]
      (is (= (assoc r3-input :exclude-channels #{:support-coverage}) @captured))
      (is (= [:support] (:adopted result)))
      (is (= [:support-coverage] (:excluded-channels result)))
      (is (= rerun-row (:mu-excl result)))
      (is (= expected-shadow (:mu-shadow result))))
    (let [failure (with-redefs [scan-shadow/shadow-row
                                (fn [_] (throw (ex-info "shadow fixture" {})))]
                    (wm/scan-shadow-for-tick
                     {:entity-id proxy :terminal-belief (:belief base)
                      :scan-learning no-adoption :scan-exposures {}
                      :r3-input {}}))
          attached (wm/with-scan-learn-outcome base no-adoption failure)]
      (is (= :scan-shadow-unavailable (:reason failure)))
      (is (= (select-keys base [:decision :belief :observation])
             (select-keys attached [:decision :belief :observation])))
      (is (= (:state no-adoption) (:scan-learn-state attached))))
    (is (= {:status :absent :reason :no-predecessor-scan-state}
           (wm/scan-shadow-for-tick
            {:entity-id proxy :terminal-belief (:belief base)
             :scan-learning {:receipt {:status :learned}}})))
    (is (not (contains? (wm/with-scan-learn-outcome base no-adoption nil)
                        :scan-shadow)))))

(deftest stale-scan-publication-invalidates-its-shadow
  (let [a (scan-outcome scan-a)
        b (scan-outcome scan-b)]
    (publish-scan scan-a a)
    (let [judgement (assoc (scan-judgement scan-b b)
                           :scan-shadow {:schema :wm/scan-shadow-v1})
          saved (:record (trace/write-trace! judgement
                                              :dir (str *dir*)
                                              :date-str "2026-09-29"
                                              :return-record? true))
          reconciled (trace/reconcile-scan-learn judgement saved)]
      (is (= {:status :absent :reason :scan-shadow-stale-predecessor}
             (:scan-shadow saved)))
      (is (= (:scan-shadow saved) (:scan-shadow reconciled))))))

;; ---------------------------------------------------------------------
;; ITEM6-CONSUMER-I: the BMR score receipt rides the same publication and
;; reconciliation routes as the accumulation receipt. Prototype label:
;; record-only scoring; adoption deferred; a typed absence, never a gate.

(def init-rational {:authority :declared :prior 1 :model/revision "v1"})
(defn inputs-rational [] (assoc (inputs) :initialization init-rational))

(deftest losing-publisher-gets-the-bmr-absence-not-a-score
  ;; Sibling of two-publishers-record-the-losing-update-without-stopping:
  ;; the losing update of a publication race gets the ABSENCE, never a
  ;; score over unpublished concentrations.
  (let [a (judged (assoc (inputs-rational) :tick-id "a"))
        b (judged (assoc (inputs-rational) :tick-id "b"))
        winner (publish a)]
    (is (= :scored (get-in winner [:bmr-receipt :status])))
    (is (zero? (:delta-f (first (filter #(= :identity (:id %))
                                      (get-in winner [:bmr-receipt :proposals]))))))
    (let [saved (publish b)
          receipt (:bmr-receipt saved)
          returned (trace/reconcile-accumulation b saved)]
      (is (= :absent (:status receipt)))
      (is (= :accumulation-stale-predecessor (:reason receipt)))
      (is (= (:accumulation-receipt saved) (:cause receipt)))
      (is (not (contains? receipt :proposals)))
      (is (= receipt (get-in saved [:decision :accumulation-bmr])
             (:bmr-receipt returned) (get-in returned [:decision :accumulation-bmr])))
      (doseq [record [saved returned] k [:accumulation-state :accumulation-update-input :accumulation-initialization]]
        (is (not (contains? record k))))
      (same-selection b saved))))

(deftest configuration-refusal-carries-its-cause-into-the-bmr-absence
  (let [refusal {:status :absent :reason :accumulation-configuration-invalid :detail :fixture}
        j (wm/with-accumulation-receipt
           {:run/id "cfg" :decision decision :observation obs :belief beliefs}
           {:receipt refusal})
        saved (publish j)]
    (is (= :absent (get-in saved [:bmr-receipt :status])))
    (is (= :accumulation-configuration-invalid (get-in saved [:bmr-receipt :reason])))
    (is (= refusal (get-in saved [:bmr-receipt :cause])))
    (is (not (contains? (:bmr-receipt saved) :proposals)))
    (is (= (:bmr-receipt saved) (get-in saved [:decision :accumulation-bmr])))))

(deftest scored-bmr-receipt-is-the-same-on-every-route
  ;; As the item-5 test does for the accumulation receipt: the returned
  ;; judgement, the one-shot receipt and the flight run record carry the
  ;; SAME BMR receipt, computed once under the append lock.
  (let [j (judged (assoc (inputs-rational) :tick-id "first"))
        publication (#'wm/write-trace-and-clock! j (str *dir*) true)
        record (:record publication)
        returned (:judgement publication)
        receipt (:bmr-receipt record)
        once (#'tick/tick-run-record "first" "start" {} {:entries-read 0 :entries-limit 0}
              returned "offline" true)]
    (is (= :scored (:status receipt)))
    (is (= 3 (count (:proposals receipt))))
    (is (= {:threshold -3 :applied false :sum-compared-once true} (:rule receipt)))
    (is (= receipt (:bmr-receipt returned)
           (get-in returned [:decision :accumulation-bmr])
           (get-in once [:decision :accumulation-bmr])))
    (let [cell (#'runner/reconcile-selection-publication
                {:judgment {:controller-decision (:decision j)} :ground {:decision (:decision j)}}
                record)
          saved (#'runner/persist-run-record!
                 {:run-record-dir (str (io/file *dir* "runs"))
                   :scan-render-fn (fn [& _] nil)}
                 "flight" "2026-09-27T00:00:00Z"
                 {:outcome :offline-no-selection :trace-path (:path publication)
                  :checkpoints {:selection cell}})
          disk (edn/read-string (slurp (:run-record saved)))]
      (is (= receipt (get-in cell [:judgment :controller-decision :accumulation-bmr])
             (get-in cell [:ground :decision :accumulation-bmr])
             (get-in disk [:decision :accumulation-bmr]))))))

(deftest scan-learner-is-enabled-exactly-as-accumulation-is
  ;; The production route calls judge with :trace? false and the accumulation
  ;; entity configured (full_loop_runtime.clj); gating on trace? alone left the
  ;; learner off on every production click.
  (let [judge-form (read-string (clojure.repl/source-fn 'futon2.report.war-machine/judge))
        forms (tree-seq coll? seq judge-form)
        learner-gate (some (fn [f]
                             (when (and (seq? f) (= 'when (first f))
                                        (seq? (nth f 2 nil))
                                        (= 'scan-learn-outcome-for-tick (first (nth f 2))))
                               (second f)))
                           forms)
        accumulation-gate (some (fn [f]
                                  (when (and (seq? f) (= 'accumulation-outcome-for-tick (first f)))
                                    (get (second f) :enabled?)))
                                forms)]
    (is (some? learner-gate))
    (is (= '(or trace? accumulation-entity-id) learner-gate))
    (is (= accumulation-gate learner-gate))))

(deftest judge-wires-the-shadow-only-for-the-accumulation-entity
  (let [judge-form (read-string (clojure.repl/source-fn 'futon2.report.war-machine/judge))
        forms (tree-seq coll? seq judge-form)
        shadow-gate (some (fn [f]
                            (when (and (seq? f) (= 'when (first f))
                                       (= 'accumulation-entity-id (second f))
                                       (seq? (nth f 2 nil))
                                       (= 'scan-shadow-for-tick (first (nth f 2))))
                              f))
                          forms)
        attachment (some (fn [f]
                           (when (and (seq? f)
                                      (= 'with-scan-learn-outcome (first f))
                                      (= 'scan-shadow-result (last f)))
                             f))
                         forms)]
    (is (some? shadow-gate))
    (is (some? attachment))))
