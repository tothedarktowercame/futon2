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
;; NEGATIVE CONTROL, run 2026-09-01 -- a gate that cannot fail proves nothing.
;; Taking the REAL route to the token, by calling load-invariant-inventory
;; with the fallback ENABLED under the same interception, fires both
;; detectors: 1 POST to http://localhost:6768/eval and 1 read of
;;   .admintoken
;; With the fallback suppressed, as run_tick_once sets it, neither happens.
;; Reproduce with wm/load-invariant-inventory passed true under the same
;; with-redefs.
;;
;; This script runs the REAL path. `http/post` is intercepted and recorded
;; rather than sent; `http/get` is left alone because GETs are read-only and we
;; want the tick to see real data; `spit` is intercepted so the run leaves no
;; artifact behind. If the recorded POST list is non-empty, do not run R2.
;;
;; ONE WRITE IS PASSED THROUGH, AND IT HAS TO BE (RUN7, 2026-09-01). The run
;; lock (RUN12) is taken by `spit`ting the holder record into
;; data/wm-trace/.run-lock, and `release!` deletes the file only when it can
;; read its own token back out of it. Intercepting that write left a ZERO-BYTE
;; lock file behind: `acquire!` had already called `.createNewFile`, the
;; content never arrived, and release read `:not-ours` and kept the file. That
;; is precisely the shape RUN12 fails closed on -- "a lock file with no pid is
;; what an acquirer looks like between creating the file and writing it" -- so
;; the FIRST S2 run attempt was refused by a lock its own pre-flight had
;; stranded, and every later run would have been too. The two mechanisms had
;; never met: RUN12 ran no tick, and the pre-flights before it predate the lock.
;; The lock is not an artifact to suppress; it is mutual exclusion, and a
;; pre-flight tick is a tick that should hold it. So its writes pass through and
;; are reported separately from the intercepted ones.
(require '[clojure.string]
         '[babashka.http-client :as http]
         '[futon2.run-tick-once :as tick])

(def posts (atom []))
(def writes (atom []))
(def reads (atom []))

;; R6b, the credential tripwire (claude-1). Zero-POST proves the four doors we
;; know about are shut. This proves the key was never taken off the hook: any
;; future route into the shared futon3c JVM has to read
;; futon3c/.admintoken first, whether or not it is an http/post. A gate that
;; only counts POSTs would pass a tick that reached :6768 some other way.
(def ^:private original-slurp slurp)
(def ^:private original-spit spit)
(def ^:private lock-writes (atom []))

(defn- run-lock-path? [path]
  (clojure.string/ends-with? (str path) "/.run-lock"))

(defn- forbidden-read? [path]
  (clojure.string/includes? (str path) ".admintoken"))

(with-redefs [http/post (fn [& r] (swap! posts conj (first r)) {:status 500 :body ""})
              spit (fn [p & r]
                     (if (run-lock-path? p)
                       (do (swap! lock-writes conj (str p))
                           (apply original-spit p r))
                       (do (swap! writes conj (str p)) nil)))
              ;; pass through, but record every path the tick reads
              slurp (fn [src & opts]
                      (swap! reads conj (str src))
                      (apply original-slurp src opts))]
  (let [t0 (System/currentTimeMillis)
        _ (try (tick/run-tick-once 14) (catch Exception e {:error (.getMessage e)}))
        ms (- (System/currentTimeMillis) t0)]
    (println (format "r6-preflight: real diagnostic tick in %d ms" ms))
    (println (format "r6-preflight: POSTs attempted: %d" (count @posts)))
    (doseq [p (distinct @posts)] (println "r6-preflight:   POST ->" p))
    (println "r6-preflight: writes the tick would make:")
    (doseq [w (distinct @writes)] (println "r6-preflight:  " w))
    (println (format "r6-preflight: run-lock writes PASSED THROUGH: %d"
                     (count @lock-writes)))
    (doseq [w (distinct @lock-writes)] (println "r6-preflight:   LOCK ->" w))
    ;; A pre-flight that took the lock must not leave it: report it, because a
    ;; stranded lock is what this repair exists to stop happening silently.
    (let [lock-file (java.io.File. (str (System/getProperty "user.home")
                                        "/code/futon2/data/wm-trace/.run-lock"))]
      (println (format "r6-preflight: run lock after the tick: %s"
                       (if (.exists lock-file)
                         (str "STILL PRESENT (" (.length lock-file)
                              " bytes) -- the run will be refused")
                         "released"))))
    (let [token-reads (filter forbidden-read? @reads)]
      (println (format "r6-preflight: paths read: %d; .admintoken reads: %d"
                       (count (distinct @reads)) (count token-reads)))
      (doseq [t (distinct token-reads)] (println "r6-preflight:   TOKEN ->" t))
      (cond
        (seq @posts)
        (do (println "r6-preflight: FAIL — a real tick issues HTTP POSTs; R2 must not run")
            (System/exit 1))

        (seq token-reads)
        (do (println "r6-preflight: FAIL — the tick read the admin token, so a route"
                     "into the shared JVM is still open even though no POST was made")
            (System/exit 1))

        :else
        (println "r6-preflight: PASS — no POST on the real path, and the admin token was never read")))))
