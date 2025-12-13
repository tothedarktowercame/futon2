(ns tools.trace-tick
  (:require [ants.aif.core :as core]
            [clojure.java.io :as io]
            [clojure.pprint :as pprint]
            [clojure.string :as str]))

(def trace-dir (io/file "doc" "trace"))

(defn- ensure-trace-dir! []
  (.mkdirs trace-dir))

(defn- baseline-world []
  {:grid {:size [7 7]
          :max-food 5.0
          :max-pher 4.0
          :cells {[2 2] {:food 4.5 :pher 1.5}
                  [2 3] {:food 2.8 :pher 1.0}
                  [3 2] {:food 1.2 :pher 2.0}
                  [1 2] {:food 0.4 :pher 0.3}
                  [2 1] {:food 0.3 :pher 0.1}
                  [4 2] {:food 0.6 :pher 1.8}
                  [4 3] {:food 4.2 :pher 2.2}
                  [5 3] {:food 3.8 :pher 0.9}
                  [5 4] {:food 0.5 :pher 0.7}
                  [0 0] {:home :aif :food 0.0 :pher 0.0}
                  [6 6] {:home :classic :food 0.0 :pher 0.0}}}
   :homes {:aif [0 0]
           :classic [6 6]}
   :colonies {:aif {:reserves 2.5}
              :classic {:reserves 2.0}}
   :config {:hunger {:queen {:initial 4.0}}}})

(defn- baseline-ant []
  {:species :aif
   :loc [2 2]
   :cargo 0.35
   :ingest 0.18
   :recent-gather 0.22
   :visit-counts {[2 2] 5
                  [2 3] 2
                  [3 2] 1}
   :mu {:h 0.62
        :pos [2 2]
        :goal [5 5]}
   :prec {:tau 1.1}})

(defn- format-ranking [policy]
  (->> (:policies policy)
       (map (fn [[action {:keys [p G]}]]
              {:action action
               :p (double p)
               :G (double G)}))
       (sort-by (comp - :p))
       vec))

(defn- trace-snapshot []
  (let [world (baseline-world)
        ant (baseline-ant)
        opts {:max-steps 4 :alpha 0.45 :beta 0.28}
        {:keys [observation perception diagnostics policy] :as step} (core/aif-step world ant opts)
        snapshot {:world (select-keys world [:grid :homes])
                  :observation observation
                  :perception {:trace (:trace perception)
                               :mu (select-keys (:mu perception) [:pos :goal :h :sens])
                               :prec (select-keys (:prec perception) [:tau :Pi-o])}
                  :policy {:action (:action policy)
                           :tau (:tau policy)
                           :ranking (format-ranking policy)}
                  :diagnostics diagnostics
                  :ant (select-keys (:ant step) [:mode :loc :mu :prec :last-trace])}]
    snapshot))

(defn- scale-value [value {:keys [min max]}]
  (let [mi (double (or min 0.0))
        ma (double (or max 1.0))
        span (clojure.core/max 1e-9 (- ma mi))
        clamped (-> (double (or value mi))
                    (clojure.core/max mi)
                    (clojure.core/min ma))]
    (/ (- clamped mi) span)))

(def svg-width 520)
(def svg-height 260)
(def svg-margin 30)

(def trace-series
  [{:key :tau :color "#d9480f" :label "tau" :range {:min 0.2 :max 2.6}}
   {:key :h :color "#1864ab" :label "h" :range {:min 0.0 :max 1.0}}
   {:key :error :color "#5f3dc4" :label "error" :range {:min 0.0 :max 0.5} :dash "6 4"}])

(defn- series->polyline [trace {:keys [key color dash range]}]
  (let [count (max 1 (dec (count trace)))
        inner-width (- svg-width (* 2 svg-margin))
        inner-height (- svg-height (* 2 svg-margin))
        step (if (zero? count) 0 (/ inner-width count))
        points (->> trace
                    (map-indexed (fn [idx entry]
                                   (let [norm (scale-value (get entry key) range)
                                         x (double (+ svg-margin (* idx step)))
                                         y (double (+ svg-margin (- inner-height (* norm inner-height))))]
                                     (format "%.1f,%.1f" x y))))
                    (str/join " "))]
    (format "<polyline fill='none' stroke='%s' stroke-width='2' %s points='%s' />"
            color
            (if dash (format "stroke-dasharray='%s'" dash) "")
            points)))

(defn- legend-items []
  (->> trace-series
       (map-indexed (fn [idx {:keys [label color]}]
                      (let [y (+ svg-margin 15 (* idx 16))]
                        (format "<g><rect x='%d' y='%d' width='12' height='3' fill='%s' />\n                               <text x='%d' y='%d' font-size='11' fill='#343a40'>%s</text></g>"
                                (- svg-width 110)
                                y
                                color
                                (- svg-width 90)
                                (+ y 3)
                                label))))
       (str/join "\n")))

(defn- render-trace-svg [trace]
  (let [axis (format "<line x1='%d' y1='%d' x2='%d' y2='%d' stroke='#868e96' stroke-width='1' />"
                     svg-margin
                     (- svg-height svg-margin)
                     (- svg-width svg-margin)
                     (- svg-height svg-margin))
        polylines (map #(series->polyline trace %) trace-series)
        legend (legend-items)]
    (format "<svg xmlns='http://www.w3.org/2000/svg' width='%d' height='%d' viewBox='0 0 %d %d'>\n  <rect width='100%%' height='100%%' fill='#f8f9fa'/>\n  %s\n  %s\n  %s\n</svg>"
            svg-width svg-height svg-width svg-height
            axis
            (str/join "\n" polylines)
            legend)))

(defn- write-edn! [file snapshot]
  (with-open [w (io/writer file)]
    (binding [*print-namespace-maps* false]
      (pprint/pprint snapshot w))))

(defn- write-svg! [file trace]
  (spit file (render-trace-svg trace)))

(defn -main [& _]
  (ensure-trace-dir!)
  (let [snapshot (trace-snapshot)
        edn-file (io/file trace-dir "single_tick.edn")
        svg-file (io/file trace-dir "single_tick.svg")]
    (write-edn! edn-file snapshot)
    (write-svg! svg-file (get-in snapshot [:perception :trace]))
    (println "wrote" (.getPath edn-file) "and" (.getPath svg-file))))
