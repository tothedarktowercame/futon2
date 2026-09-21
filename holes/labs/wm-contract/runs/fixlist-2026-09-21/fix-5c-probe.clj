;; Discovery replay only: no interpreter, dispatch, activation or mission closure.
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.java.shell :as shell]
         '[clojure.string :as str]
         '[futon2.aif.interpretation-construction :as construction]
         '[futon2.aif.interpretation-evidence :as evidence]
         '[futon2.aif.observation-checks :as checks]
         '[futon2.aif.cascade-policy :as policy]
         '[futon2.aif.cascade-model-manifest :as model])
(import '[java.nio.file Files])

(def evidence-dir (io/file (.getParentFile (io/file *file*)) "fix-5c-evidence"))
(def manifest (edn/read-string (slurp (io/file evidence-dir "manifest.edn"))))
(defn source-text [file] (slurp (io/file evidence-dir file)))
(doseq [s (:sources manifest)]
  (assert (= (:sha256 s) (evidence/sha256 (Files/readAllBytes (.toPath (io/file evidence-dir (:file s))))))))
(defn citation [file a b]
  {:source (:id (first (filter #(= file (:file %)) (:sources manifest))))
   :lines [a b] :quote (str/join "\n" (subvec (vec (str/split-lines (source-text file))) (dec a) b))})
(def want-line (nth (str/split-lines (source-text "mission.source")) 149))
(assert (= "- [ ] Compilation cost, memory use, and repeated-query latency are measured on actual candidate families." want-line))
(def p :workshop/honest-holes-gate-composed-claims)
(def q :workshop/records-carry-warrant)
(def patterns
  {p {:guard {:needs #{:want-stated :report-requested} :forbids #{:holes-written}} :produces #{:holes-written}}
   q {:guard {:needs #{:holes-written} :forbids #{:warrant-written}} :produces #{:warrant-written}}})
(def receipts
  {p {:kind :hand-simulated-interpreter :by "codex-12" :date "2026-09-21"
      :source (dissoc (first (filter #(= "holes-pattern.source" (:file %)) (:sources manifest))) :file :id)
      :citations [(citation "mission.source" 150 150) (citation "holes-pattern.source" 28 42)]
      :reading "Before publishing a measurement claim, list its missing measurements and activation evidence, or a checked none. An incomplete report can be published only with those holes visible."
      :scope-limit "Produces the bounded gap report only; does not run a benchmark, establish activation, or close the mission checkbox."
      :observation-limit "C3 sees the report path, not the truth or completeness of its hole census."}
   q {:kind :hand-simulated-interpreter :by "codex-12" :date "2026-09-21"
      :source (dissoc (first (filter #(= "warrant-pattern.source" (:file %)) (:sources manifest))) :file :id)
      :citations [(citation "mission.source" 150 150) (citation "warrant-pattern.source" 27 41)]
      :reading "Given the gap report, retain its query, input digests, source versions and explicit assumptions in a derivation packet. Its claim may be that measurements are missing."
      :scope-limit "Produces a traceable claim about the gap report, not the absent measurements or task completion."
      :observation-limit "C3 sees the packet path; it does not validate digests, replay, or scientific correctness."}})
(def locators
  {:report-requested {:class :C4 :repo "fixture" :sha "HEAD" :path "scenario.edn"
                      :decl "{:auxiliary-report-requested true}"}
   :want-stated {:class :C4 :repo "fixture" :sha "HEAD" :path "mission.md" :decl want-line}
   :mission-measured {:class :C4 :repo "fixture" :sha "HEAD" :path "mission.md"
                      :decl (str/replace-first want-line "[ ]" "[x]")}
   :holes-written {:class :C3 :repo "fixture" :sha "HEAD" :path "evidence/MEASUREMENT-HOLES.edn"}
   :warrant-written {:class :C3 :repo "fixture" :sha "HEAD" :path "evidence/MEASUREMENT-WARRANT.edn"}})
(def root (.toFile (Files/createTempDirectory "fix5c-" (make-array java.nio.file.attribute.FileAttribute 0))))
(def repo (io/file root "fixture"))
(defn git! [& args]
  (let [r (apply shell/sh "git" "-C" (str repo) args)]
    (assert (zero? (:exit r)) (:err r)) r))
(try
  (.mkdirs repo)
  (spit (io/file repo "mission.md") (source-text "mission.source"))
  (spit (io/file repo "scenario.edn") "{:auxiliary-report-requested true}\n")
  (git! "init" "--quiet")
  (git! "add" "mission.md" "scenario.edn")
  (git! "-c" "user.name=fix5c-probe" "-c" "user.email=fixture@invalid" "-c" "commit.gpgsign=false"
        "commit" "--quiet" "-m" "Pinned mission observation fixture")
  (let [observed (with-redefs [checks/repo-root (str root)] (checks/observe locators))
        _ (assert (empty? (:refused observed)))
        observation (into {} (map (fn [[t r]] [t (:observed r)]) (:results observed)))
        order (mapv #(policy/token-interpretation % (patterns %)) [p q])
        scorer (fn [want]
                 (fn [c]
                   (model/horizon-g-sparse
                    {:q0 {#{:want-stated :report-requested} 1} :horizon 2
                     :precedence-fn (constantly (mapv #(policy/token-interpretation % (patterns %)) (:precedence c)))
                     :universe (set (keys observation))
                     :rates (zipmap (keys observation) (repeat {:false-pos 0 :false-neg 0}))
                     :spec {:want (set want) :lam 1 :mu 0}})))
        run (fn [want] (construction/construct
                        {:target "M-a-wmc-scaling" :want want :observation observation
                         :interpretations patterns :interpretation-receipts receipts
                         :budget {:max-moves 1 :max-expansions 32} :horizon 2 :move-cost 0
                         :evaluate-g (scorer want)}))
        mission (run [:mission-measured])
        ;; Explicit auxiliary want: not a substitute for the real mission want.
        auxiliary (run [:warrant-written])
        candidate (first (:candidates auxiliary))
        folded (model/rollout (constantly order) {#{:want-stated :report-requested} 1} 2)]
    (assert (= :no-supported-order (:kind mission)))
    (assert (some #(= :mission-measured (:token %)) (:findings mission)))
    (assert (= [p q] (:precedence candidate)))
    (assert (every? #(contains? % :warrant-written) (keys folded)))
    (assert (not-any? #(contains? % :mission-measured) (keys folded)))
    (prn {:attribution :hand-simulated-interpreter :author "codex-12"
          :observation observation :mission (select-keys mission [:status :kind :findings])
          :auxiliary-only {:status (:status auxiliary) :precedence (:precedence candidate)
                           :moves (get-in candidate [:construction-receipt :moves]) :belief folded}
          :receipt-limit "C3 presence does not establish contents or mission completion"}))
  (finally
    (doseq [f (reverse (file-seq root))] (io/delete-file f))
    (shutdown-agents)))
