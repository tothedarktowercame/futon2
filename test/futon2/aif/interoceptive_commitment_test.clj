(ns futon2.aif.interoceptive-commitment-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.interoceptive-commitment :as commitment]))

(def pin (apply str (repeat 64 "a")))
(def other-pin (apply str (repeat 64 "b")))

(defn trip [id action]
  {:path (str "trips/" id ".edn") :sha256 pin
   :record {:trip/id id :trip/schema-version 1 :trip/action action
            :trip/wire-id :T5 :trip/recorded-at "2026-09-12T00:00:00Z"}})

(defn repair-entry [child repair-id trip-id status]
  [(str child "/" repair-id ".edn")
   {:sha256 other-pin
    :record (cond-> {:repair/id repair-id :repair/status status}
              (= child "findings")
              (assoc :failure-data {:trip/id trip-id}))}])

(defn input [trips repair-records]
  {:trip-authority {:source-root "/home/joe/code/futon2/data/wm-tripwires/trips"
                    :revision pin :read-status :ok :authority-class :production
                    :records trips}
   :repair-authority {:source-root "/home/joe/code/futon2/data/wm-repair-obligations"
                      :revision other-pin :read-status :ok
                      :authority-class :production :records (into {} repair-records)}})

(defn refusal [f]
  (try (f) nil (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e)))))

(deftest fixed-law-and-discharge-round-trip
  (let [t1 (trip "trip-real-1" :stop-line)
        t2 (trip "trip-real-2" :park-and-summon)
        f1 (repair-entry "findings" "repair-trip-1" "trip-real-1" :open)
        f2 (repair-entry "findings" "repair-trip-2" "trip-real-2" :open)]
    (is (= 1 (:machine-confidence (commitment/confidence-snapshot (input [] [])))))
    (let [one (commitment/confidence-snapshot (input [t1] [f1]))
          two (commitment/confidence-snapshot (input [t1 t2] [f1 f2]))]
      (is (= 1/2 (:machine-confidence one)))
      (is (= 1/2 (:machine-confidence two)))
      (is (= ["trip-real-1" "trip-real-2"] (:open-trip-ids two))))
    (let [one-discharged (conj [f1 f2]
                               (repair-entry "resolutions" "repair-trip-1"
                                             "trip-real-1" :resolved))
          all-discharged (conj one-discharged
                               (repair-entry "resolutions" "repair-trip-2"
                                             "trip-real-2" :resolved))]
      (is (= 1/2 (:machine-confidence
                  (commitment/confidence-snapshot (input [t1 t2] one-discharged)))))
      (is (= 1 (:machine-confidence
                (commitment/confidence-snapshot (input [t1 t2] all-discharged))))))))

(deftest exclusions-are-retained-and-not-counted
  (let [shadow (trip "trip-shadow" :record)
        result (commitment/confidence-snapshot (input [shadow] []))
        test-input (assoc-in (input [(trip "trip-test" :stop-line)] [])
                             [:trip-authority :authority-class] :test)
        test-result (commitment/confidence-snapshot test-input)]
    (is (= 1 (:machine-confidence result)))
    (is (= :shadow-record (get-in result [:excluded 0 :reason])))
    (is (= 1 (:machine-confidence test-result)))
    (is (= :test-root (get-in test-result [:excluded 0 :reason])))))

(deftest commissioned-refusals
  (let [t (trip "trip-real" :stop-line)
        f (repair-entry "findings" "repair-trip" "trip-real" :open)
        base (input [t] [f])]
    (testing "genuine report must join its finding"
      (is (= :interoceptive/missing-finding-join
             (refusal #(commitment/confidence-snapshot (input [t] []))))))
    (testing "duplicate and malformed identity"
      (is (= :interoceptive/duplicate-trip-identity
             (refusal #(commitment/confidence-snapshot (input [t t] [f])))))
      (is (= :interoceptive/malformed-trip
             (refusal #(commitment/confidence-snapshot
                        (input [(assoc-in t [:record :trip/id] "bad/id")] [f]))))))
    (testing "contradictory discharge and unknown modes"
      (is (= :interoceptive/contradictory-discharge
             (refusal #(commitment/confidence-snapshot
                        (input [t] [f (repair-entry "resolutions" "repair-trip"
                                                   "trip-real" :open)])))))
      (is (= :interoceptive/unknown-mode
             (refusal #(commitment/confidence-snapshot
                        (input [(assoc-in t [:record :trip/action] :mystery)] [f]))))))
    (testing "authority refuses closed"
      (is (= :interoceptive/authority-unavailable
             (refusal #(commitment/confidence-snapshot
                        (assoc-in base [:trip-authority :read-status] :unreadable)))))
      (is (= :interoceptive/unpinned-authority
             (refusal #(commitment/confidence-snapshot
                        (assoc-in base [:repair-authority :revision] nil))))))))
