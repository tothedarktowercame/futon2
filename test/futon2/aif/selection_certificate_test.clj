(ns futon2.aif.selection-certificate-test
  "WIRE-f-on-tick: emission, unchanged decisions and the existing Lean checker."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.policy :as policy]))

(defn- baseline []
  (edn/read-string
   (slurp (io/resource "fixtures/selection-certificate/before.edn"))))

(defn- select [entries beta]
  (policy/select-action-cascades entries {:beta beta}))

;; strict-minimum.edn is before.edn's six cases regenerated under the 2026-11-01
;; law. before.edn stays as the historical snapshot its other tests read.
(deftest decisions-byte-identical
  (doseq [{:keys [ranked beta decision-bytes refusal]}
          (:cases (edn/read-string
                   (slurp (io/resource "fixtures/selection-certificate/strict-minimum.edn"))))]
    (if refusal
      (is (= refusal
             (try (policy/select-action-cascades ranked {:beta beta})
                  nil
                  (catch clojure.lang.ExceptionInfo e (get-in (ex-data e) [:refusal :kind])))))
      (is (= (pr-str (edn/read-string decision-bytes))
             (pr-str (edn/read-string
                      (pr-str (-> (policy/select-action-cascades ranked {:beta beta})
                                  (dissoc :selection-certificate)
                                  (update :selection-law dissoc :e-source :enacted-steps))))))))))

