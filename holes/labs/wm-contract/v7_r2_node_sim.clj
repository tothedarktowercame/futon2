#!/usr/bin/env clojure
;; :V7 slice 2 -- the per-node verification harness for R2 (PERCEIVE,
;; "Structured observation").
;;
;;   clojure -M holes/labs/wm-contract/v7_r2_node_sim.clj [outdir]
;;
;; Default outdir: holes/labs/wm-contract/runs/V7-R2-node-sim.
;;
;; NO LIVE TICK, NO RUN LOCK, NOTHING WRITTEN UNDER data/. The node is run
;; against declared fixtures (sim/R2-carriers.edn); the trace corpus under
;; data/wm-trace is READ and never written, and no lock is taken because
;; nothing here writes there. No wall-clock field is written, so two runs over
;; an unchanged tree and an unchanged corpus produce a byte-identical receipt.
;;
;; WHY THIS IS NOT F3'S HARNESS. f3_node_sim.clj checks a TRANSCRIPTION of three
;; equations against an exact-rational reference. R2 has no equation:
;; aif-equations.edn:76 states a boundary, "o_t <- structured observation of the
;; world after action u_{t-1}", and the row is :class :stack-defined. So what is
;; checked here is the SHAPE the node declares about itself -- fourteen channels,
;; their source paths, their projections and their range -- and the node checked
;; is the one the machine actually runs (war_machine.clj:5936, route-tagged :R2
;; at war_machine.clj:5937), not a lab reimplementation.
;;
;; WHAT THE RUN ASSERTS, and the order matters:
;;
;;   1. Five checks the shipped node PASSES, which are also what catches a wrong
;;      node: the channel set is total and exactly the declared fourteen; the
;;      node agrees with an independent table-driven reference on every fixture
;;      it returns on; sense->vector is length-14 in declared order; it refuses a
;;      non-matching envelope; and an absent source is tagged :absent with
;;      :source-field-missing.
;;   2. Two checks that MEASURE THE NODE AGAINST ITS OWN DECLARATION and are
;;      reported as findings rather than as harness failures, because their
;;      content is the verdict this slice is here to write: does the absence
;;      envelope's promised coercion to 0.0 actually happen at each channel, and
;;      is the declared [0,1] range enforced at each channel.
;;   3. SEVEN PLANTED WRONG NODES FAIL. Each is a wrong R2 someone could write;
;;      each must be caught by one of the five checks in (1) -- not by (2), since
;;      the shipped node does not pass (2) everywhere and a check the shipped
;;      node fails cannot discriminate. The script exits non-zero if any planted
;;      node passes.
;;   4. THE SHIPPED NODE'S OWN OUTPUT HISTORY. The declared range is a claim
;;      about production, so it is measured on production: every :observation map
;;      in data/wm-trace is read and its channel set and per-channel range
;;      recorded, with the corpus pinned by per-file sha256. This is the leg that
;;      makes the range answer a fact rather than a probe.
;;
;; WHAT IT DOES NOT ASSERT. The reference is transcribed from the DECLARATION by
;; the same hand that reads the body, so it cannot catch an error the two share;
;; that is why the range is checked by probe and by corpus instead. The
;; :producer-range fields of the carriers file are code-reading claims with
;; file:line, not measurements. Nothing here is written to :choices or
;; :decisions; nothing here is a ruling.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str]
         '[futon2.aif.observation :as observation])

(def lab (io/file (System/getProperty "user.dir") "holes/labs/wm-contract"))
(def outdir (io/file (or (first *command-line-args*) (str (io/file lab "runs/V7-R2-node-sim")))))
(def trace-dir (io/file (System/getProperty "user.dir") "data/wm-trace"))

(defn- read-edn [f] (edn/read-string {:default (fn [_ v] v)} (slurp f)))

(def carriers (read-edn (io/file lab "sim/R2-carriers.edn")))
(def equations (read-edn (io/file lab "aif-equations.edn")))

