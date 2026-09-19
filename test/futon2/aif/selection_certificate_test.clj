(ns futon2.aif.selection-certificate-test
  "WIRE-f-on-tick: emission, unchanged decisions and the existing Lean checker."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.policy :as policy]))

(defn- baseline []
  (edn/read-string
   (slurp (io/resource "fixtures/selection-certificate/before.edn"))))

(defn- select [entries beta]
  (policy/select-action-cascades entries {:beta beta}))

(deftest decisions-byte-identical
  ;; Captured from the unmodified selector. Includes actual tick-001 G values,
  ;; a tied field, and scorer-produced infinite F; beta varies away from 1.
  ;; Both arms cross the same EDN read boundary before byte comparison:
  ;; sets in the original in-memory record can print in another order.
  (doseq [{:keys [ranked beta decision-bytes]} (:cases (baseline))]
    (is (= (pr-str (edn/read-string decision-bytes))
           (pr-str (dissoc (select ranked beta) :selection-certificate))))))

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
