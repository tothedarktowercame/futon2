(ns futon2.aif.handoff-contract
  "One representation of an external producer/consumer contract."
  (:require [cheshire.core :as json]
            [clojure.pprint :as pp]
            [clojure.string :as str]))

(def schema :wm/handoff-contract-v1)

(defn response-json-object
  "Decode one JSON object from an external handoff response. A response may
  be the map itself, an unfenced JSON object, or exactly one Markdown fence
  containing exactly one JSON object. Prose outside the object, multiple
  fences, arrays/scalars and malformed JSON are refused as nil.

  This is a representation rule, not a semantic validator: callers still
  validate request binding, schema and fields after decoding."
  [raw]
  (cond
    (map? raw) raw
    (string? raw)
    (or (try (let [v (json/parse-string raw true)]
               (when (map? v) v))
             (catch Exception _ nil))
        (let [trimmed (str/trim raw)]
          (when (and (str/starts-with? trimmed "```")
                     (str/ends-with? trimmed "```")
                     (= 2 (count (re-seq #"```" trimmed))))
            (try (let [body (-> trimmed
                                (str/replace #"\A```[^\n]*\n" "")
                                (str/replace #"\n?```\z" ""))
                       v (json/parse-string body true)]
                   (when (map? v) v))
                 (catch Exception _ nil)))))))

(defn contract [{:keys [id version response validator] :as value}]
  (when-not (and (keyword? id) (pos-int? version) (map? response)
                 (keyword? (:id validator)) (pos-int? (:version validator)))
    (throw (ex-info "Invalid handoff contract"
                    {:handoff/refusal :contract-invalid :value value})))
  {:schema schema :id id :version version
   :response response :validator validator})

(defn prompt-block [value]
  (str "ACCEPTANCE CONTRACT (authoritative; follow exactly):\n```edn\n"
       (with-out-str (pp/pprint (contract value)))
       "```\n"))

(defn response-payload
  "The one transport rule for Agency replies. Direct injected values pass
  through. A durable terminal job uses its full :result, then legacy :text,
  then its text events. :result-summary is intentionally never consumed."
  [answer]
  (if-not (map? answer)
    answer
    (if-not (some #(contains? answer %) [:state :result :text :events])
      answer
      (let [result (:result answer)
          text (:text answer)]
        (cond
          (and (string? result) (not (str/blank? result))) result
          (and (some? result) (not (string? result))) result
          (and (string? text) (not (str/blank? text))) text
          :else (let [events (str/join "\n"
                                      (keep :text
                                            (filter #(= "text" (:type %))
                                                    (:events answer))))]
                  (when-not (str/blank? events) events)))))))

(defn response-map
  "Normalize the Agency job envelope or a directly injected response to the
  map a handoff validator consumes.  Agency's durable terminal payload is
  `:result`; older adapters used `:text`.  Keeping that transport choice here
  prevents each handoff from drifting onto a different envelope field."
  [answer]
  (cond
    (map? (:analysis answer)) (:analysis answer)
    (and (map? answer) (nil? (:state answer))) answer
    (= "done" (:state answer))
    (some response-json-object [(:result answer) (:text answer)])
    :else nil))

(defn exchange
  "Run one external handoff through the common contact boundary.

  CONTACT receives REQUEST. DECODE converts the terminal Agency payload to
  the value the consumer validates; VALIDATE returns the consumer result.
  No caller needs to inspect Agency's :state, :result or legacy :text fields.
  Every failure is returned in the same typed shape and retains the job id
  when Agency created one."
  [{:keys [contact request decode validate]
    :or {decode identity validate identity}}]
  (if-not (fn? contact)
    {:schema :wm/handoff-exchange-v1 :status :not-contacted
     :reason :contact-unavailable :contacted? false}
    (let [answer (try (contact request)
                      (catch Throwable e
                        {::contact-threw e}))]
      (cond
        (::contact-threw answer)
        {:schema :wm/handoff-exchange-v1 :status :refused
         :reason :contact-threw :contacted? true
         :message (ex-message ^Throwable (::contact-threw answer))}

        (and (map? answer) (= :not-contacted (:state answer)))
        {:schema :wm/handoff-exchange-v1 :status :not-contacted
         :reason (or (:reason answer) :not-contacted) :contacted? false
         :agent (or (:author answer) (:seat answer))}

        (and (map? answer) (contains? answer :state)
             (not= "done" (:state answer)))
        {:schema :wm/handoff-exchange-v1 :status :refused
         :reason :job-not-done :contacted? true :state (:state answer)
         :job-id (:job-id answer)}

        :else
        (let [raw (cond
                    (not (map? answer)) answer
                    (nil? (:state answer)) answer
                    :else (response-payload answer))
              decoded (try (decode raw)
                           (catch Throwable e {::decode-threw e}))]
          (cond
            (::decode-threw decoded)
            {:schema :wm/handoff-exchange-v1 :status :refused
             :reason :response-decode-threw :contacted? true
             :job-id (:job-id answer)
             :message (ex-message ^Throwable (::decode-threw decoded))}

            (nil? decoded)
            {:schema :wm/handoff-exchange-v1 :status :refused
             :reason :response-invalid :contacted? true
             :job-id (:job-id answer)}

            :else
            (try
              {:schema :wm/handoff-exchange-v1 :status :validated
               :contacted? true :job-id (:job-id answer)
               :value (validate decoded)}
              (catch Throwable e
                {:schema :wm/handoff-exchange-v1 :status :refused
                 :reason :response-validation-threw :contacted? true
                 :job-id (:job-id answer) :message (ex-message e)}))))))))

(defn outcome [contract-value request-id state detail]
  (when-not (contains? #{:not-contacted :contacted :answered :refused
                         :validated :published :consumed} state)
    (throw (ex-info "Invalid handoff outcome state"
                    {:handoff/refusal :outcome-state-invalid :state state})))
  {:schema :wm/handoff-outcome-v1
   :contract (select-keys (contract contract-value) [:id :version :validator])
   :request-id request-id :state state :detail detail})
