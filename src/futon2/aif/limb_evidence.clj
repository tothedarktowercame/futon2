(ns futon2.aif.limb-evidence
  "Pure validation of pre-close limb evidence records and coverage bundles."
  (:require [clojure.string :as str])
  (:import (java.security MessageDigest)
           (java.time Instant)))

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

(defn- output-file! [x path]
  (text! x path)
  (when (or (#{"." ".."} x)
            (str/includes? x "/")
            (str/includes? x "\\"))
    (refuse! :output-file-invalid path))
  x)

(defn- sha256-bytes [bytes]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and 0xff %)) digest))))

(defn validate-limb-receipt [record]
  (let [required #{:schema :repair/id :limb :command :exit :stdout-sha256
                   :stderr-sha256 :recorded-at}
        optional #{:stdout-file :stderr-file}
        actual (set (keys record))]
    (when-not (and (map? record)
                   (every? actual required)
                   (every? #(contains? (into required optional) %) actual))
      (refuse! :shape-invalid [:limb-receipt]
               {:expected-required required :expected-optional optional
                :actual (some-> record keys set)})))
  (when-not (= limb-receipt-schema (:schema record))
    (refuse! :schema-mismatch [:limb-receipt :schema]))
  (text! (:repair/id record) [:limb-receipt :repair/id])
  (keyword! (:limb record) [:limb-receipt :limb])
  (text! (:command record) [:limb-receipt :command])
  (when-not (int? (:exit record))
    (refuse! :exit-invalid [:limb-receipt :exit]))
  (sha256! (:stdout-sha256 record) [:limb-receipt :stdout-sha256])
  (sha256! (:stderr-sha256 record) [:limb-receipt :stderr-sha256])
  (doseq [k [:stdout-file :stderr-file]
          :when (contains? record k)]
    (output-file! (get record k) [:limb-receipt k]))
  (instant! (:recorded-at record) [:limb-receipt :recorded-at])
  record)

(defn validate-limb-receipt-outputs
  "Verify any named companion output files through mandatory injected reads.
  READ-BYTES receives the flat filename, not a caller-supplied path."
  [receipt read-bytes]
  (validate-limb-receipt receipt)
  (when-not (fn? read-bytes)
    (refuse! :output-file-invalid [:read-bytes]))
  (doseq [[file-key digest-key] [[:stdout-file :stdout-sha256]
                                 [:stderr-file :stderr-sha256]]
          :when (contains? receipt file-key)]
    (let [filename (get receipt file-key)
          bytes (try
                  (read-bytes filename)
                  (catch Throwable e
                    (refuse! :output-file-invalid [:limb-receipt file-key]
                             {:cause (.getName (class e))})))
          _ (when-not (instance? (Class/forName "[B") bytes)
              (refuse! :output-file-invalid [:limb-receipt file-key]))
          actual (sha256-bytes bytes)
          expected (get receipt digest-key)]
      (when-not (= expected actual)
        (refuse! :output-digest-mismatch [:limb-receipt digest-key]
                 {:filename filename :expected expected :actual actual}))))
  receipt)

(defn validate-standing-decision [record]
  (when-not (map? record)
    (refuse! :shape-invalid [:standing-decision]))
  (when-not (contains? record :explanation)
    (refuse! :explanation-invalid [:standing-decision :explanation]))
  (exact-map! record
              #{:schema :entity/id :decision :decided-by
                :implementation-author :decided-at :evidence :explanation}
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
  (when-not (and (string? (:explanation record))
                 (>= (count (:explanation record)) 80)
                 (not (str/blank? (:explanation record))))
    (refuse! :explanation-invalid [:standing-decision :explanation]))
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
