(ns futon2.aif.cascade-revision
  "Typed, append-only revision of a provisional cascade after a mid-run
   blocker. The original cascade is retained; a broader whole-mission skim
   may propose a different cascade, but never installs a declaration."
  (:require [clojure.java.io :as io]
            [clojure.set :as set]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.cascade-feedback :as feedback]
            [futon2.aif.load-identity :as load-identity])
  (:import [java.nio.file Files]))

(load-identity/register! *ns* *file*)

(def blocker-schema :wm/mid-run-cascade-blocker-v1)
(def context-schema :wm/whole-mission-context-v1)
(def revision-schema :wm/provisional-cascade-revision-v1)

(defn candidate-identity [candidate]
  (identity/digest
   (select-keys candidate [:kind :id :target :precedence :want
                           :construction-receipt :interpretation-receipts])))

(defn head-seed
  "Retain the HEAD-scoped locators that seeded the selected cascade."
  [candidate]
  {:schema :wm/cascade-head-seed-v1
   :scope :head
   :target (:target candidate)
   :candidate-sha256 (candidate-identity candidate)
   :locators (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
                   (:observation-locators candidate))})

(defn whole-mission-context
  "Read and pin the entire current mission file. :content is supplied to the
   injected broader-skimming port but is removed from durable receipts."
  [mission]
  (let [file (some-> (:path mission) io/file)]
    (if-not (and file (.isFile file))
      {:status :refused :kind :whole-mission-source-unavailable
       :path (some-> file str)}
      (let [bytes (Files/readAllBytes (.toPath file))]
        {:schema context-schema
         :scope :whole-mission
         :target (:id mission)
         :path (.getCanonicalPath file)
         :sha256 (load-identity/sha256 bytes)
         :byte-count (alength bytes)
         :content (String. bytes "UTF-8")}))))

(defn blocker
  [target stage kind evidence]
  {:schema blocker-schema :status :present :target target
   :stage stage :kind kind :evidence evidence})

(defn- pattern-ids [candidate]
  (->> (:precedence candidate)
       (map #(if (map? %) (:id %) %))
       (remove nil?) vec))

(defn revise
  "Choose a blocker-responsive proposal using recorded pattern evidence.
   PROPOSALS come from the injected whole-mission skim. The revision history
   is append-only and retains both identities; patterns may be added, removed
   or replaced. A failed/no-op extension is typed repair evidence."
  [{:keys [original head-context whole-context blocker proposals
           pattern-feedback]}]
  (let [original-id (when (map? original) (candidate-identity original))
        invalid (cond
                  (not= blocker-schema (:schema blocker)) :typed-blocker-required
                  (not= :present (:status blocker)) :typed-blocker-required
                  (not= :wm/cascade-head-seed-v1 (:schema head-context)) :head-seed-required
                  (not= :head (:scope head-context)) :head-seed-required
                  (not= context-schema (:schema whole-context)) :whole-mission-context-required
                  (not= :whole-mission (:scope whole-context)) :whole-mission-context-required
                  (not (and (string? (:path whole-context))
                            (string? (:sha256 whole-context))))
                  :whole-mission-context-required
                  (not= (:target original)
                        (:target blocker)
                        (:target head-context)
                        (:target whole-context)) :target-mismatch
                  :else nil)
        viable (when-not invalid
                 (->> proposals
                      (filter map?)
                      (filter #(= (:target original) (:target %)))
                      (remove #(= original-id (candidate-identity %)))
                      vec))]
    (if (or invalid (empty? viable))
      {:schema revision-schema :status :refused
       :kind (or invalid :no-distinct-whole-mission-proposal)
       :target (:target original)
       :original {:identity original-id :patterns (pattern-ids original)}
       :blocker blocker
       :whole-context (dissoc whole-context :content)
       :repair-evidence {:status :present
                         :reason (or invalid :whole-mission-skim-found-no-extension)}}
      (let [scored (mapv (fn [candidate]
                           {:candidate candidate
                            :evidence-prior
                            (feedback/pattern-evidence-prior pattern-feedback candidate)})
                         viable)
            chosen (->> scored
                        (sort-by (fn [{:keys [candidate evidence-prior]}]
                                   [(- (:log-factor evidence-prior))
                                    (pr-str (pattern-ids candidate))]))
                        first)
            revised (:candidate chosen)
            revised-id (candidate-identity revised)
            before (set (pattern-ids original))
            after (set (pattern-ids revised))]
        {:schema revision-schema
         :status :revised
         :target (:target original)
         :provisional? true
         :blocker blocker
         :head-context head-context
         :whole-context (dissoc whole-context :content)
         :history [{:ordinal 0 :identity original-id
                    :patterns (pattern-ids original) :scope :head}
                   {:ordinal 1 :identity revised-id
                    :patterns (pattern-ids revised) :scope :whole-mission}]
         :delta {:added (vec (sort-by pr-str (set/difference after before)))
                 :removed (vec (sort-by pr-str (set/difference before after)))
                 :retained (vec (sort-by pr-str (set/intersection before after)))}
         :selection {:basis :pattern-evidence-prior
                     :evidence-prior (:evidence-prior chosen)
                     :proposal-count (count viable)}
         :original original
         :revised revised}))))

(defn revise-from-blocker
  "Runner adapter. PROPOSALS-FN receives the pinned whole mission including
   :content and returns candidate maps. Absent ports refuse honestly."
  [{:keys [mission construction blocker proposals-fn pattern-feedback]}]
  (let [original (:selected-action construction)
        whole (whole-mission-context mission)]
    (try
      (let [proposals (when (and (= context-schema (:schema whole))
                                 (fn? proposals-fn))
                        (proposals-fn {:target (:target original)
                                       :original original
                                       :blocker blocker
                                       :whole-mission whole}))]
        (revise {:original original
                 :head-context (head-seed original)
                 :whole-context whole
                 :blocker blocker
                 :proposals (or proposals [])
                 :pattern-feedback pattern-feedback}))
      (catch Exception e
        {:schema revision-schema :status :refused
         :kind :whole-mission-skim-failed
         :target (:target original)
         :original {:identity (candidate-identity original)
                    :patterns (pattern-ids original)}
         :blocker blocker
         :whole-context (dissoc whole :content)
         :repair-evidence {:status :present :reason :whole-mission-skim-failed
                           :exception-class (.getName (class e))
                           :message (.getMessage e)}}))))

(defn apply-to-construction
  "Present a validated revised action to the existing revision author/reviewer
   prompts. The original remains in the revision receipt; this only changes
   the effective contract for the amendment round."
  [construction revision]
  (if-not (= :revised (:status revision))
    construction
    (let [action (:revised revision)]
      (assoc construction
             :selected-action action
             :precedence (vec (:precedence action))
             :construction-receipt (:construction-receipt action)
             :interpretation-receipts (:interpretation-receipts action)
             :shown (mapv (fn [p] (str (if (map? p) (:id p) p)))
                          (:precedence action))))))
