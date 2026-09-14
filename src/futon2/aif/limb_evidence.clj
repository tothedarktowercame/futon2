(ns futon2.aif.limb-evidence
  "Pure validation of pre-close limb evidence records and coverage bundles."
  (:require [clojure.string :as str])
  (:import (java.time Instant)))

(def limb-receipt-schema :wm/limb-receipt-v1)
(def standing-decision-schema :wm/target-standing-decision-v1)
(def revision-pair-schema :wm/entity-revision-pair-v1)

(defn- refuse! [code path & [data]]
  (throw (ex-info "Limb evidence refused"
                  (merge {:limb-evidence/refusal code :path path} data))))

(defn- exact-map! [x ks path]
  (when-not (and (map? x) (= ks (set (keys x))))
    (refuse! :shape-invalid path
             {:expected ks :actual (some-> x keys set)}))
  x)

(defn- text! [x path]
  (when-not (and (string? x) (not (str/blank? x)))
    (refuse! :text-invalid path))
  x)

(defn- keyword! [x path]
  (when-not (keyword? x)
    (refuse! :keyword-invalid path))
  x)

(defn- sha256! [x path]
  (when-not (and (string? x) (boolean (re-matches #"[0-9a-f]{64}" x)))
    (refuse! :sha256-invalid path))
  x)

(defn- instant! [x path]
  (try
    (Instant/parse (text! x path))
    (catch clojure.lang.ExceptionInfo e (throw e))
    (catch Throwable _ (refuse! :timestamp-invalid path))))

(defn validate-limb-receipt [record]
  (exact-map! record
              #{:schema :repair/id :limb :command :exit :stdout-sha256
                :stderr-sha256 :recorded-at}
              [:limb-receipt])
  (when-not (= limb-receipt-schema (:schema record))
    (refuse! :schema-mismatch [:limb-receipt :schema]))
  (text! (:repair/id record) [:limb-receipt :repair/id])
  (keyword! (:limb record) [:limb-receipt :limb])
  (text! (:command record) [:limb-receipt :command])
  (when-not (int? (:exit record))
    (refuse! :exit-invalid [:limb-receipt :exit]))
  (sha256! (:stdout-sha256 record) [:limb-receipt :stdout-sha256])
  (sha256! (:stderr-sha256 record) [:limb-receipt :stderr-sha256])
  (instant! (:recorded-at record) [:limb-receipt :recorded-at])
  record)

(defn validate-standing-decision [record]
  (exact-map! record
              #{:schema :entity/id :decision :decided-by
                :implementation-author :decided-at :evidence}
              [:standing-decision])
  (when-not (= standing-decision-schema (:schema record))
    (refuse! :schema-mismatch [:standing-decision :schema]))
  (text! (:entity/id record) [:standing-decision :entity/id])
  (when-not (contains? #{:still-live :resolved} (:decision record))
    (refuse! :standing-decision-invalid [:standing-decision :decision]))
  (text! (:decided-by record) [:standing-decision :decided-by])
  (text! (:implementation-author record)
         [:standing-decision :implementation-author])
  (when (= (:decided-by record) (:implementation-author record))
    (refuse! :standing-decision-not-independent
             [:standing-decision :decided-by]))
  (instant! (:decided-at record) [:standing-decision :decided-at])
  (when-not (and (vector? (:evidence record)) (seq (:evidence record)))
    (refuse! :standing-evidence-invalid [:standing-decision :evidence]))
  (doseq [[i evidence-id] (map-indexed vector (:evidence record))]
    (text! evidence-id [:standing-decision :evidence i]))
  record)

(defn- validate-capture [capture path]
  (exact-map! capture #{:source-path :sha256 :captured-at} path)
  (text! (:source-path capture) (conj path :source-path))
  (sha256! (:sha256 capture) (conj path :sha256))
  (instant! (:captured-at capture) (conj path :captured-at))
  capture)

(defn validate-revision-pair [record]
  (exact-map! record
              #{:schema :entity/id :before :after :dimensions}
              [:revision-pair])
  (when-not (= revision-pair-schema (:schema record))
    (refuse! :schema-mismatch [:revision-pair :schema]))
  (text! (:entity/id record) [:revision-pair :entity/id])
  (validate-capture (:before record) [:revision-pair :before])
  (validate-capture (:after record) [:revision-pair :after])
  (when (= (get-in record [:before :sha256]) (get-in record [:after :sha256]))
    (refuse! :revision-unchanged [:revision-pair]))
  ;; The rubric requires the after revision to FOLLOW the before revision;
  ;; equal or inverted capture instants would let a stale pair pose as one.
  (let [before-at (Instant/parse (get-in record [:before :captured-at]))
        after-at (Instant/parse (get-in record [:after :captured-at]))]
    (when-not (.isBefore before-at after-at)
      (refuse! :revision-order-invalid [:revision-pair :after :captured-at]
               {:before (str before-at) :after (str after-at)})))
  (when-not (and (vector? (:dimensions record)) (seq (:dimensions record)))
    (refuse! :dimensions-invalid [:revision-pair :dimensions]))
  (doseq [[i dimension] (map-indexed vector (:dimensions record))]
    (keyword! dimension [:revision-pair :dimensions i]))
  record)

(defn validate-record [record]
  (case (:schema record)
    :wm/limb-receipt-v1 (validate-limb-receipt record)
    :wm/target-standing-decision-v1 (validate-standing-decision record)
    :wm/entity-revision-pair-v1 (validate-revision-pair record)
    (refuse! :schema-mismatch [:record :schema])))

(defn validate-limb-bundle
  "Validate CONTRACT and RECORDS, then report receipt coverage in the exact
  order of the discharge contract's :requires vector. Missing receipts are a
  typed coverage result, not a validation error."
  [contract records]
  (exact-map! contract #{:requires} [:contract])
  (when-not (and (vector? (:requires contract))
                 (= (count (:requires contract))
                    (count (distinct (:requires contract)))))
    (refuse! :requires-invalid [:contract :requires]))
  (doseq [[i limb] (map-indexed vector (:requires contract))]
    (keyword! limb [:contract :requires i]))
  (when-not (or (set? records) (vector? records))
    (refuse! :records-invalid [:records]))
  (let [validated (mapv validate-record records)
        receipt-limbs (into #{} (comp (filter #(= limb-receipt-schema (:schema %)))
                                     (map :limb))
                            validated)
        required (:requires contract)
        covered (filterv receipt-limbs required)
        absent (filterv #(not (contains? receipt-limbs %)) required)]
    {:status (if (seq absent) :incomplete :covered)
     :covered covered
     :absent absent
     :records validated}))
