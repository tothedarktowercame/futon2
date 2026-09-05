#!/usr/bin/env bb
;; F8 leg 3 slice 2 -- the REVIEWING seat's ADVERSARIAL PLANT SUITE against the
;; delivered convergence checker. Written and committed BEFORE the delivery was
;; read, so the plants are not shaped by the implementation they test.
;;
;; This is not the delivery's negative-controls section. It is the second
;; reader: it plants the cases the packet named AND cases the packet did not,
;; because a checker that passes exactly its own commissioned controls has been
;; tested by its author only.
;;
;; Every plant is on a TEMP COPY. The committed ledger is never written.
;; Usage: bb leg3-slice2-adversarial-plants.bb [checker.bb] [ledger.edn]
(require '[clojure.edn :as edn] '[clojure.string :as str] '[babashka.process :as p])

(def lab (str (System/getenv "HOME") "/code/futon2/holes/labs/wm-contract"))
(def checker (or (first *command-line-args*) (str lab "/convergence_check.bb")))
(def led-path (or (second *command-line-args*) (str lab "/CONVERGENCE.edn")))
(def led (edn/read-string {:default (fn [_ v] v)} (slurp led-path)))
(def tmp (str (System/getProperty "java.io.tmpdir") "/f8-leg3s2-plants"))
(.mkdirs (java.io.File. tmp))

(defn run-on [m nm]
  (let [f (str tmp "/" nm ".edn")]
    (spit f (pr-str m))
    (let [r (p/sh "bb" checker f)]
      {:exit (:exit r) :out (str (:out r) (:err r))})))

(def results (atom []))
;; :refuse / :accept are expectations. :observe is a case whose right answer
;; depends on a decision the packet DELEGATED to the implementer (the nested
;; run identity); it is reported and adjudicated in the review, not scored.
(defn expect! [nm expectation m & musts]
  (let [{:keys [exit out]} (run-on m nm)
        refused? (not= 0 exit)
        missing (vec (remove #(str/includes? out %) musts))
        ok (case expectation
             :refuse (and refused? (empty? missing))
             :accept (and (not refused?) (empty? missing))
             :observe true)]
    (swap! results conj {:plant nm :expect expectation :exit exit :ok ok
                         :missing missing :out (str/trim out)})))

;; --- helpers ----------------------------------------------------------
(defn upd-row [m id f] (update m :rows (fn [rs] (mapv #(if (= id (:id %)) (f %) %) rs))))
(defn drop-row [m id] (update m :rows (fn [rs] (filterv #(not= id (:id %)) rs))))
(defn row-of [m id] (first (filter #(= id (:id %)) (:rows m))))

;; === IDENTITY AND POPULATION ==========================================
(expect! "01-foreign-schema" :refuse (assoc led :schema :wm/something-else))
(expect! "02-schema-absent" :refuse (dissoc led :schema))
;; a quantity the registry declares, dropped from the ledger, is the way a
;; ledger quietly stops being about the whole contract.
(expect! "03-row-deleted" :refuse (drop-row led :temperature) "temperature")
;; and a row the registry does not declare is a quantity nobody adjudicated.
(expect! "04-row-not-in-registry" :refuse
         (update led :rows conj (assoc (row-of led :risk) :id :planted/not-an-equation))
         "planted/not-an-equation")
(expect! "05-duplicate-row-id" :refuse (update led :rows conj (row-of led :risk)) "risk")
(expect! "06-rows-emptied" :refuse (assoc led :rows []))

;; === VERBATIM TRANSCRIPTION ===========================================
;; The file's own :registry-basis says these are transcribed from HEAD. A
;; checker that reads them instead of recomputing them checks nothing at all.
(expect! "07-formal-edited" :refuse
         (upd-row led :precision #(assoc % :formal "Pi_k := 1 / Var(eps_k)")) "precision")
(expect! "08-node-edited" :refuse (upd-row led :action #(assoc % :node :R99)) "action")
(expect! "09-defines-edited" :refuse (upd-row led :depth #(assoc % :defines :TT)) "depth")

;; === THE SCALE ========================================================
(expect! "10-rung-off-scale" :refuse
         (upd-row led :risk #(assoc-in % [:impl-leg :rung] :invented-rung)) "invented-rung")
(expect! "11-rung-order-permuted" :refuse
         (assoc led :rung-order [:type-transcribed :named :formula-transcribed :witnessed
                                 :constructed :wired :validated :run-correlated]))
(expect! "12-rung-order-truncated" :refuse
         (assoc led :rung-order [:named :type-transcribed :formula-transcribed]))

;; === LICENCES =========================================================
(expect! "13-licence-past-eof" :refuse
         (upd-row led :observe #(assoc-in % [:spec-leg :licence]
                                          "mathlib4/DarkTower/WarMachine/MachineObservation.lean:58-999999")))
(expect! "14-licence-file-absent" :refuse
         (upd-row led :observe #(assoc-in % [:impl-leg :licence] "src/futon2/aif/no_such_file.clj:1-2")))
;; futon2 carries two efe.clj -- src/ants/aif/ (short) and src/futon2/aif/
;; (long). A resolver taking the first match answers about the wrong file at a
;; line that exists in neither, and still reports green.
(expect! "15-licence-ambiguous-path" :refuse
         (upd-row led :depth #(assoc-in % [:impl-leg :licence] "efe.clj:629-706")))
(expect! "16-licence-missing" :refuse
         (upd-row led :risk #(update % :spec-leg dissoc :licence)))

;; === THE RUNG RULE ====================================================
;; :witnessed and above assert that a RECORD exhibits the quantity. A readback
;; with no run identity at all must not carry one.
(expect! "17-witnessed-on-a-readback" :refuse
         (upd-row led :precision #(assoc-in % [:impl-leg :rung] :witnessed)) "precision")
(expect! "18-run-correlated-on-a-lean-module" :refuse
         (upd-row led :observe #(assoc-in % [:spec-leg :rung] :run-correlated)) "observe")
;; the delegated decision: :constructed on a record whose run identity is
;; nested, with the row's own statement of that removed.
(expect! "19-nested-basis-statement-removed" :observe
         (upd-row led :policy-set #(assoc-in % [:impl-leg :basis]
                                             "Run c149f9de constructs and scores two cascades.")))
;; and :constructed relocated onto an artifact with no run identity anywhere.
(expect! "20-constructed-on-a-runless-artifact" :refuse
         (upd-row led :policy-set
                  #(assoc-in % [:impl-leg :licence]
                             "holes/labs/wm-contract/runs/F3-node-sim/00-r5-pilot.edn:1-5")))

;; === LEADING LEG ======================================================
(expect! "21-leading-leg-flipped" :refuse
         (upd-row led :risk #(assoc % :leading-leg :spec-ahead)) "risk")
(expect! "22-leading-leg-absent" :refuse (upd-row led :action #(dissoc % :leading-leg)))

;; === CERTIFICATES AND CONVERGENCE =====================================
(expect! "23-converged-on-a-trailing-leg-row" :refuse
         (upd-row led :observe #(assoc % :converged? true)) "observe")
;; the R5 pilot certificate is GREEN but not an accepted run. Green alone is
;; exactly the shortcut the ruling forbids.
(expect! "24-converged-on-green-but-unaccepted" :refuse
         (upd-row led :risk #(assoc % :converged? true)) "risk")
;; and the pin is the authority on which runs are accepted, so a declared
;; :accepted-run? true for a run the pin does not list is a false record.
(expect! "25-accepted-run-declared-but-not-in-pin" :refuse
         (upd-row led :risk #(-> % (assoc-in [:certificate :accepted-run?] true)
                                 (assoc :converged? true))) "risk")
(expect! "26-certificate-with-neither" :refuse
         (upd-row led :observe #(assoc % :certificate {:reason "we are working on it"})))
(expect! "27-certificate-absent" :refuse (upd-row led :observe #(dissoc % :certificate)))

;; === THE CAVEATS ======================================================
;; Four, not three: the fourth is the retired scalar F with no producer at HEAD
;; and the flag-gated policy F-pi. A pin written to C531's prose would let it go.
(expect! "28-fourth-caveat-deleted" :refuse
         (update led :caveats #(vec (butlast %))))
(expect! "29-first-caveat-deleted" :refuse (update led :caveats #(vec (rest %))))
(expect! "30-caveats-emptied" :refuse (assoc led :caveats []))
(expect! "31-caveat-reworded" :refuse
         (update led :caveats #(assoc % 1 "R5 CERTIFICATE LIMIT: resolved, nothing to see here.")))

;; === ACCEPTANCE: things that are not defects ==========================
(expect! "32-unmodified" :accept led)
(expect! "33-as-of-date-changed" :accept (assoc led :as-of "2026-09-06"))
;; row ORDER is not a ruling about precedence; the spec speaks of a row SET.
(expect! "34-rows-reordered" :observe (update led :rows (comp vec reverse)))

;; --- report -----------------------------------------------------------
(println "F8 leg 3 slice 2 -- adversarial plants against" checker)
(println "ledger:" led-path)
(println)
(doseq [{:keys [plant expect exit ok missing]} @results]
  (println (format "%-42s expect %-8s exit %-3d %s%s"
                   plant (name expect) exit
                   (if (= :observe expect) "OBSERVED" (if ok "OK" "FINDING"))
                   (if (seq missing) (str "  missing: " (str/join "," missing)) ""))))
(println)
(doseq [{:keys [plant expect ok out]} @results
        :when (or (= :observe expect) (not ok))]
  (println "---" plant "(" (name expect) ")")
  (println (str/join "\n" (map #(str "    " %) (str/split-lines out)))))
(let [scored (remove #(= :observe (:expect %)) @results)
      bad (remove :ok scored)]
  (println)
  (println (count @results) "plants,"
           (count (filter #(= :observe (:expect %)) @results)) "observed,"
           (count bad) "findings")
  (System/exit (if (seq bad) 1 0)))
