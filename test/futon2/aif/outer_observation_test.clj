(ns futon2.aif.outer-observation-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-observation :as observation]))

(def makeup {:default-reach 1/2})

(def fixture-authority
  {:loaded-missions
   {:missions (mapv (fn [id]
                      {:id id :path (str "/fixture/" id ".md")
                       :status-class :open :status-line "OPEN"})
                    ["M-ready" "M-cycle" "M-waiting"])}
   :loaded-tickets {:tickets []}
   :loaded-excursions {:excursions []}
   :ticket-targets []
   :excursion-targets []})

(defn temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "outer-observation-test"
            (make-array java.nio.file.attribute.FileAttribute 0))))

(defonce ^:private giraffe-loader
  (doto (clojure.lang.DynamicClassLoader.
         (.getContextClassLoader (Thread/currentThread)))
    (.addURL (.toURL (.toURI (io/file "/home/joe/code/futon3c/src"))))))

(defn actual-var [symbol]
  ;; The production serving JVM already has this source root.  The isolated
  ;; test process adds it explicitly so these tests exercise Giraffe's own
  ;; verifier without making futon2 depend on futon3c.
  (with-bindings {clojure.lang.Compiler/LOADER giraffe-loader}
    (requiring-resolve symbol)))

(defn fixture-run! []
  (let [root (temp-dir)
        pass {:as-of "2026-10-28"
              :counts {:open 3 :ready 1 :ready-ignoring-externals 1}
              :order [{:id :M-ready :status :ready :reason :order/ready-by-unblocks
                       :unblocks 2 :cone 4 :source-order 0}
                      {:id :M-cycle :status :undetermined :unblocks 0 :cone 1}
                      {:id :M-waiting :status :not-ready :unblocks 0 :cone 0}]
              ;; Closed rows may exist in readiness, but never in :order.
              :readiness {:M-ready {:status :ready}
                          :M-cycle {:status :undetermined}
                          :M-waiting {:status :not-ready}
                          :M-closed {:status :done}}
              :cones {:M-ready [:a :b :c :d] :M-cycle [:x] :M-waiting []}
              :unblocks {:M-ready [:a :b] :M-cycle [] :M-waiting []}
              :cycles [] :anomalies []}
        write-run! (actual-var 'futon3c.giraffe.run/write-run!)
        result (write-run! {:root (.getPath root) :run-id "fixture-run"
                            :git-sha "fixture"
                            :adapter-result {:input {:as-of "2026-10-28"
                                                     :items [] :edges []}
                                             :rejected []}
                            :pass-output pass})]
    (io/file (:dir result))))

(deftest verified-field-carries-open-facts
  (testing "closed items are excluded and Giraffe statuses remain distinct"
    (let [dir (fixture-run!)
          result (binding [observation/*resolve-var* actual-var]
                   (observation/giraffe-field dir makeup fixture-authority))]
      (is (= ["M-ready" "M-cycle" "M-waiting"] (mapv :target (:field result))))
      (is (every? #(= 1/2 (:reach %)) (:field result)))
      (is (= {:status :ready :unblocks 2 :cone 4} (get-in result [:facts "M-ready"])))
      (is (= :undetermined (get-in result [:facts "M-cycle" :status])))
      (is (= :not-ready (get-in result [:facts "M-waiting" :status])))
      (is (nil? (get-in result [:facts "M-closed"])))
      (is (empty? (:outer-field/excluded result))))))

(deftest unverified-run-refuses
  (let [dir (fixture-run!)
        _ (spit (io/file dir "pass.edn") "{:tampered true}\n")
        error (binding [observation/*resolve-var* actual-var]
                (try (observation/giraffe-field dir makeup)
                     (catch clojure.lang.ExceptionInfo e e)))]
    (is (= :giraffe-run-unverified (:kind (ex-data error))))
    (is (some #(= "pass.edn" (:file %))
              (get-in (ex-data error) [:verification :mismatches])))))

(deftest unavailable-verifier-refuses
  (let [error (binding [observation/*resolve-var* (fn [_] nil)]
                (try (observation/giraffe-field (fixture-run!) makeup)
                     (catch clojure.lang.ExceptionInfo e e)))]
    (is (= :giraffe-verifier-unavailable (:kind (ex-data error))))))

(deftest unknown-relation-is-not-guessed
  (let [dir (fixture-run!)
        focus-inputs {:windows [] :relations [] :facet-edges []}
        result (binding [observation/*resolve-var* actual-var]
                 (observation/outer-observation
                  {:run-dir dir :makeup makeup :focus-inputs focus-inputs
                   :decision-as-of "2026-10-28T12:00:00Z"}))]
    (is (= :unknown ((:relation result) (:focus result) "M-unresolvable")))
    (is (= :unknown (get-in result [:focus/source :status])))))

(deftest real-giraffe-run-shape
  (let [dir (io/file "/home/joe/code/storage/giraffe/20261028T154151.285Z-b916cd4e")]
    (is (.isDirectory dir) "the retained Giraffe evidence must be present")
    (let [result (binding [observation/*resolve-var* actual-var]
                   (observation/giraffe-field dir makeup))
          targets (set (map :target (:field result)))
          excluded (some #(when (= "M-repl-wins-over-cli" (:target %)) %)
                         (:outer-field/excluded result))]
      (is (= {:target "M-repl-wins-over-cli"
              :reason :inactive
              :status-line "OPEN — parked"}
             excluded))
      (is (not (contains? targets "M-repl-wins-over-cli")))
      (is (nil? (get-in result [:facts "M-repl-wins-over-cli"])))
      (is (contains? targets "M-futonzero-prelim-practice"))
      (is (some? (get-in result [:facts "M-futonzero-prelim-practice"]))))))
