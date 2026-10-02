(ns futon2.aif.meta-injury-observation
  "Reconstruct a META injury observation from immutable WM run-record bytes."
  (:require [clojure.edn :as edn])
  (:import [java.security MessageDigest]))

(def schema :wm/injury-observation-v1)
(def capability :wm-click-completes-with-reviewable-receipts)

(defn- sha256 [^bytes bytes]
  (let [digest (.digest (MessageDigest/getInstance "SHA-256") bytes)]
    (apply str (map #(format "%02x" (bit-and % 0xff)) digest))))

(defn- canonical [x]
  (cond
    (map? x) (into (sorted-map) (map (fn [[k v]] [k (canonical v)])) x)
    (set? x) (mapv canonical (sort-by pr-str x))
    (sequential? x) (mapv canonical x)
    :else x))

(defn- digest [x]
  (sha256 (.getBytes (pr-str (canonical x)) "UTF-8")))

(defn- pin? [x]
  (and (map? x) (string? (:path x)) (string? (:sha256 x))
       (boolean (re-matches #"[0-9a-f]{64}" (:sha256 x)))))

(defn- refusal [reason details]
  {:schema schema :status :refused :reason reason :details details})

(defn produce
  "Derive the one currently declared META injury from exact run-record bytes.
   EXPECTED-SOURCE-PIN is retained independently by the caller."
  [{:keys [source-bytes expected-source-pin]}]
  (cond
    (not (bytes? source-bytes))
    (refusal :injury-source-bytes-missing {})

    (not (pin? expected-source-pin))
    (refusal :external-injury-source-authority-invalid
             {:expected-source-pin expected-source-pin})

    (not= (:sha256 expected-source-pin) (sha256 source-bytes))
    (refusal :injury-source-drift
             {:expected (:sha256 expected-source-pin)
              :actual (sha256 source-bytes)})

    :else
    (try
      (let [record (edn/read-string (String. ^bytes source-bytes "UTF-8"))
            evidence
            {:source-record (:source-record record)
             :run-id (:run-id record)
             :click-id (:click-id record)
             :failure (select-keys (:failure record) [:kind :stage :detail-kind])
             :terminal-receipt (select-keys (:terminal-receipt record)
                                            [:kind :failure-kind])
             :outer-task-selection (select-keys (:outer-task-selection record)
                                                [:schema :status :reason])
             :loop-node-exercise (select-keys (:loop-node-exercise record)
                                              [:schema :status :counts])
             :run-output (select-keys (:run-output record)
                                      [:schema :status :reason :outcome])
             :trace-written (:trace-written record)}
            active?
            (and (= :wm/injury-evidence-v1 (:schema record))
                 (pin? (:source-record evidence))
                 (string? (:run-id evidence)) (string? (:click-id evidence))
                 (= :abstained (get-in evidence [:failure :kind]))
                 (= :selection (get-in evidence [:failure :stage]))
                 (= :wm/selection-terminal-abstention
                    (get-in evidence [:failure :detail-kind]))
                 (= :failure (get-in evidence [:terminal-receipt :kind]))
                 (= :abstained (get-in evidence [:terminal-receipt :failure-kind]))
                 (= :absent (get-in evidence [:outer-task-selection :status]))
                 (= :incomplete (get-in evidence [:loop-node-exercise :status]))
                 (= :absent (get-in evidence [:run-output :status]))
                 (= :abstained (get-in evidence [:run-output :outcome]))
                 (false? (:trace-written evidence)))
            body (if active?
                   {:schema schema :status :active :capability capability
                    :source-pin expected-source-pin :evidence evidence}
                   {:schema schema :status :absent
                    :reason :required-injury-evidence-not-present
                    :source-pin expected-source-pin :evidence evidence})]
        (assoc body :observation-pin
               {:path "wm://meta-injury-observation-v1"
                :sha256 (digest body)}))
      (catch Throwable t
        (refusal :injury-source-unreadable {:message (ex-message t)})))))

(defn verify
  "Reconstruct and compare; receipt-local capability and evidence are not authority."
  [observation authority]
  (let [expected (produce authority)]
    (if (= expected observation)
      {:schema schema :status :verified
       :source-pin (:source-pin observation)
       :observation-pin (:observation-pin observation)}
      (refusal :injury-observation-does-not-match-source
               {:expected expected :actual observation}))))
