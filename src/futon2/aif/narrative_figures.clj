(ns futon2.aif.narrative-figures
  "Pure SVG figures from retained narrative data; no IO or model execution."
  (:require [clojure.string :as str]
            [clojure.set :as set]))

(defn- display [x]
  (cond (nil? x) "not recorded in this run"
        (set? x) (str/join ", " (sort (map display x)))
        (keyword? x) (str x)
        :else (str x)))
(defn- escape-xml [s]
  (str/escape (str s) {\& "&amp;" \< "&lt;" \> "&gt;" \" "&quot;" \' "&apos;"}))
(defn- tag [name attrs & children]
  (str "<" name (apply str (for [[k v] (sort-by key attrs)]
                            (str " " k "=\"" (escape-xml (if (ratio? v) (double v) v)) "\""))) ">"
       (apply str children) "</" name ">\n"))
(defn- txt [x y s & [attrs]]
  (tag "text" (merge {"x" x "y" y} attrs) (escape-xml s)))
(defn- line [x1 y1 x2 y2 attrs]
  (tag "line" (merge {"x1" x1 "y1" y1 "x2" x2 "y2" y2 "stroke" "#9ca3af"} attrs)))
(defn- finite? [x] (and (number? x) (Double/isFinite (double x))))
(defn- mass? [x] (and (finite? x) (<= 0 x 1)))
(defn- number-label [x]
  (if (finite? x) (String/format java.util.Locale/ROOT "%.6g" (to-array [(double x)])) (display x)))
(defn- wrap-lines [s n]
  ;; Hard wrapping preserves every character, including long ids without spaces.
  (map #(apply str %) (partition-all n (display s))))
(defn- text-lines [x y lines & [attrs]]
  (apply str (map-indexed #(txt x (+ y (* 16 %1)) %2 attrs) lines)))
(defn- svg [height title content & [width]]
  (str "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
       (tag "svg" {"xmlns" "http://www.w3.org/2000/svg" "width" (or width 800)
                   "height" height "viewBox" (str "0 0 " (or width 800) " " height)
                   "role" "img" "aria-label" title "font-family" "sans-serif" "font-size" 13 "fill" "#25313c"}
            (tag "title" {} (escape-xml title))
            (tag "rect" {"width" "100%" "height" "100%" "fill" "white"}) content)))

(defn selection-svg
  "Rows are scored candidates. Delta bars use one linear scale; missing values
  are labelled, never replaced by zero. Habit E is shown without normalization."
  [{:keys [rows chosen decided-by declines]}]
  (let [rows (sort-by (juxt #(if (finite? (:posterior %)) (- (:posterior %)) Double/POSITIVE_INFINITY)
                            #(str (:target %)) #(str (:cascade-id %))) rows)
        gs (filter finite? (map :G rows)) gmin (when (seq gs) (apply min gs))
        spread (when gmin (- (apply max gs) gmin))
        layouts (reductions + 110 (map #(max 64 (+ 38 (* 16 (count (wrap-lines (:target %) 37))))) rows))
        bottom (last layouts) gx 342 px 580 pw 190
        declined-lines (mapcat #(wrap-lines (str (display (:target %)) " " (display (:candidate %))
                                                " — " (display (or (:reason %) (:kind %)))) 102)
                              (sort-by (juxt #(str (:target %)) #(str (:candidate %)) #(str (:reason %))) declines))
        height (+ bottom 105 (* 16 (count declined-lines)))]
    (svg height "Candidate selection: relative G and posterior mass"
         (str (txt 24 28 "Candidate selection" {"font-size" 18})
              (txt 24 52 "Chosen row in blue; dashed line = uniform 1/N; dark tick = habit E.")
              (txt gx 76 "ΔG = G − min G (nats)")
              (txt gx 94 (str "min G = " (display gmin)))
              (txt px 76 "Posterior mass [0, 1]")
              (line gx 102 gx bottom {}) (line px 102 px bottom {})
              (line (+ px pw) 102 (+ px pw) bottom {"stroke" "#e5e7eb"})
              (when (seq rows)
                (line (+ px (/ pw (count rows))) 102 (+ px (/ pw (count rows))) bottom
                      {"stroke-dasharray" "4 4" "class" "uniform" "data-value" (/ 1.0 (count rows))}))
              (apply str
                     (map (fn [r y]
                            (let [chosen? (and (= (:target r) (:target chosen))
                                               (= (:cascade-id r) (or (:cascade-id chosen) (:id chosen))))
                                  color (if chosen? "#176b82" "#89939e")
                                  delta (when (and gmin (finite? (:G r))) (- (:G r) gmin))
                                  labels (wrap-lines (:target r) 37)]
                              (tag "g" {"class" (if chosen? "candidate chosen" "candidate") "data-target" (:target r)
                                        "data-cascade" (display (:cascade-id r))}
                                   (tag "title" {} (escape-xml (str (:target r) " " (:cascade-id r))))
                                   (text-lines 24 (+ y 14) labels {"fill" color})
                                   (txt 24 (+ y 16 (* 16 (count labels)))
                                        (str (display (:cascade-id r)) (when chosen? " · chosen")) {"fill" color})
                                   (if delta
                                     (str (tag "rect" {"class" "delta-g" "x" gx "y" (+ y 2) "height" 13
                                                       "width" (if (pos? spread) (* 190.0 (/ delta spread)) 0)
                                                       "fill" color "data-value" delta})
                                          (txt gx (+ y 34) (number-label delta)))
                                     (txt gx (+ y 34) (display (:G r))))
                                   (if (mass? (:posterior r))
                                     (str (tag "rect" {"class" "posterior" "x" px "y" (+ y 2) "height" 13
                                                       "width" (* pw (:posterior r)) "fill" color
                                                       "data-value" (:posterior r)})
                                          (txt px (+ y 34) (number-label (:posterior r))))
                                     (txt px (+ y 34) "posterior: not recorded / invalid" {"font-size" 11}))
                                   (if (mass? (:habit r))
                                     (let [x (+ px (* pw (:habit r)))]
                                       (line x y x (+ y 18) {"class" "habit" "data-value" (:habit r)
                                                            "stroke" "#111827" "stroke-width" 2}))
                                     (txt px (+ y 50) "E: not recorded / outside [0,1]" {"font-size" 11})))))
                          rows layouts))
              (when (empty? rows) (txt 24 124 "Scored candidates: not recorded in this run."))
              (txt gx (+ bottom 18) "0")
              (txt (+ gx 190) (+ bottom 18) (number-label spread) {"text-anchor" "end"})
              (txt px (+ bottom 18) "0") (txt (+ px pw) (+ bottom 18) "1" {"text-anchor" "end"})
              (txt 24 (+ bottom 44) (str "decided by: " (display decided-by)))
              (txt 24 (+ bottom 68) (if (seq declines) "Refused candidates:" "Refused candidates: none recorded.") {"fill" "#68717b"})
              (text-lines 24 (+ bottom 88) declined-lines {"fill" "#68717b" "font-size" 12})))))

(defn- needs [guard]
  (into (set (or (:needs guard) (:present guard))) (mapcat needs (:clauses guard))))
(defn- pattern-id [p] (if (map? p) (:id p) p))
(defn- dependency-edges [{:keys [patterns need-edges wires]}]
  (cond
    (some? need-edges) need-edges
    (some? wires) (map #(if (map? %) [(:from %) (:to %)] %) wires)
    :else (for [p patterns q patterns :when (not= (pattern-id p) (pattern-id q))
                :when (seq (set/intersection (set (:produces p)) (needs (:guard q))))]
            [(pattern-id p) (pattern-id q)])))

(defn cascade-svg
  "Precedence locates boxes, but does not imply dependency arrows. Wanted-token
  fills report observations; an unknown observation never becomes false."
  [{:keys [target patterns outcomes shape semilattice prediction-source] :as data}]
  (let [n (count patterns) boxw (max 150 (min 330 (- (/ 510 (max 1 n)) 24)))
        step (+ boxw 24) wantx (max 570 (+ 24 (* n step))) width (+ wantx 230)
        chars (max 16 (int (/ (- boxw 16) 7)))
        token-label #(if (and (vector? %) (= target (first %))) (display (second %)) (pr-str %))
        labels (mapv (fn [p] (concat (wrap-lines (pattern-id p) chars) ["produces:"]
                                     (if (map? p)
                                       (if (seq (:produces p)) (mapcat #(wrap-lines (token-label %) chars) (sort-by pr-str (:produces p)))
                                           [(if (contains? p :produces) "none" "not recorded")]) ["not recorded"]))) patterns)
        boxh (+ 22 (* 16 (apply max 1 (map count labels))))
        bottom (max (+ 110 boxh) (+ 100 (* 64 (count outcomes))))
        positions (zipmap (map pattern-id patterns) (map #(+ 24 (* step %)) (range)))
        edges (sort-by pr-str (set (dependency-edges data)))
        valid (filter #(every? (set (keys positions)) %) edges)
        shape-text (or (:shape-caption data) (str "shape: " (if shape (display shape)
                                   (str "not computed in this run"
                                        (when (= [] semilattice) " (literal semilattice field)")))))]
    (svg (+ bottom 136) "Cascade precedence, dependencies, and wanted-token outcomes"
         (str (txt 24 28 "Cascade and wanted-token outcomes" {"font-size" 18})
              (txt 24 52 (str "Selected target: " (display target)))
              (txt 24 75 "Patterns in precedence order; arrows show need-edges / wires only.")
              (tag "defs" {} (tag "marker" {"id" "arrow" "markerWidth" 8 "markerHeight" 8 "refX" 7 "refY" 4 "orient" "auto"}
                                   (tag "path" {"d" "M0,0 L8,4 L0,8 Z" "fill" "#68717b"})))
              (apply str (for [[i [p q]] (map-indexed vector valid)
                               :let [x1 (+ (positions p) (/ boxw 2.0)) x2 (+ (positions q) (/ boxw 2.0))
                                     y (- 96 (* 5 (mod i 3)))]]
                           (tag "path" {"class" "need-edge" "data-from" (display p) "data-to" (display q)
                                        "d" (str "M" x1 ",110 C" x1 "," y " " x2 "," y " " x2 ",110")
                                        "fill" "none" "stroke" "#68717b" "marker-end" "url(#arrow)"})))
              (apply str (for [[i lines] (map-indexed vector labels) :let [x (+ 24 (* step i))]]
                           (tag "g" {"class" "pattern" "data-pattern" (display (pattern-id (nth patterns i)))}
                                (tag "rect" {"x" x "y" 110 "width" boxw "height" boxh "rx" 5 "fill" "#f6f7f8" "stroke" "#68717b"})
                                (text-lines (+ x 8) 132 lines {"font-size" 12}))))
              (when (empty? patterns) (txt 24 132 "Patterns: not recorded in this run."))
              (txt wantx 96 "Wanted tokens")
              (apply str (for [[i row] (map-indexed vector (sort-by (comp pr-str :token) outcomes))
                               :let [observed (:observed row) y (+ 110 (* 64 i))
                                     state (cond (true? observed) "observed-true" (false? observed) "observed-false" :else "observation-missing")
                                     fill (case state "observed-true" "#dceee3" "observed-false" "#f9dfdc" "white")]]
                           (tag "g" {"class" (str "want " state) "data-token" (token-label (:token row))
                                     "data-predicted" (display (:predicted row))}
                                (tag "rect" {"x" wantx "y" y "width" 208 "height" 52 "rx" 5 "fill" fill "stroke" "#68717b"})
                                (txt (+ wantx 8) (+ y 18) (token-label (:token row)) {"font-size" 12})
                                (txt (+ wantx 8) (+ y 34) (str "prediction: " (display (:predicted row))) {"font-size" 11})
                                (txt (+ wantx 8) (+ y 47) (str "observed: " (if (boolean? observed) (str observed) "not recorded")) {"font-size" 11}))))
              (when (empty? outcomes) (txt wantx 132 "Wanted tokens: not recorded." {"font-size" 11}))
              (txt 24 (+ bottom 30) shape-text)
              (txt 24 (+ bottom 52) "Outline: observation not recorded. Green / red fill: observed true / false.")
              (txt 24 (+ bottom 96) (str "Model prediction: " (display prediction-source)))
              (txt 24 (+ bottom 74) (str (count valid) " dependency arrow(s)."
                                        (when (not= (count valid) (count edges))
                                          (str " " (- (count edges) (count valid)) " wire(s) have unrecorded endpoints.")))))
         width)))
