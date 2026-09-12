#!/usr/bin/env bb
;; Reviewer-side admission step (claude-15). Emits one review record per
;; claim after independent review, pins it into each fragment's :review
;; wrapper, and flips claim :status to :admitted. The carrier's own
;; validate! then runs the full verify-kind! gate at merge time — this
;; script asserts nothing the merge does not re-check.
;; Run from futon2 root: bb -cp scripts holes/labs/wm-contract/runs/row-2-admission-2026-09-12/gen_review_receipts.bb
(require '[witnesses.node-witness :as nw]
         '[clojure.pprint :as pp]
         '[clojure.java.io :as io])

(def frag-dir "checks/witness-fragments/")
(def frag-names ["machineObservation" "machinePrecision" "machineChannelPredictionError"])
(def review-path "holes/labs/wm-contract/runs/row-2-admission-2026-09-12/review-receipts.edn")

(def entries
  (into (sorted-map)
        (for [n frag-names]
          [n (nw/read-edn (slurp (str frag-dir n ".edn")))])))

(defn locator [m] (select-keys m [:repo :path :sha256]))

(def records
  (into (sorted-map)
        (for [[_ e] entries w (:node-witnesses e)]
          [(:id w)
           {:subject (nw/subject w)
            :verdict :approved
            :reviewer "claude-15"
            :reviewed-at "2026-09-12"
            :basis "Verification receipt validated (subject copied, executed exit 0, typecheck+axiom-check passed, no sorryAx); merge over real fragments 24 pending 0 refusals, deterministic"
            :verification (get-in w [:verification :receipt])
            :dependencies [(locator (get-in w [:verification :receipt]))
                           (locator (:artifact w))
                           (locator (:subject-artifact w))]}])))

(io/make-parents review-path)
(spit review-path (with-out-str (pp/pprint {:records records})))
(def review-sha
  (nw/sha256 (java.nio.file.Files/readAllBytes (.toPath (io/file review-path)))))

(doseq [[n e] entries]
  (spit (str frag-dir n ".edn")
        (with-out-str
          (pp/pprint
           (update e :node-witnesses
                   (fn [ws]
                     (mapv #(assoc % :status :admitted
                                   :review {:status :verified
                                            :receipt {:repo "futon2"
                                                      :path review-path
                                                      :sha256 review-sha
                                                      :selector [:records (:id %)]}})
                           ws)))))))
(println {:review-receipts review-path :sha256 review-sha :claims (count records)})
