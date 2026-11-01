(ns futon2.aif.cascade-feedback
  "Production feedback from one provisional cascade execution to later
   construction. Selection alone never reinforces a pattern. Verified enactment
   that produces grounded work attests the selected cascade when at least one
   selected want was reached or progressed; operator acceptance is a separate,
   potentially stronger observation rather than a prerequisite."
  (:require [futon2.data-paths :as data-paths]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.set :as set]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.load-identity :as load-identity])
  (:import [clojure.lang LineNumberingPushbackReader]
           [java.io RandomAccessFile]))

(load-identity/register! *ns* *file*)

(def receipt-schema :wm/cascade-execution-feedback-v1)
(def metadata-schema :wm/pattern-feedback-metadata-v1)
(def default-path
  (data-paths/path "wm-pattern-feedback" "events.edn"))

(defn- selected-patterns [action]
  (->> (:precedence action) (keep :id) distinct vec))

(defn- verified-application? [d-task]
  (let [join (or (get-in d-task [:verification :candidate-to-minted-join])
                 (:candidate-to-minted-join d-task))]
    (and (= :admitted (get-in d-task [:verification :status]))
         (= :verified (:status join))
         (string? (:selected-action-sha256 join))
         (= (:selected-action-sha256 join)
            (:enacted-action-sha256 join)))))

