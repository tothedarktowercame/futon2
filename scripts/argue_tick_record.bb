#!/usr/bin/env bb
;; argue_tick_record.bb TICK.edn OUT.html FRAME.edn FRAME.json
;; Read one ordinary inner-loop tick as an argument over solution approaches.
(require '[clojure.edn :as edn]
         '[clojure.pprint :as pprint]
         '[clojure.string :as str]
         '[cheshire.core :as json])

(def in (nth *command-line-args* 0))
(def out (nth *command-line-args* 1))
(def frame-edn (nth *command-line-args* 2))
(def frame-json (nth *command-line-args* 3))
(def r (edn/read-string {:default (fn [_ v] v)} (slurp in)))
(def decision (:decision r))
(def certificate (:selection-certificate decision))
(def candidates (:candidates certificate))
(def chosen (:chosen decision))
(def target (:target chosen))

(defn fmt [x] (if (number? x) (format "%.4f" (double x)) "nil"))
(defn esc [x] (-> (str x) (str/replace "&" "&amp;")
                  (str/replace "<" "&lt;") (str/replace ">" "&gt;")))
(defn short-id [id]
  (keyword (or (second (re-find #"/(policy-[0-9a-f]{8})" id)) id)))
(defn digest-bytes [bytes]
  (let [digest (.digest (java.security.MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and 0xff %)) digest))))

(defn flexiarg-part [lines marker]
  (when-let [i (first (keep-indexed
                        (fn [i line]
                          (when (str/starts-with? (str/trim line) marker) i))
                        lines))]
    (let [first-line (str/trim (subs (str/trim (nth lines i)) (count marker)))
          following (->> (drop (inc i) lines)
                         (take-while #(not (str/blank? %)))
                         (map str/trim))]
      (str/trim (str/join " " (cons first-line following))))))

(defn source-file [path]
  (let [file (java.io.File. (str path))]
    (if (.isAbsolute file) file (java.io.File. "/home/joe/code" (str path)))))

(defn warrant [pattern square]
  (let [source (get-in square [:objects :G :source])
        file (source-file (:path source))]
    (if (.isFile file)
      (let [bytes (java.nio.file.Files/readAllBytes (.toPath file))
            current (digest-bytes bytes)
            lines (str/split-lines (String. bytes "UTF-8"))]
        {:pattern pattern
         :source (:path source)
         :recorded-sha256 (:sha256 source)
         :sha256 current
         :source-drift (not= current (:sha256 source))
         :conclusion (flexiarg-part lines "! conclusion:")
         :because (flexiarg-part lines "+ BECAUSE:")})
      {:pattern pattern :source (:path source)
       :recorded-sha256 (:sha256 source) :sha256 nil
       :source-drift :unreadable :conclusion nil :because nil :missing true})))

(def abstention (:abstention certificate))
(def stop-reference (:chosen-reference abstention))
(def stop-row (get-in abstention [:per-policy stop-reference]))
(def stop-G (:G stop-row))

(defn argument [candidate]
  (let [identity (:id candidate)
        policy-id (:id identity)
        nodes (get-in identity [:arrangement :nodes])
        grounds (mapv (fn [node]
                        {:occurrence-id (:occurrence-id node)
                         :pattern (:pattern node)
                         :square (:blend-square node)})
                      nodes)
        stop-terms (get-in abstention [:per-policy policy-id :terms])]
    {:id (short-id policy-id)
     :policy-id policy-id
     :pick (vec (get-in identity [:precedence :co-apply :units]))
     :G (:g candidate)
     :terms stop-terms
     :habit (:habit candidate)
     :f (:f candidate)
     :reason (:reason candidate)
     :grounds grounds
     :warrants (mapv (fn [ground]
                       (warrant (:pattern ground) (:square ground)))
                     grounds)}))

(def args (mapv argument candidates))
(def stopping {:id :stopping :policy-id :stopping :pick nil :G stop-G
               :terms (:terms stop-row) :grounds [] :warrants []})
(def all-args (conj args stopping))
(def by-id (into {} (map (juxt :id identity) all-args)))

(def tie-attacks
  (vec (for [x args y args
             :when (and (not= (:id x) (:id y)) (= (:G x) (:G y)))]
         {:kind :tie :from (:id x) :to (:id y)
          :text (str (name (:id x)) " and " (name (:id y))
                     " have equal recorded G " (:G x))})))
(def stopping-attacks
  (vec (concat
         (for [x args]
           {:kind :stopping :from (:id x) :to :stopping
            :text (str (name (:id x)) " beats its recorded no-action reference")})
         (for [x args]
           {:kind :stopping :from :stopping :to (:id x)
            :text (str "stopping challenges " (name (:id x)))}))))

(def attack-vocabulary-unrecorded
  [{:kind :coverage
    :reason "The tick records per-occurrence squares and situation-source prose, but no field identifies a square's I1 situation as a live observation need; unit-set difference alone is not an attack."}
   {:kind :forbidden
    :reason "The Q6 receipt is not-triggered and names no excluded unit; a unit-addressed Q6 exclusion field would be required to compute forbidden attacks."}])

(def attacks (vec (concat tie-attacks stopping-attacks)))
(defn G [id] (:G (by-id id)))
(def attacks* (mapv #(assoc % :succeeds (not (< (G (:to %)) (G (:from %))))) attacks))
(def defeats (filter :succeeds attacks*))
(defn defeaters [id] (map :from (filter #(= id (:to %)) defeats)))
(defn acceptable? [S id]
  (every? (fn [attacker]
            (some #(contains? S %) (defeaters attacker)))
          (defeaters id)))
(def grounded
  (loop [S #{}]
    (let [next-S (set (filter #(acceptable? S %) (map :id all-args)))]
      (if (= S next-S) S (recur next-S)))))

(def winner (first (filter #(= (:policy-id %) (:id chosen)) args)))
(def losers (remove #(= (:id %) (:id winner)) args))
(def ordered-G (sort (map :G args)))
(def margin (- (second ordered-G) (first ordered-G)))
(def comparison (get-in decision [:selection-law :policy-comparison]))
(def near-tie (:near-tie-threshold comparison))
(def contribution-summary
  (select-keys (:contributions comparison) [:habit :free-energy :G :total]))

(defn unit-text [argument]
  (str/join " → " (map (fn [[pattern occurrence]]
                           (str pattern "#" occurrence)) (:pick argument))))
(defn warrant-text [argument]
  (str/join " " (map #(str "“" (:conclusion %) "”")
                     (filter :conclusion (:warrants argument)))))
(defn grounds-text [argument]
  (str (count (:grounds argument)) " occurrence squares, each retaining G/I1/I2/B, source SHA, and situation-source"))
(defn attack-sentence [attack]
  (str (:text attack) (if (:succeeds attack) " (stands)." " (fails under G).")))

(def prose
  (str/join
    "\n\n"
    (concat
      [(str "The problem: which solution approach should the inner loop use for " target
            ". The claim: " (unit-text winner) ", by " (name (:id winner))
            ". Warrants: " (warrant-text winner) " Grounds: " (grounds-text winner) ".")]
      (for [loser losers]
        (str (name (:id loser)) " argues instead for " (unit-text loser)
             ". Warrants: " (warrant-text loser) " Grounds: " (grounds-text loser) ". "
             (str/join " " (map attack-sentence
                                  (filter #(and (= (:from %) (:id loser))
                                                (= (:to %) (:id winner))) attacks*)))
             " "
             (str/join " " (map attack-sentence
                                  (filter #(and (= (:from %) (:id winner))
                                                (= (:to %) (:id loser))) attacks*)))))
      [(str "What decided: G. "
            (str/join " < " (map #(str (name (:id %)) " " (fmt (:G %)))
                                  (sort-by :G all-args)))
            ". Four of five approaches have exactly the same recorded G; none of habit, free energy, or another retained contribution separates those four. The chosen approach is lower by "
            (fmt margin) " nats. Grounded extension: {"
            (str/join ", " (map name grounded)) "}"
            (if (contains? grounded (:id winner))
              ", which is the record's decision."
              ", which is NOT the record's decision."))])))

(def what-G-read
  (str "What G read: all five candidates have habit 0.2 and action F is nil under certified-zero-history. The retained policy comparison says :decided-by :G and records contributions "
       (pr-str contribution-summary)
       ". Raw candidate G separates the winner by " (fmt margin)
       " nats from four alternatives tied at " (fmt (second ordered-G))
       ". The g-term-decomposition has status "
       (get-in certificate [:g-term-decomposition :status])
       ", with consumed A and Q unrecorded, so the record does not retain a further term-level explanation of that difference. Between the four tied alternatives, the choice is made by nothing read. policy-comparison declares no numerical near-tie threshold ("
       (pr-str near-tie) ")."))

(def frame
  {:schema :cascade/inner-argument-frame-v0
   :run (:run/id r)
   :problem (str "which solution approach for " target)
   :arguments all-args
   :attacks attacks*
   :attack-vocabulary-unrecorded attack-vocabulary-unrecorded
   :preference :G-lower-is-better
   :semantics :grounded-under-preference
   :grounded (vec grounded)
   :decision (:id chosen)
   :winner (:id winner)
   :agrees (contains? grounded (:id winner))
   :margin margin
   :near-tie near-tie
   :policy-comparison-status (:status comparison)
   :prose prose
   :what-G-read what-G-read})

(defn argument-row [argument]
  (str "<tr" (when (= (:id argument) (:id winner)) " class=sel") ">"
       "<td><b>" (esc (name (:id argument))) "</b></td>"
       "<td>" (esc (if (:pick argument) (unit-text argument) "stop")) "</td>"
       "<td>" (esc (if (= :stopping (:id argument)) "no-action reference" (grounds-text argument))) "</td>"
       "<td>" (fmt (:G argument)) "</td></tr>"))

(def html
  (str "<!doctype html><meta charset=utf-8><title>Why this solution approach</title>"
       "<style>body{max-width:76em;margin:2em auto;padding:0 1em;font:16px/1.5 Georgia,serif;color:#222}h1,h2{font-family:system-ui,sans-serif}table{border-collapse:collapse;font-size:85%}td,th{border:1px solid #ccc;padding:.3em .6em;vertical-align:top}.sel{background:#eef8ee}.prose{border:1px solid #cde;background:#f6fbff;padding:.8em}.fails{color:#777}</style>"
       "<h1>Why this solution approach and not that one</h1>"
       "<p>One retained inner-loop tick for <code>" (esc target) "</code>.</p>"
       "<div class=prose>" (str/join "" (map #(str "<p>" (esc %) "</p>") (str/split prose #"\n\n")))
       "<p><i>" (esc what-G-read) "</i></p></div>"
       "<h2>Arguments</h2><table><tr><th>argument</th><th>approach</th><th>grounds</th><th>G</th></tr>"
       (str/join "" (map argument-row all-args)) "</table>"
       "<h2>Attacks</h2><ul>"
       (str/join "" (for [attack attacks*]
                      (str "<li class=" (when-not (:succeeds attack) "fails") ">"
                           (esc (:text attack)) " — "
                           (if (:succeeds attack) "stands" "fails under G") "</li>")))
       "</ul><h2>Unrecorded attack vocabulary</h2><ul>"
       (str/join "" (for [entry attack-vocabulary-unrecorded]
                      (str "<li><b>" (name (:kind entry)) ":</b> " (esc (:reason entry)) "</li>")))
       "</ul><p>Grounded: {" (esc (str/join ", " (map name grounded)))
       "}. Decision agreement: " (:agrees frame) ".</p>"))

(spit out html)
(spit frame-edn (with-out-str (pprint/pprint frame)))
(spit frame-json (json/generate-string frame {:pretty true}))
(println "wrote" out "," frame-edn "," frame-json)
(println "attacks:" (mapv (juxt :from :to :kind :succeeds) attacks*))
(println "grounded:" grounded "agrees:" (:agrees frame))
(println)
(println what-G-read)
