#!/usr/bin/env clojure
;; RE4 -- exercise the decision-time rationale over recorded decisions.
;;
;; NO LIVE RUN, NO RUN LOCK, NOTHING UNDER data/ (worklist RE4: "exercise via
;; replay or shadow tick, the U49 precedent"). This reads the four recorded
;; decisions of run 2026-09-01-s5 -- the same run RE3's ledger rows are
;; deposited against -- and drives them through the SAME function the live seam
;; calls, `futon2.aif.selection-rationale/emit!`, with the store redirected
;; under runs/RE4-rationale-logging/.
;;
;; WHY emit! AND NOT `write-trace-and-clock!` HERE. The seam persists the trace
;; first and `trace/trace-record` MINTS A FRESH `:timestamp` (trace.clj:539).
;; Pushing recorded records back through it would re-time four decisions from
;; September 1st to now and make the replay irreproducible. The seam's own call
;; site is exercised instead by
;; `test/futon2/aif/selection_rationale_test.clj` (two seam tests, one positive
;; and one on the illegible-decision path), which is where a wall clock does no
;; harm because the assertions are about equality between the record written
;; and the rationale written beside it.
;;
;; DETERMINISM. Every emitted field is read from the recorded record; the only
;; wall-clock-shaped value in the store is each record's own recorded
;; `:timestamp`. Control 3 below re-runs the whole emit and compares SHA-256.
;;
;; Run from the futon2 root:  clojure -M holes/labs/wm-contract/re4_rationale_shadow.clj

(ns re4-rationale-shadow
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [futon2.aif.selection-rationale :as sr]
            [futon2.report.war-machine :as wm]))

(def lab "holes/labs/wm-contract")
(def out-dir (str lab "/runs/RE4-rationale-logging"))
(def store-dir (str out-dir "/store"))
(def negative-store-dir (str out-dir "/negative-control-store"))
(def source-trace (str lab "/runs/2026-09-01-s5/wm-trace-s5.edn"))

(def read-opts
  {:default (fn [t v] {:unread-tag t :value v})})

