(ns futon2.aif.realized-recording
  "19b stronger envelope. Legacy normalization never confers this contract.
   RUN4 choices: claude-1 invoke-1788971284841-16610-41950f7a; Item 23."
  (:require [clojure.set :as set]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [futon2.aif.full-loop-cohort :as cohort]
            [futon2.aif.observation :as observation]
            [futon2.aif.preference-module :as preference]))

(def contract :wm/realized-recording-v1)
(def required-fields
  #{:schema :recording-contract :record/id :revision :supersedes
    :run/id :decision/ref :attempt/id :policy :tick :window :subject :execution
    :expected-score :realized-score :scale :measurement :outcome :classification
    :closure :observations :preferences :evidence :review})

(defn marked? [r] (= contract (:recording-contract r)))
(defn unknown [reason] {:status :unknown :reason reason :obtain :capture-at-source})
(defn observed [value domain evidence]
  {:status :observed :value value :domain domain :evidence evidence})
(defn adapter-error [message evidence]
  (throw (ex-info message {:type :observation-adapter-error :evidence evidence})))

(defn observation? [o]
  (and (map? o)
       (or (= :observed (:status o)) (not (contains? o :distribution)))
       (case (:status o)
         :observed (and (contains? o :value) (some? (:value o))
                        (some? (:domain o)) (some? (:evidence o)))
         :unknown (and (:reason o) (:obtain o) (not (contains? o :value)))
         :inadmissible (and (:reason o) (:evidence o) (not (contains? o :value)))
         :not-applicable (and (:reason o) (not (contains? o :value)))
         false)))

(defn- require! [ok clause]
  (when-not ok (throw (ex-info "Invalid realized recording" {:clause clause}))))

(defn- before? [a b]
  (try (.isBefore (java.time.Instant/parse a) (java.time.Instant/parse b))
       (catch Exception _ false)))