(deftest computed-and-consumed-are-distinct
  (let [{:keys [ranked beta]} (first (:cases (baseline)))
        cert (:selection-certificate (select ranked beta))]
    (is (= {:value beta :status :declared} (:beta cert)))
    (is (= 2 (count (:policies cert))))
    (doseq [[entry candidate finite] (map vector ranked (:candidates cert) (:policies cert))]
      (is (= (get-in entry [:certificate :f]) (:computed-f candidate)))
      (is (= ##Inf (get-in candidate [:computed-f :value])))
      (is (= :absent (get-in candidate [:inputs :f :presence])))
      (is (= :computed-not-attached (:f-status candidate) (:f-status finite)))
      (is (= :non-finite-under-identity-a (:reason candidate) (:reason finite)))
      (is (= 0 (:f candidate) (:f finite)))
      (is (= 1 (:habit candidate) (:habit finite)))
      (is (= :declared-neutral (:habit-status finite))))))

(deftest presence-and-attached-inputs
  (doseq [[extra presence] [[{} :absent] [{:f nil :habit nil} :null]
                           [{:f false :habit false} :false]
                           [{:f 0 :habit 1} :present]
                           [{:f 2 :habit 3} :present]]]
    (let [entry (merge {:action :a :controller-score 1} extra)
          cert (:selection-certificate (select [entry] 2))
          c (first (:candidates cert))]
      (is (= presence (get-in c [:inputs :f :presence])))
      ;; Malformed action identity cannot read E. The mandatory seam records
      ;; its whole-menu neutral fallback, overriding any caller habit.
      (is (= :present (get-in c [:inputs :habit :presence])))
      (is (= :missing-policy-identity (get-in c [:habit-provenance :reason])))
      (is (= (or (:f extra) 0) (:f c)))
      (is (= 1 (:habit c)))
      (is (= (if (= presence :present) :attached :declared-neutral) (:f-status c)))))
  ;; The guard is reachable for an ATTACHED non-finite F. No new guard added.
  (doseq [f [##Inf ##-Inf ##NaN]]
    (is (= :invalid-free-energy
           (try (select [{:action :a :controller-score 1 :f f}] 1)
                (catch clojure.lang.ExceptionInfo e (get-in (ex-data e) [:refusal :kind])))))))

(defn- rational-literal [x]
  (let [r (rationalize x)]
    (if (ratio? r)
      (str "(" (numerator r) " / " (denominator r) ")")
      (str r))))

(defn- lean-status [status reason]
  (case status
    :computed ".computed"
    :declared-neutral ".declaredNeutral"
    :computed-not-attached (str ".computedNotAttached " (pr-str (name reason)))))

(defn- lean-certificate [c]
  (str "({ betaDeclared := " (rational-literal (:beta-declared c))
       ", habit := " (rational-literal (:habit c))
       ", habitStatus := " (lean-status (:habit-status c) nil)
       ", f := " (rational-literal (:f c))
       ", fStatus := " (lean-status (:f-status c) (:reason c))
       " } : SelectionCertificateQ)"))

(deftest ^:slow existing-lean-checker-accepts-and-rejects
  ;; Offline certificate check only: no runner, stores, actuators or shared JVM.
  ;; This is serialization into the EXISTING checker, not a Clojure validator.
  (let [{:keys [ranked beta]} (first (:cases (baseline)))
        decision (select ranked beta)
        certificates (get-in decision [:selection-certificate :policies])
        c (first certificates)
        perturbations [(assoc c :beta-declared 0)
                       (assoc c :habit 2)
                       (assoc c :f 1)]
        source (str "import DarkTower.AIF.CertificateChecker\n"
                    "open DarkTower.AIF\n"
                    (str/join "\n"
                              (concat
                               (map #(str "#guard " (lean-certificate %) ".check = true") certificates)
                               (map #(str "#guard " (lean-certificate %) ".check = false") perturbations)))
                    "\n")
        file (java.io.File/createTempFile "selection-certificate-" ".lean")]
    (try
      (spit file source)
      (let [result (shell/sh "lake" "env" "lean" (.getAbsolutePath file)
                             :dir "../mathlib4")]
        (is (zero? (:exit result)) (pr-str result))
        (println "SELECTION-CERTIFICATE-RECEIPT"
                 (pr-str {:selection decision :positive-controls (count certificates)
                          :negative-controls (count perturbations)
                          :checker "DarkTower.AIF.SelectionCertificateQ.check"
                          :lean-source source :lean-result result
                          :baseline-comparisons (count (:cases (baseline)))
                          :scope :offline-selection-replay})))
      (finally (.delete file)))))


;; --- Empty cascades do not enter the action marginal (2026-09-21) ---
;;
;; Run 2026-09-21-1789951020 selected an EMPTY cascade while the per-policy
;; posterior argmax was a three-pattern cascade at exactly twice its
;; probability. `cascade-first-action` returns nil for an empty cascade, and
;; `bayes-choice` sums mass per key, so 21 structurally distinct do-nothings
;; pooled to 0.785275 and outvoted the best acting key at 0.179031. These pin
;; both directions: a guard that only ever excludes is as wrong as one that
;; never does.

(defn- pattern [id target] {:id id :target target})

(defn- cascade [target pattern-ids score]
  {:action {:kind :cascade-candidate :target target
            :precedence (mapv #(pattern % target) pattern-ids)}
   :controller-score score
   :certificate {:f nil}})

(defn- co-pattern
  [id needs forbids produces]
  {:id id :guard {:status :interpreted
                  :clauses [{:present (set needs) :absent (set forbids)}]}
   :produces (set produces) :theta 1/2})

(defn- co-action
  [id units descent patterns]
  {:kind :cascade-candidate :id id :target :co-target
   :precedence {:co-apply {:units units :descent descent :patterns patterns}}})

(deftest co-application-first-action-is-the-declared-root-frontier
  (let [patterns {:a (co-pattern :a [] [] [:a-done])
                  :b (co-pattern :b [:a-done] [] [:b-done])
                  :c (co-pattern :c [:b-done] [] [:c-done])}
        first-action @#'policy/cascade-first-action]
    (is (= [(patterns :a)]
           (first-action (co-action :chain [:a :b :c] [[:a :b] [:b :c]] patterns))))
    (is (= (mapv patterns [:a :b :c])
           (first-action (co-action :independent [:a :b :c] [] patterns))))
    (is (= (mapv patterns [:a :b])
           (first-action (co-action :partial [:a :b :c] [[:a :c]] patterns))))
    (is (nil? (first-action (co-action :empty [] [] {})))
        "zero units are the same absence as an empty vector precedence")))

(deftest same-prediction-cascades-form-one-class
  (let [steps [{:prediction {[0 false] 1/2 [1 false] 1/2}}]
        emission {:certificate {:steps [{:prediction {:stop-the-line 1}}]}}
        a (co-pattern :a [] [] [:a-done])
        b (co-pattern :b [:a-done] [] [:b-done])
        c (co-pattern :c [:a-done] [] [:c-done])
        x (co-pattern :x [] [] [:x-done])
        y (co-pattern :y [] [] [:y-done])
        entry (fn [action g steps]
                {:action action :cascade-id (:id action) :controller-score g
                 :certificate {:f nil :steps steps} :class-emission emission})
        ab (co-action :ab [:a :b] [[:a :b]] {:a a :b b})
        ac (co-action :ac [:a :c] [[:a :c]] {:a a :c c})
        xy (co-action :xy [:x :y] [] {:x x :y y})
        decision (policy/select-action-cascades
                  [(entry xy 2.0 [{:prediction {[0 false] 1}}])
                   (entry ab 1.0 steps) (entry ac 1.0 steps)]
                  {:beta 1})
        law (:selection-law decision)]
    (is (= :ab (:id (:action decision))) "the class's first in ranked order is enacted")
    (is (= [:ab :ac] (:class-members law)))
    (is (= :ab (:enacted law)))
    (is (= :first-in-ranked-order-within-one-prediction-class (:representative-rule law)))
    (is (= :G (get-in law [:policy-comparison :decided-by])))
    (is (= :prediction-class (get-in law [:policy-comparison :comparison-domain])))
    (testing "the two equal G are one class, so they do not tie"
      (is (== (:chosen-action-mass decision)
              (+ (get-in law [:posterior ab]) (get-in law [:posterior ac])))))))

(deftest same-prediction-different-g-compete
  ;; What the digest leaves out (fit-ambiguity, parameter information) can
  ;; separate two policies that predict alike. They are two policies.
  (let [steps [{:prediction {[0 false] 1}}]
        entry (fn [id g] {:action {:kind :cascade-candidate :id id :target "M"
                                   :precedence [{:id id :target "M"}]}
                          :controller-score g :certificate {:f nil :steps steps}})
        decision (policy/select-action-cascades [(entry :q 2.0) (entry :p 1.0)] {:beta 1})
        law (:selection-law decision)]
    (is (= :p (:id (:action decision))))
    (is (= [:p] (:class-members law)))
    (is (= :G (get-in law [:policy-comparison :decided-by])))))

(deftest g-within-a-scale-unit-is-a-tie
  (let [entry (fn [id g steps]
                {:action {:kind :cascade-candidate :id id :target "M"
                          :precedence [{:id id :target "M"}]}
                 :controller-score g :certificate {:f nil :steps steps}})
        refusal (try (policy/select-action-cascades
                      [(entry :p 1.23456701 [{:prediction {[0 false] 1}}])
                       (entry :q 1.23456708 [{:prediction {[1 false] 1}}])]
                      {:beta 1})
                     (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e))))]
    (is (= :tie-at-minimum (:kind refusal)))
    (is (= 2 (count (get-in refusal [:detail :digests]))))))

(deftest co-application-enacted-step-is-the-enabled-frontier
  (let [patterns {:a (co-pattern :a [] [:a-done] [:a-done])
                  :b (co-pattern :b [:a-done] [:b-done] [:b-done])
                  :c (co-pattern :c [:b-done] [:c-done] [:c-done])}
        action (co-action :chain [:a :b :c] [[:a :b] [:b :c]] patterns)]
    (is (= [:b] (policy/enacted-step-of action #{:a-done})))
    (is (= {:absent :no-enabled-step}
           (policy/enacted-step-of action #{:a-done :b-done :c-done})))))

(deftest empty-cascades-cannot-outvote-an-acting-one-by-count
  ;; The bad case the guard is named for: many empty cascades, one acting.
  ;; Under the pooled-nil defect the empties' summed mass wins.
  (let [empties (mapv #(cascade (str "M-empty-" %) [] 10.0) (range 12))
        acting (cascade "M-real" [:pattern/do-the-thing] 10.0)
        decision (policy/select-action-cascades (conj empties acting) {:beta 1})
        law (:selection-law decision)]
    (is (= (pattern :pattern/do-the-thing "M-real") (:chosen-action decision))
        "the single acting cascade must beat twelve pooled do-nothings")
    (is (= "M-real" (get-in decision [:action :target])))
    (is (nil? (get (:softmax-weights decision) nil))
        "nil is not an action and must not appear in the marginal")
    (is (= 12 (get-in law [:excluded-non-actions :count]))
        "the excluded non-actions are counted in the record")
    (is (pos? (get-in law [:excluded-non-actions :mass]))
        "and their mass is recorded, not silently dropped")
    (is (== (:chosen-action-mass decision)
            (get (:softmax-weights decision) (pattern :pattern/do-the-thing "M-real")))
        "the recorded mass is the enacted class's")))

(deftest softmax-weights-still-sum-duplicate-keys
  ;; :softmax-weights decides nothing now, and its readers keep a true record:
  ;; two cascades sharing a first acting pattern sum.
  (let [shared (pattern :pattern/shared "M-shared")
        a {:action {:kind :cascade-candidate :target "M-shared"
                    :precedence [shared (pattern :pattern/tail-a "M-shared")]}
           :controller-score 10.0 :certificate {:f nil}}
        b {:action {:kind :cascade-candidate :target "M-shared"
                    :precedence [shared (pattern :pattern/tail-b "M-shared")]}
           :controller-score 11.0 :certificate {:f nil}}
        other (cascade "M-three" [:pattern/other] 12.0)
        decision (policy/select-action-cascades [a b other] {:beta 1})
        posterior (get-in decision [:selection-law :posterior])]
    (is (== (get (:softmax-weights decision) shared)
            (+ (posterior (:action a)) (posterior (:action b)))))))

(deftest a-lone-typed-no-op-still-competes-and-can-win
  ;; The other side: the guard must not swallow a GENUINE abstention. A typed
  ;; no-op carries :type, keeps its own key, and must remain selectable.
  (let [no-op {:action {:kind :no-op :type :abstain/stand-down}
               :controller-score 10.0 :certificate {:f nil}}
        acting (cascade "M-real" [:pattern/do-the-thing] 11.0)
        decision (policy/select-action-cascades [no-op acting] {:beta 1})
        weights (:softmax-weights decision)]
    (is (contains? weights :abstain/stand-down)
        "a typed no-op is an action and keeps its key in the marginal")
    (is (zero? (get-in decision [:selection-law :excluded-non-actions :count]))
        "nothing is excluded: neither candidate is an empty cascade")))

(deftest an-all-empty-roster-refuses-with-a-reason
  ;; When nothing is acting the machine declines BY SAYING SO, rather than
  ;; picking an empty cascade and letting an author discover it downstream
  ;; (2026-09-21: the author refused
  ;; :empty-selected-cascade-with-operator-gated-target).
  (let [empties (mapv #(cascade (str "M-empty-" %) [] 10.0) (range 3))
        refusal (try (policy/select-action-cascades empties {:beta 1})
                     (catch clojure.lang.ExceptionInfo e (:refusal (ex-data e))))]
    (is (= :no-acting-cascade-candidate (:kind refusal)))
    (is (= 3 (get-in refusal [:detail :empty-cascades])))
    (is (string? (get-in refusal [:detail :reason])))))
