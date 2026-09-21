(ns futon2.report.cascade-habit-accumulation-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.cascade-habit-reinforcement :as reinforcement]
            [futon2.aif.cascade-habit-reinforcement-test :as outcome-fixture]
            [futon2.aif.cascade-prior :as prior]
            [futon2.aif.cascade-policy :as cascade]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.policy :as policy]
            [futon2.report.cascade-decision-test :as fixture]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.locator-fixtures :as locators]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn- with-store [f]
  (let [dir (.toFile (Files/createTempDirectory "habit-replay-"
                                                (make-array FileAttribute 0)))]
    (try (f (str (io/file dir "cascade-prior.edn")))
         (finally (doseq [file (reverse (file-seq dir))] (.delete file))))))

(defn- production-menu []
  ;; Read the declared production IDs and interpretation, not a string-only
  ;; facsimile. No observation jobs or real runner are invoked by this replay.
  (let [d (edn/read-string (slurp (io/resource "wm/cascade-sources/M-wm-08-external-f2.edn")))
        c (first (:candidates d))
        base {:kind :cascade-candidate :target (:target d)
              :interpretation-receipts (:interpretation-receipts d)}]
    [(assoc base :id :empty :precedence []
            :construction-receipt {:kind :construction-receipt :construction :empty-cascade})
     (assoc base :id :work :construction-receipt (:construction-receipt c)
            :precedence (mapv #(cascade/token-interpretation % (get (:patterns d) %))
                              (:precedence c)))]))

(deftest identities-include-empty-and-preserve-types
  (let [base {:mission :m :semilattice {}}
        key-of #(prior/policy-key (assoc base :shown %))]
    (is (some? (key-of [])))
    (is (= [:a/b] (nth (key-of [:a/b]) 2)))
    (is (not= (key-of [:a/b]) (key-of ["a/b"])))
    (is (not= (key-of [:a/b]) (key-of [:c/b])))
    (is (nil? (prior/policy-key base)))
    (doseq [bad [nil '(:a/b) [1] [nil] [:a/b 1]]]
      (is (nil? (key-of bad))))
    (is (= :mixed-pattern-id-types
           (try (key-of [:a/b "a/b"])
                (catch clojure.lang.ExceptionInfo e
                  (get-in (ex-data e) [:refusal :kind])))))))

(deftest offline-observed-outcomes-accumulate-with-identical-selection-bytes
  (with-store
    (fn [path]
      (let [work (second (production-menu))
            other (-> work (assoc :target "M-other" :id :other)
                      (assoc-in [:precedence 0 :id] :test/other-acting-pattern))
            menu [other work]
            ;; Two genuinely eligible nonempty choices. Alternate which has lower G;
            ;; beta != 1 and nonzero F ensure these are not neutral-only arms.
            fields (fn [work?]
                     [{:action other :controller-score (if work? 100 0) :f 0.2}
                      {:action work :controller-score (if work? 0 100) :f 0.7}])
            ticks [true true false true true false]
            select (fn [work? opts]
                     {:decision (policy/select-action-cascades (fields work?) opts)})
            opts {:beta 2 :cascade-habit-path path}
            before (atom [])
            after (with-redefs [wm/cascade-decision select]
                    (mapv (fn [tick]
                            (let [_ (swap! before conj (pr-str (select tick opts)))
                                  r (wm/select-and-record-cascade! tick opts)]
                              (reinforcement/close! path (:decision r) :grounded-change
                                                    (outcome-fixture/comparison (:decision r) true))
                              (pr-str r))) ticks))
            views (mapv habit/policy-view menu)
            keys (mapv prior/policy-key views)
            state (habit/read-state path)
            logs (prior/log-priors state views)
            bytes-at-six (.length (io/file path))]
        (is (= @before after))
        (is (.isFile (io/file path)))
        (is (= 6 (:samples state)))
        (is (= (zipmap keys [2 4]) (:counts state)))
        (is (= "M-other" (second (first keys))))
        (is (= [:cascade-construction/run-it-on-a-real-case] (nth (second keys) 2)))
        (is (= (zipmap keys (repeat reinforcement/rule-id)) (:reinforcement-bases state)))
        (is (< (Math/abs (- (first logs) (Math/log 0.375))) 1e-12))
        (is (< (Math/abs (- (second logs) (Math/log 0.625))) 1e-12))
        ;; Re-open/fold/write every time: no in-memory state can hide a failed
        ;; persistence or restart. Repetition grows digits, not entry count.
        (dotimes [_ 100]
          (let [decision (:decision (select true opts))]
            (reinforcement/close! path decision :grounded-change
                                  (outcome-fixture/comparison decision true))))
        (let [grown (habit/read-state path)]
          (is (= 106 (:samples grown)))
          (is (= 2 (count (:counts grown))))
          (println "HABIT-ACCUMULATION-RECEIPT"
                   (pr-str {:scope :offline-outcome-replay :ticks 6
                            :decision-byte-comparisons 6 :counts (:counts state)
                            :log-priors logs :probabilities (mapv #(Math/exp %) logs)
                            :bytes-at-six bytes-at-six :bytes-at-106 (.length (io/file path))
                            :classes-at-106 (count (:counts grown)) :clicks 0})))))))

(deftest actual-cascade-decision-is-recorded-unchanged
  (with-store
    (fn [path]
      (let [assembled (problems/assemble
                       {:targets [fixture/tick-1-target]
                        :sources (locators/locate-all fixture/tick-1-sources)})
            ;; The focus receipt (improve-7a) stamps wall-clock :as-of; pin it.
            opts (assoc fixture/live-c-opts :cascade-habit-path path
                        :focus-as-of "2026-09-21T18:00:00Z")
            before (wm/cascade-decision assembled opts)
            after (wm/select-and-record-cascade! assembled opts)]
        ;; Lane route telemetry includes wall-clock timestamps on each run.
        ;; Compare the complete live decision, including its certificate.
        (is (= (pr-str (:decision before)) (pr-str (:decision after))))
        (is (= 0 (:samples (habit/read-state path))))
        (is (= {} (:counts (habit/read-state path))))))))

(deftest absent-and-unconstructible-actions-do-not-write
  (with-store
    (fn [path]
      (doseq [decision [{:status :abstained}
                        {:action (dissoc (second (production-menu)) :construction-receipt)}]]
        (is (identical? decision (habit/record-selection! path decision))))
      (is (not (.exists (io/file path)))))))

(deftest malformed-history-is-not-overwritten
  (with-store
    (fn [path]
      (spit path "{:version :invalid}")
      (is (thrown? clojure.lang.ExceptionInfo
                   (habit/record-selection! path {:action (second (production-menu))})))
      (is (= "{:version :invalid}" (slurp path))))))
