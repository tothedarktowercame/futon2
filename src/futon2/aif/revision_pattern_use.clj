(ns futon2.aif.revision-pattern-use
  "Structured author claim and independent reviewer verification for use of a
  retrieved pattern during the reviewer-triggered revision round."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.action-identity :as identity]))

(def claim-schema :wm/revision-pattern-application-claim-v1)
(def review-schema :wm/revision-pattern-application-review-v1)
(def observation-schema :wm/revision-pattern-use-observation-v1)
(def author-marker "WM_REVISION_PATTERN_APPLICATION:")
(def reviewer-marker "WM_REVISION_PATTERN_VERIFICATION:")

(defn- marked-edn [text marker]
  (when-let [line (some #(when (str/starts-with? % marker) %) (str/split-lines (or text "")))]
    (try (edn/read-string (str/trim (subs line (count marker))))
         (catch Exception _ ::malformed))))

(defn author-claim [text] (marked-edn text author-marker))
(defn reviewer-verification [text] (marked-edn text reviewer-marker))

(defn observation
  [{:keys [author-text reviewer-text pattern pattern-source action-sha256
           artifact-commit reviewer-job-id reviewer-verdict]}]
  (let [claim (author-claim author-text)
        review (reviewer-verification reviewer-text)
        absent (cond
                 (nil? claim) :author-application-claim-absent
                 (= ::malformed claim) :author-application-claim-malformed
                 (nil? review) :reviewer-verification-absent
                 (= ::malformed review) :reviewer-verification-malformed)
        loci (:loci claim)
        loci-digest (when (vector? loci) (identity/digest loci))
        mismatches
        (when-not absent
          (cond-> []
            (not (and (map? pattern-source)
                      (string? (:path pattern-source))
                      (not (str/blank? (:path pattern-source)))
                      (string? (:sha256 pattern-source))
                      (not (str/blank? (:sha256 pattern-source)))))
            (conj :pattern-source-pin)
            (not= claim-schema (:schema claim)) (conj :author-schema)
            (not= review-schema (:schema review)) (conj :reviewer-schema)
            (not= :verified (:verdict review)) (conj :reviewer-verdict)
            (not= :approve reviewer-verdict) (conj :full-loop-review-verdict)
            (or (empty? loci)
                (not-every? #(and (string? (:path %))
                                  (not (str/blank? (:path %)))
                                  (string? (:evidence %))
                                  (not (str/blank? (:evidence %)))) loci))
            (conj :application-loci)
            (not= pattern (:pattern claim) (:pattern review)) (conj :pattern)
            (not= action-sha256 (:dispatched-action-sha256 claim)
                                (:dispatched-action-sha256 review)) (conj :dispatched-action)
            (not= artifact-commit (:artifact-commit claim) (:artifact-commit review))
            (conj :artifact-commit)
            (not= loci-digest (:loci-sha256 review)) (conj :application-loci-digest)))]
    (cond
      absent {:schema observation-schema :status :absent :reason absent}
      (seq mismatches) {:schema observation-schema :status :refused
                        :reason :pattern-use-verification-mismatch
                        :mismatches mismatches}
      :else
      (let [base {:schema observation-schema :kind :reviewer-verified-pattern-application
                  :status :verified :pattern pattern :pattern-source pattern-source
                  :action-sha256 action-sha256 :artifact-commit artifact-commit
                  :application-claims loci
                  :reviewer {:verdict reviewer-verdict :job-id reviewer-job-id
                             :verification-schema review-schema
                             :loci-sha256 loci-digest}}]
        (assoc base :observation-sha256 (identity/digest base))))))
