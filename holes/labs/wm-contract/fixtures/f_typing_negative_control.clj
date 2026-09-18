#!/usr/bin/env clojure -M
;; F-typing negative control — WIRE-f-on-tick, 2026-09-18.
;;
;; Run: cd /home/joe/code/futon2 && clojure -M holes/labs/wm-contract/fixtures/f_typing_negative_control.clj
;;
;; WHY THIS EXISTS. WIRE-f-on-tick asked: does F become finite on a realistic
;; case once rates are on? It does. The probe then asked the question that
;; matters — what happens when you wire it — and the answer was that the
;; machine stops acting. claude-12 adjudicated from the theory (futon2
;; a88382af): the wired quantity is not eq. 4.14's F at all.
;;
;;   eq. 4.14's F(pi) scores a policy's account of observations ALREADY MADE
;;   (retrodiction; o_1..o_t, fixed). What cascade-free-energy computes is the
;;   divergence of the policy's FORWARD ROLLOUT at tau >= 1 from the PRESENT
;;   evidence set. Right formula, wrong time index on o.
;;
;; A quantity defined as "distance of predicted future from present evidence"
;; is minimized identically by whatever changes nothing. That is not a
;; dark-room tendency of the model; it is a property of the wrong definition,
;; which is why no rate setting repairs it (control 2 below).
;;
;; This file is the NEGATIVE CONTROL: the recorded demonstration that wiring
;; the wrong F manufactures a dark room. It is meant to sit beside the record
;; that the RIGHT F changes nothing (control 4: it is candidate-constant, and
;; the softmax is invariant under a candidate-constant shift).
;;
;; Every assertion below is checked; the script exits 1 on any failure.

