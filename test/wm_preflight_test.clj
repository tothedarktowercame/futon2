(ns wm-preflight-test
  (:require [clojure.test :refer [deftest is]]
            [wm-preflight :as preflight])
  (:import [java.time Instant]))

(deftest readiness-is-explicitly-fence-conditional
  (let [start (Instant/parse "2026-09-01T00:00:00Z")
        finish (Instant/parse "2026-09-01T00:00:01Z")
        unfenced (preflight/readiness-claim start finish nil)
        fenced (preflight/readiness-claim start finish "quiet-window-42")]
    (is (= :unverified (:event-free? unfenced)))
    (is (= false (:distinguishable-cause? unfenced)))
    (is (= {:status :absent :reason :not-declared} (:writer-fence unfenced)))
    (is (= true (:event-free? fenced)))
    (is (= {:status :held :id "quiet-window-42"} (:writer-fence fenced)))
    (is (re-find #"FENCE-CONDITIONAL quiet-window-42"
                 (preflight/readiness-label true "quiet-window-42")))
    (is (re-find #"event-free unverified"
                 (preflight/readiness-label true nil)))))

(deftest writer-fence-is-an-explicit-cli-declaration
  (is (= {:writer-fence-id "fence-7" :missions ["M-one" "M-two"]}
         (preflight/parse-cli ["M-one" "--writer-fence" "fence-7" "M-two"]))))
