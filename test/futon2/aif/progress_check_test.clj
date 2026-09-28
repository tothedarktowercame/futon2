(ns futon2.aif.progress-check-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.progress-check :as progress]))

(defn click
  ([id target produced needs]
   (click id target produced needs []))
  ([id target produced needs advanced]
   {:click-id id :target target :advanced advanced
    :produced produced :needs needs}))

(deftest later-click-on-the-same-target-consumes-produced-token
  (let [result (progress/check-flight
                [(click "c1" "M-x" #{:roles-named} #{})
                 (click "c2" "M-x" #{} #{:roles-named})])]
    (is (= {:click-id "c1" :progress :token-consumed
            :consumed [{:token ["M-x" :roles-named] :by "c2"}]}
           (first (:clicks result))))))

(deftest same-token-on-a-different-target-is-not-consumption
  (let [verdict (first (:clicks
                        (progress/check-flight
                         [(click "c1" "M-x" #{:roles-named} #{})
                          (click "c2" "M-y" #{} #{:roles-named})])))]
    (is (= {:click-id "c1" :no-progress :produced-not-consumed
            :produced #{["M-x" :roles-named]}}
           verdict))))

(deftest same-click-needs-do-not-count-as-later-consumption
  (let [verdict (first (:clicks
                        (progress/check-flight
                         [(click "c1" "M-x" #{:binding-recorded}
                                 #{:binding-recorded})])))]
    (is (= :produced-not-consumed (:no-progress verdict)))
    (is (= #{["M-x" :binding-recorded]} (:produced verdict)))))

(deftest unknown-later-needs-make-an-unmatched-token-unverifiable
  (let [verdict (first (:clicks
                        (progress/check-flight
                         [(click "c1" "M-x" #{:roles-named} #{})
                          (click "c2" "M-x" #{} {:absent :no-decision})
                          (click "c3" "M-x" #{} #{:binding-recorded})])))]
    (is (= :later-needs-unknown (:unverifiable verdict)))
    (is (= ["c2"] (:unknown-click-ids verdict)))))

(deftest found-consumer-wins-over-unknown-later-needs
  (let [verdict (first (:clicks
                        (progress/check-flight
                         [(click "c1" "M-x" #{:roles-named} #{})
                          (click "c2" "M-x" #{} {:absent :no-decision})
                          (click "c3" "M-x" #{} #{:roles-named})])))]
    (is (= :token-consumed (:progress verdict)))
    (is (= [{:token ["M-x" :roles-named] :by "c3"}]
           (:consumed verdict)))))

(deftest advanced-wants-witness-the-criterion-met-disjunct
  (let [verdict (first (:clicks
                        (progress/check-flight
                         [(click "c1" "M-x" #{:roles-named} #{}
                                 [:criterion-a])])))]
    (is (= {:click-id "c1" :progress :criterion-met
            :advanced [:criterion-a]}
           verdict))))

(deftest criterion-met-also-reports-found-consumers
  (let [verdict (first (:clicks
                        (progress/check-flight
                         [(click "c1" "M-x" #{:roles-named} #{} [:criterion-a])
                          (click "c2" "M-x" #{} #{:roles-named})])))]
    (is (= :criterion-met (:progress verdict)))
    (is (= [{:token ["M-x" :roles-named] :by "c2"}]
           (:consumed verdict)))))

(deftest empty-production-and-absent-production-have-distinct-verdicts
  (let [verdicts (:clicks
                  (progress/check-flight
                   [(click "c1" "M-x" #{} #{})
                    (click "c2" "M-x" {:absent :no-decision} #{})]))]
    (is (= {:click-id "c1" :no-progress :no-token-produced}
           (first verdicts)))
    (is (= {:click-id "c2" :unverifiable :no-decision}
           (second verdicts)))))

(deftest final-producing-click-is-produced-not-consumed
  (let [verdict (first (:clicks
                        (progress/check-flight
                         [(click "last" "M-x" #{:roles-named} #{})])))]
    (is (= :produced-not-consumed (:no-progress verdict)))))

(deftest summary-counts-each-per-click-verdict-kind
  (let [result (progress/check-flight
                [(click "c1" "M-x" #{:roles-named} #{} [:criterion-a])
                 (click "c2" "M-x" #{:binding-recorded} #{:roles-named})
                 (click "c3" "M-x" #{} #{:binding-recorded})
                 (click "c4" "M-x" {:absent :no-enactment-record} #{})])]
    (is (= {:criterion-met 1 :token-consumed 1
            :no-token-produced 1 :unverifiable 1}
           (:summary result)))
    (is (= 4 (reduce + (vals (:summary result)))))
    (is (= 4 (count (:clicks result))))))

(deftest invalid-produced-needs-and-duplicate-ids-throw-typed-kinds
  (doseq [[expected input]
          [[:invalid-produced [(click "c1" "M-x" [:roles-named] #{})]]
           [:invalid-needs [(click "c1" "M-x" #{} [:roles-named])]]
           [:duplicate-click-id [(click "same" "M-x" #{} #{})
                                 (click "same" "M-x" #{} #{})]]]]
    (testing (name expected)
      (try
        (progress/check-flight input)
        (is false "expected ex-info")
        (catch clojure.lang.ExceptionInfo e
          (is (= expected (:kind (ex-data e)))))))))
