(ns futon2.aif.kernel-example
  "Record-only aligned observations. No fit, preference or causal inference."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.cascade-sources :as sources]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.d-predecessor-task-authority :as task]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.load-identity :as load-identity]
            [futon2.aif.ruled-outcome-c :as ruled]))

(load-identity/register! *ns* *file*)

(defn declaration []
  (edn/read-string (slurp (io/resource "wm/kernel-example-domain.edn"))))
(defn- require! [ok kind]
  (when-not ok (throw (ex-info "Kernel example refused" {:kernel-example/refusal kind}))))

(defn validate-domain! [domain]
  (let [mapping (:mapping domain)
        dispositions (filter #(= :disposition (:status (val %))) mapping)
        administrative (filter #(= :not-a-disposition (:status (val %))) mapping)]
    (require! (= :wm/kernel-example-domain-v1 (:schema domain)) :mapping-schema-mismatch)
    (require! (= cohort/outcome-kinds (set (keys mapping))) :mapping-support-mismatch)
    (require! (= ruled/disposition-outcomes (set (map key dispositions))) :disposition-support-mismatch)
    (require! (every? (fn [[k v]] (= k (:value v))) dispositions) :disposition-meaning-mismatch)
    (require! (= ruled/non-disposition-outcomes (set (map key administrative))) :administrative-support-mismatch)
    (require! (every? (fn [[k v]] (and (= k (:administrative v)) (keyword? (:reason v)))) administrative)
              :administrative-meaning-mismatch)
    domain))

(defn join-counts
  "Account for all 14 close labels without pretending administrative states are
   flight dispositions. No renormalization: both strata and all zeros survive."
  [domain counts]
  (validate-domain! domain)
  (require! (= (set (keys counts)) (set (keys (:mapping domain)))) :count-support-mismatch)
  (require! (every? #(and (number? %) (<= 0 %)) (vals counts)) :invalid-count)
  {:mapping domain :mapping-sha256 (identity/digest domain)
   :dispositions (select-keys counts ruled/disposition-outcomes)
   :administrative (select-keys counts ruled/non-disposition-outcomes)
   :total (reduce + 0 (vals counts))
   :disposition-total (reduce + 0 (vals (select-keys counts ruled/disposition-outcomes)))
   :administrative-total (reduce + 0 (vals (select-keys counts ruled/non-disposition-outcomes)))})

(defn- digest-matches? [digest value]
  ;; Existing projection evidence digests use the caller's namespace-map mode.
  ;; Both known producer modes are explicit; new example identities use v2.
  (boolean (some #(= digest (identity/with-printer % (fn [] (evidence/value-digest value))))
                 [false true])))

(defn align
  "Pure alignment of an independently verified v2 projection and its record.
   A refused execution yields typed missing observations, never false."
  [{:keys [prediction occurrence signed record artifact-sha outcome domain] :as inputs}]
  (try
    (validate-domain! domain)
    (retention/validate-occurrence occurrence)
    (require! (contains? (:mapping domain) outcome) :close-outcome-unmapped)
    (when (= :frozen (:status prediction))
      (require! (= (:action prediction) (:action/value occurrence)) :prediction-occurrence-mismatch))
    (when record
      (require! (= occurrence (get-in record [:dispatch :occurrence])) :observation-occurrence-mismatch))
    (when (contains? #{:occurrence-mismatch :carry-occurrence-mismatch :carry-domain-changed
                       :declaration-pins-mismatch :declaration-snapshot-mismatch
                       :observation-artifact-binding-mismatch :after-token-evidence-mismatch}
                     (:kind signed))
      ;; Only identity/binding refusals refuse the example. The verifier's
      ;; catch-all :observation-input-invalid (e.g. an author job Agency cannot
      ;; return) leaves execution unverified: observations are unavailable, and
      ;; the close disposition is still retained.
      (require! false (:kind signed)))
    (when-let [revision (get-in record [:revision-pair :after])]
      (require! (= artifact-sha revision) :observation-artifact-mismatch))
    (let [admitted? (= :admitted (:status signed))
          declarations (get-in record [:dispatch :declarations])
          rows (:observations signed)]
      (when admitted?
        (require! (and (= :wm/d-task-token-observations-v2 (:schema signed))
                       (= task/observation-authority (:authority signed))
                       (= task/observation-scope (:scope signed))) :observation-authority-mismatch)
        (require! (= occurrence (:occurrence signed)) :observation-occurrence-mismatch)
        (require! (digest-matches? (:record-sha256 signed) record) :observation-record-mismatch)
        (require! (= artifact-sha (get-in signed [:revision-pair :after])
                     (get-in record [:revision-pair :after])) :observation-artifact-mismatch)
        (require! (= (:universe signed) (set (keys rows)) (get-in record [:dispatch :universe]))
                  :observation-domain-mismatch)
        (doseq [[token {:keys [meaning meaning-sha256 schedule schedule-sha256 artifact-observation]}] rows]
          (let [decls (filter #(= (first token) (get-in % [:snapshot :target])) declarations)
                {:keys [snapshot sha256]} (first decls)
                measurement (:measurement artifact-observation)]
            (require! (= 1 (count decls)) :observation-meaning-mismatch)
            (require! (= meaning {:token token :declaration-sha256 sha256
                                 :locator (get-in snapshot [:locators (second token)])}) :observation-meaning-mismatch)
            (require! (digest-matches? meaning-sha256 meaning) :observation-meaning-mismatch)
            (require! (and (= schedule (sources/observation-schedule snapshot))
                           (digest-matches? schedule-sha256 schedule)) :observation-schedule-mismatch)
            (require! (= artifact-sha (:artifact-sha artifact-observation)) :observation-artifact-mismatch)
            (require! (= (:observed artifact-observation)
                         (if (boolean? (get-in measurement [:result :observed]))
                           (get-in measurement [:result :observed])
                           {:status :missing :kind (or (get-in measurement [:result :kind])
                                                      (if measurement :observation-unavailable :no-locator))}))
                      :observation-evidence-mismatch)
            (when measurement
              (require! (some #{measurement} (:after-token-evidence record)) :observation-evidence-mismatch)
              (require! (digest-matches? (:evidence-sha256 artifact-observation) (:result measurement))
                        :observation-evidence-mismatch)
              (when (boolean? (:observed artifact-observation))
                (require! (= (:observed artifact-observation) (get-in measurement [:result :observed]))
                          :observation-evidence-mismatch)))))
        (doseq [{:keys [token]} (:wanted prediction)]
          (require! (contains? rows token) :observation-domain-mismatch)
          (require! (= (get-in prediction [:observation-locators token])
                       (get-in rows [token :meaning :locator])) :prediction-meaning-mismatch)))
      {:schema :wm/aligned-kernel-example-v1 :status :recorded
       :use :record-only :causal-attribution :not-established
       :occurrence occurrence :occurrence-identity (retention/occurrence-identity-receipt occurrence)
       :artifact (if artifact-sha {:status :present :sha artifact-sha :repository (:repository record)}
                     {:status :absent :reason :no-authored-artifact})
       :prediction prediction :prediction-sha256 (identity/digest prediction)
       :observation-projection signed
       :observation-source (:source inputs)
       :close-outcome outcome :disposition (get-in domain [:mapping outcome])
       :mapping domain :mapping-sha256 (identity/digest domain)
       :tokens (mapv (fn [{:keys [token predicted]}]
                       (let [row (get rows token)
                             observation (if admitted? (get-in row [:artifact-observation :observed])
                                             {:status :missing :kind (or (:kind signed) :observation-not-admitted)})]
                         {:token token :predicted predicted :observed observation
                          :observation (when admitted? row)
                          :attestation {:status :absent :want token :reason :warrant-join-not-implemented}}))
                     (:wanted prediction))
       :missingness {:prediction (if (= :frozen (:status prediction)) :available :unavailable)
                     :observations (if admitted? :admitted :unavailable)
                     :reason (when-not admitted? (:kind signed))
                     :missing-tokens (if admitted?
                                       (set (for [[token row] rows
                                                  :when (not (boolean? (get-in row [:artifact-observation :observed])))] token))
                                       (set (map :token (:wanted prediction))))}})
    (catch clojure.lang.ExceptionInfo e
      {:schema :wm/aligned-kernel-example-v1 :status :refused
       :kind (or (:kernel-example/refusal (ex-data e)) :occurrence-invalid)
       :detail (ex-data e)})))

(defn collect
  "Run the unchanged D-task verification before aligning; no execution waiver."
  [inputs expected read-job]
  (align (assoc inputs :signed
                (if-let [record (:record inputs)]
                  (task/verify-observations-v2 record expected read-job)
                  {:status :refused :kind :d-task-record-unavailable}))))
