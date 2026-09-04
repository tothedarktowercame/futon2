#!/usr/bin/env bb
(ns checks.contract-authority-current
  (:require [babashka.process :as process]
            [writer-fence-capability :as fence]
            [cheshire.core :as json]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.string :as str])
  (:import [java.time Instant]))

(def mathlib-root "/home/joe/code/mathlib4")
(def contract-path
  "/home/joe/code/mathlib4/DarkTower/WarMachine/holes-contract.json")
(def lean-path "DarkTower/WarMachine/Holes.lean")

(def repo-root ;; derived from the script location, so a worktree run targets its own checkout
  (-> (java.io.File. (System/getProperty "babashka.file"))
      .getAbsoluteFile .getParentFile .getParentFile .getPath))

(defn shell [& argv]
  (apply process/shell {:continue true :out :string :err :string
                        :dir mathlib-root}
         argv))

(defn current-state []
  (let [started-at (str (Instant/now))
        head-result (shell "git" "rev-parse" "HEAD")
        last-change-result (shell "git" "log" "-1" "--format=%H" "--" lean-path)
        diff-result (shell "git" "diff" "--quiet" "HEAD" "--" lean-path)
        blob-result (shell "git" "rev-parse" (str "HEAD:" lean-path))
        contract (json/parse-string (slurp contract-path) true)
        recorded-authority (get-in contract [:source :git-sha])
        recorded-blob-result (shell "git" "rev-parse"
                                    (str recorded-authority ":" lean-path))
        finish-head-result (shell "git" "rev-parse" "HEAD")
        finish-blob-result (shell "git" "rev-parse" (str "HEAD:" lean-path))]
    {:recorded-authority recorded-authority
     :mathlib-head (str/trim (:out head-result))
     :mathlib-head-after (str/trim (:out finish-head-result))
     :holes-last-content-change (str/trim (:out last-change-result))
     :recorded-holes-blob (str/trim (:out recorded-blob-result))
     :current-holes-blob (str/trim (:out blob-result))
     :current-holes-blob-after (str/trim (:out finish-blob-result))
     :holes-clean? (zero? (:exit diff-result))
     :recorded-authority-readable? (zero? (:exit recorded-blob-result))
     :readable? (and (zero? (:exit head-result))
                     (zero? (:exit last-change-result))
                     (zero? (:exit blob-result))
                     (zero? (:exit finish-head-result))
                     (zero? (:exit finish-blob-result)))
     :observation-interval {:started-at started-at
                            :finished-at (str (Instant/now))}}))

(defn assess [{:keys [recorded-authority holes-last-content-change
                      recorded-holes-blob current-holes-blob holes-clean?
                      recorded-authority-readable? readable? mathlib-head
                      mathlib-head-after current-holes-blob-after]
               :as state}]
  (let [failures (cond-> []
                   (not readable?) (conj :source-unreadable)
                   (not recorded-authority-readable?)
                   (conj :recorded-authority-unreadable)
                   (not holes-clean?) (conj :holes-working-tree-dirty)
                   (not= recorded-authority holes-last-content-change)
                   (conj :contract-authority-not-last-source-change)
                   (and recorded-authority-readable?
                        (not= recorded-holes-blob current-holes-blob))
                   (conj :contract-source-not-current)
                   (or (not= mathlib-head mathlib-head-after)
                       (not= current-holes-blob current-holes-blob-after))
                   (conj :repository-basis-moved))]
    (assoc state :pass? (empty? failures) :failures failures)))

;; ---------------------------------------------------------------------------
;; --deposit <run-id> -- one run-era ledger row (RE3)
;; ---------------------------------------------------------------------------
;;
;; This check reads the mathlib4 working tree AS IT STANDS: whether the
;; committed contract was generated from the current content of `Holes.lean`.
;; That is a property of the tree at the moment of asking, and a ledger row is
;; about a named RUN, so the deposit asks the run store rather than the tree.
;;
;; The decisive question for this check is whether the run store records any
;; contract identity the pin could be evaluated against. It is answered by
;; searching the store for the contract's recorded authority sha -- not by
;; asserting the absence -- and the search is written into the receipt.
;; If the store carries no such identity, the deposited verdict is
;; :typed-absence and the live derivation is recorded in the receipt, plainly
;; labelled as a statement about the tree at deposit time.

