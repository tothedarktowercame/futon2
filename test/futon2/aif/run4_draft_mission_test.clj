(ns futon2.aif.run4-draft-mission-test
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.action-proposer :as ap]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.mission-registry :as missions]
            [futon2.report.cascade-lane :as cascade])
  (:import (java.io File)
           (java.nio.file Files)
           (java.nio.file.attribute FileAttribute)))

(def draft-path
  "holes/labs/wm-contract/runs/RUN4-preparation-2026-09-10/draft-missions/M-run4-outer-loop-successor.md")

(def mission-id "M-run4-outer-loop-successor")

(defn- with-temp-code-root [f]
  (let [root (Files/createTempDirectory "run4-draft-mission"
                                        (into-array FileAttribute []))]
    (try
      (f (str root))
      (finally
        (doseq [^File child (reverse (file-seq (io/file (str root))))]
          (.delete child))))))

(defn- install-isolated! [root body]
  (let [target (io/file root "futon2/holes/missions"
                        (str mission-id ".md"))]
    (io/make-parents target)
    (spit target body)
    (.getAbsolutePath target)))

(deftest draft-is-non-live-but-fits-ordinary-mission-lifecycle
  (let [body (slurp draft-path)]
    (is (not (contains? (set (map :id (missions/open-missions))) mission-id))
        "the lab draft is outside production mission discovery")
    (with-temp-code-root
      (fn [root]
        (install-isolated! root body)
        (let [parsed (first (:missions (missions/load-missions root)))]
          (is (= mission-id (:id parsed)))
          (is (= :draft (:status-class parsed)))
          (is (empty? (missions/open-missions {:missions [parsed]}))))

        ;; Simulate, only inside the disposable root, the separate lifecycle
        ;; action that could follow independent review.  Production stays DRAFT.
        (install-isolated!
         root
         (str/replace body
                      "DRAFT — NON-LIVE; independent review and explicit activation required"
                      "OPEN — first fixture experiment milestone pending"))
        (let [parsed (first (:missions (missions/load-missions root)))
              state {:missions (missions/open-missions {:missions [parsed]})}
              action (first (ap/propose missions/mission-enumerator-proposer state))
              seen (atom nil)
              construction
              (with-redefs [cascade/cascade-lane
                            (fn [entries opts]
                              (reset! seen {:entries entries :opts opts})
                              [{:psi "advance the reviewed bounded mission"
                                :shown [] :semilattice [] :policy-holes []}])]
                (runner/construct-for-decision {:action action}))]
          (is (= :open (:status-class parsed)))
          (is (pos? (:open-hole-count parsed)))
          (is (= {:type :advance-mission :target mission-id}
                 (select-keys action [:type :target])))
          (is (true? (fm/can-execute? state action)))
          (is (= action (get-in @seen [:entries 0 :action])))
          (is (= :selected-policy (:construction-kind construction)))
          (is (= action (:selected-action construction))))))))
