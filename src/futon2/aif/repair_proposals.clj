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
  "Fresh read each tick: dispositions are never cached as open proposals.
   Resolution/dismissal filtering delegates to the real repair-store reader."
  ([] (supply repair/default-root))
  ([root]
   (let [open (repair/open-obligations root)
         entries (mapv #(finding-proposal root (:repair/id %)) open)]
     {:proposals (vec (keep :proposal entries))
      :declines (vec (keep :decline entries))
      :repair-scan {:root (.getCanonicalPath (io/file root))
                    :read-at (str (Instant/now))
                    :open-finding-ids (mapv :repair/id open)
                    :openness-authority :repair-obligation/open-obligations}})))
