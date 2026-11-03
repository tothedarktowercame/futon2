(ns futon2.aif.outer-argue
  "A preference-based argument reading of one outer selection.

  The executable semantics conform to
  DarkTower.WarMachine.SelectionArgument (commit dda98c192)."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- word [x]
  (cond (keyword? x) (name x)
        (symbol? x) (name x)
        (nil? x) "absent"
        :else (str x)))

(defn- fmt4 [g]
  (if (number? g)
    (String/format java.util.Locale/ROOT "%.4f" (to-array [(double g)]))
    "nil"))

(defn- flexiarg-part [lines marker]
  (when-let [i (first (keep-indexed #(when (str/starts-with? (str/trim %2) marker) %1)
                                    lines))]
    (let [first-line (str/trim (subs (str/trim (nth lines i)) (count marker)))
          following (->> lines (drop (inc i))
                         (take-while #(not (str/blank? %)))
                         (map str/trim))]
      (str/trim (str/join " " (cons first-line following))))))

(defn- sha256-bytes [bytes]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and 0xff %)) digest))))

(defn warrant
  "Read one task-selection flexiarg and bind the warrant to its exact bytes."
  [library-root pattern]
  (let [file (io/file library-root (str pattern ".flexiarg"))]
    (if (.isFile file)
      (let [bytes (java.nio.file.Files/readAllBytes (.toPath file))
            lines (str/split-lines (String. bytes "UTF-8"))]
        {:pattern pattern
         :sha256 (sha256-bytes bytes)
         :conclusion (flexiarg-part lines "! conclusion:")
         :because (flexiarg-part lines "+ BECAUSE:")})
      {:pattern pattern :sha256 nil :conclusion nil :because nil :missing true})))

(defn- default-library-root []
  (str (System/getProperty "user.home") "/code/futon3/library/task-selection"))

