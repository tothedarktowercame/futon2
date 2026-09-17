(ns futon2.aif.flat-path-gate-test
  "E2 of SPEC-flat-removal-and-cascade-decision (p4ng
  wm-walkthroughs/build-loop/closure/): a source gate over production code for
  calls to the flat selectors.

  Joe, 2026-09-17: \"I reject it wholly, and I always have... Let's rip it
  out\", and then: make it impossible to run the machine with that setting.
  The decision gate (futon2.aif.decision-gate) refuses a flat decision at
  emit time; this gate refuses a flat CALL SITE at test time, so the flat law
  cannot be reached from production code again without this test failing.

  Scanned: every .clj under src/futon2 and scripts. A match is a call
  position — `(select-action`, `(policy/select-action`, ... — after line
  comments are stripped, so docstrings and prose that name the retired
  functions are left alone. `select-action-cascades` is the CASCADE selector
  and is not matched.

  One exception remains, `src/futon2/aif/policy.clj`, where the retired
  functions still call each other internally. H6b deletes them and empties
  the exception list; `exception-still-needed` fails when that has happened,
  so the exception cannot outlive the code it covers."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]))

(def retired
  "The flat selectors. Nothing on the production path may call them."
  ["select-action" "default-mode-select" "strategic-recommendation"
   "compose-proposers"])

(def call-site
  ;; an open paren, an optional namespace alias, the name, and a terminator
  ;; that is not a symbol character (so select-action-cascades does not match)
  (re-pattern
   (str "\\((?:[A-Za-z0-9._<>*+!?=-]+/)?(?:"
        (str/join "|" retired)
        ")(?![A-Za-z0-9*+!_?<>=-])")))

(def scanned-roots ["src/futon2" "scripts"])

(def exceptions
  "Files where the retired functions still call each other. H6b empties this."
  #{"src/futon2/aif/policy.clj"})

(defn strip-comments [line]
  (let [i (str/index-of line ";")]
    (if i (subs line 0 i) line)))

(defn- clj-files [root]
  (->> (file-seq (io/file root))
       (filter #(.isFile ^java.io.File %))
       (filter #(str/ends-with? (.getName ^java.io.File %) ".clj"))))

(defn- violations-in [^java.io.File f]
  (let [path (str/replace (.getPath f) #"^\./" "")]
    (when-not (contains? exceptions path)
      (->> (str/split-lines (slurp f))
           (map-indexed (fn [i line] [(inc i) (strip-comments line)]))
           (keep (fn [[n line]]
                   (when (re-find call-site line)
                     (str path ":" n ": " (str/trim line)))))
           seq))))

(defn violations []
  (->> scanned-roots (mapcat clj-files) (mapcat violations-in) vec))

(deftest no-flat-selector-call-sites-in-production-source
  (let [found (violations)]
    (is (empty? found)
        (str "production source calls a retired flat selector. The production "
             "tick decides over cascades (a cascade is a policy, and G is "
             "computed over policies) or abstains with typed refusals:\n"
             (str/join "\n" found)))))

(deftest gate-detects-a-call-site
  ;; the gate is only worth having if it would fail: check the pattern on
  ;; text, not on the tree, so this stays true after H6b's deletions
  (is (re-find call-site "  (policy/select-action ranked {})"))
  (is (re-find call-site "(default-mode-select x)"))
  (is (re-find call-site "(ap/compose-proposers [a b])"))
  (is (nil? (re-find call-site "(select-action-cascades problem)"))
      "the cascade selector is not the flat one")
  (is (nil? (re-find call-site (strip-comments "  ;; the old (select-action) call lived here")))
      "prose in a line comment is not a call site")
  (is (nil? (re-find call-site "   `policy/select-action` returns :softmax-weights"))
      "a docstring reference is not a call site"))

(deftest exception-still-needed
  (doseq [path exceptions]
    (is (.exists (io/file path))
        (str path " is gone: delete it from `exceptions` so the gate covers "
             "every production file again"))))
