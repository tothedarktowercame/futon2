#!/usr/bin/env bb
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[clojure.string :as str])

(def here (.getCanonicalFile (io/file (or (System/getenv "WM_CONTRACT_DIR") "."))))
(def accounting-file (io/file here "variable-situation-accounting.edn"))
(def audit-file (io/file here "runs/U27-hole-closability/audit.edn"))
(def audit-md-file (io/file here "runs/U27-hole-closability/AUDIT.md"))

(defn fail! [s] (binding [*out* *err*] (println s)) (System/exit 1))
(defn read-edn [f]
  (try (edn/read-string (slurp f))
       (catch Throwable t (fail! (str "cannot read " f ": " (.getMessage t))))))

(let [accounting (read-edn accounting-file)
      rows (->> (:rows accounting)
                (filter #(= :open-hole (:content-status %)))
                (mapv (fn [r]
                        (cond-> (select-keys r [:name :row-source :area :closability
                                                :readiness :owner :basis :runtime-evidence])
                          :always (assoc :pointers
                                         (vec (keep :licence (:rung-ladder r))))))))
      names (set (map :name rows))
      required #{"wmRunConformsToWiring" "enactedEqualsSelectedWhenRankOneGated"}
      missing (seq (remove names required))
      authority (:authority accounting)
      contract-count (count (filter #(= :contract-declaration (:row-source %)) rows))
      audit {:schema :wm/hole-closability-audit-v2
             :as-of (:as-of accounting)
             :authority {:contract-file "mathlib4/DarkTower/WarMachine/holes-contract.json"
                         :contract-git-sha (:contract-git-sha authority)
                         :contract-sha256 (:contract-sha256 authority)
                         :accounting "futon2/holes/labs/wm-contract/variable-situation-accounting.edn"
                         :accounting-open-hole-rows (count rows)}
             :counts {:rows (count rows)
                      :contract-declarations contract-count
                      :glossary-side (- (count rows) contract-count)
                      :closability (frequencies (map :closability rows))
                      :readiness (frequencies (map :readiness rows))}
             :rows rows}
      md (str "# U27 hole-closability audit — refreshed\n\n"
              "As of `" (:as-of audit) "`; contract `" (:contract-git-sha (:authority audit)) "`.\n\n"
              "This report is generated from `variable-situation-accounting.edn`; "
              "the EDN artifact is authoritative. Open rows: " (count rows)
              " (" contract-count " contract declarations, " (- (count rows) contract-count)
              " glossary-side).\n\n"
              (str/join "\n" (for [r rows]
                                (str "- `" (:name r) "`: `" (:closability r) "`, `"
                                     (:readiness r) "`; " (or (:basis r) "not found")))) "\n")]
  (when missing (fail! (str "required RUN4 holes missing: " (pr-str missing))))
  (spit audit-file (with-out-str (pp/pprint audit)))
  (spit audit-md-file md)
  (println "hole-closability refresh: WROTE" (count rows) "rows at" (:contract-git-sha (:authority audit))))
