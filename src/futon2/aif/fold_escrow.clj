(ns futon2.aif.fold-escrow
  "The fold-turn ESCROW — deposits and replay lookup for `futon2.aif.fold-llm`
   (E-live-loop-2 step 2d; design: futon2/holes/E-live-loop-1-s2-escrow-design.md).

   A deposit is one recorded LLM fold-turn: `ft-*.edn` in
   `futon6/data/fold-turns/`, an :authored-tier record of the sortie-11 triple
   family. The constructive step is paid ONCE, out-of-process, under operator
   arming; the live path replays it as a hash-gated table lookup — never an LLM
   call (fold-llm's INCIDENT-SAFE clause, unchanged).

   Three pins, enforced here (all K2-class, reject-loudly):
     1. REPLAY-VALIDITY — a deposit binds to the sha-256 of the EXACT
        `fold-prompt` string it answered; replay fires only on hash match, so
        any drift in cascade/circumstance/prose ⇒ nil ⇒ the fold abstains
        exactly as today.
     2. NO ARMING, NO DEPOSIT — `:arming` (operator, verbatim word, timestamp,
        scope) is a required field of the record, not ambient context.
     3. ΔG RECOMPUTABLE — the stored `:eval :delta-g` must equal
        `fold-eval/coverage-delta-g` recomputed fresh from the stored answer;
        a drifted deposit is rejected, never silently served
        (silent-wrong-results are this stack's known hazard class).

   `load-deposits` REJECTS invalid files loudly (stderr + `:rejected` entries
   with file + reason) but does not poison the lane: valid deposits still
   serve. Missing dir ⇒ no deposits ⇒ the fold abstains — honest by default."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [futon2.aif.fold-eval :as fe]
            [futon2.aif.fold-llm :as fl])
  (:import [java.security MessageDigest]))

(def default-deposit-dir
  "Sibling of futon6/data/mission-triples (the sortie-11 corpus); NOT meme.db,
   NOT a substrate-2 write (design §B)."
  "/home/joe/code/futon6/data/fold-turns")

(defn prompt-sha
  "Lowercase hex sha-256 of the exact prompt string (pin 1's binding)."
  [^String s]
  (let [d (.digest (MessageDigest/getInstance "SHA-256") (.getBytes s "UTF-8"))]
    (apply str (map #(format "%02x" %) d))))

(defn- reject!
  [file reason msg]
  (throw (ex-info (str "fold-turn REJECTED [" (name reason) "] " file ": " msg)
                  {:file (str file) :reason reason})))

(defn- present-string? [x] (and (string? x) (not (str/blank? x))))

(defn validate-deposit
  "PURE given the record: the ft-record contract. Returns the deposit map or
   throws ex-info {:file :reason} (reject-loudly). `file` is for the message."
  [file d]
  (when-not (map? d)
    (reject! file :not-a-map (pr-str (type d))))
  (when-not (present-string? (:fold-turn/id d))
    (reject! file :missing-id (pr-str (:fold-turn/id d))))
  (when-not (present-string? (:mission d))
    (reject! file :missing-mission (pr-str (:mission d))))
  (let [pids (get-in d [:cascade :pattern-ids])]
    (when-not (and (sequential? pids) (seq pids) (every? present-string? pids))
      (reject! file :bad-cascade (str ":cascade :pattern-ids must be a non-empty vector of strings, got " (pr-str pids)))))
  (let [sha (get-in d [:prompt :sha256])]
    (when-not (and (string? sha) (re-matches #"[0-9a-f]{64}" sha))
      (reject! file :bad-prompt-sha (pr-str sha))))
  (let [{:keys [operator word at]} (:arming d)]
    (when-not (and (present-string? operator) (present-string? word)
                   (present-string? at) (contains? (:arming d) :scope))
      (reject! file :missing-arming
               "pin 2: :arming needs :operator, verbatim :word, :at, :scope — no arming, no deposit")))
  (let [answer (get-in d [:turn :answer])
        boxes  (:boxes answer)]
    (when-not (map? answer)
      (reject! file :missing-answer (pr-str (:turn d))))
    (when-not (and (sequential? boxes) (seq boxes))
      (reject! file :empty-construction
               "a deposit with no boxes replays a nothing; the honest nothing is NO deposit"))
    (let [stored     (get-in d [:eval :delta-g])
          recomputed (fe/coverage-delta-g (fl/construction->wiring answer))]
      (when-not (number? stored)
        (reject! file :missing-delta-g (pr-str (:eval d))))
      (when (or (nil? recomputed)
                (> (Math/abs (- (double recomputed) (double stored))) 1e-9))
        (reject! file :delta-g-mismatch
                 (str "pin 3: stored " stored " vs recomputed " recomputed)))))
  d)

(defn load-deposit
  "One ft-*.edn file → validated deposit, or throws (reject-loudly)."
  [file]
  (let [d (try (edn/read-string (slurp file))
               (catch Exception e
                 (reject! file :unreadable-edn (ex-message e))))]
    (validate-deposit file d)))

(defn load-deposits
  "Dir → {:deposits [valid…] :rejected [{:file :reason :message}…]}.
   Rejections go to stderr (loud) AND the return value (auditable); valid
   deposits still serve. Missing dir ⇒ {:deposits [] :rejected []}."
  ([] (load-deposits default-deposit-dir))
  ([dir]
   (let [files (->> (.listFiles (io/file dir))
                    (filter #(re-matches #"ft-.*\.edn" (.getName ^java.io.File %)))
                    (sort-by #(.getName ^java.io.File %)))]
     (reduce (fn [acc f]
               (try (update acc :deposits conj (load-deposit f))
                    (catch clojure.lang.ExceptionInfo e
                      (binding [*out* *err*]
                        (println (ex-message e)))
                      (update acc :rejected conj
                              (assoc (ex-data e) :message (ex-message e))))))
             {:deposits [] :rejected []}
             files))))

(defn escrow-turn-fn
  "Deposits → a `:turn-fn` for `fold-llm/llm-fold`: sha-256 the incoming prompt,
   return the matching deposit's recorded answer, else nil (⇒ the fold abstains,
   byte-identical to today). A hash-gated table lookup — no LLM call, no I/O."
  [deposits]
  (let [by-sha (into {} (map (juxt #(get-in % [:prompt :sha256]) identity)) deposits)]
    (fn [prompt]
      (some-> (by-sha (prompt-sha prompt))
              (get-in [:turn :answer])))))
