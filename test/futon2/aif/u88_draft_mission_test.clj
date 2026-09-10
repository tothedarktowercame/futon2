(ns futon2.aif.u88-draft-mission-test
  "Parser validation for the U88 and F10/U83 draft missions, in the same
   convention as futon2.aif.run4-draft-mission-test: the lab drafts live
   outside production mission discovery; a disposable canonical-shaped path
   shows the SAME doc parses as :draft (excluded) and, after the simulated
   lifecycle marker flip, as an ordinary open mission (accepted). Production
   is never activated."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
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

(deftest drafts-are-excluded-from-production-but-parse-as-ordinary-missions
  (doseq [[path id] (map vector drafts draft-ids)]
    (let [body (slurp path)]
      (is (not (contains? (set (map :id (missions/open-missions))) id))
          "the lab draft is outside production mission discovery")
      (with-temp-code-root
        (fn [root]
          (install-isolated! root id body)
          (let [parsed (first (filter #(= id (:id %))
                                      (:missions (missions/load-missions root))))]
            (is (some? parsed) "parses at the canonical mission path")
            (is (= :draft (:status-class parsed)))
            (is (empty? (missions/open-missions {:missions [parsed]}))
                "a DRAFT mission is not open work"))
          ;; simulate, only inside the disposable root, the separate lifecycle
          ;; action that could follow independent review
          (install-isolated!
           root id
           (str/replace body
                        "DRAFT — NON-LIVE; independent review and explicit activation required"
                        "OPEN — fixture episode milestone pending"))
          (let [parsed (first (filter #(= id (:id %))
                                      (:missions (missions/load-missions root))))]
            (is (= :open (:status-class parsed)))
            (is (= [id] (map :id (missions/open-missions {:missions [parsed]})))
                "the simulated OPEN mission is ordinary open work")))))))

(deftest production-still-carries-no-these-missions
  (is (empty? (filter #(contains? (set draft-ids) (:id %))
                      (missions/open-missions)))))
