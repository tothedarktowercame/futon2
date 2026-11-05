(ns futon2.aif.wm.run-brief
  "M-run-produces-its-own-brief — the decisions half of the run brief.

  The mission's bar: a run emits, without a person assembling it, a list of
  outstanding decisions and — with this namespace's sibling work — qualified
  build state, each carrying its own basis. DECISIONS-REGISTER.md was written
  by hand from a conversation buffer; if that session had been lost it could
  not have been regenerated, because the source was a transcript, not an
  artifact.

  WHAT THIS NAMESPACE IS. A typed occasion record and a typed decision entry,
  a durable EDN ledger they live in, and an emitter that projects the ledger
  into an operator-level brief distinguishing decisions settled, newly opened,
  and still outstanding since the previous occasion. A decision is recorded
  as data at the moment it arises (see `holes/missions/.../run-decisions.edn`);
  the brief is a rendering of that structure, never of a conversation.

  WHAT IT IS ALLOWED TO DO. Read one named ledger file and report what is in
  it. No conversation buffer, no chat log, no defaulting. An entry that does
  not validate is a typed error naming the entry, not a silent drop — the
  ledger is the record, and a record that quietly loses rows is worse than
  one that refuses to load.

  DETERMINISM. `render-brief` emits only from ledger data; no clock, no
  environment. Running the generator twice leaves no diff."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]))

(def ledger-schema :wm/run-decision-ledger-v1)
(def occasion-schema :wm/occasion-v1)
(def decision-schema :wm/run-decision-entry-v1)

(def statuses #{:settled :newly-opened :outstanding})

(defn- nonblank [x] (and (string? x) (not (str/blank? x))))

(defn validate-occasion
  "An occasion is a recordable run-as-a-thing: a milestone with an id, a
  sequence number and a summary. Returns nil when valid, else a typed error."
  [occasion]
  (if (and (map? occasion)
           (= occasion-schema (:schema occasion))
           (nonblank (:occasion/id occasion))
           (int? (:occasion/seq occasion))
           (nonblank (:occasion/summary occasion)))
    nil
    {:schema :wm/run-brief-error-v1
     :kind :invalid-occasion
     :entry occasion}))

(defn validate-decision
  "A decision entry attaches to an occasion and carries its own basis, so a
  reader can tell a current claim from a remembered one. Returns nil when
  valid, else a typed error."
  [decision]
  (if (and (map? decision)
           (= decision-schema (:schema decision))
           (nonblank (:decision/id decision))
           (nonblank (:occasion/id decision))
           (contains? statuses (:decision/status decision))
           (nonblank (:decision/summary decision))
           (nonblank (:decision/basis decision)))
    nil
    {:schema :wm/run-brief-error-v1
     :kind :invalid-decision-entry
     :entry decision}))

(defn validate-ledger
  "Validate the whole ledger: every occasion and entry well-formed, every
  entry attached to a declared occasion, ids unique. Typed errors, no drops."
  [ledger]
  (let [occasion-errors (keep validate-occasion (:occasions ledger))
        decisions (:decisions ledger)
        decision-errors (keep validate-decision decisions)]
    (cond
      (not= ledger-schema (:schema ledger))
      {:schema :wm/run-brief-error-v1 :kind :invalid-ledger-schema :ledger ledger}

      (or (seq occasion-errors) (seq decision-errors))
      {:schema :wm/run-brief-error-v1 :kind :invalid-entries
       :occasion-errors occasion-errors :decision-errors decision-errors}

      :else
      (let [occasion-ids (set (map :occasion/id (:occasions ledger)))
            orphans (remove #(contains? occasion-ids (:occasion/id %)) decisions)
            all-decision-ids (map :decision/id decisions)
            dupes (when (not= (count all-decision-ids)
                               (count (distinct all-decision-ids)))
                    true)]
        (cond
          (seq orphans)
          {:schema :wm/run-brief-error-v1 :kind :decision-occasion-unknown
           :entries orphans}

          dupes
          {:schema :wm/run-brief-error-v1 :kind :duplicate-decision-id}

          :else ledger)))))

(defn load-ledger
  "Read and validate the ledger at PATH. A missing file is a typed absence
  naming what would have to exist; an invalid file is a typed error. Neither
  is ever an empty brief."
  [path]
  (if (.exists (io/file path))
    (validate-ledger (edn/read-string (slurp path)))
    {:schema :wm/run-brief-error-v1 :kind :ledger-absent :path (str path)}))

(defn classify-since
  "Group the decisions attached to the latest occasion (the occasion with the
  highest :occasion/seq — i.e. those that arose after the previous occasion)
  by status: :settled, :newly-opened, :outstanding. Each row keeps its basis."
  [ledger]
  (let [occasions (sort-by :occasion/seq (:occasions ledger))
        latest (peek (vec occasions))
        previous (peek (pop (vec occasions)))]
    {:schema :wm/run-brief-classification-v1
     :since-occasion {:previous (:occasion/id previous)
                      :latest (:occasion/id latest)}
     :by-status (group-by :decision/status
                          (filter #(= (:occasion/id latest) (:occasion/id %))
                                  (:decisions ledger)))}))

(defn render-brief
  "Project the ledger into the operator-level decisions brief: what was
  settled since the previous occasion, what newly needs an operator call, and
  what is still outstanding — every row with its basis. Deterministic: the
  same ledger always renders to the same text."
  [ledger]
  (let [validated (validate-ledger ledger)]
    (if (= :wm/run-brief-error-v1 (:schema validated))
      validated
      (let [occasions (sort-by :occasion/seq (:occasions ledger))
            latest (peek (vec occasions))
            previous (peek (pop (vec occasions)))
            rows (fn [entries]
                   (if (seq entries)
                     (for [e (sort-by :decision/id entries)]
                       (format "- **%s** — %s%n  basis: %s"
                               (:decision/id e) (:decision/summary e)
                               (:decision/basis e)))
                     ["- (none)"]))
            latest-id (:occasion/id latest)
            at-latest (fn [status]
                        (filter #(and (= latest-id (:occasion/id %))
                                      (= status (:decision/status %)))
                                (:decisions ledger)))
            group (fn [entries label]
                    (concat [(format "### %s" label)] (rows entries)))
            header (format "# Run brief — decisions since %s (occasion %s)"
                           (or (:occasion/id previous) "(none recorded)")
                           (:occasion/id latest))]
        (str/join "\n" (concat [header ""]
                               (group (at-latest :settled)
                                      "Settled since the previous occasion")
                               [""]
                               (group (at-latest :newly-opened)
                                      "Newly needing an operator call")
                               [""]
                               (group (filter #(= :outstanding (:decision/status %))
                                              (:decisions ledger))
                                      "Still outstanding")
                               [""]))))))

(defn write-brief!
  "Render the ledger at LEDGER-PATH and write the brief to OUT-PATH. Returns
  {:written true :path ... :bytes n} or the typed error unchanged. Idempotent
  on the same ledger bytes."
  [ledger-path out-path]
  (let [ledger (load-ledger ledger-path)]
    (if (= :wm/run-brief-error-v1 (:schema ledger))
      ledger
      (let [text (render-brief ledger)
            bytes (.getBytes text "UTF-8")]
        (with-open [w (io/writer out-path)]
          (.write w text))
        {:written true :path (str out-path) :bytes (alength bytes)}))))
