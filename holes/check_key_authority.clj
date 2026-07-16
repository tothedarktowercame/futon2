;; KEY AUTHORITY — does a pattern's declared parameter ever reach behaviour?
;;
;; Requested by oxf-claude-2 (2026-07-16) after re-running the authority gate and
;; finding its verdict wrong in BOTH directions: pheromone-tuner is a live
;; actuator the sweep {0.1,0.5,1.0} was too narrow to see (moves 5/5 seeds at
;; lambda=20), while cargo-return and hunger-coupling are byte-identical even at
;; lambda=100 — not weak, INERT. Their ask, verbatim: "the overlay should flag
;; 'written but never read' alongside 'these keys contend'. That check would have
;; caught this gate error, the cyberants :config error, and the C-vector error —
;; three instances of one bug."
;;
;; This is the KEY-level authority gate, one level below the pattern-level one.
;; The pattern gate asks "does this pattern move the ant?" — expensive, needs
;; seeds and ticks, and answers with a number that a bad lambda sweep can fake.
;; This asks "is this key ever READ?" — static, seconds, and no sweep to get
;; wrong. A key nothing reads cannot move the ant at any lambda, and no amount
;; of simulation is needed to know it.
;;
;; WHAT IT CAN AND CANNOT PROVE — the asymmetry is the whole design:
;;   ABSENCE IS DECISIVE. If the key's name appears NOWHERE in the ant source,
;;   nothing reads it. That is a proof of inertness, not a hint.
;;   PRESENCE IS ONLY SUGGESTIVE. The name appearing somewhere means it is
;;   mentioned, not that it is read on the live path. cargo-return is the exact
;;   trap: oxf-claude-2 found its keys ARE read, and it is STILL inert, because
;;   the mode machine decides :return before the penalty applies. So a PASS here
;;   means "not provably dead", never "live".
;; Reporting it any other way would rebuild the false-confidence bug this check
;; exists to catch.
;;
;; Run: cd ~/code/futon2/holes && bb check_key_authority.clj

