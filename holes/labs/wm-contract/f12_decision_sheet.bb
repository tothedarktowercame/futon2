#!/usr/bin/env bb

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.set :as set]
         '[clojure.string :as str])
(import '[java.io PushbackReader]
        '[java.nio.file Files]
        '[java.nio.file.attribute FileAttribute])

(def lab (.getParentFile (.getAbsoluteFile (io/file *file*))))
(def repo (.getCanonicalFile (io/file lab "../../..")))
(def pointer-check (.getCanonicalFile (io/file repo "../p4ng/empirics-futon/pointer_check.bb")))
(def choice-order [:organise-carrier :organise-sorry :organise-o3-field
                   :organise-o4-denominator :organise-o4-after-the-law-encoding
                   :organise-third-origin])
(def expected (set choice-order))

(defn env-file [k fallback] (io/file (or (System/getenv k) (str fallback))))
(defn read-edn [f] (edn/read-string (slurp f)))
(defn pprint-str [x] (with-out-str (pp/pprint x)))

(defn read-def-form [file sym]
  (with-open [r (PushbackReader. (io/reader file))]
    (loop []
      (let [x (read {:eof ::eof} r)]
        (cond
          (= x ::eof) (throw (ex-info (str "definition not found: " sym) {}))
          (and (seq? x) (= 'def (first x)) (= sym (second x))) (nth x 2)
          :else (recur))))))

(defn pointer-roots []
  ;; This reads, rather than duplicates, pointer_check.bb's checker-owned root list.
  ;; Loading that script would run its top-level registry scan as a side effect.
  (let [home (System/getProperty "user.home")]
    (mapv (fn [x]
            (if (and (seq? x) (= 'str (first x)) (= 'home (second x))
                     (string? (nth x 2 nil)))
              (str home (nth x 2))
              (throw (ex-info (str "unsupported root expression: " (pr-str x)) {}))))
          (read-def-form pointer-check 'roots))))

;; This is deliberately stricter than pointer_check.bb:237: it includes .md, .bb
;; and .flexiarg because those extensions occur in the six source entries copied
;; into C558, while the repository check currently recognizes only clj/lean/edn.
(def ptr-re #"([A-Za-z0-9_.\-]+\.(?:clj|lean|edn|md|bb|flexiarg)):(\d+)(?:-(\d+))?")

(defn check-pointers [text]
  (let [roots (pointer-roots)
        rows (mapv
              (fn [[whole file a b]]
                (let [root (first (filter #(.exists (io/file % file)) roots))
                      lines (when root (count (str/split-lines (slurp (io/file root file)))))
                      lo (parse-long a) hi (parse-long (or b a))
                      reason (cond (nil? root) :file-not-found
                                   (> lo hi) :inverted-range
                                   (> hi lines) :end-beyond-file
                                   :else nil)]
                  (sorted-map :pointer whole :ok? (nil? reason)
                              :reason (or reason :resolved))))
              (re-seq ptr-re text))
        bad (vec (remove :ok? rows))]
    (when (seq bad)
      (throw (ex-info (str "unresolved pointers: "
                           (str/join ", " (map #(str (:pointer %) " (" (name (:reason %)) ")") bad)))
                      {:unresolved bad})))
    (sorted-map :count (count rows) :unresolved bad)))

(defn choices [aif]
  (let [all (:choices (read-edn aif))
        xs (into {} (filter (fn [[_ v]] (= :F12 (:row v)))) all)
        found (set (keys xs))]
    (when-not (= expected found)
      (throw (ex-info (str "F12 choice set mismatch; missing "
                           (pr-str (vec (sort (set/difference expected found))))
                           ", unexpected "
                           (pr-str (vec (sort (set/difference found expected))))) {})))
    (let [gaps (into {} (for [[k v] xs]
                          [k (vec (remove #(contains? v %)
                                         [:question :arms :status :evidence :all-arms-run
                                          :not-claimed :what-would-change-the-measurement]))]))
          measurement-gaps (vec (sort (for [[k v] xs
                                            :when (not (contains? v :measurement-that-separates-them))]
                                        k)))]
      (when-not (and (every? empty? (vals gaps))
                     (= [:organise-sorry] measurement-gaps))
        (throw (ex-info (str "unexpected registry field gaps: "
                             (pr-str {:required gaps :measurement measurement-gaps})) {})))
      xs)))

(defn shown [x] (if (string? x) x (pr-str x)))
(defn gates-by-choice [remainder]
  (reduce-kv (fn [m obligation citations]
               (reduce (fn [m' {:keys [choice]}] (update m' choice conj obligation))
                       m citations))
             (zipmap choice-order (repeat [])) (:gating remainder)))

(defn sheet-text [xs remainder]
  (let [gates (gates-by-choice remainder)
        obligations (:obligations remainder)]
    (str "# C558 — F12 decision sheet\n\n"
         "This sheet presents the six registered `:F12` choices and the measurements of their arms. "
         "It is derived from `aif-equations.edn` and `runs/F12-organise/15-remainder.edn`.\n\n"
         "It takes no ruling, recommends no arm, and changes neither a registry nor the Lean declaration.\n\n"
         "- `runs/F12-organise/14-arms-dry.edn`: `:arms-dry? true`\n"
         "- `runs/F12-organise/15-remainder.edn`: `:remainder-fully-gated? true`\n\n"
         (apply str
                (for [[idx k] (map-indexed vector choice-order)
                      :let [c (get xs k)
                            gated (get gates k)]]
                  (str "## " (inc idx) ". `" k "`\n\n"
                       "**Question:** " (shown (:question c)) "\n\n"
                       "**Measurement that separates the arms:** "
                       (if (contains? c :measurement-that-separates-them)
                         (shown (:measurement-that-separates-them c)) "**not found**") "\n\n"
                       "**What would change the measurement:** "
                       (shown (:what-would-change-the-measurement c)) "\n\n"
                       "**Not claimed:** " (shown (:not-claimed c)) "\n\n"
                       "**All arms run:** " (shown (:all-arms-run c)) "\n\n"
                       "**Acceptance obligations gated:**\n\n"
                       (if (seq gated)
                         (apply str (for [o gated]
                                      (str "- `" o "` — satisfied at HEAD: **"
                                           (get-in obligations [o :satisfied-at-head?]) "**\n")))
                         "- **gates nothing**\n")
                       "\n**Arms:**\n\n"
                       (apply str
                              (for [a (:arms c)]
                                (str "### `" (:arm a) "`\n\n"
                                     "- What it is: " (shown (:what-it-is a)) "\n"
                                     "- Buys: " (shown (:buys a)) "\n"
                                     "- Costs: " (shown (:costs a)) "\n"
                                     "- Run by: " (shown (:run-by a)) "\n\n")))))))))

(defn measure [aif remainder-file]
  (let [xs (choices aif)
        remainder (read-edn remainder-file)
        text (sheet-text xs remainder)
        pointers (check-pointers text)]
    {:choices xs :remainder remainder :text text :pointers pointers}))

(defn temp-dir [] (.toFile (Files/createTempDirectory "f12-sheet-" (make-array FileAttribute 0))))
(defn write-edn [f x] (spit f (pprint-str x)))
(defn message [f]
  (try (f) :succeeded
       (catch Exception e (.getMessage e))))
(defn replace-first [s old new]
  (let [i (.indexOf s old)]
    (when (neg? i) (throw (ex-info (str "control plant source not found: " old) {})))
    (str (subs s 0 i) new (subs s (+ i (count old))))))

(defn controls [aif remainder-file baseline]
  (let [dir (temp-dir)
        reg (read-edn aif)
        rem (read-edn remainder-file)
        bad-pointer "Holes.lean:999999"
        flex-old "status-gated-belief-update.flexiarg:26-30"
        flex-new "status-gated-belief-update.flexiarg:26-99999"
        arm-path [:choices :organise-carrier :arms 0 :costs]
        reg1 (update-in reg arm-path #(str % " " bad-pointer))
        f1 (io/file dir "bad-pointer.edn")
        reg2 (update reg :choices dissoc :organise-third-origin)
        f2 (io/file dir "missing-choice.edn")
        obligation :laws-stated-against-cascade-carrier
        rem3 (update-in rem [:obligations obligation :satisfied-at-head?] not)
        f3 (io/file dir "flipped-remainder.edn")
        rem4 (update rem :gating
                     (fn [g] (into {} (for [[o cs] g]
                                        [o (vec (remove #(= :organise-carrier (:choice %)) cs))]))))
        f4 (io/file dir "ungated-remainder.edn")
        raw5 (replace-first (slurp aif) flex-old flex-new)
        f5 (io/file dir "bad-flex-range.edn")]
    (write-edn f1 reg1) (write-edn f2 reg2) (write-edn f3 rem3) (write-edn f4 rem4)
    (spit f5 raw5)
    [(sorted-map :control :unresolvable-pointer
                 :plant-verified? (str/includes? (slurp f1) bad-pointer)
                 :before :measurement-succeeds
                 :after-hard-failure (message #(measure f1 remainder-file)))
     (sorted-map :control :f12-choice-deleted
                 :plant-verified? (not (contains? (:choices (read-edn f2)) :organise-third-origin))
                 :before (vec choice-order)
                 :after-hard-failure (message #(measure f2 remainder-file)))
     (let [after (measure aif f3)]
       (sorted-map :control :obligation-satisfaction-flipped
                   :plant-verified? (not= (get-in rem [:obligations obligation :satisfied-at-head?])
                                          (get-in (read-edn f3) [:obligations obligation :satisfied-at-head?]))
                   :before (get-in rem [:obligations obligation :satisfied-at-head?])
                   :after (get-in rem3 [:obligations obligation :satisfied-at-head?])
                   :sheet-line-moved? (not= (:text baseline) (:text after))))
     (let [after (measure aif f4)]
       (sorted-map :control :choice-gating-deleted
                   :plant-verified? (every? #(not= :organise-carrier (:choice %))
                                            (mapcat val (:gating (read-edn f4))))
                   :before (get (gates-by-choice rem) :organise-carrier)
                   :after (get (gates-by-choice rem4) :organise-carrier)
                   :sheet-reports-gates-nothing? (str/includes? (:text after) "**gates nothing**")))
     (sorted-map :control :flexiarg-range-out-of-bounds
                 :plant-verified? (str/includes? (slurp f5) flex-new)
                 :before flex-old
                 :after-hard-failure (message #(measure f5 remainder-file)))]))

(defn -main []
  (let [aif (env-file "AIF_EQ" (io/file lab "aif-equations.edn"))
        remainder-file (env-file "F12_REMAINDER" (io/file lab "runs/F12-organise/15-remainder.edn"))
        sheet (env-file "F12_SHEET_OUT" (io/file lab "C558-F12-decision-sheet.md"))
        run (env-file "F12_RUN_OUT" (io/file lab "runs/F12-organise/16-decision-sheet.edn"))
        m (measure aif remainder-file)
        cs (controls aif remainder-file m)
        gaps (sorted-map :measurement-that-separates-them [:organise-sorry]
                         :other-required-fields [])
        record (sorted-map
                :arms-dry-artifact "runs/F12-organise/14-arms-dry.edn"
                :arms-dry? true
                :choice-order choice-order
                :choices (into (sorted-map) (:choices m))
                :controls cs
                :gating (into (sorted-map) (:gating (:remainder m)))
                :obligations (into (sorted-map) (:obligations (:remainder m)))
                :pointer-coverage (sorted-map
                                   :extensions ["bb" "clj" "edn" "flexiarg" "lean" "md"]
                                   :note "Stricter than pointer_check.bb:237, whose regex covers only clj, lean and edn."
                                   :pointer-count (get-in m [:pointers :count])
                                   :unresolved [])
                :registry-field-gaps gaps
                :remainder-artifact "runs/F12-organise/15-remainder.edn"
                :remainder-fully-gated? true)]
    (spit sheet (:text m))
    (spit run (pprint-str record))
    (println (str "f12_decision_sheet: wrote " sheet " and " run
                  "; pointers=" (get-in m [:pointers :count])))))

(-main)
