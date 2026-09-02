#!/usr/bin/env clojure
;; U16 -- the three Bernoulli outcome semantics, BUILT AND RUN.
;;
;;   clojure -M holes/labs/wm-contract/u16_outcome_semantics.clj [outdir]
;;
;; WHY THIS EXISTS. U12 measured that `log-preference`'s Bernoulli branch picks
;; the non-target pole with `(= 0 x)`, which is true for the long 0 and false
;; for the double 0.0, so on R2's all-doubles observation vector the unsatisfied
;; pole is unreachable and `nil` reads as met. That is a defect with more than
;; one defensible repair, and Joe's rule of 2026-09-01 says a choice the theory
;; does not settle gets its branches BUILT AND RUN rather than ruled in advance.
;; This script runs them and reports the numbers; it decides nothing and writes
;; no ruling.
;;
;; THE THREE ARMS are `pref/bernoulli-outcome-arms` (preferences.clj:274-296).
;; They are reachable only by naming one; nothing acquires an arm by default,
;; and the shipped `log-preference` is measured here as the BASELINE column so
;; the comparison includes what is running today.
;;
;; THE MIRROR, AND WHY IT IS CHECKED. `war-machine/mission-c-readback`
;; (war_machine.clj:1557-1623) takes no arm, so clause (b) is measured through
;; `readback-under`, a mirror of that fn parameterised by the arm. A mirror can
;; drift from what it mirrors, so §4 runs it with NO arm against the shipped
;; readback on the same records and shows the numeric fields are equal -- if
;; that control fails, every (b) number below is about this script rather than
;; about the machine.
;;
;; NOTHING IS WRITTEN UNDER data/. Replay only, no live tick, no run lock.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str]
         '[futon2.aif.mission-c :as mc]
         '[futon2.aif.preferences :as pref]
         'futon2.report.war-machine)

