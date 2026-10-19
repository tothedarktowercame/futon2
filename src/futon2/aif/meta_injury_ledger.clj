(ns futon2.aif.meta-injury-ledger
  "Pure, append-only META injury history and total transition validator.

  This module deliberately has no filesystem, serving, runner, or mutable
  production-data dependency."
  (:require [clojure.string :as str]
            [futon2.aif.action-identity :as identity]))

(def ledger-schema :wm/meta-injury-ledger-v1)
(def transition-schema :wm/meta-injury-transition-v1)
(def observation-schema :wm/meta-injury-observation-v1)
(def pin-re #"[0-9a-f]{64}")
(def git-re #"[0-9a-f]{40,64}")
(def states #{:open :claimed :authored :reviewed :enacted
              :discharged :reopened :abandoned})
(def terminal-states #{:discharged :abandoned})
(def active-states (disj states :discharged :abandoned))
(def allowed-predecessors
  {:open #{nil}
   :claimed #{:open :reopened}
   :authored #{:claimed}
   :reviewed #{:authored}
   :enacted #{:reviewed}
   :discharged #{:enacted}
   :reopened #{:enacted}
   :abandoned #{:enacted}})

(defn- nonblank? [x] (and (string? x) (not (str/blank? x))))
(defn- pin? [x]
  (and (map? x) (nonblank? (:path x))
       (string? (:sha256 x)) (boolean (re-matches pin-re (:sha256 x)))))
(defn- sha? [x] (and (string? x) (boolean (re-matches git-re x))))
(defn- positive-int? [x] (and (integer? x) (pos? x)))
(defn- refusal [reason & [details]]
  (merge {:status :refused :reason reason} details))
(defn- body [entry] (dissoc entry :entry-id :entry-pin))
(defn- entry-id [entry] (identity/digest [:meta-injury-entry (body entry)]))
(defn- entry-path [entry]
  (str "meta-injury-ledger/" (or (:injury-id entry) "external") "/"
       (:ordinal entry) ".edn"))
(defn seal-entry
  "Seal a transition/observation. Existing seals are replaced, never trusted."
  [entry]
  (let [id (entry-id entry)]
    (assoc entry :entry-id id
           :entry-pin {:path (entry-path entry) :sha256 id})))
(defn- valid-entry-seal? [entry]
  (and (pin? (:entry-pin entry))
       (= (:entry-id entry) (entry-id entry)
          (get-in entry [:entry-pin :sha256]))
       (= (entry-path entry) (get-in entry [:entry-pin :path]))))
(defn ledger-digest [ledger]
  (identity/digest [:meta-injury-ledger
                    (dissoc ledger :ledger-pin)]))
(defn seal-ledger [ledger]
  (assoc ledger :ledger-pin
         {:path "meta-injury-ledger/ledger.edn"
          :sha256 (ledger-digest ledger)}))
(def empty-ledger
  (seal-ledger {:schema ledger-schema :attempt-limit 3 :entries []}))

(defn- transition? [entry] (= transition-schema (:schema entry)))
(defn- observation? [entry] (= observation-schema (:schema entry)))
(defn- transitions [ledger] (filter transition? (:entries ledger)))
(defn- injury-history [ledger injury-id]
  (filter #(= injury-id (:injury-id %)) (transitions ledger)))
(defn- latest [ledger injury-id] (last (injury-history ledger injury-id)))
(defn- authored [ledger injury-id]
  (last (filter #(= :authored (:state %)) (injury-history ledger injury-id))))
(defn- claim [ledger injury-id]
  (last (filter #(= :claimed (:state %)) (injury-history ledger injury-id))))
(defn- prior-session-ids [ledger]
  (into #{} (mapcat (juxt :author-session :reviewer-session))
        (filter #(= :claimed (:state %)) (transitions ledger))))

(defn- common-defects [entry]
  (cond-> []
    (not= transition-schema (:schema entry)) (conj :transition/schema)
    (not (nonblank? (:injury-id entry))) (conj :injury/id)
    (not (contains? states (:state entry))) (conj :transition/state)
    (not (positive-int? (:attempt entry))) (conj :attempt/positive-integer)))

(defn- state-defects [ledger entry]
  (let [state (:state entry)
        previous (latest ledger (:injury-id entry))
        previous-state (:state previous)
        claimed (claim ledger (:injury-id entry))
        patch (authored ledger (:injury-id entry))
        used-sessions (prior-session-ids ledger)
        attempt (:attempt entry)
        limit (:attempt-limit ledger)]
    (cond-> []
      (not (contains? (get allowed-predecessors state #{}) previous-state))
      (conj :transition/illegal-order)
      (and previous (not= (:entry-id previous)
                          (get-in entry [:injury-predecessor :entry-id])))
      (conj :predecessor/id)
      (and previous (not= (:entry-pin previous)
                          (get-in entry [:injury-predecessor :entry-pin])))
      (conj :predecessor/pin)
      (and (nil? previous) (:injury-predecessor entry)) (conj :predecessor/unexpected)
      (and previous (not= attempt (:attempt previous))
           (not= state :reopened)) (conj :attempt/changed-outside-reopen)
      (and (= state :reopened) (not= attempt (inc (:attempt previous))))
      (conj :attempt/not-incremented)
      (and (not= state :open) claimed
           (not= (:repair-id entry) (:repair-id claimed)))
      (conj :repair/id-substitution)
      (and (= state :open)
           (or (not (pin? (:cause-record-pin entry)))
               (not (keyword? (:failure-stage entry)))
               (not (keyword? (:failure-kind entry)))
               (not (nonblank? (:detected-click entry)))
               (not= 1 attempt)))
      (conj :open/evidence)
      (and (= state :claimed)
           (or (not (nonblank? (:repair-id entry)))
               (not (nonblank? (:algorithm entry)))
               (not (nonblank? (:author-session entry)))
               (not (nonblank? (:reviewer-session entry)))
               (= (:author-session entry) (:reviewer-session entry))))
      (conj :claim/identities)
      (and (= state :claimed)
           (or (contains? used-sessions (:author-session entry))
               (contains? used-sessions (:reviewer-session entry))))
      (conj :claim/session-not-fresh)
      (and (= state :authored)
           (or (not (sha? (:patch-sha entry)))
               (not (nonblank? (:worktree entry)))))
      (conj :authored/evidence)
      (and (= state :reviewed)
           (or (not (#{:approved :request-changes} (:verdict entry)))
               (not (sha? (:reviewer-sha entry)))
               (not= (:reviewer-session entry) (:reviewer-session claimed))))
      (conj :review/evidence)
      (and (= state :enacted)
           (or (not= :approved (:verdict previous))
               (not (nonblank? (:retry-click entry)))
               (= (boolean (:reload-receipt entry))
                  (boolean (:integration-commit entry)))
               (and (:reload-receipt entry) (not (pin? (:reload-receipt entry))))
               (and (:integration-commit entry)
                    (not (sha? (:integration-commit entry))))))
      (conj :enactment/evidence)
      (and (#{:discharged :reopened} state)
           (or (not (nonblank? (:retry-click entry)))
               (not= (:patch-sha entry) (:patch-sha patch))
               (not= (:retry-click entry) (:retry-click previous))
               (= (:retry-click entry)
                  (:detected-click (first (injury-history ledger (:injury-id entry)))))))
      (conj :retry/attribution)
      (and (= state :discharged)
           (not (#{:success :grounded-change :closed} (:outcome entry))))
      (conj :discharge/outcome)
      (and (= state :reopened)
           (or (not (pin? (:new-cause-pin entry))) (> attempt limit)))
      (conj :reopen/evidence-or-limit)
      (and (= state :abandoned)
           (or (not= attempt limit)
               (not= :attempt-limit-exhausted (:reason entry))))
      (conj :abandonment/not-exhausted))))

(declare validate-ledger)
(defn validate-transition
  "Total validation against an already sealed ledger."
  [ledger proposed]
  (try
    (let [history-check (validate-ledger ledger)]
      (if-not (= :valid (:status history-check))
        (refusal :ledger/tampered {:ledger-defects (:defects history-check)})
        (let [entry (-> proposed
                        (assoc :schema transition-schema
                               :ordinal (inc (count (:entries ledger)))
                               :predecessor
                               (when-let [global-previous (last (:entries ledger))]
                                 {:entry-id (:entry-id global-previous)
                                  :entry-pin (:entry-pin global-previous)})
                               :injury-predecessor
                               (when-let [injury-previous
                                          (latest ledger (:injury-id proposed))]
                                 {:entry-id (:entry-id injury-previous)
                                  :entry-pin (:entry-pin injury-previous)}))
                        (dissoc :entry-id :entry-pin))
              defects (into (common-defects entry) (state-defects ledger entry))]
          (if (seq defects)
            (refusal :transition/invalid {:defects (vec (distinct defects))})
            {:status :valid :entry (seal-entry entry)}))))
    (catch Throwable t
      (refusal :transition/malformed {:detail (ex-message t)}))))

(defn append-transition [ledger proposed]
  (let [replayed? (and (:entry-id proposed)
                       (some #(= (:entry-id proposed) (:entry-id %))
                             (:entries ledger)))
        checked (if replayed?
                  (refusal :transition/duplicate-replay)
                  (validate-transition ledger proposed))]
    (if-not (= :valid (:status checked))
      checked
      (if (some #(= (:entry-id (:entry checked)) (:entry-id %)) (:entries ledger))
        (refusal :transition/duplicate-replay)
        {:status :appended
         :ledger (seal-ledger (update ledger :entries conj (:entry checked)))
         :entry (:entry checked)}))))

(defn append-observation
  "Append a pinned non-transition record. It cannot alter injury state."
  [ledger record-pin observation-kind]
  (try
    (let [checked (validate-ledger ledger)]
      (cond
        (not= :valid (:status checked))
        (refusal :ledger/tampered {:ledger-defects (:defects checked)})
        (not (pin? record-pin)) (refusal :observation/record-pin)
        (not (keyword? observation-kind)) (refusal :observation/kind)
        :else
        (let [previous (last (:entries ledger))
              entry (seal-entry
                     {:schema observation-schema
                      :ordinal (inc (count (:entries ledger)))
                      :record-pin record-pin :observation-kind observation-kind
                      :predecessor (when previous
                                     {:entry-id (:entry-id previous)
                                      :entry-pin (:entry-pin previous)})})]
          {:status :appended
           :ledger (seal-ledger (update ledger :entries conj entry))
           :entry entry})))
    (catch Throwable t
      (refusal :observation/malformed {:detail (ex-message t)}))))

(defn validate-ledger
  "Total validation of seals, global append chain and every injury transition."
  [ledger]
  (try
    (let [top-defects (cond-> []
                        (not= ledger-schema (:schema ledger)) (conj :ledger/schema)
                        (not (positive-int? (:attempt-limit ledger))) (conj :ledger/attempt-limit)
                        (not (vector? (:entries ledger))) (conj :ledger/entries)
                        (not (pin? (:ledger-pin ledger))) (conj :ledger/pin)
                        (and (pin? (:ledger-pin ledger))
                             (not= (ledger-digest ledger)
                                   (get-in ledger [:ledger-pin :sha256])))
                        (conj :ledger/seal))
          entries (if (vector? (:entries ledger)) (:entries ledger) [])]
      (loop [prefix (seal-ledger (assoc (select-keys ledger [:schema :attempt-limit])
                                       :entries []))
             remaining entries defects top-defects seen #{}]
        (if-let [entry (first remaining)]
          (let [ordinal (inc (count (:entries prefix)))
                previous (last (:entries prefix))
                chain-ok? (and (= ordinal (:ordinal entry))
                               (= (when previous
                                    {:entry-id (:entry-id previous)
                                     :entry-pin (:entry-pin previous)})
                                  (:predecessor entry)))
                duplicate? (contains? seen (:entry-id entry))
                base-defects (cond-> defects
                               (not (or (transition? entry) (observation? entry)))
                               (conj :entry/schema)
                               (not (valid-entry-seal? entry)) (conj :entry/seal)
                               (not chain-ok?) (conj :entry/predecessor-chain)
                               duplicate? (conj :entry/duplicate))
                semantic-defects (if (transition? entry)
                                   (into (common-defects entry)
                                         (state-defects prefix entry))
                                   (cond-> []
                                     (not (pin? (:record-pin entry)))
                                     (conj :observation/record-pin)
                                     (not (keyword? (:observation-kind entry)))
                                     (conj :observation/kind)))
                next-defects (cond-> base-defects
                               (seq semantic-defects)
                               (conj :entry/transition-invalid))
                next-prefix (seal-ledger (update prefix :entries conj entry))]
            (recur next-prefix (next remaining) next-defects
                   (conj seen (:entry-id entry))))
          (if (seq defects)
            {:status :refused :reason :ledger/invalid
             :defects (vec (distinct defects))}
            {:status :valid :entry-count (count entries)}))))
    (catch Throwable t
      (refusal :ledger/malformed {:detail (ex-message t)}))))

(defn active-injuries
  "Map injury id to its latest valid nonterminal transition. Tampering refuses."
  [ledger]
  (let [checked (validate-ledger ledger)]
    (if-not (= :valid (:status checked))
      checked
      {:status :computed
       :injuries (->> (transitions ledger)
                      (group-by :injury-id)
                      (keep (fn [[injury-id history]]
                              (let [entry (last history)]
                                (when (contains? active-states (:state entry))
                                  [injury-id entry]))))
                      (into {}))})))

(defn append-retry-result
  "Append reopened, or the typed abandoned state when the next attempt would
  exceed the configured limit."
  [ledger proposed]
  (try
    (let [checked (validate-ledger ledger)]
      (if-not (= :valid (:status checked))
        (refusal :ledger/tampered {:ledger-defects (:defects checked)})
        (let [previous (latest ledger (:injury-id proposed))
              next-attempt (inc (or (:attempt previous) 0))]
          (if (> next-attempt (:attempt-limit ledger))
            (append-transition ledger
                               (-> proposed
                                   (assoc :state :abandoned
                                          :attempt (:attempt-limit ledger)
                                          :reason :attempt-limit-exhausted)
                                   (dissoc :retry-click :new-cause-pin :patch-sha)))
            (append-transition ledger (assoc proposed :state :reopened
                                             :attempt next-attempt))))))
    (catch Throwable t
      (refusal :retry-result/malformed {:detail (ex-message t)}))))
