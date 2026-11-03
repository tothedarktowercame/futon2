#!/usr/bin/env bb
;; argue_selection_record.bb RECORD.edn OUT.html FRAME.edn FRAME.json
;;
;; Read a retained outer-selection record as an argument: why this problem and
;; not that one. Each scored cascade class is an argument for its pick, with
;; grounds (the facts its steps read), warrants (the conclusions of its
;; patterns) and a G. Attacks are computed, not written: one argument attacks
;; another when its gates exclude the other's pick, or when its own order
;; prefers the other's pick. G is the preference that resolves the attacks
;; (preference-based argumentation: an attack from a less-preferred argument on
;; a more-preferred one fails), and the grounded extension is compared with the
;; record's decision. Joe, 2026-11-03: a reader "that only redescribes the
;; picture ... without using it to explain *why* it is selecting this problem vs
;; that problem" is not enough; "reading the rationale off of a cascade isn't
;; hype, but argumentation".
(require '[clojure.edn :as edn] '[clojure.string :as str] '[cheshire.core :as json])

(def in (nth *command-line-args* 0)) (def out (nth *command-line-args* 1))
(def frame-edn (nth *command-line-args* 2)) (def frame-json (nth *command-line-args* 3))
(def r (edn/read-string {:default (fn [_ v] v)} (slurp in)))
(def sel (:outer/selection r))
(def library "/home/joe/code/futon3/library/task-selection/")

(defn kw [x] (cond (keyword? x) (name x) (symbol? x) (name x) (nil? x) "absent" :else (str x)))
(defn fmt [g] (if (number? g) (format "%.4f" (double g)) "nil"))
(defn esc [s] (-> (str s) (str/replace "&" "&amp;") (str/replace "<" "&lt;") (str/replace ">" "&gt;")))

;; ---- warrants: the conclusion and BECAUSE of each pattern, read from the flexiarg
(defn flexiarg-part [lines marker]
  (let [i (first (keep-indexed (fn [i l] (when (str/starts-with? (str/trim l) marker) i)) lines))]
    (when i
      (let [first-line (str/trim (subs (str/trim (nth lines i)) (count marker)))
            rest-lines (->> (drop (inc i) lines) (take-while #(not (str/blank? %))) (map str/trim))]
        (str/trim (str/join " " (cons first-line rest-lines)))))))

(defn warrant [pattern]
  (let [f (str library pattern ".flexiarg")]
    (if (.exists (java.io.File. f))
      (let [lines (str/split-lines (slurp f))]
        {:pattern pattern :conclusion (flexiarg-part lines "! conclusion:") :because (flexiarg-part lines "+ BECAUSE:")})
      {:pattern pattern :conclusion nil :because nil :missing true})))

(def cascade-patterns (into {} (for [c (:cascades sel)] [(:id c) (:patterns c)])))
(def cascade-steps (into {} (for [c (:cascades sel)] [(:id c) (:steps c)])))  ; the executable steps, not the why's step results
(def relations (into {} (for [x (:cascade-pick-relations sel)] [(:target x) x])))

;; ---- arguments: one per scored class, plus stopping
(defn argument [c]
  (let [ids (:cascades c)
        lead (apply max-key #(count (cascade-patterns % [])) ids)
        pats (cascade-patterns lead [])
        g (:giraffe c) p (:placement c)]
    {:id (keyword (kw lead)) :cascades (mapv kw ids) :pick (:pick c) :G (:G c) :terms (:terms c)
     :steps (cascade-steps lead []) :step-results (:steps c)
     :facts {:status (kw (:status g)) :reason (some-> (:reason g) kw) :cone (:cone g) :unblocks (:unblocks g) :rank (:rank g)
             :prerequisites (mapv str (:prerequisites g))
             :placement {:status (kw (:status p)) :action (some-> (:action p) kw) :ways (mapv str (:ways p))}
             :relation (some-> (get-in relations [(:pick c) :relation]) kw)
             :header (get-in c [:source :header])}
     :warrants (mapv warrant pats)}))

(def args (mapv argument (get-in sel [:why :classes])))
(def stopping (let [a (get-in sel [:why :abstention])]
                {:id :stopping :cascades ["stopping"] :pick nil :G (:G a) :terms (:terms a) :steps [] :facts {} :warrants []}))
(def all-args (conj args stopping))
(def by-id (into {} (map (juxt :id identity) all-args)))

;; ---- attacks, computed from the steps and the facts
(defn gate-fails [step facts]
  (cond
    (= step :ready-gate) (when (not= "ready" (:status facts))
                           {:kind :gate :step step :fact (str "status " (:status facts) (when (:reason facts) (str " (" (:reason facts) ")")))})
    (= step :placed-gate) (let [p (:placement facts)]
                            (cond (= "absent" (:status p)) {:kind :absence :step step :fact "no placement row: nobody has read where this item is placed"}
                                  (not= "open" (:status p)) {:kind :gate :step step :fact (str "placement " (:status p))}
                                  (not (or (= "work" (:action p)) (str/starts-with? (str (:action p)) "cluster:"))) {:kind :gate :step step :fact (str "placement action " (:action p))}
                                  (empty? (:ways p)) {:kind :gate :step step :fact "placement names no way"}))
    (and (map? step) (:status-gate step)) (when (not= (kw (:status-gate step)) (:status facts))
                                            {:kind :gate :step step :fact (str "status " (:status facts) ", not " (kw (:status-gate step)))})))

(def order-dir {:by-cone :max :by-unblocks :max :by-rank :min})
(defn order-term [step facts] (get facts (case step :by-cone :cone :by-unblocks :unblocks :by-rank :rank)))

(defn attacks-between [x y]
  "What x's cascade says about y's pick: a gate that excludes it (x attacks y), or an order of x's that prefers y's pick (y attacks x)."
  (let [steps (:steps x)
        gate (some #(gate-fails % (:facts y)) steps)]
    (if gate
      [(assoc gate :from (:id x) :to (:id y) :text (str (name (:id x)) "'s " (kw (:step gate)) " excludes " (:pick y) ": " (:fact gate)))]
      (let [orders (filter order-dir steps)
            decided (some (fn [o] (let [vx (order-term o (:facts x)) vy (order-term o (:facts y))]
                                    (when (and (number? vx) (number? vy) (not= vx vy))
                                      (let [x-wins (if (= :max (order-dir o)) (> vx vy) (< vx vy))]
                                        {:order o :vx vx :vy vy :x-wins x-wins}))))
                          orders)]
        (cond
          (nil? decided) []
          (:x-wins decided) [{:kind :order :from (:id x) :to (:id y) :step (:order decided)
                              :text (str (name (:id x)) "'s " (kw (:order decided)) " prefers its own pick: " (:pick x) " " (:vx decided) " against " (:pick y) " " (:vy decided))}]
          :else [{:kind :order :from (:id y) :to (:id x) :step (:order decided)
                  :text (str (name (:id x)) "'s own " (kw (:order decided)) " prefers " (:pick y) " (" (:vy decided) ") over its pick " (:pick x) " (" (:vx decided) ")")}])))))

(def attacks (vec (distinct (concat (for [x args y args :when (not= x y) a (attacks-between x y)] a)
                                    ;; stopping and each pick attack each other; G (the abstention rule) resolves it
                                    (for [x args] {:kind :stopping :from (:id x) :to :stopping :text (str (name (:id x)) " has a pick below abstention: " (:pick x))})
                                    (for [x args] {:kind :stopping :from :stopping :to (:id x) :text (str "stopping: no pick is worth more than abstaining, against " (:pick x))})))))

;; ---- preference = G (lower is better); an attack from a less-preferred argument on a more-preferred one fails
(defn G [id] (:G (by-id id)))
(def attacks* (mapv #(assoc % :succeeds (not (< (G (:to %)) (G (:from %))))) attacks))
(def defeats (filter :succeeds attacks*))
(defn defeaters [id] (map :from (filter #(= id (:to %)) defeats)))
(defn acceptable? [S id] (every? (fn [d] (some #(contains? S %) (defeaters d))) (defeaters id)))
(def grounded (loop [S #{}] (let [S' (set (filter #(acceptable? S %) (map :id all-args)))] (if (= S S') S (recur S')))))
(def decision (get-in sel [:why :decision]))
(def winner (first (filter #(= (:pick decision) (:pick %)) args)))

;; ---- the plain-language argument, generated from the frame
(defn grounds-text [a]
  (let [f (:facts a) p (:placement f)]
    (str/join "; " (remove nil?
      [(str "Giraffe reads it " (:status f) (when (:reason f) (str " (" (:reason f) ")")) (when (:rank f) (str ", rank " (:rank f))))
       (when (number? (:cone f)) (str "cone " (:cone f) ", unblocks " (:unblocks f)))
       (if (= "absent" (:status p)) "no placement row"
           (str "placement " (:status p) " under " (str/join ", " (:ways p)) ", action " (:action p)))
       (str "relation to the focus " (or (:relation f) "unknown"))]))))

(defn warrants-text [a] (str/join " " (map #(str "“" (:conclusion %) "”") (filter :conclusion (:warrants a)))))

(def losers (remove #(= (:id winner) (:id %)) args))
(defn attack-sentence [at]
  (str (if (= :absence (:kind at)) "From absence, " "") (:text at) (if (:succeeds at) "" " (fails under G)") "."))

(def prose
  (let [n-open (get-in sel [:field-facts :open-tasks] "?")
        w winner]
    (str/join "\n\n"
      (concat
        [(str "The problem: of the open tasks, which should the War Machine work on next. The claim: " (:pick w)
              ", by " (name (:id w)) ". Warrants: " (warrants-text w) " Grounds: " (grounds-text w) ".")]
        (for [l losers]
          (str (name (:id l)) (when (> (count (:cascades l)) 1) (str " (its class also holds " (str/join ", " (remove #{(name (:id l))} (:cascades l))) ")")) " argues instead for " (:pick l) ". Warrants: " (warrants-text l)
               " Grounds: " (grounds-text l) ". "
               (str/join " " (map attack-sentence (filter #(and (= (:from %) (:id l)) (= (:to %) (:id w))) attacks*)))
               " "
               (str/join " " (map attack-sentence (filter #(and (= (:from %) (:id w)) (= (:to %) (:id l))) attacks*)))))
        [(str "What decided: G. " (str/join " < " (map #(str (name (:id %)) " " (fmt (:G %))) (sort-by :G all-args)))
              ". Under that preference an attack from a higher-G argument on a lower-G one fails, so "
              (str/join "; " (map #(str (name (:from %)) " against " (name (:to %)) (if (:succeeds %) " stands" " fails")) attacks*))
              ". Grounded extension: {" (str/join ", " (map name grounded)) "}"
              (if (contains? grounded (:id w)) ", which is the record's decision." ", which is NOT the record's decision."))]))))

(def what-G-read
  (str "What G read: the pick's Giraffe status (reach prior), its relation to the focus (a known relation lands on that ending; an unknown one lands on the pinned prior 11/19 focused, 7/19 related, 1/19 unrelated) and its unblocks as gain (outer_score.clj kernel-parameters). It did not read the placement, so the winning cascade's own gate contributed nothing to the number that made it win."
       (when-let [l (first (filter #(= "associated" (get-in % [:facts :relation])) losers))]
         (when (= "unknown" (get-in winner [:facts :relation]))
           (str " Here " (:pick l) " is known to be associated with the focus and lands on the related ending; " (:pick winner)
                "'s relation is unknown and lands on the prior, which puts most mass on the focused ending that the preference likes best. The lower risk of the winner (" (fmt (get-in winner [:terms :risk])) " against " (fmt (get-in l [:terms :risk]))
                ") comes from not knowing, not from knowing.")))))

(def frame {:schema :cascade/argument-frame-v0
            :run (:run/id r) :click (:click/id r)
            :problem "which open task the War Machine works on next"
            :arguments all-args :attacks attacks* :preference :G-lower-is-better
            :semantics :grounded-under-preference :grounded (vec grounded)
            :decision (:pick decision) :agrees (contains? grounded (:id winner))
            :prose prose :what-G-read what-G-read})

;; ---- page
(defn arg-row [a]
  (str "<tr" (when (= (:id a) (:id winner)) " class=sel") "><td><b>" (esc (name (:id a))) "</b><br><small>" (esc (str/join ", " (:cascades a))) "</small></td>"
       "<td>" (esc (or (:pick a) "stop")) "</td><td>" (esc (if (seq (:facts a)) (grounds-text a) "none: stopping has no pick")) "</td>"
       "<td>" (str/join "<br>" (for [w (:warrants a)] (str "<i>" (esc (:pattern w)) "</i>: " (esc (or (:conclusion w) "(no conclusion line)"))))) "</td>"
       "<td class=num>" (fmt (:G a)) "<br><small>" (esc (str/join " " (for [[k v] (:terms a)] (str (name k) " " (fmt v))))) "</small></td></tr>"))

(def html
  (str "<!doctype html><meta charset=utf-8><title>Why this problem and not that one: run " (esc (:run/id r)) "</title>
<style>body{max-width:60em;margin:2em auto;padding:0 1em;font:16px/1.5 Georgia,serif;color:#222}h1,h2{font-family:system-ui,sans-serif}
table{border-collapse:collapse;font-size:90%}td,th{border:1px solid #ccc;padding:.3em .6em;vertical-align:top}.num{text-align:right;font-family:monospace}
.sel{background:#eef8ee}.prose{border:1px solid #cde;background:#f6fbff;padding:.8em}.prose p{margin:.4em 0}.absence{color:#b23b3b}.fails{color:#888}
.ok{color:#2e7d4f}</style>
<h1>Why this problem and not that one</h1>
<p>Run <code>" (esc (:run/id r)) "</code>, click <code>" (esc (:click/id r)) "</code>. Each scored cascade class is an argument for its pick; attacks are computed from the steps and the facts, never written by hand; G is the preference that resolves them.</p>
<h2>The argument, in prose</h2><div class=prose>" (str/join "" (map #(str "<p>" (esc %) "</p>") (str/split prose #"\n\n"))) "<p><i>" (esc what-G-read) "</i></p></div>
<h2>The arguments</h2><table><tr><th>argument</th><th>pick</th><th>grounds (facts read)</th><th>warrants (pattern conclusions)</th><th>G</th></tr>" (str/join "\n" (map arg-row all-args)) "</table>
<h2>The attacks</h2><ul>" (str/join "\n" (for [a attacks*] (str "<li class=" (cond (not (:succeeds a)) "fails" (= :absence (:kind a)) "absence" :else "ok") ">" (esc (name (:from a))) " → " (esc (name (:to a))) " (" (name (:kind a)) "): " (esc (:text a)) (if (:succeeds a) " — stands under G" " — fails under G: the target is preferred") "</li>"))) "</ul>
<p>Semantics: grounded extension under preference (an attack from a less-preferred argument on a more-preferred one does not defeat). Grounded: {" (esc (str/join ", " (map name grounded))) "}. Record's decision: " (esc (:pick decision)) (if (:agrees frame) " — agrees." " — DISAGREES.") "</p>
<p>Frame: <code>" (esc frame-edn) "</code> (schema :cascade/argument-frame-v0). Record: <code>" (esc in) "</code>.</p>"))

(spit out html)
(spit frame-edn (with-out-str (clojure.pprint/pprint frame)))
(spit frame-json (json/generate-string frame {:pretty true}))
(println "wrote" out "," frame-edn "," frame-json)
(println "grounded:" grounded "agrees:" (:agrees frame))
(println)
(println prose)
(println)
(println what-G-read)
