(ns futon2.aif2.preference-test
  "Slice-2 (β) tests for the playful-precision-prior: the exploration/info-weight
   inferred from the false-floor lull. Invariants mirror slice-1's discipline."
  (:require [clojure.test :refer [deftest testing is]]
            [futon2.aif2.preference :as p]
            [futon2.aif.intrinsic-values :as iv]))

(deftest monotonic-in-lull
  (testing "a drier lull (→1) infers a HIGHER exploration weight than a busy one (→0)"
    (is (> (p/infer-step p/w0 1.0) (p/infer-step p/w0 0.0))
        "forage when idle, exploit when busy")))

(deftest bounded
  (testing "the inferred weight stays a valid preference in [w-min, w-max]"
    (doseq [prior [0.0 p/w-min p/w0 p/w-max 1.0 -5.0 5.0]
            lull  [-1.0 0.0 0.5 1.0 2.0]]
      (let [w (p/infer-step prior lull)]
        (is (<= p/w-min w p/w-max))))))

(deftest inferred-not-static
  (testing "(B)-not-(A): the weight is INFERRED — under signal it moves off the static baseline"
    (is (not= p/w0 (p/infer-step p/w0 1.0)))))

(deftest inv-reduction-frozen
  (testing "consent denied ⇒ the static prior is returned unchanged (aif preserved)"
    (is (== p/w0 (p/inferred-info-weight p/preference-entry {:operator-ratified? false} p/w0 1.0)))
    (is (true? (p/reduces-to-static?)))))

(deftest inv-consent-credited-and-gated
  (testing "entry carries a Beta credit AND mutation is admissibility-gated"
    (is (number? (p/credit)) "live Beta credit keyed by entry id")
    (is (== p/w0 (p/inferred-info-weight (assoc p/preference-entry :status :pruned)
                                         p/default-consent p/w0 1.0))
        "pruned entry ⇒ no mutation (frozen at prior)")
    (is (not= p/w0 (p/inferred-info-weight p/preference-entry p/default-consent p/w0 1.0))
        "active + consented ⇒ inference proceeds")))

(deftest false-floor-response
  (testing "a sustained queue-dry lull drives the weight toward forage (w-max); busy toward exploit (w-min)"
    (let [dry  (nth (iterate #(p/infer-step % 1.0) p/w0) 30)
          busy (nth (iterate #(p/infer-step % 0.0) p/w0) 30)]
      (is (> dry 0.85)  "false-floor lull ⇒ forage at the competence edge")
      (is (< busy 0.15) "plenty of work ⇒ exploit"))))

(deftest contraction-stable
  (testing "each step contracts toward the lull-target (no oscillation/divergence)"
    (let [tgt (p/lull-target 1.0)
          w   0.30]
      (is (< (Math/abs (- (p/infer-step w 1.0) tgt))
             (Math/abs (- w tgt)))))))
