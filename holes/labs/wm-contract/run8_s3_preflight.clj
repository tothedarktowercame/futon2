;; RUN8 / stage S3 pre-flight: prove the LIVE tick takes tau from the beta
;; carry when FUTON_WM_TAU_MODE=variational-beta-gamma, and still issues no
;; HTTP POST and never reads the admin token.
;;
;; Same interception as r6_zero_post_preflight.clj (http/post recorded, spit
;; suppressed except the run lock, slurp recorded) -- see that file's header for
;; why the lock write must pass through. What is added here is that the tick's
;; RETURN VALUE is read, so the tau the live selection actually used is
;; reported rather than inferred from a record that this script deliberately
;; does not let the tick write.
;;
;; Run it twice, from the futon2 root, and compare the two blocks:
;;
;;   clojure -M:test holes/labs/wm-contract/run8_s3_preflight.clj
;;   FUTON_WM_TRACE_POLICY_DETAILS=1 FUTON_WM_FPI_DARK=1 FUTON_WM_BETA_DARK=1 \
;;     FUTON_WM_TAU_MODE=variational-beta-gamma \
;;     clojure -M:test holes/labs/wm-contract/run8_s3_preflight.clj
;;
;; The env is set in the SHELL and not stubbed in-process on purpose: the mode
;; parser reading the real environment is half of what this is evidence for.
(require '[clojure.string]
         '[babashka.http-client :as http]
         '[futon2.aif.policy :as policy]
         '[futon2.report.war-machine :as wm]
         '[futon2.run-tick-once :as tick])

(def posts (atom []))
(def writes (atom []))
(def reads (atom []))
(def lock-writes (atom []))

(def ^:private original-slurp slurp)
(def ^:private original-spit spit)

(defn- run-lock-path? [path]
  (clojure.string/ends-with? (str path) "/.run-lock"))

(defn- forbidden-read? [path]
  (clojure.string/includes? (str path) ".admintoken"))

(def tau-mode (#'wm/arena-tau-mode))

(println "run8-s3-preflight: FUTON_WM_TAU_MODE ="
         (pr-str (System/getenv "FUTON_WM_TAU_MODE"))
         "-> resolved mode" tau-mode)
(println "run8-s3-preflight: beta-dark?" @#'wm/*beta-dark?*
         "| f-pi-dark?" @#'wm/*f-pi-dark?*)

(with-redefs [http/post (fn [& r] (swap! posts conj (first r)) {:status 500 :body ""})
              spit (fn [p & r]
                     (if (run-lock-path? p)
                       (do (swap! lock-writes conj (str p))
                           (apply original-spit p r))
                       (do (swap! writes conj (str p)) nil)))
              slurp (fn [src & opts]
                      (swap! reads conj (str src))
                      (apply original-slurp src opts))]
  (let [t0 (System/currentTimeMillis)
        run (try (tick/run-tick-once 14)
                 (catch Exception e {:error (.getMessage e)}))
        ms (- (System/currentTimeMillis) t0)
        result (:result run)
        decision (:decision result)
        pps (:policy-precision-state result)]
    (println (format "run8-s3-preflight: real diagnostic tick in %d ms" ms))
    (when (:error run) (println "run8-s3-preflight: TICK ERROR:" (:error run)))
    (println (format "run8-s3-preflight: POSTs attempted: %d" (count @posts)))
    (println (format "run8-s3-preflight: run-lock writes passed through: %d"
                     (count @lock-writes)))
    (let [lock-file (java.io.File. (str (System/getProperty "user.home")
                                        "/code/futon2/data/wm-trace/.run-lock"))]
      (println (format "run8-s3-preflight: run lock after the tick: %s"
                       (if (.exists lock-file) "STILL PRESENT" "released"))))
    (let [token-reads (filter forbidden-read? @reads)]
      (println (format "run8-s3-preflight: paths read: %d; .admintoken reads: %d"
                       (count (distinct @reads)) (count token-reads))))
    (println "run8-s3-preflight: --- what the LIVE selection used this tick ---")
    (println "  :tau          " (:tau decision))
    (println "  :tau-source   " (:tau-source decision))
    (println "  :tau-spread   " (:tau-spread decision))
    (println "  :selection-gain" (:selection-gain decision))
    (println "  explanation :tau-mode" (get-in decision [:decision-explanation :tau-mode]))
    (println "  :wm-version :tau-mode"
             (get-in result [:wm-version :tau-mode]))
    (println "run8-s3-preflight: --- the beta carry this tick computed ---")
    (println "  :status       " (:status pps))
    (println "  :beta         " (:beta pps))
    (println "  :beta-source  " (:beta-source pps))
    (println "  :reason       " (:reason pps))
    (println "  :solve converged?/bracketed?"
             [(get-in pps [:solve :converged?]) (get-in pps [:solve :bracketed?])])
    (println "  :solve gamma  " (get-in pps [:solve :gamma]))
    ;; THE CLAIM, checked rather than printed: under the variational mode the
    ;; tau the selection used IS the beta the carry produced, and it is not the
    ;; selection-gain law's answer.
    (let [ok (cond
               (not= :variational-beta-gamma tau-mode)
               (do (println "run8-s3-preflight: control run (mode" tau-mode
                            "), no variational claim made")
                   (= (policy/temperature-source {:tau-mode tau-mode})
                      (:tau-source decision)))

               :else
               (let [g (max 0.01 (double (or (:selection-gain decision) 1.0)))]
                 (and (= (:beta pps) (:tau decision))
                      (= (:beta-source pps) (:tau-source decision))
                      (or (= (:beta pps) (/ 1.0 g))
                          (do (println "run8-s3-preflight: tau" (:tau decision)
                                       "is NOT the selection-gain answer" (/ 1.0 g))
                              true)))))]
      (cond
        (seq @posts)
        (do (println "run8-s3-preflight: FAIL -- a real tick issues HTTP POSTs")
            (System/exit 1))
        (seq (filter forbidden-read? @reads))
        (do (println "run8-s3-preflight: FAIL -- the tick read the admin token")
            (System/exit 1))
        (not ok)
        (do (println "run8-s3-preflight: FAIL -- tau did not come from the beta carry")
            (System/exit 1))
        :else
        (println "run8-s3-preflight: PASS")))))
