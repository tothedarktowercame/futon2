(require '[futon3c.agency.invoke-lifecycle-reconciliation :as r])
(import '(java.security MessageDigest) '(java.nio.charset StandardCharsets))
(defn source [record]
  (let [bs (.getBytes (pr-str record) StandardCharsets/UTF_8)
        h (apply str (map #(format "%02x" (bit-and 255 %))
                         (.digest (doto (MessageDigest/getInstance "SHA-256") (.update bs)))))]
    (fn [] {:bytes bs :expected-sha256 h :path "isolated-injected-control"})))
(let [result (r/reconcile
              {:controller (source {:schema :agency/ingress-controller-snapshot-v1
                                    :mode :closed :generation 7 :waiting-writer 0
                                    :accepted-queued 0 :executing 0 :final-delivery 0})
               :hot-ledger (source {:schema :agency/invoke-hot-ledger-snapshot-v1 :generation 7 :jobs {}})
               :accepted-queue (source {:schema :agency/accepted-queue-snapshot-v1 :generation 7})
               :execution (source {:schema :agency/execution-snapshot-v1 :generation 7})
               :final-delivery (source {:schema :agency/final-delivery-snapshot-v1 :generation 7})
               :deferred (source {:schema :agency/deferred-resume-snapshot-v1})})]
  (assert (= :complete-census (:status result)))
  (assert (true? (:zero-in-flight? result)))
  (prn (select-keys result [:status :job-count :zero-in-flight? :deferred-resumes :restart-authorized?])))
