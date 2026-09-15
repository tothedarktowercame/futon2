#!/usr/bin/env bb
;; Run the adapter on a real state packet: classify under both wirings,
;; emit dry-run receipts + meters. NO live kill (execute! refuses).
(require '[cheshire.core :as json]
         '[clojure.pprint :as pp]
         '[buffer-cleaner.classify :as c])

(def raw (json/parse-string (slurp (or (first *command-line-args*) "runs/state-packet.json"))))
;; normalize JSON booleans absent->"false" style already handled by truthy? checks
(def packet {:buffers (mapv (fn [b] {:name (get b "name")
                                     :kind (get b "kind")
                                     :file (str (get b "file"))
                                     :modified (str (get b "modified"))
                                     :has-process (get b "has-process")
                                     :visible (get b "visible")
                                     :active-agent (get b "active-agent")
                                     :server-clients (get b "server-clients")
                                     :display-age-seconds (get b "display-age-seconds")})
                           (get raw "buffers"))})
;; fuel comes from the NODE ARGS (library declaration), not duplicated constants
(def fuel-args (:fuel (c/load-edn "../../../../../futon3/library/buffer-cleaner/nodes/kill.params.edn")))
(def categories (assoc (c/load-edn "../../../../../futon3/library/buffer-cleaner/params/categories.edn")
                       :fuel fuel-args))
(defn wiring [p] (assoc (c/load-edn (str "../../../../../futon3/library/buffer-cleaner/" p)) :wiring/id (keyword (last (re-find #"/(\w+)\.edn$" p)))))
(def wirings [(wiring "wirings/aggressive.edn") (wiring "wirings/conservative.edn")])
(def yield-args (c/load-edn "../../../../../futon3/library/buffer-cleaner/nodes/yield-settled.params.edn"))
(doseq [w wirings]
  (let [r (c/classify-packet packet w categories)
        out (assoc (select-keys r [:wiring :receipts :meters :sources])
                   :revisit-truth (:revisit-truth yield-args)
                   :clean-enough-threshold (:clean-enough-threshold yield-args))]
    (spit (str "runs/receipts-" (name (:wiring r)) ".edn")
          (with-out-str (pp/pprint out)))
    (pp/pprint {:wiring (:wiring r) :meters (:meters r)})))
