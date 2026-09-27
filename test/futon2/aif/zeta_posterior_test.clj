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
