(ns futon2.aif.flight-test
  "A flight: target fixed across clicks, stop-lines first, wants carried,
  closure, and a click that advances nothing ends the flight. The judge's
  side (construction-inputs/flight-assembly-input) restricts
  assembly to the flight's target and wants."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.flight :as flight]
            [futon2.aif.repair-obligation :as repair]
            [futon2.aif.repair-proposals :as repair-proposals]
            [futon2.aif.wm.construction-inputs :as construction-inputs])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def target "M-test")
(def wants [:a :b :c])

(def sources {:wants {target wants "M-other" [:z]}})

(defn- new-flight [& [ws]]
  (flight/start (flight/choose-target {:requested target})
                (or ws {:kind :checkbox}) {:id "flight-t"}))

(defn- with-repair-store [f]
  (let [root (.toFile (Files/createTempDirectory "flight-repair-supply-"
                                                  (make-array FileAttribute 0)))]
    (try
      (f (str root))
      (finally
        (doseq [file (reverse (file-seq root))]
          (Files/delete (.toPath file)))))))

(defn- finding! [root id opened-at & [changes]]
  (repair/record-system-failure!
   root
   (merge {:attempt-id (str "attempt-" id)
           :repair-id id
           :repair-class :machine-failure
           :target "M-original"
           :failure-stage :construction
           :outcome :failed
           :failure-kind :fold-output-invalid
           :error "Constructor supplied no folded coverage"
           :backtrace {:error-class "clojure.lang.ExceptionInfo"
                       :code-state {:repo "futon2"}}
           :opened-at opened-at
           :discharge-contract repair/review-failure-discharge-contract}
          changes)))

(defn- record! [root directory record]
  (let [file (io/file root directory (str (:repair/id record) ".edn"))]
    (io/make-parents file)
    (spit file (pr-str record))
    record))

(deftest lifecycle-qualified-repair-supply-chooses-only-concrete-actionable-proposals
  (with-repair-store
    (fn [root]
      ;; The canonical lifecycle/proposal readers see all three findings.  The
      ;; two older records are retained as evidence, but only NEW is a concrete
      ;; actionable proposal eligible for flight priority.
      (finding! root "old-awaiting" "2026-10-19T01:00:00Z")
      (record! root "implementations"
               {:repair/id "old-awaiting" :repair/status :awaiting-validation
                :implemented-at "2026-10-19T01:01:00Z"})
      (finding! root "old-blocked" "2026-10-19T02:00:00Z"
                {:discharge-contract nil})
      (finding! root "new-concrete" "2026-10-19T03:00:00Z")
      (let [supplied (repair-proposals/supply root)
            chosen (flight/choose-target {:requested target
                                           :repair-proposal-supply supplied})
            evidence (get-in chosen [:chosen-because :repair-proposal-supply])]
        (is (= "T-new-concrete" (:target chosen)))
        (is (= :actionable-repair-proposal (get-in chosen [:chosen-because :kind])))
        (is (= "new-concrete" (get-in chosen [:chosen-because :repair/id])))
        (is (= supplied evidence))
        (is (= ["old-awaiting"]
               (get-in evidence [:repair-scan :lifecycle-census
                                 :awaiting-validation :ids])))
        (is (= ["old-blocked"]
               (get-in evidence [:repair-scan :blocked-ids])))
        (is (= :repair-finding-evidence-missing
               (get-in evidence [:blocked 0 :reason])))
        (is (= ["new-concrete"]
               (get-in evidence [:repair-scan :proposal-ids])))))))

(deftest no-concrete-repair-proposal-falls-back-to-requested-with-supply-evidence
  (with-repair-store
    (fn [root]
      (finding! root "awaiting" "2026-10-19T01:00:00Z")
      (record! root "implementations"
               {:repair/id "awaiting" :repair/status :awaiting-validation
                :implemented-at "2026-10-19T01:01:00Z"})
      (finding! root "blocked" "2026-10-19T02:00:00Z" {:discharge-contract nil})
      (let [supplied (repair-proposals/supply root)
            chosen (flight/choose-target {:requested target
                                           :repair-proposal-supply supplied})]
        (is (empty? (:proposals supplied)))
        (is (= target (:target chosen)))
        (is (= :requested (get-in chosen [:chosen-because :kind])))
        (is (= supplied (get-in chosen [:chosen-because :repair-proposal-supply])))))))

(deftest legacy-open-obligations-do-not-supply-executable-repair-authority
  (is (= target
         (:target (flight/choose-target
                   {:requested target
                    :open-obligations [{:repair/id "old-stop-line"
                                        :opened-at "2026-10-19T01:00:00Z"}]}))))
  (is (nil? (flight/choose-target {}))))

