(ns futon2.aif.flight-operator-stop-test
  "R1(b): an operator stops a running flight between clicks with a durable
  file. The stop is read at the loop head and again just before each click,
  so a stop written while the read or ask step runs still prevents the click."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight :as flight]
            [futon2.aif.flight-driver :as driver]
            [futon2.aif.observation-checks :as checks])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

;; ---- run! level: a world whose every click advances one want ----

(def target "M-test")
(def sources {:wants {target [:a :b :c]}})

(defn- new-flight []
  (flight/start (flight/choose-target {:requested target})
                {:kind :checkbox} {:id "flight-stop"}))

(defn- world
  "Three clicks, each advancing one want, close the flight. STOP is an atom
  the stop-fn reads; ON-CLICK / ON-ASK run with the call number."
  [stop & {:keys [on-click on-ask]}]
  (let [facts (atom {:a false :b false :c false})
        steps [{:a true :b false :c false}
               {:a true :b true :c false}
               {:a true :b true :c true}]
        clicks (atom 0) asks (atom 0)]
    {:clicks clicks
     :opts {:click-fn (fn [_]
                        (let [i (swap! clicks inc)]
                          (reset! facts (nth steps (dec i)))
                          (when on-click (on-click i))
                          {:click-id (str "click-" i) :unreached-wants []}))
            :ask-fn (fn [_ _ _]
                      (let [i (swap! asks inc)]
                        (when on-ask (on-ask i))
                        {:asked [] :needs []}))
            :observe-fn (fn [_ _] @facts)
            :sources-fn (constantly sources)
            :stop-fn (when stop #(deref stop))
            :max-clicks 5}}))

(def stop-map {:path "/fixture/flights/STOP" :read-at "fixture" :text ""})

(deftest without-a-stop-the-world-runs-three-clicks
  ;; the control that makes the stop tests non-vacuous: the same world with
  ;; no stop-fn, and with a stop-fn that never fires, keeps clicking to closure
  (doseq [stop [nil (atom nil)]]
    (let [{:keys [clicks opts]} (world stop)
          f (flight/run! (new-flight) opts)]
      (is (= 3 @clicks))
      (is (= :closed (:status f)))
      (is (nil? (:stop f))))))

(deftest stop-after-click-one-makes-no-second-click
  (let [stop (atom nil)
        {:keys [clicks opts]} (world stop :on-click #(when (= 1 %) (reset! stop stop-map)))
        f (flight/run! (new-flight) opts)]
    (is (= 1 @clicks))
    (is (= 1 (count (:clicks f))))
    (is (= :operator-stopped (:status f)))
    (is (= stop-map (:stop f)))))

(deftest stop-written-during-the-ask-skips-that-click
  ;; the stop appears after the loop head has passed, while the ask step for
  ;; click 2 runs: click 2 must still not be made
  (let [stop (atom nil)
        {:keys [clicks opts]} (world stop :on-ask #(when (= 2 %) (reset! stop stop-map)))
        f (flight/run! (new-flight) opts)]
    (is (= 1 @clicks))
    (is (= :operator-stopped (:status f)))
    (testing "the ask that ran is still on the record"
      (is (= [1 2] (mapv :before-click (:asks f))))))
  (testing "during the first ask: zero clicks"
    (let [stop (atom nil)
          {:keys [clicks opts]} (world stop :on-ask #(when (= 1 %) (reset! stop stop-map)))
          f (flight/run! (new-flight) opts)]
      (is (zero? @clicks))
      (is (= :operator-stopped (:status f))))))

;; ---- run-flight! level: the default stop file ----
;; stubs as in flight_driver_budget_preflight_test

(def mission-text
  (slurp "test/fixtures/mission-criteria/M-futon-seams@futon3c-20959e4f.md"))

(def lifecycle-text
  (slurp "test/fixtures/mission-criteria/M-futon-seams-lifecycle@futon3c-d74a7c5a.edn"))

(defn- temp-dir []
  (.getCanonicalPath
   (.toFile (Files/createTempDirectory "flight-operator-stop"
                                       (make-array FileAttribute 0)))))

(defn- drive [root]
  (let [calls (atom 0)
        opts {:target "M-futon-seams" :seat "fixture" :repo "futon3c"
              :path "holes/missions/M-futon-seams.md"
              :lifecycle-path "holes/labs/M-futon-seams/lifecycle.edn"
              :store root :run-record-dir root :id "flight-op-stop" :max-clicks 2
              :read-text (fn [_ _ path]
                           (if (str/ends-with? path ".edn") lifecycle-text mission-text))
              :observe #(checks/decl-present? mission-text (:decl %))
              :sources {:beta-by-context {:WM {:beta 1}}}
              :answer-fn (fn [_] {:seat "fixture" :job-id "none" :state "failed"})
              :availability-fn (constantly {:schema :wm/ordinary-click-availability-v1
                                            :unit :ordinary-click :available 5})
              :click-fn (fn [_]
                          (let [n (swap! calls inc)]
                            {:click-id (str "fixture-click-" n)
                             :unreached-wants []
                             :abstention {:kind :fixture-no-progress}}))}]
    {:calls calls :result (driver/run-flight! opts {:flight-id "flight-op-stop"})}))

(defn- write-stop! [root name text]
  (let [f (io/file root "flights" name)]
    (io/make-parents f)
    (spit f text)
    (.getCanonicalPath f)))

(deftest stop-file-before-the-first-click
  (doseq [name ["STOP" "flight-op-stop.stop"]]
    (testing name
      (let [root (temp-dir)
            path (write-stop! root name "  operator: hold  \n")
            {:keys [calls result]} (drive root)
            ;; the fixture's stub fns print as #object in the plan
            on-disk (edn/read-string {:default (fn [_ v] v)} (slurp (:record-path result)))]
        (is (zero? @calls))
        (is (= :operator-stopped (:status result)))
        (is (= path (get-in result [:stop :path])))
        (is (= "operator: hold" (get-in result [:stop :text])))
        (is (= :operator-stopped (get-in on-disk [:flight :status])))
        (is (= path (get-in on-disk [:flight :stop :path])))
        (testing "the file is left for the operator"
          (is (.isFile (io/file path))))))))

(deftest another-flights-stop-file-does-not-stop-this-one
  (let [root (temp-dir)
        _ (write-stop! root "flight-other.stop" "")
        {:keys [calls result]} (drive root)]
    (is (= 1 @calls))
    (is (not= :operator-stopped (:status result)))
    (is (nil? (:stop result)))))

(deftest no-stop-file-is-unchanged
  ;; the no-progress stub ends the flight after one click, as it does on main
  (let [{:keys [calls result]} (drive (temp-dir))]
    (is (= 1 @calls))
    (is (= :no-progress (:status result)))
    (is (nil? (:stop result)))))
