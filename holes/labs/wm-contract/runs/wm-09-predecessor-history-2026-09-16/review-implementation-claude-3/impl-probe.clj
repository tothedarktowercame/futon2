;; claude-3 implementation-review controls for 6c417fc8. Temporary stores; the
;; real-roots case reads a /tmp COPY of data/wm-full-loop* (no production reads).
(require '[clojure.string :as str]
         '[clojure.java.io :as io]
         '[futon2.aif.receipt-construction :as construction]
         '[futon2.aif.receipt-construction-test :as fixture]
         '[futon2.aif.close-retention :as retention]
         '[futon2.aif.interpretation-job :as job]
         '[futon2.aif.full-loop-runner :as runner])
(import '[java.nio.file Files] '[java.nio.file.attribute FileAttribute])

(defn outcome [f]
  (try (let [x (f)] {:admission-reason (:admission-reason x)
                    :status (get-in x [:provenance :status])
                    :epoch (get-in x [:provenance :identity :semantic-epoch])})
       (catch Throwable e
         (let [d (ex-data e)]
           {:exception (.getName (class e))
            :refusal (select-keys d [:construction/refusal :reason :interpretation/refusal])
            :file (some-> (or (:file d) (:history/close-file d)) str (str/replace #"/tmp/[^/]+/" "TMP/"))
            ::e e}))))

(defn temp [] (.toFile (Files/createTempDirectory "c3-wm09-impl" (make-array FileAttribute 0))))
(defn rm! [dir] (doseq [f (reverse (file-seq dir))] (Files/delete (.toPath f))))

(defn not-reached! [root cohort attempt recorded-at & {:keys [selection-target mission-judgment]}]
  ;; Producer form from close-core!: construction checkpoint {:sorry {:kind :not-reached-construction :outcome ...}}
  (let [dir (io/file root cohort attempt)
        env (fn [seq cp] {:event/schema-version 1 :cohort/id (keyword cohort) :attempt/id attempt
                          :attempt/ordinal 1 :event/sequence seq :checkpoint/type cp :recorded-at recorded-at})]
    (.mkdirs dir)
    (spit (io/file dir "001-time-step.edn") (pr-str (assoc (env 1 :time-step) :payload {:judgment {:semantic-epoch :full-loop-real-actuation-v6}})))
    (spit (io/file dir "002-selection.edn")
          (pr-str (assoc (env 2 :selection) :payload (if selection-target
                                                       {:judgment {:selected-action {:type :advance-mission :target selection-target}}}
                                                       {:sorry {:kind :not-reached-selection :outcome :agent-unavailable}}))))
    (spit (io/file dir "003-construction.edn")
          (pr-str (assoc (env 3 :construction) :payload (if mission-judgment
                                                          {:judgment {:mission mission-judgment :cascade {}}}
                                                          {:sorry {:kind :not-reached-construction :outcome :agent-unavailable}}))))
    (spit (io/file dir "007-closed.edn")
          (pr-str (assoc (env 7 :closed) :payload {:judgment {:outcome :agent-unavailable :grounded? false} :ground {}})))
    dir))

(defn current-for [latest at] (assoc-in (:identity latest) [:occurrence :action-at] at))

(defn with-valid [label f]
  (let [t (temp) a (io/file t "a") b (io/file t "b")]
    (try (let [latest (fixture/history-fixture! a :valid "2026-09-15T11:00:00Z" "2026-09-15T11:01:00Z")]
           (.mkdirs b)
           (f latest a b))
         (catch Throwable e {:case label :setup-error (str e)})
         (finally (rm! t)))))

(def results
  [(with-valid :k2-selected-target-not-reached-above-valid
     (fn [latest a b]
       (not-reached! b "cohort-x" "attempt-001" "2026-09-15T11:30:00Z" :selection-target "M-history")
       {:case :k2-selected-target-not-reached-above-valid :contract "exclude marker, carry :valid"
        :result (outcome #(construction/previous! (current-for latest "2026-09-15T12:00:00Z") [a b]))}))
   (with-valid :k3-no-selection-not-reached-alongside-valid
     (fn [latest a b]
       (not-reached! b "cohort-y" "attempt-001" "2026-09-15T11:30:00Z")
       {:case :k3-no-selection-not-reached-alongside-valid :contract "exclude marker, carry :valid"
        :result (outcome #(construction/previous! (current-for latest "2026-09-15T12:00:00Z") [a b]))}))
   (with-valid :unrelated-old-shaped-construction-other-root
     (fn [latest a b]
       (not-reached! b "cohort-z" "attempt-001" "2026-09-15T10:30:00Z" :selection-target "M-other" :mission-judgment "M-other")
       {:case :unrelated-old-shaped-construction-other-root :contract "rev2: may exclude only if proven unrelated by trustworthy identity"
        :result (outcome #(construction/previous! (current-for latest "2026-09-15T12:00:00Z") [a b]))}))
   (let [t (temp) b (io/file t "b")]
     (try
       (not-reached! b "cohort-z" "attempt-001" "2026-09-15T10:30:00Z" :selection-target "M-other" :mission-judgment "M-other")
       (let [ident {:occurrence {:action/value {:type :advance-mission :target "M-fresh"} :action-at "2026-09-15T12:00:00Z"}}]
         {:case :fresh-target-with-only-unrelated-old-history :contract "initial cascade if unrelated is excludable"
          :result (outcome #(construction/previous! ident [b]))})
       (finally (rm! t))))
   (let [roots (->> (.listFiles (io/file "/tmp/c3-wm09i/realcopy")) (filter #(.isDirectory %)) (map str) sort vec)
         ident {:occurrence {:action/value {:type :advance-mission :target "M-review-synthetic-fresh-target"}
                             :action-at "2026-09-16T16:00:00Z"}}]
     {:case :real-roots-copy-fresh-target :roots (count roots)
      :result (outcome #(construction/previous! ident roots))})])

(doseq [r results] (prn (update r :result dissoc ::e)))

;; Caller classification at the pinned caller revision: data refusal vs code fault.
(def real-refusal (::e (:result (last results))))
(def classify @#'job/failure-classification)
(def transport @#'runner/transport-failure-kind)
(defn cls [e] (let [{:keys [kind failure-kind]} (classify e transport)]
                {:job-kind kind :failure-kind failure-kind
                 :repair-class (#'runner/repair-class-for failure-kind)}))
(prn {:classification :real-roots-discovery-refusal :result (when real-refusal (cls real-refusal))})
(let [t (temp) a (io/file t "a")]
  (try
    (let [latest (fixture/history-fixture! a :valid "2026-09-15T11:00:00Z" "2026-09-15T11:01:00Z")
          e (try (with-redefs [retention/validate-retention-block (fn [_] (throw (NullPointerException. "code fault in validation")))]
                   (construction/previous! (current-for latest "2026-09-15T12:00:00Z") [a]) nil)
                 (catch Throwable e e))]
      (prn {:classification :npe-inside-predecessor-validation :exception (some-> e class .getName) :result (when e (cls e))}))
    (finally (rm! t))))
(shutdown-agents)
