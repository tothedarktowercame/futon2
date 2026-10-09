(ns futon2.aif.scoring-cache-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.efe :as efe]
            [futon2.aif.observation-model-route-test :as route])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- setup []
  (let [dir (Files/createTempDirectory "wm-score-cache-" (make-array FileAttribute 0))
        path (str (.resolve dir "global-rank.edn"))
        candidates (mapv #(assoc % :id (keyword (str "C" %2)))
                         (take 4 (cycle (:candidates route/fixture))) (range 4))
        opts (assoc (route/fixture-opts :independent-judgement)
                    :scoring-cache? true :scoring-cache-path path
                    :scoring-cache-top-k 2 :scoring-cache-refresh-count 1)]
    {:dir dir :path path :candidates candidates :opts opts
     :state {:cascade-belief (:q0 route/fixture)}}))
(defn- run [{:keys [state candidates opts]}] (efe/rank-actions state candidates opts))
(defn- statuses [xs] (mapv (comp :status :cache) xs))

(deftest cache-invalidates-and-refreshes-deterministically
  (let [{:keys [path candidates opts state] :as x} (setup)
        first-run (run x) second-run (run x)
        edited (assoc x :opts (assoc opts :cascade-spec (assoc (:cascade-spec opts) :mu 2)))
        edited-run (run edited)]
    (is (= [:fresh :fresh :fresh :fresh] (statuses first-run)))
    (is (= 3 (count (filter #{:fresh} (statuses second-run)))))
    (is (= 4 (count (filter #{:fresh} (statuses edited-run)))))
    (is (= (mapv :controller-score (sort-by :cascade-id first-run))
           (mapv :controller-score
                 (sort-by :cascade-id
                          (efe/rank-actions state candidates (assoc opts :scoring-cache? false))))))
    (is (= 2 (get-in (meta second-run) [:cascade-scoring :cache-policy :top-k])))
    (is (.isFile (java.io.File. path)))))

(deftest corrupt-cache-is-typed-cold-start-and-atomic-write-leaves-no-temp
  (let [x (setup)]
    (run x)
    (spit (:path x) "{:schema :wm-global-scoring-cache-v1, :entries {")
    (let [again (run x)]
      (is (every? #{:fresh} (statuses again)))
      (is (every? #(= :corrupt-cache (get-in % [:cache :cold-start-reason])) again))
      (is (not-any? #(re-find #"\.tmp-" (.getName %)) (.listFiles (.toFile (:dir x)))))
      (is (= :wm-global-scoring-cache-v1 (:schema (edn/read-string (slurp (:path x)))))))))

(deftest stale-entries-are-refreshed-with-a-declared-age-bound
  (let [base (setup)
        x (assoc base :opts (assoc (:opts base) :scoring-cache-top-k 1
                                   :scoring-cache-refresh-count 1))]
    (dotimes [_ 7] (run x))
    (let [last-run (run x) policy (:cache-policy (edn/read-string (slurp (:path x))))]
      (is (= 1 (:refresh-width policy)))
      (is (= 4 (:max-age-clicks policy)))
      (is (every? #(<= (long (get-in % [:cache :age])) (:max-age-clicks policy)) last-run)))))

(deftest parallelism-is-byte-and-choice-deterministic
  (let [a (setup) b (setup)
        ra (run (assoc a :opts (assoc (:opts a) :scoring-parallelism 1)))
        rb (run (assoc b :opts (assoc (:opts b) :scoring-parallelism 16)))]
    (is (= (slurp (:path a)) (slurp (:path b))))
    (is (= (mapv :cascade-id ra) (mapv :cascade-id rb)))
    (is (= (mapv :rank ra) (mapv :rank rb)))))
