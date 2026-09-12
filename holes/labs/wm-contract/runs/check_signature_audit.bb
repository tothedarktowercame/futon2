#!/usr/bin/env bb
;; Breach detector for signature-audit.edn, per its own pattern
;; (violation-signature-before-work): the summary must derive from the
;; vector; a wrong count, duplicate/missing row id, unknown status, or
;; a row without its :signature/:gap text exits 1 with the finding.
;; Usage: bb check_signature_audit.bb [path-to-audit.edn]
(require '[clojure.edn :as edn])

(def path (or (first *command-line-args*)
              "holes/labs/wm-contract/runs/signature-audit.edn"))
(def audit (edn/read-string (slurp path)))
(def rows (:rows audit))

(defn fail! [finding detail]
  (binding [*out* *err*] (prn {:signature-audit/violation finding :detail detail}))
  (System/exit 1))

(when-not (= (range 1 40) (sort (map :row rows)))
  (fail! :row-ids-not-1-to-39 (vec (sort (map :row rows)))))
(doseq [r rows]
  (case (:status r)
    :has-signature (when-not (and (string? (:signature r)) (seq (:signature r)))
                     (fail! :has-signature-without-signature (:row r)))
    :signature-pending (when-not (and (string? (:gap r)) (seq (:gap r)))
                         (fail! :pending-without-gap (:row r)))
    (fail! :unknown-status {:row (:row r) :status (:status r)})))
(let [derived (frequencies (map :status rows))
      summary (:summary audit)]
  (when-not (= {:rows (count rows)
                :has-signature (:has-signature derived 0)
                :signature-pending (:signature-pending derived 0)}
               (select-keys summary [:rows :has-signature :signature-pending]))
    (fail! :summary-does-not-derive-from-vector
           {:derived derived :summary (select-keys summary [:rows :has-signature :signature-pending])})))
(println {:signature-audit/check :pass :rows (count rows)
          :has-signature (:has-signature (frequencies (map :status rows)))
          :signature-pending (:signature-pending (frequencies (map :status rows)))})
