(ns wm-preflight-test
  (:require [clojure.test :refer [deftest is]]
            [cheshire.core :as json]
            [wm-preflight :as preflight])
  (:import [java.time Instant]))

(deftest readiness-is-explicitly-fence-conditional
  (let [start (Instant/parse "2026-09-01T00:00:00Z")
        finish (Instant/parse "2026-09-01T00:00:01Z")
        absent {:verified? false :status :absent :reason :not-declared}
        held {:verified? true :status :observed-held :id "quiet-window-42"
              :receipt-sha256 "abc"}
        unfenced (preflight/readiness-claim start finish absent)
        fenced (preflight/readiness-claim start finish held)]
    (is (= :unverified (:event-free? unfenced)))
    (is (= false (:distinguishable-cause? unfenced)))
    (is (= {:status :absent :reason :not-declared} (:writer-fence unfenced)))
    (is (= true (:event-free? fenced)))
    (is (= :observed-held (get-in fenced [:writer-fence :status])))
    (is (re-find #"FENCE-VERIFIED quiet-window-42"
                 (preflight/readiness-label true held)))
    (is (re-find #"event-free unverified"
                 (preflight/readiness-label true absent)))))

(deftest writer-fence-requires-evidence-not-an-id-alone
  (is (= {:writer-fence-id "fence-7" :writer-fence-evidence "receipt.json"
          :missions ["M-one" "M-two"]}
         (preflight/parse-cli ["M-one" "--writer-fence" "fence-7"
                               "--writer-fence-evidence" "receipt.json" "M-two"])))
  (is (= :invalid (:status (preflight/verify-fence-evidence "fabricated" nil))))
  (is (= :unverified
         (:event-free? (preflight/readiness-claim
                        (Instant/now) (Instant/now)
                        (preflight/verify-fence-evidence "fabricated" nil))))))

(deftest evidence-content-not-filename-binds-the-fence
  (let [path (java.nio.file.Files/createTempFile "wm-fence" ".json"
                                                  (make-array java.nio.file.attribute.FileAttribute 0))
        receipt {:verdict "FENCE-VERIFIABLE" :fence-id "fence-7"
                 :observation-interval {:started-at "2026-09-01T00:00:00Z"
                                        :finished-at "2026-09-01T00:00:01Z"}
                 :classification
                 {:fence-id "fence-7"
                  :attested {:status "complete"
                             :value {:fence-id "fence-7"
                                     :expires-at "2099-01-01T00:00:00Z"}}}}]
    (try
      (spit (.toFile path) (json/generate-string receipt))
      (let [live {:verdict "FENCE-VERIFIABLE" :fence-id "fence-7"
                  :observation-interval {:started-at "s" :finished-at "f"}}
            verified (binding [preflight/*run-fence-evidence*
                               (fn [_ _] {:exit 0 :out (json/generate-string live)})]
                       (preflight/verify-fence-evidence "fence-7" (str path)))
            breached (binding [preflight/*run-fence-evidence*
                               (fn [_ _] {:exit 1
                                          :out (json/generate-string
                                                {:verdict "FENCE-BREACH"
                                                 :fence-id "fence-7"})})]
                       (preflight/verify-fence-evidence "fence-7" (str path)))]
        (is (:verified? verified))
        (is (= :observed-held (:status verified)))
        (is (false? (:verified? breached)))
        (is (= :unverified (:status breached)))
        (is (some #{:live-fence-check-failed} (:problems breached))))
      (finally (java.nio.file.Files/deleteIfExists path)))))
