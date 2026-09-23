(ns futon2.aif.held-out-calibration-test
  "⟨1⟩6: the calibration boundary scores frozen predictions against observed
  outcomes and publishes CALIBRATION-EVIDENCE-PASSING only when the declared
  metrics are inside the declared bounds. The bad cases -- a failing
  calibration, a truncated window, a broken provenance link, a hostile key
  order -- are the point."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.held-out-calibration :as cal]
            [futon2.aif.observation-checks :as checks]))

(def t "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")

(def declaration
  (edn/read-string (slurp (io/resource "wm/eig/held-out-split-v2.edn"))))

(def observations-packet
  (edn/read-string (slurp (io/resource "wm/eig/held-out-observations.edn"))))

(defn- scored-row
  "A calibration row as rows-from-observations emits one, fabricated."
  [run-id predicted realised]
  {:run-id run-id
   :token [t :restoration-accepted]
   :predicted predicted
   :realised realised
   :source {:path "x" :sha256 (apply str (repeat 64 "a"))}
   :close-source {:path "y" :sha256 (apply str (repeat 64 "b"))}})

;; Case 1 -- the case the whole boundary exists for. A calibration whose
;; metrics fall outside the declared bounds is a real result about the
;; machine's predictions; it must render WITHOUT the head, so the C4
;; locator reads :repair/calibration-evidence-present FALSE.
(deftest a-failing-calibration-never-publishes-the-head
  (let [;; confidently wrong: predicted ~certain, realised false twice ->
        ;; mean log-loss ~4.6 >> ln 4, mean brier ~0.98 >> 0.25
        rows [(scored-row "r1" 0.99 false) (scored-row "r2" 0.99 false)]
        result (cal/calibrate declaration
                              {:rows rows :excluded []
                               :window {:next-n 2 :contributing ["r1" "r2"]}})]
    (is (= :failing (:status result)))
    (is (= [:mean-log-loss-outside-bounds :mean-brier-outside-bounds]
           (:failing-reasons result)))
    (is (not (contains? result :disposition)))
    (let [rendered (cal/render-packet result)]
      (is (not (checks/decl-present? rendered (str cal/disposition)))
          "the C4 predicate itself cannot see the head in a failing record")
      (is (map? (edn/read-string rendered)) "and the record still parses"))))

(deftest a-failing-calibration-writes-a-headless-record
  ;; the write path too: a failing packet is materialized (it is the honest
  ;; result) and the token is not observable anywhere in the file
  (let [rows [(scored-row "r1" 0.99 false) (scored-row "r2" 0.99 false)]
        failing (cal/calibrate declaration
                               {:rows rows :excluded []
                                :window {:next-n 2 :contributing ["r1" "r2"]}})
        dir (.toFile (java.nio.file.Files/createTempDirectory
                      "held-out-cal" (make-array java.nio.file.attribute.FileAttribute 0)))
        out (str dir "/calibration.edn")]
    (with-redefs [cal/snapshot (fn [& _] failing)]
      (cal/write-snapshot! "decl.edn" "obs.edn" "data" out))
    (let [text (slurp out)]
      (is (= :failing (:status (edn/read-string text))))
      (is (not (checks/decl-present? text (str cal/disposition)))))))

;; Case 2 -- the window is the DECLARED one. The real packet has three
;; valid rows and :next-n 2: exactly the first two contribute, and the
;; third is named as excluded with the reason.
(deftest the-window-is-the-declared-one
  (let [{:keys [rows excluded window]}
        (cal/rows-from-observations declaration observations-packet)]
    (is (= 2 (count rows)) "exactly :next-n rows enter the window")
    (is (= ["2026-09-23-1790184736" "2026-09-23-1790187227"]
           (mapv :run-id rows))
        "the first two valid observations, in recorded order")
    (is (= [{:run-id "2026-09-23-1790189901" :reason :outside-declared-window}]
           excluded)
        "the third valid row is named, and why it does not count")
    (is (= ["2026-09-23-1790184736" "2026-09-23-1790187227"]
           (:contributing window)))))

