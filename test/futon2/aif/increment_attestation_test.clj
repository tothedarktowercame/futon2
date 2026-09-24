(ns futon2.aif.increment-attestation-test
  "PROOF-wm-works-2026-09-22 step ⟨1⟩7 part 2: the two missing halves of the
  run-ending attestation. A run whose work registered a warrant attests its
  increment and classifies to a facet class; a run that registered no
  warrant still refuses to attest — :unknown with
  :missing [:attested-increment]. The second case is the one that matters:
  an attestation that cannot be absent is not an attestation."
  (:require [babashka.http-client]
            [cheshire.core]
            [clojure.test :refer [deftest is]]
            [futon2.aif.action-identity :as identity]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.increment-attestation :as attestation]
            [futon2.aif.route-attestation :as route]
            [futon2.aif.run-ending-classification :as kernel]))

(def declarations (attestation/declarations))
(def criterion (first (:criteria declarations)))
(def target (:target criterion))
(def warrant-id (str "test-registry-" (apply str (repeat 64 "b"))))

(def run-record
  {:schema "test-registry/v1" :kind :run :run/id "run-1"
   :author "wm-author" :warrant? true
   :repo/root "/home/joe/code/futon2"
   :command ["clojure" "-X:test" ":nses"
             "[futon2.aif.held-out-observations-test futon2.aif.held-out-calibration-test futon2.aif.repair-recheck-test]"]
   :ran-at "2026-09-23T20:00:00Z" :finished-at "2026-09-23T20:01:00Z"})

(defn entry [id record]
  {:evidence/id id :evidence/body {:payload-edn (pr-str record)}})

(def warrant-entry (entry warrant-id run-record))
(def since "2026-09-23T19:00:00Z")

(defn evidence-with [entries]
  (attestation/increment-evidence
   {:warrant-lookup-fn (fn [_ _] entries)}
   {:author "wm-author" :since since}))

(defn events [increment]
  {:dispatch {:event/sequence 4 :recorded-at "2026-09-23T20:02:00Z" :payload {:judgment {}}}
   :build (cond-> {:event/sequence 5 :recorded-at "2026-09-23T20:03:00Z"
                   :payload {:judgment {}}}
            increment (assoc-in [:payload :judgment :increment] increment))})

(def occurrence
  {:schema :wm/action-transition-occurrence-v2 :run/id "run" :cohort/id "cohort"
   :attempt/id "attempt-002" :transition/id "transition-00000000-0000-0000-0000-000000000001"
   :action/id "action-00000000-0000-0000-0000-000000000002"
   :action/value {:id :C1 :target target}
   :action/value-sha256 (identity/digest {:id :C1 :target target})
   :action-at "2026-09-23T20:04:00Z"})

(defn classify-close [route-receipt & {:keys [failure?]}]
  (kernel/classify
   {:close (cond-> {:outcome (if failure? :build-failed :grounded-change)
                    :grounded? (not failure?) :artifact-only? false
                    :occurrence occurrence :route-attestation route-receipt}
             failure? (assoc :failure-kind :compiler-refusal))
    :occurrence occurrence
    :route-attestation route-receipt
    :focus-receipt {:schema :wm/focus-receipt-v1 :mode :record-only
                    :candidates [{:target target :class :focus}]}}))

(deftest declarations-are-supplied-by-runner-config
  (is (= declarations (:route-attestation (runner/config {}))))
  (is (= :holds (get-in declarations [:institutions 0 :situation :status])))
  (is (= {:checkpoint :build :path [:increment]}
         (select-keys (first (:bindings declarations)) [:checkpoint :path]))))

(deftest a-run-with-a-registered-warrant-attests-and-classifies
  (let [evidence (evidence-with [warrant-entry])
        receipt (route/receipt {:declarations declarations :events (events evidence)
                                :target target})
        binding (first (:bindings receipt))
        result (classify-close receipt)]
    (is (= warrant-id (:warrant-id evidence)))
    (is (= (subs warrant-id (count "test-registry-")) (:sha256 evidence)))
    (is (= :matched (:status binding)))
    (is (= :present (get-in binding [:attestation :status])))
    (is (= 1 (count (:increments receipt))))
    (is (= :focus-increment (:class result)))
    (is (nil? (:missing result)))))

(deftest a-warrant-less-run-still-refuses-to-attest
  ;; The case that matters. No qualifying warrant — none registered, an
  ;; unwarranted run, a run over the wrong tests, a run before the attempt
  ;; window — so the evidence is ABSENT, never synthesised, and the class
  ;; stays honestly :unknown.
  (is (nil? (evidence-with [])))
  (is (nil? (evidence-with [(entry warrant-id (assoc run-record :warrant? false))])))
  (is (nil? (evidence-with [(entry warrant-id (assoc run-record :command ["make" "test"]))])))
  (is (nil? (evidence-with [(entry warrant-id (assoc run-record
                                                     :ran-at "2026-09-23T18:00:00Z"
                                                     :finished-at "2026-09-23T18:01:00Z"))])))
  (is (nil? (evidence-with [(entry warrant-id (assoc run-record :repo/root "/home/joe/code/other"))])))
  (let [receipt (route/receipt {:declarations declarations :events (events nil)
                                :target target})
        result (classify-close receipt)]
    (is (= :declared (:status receipt)))
    (is (= :checkpoint-evidence-missing (:reason (first (:bindings receipt)))))
    (is (empty? (:increments receipt)))
    (is (= :recorded (:status result)))
    (is (= :unknown (:class result)))
    (is (= [:attested-increment] (:missing result)))))

