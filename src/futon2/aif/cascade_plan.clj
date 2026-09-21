(ns futon2.aif.cascade-plan
  "Pure presentation of recorded construction evidence; never observes or infers guards."
  (:require [clojure.string :as str]))

(def max-plan-chars 24000)

(defn- text [x]
  (cond
    (nil? x) "not recorded"
    (map? x) (str/join "; " (for [[k v] (sort-by (comp str key) x)]
                              (str (text k) " = " (text v))))
    (set? x) (str/join ", " (sort (map text x)))
    (sequential? x) (str/join " / " (map text x))
    :else (str x)))

(defn- bounded [s limit]
  (if (<= (count s) limit)
    s
    (let [prefix (max 0 (- limit 40))
          removed (- (count s) prefix)]
      (str (subs s 0 prefix) "\n[truncated " removed " chars]\n"))))

(defn- condition-text [{:keys [condition status witness]}]
  (let [{:keys [token expected observation]} witness
        evidence (:evidence observation)]
    (str "  - " (if token (str (text token) " expected " (text expected)) (text condition))
         ": " (if (= :established status) "established" "not established")
         " (recorded status: " (text status) ")"
         "; check " (text (:check observation))
         "; observed " (text (:observed observation))
         "; resolved sha " (text (:resolved-sha evidence))
         "; evidence " (text evidence) "\n")))

(defn- pattern-text [index pattern boxes holes receipts]
  (let [id (if (map? pattern) (:id pattern) pattern)
        box (first (filter #(= (str id) (str (:id %))) boxes))
        hole (first (filter #(= (str id) (str (:unfolded-pattern %))) holes))
        receipt (or (:interpretation-receipt box) (get receipts id))
        source (:source receipt)
        conditions (or (:conditions box) (:guard-evidence hole))]
    (str "pattern " (inc index) ": " (text id) "\n"
         "  flexiarg: " (text (:path source)) "\n"
         "  sha256: " (text (:sha256 source)) "\n"
         "  reading: " (text (:reading receipt)) "\n"
         "  scope limit: " (text (:scope-limit receipt)) "\n"
         "  observation limit: " (text (:observation-limit receipt)) "\n"
         "  guards:\n"
         (if (seq conditions)
           (apply str (map condition-text conditions))
           (str "  - not established: no recorded guard checks; declared guard: "
                (text (:guard pattern)) "\n"))
         "  produces: " (text (or (:produces box) (:produces pattern))) "\n")))

(defn cascade-plan-text
  "Render patterns in precedence order, then holes and wires. Maximum 24,000
  characters: 18,000 shared equally between pattern blocks, 2,800 each for
  holes and wires, with room for headings. Every truncation counts omitted
  characters explicitly. Extremely numerous patterns that cannot each fit a
  truncation marker are covered by a final counted section truncation."
  [construction]
  (let [wiring (or (:wiring construction) (get-in construction [:fold-output :wiring]))
        boxes (:boxes wiring)
        holes (or (get-in construction [:fold-output :policy-holes])
                  (:policy-holes wiring) (:policy-holes construction))
        patterns (or (seq (:precedence construction))
                     (seq (map :id boxes)) (:shown construction))
        blocks (map-indexed #(pattern-text %1 %2 boxes holes
                                          (:interpretation-receipts construction)) patterns)
        allowance (quot 18000 (max 1 (count patterns)))
        pattern-section (if (< allowance 40)
                          (bounded (apply str blocks) 18000)
                          (apply str (map #(bounded % allowance) blocks)))
        hole-section (if (seq holes)
                       (str "holes:\n"
                            (str/join "\n" (map #(str "- " (text %)) holes)))
                       "holes: none")
        wires (:wires wiring)
        wire-section (if (seq wires)
                       (str "wires:\n"
                            (str/join "\n" (map #(str "- " (text %)) wires)))
                       (if (some? wires) "wires: none" "wires: not recorded"))]
    (str "Recorded cascade plan (limit " max-plan-chars " chars).\n"
         (if (seq patterns) pattern-section "patterns: none recorded\n")
         "\n" (bounded hole-section 2800) "\n" (bounded wire-section 2800) "\n")))