(defn- store-files
  "Every file of the run store, as store-relative paths, sorted. RECURSIVE
   (RE5): the RE4 rationale records live in a `rationale/` SUBDIRECTORY, and a
   top-level-only scan cannot see them, so the search below would have had to
   find the authority sha in the README's prose or not at all. A run store with
   no subdirectory yields exactly what the flat listing yielded."
  [^java.io.File d]
  (when (.isDirectory d)
    (let [base (str (.getPath d) "/")]
      (->> (file-seq d)
           (filter #(.isFile ^java.io.File %))
           (map #(str/replace-first (.getPath ^java.io.File %) base ""))
           sort
           vec))))

(defn run-store-scan
  "What the run's own store says about a contract identity. `mentions` is every
   file whose text contains \"contract\" together with the distinct spellings
   found, so a reader can see what the store DOES carry (the producer-contract
   tag) beside what it does not (the mathlib4 source sha).

   When the authority IS found, `:recorded-authority-appears-in` names the files
   that carry it. A bare boolean is not a witness: a README sentence quoting the
   sha satisfies it exactly as a machine-written record does, and only the list
   lets a reader tell those apart. The key is omitted when nothing matched, so a
   typed-absence receipt is unchanged."
  [root rel authority]
  (let [d (io/file root rel)
        files (store-files d)
        texts (into {} (for [f files] [f (slurp (io/file d f))]))
        carriers (vec (sort (keep (fn [[f t]] (when (str/includes? t authority) f)) texts)))]
    (cond-> {:dir rel
             :exists? (boolean files)
             :holds (vec files)
             :mentions-of-contract
             (into (sorted-map)
                   (for [[f t] texts
                         :let [ms (vec (sort (distinct (re-seq #"[:a-zA-Z0-9_-]*[Cc]ontract[:a-zA-Z0-9_/.-]*" t))))]
                         :when (seq ms)]
                     [f ms]))
             :recorded-authority-sought authority
             :recorded-authority-appears-in-store? (boolean (seq carriers))}
      (seq carriers) (assoc :recorded-authority-appears-in carriers))))

(defn deposit-receipt [run-id result claim scan]
  (array-map
   :schema :wm/run-era-deposit-receipt-v1
   :row :RE3
   :check :contract-pin
   :run-id run-id
   :produced-by "checks/contract_authority_current.clj --deposit"
   :deterministic
   (str "No wall-clock field: the observation interval and the event claim's interval are "
        "omitted here on purpose, so this receipt is rewritten byte-identically on every "
        "deposit. That is what lets the deposit require it to be committed and unmodified, "
        "and lets the same deposit repeat as :already-present. The interval is still printed "
        "on stdout by an ordinary invocation.")
   :run-store scan
   :verdict-deposited (cond (not (:pass? result)) :red
                            (:recorded-authority-appears-in-store? scan) :green
                            :else :typed-absence)
   :live-derivation
   (array-map
    :read-from "the mathlib4 working tree at deposit time, NOT the tree the run was taken at"
    :assertion "the contract was generated from the current content of Holes.lean"
    :pass? (:pass? result)
    :failures (:failures result)
    :recorded-authority (:recorded-authority result)
    :holes-last-content-change (:holes-last-content-change result)
    :recorded-holes-blob (:recorded-holes-blob result)
    :current-holes-blob (:current-holes-blob result)
    :mathlib-head (:mathlib-head result)
    :holes-clean? (:holes-clean? result)
    :event-claim (dissoc claim :interval))
   :not-what-this-says
   (str "The live derivation above is a property of the mathlib4 tree at deposit time. It is "
        "recorded so the absence is legible, and it is NOT the deposited verdict.")))

(defn deposit-notes [run-id receipt]
  (let [d (:live-derivation receipt)
        scan (:run-store receipt)]
    (case (:verdict-deposited receipt)
      :typed-absence
      (str "runs/" run-id "/ records no contract identity to pin against: the store holds "
           (str/join ", " (:holds scan))
           ", and a search of every one of those files for the contract's recorded authority "
           (:recorded-authority-sought scan) " finds it in none of them. What the store does "
           "name is "
           (str/join ", " (sort (distinct (filter #(str/starts-with? % ":")
                                                  (mapcat val (:mentions-of-contract scan))))))
           ", the tick's own contract tag, which is not the mathlib4 holes-contract source sha "
           "(the full scan, including the non-keyword spellings, is in this receipt). "
           "This check has no as-of-sha mode, so the pin AT this run cannot be reconstructed. "
           "The pin observed at deposit time is "
           (if (:pass? d) "green" (str "RED " (pr-str (:failures d))))
           " at authority " (:recorded-authority d)
           "; that is a statement about the tree at deposit time, recorded in the artifact this "
           "row points at, and not deposited as this run's verdict.")
      :red
      (str "the contract pin FAILS at deposit time: " (pr-str (:failures d))
           " (recorded authority " (:recorded-authority d)
           ", last content change " (:holes-last-content-change d) ")")
      :green
      (str "the run store names the contract authority " (:recorded-authority d)
           " and the pin holds against it. Named in "
           (str/join ", " (:recorded-authority-appears-in scan))
           " -- the ledger row's basis is which of those a reader judges to be a "
           "RECORD of the run rather than narrative about it."))))

(defn deposit! [run-id result claim]
  (let [scan (run-store-scan repo-root (str "holes/labs/wm-contract/runs/" run-id)
                             (:recorded-authority result))
        receipt (deposit-receipt run-id result claim scan)
        rel (str "holes/labs/wm-contract/runs/RE3-check-deposits/contract-pin-" run-id ".edn")
        path (io/file repo-root rel)]
    (io/make-parents path)
    (spit path (with-out-str (pp/pprint receipt)))
    (println "contract-authority-current --deposit: receipt" rel)
    (let [{:keys [exit out err]}
          (process/shell {:dir repo-root :out :string :err :string :continue true}
                         "bb" "holes/labs/wm-contract/run_era_ledger.bb" "--deposit"
                         "--run-id" run-id
                         "--check-id" ":contract-pin"
                         "--verdict" (str (:verdict-deposited receipt))
                         "--artifact" rel
                         "--author" "checks/contract_authority_current.clj --deposit"
                         "--deposited-by" "RE3 -- wire the existing checks to deposit ledger rows"
                         "--notes" (deposit-notes run-id receipt))]
      (print out) (print err) (flush)
      (when-not (zero? exit)
        (println (format "contract-authority-current --deposit: the ledger refused the row (exit %d)" exit))
        (println "  if the refusal is artifact-untracked or artifact-dirty, commit" rel "and re-run"))
      exit)))

(defn parse-args [args]
  (loop [xs args out {:negative? false
                      :writer-fence-id (System/getenv "FUTON_WRITER_FENCE_ID")
                      :writer-fence-evidence (System/getenv "FUTON_WRITER_FENCE_EVIDENCE")}]
    (if-let [x (first xs)]
      (case x
        "--negative-control" (recur (rest xs) (assoc out :negative? true))
        "--deposit" (if-let [run-id (second xs)]
                      (recur (nnext xs) (assoc out :deposit run-id))
                      (throw (ex-info "--deposit requires a run-id" {})))
        "--writer-fence" (if-let [id (second xs)]
                           (recur (nnext xs) (assoc out :writer-fence-id id))
                           (throw (ex-info "--writer-fence requires an id" {})))
        "--writer-fence-evidence" (if-let [path (second xs)]
                                    (recur (nnext xs) (assoc out :writer-fence-evidence path))
                                    (throw (ex-info "--writer-fence-evidence requires a path" {})))
        (throw (ex-info "unknown argument" {:argument x})))
      out)))

(defn event-claim [result writer-fence-id writer-fence-evidence]
  (let [moved? (some #{:repository-basis-moved} (:failures result))]
    (fence/assess (:observation-interval result) moved?
                  writer-fence-id writer-fence-evidence)))

(defn -main [& args]
  (let [{:keys [negative? writer-fence-id writer-fence-evidence deposit]} (parse-args args)
        state (current-state)
        tested (if negative?
                 (assoc state
                        :recorded-authority (apply str (repeat 40 "0"))
                        :recorded-holes-blob (apply str (repeat 40 "0")))
                 state)
        result (assess tested)
        claim (event-claim result writer-fence-id writer-fence-evidence)
        success? (if negative? (not (:pass? result)) (:pass? result))]
    (when deposit
      (when negative?
        (throw (ex-info "--deposit and --negative-control together would deposit a mutated verdict" {})))
      (System/exit (deposit! deposit result claim)))
    (println "contract-authority-current:"
             (cond
               negative? (if success? "negative-control PASS" "mutation slipped")
               (not success?) "FAIL"
               (= true (:event-free? claim)) (str "PASS (FENCE-CONDITIONAL " writer-fence-id ")")
               :else "PASS-CONTENT-ONLY (event-free unverified)")
             "assertion=the contract was generated from the current content of Holes.lean"
             (pr-str (assoc result :negative-control negative? :event-claim claim))
             "exit-convention=0-pass/1-fail")
    (System/exit (if success? 0 1))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
