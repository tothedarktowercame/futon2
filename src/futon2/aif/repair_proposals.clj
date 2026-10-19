(ns futon2.aif.repair-proposals
  "Open repair findings as evidence-only proposal supply. No interpretation,
   pattern, token production or closure locator is inferred from a finding.
   The existing repair store alone determines which findings remain open."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.repair-obligation :as repair])
  (:import [java.nio.file Files]
           [java.time Instant]))

(defn target-id [finding-id] (str "T-" finding-id))

(defn- timestamp? [x]
  (and (string? x) (try (Instant/parse x) true (catch Exception _ false))))

(defn missing-evidence [finding]
  (let [contract (:discharge-contract finding)
        requirements (:requires contract)]
    (cond-> []
      (not (pos-int? (:repair/schema-version finding))) (conj :repair/schema-version)
      (not (keyword? (:failure-kind finding))) (conj :failure-kind)
      (not (keyword? (:failure-stage finding))) (conj :failure-stage)
      (not (timestamp? (:opened-at finding))) (conj :opened-at)
      (not (map? contract)) (conj :discharge-contract)
      (not (and (vector? requirements) (seq requirements)
                (every? keyword? requirements)
                (= (count requirements) (count (set requirements)))))
      (conj [:discharge-contract :requires])
      (not (contains? repair/artifact-shapes (:artifact-shape contract)))
      (conj [:discharge-contract :artifact-shape]))))

(defn- finding-proposal [root id]
  (when-not (and (string? id) (not (str/blank? id))
                 (not (re-find #"[/\\]" id)) (not (#{"." ".."} id)))
    (throw (ex-info "Repair finding identity is not a safe store key"
                    {:proposal/refusal :repair-id-invalid :repair/id id})))
  (let [file (io/file root "findings" (str id ".edn"))
        bytes (Files/readAllBytes (.toPath file))
        finding (edn/read-string (String. bytes "UTF-8"))
        _ (when-not (= id (:repair/id finding))
            (throw (ex-info "Repair finding identity disagrees with its store key"
                            {:proposal/refusal :repair-id-mismatch :repair/id id})))
        pin {:path (.getCanonicalPath file) :sha256 (evidence/sha256 bytes)}
        missing (missing-evidence finding)
        target (target-id id)]
    (if (seq missing)
      {:decline {:target target :repair/id id :stage :proposal-supply
                 :reason :repair-finding-evidence-missing :missing-evidence missing
                 :finding-source pin}}
      (let [trail (assoc (select-keys finding [:repair/id :repair/schema-version :repair/class
                                             :failure-kind :failure-stage :target :opened-at
                                             :discharge-contract])
                         :finding-source pin
                         :backtrace (if (some? (:backtrace finding))
                                      (assoc pin :status :retained :edn-path [:backtrace])
                                      {:status :absent :reason :not-retained})
                         :closure-observation
                         {:status :unavailable
                          :reason :unversioned-resolution-has-no-admitted-locator
                          :resolution-record {:path (.getCanonicalPath (io/file root "resolutions" (str id ".edn")))
                                              :repair/id id}
                          :required-authority :produced-resolution-evidence})]
        {:proposal {:schema :wm/cascade-proposal-v1 :status :proposed
                    :origin :repair-finding-proposed :target-type :T :target target
                    :repair/id id :proposal-id (evidence/value-digest trail)
                    :evidence trail}}))))

(defn supply
  "Fresh read each tick.  Only lifecycle-qualified actionable IDs may enter
   proposal construction; every such ID reconciles to a proposal or a pinned
   blocked record."
  ([] (supply repair/default-root))
  ([root]
   (let [census (repair/lifecycle-census root)
         actionable-ids (get-in census [:actionable-open :ids])
         entries (mapv #(finding-proposal root %) actionable-ids)
         proposals (vec (keep :proposal entries))
         blocked (vec (keep :decline entries))
         proposal-ids (mapv :repair/id proposals)
         blocked-ids (mapv :repair/id blocked)
         reconciled-ids (vec (sort (concat proposal-ids blocked-ids)))
         buckets (into {}
                       (map (fn [bucket] [bucket (get census bucket)]))
                       [:actionable-open :awaiting-validation
                        :environmental-hold :closed])]
     {:proposals proposals
      ;; Preserve the established caller key while naming its lifecycle role.
      :declines blocked
      :blocked blocked
      :repair-scan
      {:root (.getCanonicalPath (io/file root))
       :read-at (str (Instant/now))
       :lifecycle-census buckets
       :lifecycle-census-schema (:schema census)
       :lifecycle-census-authority :repair-obligation/lifecycle-census
       :lifecycle-census-pin
       {:path "repair-obligation/lifecycle-census"
        :sha256 (evidence/value-digest census)}
       :finding-count (:finding-count census)
       :lifecycle-reconciled? (:reconciled? census)
       :actionable-ids actionable-ids
       :proposal-ids proposal-ids
       :blocked-ids blocked-ids
       :blocked-reasons
       (mapv #(select-keys % [:repair/id :reason :missing-evidence
                              :finding-source]) blocked)
       :actionable-reconciliation
       {:actionable-count (count actionable-ids)
        :proposed-count (count proposal-ids)
        :blocked-count (count blocked-ids)
        :reconciled-ids reconciled-ids
        :reconciled? (= (vec (sort actionable-ids)) reconciled-ids)}}})))
