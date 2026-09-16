;; Reads a /tmp COPY of data/wm-full-loop* with :not-reached-construction attempts removed,
;; to expose the next discovery blocker once K2/K3 exclusions exist.
(require '[futon2.aif.receipt-construction :as construction] '[clojure.string])
(def roots (->> (.listFiles (java.io.File. "/tmp/c3-wm09i/realcopy2")) (filter #(.isDirectory %)) (map str) sort vec))
(doseq [target ["M-review-synthetic-fresh-target" "M-learning-loop"]]
  (prn {:target target
        :result (try (select-keys (construction/previous! {:occurrence {:action/value {:type :advance-mission :target target}
                                                                         :action-at "2026-09-16T16:00:00Z"}} roots)
                                  [:admission-reason])
                     (catch clojure.lang.ExceptionInfo e
                       (-> (ex-data e)
                           (select-keys [:construction/refusal :reason :file :history/close-file])
                           (update-vals #(if (string? %) (clojure.string/replace % "/tmp/c3-wm09i/realcopy2/" "COPY/") %)))))}))
