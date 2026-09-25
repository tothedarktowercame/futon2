(ns futon2.aif.flight-enact-test
  "M-wm-wiring row 0: the enactment step. A fixture dispatch function stands
  in for the seat (no seat is asked); nothing is flown."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-runner :as fr])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn- temp-dir [] (str (.toFile (Files/createTempDirectory "enact" (make-array FileAttribute 0)))))

(def interps {:p/a {:produces #{:t/a}} :p/b {:produces #{:t/b}}})
(def click {:click-id "run-1" :chosen {:candidate :cand/x :precedence [:p/a :p/b]}})
(def flight-0 {:flight/id "flight-e" :target "M-t"})

(defn- dispatch [calls & [fail]]
  (fn [step]
    (swap! calls conj (select-keys step [:pattern :phase]))
    (if (= fail (:pattern step))
      {:failed {:reason :seat-reported-failure :detail "fixture"}}
      {:commit (str "c-" (name (:pattern step))) :produced (first (get-in step [:interpretation :produces]))
       :check {:class :fixture :token (first (get-in step [:interpretation :produces]))}})))

(defn- enact [dir calls & [fail]]
  (fr/enact-fn {:dispatch-step! (dispatch calls fail)
                :check-fn (fn [check] {:observed (= :t/a (:token check))})
                :interpretations (constantly interps)
                :fetch-run-record (fn [id] {:run-id id})
                :record-dir dir}))

(deftest two-steps-two-attempts-each-with-its-check
  (let [dir (temp-dir) calls (atom [])
        {:keys [enactment record-path]} ((enact dir calls) flight-0 click)]
    (is (= [:p/a :p/b] (mapv :pattern (:attempts enactment))))
    (is (= ["c-a" "c-b"] (mapv :commit (:attempts enactment))))
    (is (= [{:observed true} {:observed false}] (mapv #(get-in % [:check :result]) (:attempts enactment))))
    (is (= [true false] (mapv :success (:attempts enactment))))
    (is (= [{:pattern :p/a :phase :commit} {:pattern :p/b :phase :commit}] @calls) "no :plan asked without a grain pattern")
    (is (= {:absent :candidate-names-no-grain-pattern} (:grain-attempt enactment)))
    (is (= :grain-not-declared (get-in enactment [:grain-gate :reason])) "the gate's own refusal, recorded")
    (is (= {:click-id "run-1" :present true} (:run-record enactment)))
    (is (= :cand/x (:decision-candidate enactment)) "the decision's id is on the record for W_c's join")
    (is (= {:absent :no-repair-obligation-for-target :target "M-t"} (:publication-observed enactment)) "no repair id for the target: publication does not apply (step 12)")
    (is (= enactment (edn/read-string (slurp record-path))) "the record is written where the flight's records go")
    (is (.startsWith ^String record-path (.getCanonicalPath (io/file dir))))))

(deftest a-failed-step-is-a-deviation-not-dropped
  (let [{:keys [enactment]} ((enact (temp-dir) (atom []) :p/a) flight-0 click)]
    (is (= 2 (count (:attempts enactment))))
    (is (= {:reason :seat-reported-failure :detail "fixture"} (:failed (first (:attempts enactment)))))
    (is (= [{:kind :step-failed :pattern :p/a :failed {:reason :seat-reported-failure :detail "fixture"}}]
           (get-in enactment [:conformance :deviations])))))

(deftest no-decision-no-record
  (let [dir (temp-dir)
        r ((enact dir (atom [])) flight-0 {:click-id "run-2" :abstention {:kind :missing}})]
    (is (= {:absent :no-decision :click-id "run-2"} r))
    (is (empty? (rest (file-seq (io/file dir)))) "nothing written")))

(deftest the-flight-loop-calls-the-enactment-step-after-the-click
  (let [dir (temp-dir)
        f (flight/run! (flight/start {:target "M-t" :chosen-because {:kind :requested}}
                                     {:kind :a-exits :repo "futon3c" :path "p" :read-text (fn [& _] "")}
                                     {:id "flight-loop"})
                       {:click-fn (fn [_] click)
                        :enact-fn (enact dir (atom []))
                        :observe-fn (fn [_ _] {})
                        :sources-fn (constantly {})
                        :max-clicks 1})
        abst (flight/run! (flight/start {:target "M-t" :chosen-because {:kind :requested}}
                                        {:kind :a-exits :repo "futon3c" :path "p" :read-text (fn [& _] "")}
                                        {:id "flight-loop-2"})
                          {:click-fn (fn [_] {:click-id "run-3"})
                           :enact-fn (enact dir (atom []))
                           :observe-fn (fn [_ _] {})
                           :sources-fn (constantly {})
                           :max-clicks 1})]
    (is (= "run-1" (:click-id (first (:enactments f)))))
    (is (string? (:record-path (first (:enactments f)))))
    (is (= [{:enactment {:absent :no-decision} :click-id "run-3"}] (:enactments abst)))))
