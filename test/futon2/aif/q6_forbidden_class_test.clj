(ns futon2.aif.q6-forbidden-class-test
  "Ruling 3 on real policies: a refused policy stays compared, and its prediction
  class cannot be chosen (Requirements Q6, Q11)."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.policy :as policy]
            [futon2.aif.previous-run :as previous-run]))

;; Run e71c7380's five candidates, verbatim subset. It enacted policy-e77a by the
;; action marginal and was refused; d5b9b513 then excluded that key and chose
;; policy-bd8f, a different key in the same prediction class at the same G.
(def recorded
  (edn/read-string {:default tagged-literal}
                   (slurp "test/fixtures/wm-run-facts/e71c7380-candidates.edn")))

(def ranked (:ranked recorded))
(def rows (map selection/selection-row ranked))

(defn- short-id [id] (last (str/split (str id) #"/")))

(defn- key-of [suffix]
  (some #(when (str/ends-with? (:cascade-id %) suffix) (get-in % [:action :policy-key])) ranked))

(defn- forbidding [& keys]
  {:keys (vec keys) :classes (selection/forbidden-classes rows keys)})

(defn- select [forbidden]
  (policy/select-action-cascades ranked {:beta (:beta recorded) :forbidden forbidden}))

(defn- refusal-kind [forbidden]
  (try (select forbidden) nil
       (catch clojure.lang.ExceptionInfo e (get-in (ex-data e) [:refusal :kind]))))

(deftest the-refused-class-is-the-prediction-class
  (let [e77a (key-of "policy-e77a1ab8b601e8195b8e")
        {:keys [classes] :as forbidden} (forbidding e77a)]
    (is (= 1 (count classes)))
    (is (= 4 (count (filter #(selection/forbidden-row? forbidden %) rows)))
        "bd8f and its three variants are one policy with e77a")))

(deftest the-d5b9b513-shape-keeps-five-compared
  (let [d (select (forbidding (key-of "policy-e77a1ab8b601e8195b8e")))
        law (:selection-law d)]
    (is (= 5 (count (get-in d [:selection-certificate :candidates])) (count (:posterior law))))
    (is (= "policy-e0e104ea5c0875d5820b" (short-id (get-in d [:action :id]))))
    (is (nil? (get-in law [:policy-comparison :stepped-over]))
        "the refused class was not the minimum, so nothing was stepped over")))

(deftest same-class-different-key-cannot-be-chosen
  ;; bd8f is what d5b9b513 took. With its class forbidden and the minimum class also
  ;; forbidden, nothing admissible remains: the re-choice is refused at the selector.
  (is (= :no-admissible-class
         (refusal-kind (forbidding (key-of "policy-e77a1ab8b601e8195b8e")
                                   (key-of "policy-e0e104ea5c0875d5820b"))))))

(deftest stepping-over-the-minimum-is-recorded
  (let [d (select (forbidding (key-of "policy-e0e104ea5c0875d5820b")))
        comparison (get-in d [:selection-law :policy-comparison])]
    (is (not= "policy-e0e104ea5c0875d5820b" (short-id (get-in d [:action :id]))))
    (is (= :q6-forbidden (:decided-by comparison)) "the refusal left one class")
    (is (= (get-in d [:action :policy-key])
           (get-in comparison [:admissible-minimum :policy-key])))
    (is (= :q6-forbidden (get-in comparison [:stepped-over :reason])))
    (is (= 1.699638937845117 (get-in comparison [:stepped-over :g])))))

(deftest a-key-outside-the-menu-forbids-itself-alone
  (is (= "policy-e0e104ea5c0875d5820b"
         (short-id (get-in (select (forbidding [:not-compared])) [:action :id])))))

(defn- exclusion-of [keys]
  (with-redefs-fn {#'previous-run/decide-exclusion
                   (fn [_ _] {:status :excluded :reason :previous-choice-refused-on-same-inputs
                              :excluded-keys keys})}
    #(previous-run/exclusion {:status :present} ranked {} 1 "precision")))

(deftest the-carrier-says-what-happened
  (let [{:keys [carrier forbidden refusal]} (exclusion-of [(key-of "policy-e77a1ab8b601e8195b8e")])]
    (is (= :forbidden (:status carrier)))
    (is (= 5 (:menu carrier)))
    (is (= 1 (count (:forbidden-classes carrier))))
    (is (not (contains? carrier :removed)) "nothing was removed, so nothing says it was")
    (is (not (contains? carrier :excluded-keys)))
    (is (= (:forbidden-classes carrier) (:classes forbidden)))
    (is (nil? refusal))))

(deftest every-class-forbidden-is-the-existing-repeat-refusal
  (let [{:keys [carrier refusal]}
        (exclusion-of [(key-of "policy-e77a1ab8b601e8195b8e")
                       (key-of "policy-e0e104ea5c0875d5820b")])]
    (is (= carrier refusal))
    (is (= [:refused :repeat-choice-after-refusal true]
           ((juxt :status :kind :no-admissible-class) carrier)))
    (is (some? (:rearm carrier)))))

;; -- the history of refusals ------------------------------------------------------

;; The run-stream lines of four real runs on M-pattern-retrieval-calibration,
;; verbatim. e71c7380 refused, d5b9b513 changed, on one digest; f4b95abf refused,
;; c5590f95 changed, on another. Both refusals were of policy-e77a.
(def stream-fixture "test/fixtures/wm-run-facts/mprc-run-stream.edn")
(def third "2026-11-01-c5590f95-a22e-4f21-a5e2-2cf47f393803")
(def third-started "2026-11-01T03:13:19.143307492Z")

(defn- stream-dir []
  (let [dir (str (java.nio.file.Files/createTempDirectory
                  "q6-history-" (make-array java.nio.file.attribute.FileAttribute 0)))]
    (io/copy (io/file stream-fixture) (io/file dir previous-run/stream-name))
    dir))

(defn- history-of [predecessor]
  (previous-run/task-refusal-history predecessor #{"M-pattern-retrieval-calibration"}))

(defn- exclusion-at [predecessor digest entries]
  (with-redefs-fn {#'previous-run/selection-inputs-digest (constantly digest)
                   #'previous-run/decide-exclusion (fn [_ _] {:status :not-triggered :reason :test})}
    #(previous-run/exclusion predecessor entries {} 1 "precision")))

(deftest the-real-lineage-retains-both-refusals
  (let [history (history-of (previous-run/predecessor (stream-dir) third third-started))]
    (is (= ["2026-10-31-e71c7380-2fc2-41ac-b7f9-c226c27e395e"
            "2026-10-31-f4b95abf-6e0f-48d0-bf75-83d5662b43be"]
           (mapv :run-id history)) "oldest first, and the changed run is not a refusal")
    (is (= 2 (count (set (map :input-digest history)))) "on two digests")
    (is (= 1 (count (set (map :cascade history)))) "of one policy")))

(deftest only-refusals-on-these-inputs-forbid
  (let [predecessor (previous-run/predecessor (stream-dir) third third-started)
        [older latest] (history-of predecessor)
        current (exclusion-at predecessor (:input-digest latest) ranked)
        other (exclusion-at predecessor "another-digest" ranked)]
    (is (= 2 (count (get-in current [:carrier :refusal-history]))) "the history stays whole")
    (is (= [(:run-id latest)] (get-in current [:carrier :forbidden-runs])))
    (is (not= (:input-digest older) (:input-digest latest)))
    (is (= :forbidden (get-in current [:carrier :status])))
    (is (= 2 (count (get-in other [:carrier :refusal-history]))))
    (is (nil? (:forbidden other)) "a refusal on other inputs forbids nothing")))

(deftest the-chosen-is-the-admissible-minimum-on-the-real-lineage
  (let [predecessor (previous-run/predecessor (stream-dir) third third-started)
        latest (peek (history-of predecessor))
        {:keys [forbidden]} (exclusion-at predecessor (:input-digest latest) ranked)
        d (select forbidden)]
    (is (= "policy-e0e104ea5c0875d5820b" (short-id (get-in d [:action :id]))))
    (is (= 5 (count (get-in d [:selection-law :posterior]))))
    (is (= 4 (count (filter #(selection/forbidden-row? forbidden %) rows))))))

(defn- arm [id g digest]
  {:action {:kind :cascade-candidate :id id :target "M-task" :policy-key [:key id]
            :precedence [{:id id :target "M-task"}]}
   :cascade-id id :controller-score g
   :certificate {:f nil :steps [{:prediction {digest 1}}]}})

(def arms [(arm :a 1.0 :pa) (arm :b 1.5 :pb) (arm :c 2.0 :pc)])

(defn- refused-history [& ids]
  (previous-run/seal
   {:refusal-history
    {:entries (mapv (fn [id] {:cascade (previous-run/cascade-identity [:key id])
                              :input-digest "d" :run-id (name id) :target "M-task"})
                    ids)
     :unsealed 0}}))

(deftest ping-pong-does-not-return-to-the-first-refusal
  (let [{:keys [forbidden carrier]} (exclusion-at (refused-history :a :b) "d" arms)
        d (policy/select-action-cascades arms {:beta 1 :forbidden forbidden})]
    (is (= ["a" "b"] (:forbidden-runs carrier)))
    (is (= 2 (count (:forbidden-classes carrier))))
    (is (= :c (get-in d [:action :id])) "A refused then B refused does not free A")))

(deftest a-history-that-covers-every-class-leaves-nothing
  (let [{:keys [forbidden refusal]} (exclusion-at (refused-history :a :b :c) "d" arms)]
    (is (= [:refused true] ((juxt :status :no-admissible-class) refusal)))
    (is (= :no-admissible-class
           (try (policy/select-action-cascades arms {:beta 1 :forbidden forbidden})
                nil
                (catch clojure.lang.ExceptionInfo e (get-in (ex-data e) [:refusal :kind])))))))

(deftest another-task-s-refusal-is-not-this-task-s
  (let [other (previous-run/seal
               (update-in (dissoc (refused-history :a) :sha256)
                          [:refusal-history :entries 0] assoc :target "M-else"))
        {:keys [forbidden carrier]} (exclusion-at other "d" arms)]
    (is (nil? forbidden))
    (is (= [] (:refusal-history carrier)))))

(deftest a-tampered-terminal-is-counted-not-trusted
  (let [dir (stream-dir)
        path (io/file dir previous-run/stream-name)
        _ (spit path (str/replace (slurp path) "guardrail-refusal" "guardrail-rewritten"))
        {:keys [entries unsealed]} (:refusal-history (previous-run/predecessor dir third third-started))]
    (is (= [] entries))
    (is (= 2 unsealed))))

(deftest a-tampered-carrier-forbids-nothing
  (let [tampered (assoc-in (refused-history :a) [:refusal-history :unsealed] 9)
        {:keys [forbidden carrier]} (exclusion-at tampered "d" arms)]
    (is (nil? forbidden))
    (is (= [] (:refusal-history carrier)))))
