;; WM build loop, packet P1 reproducer (claude-2, 2026-09-15).
;; Read-only: calls the unchanged row-7 reader over the retained :mu-post of the
;; last record of data/wm-trace/wm-trace-2026-09-12.edn, for the target that
;; record's own decision selected, current registry missions and tickets, one
;; stack-section positive control and one non-registry negative control.
;; Run from the futon2 root:  clojure -M holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/p1-reproducer/reproduce.clj
(require '[clojure.edn :as edn]
         '[clojure.java.io :as io]
         '[futon2.aif.machine-belief :as mb]
         '[futon2.aif.mission-registry :as mr])

(def trace-path "data/wm-trace/wm-trace-2026-09-12.edn")

(def record
  (with-open [r (io/reader trace-path)]
    (edn/read-string {:default (fn [_ v] v)} (last (line-seq r)))))

(def mu-post (:mu-post record))
(def selected (get-in record [:decision :action :target]))

(def missions (:missions (mr/load-missions)))
(def live-missions (mr/open-missions {:missions missions}))
(def tickets (:tickets (mr/load-tickets)))
(def live-tickets (filterv mr/live-ticket? tickets))
(def registry-ids (set (concat (map :id missions) (map :id tickets))))

;; Model identity of the row 7 -> row 8 -> row 9 witness chain
;; (runs/row-9-predicted-state/input.edn :model).
(def model {:id "wm-status-transition" :revision "row-8-declared-v1"})

(defn context [e]
  {:entity/id e :mode :single-entity :state-support mb/state-support
   :model model :policy-entities [e]})

(defn probe [role e]
  (let [r (mb/belief-state-distribution (context e) mu-post)]
    {:role role :entity/id e
     :in-registry? (contains? registry-ids e)
     :result (if (:ok r) {:ok true :posterior (get-in r [:belief-input :posteriors e])} r)}))

(def probes
  (vec (concat
        [(probe :selected-by-this-record selected)]
        (map #(probe :live-mission (:id %)) (take 3 (sort-by :id live-missions)))
        (map #(probe :live-ticket (:id %)) (take 3 (sort-by :id live-tickets)))
        [(probe :positive-control-stack-section "arxana/stack/futon-v1/leaf/2/2")
         (probe :negative-control-non-registry "X-claude-2-not-a-registry-id")])))

(prn {:schema :wm-build/p1-reproducer-v1
      :trace {:path trace-path :line :last
              :timestamp (:timestamp record)
              :decision-target selected
              :decision-type (get-in record [:decision :action :type])}
      :mu-post-domain {:count (count mu-post)
                       :registry-ids-present (count (filter registry-ids (keys mu-post)))}
      :registry-now {:missions (count missions) :live-missions (count live-missions)
                     :tickets (count tickets) :live-tickets (count live-tickets)
                     :selected-entry (some #(when (= selected (:id %))
                                              (select-keys % [:id :status-class :phase]))
                                           missions)}
      :non-progress-consumer
      {:site "scripts/futon2/report/war_machine.clj previous-selection-non-progress?"
       :mu-pre-at-target (get-in record [:mu-pre selected])
       :mu-post-at-target (get-in record [:mu-post selected])}
      :probes probes})
(shutdown-agents)