(require '[futon2.aif.efe :as efe]
         '[futon2.aif.cascade-model-manifest :as m]
         '[futon2.aif.cascade-free-energy :as cfe]
         '[futon2.aif.cascade-selection :as cs])

(def ^:private failures (atom 0))
(defn- check [label ok? detail]
  (println (format "  [%s] %s%s" (if ok? "ok" "FAIL") label
                   (if ok? "" (str "  <- " (pr-str detail)))))
  (when-not ok? (swap! failures inc)))

(def p-evidence
  {:id :p-evidence :produces #{"evidence"}
   :guard {:status :interpreted
           :clauses [{:status :interpreted :present #{"ready"} :absent #{"evidence"}}]}})
(def p-other
  {:id :p-other :produces #{"other"}
   :guard {:status :interpreted
           :clauses [{:status :interpreted :present #{"ready"} :absent #{"other"}}]}})

(def universe #{"ready" "evidence" "other"})
(def q0 (m/observed-belief #{"ready"}))
(def observed #{"ready"})
(defn rates-at [fneg] (zipmap universe (repeat {:false-neg fneg :false-pos 1/16})))

(def candidates
  [{:id :produce       :precedence [p-evidence]}
   {:id :produce-other :precedence [p-other]}
   {:id :do-nothing    :precedence []}])

;; ---------------------------------------------------------------------------
;; The correctly-typed quantity: F over observations that ARRIVED, under the
;; current belief. -ln sum_s q0(s) A(o|s). No rollout. No candidate appears in
;; its inputs, which is the whole point.
;; ---------------------------------------------------------------------------
(defn shared-f [rates]
  (let [p (reduce (fn [acc [s mass]] (+ acc (* mass (m/token-likelihood rates s observed))))
                  0 q0)]
    (double (- (Math/log (double p))))))

(println "\n=== control 1: identity A retires F by refusing to attach it ===")
(let [ranked (efe/rank-cascade-actions
              {:cascade-belief q0}
              (mapv #(assoc % :kind :cascade-candidate) candidates)
              {:horizon-steps 1
               :cascade-spec {:want #{"evidence"} :evidence observed :lam 1 :mu 1}})
      by-id (into {} (map (juxt :cascade-id #(get-in % [:certificate :f]))) ranked)]
  (check "productive candidates get F = Inf, :computed-not-attached"
         (every? (fn [id] (and (= ##Inf (:value (by-id id)))
                               (= :computed-not-attached (:status (by-id id)))))
                 [:produce :produce-other])
         by-id)
  ;; The do-nothing rollout is the identity on the belief, so its "future"
  ;; equals the present and its F is finite even here.
  (check "do-nothing alone is finite" (= :computed (:status (by-id :do-nothing))) by-id))

(println "\n=== control 2: rates make F finite — and do NOT remove the inaction bias ===")
(doseq [fneg [1/8 1/4 1/2]]
  (let [f (:f (cfe/policy-free-energy {:q0 q0 :candidates candidates :tau 1
                                       :observed-tokens observed :rates (rates-at fneg)}))]
    (check (format "false-neg=%s: every F finite" fneg)
           (every? #(Double/isFinite (double %)) (vals f)) f)
    (check (format "false-neg=%s: do-nothing still has the LOWEST F" fneg)
           (= :do-nothing (key (apply min-key val f))) f)))

(println "\n=== control 3: wiring it flips the decision to inaction at every beta ===")
(let [rates (rates-at 1/8)
      ranked (efe/rank-cascade-actions
              {:cascade-belief q0}
              (mapv #(assoc % :kind :cascade-candidate) candidates)
              {:horizon-steps 1 :adjudication-rates rates
               :cascade-spec {:want #{"evidence"} :evidence observed :lam 1 :mu 1}})
      f-by-id (:f (cfe/policy-free-energy {:q0 q0 :candidates candidates :tau 1
                                           :observed-tokens observed :rates rates}))
      mk (fn [with-f?]
           (mapv (fn [e] {:id (:cascade-id e) :habit 1.0
                          :f (if with-f? (double (f-by-id (:cascade-id e))) 0.0)
                          :g (:G-efe e)})
                 ranked))
      winner (fn [with-f? beta]
               (key (apply max-key val (cs/selection-posterior
                                        {:beta beta :candidates (mk with-f?)}))))]
  (doseq [beta [0.5 1.0 2.0 5.0]]
    (check (format "beta=%.1f: F absent -> acts; F wired -> does nothing" beta)
           (and (not= :do-nothing (winner false beta))
                (=    :do-nothing (winner true  beta)))
           {:absent (winner false beta) :wired (winner true beta)})))

(println "\n=== control 4: the correctly-typed F is candidate-CONSTANT ===")
;; claude-12's requested assertion, and the observable signature that
;; distinguishes the right quantity from the rollout-divergence one.
(let [rates (rates-at 1/8)
      sf (shared-f rates)
      rollout (:f (cfe/policy-free-energy {:q0 q0 :candidates candidates :tau 1
                                           :observed-tokens observed :rates rates}))]
  (check "shared F is one number for the whole family (a constant column)"
         (number? sf) sf)
  (check "the rollout F is NOT constant across candidates — that is the defect"
         (< 1 (count (set (vals rollout)))) rollout)
  ;; The sharpest statement of the type error available: the empty precedence
  ;; leaves the belief untouched, so its "rollout at tau 1" IS the present.
  ;; The producer therefore computes the CORRECT quantity for do-nothing and a
  ;; different one for every candidate that acts — which is precisely why the
  ;; wrong F reads as an anti-action prior.
  (check "do-nothing's rollout F == the shared F, exactly"
         (= (double (rollout :do-nothing)) sf)
         {:do-nothing (rollout :do-nothing) :shared sf}))

(println "\n=== control 5: the producer CANNOT be asked for the right quantity ===")
;; cascade-free-energy/policy-free-energy requires tau to be a POSITIVE integer,
;; so there is no argument that makes it compute F over arrived observations.
;; The corrected discharge path (record the shared F in the SelectionCertificate)
;; needs a different producer, not a different tau.
(let [r (cfe/policy-free-energy {:q0 q0 :candidates candidates :tau 0
                                 :observed-tokens observed :rates (rates-at 1/8)})]
  (check "tau=0 is refused :invalid-tau" (= :invalid-tau (:kind r)) r))

(println (format "\n%s  (%d failure(s))"
                 (if (zero? @failures) "ALL CONTROLS HOLD" "CONTROLS FAILED") @failures))
(System/exit (if (zero? @failures) 0 1))