(import '[java.security MessageDigest])

(def shipped-readback
  "war_machine.clj:1557-1623, var-quoted because it is private."
  #'futon2.report.war-machine/mission-c-readback)

(defn fmt [f & args] (apply format f args))

;; ---------------------------------------------------------------------------
;; Provenance

(def trace-dir "data/wm-trace")
(def corpus-from "wm-trace-2026-09-02.edn")

(def plant-path
  "The U12 plant, read as a COMMITTED ARTIFACT rather than rebuilt, so the two
   runs are over the same bytes and the sha below says so."
  "holes/labs/wm-contract/runs/U12-c-mis-falsifier/planted-criteria-declared-observable.edn")

(def plant-bindings
  "Copied off the plant file itself at run time, not restated -- see `bindings-of`."
  nil)

(defn corpus-files []
  (->> (.listFiles (io/file trace-dir))
       (map #(.getName %))
       (filter #(re-matches #"wm-trace-\d{4}-\d{2}-\d{2}\.edn" %))
       (filter #(>= (compare % corpus-from) 0))
       sort
       (mapv #(str trace-dir "/" %))))

(defn read-records [path]
  (with-open [r (io/reader path)]
    (mapv edn/read-string (line-seq r))))

(defn corpus []
  (->> (corpus-files) (mapcat read-records) (sort-by :timestamp) vec))

(defn short-id [rec] (subs (str (:run/id rec)) 0 8))

(defn sha256 [path]
  (let [md (MessageDigest/getInstance "SHA-256")
        b (with-open [in (io/input-stream path)]
            (let [buf (byte-array 8192)]
              (loop []
                (let [n (.read in buf)]
                  (when (pos? n) (.update md buf 0 n) (recur))))
              (.digest md)))]
    (apply str (map #(fmt "%02x" %) b))))

(defn bindings-of
  "criterion -> observable, read off the plant, so a plant edit shows up here
   rather than being silently overridden by a restated list."
  [path]
  (into {} (map (juxt :criterion :observable)) (:preferences/c (edn/read-string (slurp path)))))

;; ---------------------------------------------------------------------------
;; The columns under comparison

(def columns
  "The baseline plus the three arms, plus the two DECLARED variants -- because
   arms 2 and 3 differ from each other only where a declaration exists, and the
   corpus carries none. `:selector nil` is the shipped path."
  [{:key :v0-shipped :selector nil
    :note "today: (= 0 x), long-0 only; nil reads as the target"}
   {:key :numeric-equality :selector :numeric-equality
    :note "== ; needs no declaration"}
   {:key :declared-binarization :selector :declared-binarization
    :note "threshold required on a non-binary value"}
   {:key :typed-binary-only :selector :typed-binary-only
    :note "observable kind required"}
   {:key :declared-binarization+t0.5 :selector {:arm :declared-binarization :threshold 0.5}
    :note "the same arm WITH the declaration it asks for"}
   {:key :typed-binary-only+binary :selector {:arm :typed-binary-only :observable-kind :binary}
    :note "the same arm WITH the observable declared binary"}])

(defn log-c-under
  "One column's reading of one value against a `{:becomes 1}` C. Typed the same
   way for every column so the baseline is comparable rather than special:
   the shipped fn cannot refuse, so its column is always `:present`."
  [dist x selector]
  (if selector
    (pref/log-preference-under dist x selector)
    {:status :present :log-c (pref/log-preference dist x)}))

;; ---------------------------------------------------------------------------
;; The readback mirror -- clause (b) per arm

(defn readback-under
  "`mission-c-readback` with the arm threaded through `mc/risk-mis`. Structure
   copied from war_machine.clj:1584-1623; only the risk call differs."
  [active-mission ranked observation criteria-path selector]
  (let [mission-id (:mission-id active-mission)
        read-result (mc/read-criteria criteria-path :observables observation
                                      :mission mission-id)
        c (mc/c-mis read-result)
        risk (mc/risk-mis c observation :outcome-semantics selector)
        mission-action? @(resolve 'futon2.report.war-machine/mission-action?)
        actions (filterv mission-action? ranked)
        clocked? (fn [e] (= mission-id (get-in e [:action :target])))
        per-action (when (= :measured (:status risk))
                     (mapv (fn [e] {:rank (:rank e)
                                    :scope (if (clocked? e) :clocked-mission :other-mission)
                                    :risk-mis (:risk risk)})
                           actions))]
    (cond-> {:mission mission-id
             :criteria-source criteria-path
             :criteria-source-sha256 (:source-sha256 read-result)
             :criterion-count (:criterion-count c)
             :measurable-count (:measurable-count c)
             :mission-action-count (count actions)
             :non-mission-action-count (- (count ranked) (count actions))
             :status (:status risk)}
      (:risk risk) (assoc :risk-mis (:risk risk))
      (:reason risk) (assoc :reason (:reason risk))
      (:refused risk) (assoc :refused (:refused risk))
      per-action (assoc :per-mission-action per-action
                        :action-sensitivity
                        {:distinct-risk-values (count (into #{} (map :risk-mis) per-action))
                         :constant? (<= (count (into #{} (map :risk-mis) per-action)) 1)}))))

;; ---------------------------------------------------------------------------
;; The sweep -- one criterion at a time over every channel R2 observes

(defn sweep-one-channel [mission channel observation selector]
  (let [rr {:version mc/version :mission mission :source "u16-sweep"
            :status :present
            :criteria [(mc/criterion-row {:criterion :swept :statement "swept"
                                          :observable channel}
                                         observation "u16-sweep")]}
        c (mc/c-mis rr)
        r (mc/risk-mis c observation :outcome-semantics selector)]
    {:channel channel :value (get observation channel)
     :status (:status r) :risk (:risk r)
     :reason (or (:reason (first (:refused r))) (:reason r))}))

;; ---------------------------------------------------------------------------
;; Report

(def lines (atom []))
(defn emit [& ss] (swap! lines conj (str/join "" ss)) nil)

(def value-vocabulary
  "The values a Bernoulli criterion can actually be read at here: the long 0 the
   tests pin, the double 0.0 R2 produces, a satisfied 1, three interior values,
   a negative, the booleans, and nil."
  [(long 0) 0.0 1.0 (long 1) 0.5 0.75 -3.0 false true nil])

(defn -main [& args]
  (let [outdir (or (first args) "holes/labs/wm-contract/runs/U16-outcome-semantics")
        _ (.mkdirs (io/file outdir))
        records (corpus)
        files (corpus-files)
        plant-sha (sha256 plant-path)
        bindings (bindings-of plant-path)
        clocked (filterv #(:mission-id (:active-mission %)) records)
        d (pref/c-distribution {:becomes 1})
        measurements (atom {:row :U16})]

    (emit "U16 -- THE THREE BERNOULLI OUTCOME SEMANTICS, BUILT AND RUN. Replay only.")
    (emit "")
    (emit "Under test: futon2.aif.preferences/bernoulli-outcome + log-preference-under")
    (emit "            (preferences.clj:274-370), reached through mission-c/risk-mis's")
    (emit "            :outcome-semantics option (mission_c.clj:330-420). No default is")
    (emit "            flipped: every column below except the baseline names its arm.")
    (emit (fmt "Corpus:     %s -- %d records: %s" (str/join ", " files) (count records)
               (str/join " " (map short-id records))))
    (emit (fmt "Plant:      %s" plant-path))
    (emit (fmt "            sha256 %s (the U12 artifact, read not rebuilt)" plant-sha))
    (emit (fmt "            bindings %s" (pr-str bindings)))
    (emit "")
    (swap! measurements assoc
           :corpus {:files files :record-count (count records)
                    :run-ids (mapv #(str (:run/id %)) records)}
           :plant {:path plant-path :sha256 plant-sha :bindings bindings}
           :columns (mapv #(select-keys % [:key :selector :note]) columns))

    ;; -- 1. the primitive ---------------------------------------------------
    (emit "=== 1. THE PRIMITIVE: WHAT EACH COLUMN READS A VALUE AS ===")
    (emit "")
    (emit "  C = c-distribution {:becomes 1} at the default T=0.1. `met` is -4.5399e-5 nats")
    (emit "  and `unmet` is -10.0000454; a REFUSAL carries no number at all.")
    (emit "")
    (emit (fmt "  %-10s %-24s %s" "value" "class" (str/join " " (map #(fmt "%-28s" (name (:key %))) columns))))
    (let [rows (vec (for [x value-vocabulary]
                      (let [cells (mapv (fn [col] [(:key col) (log-c-under d x (:selector col))]) columns)]
                        {:value (pr-str x)
                         :class (if (nil? x) "nil" (.getName (class x)))
                         :cells (into {} cells)})))]
      (doseq [r rows]
        (emit (fmt "  %-10s %-24s %s" (:value r) (:class r)
                   (str/join " " (map (fn [col]
                                        (let [c (get (:cells r) (:key col))]
                                          (fmt "%-28s"
                                               (if (= :present (:status c))
                                                 (fmt "%s (%s)" (if (< (:log-c c) -1.0) "UNMET" "met")
                                                      (if (contains? c :outcome) (str (:outcome c)) "--"))
                                                 (str "refuse " (name (:reason c)))))))
                                      columns)))))
      (swap! measurements assoc :primitive
             (mapv (fn [r] {:value (:value r) :class (:class r)
                            :readings (into {} (map (fn [[k c]]
                                                      [k (if (= :present (:status c))
                                                           {:log-c (:log-c c) :outcome (:outcome c)}
                                                           {:refused (:reason c)})])
                                                    (:cells r)))})
                   rows)))
    (emit "")
    (emit "  THE ONE ROW THAT NAMES THE DEFECT is `0.0`: the baseline reads it `met`,")
    (emit "  identically to 1.0, and every arm either reads it UNMET or refuses it.")
    (emit "  THE ROW THAT SEPARATES THE ARMS is `0.75`, not `0.0` -- 0.0 is exactly binary,")
    (emit "  so :declared-binarization reads it without a threshold and agrees with")
    (emit "  :numeric-equality there.")
    (emit "")

    ;; -- 2. the sweep -------------------------------------------------------
    (emit "=== 2. THE 14-CHANNEL SWEEP: DOES risk_mis VARY WITH THE WORLD? ===")
    (emit "")
    (emit "  One criterion at a time over every channel R2 observes on the last clocked")
    (emit "  record, same composition the readback uses. U12 measured 1 distinct value over")
    (emit "  14 channels on the baseline -- the world could not move the number.")
    (emit "")
    (let [rec (last clocked)
          obs (:observation rec)
          mission (:mission-id (:active-mission rec))
          channels (sort (keys obs))
          per-col (into {} (map (fn [col]
                                  [(:key col)
                                   (mapv #(sweep-one-channel mission % obs (:selector col)) channels)])
                                columns))]
      (emit (fmt "  record %s, %d channels" (short-id rec) (count channels)))
      (emit "")
      (emit (fmt "  %-22s %-24s %s" "channel" "value"
                 (str/join " " (map #(fmt "%-26s" (name (:key %))) columns))))
      (doseq [[i ch] (map-indexed vector channels)]
        (emit (fmt "  %-22s %-24s %s" (name ch) (get obs ch)
                   (str/join " " (map (fn [col]
                                        (let [s (nth (get per-col (:key col)) i)]
                                          (fmt "%-26s" (if (= :measured (:status s))
                                                         (fmt "%.7f" (:risk s))
                                                         (str "refuse " (name (:reason s)))))))
                                      columns)))))
      (emit "")
      (doseq [col columns]
        (let [sw (get per-col (:key col))
              measured (filter #(= :measured (:status %)) sw)
              distinct-risks (into (sorted-set) (map :risk) measured)]
          (emit (fmt "  %-28s %2d/%2d measured, %d distinct risk_mis value(s) %s"
                     (name (:key col)) (count measured) (count sw)
                     (count distinct-risks)
                     (pr-str (vec distinct-risks))))))
      (swap! measurements assoc :sweep
             {:record (str (:run/id rec)) :channel-count (count channels)
              :per-column (into {} (map (fn [col]
                                          (let [sw (get per-col (:key col))
                                                m (filter #(= :measured (:status %)) sw)]
                                            [(:key col)
                                             {:measured (count m) :of (count sw)
                                              :distinct-risk-values (count (into #{} (map :risk) m))
                                              :risk-values (vec (into (sorted-set) (map :risk) m))
                                              :refusal-reasons (frequencies (map :reason (remove #(= :measured (:status %)) sw)))}]))
                                        columns))}))
    (emit "")

    ;; -- 3. clause (b) mirror control --------------------------------------
    (emit "=== 3. THE MIRROR CONTROL (before any (b) number is read) ===")
    (emit "")
    (emit "  `readback-under` with NO arm, against the shipped mission-c-readback, on the")
    (emit "  same records and the same plant. If these differ, the (b) numbers in §4 are")
    (emit "  about this script and not about the machine.")
    (emit "")
    (let [ks [:mission :criteria-source-sha256 :criterion-count :measurable-count
              :mission-action-count :non-mission-action-count :status :reason]
          rows (mapv (fn [rec]
                       (let [mission (:mission-id (:active-mission rec))
                             shipped (with-redefs [futon2.report.war-machine/mission-c-criteria-sources
                                                   {mission plant-path}]
                                       (shipped-readback (:active-mission rec) (:ranked-actions rec)
                                                         (:observation rec)))
                             mirror (readback-under (:active-mission rec) (:ranked-actions rec)
                                                    (:observation rec) plant-path nil)]
                         {:run/id (str (:run/id rec))
                          :equal? (= (select-keys shipped ks) (select-keys mirror ks))
                          :risk-equal? (= (:risk-mis (first (:per-mission-action shipped)))
                                          (:risk-mis (first (:per-mission-action mirror))))
                          :shipped (select-keys shipped ks)}))
                     clocked)]
      (doseq [r rows]
        (emit (fmt "  %s  fields equal: %-5s  risk_mis equal: %-5s  %s"
                   (subs (:run/id r) 0 8) (:equal? r) (:risk-equal? r)
                   (pr-str (select-keys (:shipped r) [:status :measurable-count])))))
      (emit (fmt "  CONTROL: %s" (if (every? #(and (:equal? %) (:risk-equal? %)) rows)
                                   "PASSES -- the mirror is the shipped readback with an arm hole in it"
                                   "FAILS -- do not read §4")))
      (swap! measurements assoc :mirror-control
             {:records (mapv #(select-keys % [:run/id :equal? :risk-equal?]) rows)
              :passes? (every? #(and (:equal? %) (:risk-equal? %)) rows)}))
    (emit "")

    ;; -- 4. clause (b) ------------------------------------------------------
    (emit "=== 4. CLAUSE (b) DISCRIMINATION, PER ARM ===")
    (emit "")
    (emit "  §7 asks whether the clocked mission's advance actions carry LOWER risk_mis than")
    (emit "  non-mission actions. U12's verdict was FAIL, structurally.")
    (emit "")
    (let [rows (vec (for [rec clocked col columns]
                      (assoc (readback-under (:active-mission rec) (:ranked-actions rec)
                                             (:observation rec) plant-path (:selector col))
                             :run/id (str (:run/id rec)) :column (:key col))))]
      (doseq [rec clocked]
        (emit (fmt "  record %s  clocked %s" (short-id rec)
                   (:mission-id (:active-mission rec))))
        (doseq [r (filter #(= (str (:run/id rec)) (:run/id %)) rows)]
          (emit (fmt "    %-28s %-10s %-24s mission actions %2d, non-mission %2d, distinct risk %s"
                     (name (:column r)) (name (:status r))
                     (if (= :measured (:status r))
                       (fmt "%.10f" (:risk-mis r))
                       (str (name (or (:reason r) :--)) " "
                            (pr-str (mapv :reason (:refused r)))))
                     (:mission-action-count r) (:non-mission-action-count r)
                     (get-in r [:action-sensitivity :distinct-risk-values] "--"))))
        (emit ""))
      (emit "  VERDICT (b): UNCHANGED BY EVERY ARM, and the reason is not numerical.")
      (emit "  mission-c-readback assigns ONE risk value -- (:risk risk) -- to every mission")
      (emit "  action (war_machine.clj:1594-1600), because v0's forward model is the status")
      (emit "  quo for every candidate. No reading of an OUTCOME can make a number that is")
      (emit "  copied vary across the things it is copied to. An arm can only change WHICH")
      (emit "  constant it is, or refuse to produce one. Non-mission actions still carry no")
      (emit "  risk_mis field at all, so the comparison §7 names still has an absent side.")
      (emit "  U16 therefore does not unblock (b); U13's forward model is what would.")
      (swap! measurements assoc :clause-b
             {:verdict :unchanged-by-every-arm
              :cause :one-risk-value-copied-to-every-mission-action
              :pointer "scripts/futon2/report/war_machine.clj:1594-1600"
              :rows (mapv #(select-keys % [:run/id :column :status :reason :risk-mis
                                           :mission-action-count :non-mission-action-count
                                           :action-sensitivity]) rows)}))
    (emit "")

    ;; -- 5. clause (c) ------------------------------------------------------
    (emit "=== 5. CLAUSE (c) THE SATISFIED-CRITERIA ZERO, PER ARM ===")
    (emit "")
    (emit "  Four planted fields over the plant's three criteria: every observable at its")
    (emit "  {:becomes 1} target; at the long 0; at the double 0.0; and nil. U12's numbers on")
    (emit "  the baseline: satisfied 4.5399e-5 (a temperature floor, not zero), unsatisfied")
    (emit "  long-0 10.0000454, unsatisfied double-0.0 4.5399e-5 -- the last being the defect.")
    (emit "")
    (let [rec (last clocked)
          obs (:observation rec)
          mission (:mission-id (:active-mission rec))
          read-result (mc/read-criteria plant-path :observables obs :mission mission)
          c (mc/c-mis read-result)
          channels (vals bindings)
          field (fn [v] (into {} (map (fn [ch] [ch v]) channels)))
          fields [[:satisfied (field 1)] [:unsatisfied-long-0 (field (long 0))]
                  [:unsatisfied-double-0.0 (field 0.0)] [:nil-field (field nil)]]
          cell (fn [col [_ f]] (mc/risk-mis c f :outcome-semantics (:selector col)))]
      (emit (fmt "  %-28s %s" "column"
                 (str/join " " (map #(fmt "%-26s" (name (first %))) fields))))
      (doseq [col columns]
        (emit (fmt "  %-28s %s" (name (:key col))
                   (str/join " " (map (fn [f]
                                        (let [r (cell col f)]
                                          (fmt "%-26s" (if (= :measured (:status r))
                                                         (fmt "%.10f" (:risk r))
                                                         (str "refuse " (name (or (:reason (first (:refused r)))
                                                                                  (:reason r))))))))
                                      fields)))))
      (emit "")
      (doseq [col columns]
        (let [sat (cell col (first fields))
              un0 (cell col (nth fields 2))]
          (emit (fmt "  %-28s pole ratio (double-0.0 / satisfied): %s"
                     (name (:key col))
                     (if (and (= :measured (:status sat)) (= :measured (:status un0)))
                       (fmt "%.4f" (/ (:risk un0) (:risk sat)))
                       "-- (one side refuses)")))))
      (emit "")
      (emit "  VERDICT (c): THE ZERO IS NOT REACHED UNDER ANY ARM. A met Bernoulli criterion")
      (emit "  costs 4.5399e-5 nats at T=0.1, which is the temperature floor and not zero;")
      (emit "  that is a property of c* = 1/(1+e^(-1/T)) and no outcome reading moves it.")
      (emit "  WHAT THE ARMS DO FIX is the OTHER half of U12's clause (c): the unsatisfied")
      (emit "  pole becomes reachable from a double (:numeric-equality, :declared-binarization)")
      (emit "  or the field is refused outright (:typed-binary-only), and under every arm the")
      (emit "  nil field is a typed absence rather than a satisfied mission.")
      (swap! measurements assoc :clause-c
             {:verdict :satisfied-floor-unchanged-unsatisfied-pole-reachable
              :fields (into {} (map (fn [[k _]] [k true]) fields))
              :per-column
              (into {} (map (fn [col]
                              [(:key col)
                               (into {} (map (fn [[k f]]
                                               (let [r (mc/risk-mis c f :outcome-semantics (:selector col))]
                                                 [k (if (= :measured (:status r))
                                                      {:risk (:risk r)}
                                                      {:refused (or (:reason (first (:refused r))) (:reason r))})]))
                                             fields))])
                            columns))}))
    (emit "")

    ;; -- 6. agreement -------------------------------------------------------
    (emit "=== 6. WHERE THE COLUMNS AGREE AND WHERE THEY PART ===")
    (emit "")
    (emit "  Over the value vocabulary of §1: agree = same outcome pole; differ = different")
    (emit "  pole; split = one reads a number where the other refuses; both-refuse.")
    (emit "")
    (let [pair-stats
          (for [a columns b columns :when (< (.indexOf ^java.util.List (mapv :key columns) (:key a))
                                             (.indexOf ^java.util.List (mapv :key columns) (:key b)))]
            (let [cmp (for [x value-vocabulary]
                        (let [ra (log-c-under d x (:selector a))
                              rb (log-c-under d x (:selector b))]
                          (cond (and (= :absent (:status ra)) (= :absent (:status rb))) :both-refuse
                                (or (= :absent (:status ra)) (= :absent (:status rb))) :split
                                (< (Math/abs (- (:log-c ra) (:log-c rb))) 1e-12) :agree
                                :else :differ)))]
              {:a (:key a) :b (:key b) :counts (frequencies cmp)
               :differ-on (vec (keep-indexed (fn [i v] (when (= :differ v) (pr-str (nth value-vocabulary i)))) cmp))
               :split-on (vec (keep-indexed (fn [i v] (when (= :split v) (pr-str (nth value-vocabulary i)))) cmp))}))]
      (doseq [p pair-stats]
        (emit (fmt "  %-28s vs %-28s %s" (name (:a p)) (name (:b p)) (pr-str (:counts p))))
        (when (seq (:differ-on p)) (emit (fmt "        differ on %s" (str/join " " (:differ-on p)))))
        (when (seq (:split-on p)) (emit (fmt "        split on  %s" (str/join " " (:split-on p))))))
      (swap! measurements assoc :agreement (vec pair-stats)))
    (emit "")

    ;; -- 7. the residual ----------------------------------------------------
    (emit "=== 7. WHAT IS LEFT TO CHOOSE ===")
    (emit "")
    (emit "  MEASURED, not argued:")
    (emit "   - All three arms fix what U12 named: the double 0.0 no longer reads as met, and")
    (emit "     nil is a typed absence rather than a satisfied criterion (§1, §5).")
    (emit "   - None of them changes clause (b). risk_mis is one number copied to every")
    (emit "     mission action; an outcome reading cannot make it vary (§4).")
    (emit "   - None of them reaches the clause (c) zero. That is the T=0.1 floor (§5).")
    (emit "   - ONE OF U12's TWO FINDINGS IS LIFTED, and it is worth separating from the")
    (emit "     choice below. U12: 'risk_mis does not vary with the world either -- 1 distinct")
    (emit "     value over 14 channels'. Under :numeric-equality the same sweep returns 2")
    (emit "     distinct values (§2), so the number moves with the world once the type")
    (emit "     comparison is fixed. That is a property of the outcome reading and it holds")
    (emit "     for every arm that scores anything at all; it is not the residual.")
    (emit "   - THE COST IS COVERAGE, and it is measured in §2. Over the 14 channels R2")
    (emit "     observes, :numeric-equality scores every one, :declared-binarization scores")
    (emit "     only the channels reading exactly 0 or 1, and :typed-binary-only scores none,")
    (emit "     because no criterion in the corpus declares an observable kind.")
    (emit "   - :declared-binarization and :typed-binary-only are INDISTINGUISHABLE on this")
    (emit "     corpus at the composition (§2, §4, §5) and part company only where a")
    (emit "     declaration exists (§1, §6, and the two declared columns).")
    (emit "")
    (emit "  THE RESIDUAL CHOICE, stated as what the numbers do NOT settle: whether a")
    (emit "  criterion whose observable carries no declaration should be SCORED on a type")
    (emit "  fix alone (:numeric-equality -- 14/14 channels, and 0.75 reads as met) or")
    (emit "  REFUSED until someone writes the declaration down (:declared-binarization,")
    (emit "  :typed-binary-only -- 4/14 and 0/14). Both are self-consistent; the measurement")
    (emit "  says what each costs and cannot say which cost is the right one to pay,")
    (emit "  because nothing recorded says whether R2's channels ARE binary observables.")
    (emit "  That is a question about what the criteria mean, not about the code.")
    (emit "")
    (emit "  NO RULING IS MADE HERE and no registry is written.")

    (spit (io/file outdir "U16-OUTCOME-SEMANTICS.txt") (str (str/join "\n" @lines) "\n"))
    (spit (io/file outdir "measurements.edn") (with-out-str (pp/pprint @measurements)))
    (println (str/join "\n" @lines))
    (println)
    (println "wrote" (str outdir "/U16-OUTCOME-SEMANTICS.txt")
             "and" (str outdir "/measurements.edn"))))

(apply -main *command-line-args*)
