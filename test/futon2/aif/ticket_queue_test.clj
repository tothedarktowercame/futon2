(ns futon2.aif.ticket-queue-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.policy :as policy]
            [futon2.aif.decision-gate :as gate]
            [futon2.aif.cascade-problems :as problems]
            [futon2.aif.locator-fixtures :as locators]
            [futon2.aif.live-c :as live-c]
            [futon2.aif.focus-receipt :as focus-receipt]
            [futon2.report.war-machine :as wm])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def declaration {:schema :wm/ticket-queue-v1 :placement :front
                  :order [:inserted-at :ticket] :within-stratum :cascade-selection-posterior :entries []})
(defn queue [& entries] (assoc declaration :entries (vec entries)))
(defn entry [ticket at] {:ticket ticket :inserted-at at})
(def early "2026-09-20T00:00:00Z")
(def late "2026-09-21T00:00:00Z")

(defn ranked [target id g]
  {:action {:kind :cascade-candidate :id id :target target
            :precedence [{:id id :target target :guard {:clauses [{:present #{} :absent #{}}]} :produces #{}}]
            :construction-receipt {:kind :fixture :id id}
            :interpretation-receipts {id {:kind :fixture}}}
   :controller-score g})

(def roster [(ranked "M-main" :p/mission 0) (ranked "T-documentation" :p/docs 8)])

(defn with-inputs [f]
  (let [root (.toFile (Files/createTempDirectory "ticket-queue-test-" (make-array FileAttribute 0)))]
    (try (f {:beta 1 :novelty-inputs {} :cascade-habit-path (str (io/file root "absent.edn"))})
         (finally (doseq [file (reverse (file-seq root))] (io/delete-file file true))))))

(defn select [ranked opts q]
  (policy/select-action-cascades ranked (assoc opts :ticket-queue q)))
(defn receipt [decision] (get-in decision [:selection-certificate :ticket-queue]))
(defn gate-reason [decision]
  (try (gate/emit! decision) nil
       (catch clojure.lang.ExceptionInfo e (:reason (ex-data e)))))

(deftest front-ticket-wins-with-full-family-evidence
  (with-inputs
    (fn [opts]
      (let [ordinary (select roster opts declaration)
            front (select roster opts (queue (entry "T-documentation" early)))
            r (receipt front)]
        (is (= "M-main" (get-in ordinary [:action :target])))
        (is (= "T-documentation" (get-in front [:action :target])))
        (is (= :ticket-queue (:decided-by r)))
        (is (= ["T-documentation"] (:eligible-targets r)))
        (is (= (:chosen-action ordinary) (get-in r [:unrestricted-choice :action])))
        (is (= (get-in ordinary [:selection-law :posterior]) (get-in front [:selection-law :posterior])))
        (is (= (get-in ordinary [:selection-law :action-marginal]) (get-in front [:selection-law :action-marginal])))
        (is (= (get-in ordinary [:selection-certificate :candidates]) (get-in front [:selection-certificate :candidates])))
        (is (= r (get-in front [:selection-law :ticket-queue])))
        (is (= :unrestricted (get-in front [:selection-law :action-comparison :selection-domain])))
        (is (= front (gate/emit! front)))
        (is (= :ticket-queue-choice-invalid
               (gate-reason (-> front
                                (assoc-in [:selection-certificate :ticket-queue :eligible-targets] ["M-main"])
                                (assoc-in [:selection-law :ticket-queue :eligible-targets] ["M-main"])))))))))

(deftest oldest-and-id-order-independent-of-input-order
  (with-inputs
    (fn [opts]
      (let [a (ranked "T-a" :p/a 9) b (ranked "T-b" :p/b 1)]
        (doseq [rows [[a b] [b a]]
                entries [[(entry "T-a" early) (entry "T-b" late)] [(entry "T-b" late) (entry "T-a" early)]]]
          (is (= "T-a" (get-in (select rows opts (apply queue entries)) [:action :target]))))
        (doseq [rows [[a b] [b a]]]
          (is (= "T-a" (get-in (select rows opts (queue (entry "T-b" early) (entry "T-a" early))) [:action :target]))))
        ;; Instants with different offsets represent the same insertion time.
        (is (= "T-a" (get-in (select [b a] opts (queue (entry "T-b" early)
                                                                       (entry "T-a" "2026-09-20T01:00:00+01:00"))) [:action :target])))))))

(deftest policy-law-still-decides-within-the-front-ticket
  (with-inputs
    (fn [opts]
      (let [rows [(ranked "M-main" :p/mission -100) (ranked "T-a" :p/slow 5) (ranked "T-a" :p/fast 1)]
            d (select rows opts (queue (entry "T-a" early)))]
        (is (= :p/fast (get-in d [:action :id])))
        (is (= 2 (count (:stratum-posterior (receipt d)))))
        (is (= d (gate/emit! d)))))))

(deftest finite-underflow-is-not-inadmission
  (with-inputs
    (fn [opts]
      (let [rows [(ranked "M-main" :p/mission 0) (ranked "T-a" :p/task 1000)]
            d (select rows opts (queue (entry "T-a" early)))]
        (is (= "T-a" (get-in d [:action :target])))
        (is (zero? (:chosen-action-mass d)))
        (is (let [mass (get-in (receipt d) [:choice :mass])] (and (number? mass) (pos? mass))))
        (is (= d (gate/emit! d)))))))

(deftest ordinary-refusal-does-not-block-admitted-candidates
  ;; A target whose relation cannot be resolved is classified :unknown
  ;; (focus-receipt/classify-target, "never guessed"), and the class
  ;; observation model then refuses :class-unknown-no-scalar-g rather than
  ;; scoring it -- codex-20's ruling, carried in the comment at
  ;; observation_model.clj:210-221: "a target whose relation genuinely cannot
  ;; be resolved gets NO scalar G -- no stop-the-line scoring, no worst case,
  ;; no averaging, no uniform, no exclusion." So this test's synthetic M-main
  ;; injects its own relation row through the :focus-inputs seam (the same
  ;; seam cascade_decision_test uses for its synthetic targets), keeping the
  ;; real :windows so the decision time is inside a discovery window.
  ;;
  ;; On the record, and not this test's to settle: the production corpus
  ;; resources/wm/focus/commit-facets-v1.json carries nine relation rows, and
  ;; M-autoclock-in -- the first flight's target -- is not one of them, so a
  ;; click on it that reaches scoring refuses here too (FAILING-TESTS-D,
  ;; futon2 3e9b1e76). Whether it gets a row is the focus receipt owner's
  ;; judgment about that mission, not a fixture edit.
  (with-inputs
    (fn [opts]
      (let [sources (locators/locate-all
                     {:universes {"M-main" {:done false}}
                      :wants {"M-main" [:done]}
                      :interpretations {"M-main" {:patterns {:p/do {:guard {:needs #{} :forbids #{:done}} :produces #{:done}}}
                                                   :receipts {:p/do {:kind :fixture}}}}
                      :candidates {"M-main" [{:precedence [:p/do] :construction-receipt {:kind :fixture}}]}
                      :horizon-steps 2 :beta-by-context {:WM {:beta 1}} :context-of (constantly :WM)})
            assembled (problems/assemble {:targets ["T-missing" "M-main"] :sources sources})
            q (queue (entry "T-missing" early))
            ;; noon, not `early`: `early` is the discovery window's own start
            ;; instant, and discover credits only commits at-or-before the
            ;; decision time, so at the window's first moment no focus is
            ;; established and every target classifies :unknown. The queue's
            ;; own ordering uses the entries' :inserted-at, not this.
            opts (merge opts {:ticket-queue q :focus-as-of "2026-09-20T12:00:00Z"
                              :focus-inputs (assoc (focus-receipt/read-inputs)
                                                   :relations
                                                   [{:target "M-main" :facet "WM" :relation "focus"
                                                     :source {:repo "fixture" :commit "0"
                                                              :path "test" :section "fixture"}
                                                     :effective-from "2026-01-01T00:00:00Z"}])
                              :live-c {:sources {} :sources-now {}
                                       :derived {:want #{} :weights {} :lam 1 :entries [] :gaps [] :refusals nil :signature (live-c/signature-of {})}}})
            result (wm/cascade-decision assembled opts)
            d (:decision result)]
        (is (= "M-main" (get-in d [:action :target])))
        (is (= :universe-not-admitted (get-in assembled [:refusals 0 :kind])))
        (is (= :not-admitted (get-in (receipt d) [:entries 0 :status])))
        (is (= :universe-not-admitted (get-in (receipt d) [:entries 0 :refusals 0 :kind])))
        (is (some #(= "T-missing" (:target %)) (:dropped-candidates result)))
        (let [all-refused (problems/assemble {:targets ["T-missing"] :sources {:horizon-steps 2}})
              r (wm/cascade-decision all-refused opts)]
          (is (= :abstained (get-in r [:decision :status])))
          (is (= :no-admitted-front-entry (get-in r [:decision :selection-certificate :ticket-queue :status]))))))))

(deftest no-entries-preserves-frozen-decision-bytes
  (with-inputs
    (fn [opts]
      (let [expected (edn/read-string (slurp (io/resource "fixtures/ticket-queue/before.edn")))]
        (is (= expected (pr-str (policy/select-action-cascades roster opts))))
        (is (= expected (pr-str (select roster opts declaration))))))))

(deftest invalid-declaration-refuses-instead-of-defaulting
  (with-inputs
    (fn [opts]
      (doseq [q [nil (queue (entry "M-main" early))
                 (queue (entry "T-a" "yesterday"))
                 (queue (entry "T-a" early) (entry "T-a" late))
                 (assoc declaration :placement :back)]]
        (is (= :invalid-ticket-queue
               (try (select roster opts q) nil
                    (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))))))

(deftest queue-consumer-has-no-origin-specific-input
  (let [source (some-> (io/resource "futon2/aif/ticket_queue.clj") slurp)]
    (is (some? source))
    (is (nil? (re-find #":repair/id|T-repair-|repair[-_]proposals" (or source "")))))
  (with-inputs
    (fn [opts]
      (is (= :invalid-ticket-queue
             (try (select roster opts (queue (assoc (entry "T-documentation" early) :origin :arbitrary))) nil
                  (catch clojure.lang.ExceptionInfo e (:kind (ex-data e)))))))))
