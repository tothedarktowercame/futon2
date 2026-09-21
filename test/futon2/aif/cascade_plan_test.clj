(ns futon2.aif.cascade-plan-test
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.cascade-plan :as plan]))

(def construction
  ;; Cut from attempt-001/003-construction.edn in run 1789964661.
  (edn/read-string (slurp "test/fixtures/narrative-trace/construction-1789964661.edn")))

(defn prompts [construction]
  [(#'runner/author-prompt {:author "author" :reviewer "reviewer"}
                          (:mission construction) {:id (:mission construction)} construction [])
   (#'runner/reviewer-prompt {:author "author" :reviewer "reviewer"}
                            (:mission construction) construction "/repo" "commit" {} [])])

(deftest recorded-cascade-reaches-both-prompts
  (let [receipt (get-in construction [:wiring :boxes 0 :interpretation-receipt])]
    (doseq [prompt (prompts construction)]
      (is (str/includes? prompt (:reading receipt)))
      (is (str/includes? prompt (get-in receipt [:source :sha256]))))))


(deftest recorded-plan-preserves-evidence-and-order
  (let [rendered (plan/cascade-plan-text construction)
        receipt (get-in construction [:wiring :boxes 0 :interpretation-receipt])]
    (doseq [value [":apparatus/one-authority-per-question"
                   (get-in receipt [:source :path]) (get-in receipt [:source :sha256])
                   (:reading receipt) (:scope-limit receipt) (:observation-limit receipt)
                   "admission/task-stated expected true: established"
                   "hole/h6378c65a4012 expected false: established"
                   "resolved sha cb2045b8279853fbd48d6a07c1e2da952f2e338f"
                   "resolved sha 2ea86caf9c61a00f34b971f0e17710edfc7b11e6"
                   "produces: M-aif-policy-conditioned-eig / :hole/h6378c65a4012"
                   "holes: none" "wires: none"]]
      (is (str/includes? rendered value) value))
    (is (= 2 (count (re-seq #"; check :C4" rendered))))
    ;; The C4 :decl is the searched-for line: "- [x]" at 2ea86caf was looked
    ;; for and not found (the file has "- [ ]" there), so it must not read as
    ;; evidence of what was found.
    (is (str/includes? rendered "observed false; resolved sha 2ea86caf9c61a00f34b971f0e17710edfc7b11e6; looked for line - [x]"))
    (is (not (re-find #"evidence :decl" rendered)))
    (is (= rendered (plan/cascade-plan-text construction)))
    (doseq [prompt (prompts construction)]
      (is (str/includes? prompt (str "PATTERN CASCADE:\n" rendered))))
    (is (str/includes? (first (prompts construction))
                       "Name in your reply which pattern(s) your change enacts."))))

(deftest twenty-patterns-have-an-explicit-size-bound
  (let [patterns (mapv #(hash-map :id (keyword (str "pattern-" %))
                                 :produces #{:z :a}) (range 20))
        receipts (into {} (map (fn [{:keys [id]}]
                                [id {:reading (apply str (repeat 10000 "x"))}]) patterns))
        c {:precedence patterns :interpretation-receipts receipts
           :wiring {:boxes [] :wires []}}
        rendered (plan/cascade-plan-text c)]
    (is (<= (count rendered) plan/max-plan-chars))
    (is (= 20 (count (re-seq #"\[truncated [0-9]+ chars\]" rendered))))
    (is (= (mapv #(str "pattern " (inc %) ": :pattern-" %) (range 20))
           (re-seq #"pattern [0-9]+: :pattern-[0-9]+" rendered)))
    (is (= rendered (plan/cascade-plan-text c)))
    (is (str/includes? rendered "wires: none"))))

(deftest holes-wires-and-unestablished-guards-are-not-hidden
  (let [c {:precedence [{:id :second} {:id :first}]
           :wiring {:boxes [{:id ":first" :produces #{:z :a}}]
                    :policy-holes
                    [{:unfolded-pattern ":second"
                      :construction-blockers [:guard-not-established]
                      :guard-evidence [{:status :unchecked
                                        :witness {:token [:mission :condition]
                                                  :expected false
                                                  :observation {:check :C6 :observed false}}}]}]
                    :wires [{:from ":first" :to ":second" :token [:mission :condition]}]}}
        rendered (plan/cascade-plan-text c)]
    (is (< (str/index-of rendered "pattern 1: :second")
           (str/index-of rendered "pattern 2: :first")))
    (is (str/includes? rendered "expected false: not established"))
    (is (str/includes? rendered "check :C6"))
    (is (str/includes? rendered "resolved sha not recorded"))
    (is (str/includes? rendered "construction-blockers = :guard-not-established"))
    (is (str/includes? rendered "wires:\n- :from = :first; :to = :second"))
    (is (str/includes? rendered "produces: :a, :z"))
    (is (= rendered (plan/cascade-plan-text
                    (assoc-in c [:wiring :wires 0]
                              (array-map :token [:mission :condition]
                                         :to ":second" :from ":first")))))))
