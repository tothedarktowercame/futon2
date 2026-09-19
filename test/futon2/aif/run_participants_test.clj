(ns futon2.aif.run-participants-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is use-fixtures]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.hermetic-repair-fixture :as hermetic]
            [futon2.aif.run-participants :as participants]))

(use-fixtures :once hermetic/with-hermetic-stores)

(defn write-record [opts]
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                     "participants-test" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try
      (let [result (#'runner/persist-run-record! (assoc opts :run-record-dir (.getPath dir))
                    "2026-09-19-participants-test" "2026-09-19T00:00:00Z"
                    {:outcome :incomplete :checkpoints {}})]
        (edn/read-string (slurp (:run-record result))))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))

(deftest repair-reviewer-and-configured-reviewer-survive-writing
  (let [opts {:author "author" :reviewer "configured" :repair-reviewer "repair"
              :participants/state (atom nil)}
        reviewer (participants/observe! opts)]
    ;; This is the same selection/reset function used by the runner repair branch.
    (is (= "repair" (participants/select-reviewer! reviewer true "configured" "repair")))
    (let [record (write-record opts)
          expected {:status :present :identity "repair"}
          broken (assoc-in record [:participants :roles :reviewer-of-record]
                           {:status :present :identity "configured"})]
      (is (= expected (participants/read-role record :reviewer-of-record)))
      (is (= {:status :present :identity "configured"}
             (participants/read-role record :configured-reviewer)))
      (is (= {:status :present :identity "author"} (participants/read-role record :author)))
      (is (= expected (participants/read-role record :repair-reviewer)))
      (is (not= record broken))
      (is (not= expected (participants/read-role broken :reviewer-of-record)))
      (println "PARTICIPANTS-TEST-RECORD" (pr-str record)))
    (is (= "configured" (participants/select-reviewer! reviewer false "configured" "repair")))))

(deftest absent-versus-unrecorded
  (let [opts {:author "author" :reviewer nil :repair-reviewer nil
              :participants/state (atom nil)}
        _ (participants/observe! opts)
        record (write-record opts)
        broken (dissoc record :participants)]
    (is (= {:status :absent} (participants/read-role record :reviewer-of-record)))
    (is (not= record broken))
    (is (= {:status :not-observed} (participants/read-role broken :reviewer-of-record)))
    (is (= {:status :not-observed} (participants/read-role (write-record {}) :author)))
    (is (= {:status :incompatible-meaning}
           (participants/read-role (assoc record :participants {:schema :other}) :author)))))

(deftest issuer-three-way-control
  (doseq [identity ["commissioner" :caller-unknown]]
    (let [provenance {:status :present :identity identity :source :wm-click-http-boundary}
          record (write-record {:issuer-provenance provenance})
          broken (update-in record [:participants :roles] dissoc :issuing-caller)]
      (is (= provenance (participants/read-role record :issuing-caller)))
      (is (not= record broken))
      (is (not= provenance (participants/read-role broken :issuing-caller)))))
  (doseq [opts [{} {:issuer-provenance nil}]]
    (is (= {:status :not-observed} (participants/read-role (write-record opts) :issuing-caller)))))

(deftest wrapper-carries-observation-to-writer
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                     "participants-wrapper-test" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (try
      (with-redefs-fn
        {#'runner/ensure-dispatch-seat! (constantly nil)
         #'runner/refuse-on-runner-source-drift! (constantly {:test-only true})
         #'runner/post-wm-status! (fn [& _] nil)
         #'runner/run-opportunity-core!
         (fn [opts]
           (let [reviewer (participants/observe! (runner/config opts))]
             (participants/select-reviewer! reviewer true (:reviewer opts) (:repair-reviewer opts))
             {:outcome :incomplete :checkpoints {}}))}
        (fn []
          (let [result (runner/run-opportunity!
                        {:author "author" :reviewer "configured" :repair-reviewer "repair"
                         :run-record-dir (.getPath dir)})
                record (edn/read-string (slurp (:run-record result)))]
            (is (= "repair" (:identity (participants/read-role record :reviewer-of-record))))
            (is (= "configured" (:identity (participants/read-role record :configured-reviewer)))))))
      (finally (doseq [f (reverse (file-seq dir))] (io/delete-file f true))))))
