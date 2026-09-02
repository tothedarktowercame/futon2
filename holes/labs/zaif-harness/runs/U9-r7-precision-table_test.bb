#!/usr/bin/env bb
(require '[clojure.edn :as edn])

(def table-path "holes/labs/zaif-harness/runs/U9-r7-precision-table.edn")
(def fixture-path
  "holes/labs/wm-contract/runs/U12-c-mis-falsifier/node-fixtures/801976e7-R7.edn")

(defn read-edn [path]
  (edn/read-string (slurp path)))

(defn row [table channel-class]
  (some #(when (= channel-class (:channel-class %)) %) (:rows table)))

(defn check! [pred code data]
  (when-not pred
    (throw (ex-info (name code) (assoc data :finding code)))))

(let [table (read-edn table-path)
      fixture (read-edn fixture-path)
      declared (row table :declared-operator-mark)
      lexical (row table :lexical-probe)
      tool (row table :tool-result)
      wm (row table :war-machine-observation)]
  (check! (> (:precision declared) (:precision lexical))
    :u9/declared-does-not-outweigh-lexical
    {:declared (:precision declared) :lexical (:precision lexical)})
  (check! (= {:status :absent :reason :no-measured-tool-result-precision}
             (select-keys (:precision tool) [:status :reason]))
    :u9/tool-precision-not-typed-absence {})
  (check! (= 8 (:covered-count wm)) :u9/wm-covered-count-changed {})
  (check! (= 6 (count (:absent-not-zero wm))) :u9/wm-absence-count-changed {})
  (check! (= (:run/id fixture) (:run/id wm)) :u9/live-run-id-mismatch {})
  (check! (= (get-in fixture [:value :coupling-density :precision])
             (get-in wm [:precision :coupling-density]))
    :u9/live-precision-pin-mismatch {})
  (check! (= 7.285663818719639
             (get-in fixture [:value :coupling-density :precision]))
    :u9/live-precision-value-changed {})
  (println "U9 R7 precision table: 7 assertions green; declared 1.0 > lexical 0.42; live pin 801976e7 coupling-density 7.285663818719639"))
