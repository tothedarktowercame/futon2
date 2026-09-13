(ns futon2.aif.machine-portfolio-restriction
  "Pure E2a restriction of the complete R6 support by a freshly resolved,
   canonical R11 portfolio. The public boundary accepts only the independent
   E1 resolver configuration; caller-shaped selected ids and accounting are
   not inputs. This module neither selects an action nor enacts one."
  (:require [clojure.set :as set]
            [futon2.aif.machine-budget-authority :as authority]))

(def schema-version :wm/r11-r6-portfolio-restriction-v1)

(defn- refuse! [kind message data]
  (throw (ex-info message (assoc data :refusal kind))))

(defn- proposal-ids [rows]
  (mapv :id rows))

(defn- verify-e1! [e1]
  (when-not (= :wm/r6-r11-mapping-v1 (:schema/version e1))
    (refuse! :e2a/e1-unverified "E2a requires the verified E1 mapping schema" {}))
  (when-not (and (= :wm/r6-r11-authority-resolver-v1
                    (get-in e1 [:verification :resolver/version]))
                 (= 5 (count (get-in e1 [:verification :sources]))))
    (refuse! :e2a/e1-unverified "E1 byte-resolution evidence is incomplete" {}))
  (let [support (:ordered-support e1)
        support-ids (mapv :candidate/id support)
        support-set (set support-ids)
        response (:response e1)
        selected (:selected response)
        rejected (:rejected response)
        selected-list (proposal-ids selected)
        rejected-list (proposal-ids rejected)
        selected-set (set selected-list)
        rejected-set (set rejected-list)
        accounting (:accounting e1)
        accounting-ids (mapv :candidate/id accounting)
        support-by-id (into {} (map (juxt :candidate/id identity) support))]
    (when-not (and (vector? support) (seq support)
                   (= (count support-ids) (count (distinct support-ids))))
      (refuse! :e2a/support-invalid "E1 full support is empty or has duplicate ids" {}))
    (when-not (= (count selected-list) (count selected-set))
      (refuse! :e2a/duplicate-selected-id "R11 selected occurrences contain duplicates"
               {:selected-ids selected-list}))
    (when-not (= (count rejected-list) (count rejected-set))
      (refuse! :e2a/duplicate-rejected-id "R11 rejected occurrences contain duplicates"
               {:rejected-ids rejected-list}))
    (when-let [unknown (seq (remove support-set (concat selected-list rejected-list)))]
      (refuse! :e2a/unknown-portfolio-id "R11 portfolio names an unknown occurrence"
               {:unknown-ids (vec unknown)}))
    (when-let [conflicts (seq (set/intersection selected-set rejected-set))]
      (refuse! :e2a/conflicting-disposition
               "An occurrence cannot be both selected and rejected"
               {:conflicting-ids (vec conflicts)}))
    (when-not (= support-set (set/union selected-set rejected-set))
      (refuse! :e2a/omitted-portfolio-id
               "Selected plus rejected must cover the full R6 support"
               {:support-ids support-ids :selected-ids selected-list
                :rejected-ids rejected-list}))
    (when-not (= support-ids accounting-ids)
      (refuse! :e2a/accounting-order-mismatch
               "E1 accounting must cover full support in source order"
               {:support-ids support-ids :accounting-ids accounting-ids}))
    (doseq [proposal (concat selected rejected)]
      (let [source (support-by-id (:id proposal))]
        (when-not (= (:action source) (:action proposal) (:proposal/action proposal))
          (refuse! :e2a/action-mutation "Portfolio action differs from R6 source bytes"
                   {:candidate/id (:id proposal)}))
        (when-not (= (:rank source) (:rank proposal))
          (refuse! :e2a/rank-mutation "Portfolio rank differs from R6 source order"
                   {:candidate/id (:id proposal)}))))
    (doseq [row accounting]
      (let [id (:candidate/id row)
            source (support-by-id id)
            expected (if (contains? selected-set id) :selected :rejected)]
        (when-not (= (:action source) (:action row))
          (refuse! :e2a/forged-accounting "Accounting action differs from R6 source bytes"
                   {:candidate/id id}))
        (when-not (= expected (:disposition row))
          (refuse! :e2a/forged-accounting "Accounting disposition disagrees with R11"
                   {:candidate/id id :expected expected
                    :actual (:disposition row)}))))
    (when-not (seq selected-set)
      (refuse! :e2a/empty-approved-support
               "R11 approved no R6 occurrence; selection cannot continue" {}))
    {:support support :support-ids support-ids :selected-set selected-set}))

(defn- restrict-e1 [e1]
  (let [{:keys [support selected-set]} (verify-e1! e1)
        approved (filterv #(contains? selected-set (:candidate/id %)) support)
        excluded (filterv #(not (contains? selected-set (:candidate/id %))) support)]
    {:schema/version schema-version
     :scope (:scope e1)
     :identity (:identity e1)
     :source {:declaration :hierarchical-budget/arbitrate
              :field :selected-occurrences
              :e1-verification (:verification e1)}
     :target {:declaration :machinePolicySet :field :pi-approved}
     :full-support support
     :approved-support approved
     :excluded-support excluded
     :approved-occurrence-ids (mapv :candidate/id approved)
     :excluded-occurrence-ids (mapv :candidate/id excluded)
     :r11-response (:response e1)
     :e1-accounting (:accounting e1)
     :restriction-law :stable-occurrence-filter-preserving-source-order-and-action-bytes}))

(defn restrict-portfolio
  "Resolve E1 again from its independent source configuration and restrict the
   full R6 support to the canonical R11 selected occurrence set."
  [resolver-config]
  (let [e1 (authority/resolve-and-map resolver-config)
        output (restrict-e1 e1)]
    (assoc output :replay/receipt {:schema/version schema-version
                                   :resolver-config resolver-config
                                   :output output})))

(defn replay
  "Re-resolve all E1 source bytes and compare the complete E2a output."
  [{receipt-version :schema/version :keys [resolver-config output]}]
  (when-not (= schema-version receipt-version)
    (refuse! :e2a/replay-schema-unsupported "Unsupported E2a replay schema"
             {:schema/version receipt-version}))
  (let [actual (dissoc (restrict-portfolio resolver-config) :replay/receipt)]
    {:replay/identical? (= output actual)
     :replay/expected output
     :replay/actual actual}))
