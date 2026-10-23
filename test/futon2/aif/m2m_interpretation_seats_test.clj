(ns futon2.aif.m2m-interpretation-seats-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [futon2.aif.flight-runner :as flight]
            [futon2.aif.wm.click-ask :as ask]
            [futon2.aif.want-interpretation :as wi]))
(def seats ["zai-4" "zai-5" "zai-6"])
(deftest explicit-seats-reach-agency-answer-construction
  (let [seen (atom [])]
    (with-redefs [flight/agency-answer-fn (fn [opts] (swap! seen conj (:seat opts)) (constantly nil))
                  wi/unproduced-wants (fn [& _] [:a :b :c])]
      (with-redefs-fn {#'flight/target-view (fn [& _] {})
                       #'flight/ask-one (fn [& _] {:outcome :not-answered})}
        #((ask/click-ask-fn {:interpretation-seats seats :cascade-sources {}
                            :mission-hole-sources [{:target "M-test" :want [:a :b :c]
                                                    :holes [{} {} {}]}]})
          {} {:target "M-test"})))
    (is (= (sort seats) (sort @seen)))))
(deftest script-payload-carries-ask-seats
  ;; Execute the script's actual payload builder only: no curl, no click.
  (let [script (slurp "scripts/wm_click.sh")
        builder (re-find #"(?s)  payload=\$\(python3.*?\n\)" script)
        result (shell/sh "bash" "-c"
                         (str "RUNID=test AUTHOR=zai-2 REVIEWER=zai-3 REPAIR=zai-8 ISSUING_CALLER=test G_LAW='' ASK_SEATS=zai-4,zai-5,zai-6\n"
                              builder "\nprintf '%s' \"$payload\"\n"))]
    (is (= 0 (:exit result)))
    (is (str/includes? (:out result) "\"interpretation-seats\": [\"zai-4\", \"zai-5\", \"zai-6\"]"))
    (is (str/includes? script "--ask-seats)"))))

(deftest default-asks-prefer-idle-seats-outside-custom-cast
  (let [function (re-find #"(?s)default_ask_seats\(\) \{.*?\n\}" (slurp "scripts/wm_click.sh"))
        roster "{\"agents\":{\"zai-4\":{\"status\":\"idle\",\"invoke-ready?\":true},\"zai-5\":{\"status\":\"invoking\"},\"zai-6\":{\"status\":\"idle\",\"invoke-ready?\":true},\"zai-7\":{\"status\":\"idle\",\"invoke-ready?\":true},\"zai-9\":{\"status\":\"idle\",\"invoke-ready?\":true}}}"
        result (shell/sh "bash" "-c" (str "AUTHOR=zai-4 REVIEWER=zai-2 REPAIR=zai-8 ISSUING_CALLER=test\n"
                                           function "\ndefault_ask_seats") :in roster)]
    (is (= 0 (:exit result)))
    (is (= "zai-6,zai-7,zai-9" (str/trim (:out result))))))