(defn- world
  "A target whose facts change as clicks land: STEPS is a vector of the
  facts after each click. Returns click-fn / observe-fn over an atom, and
  records every judge-opts the flight passed."
  [initial steps & [unreached]]
  (let [facts (atom initial) n (atom 0) seen (atom [])]
    {:seen seen
     :click-fn (fn [opts]
                 (swap! seen conj opts)
                 (let [i @n]
                   (swap! n inc)
                   (when (< i (count steps)) (reset! facts (nth steps i)))
                   {:click-id (str "click-" (inc i))
                    :unreached-wants (get unreached i [])}))
     :observe-fn (fn [_ _] @facts)
     :sources-fn (constantly sources)}))

(deftest a-flight-closes-its-target
  (let [w (world {:a false :b false :c false}
                 [{:a true :b false :c false}
                  {:a true :b true :c true}])
        f (flight/run! (new-flight) (assoc w :max-clicks 5))]
    (is (= :closed (:status f)))
    (is (= [[:a] [:b :c]] (mapv :advanced (:clicks f))))
    (testing "the target never changes across clicks"
      (is (= #{target} (set (map #(get-in % [:flight :target]) @(:seen w))))))
    (testing "the judge is told the flight's wants"
      (is (= wants (get-in (first @(:seen w)) [:flight :wants]))))))

(deftest a-click-that-advances-nothing-ends-the-flight
  ;; the bad case rule 2 names: a click with no progress must not be
  ;; followed by more clicks
  (let [w (world {:a false :b false :c false}
                 [{:a true :b false :c false}
                  {:a true :b false :c false}
                  {:a true :b true :c true}])
        f (flight/run! (new-flight) (assoc w :max-clicks 5))]
    (is (= :no-progress (:status f)))
    (is (= 2 (count (:clicks f))))
    (is (= [true false] (mapv :progress? (:clicks f))))))

(deftest unreached-wants-are-carried-into-the-next-click
  ;; the want source drops :d (not in the mission's list), but click 1's
  ;; plan left it unreached, so click 2 still asks for it
  (let [w (world {:a false :b false :c false}
                 [{:a true :b false :c false} {:a true :b true :c true :d true}]
                 [[{:token :d :reason :beyond-horizon}]])
        f (flight/run! (new-flight) (assoc w :max-clicks 5))]
    (is (= [:a :b :c :d] (get-in (second @(:seen w)) [:flight :wants])))
    (is (= [:d] (:carried-wants f)))
    (is (= :closed (:status f)))))

(deftest abstention-missing-input-is-recorded-as-a-need
  (let [f (flight/record-click (new-flight)
                               {:click-id "c1" :wants wants
                                :before {:a false} :after {:a true}
                                :abstention {:kind :no-admitted-interpretation
                                             :missing :interpretations}})]
    (is (= [{:click-id "c1" :kind :no-admitted-interpretation :missing :interpretations}]
           (:needs f)))))

(deftest click-limit-is-not-closure
  (let [w (world {:a false :b false :c false}
                 [{:a true :b false :c false} {:a true :b true :c false}])
        f (flight/run! (new-flight) (assoc w :max-clicks 2))]
    (is (= :click-limit (:status f)))
    (is (= [:c] (:open-after (last (:clicks f)))))))

(deftest operator-declared-wants-are-typed
  (let [f (new-flight {:kind :operator-declared :wants [:x] :declared-by "test"})
        {:keys [wants source]} (flight/click-wants f sources)]
    (is (= [:x] wants))
    (is (= :operator-declared (:kind source)))))

(deftest judge-assembles-only-the-flight-target
  (let [input {:targets [target "M-other" "T-occ-z"]
               :sources {:wants {target [:a] "M-other" [:z]} :horizon-steps 2}}
        f {:target target :wants [:a :b :c :d]}
        out (construction-inputs/flight-assembly-input f input)]
    (is (= [target] (:targets out)))
    (is (= [:a :b :c :d] (get-in out [:sources :wants target])))
    (is (= input (construction-inputs/flight-assembly-input nil input)) "no flight: unchanged")))

(deftest a-flight-target-the-sources-never-declared-gets-a-context
  ;; bad case: M-futon-seams has no hand source and no checkbox, so the
  ;; tick's :context-of knows nothing of it and assembly refused it
  ;; :beta-not-declared
  (let [input {:targets ["M-other"] :sources {:context-of (fn [t] (when (= t "M-other") :OTHER))}}
        out (construction-inputs/flight-assembly-input {:target "M-seams" :wants []} input)
        cf (get-in out [:sources :context-of])]
    (is (= :WM (cf "M-seams")))
    (is (= :OTHER (cf "M-other")) "a context the sources give still wins")
    (is (= :X ((get-in (construction-inputs/flight-assembly-input {:target "M-seams" :wants [] :context :X} input)
                       [:sources :context-of]) "M-seams")))))
