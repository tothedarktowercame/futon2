(ns futon2.aif.cascade-equivalence-test
  "X₀ bad cases and the Step 0 CHECK's positive checks for the predeclared
  cascade-equivalence relation. Every test names, in its leading comment,
  the concrete bug it would catch — none of these is vacuous."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-equivalence :as ce]
            [futon2.aif.cascade-model-manifest :as m]))

(def target "T-repair-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")

(defn pat
  "A pattern in the run-record candidate shape: :guard with :present/:absent
  clauses (consumes/forbids), :transition/:produces, :authority, optional
  :theta and any junk fields the normalizer must discard."
  [id consumes forbids produces & {:keys [theta authority]}]
  (cond-> {:id id
           :authority (or authority :documented-interpretation)
           :produces produces
           :guard {:status :interpreted :operator :and
                   :clauses [{:status :interpreted :present consumes :absent forbids}]}
           :transition {:status :interpreted :operator :union :produces produces}
           :target target}
    theta (assoc :theta theta)))

(defn cand
  "A run-record-shaped candidate: payload under :id, with derivation,
  locator, acceptance and scope fields alongside."
  [id precedence & {:keys [construction interpretation]}]
  {:id {:kind :cascade-candidate :id id :target target :precedence precedence}
   :locators [{:path "storage/wm/candidates/old-position.edn"}
              {:path "storage/wm/candidates/another.edn"}]
   :acceptance {:predicate "produces appear in the reviewed revision"
                :kind :observable-tokens}
   :scope {:feasible :unrestricted}
   :interpretation (or interpretation {:kind :registry-pattern
                                       :interpreted-at "2026-09-24T10:00:00Z"})
   :construction (or construction {:kind :derived-from-task
                                   :author "claude-8"
                                   :constructed-at "2026-09-24T10:01:00Z"
                                   :source {:kind :mission-registry
                                            :row-sha256 "abc"}})})

