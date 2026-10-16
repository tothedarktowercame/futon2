;; Produce Q4 run-fact exports through the real runner and the Lean terms
;; wm_check_run_facts generates from them. Run from the futon2 worktree;
;; Q4_LEAN_OUT names the output directory.
;;
;; From records:
;; - Single: one run, two compared policies of horizon 2 (holds)
;; - Excluded: a second run, one policy excluded by Q6 (holds)
;; - MixedHorizons: p/a + p/c, horizons 2 and 3 under the per-step
;;   normalization (holds)
;; - TermMissing: Excluded with one receipt's information-gain term
;;   removed and the carrier recomputed (violated)
;; - MixedHorizonsRaw: MixedHorizons with every receipt re-declared raw
;;   cumulative, consistently (violated)
;; - MixedNormalization: MixedHorizons with one receipt re-declared raw
;;   cumulative (violated)
;; - NormalizationUnknown, NormalizationAbsent: one receipt's declaration
;;   unrecognized or removed (Q4 unverifiable)
;; From an edited export, since the exporter refuses an edited carrier:
;; - OmittedPolicyRecord, ForeignPolicyRecord: MixedHorizons' export with
;;   one policy record dropped, or replaced by a policy that was not
;;   compared (violated)
(require 'futon2.aif.wm.q4-preference-route-test '[cheshire.core :as json] 'wm-check-run-facts
         'clojure.string 'futon2.aif.load-identity)
(in-ns 'futon2.aif.wm.q4-preference-route-test)
(require '[cheshire.core :as json] 'wm-check-run-facts)
(defn export-of [record]
  (json/parse-string (json/generate-string (facts/facts-for-record record "r" {}))))
(defn emit-export [out label export]
  (let [path (str out "/Q4" label ".lean")]
    (spit path (wm-check-run-facts/lean-source export (str "q4" label)))
    (println label path)))
(defn emit [out label record]
  (println label (holds? record))
  (emit-export out label (export-of record)))
(def out (or (System/getenv "Q4_LEAN_OUT") "test/fixtures/q4-lean"))
(def worktree (str (System/getProperty "user.dir") "/"))
(with-redefs [futon2.aif.load-identity/required-sources
              (into {} (map (fn [[n p]] [n (clojure.string/replace p "/home/joe/code/futon2/" worktree)]))
                    futon2.aif.load-identity/required-sources)]
  (futon2.aif.hermetic-repair-fixture/with-hermetic-stores
   (fn []
     (futon2.test-support.runner-fixture/with-hermetic-traces
      (fn []
        (binding [futon2.aif.full-loop-runner/*wm-status-reporting?* false]
          (route/with-empty-learning-ledger
           (fn []
             (let [fx (route/fixture)
                   dir (route/temp-dir "q4-lean-")
                   one (:record (click! fx dir))
                   two (:record (click! fx dir))
                   mixed (:record (click! (route/fixture ["p/a" "p/c"]) (route/temp-dir "q4-lean-")))
                   mixed-export (export-of mixed)
                   ps (get-in mixed-export ["facts" "policyPreferences"])]
               (emit out "Single" one)
               (emit out "Excluded" two)
               (emit out "MixedHorizons" mixed)
               (emit out "TermMissing"
                     (reseal two #(assoc-in (vec %) [0 :g-terms :information-gain] nil)))
               (emit out "MixedHorizonsRaw" (reseal mixed #(mapv raw-cumulative %)))
               (emit out "MixedNormalization" (reseal mixed #(r0 % raw-cumulative)))
               (emit out "NormalizationUnknown"
                     (reseal mixed #(r0 % (fn [r] (assoc-in r [:normalization :declaration :law]
                                                            :per-step-capacity)))))
               (emit out "NormalizationAbsent"
                     (reseal mixed #(r0 % (fn [r] (dissoc r :normalization)))))
               (emit-export out "OmittedPolicyRecord"
                            (assoc-in mixed-export ["facts" "policyPreferences"] [(first ps)]))
               (emit-export out "ForeignPolicyRecord"
                            (assoc-in mixed-export ["facts" "policyPreferences"]
                                      [(first ps) (assoc (second ps) "policy" "[\"foreign\"]")])))))))))))
