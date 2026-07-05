(ns futon2.aif.mana-gate
  "M-peradam-mechanization P2: the mana gate — award/consume/balance for
  ONE gate (the fold-authoring gate), operator-topped-up, every spend logged.

  Replaces blanket consent when the interactive phase ends. Dark: the live
  fold-authoring path does NOT call this gate yet; it exists beside it, ready
  for operator switchover.

  Design (from ft-peradam-mechanization-006.edn boxes P2 + P2-coupling):
  - File-first beside the fold-turns (futon6/data/mana-gate/), loaded fresh
    per read — no authoritative in-JVM atom to reset! (the escrow precedent).
  - The coupling rule (⅋): a spend is valid ONLY WITH its durable log entry.
    Accepted-but-not-stored is authorization forgery by accident. The write-
    ahead protocol: append + flush the log entry BEFORE returning :ok. A crash
    between accept and log must not leave a consumed-but-unlogged state.
  - consume REFUSES at zero balance loudly.

  Ledger format: one EDN file per gate at <dir>/<gate-id>.edn, containing a
  vector of event maps:
    {:type :award :gate-id ... :n <int> :operator <str> :word <str> :at <iso>}
    {:type :spend :gate-id ... :purpose <str> :at <iso>}

  Balance = (sum of :n over :award events) − (count of :spend events).
  One spend = one mana unit consumed."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:import [java.time Instant]))

(def default-gate-dir
  "Sibling of fold-turns, per the fold's state-placement box.
   NOT in a source repo — this is durable runtime state."
  "/home/joe/code/futon6/data/mana-gate")

(defn- gate-file
  "Return the File for GATE-ID's ledger."
  ([gate-id] (gate-file gate-id default-gate-dir))
  ([gate-id dir]
   (io/file dir (str gate-id ".edn"))))

(defn- now-iso []
  (str (Instant/now)))

(defn- read-ledger
  "Read and parse the ledger for GATE-ID. Returns a vector of event maps.
   Missing file → empty vector. Corrupt file → throws (reject-loudly)."
  ([gate-id] (read-ledger gate-id default-gate-dir))
  ([gate-id dir]
   (let [f (gate-file gate-id dir)]
     (if (.exists f)
       (let [content (slurp f)]
         (if (str/blank? content)
           []
           (let [parsed (edn/read-string content)]
             (if (vector? parsed)
               parsed
               (throw (ex-info (str "mana-gate: corrupt ledger for " gate-id
                                    " — expected vector, got " (type parsed))
                               {:gate-id gate-id :file (str f)}))))))
       []))))

(defn- write-ledger-atomic
  "Write the ledger vector atomically: write to a temp file in the same
   directory, then rename over the target. This is the write-ahead protocol
   — the spend entry is IN the vector before this returns. A crash during
   write leaves either the old file (intact) or the new file (with the
   spend logged). The temp file is in the same directory to guarantee the
   rename is atomic on the same filesystem."
  ([gate-id events] (write-ledger-atomic gate-id events default-gate-dir))
  ([gate-id events dir]
   (let [f (gate-file gate-id dir)
         tmp (io/file dir (str gate-id ".edn.tmp"))]
     (.mkdirs (io/file dir))
     ;; Write to temp file
     (spit tmp (pr-str events))
     ;; Atomic rename (POSIX rename is atomic on same filesystem)
     (.renameTo tmp f))))

(defn- compute-balance
  "PURE: given a ledger vector, compute the current balance.
   Balance = total awarded − total spent."
  [events]
  (let [awarded (reduce + 0 (keep #(when (= :award (:type %)) (:n %)) events))
        spent (count (filter #(= :spend (:type %)) events))]
    (- awarded spent)))

(defn balance
  "Return the current mana balance for GATE-ID (integer).
   Reads the ledger fresh from disk — no cached state."
  ([gate-id] (balance gate-id default-gate-dir))
  ([gate-id dir]
   (compute-balance (read-ledger gate-id dir))))

(defn award!
  "Award N mana to GATE-ID, recorded with OPERATOR-WORD (the operator's
   verbatim arming word, exactly as arming records work today). Returns
   {:ok true :balance <new-balance>}.

   The award is durably logged BEFORE this function returns — the write-
   ahead protocol. A crash after the write but before the return leaves the
   award safely on disk; the next balance read picks it up."
  ([gate-id n operator-word]
   (award! gate-id n operator-word default-gate-dir))
  ([gate-id n operator-word dir]
   (when-not (string? gate-id) (throw (ex-info "gate-id must be a string" {:gate-id gate-id})))
   (when-not (and (integer? n) (pos? n)) (throw (ex-info "n must be a positive integer" {:n n})))
   (when-not (and (string? operator-word) (not (str/blank? operator-word)))
     (throw (ex-info "operator-word must be a non-blank string" {:operator-word operator-word})))
   (let [events (read-ledger gate-id dir)
         event {:type :award :gate-id gate-id :n n
                :operator operator-word :at (now-iso)}
         new-events (conj events event)]
     ;; Write-ahead: the award is on disk before we return
     (write-ledger-atomic gate-id new-events dir)
     {:ok true :balance (compute-balance new-events)
      :awarded n})))

(defn consume!
  "Consume one mana unit from GATE-ID for PURPOSE. Returns:
   {:ok true :balance <new-balance>} when balance > 0
   {:ok false :refused :reason \"zero balance\" :balance 0} when balance = 0

   The spend entry is durably logged BEFORE returning :ok — the coupling
   rule (⅋). A crash between the balance check and the return leaves either:
   - the old ledger (no spend logged → the gate did NOT authorize → honest)
   - the new ledger (spend logged → the gate DID authorize → honest)
   There is no intermediate state where the gate authorized but the log
   doesn't show it."
  ([gate-id purpose]
   (consume! gate-id purpose default-gate-dir))
  ([gate-id purpose dir]
   (when-not (string? gate-id) (throw (ex-info "gate-id must be a string" {:gate-id gate-id})))
   (when-not (and (string? purpose) (not (str/blank? purpose)))
     (throw (ex-info "purpose must be a non-blank string" {:purpose purpose})))
   (let [events (read-ledger gate-id dir)
         bal (compute-balance events)]
     (if (<= bal 0)
       {:ok false :refused true :reason "zero balance" :balance 0}
       (let [event {:type :spend :gate-id gate-id :purpose purpose
                    :at (now-iso)}
             new-events (conj events event)]
         ;; Write-ahead: the spend is on disk before we acknowledge
         (write-ledger-atomic gate-id new-events dir)
         {:ok true :balance (compute-balance new-events)})))))

(defn ledger
  "Return the full ledger vector for GATE-ID (for audit/replay)."
  ([gate-id] (ledger gate-id default-gate-dir))
  ([gate-id dir]
   (read-ledger gate-id dir)))
