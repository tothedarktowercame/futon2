;; Read-only join: OPEN repair tickets -> findings -> meta-injury ledger (by failure-kind).
;; The ledger canonically holds one injury per failure-kind cause; tickets are
;; occurrences. Text-grepping run records is unusable: every pinned tick-run
;; record enumerates thousands of ticket/attempt ids (e.g. the 2026-10-31-4955b6e6
;; Q1 diagnostic lists all 609 task names), so the join here is structural:
;; ticket finding :failure-kind == ledger injury :failure-kind.
;; Run: bb make-report.clj > DISCHARGED-INJURY-TICKETS-LEFT-OPEN-2026-10-31.md
(require '[clojure.string :as s])

(def canonical "/home/joe/code/futon2")
(def ledger-path (str canonical "/data/wm-meta-injuries-live-2026-10-22/ledger.edn"))

;; --- OPEN tickets + findings ---
(defn ticket-info [f]
  (let [txt (slurp f)
        finding-path (second (re-find #"(?m)Finding: \[[^]]+\]\(([^)]+)\)" txt))]
    {:ticket (s/replace (.getName f) #"\.md$" "")
     :finding-path finding-path}))

(def tickets
  (->> (file-seq (java.io.File. (str canonical "/holes/tickets")))
       (filter #(.isFile %))
       (filter #(s/starts-with? (.getName %) "T-repair-"))
       (filter #(s/ends-with? (.getName %) ".md"))
       (filter #(re-find #"\*\*Status:\*\* OPEN" (slurp %)))
       (mapv ticket-info)))

(defn finding-info [{:keys [ticket finding-path] :as t}]
  (let [f (read-string (slurp finding-path))]
    (assoc t
           :failure-kind (:failure-kind f)
           :failure-stage (:failure-stage f)
           :opened-at (:opened-at f)
           :repair-status (:repair/status f))))

(def tickets (mapv finding-info tickets))

;; --- ledger, grouped by RESOLVED injury (medic observations carry no :injury-id) ---
(def ledger (read-string (slurp ledger-path)))
(def entries (:entries ledger))
(def by-entry-id (into {} (map (juxt :entry-id identity)) entries))

(defn resolve-injury [e]
  (or (:injury-id e)
      ;; medic observations carry their injury id inside the candidate
      (get-in e [:medic-operation :request :candidate :injury-id])
      (loop [p (:predecessor e) i 0]
        (when (and p (< i 300))
          (let [pe (by-entry-id (:entry-id p))]
            (or (some-> pe resolve-injury) (recur (:predecessor pe) (inc i))))))))

(def by-injury (group-by resolve-injury entries))

(def injuries
  (keep (fn [[iid es]]
          (when iid
            (let [es (sort-by :ordinal es)
                  stated (filter :state es)
                  medic (filter #(= (:observation-kind %) :medic-repaired) es)]
              {:injury-id iid
               :kinds (vec (distinct (keep :failure-kind es)))
               :current-state (or (:state (last stated)) (if (seq medic) :medic-only :none))
               :ordinals (mapv :ordinal es)
               :medic-ordinals (mapv :ordinal medic)
               :medic-fixed-by (vec (distinct (keep :fixed-by medic)))
               :integration-commits (vec (distinct (keep :integration-commit es)))
               :detected-clicks (vec (distinct (keep :detected-click es)))})))
        by-injury))

(defn classify [inj]
  (cond (some #{:discharged} [(:current-state inj)]) "discharged"
        (seq (:medic-ordinals inj)) "medic-stamped (state still open)"
        (some #{:reopened :open :claimed :authored :reviewed :enacted :abandoned}
              [(:current-state inj)])
        (str "live (" (name (or (:current-state inj) :none)) ")")
        :else "no ledger entry"))

(def kind->injuries
  (reduce (fn [m i] (reduce #(update %1 %2 conj i) m (:kinds i))) {} injuries))

(def kind-groups
  (group-by :failure-kind tickets))

(defn short [iid] (subs iid 7 15))

;; --- emit markdown ---
(println "# Row DISCHARGED-INJURY-TICKETS-LEFT-OPEN — OPEN repair tickets joined to the meta-injury ledger")
(println)
(println (str "Generated 2026-10-31 by zai-6 (read-only evidence lane). Sources: `holes/tickets/T-repair-*.md` (Status OPEN), `data/wm-repair-obligations/findings/`, `data/wm-meta-injuries-live-2026-10-22/ledger.edn` (live snapshot while a click is running; nothing was closed or mutated)."))
(println)
(println "**Join method.** The ledger canonically holds one injury per failure-kind cause; OPEN tickets are occurrences whose finding `:failure-kind` names that cause. Attempt-id text matching against pinned tick-run records was tried and rejected: every recent run record enumerates thousands of ticket/attempt ids (the 2026-10-31-4955b6e6 record's Q1 diagnostic alone lists all 609 task names), making any text match a false positive. The table below is therefore keyed on failure-kind, with ambiguity flagged where one kind has more than one ledger injury. Nothing here closes a ticket.")
(println)
(println (format "OPEN tickets: **%d**. Ledger injuries (live-2026-10-22, by resolved injury id): **%d**." (count tickets) (count injuries)))
(println)

(println "## Summary counts")
(println)
(println "| Ledger outcome of the kind | Distinct kinds | OPEN tickets |")
(println "|---|---|---|")
(let [groups (sort-by (comp count val) > (into {} kind-groups))]
  (doseq [[outcome pred] [["discharged injury" #(= :discharged (:current-state %))]
                          ["medic-stamped injury" #(seq (:medic-ordinals %))]
                          ["live injury (open/claimed/authored/reviewed/enacted/reopened/abandoned)" #(and (empty? (:medic-ordinals %)) (not= :discharged (:current-state %)) (seq (:ordinals %)))]]
          :let [kinds (filter (fn [k] (when-let [is (kind->injuries k)] (pred (first is)))) (keys groups))
                tkts (mapcat (fn [k] (get kind-groups k)) kinds)]]
    (println (format "| %s | %d | %d |" outcome (count kinds) (count tkts))))
  (let [gs (into [] kind-groups)
        nokind (filter (fn [[k _]] (nil? (kind->injuries k))) gs)]
    (println (format "| no ledger injury of that kind | %d | %d |" (count nokind) (count (mapcat second nokind)))))
  (let [gs (into [] kind-groups)
        amb (filter (fn [[k _]] (> (count (kind->injuries k)) 1)) gs)]
    (println (format "| ambiguous (kind has >1 injury) | %d | %d |" (count amb) (count (mapcat second amb))))))
(println)

(println "## Per failure-kind join")
(println)
(println "| failure-kind | OPEN tickets | ledger injuries (short id, ordinals, state, medic, fixed-by / integration) | verdict |")
(println "|---|---|---|---|")
(defn injury-cell [i]
  (format "%s ord %s, %s%s%s"
          (short (:injury-id i))
          (pr-str (vec (take 3 (:ordinals i))))
          (name (:current-state i))
          (if (seq (:medic-ordinals i))
            (str ", medic-repaired at ord " (pr-str (:medic-ordinals i))
                 " fixed-by " (s/join "," (:medic-fixed-by i))) "")
          (if (seq (:integration-commits i))
            (str ", integration " (s/join "," (map #(subs % 0 7) (:integration-commits i)))) "")))

(doseq [[k ts] (sort-by (comp count val) > kind-groups)]
  (let [is (sort-by :ordinals (kind->injuries k))
        cell (if (seq is) (s/join "; " (map injury-cell is)) "—")
        verdict (if (empty? is) "live work, no ledger entry (repair-obligation lane only)"
                    (if (> (count is) 1)
                      "ambiguous — see injury ids"
                      (classify (first is))))]
    (println (format "| %s | %d | %s | %s |"
                     (if k (name k) "(nil kind)")
                     (count ts) cell verdict))))
(println)

(println "## Tickets belonging to discharged or medic-stamped injuries (the row's headline)")
(println)
(doseq [k (sort (keys kind-groups))
        :let [is (kind->injuries k)]
        :when (seq is)
        :let [hit (filter #(or (= :discharged (:current-state %)) (seq (:medic-ordinals %))) is)]]
  (when (seq hit)
    (doseq [i hit]
      (println (format "- kind **%s** -> injury `%s` %s: %s. OPEN tickets of this kind: %s"
                       (name k) (:injury-id i)
                       (if (= :discharged (:current-state i)) "DISCHARGED" "medic-stamped")
                       (cond (= :discharged (:current-state i))
                             (str "ledger ordinals " (pr-str (:ordinals i))
                                  ", integration commit(s) " (s/join ", " (:integration-commits i)))
                             :else
                             (str "ledger state still `" (name (:current-state i)) "`, medic-repaired at ordinal(s) "
                                  (pr-str (:medic-ordinals i)) ", fixed-by " (s/join ", " (:medic-fixed-by i))))
                       (s/join ", " (sort (map :ticket (kind-groups k)))))))))
(println)

(println "## Ledger injuries with no OPEN ticket of their kind")
(println)
(let [used (set (keys kind-groups))]
  (doseq [i (sort-by :injury-id injuries)]
    (when-not (some used (:kinds i))
      (println (format "- `%s` kind %s, state %s, ordinals %s"
                       (:injury-id i) (pr-str (:kinds i)) (name (:current-state i)) (pr-str (take 5 (:ordinals i))))))))
