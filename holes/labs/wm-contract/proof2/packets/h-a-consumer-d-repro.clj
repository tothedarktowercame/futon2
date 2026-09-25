;; H-A-CONSUMER-D reproduction (PROOF-2a hole H-A, consumer side).
;; DISCOVERY ONLY: read-only. No clicks, no writes, no load-file into a shared
;; JVM. Runs in its own JVM from the futon2 checkout:
;;
;;   cd /home/joe/code/futon2
;;   clojure -M holes/labs/wm-contract/proof2/packets/h-a-consumer-d-repro.clj
;;
;; Three experiments, each printing what it observed rather than asserting it.
;;
;; E1. THE DISCARD. futon2.aif.observation-rates/sourced-rates
;;     (src/futon2/aif/observation_rates.clj:154) is handed labels that carry
;;     measured error rates for class :C4, and then the same call with nil
;;     labels. If the two results are equal, the labels reached no rate: the
;;     branch at observation_rates.clj:140 in token-likelihood-rates
;;
;;         (if (= :checkable (:kind cls))
;;           (assoc acc token {:false-neg 0 :false-pos 0 :basis :checkable})
;;           ...)
;;
;;     returns the exact zero kernel for every class the production contract
;;     declares (:C3 :C4 :C5 :C6 are all :kind :checkable) BEFORE it consults
;;     the rates argument. rates-by-class is printed on the same labels to show
;;     the labels were well formed and the producer did compute rates from them.
;;
;; E2. WHAT THE LEDGER MEASURED. The 22 rows of the A-S exemplar ledger
;;     (futon3c/holes/labs/M-futon-seams/exemplar/check-ledger.edn) are listed
;;     by kind with the :check each row ran, beside the four check functions the
;;     observation contract names for its classes (:checks-implemented). The
;;     question the packet has to answer is whether any ledger row is a run of a
;;     C3-C6 class check.
;;
;; E3. WHETHER A NON-ZERO KERNEL WOULD MOVE G. The real cascade-lane is run
;;     twice on one minimal problem: once as production runs it, once with
;;     token-likelihood-rates redefined so :C4 tokens take a non-zero kernel
;;     (design (a) of the packet, simulated here only to read G; nothing is
;;     changed on disk). The injected numbers are A-S's measured :grep rates
;;     (fp 3/10, fn 1/6) used as an ILLUSTRATION of the arithmetic -- :grep is
;;     not a C4 measurement, see E2 and section 2 of the packet.

