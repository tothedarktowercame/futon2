(ns war-machine.client.labels
  "Display-only aliases for overloaded stack identifiers.

   Canonical ids stay unchanged in the data model and DOM attributes; these
   helpers only control what the operator sees in the War Machine surface."
  (:require [clojure.string :as str]))

(def ^:private stack-spine-aliases
  {"S0" "🐜0"
   "S1" "🐜1"
   "S2" "🐜2"
   "S3" "🐜3"
   "S4" "🐜4"
   "S5" "🐜5"
   "S6" "🐜6"
   "S7" "🐜7"
   "S8" "🐜8"
   "S9" "🐜9"})

(def ^:private stack-spine-hover-titles
  {"S0" "self-maintaining system"
   "S1" "Pillar I: Argument"
   "S2" "Pillar II: Invariants"
   "S3" "Pillar III: Missions"
   "S4" "5-step cycle"
   "S5" "self-representing stack"
   "S6" "AIF stack"
   "S7" "pattern canon + audit"
   "S8" "durable substrate"
   "S9" "sorry currency"})

(def ^:private strategic-sorry-aliases
  {"SORRY-market-interface"   "🌐1-market-interface"
   "SORRY-mode-violation"     "🌐2-mode-violation"
   "SORRY-peer-eval-artifact" "🌐3-peer-eval-artifact"
   "SORRY-paragogy-revenue"   "🌐4-paragogy-revenue"
   "SORRY-vsat-revenue"       "🌐5-vsat-revenue"
   "SORRY-governance-interface" "🌐6-governance-interface"
   "SORRY-novelty-floor"      "🌐7-novelty-floor"
   "SORRY-policy-transition"  "🌐8-policy-transition"})

(defn id->text [id]
  (cond
    (keyword? id) (name id)
    (nil? id) nil
    :else (str id)))

(defn stack-spine-display-id
  "War Machine alias for the stack-level spine ids from THE-STACK.aif.edn."
  [id]
  (let [s (id->text id)]
    (or (get stack-spine-aliases s) s)))

(defn stack-spine-hover-label
  "Expanded in-hex title shown only while hovering a stack spine node."
  [id]
  (let [s (id->text id)]
    (or (get stack-spine-hover-titles s)
        (stack-spine-display-id s))))

(defn strategic-sorry-display-id
  "War Machine alias for Strategic SORRY ids from alignment.edn :sorry-topology."
  [id]
  (let [s (id->text id)]
    (or (get strategic-sorry-aliases s) s)))

(defn strategic-sorry-short-label
  "Compact in-hex label for a Strategic SORRY. Example:
   SORRY-market-interface -> 🌐1."
  [id]
  (let [display (strategic-sorry-display-id id)]
    (or (second (re-matches #"^(🌐[0-9]+)(?:-.+)?$" (or display "")))
        display)))

(defn strategic-sorry-hover-label
  "Expanded in-hex label shown only while hovering a Strategic SORRY.
   Example: SORRY-market-interface -> market-interface."
  [id]
  (let [canon (id->text id)]
    (or (second (re-matches #"^(?:SORRY-|sorry-)(.+)$" (or canon "")))
        canon)))

(defn display-id-with-canonical
  "Human label for detail panes: alias first, canonical id second when they
   differ. Example: 🐜6 (S6)."
  [id]
  (let [canon (id->text id)
        disp  (stack-spine-display-id id)]
    (cond
      (nil? canon) nil
      (= canon disp) canon
      :else (str disp " (" canon ")"))))

(defn stack-text->display
  "Rewrite stack-spine references inside prose so the War Machine surface uses
   the same visible namespace as the hex labels."
  [s]
  (if (string? s)
    (str/replace s #"\bS([0-9])\b"
                 (fn [[match]]
                   (stack-spine-display-id match)))
    s))
