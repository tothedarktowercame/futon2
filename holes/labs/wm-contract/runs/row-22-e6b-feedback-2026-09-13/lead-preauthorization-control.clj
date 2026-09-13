(require '[futon2.aif.machine-slow-feedback-evidence :as e]
 '[futon2.aif.machine-slow-feedback-evidence-test :as t]
 '[futon2.aif.machine-pre-enact-authorization-test :as e3t])
(let [rs (#'t/base-records)
      auth (:authorization-at e3t/pending)
      enacted (get-in rs [:lifecycle-relation :enactment/at])
      out (e/verify-feedback (#'t/fixture identity))]
 (assert (.isBefore (java.time.Instant/parse enacted) (java.time.Instant/parse auth)))
 (assert (true? (:replay/identical? out)))
 (prn {:control :enactment-before-canonical-authorization :authorization-at auth
       :enactment-at enacted :replay (:replay/identical? out)}))
