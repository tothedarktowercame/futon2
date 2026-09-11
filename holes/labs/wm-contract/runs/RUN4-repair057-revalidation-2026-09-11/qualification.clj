(require '[clojure.java.shell :as sh]
         '[clojure.string :as str]
         '[futon2.aif.c-fold-config :as digest])

(def finding "data/wm-repair-obligations/findings/repair-attempt-057-untyped-failure.edn")
(def expected "2ed74a8a61725b0db936063a19d3d4cc2e8080ab34ebab3a808e2822fd28b1c5")
(def first-commit "9ab503bd61be1d63e7a24731e8e8aa285a9e44da")
(def last-commit "3bdc381e76518e69f90077397fa46495da98e61c")

(defn qualifies? [{:keys [reviewer author review-job checks verification-id]}]
  (and (= expected (digest/sha256 (slurp finding)))
       (zero? (:exit (sh/sh "git" "merge-base" "--is-ancestor" first-commit last-commit)))
       (zero? (:exit (sh/sh "git" "merge-base" "--is-ancestor" last-commit "HEAD")))
       (string? verification-id) (not (str/blank? verification-id))
       (string? review-job) (not (str/blank? review-job))
       (not= author reviewer)
       (seq checks) (every? #(true? (:passed? %)) checks)))

(assert (qualifies? {:author "zai-2" :reviewer "codex-10"
                     :review-job "fresh-review-job"
                     :verification-id "repair057-verification-attempt-002"
                     :checks [{:id :timeout-recovery :passed? true}
                              {:id :timeout-exhaustion :passed? true}]}))
(assert (not (qualifies? {:author "zai-2" :reviewer "zai-2"
                          :review-job "fresh-review-job"
                          :verification-id "v" :checks [{:passed? true}]})))
(assert (not (qualifies? {:author "zai-2" :reviewer "codex-10"
                          :review-job "fresh-review-job"
                          :verification-id "v" :checks []})))
(assert (not (qualifies? {:author "zai-2" :reviewer "codex-10"
                          :review-job "fresh-review-job"
                          :verification-id "v" :checks [{:passed? false}]})))
(prn {:qualified-positive true :negative-controls 3 :adopted false})
