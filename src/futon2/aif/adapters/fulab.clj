(ns futon2.aif.adapters.fulab
  "AIF adapter for fulab (fucodex/fuclaude/fubar) with softmax sampling.

   Implements deterministic stochastic policy sampling:
   - Given candidates and G scores, computes softmax probabilities over -G/tau
   - Uses seeded RNG for reproducibility: seed = hash(session-id, decision-id, candidates)
   - Supports abstain policy when tau < min-sample threshold
   - Tracks pattern evidence to adjust G scores over time
   - AC7: the temperature input is a typed record, never a 0.0 default, and
     sampling REFUSES when that record does not come back `:present`."
  (:require [clojure.string :as str]
            [futon2.aif.adapter :as adapter])
  (:import [java.util Random]))

(defn- text-score [value]
  (cond
    (string? value) (count (str/split value #"\s+"))
    (coll? value) (count value)
    :else 1))

(defn- uncertainty-score [value]
  (cond
    (nil? value) 1.0
    (number? value) (double (max 0.1 value))
    (string? value) (double (max 1 (count (str/split value #"\s+"))))
    (coll? value) (double (max 1 (count value)))
    :else 1.0))

(def default-config
  {:g/weights {:base 0.1
               :anchors 0.05
               :forecast 0.02}
   :evidence/weights {:read -0.02
                      :off-trail 0.12
                      :implement -0.08
                      :update -0.05}
   :evidence/min -0.3
   :evidence/max 0.3
   :tau/scale 1.0
   :tau/min 0.1
   :tau/max 2.0
   :tau/min-sample 0.55})

(defn- clamp [value min-val max-val]
  (-> value (max min-val) (min max-val)))

(defn- normalize-action [action]
  (cond
    (keyword? action) action
    (string? action) (keyword action)
    :else :unknown))

(defn- candidate-base-score [candidate context]
  (if-let [score (get-in context [:candidate-scores candidate])]
    (double score)
    (text-score candidate)))

(defn- evidence-score [state candidate config]
  (let [weights (merge (:evidence/weights default-config)
                       (:evidence/weights config))
        counts (get-in state [:pattern-evidence candidate])
        raw (reduce-kv (fn [acc action cnt]
                         (+ acc (* (double (get weights action 0.0))
                                   (double (or cnt 0)))))
                       0.0
                       (or counts {}))]
    (clamp raw
           (double (or (:evidence/min config) (:evidence/min default-config)))
           (double (or (:evidence/max config) (:evidence/max default-config))))))

(defn- compute-g [candidate state context config]
  (let [{:keys [base anchors forecast]} (:g/weights config)
        base-score (candidate-base-score candidate context)
        anchor-score (text-score (:anchors context))
        forecast-score (text-score (:forecast context))
        evidence (evidence-score state candidate config)]
    (+ (* base (+ base-score evidence))
       (* anchors anchor-score)
       (* forecast forecast-score))))

;; ---------------------------------------------------------------------------
;; AC7 (Joe's 2026-09-02 ruling on C130 §7): Fulab's temperature input.
;;
;; `compute-tau` read the surplus as `(double (or (:outcome-size-surplus
;; context) 0.0))`, so a context that never measured outcome size entered the
;; temperature denominator as one that measured NO surplus. That is not a
;; neutral default: surplus sits in the denominator of `tau/scale / (uncertainty
;; + surplus)`, so the substituted zero yields the HIGHEST temperature the
;; uncertainty term alone permits -- the flattest softmax, the most exploratory
;; sample -- and it is indistinguishable from an outcome of minimum size that
;; really was measured.
;;
;; C226 established two facts this row rests on: the quantity at this seam is a
;; nonnegative outcome-size surplus, NOT canonical signed epsilon `o - mu`; and
;; no call site in `src/`, `test/` or `checks/` constructs this adapter, so the
;; seam is dormant. The ruling for this site is "refuse without error": the
;; adapter does not sample at a temperature it cannot justify, and the seam
;; stays typed so a later connection can neither fabricate a temperature nor
;; clamp a signed epsilon to zero on its way in.
;; ---------------------------------------------------------------------------

(def temperature-contract
  "Producer contract stamped on every record `temperature-record` emits,
   whether it read a surplus, found none, or refused a malformed one."
  :fulab-temperature/v1)

(def surplus-field
  "The one context field Fulab's temperature rests on beyond the uncertainty
   score. Named as a var because the C12 census row, the absence lint and the
   tests all have to agree on which field this row is about."
  :outcome-size-surplus)

(def signed-error-field
  "The key Fulab must never consume: canonical signed prediction error `o - mu`
   (`free_energy.clj:197-272`), every negative value of which this seam would
   map to zero. C226 found the name collision; the guard keeps it typed."
  :prediction-error)

(defn- finite-double
  "X as a double when it is a finite number, else nil. Distinguishes a measured
   0.0 surplus -- an outcome of minimum size, a real reading -- from a value
   that cannot enter the temperature denominator at all."
  [x]
  (when (number? x)
    (let [d (double x)]
      (when-not (or (Double/isNaN d) (Double/isInfinite d)) d))))

(defn temperature-record
  "Classify Fulab's temperature input in CONTEXT as a typed record. AC7 removed
   the `0.0` the surplus used to be defaulted to here.

   `:present` -- a surplus was supplied, carried as `:value`, with `:basis`
   naming where it came from (`:outcome-size-surplus` when the caller supplied
   it, `:computed-outcome-size` when the generic update computed it from the
   observed outcome). A supplied `0.0` lands here: an outcome of minimum size
   is a measured surplus.

   `:absent` -- no surplus was supplied (key missing, or present as nil). No
   `:value` key; `:absent` names the field and whether the key was there at
   all, so a nil-valued key and a missing key stay apart.

   `:refused` -- three malformations, each named rather than clamped:
     `:canonical-signed-error-at-surplus-seam` -- the context carries
       `:prediction-error`, the signed quantity this seam does not consume
       (C226). Checked FIRST, so a caller that also supplied a surplus is still
       told which of the two quantities it is confused about.
     `:malformed-outcome-size-surplus` -- a surplus that is not a finite
       number (a non-number, NaN, or an infinity).
     `:signed-value-at-surplus-seam` -- a NEGATIVE surplus, i.e. a value with
       the shape of signed epsilon arriving under the surplus name. The old
       code threw here; it is now a record for the same reason the other two
       are -- the refusal has to be persistable for AC8's harvester, and a
       refusal that returns no sample is not quieter than an exception."
  ([context] (temperature-record context surplus-field))
  ([context basis]
   (let [stamp {:producer-contract temperature-contract}
         supplied (get context surplus-field)
         s (finite-double supplied)]
     (cond
       (contains? context signed-error-field)
       (merge stamp {:status :refused
                     :reason :canonical-signed-error-at-surplus-seam
                     :offending {:field signed-error-field
                                 :status :wrong-quantity
                                 :value (get context signed-error-field)}})

       (and (some? supplied) (nil? s))
       (merge stamp {:status :refused
                     :reason :malformed-outcome-size-surplus
                     :offending {:field surplus-field
                                 :status :not-finite
                                 :value supplied}})

       (and (some? s) (neg? s))
       (merge stamp {:status :refused
                     :reason :signed-value-at-surplus-seam
                     :offending {:field surplus-field
                                 :status :negative
                                 :value s}})

       (some? s)
       (merge stamp {:status :present :value s :basis basis})

       :else
       (merge stamp {:status :absent
                     :reason :outcome-size-surplus-not-supplied
                     :absent [{:field surplus-field
                               :key-present? (contains? context surplus-field)}]})))))

(defn temperature-events
  "The present-only projection over one temperature RECORD: a vector of at most
   one, empty when the surplus was read. Emitting the record only when the read
   was not clean is the AC1-AC6 discipline -- an empty vector means the
   temperature input was observed, which is a different claim from \"the
   adapter did not report\"."
  [record]
  (if (and (map? record) (not= :present (:status record)))
    [record]
    []))

(defn- tau-from-record
  "Temperature from a `:present` temperature RECORD, or nil when the record is
   `:absent` or `:refused`. nil is the whole point: there is no temperature to
   return when the surplus was not read, and no number is invented in its
   place. The arithmetic is unchanged from the pre-AC7 `compute-tau` --
   `tau/scale / max(1e-6, uncertainty + surplus)`, clamped to
   `[tau/min, tau/max]`."
  [record context config]
  (when (= :present (:status record))
    (let [uncertainty (uncertainty-score (:uncertainty context))
          combined (+ uncertainty (double (:value record)))
          tau (double (/ (:tau/scale config) (max 1.0e-6 combined)))]
      (clamp tau (:tau/min config) (:tau/max config)))))

;; Softmax sampling implementation

(defn- stable-seed [context]
  (let [sid (:session/id context)
        decision (:decision/id context)
        candidates (:candidates context)
        basis (str sid "|" decision "|" (str/join "," (sort candidates)))]
    (long (hash basis))))

(defn- logits-from-g [g-map tau]
  (into {}
        (map (fn [[k g]]
               [k (/ (- (double g)) (double tau))]))
        g-map))

(defn- softmax [logits]
  (let [values (vals logits)]
    (if (seq values)
      (let [max-logit (apply max values)
            exp-map (into {}
                          (map (fn [[k v]]
                                 [k (Math/exp (- (double v) (double max-logit)))]))
                          logits)
            total (reduce + (vals exp-map))]
        (into {}
              (map (fn [[k v]]
                     [k (if (pos? total)
                          (/ (double v) (double total))
                          0.0)]))
              exp-map))
      {})))

(defn- sample-choice [probs seed]
  (when (seq probs)
    (let [sorted (sort-by key probs)
          rng (Random. (long seed))
          target (.nextDouble rng)]
      (loop [remaining sorted
             acc 0.0]
        (when-let [[k p] (first remaining)]
          (let [next-acc (+ acc (double p))]
            (if (<= target next-acc)
              k
              (recur (rest remaining) next-acc))))))))

(defrecord FulabAdapter [config]
  adapter/AifAdapter
  (select-pattern [_ state context]
    (let [candidates (vec (:candidates context))
          scored (into {}
                       (for [c candidates]
                         [c (compute-g c state context config)]))
          ;; WHEN THE TEMPERATURE IS REQUIRED, precisely: exactly when this
          ;; call would SAMPLE -- the caller named no `:chosen` and there is a
          ;; candidate set to sample from. With a caller-supplied `:chosen`, or
          ;; with no candidates, tau is a diagnostic nobody branches on, so an
          ;; unread surplus omits `:tau` and blocks nothing. The record reports
          ;; the absence either way, carrying `:required?`.
          temperature (temperature-record context)
          required? (boolean (and (nil? (:chosen context)) (seq candidates)))
          record (assoc temperature :required? required?)
          tau (tau-from-record record context config)
          refused? (and required? (nil? tau))
          seed (or (:seed context) (stable-seed context))
          min-sample (double (or (:tau/min-sample config) 0.55))
          logits (when (and tau (seq candidates) (pos? tau))
                   (logits-from-g scored tau))
          probs (when (map? logits) (softmax logits))
          abstain? (and (not refused?)
                        (nil? (:chosen context))
                        (number? tau)
                        (< (double tau) min-sample))
          sampled (when (and (not refused?)
                             (not abstain?)
                             (nil? (:chosen context))
                             (seq probs))
                    (sample-choice probs seed))
          chosen (cond
                   refused? nil
                   (:chosen context) (:chosen context)
                   abstain? nil
                   sampled sampled
                   (seq candidates) (first (sort-by scored candidates))
                   :else nil)
          sampled? (and sampled (nil? (:chosen context)))
          result {:decision/id (:decision/id context)
                  :candidates candidates
                  :chosen chosen
                  :aif (cond-> {:G-chosen (get scored chosen)
                                :G-rejected (apply dissoc scored [chosen])
                                :G-scores scored
                                :seed seed
                                :sampled? sampled?
                                :abstain? abstain?
                                :min-sample min-sample
                                :refused? refused?
                                :temperature-events (temperature-events record)
                                :belief-id (or (:belief-id context) (:decision/id context))}
                         ;; No tau, no logits and no probs when the surplus was
                         ;; not read: the pre-AC7 code reported all three off a
                         ;; substituted zero.
                         (some? tau) (assoc :tau tau :logits logits :probs probs))}]
      (tap> (merge {:type :aif/fulab
                    :event (if refused? :refuse :select)
                    :session/id (:session/id context)}
                   result))
      result))

  (update-beliefs [_ state observation]
    (if (and (:pattern/id observation) (:pattern/action observation))
      ;; Pattern action observation - update evidence counts
      (let [pattern-id (:pattern/id observation)
            action (normalize-action (:pattern/action observation))
            prev-score (evidence-score state pattern-id config)
            updated (update-in state [:pattern-evidence pattern-id action] (fnil inc 0))
            next-score (evidence-score updated pattern-id config)
            counts (get-in updated [:pattern-evidence pattern-id])
            ;; The evidence counts are the belief update here; tau is a
            ;; reported diagnostic nobody branches on, so an unread surplus
            ;; omits `:tau-updated` and leaves the count update alone (AC2's
            ;; "reject the member, not the collection").
            record (assoc (temperature-record observation) :required? false)
            tau (tau-from-record record observation config)
            result {:aif/state updated
                    :aif (cond-> {:evidence-score next-score
                                  :evidence-delta (when (and (number? next-score) (number? prev-score))
                                                    (- (double next-score) (double prev-score)))
                                  :evidence-counts counts
                                  :temperature-events (temperature-events record)
                                  :belief-delta {:decision/id (:decision/id observation)
                                                 :pattern/id pattern-id
                                                 :action action
                                                 :status (or (:status observation) :observed)}}
                           (some? tau) (assoc :tau-updated tau))}]
        (tap> (merge {:type :aif/fulab
                      :event :pattern-action
                      :session/id (:session/id observation)}
                     result))
        result)
      ;; Generic observation - use outcome size as an engineering temperature proxy.
      (let [surplus (double (max 0.0 (dec (text-score (:outcome observation)))))
            ;; This branch is its own producer: it MEASURES the surplus off the
            ;; observed outcome, so the record is `:present` with basis
            ;; `:computed-outcome-size` -- unless the caller also passed the
            ;; signed-error key, which refuses whatever was computed.
            record (assoc (temperature-record (assoc observation surplus-field surplus)
                                              :computed-outcome-size)
                          :required? false)
            tau (tau-from-record record observation config)
            result {:aif/state {:belief-updated true}
                    :aif (cond-> {:outcome-size-surplus surplus
                                  :temperature-events (temperature-events record)
                                  :belief-delta {:decision/id (:decision/id observation)
                                                 :status (or (:status observation) :unknown)}}
                           (some? tau) (assoc :tau-updated tau))}]
        (tap> (merge {:type :aif/fulab
                      :event :update
                      :session/id (:session/id observation)}
                     result))
        result))))

(defn new-adapter
  ([] (->FulabAdapter default-config))
  ([config] (->FulabAdapter (merge default-config config))))
