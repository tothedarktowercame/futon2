(ns futon2.report.focus-discovery-test
  "Checks the offline focus counterexample; no production preference changes."
  (:require [clojure.test :refer [deftest is]]))

(binding [*out* (java.io.StringWriter.)]
  (load-file "holes/labs/wm-contract/runs/fixlist-2026-09-21/improve-7-evidence/improve_7_replay.clj"))

(deftest focus-domain-counterexamples
  (let [[latest earlier] @(ns-resolve 'improve-7-replay 'results)
        case-of (fn [r k] (first (filter #(= k (:case %)) (:cases r))))
        literal (case-of latest :literal-60-40-zero)
        global (case-of latest :global-split-on-token-powerset-counterexample)
        epsilon (case-of earlier :epsilon-nothing-zero)]
    (is (= {:adjacent 0.4} (:unrepresented-class-mass literal)))
    (is (= "M-aif-policy-conditioned-eig" (get-in literal [:choice :action :target])))
    (is (= :no-admissible-candidate (get-in (case-of earlier :literal-60-40-zero) [:selection-refusal :kind])))
    (is (= "M-expressions-of-interest" (get-in epsilon [:choice :action :target])))
    (is (= :habit (:policy-decided-by epsilon)))
    (is (= "M-wm-08-external-f2" (get-in global [:choice :action :target])))
    (is (= #{:G} (:action-decided-by global)))
    (is (> 1 (:odds (first (:odds-at-initial-context global)))))
    (doseq [r [latest earlier] c (:cases r) :when (nil? (:selection-refusal c))]
      (is (< (abs (- 1 (reduce + (map :posterior (:rows c))))) 1e-8)))))

(deftest entropy-threshold-depends-on-the-observation-grain
  (let [h (fn [p] (- (+ (* p (Math/log p)) (* (- 1 p) (Math/log (- 1 p))))))
        p 0.9 q (/ 9.0 11) p2 (- 1 (* (- 1 p) (- 1 p))) q2 (- 1 (* (- 1 q) (- 1 q)))
        endpoint (/ (- (h q) (h p)) (- p q))
        firing (/ (- (+ (h q) (h q2)) (+ (h p) (h p2))) (- p2 q2))]
    (is (< (abs (- endpoint 1.8217997070336451)) 1e-10))
    (is (< (abs (- firing 10.333605607198994)) 1e-10))
    (is (> (Math/log 11) endpoint))
    (is (< (Math/log (/ 187.0 36)) endpoint))
    (is (< (Math/log 11) firing))))

(deftest local-focus-conditioning-is-not-a-global-mixture
  (let [[latest earlier] @(ns-resolve 'improve-7-replay 'results)
        case-of (fn [r k] (first (filter #(= k (:case %)) (:cases r))))
        local (case-of latest :local-focus-token-proxy)
        global (case-of latest :global-split-on-token-powerset-counterexample)]
    (is (= "M-aif-policy-conditioned-eig" (get-in local [:choice :action :target])))
    (is (= "M-wm-08-external-f2" (get-in global [:choice :action :target])))
    (is (= :no-admissible-candidate
           (get-in (case-of earlier :local-focus-token-proxy) [:selection-refusal :kind])))
    ;; Conditioning on F divides C by .55 on its support: terminal G shifts
    ;; by log(.55) for each policy with terminal state in that class.
    (doseq [[l g] (map vector (take 2 (:rows local)) (take 2 (:rows global)))]
      (is (< (abs (- (:G l) (:G g) (Math/log 0.55))) 1e-8)))
    (is (= Double/POSITIVE_INFINITY (:G (last (:rows local)))))
    (is (= #{:G} (:action-decided-by local)))))

(deftest stationarity-does-not-survive-an-unmodelled-exit-gate
  (let [pi [55/100 35/100 5/100 5/100]
        alpha 1/100
        transition (mapv (fn [i] (mapv (fn [j] (+ (if (= i j) (- 1 alpha) 0)
                                                  (* alpha (pi j)))) (range 4))) (range 4))
        advance (fn [t] (mapv (fn [j] (reduce + (map-indexed (fn [i p] (* p (get-in t [i j]))) pi))) (range 4)))
        gated (assoc transition 0 [1 0 0 0])]
    (is (= pi (advance transition)))
    (is (every? #(= 1 (reduce + %)) transition))
    (is (not= pi (advance gated)))
    (is (> (first (advance gated)) (first pi)))))
