;; Standalone OFFLINE replay — zai-8, 2026-10-22, effect-prediction-review.
;; Runs against the WORKTREE /home/joe/code/futon2-effect-prediction-review
;; classpath (widened freeze-prediction accepting the {:co-apply …} step),
;; with git reads only from the canonical futon2 repo at PINNED revisions.
;; LIMIT (stated per dispatch): the decision input is MANUALLY ASSEMBLED from
;; the historical source declaration — no live runner route produced this
;; precedence/certificate; that bridge (construct-selected-action enrichment,
;; compiled-route assembly) is deliberately NOT implemented here.
(require '[futon2.aif.cascade-model-manifest :as manifest]
         '[futon2.aif.token-outcome :as token-outcome]
         '[futon2.aif.selected-want-outcome :as swo]
         '[futon2.aif.reviewer-falsifier :as rf]
         '[futon2.aif.d-predecessor-task-authority :as d]
         '[futon2.aif.observation-checks :as oc]
         '[clojure.edn :as edn]
         '[clojure.java.io :as io])

(def decl-path "resources/wm/cascade-sources/T-repair-occ-444fb018.edn")
(def decl (edn/read-string (slurp decl-path)))
(def target (:target decl))
(def q (fn [t] [target t]))
(def repo (io/file "/home/joe/code/futon2"))
(def before-sha "0798f96ad2082a8f80c8a850c8e1138c036213f9")  ; ticket OPEN
(def after-sha  "97e17e10f2695c481c55ddaebe9026f2d245663f")  ; ticket DONE
(def locator-for (fn [token] (get-in decl [:locators token])))

;; 1. Observed FACTS at the before revision through the real C4 reader —
;;    observed q0 ONLY, no synthesized prior.
(def fact-observations
  (into {} (for [fact (:facts decl)]
             [fact (boolean (:observed (oc/check-decl-in-file
                                        (assoc (locator-for fact) :sha before-sha))))])))
(def universe (into {} (for [[f v] fact-observations] [(q f) v])))
(def have (set (for [[f v] fact-observations :when v] (q f))))
(def q0 (manifest/observed-belief have))

;; 2. The declared patterns as a CO-APPLY step: every pattern is its own unit,
;;    no declared descent, so every unit whose guard holds at the state is on
;;    the enabled frontier simultaneously (>= 2 at q0 — true co-application,
;;    not a chain). Same qualification as the domain universe.
(defn qualify-pattern [id]
  (let [p (get-in decl [:patterns id])]
    {:id id
     :guard {:status :interpreted
             :needs (set (map q (:needs (:guard p))))
             :forbids (set (map q (:forbids (:guard p))))
             :clauses [{:present (set (map q (:needs (:guard p))))
                        :absent (set (map q (:forbids (:guard p))))}]}
     :produces (set (map q (:produces p)))}))
(def pattern-ids [:aif/declare-the-conditioning
                  :aif/measurement-window-hygiene
                  :aif/two-layer-calibration
                  :contracts/holder-states-the-claim])
(def coapply-step
  {:co-apply {:units (vec pattern-ids)
              :descent []
              :patterns (into {} (map (fn [id] [id (qualify-pattern id)]) pattern-ids))}})

(defn certificate [precedence]
  {:precision-family {:model {:q0 q0 :horizon 4}}
   :token-belief-stage
   {:domain-inputs [{:target target
                     :declaration {:facts universe :want (:want decl)}}]}})
(def decision {:action {:kind :cascade-candidate :id "T-repair/coapply" :target target
                        :precedence coapply-step}
               :selection-certificate (certificate coapply-step)})

;; 3. POSITIVE through the WIDENED freeze: the co-apply step rolls out under
;;    the same model the vector path uses.
(def prediction (token-outcome/freeze-prediction decision))
(def want-token (q :restoration-accepted))
(def want-predicted
  (some->> (:wanted prediction)
           (filter #(= want-token (:token %))) first :predicted))
(def frontier-kinds
  (some->> (get-in prediction [:rollout :evaluations])
           (mapcat :states) (map :kernel-kind) distinct vec))

;; 4. Measurements at the after revision through the real d-task producer,
;;    then compare / accounting / falsifier — the real chain.
(def dispatch {:universe (conj (set (keys universe)) want-token)
               :declarations [{:snapshot decl
                               :sha256 "1b9d390201fb96d3145222c070f1adc76a8dd27b0e42edc3528a72452b80027b"}]})
(def measurements (d/artifact-tokens dispatch repo after-sha))
(def comparison (token-outcome/compare-outcomes prediction measurements after-sha))
(def accounting (swo/receipt {:selected-action
                              {:target target :want (:want decl)
                               :observation-locators {want-token (locator-for :restoration-accepted)}}
                              :token-comparison comparison}))
(def falsifier-input
  {:target target :commit after-sha
   :work-evidence {:kind :ordinary-cascade}
   :mission-standing {:status :not-applicable}
   :selected-want-outcomes accounting
   :disposition nil
   :artifact-binding {:repo "futon2" :commit after-sha
                      :pre-dispatch-head before-sha :observed-head after-sha
                      :corroborates? true}
   :review-gate {:passed? true :required? true :executed? true :tool-events 1
                 :code-files [] :execution-source :offline-replay}})
(def falsifier (rf/receipt falsifier-input))
(def falsifier-verify (rf/verify falsifier falsifier-input))

;; 5. NEGATIVE a — malformed co-apply step structurally rejected by the new
;;    guard (patterns missing a unit), with the typed finding surfaced.
(def malformed-step (update coapply-step :co-apply
                            #(assoc % :units (conj (:units %) :no-such-unit))))
(def negative-malformed
  (token-outcome/freeze-prediction
    (assoc-in decision [:action :precedence] malformed-step)))

;; 6. NEGATIVE b — measure at the BEFORE revision (ticket OPEN): the want is
;;    predicted (frontier fires under theta defaults) but not observed.
(def measurements-before (d/artifact-tokens dispatch repo before-sha))
(def comparison-before
  (token-outcome/compare-outcomes prediction measurements-before before-sha))

;; 7. REGRESSION — the legacy VECTOR precedence over the same patterns still
;;    freezes with identical semantics (:precedence-vector).
(def legacy-vector (mapv qualify-pattern pattern-ids))
(def legacy-decision
  {:action {:kind :cascade-candidate :id "T-repair/C2" :target target
            :precedence legacy-vector}
   :selection-certificate (certificate legacy-vector)})
(def legacy-prediction (token-outcome/freeze-prediction legacy-decision))

(def result
  {:schema :wm/coapply-declared-effect-offline-replay-v1
   :label "offline replay through widened freeze-prediction; manually assembled input, not production evidence"
   :worktree "effect-prediction-review"
   :target target
   :source-declaration {:path decl-path
                        :sha256 (manifest/sha256 (slurp decl-path))}
   :revision-pair {:before before-sha :after after-sha}
   :facts-at-before fact-observations
   :coapply {:units (get-in coapply-step [:co-apply :units])
             :descent (get-in coapply-step [:co-apply :descent])
             :frontier-kinds frontier-kinds}
   :positive {:prediction-status (:status prediction)
              :prediction-kind (:precedence-kind prediction)
              :prediction-reason (:reason prediction)
              :want-token want-token
              :want-predicted want-predicted
              :intended-outputs (vec (:intended-outputs prediction))
              :measurement-rows (count measurements)
              :comparison-status (:status comparison)
              :want-verdict (some->> (:tokens comparison)
                                     (filter #(= want-token (:token %))) first :verdict)
              :accounting-status (:status accounting)
              :accounting-by-class (:by-class accounting)
              :falsifier-status (:status falsifier)
              :falsifier-failed (:failed-checks falsifier)
              :falsifier-verify-status (:status falsifier-verify)}
   :negative-malformed {:status (:status negative-malformed)
                        :reason (:reason negative-malformed)
                        :finding (:precedence-finding negative-malformed)}
   :negative-before-measure {:comparison-status (:status comparison-before)
                             :want-verdict (some->> (:tokens comparison-before)
                                                    (filter #(= want-token (:token %))) first :verdict)}
   :legacy-vector-regression {:status (:status legacy-prediction)
                              :kind (:precedence-kind legacy-prediction)
                              :want-predicted
                              (some->> (:wanted legacy-prediction)
                                       (filter #(= want-token (:token %))) first :predicted)}})

(println (pr-str result))
