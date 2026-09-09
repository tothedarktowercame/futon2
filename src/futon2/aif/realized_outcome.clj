(ns futon2.aif.realized-outcome
  "ONE outcome vocabulary for the realized-outcome record, and marked accessors
   for the vocabularies that came before it.

   THE DEFECT THIS EXISTS TO CLOSE (measured in C511-repair-or-elaborate.md
   section 3, over 57 daily files and 889 records): three key-sets name one
   quantity and no reader sees any writer.

     1. the 88 recorded outcomes (data/wm-trace/wm-trace-2026-07-02.edn ..
        -06.edn) carry `{:policy :expected-G :realized-G :tick}`;
     2. today's producer writes `{:policy :expected-score :realized-score :tick}`
        (fold_realized.clj:96-100, :204-231);
     3. the readers look for a CATEGORICAL `:outcome`
        (war_machine.clj:2425-2428, u39_selection_retrospective.bb:55-59) and
        selection-gain requires the numeric legs by their (2) names
        (selection_gain.clj:197-205).

   So the corpus is invisible to gamma, and the categorical readers see nothing
   at all: 889 records, 98 rankings, 0 readable outcomes since 2026-07-06.

   THE ONE SCHEMA, `:wm/realized-outcome-v1`. A realized-outcome record is

     {:schema         :wm/realized-outcome-v1
      :policy         <policy / mission id>       ; who the outcome is about
      :tick           <enactment tick>            ; gamma's dedup key
      :expected-score <number>                    ; the leg predicted at decision time
      :realized-score <number>                    ; the same quantity re-observed
      :outcome        <categorical>               ; full-loop-cohort/outcome-vocabulary
      :scale          <keyword>}                  ; what the two legs count

   The numeric legs and the categorical outcome are DIFFERENT ANSWERS, not two
   spellings of one: the legs are gamma's calibration pair, the categorical is
   what `recent-non-progress-count` and the tripwires branch on. Carrying both
   under one key is the unification; picking one would have left the other
   reader dark.

   `:expected-G` / `:realized-G` are the July 2026 spelling of the two legs and
   nothing else. They are read here, marked `:july-2026-delta-g`, and never
   written: a vocabulary is retired by making it readable, not by rewriting the
   records that used it.

   Registered as a free hand in aif-equations.edn `:choices`
   `:realized-outcome-schema` (`:status :observed-not-decided` -- this states
   what the machine is made to do, and Joe has not ruled).

   PURE: no I/O, no state, no loop. Loadable from babashka (u39 adds `src` to
   the classpath) so the lab's readers and the JVM's readers cannot drift."
  (:require [clojure.set :as set]
            [futon2.aif.realized-recording :as recording]))

(def schema
  "The one schema key. Written by every producer of a realized outcome."
  :wm/realized-outcome-v1)

(def vocabularies
  "Every key-set that has named the two numeric legs, newest first. The value is
   the leg spelling; the key is the name the accessors report."
  (array-map
   :v1               {:expected :expected-score :realized :realized-score}
   :july-2026-delta-g {:expected :expected-G     :realized :realized-G}))

(def historical-vocabularies
  "The vocabularies that are READ and never written. Marked, so a reader that
   reports one is saying it read an old record and not that the producer moved."
  #{:july-2026-delta-g})

(def categorical-places
  "The three places a CATEGORICAL outcome has ever been written, in the order
   `war_machine.clj:2425-2428` looks. Unchanged: this ns does not move them, it
   gives them one definition instead of the three copies C511 found."
  [[:outcome] [:enactment :outcome] [:realized-outcome :outcome]])

(defn vocabulary
  "Which leg vocabulary RO is written in, or nil when neither leg pair is
   readable. A record carrying both spellings is `:v1`: the newest wins, and
   `mixed-vocabulary?` is how a caller notices the overlap."
  [ro]
  (when (map? ro)
    (some (fn [[name {:keys [expected realized]}]]
            (when (and (number? (get ro expected)) (number? (get ro realized)))
              name))
          vocabularies)))

(defn mixed-vocabulary?
  "Does RO carry legs in more than one vocabulary? Nothing writes such a record;
   a true here means a producer is mid-migration and the reader should say so."
  [ro]
  (and (map? ro)
       (< 1 (count (filter (fn [[_ {:keys [expected realized]}]]
                             (or (contains? ro expected) (contains? ro realized)))
                           vocabularies)))))

(defn expected-score
  "The predicted leg, in whichever vocabulary the record uses."
  [ro]
  (when-let [v (vocabulary ro)] (get ro (:expected (get vocabularies v)))))

(defn realized-score
  "The re-observed leg, in whichever vocabulary the record uses."
  [ro]
  (when-let [v (vocabulary ro)] (get ro (:realized (get vocabularies v)))))

(defn legs-readable?
  "Both numeric legs present under one vocabulary -- gamma's precondition."
  [ro]
  (boolean (vocabulary ro)))

(defn categorical-outcome
  "The categorical outcome carried by a TRACE RECORD, from the three places.
   Returns nil when the record carries none, which is what 889 of 889 records
   in the live corpus do."
  [trace-record]
  (when (map? trace-record)
    (some (fn [path]
            (let [container (if (= path [:outcome]) trace-record
                                (get-in trace-record (butlast path)))]
              (if (recording/marked? container)
                (recording/category container :step)
                (get-in trace-record path)))) categorical-places)))

(defn realized-outcome
  "The realized-outcome record riding a trace record, if any."
  [trace-record]
  (when (map? trace-record) (:realized-outcome trace-record)))

(defn normalize
  "RO in the one schema, with the vocabulary it was READ in recorded on it.
   Historical legs are copied to their v1 names and the originals are LEFT IN
   PLACE: this is a reading, and a reading that deleted the bytes it read from
   would make the next reader's disagreement unfindable."
  [ro]
  (when-let [v (vocabulary ro)]
    (cond-> (assoc ro
                   :schema schema
                   :expected-score (expected-score ro)
                   :realized-score (realized-score ro)
                   :outcome/read-as v)
      (contains? historical-vocabularies v) (assoc :outcome/historical? true))))

(defn conforms?
  "Does RO carry the one schema's required keys under the v1 spelling?"
  [ro]
  (and (map? ro)
       (= schema (:schema ro))
       (= :v1 (vocabulary ro))
       (empty? (set/difference #{:policy :tick} (set (keys ro))))))
