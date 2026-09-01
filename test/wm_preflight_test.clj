(ns wm-preflight-test
  (:require [clojure.test :refer [deftest is]]
            [wm-preflight :as preflight])
  (:import [java.time Instant]))

(deftest readiness-is-explicitly-unverified-without-evidence
  (let [claim (preflight/readiness-claim
               (Instant/parse "2026-09-01T00:00:00Z")
               (Instant/parse "2026-09-01T00:00:01Z") nil nil)]
    (is (= :unverified (:event-free? claim)))
    (is (= false (:distinguishable-cause? claim)))
    (is (= {:status :absent :reason :not-declared}
           (:writer-fence claim)))
    (is (re-find #"event-free unverified"
                 (preflight/readiness-label true claim)))))

(deftest writer-fence-requires-evidence-not-an-id-alone
  (is (= {:writer-fence-id "fence-7" :writer-fence-evidence "receipt.json"
          :missions ["M-one" "M-two"]}
         (preflight/parse-cli ["M-one" "--writer-fence" "fence-7"
                               "--writer-fence-evidence" "receipt.json" "M-two"])))
  (is (= :unverified
         (:event-free? (preflight/readiness-claim
                        (Instant/now) (Instant/now) "fabricated" nil)))))
