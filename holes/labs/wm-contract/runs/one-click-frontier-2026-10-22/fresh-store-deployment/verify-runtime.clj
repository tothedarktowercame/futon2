(let [root "/home/joe/code/futon2/data/wm-meta-injuries-live-2026-10-22"
 paths {:deposit futon2.aif.meta-injury-store/default-root
        :ledger futon2.aif.meta-self-heal-production/default-ledger-path
        :transaction futon2.aif.meta-self-heal-production/default-transaction-path
        :diagnoses futon2.aif.meta-self-heal-production/default-diagnosis-root
        :acceptances futon2.aif.meta-self-heal-production/default-acceptance-root
        :acceptance-runs futon3c.wm.meta-self-heal-adapters/*acceptance-runs-root*}
 configured (futon3c.wm.runner-service/meta-self-heal-preflight! {})
 loaded (futon2.aif.meta-injury-store/load-ledger (:ledger paths))]
 (assert (= root (:deposit paths)))
 (assert (every? #(clojure.string/starts-with? % root) (vals paths)))
 (assert (= (:ledger paths) (:meta-injury-ledger-path configured)))
 (assert (= (:transaction paths) (:meta-repair-transaction-path configured)))
 (assert (= :new (:source loaded)))
 (assert (empty? (get-in loaded [:ledger :entries])))
 {:paths paths :effective-preflight (select-keys configured [:meta-injury-ledger-path :meta-repair-transaction-path :meta-self-heal/readiness-route])
 :ledger (select-keys loaded [:status :source]) :entry-count (count (get-in loaded [:ledger :entries]))
 :running? (:running? (futon3c.wm.runner-service/status))
 :runner-source (select-keys (futon2.aif.full-loop-runner/runner-source-drift) [:runner/source-check :runner/loaded-sha256 :runner/canonical-sha256])})
