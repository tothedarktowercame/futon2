;; R6 pre-flight: prove a REAL diagnostic tick issues no HTTP POST.
;;
;; Run immediately before any validation run (R2), from the futon2 root:
;;   clojure -M:test -m nil -e '(load-file "holes/labs/wm-contract/r6_zero_post_preflight.clj")'
;; or simply:  clojure -M:test holes/labs/wm-contract/r6_zero_post_preflight.clj
;;
;; WHY THIS EXISTS AND THE UNIT TEST DOES NOT REPLACE IT. The test committed
;; with R6 (run_tick_once_test/full-diagnostic-tick-issues-no-http-post-test)
;; redefines `wm/generate-war-machine` with a stub that calls the two producers
;; of interest. That is a two-site test, not a whole-tick test: the real
;; generator runs ~20 scans, the real judge, and the invariant inventory, and
;; none of them execute under the stub. It therefore cannot catch a site nobody
;; thought of, which is the only kind that matters here.
;;
;; This script runs the REAL path. `http/post` is intercepted and recorded
;; rather than sent; `http/get` is left alone because GETs are read-only and we
;; want the tick to see real data; `spit` is intercepted so the run leaves no
;; artifact behind. If the recorded POST list is non-empty, do not run R2.
(require '[babashka.http-client :as http]
         '[futon2.run-tick-once :as tick])

(def posts (atom []))
(def writes (atom []))

(with-redefs [http/post (fn [& r] (swap! posts conj (first r)) {:status 500 :body ""})
              spit (fn [p & _] (swap! writes conj (str p)) nil)]
  (let [t0 (System/currentTimeMillis)
        _ (try (tick/run-tick-once 14) (catch Exception e {:error (.getMessage e)}))
        ms (- (System/currentTimeMillis) t0)]
    (println (format "r6-preflight: real diagnostic tick in %d ms" ms))
    (println (format "r6-preflight: POSTs attempted: %d" (count @posts)))
    (doseq [p (distinct @posts)] (println "r6-preflight:   POST ->" p))
    (println "r6-preflight: writes the tick would make:")
    (doseq [w (distinct @writes)] (println "r6-preflight:  " w))
    (if (seq @posts)
      (do (println "r6-preflight: FAIL — a real tick issues HTTP POSTs; R2 must not run")
          (System/exit 1))
      (println "r6-preflight: PASS — no POST on the real path"))))
