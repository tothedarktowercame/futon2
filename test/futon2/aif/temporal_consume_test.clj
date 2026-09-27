(ns futon2.aif.temporal-consume-test
  "Real flight loop, isolated git checks, decision/ranker and persisted records.
   Transport executes declared primitives locally. Three declared artifact
   wants permit three chronological clicks; no progress still closes."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.efe :as efe]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as runner]
            [futon2.aif.full-loop-runner :as loop-runner]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.temporal-input-test :as fixture]
            [futon2.aif.temporal-update :as temporal]
            [futon2.aif.token-belief-predecessor :as predecessor]
            [futon2.aif.token-initialization-policy :as initialization]
            [futon2.aif.zeta-posterior :as zeta]
            [futon2.aif.likelihood-precision :as lprec]
            [futon2.report.observation-labels-consume-test :as population]
            [futon2.report.cascade-decision-test :as decision-fixture]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- read! [path] (edn/read-string (slurp path)))
(defn- git! [repo & args]
  (let [{:keys [exit out err]} (apply sh/sh "git" "-C" (str repo) args)]
    (when-not (zero? exit) (throw (ex-info "git fixture" {:err err})))
    (str/trim out)))
(defn- isolated [f]
  (let [root (.toFile (Files/createTempDirectory "temporal-consume-" (make-array FileAttribute 0)))]
    (try (with-redefs [checks/repo-root (str root)] (f root))
         (finally (doseq [file (reverse (file-seq root))] (io/delete-file file true))))))

