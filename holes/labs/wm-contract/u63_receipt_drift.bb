#!/usr/bin/env bb
;; U63 -- RECEIPT DRIFT: what a tension added to the ledger does to the receipts
;; of runs already deposited.
;;
;;   bb holes/labs/wm-contract/u63_receipt_drift.bb              ; the curated ledger
;;   FUTON_TENSION_LEDGER=<other> bb .../u63_receipt_drift.bb    ; any other ledger
;;
;; WHY THIS EXISTS AS A SCRIPT AND NOT AS A PASTED TRANSCRIPT. :U63 mints into
;; the curated ledger, and `:live-derivation` folds the WHOLE ledger into EVERY
;; run's receipt (u41_tension_ledger.bb:712-722), so every deposited run replays
;; differently afterwards. The row is required to REPORT that rather than repair
;; it, and a report a reviewer cannot re-derive is an assertion. Pointing
;; FUTON_TENSION_LEDGER at the pre-mint ledger --
;;
;;   git show <sha>:holes/labs/wm-contract/tension-ledger.edn > /tmp/before.edn
;;
;; -- reproduces the BEFORE column of RECEIPT-U63-mint.md section 3.
;;
;; READ-ONLY. It calls u41's report functions; it never deposits, never writes a
;; receipt, and never touches a ledger. No tick, no run lock, no network.

(require '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.pprint :as pp])

(def repo-root
  (-> (java.io.File. *file*) .getAbsoluteFile .getParentFile .getParentFile
      .getParentFile .getParentFile .getPath))

;; the sole reader, loaded rather than reimplemented -- the point is to replay
;; the receipt the deposit path would build, not a second version of it
(load-file (str (io/file repo-root "holes/labs/wm-contract/u41_tension_ledger.bb")))

(def deposited
  "The runs that HAVE a deposited :tensions-cashed row. Read off the run-era
   ledger rather than listed here, so a run deposited later is picked up."
  (->> (:rows (edn/read-string (slurp (io/file repo-root "holes/labs/wm-contract/run-era-ledger.edn"))))
       (filter #(= :tensions-cashed (:row/check-id %)))
       (map :row/run-id)
       sort vec))

(let [lp (str @(resolve 'user/ledger-path))
      lt (slurp lp)
      l (edn/read-string lt)
      defects (@(resolve 'user/validate) l)
      statuses (mapv #((resolve 'user/current-status) l (:tension/id %)) (:tensions l))
      birth (@(resolve 'user/birth-rule) l)
      ctrls (into (sorted-map) (@(resolve 'user/controls) l defects birth))]
  (println "ledger" lp)
  (println "  tensions" (count (:tensions l)) " events" (count (:events l))
           " structural-carriers"
           (count (filter #(seq ((resolve 'user/structured-run-keys) %)) (:tensions l)))
           " defects" (count defects))
  (doseq [rid deposited]
    (let [ticks (or ((resolve 'user/run-tick-ids) rid) [])
          scan ((resolve 'user/run-provenance-scan) lt l rid ticks)
          r ((resolve 'user/deposit-receipt) rid l defects statuses birth ctrls scan)
          f (io/file repo-root "holes/labs/wm-contract/runs/RE6-check-deposits"
                     (str "tensions-cashed-" rid ".edn"))
          committed (edn/read-string (slurp f))
          moved (vec (sort (map str (for [k (distinct (concat (keys r) (keys committed)))
                                          :when (not= (get r k) (get committed k))]
                                      k))))]
      (println (format "  %-26s replays-identically %-5s  deposited %-14s replays %-14s  tensions %s->%s"
                       rid
                       (= (with-out-str (pp/pprint r)) (slurp f))
                       (:verdict-deposited committed)
                       (:verdict-deposited r)
                       (get-in committed [:live-derivation :tensions])
                       (get-in r [:live-derivation :tensions])))
      (println (format "  %-26s fields-moved %s" "" (pr-str moved))))))
