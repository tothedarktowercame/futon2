(require '[clojure.test :as t]
         '[clojure.edn :as edn]
         '[futon2.aif.full-loop-runner :as runner]
         '[futon2.aif.full-loop-runner-test :as rt]
         '[futon3c.wm.run4-historical-projection :as projection])
(let [actual runner/run-opportunity!
      captured (atom nil)
      root (str (java.nio.file.Files/createTempDirectory "historical-producer-review" (make-array java.nio.file.attribute.FileAttribute 0)))
      requested {:status :authenticated-not-enacted
                 :identity {:series-id "review-series" :trial-id "review-trial"
                            :pin-sha256 (apply str (repeat 64 "a"))}}]
  (with-redefs [runner/run-opportunity!
                (fn [opts]
                  (let [r (actual (assoc opts :run4/requested-pin requested
                                        :click-id "review-click" :run-record-dir root))]
                    (reset! captured r) r))]
    (t/test-vars [#'rt/historical-verification-action-commits-without-author-dispatch]))
  (let [r @captured]
    (prn {:outcome (:outcome r)
          :run-record-status (:run-record-status r)
          :run-record-keys (when (:run-record r) (set (keys (edn/read-string (slurp (:run-record r))))))
          :actual-selection-ground (get-in r [:checkpoints :selection :ground])
          :projection (try (projection/projection "review-click" r)
                           (catch Exception e (ex-data e)))})))
