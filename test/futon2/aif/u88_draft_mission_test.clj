(ns futon2.aif.u88-draft-mission-test
  "Parser validation distinguishes retained preparation drafts from the
   activated canonical U88 mission. Historical draft bytes remain :draft and
   excluded; production discovery now carries the separately activated OPEN
   mission. The F10/U83 preparation document remains draft-only."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.mission-registry :as missions])
  (:import (java.io File)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(def drafts
  ["holes/labs/wm-contract/runs/RUN4-preparation-2026-09-10/draft-missions/M-u88-contextual-preferences.md"
   "holes/labs/wm-contract/runs/RUN4-preparation-2026-09-10/draft-missions/M-f10-u83-blockage-reconciliation.md"])

(def draft-ids ["M-u88-contextual-preferences"
                "M-f10-u83-blockage-reconciliation"])

(defn- with-temp-code-root [f]
  (let [root (Files/createTempDirectory "u88-draft-mission"
                                        (into-array FileAttribute []))]
    (try
      (f (str root))
      (finally
        (doseq [^File child (reverse (file-seq (io/file (str root))))]
          (.delete child))))))

(defn- install-isolated! [root id body]
  (let [target (io/file root "futon2/holes/missions" (str id ".md"))]
    (io/make-parents target)
    (spit target body)
    (.getAbsolutePath target)))

(deftest retained-preparation-documents-remain-drafts
  (doseq [[path id] (map vector drafts draft-ids)]
    (let [body (slurp path)]
      (with-temp-code-root
        (fn [root]
          (install-isolated! root id body)
          (let [parsed (first (filter #(= id (:id %))
                                      (:missions (missions/load-missions root))))]
            (is (some? parsed) "historical draft parses at a canonical-shaped fixture path")
            (is (= :draft (:status-class parsed)))
            (is (empty? (missions/open-missions {:missions [parsed]})))))))))

(deftest production-distinguishes-activated-u88-from-retained-f10-draft
  (let [open-by-id (into {} (map (juxt :id identity) (missions/open-missions)))]
    (is (= :open (:status-class (get open-by-id "M-u88-contextual-preferences"))))
    (is (nil? (get open-by-id "M-f10-u83-blockage-reconciliation")))))