(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[clojure.pprint :as pp]
         '[futon2.aif.observation-rates :as observation-rates]
         '[futon2.report.war-machine :as wm])

(def contract
  (edn/read-string (slurp (io/resource "wm/observation-contract.edn"))))

(def ledger-path
  "/home/joe/code/futon3c/holes/labs/M-futon-seams/exemplar/check-ledger.edn")

(defn- line [c] (println (apply str (repeat 72 c))))
(defn- heading [s] (println) (line "=") (println s) (line "="))

;; ---------------------------------------------------------------- E1

(def tokens #{:t/observed :t/other :t/wanted})

(defn- locators [class]
  (into {} (map (fn [t] [t {:class class :repo "futon2" :sha "fixture"
                            :path (str "fixture/" (name t))}])) tokens))

;; Labels in exactly the shape rates-by-class documents: :token-class, the
;; RECORDED verdict, and the ADMITTED reference label. Four labels for :C4:
;; one established token reported, one established token missed (a false
;; negative), one absent token correctly not reported, one absent token
;; reported (a false positive). So fn = 1/2 and fp = 1/2 -- rates no reader
;; could mistake for zero.
(def measured-labels
  [{:token-class :C4 :admitted :present :recorded true}
   {:token-class :C4 :admitted :present :recorded false}
   {:token-class :C4 :admitted :absent  :recorded false}
   {:token-class :C4 :admitted :absent  :recorded true}])

(def measured-subjects {:C4 4})

(defn e1 []
  (heading "E1. sourced-rates with measured labels vs nil labels")
  (println "\nrates-by-class over the same labels (the PRODUCER):")
  (pp/pprint (observation-rates/rates-by-class measured-labels measured-subjects))
  (let [with-labels (observation-rates/sourced-rates
                     measured-labels measured-subjects nil (locators :C4) contract)
        without (observation-rates/sourced-rates
                 nil nil nil (locators :C4) contract)]
    (println "\nsourced-rates WITH measured labels:")
    (pp/pprint with-labels)
    (println "\nsourced-rates WITH nil labels:")
    (pp/pprint without)
    (println "\nequal?" (= with-labels without))
    (println "printed forms equal?" (= (pr-str with-labels) (pr-str without)))
    (println "every token :basis :checkable?"
             (every? #(= :checkable %) (vals (:basis with-labels))))
    (println "\nclasses the contract declares, with their :kind:")
    (doseq [c (:classes contract)]
      (println " " (:id c) (:kind c)))))

;; ---------------------------------------------------------------- E2

(defn e2 []
  (heading "E2. What the A-S exemplar ledger measured")
  (let [ledger (edn/read-string (slurp ledger-path))
        rows (:rows ledger)
        class-checks (get-in contract [:production-path :checks-implemented])]
    (println "\nledger:" ledger-path)
    (println "schema:" (:schema ledger) " rows:" (count rows))
    (println "\ncontract classes and the check function each names:")
    (doseq [[k v] (sort-by key class-checks)] (println " " k "->" v))
    (println "\nledger rows by kind, with the check each row ran:")
    (doseq [[kind kind-rows] (sort-by (comp str key) (group-by :kind rows))]
      (println " " kind (str "(" (count kind-rows) " rows, "
                             (reduce + (map #(max 1 (or (:count %) 1)) kind-rows))
                             " runs)"))
      (doseq [r kind-rows]
        (println "   " (:id r) "|" (:check r)
                 (if (:error r) (str "| " (:error r)) ""))))
    (println "\nrows whose :check names one of the contract's check functions:")
    (let [fn-names (set (map (comp name str) (vals class-checks)))
          hits (filter (fn [r] (some #(re-find (re-pattern (str "(?i)" %)) (str (:check r)))
                                     fn-names))
                       rows)]
      (println "  check-fn names looked for:" (vec (sort fn-names)))
      (println "  matching rows:" (if (seq hits) (mapv :id hits) "NONE")))))

;; ---------------------------------------------------------------- E3

;; One minimal, complete cascade-lane problem (the shape used by
;; test/futon2/aif/wire5_sourced_rates_test.clj): one true fact, one want, one
;; pattern whose guard needs the fact and produces the want, one candidate
;; order, T=2, beta=1. The locators' repo/sha/path are fixture strings: R5 uses
;; only their :class, so no git IO happens.
(defn- problem [& {:as overrides}]
  (merge {:facts {:t/observed true :t/other false}
          :want [:t/wanted]
          :interpretations {:p/appears {:guard {:needs #{:t/observed} :forbids #{}}
                                        :produces #{:t/wanted}}}
          :repository {:patterns #{:p/appears} :stands-on #{}}
          :precedences [[:p/appears]]
          :horizon-steps 2
          :cascade-spec {:want #{:t/wanted}}
          :beta 1}
         overrides))

(def measured-grep-kernel
  "A-S section 3's measured :grep rates as exact rationals:
   fp = (1 + 1/2)/(4 + 1) = 3/10, fn = (0 + 1/2)/(2 + 1) = 1/6."
  {:false-neg 1/6 :false-pos 3/10})

(defn- ranked-g [lane]
  (mapv (fn [e] [(:cascade-id e) (:G-efe e)]) (:ranked lane)))

(defn e3 []
  (heading "E3. Would a non-zero kernel for a checkable class move G?")
  (let [p (problem :locators (locators :C4))
        production (wm/cascade-lane p)
        ;; design (a), simulated: a class with a measured rate takes it; the
        ;; zero kernel stays only where nothing was measured.
        design-a (with-redefs [observation-rates/token-likelihood-rates
                               (fn [_rates _contract token-classes]
                                 (into {} (map (fn [[t c]]
                                                 [t (if (= :C4 c)
                                                      (assoc measured-grep-kernel :basis :measured)
                                                      {:false-neg 0 :false-pos 0 :basis :checkable})]))
                                       token-classes))]
                    (wm/cascade-lane p))]
    (println "\nproduction (checkable zero kernel):")
    (println "  stopped-at:" (:stopped-at production) " refusal:" (:refusal production))
    (println "  ranked [cascade-id G-efe]:" (ranked-g production))
    (println "\ndesign (a) simulated (:C4 takes" (pr-str measured-grep-kernel) "):")
    (println "  stopped-at:" (:stopped-at design-a) " refusal:" (:refusal design-a))
    (println "  ranked [cascade-id G-efe]:" (ranked-g design-a))
    (println "\nG equal?" (= (ranked-g production) (ranked-g design-a)))
    (let [scoring-of #(:cascade-scoring (meta (:ranked %)))]
      (println "\nfamily meta :rates of each run:")
      (println "  production:" (:rates (scoring-of production)))
      (println "  design (a):" (:rates (scoring-of design-a)))
      (println "\nrates actually scored, production:")
      (pp/pprint (get-in (scoring-of production) [:precision-model :rates]))
      (println "rates actually scored, design (a):")
      (pp/pprint (get-in (scoring-of design-a) [:precision-model :rates])))))

(e1)
(e2)
(e3)
(println)
(line "=")
(println "read-only: no click, no store write, no shared-JVM load.")
(line "=")
(shutdown-agents)
