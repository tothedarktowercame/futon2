#!/usr/bin/env bb
(require '[babashka.fs :as fs] '[clojure.edn :as edn] '[clojure.string :as str])
(def families [:fullLoopCheckpoints :wmTraceRecords :tickRunRecords :closeCohortRecords
               :dispatchJobRecords :parkContinuationRecords :reviewAdmissionRecords])
(defn refuse [k x] (throw (ex-info (name k) {:refusal k :data x})))
(defn q [s] (str "\"" (str/escape s {\\ "\\\\" \" "\\\"" \newline "\\n" \return "\\r" \tab "\\t"}) "\""))
(defn exact-keys! [m ks] (when-not (= (set (keys m)) (set ks)) (refuse :keys (keys m))))
(defn node [x]
  (exact-keys! x [:id :state :claim :scope :reason])
  (case (:state x)
    :unvalidated (format "⟨%s, .unvalidated %s %s⟩" (q (:id x)) (q (:claim x)) (q (:scope x)))
    :typed-absence (format "⟨%s, .typedAbsence %s %s %s⟩" (q (:id x)) (q (:claim x)) (q (:scope x)) (q (:reason x)))
    (refuse :unsupported-node-state (:state x))))
(defn edge [x]
  (exact-keys! x [:id :state :scope])
  (when-not (= :mandatory-unfired (:state x)) (refuse :unsupported-edge-state (:state x)))
  (format "⟨%s, .mandatoryUnfired %s⟩" (q (:id x)) (q (:scope x))))
(defn family [x]
  (exact-keys! x [:family :state :reason])
  (case (:state x)
    :typed-gap (format ".typedGap .%s %s" (name (:family x)) (q (:reason x)))
    (refuse :unsupported-family-state (:state x))))
(defn main [in out expected]
  (let [bs (fs/read-all-bytes in) observed (let [d (.digest (java.security.MessageDigest/getInstance "SHA-256") bs)] (apply str (map #(format "%02x" (bit-and % 255)) d)))]
    (when-not (= expected observed) (refuse :pin-mismatch [expected observed]))
    (let [x (edn/read-string (String. bs "UTF-8"))]
      (exact-keys! x [:schema :declared-nodes :nodes :declared-connections :connections :selection :families])
      (when-not (= :wm/typed-gap-census-v1 (:schema x)) (refuse :schema (:schema x)))
      (when-not (= (:declared-nodes x) (mapv :id (:nodes x))) (refuse :node-order nil))
      (when-not (= (:declared-connections x) (mapv :id (:connections x))) (refuse :edge-order nil))
      (when-not (= families (mapv :family (:families x))) (refuse :family-order nil))
      (when-not (= :refused-shape (get-in x [:selection :state])) (refuse :selection nil))
      (let [body (format "import DarkTower/ Waters Unexpected token '%s' in string value" "Dark")]))))
(apply main *command-line-args*)
