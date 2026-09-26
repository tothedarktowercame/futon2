(ns futon2.aif.loaded-displacement-test
  "RUNNER-DRIFT-I: the drift report covers every loaded futon2.aif.* /
  futon2.report.* namespace by displacement (a var's recorded line no longer
  holds its definition in the classpath file). The fifth flight ran an older
  decision gate while the drift report, a hand-kept list that did not
  include the gate, read zero stale. Report only: the runner's refusal is
  unchanged."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.c-vector]
            [futon2.aif.flight-runner :as fr]
            [futon2.aif.full-loop-cohort]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.held-out-observations]
            [futon2.aif.intrinsic-values]
            [futon2.aif.learning-trial-ledger]
            [futon2.aif.loaded-displacement :as ld]
            [futon2.aif.observation-rates]
            [futon2.aif.policy-depth]))

(defn- line-of
  "1-based line of the first line of the classpath FILE containing S."
  [file s]
  (inc (.indexOf ^java.util.List (vec (str/split-lines (slurp (io/resource file))))
                 (first (filter #(str/includes? % s) (str/split-lines (slurp (io/resource file))))))))

(defn- fixture-ns [vars]
  (let [n (create-ns (symbol (str "futon2.aif.displacement-fixture-" (subs (str (random-uuid)) 0 8))))]
    (doseq [[sym m] vars] (intern n (with-meta sym m) :fixture))
    n))

(deftest a-namespace-loaded-from-an-older-copy-reads-displaced
  ;; the real mechanism: the JVM holds a version of the file whose lines
  ;; differ from the classpath file's. Load policy_depth.clj with three lines
  ;; prepended under its own classpath path, then reload it from the checkout
  (let [file "futon2/aif/policy_depth.clj"
        older (str ";;\n;;\n;;\n" (slurp (io/resource file)))]
    (try
      (clojure.lang.Compiler/load (java.io.StringReader. older) file "policy_depth.clj")
      (let [r (ld/namespace-displacement (the-ns 'futon2.aif.policy-depth))]
        (is (= :displaced (:status r)))
        (is (= 3 (:of r)) "validate, configured, anticipation")
        (is (= 3 (:mismatched r)))
        (is (= ["validate" 8] (first (:sample r)))))
      (finally (require 'futon2.aif.policy-depth :reload)))
    (is (= {:status :current :of 3} (ld/namespace-displacement (the-ns 'futon2.aif.policy-depth)))
        "reloaded from the checkout it reads current")))

(deftest vars-claiming-lines-where-their-names-do-not-sit
  (let [file "futon2/aif/loaded_displacement.clj"
        n (fixture-ns {'summary {:file file :line (line-of file "(defn summary")}
                       'not-here {:file file :line 1}
                       'report {:file file :line (line-of file "(defn namespace-displacement")}})]
    (try
      (let [r (ld/namespace-displacement n)]
        (is (= :displaced (:status r)))
        (is (= 2 (:mismatched r)))
        (is (= 3 (:of r)))
        (is (= #{"not-here" "report"} (set (map first (:sample r))))))
      (finally (remove-ns (ns-name n))))))

(deftest a-file-not-on-the-classpath-is-typed
  (let [n (fixture-ns {'x {:file "futon2/aif/no_such_file_anywhere.clj" :line 1}})
        e (fixture-ns {})]
    (try
      (is (= {:status :no-resource :file "futon2/aif/no_such_file_anywhere.clj"}
             (ld/namespace-displacement n)) "typed, never :current")
      (is (= {:status :no-vars} (ld/namespace-displacement e)))
      (testing "the default membership is all-ns, so a new futon2.aif namespace is in it"
        (is (contains? (ld/report) (ns-name n))))
      (finally (remove-ns (ns-name n)) (remove-ns (ns-name e))))))

(deftest multi-line-defs-and-generated-vars-are-in-place
  ;; the rule: the form that starts at the var's :line names it among its
  ;; top-level elements (defrecord also names ->X and map->X). A defonce
  ;; whose ^{:doc ..} map puts the name lines below (c-vector's c-state,
  ;; intrinsic-values' state) and defrecord's generated vars (full-loop-
  ;; cohort's ->PinnedPreregistration) are what a name-on-the-line rule
  ;; flagged falsely
  (let [cv "futon2/aif/c_vector.clj"
        n (fixture-ns {'c-state {:file cv :line (line-of cv "(defonce ^{:doc \"The maintained live C-vector")}})]
    (try
      (is (not (str/includes? (nth (str/split-lines (slurp (io/resource cv)))
                                   (dec (line-of cv "(defonce ^{:doc \"The maintained live C-vector")))
                              "c-state"))
          "the name is not on the form's start line")
      (is (= :current (:status (ld/namespace-displacement n))))
      (finally (remove-ns (ns-name n)))))
  (is (= :current (:status (ld/namespace-displacement (the-ns 'futon2.aif.full-loop-cohort))))))

(deftest the-seven-flagged-after-a-clean-reload-read-current
  (doseq [n '[futon2.aif.c-vector futon2.aif.full-loop-cohort futon2.aif.full-loop-runner
              futon2.aif.held-out-observations futon2.aif.intrinsic-values
              futon2.aif.learning-trial-ledger futon2.aif.observation-rates]]
    (is (= :current (:status (ld/namespace-displacement (the-ns n)))) (str n))))

(deftest the-runner-report-carries-it-and-still-refuses-only-on-its-own-file
  (let [check (runner/runner-source-drift)
        r (:loaded-displacement check)]
    (is (map? r))
    (is (= :current (get-in r ['futon2.aif.full-loop-runner :status])))
    (is (contains? r 'futon2.aif.policy-depth))
    (is (contains? check :namespaces) "the existing report is unchanged")
    (is (not= :drift (:runner/source-check check)))))

(deftest the-flight-reads-it
  (let [displaced {'futon2.aif.decision-gate {:status :displaced :mismatched 4 :of 9}
                   'futon2.aif.flight {:status :current :of 30}}
        record {:runner/source {:loaded-displacement displaced} :route []}]
    (is (= {:checked 2 :displaced-count 1 :displaced ['futon2.aif.decision-gate]}
           (:displacement (fr/record-summary "M" "c1" record))))
    (is (= {:absent :no-loaded-displacement-on-run-record}
           (:displacement (fr/record-summary "M" "c1" {:route []}))))
    (let [dir (java.nio.file.Files/createTempDirectory "ld-runs" (make-array java.nio.file.attribute.FileAttribute 0))
          f (io/file (str dir) "tick-run-record-r1.edn")]
      (is (= :no-run-record (:absent (fr/latest-displacement (str dir)))))
      (spit f (pr-str (assoc record :run/id "r1" :startedAt "2026-09-26T03:00:00Z")))
      (is (= {:from-run-record "r1" :started-at "2026-09-26T03:00:00Z"
              :checked 2 :displaced-count 1 :displaced ['futon2.aif.decision-gate]}
             (fr/latest-displacement (str dir)))))))
