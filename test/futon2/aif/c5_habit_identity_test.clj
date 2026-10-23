(ns futon2.aif.c5-habit-identity-test
  "The ordinary record retains the sealed reading-policy key at selection-event;
  its :decision/:chosen projection cannot reconstruct the action arrangement."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.enactment-habit :as habit]
            [futon2.aif.policy-prefix-admission :as admission])
  (:import [java.security MessageDigest]))

(def ^:private record-path
  "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-10-23-57cd4dc4-7bf2-43ad-9cbd-7549e183ab32.edn")

(def ^:private record-sha256
  "be624b0bf12828772fdb328c75c50e8551910da7c70823453835ff9f4f1af87f")

(defn- sha256 [text]
  (apply str (map #(format "%02x" (bit-and 0xff %))
                  (.digest (MessageDigest/getInstance "SHA-256")
                           (.getBytes text "UTF-8")))))

(defn- live-record []
  (let [text (slurp record-path)]
    (is (= record-sha256 (sha256 text)) "the live pin changed")
    (edn/read-string text)))

(defn- reading-action [record key]
  (some (fn [entry]
          (let [action (:action entry)]
            (when (= (nth key 2) (:id action)) action)))
        (get-in record [:outer-task-selection :policy
                        :selected-outer-cascade-policy :sealed-menu :entries])))

(deftest recorded-selection-event-key-is-the-ordinary-reading-identity
  (let [record (live-record)
        chosen (get-in record [:decision :chosen])
        key (get-in record [:selection-event :policy-key])
        click {:decision {:chosen chosen}
               :selection-event {:policy-key key}}
        candidate (:id chosen)]
    (testing "the live projection is the recorded source, not a reconstructed action"
      (is (= {:target "M-stack-geometry"
              :id "M-stack-geometry/policy-c7d7518e1e185c2e2318"}
             (select-keys chosen [:target :id])))
      (is (= :wm/reading-policy-v1 (first key)))
      (is (= candidate (nth key 2))))
    (is (= key (habit/policy-key-for {:candidate candidate} click nil)))
    (testing "a key for another policy is not borrowed"
      (let [result (habit/policy-key-for
                    {:candidate "M-stack-geometry/policy-0000"} click nil)]
        (is (map? result))
        (is (not= key result))))))

(deftest decision-action-still-precedes-the-recorded-selection-event-key
  (let [record (live-record)
        recorded-key (get-in record [:selection-event :policy-key])
        action (reading-action record recorded-key)
        action-key (admission/reading-policy-key action)
        click {:decision {:action action}
               :selection-event {:policy-key
                                 (assoc recorded-key 2 "M-stack-geometry/policy-other")}}]
    (is (map? action))
    (is (= action-key
           (habit/policy-key-for {:candidate (:id action)} click nil)))))
