(ns futon2.aif.declared-preference-scales-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-sources :as sources]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.live-c :as live-c]
            [futon2.report.war-machine :as wm]))

(def target "M-expressions-of-interest")
(defn declaration []
  (edn/read-string (slurp (io/resource "wm/cascade-sources/M-expressions-of-interest.edn"))))

(defn load-one [d]
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "declared-scales" (make-array java.nio.file.attribute.FileAttribute 0)))
        file (io/file dir "source.edn")]
    (try (spit file (pr-str d)) (sources/load-declared (str dir))
         (finally (io/delete-file file true) (io/delete-file dir true)))))

(defn spec-from [d]
  (let [loaded (load-one d)
        assembled (problems/assemble
                   {:targets [target]
                    :sources (assoc (sources/with-context-fn loaded) :horizon-steps 2)})
        _ (is (= (get-in d [:lam :value])
                 (get-in assembled [:problems 0 :cascade-problem :cascade-spec :lam])))
        want (set (map #(vector target %) (:want d)))
        derived {:want #{:closed/M-expressions-of-interest}
                 :weights {:closed/M-expressions-of-interest 1}
                 :signature "test"}
        spec (live-c/cascade-spec derived want want
                                  (live-c/family-scales (:problems assembled)))]
    (wm/merge-live-cascade-spec want spec)))

(deftest declared-scales-reach-production-spec-and-weights
  (let [d (declaration)
        one (spec-from d)
        two (spec-from (assoc-in d [:lam :value] 2))
        mu (spec-from (assoc-in d [:mu :value] 3))
        outcome #{[target :change-authored-and-bound]}]
    (is (= 1 (:lam one)))
    (is (= 0 (:mu one)))
    (is (= 2 (:lam two)))
    (is (= 3 (:mu mu)))
    (is (= 1 (reduce + (vals (:weights one)))))
    (is (= 2 (reduce + (vals (:weights two)))))
    (is (not= ((model/log-preference-fn one) outcome)
              ((model/log-preference-fn two) outcome)))
    (println "DECLARED-SCALES" (pr-str {:one one :two two :mu-three mu}))))

(deftest invalid-declarations-refuse-and-omissions-are-labelled
  (doseq [[field value] [[:lam 0] [:lam -1] [:mu -1] [:lam 1.0]]]
    (let [d (declaration) bad (assoc-in d [field :value] value)]
      (is (not= d bad))
      (is (= {:kind :invalid-preference-scale :field field}
             (try (load-one bad) nil
                  (catch clojure.lang.ExceptionInfo e
                    (select-keys (ex-data e) [:kind :field])))))))
  (let [legacy (load-one (dissoc (declaration) :lam :mu))]
    (doseq [field [:lam :mu]]
      (is (= :defaulted (get-in legacy [:preference-scales target field :status])))
      (is (= :parameter-not-declared
             (get-in legacy [:preference-scales target field :reason]))))))

(deftest live-family-agrees-and-disagreement-refuses
  (let [loaded (sources/load-declared)
        assembled (problems/assemble
                   {:targets (keys (:universes loaded))
                    :sources (assoc (sources/with-context-fn loaded) :horizon-steps 2)})
        family (:problems assembled)
        good (live-c/family-scales family)
        bad (assoc-in family [0 :cascade-problem :preference-scales :lam :value] 2)]
    (is (empty? (:refusals assembled)))
    (is (= [1 0] [(:lam good) (:mu good)]))
    (is (every? #(= :declared (:status %)) (mapcat vals (vals (:by-target good)))))
    (is (not= family bad))
    (is (= :incommensurable-family
           (try (live-c/family-scales bad) nil
                (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))))
