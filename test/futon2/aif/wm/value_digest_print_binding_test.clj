(ns futon2.aif.wm.value-digest-print-binding-test
  "R18: a seal is a function of the value alone. Before the binding inside
  `value-digest`, a carrier sealed under one ambient `*print-namespace-maps*`
  read as tampered under the other: the 10-22..24 click terminals (sealed in
  the futon3c server, root value false) failed `intact?` in every `clojure -M`
  process, and the 10-25 terminals (sealed with it true) fail in the server."
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.previous-run :as previous-run]))

(def namespaced-value
  "A map with a submap whose keys share one namespace: the shape
  `*print-namespace-maps*` prints two ways (`#:wm{...}` vs `{:wm/...}`).
  The terminal's `:valence-carry` is such a value."
  {:schema :wm/q6-terminal-v1
   :valence-carry {:wm/gamma-applied false :wm/update :not-performed}
   :rows (vec (range 40))
   :deep {:a {:b {:c {:d 1}}}}})

(defn- seal-under [v bindings]
  (with-bindings bindings (previous-run/seal v)))

(defn- intact-under? [v bindings]
  (with-bindings bindings (previous-run/intact? v)))

(def ^:private server-printer {#'*print-namespace-maps* false})
(def ^:private main-printer {#'*print-namespace-maps* true})
(def ^:private repl-printer {#'*print-namespace-maps* true #'*print-length* 3
                             #'*print-level* 2 #'*print-meta* true})

(deftest seal-does-not-depend-on-the-ambient-printer
  (testing "the bad case: sealed under one printer, verified under another"
    (let [sealed-in-server (seal-under namespaced-value server-printer)
          sealed-in-main (seal-under namespaced-value main-printer)
          sealed-at-repl (seal-under namespaced-value repl-printer)]
      (is (= (:sha256 sealed-in-server) (:sha256 sealed-in-main) (:sha256 sealed-at-repl)))
      (is (intact-under? sealed-in-server main-printer))
      (is (intact-under? sealed-in-main server-printer))
      (is (intact-under? sealed-in-server repl-printer))))
  (testing "a changed value still reads as tampered"
    (let [sealed (seal-under namespaced-value main-printer)]
      (is (not (intact-under? (assoc-in sealed [:valence-carry :wm/gamma-applied] true)
                              main-printer)))
      (is (not (intact-under? (assoc-in sealed [:valence-carry :wm/gamma-applied] true)
                              server-printer))))))

(deftest live-stream-terminal-verifies-under-both-printers
  ;; Two live pins, captured verbatim from data/wm-runs/run-stream.edn on
  ;; 2026-10-26: the first closed terminal sealed with the var true (run
  ;; 2026-10-25-6aba0b1d, a clojure.main process), which is the predecessor
  ;; the next click reads and read as tampered in the server before R18; and
  ;; the attempt-002 terminal (2026-10-24-25e24e8b), sealed with it false,
  ;; which stays a failing control: its stored sha is not the value's digest
  ;; under the fixed printer.
  (let [fixture (fn [f] (edn/read-string (slurp (str "test/futon2/aif/wm/fixtures/" f))))
        t (fixture "q6-terminal-2026-10-25-6aba0b1d.edn")
        legacy (fixture "q6-terminal-2026-10-24-25e24e8b.edn")]
    (is (= :wm/q6-terminal-v1 (:schema t)))
    (is (intact-under? t server-printer))
    (is (intact-under? t main-printer))
    (is (= :wm/q6-terminal-v1 (:schema legacy)))
    (is (not (intact-under? legacy server-printer))
        "sealed under the server printer before R18: its sha is not the fixed digest")
    (is (not (intact-under? legacy main-printer)))))