(require '[clojure.edn :as edn] '[clojure.string :as str] '[clojure.java.io :as io])

(def ANTS-LIB "/home/joe/code/futon3/library/ants")
(def SRC-DIRS ["/home/joe/code/futon2/src/ants" "/home/joe/code/futon2/src/futon2/aif"])

(defn read-delta [f]
  (let [txt (slurp f)]
    (when-let [i (str/index-of txt "@aif-delta")]
      (let [after (subs txt (+ i (count "@aif-delta")))
            cleaned (->> (str/split-lines after) (map #(str/replace % #";;.*$" "")) (str/join "\n"))]
        (try (edn/read-string cleaned) (catch Exception _ nil))))))

(defn leaf-paths [m prefix]
  (mapcat (fn [[k v]] (if (map? v) (leaf-paths v (conj prefix k)) [[(conj prefix k) v]])) m))

(def patterns
  (->> (.listFiles (io/file ANTS-LIB))
       (filter #(str/ends-with? (.getName %) ".flexiarg"))
       (map (fn [f] {:id (str/replace (.getName f) ".flexiarg" "") :delta (read-delta f)}))
       (sort-by :id)))

;; the ant source, as one blob to search
(def src-files
  (->> SRC-DIRS
       (mapcat (fn [d] (when (.exists (io/file d)) (file-seq (io/file d)))))
       (filter #(and (.isFile %) (str/ends-with? (.getName %) ".clj")))))
(def src-blob (str/join "\n" (map slurp src-files)))

(println "=== KEY AUTHORITY — are patterns' declared parameters ever read?")
(println "    patterns:" (count patterns) "| ant source files:" (count src-files))
(println "    NB absence is decisive (nothing reads it); presence is NOT proof of life.\n")

(defn reads? [k]
  ;; the leaf keyword's name appearing anywhere in the source. Deliberately the
  ;; WIDEST possible reading — destructuring, (get m :k), (:k m), get-in — so a
  ;; DEAD verdict is beyond argument.
  (str/includes? src-blob (name k)))

(def rows
  (for [{:keys [id delta]} patterns
        [path v] (leaf-paths (or delta {}) [])]
    {:ant id :path path :val v :leaf (last path) :read? (reads? (last path))}))

(println (format "%-28s %-38s %8s  %s" "pattern" "key path" "value" "leaf name in ant source?"))
(doseq [{:keys [ant path val leaf read?]} rows]
  (println (format "%-28s %-38s %8s  %s" ant (pr-str path) val
                   (if read? "found" "*** ABSENT — provably unread ***"))))

(def dead (remove :read? rows))
(def live-ish (filter :read? rows))

(println (format "\n--- %d of %d declared keys are PROVABLY UNREAD" (count dead) (count rows)))
(doseq [{:keys [ant path]} dead] (println (format "    %-28s %s" ant (pr-str path))))
(when (empty? dead) (println "    (none — every declared key's name occurs in the source)"))

(println (format "\n--- %d keys are mentioned in the source. NOT a pass:" (count live-ish)))
(println "    oxf-claude-2 re-ran the gate at lambda=100 and found cargo-return and")
(println "    hunger-coupling BYTE-IDENTICAL to baseline — inert despite their keys")
(println "    being read, because the mode machine decides :return first. Mention is")
(println "    not authority; only the gate settles it, and only with a wide sweep.")

;; ---------------------------------------------------------------------------
;; THE CHECK THAT ACTUALLY BIT — flexiarg @aif-delta vs cyber.clj's SHADOW COPY.
;;
;; The name-presence check above found 0 of 18. It is too weak for this corpus
;; and I am leaving it in, reporting its own failure, rather than deleting the
;; evidence that it did not work. Every key IS read (affect.clj destructures them
;; with `{:keys [...]}` defaults), so oxf-claude-2's inertness is NOT
;; "never read" — it is READ BUT MASKED (the mode machine decides :return before
;; cargo-return's penalty applies). Those are two different bugs, and only the
;; first is statically catchable. Their "three instances of one bug" is at least
;; two: the cyberants :config error was genuinely never-read; cargo-return's
;; inertness is downstream masking and no static check can see it — only the
;; gate can, and only with a wide enough sweep.
;;
;; But looking for the read found something else. cyber.clj:72 carries
;; `default-deltas` — a SECOND, hard-coded copy of every pattern's delta, used
;; when @aif-delta is absent: `(or (parse-aif-delta content) (get default-deltas
;; pattern-key {}))`. The flexiarg wins when parseable, which is WR-2-correct by
;; design. The shadow copy has nevertheless DRIFTED — by omission, on 4 of 5.
;; ---------------------------------------------------------------------------
(def shadow
  ;; transcribed from src/ants/cyber.clj:72-97
  {"hunger-precision-coupling" {:precision {:need-gain 0.7 :dhdt-gain 0.9} :efe {:lambda {:survival 1.4}}}
   "cargo-return-discipline"   {:modes {:cargo-high 0.55} :precision {:tau-cap 1.2} :actions {:return {:cargo-thresh 0.1}}}
   "pheromone-trail-tuner"     {:precision {:pher-scale 1.3 :trail-grad-scale 1.2} :efe {:lambda {:info 0.5}}}
   "white-space-scout"         {:precision {:food-scale 1.8 :novelty-scale 1.4} :efe {:lambda {:info 0.6 :ambiguity 0.4}}}
   "baseline-cyber-ant"        {}})

(println "\n--- flexiarg @aif-delta vs cyber.clj default-deltas (the fallback)")
(def drift
  (for [{:keys [id delta]} patterns
        :let [fl (into {} (leaf-paths (or delta {}) []))
              sh (into {} (leaf-paths (get shadow id {}) []))
              missing (remove (set (keys sh)) (keys fl))]
        :when (seq missing)]
    {:id id :missing missing :parsed? (some? delta)}))
(doseq [{:keys [id missing]} drift]
  (doseq [k missing]
    (println (format "    %-28s DECLARED, absent from fallback: %s" id (pr-str k)))))
(println (format "    -> %d of %d patterns' fallbacks have drifted from the pattern." (count drift) (count patterns)))
(println "    All 5 parse OK today, so this is LATENT, not live: the flexiarg wins.")
(println "    It fires only if parse-aif-delta ever returns nil — and then the ant")
(println "    silently runs an OLDER, SMALLER delta and nothing says so. That is")
(println "    WR-2's unenforced 'local copies should not diverge', in the ants.")
(println "    It bites the tau family specifically: :tau-survival-gain and")
(println "    :tau-floor are BOTH absent from the fallback, so a silent fallback")
(println "    drops two of the three tau writers and the contention changes shape.")

;; ---- what this means for the tau contention --------------------------------
(println "\n--- consequence for the tau contention (the overlay's headline finding)")
(def tau-leaves #{:need-gain :dhdt-gain :tau-survival-gain :tau-cap :tau-floor})
(def tau-rows (filter #(tau-leaves (:leaf %)) rows))
(doseq [{:keys [ant path read?]} tau-rows]
  (println (format "    %-28s %-38s %s" ant (pr-str path) (if read? "mentioned" "PROVABLY UNREAD"))))
(println "    overlay says: 3 patterns contend on :tau, invisible to merge-deep.")
(println "    oxf-claude-2 says: 2 of those 3 (cargo-return, hunger-coupling) are")
(println "    INERT at lambda=100. So the contention is real on paper and, today,")
(println "    between two patterns that cannot act and one that can. Canonicalising")
(println "    the keys stays worth doing — it is what makes the contention DECIDABLE")
(println "    when they do act — but it must not be reported as a live conflict now.")
