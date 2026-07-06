;; gate-decision smoke test — verifies decision-entry logic + flag behavior
;; Run: cd futon2 && clojure -M -e '(load-file "test/futon2/gate_decision_test.clj")'
(in-ns 'user)
(require '[futon2.report.cascade-lane :as lane])

;; Access the private decision-entry fn
(def decision-entry @(get (ns-interns 'futon2.report.cascade-lane) 'decision-entry))

(defn check [label pass?]
  (println (if pass? "  PASS:" "  FAIL:") label)
  (when-not pass? (System/exit 1)))

(println "=== gate-the-decision tests ===")

;; (a) rank-1 :advance-mission target extracted as decision entry
(let [ranked [{:rank 1, :action {:type :advance-mission, :target "M-first-flights"}}
              {:rank 2, :action {:type :open-mission, :target "M-canon-fingerprint-store"}}]
      de (decision-entry ranked)]
  (check "(a) decision target is M-first-flights"
         (= "M-first-flights" (get-in de [:action :target])))
  (check "(a) decision type is :advance-mission"
         (= :advance-mission (get-in de [:action :type]))))

;; (b) dedup: rank-1 IS open-mission => nil (no separate decision entry)
(let [ranked [{:rank 1, :action {:type :open-mission, :target "M-first-flights"}}
              {:rank 2, :action {:type :open-mission, :target "M-canon-fingerprint-store"}}]
      de (decision-entry ranked)]
  (check "(b) rank-1 open-mission returns nil (dedup automatic)"
         (nil? de)))

;; (c) flag-off: flag is dynamic, default true, bindable to false
(check "(c) flag default is true" lane/*gate-decision-target?*)
(binding [lane/*gate-decision-target?* false]
  (check "(c) flag binds to false" (not lane/*gate-decision-target?*)))

;; (d) rank-1 without :target => nil (skipped safely)
(let [ranked [{:rank 1, :action {:type :advance-mission}}  ; no :target
              {:rank 2, :action {:type :open-mission, :target "M-canon"}}]
      de (decision-entry ranked)]
  (check "(d) no :target returns nil" (nil? de)))

(println "\n4 tests, 4 pass, 0 fail")
