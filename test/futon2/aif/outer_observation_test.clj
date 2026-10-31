(ns futon2.aif.outer-observation-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.outer-observation :as observation]
            [futon2.aif.selection-world :as selection-world]))

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

(def fixture-authority-with-tickets
  "One ticket the fixture Giraffe field enumerated (T-in-field) and one that is
  open in the registry but absent from every pass row (T-minted-after-run) --
  the exact shape of run 4955b6e6's missing ticket."
  (-> fixture-authority
      (assoc :ticket-targets ["T-in-field"])
      (assoc-in [:loaded-tickets :tickets]
                [{:id "T-in-field" :path "/fixture/T-in-field.md"
                  :status-class :live :status-line "OPEN" :item-line nil}
                 {:id "T-minted-after-run" :path "/fixture/T-minted-after-run.md"
                  :status-class :live :status-line "OPEN" :item-line nil}])))

(deftest registry-open-ticket-absent-from-field-is-typed-excluded
  (testing "a live ticket the verified field never saw is excluded with a typed
            reason, not silently dropped; no facts are invented for it"
    (let [pass {:as-of "2026-10-28"
                :counts {:open 4 :ready 1 :ready-ignoring-externals 1}
                :order [{:id :M-ready :status :ready :reason :order/ready-by-unblocks
                         :unblocks 2 :cone 4 :source-order 0}
                        {:id :M-cycle :status :undetermined :unblocks 0 :cone 1}
                        {:id :M-waiting :status :not-ready :unblocks 0 :cone 0}
                        {:id :T-in-field :status :undetermined :unblocks 0 :cone 1}]
                :readiness {:M-ready {:status :ready}
                            :M-cycle {:status :undetermined}
                            :M-waiting {:status :not-ready}
                            :T-in-field {:status :undetermined}}
                :cones {:M-ready [:a :b :c :d] :M-cycle [:x] :M-waiting [] :T-in-field [:t]}
                :unblocks {:M-ready [:a :b] :M-cycle [] :M-waiting [] :T-in-field []}
                :cycles [] :anomalies []}
          write-run! (actual-var 'futon3c.giraffe.run/write-run!)
          run-dir (io/file (:dir (write-run! {:root (.getPath (temp-dir))
                                              :run-id "fixture-run-tickets"
                                              :git-sha "fixture"
                                              :adapter-result
                                              {:input {:as-of "2026-10-28"
                                                       :items [] :edges []}
                                               :rejected []}
                                              :pass-output pass})))
          result (binding [observation/*resolve-var* actual-var]
                   (observation/giraffe-field run-dir makeup fixture-authority-with-tickets))
          excluded (into {} (map (juxt :target identity))
                         (:outer-field/excluded result))]
      ;; The enumerated ticket behaves as before: in the field, admitted.
      (is (contains? (set (map :target (:field result))) "T-in-field"))
      (is (nil? (get excluded "T-in-field")))
      ;; The unseen open ticket: a typed exclusion, never in the field/facts.
      (is (= {:target "T-minted-after-run"
              :reason :registry-open-not-in-giraffe-field
              :status-line "OPEN"
              :registry-path "/fixture/T-minted-after-run.md"}
             (get excluded "T-minted-after-run")))
      (is (not (contains? (set (map :target (:field result)))
                          "T-minted-after-run")))
      (is (nil? (get-in result [:facts "T-minted-after-run"]))))))

(deftest registry-open-ticket-exclusion-reaches-selector-population
  (testing "the receiver's population (selection-world Q1) counts the ticket:
            open census N, enumerated N, missing 0"
    (let [outer-selection {:schema :wm/outer-task-selection-v1
                           :outer-field/admitted ["M-ready"]
                           :outer-field/excluded
                           [{:target "T-minted-after-run"
                             :reason :registry-open-not-in-giraffe-field
                             :status-line "OPEN"}]}
          world (selection-world/capture
                 {:controller-ranking []}
                 {"seat-1" {:type "zai"}}
                 nil
                 outer-selection
                 {:code-root "/nonexistent-hermetic"
                  :world-task-sets-fn (constantly
                                       {:missions ["M-ready"]
                                        :excursions []
                                        :tickets ["T-minted-after-run"]})
                  :world-pattern-manifest-fn (constantly {:file-count 0 :digest "x"})
                  :world-now-fn (constantly (java.time.Instant/parse
                                             "2026-10-31T00:00:00Z"))})
          q1 (:q1-diagnostic world)]
      (is (= 2 (:open-task-count q1)))
      (is (= 2 (:enumerated-task-count q1)))
      (is (= 0 (get-in q1 [:open-tasks-missing-from-outer-population :count])))
      (is (empty? (get-in q1 [:open-tasks-missing-from-outer-population :ids]))))))

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
