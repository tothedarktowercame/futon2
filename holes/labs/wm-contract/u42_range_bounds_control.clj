#!/usr/bin/env clojure
;; U42 -- negative control for the ONE judgement in this slice's repair of
;; checks/trace_schema_compatibility.clj.
;;
;;   clojure -M holes/labs/wm-contract/u42_range_bounds_control.clj
;;
;; THE JUDGEMENT. That check compared three readers over a window fixed in its
;; source (2026-05-18..2026-08-31). The window is now derived from the store's
;; own daily files, which removes the calendar dependency but also makes
;; `read-all-traces` vs `read-trace-range` a near-identity: both enumerate the
;; same files, so on its own that equality no longer states anything the store
;; can violate. The check therefore also reads a two-day sub-window and
;; requires it to equal both the same two daily files read one at a time
;; through `read-trace` and the tail of the corpus.
;;
;; WHAT THIS SHOWS. That the sub-window assertion can FAIL -- separately at
;; each bound. It re-reads the last two store dates three ways: as shipped
;; (both bounds inclusive), with the start moved a day later (the mistake a
;; start read as exclusive would make) and with the end moved a day earlier.
;; Only the shipped reading agrees. The corpus-tail half alone would pass the
;; start case, because a shorter tail is still a tail; the read-trace half is
;; what refuses it.
;;
;; READ-ONLY: reads data/wm-trace; writes nothing.

(require '[futon2.aif.trace :as trace])
(import '(java.time LocalDate))

(defn -main [& _]
  (let [records (trace/read-all-traces)
        dates (->> (java.io.File. (str (System/getProperty "user.home")
                                       "/code/futon2/data/wm-trace"))
                   .listFiles
                   (keep #(second (re-matches #"wm-trace-(\d{4}-\d{2}-\d{2})\.edn"
                                              (.getName ^java.io.File %))))
                   sort
                   (mapv #(LocalDate/parse %)))
        tail-dates (vec (take-last 2 dates))
        [^LocalDate a ^LocalDate b] tail-dates
        by-file (vec (mapcat #(trace/read-trace :date-str (str %)) tail-dates))
        agrees? (fn [rs]
                  {:equals-read-trace? (= (vec rs) by-file)
                   :equals-corpus-tail? (= (vec rs)
                                           (vec (take-last (count rs) records)))})
        arms [["inclusive-both (shipped)" a b]
              ["start+1 (start read exclusive)" (.plusDays a 1) b]
              ["end-1 (end read exclusive)" a (.minusDays b 1)]]
        rows (for [[label s e] arms
                   :let [rs (trace/read-trace-range s e)
                         verdict (agrees? rs)]]
               (assoc verdict :arm label :window [(str s) (str e)] :n (count rs)
                      :pass? (and (:equals-read-trace? verdict)
                                  (:equals-corpus-tail? verdict))))
        rows (vec rows)]
    (println "u42-range-bounds-control")
    (println "  corpus" (count records) "records;" (count dates) "daily files;"
             "tail dates" (mapv str tail-dates) "=" (count by-file) "records")
    (doseq [{:keys [arm window n equals-read-trace? equals-corpus-tail? pass?]} rows]
      (println (format "  %-34s %s n=%-4d read-trace=%-5s corpus-tail=%-5s %s"
                       arm (pr-str window) n
                       equals-read-trace? equals-corpus-tail?
                       (if pass? "AGREES" "REFUSED"))))
    (let [ok (and (:pass? (first rows))
                  (not-any? :pass? (rest rows)))]
      (println (str "u42-range-bounds-control: " (if ok "PASS" "FAIL")
                    " (shipped bounds agree; each off-by-one bound refused)"
                    " exit-convention=0-pass/1-fail"))
      (System/exit (if ok 0 1)))))

(-main)
