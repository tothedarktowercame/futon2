(require '[futon3c.agency.invoke-lifecycle-snapshot :as s]
         '[futon3c.agency.invoke-lifecycle-snapshot-test :as t])
(let [{b :boundary} (#'t/fixture-boundary)
      changed (atom false)
      provider (get @(:providers b) :hot-ledger)
      original (:capture provider)]
  (swap! (:providers b) assoc-in [:hot-ledger :capture]
         (fn [g] (assoc (original g) :partial-change @changed)))
  (let [before (s/capture! b)]
    (try (s/mutate! b #(do (reset! changed true) (throw (ex-info "partial write" {}))))
         (catch clojure.lang.ExceptionInfo _))
    (let [after (s/capture! b)]
      (assert (= (:generation before) (:generation after)))
      (assert (not= (get-in before [:sources :hot-ledger :sha256])
                    (get-in after [:sources :hot-ledger :sha256])))
      (prn {:case :failed-mutation-recaptured :generation (:generation after)
            :partial-change (get-in after [:sources :hot-ledger :record :partial-change])}))))
(let [{b :boundary} (#'t/fixture-boundary)
      capture (s/capture! b)
      resolver (:hot-ledger (s/capture-resolvers capture))
      bs (:bytes (resolver)) old (aget bs 0)]
  (aset-byte bs 0 (byte 32))
  (assert (= (byte 32) (aget ^bytes (:bytes (resolver)) 0)))
  (prn {:case :resolver-buffer-mutable :original-first-byte old
        :observed-first-byte (aget ^bytes (:bytes (resolver)) 0)}))
(shutdown-agents)
