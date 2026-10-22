;; Standalone OFFLINE replay of already-supported functions — zai-8, 2026-10-22.
;; Proves the declared-effect positive path end-to-end for a real target with a
;; real revision pair, plus the two frozen negatives. Read-only: performs git
;; reads only; writes nothing except its printed result (redirected by caller).
;; No production state, ledger, or shared JVM is touched.
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

;; 1. Observe the declared FACTS at the before revision through the real C4
;;    reader (same function artifact-tokens uses). Only admission and the
;;    split hold there; the want is false.
(def fact-observations
  (into {} (for [fact (:facts decl)]
             [fact (boolean (:observed (oc/check-decl-in-file
                                        (assoc (locator-for fact) :sha before-sha))))])))
(def universe (into {} (for [[f v] fact-observations] [(q f) v])))
(def have (set (for [[f v] fact-observations :when v] (q f))))

;; 2. Point-mass initial belief at the observed state — the real producer
;;    (manifest/observed-belief), fact-initialized, not synthetic.
(def q0 (manifest/observed-belief have))

;; 3. The declared candidate's precedence, interpreted pattern maps with
;;    tokens qualified the way the domain universe is (token-belief-carry
;;    token-universe qualifies [target token]).
(defn qualify-pattern [id]
  (let [p (get-in decl [:patterns id])]
    (assoc (select-keys p [:produces])
         :id id
         :guard (-> {:status :interpreted
                     :needs (set (map q (:needs (:guard p))))
                     :forbids (set (map q (:forbids (:guard p))))}
                    (assoc :clauses [{:present (set (map q (:needs (:guard p))))
                                      :absent (set (map q (:forbids (:guard p))))}]))
         :produces (set (map q (:produces p))))))
(def c2-precedence (mapv qualify-pattern
                         [:aif/declare-the-conditioning
                          :aif/measurement-window-hygiene
                          :aif/two-layer-calibration
                          :contracts/holder-states-the-claim]))

(def action {:kind :cascade-candidate :id "T-repair/C2" :target target
             :precedence c2-precedence})
(def decision {:action action
               :selection-certificate
               {:precision-family {:model {:q0 q0 :horizon 4}}
                :token-belief-stage
                {:domain-inputs [{:target target
                                  :declaration {:facts universe
                                                :want (:want decl)}}]}}})

;; 4. POSITIVE prediction: the real freeze-prediction over the real rollout.
(def prediction (token-outcome/freeze-prediction decision))

;; 5. Measurements at the after revision through the real d-task producer.
(def dispatch {:universe (conj (set (keys universe)) (q :restoration-accepted))
               :declarations [{:snapshot decl
                               :sha256 "1b9d390201fb96d3145222c070f1adc76a8dd27b0e42edc3528a72452b80027b"}]})
(def measurements (d/artifact-tokens dispatch repo after-sha))

;; 6. Comparison, accounting, falsifier — the real chain.
(def comparison (token-outcome/compare-outcomes prediction measurements after-sha))
(def accounting (swo/receipt {:selected-action
                              {:target target :want (:want decl)
                               :observation-locators {(q :restoration-accepted)
                                                      (locator-for :restoration-accepted)}}
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

;; 7. NEGATIVE a — Trial5's exact shape: a want-less selected action.
(def negative-wantless (swo/receipt {:selected-action {:kind :cascade-candidate
                                                       :id "compiled" :target target}
                                     :token-comparison comparison}))

;; 8. NEGATIVE b — the compiled route's co-apply precedence map: freeze-prediction
;;    refuses on its (vector? ...) guard, while the rollout model itself
;;    evaluates the same map shape (cascade_model_manifest push-forward /
;;    evaluate-co-apply). Pattern map for the want-producing unit:
(def co-apply-precedence
  {:co-apply {:units (mapv :id c2-precedence)
              :descent []
              :patterns (into {} (map (fn [p] [(:id p) p]) c2-precedence))}})
(def negative-coapply-freeze
  (token-outcome/freeze-prediction
   (assoc-in decision [:action :precedence] co-apply-precedence)))
(def coapply-rollout
  (manifest/rollout (constantly co-apply-precedence) q0 4))
(def coapply-want-mass
  (if (contains? coapply-rollout :status)
    ::refused
    (reduce (fn [acc [s mass]] (+ acc (if (contains? s (q :restoration-accepted)) mass 0)))
            0 coapply-rollout)))

;; 9. NEGATIVE c — before-false guard: measure the want at the BEFORE revision
;;    (ticket OPEN): observed false at after-locator = before-sha.
(def measurements-before
  (d/artifact-tokens dispatch repo before-sha))
(def comparison-before
  (token-outcome/compare-outcomes prediction measurements-before before-sha))
(def accounting-before
  (swo/receipt {:selected-action
                {:target target :want (:want decl)
                 :observation-locators {(q :restoration-accepted)
                                        (locator-for :restoration-accepted)}}
                :token-comparison comparison-before}))

(def result
  {:schema :wm/declared-effect-offline-replay-v1
   :label "offline replay of already-supported functions; not production evidence"
   :target target
   :revision-pair {:before before-sha :after after-sha}
   :facts-at-before fact-observations
   :q0-keys (count q0)
   :prediction-status (:status prediction)
   :prediction-reason (:reason prediction)
   :wanted (get prediction :wanted)
   :measurement-rows (count measurements)
   :comparison-status (:status comparison)
   :comparison-tokens (get comparison :tokens)
   :accounting-status (:status accounting)
   :accounting-reason (:reason accounting)
   :accounting-by-class (:by-class accounting)
   :falsifier-status (:status falsifier)
   :falsifier-failed (:failed-checks falsifier)
   :falsifier-verify-status (:status falsifier-verify)
   :falsifier-verify-reason (:reason falsifier-verify)
   :negative-wantless-status (:status negative-wantless)
   :negative-wantless-reason (:reason negative-wantless)
   :negative-coapply-freeze-status (:status negative-coapply-freeze)
   :negative-coapply-freeze-reason (:reason negative-coapply-freeze)
   :coapply-rollout-want-mass coapply-want-mass
   :coapply-rollout (if (contains? coapply-rollout :status) coapply-rollout ::ok)
   :negative-before-measure-accounting-status (:status accounting-before)
   :negative-before-measure-verdicts (mapv :verdict (get comparison-before :tokens))})

(println (pr-str result))
