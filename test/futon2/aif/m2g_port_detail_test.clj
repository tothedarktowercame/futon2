(ns futon2.aif.m2g-port-detail-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.meta-self-heal-harness :as harness]
            [futon2.aif.meta-self-heal-production :as production]))

(defn- temp-root []
  (.getCanonicalPath
   (.toFile
    (java.nio.file.Files/createTempDirectory
     "m2g-port-detail"
     (make-array java.nio.file.attribute.FileAttribute 0)))))

(deftest thrown-port-detail-reaches-click-disposition
  (let [env (harness/prepare-environment! (temp-root))
        ports (assoc (harness/fake-ports (:repo env) (atom {}) :success)
                     :author (fn [_] (throw (ex-info "boom-detail" {}))))
        opts (merge (harness/work-order-options env)
                    {:meta-injury-ledger-path (:ledger-path env)
                     :meta-repair-transaction-path (str (:root env) "/transaction.edn")
                     :meta-acceptance-run-fn
                     (constantly {:status :ran :outcome :passed :exit 0
                                  :summary "fixture pass"
                                  :run-pin {:path "fixture://acceptance-run"
                                            :sha256 (apply str (repeat 64 "a"))}})
                     :meta-acceptance-observation {:status :present}
                     :meta-repair-ports ports})
        result (production/route! opts)]
    (is (= :refused (:status result)))
    (is (= :coordinator/port-threw (:reason result)))
    (is (= :author (:phase result)))
    (is (= "boom-detail" (:detail result)))))
