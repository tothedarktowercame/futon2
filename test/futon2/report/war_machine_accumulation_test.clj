(ns futon2.report.war-machine-accumulation-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.edn :as edn] [clojure.java.io :as io]
            [futon2.aif.trace :as trace]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.full-loop-runner :as runner]
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
  (is (= (pr-str (dissoc (:decision a) :accumulation))
         (pr-str (dissoc (:decision b) :accumulation)))))
(defn assert-absent [kind input]
  (let [off (judged (assoc input :enabled? false)) j (judged input)
        saved (#'runner/persist-run-record!
               {:run-record-dir (str (io/file *dir* "runs"))}
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
               {:run-record-dir (str (io/file *dir* "runs"))}
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

(deftest two-publishers-cannot-commit-from-the-same-predecessor
  ;; Both read the actual same disk state before either publishes. The
  ;; append-lock validation must reject the second without writing its state.
  (let [a (judged (assoc (inputs) :tick-id "a"))
        b (judged (assoc (inputs) :tick-id "b"))]
    (publish a)
    (is (= :accumulation-stale-predecessor (refusal #(publish b))))
    (is (= ["a"] (mapv :run/id (:records (trace/read-history-strict 12 :dir (str *dir*))))))))

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
      (#'runner/default-selection-judge {:trace-dir (str *dir*) :run-id "flight" :flight {:target "m"}} 3)
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
