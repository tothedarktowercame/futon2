(ns extended-four-numbers
  (:require [clojure.edn :as edn]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [futon2.aif.cascade-model-manifest :as manifest]
            [futon2.aif.cascade-shape-g :as shape-g]
            [futon2.aif.meta-outer-policy :as outer]))

(def proposal-path "holes/labs/wm-contract/proposals/meta-outer-policy-cascade-extended-proposal.edn")
(def original-path "/home/joe/code/futon3/library/meta/meta-outer-policy-cascade.edn")
(def report-path "holes/labs/wm-contract/REPORT-extended-diamond-validation-2026-10-06.md")
(def target "meta-outer-policy-cascade")
(defn pattern-string [x] (subs (str x) 1))
(defn edge [[a b]] {:from (pattern-string a) :to (pattern-string b) :kind :precedes})
(defn log2 [n] (/ (Math/log (double n)) (Math/log 2.0)))
(defn fmt [x] (format "%.12f" (double x)))
(defn git-sha [] (str/trim (:out (sh/sh "git" "rev-parse" "HEAD"))))

(defn closure [nodes edges]
  (let [out (reduce (fn [m [a b]] (update m a (fnil conj #{}) b)) {} edges)]
    (into {} (for [n nodes]
               [n (loop [todo (seq (out n)) seen #{}]
                    (if-let [x (first todo)]
                      (if (seen x) (recur (next todo) seen)
                          (recur (concat (next todo) (out x)) (conj seen x)))
                      seen))]))))
(defn incomparable? [reach a b] (and (not ((reach a) b)) (not ((reach b) a))))
(defn meets [nodes edges direction]
  (let [reach (closure nodes edges)
        related (fn [candidate x]
                  (case direction
                    :common-descendant (or (= candidate x) ((reach x) candidate))
                    :common-ancestor (or (= candidate x) ((reach candidate) x))))
        extremal? (fn [candidate commons]
                    (every? (fn [other]
                              (case direction
                                :common-descendant (or (= candidate other) ((reach candidate) other))
                                :common-ancestor (or (= candidate other) ((reach other) candidate))))
                            commons))]
    (vec (for [i (range (count nodes)) j (range (inc i) (count nodes))
               :let [x (nodes i) y (nodes j)] :when (incomparable? reach x y)
               :let [commons (filter #(and (related % x) (related % y)) nodes)
                     m (first (filter #(extremal? % commons) commons))]
               :when m]
           {:x x :y y :meet m}))))

(defn linear-extensions [nodes edges]
  (let [n (count nodes) idx (zipmap nodes (range))
        prereq (reduce (fn [v [a b]] (update v (idx b) bit-or (bit-shift-left 1 (idx a))))
                       (vec (repeat n 0)) edges)
        full (dec (bit-shift-left 1 n)) memo (atom {})]
    (letfn [(go [mask]
              (if (= mask full) 1
                  (if-let [cached (find @memo mask)] (val cached)
                      (let [answer (reduce + 0
                                           (for [i (range n)
                                                 :when (and (zero? (bit-and mask (bit-shift-left 1 i)))
                                                            (= (prereq i) (bit-and mask (prereq i))))]
                                             (go (bit-or mask (bit-shift-left 1 i)))))]
                        (swap! memo assoc mask answer) answer))))]
      (go 0))))

(defn arrangement [id label patterns precedence pairs extensions]
  {:id id :label label :patterns patterns :precedence precedence :pairs pairs
   :linear-extensions extensions
   :cascade {:nodes (mapv #(hash-map :pattern %) patterns)
             :precedence precedence :edges (mapv edge pairs)}})
(defn measure [{:keys [id patterns cascade linear-extensions] :as a}]
  (let [score (shape-g/score-arranged target id cascade)]
    (when (= :refused (:status score))
      (throw (ex-info "score-arranged refused" {:arrangement id :refusal score})))
    (let [candidate (:candidate score)
          all-done (set (map #(vector target :pattern-done %) patterns))
          rollout (manifest/rollout (constantly (:precedence candidate)) {#{} 1} (:horizon score))]
      (assoc a :score score :all-done (get rollout all-done 0)
             :ordering (log2 linear-extensions)
             :theta (for [[p {:keys [theta theta-record]}]
                          (sort-by key (get-in candidate [:precedence :co-apply :patterns]))]
                      {:pattern p :theta theta :status (:status theta-record)})))))

(defn short-pattern [p] (str "`" p "`"))
(defn meet-table [rows]
  (str/join "\n" (for [{:keys [x y meet]} rows]
                    (str "| " (short-pattern x) " | " (short-pattern y) " | " (short-pattern meet) " |"))))
(defn theta-text [xs]
  (str/join "<br>" (for [{:keys [pattern theta status]} xs]
                       (str (short-pattern pattern) " = " theta " (`" status "`)"))))
(defn render [{:keys [sha proposal-errors original-errors nodes extensions descendants ancestors results]}]
  (str "# Extended outer diamond: validation, structure, and scores — 2026-10-06\n\n"
       "Reproduce from `/home/joe/code/futon2` in a fresh process:\n\n```sh\nclojure -M scripts/extended_four_numbers.clj\n```\n\n"
       "futon2 HEAD before generation: `" sha "`. The proposal is read unchanged from `" proposal-path "`; the control is read from `" original-path "`.\n\n"
       "## 1. Real contract validator\n\nThe exact returned values from `#'futon2.aif.meta-outer-policy/contract-errors` were:\n\n```clojure\n"
       "proposal => " (pr-str proposal-errors) "\noriginal => " (pr-str original-errors) "\n```\n\n"
       (if (seq proposal-errors)
         "The proposal was rejected; the keywords above name the checks that rejected it. No validator or proposal edit was made.\n\n"
         "The validator accepted the proposal. In particular, its current checks do not reject the extra top-level `:tokens`, the extra `:generative-model :conditioning`, or the added `:operator-demand` outcome; acceptance here records the validator's present boundary, not a ruling on those additions. The original control also returned `[]`.\n\n")
       "## 2. Precedence structure\n\nThe ten-edge proposal has **" extensions " linear extensions** (computed by subset DP over " (count nodes) " units), with ordering term `log2(" extensions ") = " (fmt (log2 extensions)) "`.\n\n"
       "### ConstructionReceipt direction: common descendant\n\nHere an incomparable pair meets at the common descendant that precedes every other common descendant.\n\n| x | y | meet |\n|---|---|---|\n" (meet-table descendants) "\n\n"
       "### CascadeOrder direction: common ancestor\n\nHere an incomparable pair meets at the common ancestor that follows every other common ancestor.\n\n| x | y | meet |\n|---|---|---|\n" (meet-table ancestors) "\n\n"
       "## 3. Existing-scorer measurements\n\nAll edges, including `read-library-coupling → minimise`, are passed as `:precedes` in the extended run. Ordering is reported separately and is not added to G.\n\n"
       "| arrangement | status | G | risk | ambiguity | information gain | P(all done at horizon) | horizon | linear extensions | ordering term |\n|---|---|---:|---:|---:|---:|---:|---:|---:|---:|\n"
       (str/join "\n" (for [{:keys [label score all-done linear-extensions ordering]} results]
                            (str "| " label " | `" (:status score) "` | " (fmt (:g score)) " | "
                                 (fmt (:risk score)) " | " (fmt (:ambiguity score)) " | "
                                 (fmt (:information-gain score)) " | " (fmt all-done) " | " (:horizon score)
                                 " | " linear-extensions " | " (fmt ordering) " |")))
       "\n\n### Theta by arrangement\n\n"
       (str/join "\n\n" (for [{:keys [label theta]} results] (str "- " label ": " (theta-text theta))))
       "\n\n## Reading against the 2026-10-05 four-number result\n\n"
       "The earlier four-node measurement ordered G as bag (0.392913018968) < diamond (1.279742336087) < either chain (1.516585504479). The control below reproduces the diamond value. In the seven-unit measurement, the measured table similarly exposes how earlier co-application changes risk and all-done probability: the bag enables all seven immediately, the extended diamond enables five middle readings together after observe, and the chain enables one unit at a time. The ordering-term column is descriptive only and does not change any reported G.\n"))

(defn -main [& _]
  (let [proposal (edn/read-string (slurp proposal-path)) original (edn/read-string (slurp original-path))
        proposal-errors (#'outer/contract-errors proposal)
        original-errors (#'outer/contract-errors original)
        nodes (:patterns proposal) pairs (:precedence proposal) extensions (linear-extensions nodes pairs)
        ps (mapv pattern-string nodes) pp (mapv pattern-string (:patterns proposal))
        original-ps (mapv pattern-string (:patterns original))
        original-pairs (:precedence original)
        original-order (mapv pattern-string (:patterns original))
        chain-pairs (mapv vec (partition 2 1 nodes))
        arrangements [(arrangement "original-diamond" "A. original diamond (control)" original-ps original-order original-pairs 2)
                      (arrangement "extended-diamond" "B. extended seven-unit shape" ps pp pairs extensions)
                      (arrangement "extended-bag" "C. extended bag" ps pp [] 5040)
                      (arrangement "extended-chain" "D. extended chain" ps pp chain-pairs 1)]
        results (mapv measure arrangements)
        result {:sha (git-sha) :proposal-errors proposal-errors :original-errors original-errors
                :nodes nodes :pairs pairs :extensions extensions
                :descendants (meets nodes pairs :common-descendant)
                :ancestors (meets nodes pairs :common-ancestor) :results results}]
    (when-not (= 120 extensions) (throw (ex-info "unexpected extension count" {:actual extensions})))
    (spit report-path (render result))
    (println report-path)))

(apply -main *command-line-args*)