(def registry-rows
  "R2's own rows of the equations registry -- read out of the registry rather
   than restated here. There is exactly one."
  (filterv #(= :R2 (:node %)) (:equations equations)))

(def declared-channels (mapv :channel (:channels carriers)))
(def channel-spec (into {} (map (juxt :channel identity)) (:channels carriers)))
(def tolerance 1.0e-12)

;; ---------------------------------------------------------------------------
;; The independent reference: a generic interpreter over the carriers' :reference
;; specs. Structurally a table walk; observe's body is a literal fourteen-key map.
;; ---------------------------------------------------------------------------

(defn- num-at [data path default]
  (let [v (get-in data path)]
    (if (number? v) (double v) (double default))))

(defn- present-number? [data path]
  (number? (get-in data path)))

(defn- present-at?
  "Presence in the sense the absence envelope uses: any non-nil value at the
   path. Mirrors present-value? at src/futon2/aif/observation.clj:36-39. Not the
   same test as present-number? above -- :ticks-firing-ratio's declared source
   [:graph :dynamics :ticks] is a vector, so a numeric test would report every
   fixture as having lost that channel too."
  [data path]
  (some? (get-in data path)))

(defn- reference-channel
  [data {:keys [kind path default num num-default den n n-default den-count scale cap]}]
  (case kind
    :path (num-at data path default)
    :ratio (if (and (present-number? data den) (pos? (num-at data den 0)))
             (/ (num-at data num num-default) (num-at data den 0))
             (double default))
    :capped-scale (min (double cap) (/ (num-at data num num-default) (double scale)))
    :capped-pair-density (let [k (num-at data n n-default)
                               max-edges (/ (* k (dec k)) 2.0)]
                           (if (pos? max-edges)
                             (min (double cap) (/ (num-at data num num-default) max-edges))
                             (double default)))
    :capped-count-ratio (let [total (count (or (get-in data den-count) []))]
                          (if (pos? total)
                            (/ (num-at data num num-default) (double total))
                            (double default)))))

(defn- reference-observe
  "The declared projection of every channel, evaluated from the carriers table."
  [data]
  (into {} (map (fn [ch] [ch (reference-channel data (:reference (channel-spec ch)))]))
        declared-channels))

;; ---------------------------------------------------------------------------
;; Fixtures: the declared ones, plus one per channel with that channel's
;; requirement paths removed.
;; ---------------------------------------------------------------------------

(defn- dissoc-path [m path]
  (if (= 1 (count path))
    (dissoc m (first path))
    (if (map? (get m (first path)))
      (update m (first path) dissoc-path (vec (rest path)))
      m)))

(def complete-data
  (:data (first (filter #(= :complete (:id %)) (:fixtures carriers)))))

(def absence-fixtures
  "One fixture per channel: :complete with that channel's declared requirement
   paths removed. :shares-paths-with names the other channels the same removal
   makes absent, because two channels read [:graph :summary :total-repos]."
  (vec (for [ch declared-channels
             :let [paths (:requirement-paths (channel-spec ch))
                   data (reduce dissoc-path complete-data paths)
                   also (vec (sort (remove #{ch}
                                           (for [other declared-channels
                                                 :when (some (fn [p] (not (present-at? data p)))
                                                             (:requirement-paths (channel-spec other)))]
                                             other))))]]
         {:id (keyword (str "absent-" (name ch)))
          :drops ch
          :removed-paths paths
          :shares-paths-with also
          :data data})))

(def fixtures
  (into (mapv #(select-keys % [:id :why :data]) (:fixtures carriers))
        absence-fixtures))

;; ---------------------------------------------------------------------------
;; Running a node over the fixtures. A node that raises is recorded, not caught
;; and hidden: the refusal is one of the things being measured.
;; ---------------------------------------------------------------------------

(defn- run-node
  [node-fn]
  (vec (for [{:keys [id data]} fixtures]
         (try
           {:fixture id :status :returned :observation (node-fn data)}
           (catch Throwable t
             {:fixture id :status :threw
              :class (.getName (class t))
              :message (ex-message t)})))))

(defn- returned [runs] (filter #(= :returned (:status %)) runs))

;; ---------------------------------------------------------------------------
;; The five discriminating checks
;; ---------------------------------------------------------------------------

(defn- check-channel-set
  [runs]
  (let [declared (set declared-channels)
        bad (vec (for [{:keys [fixture observation]} (returned runs)
                       :when (not= declared (set (keys observation)))]
                   {:fixture fixture
                    :only-in-node (vec (sort (remove declared (keys observation))))
                    :only-in-declaration (vec (sort (remove (set (keys observation)) declared)))}))]
    {:check :channel-set-is-the-declared-fourteen
     :declared-count (count declared)
     :status (if (seq bad) :fail :pass)
     :disagreements bad}))

(defn- check-reference-agreement
  [runs]
  (let [fixture-data (into {} (map (juxt :id :data)) fixtures)
        devs (for [{:keys [fixture observation]} (returned runs)
                   :let [ref (reference-observe (get fixture-data fixture))]
                   ch declared-channels
                   :let [nv (get observation ch)
                         rv (get ref ch)]]
               {:fixture fixture :channel ch
                :node nv :reference rv
                :deviation (if (and (number? nv) (number? rv))
                             (Math/abs (- (double nv) (double rv)))
                             Double/POSITIVE_INFINITY)})
        worst (reduce max 0.0 (map :deviation devs))
        bad (vec (take 20 (filter #(>= (:deviation %) tolerance) devs)))]
    {:check :node-agrees-with-independent-reference
     :fixtures-compared (count (returned runs))
     :comparisons (count devs)
     :max-deviation worst
     :status (if (seq bad) :fail :pass)
     :disagreements bad}))

(defn- check-vector-boundary
  [runs]
  (let [bad (vec (for [{:keys [fixture observation]} (returned runs)
                       :let [env (observation/observation-envelope observation)
                             v (try (observation/sense->vector observation env)
                                    (catch Throwable t [:threw (ex-message t)]))
                             expected (mapv #(get observation % 0.0) declared-channels)]
                       :when (not= v expected)]
                   {:fixture fixture :vector v :expected expected}))]
    {:check :vector-is-declared-order-and-length
     :declared-order declared-channels
     :length (count declared-channels)
     :status (if (seq bad) :fail :pass)
     :disagreements bad}))

(defn- check-envelope-refusal
  [runs]
  ;; The refusal under measurement is a VARIANT mismatch at identical numeric
  ;; coordinates: :empty and the :complete-with-loop-health-removed observation
  ;; can agree on a coordinate and still differ in provenance. Here the probe is
  ;; blunter and always available: hand each observation the envelope of another.
  (let [rs (vec (returned runs))
        outcomes (vec (for [i (range (count rs))
                            :let [a (nth rs i)
                                  b (nth rs (mod (inc i) (count rs)))
                                  env-b (observation/observation-envelope (:observation b))
                                  same? (= (observation/observation-envelope (:observation a)) env-b)]
                            :when (not same?)]
                        {:fixture (:fixture a) :other (:fixture b)
                         :outcome (try (observation/sense->vector (:observation a) env-b) :accepted
                                       (catch clojure.lang.ExceptionInfo _ :refused)
                                       (catch Throwable t (keyword (str "threw-" (.getName (class t))))))}))
        bad (vec (remove #(= :refused (:outcome %)) outcomes))]
    {:check :mismatched-envelope-is-refused
     :pairs-tested (count outcomes)
     :status (if (or (empty? outcomes) (seq bad)) :fail :pass)
     :disagreements bad}))

(defn- check-absence-tagged
  [runs]
  (let [by-fixture (into {} (map (juxt :fixture identity)) runs)
        rows (vec (for [{:keys [id drops]} absence-fixtures
                        :let [r (get by-fixture id)]
                        :when (= :returned (:status r))
                        :let [st (observation/observation-status (:observation r) drops)]]
                    {:fixture id :channel drops
                     :variant (:variant st) :reason (:reason st)}))
        bad (vec (remove #(and (= :absent (:variant %))
                               (= :source-field-missing (:reason %)))
                         rows))]
    {:check :absent-source-is-tagged-absent
     :channels-tested (count rows)
     :status (if (seq bad) :fail :pass)
     :disagreements bad
     :not-tested (vec (sort (for [{:keys [id drops]} absence-fixtures
                                  :when (not= :returned (:status (get by-fixture id)))]
                              drops)))}))

(def discriminating-checks
  [check-channel-set check-reference-agreement check-vector-boundary
   check-envelope-refusal check-absence-tagged])

(defn- run-checks [runs] (mapv #(% runs) discriminating-checks))

;; ---------------------------------------------------------------------------
;; The two measurements of the node against its own declaration. These are
;; FINDINGS, not gates: the shipped node does not satisfy them everywhere, and
;; that is what this slice is here to record.
;; ---------------------------------------------------------------------------

(def coercion-promise
  (let [by-fixture (into {} (map (juxt :fixture identity)) (run-node observation/observe))]
    (vec (for [{:keys [id drops removed-paths shares-paths-with]} absence-fixtures
               :let [r (get by-fixture id)]]
           (merge {:channel drops :removed-paths removed-paths}
                  (cond
                    (not= :returned (:status r))
                    {:kept :broken-by-refusal
                     :promised 0.0
                     :outcome {:status :threw :class (:class r) :message (:message r)}
                     :collateral (vec (sort (remove #{drops} declared-channels)))}

                    (zero? (double (get (:observation r) drops)))
                    {:kept true :promised 0.0 :observed (get (:observation r) drops)}

                    :else
                    {:kept false :promised 0.0 :observed (get (:observation r) drops)})
                  (when (seq shares-paths-with) {:shares-paths-with shares-paths-with}))))))

(def range-probes
  "One probe per channel: an input built to put that channel outside [0,1] if
   nothing stops it. A channel whose probe lands inside the range is a channel
   the NODE bounds; the rest are bounded, if at all, by their producer."
  (let [big 7.0
        probe {:loop-health {:overall big}
               :support-attack {:support-coverage big :attack-coverage (- big)}
               :mission-triage {:health (- big)}
               :graph {:dynamics {:commit-percentages {:stack big :consulting (- big)
                                                       :portfolio big :mathematics (- big)}
                                  :ticks [{:fired? true}]}
                       :summary {:active-repos 40 :total-repos 4 :coupling-edges 400
                                 :total-sorrys 400 :ticks-firing 9}}
               :frames {:depositing-signal big}
               :annotation-graph {:health (- big)}}
        obs (observation/observe probe)]
    {:input probe
     :channels (vec (for [ch declared-channels
                          :let [v (double (get obs ch))
                                spec (channel-spec ch)
                                [lo hi] (:declared-range spec)]]
                      {:channel ch
                       :declared-range [lo hi]
                       :observed v
                       :in-declared-range (<= (double lo) v (double hi))
                       :bounded-by (if (<= (double lo) v (double hi)) :the-node :nothing-in-the-node)
                       :producer-range (:producer-range spec)}))}))

;; ---------------------------------------------------------------------------
;; Planted wrong nodes. Each must be caught by one of the five checks above.
;; They are built by substituting into the shipped observation so that the plant
;; differs from the shipped node in exactly the one way its name says.
;; ---------------------------------------------------------------------------

(def statuses-key :futon2.aif.observation/channel-statuses)

(defn- planted
  [id statement f]
  (let [runs (run-node f)
        checks (run-checks runs)
        failed (vec (sort (map :check (filter #(= :fail (:status %)) checks))))]
    {:id id :statement statement
     :verdict (if (seq failed) :caught :passed)
     :caught-by failed}))

(def controls
  [(planted
    :drops-annotation-health
    "Thirteen channels instead of fourteen -- the shape the trace corpus actually carries before v0.10, so it is a node that once existed."
    (fn [data] (dissoc (observation/observe data) :annotation-health)))

   (planted
    :inverted-active-repo-ratio
    "total/active instead of active/total. Both are ratios of the same two counts and both are 1.0 when the repos are all active, so a fixture where they differ is what separates them."
    (fn [data]
      (let [s (get-in data [:graph :summary])
            obs (observation/observe data)]
        (assoc obs :active-repo-ratio
               (if (and s (pos? (:active-repos s 0)))
                 (/ (double (:total-repos s 0)) (:active-repos s))
                 0.0)))))

   (planted
    :uncapped-sorry-count-norm
    "open sorrys / 10 without the (min 1.0 ...). Identical on every input with fewer than ten open sorrys, which is most of them."
    (fn [data]
      (let [s (get-in data [:graph :summary])]
        (assoc (observation/observe data)
               :sorry-count-norm (/ (double (:total-sorrys s 0)) 10.0)))))

   (planted
    :coupling-density-over-n-squared
    "coupling edges / n^2 instead of / (n(n-1)/2). Same order of magnitude, always smaller, never negative -- no sign or range check can see it."
    (fn [data]
      (let [s (get-in data [:graph :summary])
            n (double (:total-repos s 0))]
        (assoc (observation/observe data)
               :coupling-density (if (pos? n)
                                   (min 1.0 (/ (double (:coupling-edges s 0)) (* n n)))
                                   0.0)))))

   (planted
    :ticks-firing-over-firing-plus-one
    "A smoothed firing ratio, firing/(firing+1), in place of firing/total. It is in [0,1) and monotone in the firing count, so it passes every range and shape check."
    (fn [data]
      (let [f (double (get-in data [:graph :summary :ticks-firing] 0))]
        (assoc (observation/observe data) :ticks-firing-ratio (/ f (inc f))))))

   (planted
    :absent-coerced-to-one
    "The absence envelope's promised coercion made to 1.0 rather than 0.0 -- a node that reads a missing source as full health instead of none."
    (fn [data]
      (let [obs (observation/observe data)]
        (reduce (fn [o ch]
                  (if (= :absent (:variant (observation/observation-status obs ch)))
                    (assoc o ch 1.0)
                    o))
                obs
                declared-channels))))

   (planted
    :every-variant-observed
    "The numbers left exactly as the shipped node computes them, and every channel's provenance rewritten to :observed. This is the node that erases the measured-zero / substituted-zero distinction the envelope exists to keep, and no numeric check can see it."
    (fn [data]
      (let [obs (observation/observe data)]
        (vary-meta obs assoc statuses-key
                   (into {} (map (fn [ch] [ch {:variant :observed}])) declared-channels)))))])

(def all-controls-caught (every? #(= :caught (:verdict %)) controls))

;; ---------------------------------------------------------------------------
;; The shipped node's own output history: every :observation in data/wm-trace.
;; READ ONLY. The corpus is pinned by per-file sha256 so a receipt that changes
;; says which file moved.
;; ---------------------------------------------------------------------------

(defn- sha256 [^java.io.File f]
  (let [md (java.security.MessageDigest/getInstance "SHA-256")
        buf (byte-array 65536)]
    (with-open [in (io/input-stream f)]
      (loop []
        (let [n (.read in buf)]
          (when (pos? n) (.update md buf 0 n) (recur)))))
    (str/join (map #(format "%02x" %) (.digest md)))))

(def trace-files
  "The daily trace files, by their writer's own naming (futon2.aif.trace).
   data/wm-trace also holds a dotfile index and a shadow-step JSON, neither of
   which is a trace record; they are excluded by name rather than by parsing."
  (vec (sort-by #(.getName ^java.io.File %)
                (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn"
                                     (.getName ^java.io.File %))
                        (or (seq (.listFiles trace-dir)) [])))))

(def lane-index
  "An independent count of the same corpus. The sweeper's own index file carries
   :record-count over the trace store; it is read here only so the line-by-line
   scan below has something other than itself to agree with."
  (let [f (io/file trace-dir ".lane-futility-index.edn")]
    (when (.exists f)
      {:file ".lane-futility-index.edn"
       :sha256 (sha256 f)
       :record-count (:record-count (read-edn f))})))

(defn- scan-trace-file
  [^java.io.File f]
  (with-open [rdr (io/reader f)]
    (reduce
     (fn [acc [idx line]]
       (let [rec (try (edn/read-string {:default (fn [_ v] v)} line)
                      (catch Throwable _ ::unreadable))]
         (cond
           (= ::unreadable rec) (update acc :unreadable inc)
           (not (map? rec)) (update acc :not-a-map inc)
           (nil? (:observation rec)) (update acc :without-observation inc)
           :else
           (let [obs (:observation rec)
                 chs (vec (sort (keys obs)))]
             (-> acc
                 (update :with-observation inc)
                 (update-in [:channel-sets chs] (fnil inc 0))
                 (update :per-channel
                         (fn [pc]
                           (reduce (fn [m [ch v]]
                                     (if (number? v)
                                       (-> m
                                           (update-in [ch :n] (fnil inc 0))
                                           (update-in [ch :min] (fnil min Double/POSITIVE_INFINITY) (double v))
                                           (update-in [ch :max] (fnil max Double/NEGATIVE_INFINITY) (double v)))
                                       (update-in m [ch :non-numeric] (fnil inc 0))))
                                   pc
                                   obs)))
                 (update :out-of-range
                         into
                         (for [[ch v] obs
                               :when (and (number? v) (not (<= 0.0 (double v) 1.0)))]
                           {:file (.getName f) :record (inc idx) :channel ch :value (double v)})))))))
     {:with-observation 0 :without-observation 0 :not-a-map 0 :unreadable 0
      :channel-sets {} :per-channel {} :out-of-range []}
     (map-indexed vector (line-seq rdr)))))

(defn- merge-scan [a b]
  {:with-observation (+ (:with-observation a) (:with-observation b))
   :without-observation (+ (:without-observation a) (:without-observation b))
   :not-a-map (+ (:not-a-map a) (:not-a-map b))
   :unreadable (+ (:unreadable a) (:unreadable b))
   :channel-sets (merge-with + (:channel-sets a) (:channel-sets b))
   :per-channel (merge-with (fn [x y]
                              {:n (+ (:n x 0) (:n y 0))
                               :min (min (:min x Double/POSITIVE_INFINITY) (:min y Double/POSITIVE_INFINITY))
                               :max (max (:max x Double/NEGATIVE_INFINITY) (:max y Double/NEGATIVE_INFINITY))
                               :non-numeric (+ (:non-numeric x 0) (:non-numeric y 0))})
                            (:per-channel a) (:per-channel b))
   :out-of-range (into (:out-of-range a) (:out-of-range b))})

(def corpus
  (let [per-file (mapv (fn [^java.io.File f]
                         (let [s (scan-trace-file f)]
                           {:file (.getName f)
                            :bytes (.length f)
                            :sha256 (sha256 f)
                            :records-with-observation (:with-observation s)
                            :scan s}))
                       trace-files)
        total (reduce merge-scan
                      {:with-observation 0 :without-observation 0 :not-a-map 0 :unreadable 0
                       :channel-sets {} :per-channel {} :out-of-range []}
                      (map :scan per-file))
        shapes (vec (for [[chs n] (sort-by (comp - val) (:channel-sets total))]
                      {:channels chs
                       :channel-count (count chs)
                       :records n
                       :missing-from-declaration (vec (sort (remove (set chs) declared-channels)))
                       :not-in-declaration (vec (sort (remove (set declared-channels) chs)))
                       :first-file (->> per-file
                                        (filter #(contains? (get-in % [:scan :channel-sets]) chs))
                                        first :file)
                       :last-file (->> per-file
                                       (filter #(contains? (get-in % [:scan :channel-sets]) chs))
                                       last :file)}))]
    {:root "data/wm-trace"
     :read-only "This leg opens the corpus for reading only. No run lock is taken because nothing here writes to data/."
     :files (mapv #(dissoc % :scan) per-file)
     :file-count (count per-file)
     :records-with-observation (:with-observation total)
     :records-without-observation (:without-observation total)
     :unreadable-lines (:unreadable total)
     :channel-shapes shapes
     :per-channel (into (sorted-map)
                        (map (fn [[ch m]]
                               [ch (assoc m :in-declaration (boolean ((set declared-channels) ch))
                                          :declared-range (:declared-range (channel-spec ch))
                                          :within-declared-range
                                          (and (>= (:min m Double/POSITIVE_INFINITY) 0.0)
                                               (<= (:max m Double/NEGATIVE_INFINITY) 1.0)))]))
                        (:per-channel total))
     :out-of-range-observations (:out-of-range total)
     :out-of-range-count (count (:out-of-range total))
     :independent-record-count
     (assoc lane-index
            :agrees-with-scan (= (:record-count lane-index) (:with-observation total))
            :reads (str "The sweeper's index is derived from the same files, so this is a "
                        "cross-check on the READ and not on the corpus: it says the line-by-line "
                        "scan here did not drop or double-count a record."))}))

;; ---------------------------------------------------------------------------
;; The shipped run and the receipt
;; ---------------------------------------------------------------------------

(def shipped-runs (run-node observation/observe))
(def shipped-checks (run-checks shipped-runs))
(def shipped-pass (every? #(= :pass (:status %)) shipped-checks))

(def receipt
  {:harness :v7-r2-node-sim
   :row :V7
   :slice 2
   :node :R2
   :stage "PERCEIVE"
   :stage-at "p4ng/empirics-futon/control-stages.edn:15"
   :what-this-is
   (str "One control-stages node run alone against declared fixtures, plus a read of its own "
        "output history. UNLIKE :F3's R5 pilot this IS the shipped call path: "
        "scripts/futon2/report/war_machine.clj:5936 calls futon2.aif.observation/observe and tags "
        "the route :R2 at scripts/futon2/report/war_machine.clj:5937. R2's registry row states a "
        "BOUNDARY and not a formula (aif-equations.edn:76), so what is checked is the shape the "
        "node declares about itself, not a transcription of an equation.")
   :code {:node "src/futon2/aif/observation.clj:103-146 (observe)"
          :envelope "src/futon2/aif/observation.clj:84-101 (observation-envelope), src/futon2/aif/observation.clj:41-76 (channel-statuses)"
          :vector-boundary "src/futon2/aif/observation.clj:148-158 (sense->vector)"
          :reference "holes/labs/wm-contract/v7_r2_node_sim.clj:99-121 (the table interpreter) over sim/R2-carriers.edn :reference"
          :runner "holes/labs/wm-contract/v7_r2_node_sim.clj"
          :carriers "holes/labs/wm-contract/sim/R2-carriers.edn"
          :call-site "scripts/futon2/report/war_machine.clj:5936-5937"
          :test "test/futon2/aif/observation_test.clj"}
   :equations-registry
   {:rows (mapv (fn [r] {:id (:id r) :formal (:formal r) :class (:class r)
                         :imports (:imports r) :lean (:lean r) :lean-status (:lean-status r)
                         :code (:code r)})
                registry-rows)
    :source "holes/labs/wm-contract/aif-equations.edn:74-79"
    :note "One row. It carries no :code field: the runtime sites are named in its :lean-note instead."}
   :fixtures (mapv #(select-keys % [:id :why :drops :removed-paths :shares-paths-with]) fixtures)
   :fixture-count (count fixtures)
   :node-runs (mapv #(select-keys % [:fixture :status :class :message]) shipped-runs)
   :checks shipped-checks
   :verdict (if shipped-pass :pass :fail)
   :declaration-measurements
   {:reads (str "These two are NOT gates. They measure the shipped node against assertions the node "
                "makes about itself, and the shipped node does not satisfy them everywhere -- which "
                "is the content this slice is here to record. A planted node is therefore never "
                "allowed to be caught by them.")
    :absence-coercion-promise
    {:promised "src/futon2/aif/observation.clj:70-74 -- every absent channel is written {:variant :absent :reason :source-field-missing :coerced-to 0.0}"
     :channels coercion-promise
     :kept (count (filter #(true? (:kept %)) coercion-promise))
     :broken (vec (sort (map :channel (remove #(true? (:kept %)) coercion-promise))))}
    :declared-range
    {:asserted-at (get-in carriers [:declaration-sites :range-assertion])
     :probe range-probes
     :bounded-by-the-node (vec (sort (map :channel (filter :in-declared-range (:channels range-probes)))))
     :not-bounded-by-the-node (vec (sort (map :channel (remove :in-declared-range (:channels range-probes)))))
     :producer-range-census
     (into (sorted-map) (map (fn [{:keys [channel producer-range producer-at]}]
                               [channel {:producer-range producer-range :producer-at producer-at}]))
           (:channels carriers))}}
   :negative-controls
   {:planted-nodes controls
    :all-caught all-controls-caught
    :reads (str "Each is a wrong R2 someone could write. A plant is caught only by a check the "
                "SHIPPED node passes -- the two declaration measurements above are excluded, "
                "because a check the shipped node fails cannot discriminate between a right node "
                "and a wrong one.")}
   :output-history corpus
   :not-done
   ["Only R2. The other control-stages nodes are later slices of their own, in aif-equations dependency order."
    "No live tick, no run lock, nothing written under data/. The trace corpus is opened for reading."
    "gen_aif_dag.bb not run; nothing regenerated into a publish (TN 9a)."
    "aif-equations.edn is READ and not written."
    "No Lean was elaborated. The :observe row's :lean-status :closed is read from the registry; leg (b) of :V7 re-runs the Clojure readback, which is a different claim."
    "The reference is transcribed from the DECLARATION and cannot catch an error the declaration and the body share. The declared range is therefore checked by probe and by the output history instead, not by reference agreement."]})

(.mkdirs outdir)
(def receipt-file (io/file outdir "00-r2.edn"))
(with-open [w (io/writer receipt-file)]
  (binding [*out* w] (pp/pprint receipt)))

(println "V7 slice 2 node-sim -- node R2, carriers" (:id carriers))
(println "  fixtures:" (count fixtures) "| returned:" (count (returned shipped-runs))
         "| threw:" (- (count shipped-runs) (count (returned shipped-runs))))
(doseq [c shipped-checks]
  (println (format "  %-42s %s" (name (:check c)) (name (:status c)))))
(println "  coercion promise kept at"
         (count (filter #(true? (:kept %)) coercion-promise)) "of" (count coercion-promise)
         "channels; broken at" (pr-str (vec (sort (map :channel (remove #(true? (:kept %)) coercion-promise))))))
(println "  declared [0,1] bounded by the node at"
         (count (filter :in-declared-range (:channels range-probes))) "of" (count declared-channels)
         "channels")
(println "  trace corpus:" (:file-count corpus) "files,"
         (:records-with-observation corpus) "observation records,"
         (count (:channel-shapes corpus)) "distinct channel sets,"
         (:out-of-range-count corpus) "coordinates outside [0,1]")
(doseq [c controls]
  (println (format "  planted %-34s %s caught-by %s" (name (:id c)) (name (:verdict c)) (pr-str (:caught-by c)))))
(println "  verdict:" (name (if shipped-pass :pass :fail)) "| all planted nodes caught:" all-controls-caught)
(println "  receipt:" (str receipt-file))

(System/exit (if (and shipped-pass all-controls-caught) 0 1))
