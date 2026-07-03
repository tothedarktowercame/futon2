#!/usr/bin/env bb
;; r18_badges_to_js.bb — regenerate the explainer's embedded R18 badge data.
;; file:// pages can't fetch local files, so aif-wiring-explainer.html embeds the
;; badges as a JS const. After a re-audit updates data/r18-badges.edn, run:
;;   bb scripts/r18_badges_to_js.bb   # prints the replacement block
;; and paste it between the R18-BADGES-BEGIN/END markers in the explainer.
(require '[cheshire.core :as json])
(let [d (clojure.edn/read-string (slurp "data/r18-badges.edn"))
      qs (into {} (map (fn [[k v]]
                         [(name k) (-> v
                                       (update :badge name)
                                       (select-keys [:badge :claims :cite :code-ref :computes :repair :note]))])
                       (:quantities d)))]
  (println (str "const R18B=" (json/generate-string qs) ";")))
