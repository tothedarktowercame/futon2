(ns futon2.aif.prefix-free-energy-cold-start
  "Pure, unwired correspondence for PrefixFreeEnergyColdStart.lean."
  (:require [clojure.string :as str]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.policy-prefix-admission :as admission]
            [futon2.aif.policy-prefix-evidence :as prefix-evidence]))

(defn digest [x] (evidence/value-digest (dissoc x :digest)))
(defn seal [x] (assoc x :digest (digest x)))
(defn- fail! [reason path] (throw (ex-info "cold-start prefix F refused" {:prefix-f-cold-start/refusal reason :path path})))
(defn- need! [p reason path] (when-not p (fail! reason path)))
(defn- finite? [x] (and (number? x) (Double/isFinite (double x))))
(defn- carrier! [x path]
  (need! (and (map? x) (string? (:id x)) (not (str/blank? (:id x)))) :carrier-invalid path)
  (need! (= (:digest x) (digest x)) :carrier-digest-mismatch path)
  x)
(defn- source! [pin captured path]
  (let [bs (get captured [(:path pin) (:revision pin)])]
    (need! (bytes? bs) :source-unresolved path)
    (need! (= (:sha256 pin) (evidence/sha256 bs)) :source-digest-mismatch path)))
(defn- job! [ref jobs expected path]
  (let [job (get jobs (:job-id ref))]
    (need! (map? job) :authority-job-unresolved path)
    (need! (= "done" (:state job)) :authority-job-not-terminal path)
    (need! (= (:authority ref) (:agent-id job)) :authority-agent-mismatch path)
    (need! (= (:result-digest ref) (:result-digest job)) :authority-result-mismatch path)
    (need! (= (:result-digest job) (evidence/value-digest (dissoc job :result-digest)))
           :authority-result-digest-mismatch path)
    (need! (= expected (:result job)) :authority-payload-mismatch path)
    job))

(defn registered-executable
  "Creates the caller-attested executable object consumed by this unwired adapter.
  Metadata is an identity token, not proof that the function implements the bytes."
  [descriptor f]
  {:descriptor descriptor
   :executable (with-meta f {:wm/executable-registration-digest (:digest descriptor)})})

(defn- executable!
  [kind id version implementation-digest registry authority-jobs path]
  (let [{:keys [descriptor executable authority]} (get registry id)]
    (carrier! descriptor (conj path :descriptor))
    (need! (= {:kind kind :executable-id id :version version :implementation-digest implementation-digest}
              (select-keys descriptor [:kind :executable-id :version :implementation-digest]))
           :executable-descriptor-mismatch (conj path :descriptor))
    (need! (fn? executable) :executable-unavailable (conj path :executable))
    (need! (= (:digest descriptor) (:wm/executable-registration-digest (meta executable)))
           :executable-registration-mismatch (conj path :executable))
    (job! authority authority-jobs
          {:schema :wm/executable-registry-authorization-v1 :descriptor descriptor}
          (conj path :authority))
    {:fn executable :registration-digest (:digest descriptor)}))

