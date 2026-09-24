;; E-cascade-real interpretation probe: freeze the input set.
;; Run from /home/joe/code/futon2:
;;   clojure -M holes/labs/wm-contract/E-cascade-real/probe-interp/select_inputs.clj
;; Read-only. Writes inputs.edn next to this file.
;; Selection: live missions with observable hole wants (mission-hole-wants),
;; excluding targets that already have a declared cascade source; keep those
;; with 2-6 wanted tokens; order by target id; take the first 10.
(require '[futon2.aif.mission-hole-wants :as mhw]
         '[futon2.aif.mission-registry :as registry]
         '[futon2.aif.cascade-sources :as cs]
         '[clojure.pprint :as pp])
(let [declared (set (keys (:universes (cs/load-declared cs/default-dir))))
      {:keys [sources coverage]} (mhw/mission-sources registry/default-code-root
                                                      (:missions (registry/load-missions)))
      pool (->> sources
                (remove #(declared (:target %)))
                (filter #(<= 2 (count (:want %)) 6))
                (sort-by :target))
      chosen (vec (take 10 pool))
      out (str "holes/labs/wm-contract/E-cascade-real/probe-interp/inputs.edn")]
  (spit out (with-out-str
              (pp/pprint {:schema :e-cascade-real/probe-inputs-v1
                          :coverage coverage
                          :pool-size (count pool)
                          :targets (mapv #(select-keys % [:target :want :universe :locators :holes]) chosen)})))
  (println "pool" (count pool) "chosen" (mapv :target chosen))
  (println (mapv (juxt :target (comp count :want)) chosen)))
(shutdown-agents)
