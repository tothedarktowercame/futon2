(ns writer-fence-capability-test
  (:require [cheshire.core :as json]
            [clojure.test :refer [deftest is]]
            [checks.contract-authority-current :as authority]
            [checks.mutable-read-set :as read-set]
            [checks.wm-workspace-gate :as gate]
            [writer-fence-capability :as fence]))

(defn- verified-capability []
  (let [path (java.nio.file.Files/createTempFile
              "writer-fence-capability-" ".json"
              (make-array java.nio.file.attribute.FileAttribute 0))
        receipt {:verdict "FENCE-VERIFIABLE" :fence-id "fence-real"
                 :observation-interval {:started-at "s" :finished-at "f"}
                 :classification {:fence-id "fence-real"
                                  :attested {:status "complete"
                                             :value {:fence-id "fence-real"
                                                     :expires-at "2099-01-01T00:00:00Z"}}}}
        live {:verdict "FENCE-VERIFIABLE" :fence-id "fence-real"
              :observation-interval {:started-at "s2" :finished-at "f2"}}]
    (try
      (spit (.toFile path) (json/generate-string receipt))
      (binding [fence/*run-evidence*
                (fn [_ _] {:exit 0 :out (json/generate-string live)})]
        (fence/verify "fence-real" (str path)))
      (finally (java.nio.file.Files/deleteIfExists path)))))

(deftest all-acceptors-require-the-shared-capability
  (let [forged {:schema :writer-fence-capability/v1
                :verified? true :status :observed-held :id "fabricated"}
        genuine (verified-capability)
        movement {:status :stable}
        result {:observation-interval {:started-at "s" :finished-at "f"}
                :failures []}
        observation {:endpoint-equal? true
                     :interval {:started-at "s" :finished-at "f"}}]
    (is (not (fence/observed-held? forged)))
    (is (= :unverified (:event-free? (gate/gate-event-claim movement forged "s" "f"))))
    (is (= :unverified (:event-free? (authority/event-claim result forged))))
    (is (= :unverified (:event-free? (read-set/assess-claim
                                      observation :event-free
                                      {:writer-fence-capability forged}))))
    (is (fence/observed-held? genuine))
    (is (= true (:event-free? (gate/gate-event-claim movement genuine "s" "f"))))
    (is (= true (:event-free? (authority/event-claim result genuine))))
    (is (= true (:event-free? (read-set/assess-claim
                               observation :event-free
                               {:writer-fence-capability genuine}))))))
