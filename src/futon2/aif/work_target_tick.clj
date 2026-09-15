(ns futon2.aif.work-target-tick
  "Pure P1b-2a proposal construction; no store access, genesis or activation.
   The store hashes [expected-head operation payload] as the intent. No volatile
   metadata (wall clock, durations, hostnames) may enter operation or payload;
   callers must supply stable semantic inputs, including candidate receipts.
   Changing a semantic input must change the intent. A retry retains the original
   expected head, operation and payload; this namespace never retries/rebases."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [futon2.aif.machine-model :as model]
            [futon2.aif.work-target-belief :as belief])
  (:import (java.security MessageDigest)
           (java.time Instant)
           (java.time.format DateTimeParseException)))

(defn- refusal [kind path] {:ok false :refusal {:kind kind :path path}})
(defn- demand! [condition kind path]
  (when-not condition (throw (ex-info "Invalid work-target input" {:kind kind :path path}))))
(defn- validate [f]
  (try (f) :ok
       (catch clojure.lang.ExceptionInfo e {:ok false :refusal (ex-data e)})))
(defn- text? [x] (and (string? x) (not (str/blank? x))))
(defn- hash? [x] (and (string? x) (boolean (re-matches #"[0-9a-f]{64}" x))))
(defn- instant [x]
  (when (string? x)
    (try (Instant/parse x) (catch DateTimeParseException _ nil))))
(defn- ordered? [a b c]
  (let [a (instant a) b (instant b) c (instant c)]
    (and a b c (not (.isAfter a b)) (not (.isAfter b c)))))
(defn- pin? [p]
  (and (map? p) (text? (:id p)) (text? (:path p))
       (hash? (:sha256 p)) (keyword? (:status-class p))))

(defn payload-validator
  "Construct a pure, reusable validator for every proposed/ancestral payload.
   Verify the envelope and derive model context once via P1a's empty carry.
   Its validation-only timestamp is the declaration decision date; no rows
   are introduced, and this date is never used as a payload cutoff. All actual
   payload/lineage times must be ISO-8601 instant strings."
  [declaration-envelope]
  (let [template (belief/carry-and-introduce
                  declaration-envelope {:status :established-no-snapshots}
                  {:ok true :admitted {} :not-admitted []}
                  {:timestamp (get-in declaration-envelope [:declaration :decision :at])})
        d (:declaration declaration-envelope)
        expected-d {:name (get-in d [:initial-distribution :name])
                    :revision (:revision d)
                    :authority (get-in d [:initial-distribution :authority])
                    :declaration-sha256 (get-in declaration-envelope [:source :sha256])}]
    (fn [payload]
      (if-not (:ok template)
        template
        (validate
         (fn []
           (demand! (map? payload) :invalid-payload [])
           (demand! (= (:model-context template) (:model-context payload))
                    :model-context-mismatch [:model-context])
           (demand! (and (map? (:belief payload)) (map? (:lineage payload))
                         (= (set (keys (:belief payload))) (set (keys (:lineage payload)))))
                    :carry-missing [:belief :lineage])
           (demand! (instant (:information-cutoff payload)) :invalid-cutoff [:information-cutoff])
           (doseq [[id row] (:belief payload)]
             (demand! (text? id) :invalid-target [:belief id])
             (demand! (and (map? row) (= (set (:state-support d)) (set (keys row))))
                      :posterior-support-mismatch [:belief id])
             ;; A nonnegative unit-mass row cannot contain a mass above one.
             ;; Reject it before exact summation can overflow on malformed longs.
             (demand! (every? #(and (number? %) (Double/isFinite (double %)) (<= 0 % 1))
                             (vals row)) :invalid-mass [:belief id])
             (demand! (some? (model/row-sum-admission row)) :invalid-mass [:belief id])
             (let [lineage (get-in payload [:lineage id])]
               (demand! (= expected-d (:D lineage)) :lineage-D-mismatch [:lineage id :D])
               (demand! (= (:revision d) (:interpretation-revision lineage))
                        :interpretation-revision-mismatch [:lineage id :interpretation-revision])
               (demand! (= (get-in d [:decision :bell]) (:decision-ref lineage))
                        :decision-ref-mismatch [:lineage id :decision-ref])
               (demand! (= :no-admitted-observations (:updates lineage))
                        :unsupported-update-lineage [:lineage id :updates])
               (demand! (ordered? (:introduced-at lineage) (:information-cutoff lineage)
                                  (:information-cutoff payload))
                        :lineage-cutoff-mismatch [:lineage id :information-cutoff])))
           (let [population (:candidate-population payload)
                 registry (:registry-context payload)
                 pins (:pins registry)]
             (demand! (and (map? population) (nat-int? (:count population))
                           (hash? (:sha256 population)))
                      :candidate-population-pin-missing [:candidate-population])
             (demand! (and (map? registry) (instant (:read-at registry))
                           (vector? pins) (every? pin? pins)
                           (= (count pins) (count (set (map :id pins))))
                           (= (mapv :id pins) (vec (sort (map :id pins)))))
                      :registry-pin-missing [:registry-context]))))))))

(defn- head? [h status]
  (and (map? h) (uuid? (:store/id h)) (hash? (:genesis-sha256 h))
       (= status (:status h)) (nat-int? (:seq h))
       (if (= status :established-no-snapshots)
         (and (zero? (:seq h)) (nil? (:snapshot-sha256 h)) (nil? (:operation/id h)))
         (and (pos? (:seq h)) (hash? (:snapshot-sha256 h)) (some? (:operation/id h))))))

(defn predecessor-from-store
  "Adapt an explicit activation and a read-store result. Never infer activation
   from paths or tracing. Stop results preserve the store's own reason."
  [activation store-read]
  (let [status (:status store-read)
        stop (fn [kind] {:mode :stop :failure {:kind kind :store-status status
                                              :reason (:reason store-read)}})
        state (select-keys (get-in store-read [:snapshot :payload])
                           [:belief :lineage :model-context])]
    (cond
      (= :not-activated (:status activation))
      (if (= :model-not-established status)
        {:mode :inert :reason :model-not-established}
        (stop :activation-mismatch))
      (not (and (= :activated (:status activation)) (some? (:evidence activation))))
      (stop :invalid-activation)
      (= :model-not-established status) (stop :activated-store-missing)
      (not (#{:established-no-snapshots :committed} status)) (stop :store-not-usable)
      (not (head? (:head store-read) status))
      (assoc-in (stop :store-not-usable) [:failure :detail] :invalid-store-head)
      (and (= :committed status)
           (not (every? #(map? (get state %)) [:belief :lineage :model-context])))
      (assoc-in (stop :store-not-usable) [:failure :detail] :invalid-predecessor-state)
      :else
      {:mode :state-writing :expected-head (:head store-read)
       :predecessor (if (= :committed status)
                      {:status :present :state state}
                      {:status :established-no-snapshots})})))

(defn operation-identity
  "Structured occurrence/store identity. Cutoff belongs to intent, not identity;
   sequence is deliberately absent. IDs are nonblank strings or UUIDs."
  [caller store-head cutoff]
  (let [fields (case (:kind caller)
                 :full-loop-attempt [:run/id :attempt/id]
                 :scheduled-tick [:tick-run/id]
                 nil)
        missing (some #(when-not (or (text? (get caller %)) (uuid? (get caller %))) %) fields)]
    (cond
      (nil? fields) (refusal :unknown-caller-kind [:caller :kind])
      missing (refusal :missing-caller-identity [:caller missing])
      (not (uuid? (:store/id store-head))) (refusal :missing-store-scope [:store-head :store/id])
      (not (hash? (:genesis-sha256 store-head)))
      (refusal :missing-store-scope [:store-head :genesis-sha256])
      (nil? (instant cutoff)) (refusal :invalid-cutoff [:information-cutoff])
      :else
      {:id {:purpose :work-target :caller (:kind caller)
            :occurrence (select-keys caller fields)
            :store/id (:store/id store-head) :genesis-sha256 (:genesis-sha256 store-head)}
       :kind :carry-and-introduce :caller-identity-type (:kind caller)
       :information-cutoff cutoff})))

(defn- action [candidate] (if (contains? candidate :action) (:action candidate) candidate))
(defn- canonical [x]
  (cond
    (map? x) (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
                   (map (fn [[k v]] [(canonical k) (canonical v)])) x)
    (set? x) (into (sorted-set-by #(compare (pr-str %1) (pr-str %2))) (map canonical) x)
    (vector? x) (mapv canonical x)
    (list? x) (apply list (map canonical x))
    :else x))
(defn- population-pin [actions]
  ;; Same canonical EDN convention as the store: recursive sorted maps/sets,
  ;; original sequence order, UTF-8, one trailing newline, no printer metadata.
  (binding [*print-length* nil *print-level* nil *print-meta* false
            *print-namespace-maps* false *print-readably* true *print-dup* false]
    (let [text (str (pr-str (canonical actions)) "\n")]
      (demand! (= actions (edn/read-string text)) :non-edn-candidate [:candidates])
      {:count (count actions)
       :sha256 (format "%064x" (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256")
                                                     (.getBytes text "UTF-8"))))})))
(defn- stop-refusal [result stage]
  {:mode :stop :failure (merge (:refusal result) {:stage stage}
                               (dissoc result :ok :refusal))})

(defn build-proposal
  "Build, but never persist, a deterministic proposal. All occurrence times
   come from tick-context; registry acquisition time is retained from the
   supplied snapshot. Row-7 inputs are outside the payload/commit intent."
  [{:keys [declaration activation store-read registry-snapshot candidates caller tick-context]}]
  (let [mode (predecessor-from-store activation store-read)]
    (if-not (= :state-writing (:mode mode))
      mode
      (let [admissions (belief/admissions candidates registry-snapshot tick-context)]
        (if-not (:ok admissions)
          (stop-refusal admissions :admissions)
          (let [state (belief/carry-and-introduce declaration (:predecessor mode) admissions tick-context)]
            (cond
              (not (:ok state)) (stop-refusal state :carry)
              (seq (:target-refusals state))
              {:mode :stop :failure {:kind :carry-missing :stage :carry
                                    :targets (vec (sort (keys (:target-refusals state))))
                                    :target-refusals (:target-refusals state)}}
              :else
              (let [pin-result (try {:pin (population-pin (mapv action candidates))}
                                    (catch Exception _ (refusal :non-edn-candidate [:candidates])))
                    payload (merge (select-keys state [:model-context :belief :lineage])
                                   {:information-cutoff (:timestamp tick-context)
                                    :admissions (:admitted admissions)
                                    :not-admitted
                                    (mapv #(cond-> %
                                             (= :open-mission (:type (action (:candidate %))))
                                             (assoc :coverage :unresolved-full-policy-coverage))
                                          (:not-admitted admissions))
                                    :candidate-population (:pin pin-result)
                                    :registry-context
                                    {:read-at (:read-at registry-snapshot)
                                     :pins (->> (:entries registry-snapshot)
                                                (map #(select-keys % [:id :path :sha256 :status-class]))
                                                (sort-by :id) vec)}})
                    verdict ((payload-validator declaration) payload)
                    operation (operation-identity caller (:expected-head mode) (:timestamp tick-context))]
                (cond
                  (:refusal pin-result) (stop-refusal pin-result :candidate-population)
                  (not= :ok verdict) (stop-refusal verdict :payload-validation)
                  (:refusal operation) (stop-refusal operation :operation-identity)
                  :else
                  {:mode :proposal :payload payload :expected-head (:expected-head mode)
                   :operation operation
                   :row-7-inputs
                   (into {} (for [target (keys (:admitted admissions))]
                              [target (belief/target-belief-input
                                       (assoc (:model-context state) :entity/id target
                                              :mode :single-entity :policy-entities [target])
                                       state (set (map :id (:entries registry-snapshot))) target)]))})))))))))
