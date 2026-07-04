(ns futon2.aif.evidence-emit-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.evidence-emit :as evidence-emit]))

(def ^:private sample-tick
  {:timestamp "2026-07-04T12:34:56Z"
   :mode :multiplied
   :ranked-actions [{:action {:type :address-sorry :target :sorry/x}
                     :G-total 0.25}]
   :decision {:action {:type :address-sorry :target :sorry/x}
              :G-total 0.25}
   :act-gate-verdicts [{:verdict :pass}
                       {:verdict :fail}
                       {:verdict :fail}]
   :belly 3
   :enactment {:mission "M-x"}
   :realized-outcome {:realized-G 0.1
                      :expected-G 0.25}
   :wm-version {:trigger :duree-click-regulated}})

(deftest evidence-entry-shape-test
  (testing "WM ticks become compact coordination step evidence"
    (let [entry (evidence-emit/evidence-entry sample-tick)]
      (is (= "coordination" (:type entry)))
      (is (= "step" (:claim-type entry)))
      (is (= "war-machine" (:author entry)))
      (is (= {:ref/type "war-machine" :ref/id "futon2"} (:subject entry)))
      (is (= ["wm-tick" "wm-click"] (:tags entry)))
      (is (= {:mode :multiplied
              :decision :address-sorry
              :target :sorry/x
              :G 0.25
              :belly 3
              :gates {:pass 1 :fail 2}
              :enacted "M-x"
              :realized-G 0.1
              :expected-G 0.25
              :trigger :duree-click-regulated
              :candidates 1
              :at "2026-07-04T12:34:56Z"}
             (:body entry))))))

(deftest evidence-entry-cron-tag-test
  (testing "wallclock ticks carry the cron basis tag"
    (is (= ["wm-tick" "wm-cron"]
           (:tags (evidence-emit/evidence-entry
                   (assoc-in sample-tick [:wm-version :trigger] :wallclock-cron))))))
  (testing "unspecified triggers do not invent a basis tag"
    (is (= ["wm-tick"]
           (:tags (evidence-emit/evidence-entry
                   (assoc-in sample-tick [:wm-version :trigger] :unspecified)))))))

(deftest emit-noop-when-flag-off-test
  (testing "flag off means no POST is attempted"
    (with-redefs [evidence-emit/enabled? (constantly false)
                  evidence-emit/post-evidence! (fn [_]
                                                 (throw (ex-info "should not post" {})))]
      (is (nil? (evidence-emit/emit! sample-tick))))))

(deftest emit-no-throw-on-post-failure-test
  (testing "POST failures are best-effort and do not escape the tick"
    (with-redefs [evidence-emit/enabled? (constantly true)
                  evidence-emit/post-evidence! (fn [_]
                                                 (throw (ex-info "bus down" {})))]
      (is (nil? (evidence-emit/emit! sample-tick))))))
