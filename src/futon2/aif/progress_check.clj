(ns futon2.aif.progress-check
  "PROOF-2a Clause 2, intermediate progress, as a pure check over an explicit
  flight-order vector. The clause says: \"A click that meets no criterion and
  produces no token a later click in the flight consumes is overhead with no
  progress, and counts against the machine as a stop would.\" Joe's ruling of
  2026-09-27 makes this checker distinct from flight.clj's wants-flip.

  Runtime token identity is [target token]. Later means a strictly greater
  flight position, never the producing click itself. The existing :advanced
  vector supplies the criterion-met disjunct and is not recomputed here.
  Unknown later needs are never negative evidence: absent a found consumer,
  they make the result unverifiable.

  Pure: reads no flight, enactment, run, file, clock, store or environment."
  )

(defn- typed-absence? [x]
  (and (map? x) (contains? x :absent)))

(defn- invalid! [kind click-id value]
  (throw (ex-info "invalid progress-check input"
                  {:kind kind :click-id click-id :value value})))

(defn- validate-clicks! [clicks]
  (when-not (vector? clicks)
    (invalid! :invalid-flight nil clicks))
  (let [ids (mapv :click-id clicks)]
    (when-not (= (count ids) (count (distinct ids)))
      (invalid! :duplicate-click-id nil ids)))
  (doseq [{:keys [click-id produced needs]} clicks]
    (when-not (or (set? produced) (typed-absence? produced))
      (invalid! :invalid-produced click-id produced))
    (when-not (or (set? needs) (typed-absence? needs))
      (invalid! :invalid-needs click-id needs))))

(defn- consumption [click later-clicks]
  (let [qualified (set (map #(vector (:target click) %) (:produced click)))
        consumed (for [{consumer-target :target
                        consumer-id :click-id
                        needs :needs} later-clicks
                       :when (set? needs)
                       token needs
                       :let [identity [consumer-target token]]
                       :when (contains? qualified identity)]
                   {:token identity :by consumer-id})
        unknown (->> later-clicks
                     (filter #(typed-absence? (:needs %)))
                     (map :click-id)
                     (sort-by str)
                     vec)]
    {:qualified qualified
     :consumed (->> consumed
                    (sort-by (juxt (comp pr-str :token) (comp str :by)))
                    vec)
     :unknown unknown}))

(defn- check-click [click later-clicks]
  (let [{:keys [click-id advanced produced]} click
        criterion? (seq advanced)
        {:keys [qualified consumed unknown]}
        (if (set? produced)
          (consumption click later-clicks)
          {:qualified #{} :consumed [] :unknown []})]
    (cond
      criterion?
      (cond-> {:click-id click-id :progress :criterion-met :advanced advanced}
        (seq consumed) (assoc :consumed consumed))

      (typed-absence? produced)
      {:click-id click-id :unverifiable (:absent produced)}

      (empty? produced)
      {:click-id click-id :no-progress :no-token-produced}

      (seq consumed)
      {:click-id click-id :progress :token-consumed :consumed consumed}

      (seq unknown)
      {:click-id click-id :unverifiable :later-needs-unknown
       :unknown-click-ids unknown}

      :else
      {:click-id click-id :no-progress :produced-not-consumed
       :produced qualified})))

(defn- verdict-kind [verdict]
  (cond
    (:progress verdict) (:progress verdict)
    (:no-progress verdict) (:no-progress verdict)
    (contains? verdict :unverifiable) :unverifiable))

(defn check-flight
  "Check one explicit vector of clicks in flight order. Returns per-click
  verdicts in that order and counts by verdict kind. Invalid produced/needs
  shapes and duplicate click ids throw ex-info with a typed :kind."
  [clicks]
  (validate-clicks! clicks)
  (let [verdicts (mapv (fn [position click]
                         (check-click click (subvec clicks (inc position))))
                       (range (count clicks)) clicks)]
    {:clicks verdicts
     :summary (frequencies (map verdict-kind verdicts))}))
