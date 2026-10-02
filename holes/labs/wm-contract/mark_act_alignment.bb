#!/usr/bin/env bb
;; Align the operator's reply-proforma marks with the War Machine's click acts.
;; Run: bb holes/labs/wm-contract/mark_act_alignment.bb   (from futon2)
;;
;; Three sources, none edited here:
;;   wm-click-acts.edn (this directory)  act -> stage, R-node, :nearest-intent.
;;     This is the alignment followed: a mark sits where the acts it is the
;;     nearest intent of sit.
;;   futon3c emacs/xiaoxiang-preview.el, xiaoxiang-mark-keys  mark -> intent,
;;     PBASE stage as the operator's C-c ; menu groups it.
;;   p4ng empirics-futon/control-stages.edn  R-node -> stage, band, label.
;; Prints a markdown report: the table by mark, then the three audit lists.
(require '[clojure.edn :as edn]
         '[clojure.string :as str])

(def root "/home/joe/code")
(def acts-file (str root "/futon2/holes/labs/wm-contract/wm-click-acts.edn"))
(def hydra-file (str root "/futon3c/emacs/xiaoxiang-preview.el"))
(def stages-file (str root "/p4ng/empirics-futon/control-stages.edn"))

(defn hydra-marks
  "The rows of xiaoxiang-mark-keys, in menu order: [key intent mark stage]."
  []
  (let [text (slurp hydra-file)
        start (str/index-of text "(defconst xiaoxiang-mark-keys")
        end (str/index-of text "\"Key, intent, mark" start)]
    (when-not (and start end) (throw (ex-info "xiaoxiang-mark-keys not found" {:file hydra-file})))
    (vec (for [[_ k intent mark stage]
               (re-seq #"\(\"([^\"]+)\" \"([^\"]+)\" \"([^\"]+)\" (\w+)\)" (subs text start end))]
           {:key k :intent (keyword intent) :mark mark
            :stage (keyword (str/upper-case stage))}))))

(def marks (hydra-marks))
(def acts (:acts (edn/read-string (slurp acts-file))))
(def nodes (into {} (map (juxt (comp keyword :node) identity))
                 (:nodes (edn/read-string (slurp stages-file)))))
(def mark-of (into {} (map (juxt :intent :mark)) marks))

(defn node-stage [r] (some-> (get-in nodes [r :stage]) keyword))
(defn act-name [a] (name (:act/id a)))

(defn act-cell [a]
  (let [r (:control/r-node a)
        ns-stage (node-stage r)]
    (str (act-name a) " " (name r) " " (name (:loop/stage a))
         (when (and ns-stage (not= ns-stage (:loop/stage a)))
           (str " (registry: " (name ns-stage) ")")))))

(defn agreement [m as]
  (cond (empty? as) "no click act"
        (every? #(= (:stage m) (:loop/stage %)) as) "same stage"
        :else (str "acts at " (str/join ", " (distinct (map (comp name :loop/stage) as))))))

(println "| mark | intent | menu stage | click acts naming it (R-node, act stage) | stage |")
(println "|---|---|---|---|---|")
(doseq [m marks
        :let [as (filter #(= (:intent m) (:nearest-intent %)) acts)]]
  (println (str "| " (:mark m) " | " (name (:intent m)) " | " (name (:stage m)) " | "
                (if (seq as) (str/join "; " (map act-cell as)) "-") " | "
                (agreement m as) " |")))

(println "\n**Click acts with no nearest intent**\n")
(doseq [a acts :when (= :none (:nearest-intent a))]
  (println (str "- " (act-cell a) " — " (:description a))))

(println "\n**Intents in the acts file that are not menu marks**\n")
(let [unknown (remove #(or (= :none %) (mark-of %)) (distinct (map :nearest-intent acts)))]
  (println (if (seq unknown) (str/join ", " (map name unknown)) "none")))

(println "\n**Loop and assurance nodes no click act is tagged with**\n")
(let [used (set (map :control/r-node acts))]
  (doseq [[stage rs] (group-by (comp :stage val) (remove (comp used key) nodes))]
    (println (str "- " stage ": "
                  (str/join ", " (map (fn [[r n]] (str (name r) " " (:label n))) rs))))))

(println "\n**Act stage differs from the node registry**\n")
(let [bad (filter #(let [s (node-stage (:control/r-node %))] (and s (not= s (:loop/stage %)))) acts)]
  (if (seq bad)
    (doseq [a bad] (println (str "- " (act-cell a))))
    (println "none")))