(defn- harness [root {:keys [first-pattern outcome theta stale? max-clicks observation-labels-path]
                      :or {first-pattern :write-a outcome true theta 1/2 max-clicks 3}}]
  (let [{:keys [repo interps check]} (fixture/fixture root)
        target fixture/target tokens [:a :b :open]
        domain (set (map #(vector target %) tokens))
        interps (assoc interps :write-open (assoc (:write-a interps) :produces #{:open}))
        interps (into {} (map (fn [[k v]] [k (assoc v :domain domain :theta theta
                                                   :guard {:needs #{} :forbids #{}})])) interps)
        locators (into {} (for [t tokens] [t {:class :C3 :repo "artifacts" :sha "HEAD" :path (name t)}]))
        facts #(into {} (map (fn [[t result]] [t (:observed result)]))
                     (:results (checks/observe locators)))
        calls (atom []) paths (atom {}) results (atom []) q0s (atom []) joint-rankings (atom []) lane-options (atom []) lane-rankings (atom [])
        real-rank efe/rank-actions real-lane wm/cascade-lane
        source (fn [pattern]
                 {:universes {target (facts)} :locators {target locators}
                  :wants {target tokens} :horizon-steps 1
                  :beta-by-context {:test 1} :context-of (constantly :test)
                  :token-initialization {target {:policy initialization/disabled}}
                  :interpretations {target {:patterns interps
                                            :receipts (zipmap (keys interps) (repeat {:receipt "declared fixture"}))}}
                  :candidates {target [{:precedence [pattern] :construction-receipt decision-fixture/receipt}]}})
        click (fn [opts]
                (let [n (inc (count @calls)) id (str "click-" n)
                      pattern (if (= n 1) first-pattern (if (= n 2) (if (= first-pattern :write-b) :write-a :write-b) :write-open))
                      assembled (problems/assemble {:targets [target] :sources (source pattern)})
                      decision (:decision
                                (with-redefs [efe/rank-actions
                                              (fn [belief actions options]
                                                (when (:prediction-context options)
                                                  (swap! q0s conj (:cascade-belief belief)))
                                                (let [ranked (real-rank belief actions options)]
                                                  (when (:prediction-context options)
                                                    (swap! joint-rankings conj {:state belief :actions actions :opts options :ranked ranked}))
                                                  ranked))
                                              wm/cascade-lane
                                              (fn [problem options]
                                                (swap! lane-options conj options)
                                                (let [lane (real-lane problem options)]
                                                  (swap! lane-rankings conj (:ranked lane)) lane))]
                                  (wm/cascade-decision assembled
                                   (merge decision-fixture/live-c-opts opts
                                          {:observation-labels-path observation-labels-path
                                           :focus-inputs (update (:focus-inputs decision-fixture/live-c-opts) :relations
                                                                 conj (assoc (first (get-in decision-fixture/live-c-opts [:focus-inputs :relations]))
                                                                             :target target))
                                           :cascade-habit-path (str (io/file root "no-habit"))
                                           :token-belief-context {:occurrence-id (str "selection-" n)}}))))
                      saved (#'loop-runner/persist-run-record!
                             {:run-record-dir (str (io/file root "runs")) :click-id id}
                             id "2026-09-27T03:00:00Z"
                             {:outcome :offline-no-selection
                              :checkpoints {:selection {:judgment {:controller-decision decision}}}})
                      record (read! (:run-record saved))]
                  (swap! paths assoc id (:run-record saved))
                  (swap! calls conj record)
                  {:click-id id :chosen (get-in record [:decision :chosen])}))
        fetch #(read! (get @paths (if (= % "racer") "click-2" %)))
        enact (runner/enact-fn
               {:interpretations (constantly interps) :check-fn check
                :trace-dir (str (io/file root "trace")) :record-dir (str (io/file root "enactments"))
                :fetch-run-record fetch
                :dispatch-step!
                (fn [{:keys [interpretation]}]
                  (let [t (first (:produces interpretation))]
                    (when (or (> (count @calls) 1) outcome)
                      (spit (io/file repo (name t)) (str t)))
                    (git! repo "add" ".") (git! repo "commit" "--allow-empty" "-qm" "execute")
                    (let [sha (git! repo "rev-parse" "HEAD")]
                      {:commit sha :produced t :check (assoc (get locators t) :sha sha)})))})
        run (fn []
              (flight/run!
               (flight/start {:target target} {:kind :operator-declared :wants tokens :declared-by :test}
                             {:id "consume-fixture"})
               {:max-clicks max-clicks :click-fn click :sources-fn #(source first-pattern)
                :observe-fn (fn [_ _] (facts)) :fetch-run-record fetch
                :enact-fn (fn [f c]
                           (when (and stale? (= "click-2" (:click-id c)))
                             (enact f (assoc c :click-id "racer")))
                           (let [r (enact f c)] (swap! results conj r) r))}))]
    {:run run :click click :calls calls :results results :q0s q0s :interps interps :lane-options lane-options :joint-rankings joint-rankings :lane-rankings lane-rankings}))

(defn- receipt [record] (get-in record [:decision :selection-certificate :token-belief-input]))
(defn- stage [record] (get-in record [:decision :selection-certificate :token-belief-stage]))

(deftest ^:slow three-clicks-consume-the-published-posterior
  (isolated
   (fn [root]
     (let [{:keys [run calls results q0s]} (harness root {})
           f (run) records @calls enactments (mapv #(read! (:record-path %)) @results)]
       (is (= 3 (count records)) (pr-str (select-keys f [:status :clicks])))
       (is (= [:trajectory-start :temporal-posterior :temporal-posterior]
              (mapv (comp :conditioning-status receipt) records)))
       (is (= 3 (count @q0s)))
       (doseq [i [1 2]]
         (let [r (receipt (nth records i)) prior-record (nth enactments (dec i))
               posterior (get-in prior-record [:temporal-posterior :posterior])]
           (is (= (evidence/value-digest posterior)
                  (evidence/value-digest (:continuation-belief r))
                  (evidence/value-digest (nth @q0s i))))
           (is (= (select-keys (:temporal-receipt (nth @results (dec i))) [:record-path :digest])
                  (get-in r [:temporal-previous :publication])))
           (is (predecessor/valid-input? r (stage (nth records i))))))
       (is (predecessor/valid-input? (receipt (first records)) (stage (first records))))
       (is (= [:write-a :write-b :write-open]
              (mapv #(get-in % [:attempts 0 :pattern]) enactments)))
       (is (not= (get-in enactments [0 :temporal-posterior :prior])
                 (get-in enactments [0 :temporal-posterior :predicted-state])))
       (testing "replaying the first event cannot publish or change its record"
         (let [first-result (first @results) r (first enactments)
               before (slurp (:record-path first-result))
               replay (temporal/publish! (:record-path first-result) r
                                         (get-in r [:temporal-input :previous])
                                         (str (io/file root "trace")))]
           (is (= :event-already-consumed (get-in replay [:receipt :reason])))
           (is (= before (slurp (:record-path first-result))))))
       (testing "forged posterior, citation envelope, or replay status is invalid"
         (let [r (receipt (second records)) s (stage (second records))]
           (doseq [bad [(dissoc r :temporal-previous)
                        (assoc r :continuation-belief {#{} 1})
                        (assoc-in r [:inspection :temporal-previous :record :posterior] {#{} 1})
                        (assoc (receipt (first records)) :conditioning-status :temporal-posterior)]]
             (is (not (predecessor/valid-input? bad s))))))
       (testing "domain, model, absence, and old declarations retain initialization"
         (let [r (receipt (second records)) s (stage (second records)) prev (:temporal-previous r)]
           (doseq [[p expected] [[{:status :absent :reason :temporal-posterior} :temporal-previous-absent]
                                [(assoc prev :domain #{}) :domain-changed]
                                [(assoc prev :model-identity {:A "other" :B "other"}) :model-identity-changed]
                                [(assoc-in prev [:record :posterior] {#{} 1}) :temporal-record-mismatch]
                                [{:status :absent :reason :temporal-record-unreadable} :temporal-record-unreadable]]]
             (let [v (predecessor/input-receipt s (predecessor/inspect-trace nil {:flight {:temporal-previous p}})
                                               fixture/admission nil)]
               (is (= expected (:conditioning-status v)))
               (is (= (get-in s [:initialization :value]) (:continuation-belief v)))
               (is (predecessor/valid-input? v s))))
           (let [old (update-in s [:domain-inputs 0 :declaration :interpretations]
                                #(into {} (map (fn [[k v]] [k (dissoc v :model-identity)])) %))
                 v (predecessor/input-receipt old (:inspection r) fixture/admission nil)]
             (is (= :temporal-previous-unverifiable (:conditioning-status v))))))
       (println "CONSUMED-RECEIPT" (pr-str (receipt (second records))))
       (println "INITIALIZATION-RECEIPT" (pr-str (receipt (first records))))))))

(deftest ^:slow action-and-observation-controls-reach-the-next-ranker
  (let [next-q (fn [options]
                 (isolated
                  (fn [root]
                    (let [{:keys [run click q0s results]} (harness root options)
                          f (run)
                          bytes (slurp (:record-path (first @results)))]
                      ;; A false observation closes the flight for no progress.
                      ;; Still exercise its next-input courier/decision directly.
                      (when (= 1 (count @q0s)) (click (flight/judge-opts f {:wants [:a :b :open]})))
                      (is (= bytes (slurp (:record-path (first @results))))
                          "later selection cannot rewrite the published record")
                      (second @q0s)))))
        a (next-q {}) b (next-q {:first-pattern :write-b}) no-a (next-q {:outcome false})]
    (is (= 1 (get a #{[fixture/target :a]})))
    (is (= 1 (get b #{[fixture/target :b]})))
    (is (= 1 (get no-a #{})))
    (is (not= a b no-a))
    (println "CONSUMED-CONTROLS" (pr-str {:action-a a :action-b b :false-check no-a}))))

(deftest ^:slow stale-and-contradiction-initialize-the-following-selection
  (doseq [[options expected index] [[{:stale? true} :temporal-stale-predecessor 2]
                                  [{:theta 0} :temporal-contradiction 1]]]
    (isolated
     (fn [root]
       (let [{:keys [run calls results q0s]} (harness root options)
             _ (run) r (receipt (nth @calls index)) s (stage (nth @calls index))
             enacted (read! (:record-path (nth @results (dec index))))]
         (is (= expected (:conditioning-status r)))
         (is (= expected (get-in enacted [:temporal-receipt :reason])))
         (is (= (get-in s [:initialization :value]) (:continuation-belief r) (nth @q0s index)))
         (is (= :declared-initialization (:basis r)))
         (is (predecessor/valid-input? r s))
         (when (:stale? options)
           (is (= :temporal-posterior (:conditioning-status (receipt (second @calls))))))
         (when (= 0 (:theta options))
           (is (= :refused (get-in enacted [:temporal-posterior :status])))))))))

(deftest courier-omits-a-missing-enactment-and-retains-typed-absence
  (is (not (contains? (predecessor/inspect-trace {:temporal-previous {:basis :posterior}}
                                                {:flight {}}) :temporal-previous))
      "a trace annotation cannot replace the flight courier")
  (is (not (contains? (:flight (flight/judge-opts {:clicks []} {})) :temporal-previous)))
  (let [absence {:status :absent :reason :temporal-stale-predecessor :detail {:expected :old}}]
    (is (= absence (get-in (flight/judge-opts {:enactments [{:temporal-receipt absence}]} {})
                           [:flight :temporal-previous])))))

(deftest ^:slow changing-only-the-next-candidate-cannot-change-the-consumed-posterior
  (isolated
   (fn [root]
     (let [{:keys [run click calls q0s results]} (harness root {:max-clicks 1})
           f (run) opts (flight/judge-opts f {:wants [:a :b :open]})
           path (:record-path (first @results)) before (slurp path)
           b (click opts) c (click opts)]
       (is (= [:write-b] (get-in b [:chosen :precedence])))
       (is (= [:write-open] (get-in c [:chosen :precedence])))
       (is (= (get-in (read! path) [:temporal-posterior :posterior])
              (second @q0s) (nth @q0s 2)))
       (is (= (:temporal-previous (receipt (second @calls)))
              (:temporal-previous (receipt (nth @calls 2)))))
       (is (= before (slurp path)))))))


(deftest ^:slow token-rates-lane-zeta-published-and-replayed
  (isolated
   (fn [root]
     (binding [population/*dir* root]
       ;; Real admitted C3 population at the population fixture's pinned repo.
       (with-redefs [checks/repo-root "/home/joe/code"] (#'population/fill! 5)))
     (let [{:keys [run results calls]} (harness root {:max-clicks 2 :theta 1/4
                                                    :observation-labels-path (str (io/file root "labels.edn"))})
           _ (run)
           records (mapv #(read! (:record-path %)) @results)
           posterior (:zeta-posterior (last records))
           sc (get-in (first @calls) [:decision :selection-certificate :token-rate-lanes fixture/target])]
       (is (= 2 (count records)))
       (println "BZ-PUBLISHED" (pr-str posterior))
       (is (= :posterior (:basis posterior)))
       (is (= :token-rates-lane (:scope posterior)))
       (is (= #{ {:false-neg 1/12 :false-pos 1/12}}
              (set (vals (get-in sc [:precision-model :rates])))))
       (is (= (:rates-provenance sc) (get-in (first records) [:zeta-likelihood :rates-provenance])))
       (is (= posterior (zeta/trajectory-posterior records)))
       (is (= (:zeta-mean (:zeta-posterior (first records))) (:evaluated-at-zeta posterior)))
       (is (= 1 (:beta-prior posterior)))
       (is (= 2 (:trials posterior)))
       (is (= (get-in (first @calls) [:decision :selection-certificate :precision-family])
              (:precision-family (first records))))
       (is (= :rates-provenance-missing
              (:reason (zeta/retain-lane {} fixture/target (:attempts (first records))))))
       (is (= :rates-provenance-missing
              (:reason (zeta/trajectory-posterior (update records 0 dissoc :zeta-likelihood)))))
       (is (= :ok (get-in (last records) [:temporal-posterior :status])))))))


(deftest ^:slow third-click-tempers-only-the-admitted-token-lane
  (isolated
   (fn [root]
     (binding [population/*dir* root]
       (with-redefs [checks/repo-root "/home/joe/code"] (#'population/fill! 5)))
     (let [{:keys [run results calls lane-options joint-rankings lane-rankings]}
           (harness root {:theta 1/4 :observation-labels-path (str (io/file root "labels.edn"))})
           finished (run)
           records (mapv #(read! (:record-path %)) @results)
           published (:zeta-posterior (second records))
           sc #(get-in % [:decision :selection-certificate :token-rate-lanes fixture/target])
           third-sc (sc (nth @calls 2))
           effective (lprec/tempered-rates (get-in third-sc [:precision-model :rates]) (:zeta-mean published))
           courier (flight/judge-opts finished {:wants [:a :b :open]})]
       (println "BZ-CONSUMED" (pr-str (select-keys third-sc [:zeta :zeta-basis :precision-model])))
       (is (= 3 (count records)))
       (is (= (:zeta-mean published) (:zeta third-sc)))
       (is (= (select-keys published [:basis :trajectory-digest :zeta-mean]) (:zeta-basis third-sc)))
       (is (= effective (get-in third-sc [:precision-model :tempered-rates])))
       (is (= (:zeta-basis third-sc) (get-in (first (last @lane-rankings)) [:certificate :zeta-basis])))
       (is (= :learned-posterior-applied (get-in (first (last @lane-rankings)) [:certificate :zeta-status])))
       (let [refusal {:status :absent :reason :nonpositive-beta-post}]
         (is (= refusal (:zeta-posterior (temporal/envelope (assoc (last records) :zeta-posterior refusal))))))
       (is (not= effective (get-in third-sc [:precision-model :rates])))
       (is (not (contains? (first @lane-options) :zeta)))
       (is (= :prior-no-trials (get-in (sc (first @calls)) [:zeta-basis :basis])))
       (is (= 1 (get-in (sc (first @calls)) [:zeta-basis :beta-prior])))
       (is (every? #(not (contains? (:opts %) :zeta)) @joint-rankings))
       (is (= {:absent :class-emission-not-tempered}
              (get-in (nth @calls 2) [:decision :selection-certificate :precision-family :zeta-basis])))
       (is (= (:zeta-posterior (last records)) (get-in courier [:flight :temporal-previous :zeta-posterior])))
       ))))

(deftest ^:slow rejected-domain-never-lifts-a-published-zeta
  (isolated
   (fn [root]
     (binding [population/*dir* root]
       (with-redefs [checks/repo-root "/home/joe/code"] (#'population/fill! 5)))
     (let [{:keys [run calls click lane-options]}
           (harness root {:max-clicks 2 :theta 1/4
                          :observation-labels-path (str (io/file root "labels.edn"))})
           f (run)
           courier (flight/judge-opts f {:wants [:a :b :open]})]
       ;; The third want is still open; rejection reaches the actual lane,
       ;; rather than an unrelated all-wants-complete early return.
       (is (= :posterior (get-in courier [:flight :temporal-previous :zeta-posterior :basis])))
       (click (update-in courier [:flight :temporal-previous :domain] conj [fixture/target :foreign]))
       (is (not (contains? (last @lane-options) :zeta)))
       (is (= :domain-changed (:conditioning-status (receipt (last @calls)))))
       (is (= {:absent :domain-changed}
              (get-in (last @calls) [:decision :selection-certificate :token-rate-lanes fixture/target :zeta-basis])))))))
