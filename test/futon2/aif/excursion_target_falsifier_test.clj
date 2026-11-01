(ns futon2.aif.excursion-target-falsifier-test
  "EXCURSION-TARGET-FALSIFIER-AND-REVISION (2026-11-01).

  Fixtures are the real receipt shapes from run 2026-11-01-ac5bc477-9e34-4249-a692-f13a9f22a324
  (click 3a9ee0a1), finding repair-occ-e14c140ee3f5: the excursion
  E-comprehension-foundation selected with an empty want set, author commit
  7863763527cdea0fccc4ea2e482aa8da08552db4, whose review-gate and
  artifact-binding passed and whose only failing falsifier checks were
  :selected-want-accounting (:selected-want-identity-missing over wants [])
  and :evidence-authority (:revision-authority-missing over measurements []).
  The work passed on its merits; those two checks were vacuous for a target
  that selected no wants."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-revision :as revision]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.reviewer-falsifier :as falsifier]))

(def target "E-comprehension-foundation")
(def commit "7863763527cdea0fccc4ea2e482aa8da08552db4")

;; Verbatim from the finding's reviewer-falsifier receipt: the refused
;; want-accounting receipt the excursion click actually produced.
(def excursion-want-outcomes
  {:schema :wm/selected-want-outcome-v1
   :status :refused
   :reason :selected-want-identity-missing
   :details {:target target :wants []}})

(defn excursion-inputs []
  {:target target :commit commit
   :work-evidence {:kind :ordinary-cascade}
   :mission-standing {:status :not-applicable}
   :selected-want-outcomes excursion-want-outcomes
   :disposition nil
   :artifact-binding {:repo "/home/joe/code/futon2" :commit commit
                      :pre-dispatch-head "b055fb78c9084a359c7261fd964b4ea493ddd6d7"
                      :observed-head commit :corroborates? true}
   :review-gate {:required? true
                 :code-files ["src/futon2/aif/wm/enacted/e_comprehension_foundation.clj"]
                 :executed? true :tool-events 8 :execution-source :job-summary
                 :passed? true}})

(deftest excursion-without-wants-falsifier-checks-are-not-applicable
  (let [receipt (falsifier/receipt (excursion-inputs))
        verification (falsifier/verify receipt (excursion-inputs))]
    (is (= :not-applicable
           (get-in receipt [:checks :selected-want-accounting :status])))
    (is (= :no-selected-wants-to-account
           (get-in receipt [:checks :selected-want-accounting :reason])))
    (is (= :not-applicable
           (get-in receipt [:checks :evidence-authority :status])))
    (is (= :no-selected-wants-no-measurements
           (get-in receipt [:checks :evidence-authority :reason])))
    (is (= :verified (:status receipt)))
    (is (= [] (:failed-checks receipt)))
    (is (= :verified (:status verification)))))

(deftest mission-target-with-wants-still-fails-when-unaccounted
  ;; The bad case that must keep failing: a mission target that selected a
  ;; want but cannot account for it. Census mismatch over a real want set.
  (let [mtarget "M-current"
        refused {:schema :wm/selected-want-outcome-v1 :status :refused
                 :reason :selected-want-census-mismatch
                 :details {:selected [[mtarget "DOCUMENT"]]
                           :observed []}}
        in (-> (excursion-inputs)
               (assoc :target mtarget)
               (assoc :mission-standing
                      {:status :consistent :source {:path "/m.md" :sha256 "s"}
                       :declarations []})
               (assoc :selected-want-outcomes refused))
        receipt (falsifier/receipt in)]
    (is (= :fail (get-in receipt [:checks :selected-want-accounting :status])))
    (is (= :selected-want-census-mismatch
           (get-in receipt [:checks :selected-want-accounting :reason])))
    (is (= :fail (get-in receipt [:checks :evidence-authority :status])))
    (is (= :refused (:status receipt)))
    (is (contains? (set (:failed-checks receipt)) :selected-want-accounting))
    (is (contains? (set (:failed-checks receipt)) :evidence-authority))))

(deftest unexplained-identity-refusal-with-nonempty-wants-is-not-exempt
  ;; :selected-want-identity-missing with wants PRESENT in the details (or no
  ;; details at all) is a broken accounting receipt, not an exemption.
  (let [bad {:schema :wm/selected-want-outcome-v1 :status :refused
             :reason :selected-want-identity-missing
             :details {:target "M-current" :wants [["M-current" "DOCUMENT"]]}}
        receipt (falsifier/receipt (assoc (excursion-inputs)
                                          :selected-want-outcomes bad))]
    (is (= :fail (get-in receipt [:checks :selected-want-accounting :status])))
    (is (= :fail (get-in receipt [:checks :evidence-authority :status])))))

(deftest excursion-entry-resolves-to-its-real-source-file
  (let [entry (runner/excursion-entry-for target)]
    (is (map? entry))
    (is (= :excursion (:kind entry)))
    (is (= target (:id entry)))
    (is (.isFile (io/file (:path entry)))
        "the revision path must pin a real excursion doc")
    (is (string? (get-in entry [:source :sha256])))))

(deftest excursion-mission-gives-revision-a-real-whole-context
  ;; Pre-fix, the construction's :mission-entry was nil for excursions, so
  ;; whole-mission-context refused :whole-mission-source-unavailable with
  ;; :path nil and cascade revision never reached a proposal.
  (let [entry (runner/excursion-entry-for target)
        whole (revision/whole-mission-context entry)
        candidate (fn [id patterns]
                    {:kind :cascade-candidate :id id :target target
                     :want []
                     :precedence (mapv (fn [p] {:id p :target target}) patterns)
                     :construction-receipt {:kind :machine-constructed}
                     :interpretation-receipts {}})
        original (candidate :C1 [:patterns/a])
        blocker (revision/blocker target :reviewer-verdict :review-request-changes
                                  {:review-job "invoke-1793505714222-9057-6a4cf703"
                                   :findings-sha256
                                   "c85a73ce4e7f2b743087b274d40eb9dcd303e1ea150c40da69b33b16b52f1ce9"})
        result (revision/revise-from-blocker
                {:mission entry
                 :construction {:selected-action original}
                 :blocker blocker
                 :proposals-fn (constantly [(candidate :C2 [:patterns/a :patterns/b])])
                 :producer-context {}})]
    (is (= revision/context-schema (:schema whole)))
    (is (string? (:path whole)))
    (is (string? (:sha256 whole)))
    (is (= :revised (:status result))
        "revision reaches a real proposal through the excursion source")
    (is (= [:patterns/b] (get-in result [:delta :added])))))

(deftest nil-mission-still-refuses-with-a-nil-path
  ;; The old excursion behaviour, retained as the typed refusal for a target
  ;; with no resolvable source: explicit, not a silent empty pass.
  (let [whole (revision/whole-mission-context nil)]
    (is (= :refused (:status whole)))
    (is (= :whole-mission-source-unavailable (:kind whole)))
    (is (nil? (:path whole)))))
