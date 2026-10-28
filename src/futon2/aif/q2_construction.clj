(ns futon2.aif.q2-construction
  "Source-pinned construction census for the selected task’s inner policies."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.previous-run :as previous-run]
            [futon2.aif.wm.family-selection :as family-selection])
  (:import [java.nio.file Files]))

(def census-schema :wm/q2-construction-census-v1)
(def library-schema :wm/pattern-library-manifest-v1)

(def seal previous-run/seal)
(def intact? previous-run/intact?)
(def digest evidence/value-digest)

(defn library-manifest
  "Every pattern file under ROOT (<root>/<pattern>.flexiarg, the file
  selected-reading pins), by pattern id, with its bytes' sha256; or a typed
  absence."
  [root]
  (let [dir (io/file (str root))]
    (if-not (.isDirectory dir)
      {:schema library-schema :status :absent :reason :library-root-absent :root (str root)}
      (let [prefix (str (.getCanonicalPath dir) "/")
            patterns (into (sorted-map)
                           (for [^java.io.File f (file-seq dir)
                                 :when (and (.isFile f) (str/ends-with? (.getName f) ".flexiarg"))
                                 :let [path (.getCanonicalPath f)]]
                             [(str/replace (subs path (count prefix)) #"\.flexiarg$" "")
                              (identity/sha256 (Files/readAllBytes (.toPath f)))]))]
        (if (empty? patterns)
          {:schema library-schema :status :absent :reason :library-empty :root (str root)}
          {:schema library-schema :status :pinned :root (str root)
           :count (count patterns) :sha256 (digest patterns) :patterns patterns})))))

;; -- the selected task's construction census ------------------------------------

(defn- keyed? [k] (= :wm/reading-policy-v1 (first k)))

(defn- census-entry [family]
  (let [prov (:provenance family)
        ks (map #(family-selection/constructed-key % nil) (:policies family))]
    {:task (str (:target-id family))
     :status (:status family)
     :reading (select-keys prov [:excerpt-digest :analysis-digest])
     :graph-digest (:graph-digest prov)
     :kernel {:retraction (:retraction prov)
              :transition-kernel family-selection/kernel-version}
     :slice (vec (:slice prov))
     :policies (vec (sort-by pr-str (distinct (filter keyed? ks))))
     :failures (into (vec (:failures family))
                     (for [k (remove keyed? ks)]
                       {:kind :policy-arrangement-not-keyable :policy-id (nth k 2)}))}))

(defn construction-census
  "The sealed construction census of the inner FIELD (families-for-field's
  result, policies included) with the LIBRARY manifest pinned at
  construction. A field that was not formed is recorded as such."
  [field library]
  (seal (if (not= :formed (:status field))
          {:schema census-schema :status :absent
           :reason (or (:reason field) :target-policy-field-not-formed)}
          {:schema census-schema :status :recorded
           :library library
           :graph (:graph field)
           :entries (mapv census-entry (:families field))})))

;; -- the record carrier ----------------------------------------------------------
