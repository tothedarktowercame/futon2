(ns futon2.aif.phase-replay-input-test
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.test-support.runner-fixture :as fixture]))

(def busy-body "futon1b busy (HTTP 503): the read was not served")

(defn- temp-dir []
  (.toFile (java.nio.file.Files/createTempDirectory
            "phase-replay-input-test" (make-array java.nio.file.attribute.FileAttribute 0))))

(defn- read-pin [pin]
  (edn/read-string (slurp (:path pin))))

(defn- sha256-file [path]
  (let [digest (java.security.MessageDigest/getInstance "SHA-256")]
    (with-open [input (java.io.FileInputStream. path)]
      (let [buffer (byte-array 8192)]
        (loop []
          (let [n (.read input buffer)]
            (when (pos? n)
              (.update digest buffer 0 n)
              (recur))))))
    (apply str (map #(format "%02x" (bit-and 0xff %)) (.digest digest)))))

(deftest strategic-selection-journals-failure-and-terminal-pin
  (let [journal-root (temp-dir)
        run-root (temp-dir)
        request {:scheduler-habit-ranking ["M-a"]}
        failure (try
                  (runner/strategic-selection!
                   {:phase-replay-input-root (.getPath journal-root)
                    :strategic-selection-invoke-fn
                    (fn [_] {:raw busy-body})
                    :strategic-selection-sleep-fn (fn [_])}
                   request)
                  nil
                  (catch clojure.lang.ExceptionInfo e e))
        pin (:phase-replay-input-pin (ex-data failure))
        journal (read-pin pin)
        written (runner/persist-run-record!
                 (assoc (fixture/isolated-runner-opts)
                        :run-record-dir (.getPath run-root)) "phase-replay-failure"
                 "2026-10-29T00:00:00Z"
                 {:outcome :incomplete
                  :wm/route [{:node :SELECTION :at "before"}
                             {:node :FULL_LOOP_CLOSE :at "after"}]
                  :data {:failure-kind :strategic-selection-unavailable
                         :failure-stage :selection
                         :error "busy"
                         :error-data (ex-data failure)}})
        record (edn/read-string (slurp (:run-record written)))]
    (is (= request (:request journal)))
    (is (= busy-body (get-in journal [:response :raw])))
    (is (= pin (get-in record [:failure :detail :phase-replay-input-pin])))
    (is (= (:sha256 pin) (sha256-file (:path pin))))))

(deftest strategic-selection-journals-success-without-credentials
  (let [journal-root (temp-dir)
        credential "Bearer must-not-be-persisted"
        result (runner/strategic-selection!
                {:phase-replay-input-root (.getPath journal-root)
                 :strategic-selection-invoke-fn
                 (fn [_] {:ok true
                          :selection {:status :verified-live-selection
                                      :selected-mission-ids ["M-a"]}})}
                {:scheduler-habit-ranking ["M-a"]
                 :authorization credential})
        journals (->> (.listFiles journal-root)
                      (filter #(.endsWith (.getName %) ".edn"))
                      (map #(vector % (edn/read-string (slurp %)))))
        [file journal] (first (filter #(= :wm/phase-replay-input-v1
                                          (:schema (second %))) journals))
        text (slurp file)]
    (is (= :returned (get-in journal [:response :status])))
    (is (= true (get-in journal [:response :body :ok])))
    (is (= :verified-live-selection (:status result)))
    (is (not (str/includes? text credential)))
    (is (= {:authority-reference :authorization}
           (get-in journal [:request :authorization])))))
