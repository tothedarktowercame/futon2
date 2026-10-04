(ns futon2.aif.meta-decision-manifest-test
  (:require [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.test :refer [deftest is]]
            [futon2.aif.meta-decision-manifest :as manifest]
            [futon2.aif.meta-field-observation :as field]
            [futon2.aif.meta-live-outer-selector :as live]))

(defn- git! [repo & args]
  (let [{:keys [exit out err]} (apply shell/sh "git" "-C"
                                      (.getAbsolutePath repo) args)]
    (when-not (zero? exit) (throw (ex-info err {:args args})))
    out))

(defn- pin [path bytes]
  {:path path :sha256 (field/sha256 (.getBytes bytes "UTF-8"))})

(defn- committed-task [repo id]
  (let [rel (str id ".md")
        path (.getAbsolutePath (io/file repo rel))
        text (str "# Mission: " id "\n\n**Status:** OPEN\n")]
    (spit path text)
    (git! repo "add" rel)
    (git! repo "commit" "-m"
          (str "add " id "\n\nAgent-Id: codex-test\n"
               "Agency-Job: invoke-test\nDispatched-By: test-owner\n"))
    {:id id :kind :mission :source (pin path text)}))

(defn- fixture []
  (let [repo (.toFile (java.nio.file.Files/createTempDirectory
                       "meta-manifest"
                       (make-array java.nio.file.attribute.FileAttribute 0)))
        _ (git! repo "init")
        _ (git! repo "config" "user.email" "test@example.invalid")
        _ (git! repo "config" "user.name" "Test")
        tasks [(committed-task repo "M-a") (committed-task repo "M-b")
               (committed-task repo "M-off-map")
               {:id "M-snapshot-only" :kind :mission
                :status-class :unknown :source {:path nil :sha256 nil}}]
        summary {:consistent? true
                 :standards {:s1-regenerates true :s2-evidence true
                             :s3-reconstitution true :s4-honest-holes true
                             :s5-composed true}}
        graph {:section-status {:clusters {:status :ok}}
               :lineage [] :clusters [{:mission "M-a"} {:mission "M-b"}]
               :arrows [] :held []
               :tickets {:items [{:stem "M-a" :mtime-ms 200}
                                 {:stem "M-b" :mtime-ms 100}]}
               :patterns {:edges [{:mission "M-a" :pattern "p/x"}]}}
        agency {:agents {}}
        summary-bytes (json/generate-string summary)
        graph-bytes (json/generate-string graph)
        agency-bytes (json/generate-string agency)
        snapshot {:schema :wm/pipeline-cascade-snapshot-v1
                  :summary-source (pin "summary" summary-bytes)
                  :graph-source (pin "graph" graph-bytes)
                  :agency-source (pin "agency" agency-bytes)
                  :source-bytes {:summary summary-bytes :graph graph-bytes
                                 :agency agency-bytes}
                  :summary summary :graph graph :agency agency}
        receipt (live/select-live {:tasks tasks
                                   :fetch-snapshot (constantly snapshot)})
        candidate-inputs (subvec tasks 0 2)
        candidates (live/task-state-candidates candidate-inputs snapshot)
        registry-content (manifest/registry-snapshot-content tasks)
        registry-snapshot {:source (pin "registry" registry-content)
                           :content registry-content}
        m (manifest/build receipt {:tasks tasks :snapshot snapshot
                                   :registry-snapshot registry-snapshot
                                   :candidate-inputs candidate-inputs
                                   :candidates candidates})]
    {:tasks tasks :snapshot snapshot :manifest m}))

(deftest immutable-manifest-verifies-and-adversarial-changes-refuse
  (let [{:keys [tasks snapshot manifest]} (fixture)
        verify #(manifest/verify %1 {:tasks %2 :snapshot %3})
        snapshot-row (some #(when (= "M-snapshot-only" (:id %)) %)
                           (:field manifest))]
    (is (= :verified (:status (verify manifest tasks snapshot))))
    (is (= :registry-snapshot-only (:authority-kind snapshot-row)))
    (is (= :task-document-source-unavailable
           (get-in manifest [:decision :excluded-reasons "M-snapshot-only"])))
    (is (= :decision-field-identity-mismatch
           (:reason (verify manifest (pop tasks) snapshot))))
    (is (= :decision-field-identity-mismatch
           (:reason (verify manifest (subvec tasks 1) snapshot))))
    (is (= :decision-field-identity-mismatch
           (:reason (verify manifest (conj tasks (assoc (first tasks) :id "M-extra"))
                            snapshot))))
    (is (= :decision-input-source-substitution
           (:reason (verify manifest
                            (assoc-in tasks [0 :source :sha256] (apply str (repeat 64 "0")))
                            snapshot))))
    (is (= :decision-snapshot-pin-mismatch
           (:reason (verify manifest tasks
                            (assoc-in snapshot [:graph-source :sha256]
                                      (apply str (repeat 64 "f")))))))
    (is (= :decision-channel-evidence-mismatch
           (:reason (verify (assoc-in manifest [:candidates 0 :channels
                                                :pipeline-freshness-cost :value]
                                     0.123)
                            tasks snapshot))))
    (is (= :decision-result-mismatch
           (:reason (verify (assoc-in manifest [:decision :chosen-id] "M-b")
                            tasks snapshot))))
    (is (= :decision-input-authority-unavailable
           (:reason (verify (assoc-in manifest [:field 0 :source :path] "wrong.md")
                            tasks snapshot))))
    (is (= :decision-snapshot-only-exclusion-mismatch
           (:reason (verify (assoc-in manifest
                                      [:decision :excluded-reasons "M-snapshot-only"]
                                      :pipeline/not-on-current-map)
                            tasks snapshot))))
    (let [promoted (-> manifest
                       (update :candidate-inputs conj
                               {:id "M-snapshot-only" :kind :mission
                                :source {:path nil :sha256 nil}})
                       (update-in [:decision :support-ids] conj "M-snapshot-only")
                       (update-in [:decision :excluded-ids]
                                  #(vec (remove #{"M-snapshot-only"} %))))]
      (is (= :decision-snapshot-only-task-promoted
             (:reason (verify promoted tasks snapshot)))))))

(deftest ^:slow current-pathless-mission-is-snapshot-authoritative-not-promoted
  (let [tasks (live/live-registry-tasks)
        snapshot (live/fetch-pipeline-snapshot)
        receipt (live/select-live {:tasks tasks
                                   :fetch-snapshot (constantly snapshot)
                                   :retain-manifest? true})
        m (:decision-input-manifest receipt)
        rows (filterv #(= "M-ukrns-wp" (:id %)) (:field m))
        excluded (filterv #(= "M-ukrns-wp" (:id %)) (:excluded receipt))
        candidates (filterv #(= "M-ukrns-wp" (:id %)) (:candidate-inputs m))
        verdict (manifest/verify m {:tasks tasks :snapshot snapshot
                                    :manifest-sha256
                                    (:decision-input-manifest-sha256 receipt)})]
    (is (= :captured (:status m)))
    (is (= :verified (:status verdict)))
    (is (= 1 (count rows)))
    (is (= :registry-snapshot-only (:authority-kind (first rows))))
    (is (= 1 (count excluded)))
    (is (= :task-document-source-unavailable
           (:ineligible-reason (first excluded))))
    (is (empty? candidates))
    (is (string? (get-in receipt [:chosen :id])))))
