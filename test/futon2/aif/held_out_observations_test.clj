(ns futon2.aif.held-out-observations-test
  "⟨1⟩6: the held-out rows come from real run records with verifiable
  provenance; a row that cannot be tied to a record cannot close the window."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.held-out-observations :as obs]))

(def t "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")

(def declaration
  (edn/read-string (slurp (io/resource "wm/eig/held-out-split-v2.edn"))))

(defn- fabricated-rows []
  ;; two rows that pass every ORIGINAL hygiene check — distinct run-ids,
  ;; instants after registration, 64-hex digests, declared outcome classes —
  ;; but name run records that DO NOT EXIST
  [{:run-id "2026-09-25-1799999999"
    :target t
    :recorded-at "2026-09-25T10:00:00Z"
    :close-sha256 (apply str (repeat 64 "a"))
    :outcome-class :no-result
    :source {:path "data/wm-runs/tick-run-record-2026-09-25-1799999999.edn"
             :sha256 (apply str (repeat 64 "a"))}}
   {:run-id "2026-09-26-1799999998"
    :target t
    :recorded-at "2026-09-26T10:00:00Z"
    :close-sha256 (apply str (repeat 64 "b"))
    :outcome-class :no-result
    :source {:path "data/wm-runs/tick-run-record-2026-09-26-1799999998.edn"
             :sha256 (apply str (repeat 64 "b"))}}])

(deftest fabricated-window-cannot-close
  ;; BEFORE this change: two such rows closed the window (status :closed,
  ;; window-closed? true) and would have made the C4 locator observe
  ;; HELD-OUT-OBSERVATIONS-COLLECTED. AFTER: each is :hygiene :invalid with
  ;; :source-missing and the window stays open.
  (let [w (obs/collect-window declaration (fabricated-rows))]
    (is (= :open (:status w)) (pr-str (select-keys w [:status])))
    (is (zero? (:valid-count w)))
    (is (= :source-missing (-> w :observations first :hygiene-reason)))
    (is (= :source-missing (-> w :observations second :hygiene-reason)))))

(deftest doctored-row-disagrees-with-source
  ;; a row whose source names the REAL record with the RIGHT digest but
  ;; whose outcome-class disagrees with what the record says
  (let [rows (obs/rows-from-runs declaration)
        real (first (filter #(= "2026-09-23-1790184736" (:run-id %)) rows)) ;; outcome :grounded-no-change → :no-result
        doctored (assoc real :outcome-class :result)]
    (is (some #(= "2026-09-23-1790184736" (:run-id %)) rows))
    (let [w (obs/collect-window declaration [doctored])]
      (is (= :row-disagrees-with-source
             (-> w :observations first :hygiene-reason)))
      (is (= :open (:status w))))))

(deftest real-records-and-exclusions
  ;; rows-from-runs over the real root yields exactly this ticket's runs
  (let [rows (obs/rows-from-runs declaration)]
    (is (= 4 (count rows)))
    (is (every? #(= t (:target %)) rows))
    (is (every? #(map? (:source %)) rows))
    (let [w (obs/collect-window declaration rows)]
      (is (= 1 (:valid-count w)) "only the post-registration run counts")
      (is (some #(= :before-registration (:hygiene-reason %)) (:observations w))
      (is (= :open (:status w)))))))

(deftest unmapped-outcome-not-counted
  ;; an unmapped outcome is retained, reported, does not count
  (let [rows (obs/rows-from-runs declaration)
        real (first (filter #(= "2026-09-23-1790184736" (:run-id %)) rows))
        unmapped-val (:unmapped-outcome
                     (edn/read-string
                      (slurp (io/resource "wm/eig/held-out-outcome-class-mapping.edn"))))
        unmapped-row (assoc real :outcome-class unmapped-val :unmapped? true)]
    (let [w (obs/collect-window declaration [unmapped-row])]
      (is (= :unclassified-outcome-not-counted
             (-> w :observations first :hygiene-reason)))
      (is (zero? (:valid-count w)))
      (is (= :open (:status w))))))
