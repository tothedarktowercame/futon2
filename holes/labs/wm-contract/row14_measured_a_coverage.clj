(ns row14-measured-a-coverage
  "Report factual measured-A coverage from the close corpus and a pinned census.

  This schedules no attempts and makes no probability claim. It is intentionally
  rerunnable as retention accrues, but every invocation must name a new dated
  output artifact: the reporter refuses to overwrite an existing report."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [clojure.set :as set]
            [futon2.aif.belief :as belief]
            [futon2.aif.full-loop-cohort :as cohort]))

(def default-census
  "holes/labs/wm-contract/runs/row-14-measured-a-sources-2026-09-12/counts.edn")
(def default-close-root "data/wm-full-loop")

(def statuses (vec (sort belief/status-set)))
(def dispositions
  (vec (sort (disj cohort/outcome-kinds
                   :historical-verification-awaiting-validation
                   :historical-verification-refused))))

(defn- read-edn [file]
  (try (edn/read-string (slurp file)) (catch Exception _ nil)))

(defn- close-files [root]
  (->> (file-seq (io/file root))
       (filter #(.isFile %))
       (filter #(re-matches #"\d+-closed\.edn" (.getName %)))
       (sort-by str)))

(defn- close-outcome [file]
  (get-in (read-edn file) [:payload :judgment :outcome]))

(defn- zero-cells [cells]
  (vec (for [status statuses
             disposition dispositions
             :when (zero? (get-in cells [status disposition] 0))]
         {:status status :disposition disposition})))

(defn- disposition-opportunity [disposition]
  {:dimension :disposition
   :value disposition
   :observation
   (case disposition
     :abstained "fills only when policy selection naturally abstains"
     :artifact-only "fills only when a real attempt closes with an authored artifact but no grounding"
     :cancelled "fills only when a real attempt is cancelled"
     :dispatch-failed "fills only when an actual dispatch fails"
     :grounded-no-change "fills only when grounding honestly observes no state change"
     :guardrail-refusal "fills only when a guardrail actually refuses"
     "fills only when that disposition naturally closes")})

(defn report [close-root census-path as-of]
  (let [census (read-edn census-path)
        cells (get-in census [:counts :matrix])
        _ (when-not (= [statuses dispositions]
                       [(get-in census [:support :states])
                        (get-in census [:support :outcomes])])
            (throw (ex-info "Pinned census support differs from declared 7x12 support"
                            {:refusal :census-support-mismatch})))
        corpus-observed (->> (close-files close-root)
                             (keep close-outcome)
                             (filter (set dispositions)) set)
        measured-statuses (->> cells
                               (keep (fn [[status row]]
                                       (when (some pos? (vals row)) status)))
                               set)
        observed-dispositions (vec (sort corpus-observed))
        never-dispositions (vec (sort (set/difference (set dispositions)
                                                       corpus-observed)))
        observed-statuses (vec (sort measured-statuses))
        never-statuses (vec (sort (set/difference (set statuses)
                                                   measured-statuses)))]
    {:schema :wm/measured-a-coverage-v1
     :as-of as-of
     :claim :coverage-observation-not-probability
     :source {:close-root close-root
              :pinned-census census-path
              :pinned-census-schema (:schema census)}
     :cells cells
     :zero-cells (zero-cells cells)
     :dispositions {:observed observed-dispositions
                    :never-observed never-dispositions}
     :statuses {:observed observed-statuses
                :never-observed never-statuses}
     :capture-capabilities
     {:status-at-close {:since "d30ed68f" :deployed? false}
      :outcome-entity {:since "6deb8019" :deployed? false}
      :cross-ledger {:since "4c776dc2" :deployed? false}
      :reload-status :pending}
     :retention-opportunities
     (vec (concat (map disposition-opportunity never-dispositions)
                  (for [status never-statuses]
                    {:dimension :status :value status
                     :observation
                     "fills only when a naturally closing single entity carries this retained status"})))
     :constraints {:induces-failures? false
                   :schedules-attempts? false
                   :probability-claim? false}}))

(defn -main [& [output as-of close-root census-path]]
  (when-not (and output as-of)
    (throw (ex-info "Usage: reporter OUTPUT AS-OF [CLOSE-ROOT] [CENSUS]"
                    {:refusal :dated-output-required})))
  (when (.exists (io/file output))
    (throw (ex-info "Coverage artifacts are append-only; output already exists"
                    {:refusal :coverage-report-overwrite :path output})))
  (io/make-parents output)
  (with-open [writer (io/writer output)]
    (pp/pprint (report (or close-root default-close-root)
                       (or census-path default-census) as-of)
               writer)))

(apply -main *command-line-args*)
