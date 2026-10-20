(ns futon2.aif.f44a-unfenced-object-test
  "F44a (2026-10-20): a handoff answer with prose before one unfenced JSON
  object decodes. Live pin: the click 14 author answer, job
  invoke-1792536618647-168-cc36fe17, 3,293 characters, the object from
  character 448, kept verbatim in fixtures/f44a-click14-author-answer.txt."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.handoff-contract :as handoff]))

(def live-answer
  (slurp (io/resource "futon2/aif/fixtures/f44a-click14-author-answer.txt")))

(def requisition-id
  "790c9db1a5ccda5cfe9f0b9834ae4ee3cc37fefd908b553634096dd361965d1e")

(deftest click-14-author-answer-decodes
  (let [decoded (handoff/response-json-object live-answer)]
    (is (= 3293 (count live-answer)))
    (is (= 448 (.indexOf ^String live-answer "{")))
    (is (= requisition-id (:requisition-id decoded)))
    (is (= 8 (count (:decisions decoded))))
    (testing "the handler's path: response-map on the done job"
      (is (= decoded (handoff/response-map {:state "done" :result live-answer}))))
    (testing "a trailing sentence after the object"
      (is (= decoded (handoff/response-json-object
                      (str live-answer "\nDone; nothing further.")))))))

(deftest nested-objects-inside-the-one-object-decode
  ;; the click 14 object holds nested objects (decisions, patch); the first
  ;; draft of the walker lost the span start at an inner close and threw
  (is (= {:a {:b 1}} (handoff/response-json-object "prose {\"a\":{\"b\":1}} tail")))
  (is (= {:a {:b {:c "}"}}}
         (handoff/response-json-object "x {\"a\":{\"b\":{\"c\":\"}\"}}} y"))))

(deftest unfenced-refusals-stay-nil
  (doseq [bad ["{\"a\":1} and then {\"b\":2}"          ; two objects, prose between
               "{\"a\":1}{\"b\":2}"                     ; object followed by object
               "{} {}"
               "see { here {\"a\":1}"                    ; a { in the prose before
               "{\"a\":1} } tail"                        ; a } the object did not open
               "{not json}"
               "[1]"
               "[{\"a\":1}]"
               "42"
               "prose only, no fence"
               ""]]
    (is (nil? (handoff/response-json-object bad)) (pr-str bad))))

(deftest fence-rule-keeps-precedence
  (let [payload {:answer "yes"}
        fenced "```json\n{\"answer\":\"yes\"}\n```"]
    (is (= payload (handoff/response-json-object (str "prose\n" fenced))))
    (is (= payload (handoff/response-json-object
                    (str fenced "\nValidated; not published."))))
    (is (nil? (handoff/response-json-object (str fenced "\n" fenced))))
    (testing "an unfenced object beside one fence is not read"
      (is (= payload (handoff/response-json-object
                      (str fenced "\n{\"answer\":\"no\"}")))))
    (testing "a bare object still decodes"
      (is (= payload (handoff/response-json-object "{\"answer\":\"yes\"}"))))))
