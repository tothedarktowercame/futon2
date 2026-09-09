(ns checks.fold-c-axes
  "Two typed declaration axes; source inspection is backed by Lean elaboration
   and the independently checked finite runtime certificate in the wrapper."
  (:require [clojure.edn :as edn]
            [checks.positive-proof-receipt :as receipt]
            [futon2.aif.ruled-outcome-c :as ruled]))

(def boundary "../mathlib4/DarkTower/WarMachine/PreferenceRiskBoundary.lean")
(def fixture-path "holes/labs/wm-contract/fold-c-axes.edn")
(def era-report "runs/FOLDC-era-comparison-2026-09-09.md")

(defn runtime-axes []
  (reduce (fn [axes row]
            (if (and (:folded? row) (= :yes (:in-ruled-sum row)))
              (let [axis (:composition-axis row)]
                (when-not (contains? axes axis)
                  (throw (ex-info "active contribution has no known axis" {:row row})))
                (update axes axis conj (:layer/id row)))
              axes))
          {:preference-layer #{} :risk-contribution #{}} ruled/fold-declaration))

(defn lean-risk-ids []
  (let [source (slurp boundary)
        literal (second (re-find #"(?m)^def runtimeRiskContributionIds : List String := (\[[^\n]*\])$" source))]
    (when-not literal (throw (ex-info "risk declaration not a literal list" {:file boundary})))
    (let [ids (edn/read-string literal)]
      (when-not (and (vector? ids) (every? string? ids) (= (count ids) (count (set ids))))
        (throw (ex-info "invalid risk declaration" {:ids ids})))
      (set (map keyword ids)))))

(defn derived-fixture []
  {:schema :fold-c-axes/v1
   :era-report era-report
   :preference-layer-ids []
   :risk-contribution-ids (vec (sort (lean-risk-ids)))
   :risk-declaration-sha256
   (receipt/sha256-text (receipt/declaration-text (slurp boundary) "runtimeRiskContributionIds"))})

(defn risk-matches? [runtime lean fixture]
  (= (:risk-contribution runtime) lean (set (:risk-contribution-ids fixture))))
