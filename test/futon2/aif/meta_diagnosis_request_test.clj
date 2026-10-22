(ns futon2.aif.meta-diagnosis-request-test
  "Pinned live-record discovery fixture; no seat invokes or source mutations."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.meta-self-heal-production :as sut]))

(def source-pin
  {:path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-22-02587641-d615-4a5b-8bc8-a3df48e2ab45.edn"
   :sha256 "64b3eff6a27638cc429c7e7046687e3d3258756e90b9cca8a9dc63b0e32db63f"})
(def injury-id "injury/04e14314a0eec3aab88d8e7044be26f3ae5dfe6d")
(def source (delay (java.nio.file.Files/readAllBytes (.toPath (io/file (:path source-pin))))))
(def record (delay (edn/read-string (String. ^bytes @source "UTF-8"))))
(defn serialized [x] (binding [*print-namespace-maps* false] (pr-str x)))
(defn byte-size [x] (count (.getBytes (serialized x) "UTF-8")))

(deftest real-source-remains-pinned-and-causal-evidence-verbatim
  (let [r @record q (sut/bounded-diagnosis-request injury-id source-pin r)]
    (is (= (:sha256 source-pin) (identity/sha256 @source)))
    (is (< (byte-size q) sut/diagnosis-request-budget-bytes))
    (is (= injury-id (:injury-id q)))
    (is (= source-pin (:source-record-pin q)))
    (is (= (:failure r) (get-in q [:injury-record :failure])))
    (is (= :reviewer-falsifier-failed (get-in q [:injury-record :failure :kind])))
    (is (= :reviewer-wait (get-in q [:injury-record :failure :stage])))
    ;; The opening record itself omits full checks. Retain that fact verbatim.
    (is (some #{:failed-checks} (get-in q [:injury-record :failure :detail :omitted])))
    (doseq [k [:outer-task-selection :decision]]
      (let [row (first (filter #(= [k] (:path %)) (:abbreviated-fields q)))]
        (is (= (byte-size (get r k)) (:serialized-bytes row)))
        (is (not (contains? (:injury-record q) k)))))
    (is (= (set (keys r)) (set (map (comp first :path) (:record-index q)))))))

(deftest producer-dispatches-the-bounded-request
  (let [seen (atom nil)
        result (sut/produce-diagnosis!
                 {:meta-diagnosis-fn (fn [q] (reset! seen q) {:status :failed :reason :fixture-only})}
                 {:ledger {:entries [{:injury-id injury-id :cause-record-pin source-pin}]}}
                 {:injury-id injury-id})]
    (is (= :meta-self-heal/diagnosis-unavailable (:reason result)))
    (is (= (sut/bounded-diagnosis-request injury-id source-pin @record) @seen))
    (is (< (byte-size @seen) sut/diagnosis-request-budget-bytes))))

(deftest oversized-causal-evidence-is-explicit-not-dropped
  (let [value {:failed-checks [(str "HEAD" (apply str (repeat 90000 "界")) "TAIL")]}
        q (sut/bounded-diagnosis-request injury-id source-pin {:failure value :failed-checks [:exact-check]})
        preview (get-in q [:injury-record :failure])]
    (is (< (byte-size q) sut/diagnosis-request-budget-bytes))
    (is (= :abbreviated (:status preview)))
    (is (= (byte-size value) (:serialized-bytes preview)))
    (is (.contains (:head preview) "HEAD"))
    (is (.contains (:tail preview) "TAIL"))
    (is (= [:exact-check] (get-in q [:injury-record :failed-checks])))
    (is (= [{:path [:failure] :serialized-bytes (byte-size value) :reason :causal-field-over-budget}]
           (:abbreviated-fields q)))))

(deftest over-budget-request-is-a-typed-diagnosis-refusal
  (with-redefs [futon2.aif.meta-self-heal-production/active-cause-pin (fn [& _] source-pin)
                futon2.aif.meta-self-heal-production/source-record (fn [_] {:status :available :record {}})
                futon2.aif.meta-self-heal-production/bounded-diagnosis-request
                (fn [& _] (throw (ex-info "over" {:kind :meta-self-heal/diagnosis-request-over-budget
                                                  :bytes 1 :budget 0})))]
    (let [called (atom false)
          r (#'futon2.aif.meta-self-heal-production/produce-diagnosis!
             {:meta-diagnosis-fn (fn [_] (reset! called true) {:status :succeeded})}
             ::durable {:injury-id "injury/x"})]
      (is (= :refused (:status r)) (pr-str r))
      (is (false? @called)))))