(defn- empirical [policy raw-records candidate]
  (if (empty? raw-records)
    {:kind :unseen}
    (if-not (every? #(= policy (get-in % [:step :policy-key])) raw-records)
      {:kind :malformed :reason :foreign-policy}
      (let [p (admission/admit policy raw-records)
            f (prefix-evidence/prefix-f {:action candidate} p nil)]
        (if (not= :admitted (:conditioning-status p))
          {:kind :malformed :reason (:conditioning-status p) :detail p}
          (case (:status f)
            :computed {:kind :coherent :f (:f f) :prefix p}
            :zero-support {:kind :contradiction :detail f}
            {:kind :malformed :reason (or (:reason f) (:conditioning-status p)) :detail f}))))))

(defn evaluate!
  [{:keys [policy candidate raw-records bootstrap-prior scorer-authority]}
   {:keys [captured-sources authority-jobs law-registry executable-registry]}]
  (need! (= policy (admission/candidate-key candidate)) :policy-key-mismatch [:policy])
  (let [assessment (empirical policy raw-records candidate)]
    (case (:kind assessment)
    :coherent (let [e assessment]
                {:schema :wm/prefix-f-route-v1 :policy policy :route :empirical :f (:f e)
                 :prefix (:prefix e)})
    :malformed (fail! :malformed-empirical-history [:raw-records])
    :contradiction (fail! :contradictory-empirical-history [:raw-records])
    :unseen
    (do
      (need! (map? bootstrap-prior) :unseen-without-bootstrap [:bootstrap-prior])
      (need! (not (contains? bootstrap-prior :supplied-f)) :supplied-f-forbidden [:bootstrap-prior :supplied-f])
      (need! (= (:digest bootstrap-prior) (digest bootstrap-prior)) :prior-digest-mismatch [:bootstrap-prior])
      (need! (= policy (:policy-key bootstrap-prior)) :prior-policy-mismatch [:bootstrap-prior :policy-key])
      (doseq [k [:calibration-law-id :model :version :calibration-epoch :rationale]]
        (need! (and (string? (get bootstrap-prior k)) (not (str/blank? (get bootstrap-prior k))))
               :prior-field-invalid [:bootstrap-prior k]))
      (doseq [k [:parameters :distribution :evidence :ledger :update-rule]]
        (carrier! (get bootstrap-prior k) [:bootstrap-prior k]))
      (let [law-id (:calibration-law-id bootstrap-prior)
            law (get law-registry law-id)]
        (need! (map? law) :calibration-law-unregistered [:calibration-law-id])
        (need! (= law-id (:id law)) :calibration-law-identity-mismatch [:calibration-law-id])
        (need! (= [(:model bootstrap-prior) (:version bootstrap-prior)] [(:model law) (:version law)])
               :calibration-law-version-mismatch [:calibration-law-id])
        (source! (:source-pin law) captured-sources [:calibration-law :source-pin])
        (need! (= (:implementation-digest law) (get-in law [:source-pin :sha256]))
               :calibration-law-implementation-mismatch [:calibration-law :implementation-digest])
        (need! (not= scorer-authority (:authority law)) :self-authored-calibration-law [:calibration-law :authority])
        (need! (not= scorer-authority (get-in law [:registry-authority :authority]))
               :self-authored-law-registry [:calibration-law :registry-authority])
        (need! (not= scorer-authority (get-in bootstrap-prior [:authority :authority]))
               :self-authored-bootstrap [:bootstrap-prior :authority])
        (job! (:registry-authority law) authority-jobs
              {:schema :wm/calibration-law-authorization-v1
               :law (dissoc law :registry-authority)} [:calibration-law :registry-authority])
        (job! (:authority bootstrap-prior) authority-jobs
              {:schema :wm/prefix-f-bootstrap-prior-v1
               :prior (dissoc bootstrap-prior :authority :digest)} [:bootstrap-prior :authority])
        (let [{eval-fn :fn registration-digest :registration-digest}
              (executable! :calibration-evaluator (:evaluator-id law) (:evaluator-version law)
                           (:implementation-digest law) executable-registry authority-jobs
                           [:calibration-law :evaluator])
              f (eval-fn {:parameters (:parameters bootstrap-prior)
                          :distribution (:distribution bootstrap-prior)
                          :evidence (:evidence bootstrap-prior)})]
          (need! (finite? f) :bootstrap-f-nonfinite [:calibration-law :evaluation])
          {:schema :wm/prefix-f-route-v1 :policy policy :route :bootstrap :f (double f)
           :law {:id law-id :model (:model law) :version (:version law)
                 :implementation-digest (:implementation-digest law)
                 :executable-registration-digest registration-digest}
           :prior-digest (:digest bootstrap-prior)}))))))

(defn validate-transition!
  [{:keys [before after new-evidence authority update-implementation raw-records policy candidate update-rule] :as t}
   {:keys [captured-sources authority-jobs executable-registry scorer-authority]}]
  (doseq [[k v] [[:before before] [:after after] [:new-evidence new-evidence]
                 [:update-rule update-rule]]] (carrier! v [k]))
  (need! (not= (:id new-evidence) (get-in before [:content :evidence-id])) :old-evidence-reused [:new-evidence])
  (need! (not= (get-in before [:content :epoch]) (get-in after [:content :epoch])) :epoch-not-advanced [:after])
  (need! (not= (get-in before [:content :ledger-digest]) (get-in after [:content :ledger-digest])) :ledger-not-advanced [:after])
  (need! (= policy (admission/candidate-key candidate)) :policy-key-mismatch [:policy])
  (need! (= :coherent (:kind (empirical policy raw-records candidate)))
         :update-evidence-not-coherent [:raw-records])
  (source! (:source-pin update-implementation) captured-sources [:update-implementation :source-pin])
  (need! (= (:implementation-digest update-implementation) (get-in update-implementation [:source-pin :sha256]))
         :update-implementation-mismatch [:update-implementation])
  (need! (not= scorer-authority (:authority authority)) :self-authored-update [:authority])
  (job! authority authority-jobs
        {:schema :wm/prefix-f-calibration-update-v1 :transition (dissoc t :authority)} [:authority])
  (let [{relation :fn registration-digest :registration-digest}
        (executable! :calibration-update-relation (:relation-id update-implementation)
                     (:version update-implementation) (:implementation-digest update-implementation)
                     executable-registry authority-jobs [:update-implementation :relation])]
    (need! (true? (relation {:before before :after after :new-evidence new-evidence
                             :raw-records raw-records :policy policy :update-rule update-rule}))
           :update-relation-refused [:update-implementation])
    (assoc t :schema :wm/prefix-f-calibration-transition-v1 :status :valid
           :executable-registration-digest registration-digest)))

(defn evaluate-menu! [requests context]
  (mapv (fn [request]
          (let [r (evaluate! request context)]
            {:policy (:policy r) :route (:route r) :f (:f r)})) requests))