(deftest duplicate-criterion-refuses
  (let [doubled (update declarations :criteria conj criterion)
        evidence (evidence-with [warrant-entry])
        receipt (route/receipt {:declarations doubled :events (events evidence)
                                :target target})]
    (is (= :duplicate-criterion (:reason (first (:bindings receipt)))))))

(deftest registration-command-covers-the-scope-tests
  (let [command (attestation/registration-command declarations)]
    (is (= ["clojure" "-X:test" ":nses"] (take 3 command)))
    (doseq [test-ns (:tests (:scope criterion))]
      (is (re-find (re-pattern test-ns) (nth command 3))))))

(deftest registration-reports-the-registrys-verdict
  ;; Passing run: the registry executed the command and minted a warrant;
  ;; the client reports the id. The body sent names what to run, never what
  ;; happened.
  (let [sent (atom nil)]
    (with-redefs [babashka.http-client/post
                  (fn [url request]
                    (reset! sent {:url url :body (cheshire.core/parse-string (:body request) true)})
                    {:status 200
                     :body (cheshire.core/generate-string
                            {:evidence/id warrant-id :warrant? true
                             :postcheck {:status "matched"}})})]
      (let [result (attestation/register-warrant-http
                    {:agency-base "http://agency"}
                    {:repo "/home/joe/code/futon2" :author "wm-author"
                     :artifact-dir "/tmp/x"})]
        (is (= "http://agency/api/alpha/test-registry/run" (:url @sent)))
        (is (not (some #(contains? (:body @sent) %) ["warrant?" "results" "outcome"])))
        (is (true? (:warrant? result)))
        (is (= warrant-id (:evidence/id result)))))))

(deftest a-failing-command-registers-no-warrant-and-no-increment
  ;; The bad case, built and watched: the registry executed the command,
  ;; it failed, the mint refused. The client reports warrant? false with
  ;; the reason, the runner's gate supplies no evidence, and the close
  ;; still classifies :unknown with :missing [:attested-increment].
  (with-redefs [babashka.http-client/post
                (fn [_ _]
                  {:status 200
                   :body (cheshire.core/generate-string
                          {:record/type "test-registry/refusal" :warrant? false
                           :reason "command-failed" :details {:exit 1}})})]
    (let [registration (attestation/register-warrant-http
                        {:agency-base "http://agency"}
                        {:repo "/home/joe/code/futon2" :author "wm-author"
                         :artifact-dir "/tmp/x"})]
      (is (false? (:warrant? registration)))
      (is (= "command-failed" (:reason registration)))
      ;; The runner only looks up evidence when registration warranted; the
      ;; lookup over a warrant-less store finds nothing either way.
      (is (nil? (evidence-with [(entry warrant-id (assoc run-record :warrant? false))])))
      (let [receipt (route/receipt {:declarations declarations :events (events nil)
                                    :target target})
            result (classify-close receipt)]
        (is (= :unknown (:class result)))
        (is (= [:attested-increment] (:missing result)))))))

(deftest an-unreachable-registry-is-visible-and-warrant-less
  (with-redefs [babashka.http-client/post
                (fn [_ _] (throw (java.net.ConnectException. "refused")))]
    (let [registration (attestation/register-warrant-http
                        {:agency-base "http://agency"}
                        {:repo "/home/joe/code/futon2" :author "wm-author"
                         :artifact-dir "/tmp/x"})]
      (is (false? (:warrant? registration)))
      (is (= :registry-unreachable (:reason registration)))
      (is (string? (get-in registration [:details :message])))))
  (with-redefs [babashka.http-client/post (fn [_ _] {:status 503 :body ""})]
    (let [registration (attestation/register-warrant-http
                        {:agency-base "http://agency"}
                        {:repo "/home/joe/code/futon2" :author "wm-author"
                         :artifact-dir "/tmp/x"})]
      (is (false? (:warrant? registration)))
      (is (= :registry-http-error (:reason registration)))
      (is (= 503 (:status registration))))))

(deftest an-increment-on-a-typed-failure-refuses
  (let [evidence (evidence-with [warrant-entry])
        receipt (route/receipt {:declarations declarations :events (events evidence)
                                :target target})
        result (classify-close receipt :failure? true)]
    (is (= :refused (:status result)))
    (is (= :increment-failure-contradiction (:kind result)))))
