(ns futon2.aif.calibration-cycle
  "Cascade node R12-4: connect the current calibration producer to a distinct
   consumer through the admission boundary.

   The producer is `futon2.report.war-machine/scan-r12-apparatus` (route-tagged
   R12).  This namespace wraps its scan result into a commission-tied return
   carrying the standing catalogue labels, derives a structural check, and
   passes commission/return/check through
   `futon2.aif.calibration-admission/admit!`.

   Layer 1 (prediction-versus-model-realisation consistency) stays labelled
   never-value-evidence (p4ng/sec-catalog.tex:387).  Layer 2 (prediction
   versus independently witnessed outcome) is reported as NOT AVAILABLE: no
   eligible independent pair exists and the eligibility rule is pending
   prospective reviewed authority (WM-04 provenance packet, 2026-09-16).
   Nothing here may promote an L1 result to value evidence."
  (:require [futon2.aif.calibration-admission :as admission]))

(def layer-1-label
  "Standing catalogue label carried on every R12 return artifact."
  {:layer :R12/layer-1
   :consistency-scope "prediction-versus-model-realisation"
   :value-evidence :never
   :source "p4ng/sec-catalog.tex:387"})

(def layer-2-not-available
  "Honest L2 status until independent prediction/outcome pairs are acquired
   under an authorized eligibility rule (cascade node R12-3)."
  {:layer :R12/layer-2
   :status :not-available
   :reason (str "No eligible independent prediction/outcome pair exists; "
               "eligibility rule pending prospective reviewed authority "
               "(WM-04 provenance packet, 2026-09-16).")})

(defn commission
  [commission-id]
  {:node :R12
   :schema :wm/r12-commission-v1
   :commission/id commission-id
   :target "futon2.report.war-machine/scan-r12-apparatus"})

(defn apparatus-return
  "Wrap the apparatus SCAN into a commission-tied return.  The raw scan stays
   visible under :scan for Layer-1 diagnostics."
  [commission-id return-id scan]
  {:node :R12
   :schema :wm/r12-apparatus-return-v1
   :commission/id commission-id
   :return/id return-id
   :artifact {:schema :wm/r12-apparatus-artifact-v1
              :scan scan
              :layer-1/label layer-1-label
              :layer-2/independent-evidence layer-2-not-available}})

(defn- numeric? [x] (instance? Number x))

(defn check-apparatus
  "Derive the structural CHECK for an apparatus return artifact.  Approves
   only a scan map with an explicit boolean :available?; when available, every
   per-class entry must carry numeric :alpha and :beta and the counts must be
   internally consistent.  Unavailable scans (XTDB unreachable) are valid
   negative results and are approved with :scan/available? false."
  [commission-id return-id artifact]
  (let [scan (:scan artifact)
        verdict
        (cond
          (not (map? scan)) {:verdict :refuse :reason :r12/scan-not-a-map}
          (not (contains? scan :available?))
          {:verdict :refuse :reason :r12/scan-missing-availability}
          (not (boolean? (:available? scan)))
          {:verdict :refuse :reason :r12/scan-availability-not-boolean}
          (not (:available? scan))
          {:verdict :approve :scan/available? false}
          :else
          (let [per-class (:per-class scan)]
            (cond
              (not (map? per-class))
              {:verdict :refuse :reason :r12/scan-missing-per-class}
              (not (every? (fn [[_ c]]
                             (and (map? c)
                                  (numeric? (:alpha c))
                                  (numeric? (:beta c))))
                           per-class))
              {:verdict :refuse :reason :r12/scan-nonnumeric-posterior}
              (and (contains? scan :class-count)
                   (not= (:class-count scan) (count per-class)))
              {:verdict :refuse :reason :r12/scan-count-mismatch}
              :else {:verdict :approve :scan/available? true
                     :scan/class-count (count per-class)})))]
    (merge {:node :R12
            :check/id (str "r12-check-" (or return-id "unknown"))
            :commission/id commission-id
            :return/id return-id}
           verdict)))

(defn admit-apparatus!
  "Run the R12-4 cycle for one apparatus SCAN: build the commission, the
   labelled return and the derived check, and pass all three through
   `admission/admit!`.  Returns admit!'s result augmented with :check so a
   refusal carries its structural reason."
  [commission-id return-id scan]
  (let [c (commission commission-id)
        r (apparatus-return commission-id return-id scan)
        k (check-apparatus commission-id return-id (:artifact r))]
    (assoc (admission/admit! c r k) :check k)))