(defn arguments
  "Construct the argument carrier from retained `why`, cascades, and relations."
  ([why cascades relations]
   (arguments why cascades relations (default-library-root)))
  ([why cascades relations library-root]
   (let [cascade-patterns (into {} (map (juxt :id :patterns) cascades))
         cascade-steps (into {} (map (juxt :id :steps) cascades))
         relations-by-target (into {} (map (juxt :target identity) relations))
         pick-argument
         (fn [class]
           (let [ids (:cascades class)
                 lead (apply max-key #(count (get cascade-patterns % [])) ids)
                 giraffe (:giraffe class)
                 placement (:placement class)]
             {:id (keyword (word lead))
              :cascades (mapv word ids)
              :pick (:pick class)
              :G (:G class)
              :terms (:terms class)
              :steps (get cascade-steps lead [])
              :step-results (:steps class)
              :facts {:status (word (:status giraffe))
                      :reason (some-> (:reason giraffe) word)
                      :cone (:cone giraffe)
                      :unblocks (:unblocks giraffe)
                      :rank (:rank giraffe)
                      :prerequisites (mapv str (:prerequisites giraffe))
                      :placement {:status (word (:status placement))
                                  :action (some-> (:action placement) word)
                                  :ways (mapv str (:ways placement))}
                      :relation (some-> (get-in relations-by-target
                                               [(:pick class) :relation]) word)
                      :header (get-in class [:source :header])}
              :warrants (mapv #(warrant library-root %)
                              (get cascade-patterns lead []))}))
         picks (mapv pick-argument (:classes why))
         abstention (:abstention why)]
     (conj picks {:id :stopping :cascades ["stopping"] :pick nil
                  :G (:G abstention) :terms (:terms abstention)
                  :steps [] :facts {} :warrants []}))))

(defn- gate-fails [step facts]
  (cond
    (= step :ready-gate)
    (when (not= "ready" (:status facts))
      {:kind :gate :step step
       :fact (str "status " (:status facts)
                  (when (:reason facts) (str " (" (:reason facts) ")")))})

    (= step :placed-gate)
    (let [placement (:placement facts)]
      (cond
        (= "absent" (:status placement))
        {:kind :absence :step step
         :fact "no placement row: nobody has read where this item is placed"}
        (not= "open" (:status placement))
        {:kind :gate :step step :fact (str "placement " (:status placement))}
        (not (or (= "work" (:action placement))
                 (str/starts-with? (str (:action placement)) "cluster:")))
        {:kind :gate :step step :fact (str "placement action " (:action placement))}
        (empty? (:ways placement))
        {:kind :gate :step step :fact "placement names no way"}))

    (and (map? step) (:status-gate step))
    (when (not= (word (:status-gate step)) (:status facts))
      {:kind :gate :step step
       :fact (str "status " (:status facts) ", not " (word (:status-gate step)))})))

(def ^:private order-direction {:by-cone :max :by-unblocks :max :by-rank :min})

(defn- order-term [step facts]
  (get facts (case step :by-cone :cone :by-unblocks :unblocks :by-rank :rank)))

(defn- attacks-between [x y]
  (if-let [gate (some #(gate-fails % (:facts y)) (:steps x))]
    [(assoc gate :from (:id x) :to (:id y)
                 :text (str (name (:id x)) "'s " (word (:step gate)) " excludes "
                            (:pick y) ": " (:fact gate)))]
    (let [decision (some (fn [order]
                           (let [vx (order-term order (:facts x))
                                 vy (order-term order (:facts y))]
                             (when (and (number? vx) (number? vy) (not= vx vy))
                               {:order order :vx vx :vy vy
                                :x-wins (if (= :max (order-direction order))
                                          (> vx vy) (< vx vy))})))
                         (filter order-direction (:steps x)))]
      (cond
        (nil? decision) []
        (:x-wins decision)
        [{:kind :order :from (:id x) :to (:id y) :step (:order decision)
          :text (str (name (:id x)) "'s " (word (:order decision))
                     " prefers its own pick: " (:pick x) " " (:vx decision)
                     " against " (:pick y) " " (:vy decision))}]
        :else
        [{:kind :order :from (:id y) :to (:id x) :step (:order decision)
          :text (str (name (:id x)) "'s own " (word (:order decision)) " prefers "
                     (:pick y) " (" (:vy decision) ") over its pick " (:pick x)
                     " (" (:vx decision) ")")}]))))

(defn defeats?
  "Mirror Lean `defeats`: self-attacks and unknown IDs fail; otherwise the
  attack succeeds unless the target has strictly lower G than the source."
  [arguments attack]
  (let [argument? (fn [id] (first (filter #(= id (:id %)) arguments)))
        source (argument? (:from attack))
        target (argument? (:to attack))]
    (boolean (and (not= (:from attack) (:to attack)) source target
                  (not (< (:G target) (:G source)))))))

(defn attacks
  "Compute gate, absence, order, and stopping attacks, then annotate defeat."
  [arguments]
  (let [picks (filterv #(not= :stopping (:id %)) arguments)
        raw (vec
             (distinct
              (concat (for [x picks y picks :when (not= x y)
                            attack (attacks-between x y)]
                        attack)
                      (for [x picks]
                        {:kind :stopping :from (:id x) :to :stopping
                         :text (str (name (:id x)) " has a pick below abstention: "
                                    (:pick x))})
                      (for [x picks]
                        {:kind :stopping :from :stopping :to (:id x)
                         :text (str "stopping: no pick is worth more than abstaining, against "
                                    (:pick x))}))))]
    (mapv #(assoc % :succeeds (defeats? arguments %)) raw)))

(defn defeaters
  "Mirror Lean `defeaters`: successful incoming attack sources, in attack order."
  [arguments attacks id]
  (mapv :from (filter #(and (= id (:to %)) (defeats? arguments %)) attacks)))

(defn acceptable?
  "Mirror Lean `acceptable`: every defeater is defeated by some member of S."
  [arguments attacks S id]
  (every? (fn [attacker]
            (some (fn [defender]
                    (some #(and (= defender (:from %))
                                (= attacker (:to %))
                                (defeats? arguments %))
                          attacks))
                  S))
          (defeaters arguments attacks id)))

(defn grounded-step
  "Mirror Lean `groundedStep`, preserving argument-list order."
  [arguments attacks S]
  (->> arguments
       (filter #(acceptable? arguments attacks S (:id %)))
       (mapv :id)))

(defn grounded
  "Mirror Lean `grounded`: run `groundedStep` exactly args.length + 1 times."
  [arguments attacks]
  (nth (iterate #(grounded-step arguments attacks %) [])
       (inc (count arguments))))

(defn- grounds-text [argument]
  (if (= :stopping (:id argument))
    "none: stopping has no pick"
    (let [facts (:facts argument) placement (:placement facts)]
      (str/join "; "
                (remove nil?
                        [(str "Giraffe reads it " (:status facts)
                              (when (:reason facts) (str " (" (:reason facts) ")"))
                              (when (:rank facts) (str ", rank " (:rank facts))))
                         (when (number? (:cone facts))
                           (str "cone " (:cone facts) ", unblocks " (:unblocks facts)))
                         (if (= "absent" (:status placement))
                           "no placement row"
                           (str "placement " (:status placement) " under "
                                (str/join ", " (:ways placement)) ", action "
                                (:action placement)))
                         (str "relation to the focus "
                              (or (:relation facts) "unknown"))])))))

(defn- warrants-text [argument]
  (str/join " " (map #(str "“" (:conclusion %) "”")
                     (filter :conclusion (:warrants argument)))))

(defn- attack-sentence [attack]
  (str (when (= :absence (:kind attack)) "From absence, ")
       (:text attack) (when-not (:succeeds attack) " (fails under G)") "."))

(defn- prose [arguments attacks grounded decision]
  (let [picks (remove #(= :stopping (:id %)) arguments)
        winner (first (filter #(= (:pick decision) (:pick %)) arguments))
        losers (remove #(= (:id winner) (:id %)) picks)]
    (str/join
     "\n\n"
     (concat
      [(str "The problem: of the open tasks, which should the War Machine work on next. The claim: "
            (:pick winner) ", by " (name (:id winner)) ". Warrants: "
            (warrants-text winner) " Grounds: " (grounds-text winner) ".")]
      (for [loser losers]
        (str (name (:id loser))
             (when (> (count (:cascades loser)) 1)
               (str " (its class also holds "
                    (str/join ", " (remove #{(name (:id loser))} (:cascades loser))) ")"))
             " argues instead for " (:pick loser) ". Warrants: "
             (warrants-text loser) " Grounds: " (grounds-text loser) ". "
             (str/join " " (map attack-sentence
                                  (filter #(and (= (:from %) (:id loser))
                                                (= (:to %) (:id winner))) attacks)))
             " "
             (str/join " " (map attack-sentence
                                  (filter #(and (= (:from %) (:id winner))
                                                (= (:to %) (:id loser))) attacks)))))
      [(str "What decided: G. "
            (str/join " < " (map #(str (name (:id %)) " " (fmt4 (:G %)))
                                  (sort-by :G arguments)))
            ". Under that preference an attack from a higher-G argument on a lower-G one fails, so "
            (str/join "; " (map #(str (name (:from %)) " against " (name (:to %))
                                        (if (:succeeds %) " stands" " fails")) attacks))
            ". Grounded extension: {" (str/join ", " (map name grounded)) "}"
            (if (some #{(:id winner)} grounded)
              ", which is the record's decision."
              ", which is NOT the record's decision."))]))))

(defn- what-g-read [arguments decision]
  (let [picks (remove #(= :stopping (:id %)) arguments)
        winner (first (filter #(= (:pick decision) (:pick %)) arguments))
        losers (remove #(= (:id winner) (:id %)) picks)]
    (str "What G read: the pick's Giraffe status (reach prior), its relation to the focus (a known relation lands on that ending; an unknown one lands on the pinned prior 11/19 focused, 7/19 related, 1/19 unrelated) and its unblocks as gain (outer_score.clj kernel-parameters). It did not read the placement, so the winning cascade's own gate contributed nothing to the number that made it win."
         (when-let [loser (and (not= :stopping (:id winner))
                               (first (filter #(= "associated"
                                                  (get-in % [:facts :relation]))
                                              losers)))]
           (when (= "unknown" (get-in winner [:facts :relation]))
             (str " Here " (:pick loser) " is known to be associated with the focus and lands on the related ending; "
                  (:pick winner) "'s relation is unknown and lands on the prior, which puts most mass on the focused ending that the preference likes best. The lower risk of the winner ("
                  (fmt4 (get-in winner [:terms :risk])) " against "
                  (fmt4 (get-in loser [:terms :risk])) ") comes from not knowing, not from knowing."))))))

(defn frame
  "Build the retained argument reading for an outer selection."
  [{:keys [why cascades relations library-root]}]
  (let [arguments (arguments why cascades relations (or library-root (default-library-root)))
        attacks (attacks arguments)
        grounded (grounded arguments attacks)
        decision (:decision why)
        winner (first (filter #(= (:pick decision) (:pick %)) arguments))]
    {:schema :cascade/argument-frame-v1
     :arguments arguments
     :attacks attacks
     :preference :G-lower-is-better
     :semantics :grounded-under-preference
     :grounded grounded
     :decision (:pick decision)
     :agrees (boolean (some #{(:id winner)} grounded))
     :prose (prose arguments attacks grounded decision)
     :what-G-read (what-g-read arguments decision)}))
