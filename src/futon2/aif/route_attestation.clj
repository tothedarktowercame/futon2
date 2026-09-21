(ns futon2.aif.route-attestation
  "Record supplied institutional/attestation evidence. No registry calls, credit,
  preference weights, admission changes or decision updates. Matching is not verification."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.load-identity :as load-identity])
  (:import [java.time Instant]))

(load-identity/register! *ns* *file*)

(def principles [:1A :1B :2A :2B :3 :4A :4B :5 :6 :7 :8])
(def evidence-kinds #{:registered-test-warrant :registration :pattern-application
                      :independent-review :token-observation})
(defn- missing [reason] {:status :missing :reason reason})
(defn- named-version? [x] (and (map? x) (some? (:id x)) (some? (:version x))))
(defn- instant [x] (try (Instant/parse x) (catch Exception _ nil)))

(defn- institution [x]
  (let [s (:situation x) p (:preference x)
        reason (cond
                 (or (contains? x :weight) (contains? x :score)) :institution-is-not-a-weight
                 (not (and (named-version? x) (named-version? (:condition s))
                           (contains? #{:must :may :may-not} (:valence x)))) :invalid-institution
                 (= :not-holds (:status s)) :situation-not-applicable
                 (false? (:engaged? p)) :preference-not-engaged
                 (not (and (= :holds (:status s)) (named-version? p)
                           (true? (:engaged? p)))) :applicability-unknown)]
    (assoc (select-keys x [:id :version :situation :preference :valence])
           :activation (if reason
                         {:status (if (#{:situation-not-applicable :preference-not-engaged} reason)
                                    :inert :missing) :reason reason}
                         {:status :active}))))

(defn- source [events binding]
  (let [stage (:checkpoint binding) path (:path binding) event (get events stage)]
    (when (and event (vector? path) (seq path))
      {:checkpoint stage :event/sequence (:event/sequence event)
       :recorded-at (:recorded-at event) :path (into [:payload :judgment] path)
       :value (get-in event (into [:payload :judgment] path))})))

(defn- match-binding [events criterion bindings target]
  (let [bs (filter #(= (:criterion %) (select-keys criterion [:id :version])) bindings)
        b (first bs) s (source events b) e (:value s)
        expected (:evidence-kind criterion)
        dispatch (instant (get-in events [:dispatch :recorded-at]))
        dispatch-seq (get-in events [:dispatch :event/sequence])
        at (instant (:at e)) recorded (instant (:recorded-at s))
        reason (cond
                 (not (and (named-version? criterion)
                           (= target (:target criterion)) (map? (:scope criterion))
                           (seq (:scope criterion)) (evidence-kinds expected)
                           (#{:stamp :increment :mission-closure} (:kind criterion)))) :invalid-criterion
                 (empty? bs) :binding-absent
                 (not= 1 (count bs)) :ambiguous-binding
                 (not (map? e)) :checkpoint-evidence-missing
                 (not= expected (:kind e)) :evidence-kind-mismatch
                 (not= (select-keys criterion [:id :version]) (:criterion e)) :criterion-mismatch
                 (not= target (:target e)) :target-mismatch
                 (and (#{:increment :mission-closure} (:kind criterion))
                      (not (and (vector? (:want criterion)) (= 2 (count (:want criterion)))
                                (= target (first (:want criterion))) (= (:want criterion) (:want e))))) :want-mismatch
                 (not= (:scope criterion) (:scope e)) :scope-mismatch
                 (not= :present (:status e)) :evidence-not-present
                 (not (and (string? (:sha256 e)) (re-matches #"[0-9a-f]{64}" (:sha256 e)))) :evidence-digest-missing
                 (and (= expected :registered-test-warrant)
                      (not (and (string? (:warrant-id e)) (str/starts-with? (:warrant-id e) "test-registry-")))) :warrant-id-missing
                 (and (:timing criterion) (not= :before-dispatch (:timing criterion))) :unsupported-timing
                 (and (= :before-dispatch (:timing criterion))
                      (not (and at dispatch recorded (.isBefore at dispatch)
                                (.isBefore recorded dispatch)
                                (integer? (:event/sequence s)) (integer? dispatch-seq)
                                (< (:event/sequence s) dispatch-seq)))) :not-recorded-before-dispatch)]
    (cond-> {:criterion (select-keys criterion [:id :version :target :scope :kind :evidence-kind :timing])
             :status (if reason :missing :matched)
             :attestation (if (and (nil? reason) (= expected :registered-test-warrant))
                            {:status :present :want (:want criterion) :warrant-id (:warrant-id e)
                             :verification :supplied-not-checked}
                            {:status :absent :want (:want criterion)
                             :reason (or reason :not-warrant-evidence)})}
      reason (assoc :reason reason)
      s (assoc :source (dissoc s :value))
      (nil? reason) (assoc :evidence e))))

(defn- build-receipt
  "Input contract: declarations contain :institutions, :criteria, :bindings and
  :iad-profile. Bindings resolve only paths in supplied checkpoint judgments.
  No declaration is inferred from a prompt, selected action or grounded outcome."
  [{:keys [declarations events target token-comparison]}]
  (let [institutions (mapv institution (:institutions declarations))
        criteria (:criteria declarations)
        matched (mapv (fn [c]
                        (let [owners (filter #(= (:institution c) (select-keys % [:id :version])) institutions)
                              owner (first owners)
                              applicability (if (= 1 (count owners)) (:activation owner)
                                                (missing :institution-binding-missing-or-ambiguous))]
                          (assoc (if (= :active (:status applicability))
                                   (if (= 1 (count (filter #(= (select-keys c [:id :version]) (select-keys % [:id :version])) criteria)))
                                     (match-binding events c (:bindings declarations) target)
                                     {:criterion (select-keys c [:id :version :kind]) :status :missing :reason :duplicate-criterion
                                      :attestation {:status :absent :want (:want c) :reason :duplicate-criterion}})
                                   {:criterion (select-keys c [:id :version :target :scope :kind :evidence-kind :timing])
                                    :status (:status applicability) :reason (:reason applicability)
                                    :attestation {:status :absent :want (:want c) :reason (:reason applicability)}})
                                 :institution (:institution c) :valence (:valence owner)
                                 :applicability applicability))) criteria)
        profile (mapv (fn [id]
                        (let [b (get (:iad-profile declarations) id) s (source events b) v (:value s)]
                          (if (and (map? v) (= :artifact (:kind v))
                                   (string? (:path v)) (string? (:sha256 v))
                                   (re-matches #"[0-9a-f]{64}" (:sha256 v))
                                   (not (or (contains? v :score) (contains? v :weight))))
                            {:principle id :status :artifact :artifact (select-keys v [:kind :path :sha256]) :source (dissoc s :value)
                             :verification :supplied-not-checked}
                            {:principle id :status :gap :reason :profile-artifact-not-supplied}))) principles)]
    {:schema :wm/route-attestation-v1 :mode :record-only :target target
     :status (if (seq declarations) :declared :none-declared)
     :verification :supplied-checkpoint-evidence-only
     :institutions institutions :bindings matched :iad-profile profile
     :token-outcome-comparison (or token-comparison {:status :absent :reason :comparison-not-supplied})
     :increments (filterv #(and (= :matched (:status %))
                               (= :increment (get-in % [:criterion :kind]))
                               (not= :may-not (:valence %))
                               (= :present (get-in % [:attestation :status]))) matched)}))

(defn receipt [input]
  (try (build-receipt input)
       (catch RuntimeException e
         {:schema :wm/route-attestation-v1 :mode :record-only :status :missing
          :reason :invalid-declaration :error {:class (.getName (class e)) :message (.getMessage e)}
          :institutions [] :bindings [] :increments []
          :iad-profile (mapv #(hash-map :principle % :status :gap :reason :invalid-declaration) principles)
          :token-outcome-comparison (:token-comparison input)})))

(defn retain!
  "Only writes below attempt/retained; the returned manifest entry joins close retention."
  [attempt-dir evidence-id r]
  (let [file (io/file attempt-dir "retained" "route-attestation.edn")
        bytes (.getBytes (pr-str r) "UTF-8")]
    (io/make-parents file)
    (with-open [out (io/output-stream file)] (.write out bytes))
    {:receipt r :reference {:status :present :path (.getAbsolutePath file)
                           :sha256 (load-identity/sha256 bytes)}
     :entry {:evidence/id evidence-id :source-path (.getAbsolutePath file)
             :expected-sha256 (load-identity/sha256 bytes) :admitted-at (str (Instant/now))}}))

(defn paragraph [r]
  (cond
    (or (nil? r) (= :none-declared (:status r))) "Route and attestation: none declared.\n"
    (= :missing (:status r)) (str "Route and attestation: unavailable (" (:reason r) ").\n")
    :else (let [names (fn [xs] (if (seq xs) (str/join ", " (map #(str (get-in % [:criterion :id])) xs)) "none"))
          active (filter #(= :active (get-in % [:applicability :status])) (:bindings r))
          must (filter #(= :must (:valence %)) active)]
      (str "Route and attestation (supplied evidence, not independently verified): MUST stamps met "
           (names (filter #(= :matched (:status %)) must)) "; missing "
           (names (remove #(= :matched (:status %)) must)) "; MAY stamps "
           (names (filter #(= :may (:valence %)) active)) "; MAY NOT conditions "
           (names (filter #(= :may-not (:valence %)) active))
           " (matches record prohibited events, not compliance); attested increments "
           (names (:increments r)) "; inactive/unknown bindings "
           (count (remove #(= :active (get-in % [:applicability :status])) (:bindings r))) ".\n"))))
