;; portfolio-action-proposer dry-run test
;; Run: cd futon2 && clojure -M -e '(load-file "test/futon2/portfolio_dry_run.clj")'
(in-ns 'user)
(require '[futon2.aif.portfolio-action-proposer :as pap])
(require '[futon2.aif.action-proposer :as ap])

(defn check [label pass?]
  (println (if pass? "  PASS:" "  FAIL:") label)
  (when-not pass? (System/exit 1)))

(println "=== portfolio-action-proposer dry-run ===")

;; Synthetic state: 3 missions, one complete, one in-progress, one open
(def test-state
  {:missions
   [{:id "M-first-flights" :status-class :active :title "first flights"}
    {:id "M-bayesian-structure-learning" :status-class :complete :title "bayesian"}
    {:id "M-canon-fingerprint-store" :status-class :active :title "canon"}]
   :escrow-missions #{"M-first-flights"}})

;; (a) dry-run-portfolio shows all four families
(println "\n(a) dry-run-portfolio output:")
(def portfolio (pap/dry-run-portfolio test-state))
(doseq [[k v] portfolio] (println (format "    %s: %d" (name k) v)))

(check "(a) advance-mission count = 3" (= 3 (:advance-mission portfolio)))
(check "(a) close-mission count = 1 (one :complete)" (= 1 (:close-mission portfolio)))
(check "(a) survey-mission count = 3 (all open)" (= 3 (:survey-mission portfolio)))
(check "(a) apply-cascade count = 1 (one in escrow)" (= 1 (:apply-cascade portfolio)))

;; (b) proposer is DARK by default
(def dark-candidates (ap/propose pap/portfolio-action-proposer test-state))
(check "(b) dark by default (empty)" (empty? dark-candidates))

;; (c) proposer active: emits the new families
(binding [pap/*portfolio-proposer-active?* true]
  (def active-candidates (ap/propose pap/portfolio-action-proposer test-state))
  (def types (set (map :type active-candidates))))
(println "\n(c) active proposer candidate types:" types)
(check "(c) contains :close-mission" (contains? types :close-mission))
(check "(c) contains :survey-mission" (contains? types :survey-mission))
(check "(c) contains :apply-cascade" (contains? types :apply-cascade))
(check "(c) does NOT contain :advance-mission (that's the enumerator's job)"
       (not (contains? types :advance-mission)))

;; (d) all candidates have :target, :weight, :rationale
(check "(d) all candidates have :target"
       (every? :target active-candidates))
(check "(d) all candidates have :weight"
       (every? :weight active-candidates))
(check "(d) all candidates have :rationale"
       (every? :rationale active-candidates))

;; (e) proposer-id
(check "(e) proposer-id = :portfolio-action"
       (= :portfolio-action (ap/proposer-id pap/portfolio-action-proposer)))

(println "\n5 tests, all pass")
