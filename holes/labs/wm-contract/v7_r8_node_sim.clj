#!/usr/bin/env clojure
;; :V7 slice 3 -- the per-node verification harness for R8 (PERCEIVE,
;; "Present-fit mismatch").
;;
;;   clojure -M holes/labs/wm-contract/v7_r8_node_sim.clj [outdir]
;;
;; Default outdir: holes/labs/wm-contract/runs/V7-R8-node-sim.
;;
;; NO LIVE TICK, NO RUN LOCK, NOTHING WRITTEN UNDER data/. The producers are
;; run against declared fixtures (sim/R8-carriers.edn); the trace corpus under
;; data/wm-trace is READ and never written, and no lock is taken because
;; nothing here writes there. No wall-clock field is written, so two runs over
;; an unchanged tree and an unchanged corpus produce a byte-identical receipt.
;;
;; WHY R8 NEEDS THREE BLOCKS AND NOT ONE. Slices 1 and 2 each faced a node with
;; one state; R8 hosts three registry rows in three states, and the verdict is
;; about the difference between them:
;;
;;   eps   (:prediction-error, aif-equations.edn:80-85) -- shipped and
;;         route-tagged :R8 at scripts/futon2/report/war_machine.clj:6252,
;;         and recorded in every trace record as :prediction-errors.
;;   F     (:free-energy, aif-equations.edn:92-101) -- RETIRED under Joe's J2
;;         ruling and its producer DELETED. The check is that the absence is
;;         real, and that the trace stopped carrying the scalar.
;;   F_pi  (:policy-free-energy, aif-equations.edn:102-112) -- shipped, but
;;         behind FUTON_WM_FPI_POSTERIOR, default off.
;;
;; WHAT THE RUN ASSERTS:
;;
;;   1. EIGHT CHECKS THE SHIPPED PRODUCERS PASS, which are also what catches a
;;      wrong producer: the eps typed triple matches the declaration on every
;;      fixture; eps agrees with an EXACT-RATIONAL reference; an absent record
;;      carries no numbers; a refusal names its offending members; every record
;;      carries the producer contract; F_pi takes the declared branch on every
;;      fixture; F_pi agrees with a reference written in a different
;;      association; and the retired F scalar resolves to no var.
;;   2. THIRTEEN PLANTED WRONG PRODUCERS FAIL. Each is a wrong R8 someone could
;;      write. The script exits non-zero if any planted producer passes.
;;   3. THE SHIPPED PRODUCER'S OWN OUTPUT HISTORY. eps is recorded per tick, so
;;      it is replayed against production: every :prediction-errors entry in
;;      data/wm-trace is re-derived from that record's OWN :observed,
;;      :predicted-mean and :predicted-variance and compared with what the tick
;;      wrote. The corpus is pinned by per-file sha256.
;;   4. TWO CENSUSES REPORTED AS FINDINGS, NOT GATES, because their content is
;;      the verdict this slice is here to write: how many records ever carried
;;      the per-tick scalar the catalogue asks R8 for, and what the recorded
;;      :f-pi-posterior status is across the corpus.
;;
;; WHAT IT DOES NOT ASSERT. The eps reference is exact -- every fixture value is
;; dyadic and evaluated in rationals -- with the one named exception that the
;; default min-variance 0.01 is not a dyadic, so the floored cases divide by the
;; same double the producer does. The F_pi reference cannot be exact at all,
;; because ln(2 pi v) is transcendental: what is independent there is the
;; ASSOCIATION (the reference sums the log-normalisers and the normalised
;; residuals separately and multiplies by 1/2 once), not the logarithm, which
;; both sides take from java.lang.Math. Nothing here is written to :choices or
;; :decisions; nothing here is a ruling.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str]
         '[futon2.aif.free-energy :as fe]
         '[futon2.aif.policy-free-energy :as pfe])