(def p-alpha (pat :aif/declare-the-conditioning
                  #{[target :admission/task-stated]} #{[target :repair/split-declared-invalid]}
                  #{[target :repair/split-declared-valid]}))
(def p-beta (pat :aif/measurement-window-hygiene
                 #{[target :repair/split-declared-valid]} #{}
                 #{[target :repair/held-out-observations-collected]}))
(def p-gamma (pat :aif/two-layer-calibration
                  #{[target :repair/held-out-observations-collected]} #{}
                  #{[target :repair/calibration-evidence-present]}))

(def s0 #{[target :admission/task-stated]})

(deftest clone-case-x0a
  ;; X₀ (a), mandatory: clone under fresh ids, prose, timestamps and locator
  ;; order. Bug this catches: a normalizer that hashes ids, author names,
  ;; timestamps, locator presentation order or prose, so a re-registered
  ;; clone counts as a NEW candidate and B4 pads the candidate field with
  ;; duplicates (exactly the proliferation P₀'s distinctness check exists to
  ;; refuse).
  (let [original (cand :C1 [p-alpha p-beta])
        clone (-> (cand :C9 [(assoc p-alpha :id :aif/renamed-1)
                             (assoc p-beta :id :aif/renamed-2)])
                  (assoc-in [:acceptance :notes] "THE SAME acceptance, reworded prose")
                  (assoc-in [:construction :constructed-at] "2026-12-25T00:00:00Z")
                  (assoc-in [:construction :author] "someone-else")
                  (update :locators reverse))]
    (is (true? (ce/equivalent? original clone)))
    (is (= :equivalent (:verdict (ce/distinct-with-differing-effects? original clone s0))))
    (is (= (:normalized-cascade-sha256 (ce/normalize original))
           (:normalized-cascade-sha256 (ce/normalize clone))))))

(deftest hand-admitted-case-x0b
  ;; X₀ (b), mandatory: genuinely different kernels, one with :hand-admitted
  ;; nested two levels down in the construction chain. Bug this catches: a
  ;; provenance check that only looks at the TOP-LEVEL :kind would pass this
  ;; candidate, letting a hand-built cascade into the field P₀ must keep
  ;; hand-free; and a check that let a kernel difference rescue inadmissible
  ;; provenance would admit it for being 'interesting'.
  (let [clean (cand :C1 [p-alpha p-beta])
        hand-built (cand :C2 [p-gamma p-beta]
                         :construction {:kind :derived-from-task
                                        :source {:kind :review-receipt
                                                 :source {:kind :hand-admitted}}})]
    (is (= :distinct (:verdict (ce/distinct-with-differing-effects? clean hand-built s0))))
    (let [v (ce/admissible-provenance? hand-built)]
      (is (false? (:admissible v)))
      (is (= :hand-admitted (:kind v)))
      (is (= [:construction :source :source] (:path v))))
    (is (true? (:admissible (ce/admissible-provenance? clean))))))

(deftest positive-pair-is-distinct
  ;; Step 0 CHECK positive requirement: two candidates whose first enabled
  ;; pattern differs at s₀, with differing produces and kernel rows. Bug
  ;; this catches: an over-coarse normalization (e.g. hashing only the
  ;; target, or only pattern COUNTS) would fold genuinely different cascades
  ;; together and the field could never satisfy W₀'s distinctness clause.
  (let [c1 (cand :C1 [(pat :aif/x #{} #{} #{[target :a]})])
        c2 (cand :C2 [(pat :aif/y #{} #{} #{[target :b]})])]
    (is (= :distinct (:verdict (ce/distinct-with-differing-effects? c1 c2 s0))))
    (is (not= (m/cascade-kernel (:precedence (:id c1)) s0)
              (m/cascade-kernel (:precedence (:id c2)) s0)))))

(deftest theta-only-pair
  ;; The theta DECISION: same slots, different theta. Bug this catches: a
  ;; normalizer that includes theta in N(π) (as the PROOF-2 draft literally
  ;; wrote it) would split one cascade into two on a learned parameter,
  ;; violating the B4 amendment's intent; and an effect-equivalence that
  ;; ignores theta entirely would miss that it changes the kernel row.
  (let [c1 (cand :C1 [(pat :aif/x #{[target :admission/task-stated]} #{} #{[target :b]}
                          :theta 1/4)])
        c2 (cand :C2 [(pat :aif/y #{[target :admission/task-stated]} #{} #{[target :b]}
                          :theta 3/4)])]
    (is (true? (ce/equivalent? c1 c2)))
    (is (false? (ce/effect-equivalent-at? c1 c2 s0)))
    ;; achieved at s₀: produces ⊆ s₀ makes patternKernel the point mass for
    ;; ANY theta (patternKernel_of_achieved), so effect-equivalence holds.
    (let [s-done #{[target :admission/task-stated] [target :b]}
          c3 (cand :C3 [(pat :aif/x #{} #{} #{[target :b]} :theta 1/4)])
          c4 (cand :C4 [(pat :aif/y #{} #{} #{[target :b]} :theta 3/4)])]
      (is (true? (ce/effect-equivalent-at? c3 c4 s-done))))))

(deftest perturbed-kernel-row-x0-third-check
  ;; X₀'s third check / W₀ item 4: a pair with unequal first actions and
  ;; unequal cascadeKernel rows at s₀. Bug this catches: sparse-kernel
  ;; equality done on KEYS alone (or via pr-str of a hash-set-containing
  ;; map, whose order is unstable) would report equality for rows whose
  ;; masses differ, letting a numerically perturbed clone pass as the
  ;; original.
  (let [c1 (cand :C1 [(pat :aif/x #{} #{} #{[target :b]})])
        perturbed (cand :C2 [(assoc (pat :aif/y #{} #{} #{[target :b]})
                                    :produces #{[target :b] [target :c]}
                                    :transition {:status :interpreted :operator :union
                                                 :produces #{[target :b] [target :c]}})])]
    (is (= :distinct (:verdict (ce/distinct-with-differing-effects? c1 perturbed s0))))
    (is (not= (m/cascade-kernel (:precedence (:id c1)) s0)
              (m/cascade-kernel (:precedence (:id perturbed)) s0)))))

(deftest uninterpreted-pattern-refuses
  ;; A firing pattern without an interpretation is a typed hole, not a
  ;; silent boolean. Bug this catches: normalize/equivalent? coercing a
  ;; missing interpretation to false or true instead of propagating the
  ;; refusal (interpret_eq_none_iff).
  (let [broken (cand :C1 [(dissoc p-alpha :guard :transition)])
        good (cand :C2 [p-alpha])]
    (is (= :missing (:status (ce/normalize broken))))
    (is (= :missing-pattern-interpretation (:kind (ce/normalize broken))))
    (is (map? (ce/equivalent? broken good)))))

(deftest provenance-kinds-and-nesting
  ;; The other two inadmissible kinds, at top level and nested, plus the
  ;; clean case. Bug this catches: a scanner keyed only to :hand-admitted,
  ;; or only to the top level, missing proof-fixture/reference-field deeper
  ;; in the chain.
  (let [fixture {:construction {:kind :proof-fixture}}
        reffield {:review-publication {:status :published :source {:kind :reference-field}}}
        top {:admission {:kind :reference-field}}]
    (is (false? (:admissible (ce/admissible-provenance? fixture))))
    (is (= [:construction] (:path (ce/admissible-provenance? fixture))))
    (is (= [:review-publication :source] (:path (ce/admissible-provenance? reffield))))
    (is (= [:admission] (:path (ce/admissible-provenance? top))))
    (is (true? (:admissible (ce/admissible-provenance? (cand :C1 [])))))))
