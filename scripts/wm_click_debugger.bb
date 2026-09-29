#!/usr/bin/env bb
;; wm_click_debugger.bb RUN-RECORD -- print, as JSON, what a click that did
;; not select was refused on: the failure, and every refused target grouped
;; by refusal kind. wm_click_repair_loop.py writes this into the click's
;; debugger page and hands it to the repair seat (Joe, 2026-09-29: an
;; abstained click is a problem to debug, not a pattern to learn from).
(require '[clojure.edn :as edn] '[cheshire.core :as json])

(let [[path] *command-line-args*
      r (edn/read-string {:default (fn [_ v] v)} (slurp path))
      targets (get-in r [:decision :abstention :targets])
      kind-of (fn [t] (str (name (or (:kind t) :unknown)) "/" (name (or (:missing t) :unknown))))]
  (println
   (json/generate-string
    {:run-id (:run/id r)
     :failure (select-keys (:failure r) [:kind :stage :error])
     :abstention-status (get-in r [:decision :abstention :status])
     :chosen (get-in r [:decision :chosen])
     :open-stop-lines (:open-stop-lines r)
     :counts (frequencies (map kind-of targets))
     :targets (for [t targets]
                {:target (:target t) :refusal (kind-of t)
                 :declines (pr-str (:declines t))})})))
