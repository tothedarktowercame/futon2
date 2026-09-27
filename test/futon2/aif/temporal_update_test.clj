(ns futon2.aif.temporal-update-test
  "Compute/publish only: subsequent input receipts are supplied explicitly here;
   this test does not claim selection consumes the posterior yet. Real git and
   stamped checks; only execution transport is replaced by local artifact work."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight-runner :as runner]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.lane-futility :as lane]
            [futon2.aif.temporal-input-test :as fixture]
            [futon2.aif.temporal-update :as temporal])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- point [state] (assoc (zipmap fixture/states (repeat 0)) state 1))

(defn- read! [path] (edn/read-string (slurp path)))
(defn- git! [repo & args]
  (let [{:keys [exit out err]} (apply sh/sh "git" "-C" (str repo) args)]
    (when-not (zero? exit) (throw (ex-info "git fixture" {:err err})))
    (str/trim out)))

(defn- harness [root theta]
  (let [{:keys [repo interps check receipt stage]} (fixture/fixture root)
        interps (into {} (map (fn [[k v]] [k (assoc v :theta theta :guard {:needs #{} :forbids #{}})])) interps)
        flight (atom {:flight/id "publication-fixture" :target fixture/target :enactments []})
        observed (atom true)
        enact (runner/enact-fn
               {:interpretations (constantly interps) :check-fn check
                :trace-dir (str (io/file root "trace"))
                :record-dir (str (io/file root "published"))
                :fetch-run-record #(read! (io/file root (str % ".edn")))
                :dispatch-step!
                (fn [{:keys [interpretation]}]
                  (let [t (first (:produces interpretation))]
                    (when @observed (spit (io/file repo (name t)) (str t))))
                  (git! repo "add" ".")
                  (git! repo "commit" "--allow-empty" "-qm" "execute")
                  (let [t (first (:produces interpretation)) sha (git! repo "rev-parse" "HEAD")]
                    {:commit sha :produced t :check {:class :C3 :repo "artifacts" :sha sha :path (name t)}}))})
        execute (fn [click-id pattern previous outcome]
                  (reset! observed outcome)
                  (let [input (cond-> (assoc receipt :occurrence-id (if previous (str "selection-" click-id)
                                                                      (:occurrence-id receipt)))
                                previous (assoc :temporal-previous previous))
                        run-record {:decision {:selection-certificate
                                               {:token-belief-input input :token-belief-stage stage}}}
                        _ (spit (io/file root (str click-id ".edn")) (pr-str run-record))
                        result (enact @flight {:click-id click-id :chosen {:id :menu :precedence [pattern]}})]
                    (swap! flight update :enactments conj (select-keys result [:record-path :temporal-receipt]))
                    result))]
    {:execute execute :flight flight}))

(defn- isolated [f]
  (let [root (.toFile (Files/createTempDirectory "temporal-update-" (make-array FileAttribute 0)))]
    (try (with-redefs [checks/repo-root (str root)] (f root))
         (finally (doseq [file (reverse (file-seq root))] (io/delete-file file true))))))

(deftest ^:slow publish-three-executions-and-refuse-replay-and-stale-cursors
  (isolated
   (fn [root]
     (let [{:keys [execute]} (harness root 1/2)
           a (execute "a" :write-a nil true)
           record-a (read! (:record-path a))
           previous-a (temporal/read-receipt (:temporal-receipt a))
           b (execute "b" :write-b previous-a true)
           record-b (read! (:record-path b))
           previous-b (temporal/read-receipt (:temporal-receipt b))
           c (execute "c" :write-a previous-b true)
           record-c (read! (:record-path c))]
       (is (= [:published :published :published]
              (mapv #(get-in % [:temporal-receipt :status]) [a b c])))
       (is (= [2 3 4] (mapv #(count (get-in % [:temporal-cursor :consumed-event-ids]))
                            [record-a record-b record-c])))
       (is (= (get-in record-a [:temporal-posterior :posterior])
              (get-in record-b [:temporal-posterior :prior])))
       (is (= (point #{[fixture/target :a]}) (get-in record-a [:temporal-posterior :posterior])))
       (is (= (point fixture/domain) (get-in record-b [:temporal-posterior :posterior])))
       (is (= record-a (read! (:record-path a))) "later selected candidates never rewrite the prior record")
       (is (= :write-a (get-in record-b [:temporal-input :previous :record :model :executed-action])))
       (is (= "selection-a" (get-in record-a [:temporal-posterior :model :occurrence-id])))
       (is (= "a" (get-in record-a [:temporal-posterior :model :click-id])))
       (is (not= (get-in record-a [:temporal-posterior :prior])
                 (get-in record-a [:temporal-posterior :predicted-state])))
       (testing "multiple attempts cannot silently select one transition"
         (let [r (temporal/publish! (io/file root "published" "multiple.edn")
                                    (assoc record-c :attempts (vec (concat (:attempts record-c) (:attempts record-c))))
                                    (temporal/envelope record-c) (str (io/file root "trace")))]
           (is (= :multiple-temporal-steps (get-in r [:receipt :reason])))
           (is (not (contains? (:record r) :temporal-posterior)))))
       (testing "event replay cannot rewrite the file or advance again"
         (let [bytes (slurp (:record-path a))
               replay (temporal/publish! (:record-path a) record-a
                                         (get-in record-a [:temporal-input :previous]) (str (io/file root "trace")))]
           (is (= :event-already-consumed (get-in replay [:receipt :reason])))
           (is (= bytes (slurp (:record-path a))))))
       (testing "the real append lock cannot be bypassed by an in-JVM publisher"
         (let [path (io/file root "published" "busy.edn")
               busy (lane/with-index-lock
                     (str (io/file root "trace"))
                     #(temporal/publish! path (dissoc record-c :temporal-posterior :temporal-cursor :temporal-input)
                                         previous-b (str (io/file root "trace"))))]
           (is (= :temporal-publication-busy (get-in busy [:receipt :reason])))
           (is (not (contains? (read! path) :temporal-posterior)))))
       (testing "an old cursor writes absence, retaining the current chain"
         (let [stale (execute "stale" :write-b previous-a true)
               r (read! (:record-path stale))]
           (is (= :temporal-stale-predecessor (get-in r [:temporal-receipt :reason])))
           (is (= (:consumed-event-ids previous-a)
                  (get-in r [:temporal-receipt :detail :expected :consumed-event-ids])))
           (is (= (:temporal-cursor record-c) (get-in r [:temporal-receipt :detail :actual])))
           (is (not (contains? r :temporal-posterior)))
           (println "TEMPORAL-STALE" (pr-str (:temporal-receipt r)))))
       (is (= :temporal-record-digest-mismatch
              (:reason (temporal/read-receipt (assoc (:temporal-receipt a) :digest "wrong")))))
       (println "TEMPORAL-PUBLISHED" (pr-str (select-keys record-a [:temporal-receipt :temporal-posterior :temporal-cursor])))))))

(deftest ^:slow executed-action-and-false-observation-change-the-computed-posterior
  (let [posterior (fn [pattern outcome]
                    (isolated (fn [root]
                                (let [result ((:execute (harness root 1/2)) "a" pattern nil outcome)
                                      r (read! (:record-path result))]
                                  (is (= :published (get-in result [:temporal-receipt :status])))
                                  (is (= outcome (get-in r [:attempts 0 :success])))
                                  (is (true? (get-in r [:attempts 0 :executed])))
                                  (get-in r [:temporal-posterior :posterior])))))
        a (posterior :write-a true) b (posterior :write-b true) no-a (posterior :write-a false)]
    (is (= (point #{[fixture/target :a]}) a))
    (is (= (point #{[fixture/target :b]}) b))
    (is (= (point #{}) no-a))
    (is (not= a b no-a))
    (println "TEMPORAL-CONTROLS" (pr-str {:action-a a :action-b b :false-check no-a}))))

(deftest ^:slow contradiction-is-retained-and-cannot-restart-the-flight
  (isolated
   (fn [root]
     (let [{:keys [execute]} (harness root 0)
           impossible (execute "a" :write-a nil true)
           r (read! (:record-path impossible))
           next-attempt (execute "b" :write-b (get-in r [:temporal-input :previous]) true)]
       (is (= :refused (get-in r [:temporal-posterior :status])))
       (is (= :temporal-contradiction (get-in r [:temporal-receipt :reason])))
       (is (= :temporal-contradiction (:reason (temporal/read-receipt (:temporal-receipt impossible)))))
       (is (not (contains? r :temporal-cursor)))
       (is (= :temporal-contradiction (get-in next-attempt [:temporal-receipt :reason])))
       (println "TEMPORAL-CONTRADICTION" (pr-str (:temporal-receipt r)))))))
