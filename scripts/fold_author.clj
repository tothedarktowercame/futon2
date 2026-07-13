(ns fold-author
  "G1 inline fold author: build the exact fold prompt for a mission, accept an
   inhabiting agent's EDN construction, compute coverage delta-G, assemble the
   armed escrow deposit, validate it with the loader pins, and optionally write
   it to the real fold-turn corpus."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [futon2.aif.fold-escrow :as esc]
            [futon2.aif.fold-eval :as fe]
            [futon2.aif.fold-llm :as fl]))

(def default-mission-triples-dir
  "/home/joe/code/futon6/data/mission-triples")

(def default-flexiarg-dir
  "/home/joe/code/futon3/library")

(defn- usage []
  (str "Usage:\n"
       "  clojure -M:fold-author <mission-id> --answer <answer.edn|-> --write \\\n"
       "    --operator joe --word <verbatim arming word> --agent <agent-id> \\\n"
       "    --model <model-id> --edge <edge-id> --at <iso8601>\n\n"
       "Options:\n"
       "  --print-prompt               Print the exact prompt and exit.\n"
       "  --write                      Write ft-*.edn; absent means dry-run.\n"
       "  --force                      Allow authoring when a matching deposit exists.\n"
       "  --deposit-dir <dir>          Default: " esc/default-deposit-dir "\n"
       "  --mission-triples-dir <dir>  Default: " default-mission-triples-dir "\n"
       "  --flexiarg-dir <dir>         Default: " default-flexiarg-dir "\n"))

(defn- die!
  [msg]
  (binding [*out* *err*]
    (println msg)
    (println)
    (println (usage)))
  (System/exit 1))

(defn- parse-args
  [args]
  (loop [xs args
         opts {:deposit-dir esc/default-deposit-dir
               :mission-triples-dir default-mission-triples-dir
               :flexiarg-dir default-flexiarg-dir}]
    (if-let [x (first xs)]
      (case x
        "--answer" (recur (nnext xs) (assoc opts :answer-path (second xs)))
        "--operator" (recur (nnext xs) (assoc opts :operator (second xs)))
        "--word" (recur (nnext xs) (assoc opts :word (second xs)))
        "--agent" (recur (nnext xs) (assoc opts :agent (second xs)))
        "--model" (recur (nnext xs) (assoc opts :model (second xs)))
        "--edge" (recur (nnext xs) (assoc opts :edge (second xs)))
        "--at" (recur (nnext xs) (assoc opts :at (second xs)))
        "--deposit-dir" (recur (nnext xs) (assoc opts :deposit-dir (second xs)))
        "--mission-triples-dir" (recur (nnext xs) (assoc opts :mission-triples-dir (second xs)))
        "--flexiarg-dir" (recur (nnext xs) (assoc opts :flexiarg-dir (second xs)))
        "--write" (recur (next xs) (assoc opts :write? true))
        "--force" (recur (next xs) (assoc opts :force? true))
        "--print-prompt" (recur (next xs) (assoc opts :print-prompt? true))
        "--help" (do (println (usage)) (System/exit 0))
        (if (:mission opts)
          (die! (str "unexpected extra argument: " x))
          (recur (next xs) (assoc opts :mission x))))
      opts)))

