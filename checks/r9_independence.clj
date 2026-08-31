#!/usr/bin/env bb
(ns checks.r9-independence
  (:require [babashka.process :refer [shell]]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def corpus-sha "6c288174")
(def corpus-path "vetting/OBLIGATIONS.md")
(def closed-ids ["O1" "O2" "O3" "O5" "O6" "O7" "O8" "O9"
                 "O14" "O15" "O16" "O17" "O20"])
(def row-text-ids #{"O7" "O14" "O15"})

(defn independence-verdict [declared producer decide?]
  (if (nil? declared) :unknown
      (if (decide? producer (:producing-part declared)) :self :independent)))

(defn checker-sound? [decide? claims]
  (every? (fn [{:keys [producer producing-part] :as claim}]
            (let [v (independence-verdict claim producer decide?)
                  inside? (contains? producing-part producer)]
              (and (or (not inside?) (not= v :independent))
                   (or (not= v :independent) (not inside?)))))
          claims))

(defn recorded-sound? [rows]
  (every? (fn [{:keys [producer declared-part verdict]}]
            (let [inside? (contains? (set declared-part) producer)]
              (and (or (not inside?) (not= verdict :independent))
                   (or (not= verdict :independent) (not inside?)))))
          rows))

(defn- sections [text]
  (->> (str/split text #"(?m)(?=^## O)")
       (keep (fn [s]
               (when-let [[_ id] (re-find #"(?m)^## (O[^ ]*)" s)] [id s])))
       (into {})))

(defn- first-status [s]
  (some-> (re-find #"\*\*Status:\s*([^*]+)\*\*" s) second str/trim))

(defn- status-kind [status]
  (cond
    (nil? status) :unmarked
    (str/starts-with? (str/lower-case status) "fixed") :fixed
    (str/starts-with? (str/lower-case status) "open") :open
    :else :other))

(def attribution-pattern
  #"(?i)\b(author|reviewer|codex-[0-9]+|zai|closed by|fixed by)\b")

(defn- attribution [text]
  (let [tokens (->> (re-seq attribution-pattern text)
                    (map (comp str/lower-case second)) distinct vec)]
    {:mentioned? (boolean (seq tokens)) :tokens tokens}))

(defn- declared-row [id]
  (let [named? (row-text-ids id)
        producer (case id "O7" "codex-1" "O14" "codex-1" "O15" "zai" "author")]
    {:row id
     :declaration-source (if named? {:row-text id} :paper-sentence)
     :producer producer
     ;; P-R9:66-68 declares commissioned agents inside the author's part.
     :declared-part (if named? ["author" "codex-1" "codex-7" "zai"] ["author"])
     :verdict (independence-verdict {:producing-part
                                     (set (if named? ["author" "codex-1" "codex-7" "zai"] ["author"]))}
                                    producer (fn [p s] (contains? s p)))}))

(defn- ledger-row [id]
  {:row id :declaration-source :paper-sentence :producer "unknown"
   :declared-part [] :verdict :unknown})

(defn- lean-source [ledger declared]
  (letfn [(src [x] (if (= x :paper-sentence) ".paperSentence"
                       (str ".rowText " (pr-str (:row-text x)))))
          (verdict [v] (str "." (name v)))
          (strings [xs] (str "[" (str/join ", " (map pr-str xs)) "]"))
          (row [r] (str "  { row := " (pr-str (:row r))
                         ", declarationSource := " (src (:declaration-source r))
                         ", producer := " (pr-str (:producer r))
                         ", declaredPart := " (strings (:declared-part r))
                         ", verdict := " (verdict (:verdict r)) " }"))
          (table [xs] (str "[\n" (str/join ",\n" (map row xs)) "\n]"))]
    (str "import DarkTower.WarMachine.Holes\n\n"
         "open Set\nnamespace DarkTower.WarMachine.Holes.R9D2\n\n"
         "def wmVerdictsLedgerAloneFixture : VerdictTable := " (table ledger) "\n\n"
         "def wmVerdictsDeclaredFixture : VerdictTable := " (table declared) "\n\n"
         "theorem verdictConsultsChecker :\n"
         "    ∀ {Part : Type*} [DecidableEq Part] (claim : Claim Part) (w : Witness Part),\n"
         "      w.producer ∈ claim.producingPart →\n"
         "      ∃ decide? : Part → Set Part → Bool,\n"
         "        ¬ (∀ p S, decide? p S = true ↔ p ∈ S) ∧\n"
         "        independenceVerdict (some claim) w decide? = .independent := by\n"
         "  intro Part inst claim w hw\n"
         "  refine ⟨fun _ _ => false, ?_, ?_⟩\n"
         "  · intro h\n    have := (h w.producer claim.producingPart).mpr hw\n    simp at this\n"
         "  · simp [independenceVerdict]\n\n"
         "theorem recordedVerdictsSound : r9VerdictsSound wmVerdictsDeclaredFixture := by\n  simp [r9VerdictsSound, wmVerdictsDeclaredFixture, VerdictRow.inDeclaredPart]\n"
         "theorem perRowDeclarations : r9PerRowDeclarations wmVerdictsDeclaredFixture := by\n  simp [r9PerRowDeclarations, wmVerdictsDeclaredFixture]\n"
         "theorem twoRunCensus :\n"
         "    wmVerdictsLedgerAloneFixture.length = 13 ∧ wmVerdictsDeclaredFixture.length = 13 ∧\n"
         "    (∀ r ∈ wmVerdictsLedgerAloneFixture, r.verdict = .unknown) ∧\n"
         "    (∀ r ∈ wmVerdictsDeclaredFixture, r.verdict = .self) := by\n  simp [wmVerdictsLedgerAloneFixture, wmVerdictsDeclaredFixture]\n\n"
         "end DarkTower.WarMachine.Holes.R9D2\n")))

(defn run-check [{:keys [p4ng badges negative? negative-table]}]
  (let [p4ng (or p4ng "/home/joe/code/p4ng")
        badges (or badges "/home/joe/code/futon2/data/r18-badges.edn")
        badge-data (edn/read-string (slurp badges))
        badge-count (count (filter #(= :derived-from-FEP (:badge %))
                                   (vals (:quantities badge-data))))
        text (:out (shell {:out :string :err :string}
                          "git" "-C" p4ng "show" (str corpus-sha ":" corpus-path)))
        ss (sections text)
        statuses (into {} (map (fn [[id s]] [id (first-status s)]) ss))
        kinds (frequencies (map status-kind (vals statuses)))
        fixed (->> statuses (keep (fn [[id st]] (when (= :fixed (status-kind st)) id))) set)
        attrs (mapv (fn [id] (assoc (attribution (get ss id)) :row id)) closed-ids)
        ledger0 (mapv ledger-row closed-ids)
        declared0 (mapv declared-row closed-ids)
        ledger (if (= negative-table :ledger) (subvec ledger0 1) ledger0)
        declared (cond
                   (= negative-table :declared) (subvec declared0 1)
                   (= negative-table :sound) (assoc-in declared0 [0 :verdict] :independent)
                   :else declared0)
        decide? (if (and negative? (nil? negative-table))
                  ;; Semantic mutation: an inside producer is misclassified as
                  ;; independent while all row and corpus shapes remain valid.
                  (fn [_producer _producing-part] false)
                  (fn [producer producing-part] (contains? producing-part producer)))
        report {:corpus {:repository p4ng :sha corpus-sha :path corpus-path}
                :sections {:total (count ss) :fixed (:fixed kinds 0)
                           :open (:open kinds 0) :unmarked (:unmarked kinds 0)
                           :closed-ids closed-ids}
                :runs {:ledger-alone {:tally (frequencies (map :verdict ledger)) :rows ledger}
                       :declared {:tally (frequencies (map :verdict declared)) :rows declared}}
                :prose-attribution {:count (count (filter :mentioned? attrs)) :rows attrs}
                :badge-headline {:path badges :stated 4 :parsed badge-count
                                 :verdict :self}
                :checks {:expected-closed-ids? (= fixed (set closed-ids))
                         :ledger-row-set? (= (set closed-ids) (set (map :row ledger)))
                         :declared-row-set? (= (set closed-ids) (set (map :row declared)))
                         :recorded-sound? (recorded-sound? declared)
                         :per-row-sources? (every? #(= (boolean (row-text-ids (:row %)))
                                                        (map? (:declaration-source %))) declared)
                         :declared-sound? (checker-sound? decide?
                            (map #(hash-map :producer (:producer %) :producing-part (set (:declared-part %))) declared))}}]
    (when-not (every? true? (vals (:checks report)))
      (throw (ex-info "R9 checker failed closed" report)))
    {:report report :lean (lean-source ledger declared)}))

(defn -main [& args]
  (let [negative-table (cond (some #{"--negative-ledger"} args) :ledger
                             (some #{"--negative-declared"} args) :declared
                             (some #{"--negative-sound"} args) :sound)
        negative? (or (some #{"--negative"} args) negative-table)
        pair-args (remove #{"--negative" "--negative-ledger" "--negative-declared"
                            "--negative-sound"} args)
        opts (assoc (apply hash-map (mapcat (fn [[k v]] [(keyword (subs k 2)) v])
                                            (partition 2 pair-args)))
                    :negative? negative?
                    :negative-table negative-table)]
    (when-not (and (:report opts) (:lean opts))
      (throw (ex-info "usage: [--negative] --report FILE --lean FILE [--p4ng DIR]" {})))
    (try
      (let [{:keys [report lean]} (run-check opts)]
        (if negative?
          (do (println "r9-independence: FAIL negative independence mutation slipped exit-convention=0-pass/1-fail")
              (System/exit 2))
          (do (spit (:report opts) (str (pr-str report) "\n"))
              (spit (:lean opts) lean)
              (println (pr-str {:runs (get report :runs) :prose-attribution (get report :prose-attribution)}))
              (println "r9-independence: PASS exit-convention=0-pass/1-fail"))))
      (catch Exception e
        (if negative?
          (do (println (str "r9-independence: PASS negative independence mutation rejected"
                            " checks=" (pr-str (get-in (ex-data e) [:checks]))
                            " exit-convention=0-pass/1-fail"))
              (System/exit 0))
          (throw e))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
