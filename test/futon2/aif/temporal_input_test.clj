(ns futon2.aif.temporal-input-test
  "Real isolated git executions/checks; filtering runs only inside this test."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.cascade-model-manifest :as manifest]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.exact-belief-adapter :as exact]
            [futon2.aif.flight-runner :as flight]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.scoring-input-receipts :as receipts]
            [futon2.aif.temporal-input :as temporal]
            [futon2.aif.token-belief-carry :as carry]
            [futon2.aif.token-belief-predecessor :as predecessor]
            [futon2.aif.token-initialization-policy :as initialization])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def target "M-temporal-artifacts")
(def domain #{[target :a] [target :b]})
(def states [#{} #{[target :a]} #{[target :b]} domain])
(def admission {:authority task/authority :scope task/scope :status :refused :kind :no-predecessor})

(defn- git! [repo & args]
  (let [{:keys [exit out err]} (apply sh/sh "git" "-C" (str repo) args)]
    (when-not (zero? exit) (throw (ex-info "Isolated git fixture failed" {:args args :err err})))
    (str/trim out)))

(defn- read-edn [path] (edn/read-string (slurp path)))
(defn- save! [root name value]
  (let [f (io/file root name)] (spit f (pr-str value)) (read-edn f)))

(defn fixture
  "Isolated artifacts, real initialization and stamped C3 checks. Optional
   pattern declarations are fixture inputs, retained by the real executor."
  [root & [pattern-declarations]]
  (let [repo (io/file root "artifacts")
        _ (.mkdirs repo)
        _ (git! repo "init" "-q")
        _ (git! repo "config" "user.name" "Temporal fixture")
        _ (git! repo "config" "user.email" "temporal@example.invalid")
        _ (git! repo "commit" "--allow-empty" "-qm" "declared start")
        start-sha (git! repo "rev-parse" "HEAD")
        descriptor (checks/loaded-check :C3)
        identity {:A (str (:mechanism-name descriptor) "@" (:mechanism-sha descriptor))
                  :B {:authority 'futon2.aif.cascade-model-manifest/pattern-kernel
                      :revision :declared-add-only-v1}}
        interps {:write-a {:guard {:needs #{} :forbids #{}} :produces #{:a}
                          :model-identity identity :domain domain}
                 :write-b {:guard {:needs #{:a} :forbids #{}} :produces #{:b}
                          :model-identity identity :domain domain}}
        interps (merge-with merge interps pattern-declarations)
        locator (fn [token sha] {:class :C3 :repo "artifacts" :sha sha :path (name token)})
        check (fn [loc] (get-in (checks/observe {:token loc}) [:results :token]))
        initial-observation (checks/observe (into {} (for [t [:a :b]] [t (locator t start-sha)])))
        facts (into {} (map (fn [[t r]] [t (:observed r)]) (:results initial-observation)))
        problem {:target target :cascade-problem {:facts facts :want #{:b} :interpretations interps}}
        init (receipts/initial-belief [problem])
        stage (carry/stage init (carry/domain-inputs [problem]) nil
                           {:occurrence-id "selection-a"
                            :observation-initialization {target {:policy initialization/disabled}}})
        receipt (predecessor/input-receipt stage (predecessor/inspect-trace nil) admission nil)
        initial-id (evidence/value-digest ["initial" start-sha initial-observation])
        previous {:basis :declared-initialization :trajectory-start? true
                  :record receipt :stage stage :initialization-authority (:initialization receipt)
                  :occurrence-id "selection-a" :domain domain :model-identity identity
                  :initial-event-id initial-id :consumed-event-ids #{initial-id}
                  :consumed-at {:occurrence-id "selection-a" :click-id "click-a"
                                :citation (evidence/value-digest receipt)}}
        executed (atom [])
        fail-check? (atom false)
        enact (flight/enact-fn
               {:interpretations (constantly interps) :check-fn check
                :record-dir (str (io/file root "enactments"))
                :dispatch-step!
                (fn [{:keys [pattern interpretation]}]
                  ;; Implements exactly one declared primitive, never the selected
                  ;; candidate's other proposed steps. The real checker reads git.
                  (let [token (first (:produces interpretation))]
                    (swap! executed conj pattern)
                    (spit (io/file repo (name token)) (str token))
                    (git! repo "add" (name token))
                    (git! repo "commit" "--allow-empty" "-qm" (name pattern))
                    (let [sha (git! repo "rev-parse" "HEAD")]
                      {:commit sha :produced token :check (cond-> (locator token sha)
                                                               @fail-check? (assoc :path "missing"))})))})
        execute (fn [click-id pattern & [fail?]]
                  (reset! fail-check? (boolean fail?))
                  (let [r (enact {:flight/id "temporal-fixture" :target target}
                                 {:click-id click-id :chosen {:id :selected-menu-entry
                                                            :candidate :not-an-executed-pattern
                                                            :precedence [pattern]}})]
                    (first (:attempts (read-edn (:record-path r))))))]
    {:previous previous :stage stage :receipt receipt :execute execute :executed executed
     :repo repo :interps interps :check check}))

(defn- posterior-after [input next-occurrence]
  (let [previous (:previous input)
        q (case (:basis previous)
            :declared-initialization (get-in previous [:record :continuation-belief])
            :posterior (get-in previous [:record :posterior]))
        pattern (get-in input [:enacted :transition])
        token [(get-in input [:enacted :target]) (get-in input [:enacted :produced])]
        result (exact/exact-update states #(hash-map (contains? % token) 1)
                                  #(manifest/pattern-kernel pattern %) true q
                                  {:model-identity (:model-identity previous) :domain domain
                                   :occurrence-id (get-in previous [:consumed-at :occurrence-id])
                                   :executed-action (get-in input [:enacted :pattern])
                                   :input-event (:consumed-event-id input)})]
    {:basis :posterior :record result :occurrence-id (get-in result [:model :occurrence-id])
     :consumed-at {:occurrence-id (str "selection-" next-occurrence) :click-id next-occurrence
                   :citation (evidence/value-digest result)}
     :domain domain :model-identity (:model-identity previous)
     :initial-event-id (:initial-event-id previous)
     :consumed-event-ids (conj (:consumed-event-ids previous) (:consumed-event-id input))}))

(deftest ^:slow two-executions-produce-replayable-temporal-inputs
  (let [root (.toFile (Files/createTempDirectory "temporal-input-" (make-array FileAttribute 0)))]
    (try
      (with-redefs [checks/repo-root (str root)]
        (let [{:keys [previous stage receipt execute executed interps check]} (fixture root)
              a (execute "click-a" :write-a)
              input-a (save! root "input-a.edn" (temporal/temporal-input previous a (:check a)))
              next-previous (save! root "posterior-a.edn" (posterior-after input-a "click-b"))
              b (execute "click-b" :write-b)
              input-b (save! root "input-b.edn" (temporal/temporal-input next-previous b (:check b)))]
          (is (= [:write-a :write-b] @executed))
          (is (= [:admitted :admitted] (mapv :status [input-a input-b])))
          (is (= :declared-initialization (get-in input-a [:previous :basis])))
          (is (= (:initialization receipt) (get-in input-a [:previous :initialization-authority])))
          (is (= :ok (get-in input-b [:previous :record :status])))
          (is (= :write-a (get-in input-b [:previous :record :model :executed-action]))
              "the previous posterior names the first executed action")
          (is (= :write-b (get-in input-b [:enacted :pattern])))
          (is (= (:record next-previous) (get-in input-b [:previous :record])))
          (is (= 1 (get-in next-previous [:record :posterior #{[target :a]}])))
          (doseq [input [input-a input-b]]
            (is (not= {#{} 1} (manifest/pattern-kernel (get-in input [:enacted :transition]) #{})))
            (is (= (:consumed-event-id input)
                   (evidence/value-digest [(get-in input [:enacted :click-id])
                                           (get-in input [:enacted :n])
                                           (select-keys (:observed input) [:class :repo :path :decl :entry :bundle-path])
                                           (get-in input [:observed :result :evidence :resolved-sha])]))))
          (testing "explicit cursor stops replay, without claiming it was published"
            (let [replay (temporal/temporal-input
                          (update next-previous :consumed-event-ids conj (:consumed-event-id input-b))
                          b (:check b))]
              (is (= :event-already-consumed (:reason replay)))
              (is (= (:consumed-event-id input-b) (get-in replay [:detail :consumed-event-id])))
              (let [alias-check (assoc (:check b) :sha "HEAD")]
                (is (= :event-already-consumed
                       (:reason (temporal/temporal-input
                                 (update next-previous :consumed-event-ids conj (:consumed-event-id input-b))
                                 (assoc b :check alias-check) alias-check)))))
              (println "TEMPORAL-ABSENT" (pr-str replay))))
          (testing "same observed value at a new occurrence is a distinct event"
            (let [prior-c (posterior-after input-b "click-c")
                  c (execute "click-c" :write-b)
                  input-c (temporal/temporal-input prior-c c (:check c))]
              (is (= :admitted (:status input-c)))
              (is (= true (get-in input-b [:observed :result :observed])
                     (get-in input-c [:observed :result :observed])))
              (is (not= (:consumed-event-id input-b) (:consumed-event-id input-c)))))
          (doseq [[p step obs reason]
                  [[(dissoc next-previous :model-identity) b (:check b) :no-model-identity]
                   [(dissoc next-previous :basis) b (:check b) :invalid-previous-posterior]
                   [(dissoc previous :trajectory-start?) a (:check a) :invalid-previous-posterior]
                   [(dissoc next-previous :consumed-event-ids) b (:check b) :no-consumed-event-cursor]
                   [(assoc-in next-previous [:record :posterior] {#{} 1}) b (:check b) :invalid-previous-posterior]
                   [(assoc next-previous :record receipt) b (:check b) :invalid-previous-posterior]
                   [next-previous (dissoc b :click-id) (:check b) :execution-not-linked]
                   [next-previous (assoc b :failed :dispatch-failed) (:check b) :execution-not-linked]
                   [next-previous (dissoc b :interpretation) (:check b) :transition-not-determinable]
                   [next-previous (dissoc b :model-identity) (:check b) :model-identity-mismatch]
                   [next-previous (assoc b :domain #{}) (:check b) :domain-mismatch]
                   [next-previous b nil :no-observed-outcome]
                   [next-previous (assoc b :commit "another-revision") (:check b) :observation-not-linked]]]
            (is (= reason (:reason (temporal/temporal-input p step obs))) (str reason)))
          (testing "a false real check remains an observation of a committed execution"
            (let [failed (execute "click-false" :write-b true)]
              (is (false? (get-in failed [:check :result :observed])))
              (is (false? (:success failed)))
              (is (= :admitted
                     (:status (temporal/temporal-input
                               (assoc-in next-previous [:consumed-at :click-id] "click-false")
                               failed (:check failed)))))))
          (testing "a real executor failure cannot become an enacted temporal input"
            (let [enact (flight/enact-fn
                         {:interpretations (constantly interps) :check-fn check
                          :record-dir (str (io/file root "failed-execution"))
                          :dispatch-step! (fn [_] {:failed {:reason :fixture-transport-failed}})})
                  result (enact {:flight/id "failed" :target target}
                                {:click-id "failed" :chosen {:id :failed :precedence [:write-a]}})
                  attempt (first (:attempts (read-edn (:record-path result))))]
              (is (false? (:executed attempt)))
              (is (false? (:success attempt)))
              (is (= :execution-not-linked
                     (:reason (temporal/temporal-input
                               (assoc-in next-previous [:consumed-at :click-id] "failed")
                               attempt (:check attempt)))))))
          (testing "receipt annotation never changes the q consumed by selection"
            (let [inspection (predecessor/inspect-trace {:temporal-previous next-previous :temporal-enactment b})
                  annotated (predecessor/input-receipt stage inspection admission nil)]
              (is (= input-b (:temporal-input annotated)))
              (is (= (:continuation-belief receipt) (:continuation-belief annotated)))
              (is (predecessor/valid-input? annotated stage))
              (is (predecessor/valid-input? (dissoc annotated :temporal-input) stage))
              (is (= :no-enactment-supplied (get-in receipt [:temporal-input :reason])))))
          (println "TEMPORAL-ADMITTED" (pr-str input-b))))
      (finally (doseq [f (reverse (file-seq root))] (io/delete-file f true))))))

(deftest unavailable-transition-is-not-identity
  (doseq [declaration [nil {:produces #{:a}} {:guard {:needs nil :forbids #{}} :produces #{:a}}]]
    (is (= :transition-not-determinable
           (:reason (temporal/transition-reading target :write-a declaration))))))
