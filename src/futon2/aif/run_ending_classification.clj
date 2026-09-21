(ns futon2.aif.run-ending-classification
  "Pure, record-only classification of an attempt's ending."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.load-identity :as load-identity]))

(load-identity/register! *ns* *file*)

(def resource "wm/run-ending-classification-v1.edn")
(defn declaration [] (edn/read-string (slurp (io/resource resource))))
(defn- refuse! [kind & [detail]]
  (throw (ex-info "Run-ending classification refused"
                  (merge {:run-ending-classification/refusal kind} detail))))

(defn- validate-declaration! [d]
  (when-not (and (= {:schema :wm/run-ending-classification-declaration-v1
                     :kernel/id :wm/run-ending-classification-kernel-v1
                     :mode :record-only}
                    (select-keys d [:schema :kernel/id :mode]))
                 (= #{:outcome :grounded? :artifact-only? :failure-kind :occurrence
                      :route-attestation :route-attestation-ref}
                    (set (:close-judgment-keys d))))
    (refuse! :unsupported-version))
  d)

(defn projection [judgment d]
  (select-keys judgment (:close-judgment-keys (validate-declaration! d))))

(defn projection-digest [judgment d]
  (identity/digest (projection judgment d)))

(defn- validate-digest! [expected value kind]
  (when (and expected (not= expected (identity/digest value)))
    (refuse! :digest-drift {:source kind :expected expected
                            :actual (identity/digest value)})))

(defn- close-judgment [close]
  (if (= :closed (:checkpoint/type close))
    (get-in close [:payload :judgment])
    close))

(defn- identity-data [close occurrence]
  (let [event? (= :closed (:checkpoint/type close))
        event-id (when event? {:cohort/id (:cohort/id close) :attempt/id (:attempt/id close)})]
    (when occurrence (retention/validate-occurrence occurrence))
    (when (and event-id occurrence
               (or (not= (name (:cohort/id close)) (:cohort/id occurrence))
                   (not= (:attempt/id close) (:attempt/id occurrence))))
      (refuse! :identity-mismatch))
    {:close event-id
     :occurrence (when occurrence
                   (select-keys occurrence [:schema :run/id :cohort/id :attempt/id
                                            :transition/id :action/id :action/value-sha256]))}))

(defn- qualifying-increments [route]
  (filterv #(and (= :matched (:status %))
                 (= :increment (get-in % [:criterion :kind]))
                 (not= :may-not (:valence %))
                 (= :present (get-in % [:attestation :status])))
           (:increments route)))

(defn- classify* [{:keys [close occurrence route-attestation focus-receipt
                           expected-digests declaration]}]
  (let [d (validate-declaration! (or declaration (futon2.aif.run-ending-classification/declaration)))
        judgment (close-judgment close)
        _ (when-not (map? judgment) (refuse! :malformed-close))
        _ (when (some #(= :wm/run-ending-classification-receipt-v1 (:schema %))
                      (filter map? (tree-seq coll? seq (projection judgment d))))
            (refuse! :self-reference))
        missing-close (filterv #(not (contains? judgment %)) (:required-close-keys d))
        route (or route-attestation (:route-attestation judgment))
        occurrence (or occurrence (:occurrence judgment))
        _ (validate-digest! (:close-projection expected-digests) (projection judgment d) :close-projection)
        _ (when route
            (when-not (= (get-in d [:receipt-schemas :route-attestation]) (:schema route))
              (refuse! :unsupported-version {:source :route-attestation}))
            (validate-digest! (:route-attestation expected-digests) route :route-attestation))
        _ (when focus-receipt
            (when-not (= (get-in d [:receipt-schemas :focus-receipt]) (:schema focus-receipt))
              (refuse! :unsupported-version {:source :focus-receipt}))
            (validate-digest! (:focus-receipt expected-digests) focus-receipt :focus-receipt))
        ids (identity-data close occurrence)
        increments (qualifying-increments route)
        typed-failure? (and (false? (:grounded? judgment))
                            (false? (:artifact-only? judgment))
                            (keyword? (:failure-kind judgment)))
        _ (when (> (count increments) 1) (refuse! :duplicate-attestations))
        _ (when (and (seq increments) typed-failure?) (refuse! :increment-failure-contradiction))
        increment (first increments)
        target (get-in increment [:criterion :target])
        facet-rows (filterv #(and (= target (:target %))
                                  (contains? (:facet-map d) (:class %)))
                            (:candidates focus-receipt))
        _ (when (> (count facet-rows) 1) (refuse! :ambiguous-facet-rows))
        facet (:class (first facet-rows))
        class (cond
                (seq missing-close) :unknown
                (and increment facet) (get-in d [:facet-map facet])
                increment :unknown
                typed-failure? :known-typed-failure
                :else :unknown)
        missing (when (= :unknown class)
                  (vec (concat
                        (map #(vector :close-key %) missing-close)
                        (when (and (empty? missing-close) (nil? increment) (not typed-failure?))
                          [:attested-increment])
                        (when (and increment (nil? facet)) [:commit-facets-v1-relation]))))]
    {:schema :wm/run-ending-classification-receipt-v1
     :kernel/id (:kernel/id d) :mode :record-only :status :recorded
     :class class :missing missing
     :failure-kind (when (= class :known-typed-failure) (:failure-kind judgment))
     :identity ids
     :close-projection (projection judgment d)
     :close-projection-sha256 (projection-digest judgment d)
     :sources {:route-attestation
               (if route {:status :present :schema (:schema route)
                          :sha256 (identity/digest route)
                          :verification (:verification route)}
                   {:status :absent :reason :route-attestation-unavailable})
               :focus-receipt
               (if focus-receipt {:status :present :schema (:schema focus-receipt)
                                  :sha256 (identity/digest focus-receipt)}
                   {:status :absent :reason :focus-receipt-unavailable})}
     :attestation (when increment increment)
     :facet (when (and increment facet) (first facet-rows))
     :input-sha256 (identity/digest
                    {:close-projection (projection judgment d)
                     :identity ids
                     :route-attestation-sha256 (some-> route identity/digest)
                     :focus-receipt-sha256 (some-> focus-receipt identity/digest)})}))

(defn classify [input]
  (try (classify* input)
       (catch clojure.lang.ExceptionInfo e
         {:schema :wm/run-ending-classification-receipt-v1
          :kernel/id :wm/run-ending-classification-kernel-v1
          :mode :record-only :status :refused
          :kind (or (:run-ending-classification/refusal (ex-data e))
                    :malformed-close)
          :detail (dissoc (ex-data e) :run-ending-classification/refusal)})))

(defn verify-close [close receipt]
  (let [d (declaration)
        judgment (close-judgment close)]
    (and (= :recorded (:status receipt))
         (map? judgment)
         (not (contains? (projection judgment d) :run-ending-classification))
         (= (:close-projection receipt) (projection judgment d))
         (= (:close-projection-sha256 receipt) (projection-digest judgment d)))))
