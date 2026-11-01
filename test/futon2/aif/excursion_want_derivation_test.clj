(ns futon2.aif.excursion-want-derivation-test
  "EXCURSION-TARGET-FALSIFIER-AND-REVISION, wants half (run
  2026-11-01-ac5bc477, click 3a9ee0a1): an excursion selected with
  :selected-wants [] had its execution outcomes refused
  :selected-want-identity-missing because selected_mission_contract's
  require-before-author! guarded only `mission?` (M-) targets, so a non-mission
  target passed with an empty want list. Joe, 2026-11-01: an excursion with no
  wants in the standard format still has wants, determined on the fly from what
  the machine read at selection; only if none can be derived may the run say so,
  as a typed stop before author dispatch.

  Fixtures are the real bytes the ac5bc477 reading consumed: the futon6
  excursion document (sha256 6863756378922c1aa289e75c0c750f04bc66e797e04dae721f3c9e39b1afb663,
  matching the run record's :source-sha256) and the ac5bc477 selected-action
  shape (:kind :cascade-candidate, target E-comprehension-foundation,
  empty want set, as the run record's :selected-wants [] attests)."
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.action-identity :as action-identity]
            [futon2.aif.d-predecessor-task-authority :as d-task]
            [futon2.aif.mission-observation :as mission-observation]
            [futon2.aif.observation-checks :as observation]
            [futon2.aif.selected-mission-contract :as contract])
  (:import (java.nio.file Files)))

(def ^:private fixture-path
  "test/resources/wm/excursion-want-derivation/E-comprehension-foundation.md")

(def ^:private target-id "E-comprehension-foundation")

(def ^:private excursion-rel-path "holes/excursions/E-comprehension-foundation.md")

(def ^:private git-routing-environment-keys
  ["GIT_DIR" "GIT_WORK_TREE" "GIT_INDEX_FILE" "GIT_COMMON_DIR"])

(defn- isolated-git-environment []
  (apply dissoc (into {} (System/getenv)) git-routing-environment-keys))

(defn- git! [repo & args]
  (let [r (apply shell/sh (concat ["git" "-C" (str repo)] args
                                  [:env (isolated-git-environment)]))]
    (when-not (zero? (:exit r))
      (throw (ex-info "fixture git failed" r)))
    (str/trim (:out r))))

