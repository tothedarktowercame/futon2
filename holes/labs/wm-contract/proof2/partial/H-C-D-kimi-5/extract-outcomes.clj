;; scripts/wm/extract-outcomes.clj — mission outcomes extractor (H-C-D)
;;
;; Usage: clojure -M scripts/wm/extract-outcomes.clj <mission.md>
;;
;; Read-only. Mission path in, EDN out (:wm/mission-outcomes-extract-v1):
;; phase exit criteria (each "**Exit criterion:**" sentence, cued to its
;; lines), the IDENTIFY exit scope sentence, instance sections with any
;; want tokens the TEXT states (backticked first column of a want table),
;; the preference/cost-ordering section, and typed absences for what the
;; text does not state. The mission text never states a served-by
;; relation, so none is emitted -- linking wants to outcomes is a
;; judgement, and this script does not invent it.
;;
;; Every cue quote is verified to resolve at its cue lines in the text;
;; the script refuses its own output (exit 1, typed ex-data) if one does
;; not. Shapes follow futon2.aif.mission-criteria (criteria cued to lines)
;; and the D11 ask channel (replies cued to lines).

(ns extract-outcomes
  (:require [clojure.string :as str]
            [clojure.pprint :as pp]))

(defn- fail! [reason data]
  (throw (ex-info (str "extract-outcomes: " (name reason))
                  (assoc data :error :extract-outcomes :reason reason))))

(defn- sentence-cue
  "Line span [start end] (1-based, inclusive) covering the sentence that
  starts on START line: extends while the next line is non-blank and not
  a heading/table row."
  [lines start]
  (let [n (count lines)]
    [start (loop [i start]
             (let [l (nth lines i nil)]
               (if (and (< i n) l
                        (not (str/blank? l))
                        (not (str/starts-with? l "#"))
                        (not (str/starts-with? l "|")))
                 (recur (inc i))
                 (dec i))))]))

(defn- section-lines
  "1-based [start end] of the section whose header matches RE, ending at
  the next header of same-or-higher level (or EOF)."
  [lines re]
  (let [start (first (keep-indexed (fn [i l] (when (re-find re l) i)) lines))]
    (when start
      (let [level (count (take-while #(= % \#) (nth lines start)))
            stop-re (re-pattern (str "^#{1," level "} "))]
        [(inc start)
         (or (first (keep-indexed (fn [i l]
                                    (when (and (> i start) (re-find stop-re l)) i))
                                  lines))
             (count lines))]))))

(defn- verify-cue!
  "A cue's quote must be a prefix of the joined text at its lines."
  [lines what {:keys [lines [s e] quote] :as cue}]
  (let [text (str/join "\n" (subvec lines (dec s) e))]
    (when-not (str/starts-with? text quote)
      (fail! :cue-does-not-resolve
             {:what what :cue cue :text-at-cue (subs text 0 (min 80 (count text)))})))
  cue)

(defn -main [& [mission-path]]
  (when-not mission-path
    (fail! :usage {:usage "clojure -M scripts/wm/extract-outcomes.clj <mission.md>"}))
  (let [text (slurp mission-path)
        lines (str/split-lines text)
        cue (fn [what s e quote] (verify-cue! lines what {:lines [s e] :quote quote}))

        ;; phase exit criteria
        exit-lines (keep-indexed (fn [i l]
                                   (when (str/includes? l "**Exit criterion:**") (inc i)))
                                 lines)
        phase-of (fn [line-n]
                   (->> (subvec lines 0 (dec line-n))
                        (keep-indexed (fn [i l] (when (re-find #"^## " l) [i l])))
                        last second
                        (re-find #"^## ([A-Z]+)")
                        second str/lower-case))
        exits (mapv (fn [line-n]
                      (let [s line-n
                            e (second (sentence-cue lines (dec line-n)))
                            quote (nth lines (dec s))]
                        {:id (keyword "exit" (phase-of line-n))
                         :whose (str "phase " (str/upper-case (phase-of line-n)))
                         :cue (cue (str "exit criterion at line " line-n)
                                   s e quote)}))
                    exits)

        ;; IDENTIFY exit scope sentence (the section has no **Exit criterion:** line)
        identify (section-lines lines #"^## IDENTIFY exit")
        identify-outcome
        (when identify
          (let [[s e] identify
                quote (nth lines (dec s))]
            {:id :exit/identify
             :whose "the mission's exit as scoped by IDENTIFY"
             :cue (cue "IDENTIFY exit scope sentence" s e quote)}))

        ;; capability sentence
        cap-line (first (keep-indexed (fn [i l]
                                        (when (str/includes? l "What this mission develops") (inc i)))
                                      lines))
        capability
        (when cap-line
          (let [[_ e] (sentence-cue lines (dec cap-line))]
            {:id :capability
             :whose "the mission's (its own stated subject)"
             :cue (cue "capability sentence" cap-line e
                       (nth lines (dec cap-line)))}))

        ;; instances: "### N. Title" under "## The eight instances"
        instance-headers
        (->> (keep-indexed
              (fn [i l]
                (when-let [[_ n title] (re-find #"^### (\d+)\.\s+(.*)$" l)]
                  {:n (Long/parseLong n) :title title :line (inc i)}))
              lines)
             (filter #(<= 1 (:n %) 8))
             vec)

        ;; want tokens the TEXT states: backticked first column of a
        ;; markdown want table (only instance 4's INSTANTIATE rows)
        want-rows
        (->> (keep-indexed
              (fn [i l]
                (when-let [[_ token] (re-find #"^\| `([^`]+)` \|" l)]
                  {:token (keyword token) :line (inc i)}))
              lines)
             vec)
        instances
        (mapv (fn [{:keys [n title]}]
                {:n n :title title
                 :wants (if (= n 4)
                          (mapv (fn [{:keys [token line]}]
                                  {:token token :cue {:lines [line line]}})
                                want-rows)
                          {:status :absent
                           :reason :no-want-tokens-stated-in-mission-text})})
              (filter #(<= 4 (:n %) 8) instance-headers))

        ;; preference: the cost-ordering section
        pref (section-lines lines #"^## Cost ordering")
        preference (if pref
                     {:status :stated :kind :ordinal
                      :cue (cue "cost ordering section" (first pref)
                                (min (second pref) (+ (first pref) 4))
                                (nth lines (dec (first pref))))}
                     {:status :absent :reason :no-cost-ordering-section})

        out {:schema :wm/mission-outcomes-extract-v1
             :mission mission-path
             :outcomes (vec (concat (when identify-outcome [identify-outcome])
                                    exits
                                    (when capability [capability])))
             :instances instances
             :preference preference
             :absences [{:what :numeric-weights :status :absent
                         :reason :ordering-is-ordinal-not-numeric}
                        {:what :instance-5-8-want-tokens :status :absent
                         :reason :stated-in-proto-cascades-not-in-mission-text}
                        {:what :served-by-relation :status :absent
                         :reason :not-stated-in-mission-text}]}]
    (pp/pprint out)))

(apply -main *command-line-args*)
