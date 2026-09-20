(ns authority-probe
  "Read-only authority compatibility probe. No fixture grants production authority."
  (:require [clojure.java.io :as io]
            [futon2.aif.machine-budget-authority :as authority]
            [futon2.aif.machine-portfolio-restriction :as portfolio]
            [futon2.aif.machine-enactment-correspondence :as correspondence]
            [futon2.aif.close-retention :as retention]
            [futon2.aif.interpretation-evidence :as evidence]))

(def e1-root "holes/labs/wm-contract/runs/row-22-e1-authority-resolution-2026-09-13/fixtures")
(def e2b-root "holes/labs/wm-contract/runs/row-22-e2b-correspondence-2026-09-13/fixtures")
(defn pin [root filename]
  {:relative-path filename
   :sha256 (evidence/sha256 (java.nio.file.Files/readAllBytes (.toPath (io/file root filename))))})
(def e1-config
  {:resolver/version authority/resolver-version :mode :isolated-test :root e1-root
   :sources (into {} (map (fn [k] [k (pin e1-root (str (name k) ".edn"))]))
                  [:ranked-support :field-membership :costs :utilities :budgets])})
(def e2b-config
  {:mode :isolated-test :e2a-resolver e1-config :witness-root e2b-root
   :witnesses (into {} (map (fn [k] [k (pin e2b-root (str (name k) ".edn"))]))
                    [:context :selection :enactment])})
(defn outcome [f]
  (try {:status :value :value (f)}
       (catch clojure.lang.ExceptionInfo e
         {:status :refused :kind (:refusal (ex-data e))})))
(def isolated (correspondence/verify-correspondence e2b-config))
(def occurrence
  (retention/mint-occurrence
   {:run-id "authority-probe" :cohort-id "isolated-probe" :attempt-id "probe-1"
    :selected-action (get-in isolated [:selected :action])
    :now #(java.time.Instant/parse "2026-09-20T00:00:00Z")
    :uuid-fn #(java.util.UUID/randomUUID)}))
(def observations
  {:isolated-control (:correspondence isolated)
   :existing-candidate-id (get-in isolated [:selected :candidate/id])
   :minted-action-id (:action/id occurrence)
   :same-action-value (= (:action/value occurrence) (get-in isolated [:selected :action]))
   :e2b-production (outcome #(correspondence/verify-correspondence
                             (assoc e2b-config :mode :production)))
   :e2a-with-production-e1 (outcome #(portfolio/restrict-portfolio
                                    (assoc e1-config :mode :production)))})
(assert (= :exact-occurrence-and-action (:isolated-control observations)))
(assert (:same-action-value observations))
(assert (= {:status :refused :kind :e2b/production-authority-unavailable}
           (:e2b-production observations)))
(assert (= {:status :refused :kind :r6-r11/production-authority-unavailable}
           (:e2a-with-production-e1 observations)))
(prn observations)
