(ns futon2.aif.wm.run-output
  "Durable, Field-Desk-ready output projection for one War Machine run.

  This does not ask an author for a second story. It projects the validated,
  independently reviewed feature card already admitted by the runner and binds
  its replay instructions to the grounded artifact."
  (:require [clojure.string :as str]))

(def schema :wm/run-output-v1)

(defn- replay-step? [x]
  (and (string? x)
       (let [i (str/index-of x "->")]
         (and i (not (str/blank? (subs x 0 i)))
              (not (str/blank? (subs x (+ i 2))))))))

(defn receipt
  "Project a run output from RESULT and GROUNDED-COMMIT. Successful grounded
  work has a present output only when the retained feature card supplies a
  nonblank summary and replayable demo steps. Other outcomes retain a typed
  absence. No prose or demo location is invented."
  [result grounded-commit]
  (let [outcome (:outcome result)
        card (get-in result [:data :feature-card])
        steps (:things-to-try card)
        grounded? (contains? #{:grounded-change :grounded-progress} outcome)]
    (cond
      (not grounded?)
      {:schema schema :status :absent :reason :run-not-grounded
       :outcome outcome}

      (or (not (map? card))
          (str/blank? (str (:built card)))
          (str/blank? (str (:want-coverage card)))
          (not (and (sequential? steps) (seq steps) (every? replay-step? steps))))
      {:schema schema :status :missing
       :reason :validated-output-evidence-not-retained
       :outcome outcome}

      :else
      {:schema schema
       :status :present
       :summary {:built (:built card)
                 :want-coverage (:want-coverage card)
                 :matches-intent? (:matches-intent? card)
                 :outcome outcome}
       :demo {:pointer (or grounded-commit
                           {:absent :no-grounded-artifact-pointer})
              :replay (vec steps)}
       :review {:reviewer-note (or (:reviewer-note card)
                                   {:absent :no-reviewer-note})}
       :supporting-artifacts
       (cond-> []
         (:fold-ref card) (conj {:kind :fold :path (:fold-ref card)})
         (:proof-ref card) (conj {:kind :proof :path (:proof-ref card)})
         (get-in result [:morning-brief-ref])
         (conj {:kind :morning-brief :ref (:morning-brief-ref result)}))})))
