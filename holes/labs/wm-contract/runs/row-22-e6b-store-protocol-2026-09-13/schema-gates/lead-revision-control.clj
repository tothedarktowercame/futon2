(require '[futon2.aif.machine-slow-feedback-store :as s] '[futon2.aif.machine-slow-feedback-store-test :as t])
(let [[_ owner] (#'t/initialized)]
 (try
  (s/compare-and-commit! owner (#'t/proposal owner "a1" "e1" "r1" 1))
  (s/compare-and-commit! owner (#'t/proposal owner "a2" "e2" "r0" 2))
  (assert (= "r0" (get-in (s/recover owner) [:head :state/revision])))
  (prn {:reused-genesis-revision-accepted true :next-refusal (#'t/refusal #(s/compare-and-commit! owner (#'t/proposal owner "a3" "e3" "r3" 3)))})
  (finally (s/release! owner))))
