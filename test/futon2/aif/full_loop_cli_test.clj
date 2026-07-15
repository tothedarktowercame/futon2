(ns futon2.aif.full-loop-cli-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-cli :as cli]
            [futon2.aif.full-loop-runner :as runner]))

(deftest continuous-count-is-parsed-and-bounds-opportunities
  (let [calls (atom [])]
    (with-redefs [runner/run-opportunity!
                  (fn [opts]
                    (swap! calls conj opts)
                    {:outcome :grounded-change})]
      (with-out-str
        (#'cli/continuous! {:count "3"
                            :interval-seconds "0"
                            :author "codex-6"
                            :reviewer "claude-7"})))
    (is (= 3 (count @calls)))
    (is (every? #(= :duree-click-continuous (:trigger %)) @calls))
    (is (every? #(= "codex-6" (:author %)) @calls))
    (is (every? #(= "claude-7" (:reviewer %)) @calls))))
