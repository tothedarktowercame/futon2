#!/usr/bin/env clojure
(require '[futon2.aif.pattern-graph-pin :as graph-pin])

(let [[source root] *command-line-args*
      result (if (and source root)
               (graph-pin/publish! source root)
               {:status :refused :kind :graph-publication-arguments})]
  (println (pr-str result))
  (when-not (= :published (:status result))
    (System/exit 1)))