(import '[java.security MessageDigest])

(def lab (io/file (System/getProperty "user.dir") "holes/labs/wm-contract"))
(def outdir (io/file (or (first *command-line-args*) (str (io/file lab "runs/V7-R8-node-sim")))))
(def trace-dir (io/file (System/getProperty "user.dir") "data/wm-trace"))

(defn- read-edn [f] (edn/read-string {:default (fn [_ v] v)} (slurp f)))

(def carriers (read-edn (io/file lab "sim/R8-carriers.edn")))
(def equations (read-edn (io/file lab "aif-equations.edn")))

(def registry-rows
  "R8's own rows of the equations registry -- read out of the registry rather
   than restated here. There are three."
  (filterv #(= :R8 (:node %)) (:equations equations)))

(def tolerance 1.0e-12)

(defn- sha256 [^java.io.File f]
  (let [md (MessageDigest/getInstance "SHA-256")]
    (with-open [in (io/input-stream f)]
      (let [buf (byte-array 65536)]
        (loop []
          (let [n (.read in buf)]
            (when (pos? n) (.update md buf 0 n) (recur))))))
    (str/join (map #(format "%02x" %) (.digest md)))))

(defn- dev
  "Absolute deviation, with a nil-safe and non-numeric-safe reading."
  [a b]
  (if (and (number? a) (number? b))
    (Math/abs (- (double a) (double b)))
    Double/POSITIVE_INFINITY))

;; ---------------------------------------------------------------------------
;; The eps reference: EXACT RATIONALS, transcribed from the registry :formal
;; line and the producer's declared typed-triple contract, not from its body.
;;
;;   eps  := o - mu                        aif-equations.edn:82
;;   Pi   := 1 / max(v, min-variance)      the eps0 floor, free_energy.clj:232
;;   weighted-error := eps * Pi
;;
;; The classification is the declaration's, restated as a table: a member is
;; MALFORMED when it is missing, nil, or not a finite number; any malformed
;; member (or a non-numeric observation) refuses; otherwise a nil observation
;; is absent; otherwise the triple is scored.
;; ---------------------------------------------------------------------------

(defn- finite-number? [x]
  (and (number? x)
       (let [d (double x)] (not (or (Double/isNaN d) (Double/isInfinite d))))))

(defn- member-malformed? [prediction k]
  (let [raw (get prediction k)]
    (or (not (contains? prediction k)) (nil? raw) (not (finite-number? raw)))))

(defn reference-prediction-error
  "The declared triple, evaluated in exact rationals wherever the inputs are
   dyadic. Returns the same three-status shape the declaration describes."
  [observed prediction {:keys [min-variance] :or {min-variance 0.01}}]
  (let [malformed (vec (filter #(member-malformed? prediction %) [:mean :variance]))
        observed-malformed? (and (some? observed) (not (finite-number? observed)))]
    (cond
      (or (seq malformed) observed-malformed?)
      {:status :refused :offending (cond-> malformed observed-malformed? (conj :observed))}

      (nil? observed) {:status :absent}

      :else
      (let [o (rationalize (double observed))
            mu (rationalize (double (:mean prediction)))
            v (rationalize (double (:variance prediction)))
            floor (rationalize (double min-variance))
            err (- o mu)
            prec (/ 1 (max v floor))]
        {:status :present
         :error (double err)
         :precision (double prec)
         :weighted-error (double (* err prec))
         :exact {:error err :precision prec :weighted-error (* err prec)}}))))

;; ---------------------------------------------------------------------------
;; The F_pi reference: a DIFFERENT ASSOCIATION of the registry's formal line.
;;
;;   registry (aif-equations.edn:104):  F_pi = sum_k 1/2 ( ln(2 pi v_k) + r_k^2 / v_k )
;;   node     (policy_free_energy.clj:139-141): accumulates that term per channel
;;   here:                              F_pi = 1/2 ( n ln(2 pi) + sum_k ln v_k )
;;                                             + 1/2 sum_k (r_k^2 / v_k)
;;
;; The residual half is summed in exact rationals and doubled once at the end;
;; only the logarithms are taken in floating point, and the 1/2 is applied
;; twice rather than n times. A missing 1/2, a missing 2 pi, or an
;; unnormalised residual therefore separates the two.
;; ---------------------------------------------------------------------------

(defn reference-f-pi
  "The declared branch table and total. Returns {:status :scored :total d} or
   {:status :rejected :error <key>}."
  [prediction observation {:keys [deterministic-tolerance absent-variance variance-floor]
                           :or {deterministic-tolerance 0.0
                                absent-variance :reject
                                variance-floor 0.01}}]
  (let [mean (:prediction-mean prediction)
        variance (:prediction-variance prediction)]
    (if-not (= (set (keys mean)) (set (keys variance)) (set (keys observation)))
      {:status :rejected :error :channel-mismatch}
      (loop [chs (sort-by str (keys mean))
             log-terms []
             residual-sum 0
             n 0]
        (if-let [ch (first chs)]
          (let [raw-v (double (get variance ch))
                absent? (= :absent (get-in prediction [:variance-status ch :status]))
                v (if (and (zero? raw-v) absent? (= :floor absent-variance))
                    (double variance-floor)
                    raw-v)
                r (- (rationalize (double (get observation ch)))
                     (rationalize (double (get mean ch))))]
            (cond
              (neg? v) {:status :rejected :error :invalid-variance :channel ch}

              (zero? v)
              (if (<= (Math/abs (double r)) (double deterministic-tolerance))
                (recur (rest chs) log-terms residual-sum n)
                {:status :rejected :error :deterministic-mismatch :channel ch})

              :else
              (recur (rest chs)
                     (conj log-terms (Math/log v))
                     (+ residual-sum (/ (* r r) (rationalize v)))
                     (inc n))))
          {:status :scored
           :total (+ (* 0.5 (+ (* n (Math/log (* 2.0 Math/PI))) (reduce + 0.0 log-terms)))
                     (* 0.5 (double residual-sum)))
           :scored-channels n})))))

;; ---------------------------------------------------------------------------
;; Running the fixtures
;; ---------------------------------------------------------------------------

(def eps-fixtures (:eps-fixtures carriers))
(def f-pi-fixtures (:f-pi-fixtures carriers))

(defn shipped-eps [observed prediction opts]
  (fe/compute-prediction-error observed prediction opts))

(defn- try-f-pi
  "Run F, returning the declared branch shape instead of throwing."
  [f prediction observation opts]
  (try {:status :scored :total (f prediction observation opts)}
       (catch clojure.lang.ExceptionInfo e
         {:status :rejected :error (:error (ex-data e))})
       (catch Exception e
         {:status :threw :class (.getName (class e))})))

(defn shipped-f-pi [prediction observation opts]
  (pfe/f-pi-for-candidate prediction observation opts))

(defn run-eps [node]
  (mapv (fn [{:keys [id observed prediction opts expect]}]
          (let [got (try {:ok (node observed prediction (or opts {}))}
                         (catch Exception e {:threw (.getName (class e))}))
                ref (reference-prediction-error observed prediction (or opts {}))]
            {:fixture id :expect expect :reference ref
             :record (:ok got) :threw (:threw got)}))
        eps-fixtures))

(defn run-f-pi [node]
  (mapv (fn [{:keys [id prediction observation opts expect error]}]
          (let [got (try-f-pi node prediction observation (or opts {}))
                ref (reference-f-pi prediction observation (or opts {}))]
            {:fixture id :expect expect :expect-error error :reference ref :result got}))
        f-pi-fixtures))

;; ---------------------------------------------------------------------------
;; The trace-corpus replay of eps, and the two censuses
;; ---------------------------------------------------------------------------

(def trace-files
  (->> (.listFiles trace-dir)
       ;; Dot-files are not trace days. .lane-futility-index.edn is the
       ;; sweeper's own index over this directory, and counting it would put a
       ;; non-tick record into every census below.
       (filter #(and (.isFile %)
                     (str/ends-with? (.getName %) ".edn")
                     (not (str/starts-with? (.getName %) "."))))
       (sort-by #(.getName %))
       vec))

(defn- read-records [f]
  (with-open [r (io/reader f)]
    (->> (line-seq r)
         (remove str/blank?)
         (mapv (fn [line]
                 (try (edn/read-string {:default (fn [_ v] v)} line)
                      (catch Exception _ ::unreadable)))))))

(def corpus
  (let [per-file
        (mapv
         (fn [f]
           (let [recs (read-records f)
                 good (remove #(= ::unreadable %) recs)
                 with-pe (filter #(seq (:prediction-errors %)) good)
                 ;; A SCORED ENTRY IS ONE THAT CARRIES THE THREE NUMBERS, not one
                 ;; that carries :status :present. Records written before the AC1
                 ;; typed triple (C130 s2, 2026-09-02) have no :status key at all,
                 ;; and excluding them would silently drop most of the corpus.
                 present (for [r with-pe
                               [ch m] (:prediction-errors r)
                               :when (and (not= :absent (:status m))
                                          (not= :refused (:status m))
                                          (number? (:observed m))
                                          (number? (:predicted-mean m))
                                          (number? (:predicted-variance m))
                                          (number? (:error m)))]
                           [ch m])]
             {:file (str "data/wm-trace/" (.getName f))
              :sha256 (sha256 f)
              :records (count recs)
              :unreadable (count (filter #(= ::unreadable %) recs))
              :records-with-prediction-errors (count with-pe)
              :present-channel-records (count present)
              :typed-present (count (filter (fn [[_ m]] (= :present (:status m))) present))
              :untyped-pre-ac1 (count (filter (fn [[_ m]] (nil? (:status m))) present))
              :carries-variational-free-energy
              (count (filter #(contains? % :variational-free-energy) good))
              :producer-contracts (frequencies (map :producer-contract good))
              :f-pi-posterior-status
              (frequencies (map #(let [p (get-in % [:decision :f-pi-posterior])]
                                   (when p [(:status p) (:reason p)]))
                                good))
              :present present}))
         trace-files)
        all-present (mapcat :present per-file)
        ;; identity 1: eps = observed - predicted-mean (every era)
        err-devs (for [[_ m] all-present]
                   (dev (:error m) (- (double (:observed m)) (double (:predicted-mean m)))))
        ;; identity 2: per-call precision = 1 / max(predicted-variance, 0.01)
        pcp (filter (fn [[_ m]] (number? (:per-call-precision m))) all-present)
        pcp-devs (for [[_ m] pcp]
                   (dev (:per-call-precision m)
                        (/ 1.0 (max (double (:predicted-variance m)) 0.01))))
        ;; identity 3: weighted-error = error * precision (R7's Pi after the overwrite)
        we (filter (fn [[_ m]] (and (number? (:precision m)) (number? (:weighted-error m)))) all-present)
        we-devs (for [[_ m] we]
                  (dev (:weighted-error m) (* (double (:error m)) (double (:precision m)))))]
    {:root "data/wm-trace"
     :read-only "This leg opens the corpus for reading only. No run lock is taken because nothing here writes to data/."
     :files (mapv #(dissoc % :present) per-file)
     :file-count (count per-file)
     :record-count (reduce + (map :records per-file))
     :records-with-prediction-errors (reduce + (map :records-with-prediction-errors per-file))
     :present-channel-records (count all-present)
     :typed-present-channel-records (reduce + (map :typed-present per-file))
     :untyped-pre-ac1-channel-records (reduce + (map :untyped-pre-ac1 per-file))
     :replay
     {:reads (str "Each recorded prediction-error entry re-derived from that record's OWN "
                  ":observed, :predicted-mean and :predicted-variance, and compared with what "
                  "the tick wrote. This is the shipped producer measured on production, not on "
                  "a fixture.")
      :error-identity {:n (count err-devs) :max-deviation (if (seq err-devs) (apply max err-devs) 0.0)}
      :per-call-precision-identity
      {:n (count pcp-devs) :max-deviation (if (seq pcp-devs) (apply max pcp-devs) 0.0)
       :note "1/max(v, 0.01) with the DEFAULT min-variance; a tick that passed another floor would show here."}
      :weighted-error-identity
      {:n (count we-devs) :max-deviation (if (seq we-devs) (apply max we-devs) 0.0)
       :note (str "The :precision on the record is R7's Pi, which precision/weighted-error "
                  "(src/futon2/aif/precision.clj:212-233) writes over the producer's per-call "
                  "value. This identity therefore measures the OVERWRITE, and is reported as a "
                  "finding rather than gated: it is R7's arithmetic on R8's error.")}}
     :per-tick-scalar-census
     {:asks "p4ng/sec-catalog.tex:202 -- R8 as 'a single number, comparable across time ... one number per tick'"
      :records-carrying-variational-free-energy
      (reduce + (map :carries-variational-free-energy per-file))
      :first-file (->> per-file (filter #(pos? (:carries-variational-free-energy %))) first :file)
      :last-file (->> per-file (filter #(pos? (:carries-variational-free-energy %))) last :file)
      :files-with-none (count (filter #(zero? (:carries-variational-free-energy %)) per-file))
      :producer-contracts (apply merge-with + (map :producer-contracts per-file))}
     :f-pi-posterior-census
     {:asks "aif-equations.edn:111 -- :status :realised-flag-gated, :live-consumer :policy-posterior"
      :by-status (apply merge-with + (map :f-pi-posterior-status per-file))}}))

;; ---------------------------------------------------------------------------
;; The checks. A check reads a run and answers :pass or :fail.
;; ---------------------------------------------------------------------------

(defn- eps-checks [runs]
  (let [present (filter #(= :present (get-in % [:reference :status])) runs)
        devs (for [r present
                   :let [rec (:record r) ref (:reference r)]
                   k [:error :precision :weighted-error]]
               {:fixture (:fixture r) :key k :deviation (dev (get rec k) (get ref k))})
        max-dev (if (seq devs) (apply max (map :deviation devs)) 0.0)]
    [{:check :eps-status-matches-declaration
      :status (if (every? #(= (:expect %) (get-in % [:record :status])) runs) :pass :fail)
      :reads "Every fixture's typed status is the one the declaration requires."
      :mismatches (vec (for [r runs :when (not= (:expect r) (get-in r [:record :status]))]
                         {:fixture (:fixture r) :expected (:expect r)
                          :got (get-in r [:record :status]) :threw (:threw r)}))}

     {:check :eps-agrees-with-exact-reference
      :status (if (<= max-dev 0.0) :pass :fail)
      :reads "Exact-rational reference from the registry formal line; the comparison is at deviation 0.0, not a tolerance."
      :comparisons (count devs)
      :max-deviation max-dev
      :worst (->> devs (sort-by (comp - :deviation)) (take 3) vec)}

     {:check :eps-absent-carries-no-numbers
      :status (if (every? (fn [r] (let [rec (:record r)]
                                    (or (not= :absent (:status rec))
                                        (and (nil? (:error rec)) (nil? (:precision rec))
                                             (nil? (:weighted-error rec)) (nil? (:observed rec))
                                             (some? (:reason rec))))))
                          runs)
                  :pass :fail)
      :reads "AC1: an observation nobody made is not an observation of zero. The record carries a reason and no numbers."}

     {:check :eps-refusal-names-offending-members
      :status (if (every? (fn [r] (let [rec (:record r)]
                                    (or (not= :refused (:status rec))
                                        (and (seq (:offending rec))
                                             (= :malformed-prediction-triple (:reason rec))
                                             (= (set (map :member (:offending rec)))
                                                (set (get-in r [:reference :offending])))))))
                          runs)
                  :pass :fail)
      :reads "Every refusal names its offending members, and names the SAME ones the reference does."}

     {:check :eps-contract-stamped
      :status (if (every? #(= :prediction-error/v1 (get-in % [:record :producer-contract])) runs) :pass :fail)
      :reads "src/futon2/aif/free_energy.clj:176-180 -- the stamp is on every record, scored, omitted or refused."}]))

(defn- f-pi-checks [runs]
  (let [scored (filter #(= :scored (get-in % [:reference :status])) runs)
        devs (for [r scored]
               {:fixture (:fixture r)
                :deviation (dev (get-in r [:result :total]) (get-in r [:reference :total]))})
        max-dev (if (seq devs) (apply max (map :deviation devs)) 0.0)]
    [{:check :f-pi-branch-matches-declaration
      :status (if (every? (fn [r]
                            (and (= (:expect r) (get-in r [:result :status]))
                                 (or (nil? (:expect-error r))
                                     (= (:expect-error r) (get-in r [:result :error])))))
                          runs)
                  :pass :fail)
      :reads "varianceTrichotomy as the declaration states it: negative rejects, a bare zero rejects off tolerance, an action-model absent zero floors under :floor."
      :mismatches (vec (for [r runs
                             :when (not (and (= (:expect r) (get-in r [:result :status]))
                                             (or (nil? (:expect-error r))
                                                 (= (:expect-error r) (get-in r [:result :error])))))]
                         {:fixture (:fixture r) :expected [(:expect r) (:expect-error r)]
                          :got [(get-in r [:result :status]) (get-in r [:result :error])]}))}

     {:check :f-pi-agrees-with-independent-association
      :status (if (<= max-dev tolerance) :pass :fail)
      :reads "The reference sums the log-normalisers and the normalised residuals separately and halves once; the node accumulates per channel. Only the logarithm is shared."
      :comparisons (count devs)
      :max-deviation max-dev
      :tolerance tolerance
      :worst (->> devs (sort-by (comp - :deviation)) (take 3) vec)}]))

(def f-scalar-check
  {:check :retired-f-scalar-has-no-producer
   :status (if (and (nil? (resolve 'futon2.aif.free-energy/compute-variational-free-energy))
                    (nil? (ns-resolve 'futon2.aif.free-energy 'compute-variational-free-energy))
                    (not (contains? (ns-publics 'futon2.aif.free-energy)
                                    'compute-variational-free-energy)))
             :pass :fail)
   :reads (str "aif-equations.edn:93 records ':code NO PRODUCER AT HEAD' for the F row. This "
               "resolves the symbol three ways and finds nothing, which is the same assertion "
               "test/futon2/aif/free_energy_test.clj:293-301 makes.")
   :vars-in-namespace-matching-variational
   (vec (sort (map str (filter #(str/includes? (str %) "variational")
                               (keys (ns-publics 'futon2.aif.free-energy))))))})

(def corpus-check
  (let [e (get-in corpus [:replay :error-identity])
        p (get-in corpus [:replay :per-call-precision-identity])]
    {:check :corpus-replay-reproduces-recorded-eps
     :status (if (and (<= (:max-deviation e) 0.0) (<= (:max-deviation p) 0.0)) :pass :fail)
     :reads "The shipped producer's two own identities, re-derived from each production record's own inputs, at deviation 0.0."
     :error-comparisons (:n e) :error-max-deviation (:max-deviation e)
     :per-call-precision-comparisons (:n p) :per-call-precision-max-deviation (:max-deviation p)}))

;; ---------------------------------------------------------------------------
;; Planted wrong producers. Each must be caught by a check the SHIPPED producer
;; passes; a check the shipped producer fails cannot discriminate.
;; ---------------------------------------------------------------------------

(def eps-plants
  [{:id :reversed-error
    :why "eps = mu - o. The sign of the miss is what R3 attributes by, so a reversed error moves belief the wrong way."
    :node (fn [o p opts]
            (let [r (fe/compute-prediction-error o p opts)]
              (if (= :present (:status r))
                (assoc r :error (- (:error r))
                       :weighted-error (- (:weighted-error r)))
                r)))}

   {:id :precision-is-the-variance
    :why "The registry's Pi is the RECIPROCAL of the floored variance. A producer that stamps the variance itself is confidently backwards."
    :node (fn [o p opts]
            (let [r (fe/compute-prediction-error o p opts)]
              (if (= :present (:status r))
                (assoc r :precision (max (double (:predicted-variance r)) 0.01))
                r)))}

   {:id :precision-without-the-floor
    :why "1/v with no max. The eps0 floor is the whole content of the term at a certain likelihood."
    :node (fn [o p opts]
            (let [r (fe/compute-prediction-error o p opts)]
              (if (= :present (:status r))
                (assoc r :precision (/ 1.0 (double (:predicted-variance r))))
                r)))}

   {:id :weighted-error-is-unweighted
    :why "R3b's precision-weighted error dropped back to the raw error -- the failure that would make R7 inert without any error message."
    :node (fn [o p opts]
            (let [r (fe/compute-prediction-error o p opts)]
              (if (= :present (:status r))
                (assoc r :weighted-error (:error r))
                r)))}

   {:id :absent-observation-scored-as-zero
    :why "The pre-AC1 producer: a channel this tick never observed scored against 0.0. Joe's 2026-09-02 ruling removed exactly this."
    :node (fn [o p opts]
            (if (nil? o)
              (fe/compute-prediction-error 0.0 p opts)
              (fe/compute-prediction-error o p opts)))}

   {:id :absence-dominates-refusal
    :why "The order of the two guards reversed: an unobserved channel whose likelihood ALSO failed would be quietly omitted instead of refusing the whole update."
    :node (fn [o p opts]
            (if (nil? o)
              {:status :absent :reason :observation-absent
               :producer-contract :prediction-error/v1}
              (fe/compute-prediction-error o p opts)))}

   {:id :negative-variance-refused
    :why "THE PLANT THAT LOOKS RIGHT. Refusing a negative predicted variance is what a reader would expect; the shipped producer scores it, because prediction-member checks finiteness and not sign (aif-equations.edn:83). The harness catches this plant, which is what makes the finding a fact about the shipped node rather than an opinion about it."
    :node (fn [o p opts]
            (if (and (number? (:variance p)) (neg? (double (:variance p))))
              {:status :refused :reason :malformed-prediction-triple
               :offending [{:member :variance :status :negative}]
               :producer-contract :prediction-error/v1}
              (fe/compute-prediction-error o p opts)))}

   {:id :contract-stamp-dropped
    :why "A record with no producer contract cannot be told from a record written by a producer that predates the contract (C129)."
    :node (fn [o p opts]
            (dissoc (fe/compute-prediction-error o p opts) :producer-contract))}])

(def f-pi-plants
  [{:id :f-pi-without-the-half
    :why "The 1/2 dropped. Every candidate's F_pi doubles, and B.9 subtracts it from the policy score."
    :node (fn [pred obs opts] (* 2.0 (pfe/f-pi-for-candidate pred obs opts)))}

   {:id :f-pi-log-variance-only
    :why "ln(v) instead of ln(2 pi v): the Gaussian normaliser without its constant. Changes every total by a fixed per-channel offset, which a within-tick comparison would not reveal."
    :node (fn [pred obs opts]
            (let [n (count (keys (:prediction-mean pred)))]
              (- (pfe/f-pi-for-candidate pred obs opts)
                 (* 0.5 n (Math/log (* 2.0 Math/PI))))))}

   {:id :f-pi-residual-not-normalised
    :why "r^2 instead of r^2/v. The channel with the least confident prediction would count most, not least."
    :node (fn [pred obs opts]
            ;; The shipped node runs first, so this plant keeps the whole branch
            ;; table and differs ONLY in the residual term -- which is what makes
            ;; the check that catches it attributable.
            (let [total (pfe/f-pi-for-candidate pred obs opts)
                  mean (:prediction-mean pred)
                  variance (:prediction-variance pred)
                  floor (double (get opts :variance-floor 0.01))
                  absent-mode (get opts :absent-variance :reject)]
              (reduce (fn [t ch]
                        (let [raw-v (double (get variance ch))
                              absent? (= :absent (get-in pred [:variance-status ch :status]))
                              v (if (and (zero? raw-v) absent? (= :floor absent-mode)) floor raw-v)
                              r (- (double (get obs ch)) (double (get mean ch)))]
                          (if (pos? v)
                            (+ t (* 0.5 (- (* r r) (/ (* r r) v))))
                            t)))
                      total (keys mean))))}

   {:id :f-pi-every-zero-floored
    :why "The :floor arm applied to a BARE zero as well as to an action-model absent one -- the collapse the two-kinds-of-zero distinction exists to prevent."
    :node (fn [pred obs opts]
            (let [variance (:prediction-variance pred)
                  patched (assoc pred :variance-status
                                 (into (or (:variance-status pred) {})
                                       (for [[ch v] variance :when (zero? (double v))]
                                         [ch {:status :absent}])))]
              (pfe/f-pi-for-candidate patched obs (assoc opts :absent-variance :floor))))}

   {:id :f-pi-negative-variance-scored
    :why "A negative variance scored on its magnitude instead of rejected. Contrast the eps producer on the same node, which scores one -- the two rows do not agree, and that is a fact about R8, not about this plant."
    :node (fn [pred obs opts]
            (let [variance (:prediction-variance pred)]
              (pfe/f-pi-for-candidate
               (assoc pred :prediction-variance
                      (into {} (for [[ch v] variance] [ch (Math/abs (double v))])))
               obs opts)))}

   {:id :f-pi-skips-unmatched-channels
    :why "Missing channels skipped instead of rejected -- a candidate scored on a subset of the evidence, which is how a partially-covered candidate would win a posterior it did not earn."
    :node (fn [pred obs opts]
            (let [mean (:prediction-mean pred) variance (:prediction-variance pred)
                  shared (filter #(contains? obs %) (keys mean))]
              (pfe/f-pi-for-candidate
               (assoc pred :prediction-mean (select-keys mean shared)
                      :prediction-variance (select-keys variance shared))
               (select-keys obs shared) opts)))}])

(defn- eps-plant-verdict [plant]
  (let [runs (run-eps (:node plant))
        checks (eps-checks runs)
        caught (vec (for [c checks :when (= :fail (:status c))] (:check c)))]
    {:id (:id plant) :why (:why plant)
     :verdict (if (seq caught) :caught :escaped)
     :caught-by caught}))

(defn- f-pi-plant-verdict [plant]
  (let [runs (run-f-pi (fn [pred obs opts] ((:node plant) pred obs opts)))
        checks (f-pi-checks runs)
        caught (vec (for [c checks :when (= :fail (:status c))] (:check c)))]
    {:id (:id plant) :why (:why plant)
     :verdict (if (seq caught) :caught :escaped)
     :caught-by caught}))

;; ---------------------------------------------------------------------------
;; The shipped run and the receipt
;; ---------------------------------------------------------------------------

(def shipped-eps-runs (run-eps shipped-eps))
(def shipped-f-pi-runs (run-f-pi shipped-f-pi))
(def shipped-checks
  (vec (concat (eps-checks shipped-eps-runs)
               (f-pi-checks shipped-f-pi-runs)
               [f-scalar-check corpus-check])))
(def shipped-pass (every? #(= :pass (:status %)) shipped-checks))

(def plant-verdicts
  (vec (concat (map eps-plant-verdict eps-plants)
               (map f-pi-plant-verdict f-pi-plants))))
(def all-plants-caught (every? #(= :caught (:verdict %)) plant-verdicts))

(def receipt
  {:harness :v7-r8-node-sim
   :row :V7
   :slice 3
   :node :R8
   :stage "PERCEIVE"
   :stage-at "p4ng/empirics-futon/control-stages.edn:16"
   :what-this-is
   (str "R8's three registry rows run against declared fixtures with independent references, "
        "plus a replay of the shipped eps producer against its own recorded output. The eps "
        "producer IS the shipped call path: scripts/futon2/report/war_machine.clj:6110-6127 "
        "calls futon2.aif.free-energy/channel-prediction-error per channel and the route is "
        "tagged :R8 at scripts/futon2/report/war_machine.clj:6252. F_pi is shipped behind a "
        "default-off flag. F has no producer at HEAD and the check is that its absence is real.")
   :code {:eps "src/futon2/aif/free_energy.clj:203-278 (compute-prediction-error)"
          :eps-envelope-reader "src/futon2/aif/free_energy.clj:280-295 (channel-prediction-error)"
          :eps-call-site "scripts/futon2/report/war_machine.clj:6110-6127"
          :eps-route-tag "scripts/futon2/report/war_machine.clj:6252"
          :f-pi "src/futon2/aif/policy_free_energy.clj:41-144 (f-pi-for-candidate), src/futon2/aif/policy_free_energy.clj:146-151 (f-pi-vector)"
          :f-pi-production-caller "scripts/futon2/report/war_machine.clj:441-564"
          :f-retired "src/futon2/aif/free_energy.clj:7-12 (the deletion), src/futon2/aif/trace.clj:241-247 (revision 21)"
          :reference "holes/labs/wm-contract/v7_r8_node_sim.clj:106-200"
          :runner "holes/labs/wm-contract/v7_r8_node_sim.clj"
          :carriers "holes/labs/wm-contract/sim/R8-carriers.edn"
          :test "test/futon2/aif/free_energy_test.clj, test/futon2/aif/policy_free_energy_test.clj"}
   :equations-registry
   {:rows (mapv (fn [r] {:id (:id r) :formal (:formal r) :class (:class r)
                         :imports (:imports r) :lean (:lean r) :lean-status (:lean-status r)
                         :status (:status r) :live-consumer (:live-consumer r)})
                registry-rows)
    :source "holes/labs/wm-contract/aif-equations.edn:80-112"
    :note "Three rows on one node, in three states: one shipped, one retired with its producer deleted, one shipped behind a default-off flag."}
   :eps {:fixtures (mapv #(select-keys % [:id :why :expect :non-dyadic]) eps-fixtures)
         :fixture-count (count eps-fixtures)
         :runs (mapv (fn [r] {:fixture (:fixture r)
                              :expect (:expect r)
                              :status (get-in r [:record :status])
                              :reference-status (get-in r [:reference :status])
                              :error (get-in r [:record :error])
                              :precision (get-in r [:record :precision])
                              :weighted-error (get-in r [:record :weighted-error])
                              :exact-reference (get-in r [:reference :exact])
                              :reason (get-in r [:record :reason])
                              :offending (mapv :member (get-in r [:record :offending]))})
                     shipped-eps-runs)}
   :f-pi {:fixtures (mapv #(select-keys % [:id :why :expect :error :non-dyadic]) f-pi-fixtures)
          :fixture-count (count f-pi-fixtures)
          :runs (mapv (fn [r] {:fixture (:fixture r)
                               :expect (:expect r)
                               :status (get-in r [:result :status])
                               :error-key (get-in r [:result :error])
                               :total (get-in r [:result :total])
                               :reference-total (get-in r [:reference :total])
                               :scored-channels (get-in r [:reference :scored-channels])})
                      shipped-f-pi-runs)}
   :checks shipped-checks
   :verdict (if shipped-pass :pass :fail)
   :negative-controls
   {:planted-producers plant-verdicts
    :n (count plant-verdicts)
    :all-caught all-plants-caught
    :reads (str "Each is a wrong R8 someone could write, and each must be caught by a check the "
                "SHIPPED producer passes. The two censuses below are excluded from that set, "
                "because they measure the node against a claim it does not meet and a check the "
                "shipped node fails cannot discriminate between a right producer and a wrong one.")}
   :output-history corpus
   :not-done
   ["Only R8. The other control-stages nodes are later slices of their own, in aif-equations dependency order."
    "No live tick, no run lock, nothing written under data/. The trace corpus is opened for reading."
    "gen_aif_dag.bb not run; nothing regenerated into a publish (TN 9a)."
    "aif-equations.edn is READ and not written."
    "No Lean was elaborated. The :lean-status :closed on all three rows is read from the registry; leg (b) of :V7 re-runs the two Clojure readbacks, which is a different claim."
    "The F_pi reference shares java.lang.Math/log with the node. What is independent there is the association and the 1/2, not the logarithm; the Lean witness is what states the log terms algebraically."
    "The F_pi flag chain was NOT exercised: no tick ran, so the census of :f-pi-posterior below reads what past ticks recorded and makes no claim about what a tick today would record."]})

(.mkdirs outdir)
(def receipt-file (io/file outdir "00-r8.edn"))
(with-open [w (io/writer receipt-file)]
  (binding [*out* w] (pp/pprint receipt)))

(println "V7 slice 3 node-sim -- node R8, carriers" (:id carriers))
(println "  registry rows:" (str/join ", " (map (comp name :id) registry-rows)))
(println "  eps fixtures:" (count eps-fixtures) "| F_pi fixtures:" (count f-pi-fixtures))
(doseq [c shipped-checks]
  (println (format "  %-46s %s" (name (:check c)) (name (:status c)))))
(println "  corpus:" (:file-count corpus) "files,"
         (:record-count corpus) "records,"
         (:records-with-prediction-errors corpus) "with prediction errors,"
         (:present-channel-records corpus) "scored channel records")
(println "  per-tick scalar carried by"
         (get-in corpus [:per-tick-scalar-census :records-carrying-variational-free-energy])
         "of" (:record-count corpus) "records")
(println "  f-pi-posterior status census:"
         (pr-str (get-in corpus [:f-pi-posterior-census :by-status])))
(doseq [p plant-verdicts]
  (println (format "  planted %-36s %s caught-by %s" (name (:id p)) (name (:verdict p)) (pr-str (:caught-by p)))))
(println "  verdict:" (name (if shipped-pass :pass :fail)) "| all planted producers caught:" all-plants-caught)
(println "  receipt:" (str receipt-file))

(System/exit (if (and shipped-pass all-plants-caught) 0 1))
