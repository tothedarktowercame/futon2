(require '[clojure.edn :as edn]
         '[futon2.aif.machine-slow-feedback-store-v2 :as store]
         '[futon2.aif.machine-slow-feedback-store-v2-test :as fixture])
(let [[_ s _] (#'fixture/setup)]
  (try
    (let [h (:head (store/recover s))
          path (.resolve (:txdir s) (str (:transaction-sha256 h) ".edn"))
          g (edn/read-string (slurp (str path)))
          forged (assoc g :authority nil :committed-at nil)
          digest (#'store/sha256 (#'store/form-bytes forged))]
      (spit (str (.resolve (:txdir s) (str digest ".edn"))) (pr-str forged))
      (spit (str (:head s)) (pr-str (assoc h :transaction-sha256 digest)))
      (let [result (store/capture s)]
        (prn {:control :self-consistent-untyped-genesis :accepted true :generation (:generation result)})))
    (finally (store/release! s))))
(let [root (#'fixture/dir) s (store/isolated-store root "invalid-genesis")]
  (try
    (let [failure (try
                    (store/initialize! s {:state {:x 1} :revision "r0"
                                          :authority {:opaque (Object.)}
                                          :committed-at "invalid-time"})
                    nil
                    (catch Exception e (:refusal (ex-data e))))
          published (.exists (.toFile (:head s)))]
      (prn {:control :non-edn-authority :refusal failure :head-published? published})
      (assert published))
    (finally (store/release! s))))
