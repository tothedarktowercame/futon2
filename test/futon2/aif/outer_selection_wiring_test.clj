(ns futon2.aif.outer-selection-wiring-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runtime :as runtime]
            [futon2.aif.outer-observation :as outer-observation]
            [futon2.aif.outer-select :as outer-select]
            [futon2.aif.outer-state :as outer-state]))

(defonce ^:private giraffe-loader
  (doto (clojure.lang.DynamicClassLoader.
         (.getContextClassLoader (Thread/currentThread)))
    (.addURL (.toURL (.toURI (io/file "/home/joe/code/futon3c/src"))))))

(defn actual-var [symbol]
  (with-bindings {clojure.lang.Compiler/LOADER giraffe-loader}
    (requiring-resolve symbol)))

(defn temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "outer-selection-wiring-test"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defn fixture-run! []
  (let [root (temp-dir)
        pass {:as-of "2026-10-28"
              :counts {:open 2 :ready 1 :ready-ignoring-externals 1}
              :order [{:id :M-selected :status :ready
                       :reason :order/ready-by-unblocks
                       :unblocks 2 :cone 3 :source-order 0}
                      {:id :M-unknown :status :undetermined
                       :reason :order/not-ready-by-source-order
                       :unblocks 0 :cone 1 :source-order 1}]
              :readiness {:M-selected {:status :ready}
                          :M-unknown {:status :undetermined}}
              :cones {:M-selected [:a :b :c] :M-unknown [:x]}
              :unblocks {:M-selected [:a :b] :M-unknown []}
              :cycles [] :anomalies []}
        write-run! (actual-var 'futon3c.giraffe.run/write-run!)
        result (write-run! {:root (.getPath root) :run-id "fixture-missions-run"
                            :git-sha "fixture"
                            :adapter-result {:input {:as-of "2026-10-28"
                                                     :items [] :edges []}
                                             :rejected []}
                            :pass-output pass})]
    (io/file (:dir result))))

(def makeup
  {:makeup/sha256 (apply str (repeat 64 "a"))
   :default-reach 1/2
   :cascades [{:id :ready :patterns ["test"] :steps [:ready-gate]}]
   :c-kit {:focused 55/100 :related 35/100 :unrelated 5/100
           :stop-the-line 5/100 :not-yet-evaluated 0}
   :kappa 100
   :state-kit {:ready {:doable 3 :blocked 1}
               :not-ready {:doable 1 :blocked 3}
               :undetermined {:doable 1 :blocked 1}}
   :gain-kit {:none 1 :one 2 :several 3}})

(defn call-ordinary [opts redefs]
  (binding [outer-observation/*resolve-var* actual-var]
    (with-redefs-fn
      (merge {#'runtime/find-giraffe-run (fn [_] (fixture-run!))
              #'outer-state/load-makeup (fn [_] makeup)
              #'outer-state/read-state (fn [m _] (outer-state/init-state m))
              #'outer-state/write-state! (fn [& _] {:status :written})}
             redefs)
      (fn [] (#'runtime/run-ordinary-opportunity! opts)))))

(deftest missing-task-selects-and-enters-existing-path
  (let [received (atom nil)
        result (call-ordinary
                {:decision-as-of "2026-10-28T12:00:00Z"}
                {#'runtime/run-ordinary-opportunity-with-task!
                 (fn [opts] (reset! received opts) {:ran true})})]
    (is (= {:ran true} result))
    (is (= {:id "M-selected"} (:task @received)))
    (is (= :ready (get-in @received [:outer/selection :cascade-id])))
    (is (vector? (get-in @received [:outer/selection :cascade-scores])))
    (is (= "fixture-missions-run"
           (get-in @received [:outer/selection :giraffe/run-id])))
    (is (nil? (get-in @received [:outer/selection :selection :candidates])))))

(deftest supplied-task-bypasses-selection
  (let [received (atom nil)
        opts {:task {:id "M-caller"}}
        result (with-redefs-fn
                 {#'outer-select/select (fn [& _] (throw (ex-info "must not select" {})))
                  #'runtime/run-ordinary-opportunity-with-task!
                  (fn [x] (reset! received x) {:ran true})}
                 (fn [] (#'runtime/run-ordinary-opportunity! opts)))]
    (is (= {:ran true} result))
    (is (= opts @received))))

(deftest no-verified-run-is-typed-absent
  (let [called? (atom false)
        result (with-redefs-fn
                 {#'runtime/find-giraffe-run (constantly nil)
                  #'runtime/run-ordinary-opportunity-with-task!
                  (fn [_] (reset! called? true))}
                 (fn [] (#'runtime/run-ordinary-opportunity! {})))]
    (is (= {:absent :no-verified-giraffe-run} result))
    (is (false? @called?))))

(deftest empty-chosen-cascade-is-typed-absent
  (let [called? (atom false)
        gates-out (assoc makeup :cascades
                         [{:id :none :patterns ["test"]
                           :steps [{:gate-status :not-ready}]}])
        result (call-ordinary
                {:decision-as-of "2026-10-28T12:00:00Z"}
                {#'outer-state/load-makeup (fn [_] gates-out)
                 #'runtime/run-ordinary-opportunity-with-task!
                 (fn [_] (reset! called? true))})]
    (is (= :no-task-selected (:absent result)))
    (is (= :none (get-in result [:outer/selection :cascade-id])))
    (is (false? @called?))))
