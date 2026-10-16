;; The final bounded representative run of the repaired requirements
;; (Q7 Q5 Q6 Q8 Q4 Q2 Q9 Q10). Three consecutive production clicks through
;; run-opportunity! (the Q8/Q10 route tests' seam: select-live outer
;; selection, the real reading-family decision for the selected task, one
;; selector call per run), hermetic stores, no hot load, no live click.
;;
;; The reading cites p/a in one fragment, then p/a and p/b over one span,
;; so the selected task's construction holds three arrangements of
;; {p/a, p/b}: the reading's precedence chain, its overlap reading, and the
;; retraction's overlap (and one chain over {p/a} alone). Each run Q6
;; removes the previous run's refused choice; at least two arrangements of
;; {p/a, p/b} stay compared, so Q10 is testable on the clicks where Q8 has
;; a prior frontier, including run 3, where the excluded choice is a
;; member of a control.
;;
;; For each record: carrier verification, the run-facts export
;; (wm-run-facts), the report card (wm-report-card/generate!, with an empty
;; preregistration root and an empty snapshot so nothing outside the
;; fixture is read), and the Lean source (wm-check-run-facts). Check the
;; Lean with `lake env lean` from mathlib4.
;; Run from the futon2 worktree:
;;   REP_OUT=<dir> clojure -M:test test/fixtures/representative-run/run.clj
(require 'futon2.aif.wm.q10-arrangement-census-route-test 'clojure.string
         'futon2.aif.load-identity 'wm-report-card 'clojure.pprint)
(in-ns 'futon2.aif.wm.q10-arrangement-census-route-test)
(def out (or (System/getenv "REP_OUT") "test/fixtures/representative-run/out"))
(def reading [["p/a"] ["p/a" "p/b"]])
(def worktree (str (System/getProperty "user.dir") "/"))

(defn- edn-files [dir]
  (set (filter #(re-matches #"tick-run-record-.*\.edn" (.getName ^java.io.File %))
               (file-seq (io/file dir)))))

(defn- click-with-path! [fx dir]
  (let [before (edn-files dir)
        {:keys [record calls]} (click! fx dir)
        [path & more] (seq (remove before (edn-files dir)))]
    (assert (and path (empty? more)) "one new run record per click")
    {:record record :calls calls :path (.getPath ^java.io.File path)}))

(defn- carriers [record]
  (into (sorted-map)
        (for [[k f] {:q2 q2/verify-record :q8 q8/verify-record
                     :q9 q9/verify-record :q10 q10/verify-record}]
          [k (let [v (f record)]
               (cond-> {:status (:status v)}
                 (:reason v) (assoc :reason (:reason v))
                 (get-in v [:carrier :status]) (assoc :carrier-status (get-in v [:carrier :status]))))])))

(defn- report! [label {:keys [record calls path]}]
  (let [dir (io/file out label)
        export (facts/facts-for-record (facts/read-edn path) path {})
        json-export (json/parse-string (json/generate-string export))
        empty-prereg (route/temp-dir "rep-prereg-")
        card-paths (wm-report-card/generate!
                    path {:output-dir (.getPath (io/file dir "card")) :snap {}
                          :preregistration-root (.getPath empty-prereg)
                          :cards-by-run {}})
        lean (io/file out (str "Rep" label ".lean"))]
    (.mkdirs dir)
    (spit (io/file dir "run-facts.json") (json/generate-string export {:pretty true}))
    (spit lean (lean-term/lean-source json-export (str "rep" label)))
    (let [c (:q10 record)
          summary {:label label :run-id (:run/id record) :selector-calls calls
                   :chosen (get-in record [:decision :chosen])
                   :carriers (carriers record)
                   :q10 {:status (:status c) :counts (:counts c)
                         :population (mapv #(select-keys % [:status :reason :patterns]) (:population c))
                         :pairs (mapv #(select-keys % [:status :reason :distinguished :g :g-differs]) (:pairs c))
                         :selected-in-control (:selected-in-control c)}
                   :q6 (facts/q6-facts record)
                   :verdicts (mapv #(select-keys % [:requirement :status :reason])
                                   (card/verdict-rows export))
                   :card card-paths :lean (.getPath lean)}]
      (spit (io/file dir "summary.edn") (with-out-str (clojure.pprint/pprint summary)))
      (clojure.pprint/pprint summary))))

(with-redefs [futon2.aif.load-identity/required-sources
              (into {} (map (fn [[n p]] [n (clojure.string/replace p "/home/joe/code/futon2/" worktree)]))
                    futon2.aif.load-identity/required-sources)]
  (futon2.aif.hermetic-repair-fixture/with-hermetic-stores
   (fn []
     (futon2.test-support.runner-fixture/with-hermetic-traces
      (fn []
        (binding [futon2.aif.full-loop-runner/*wm-status-reporting?* false]
          (route/with-empty-learning-ledger
           (fn []
             (let [fx (route/fixture reading) dir (route/temp-dir "rep-stream-")
                   runs (vec (repeatedly 3 #(click-with-path! fx dir)))]
               (doseq [[i run] (map-indexed vector runs)]
                 (report! (str "Run" (inc i)) run)))))))))))
(shutdown-agents)
