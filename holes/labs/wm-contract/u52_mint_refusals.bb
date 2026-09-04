#!/usr/bin/env bb
;; U52 -- mint the rung-3 refusals into the tension ledger.
;;
;;   bb holes/labs/wm-contract/u52_mint_refusals.bb           ; dry run: validate only
;;   bb holes/labs/wm-contract/u52_mint_refusals.bb --append  ; append through U41
;;
;; WHY THIS IS A SEPARATE STEP AND NOT PART OF THE TICK. The ruling says a
;; rung-3 refusal MINTS a tension record. `append-tension!` validates the whole
;; ledger, appends, and atomically replaces the file -- a file write, on the
;; tick's critical path, of a curated design artifact. The live ladder therefore
;; BUILDS the refusal records and attaches them to the judgement
;; (`:task-belief-refusals`), and the mint is performed here, over the recorded
;; field, by the API the ledger declares as its only writer. That boundary is
;; named rather than assumed: C507 states it, and a reviewer who thinks the mint
;; belongs inside the tick can say so against a stated position.
;;
;; IDEMPOTENT. `append-tension!` returns `:already-present` for an exact replay
;; and refuses a conflicting identity, so running this twice is a no-op and the
;; ledger is byte-identical after the second run.

(require '[clojure.edn :as edn] '[clojure.java.io :as io] '[clojure.string :as str])

(def repo-root (str (System/getProperty "user.home") "/code/futon2"))
(def lab (io/file repo-root "holes/labs/wm-contract"))
(def refusals-path (io/file lab "runs/U52-ladder/04-refusals.edn"))

(when-not (.exists refusals-path)
  (println "u52_mint_refusals: no" (str refusals-path) "-- run u52_ladder.clj first")
  (System/exit 1))

;; The sole write API, loaded rather than reimplemented.
(load-file (str (io/file lab "u41_tension_ledger.bb")))

(def u41-append!
  ;; NOT `(def append-tension! ...)`: this script also loads into `user`, so
  ;; that name would rebind the var to itself and every call would recurse into
  ;; a StackOverflowError. Found by running it.
  (resolve (quote user/append-tension!)))
(def u41-validate (resolve (quote user/validate)))
(def ledger-path (io/file lab "tension-ledger.edn"))

(def payloads
  (let [d (edn/read-string (slurp refusals-path))]
    (vec (for [[fid m] (sort-by key (:per-field d))]
           {:field fid :refused (:refused m) :mint (:mint m)}))))

(defn shape-check
  "The two rules that make a machine-born tension readable, checked before the
   ledger is touched: the statement is the in-record refusal read back verbatim
   (u41 control 12), and the mint event's type matches the tension's status."
  [{:keys [tension event]}]
  (cond-> []
    (not= (edn/read-string (:tension/statement tension)) (:tension/refusal tension))
    (conj :statement-is-not-the-refusal-verbatim)
    (not= (:tension/status tension) (:event/type event))
    (conj :mint-event-type-does-not-match-status)
    (not= (:tension/id tension) (:event/tension event))
    (conj :event-names-another-tension)
    (empty? (get-in tension [:tension/provenance :pointers]))
    (conj :no-pointers)))

(def append? (contains? (set *command-line-args*) "--append"))

(println "u52_mint_refusals:" (count payloads) "payload(s) from" (str refusals-path))
(let [defects (mapcat (fn [{:keys [field mint]}]
                        (map (fn [d] [field d]) (shape-check mint)))
                      payloads)]
  (when (seq defects)
    (println "  REFUSED, shape defects:" (pr-str (vec defects)))
    (System/exit 1))
  (println "  shape ok on all payloads"))

(if-not append?
  (do (doseq [{:keys [field refused mint]} payloads]
        (println (format "  %s: %d refusal(s) -> tension %s (dry run)"
                         (name field) refused (pr-str (get-in mint [:tension :tension/id])))))
      (println "  dry run; pass --append to write. Ledger untouched."))
  (do
    (doseq [{:keys [field refused mint]} payloads]
      (let [r (u41-append! (:tension mint) (:event mint))]
        (println (format "  %s: %d refusal(s) -> %s %s"
                         (name field) refused
                         (name (:status r)) (pr-str (:tension/id r))))))
    (let [ds (u41-validate (edn/read-string (slurp ledger-path)))]
      (if (seq ds)
        (do (println "  LEDGER INVALID AFTER APPEND:" (pr-str (mapv :defect ds))) (System/exit 1))
        (println "  ledger validates after append ("
                 (count (:tensions (edn/read-string (slurp ledger-path)))) "tensions )")))))
(println "u52_mint_refusals: done" (if append? "(appended)" "(dry run)")
         (str/join "" []))