(defn read-records [path]
  (with-open [r (io/reader path)]
    (mapv #(edn/read-string read-opts %) (line-seq r))))

(defn sha256 [^String s]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")]
    (apply str (map #(format "%02x" %) (.digest md (.getBytes s "UTF-8"))))))

(defn store-manifest [dir]
  (->> (.listFiles (io/file dir))
       (filter #(.isFile ^java.io.File %))
       (sort-by #(.getName ^java.io.File %))
       (mapv (fn [^java.io.File f] [(.getName f) (sha256 (slurp f))]))))

(defn clear! [dir]
  (let [d (io/file dir)]
    (when (.isDirectory d)
      (doseq [^java.io.File f (.listFiles d)] (.delete f)))))

;; ---------------------------------------------------------------------------
;; 1. The shadow decisions: four real recorded selections.

(def records (read-records source-trace))

(def contract (sr/contract-identity))

(defn emit-all! [dir recs]
  (clear! dir)
  (mapv (fn [r] (sr/emit! r {:dir dir :contract contract
                             :trace-path source-trace}))
        recs))

(def emitted (emit-all! store-dir records))
(def manifest-1 (store-manifest store-dir))

;; ---------------------------------------------------------------------------
;; 2. NEGATIVE CONTROL A -- the producer's failure path, in the store.
;;    A decision the rationale cannot read must land as a TYPED ABSENCE, not as
;;    an absent file. Each of the four illegible shapes is exercised, and each
;;    is checked on its SPECIFIC reason keyword rather than merely on being an
;;    absence.

(def illegible-cases
  [{:case :no-decision
    :record (dissoc (first records) :decision)
    :expect :no-decision}
   {:case :decision-with-no-outcome
    :record (assoc (first records) :decision {:reason :nothing-here}
                   :run/id "neg-no-outcome")
    :expect :no-outcome-and-no-refusals}
   {:case :empty-candidate-set
    :record (assoc (first records) :ranked-actions [] :run/id "neg-empty")
    :expect :empty-candidate-set}
   {:case :malformed-candidate-field
    :record (assoc (first records) :ranked-actions "not a list" :run/id "neg-malformed")
    :expect :malformed-record-fields}])

(clear! negative-store-dir)

(def negative-results
  (mapv (fn [{:keys [case record expect]}]
          (let [path (sr/emit! record {:dir negative-store-dir :contract contract})
                written (edn/read-string (slurp path))]
            {:case case
             :file (.getName (io/file path))
             :file-exists? (.isFile (io/file path))
             :status (:rationale/status written)
             :absence-reason (:rationale/absence-reason written)
             :expected-reason expect
             :in-closed-enum? (contains? sr/absence-reasons (:rationale/absence-reason written))
             :defects (sr/defects written)
             :pass? (and (.isFile (io/file path))
                         (= :typed-absence (:rationale/status written))
                         (= expect (:rationale/absence-reason written))
                         (empty? (sr/defects written)))}))
        illegible-cases))

;; ---------------------------------------------------------------------------
;; 3. NEGATIVE CONTROL B -- the same failure path AT THE CALL SITE.
;;    Control A shows the producer types its absences. This shows the wired seam
;;    in `war-machine` writes one, which is the claim the acceptance makes about
;;    the store, not about the producer. Writes to a temp dir; nothing under
;;    data/, nothing committed from here.

(defn seam-control []
  (let [tmp (str (java.nio.file.Files/createTempDirectory
                  "re4-seam" (into-array java.nio.file.attribute.FileAttribute [])))
        judge-output {:belief {} :observation {} :free-energy {}
                      :ranked-actions [{:action {:type :no-op}
                                        :controller-score 0.0 :G-core 0.0 :rank 1}]
                      :mode :multiplied
                      :run/id "re4-seam-control"}
        legible (assoc judge-output
                       :decision {:action {:type :no-op}
                                  :controller-ranking [{:rank 1 :action {:type :no-op}}]
                                  :reason :chosen-by-controller
                                  :selection-boundary :actuation})
        run-one (fn [out dir]
                  (#'wm/write-trace-and-clock! out dir)
                  (let [fs (->> (io/file (str dir "/rationale"))
                                .listFiles (filter #(.isFile ^java.io.File %)) vec)]
                    {:files (count fs)
                     :record (when (seq fs) (edn/read-string (slurp (first fs))))}))
        legible-result (run-one legible (str tmp "/legible"))
        illegible-result (run-one (dissoc judge-output :decision) (str tmp "/illegible"))]
    {:legible
     {:rationale-files (:files legible-result)
      :status (get-in legible-result [:record :rationale/status])
      :outcome (get-in legible-result [:record :rationale/outcome])
      :pass? (and (= 1 (:files legible-result))
                  (= :recorded (get-in legible-result [:record :rationale/status])))}
     :illegible
     {:rationale-files (:files illegible-result)
      :status (get-in illegible-result [:record :rationale/status])
      :absence-reason (get-in illegible-result [:record :rationale/absence-reason])
      :pass? (and (= 1 (:files illegible-result))
                  (= :typed-absence (get-in illegible-result [:record :rationale/status]))
                  (contains? sr/absence-reasons
                             (get-in illegible-result [:record :rationale/absence-reason])))}
     :tmp-dir-note "temporary; not committed and not under data/"}))

(def seam-control-result (seam-control))

;; ---------------------------------------------------------------------------
;; 4. CONTROL -- replay stability. The emit is re-run over the same records and
;;    the two stores compared by SHA-256, so "deterministic" is measured.

(def emitted-2 (emit-all! store-dir records))
(def manifest-2 (store-manifest store-dir))

;; ---------------------------------------------------------------------------
;; 5. The report.

(def store-records (sr/read-store store-dir))

(def report
  {:re4/source {:trace source-trace :records-read (count records)}
   :re4/store {:dir store-dir
               :files (mapv #(.getName (io/file %)) emitted)
               :records (count store-records)}
   :re4/emitted
   (mapv (fn [r]
           {:run-id (:rationale/run-id r)
            :at (:rationale/at r)
            :status (:rationale/status r)
            :outcome (:rationale/outcome r)
            :candidate-set-size (:rationale/candidate-set-size r)
            :chosen (get-in r [:rationale/chosen :action])
            :chosen-controller-rank (get-in r [:rationale/chosen :controller-rank])
            :runner-up (get-in r [:rationale/runner-up :action])
            :runner-up-margin (get-in r [:rationale/runner-up :G-core-margin-over-chosen])
            :refused-count (:rationale/refused-count r)
            :refusal-reasons (vec (sort (distinct (map :reason (:rationale/refused r)))))
            :contract-sha (get-in r [:rationale/contract-sha :git-sha])
            :defects (sr/defects r)})
         store-records)
   :re4/controls
   {:c1-negative-producer-failure-paths
    {:claim "an illegible decision lands in the store as a typed absence with a specific reason"
     :cases negative-results
     :pass? (every? :pass? negative-results)}
    :c2-negative-seam-failure-path
    {:claim "the WIRED SEAM writes a rationale for a legible decision and a typed absence for an illegible one"
     :result seam-control-result
     :pass? (and (get-in seam-control-result [:legible :pass?])
                 (get-in seam-control-result [:illegible :pass?]))}
    :c3-replay-stability
    {:claim "re-emitting the same records reproduces the store byte for byte"
     :manifest-1 manifest-1
     :identical? (= manifest-1 manifest-2)
     :pass? (and (= emitted emitted-2) (= manifest-1 manifest-2))}
    :c4-no-defects
    {:claim "every emitted record validates"
     :defects (vec (mapcat sr/defects store-records))
     :pass? (every? #(empty? (sr/defects %)) store-records)}
    :c5-contract-pin-present
    {:claim "the contract pin RE3 could not find in the run store is carried by every record"
     :pin (:git-sha contract)
     :pin-status (:status contract)
     :records-carrying (count (filter #(= (:git-sha contract)
                                          (get-in % [:rationale/contract-sha :git-sha]))
                                      store-records))
     :pass? (and (= :present (:status contract))
                 (= (count store-records)
                    (count (filter #(= (:git-sha contract)
                                       (get-in % [:rationale/contract-sha :git-sha]))
                                   store-records))))}}})

(def all-pass? (every? :pass? (vals (:re4/controls report))))

(io/make-parents (str out-dir "/x"))
(spit (str out-dir "/shadow-report.edn")
      (with-out-str (pp/pprint (assoc report :re4/all-controls-pass? all-pass?))))

(pp/pprint (assoc report :re4/all-controls-pass? all-pass?))
(println)
(println (if all-pass? "RE4 shadow: ALL CONTROLS PASS" "RE4 shadow: A CONTROL FAILED"))
(flush)
(System/exit (if all-pass? 0 1))
