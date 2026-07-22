;;; z3a_score.clj — the Z3a preregistered scorer (M-zaif-harness z3-prereg.md).
;;;
;;; Committed BEFORE activation per the prereg's analysis-provenance rule.
;;; Reads ONLY the evidence store; re-derives every recorded arm choice from
;;; its recorded :inputs-snapshot via the real controller kernel (the B1
;;; --check discipline) so recorded decisions are verified, not trusted.
;;;
;;; Run under futon3c's own (cached) classpath — a fresh -Sdeps local/root
;;; classpath computation for the full futon3c tree takes >5 min; this way
;;; is ~1 min:
;;;
;;;   cd /home/joe/code/futon3c
;;;   clojure -M ../futon2/holes/labs/M-zaif-harness/z3a_score.clj \
;;;     [--base http://127.0.0.1:7070] [--since ISO] [--out z3a-report.edn]
;;;     [--min-operator-turns 3] [--max-sessions 20] [--dry-run]
;;;
;;; Without --since, the activation marker (tag zaif-z3a-activation) is looked
;;; up in the store; if absent the run REFUSES unless --dry-run is given
;;; (a dry run is informational only, NOT the preregistered analysis).
;;;
;;; Judgment operationalization (frozen in z3-prereg.edn):
;;;   next operator chat-turn in the session after the round:
;;;     tag "correction" (or adjudicated correction) -> :correction
;;;     tag "approval"                               -> :approval
;;;     otherwise                                    -> :none (acceptance,
;;;       bounded by the marking-recall audit)
;;;   ideal arms: correction -> #{:ask :yield}; acceptance -> #{:act}.
;;;   A pair scores a point for the constant whose arm matches the ideal when
;;;   exactly one of the two matches; both-match / neither-match = tie.
;;;   Exact two-sided sign test on the points at p0 = 0.5.

(ns z3a-score
  (:require [clojure.data.json :as json]
            [clojure.pprint :as pprint]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [futon3c.agents.zaif-controller :as zaif]))

;; ---------------------------------------------------------------------------
;; Store access (read-only, paged, client-side filtering)
;; ---------------------------------------------------------------------------

(defn- fetch-json [url]
  (-> (slurp url) (json/read-str :key-fn keyword)))

(defn- fetch-page [base {:keys [tag session-id before limit]}]
  (let [url (str base "/api/alpha/evidence?limit=" (or limit 200)
                 (when tag (str "&tag=" tag))
                 (when session-id (str "&session-id=" session-id))
                 (when before (str "&before=" before)))]
    (:entries (fetch-json url))))

(defn fetch-all
  "Page a server-side-filtered query newest-first until exhausted or :at <
   since. Always pass a tag or session-id filter — an unfiltered sweep of
   the full store is hours of wall clock (H1 paging lesson). Bails with a
   warning if a page makes no progress (e.g. `before` unsupported)."
  [base params since]
  (loop [acc [] before nil]
    (let [page (fetch-page base (assoc params :before before))
          oldest (some-> (last page) :evidence/at)]
      (cond
        (empty? page) acc
        (= before oldest) (do (binding [*out* *err*]
                                (println "WARN: pager made no progress at" before
                                         "- results may be truncated"))
                              acc)
        :else
        (let [keep (filterv #(or (nil? since)
                                 (neg? (compare since (:evidence/at %))))
                            page)
              acc' (into acc keep)]
          (if (and since oldest (neg? (compare oldest since)))
            acc'
            (recur acc' oldest)))))))

(defn- has-tag? [entry tag]
  (some #(= tag (name %)) (map name (or (:evidence/tags entry) []))))

(defn find-activation-at
  "Earliest :at of an activation-tagged entry, via one server-side tag query."
  [base activation-tag]
  (some->> (fetch-page base {:tag activation-tag :limit 10})
           (map :evidence/at)
           sort
           first))

;; ---------------------------------------------------------------------------
;; Pairing + determinism
;; ---------------------------------------------------------------------------

(defn arm-choice? [entry]
  (and (has-tag? entry "zaif")
       (= "zaif-arm-choice" (some-> entry :evidence/body :event name))))

(defn operator-turn? [entry]
  (and (= "chat-turn" (some-> entry :evidence/body :event name))
       (= "user" (some-> entry :evidence/body :role name))))

(defn- body-inputs [entry]
  (walk/keywordize-keys (get-in entry [:evidence/body :inputs-snapshot])))

(defn rederive
  "Re-run decide() from the recorded snapshot + constant. Returns {:arm kw
   :match? bool} vs the recorded arm."
  [entry]
  (let [inputs (body-inputs entry)
        constant (get-in entry [:evidence/body :constant])
        ;; JSON round-trip stringifies the gamma map's mission keys — decide's
        ;; gamma-for-mission looks up by string, keyword, and str, so
        ;; keywordize-keys' keywordization of them must be undone.
        gamma (into {} (map (fn [[k v]] [(name k) v]) (:gamma inputs)))
        decision (zaif/decide (assoc inputs
                                     :gamma gamma
                                     :constants-override
                                     {:operator-attention-cost constant}))
        recorded-arm (some-> entry :evidence/body :arm name)]
    {:recorded recorded-arm
     :rederived (name (:arm decision))
     :match? (= recorded-arm (name (:arm decision)))}))

(defn pairs-by-key
  "Group arm-choice entries into {[session pairing-key] {shipped e, sweep e}}."
  [arm-entries]
  (reduce (fn [acc e]
            (let [k [(:evidence/session-id e)
                     (get-in e [:evidence/body :pairing-key])]
                  label (some-> (get-in e [:evidence/body :constant-label]) name)]
              (if (and (second k) label)
                (assoc-in acc [k label] e)
                acc)))
          {} arm-entries))

;; ---------------------------------------------------------------------------
;; Judgment
;; ---------------------------------------------------------------------------

(defn judge-after
  "Realized operator judgment: first operator turn in SESSION-TURNS (sorted
   by :at) strictly after AT. => :correction | :approval | :none | :unscored"
  [session-turns at]
  (if-let [turn (first (filter #(pos? (compare (:evidence/at %) at)) session-turns))]
    (cond
      (has-tag? turn "correction") :correction
      (has-tag? turn "approval") :approval
      :else :none)
    :unscored))

(def ideal-arms {:correction #{"ask" "yield"}
                 :approval #{"act"}
                 :none #{"act"}})

(defn score-pair [shipped sweep judgment]
  (let [ideal (get ideal-arms judgment)
        s-arm (some-> shipped :evidence/body :arm name)
        w-arm (some-> sweep :evidence/body :arm name)
        s-hit (contains? (or ideal #{}) s-arm)
        w-hit (contains? (or ideal #{}) w-arm)]
    {:shipped-arm s-arm :sweep-arm w-arm :judgment judgment
     :divergent? (not= s-arm w-arm)
     :point (cond (= s-hit w-hit) :tie
                  w-hit :sweep
                  :else :shipped)}))

(defn sign-test-p
  "Exact two-sided binomial sign test: N informative pairs, K sweep points."
  [n k]
  (if (zero? n)
    nil
    (let [choose (fn [n k] (reduce (fn [acc i] (/ (* acc (- (inc n) i)) i))
                                   1N (range 1 (inc k))))
          pmf (fn [i] (* (choose n i) (Math/pow 0.5 n)))
          k* (max k (- n k))
          tail (reduce + (map pmf (range k* (inc n))))]
      (min 1.0 (double (* 2 tail))))))

;; ---------------------------------------------------------------------------
;; Main
;; ---------------------------------------------------------------------------

(defn- parse-args [args]
  (loop [m {:base "http://127.0.0.1:7070" :out "z3a-report.edn"
            :min-operator-turns 3 :max-sessions 20
            :activation-tag "zaif-z3a-activation"}
         [a b & more :as all] (seq args)]
    (cond
      (nil? all) m
      (= a "--dry-run") (recur (assoc m :dry-run true) (next all))
      :else (recur (assoc m (keyword (subs a 2))
                          (if (re-matches #"\d+" (str b)) (parse-long b) b))
                   more))))

(defn -main [& args]
  (let [{:keys [base since out min-operator-turns max-sessions
                activation-tag dry-run]} (parse-args args)
        activation-at (or since (find-activation-at base activation-tag))
        _ (when (and (nil? activation-at) (not dry-run))
            (binding [*out* *err*]
              (println "REFUSING: no activation marker (tag" activation-tag
                       ") and no --since. Use --dry-run for an informational pass."))
            (System/exit 2))
        arm-entries (filterv arm-choice?
                             (fetch-all base {:tag "zaif"} activation-at))
        ;; operator turns fetched per cohort-candidate session (server-side
        ;; session-id filter), never as a store-wide sweep
        sids (distinct (map :evidence/session-id arm-entries))
        op-turns (into {}
                       (for [sid sids]
                         [sid (->> (fetch-all base {:session-id sid} activation-at)
                                   (filterv operator-turn?)
                                   (sort-by :evidence/at)
                                   vec)]))
        pairs (pairs-by-key arm-entries)
        ;; cohort: sessions with >= min operator turns, capped chronologically
        session-first-at (reduce (fn [acc [[sid _] m]]
                                   (let [at (some-> (or (get m "shipped") (get m "sweep"))
                                                    :evidence/at)]
                                     (update acc sid #(if (and % (neg? (compare % at))) % at))))
                                 {} pairs)
        cohort-sids (->> session-first-at
                         (filter (fn [[sid _]]
                                   (>= (count (get op-turns sid [])) min-operator-turns)))
                         (sort-by val)
                         (map key)
                         (take max-sessions)
                         set)
        complete (for [[[sid pk] m] (sort-by (comp :evidence/at #(or (get % "shipped") (get % "sweep")) val) pairs)
                       :when (and (contains? cohort-sids sid)
                                  (get m "shipped") (get m "sweep"))]
                   (let [shipped (get m "shipped") sweep (get m "sweep")
                         at (:evidence/at shipped)
                         judgment (judge-after (get op-turns sid []) at)]
                     (merge {:session sid :pairing-key pk :at at
                             :digest-match? (= (get-in shipped [:evidence/body :inputs-digest])
                                               (get-in sweep [:evidence/body :inputs-digest]))}
                            (score-pair shipped sweep judgment))))
        divergent (filterv :divergent? complete)
        informative (filterv #(and (:divergent? %)
                                   (not= :unscored (:judgment %))
                                   (not= :tie (:point %)))
                             complete)
        k-sweep (count (filter #(= :sweep (:point %)) informative))
        determinism (mapv rederive arm-entries)
        report {:params {:base base :activation-at activation-at :dry-run (boolean dry-run)
                         :min-operator-turns min-operator-turns :max-sessions max-sessions}
                :counts {:arm-choice-entries (count arm-entries)
                         :cohort-sessions (count cohort-sids)
                         :complete-pairs (count complete)
                         :divergent-pairs (count divergent)
                         :informative-pairs (count informative)}
                :divergence-rate (when (pos? (count complete))
                                   (double (/ (count divergent) (count complete))))
                :moot? (and (pos? (count complete))
                            (< (double (/ (count divergent) (count complete))) 0.05))
                :sign-test {:n (count informative) :k-sweep k-sweep
                            :p (sign-test-p (count informative) k-sweep)}
                :determinism {:checked (count determinism)
                              :mismatches (vec (remove :match? determinism))}
                :pairs (vec complete)}]
    (spit out (with-out-str (pprint/pprint report)))
    (println "Z3a" (if dry-run "DRY RUN (informational)" "preregistered analysis"))
    (println "  cohort sessions:" (count cohort-sids)
             "| pairs:" (count complete)
             "| divergent:" (count divergent)
             (str "(rate " (:divergence-rate report) ")"))
    (println "  informative:" (count informative) "| sweep points:" k-sweep
             "| sign-test p:" (get-in report [:sign-test :p]))
    (println "  determinism mismatches:" (count (get-in report [:determinism :mismatches])))
    (when (:moot? report)
      (println "  VERDICT GATE: divergence < 5% — constants comparison MOOT; inputs are binding."))
    (println "  report ->" out)))

;; No-op when loaded (load-file / -i) for testing; runs as a -M script.
(when (seq *command-line-args*)
  (apply -main *command-line-args*))