(defn receipt
  "Create the close feedback receipt. A grounded close whose retained D-task
   bridge proves selected/enacted identity attests every pattern in the enacted
   cascade, but gives full application credit only to the recorded enacted
   criterion step. Other cascade members receive a weaker supporting
   attestation. Absence of an operator acceptance declaration does not erase
   either grounded-work observation. A grounded close in which no selected
   want was reached or progressed records each pattern as :no-want-effect
   with no reinforcement: the pattern counts are unchanged by it."
  [{:keys [run-id target selected-action outcome failure accepted-increment
           d-task-enactment cascade-revision artifact want-outcome-accounting]}]
  (let [target (or target (:target selected-action))
        selected (selected-patterns selected-action)
        step (get-in accepted-increment [:criterion-step :id])
        enacted? (verified-application? d-task-enactment)
        exact-step? (and enacted?
                         (= :recorded-decision
                            (get-in accepted-increment [:criterion-step :source]))
                         (contains? (set selected) step))
        grounded? (contains? #{:grounded-change :grounded-progress} outcome)
        wants-verified? (= :verified (:status want-outcome-accounting))
        grounded-attestation? (and enacted? grounded? wants-verified?)
        want-effect? (boolean
                      (or (seq (get-in want-outcome-accounting [:by-class :reached]))
                          (seq (get-in want-outcome-accounting [:by-class :progressed]))))
        reinforced? (and grounded-attestation? want-effect?)
        applications
        (cond
          reinforced?
          (mapv (fn [pattern]
                  {:pattern pattern
                   :status (if (= pattern step)
                             :successful
                             :supporting-attestation)
                   :evidence (cond-> {:selected-enacted-action :verified
                                      :grounded-work :attested
                                      :application-role (if (= pattern step)
                                                          :enacted-step
                                                          :cascade-support)
                                      :terminal-outcome outcome}
                               (= pattern step)
                               (assoc :accepted-increment (:accepted? accepted-increment)
                                      :accepted-reason (:reason accepted-increment)))
                   :reinforcement :positive})
                selected)

          grounded-attestation?
          (mapv (fn [pattern]
                  {:pattern pattern
                   :status :no-want-effect
                   :evidence {:selected-enacted-action :verified
                              :grounded-work :attested
                              :application-role (if (= pattern step)
                                                  :enacted-step
                                                  :cascade-support)
                              :selected-want-effect :none
                              :terminal-outcome outcome}
                   :reinforcement :none})
                selected)

          (and exact-step? wants-verified?)
          [{:pattern step
            :status :incomplete
            :evidence {:selected-enacted-action :verified
                       :accepted-increment (:accepted? accepted-increment)
                       :accepted-reason (:reason accepted-increment)
                       :terminal-outcome outcome}
            :reinforcement :none}]

          :else [])
        applied (set (map :pattern applications))
        selected-only (vec (remove applied selected))
        blocker-kind (when-not grounded-attestation?
                       (or (when-not wants-verified? (:reason want-outcome-accounting))
                           (:kind failure)
                           (:reason accepted-increment)
                           (when selected-action outcome)
                           :execution-evidence-unavailable))
        base {:schema receipt-schema
              :status (if wants-verified? :verified :refused)
              :reason (when-not wants-verified? (:reason want-outcome-accounting))
              :run/id run-id
              :target target
              :cascade-status :provisional-per-run
              :mission-state {:selected-wants (vec (:want selected-action))
                              :construction-reachability
                              {:reached-wants
                               (vec (or (:reached-wants selected-action)
                                        (get-in selected-action
                                                [:construction-receipt :reached-wants]) []))
                               :unreached-wants
                               (vec (or (:unreached-wants selected-action)
                                        (get-in selected-action
                                                [:construction-receipt :unreached-wants]) []))}
                              :execution-outcomes want-outcome-accounting
                              :reached-wants (vec (get-in want-outcome-accounting [:by-class :reached]))
                              :progressed-wants (vec (get-in want-outcome-accounting [:by-class :progressed]))
                              :blocked-wants (vec (get-in want-outcome-accounting [:by-class :blocked]))
                              :untouched-wants (vec (get-in want-outcome-accounting [:by-class :untouched]))
                              :unreached-wants
                              (vec (concat (get-in want-outcome-accounting [:by-class :progressed])
                                           (get-in want-outcome-accounting [:by-class :blocked])
                                           (get-in want-outcome-accounting [:by-class :untouched])))
                              :terminal-outcome outcome}
              :patterns {:selected selected
                         :applications applications
                         :selected-only selected-only
                         :positive-reinforcement
                         (if reinforced? selected [])}
              :cascade-revision
              (or cascade-revision
                  {:status :absent :reason :no-mid-run-cascade-revision})
              :blocker (if blocker-kind
                         {:status :present
                          :kind blocker-kind
                          :stage (or (:stage failure) {:absent :not-recorded})
                          :repair-evidence applications}
                         {:status :absent :reason :verified-grounded-work})
              :artifact (if (and (map? artifact) (string? (:commit artifact)))
                          (assoc (select-keys artifact [:repo :commit])
                                 :grounded? grounded?)
                          {:status :absent :reason :no-grounded-artifact})}
        receipt-id (identity/digest base)]
    (assoc base :receipt/id receipt-id)))

(defn- read-events* [file]
  (if-not (.isFile file)
    []
    (with-open [reader (LineNumberingPushbackReader. (io/reader file))]
      (loop [events []]
        (let [value (edn/read {:eof ::eof} reader)]
          (if (= ::eof value)
            events
            (do
              (when-not (and (= receipt-schema (:schema value))
                             (string? (:receipt/id value)))
                (throw (ex-info "Malformed cascade feedback event"
                                {:cascade-feedback/refusal :invalid-event
                                 :line (.getLineNumber reader)})))
              (recur (conj events value)))))))))

(defn read-snapshot
  ([] (read-snapshot default-path))
  ([path]
   (let [file (.getAbsoluteFile (io/file path))
         events (read-events* file)]
     {:schema :wm/cascade-execution-feedback-snapshot-v1
      :status (if (.isFile file) :present :absent)
      :reason (when-not (.isFile file) :feedback-store-missing)
      :path (.getPath file)
      :events events})))

(defn record!
  "Append RECEIPT exactly once under a process/file lock."
  ([receipt] (record! default-path receipt))
  ([path receipt]
   (when-not (and (= receipt-schema (:schema receipt))
                  (string? (:receipt/id receipt)))
     (throw (ex-info "Invalid cascade feedback receipt"
                     {:cascade-feedback/refusal :invalid-receipt})))
   (let [file (.getAbsoluteFile (io/file path))]
     (io/make-parents file)
     (with-open [raf (RandomAccessFile. file "rw")
                 _lock (.lock (.getChannel raf))]
       (let [events (read-events* file)
             duplicate? (some #(= (:receipt/id receipt) (:receipt/id %)) events)]
         (when-not duplicate?
           (.seek raf (.length raf))
           (.write raf (.getBytes (str (pr-str receipt) "\n") "UTF-8"))
           (.sync (.getFD raf)))
         {:status (if duplicate? :already-recorded :recorded)
          :path (.getPath file)
          :receipt/id (:receipt/id receipt)})))))

(def revision-projection-schema :wm/retained-cascade-revision-v1)

(defn- revision-pattern-ids
  [precedence]
  (vec (for [p precedence] (if (map? p) (:id p) p))))

(defn- revised-cascade-projection
  "Project ONE close receipt's :cascade-revision into the compact form a
   later construction reads. Only a :revised revision projects; a :refused
   or absent revision never becomes a candidate. The full original/revised
   candidates stay in the event; the projection carries just the pattern
   order, the delta against the parent, and the provenance a later run
   retains to show it considered and used the revision."
  [event]
  (let [rev (:cascade-revision event)]
    (when (and (map? rev) (= :revised (:status rev)))
      (let [history (:history rev)
            parent (first history)
            child (second history)
            parent-patterns (set (:patterns parent))
            child-patterns (set (:patterns child))]
        ;; A content no-op (same pattern set as the parent, packet 1's
        ;; revise-time refusal) is never projected even if such a record
        ;; exists: it is not a way out of anything.
        (when (not= parent-patterns child-patterns)
          {:schema revision-projection-schema
         :receipt/id (:receipt/id event)
         :run/id (:run/id event)
         :target (:target event)
         :blocker-kind (get-in rev [:blocker :kind])
         :parent {:identity (:identity parent)
                  :patterns (:patterns parent)}
         :patterns (:patterns child)
         :precedence (revision-pattern-ids
                      (get-in rev [:revised :precedence]))
         :delta (:delta rev)})))))

(defn retained-revision-candidates
  "Revised cascades a later construction for TARGET may consider. METADATA
   is the target's construction-metadata entry (as cascade-problems holds
   it under [:pattern-feedback target]); the full by-target map also
   works. The per-target entry is authoritative
   and there is no global fallback, because a revision is target-local.
   The newest revision per distinct pattern set wins; an older revision of
   the same set is superseded, never both."
  [metadata target]
  (let [entries (or (:revised-cascades metadata)
                    ;; tolerate the full by-target map too
                    (get-in metadata [target :revised-cascades]))]
    (vec
     (for [[_ group] (group-by (comp set :patterns) (reverse entries))]
       (let [newest (first group)]
         {:precedence (:precedence newest)
          :patterns (:patterns newest)
          :delta (:delta newest)
          :provenance (select-keys newest
                                   [:schema :receipt/id :run/id :target
                                    :blocker-kind :parent :delta])})))))

(defn construction-metadata
  "Project retained close receipts into target-local construction metadata.
   Counts separate successful, incomplete, no-want-effect, and selected-only
   evidence. Every
   target also receives the global pattern counts: pattern experience may
   transfer across missions, while the target-local counts remain visible."
  [snapshot]
  (letfn [(counts [events]
            (let [selected (mapcat #(get-in % [:patterns :selected]) events)
                  applications (mapcat #(get-in % [:patterns :applications]) events)
                  by-pattern (group-by :pattern applications)
                  ids (set/union (set selected) (set (keys by-pattern)))]
              (into (sorted-map-by #(compare (pr-str %1) (pr-str %2)))
                    (for [id ids
                          :let [apps (get by-pattern id [])]]
                      [id {:successful-applications
                           (count (filter #(= :successful (:status %)) apps))
                           :supporting-attestations
                           (count (filter #(= :supporting-attestation (:status %)) apps))
                           :incomplete-applications
                           (count (filter #(= :incomplete (:status %)) apps))
                           ;; Counted for the record only; the evidence
                           ;; prior does not read it.
                           :no-want-effect-applications
                           (count (filter #(= :no-want-effect (:status %)) apps))
                           :selected-only
                           (count (filter #(some #{id}
                                                  (get-in % [:patterns :selected-only]))
                                          events))}]))))]
    (let [all-events (:events snapshot)
          global (counts all-events)]
      (cond->
       (into {}
             (for [[target events] (group-by :target all-events)]
               [target
                {:schema metadata-schema
                 :target target
                 :receipt-count (count events)
                 :patterns (counts events)
                 :global-patterns global
                 :latest (select-keys (last events)
                                      [:receipt/id :run/id :mission-state :blocker
                                       :cascade-revision :artifact])
                 :revised-cascades (vec (keep revised-cascade-projection events))}]))
        (seq global)
        (assoc :wm/global
               {:schema metadata-schema :scope :global
                :receipt-count (count all-events)
                :patterns global :global-patterns global})))))

(defn pattern-evidence-prior
  "A generic empirical prior for one provisional cascade. Successful verified
   applications and incomplete verified applications are the only counts.
   Merely selected patterns have no effect. Each evidenced pattern receives
   the posterior-mean likelihood ratio against the neutral Beta(1,1) mean;
   the cascade factor is their geometric mean, avoiding a length bonus."
  [metadata candidate]
  (let [counts (or (:global-patterns metadata) (:patterns metadata) {})
        ids (->> (:precedence candidate)
                 (map #(if (map? %) (:id %) %))
                 (remove nil?) distinct vec)
        rows (vec
              (keep (fn [id]
                      (let [{:keys [successful-applications supporting-attestations
                                    incomplete-applications]}
                            (get counts id)
                            s (long (or successful-applications 0))
                            a (long (or supporting-attestations 0))
                            f (long (or incomplete-applications 0))]
                        (when (pos? (+ s a f))
                          ;; Supporting membership is useful evidence, but it
                          ;; must not masquerade as enactment. Four grounded
                          ;; support observations carry the weight of one
                          ;; directly enacted application.
                          (let [positive (+ (double s) (* 0.25 (double a)))
                                factor (/ (* 2.0 (+ 1.0 positive))
                                          (+ positive (double f) 2.0))]
                            {:pattern id :successful s :supporting a :incomplete f
                             :factor factor :log-factor (Math/log factor)}))))
                    ids))
        log-factor (if (seq rows)
                     (/ (reduce + (map :log-factor rows)) (double (count rows)))
                     0.0)]
    {:schema :wm/pattern-evidence-prior-v1
     :basis :verified-application-beta11-likelihood-ratio
     :selected-only-effect :none
     :patterns rows
     :log-factor log-factor
     :factor (Math/exp log-factor)}))

(defn attach-pattern-evidence
  "Multiply ENTRY's already-attached habit mass by the pattern evidence prior.
   G is deliberately untouched: this is AIF's empirical E term, not a hidden
   addition to the certified expected-free-energy decomposition."
  [metadata entry]
  (let [prior (pattern-evidence-prior metadata (:action entry))]
    (-> entry
        (update :habit (fnil * 1.0) (:factor prior))
        (assoc-in [:habit-provenance :pattern-feedback] prior))))

(defn attach-pattern-evidence-menu
  "Apply pattern evidence to a whole candidate menu and renormalize E."
  [metadata-by-target entries]
  (let [adjusted
        (mapv #(attach-pattern-evidence
                (or (get metadata-by-target (get-in % [:action :target]))
                    (get metadata-by-target :wm/global)
                    metadata-by-target)
                %)
              entries)
        total (reduce + 0.0 (map #(double (:habit %)) adjusted))]
    (if (pos? total)
      (mapv #(-> %
                 (update :habit / total)
                 (assoc-in [:habit-provenance :pattern-feedback :normalization]
                           {:menu-total total :status :normalized}))
            adjusted)
      entries)))

(defn load-construction-metadata
  ([] (load-construction-metadata default-path))
  ([path] (construction-metadata (read-snapshot path))))
