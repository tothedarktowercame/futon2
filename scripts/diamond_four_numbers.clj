(ns diamond-four-numbers
  (:require [clojure.java.shell :as sh]
            [clojure.string :as str]
            [futon2.aif.cascade-model-manifest :as manifest]
            [futon2.aif.cascade-shape-g :as shape-g]))

(def report-path
  "holes/labs/wm-contract/REPORT-diamond-four-numbers-2026-10-05.md")
(def target "meta-outer-policy-cascade")
(def observe "meta/observe-the-meta-field")
(def fill "meta/fill-meta-policy-slots")
(def injury "meta/injury-routes-to-self-heal")
(def minimise "meta/minimise-g-over-filled-meta-policies")
(def nodes (mapv #(hash-map :pattern %) [observe fill injury minimise]))
(def edge (fn [from to] {:from from :to to :kind :precedes}))

(def arrangements
  [{:id "diamond" :label "A. diamond" :precedence [observe fill injury minimise]
    :edges [(edge observe fill) (edge observe injury)
            (edge fill minimise) (edge injury minimise)]
    :linear-extensions 2}
   {:id "chain-1" :label "B. chain-1" :precedence [observe fill injury minimise]
    :edges [(edge observe fill) (edge fill injury) (edge injury minimise)]
    :linear-extensions 1}
   {:id "chain-2" :label "C. chain-2" :precedence [observe injury fill minimise]
    :edges [(edge observe injury) (edge injury fill) (edge fill minimise)]
    :linear-extensions 1}
   {:id "bag" :label "D. bag" :precedence [observe fill injury minimise]
    :edges [] :linear-extensions 24}])

(defn git-sha []
  (let [{:keys [exit out err]} (sh/sh "git" "rev-parse" "HEAD")]
    (if (zero? exit) (str/trim out)
        (throw (ex-info "git rev-parse refused" {:stderr err})))))

(defn log2 [n] (/ (Math/log (double n)) (Math/log 2.0)))
(defn fmt [x] (format "%.12f" (double x)))
(defn probability [x] (if (nil? x) "0" (fmt x)))

(defn measure [{:keys [id nodes precedence edges linear-extensions] :as arrangement}]
  (let [cascade {:nodes nodes :precedence precedence :edges edges}
        score (shape-g/score-arranged target id cascade)]
    (when (= :refused (:status score))
      (throw (ex-info "score-arranged refused"
                      {:arrangement id :refusal score})))
    (let [candidate (:candidate score)
          all-done (set (map #(vector target :pattern-done %)
                             [observe fill injury minimise]))
          rollout (manifest/rollout (constantly (:precedence candidate))
                                    {#{} 1} (:horizon score))
          theta (into []
                      (for [[pattern {:keys [theta theta-record]}]
                            (sort-by key (get-in candidate [:precedence :co-apply :patterns]))]
                        {:pattern pattern :theta theta :theta-record theta-record}))
          ordering-term (log2 linear-extensions)]
      (assoc arrangement :cascade cascade :score score :theta theta
             :all-done-probability (get rollout all-done 0)
             :ordering-term ordering-term
             :g-plus-ordering (+ (double (:g score)) ordering-term)))))

(defn preference-text [schedule]
  (str/join "<br>"
            (for [[tau distribution] (sort-by key schedule)]
              (str "τ=" tau ": "
                   (str/join ", "
                             (for [[[done met?] p] (sort-by key distribution)]
                               (str "[" done " " met? "]=" (fmt p))))))))

(defn theta-text [theta]
  (str/join "<br>"
            (for [{:keys [pattern theta theta-record]} theta]
              (str "`" pattern "` = " theta " (`" (:status theta-record) "`)"))))

(defn ordering-text [results]
  (->> (sort-by #(get-in % [:score :g]) results)
       (partition-by #(get-in % [:score :g]))
       (map (fn [ties]
              (str/join " = " (map #(str "**" (:label %) "**") ties))))
       (str/join " < ")))

(defn render [sha results]
  (let [ordered-by-g (sort-by #(get-in % [:score :g]) results)
        bag (some #(when (= "bag" (:id %)) %) results)
        best (first ordered-by-g)]
    (str "# Outer-loop diamond: four arrangement scores — 2026-10-05\n\n"
         "Reproduce in a fresh process from `/home/joe/code/futon2`:\n\n"
         "```sh\nclojure -M scripts/diamond_four_numbers.clj\n```\n\n"
         "futon2 HEAD measured: `" sha "`. The scorer is `futon2.aif.cascade-shape-g/score-arranged`; the rollout transition is `:co-application-frontier-theta-v1`. The ordering term is reported separately and was not supplied to the scorer.\n\n"
         "## Scores\n\n"
         "| arrangement | status | G | risk | ambiguity | information gain | horizon | P(all done at horizon) | log2(linear extensions) | G + ordering term |\n"
         "|---|---|---:|---:|---:|---:|---:|---:|---:|---:|\n"
         (str/join "\n"
                   (for [{:keys [label linear-extensions ordering-term g-plus-ordering
                                 all-done-probability score]} results]
                     (str "| " label " | `" (:status score) "` | " (fmt (:g score))
                          " | " (fmt (:risk score)) " | " (fmt (:ambiguity score))
                          " | " (fmt (:information-gain score)) " | " (:horizon score)
                          " | " (probability all-done-probability) " | "
                          (fmt ordering-term) " (" linear-extensions ") | "
                          (fmt g-plus-ordering) " |")))
         "\n\n## Pattern theta\n\n"
         (str/join "\n\n" (for [{:keys [label theta]} results]
                              (str "- " label ": " (theta-text theta))))
         "\n\n## Per-step preference schedule\n\n"
         "The schedule keys are `[completed-pattern-count want-met?]`; probabilities are normalized preferences.\n\n"
         (str/join "\n\n"
                   (for [{:keys [label score]} results]
                     (str "- " label ": "
                          (preference-text (:preference-at-each-step score)))))
         "\n\n## Reading\n\n"
         "All four arrangements use the same four-node, four-step progress preference, so their different G values come from when the frontier kernel can co-apply enabled patterns. The bag exposes all four nodes at the first step; the diamond exposes observe, then fill and injury together, then minimise; each chain exposes one node per frontier. Reading the measured values from lowest to highest G gives "
         (ordering-text results)
         ". The identical preference schedule rewards earlier completed-pattern mass at every step; risk and ambiguity reflect each arrangement's rollout under that schedule, while the reported information-gain subtraction uses the same per-pattern theta evidence.\n\n"
         "**Does the bag score best on G alone?** "
         (if (= "bag" (:id best)) "Yes." "No.")
         " Its G is " (fmt (get-in bag [:score :g])) ".\n")))

(defn -main [& _]
  (let [results (mapv #(measure (assoc % :nodes nodes)) arrangements)]
    (spit report-path (render (git-sha) results))
    (println report-path)))

(apply -main *command-line-args*)
