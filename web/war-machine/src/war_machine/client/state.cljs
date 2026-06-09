(ns war-machine.client.state
  "Reagent ratoms for the web War Machine — one-to-one with the atoms in the
   Swing version (futon0/scripts/futon0/report/war_machine_visual.clj):

     data-atom     → full scan snapshot from /api/war-machine
     view-mode     → :stack | :self-watch | :aif-stack | :missions | :patterns
                     | :capabilities | :operator
     replay-atom   → {session-id {:steps :step-idx :color-idx :session-id}
                      :playing? bool} — the session-ant swarm
     hotspot-atom  → {node-id intensity} — decays per tick, bumped on visit
     tick-atom     → master tick counter (drives HUD)
     selected      → currently highlighted node (click detail)
     hovered       → node id currently under the pointer (for label expansion)"
  (:require [reagent.core :as r]))

(defonce data       (r/atom nil))
(defonce aif-data   (r/atom nil))   ;; payload from /api/alpha/aif-stack/live
(defonce capability-star-map (r/atom nil))
(defonce operator-bulletin (r/atom nil))
(defonce operator-forward-model (r/atom nil))
(defonce view-mode  (r/atom :stack))

;; Audacity-style activity waveform / timeline state.
;;   :selection      → {:start-ms ms :end-ms ms} (nil = play whole timeline)
;;   :drag           → {:anchor-ms ms} during a click-drag (nil otherwise)
;;   :hover-ms       → ms-since-epoch under cursor (nil when not hovering)
(defonce waveform   (r/atom {:selection nil :drag nil :hover-ms nil}))
(defonce track-ui   (r/atom {:enabled {} :selected nil}))
(defonce replay     (r/atom {:playing? true}))
(defonce hotspot    (r/atom {}))
(defonce tick       (r/atom 0))
(defonce selected   (r/atom nil))
(defonce hovered    (r/atom nil))
(defonce viewport   (r/atom {:w 1000 :h 700}))

;; Time window for the war-machine scan.  Cycles 14 ↔ 90 via the toolbar
;; toggle; api/load! reads this to construct the request URL.
(defonce days-window (r/atom 14))
