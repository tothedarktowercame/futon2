(ns futon2.aif.zeta-posterior-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.zeta-posterior :as zeta]))

(def rates {:x {:false-pos 4/5 :false-neg 1/5}})
(def trial {:click-id "click-1" :state {#{} 1} :observation {#{:x} 1}})

(deftest both-directions-and-no-surprise
  (let [hit (zeta/beta-posterior [trial] rates 1 1)
        miss (zeta/beta-posterior [(assoc trial :observation {#{} 1})] rates 1 1)
        calibrated (zeta/beta-posterior [(assoc trial :observation {#{} 1/5 #{:x} 4/5})] rates 1 1)]
    (is (< (Math/abs (- (:beta-post hit) (- 1 (* 0.2 (Math/log 4))))) 1e-12))
    (is (< (:zeta-mean miss) 1 (:zeta-mean hit)))
    (is (< (Math/abs (- (:beta-post miss) (+ 1 (* 0.8 (Math/log 4))))) 1e-12))
    (is (< (Math/abs (- 1 (:beta-post calibrated))) 1e-12))
    (is (= (:beta-post hit)
           (:beta-post (zeta/beta-posterior [(assoc trial :observation [0 1])] rates 1 1))))
    (is (= :fixed-A-signature (:lean-scope hit)))
    (is (not= (:trajectory-digest hit)
              (:trajectory-digest (zeta/beta-posterior [trial] rates 1 2))))))

(deftest prior-and-domain-refusals
  (is (= {:beta-post 2 :zeta-mean 1/2 :trials 0 :basis :prior-no-trials}
         (select-keys (zeta/beta-posterior [] rates 2 1/2)
                      [:beta-post :zeta-mean :trials :basis])))
  (is (= :nonpositive-likelihood
         (:reason (zeta/beta-posterior [trial] {:x {:false-pos 0 :false-neg 0}} 1 1))))
  (is (= :nonpositive-beta-post (:reason (zeta/beta-posterior [trial] rates 1/10 1))))
  (is (= :nonpositive-beta-prior (:reason (zeta/beta-posterior [] rates 0 1)))))

(deftest history-is-recomputed-from-original-prior
  (let [one (zeta/beta-posterior [trial] rates 1 1)
        trials [trial (assoc trial :click-id "click-2" :rates {:x {:false-pos 3/4 :false-neg 1/4}})]
        current (zeta/next-zeta one)
        full (zeta/beta-posterior trials rates 1 current)
        wrong (zeta/beta-posterior trials rates (:beta-post one) current)]
    (is (not= (:beta-post wrong) (:beta-post full)))
    (is (= full (zeta/beta-posterior trials rates 1 current)))
    (is (= 2 (:trials full)))))

(deftest declared-prior-is-shared-by-consumer-and-publisher
  (doseq [prior [zeta/default-prior {:beta-prior 2 :authority :caller-declared}]]
    (let [start (cond-> {:conditioning-status :trajectory-start}
                  (not= prior zeta/default-prior) (assoc :zeta-prior prior))
          opts (zeta/lane-options start)
          envelope (zeta/lane-options
                    {:conditioning-status :temporal-posterior
                     :temporal-previous {:zeta-posterior (assoc prior :basis :prior-no-trials)}})
          record {:zeta-prior prior
                  :zeta-likelihood {:target :target :checked-tokens #{:x} :rates rates}
                  :temporal-posterior {:model {:click-id "prior-agreement"}
                                       :observation true :predicted-state {#{} 1}}}
          published (zeta/trajectory-posterior [record])]
      (is (= (assoc prior :basis :prior-no-trials) (:zeta-basis opts)))
      (is (= (/ 1 (:beta-prior prior)) (:zeta opts)))
      (is (= opts envelope) "The no-trials envelope retains the declaration's authority")
      (is (= :posterior (:basis published)))
      (is (= prior (:prior-declaration published)))
      (is (= (:zeta opts) (:evaluated-at-zeta published)))
      (println :prior-basis (:zeta-basis opts) :consumer-zeta (:zeta opts)
               :publisher-zeta (:evaluated-at-zeta published))))
  (is (= {:beta-prior 1 :authority :joe-ruling-2026-09-27} zeta/default-prior))
  (doseq [beta [0 -1]]
    (let [prior {:beta-prior beta :authority :caller-declared}]
      (doseq [receipt [{:conditioning-status :trajectory-start :zeta-prior prior}
                       {:conditioning-status :temporal-posterior
                        :temporal-previous {:zeta-posterior (assoc prior :basis :prior-no-trials)}}]]
        (is (= {:zeta-basis {:absent :nonpositive-beta-prior}}
               (zeta/lane-options receipt)))))))
