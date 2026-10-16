(ns futon2.aif.handoff-contract
  "One representation of an external producer/consumer contract."
  (:require [cheshire.core :as json]
            [clojure.pprint :as pp]))

(def schema :wm/handoff-contract-v1)

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
    (some (fn [value]
            (when (string? value)
              (try (let [parsed (json/parse-string value true)]
                     (when (map? parsed) parsed))
                   (catch Exception _ nil))))
          [(:result answer) (:text answer)])
    :else nil))

(defn outcome [contract-value request-id state detail]
  (when-not (contains? #{:not-contacted :contacted :answered :refused
                         :validated :published :consumed} state)
    (throw (ex-info "Invalid handoff outcome state"
                    {:handoff/refusal :outcome-state-invalid :state state})))
  {:schema :wm/handoff-outcome-v1
   :contract (select-keys (contract contract-value) [:id :version :validator])
   :request-id request-id :state state :detail detail})