(defn validate!
  "Validate the stronger envelope, including absent-vs-observed projections."
  [r]
  (require! (and (marked? r) (= :wm/realized-outcome-v1 (:schema r))
                 (set/subset? required-fields (set (keys r)))) :field-table)
  (require! (every? some? (map r [:record/id :run/id :decision/ref :attempt/id])) :identity)
  (require! (and (nat-int? (:revision r))
                 (if (zero? (:revision r)) (nil? (:supersedes r))
                     (some? (:supersedes r)))) :revision)
  (require! (or (some? (:policy r))
                 (= :not-applicable (get-in r [:execution :policy :status]))) :policy)
  (require! (and (map? (:subject r))
                 (every? #(contains? (:subject r) %) [:mission :before :after])
                 (get-in r [:execution :state]) (get-in r [:execution :evidence])) :subject-execution)
  (require! (and (get-in r [:window :rule]) (get-in r [:window :version])
                 (get-in r [:window :clock])
                 (every? #(observation? (get-in r [:window %]))
                         [:decision :start :cutoff :observed-at])) :window)
  (require! (and (map? (:evidence r)) (seq (:evidence r))
                 (every? (fn [[_ e]]
                           (and (or (:digest e) (and (:repo e) (:path e) (:revision e)))
                                (:locator e) (:adapter e))) (:evidence r))) :evidence)
  (let [m (:measurement r)]
    (require! (every? #(some? (get m %))
                      [:id :quantity :units :sign :method :expected-source
                       :expected-window :realized-window]) :measurement)
    (doseq [[leg score] [[:expected :expected-score] [:realized :realized-score]]]
      (let [o (get m leg)]
        (require! (observation? o) :measurement-status)
        (require! (if (= :observed (:status o))
                    (and (number? (:value o)) (= (:value o) (get r score))
                         (contains? (:evidence r) (:evidence o)))
                    (nil? (get r score))) :numeric-projection))))
  (let [c (:classification r)]
    (require! (and (observation? c) (:classifier c) (:basis c) (:grain c)) :classification)
    (require! (if (= :observed (:status c))
                (and (= (:outcome r) (:value c))
                     (contains? cohort/outcome-kinds (:outcome r))
                     (contains? (:evidence r) (:evidence c)))
                (nil? (:outcome r))) :category-projection))
  (require! (and (#{:open :closed :unknown} (get-in r [:closure :state]))
                 (if (= :closed (get-in r [:closure :state]))
                   (every? #(get-in r [:closure %]) [:checkpoint :judgment :reviewer :evidence])
                   true)) :closure)
  (require! (and (#{:proposed :reviewed} (get-in r [:review :state]))
                 (get-in r [:review :scope]) (get-in r [:review :evidence])
                 (if (= :reviewed (get-in r [:review :state]))
                   (get-in r [:review :reviewer]) true)) :review)
  (let [o (:observations r)]
    (require! (and (:version o) (:alignment o)
                   (= (:attempt/id r) (:attempt/id o))
                   (= (:decision/ref r) (:decision/ref o))
                   (observation? (:checkpoints o))
                   (every? #(observation? (get-in o [:channels %])) [:pre :post])) :paired-capture)
    (when (= :observed (get-in o [:checkpoints :status]))
      (require! (and (vector? (get-in o [:checkpoints :value]))
                     (every? #(and (:event/id %) (:checkpoint %) (:domain %)
                                   (observation? (:at %))
                                   (contains? (:evidence r) (:evidence %)))
                             (get-in o [:checkpoints :value]))) :checkpoint-events)))
  (require! (every? #(observation? (get-in r [:preferences %]))
                   [:module :predicted :realized :assessment]) :preferences)
  (doseq [phase [:pre :post]
          :let [sample (get-in r [:observations :channels phase])]
          :when (= :observed (:status sample))]
    (require! (and (map? (:value sample))
                   (:event/id sample) (observation? (:at sample))
                   (every? (fn [[_ o]] (and (observation? o)
                                           (if (= :observed (:status o))
                                             (and (number? (:value o)) (:units o)) true)))
                           (:value sample))) :channel-status))
  (doseq [kind [:predicted :realized]
          :let [reading (get-in r [:preferences kind])]
          :when (= :observed (:status reading))]
    (let [module (get-in r [:preferences :module :value])]
      (require! (and (= :observed (get-in r [:preferences :module :status]))
                     (:id module) (:version module) (:context module)
                     (get-in r [:preferences :module :digest])) :module-pin)
      (doseq [[_ v] (:value reading)]
        (require! (and (:version v) (:criterion v)
                       (contains? (:evidence r) (:evidence v))
                       (if (= :realized kind)
                         (#{:observed :unknown :inadmissible :not-applicable} (:status v))
                         (#{:predicted :unknown :inadmissible :not-applicable} (:status v))))
                  :preference-reading))
      (when (= :realized kind) (preference/assess module (:value reading)))))
  (let [assessment (get-in r [:preferences :assessment])]
    (when (= :observed (:status assessment))
      (require! (and (:assessor-version assessment)
                     (#{:candidate :accepted} (:mode assessment))
                     (contains? (:evidence r) (:evidence assessment))) :assessment-provenance)))
  (let [occurrences (get-in r [:preferences :occurrences])]
    (require! (and (vector? occurrences)
                   (every? #(every? (fn [k] (contains? % k))
                                    [:mission :phase :occurrence :transition :readings]) occurrences)
                   (= (count occurrences)
                      (count (set (map #(select-keys % [:mission :phase :occurrence :transition])
                                       occurrences))))) :process-occurrences)
    (doseq [occurrence occurrences :when (seq (:readings occurrence))]
      (require! (= :observed (get-in r [:preferences :module :status])) :process-module-pin)
      (preference/assess (get-in r [:preferences :module :value]) (:readings occurrence))))
  (doseq [fact (:institutions r)]
    (require! (every? #(contains? (:evidence r) (get fact %))
                     [:declared-rule :enforcement :compliance :preference]) :institution-facts))
  r)

(defn preference-readings [r kind]
  (validate! r)
  (let [reading (get-in r [:preferences kind])]
    (when (= :observed (:status reading))
      (when (= kind :predicted)
        (require! (and (:model reading) (:horizon reading) (:outcome-domain reading)
                       (contains? (:evidence r) (:receipt reading))
                       (string? (:captured-at reading))
                       (string? (get-in r [:window :decision :value]))
                       (before? (:captured-at reading)
                                (get-in r [:window :decision :value]))) :forecast-provenance))
      (:value reading))))

(defn calibration-admitted? [r]
  (and (marked? r)
       (try (validate! r) true (catch clojure.lang.ExceptionInfo _ false))
       (= :prediction (get-in r [:measurement :expected-source]))
       (= :admissible (get-in r [:measurement :admission]))
       (every? #(= :observed (get-in r [:measurement % :status])) [:expected :realized])
       (= (:scale r) (get-in r [:measurement :units]))
       (let [m (:measurement r) forecast (:forecast m) realization (:realization m)]
         (and (:model forecast) (contains? (:evidence r) (:evidence forecast))
              (contains? (:evidence r) (:evidence realization))
              (string? (:captured-at forecast))
              (string? (get-in r [:window :decision :value]))
              (before? (:captured-at forecast) (get-in r [:window :decision :value]))
              (= (select-keys m [:quantity :units :method])
                 (select-keys forecast [:quantity :units :method])
                 (select-keys realization [:quantity :units :method]))
              (= (:expected-window m) (:window forecast))
              (= (:realized-window m) (:window realization))))
       (true? (get-in r [:measurement :aligned?]))))

(defn category [r grain]
  (validate! r)
  (when (and (= grain (get-in r [:classification :grain]))
             (= :observed (get-in r [:classification :status])))
    (:outcome r)))

(defn terminal-disposition [r]
  (when (and (= :closed (get-in r [:closure :state]))
             (= :reviewed (get-in r [:review :state]))
             (= :attempt (get-in r [:review :scope])))
    (category r :attempt)))

(defn sample-key [r]
  [(:run/id r) (:decision/ref r) (:attempt/id r) (get-in r [:measurement :id])])

(defn latest-revisions
  "Select one revision per sample. Preserve originals in the input journal.
   Replay consumers must derive anew from this projection, not append deltas."
  [records]
  (vals
   (reduce (fn [m r]
             (validate! r)
             (let [k (sample-key r) old (get m k)]
               (cond
                 (= old r) m
                 (nil? old) (do (require! (zero? (:revision r)) :missing-predecessor)
                               (assoc m k r))
                 (and (= (inc (:revision old)) (:revision r))
                      (= (:record/id old) (:supersedes r))) (assoc m k r)
                 :else (throw (ex-info "Conflicting recording revision"
                                       {:sample k :record (:record/id r)})))))
           (array-map) records)))

(defn envelope
  "Explicit opt-in for the two enactor producers; context supplies the field
   table and warrants. Without context, return original legacy bytes/shape."
  [legacy context]
  (if context
    (let [r (cond-> (merge context legacy {:schema :wm/realized-outcome-v1
                                          :recording-contract contract})
              (:expected-source legacy)
              (assoc-in [:measurement :expected-source] (:expected-source legacy)))]
      (require! (= (:scale r) (get-in r [:measurement :units])) :producer-scale)
      (validate! r))
    legacy))

(defn persist!
  "A record path is immutable. Corrections require a new path and explicit
   revision/supersedes; readers retain the original and derive latest only."
  [path r]
  (when (marked? r) (validate! r))
  (if (.exists (io/file path))
    (require! (= r (edn/read-string (slurp path))) :immutable-record-conflict)
    (do (io/make-parents path)
        (with-open [writer (io/writer (java.nio.file.Files/newOutputStream
                                      (.toPath (io/file path))
                                      (into-array java.nio.file.OpenOption
                                                  [java.nio.file.StandardOpenOption/CREATE_NEW
                                                   java.nio.file.StandardOpenOption/WRITE])))]
          (.write writer (pr-str r)))))
  r)

(defn step-envelope
  "RUN4 stepped producer: one attempt per accepted step, next accepted window.
   G-core readback is not a forecast. Joe is the designated reviewer, not an
   invented completed review. Mission closure and missing samples stay unknown."
  [legacy {:keys [run-id step-index before after evidence]}]
  (let [ref :source
        time #(if % (observed % :utc-instant ref) (unknown :time-not-recorded))
        leg #(if (number? %) (observed % :g-core ref) (unknown :score-not-recorded))
        attempt (str run-id "/" step-index)
        decision (or (:observation/observed-for-tick legacy) (:run/id before))
        channels (fn [r] (if (map? (:observation r))
                           (assoc (observed (into {} (map (fn [k]
                                                   (let [o (get-in r [:observation-envelope :channels k])
                                                         v (:value o)]
                                                    [k (if (and (= :observed (:variant o)) (number? v))
                                                         (assoc (observed v [:wm/channel k] ref)
                                                                :units :normalized)
                                                         (unknown (or (:reason o) :channel-provenance-unavailable)))]))
                                                  observation/observation-channels))
                                     :wm/channels-v1 ref)
                                  :event/id (:run/id r) :at (time (:timestamp r)))
                           (unknown :snapshot-not-recorded)))
        classification (merge (if (:outcome legacy)
                                (observed (:outcome legacy) :wm/cohort-v1 ref)
                                (unknown :category-unavailable))
                              {:classifier :mission-hole-delta-v1 :grain :step
                               :basis (:outcome/basis legacy :missing)})]
    (envelope legacy
      {:record/id (str attempt "/realized/0") :revision 0 :supersedes nil
       :run/id run-id :decision/ref decision :attempt/id attempt
       :window {:rule :next-accepted-step :version 1 :clock :utc
                :decision (time (:timestamp before)) :start (unknown :dispatch-time-not-recorded)
                :cutoff (time (:timestamp after)) :observed-at (unknown :observation-time-not-recorded)}
       :subject {:mission (:policy legacy) :before (unknown :revision-not-recorded)
                 :after (unknown :revision-not-recorded)}
       :execution {:selected (some? (:policy legacy)) :state :unknown
                   :evidence ref :policy (if (:policy legacy)
                                          (observed (:policy legacy) :policy-id ref)
                                          {:status :not-applicable :reason :no-selection})}
       :expected-score (:expected-score legacy) :realized-score (:realized-score legacy)
       :policy (:policy legacy) :tick (:tick legacy) :scale :g-core
       :measurement {:id :g-core-readback-v1 :quantity :g-core :units :g-core
                     :sign :lower-is-better :method :recorded-ranking-v1
                     :expected-source :reevaluation :admission :inadmissible
                     :aligned? true :expected-window (:timestamp before :unknown)
                     :realized-window (:timestamp after :unknown)
                     :expected (leg (:expected-score legacy)) :realized (leg (:realized-score legacy))}
       :outcome (:outcome legacy) :classification classification
       :closure {:state :unknown :reason :step-is-not-mission-closure}
       :observations {:version 1 :attempt/id attempt :decision/ref decision
                      :alignment {:rule :accepted-step-order :version 1
                                  :pre (:run/id before) :post (:run/id after)}
                      :checkpoints (unknown :checkpoint-events-not-recorded)
                      :channels {:pre (channels before) :post (channels after)}}
       :preferences {:module (unknown :module-not-pinned)
                     :predicted (unknown :forecast-not-recorded)
                     :realized (unknown :readings-not-recorded)
                     :assessment (unknown :assessment-not-performed) :occurrences []}
       :evidence {ref evidence}
       :review {:state :proposed :designated-reviewer "Joe" :scope :step
                :evidence ref}})))