;; Case 3 -- provenance. A row whose close record is absent, or whose
;; run-record digest no longer matches, is retained with a typed reason
;; and does not contribute; and if that leaves fewer than :next-n
;; contributing rows, the result is not :passing however good the metrics.
(deftest a-row-without-its-close-record-cannot-contribute
  (let [valid-rows (filterv #(= :valid (:hygiene %)) (:observations observations-packet))
        real (first valid-rows)
        ;; a row whose RUN RECORD checks out but whose run-id has no close
        ghost (assoc real :run-id "2099-01-01-0000000000")
        packet (assoc observations-packet :observations [ghost (second valid-rows)])
        {:keys [rows]} (cal/rows-from-observations declaration packet)]
    (is (= :close-record-missing (:hygiene-reason (first rows))))
    (is (nil? (:hygiene-reason (second rows)))
        "the intact row still contributes")
    (let [result (cal/calibrate declaration {:rows rows :excluded []
                                             :window {:next-n 2 :contributing []}})]
      (is (not= :passing (:status result))
          "one contributing row out of a declared two is never passing")
      (is (some #{:insufficient-contributing-rows} (:failing-reasons result)))
      (is (not (contains? result :disposition))))))

(deftest a-doctored-digest-cannot-contribute
  (let [valid-rows (filterv #(= :valid (:hygiene %)) (:observations observations-packet))
        real (first valid-rows)
        doctored (assoc-in real [:source :sha256] (apply str (repeat 64 "0")))
        packet (assoc observations-packet :observations [doctored (second valid-rows)])
        {:keys [rows]} (cal/rows-from-observations declaration packet)]
    (is (= :source-digest-mismatch (:hygiene-reason (first rows))))
    (is (not (contains? (cal/calibrate declaration {:rows rows :excluded []
                                                    :window {:next-n 2 :contributing []}})
                        :disposition)))))

;; Case 4 -- the futon2 96f166dc case: the head must be observable whatever
;; key order pprint happens to emit, and the record must still read as EDN.
(deftest the-head-is-observable-whatever-the-key-order
  (doseq [packet [{:schema :x :status :passing :disposition cal/disposition}
                  {:disposition cal/disposition :schema :x :status :passing}
                  {:a 1 :disposition cal/disposition :b 2}
                  (assoc (zipmap (map #(keyword (str "k" %)) (range 12)) (range 12))
                         :disposition cal/disposition)]]
    (let [rendered (cal/render-packet packet)]
      (is (checks/decl-present? rendered (str cal/disposition))
          (str "C4 cannot see the head in: " (pr-str (apply str (take-last 70 rendered)))))
      (is (= cal/disposition (:disposition (edn/read-string rendered)))
          "and it still reads back as EDN"))))

;; and a PASSING packet whose rendering the predicate cannot see is the one
;; artifact never worth writing (stub the renderer to exercise the guard)
(deftest write-refuses-an-unobservable-passing-record
  (let [passing {:schema cal/schema :status :passing :disposition cal/disposition}
        dir (.toFile (java.nio.file.Files/createTempDirectory
                      "held-out-cal" (make-array java.nio.file.attribute.FileAttribute 0)))
        out (str dir "/calibration.edn")]
    (with-redefs [cal/snapshot (fn [& _] passing)
                  cal/render-packet (fn [packet]
                                      (str "{:status :passing :disposition "
                                           (:disposition packet) "}\n"))]
      (let [thrown (try (cal/write-snapshot! "d" "o" "data" out)
                        nil
                        (catch clojure.lang.ExceptionInfo e (ex-data e)))]
        (is (= :disposition-head-not-observable (:held-out/refusal thrown)))
        (is (not (.exists (io/file out))) "and nothing was written")))))

;; Case 5 -- the real inputs. What the metrics ARE over the actual
;; observations packet and the actual close records. No verdict is
;; asserted here; the numbers are reported in the handoff.
(deftest the-real-inputs
  (let [result (cal/snapshot "resources/wm/eig/held-out-split-v2.edn"
                             "resources/wm/eig/held-out-observations.edn")]
    (is (contains? #{:passing :failing} (:status result)))
    (is (= ["2026-09-23-1790184736" "2026-09-23-1790187227"]
           (get-in result [:window :contributing])))
    (is (every? number? (vals (:metrics result))))
    (is (= (:passing-bounds declaration) (:bounds result))
        "the bounds compared against are the declared ones, verbatim")
    (when (= :passing (:status result))
      (is (= cal/disposition (:disposition result)))
      (is (checks/decl-present? (cal/render-packet result) (str cal/disposition))
          "a passing packet's head is observable through the real C4 predicate"))
    (println "REAL CALIBRATION:"
             (pr-str (select-keys result [:status :failing-reasons :metrics
                                          :window :disposition])))))

;; claude-5, reviewing 409d86fb. The realised leg was H(p) - H(y) = H(p),
;; which never mentions y: it returned the same number whether the prediction
;; was right or wrong, and predicted + realised came to ln 2 identically, so
;; "predicted versus realised" was x versus ln 2 - x. Nothing observed could
;; move it. The secondary metrics gate nothing, so this changes no verdict --
;; but the cascade step cites a pattern that separates internal consistency
;; from externally witnessed outcomes, and a realised quantity that witnesses
;; nothing external cannot carry that separation.
(deftest the-realised-leg-moves-with-the-outcome
  (let [rows (fn [realised]
               {:rows [{:run-id "r1" :predicted 15/64 :realised realised}
                       {:run-id "r2" :predicted 19/100 :realised realised}]
                :excluded []})
        as-false (cal/calibrate declaration (rows false))
        as-true (cal/calibrate declaration (rows true))
        realised-of #(get-in % [:metrics :mean-realised-entropy-reduction])
        predicted-of #(get-in % [:metrics :mean-predicted-entropy-reduction])]
    ;; the primary metrics already discriminate; this pins that they do
    (is (< (get-in as-false [:metrics :mean-log-loss])
           (get-in as-true [:metrics :mean-log-loss]))
        "precondition: flipping the outcome worsens log-loss")
    (is (not= (realised-of as-false) (realised-of as-true))
        "the realised leg must differ when what was realised differs")
    (is (> (realised-of as-false) 0.0)
        "outcomes the prediction favoured deliver positive information")
    (is (neg? (realised-of as-true))
        "outcomes it bet against deliver negative information — worse than the prior")
    ;; and the two legs are no longer a constant sum
    (is (not= (+ (predicted-of as-false) (realised-of as-false))
              (+ (predicted-of as-true) (realised-of as-true)))
        "predicted + realised is not a constant the outcome cannot move")))
