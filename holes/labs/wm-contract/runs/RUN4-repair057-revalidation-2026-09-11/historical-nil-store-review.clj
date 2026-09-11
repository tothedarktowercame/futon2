(require '[clojure.test :as ct] '[futon2.aif.full-loop-runner :as runner] '[futon2.aif.full-loop-runner-test :as rt] '[clojure.edn :as edn] '[clojure.java.io :as io]
         '[futon2.aif.c-fold-config :as digest]
         '[futon3c.wm.run4-terminal-evidence-test :as t]
         '[futon3c.wm.run4-historical-projection :as h])
(def captured (atom nil))
(let [actual runner/run-opportunity!] (with-redefs [runner/run-opportunity! (fn [opts] (let [result (actual opts)] (reset! captured {:opts opts :result result}) result))] (ct/test-vars [#'rt/historical-verification-action-commits-without-author-dispatch])))
(let [result (try
 (#'t/fixture
 (fn [{:keys [roots run-file binding-file binding]}]
   (let [record (assoc (edn/read-string (slurp run-file))
                       :run4/controller-attempt-id (:attempt-id t/request))
         _ (spit run-file (pr-str record))
         sha (digest/sha256 (slurp run-file))
         projection {:schema :wm/run4-historical-admission-projection-v1
                     :click/id "click-1" :run/id "run-1"
                     :controller-attempt/id (:attempt-id t/request)
                     :runner-attempt/id (get-in @captured [:result :attempt-id])
                     :execution-attempt nil
                     :requested-pin {:status :authenticated-not-enacted
                                     :identity (:identity t/request) :operator-selection nil}
                     :enacted-action {:type :revalidate-historical-repair}
                     :repair-transition nil
                     :cohort {:cohort-id (get-in @captured [:opts :execution-cohort :cohort-id])
                              :sha256 "unverified"}
                     :repair {:id nil :status :awaiting-validation :verification-id nil :verification-source nil :verification-artifact nil :resolved? false :production-successor-required? false}
                     :source {:run-record (.getPath run-file) :run-record-sha256 sha}}
         file (io/file (:projections roots) "historical.edn")]
     (spit file (pr-str projection))
     (spit binding-file (pr-str (assoc (assoc (dissoc binding :run4/terminal-projection) :outcome :historical-verification-awaiting-validation :attempt/id (get-in @captured [:result :attempt-id])) :run4/historical-projection
                                    {:path (.getPath file)
                                     :sha256 (digest/sha256 (pr-str projection))
                                     :source-sha256 sha})))
     (prn (select-keys (h/read-bundle! (assoc roots
                        :repair-root (:projections roots)
                        :cohort-preregistration (get-in @captured [:opts :execution-cohort :preregistration])
                        :cohort-data-root (get-in @captured [:opts :execution-cohort :data-root])) t/request t/started)
                       [:schema :classification])))))

 :unexpected-admission
 (catch clojure.lang.ExceptionInfo e (:reason (ex-data e))))]
 (assert (= :historical-verification-store-mismatch result) (str "Unexpected result " result))
 (prn {:regression :passed :refusal result}))
