(ns futon2.aif.held-out-observations-test
  "⟨1⟩6: the held-out rows come from real run records with verifiable
  provenance; a row that cannot be tied to a record cannot close the window."
  (:require [clojure.edn :as edn]
            [clojure.set]
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
  ;; rows-from-runs over the real root yields exactly this ticket's runs.
  ;;
  ;; This pinned the COUNTS -- 4 rows, 1 valid, window open -- which are
  ;; properties of how many clicks have been fired, not of the code. The next
  ;; click broke it while the code was correct and the window had, properly,
  ;; closed. What holds whatever the ledger says is pinned instead: every row
  ;; is this ticket's and carries provenance, and the registration instant
  ;; partitions the rows exactly (claude-5). A closing paren also made the
  ;; :open assertion the MESSAGE argument of the one above it.
  (let [rows (obs/rows-from-runs declaration)
        registered (java.time.Instant/parse (:registered-at declaration))
        w (obs/collect-window declaration rows)
        before? (fn [r] (.isBefore (java.time.Instant/parse (:recorded-at r)) registered))]
    (is (seq rows) "the ticket has run records at all")
    (is (every? (fn [r] (= t (:target r))) rows) "every row is this ticket's")
    (is (every? (fn [r] (and (map? (:source r))
                             (string? (:path (:source r)))
                             (string? (:sha256 (:source r)))))
                rows)
        "every row carries its record's path and digest")
    (is (every? (fn [r] (= :before-registration (:hygiene-reason r)))
                (filter before? (:observations w)))
        "every pre-registration row is excluded, and for that reason")
    (is (every? (fn [r] (= :valid (:hygiene r)))
                (remove before? (:observations w)))
        "every post-registration row on this ticket counts")
    (is (= (count (remove before? (:observations w))) (:valid-count w))
        "and the valid count is exactly those")
    (is (= (>= (:valid-count w) (:required w)) (= :closed (:status w)))
        "the window is closed exactly when the declared N is met")))

(deftest unmapped-outcome-not-counted
  ;; an unmapped outcome is retained, reported, does not count
  (let [rows (obs/rows-from-runs declaration)
        real (first (filter #(= "2026-09-23-1790184736" (:run-id %)) rows))
        unmapped-val (:unmapped-outcome
                     (edn/read-string
                      (slurp (io/resource "wm/eig/held-out-outcome-class-mapping.edn"))))
        unmapped-row (assoc real :outcome-class unmapped-val :unmapped? true)
        w (obs/collect-window declaration [unmapped-row])]
    (is (= :unclassified-outcome-not-counted
           (-> w :observations first :hygiene-reason)))
    (is (zero? (:valid-count w)))
    (is (= :open (:status w)))))

;; claude-5, reviewing 61c573a8. :recorded-at was the one field a row could
;; state freely: verify-source! checked run-id, target and outcome-class
;; against the record and not the instant. That instant is what decides
;; membership — three of this ticket's four runs are excluded by
;; :before-registration and nothing else — so a doctored one moves a run whose
;; outcome was already known into the held-out window, which is the single
;; thing the split exists to prevent.
(deftest a-doctored-instant-cannot-move-a-known-run-into-the-window
  (let [rows (obs/rows-from-runs declaration)
        before (first (filter #(= "2026-09-23-1790161992" (:run-id %)) rows))]
    (is (some? before) "precondition: the 11:13 run is among the rows")
    (is (= :before-registration
           (-> (obs/collect-window declaration [before]) :observations first :hygiene-reason))
        "precondition: it is excluded only by its instant")
    ;; move it past registration, changing nothing else
    (let [doctored (assoc before :recorded-at "2026-09-23T13:00:00Z")
          w (obs/collect-window declaration [doctored])]
      (is (= :row-disagrees-with-source
             (-> w :observations first :hygiene-reason))
          "the instant is checked against the record, not taken on trust")
      (is (zero? (:valid-count w)))
      (is (= :open (:status w))))))

;; The resource is the artifact the C4 locator observes and the only thing a
;; later reader has. It was committed with a leading "# regenerated ..." line,
;; which is not an EDN comment — clojure.edn/read-string throws
;; "No dispatch macro" on it — so the file said one thing to the line-oriented
;; locator and nothing at all to a parser.
;;
;; What this pins is the SAFETY direction, not equality. The resource is
;; re-materialized by an author enacting the hygiene limb, so between a run
;; landing and the next click it legitimately lags the records — claiming
;; FEWER observations than exist is harmless. Claiming more is not: that is
;; the fabricated window, arriving by staleness instead of by hand. So: every
;; valid row the resource claims must still be valid against the records, and
;; the disposition head may appear only when the records themselves close the
;; window (claude-5).
(deftest the-committed-resource-parses-and-never-claims-more-than-the-records-support
  (let [committed (edn/read-string (slurp (io/resource "wm/eig/held-out-observations.edn")))
        computed (obs/collect-window declaration (obs/rows-from-runs declaration))
        valid-of (fn [w] (set (map :run-id (filter #(= :valid (:hygiene %)) (:observations w)))))]
    (is (map? committed) "the resource parses as EDN")
    (is (clojure.set/subset? (valid-of committed) (valid-of computed))
        (str "the resource claims a valid observation the records do not support: "
             (pr-str (clojure.set/difference (valid-of committed) (valid-of computed)))))
    (is (or (nil? (:disposition committed))
            (= :closed (:status computed)))
        "the disposition head appears only when the records themselves close the window")
    (is (= (:required computed) (:required committed))
        "and it is measured against the same declared N")))