(defn- mission-stem
  [x]
  (-> (str x)
      (str/replace #".*/" "")
      (str/replace #"^M-" "")
      (str/replace #"\.edn$" "")
      (str/lower-case)))

(defn- deposit-for?
  [deposit-mission target]
  (let [d (mission-stem deposit-mission)
        t (mission-stem target)]
    (or (str/includes? d t) (str/includes? t d))))

(defn- matching-mission-file
  [dir mission]
  (let [target (mission-stem mission)
        files (->> (.listFiles (io/file dir))
                   (filter #(re-matches #"M-.*\.edn" (.getName ^java.io.File %)))
                   (filter #(= target (mission-stem (.getName ^java.io.File %))))
                   (sort-by #(.getName ^java.io.File %))
                   vec)]
    (case (count files)
      0 (die! (str "no mission triple found for " mission " in " dir))
      1 (first files)
      (die! (str "ambiguous mission triple for " mission ": "
                 (pr-str (mapv #(.getName ^java.io.File %) files)))))))

(defn- read-edn-file
  [file]
  (edn/read-string (slurp file)))

(defn- pattern-id-from-cite
  [{:keys [ident ref]}]
  (if (and (string? ref)
           (str/starts-with? ref "futon3/library/")
           (str/ends-with? ref ".flexiarg"))
    (subs ref
          (count "futon3/library/")
          (- (count ref) (count ".flexiarg")))
    ident))

(defn- ordered-pattern-ids
  [mission-triple]
  (->> (get-in mission-triple [:cascade :pattern-cites])
       (map pattern-id-from-cite)
       (filter #(and (string? %) (not (str/blank? %))))
       distinct
       vec))

(defn- prose-for
  [flexiarg-dir pattern-id]
  (slurp (io/file flexiarg-dir (str pattern-id ".flexiarg"))))

(defn- psi
  [mission-triple]
  (str "WANT: " (get-in mission-triple [:hole :want])
       ". HAVE: " (get-in mission-triple [:hole :have]) "."))

(defn prompt-inputs
  "Resolve mission id to the exact fold-prompt inputs used by the escrow pins."
  [mission opts]
  (let [file (matching-mission-file (:mission-triples-dir opts) mission)
        triple (read-edn-file file)
        pids (ordered-pattern-ids triple)
        _ (when-not (seq pids)
            (die! (str "mission has no usable :cascade :pattern-cites: " (.getPath file))))
        proses (into (sorted-map)
                     (for [p pids]
                       [p (prose-for (:flexiarg-dir opts) p)]))
        circumstance {:mission (:mission triple)
                      :psi (psi triple)}
        prompt (fl/fold-prompt pids circumstance proses)]
    {:mission-file (.getPath file)
     :mission-triple triple
     :pattern-ids pids
     :proses proses
     :circumstance circumstance
     :prompt prompt}))

(defn- next-fold-turn-id
  [deposit-dir mission]
  (let [slug (mission-stem mission)
        existing (->> (.listFiles (io/file deposit-dir))
                      (map #(.getName ^java.io.File %))
                      (keep (fn [nm]
                              (when-let [[_ n] (re-matches
                                                (re-pattern (str "ft-" (java.util.regex.Pattern/quote slug)
                                                                 "-(\\d{3})\\.edn"))
                                                nm)]
                                (parse-long n)))))
        n (inc (if (seq existing) (apply max existing) 0))]
    (format "ft-%s-%03d" slug n)))

(defn- prose-shas
  [proses]
  (into (sorted-map)
        (map (fn [[p prose]] [p (esc/prompt-sha prose)]))
        proses))

(defn- pattern-cites
  [mission-triple pids]
  (let [by-pid (into {}
                     (keep (fn [cite]
                             (when-let [pid (pattern-id-from-cite cite)]
                               [pid cite])))
                     (get-in mission-triple [:cascade :pattern-cites]))]
    (mapv (fn [order pid]
            (-> (get by-pid pid {:ident pid})
                (assoc :order order
                       :ident pid
                       :rule "fold-turn/pattern-cite")))
          (range)
          pids)))

(defn- read-answer
  [path]
  (when-not path
    (die! "--answer is required unless --print-prompt is used"))
  (let [s (if (= "-" path) (slurp *in*) (slurp path))]
    (or (fl/parse-construction s)
        (die! (str "answer is not readable EDN construction: " path)))))

(defn assemble-deposit
  "Build a complete fold-turn deposit map. Does not write."
  [inputs answer opts]
  (let [mission-triple (:mission-triple inputs)
        fold-turn-id (next-fold-turn-id (:deposit-dir opts)
                                        (get-in inputs [:circumstance :mission]))
        wiring (fl/construction->wiring answer)
        coverage-score-delta (fe/coverage-score-delta wiring)]
    (when-not (number? coverage-score-delta)
      (die! "answer produced nil delta-G; deposits with no boxes are rejected"))
    {:fold-turn/id fold-turn-id
     :mission (get-in inputs [:circumstance :mission])
     :deposit-contract :v2
     :hole (assoc (:hole mission-triple)
                  :confidence :authored
                  :rule "fold-turn/hole-from-mission-triple")
     :cascade {:psi (get-in inputs [:circumstance :psi])
               :psi-sha256 (esc/prompt-sha (get-in inputs [:circumstance :psi]))
               :pattern-ids (:pattern-ids inputs)
               :constructor {:entry "fold-author"
                             :source (:mission-file inputs)
                             :cascade-source :mission-triple-pattern-cites}
               :pattern-cites (pattern-cites mission-triple (:pattern-ids inputs))}
     :prompt {:sha256 (esc/prompt-sha (:prompt inputs))
              :prose-sha256 (prose-shas (:proses inputs))
              :prose-source "verbatim (slurp \"/home/joe/code/futon3/library/<pattern-id>.flexiarg\")"}
     :turn {:agent (:agent opts)
            :model (:model opts)
            :edge (:edge opts)
            :at (:at opts)
            :answer answer}
     :arming {:operator (:operator opts)
              :word (:word opts)
              :at (:at opts)
              :scope :one-fold
              :fold-turn/id fold-turn-id}
     :eval {:coverage-score-delta coverage-score-delta
            :g-grain :coverage}
     :wiring wiring
     :arrow-candidate {:have (get-in mission-triple [:hole :have])
                       :want (get-in mission-triple [:hole :want])
                       :format :sorry
                       :escrow "do-not-write-meme.db"}}))

(defn- validate-metadata!
  [opts]
  (doseq [k [:operator :word :agent :model :edge :at]]
    (when (str/blank? (str (get opts k)))
      (die! (str "missing required option " (name k))))))

(defn- write-edn!
  [file x]
  (with-open [w (io/writer file)]
    (binding [*out* w]
      (pprint/pprint x)
      (println))))

(defn- matching-deposit-exists?
  [deposit-dir mission]
  (some #(deposit-for? (:mission %) mission)
        (:deposits (esc/load-deposits deposit-dir))))

(defn write-deposit!
  [deposit opts]
  (let [file (io/file (:deposit-dir opts) (str (:fold-turn/id deposit) ".edn"))]
    (esc/validate-deposit file deposit)
    (when (:write? opts)
      (.mkdirs (.getParentFile file))
      (write-edn! file deposit))
    {:file (.getPath file)
     :deposit deposit
     :written? (boolean (:write? opts))}))

(defn -main
  [& args]
  (let [opts (parse-args args)
        mission (:mission opts)]
    (when-not mission (die! "missing mission id"))
    (let [inputs (prompt-inputs mission opts)]
      (if (:print-prompt? opts)
        (println (:prompt inputs))
        (do
          (validate-metadata! opts)
          (when (and (not (:force? opts))
                     (matching-deposit-exists? (:deposit-dir opts) mission))
            (die! (str "matching deposit already exists for " mission " (use --force to override)")))
          (let [answer (read-answer (:answer-path opts))
                result (write-deposit! (assemble-deposit inputs answer opts) opts)
                deposit (:deposit result)]
            (println (if (:written? result) "WROTE" "DRY-RUN") (:file result))
            (println "fold-turn/id" (:fold-turn/id deposit))
            (println "mission" (:mission deposit))
            (println "patterns" (pr-str (get-in deposit [:cascade :pattern-ids])))
            (println "prompt-sha" (get-in deposit [:prompt :sha256]))
            (println "coverage-score-delta" (get-in deposit [:eval :coverage-score-delta]))
            (println "boxes" (count (get-in deposit [:turn :answer :boxes]))
                     "policy-holes" (count (get-in deposit [:turn :answer :policy-holes])))))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