(defn- with-excursion-repo
  "A temp code root holding one repository named futon6, carrying the REAL
  excursion bytes at their real relative path, on two commits: the bytes as
  read at selection, then the same document with one closure-criteria box
  ticked (the author's possible real work). Redefines observation/repo-root so
  the C3/C4 readers resolve futon6 against the temp root, never the live
  checkout."
  [f]
  (let [root (.toFile (Files/createTempDirectory "excursion-want-"
                                                 (make-array java.nio.file.attribute.FileAttribute 0)))
        repo (io/file root "futon6")
        doc (io/file repo excursion-rel-path)]
    (try
      (.mkdirs (.getParentFile doc))
      (spit doc (slurp fixture-path))
      (git! repo "init" "-q")
      (git! repo "add" ".")
      (git! repo "-c" "user.name=E fixture"
            "-c" "user.email=e-fixture@example.invalid"
            "commit" "-qm" "excursion source at selection")
      (let [before-sha (git! repo "rev-parse" "HEAD")
            ticked (-> (slurp fixture-path)
                       (str/replace-first
                        "- [ ] Every proof record carries a corpus-relative comprehension score and an explicit gap-to-ceiling"
                        "- [x] Every proof record carries a corpus-relative comprehension score and an explicit gap-to-ceiling"))]
        (when (= ticked (slurp fixture-path))
          (throw (ex-info "fixture tick mutation did not apply" {})))
        (spit doc ticked)
        (git! repo "add" ".")
        (git! repo "-c" "user.name=E fixture"
              "-c" "user.email=e-fixture@example.invalid"
              "commit" "-qm" "tick first closure criterion")
        (let [after-sha (git! repo "rev-parse" "HEAD")]
          (with-redefs [observation/repo-root (str root)]
            (f {:root (str root) :repo (str repo)
                :before before-sha :after after-sha}))))
      (finally
        (doseq [file (reverse (file-seq root))] (.delete file))))))

(defn- ac5bc477-selected-action []
  "The ac5bc477 selected-action shape: a cascade-candidate reading action for
  the excursion target with the empty want set the run record retained."
  {:kind :cascade-candidate :id :C0 :target target-id :want []})

(deftest real-excursion-source-derives-wants-on-the-fly
  (with-excursion-repo
   (fn [{:keys [root]}]
     (let [abs-path (str root "/futon6/" excursion-rel-path)
           contract (contract/source-contract root target-id abs-path (slurp fixture-path))]
       (is (map? contract))
       (is (seq (:want contract)) "the real excursion text must yield wants")
       (let [locators (:observation-locators contract)]
         ;; The four real closure-criteria checkboxes are C4 locators over
         ;; the excursion's own document in its own repository.
         (is (= 4 (count (filter (fn [[_ l]] (and (= :C4 (:class l))
                                                  (re-find #"\[x\]" (str (:decl l)))))
                                 locators)))
             "the four closure-criteria boxes each become a C4 ticked-decl want")
         (doseq [[_ locator] locators]
           (is (= "futon6" (:repo locator)))
           (is (= excursion-rel-path (:path locator)))))
       ;; attach + guard: the ac5bc477 action passes require-before-author!
       (let [action (contract/attach (ac5bc477-selected-action) contract)]
         (is (= (:want contract) (:want action)))
         (is (seq (get action :observation-locators)))
         (is (= action (contract/require-before-author! action))))))))

(deftest derived-excursion-wants-are-measured-by-the-c3-c4-readers
  (with-excursion-repo
   (fn [{:keys [root repo before after]}]
     (let [abs-path (str repo "/" excursion-rel-path)
           contract (contract/source-contract root target-id abs-path (slurp fixture-path))
           action (contract/attach (ac5bc477-selected-action) contract)
           wants (mapv #(vector target-id %) (:want action))
           base {:occurrence {:action/id "occ-exc-ac5bc477" :action/value action}
                 :universe (set wants)
                 :declarations []
                 ;; the production D-task capture carries the selected
                 ;; action's own observation carrier; the readers read this,
                 ;; not the action alone
                 :selected-action-observation-carrier
                 {:target target-id
                  :observation-locators (:observation-locators action)
                  :selected-action-sha256 (action-identity/digest action)}
                 :before {:head before}}
           baseline (mission-observation/capture-before
                     {:dispatch base :repository repo :revision before})
           after-rows (mission-observation/capture-after
                       {:dispatch base :repository repo :revision after})
           projected (mission-observation/project
                      {:baseline baseline :dispatch base :repository repo
                       :after-revision after :after-rows after-rows})]
       (is (= :captured (:status baseline)))
       (is (every? false? (map :observed (:rows baseline)))
           "every derived want is false at the selection bytes")
       (is (= :admitted (:status projected)))
       (let [flipped (count (filter (comp true? :observed :after) (:rows projected)))]
         (is (= 1 flipped)
             "exactly the ticked closure criterion becomes true after the tick commit"))
       (is (= 1 (count (filter (fn [row] (some-> row :after :observed true?)) (:rows projected)))))))))

(deftest excursion-with-no-derivable-want-stops-typed-before-author-contact
  (with-excursion-repo
   (fn [{:keys [root repo]}]
     ;; The real document's opening section only: a real excursion text that
     ;; states no checkable closure form. Not a hand-invented shape — the
     ;; bytes are the real source's, truncated before any checkbox section.
     (let [wantless-text (->> (slurp fixture-path)
                              str/split-lines
                              (take-while #(not (re-find #"(?i)closure criteria" %)))
                              (str/join "\n"))
           _ (is (not (re-find #"(?m)^[-*]\s+\[\s\]" wantless-text))
                 "precondition: the truncated text carries no checkbox")
           contract (contract/source-contract root target-id
                                              (str repo "/" excursion-rel-path)
                                              wantless-text)]
       (is (= [] (:want contract)))
       (is (= :empty (get-in contract [:excursion-want-derivation :status])))
       (is (= :no-checkable-closure-form-in-source
              (get-in contract [:excursion-want-derivation :reason])))
       (let [action (contract/attach (ac5bc477-selected-action) contract)]
         (is (empty? (:want action)))
         (let [ex (is (thrown? Exception (contract/require-before-author! action)))]
           (is (= :refused (:outcome (ex-data ex))))
           (is (= :excursion-want-derivation-empty (:failure-kind (ex-data ex))))
           (is (= :pre-author (:failure-stage (ex-data ex))))
           (is (= :no-checkable-closure-form-in-source
                  (get-in (ex-data ex) [:derivation :reason])))
           (is (str/includes? (str (get-in (ex-data ex) [:selection-feedback]))
                              target-id)
               "the stop feeds back why a wantless target was selected at all")))))))

(deftest mission-contract-behaviour-is-unchanged-for-missions
  ;; The M- branch keeps its exact legacy semantics: nil contract for a
  ;; wantless mission source, the original refusal kind, unchanged mission?.
  (let [action {:target "M-nothing-stated" :want []}]
    (is (contract/mission? action))
    (is (not (contract/excursion? action)))
    (is (nil? (contract/source-contract "/nonexistent-root" "M-nothing-stated" "/nonexistent/x.md" "no wants here")))
    (let [ex (is (thrown? Exception (contract/require-before-author! action)))]
      (is (= :selected-want-contract-missing (:failure-kind (ex-data ex)))))))
