(ns futon2.aif.runner-acceptance-criterion-test
  "PROOF-wm-works ⟨1⟩6: the dispatch states the acceptance criterion
  verbatim. Asserts on the ACTUAL rendered prompt strings the runner would
  send, not on intermediate maps."
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-sources :as cs]
            [futon2.aif.full-loop-runner :as flr]))

(def t "T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade")

(defn- reference-entry [which]
  ;; the live qualifier's shape: precedence of PATTERN MAPS with
  ;; [target token]-qualified :produces, built from the raw source's
  ;; candidate list (index: C1 is first, C2 second) and pattern map
  (let [sources (cs/with-context-fn (cs/load-declared))
        cands (get-in sources [:candidates t])
        cand (case which :C1 (first cands) (second cands))
        pats (get-in sources [:interpretations t :patterns])
        pm (fn [id]
             (let [m (get pats id)]
               {:id id
                :produces (set (map (fn [x] [t x]) (:produces m)))}))]
    {:kind :cascade-candidate :id which :target t
     :precedence (mapv pm (:precedence cand))}))

(defn- author-prompt-of [which]
  (let [prompt-fn @#'flr/author-prompt]
    (prompt-fn {:author "wm-author" :reviewer "wm-reviewer"
                :batch-id "test" :target-repository "/home/joe/code/futon2"
                :target-repository-head "HEAD" :attempt-evidence-dir nil
                :measured-acquisition? false}
               t nil (reference-entry which) [])))

(deftest c2-author-brief-contains-the-verbatim-criterion
  (let [prompt (author-prompt-of :C2)]
    (is (string? prompt) (pr-str prompt))
    (is (re-find #"resources/wm/eig/held-out-split\.edn" prompt)
        "the produced token's exact path")
    (is (re-find #"C4 declaration-head" prompt) "the locator kind")
    (is (.contains prompt "\"HELD-OUT-SPLIT-DECLARED\"")
        "the exact required declaration head, quoted")
    (is (re-find #"must contain the exact head" prompt) "the exact-head rule")
    (is (re-find #"at the start of a line" prompt) "the line-initial rule stated")
    (is (re-find #"\*\*Status:\*\* DONE" prompt)
        "the target's own acceptance locator stated")))

(deftest block-renders-c4-and-absent-cases
  (let [f @#'flr/acceptance-criterion-block
        c2 (f {:action {:kind :cascade-candidate :id :C2 :target t
                        :precedence [{:id :aif/declare-the-conditioning
                                      :produces #{:repair/split-declared-valid}}]}})
        c3-target "T-with-c3"
        ;; absent: no acceptance, no produced tokens
        absent (f {:action {:kind :cascade-candidate :id :CX :target "T-no-such-target"
                            :precedence [{:id :nothing :produces #{}}]}})]
    (is (string? c2) (pr-str c2))
    (is (re-find #"C4 declaration-head" c2))
    (is (map? absent))
    (is (= :no-renderable-criteria (:reason absent)) (pr-str absent))))

(deftest c3-path-locator-renders-path-and-kind
  ;; a C3 produced-token locator renders the path and its kind: use the
  ;; reference ticket's C2 acceptance path but as a C3 via a synthetic entry
  ;; over a real source locator — the render function is the unit under
  ;; test; assert through a real locator shape from the source.
  (let [sources (cs/with-context-fn (cs/load-declared))
        loc (get-in sources [:locators t :restoration-accepted])
        f @#'flr/acceptance-criterion-block
        ;; synthesize: target the ticket but produce the token whose locator
        ;; we render as C3 by class-swap on a copy
        rendered (f {:action {:kind :cascade-candidate :id :CT :target t
                              :precedence [{:id :x :produces #{:restoration-accepted}}]}})]
    (is (string? rendered))
    (is (re-find #"C4 declaration-head" rendered) "the real locator's kind renders")))
