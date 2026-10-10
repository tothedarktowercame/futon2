(ns futon2.aif.cascade-revision-producer-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-revision :as revision]
            [futon2.aif.cascade-revision-producer :as producer]
            [futon2.aif.interpretation-construction :as construction]
            [futon2.aif.wm.cascade-decision :as cascade-decision]))

(def target "M-hand")
(def want :caller-converted)
(def library-root (.getCanonicalPath (io/file "test/fixtures/want-interp-library")))
(def blend-square
  (edn/read-string
   (slurp "test/fixtures/three-halves-square/publication-cadence.edn")))

(def sources
  {:universes {target {:sites-enumerated true :one-producer true
                       :caller-converted false}}
   :wants {target [want]}
   :locators {target {:sites-enumerated {:class :C3 :stated "sites enumerated"}
                      :one-producer {:class :C3 :stated "one producer"}
                      :caller-converted {:class :C3 :stated "caller converted"}}}
   :interpretations {target {:patterns {} :receipts {}}}
   :horizon-steps 4
   :beta-by-context {:revision {:beta 1}}
   :context-of (constantly :revision)
   :construction {:construct construction/construct
                  :budget {:max-moves 4 :max-expansions 20000}
                  :move-cost 0
                  :evaluate-g cascade-decision/constructed-candidate-g}})

(def response
  {:schema :wm/want-interpretation-response-v1
   :pattern :gauntlet/placenta-transfer
   :guard {:needs #{:sites-enumerated :one-producer} :forbids #{}}
   :produces #{:caller-converted}
   :receipt
   {:source {:path "futon3/library/gauntlet/placenta-transfer.flexiarg"
             :sha256 "9771eca50e93c42de6b1ea22e188c770635d62830ca190f5ae4ca18056a069cd"}
    :reading "Transfer the caller-conversion function from the operator."
    :scope-limit "Only the identified caller conversion transfers."
    :by "codex-test"
    :blend-square blend-square}})

(def context
  {:target target
   :original {:kind :cascade-candidate :id :C0 :target target
              :precedence [] :construction-receipt {:reached-wants []}}
   :blocker {:schema :wm/mid-run-cascade-blocker-v1 :status :present
             :target target :stage :review :kind :stuck}
   :whole-mission {:schema :wm/whole-mission-context-v1 :target target
                   :scope :whole-mission
                   :path "/pinned/M-hand.md" :sha256 (apply str (repeat 64 "a"))
                   :content "# M-hand\n\nThe whole mission."}
   :evidence-root "/tmp/not-used-by-injected-request"})

(defn- proposals [reply]
  ((producer/make-proposals-fn
    {:sources-fn (fn [_ _] sources)
     :request-fn (fn [{:keys [target want]}]
                   {:schema :wm/want-interpretation-request-v1
                    :target target :want {:token want}})
     :code-root library-root})
   (assoc context :ask! (constantly reply))))

(deftest selected-judgment-reconstitutes-the-production-construction-view
  (let [row {:target target
             :cascade-problem {:facts (get-in sources [:universes target])
                               :want (get-in sources [:wants target])
                               :locators (get-in sources [:locators target])
                               :interpretations {}
                               :horizon-steps 4 :beta 1}
             :interpretation-receipts {}}
        view (producer/sources-from-judgment
              {:cascade-problems {:problems [row]}
               :construction-parameters
               {:budget {:value {:max-moves 4 :max-expansions 20000}}
                :move-cost {:value 0}}}
              target)]
    (is (= (get-in sources [:universes target])
           (get-in view [:universes target])))
    (is (= {:max-moves 4 :max-expansions 20000}
           (get-in view [:construction :budget])))
    (is (fn? (get-in view [:construction :evaluate-g])))))

(deftest raw-retrieval-hit-is-not-an-interpretation
  (let [result (proposals
                "```edn\n{:schema :wm/cascade-proposal-v1 :pattern :gauntlet/placenta-transfer}\n```")]
    (is (= :refused (:status result)))
    (is (= :raw-or-uninterpreted-revision-response (:kind result)))
    (is (nil? (:candidates result)))))

(deftest validated-reading-produces-an-admitted-executable-extension
  (let [result (proposals (str "```edn\n" (pr-str response) "\n```"))
        candidate (first (:candidates result))]
    (is (= :admitted (:status result)) (pr-str result))
    (is (= :cascade-candidate (:kind candidate)))
    (is (= :machine-constructed
           (get-in candidate [:construction-receipt :kind])))
    (is (= [:gauntlet/placenta-transfer]
           (mapv :id (:precedence candidate))))
    (is (= (:sha256 (:whole-mission context))
           (get-in result [:receipt :whole-mission-sha256])))
    (is (= (:source (:receipt response))
           (get-in result [:receipt :pattern-source])))
    (let [revised
          (revision/revise
           {:original (:original context)
            :head-context (revision/head-seed (:original context))
            :whole-context (dissoc (:whole-mission context) :content)
            :blocker (:blocker context)
            :proposals (:candidates result)})]
      (is (= :revised (:status revised)))
      (is (= [:gauntlet/placenta-transfer]
             (get-in revised [:delta :added]))))))
