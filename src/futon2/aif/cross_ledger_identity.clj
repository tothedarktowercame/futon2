(ns futon2.aif.cross-ledger-identity
  "Literal run-identity join between append-only cohort closes and WM traces.")

(defn- close-judgment [close-record]
  (or (get-in close-record [:payload :judgment])
      (:judgment close-record)
      close-record))

(defn- close-run-id [close-record]
  (let [judgment (close-judgment close-record)]
    (or (get-in judgment [:entity-state-at-close :belief-source :run/id])
        (get-in judgment [:selection-identity :run/id]))))

(defn- close-cohort-attempt [close-record]
  (let [recorded (select-keys close-record [:cohort/id :attempt/id])]
    (when (= #{:cohort/id :attempt/id} (set (keys recorded))) recorded)))

(defn join-close-to-trace
  "Return the unique trace sharing the close's literal :run/id.

  Cohort/attempt ids are diagnostic only. They never substitute for run
  identity; reused historical attempt ids are surfaced as a typed collision."
  [close-record trace-records]
  (let [run-id (close-run-id close-record)
        cohort-attempt (close-cohort-attempt close-record)
        attempt-matches (when cohort-attempt
                          (filterv #(= cohort-attempt (:cohort-attempt %))
                                   trace-records))]
    (when-not run-id
      (throw (ex-info "Close has no literal WM run identity"
                      {:refusal (if (> (count attempt-matches) 1)
                                  :historical-attempt-id-collision
                                  :missing-close-run-identity)
                       :cohort-attempt cohort-attempt
                       :attempt-match-count (count attempt-matches)})))
    (let [matches (filterv #(= run-id (:run/id %)) trace-records)]
      (cond
        (empty? matches)
        (throw (ex-info "No trace carries the close run identity"
                        {:refusal :missing-trace-run-identity :run/id run-id}))

        (> (count matches) 1)
        (throw (ex-info "Multiple traces carry one run identity"
                        {:refusal :multiple-trace-run-matches
                         :run/id run-id :match-count (count matches)}))

        :else
        {:status :joined :run/id run-id
         :close close-record :trace (first matches)}))))
