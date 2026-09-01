;; RUN9 / stage S4 pre-flight: prove the LIVE tick puts F_pi into the policy
;; posterior when FUTON_WM_FPI_POSTERIOR=1, and still issues no HTTP POST and
;; never reads the admin token.
;;
;; Same interception as run8_s3_preflight.clj (http/post recorded, spit
;; suppressed except the run lock, slurp recorded) -- see r6_zero_post_preflight
;; for why the lock write must pass through. What is added here is that the
;; tick's OWN decision is re-scored WITHOUT F_pi from its own recorded
;; controller scores, habit biases and tau, and the two posteriors compared. A
;; printed :applied? true is a claim; a posterior that differs from the F_pi-free
;; recomputation of the same tick is the evidence for it.
;;
;; Run it twice, from the futon2 root, and compare the two blocks:
;;
;;   FUTON_WM_TRACE_POLICY_DETAILS=1 FUTON_WM_FPI_DARK=1 \
;;     clojure -M:test holes/labs/wm-contract/run9_s4_preflight.clj
;;   FUTON_WM_TRACE_POLICY_DETAILS=1 FUTON_WM_FPI_DARK=1 \
;;     FUTON_WM_FPI_POSTERIOR=1 \
;;     clojure -M:test holes/labs/wm-contract/run9_s4_preflight.clj
;;
;; The env is set in the SHELL and not stubbed in-process on purpose: the flags
;; being read from the real environment are half of what this is evidence for.
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

(println "run9-s4-preflight: FUTON_WM_FPI_POSTERIOR ="
         (pr-str (System/getenv "FUTON_WM_FPI_POSTERIOR"))
         "-> f-pi-posterior?" @#'wm/*f-pi-posterior?*)
(println "run9-s4-preflight: f-pi-dark?" @#'wm/*f-pi-dark?*
         "| tau-mode" tau-mode)

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
        envelope (:f-pi-posterior decision)
        ranking (:controller-ranking decision)
        tau (:tau decision)
        ;; The tick's OWN inputs, read back off its own decision: the same
        ;; g-totals and ln E `strategic-recommendation` handed the seam, in
        ;; the same order.
        g-totals (mapv :controller-score ranking)
        log-priors (mapv :habit-prior-bias ranking)
        without (when (and tau (seq g-totals))
                  (sort (policy/softmax-weights g-totals tau log-priors)))
        recorded (sort (vals (:softmax-weights decision)))]
    (println (format "run9-s4-preflight: real diagnostic tick in %d ms" ms))
    (when (:error run) (println "run9-s4-preflight: TICK ERROR:" (:error run)))
    (println (format "run9-s4-preflight: POSTs attempted: %d" (count @posts)))
    (println (format "run9-s4-preflight: run-lock writes passed through: %d"
                     (count @lock-writes)))
    (let [lock-file (java.io.File. (str (System/getProperty "user.home")
                                        "/code/futon2/data/wm-trace/.run-lock"))]
      (println (format "run9-s4-preflight: run lock after the tick: %s"
                       (if (.exists lock-file) "STILL PRESENT" "released"))))
    (let [token-reads (filter forbidden-read? @reads)]
      (println (format "run9-s4-preflight: paths read: %d; .admintoken reads: %d"
                       (count (distinct @reads)) (count token-reads))))
    (println "run9-s4-preflight: --- the F_pi envelope this tick recorded ---")
    (doseq [k [:applied? :status :reason :coverage :scaling :candidate-count
               :uncovered-count :f-pi-min :f-pi-max]]
      (when (contains? envelope k)
        (println (format "  %-18s %s" k (pr-str (get envelope k))))))
    (println "run9-s4-preflight: --- the readback the envelope was built from ---")
    (println "  readback :status  " (get-in result [:f-pi-by-candidate-id :status]))
    (println "  matched / current "
             [(get-in result [:f-pi-provenance :matched-count])
              (get-in result [:f-pi-provenance :current-candidate-count])])
    (println "  candidates selected over" (count ranking))
    (println "run9-s4-preflight: --- posterior, recorded vs F_pi-free ---")
    (println "  :tau              " tau)
    (println "  identical?        " (= recorded without))
    (when (and recorded without (= (count recorded) (count without)))
      (println "  max |delta|       "
               (reduce max 0.0 (map #(Math/abs (- (double %1) (double %2)))
                                    recorded without))))
    ;; THE CLAIM, checked rather than printed. With the flag on and coverage
    ;; complete, the recorded posterior must NOT be the one the same tick's own
    ;; scores produce without F_pi. With the flag off it must be exactly that
    ;; one -- the control half, so an unchanged posterior cannot be read as
    ;; success.
    (let [applied? (boolean (:applied? envelope))
          ok (cond
               (not @#'wm/*f-pi-posterior?*)
               (do (println "run9-s4-preflight: control run (flag off)")
                   (and (false? applied?)
                        (= :flag-off (:reason envelope))
                        (= recorded without)))

               applied?
               (and (= :present (:status envelope))
                    (= :complete (:coverage envelope))
                    (= :unscaled (:scaling envelope))
                    (not= recorded without))

               :else
               (do (println "run9-s4-preflight: flag ON but the term did not"
                            "enter -- reason" (pr-str (:reason envelope)))
                   false))]
      (cond
        (seq @posts)
        (do (println "run9-s4-preflight: FAIL -- a real tick issues HTTP POSTs")
            (System/exit 1))
        (seq (filter forbidden-read? @reads))
        (do (println "run9-s4-preflight: FAIL -- the tick read the admin token")
            (System/exit 1))
        (not ok)
        (do (println "run9-s4-preflight: FAIL -- the posterior claim does not hold")
            (System/exit 1))
        :else
        (println "run9-s4-preflight: PASS")))))
