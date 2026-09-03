#!/usr/bin/env bb
;; U42 -- record the U8 reporting gate's verdict on one REAL zaif decision as a
;; typed artifact, so a producer can read the result without the War Machine
;; tick ever touching the Z1 store.
;;
;;   bb holes/labs/wm-contract/u42_gate_receipt.bb [outfile]
;;
;; WHY A RECEIPT AND NOT A LIVE CALL. `report-gate/real-case` reads
;; http://127.0.0.1:7073 (three queries) and picks the newest recorded
;; :zaif-arm-choice by text search. A gauge producer running inside a tick must
;; not make a network call whose latency and availability the tick cannot
;; bound, and must not silently re-pick a different decision every tick. So the
;; gate runs HERE, against the live store, and writes what it saw; the producer
;; (futon2.aif.mission-gauges) reads these bytes and nothing else. No receipt
;; means the producer types an absence -- it never assumes a verdict.
;;
;; READ-ONLY: three GETs against the evidence endpoint; nothing is written to
;; the store and nothing under data/.

(require '[babashka.fs :as fs]
         '[clojure.pprint :as pp])

(def here (str (fs/parent (fs/absolutize *file*))))
(def labs (str (fs/parent here)))
(def gate-path (str (fs/path labs "zaif-harness" "report_gate.clj")))
(def views-path (str (fs/path labs "M-zaif-harness" "z1_views.clj")))

(load-file gate-path)
(load-file views-path)

(def adjudicate (resolve 'report-gate/adjudicate))
(def real-case (resolve 'report-gate/real-case))

(defn sha256 [^String s]
  (let [d (.digest (java.security.MessageDigest/getInstance "SHA-256")
                   (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" %) d))))

(def mission "M-zaif-harness-v1")

(defn -main [& args]
  (let [outfile (or (first args)
                    (str (fs/path here "runs" "U42-producers" "gate-receipt.edn")))
        {:keys [report sources decision]} (real-case mission)
        result (adjudicate report sources)
        body (:evidence/body decision)
        receipt
        {:receipt/row :U42
         :receipt/of :u8-reporting-gate
         :receipt/generated-by "holes/labs/wm-contract/u42_gate_receipt.bb"
         :receipt/at (str (java.time.Instant/now))
         :gate {:name (:gate result)
                :source "holes/labs/zaif-harness/report_gate.clj"
                :source-sha256 (sha256 (slurp gate-path))
                :views-source "holes/labs/M-zaif-harness/z1_views.clj"
                :views-source-sha256 (sha256 (slurp views-path))
                :test "holes/labs/zaif-harness/report_gate_test.clj real-recorded-decision-exposes-known-findings"}
         :store {:endpoint (var-get (resolve 'report-gate/evidence-url))
                 :access :read-only}
         ;; The decision the gate adjudicated, named exactly enough that a
         ;; later reader can tell whether the store has moved under this
         ;; receipt.
         :subject {:mission mission
                   :session-id (:evidence/session-id decision)
                   :turn-id (:turn-id body)
                   :round (:round body)
                   :pairing-key (:pairing-key body)
                   :event (:event body)
                   :arm (:arm body)
                   :recorded-mission (:mission body)}
         :verdict {:ok (:ok result)
                   :failures (:failures result)
                   :claim-count (count (:results result))
                   :failed-claim-count (count (filterv #(= :failed (:verdict %))
                                                       (:results result)))
                   :per-claim (mapv (fn [r]
                                      (cond-> {:claim/type (get-in r [:claim :claim/type])
                                               :verdict (:verdict r)}
                                        (:failure r) (assoc :failure (:failure r))))
                                    (:results result))}}]
    (fs/create-dirs (fs/parent outfile))
    (spit outfile (with-out-str (pp/pprint receipt)))
    (println "wrote" (str outfile))
    (println "  gate ok?" (:ok result) " failures:" (:failures result))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
