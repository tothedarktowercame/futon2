(ns futon2.aif.candidate-derivations-test
  "B4 slice 2b: the P₀ carrier's tests. Every test names the bug it would
  catch; the first two are written to fail on a lazy implementation that
  fills absent fields with nil or writes :status :admitted unconditionally."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.candidate-derivations :as cd]
            [futon2.aif.cascade-equivalence :as ce]
            [futon2.aif.cascade-model-manifest :as m]
            [futon2.aif.cascade-sources :as cascade-sources]))

(def target "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")

;; The exemplar record's candidate shape (tick-run-record-2026-09-23-1790131591.edn):
;; payload under :id, observation locators and BOTH receipt families on the
;; candidate map, construction receipt :hand-admitted by the declaring agent.
(def c2
  {:id {:kind :cascade-candidate :id :C2 :target target
        :precedence
        [{:id :aif/declare-the-conditioning :authority :documented-interpretation
          :produces #{[target :repair/split-declared-valid]}
          :guard {:status :interpreted :operator :and
                  :clauses [{:status :interpreted
                             :present #{[target :admission/task-stated]}
                             :absent #{[target :repair/split-declared-valid]}}]}
          :transition {:status :interpreted :operator :union
                       :produces #{[target :repair/split-declared-valid]}}
          :target target}
         {:id :aif/measurement-window-hygiene :authority :documented-interpretation
          :produces #{[target :repair/held-out-observations-collected]}
          :guard {:status :interpreted :operator :and
                  :clauses [{:status :interpreted
                             :present #{[target :repair/split-declared-valid]}
                             :absent #{[target :repair/held-out-observations-collected]}}]}
          :transition {:status :interpreted :operator :union
                       :produces #{[target :repair/held-out-observations-collected]}}
          :target target}]}
   :observation-locators
   {[target :restoration-accepted]
    {:class :C4 :repo "futon2" :sha "HEAD"
     :path "holes/tickets/T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade.md"
     :decl "**Status:** DONE"}}
   :construction-receipt
   {:kind :hand-admitted :by "zai-1" :date "2026-09-22" :moves []
    :stopped-is-not-success true
    :reading "C2, the restore route."}
   :interpretation-receipts
   {:aif/declare-the-conditioning
    {:kind :hand-admitted :by "zai-1" :date "2026-09-22"
     :source {:path "futon3/library/aif/declare-the-conditioning.flexiarg"}}}})

(def c1
  (-> c2
      (assoc-in [:id :id] :C1)
      (assoc-in [:id :precedence 0 :id] :apparatus/done-is-observed-running)
      (assoc-in [:id :precedence 0 :guard :clauses 0 :present] #{[target :admission/task-stated]})
      (assoc-in [:id :precedence 0 :guard :clauses 0 :absent] #{[target :restoration-accepted]})
      (assoc-in [:id :precedence 0 :produces] #{[target :restoration-accepted]})
      (assoc-in [:id :precedence 0 :transition :produces] #{[target :restoration-accepted]})))

(def s0 #{[target :admission/task-stated]})
(def stage {:continuation-belief {s0 1}})

(deftest exemplar-shaped-declared-candidates
  ;; Bug this catches: a lazy carrier that fills absent P₀ fields with nil
  ;; (the every-field-present + typed-absence assertions), or that launders
  ;; today's hand-admitted candidates as admitted (the :status assertion).
  (let [d (cd/derivations [c1 c2] (cd/s0-of stage) {:actions [(get c2 :id)]
                                                    :sources {:files [{:path "resources/wm/cascade-sources/t.edn"
                                                                       :sha256 "abc123" :target target}]}})]
    (is (= #{:C1 :C2} (set (keys d))))
    (doseq [[_id e] d]
      (is (every? #(contains? e %) cd/p0-fields) "every P₀ field present, no nils")
      (doseq [k [:source-revision :discovered-at :review-publication :acceptance :scope]]
        (is (and (map? (get e k)) (contains? (get e k) :status) (contains? (get e k) :reason))
            (str k " must be a typed absence, never nil or a placeholder")))
      (is (= :declared-file (:source-kind e)))
      (is (= :hand-admitted (:kind (:construction e))))
      (is (= :declared-file (:kind (:source (:construction e)))))
      (is (= "abc123" (:source-content-sha256 e)) "declared-file sha joins by target when sources are supplied")
      (is (= :declared (:kind (:interpretation e))))
      (is (= :inadmissible (:status e)))
      (is (= [:construction] (:offending-path e)) "offends at the top-level hand-admitted construction")
      (is (= :hand-admitted (:offending-kind e)))
      (is (= (:observation-locators (if (= :C1 (:source-id e)) c1 c2))
             (:locators e))))))

(deftest authored-entry-is-admitted
  ;; Bug this catches: a carrier that marks :status :admitted
  ;; unconditionally would pass the previous test's field checks while
  ;; lying about provenance; here a fully authored chain must be the ONLY
  ;; way to reach :admitted.
  (let [authored {:construction {:kind :derived-from-task
                                 :source {:kind :mission-registry :row-sha256 "deadbeef"}}
                  :interpretation {:kind :registry-pattern :interpreted-at "2026-09-24T00:00:00Z"}
                  :review-publication {:kind :cascade-sources/check-file! :path "p.edn"}
                  :admission {:kind :reviewed-publication}}
        d (cd/derivations [c1] s0 {:authored {:C1 authored}})]
    (is (= :admitted (:status (:C1 d))))
    (is (= :derived-from-task (:kind (:construction (:C1 d)))))))

(deftest id-mismatch-refuses
  ;; Bug this catches: a partial map on a bijectivity break would leave P₀
  ;; joining silently truncated (condition 5). An argmax action naming an id
  ;; that is not a candidate must refuse the WHOLE carrier.
  (let [d (cd/derivations [c1 c2] s0 {:actions [(assoc-in (get c2 :id) [:id] :C99)]})]
    (is (and (map? d) (= :refused (:status d))))
    (is (= :candidate-id-mismatch (:kind d)))
    (is (= [:C99] (:unmatched d))))
  (let [d (cd/derivations [{:id {:kind :cascade-candidate}}] s0 nil)]
    (is (= :refused (:status d)))))

(deftest hashes-and-rows-agree
  ;; Bug this catches: the carrier computing its own sha or kernel rows
  ;; instead of citing ce/normalize and the Lean-bound manifest kernel —
  ;; W₀ items 3 and 5 would then compare against a different relation than
  ;; the one the proof uses.
  (let [d (cd/derivations [c2] s0)]
    (is (= (:normalized-cascade-sha256 (ce/normalize c2))
           (:normalized-cascade-sha256 (:C2 d))))
    (is (= (m/cascade-kernel (get-in c2 [:id :precedence]) s0)
           (:transition-rows (:C2 d))))))

(deftest s0-extraction
  ;; Bug this catches: s0-of silently accepting a mixture as "the" initial
  ;; state, giving one kernel row for a belief over many states.
  (is (= s0 (cd/s0-of stage)))
  (is (nil? (cd/s0-of {:continuation-belief {s0 1/2 #{} 1/2}})))
  (is (= {:status :missing :reason :initial-state-not-a-point-mass}
         (:transition-rows (:C2 (cd/derivations [c2] (cd/s0-of {:continuation-belief {s0 1/2 #{} 1/2}})))))))

(deftest sources-populate-sha-and-acceptance
  ;; B4 slice 2c. Bug this catches: a carrier that FABRICATES a sha or an
  ;; acceptance from the candidate map alone (the no-sources assertions), or
  ;; that joins a declared file to a candidate by anything other than its
  ;; own target (the cross-target assertion).
  (let [want-token [target :restoration-accepted]
        locator {:class :C4 :repo "futon2" :sha "HEAD" :path "t.md" :decl "**Status:** DONE"}
        sources {:files [{:path "resources/wm/cascade-sources/t.edn"
                          :sha256 "feedface" :target target}]
                 :wants {target [want-token]}
                 :locators {target {want-token locator}}}
        with-sources (cd/derivations [c2] s0 {:sources sources})
        without-sources (cd/derivations [c2] s0 nil)
        other-target (cd/derivations [c2] s0
                       {:sources (update-in sources [:files 0] assoc :target "T-someone-else")})]
    (is (= "feedface" (:source-content-sha256 (:C2 with-sources))))
    (is (= (:sha256 (:source (:construction (:C2 with-sources)))) "feedface"))
    (is (= (cascade-sources/acceptance-of target {:sources sources})
           (:acceptance (:C2 with-sources))) "acceptance equals acceptance-of's value")
    (is (= {:status :missing :reason :declared-source-sha-not-retained
            :target target}
           (:source-content-sha256 (:C2 without-sources)))
        "no sources supplied: typed absence, never a fabricated sha")
    (is (= {:status :missing :reason :declared-acceptance-not-in-decision-scope}
           (:acceptance (:C2 without-sources)))
        "no sources supplied: no acceptance invented from the candidate map")
    (is (= {:status :missing :reason :declared-source-sha-not-retained
            :target target}
           (:source-content-sha256 (:C2 other-target)))
        "a declared file for ANOTHER target never joins this candidate")
    (is (= {:status :missing :reason :acceptance-source-file-not-found-for-target}
           (:acceptance (:C2 other-target)))
        "acceptance-of keys wants by target even when :files mismatches; the carrier must not accept a declaration whose declaring file does not resolve")))
