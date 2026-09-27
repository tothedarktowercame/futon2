(ns futon2.aif.lifecycle-exits-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.lifecycle-exits :as le]
            [futon2.aif.mission-criteria :as mc]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.cascade-problems :as cp]))

(def definition-pin "3ae47f1b92d62230700deb9e8b1847a1c9c8b287")
(def mission-pin "827188aa273503c138257b9b1e3148c25f392090")
(defn pinned [repo sha path]
  (let [r (shell/sh "git" "-C" (.getCanonicalPath (io/file ".." repo))
                    "show" (str sha ":" path))]
    (when-not (zero? (:exit r)) (throw (ex-info "Pinned source unavailable" r)))
    (:out r)))
(def definition (delay (pinned "futon4" definition-pin "holes/mission-lifecycle.md")))
(defn mission [id] (pinned "futon3c" mission-pin (str "holes/missions/" id ".md")))

(deftest definition-pin-and-invalid-definition
  (let [exits (le/definition-exits @definition)]
    (is (= le/phases (mapv :phase exits)))
    (is (= 8 (count exits)))
    (is (= "**Exit criterion:** Every MAP question has a concrete answer. The \"ready vs"
           (first (str/split-lines (:stated (nth exits 2))))))
    (is (= :invalid-lifecycle-definition
           (try (le/definition-exits "## MAP\n**Exit criterion:** incomplete")
                (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))
    (println :definition-exits exits)))

(deftest wiring-mission-supplies-eight
  (let [r (le/supplied "M-wm-wiring" (mission "M-wm-wiring") @definition)]
    (is (:lifecycle-shaped? r))
    (is (= le/phases (mapv :phase (:criteria r))))
    (is (every? #(= :how (:role %)) (:criteria r)))
    (println :wiring-supplied (mapv #(select-keys % [:phase :token]) (:criteria r)))))

(deftest seams-preserves-six-exits
  (let [text (mission "M-futon-seams")
        existing (filterv #(= :phase-exit (:kind %)) (mc/criteria "M-futon-seams" text))
        cs (:criteria (le/supplied "M-futon-seams" text @definition))
        combined (str text "\n" (str/join "\n\n" (map #(str "## " (name (:phase %)) "\n" (:stated %)) cs)))
        reread (filterv #(= :phase-exit (:kind %)) (mc/criteria "M-futon-seams" combined))]
    (is (= 6 (count existing)))
    (is (= [:HEAD :IDENTIFY] (mapv :phase cs)))
    (is (= (mapv :token existing) (mapv :token (take 6 reread))))
    (is (= 8 (count reread)))
    (println :seams-existing (mapv :token existing) :seams-supplied (mapv #(select-keys % [:phase :token]) cs))))

(deftest copied-map-uses-existing-token
  (let [all (:criteria (le/supplied "fixture" "## MAP\n" @definition))
        c (first (filter #(= :MAP (:phase %)) all))
        text (str "## MAP\n" (:stated c))]
    (is (= (:token c) (:token (first (mc/criteria "fixture" text)))))
    (is (= 7 (count (:criteria (le/supplied "fixture" text @definition)))))
    (is (not-any? #(= :MAP (:phase %)) (:criteria (le/supplied "fixture" text @definition))))))

(deftest not-a-lifecycle
  (is (= {:lifecycle-shaped? false :phases [] :criteria [] :reason :not-lifecycle-shaped}
         (le/supplied "fixture" "## Mapping\n### MAP\nordinary text" "unused"))))

(deftest admission-and-real-assembly
  (let [cs (:criteria (le/supplied "fixture" "## MAP\n" @definition))
        [located missing] (map :token cs)
        loc {:class :C8 :repo "futon2" :namespace "futon2.aif.lifecycle-exits-test"}
        w (le/wants cs {located loc} (constantly false))
        sources {:universes {"fixture" (:universe w)} :wants {"fixture" (:wants w)}
                 :locators {"fixture" (:locators w)}
                 :interpretations {"fixture" {:patterns {:p {:guard {:needs #{} :forbids #{}} :produces #{located}}}}}
                 :candidates {"fixture" [{:precedence [:p] :construction-receipt {:kind :fixture}}]}
                 :horizon-steps 1 :context-of (constantly :WM) :beta-by-context {:WM 1}}
        good (cp/assemble {:targets ["fixture"] :sources sources})
        bad (cp/assemble {:targets ["fixture"] :sources (update-in sources [:wants "fixture"] conj missing)})]
    (is (= [located] (:wants w)))
    (is (= {located false} (:universe w)))
    (is (= 7 (count (:unlocated w)) (count (:to-ask w)) ))
    (is (every? #(and (= :how (:role %)) (= :no-admitted-locator (:reason %))) (:unlocated w)))
    (is (empty? (:wants (le/wants cs {located {:class :J}} #(throw (ex-info "Must not observe" %))))))
    (is (thrown? clojure.lang.ExceptionInfo (le/wants cs {located loc} (constantly nil))))
    (is (empty? (:refusals good)))
    (is (= 1 (count (:problems good))))
    (is (= :universe-not-admitted (get-in bad [:refusals 0 :kind])))
    (is (= [missing] (get-in bad [:refusals 0 :tokens-without-checkable-locator])))
    (println :assembly-good good :assembly-forced-unlocated bad)))

(defn- with-verdict-repo [f]
  (let [root (.toFile (java.nio.file.Files/createTempDirectory
                       "exit-verdict-" (make-array java.nio.file.attribute.FileAttribute 0)))
        git (fn [& args]
              (let [r (apply shell/sh "git" "-C" (str root) args)]
                (when-not (zero? (:exit r)) (throw (ex-info "Fixture git failed" r)))
                (:out r)))]
    (try
      (git "init")
      (git "config" "user.name" "fixture")
      (git "config" "user.email" "fixture@example.invalid")
      (doseq [[file text]
              {"good.md" "## MAP
**MAP exit: Met.**
## ARGUE
**Met.**
"
               "not-met.md" "## MAP
**MAP exit: Not met.**
"
               "instance.md" "## MAP
**MAP exit: Met for instance 4**
"
               "paragraph.md" "## MAP
Some prose **MAP exit: Met.**
"
               "misplaced.md" "## ARGUE
**MAP exit: Met.**
"
               "wiring.md" "## MAP
**MAP exit: Met.**
## ARGUE
**ARGUE exit: Met.**
"}]
        (spit (io/file root file) text))
      (git "add" "--" "good.md" "not-met.md" "instance.md" "paragraph.md" "misplaced.md" "wiring.md")
      (git "commit" "-m" "Written verdict fixtures")
      (with-redefs [checks/repo-root (.getParent root)]
        (f {:repo (.getName root) :root root}))
      (finally (doseq [file (reverse (file-seq root))] (io/delete-file file true))))))

(deftest written-verdict-c4-and-section-limit
  (with-verdict-repo
    (fn [{:keys [repo root]}]
      (let [cs (:criteria (le/supplied "fixture" "## MAP
" @definition))
            by-phase (into {} (map (juxt :phase :token) cs))
            observe (fn [file phase]
                      (checks/check-decl-in-file
                       (get (le/verdict-locators cs {:repo repo :path file}) (by-phase phase))))]
        (is (= "**MAP exit: Met.**" (le/verdict-decl :MAP)))
        (is (= {} (le/verdict-locators [(dissoc (first cs) :supplied-by)] {:repo repo :path "good.md"})))
        (doseq [[file phase expected] [["good.md" :MAP true] ["good.md" :ARGUE false]
                                      ["not-met.md" :MAP false] ["instance.md" :MAP false]
                                      ["paragraph.md" :MAP false] ["misplaced.md" :MAP true]]]
          (let [r (observe file phase)]
            (is (= expected (:observed r)) (pr-str [file phase r]))
            (println :verdict-check file phase (:observed r))))
        (is (= {:phase :MAP :verdict :met :in-section? false :misplaced-under "ARGUE"}
               (first (filter #(= :MAP (:phase %))
                              (le/section-verdicts (slurp (io/file root "misplaced.md")))))))
        (is (= [{:phase :MAP :verdict :not-met :in-section? true}
                {:phase :MAP :verdict :not-started :in-section? true}]
               (filterv #(= :MAP (:phase %))
                        (le/section-verdicts "## MAP
**MAP exit: Not met.**
**MAP exit: Not started.**"))))
        (is (= {:phase :HEAD :verdict nil} (first (le/section-verdicts ""))))))))

(deftest all-convention-located-exits-are-admitted
  (with-verdict-repo
    (fn [{:keys [repo root]}]
      (let [text (slurp (io/file root "wiring.md"))
            cs (:criteria (le/supplied "M-wm-wiring" text @definition))
            locs (le/verdict-locators cs {:repo repo :path "wiring.md"})
            w (le/wants cs locs #(:observed (checks/check-decl-in-file %)))
            sources {:universes {"M-wm-wiring" (:universe w)} :wants {"M-wm-wiring" (:wants w)}
                     :locators {"M-wm-wiring" (:locators w)}
                     :interpretations {"M-wm-wiring" {:patterns {:p {:guard {:needs #{} :forbids #{}}
                                                                      :produces (set (:wants w))}}}}
                     :candidates {"M-wm-wiring" [{:precedence [:p] :construction-receipt {:kind :fixture}}]}
                     :horizon-steps 1 :context-of (constantly :WM) :beta-by-context {:WM 1}}
            assembled (cp/assemble {:targets ["M-wm-wiring"] :sources sources})]
        (is (= 8 (count cs) (count (:wants w)) (count (:locators w))))
        (is (= {true 2 false 6} (frequencies (vals (:universe w)))))
        (doseq [{:keys [token phase]} cs]
          (is (= (contains? #{:MAP :ARGUE} phase) (get-in w [:universe token]))))
        (is (every? #(= :verdict-line-convention (:located-by %)) (vals (:criteria-by-token w))))
        (is (every? #(= #{:class :repo :sha :path :decl} (set (keys %))) (vals locs)))
        (is (empty? (:unlocated w)))
        (is (empty? (:refusals assembled)))
        (is (= 1 (count (:problems assembled))))
        (println :verdict-universe (:universe w) :assembled assembled)))))

(deftest a-verdict-line-with-prose-after-it-is-read-as-c4-observes-it
  ;; The report and the check must agree on what counts as a verdict line.
  (let [text (str "## HEAD\n\n**HEAD exit: Met.** The operator's anchor turn is quoted above.\n\n"
                  "## MAP\n\n  **MAP exit: Not met.** (two questions open)\n\n"
                  "## ARGUE\n\n**ARGUE exit: Met.**x\n\nText **DERIVE exit: Met.** mid-line.\n")
        by-phase (into {} (map (juxt :phase identity)) (le/section-verdicts text))]
    (is (= {:phase :HEAD :verdict :met :in-section? true} (:HEAD by-phase)))
    (is (= {:phase :MAP :verdict :not-met :in-section? true} (:MAP by-phase)))
    (is (nil? (:verdict (:ARGUE by-phase))) "a character glued to the verdict is not a verdict line")
    (is (nil? (:verdict (:DERIVE by-phase))) "mid-line is not a verdict line")
    (doseq [[phase expected] {:HEAD true :MAP false :ARGUE false :DERIVE false}]
      (is (= expected (checks/decl-present? text (le/verdict-decl phase))) (name phase)))
    (println :verdict-lines-with-prose (mapv #(select-keys (get by-phase %) [:phase :verdict]) [:HEAD :MAP :ARGUE :DERIVE]))))

(deftest current-phase-is-explicit
  (is (= {:phase :IDENTIFY}
         (le/current-phase "**Status:** HEAD complete; IDENTIFY pending")))
  (is (= {:phase :DERIVE} (le/current-phase "**Status:** DERIVE (2026-09-27)")))
  (is (= {:absent :status-line-missing} (le/current-phase "## MAP")) )
  (is (= {:absent :status-phase-unrecognised :line "**Status:** UNKNOWN"}
         (le/current-phase "**Status:** UNKNOWN
**Status:** MAP")))
  (println :composite (le/current-phase "**Status:** HEAD complete; IDENTIFY pending")))

(deftest reached-exits-and-misplaced-verdicts
  (with-verdict-repo
    (fn [{:keys [repo root]}]
      (let [text "**Status:** DERIVE (2026-09-27)
## MAP
**MAP exit: Met.** Evidence.
"
            bad "**Status:** DERIVE (2026-09-27)
## ARGUE
**MAP exit: Met.**
"
            git (fn [& args] (let [r (apply shell/sh "git" "-C" (str root) args)]
                              (when-not (zero? (:exit r)) (throw (ex-info "fixture git" r)))))
            _ (spit (io/file root "reached.md") text)
            _ (spit (io/file root "bad.md") bad)
            _ (git "add" "--" "reached.md" "bad.md")
            _ (git "commit" "-m" "Phase fixtures")
            opts {:repo repo :path "reached.md" :observe #(:observed (checks/check-decl-in-file %))}
            w (le/flight-exits "fixture" text @definition opts)
            b (le/flight-exits "fixture" bad @definition (assoc opts :path "bad.md"))
            tokens (into {} (map (juxt :phase :token) (vals (:criteria-by-token w))))
            unknown (le/flight-exits "fixture" "## MAP
" @definition
                                     (assoc opts :observe (fn [_] (throw (Exception. "must not observe")))))
            sources {:universes {"fixture" (:universe w)} :wants {"fixture" (:wants w)}
                     :locators {"fixture" (:locators w)}
                     :interpretations {"fixture" {:patterns {:p {:guard {:needs #{} :forbids #{}}
                                                                 :produces (set (:wants w))}}}}
                     :candidates {"fixture" [{:precedence [:p] :construction-receipt {:kind :fixture}}]}
                     :horizon-steps 1 :context-of (constantly :WM) :beta-by-context {:WM 1}}
            assembled (cp/assemble {:targets ["fixture"] :sources sources})]
        (is (= [:HEAD :IDENTIFY :MAP :DERIVE]
               (mapv #(get-in w [:criteria-by-token % :phase]) (:wants w))))
        (is (= (zipmap (map tokens [:HEAD :IDENTIFY :MAP :DERIVE]) [false false true false])
               (:universe w)))
        (is (= [:ARGUE :VERIFY :INSTANTIATE :DOCUMENT] (mapv :phase (:not-started w))))
        (is (every? #(= :phase-not-reached (:reason %)) (:not-started w)))
        (is (not-any? (set (:wants w)) (map :token (:not-started w))))
        (is (empty? (:to-ask w)))
        (is (true? (:observed (checks/check-decl-in-file (get-in b [:locators (tokens :MAP)])))))
        (is (false? (get-in b [:universe (tokens :MAP)])))
        (is (= {:reason :verdict-line-misplaced :misplaced-under "ARGUE"}
               (get-in b [:criteria-by-token (tokens :MAP) :not-counted])))
        (is (empty? (:wants unknown)))
        (is (= 8 (count (:not-started unknown))))
        (is (every? #(= :current-phase-unknown (:reason %)) (:not-started unknown)))
        (is (= {:absent :status-line-missing} (:current-phase unknown)))
        (is (empty? (:refusals assembled)))
        (is (= 1 (count (:problems assembled))))
        (println :reached w :misplaced b :unknown unknown :assembly-count (count (:problems assembled)))))))

(deftest pinned-seams-flight-exits
  (let [text (mission "M-futon-seams")
        r (le/flight-exits "M-futon-seams" text @definition
                           {:repo "futon3c" :path "holes/missions/M-futon-seams.md"
                            :sha mission-pin :observe (constantly false)})]
    (is (= {:phase :DOCUMENT :state :complete} (:current-phase r)))
    (is (= 2 (count (:wants r))))
    (is (empty? (:not-started r)))
    (println :seams-pin mission-pin :current-phase (:current-phase r)
             :supplied-wants (:wants r) :not-started (:not-started r))))


(deftest lifecycle-status-vocabulary-and-census
  (doseq [status ["Complete" "COMPLETE" "DONE" "**COMPLETE (date)**"]]
    (is (= {:phase :DOCUMENT :state :complete} (le/current-phase (str "**Status:** " status)))))
  (doseq [[status phase] [["INSTANTIATE complete" :INSTANTIATE]
                          ["INSTANTIATE near complete — work" :INSTANTIATE]
                          ["MAP → DERIVE iterating" :DERIVE]]]
    (is (= {:phase phase} (le/current-phase (str "**Status:** " status)))))
  (doseq [status ["BLOCKED" "DEFERRED" "NONSTARTER" "RE-OPENED" "OPEN — HEAD through VERIFY drafted"]]
    (is (= :status-phase-unrecognised (:absent (le/current-phase (str "**Status:** " status))))))
  (let [records (for [repo ["futon2" "futon3c"]
                      :let [sha (str/trim (:out (shell/sh "git" "-C" (str "../" repo) "rev-parse" "HEAD")))
                            paths (str/split-lines (:out (shell/sh "git" "-C" (str "../" repo)
                                                                  "ls-tree" "-r" "--name-only" sha "holes/missions")))]
                      path paths :when (re-find #"/M-[^/]+\.md$" path)
                      :let [text (pinned repo sha path)]
                      :when (:lifecycle-shaped? (le/lifecycle-shaped? text))]
                  (le/current-phase text))]
    (println :status-census {:phase (count (filter :phase records))
                             :absent (count (filter :absent records))
                             :unrecognised (frequencies (map :line (filter #(= :status-phase-unrecognised (:absent %)) records)))})))
