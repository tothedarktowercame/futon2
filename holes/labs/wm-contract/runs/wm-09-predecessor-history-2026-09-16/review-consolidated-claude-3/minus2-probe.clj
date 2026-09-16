;; claude-3: realroots-probe over the manifest-relocated copy with the two refusing records (40-v1/attempt-036, 43-v1/attempt-053) removed.
(require '[futon2.aif.receipt-construction :as construction] '[clojure.string :as str])
(def base "/tmp/c3-wm09c/minus2/")
(def roots (->> (.listFiles (java.io.File. base)) (filter #(.isDirectory %)) (map str) sort vec))
(defn shorten [s] (if (string? s) (str/replace s base "COPY/") s))
(doseq [target ["M-review-synthetic-fresh-target" "M-learning-loop" "M-expressions-of-interest"
                :sorry/pudding-g1-arrow-witness-binding ":sorry/pudding-g1-arrow-witness-binding"
                "sorry/pudding-g1-arrow-witness-binding"]]
  (prn {:target target
        :result (try (let [r (construction/previous! {:occurrence {:action/value {:type :advance-mission :target target}
                                                                    :action-at "2026-09-16T16:00:00Z"}} roots)]
                       {:admission-reason (:admission-reason r)
                        :excluded (frequencies (map :reason (get-in r [:provenance :excluded-attempts])))})
                     (catch clojure.lang.ExceptionInfo e
                       (let [d (ex-data e)]
                         {:refusal (select-keys d [:construction/refusal :reason])
                          :file (shorten (or (:file d) (:history/close-file d)))
                          :rejection-kind (get-in d [:history/rejection :kind])
                          :excluded (frequencies (map :reason (:history/excluded-attempts d)))})))}))
(shutdown-agents)
