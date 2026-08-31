#!/usr/bin/env bb
(ns checks.control-map-figure-agreement-check
  "Fail closed when Figure 4's EDN, SVG, or tracked PDF disagree.

   Exit convention: 0 = agreement, 1 = disagreement/check failure, 2 = usage."
  (:require [babashka.fs :as fs]
            [babashka.process :as process]
            [clojure.edn :as edn]
            [clojure.string :as str]))

(def defaults
  {:edges "/home/joe/code/p4ng/empirics-futon/control-map-edges.edn"
   :svg "/home/joe/code/p4ng/aif-control-map-paper.svg"
   :pdf "/home/joe/code/p4ng/aif-control-map-paper.pdf"})

(defn- numbers [s]
  (mapv parse-double (re-seq #"-?\d+(?:\.\d+)?" s)))

(defn- visible-text [s]
  (-> s
      (str/replace #"<[^>]+>" " ")
      (str/replace "&amp;" "&")
      (str/replace "&lt;" "<")
      (str/replace "&gt;" ">")
      (str/replace #"\s+" " ")
      str/trim))

(defn- parse-nodes [svg]
  (into {}
        (for [[_ attrs body] (re-seq #"(?s)<g\s+([^>]*)>(.*?)</g>" svg)
              :let [[_ class] (re-find #"class=\"([^\"]*)\"" attrs)
                    [_ x y] (re-find #"transform=\"translate\(([-\d.]+)[ ,]+([-\d.]+)\)\"" attrs)
                    [_ w h] (re-find #"<rect\s+width=\"([-\d.]+)\"\s+height=\"([-\d.]+)\"" body)
                    [_ id] (re-find #"(?s)<text\s+class=\"node-id\"[^>]*>(.*?)</text>" body)]
              :when (and (some-> class (str/split #"\s+") set (contains? "node"))
                         x y w h id)]
          [(keyword (visible-text id))
           {:x (parse-double x) :y (parse-double y)
            :w (parse-double w) :h (parse-double h)}])))

(defn- point-rect-distance [[px py] {:keys [x y w h]}]
  (let [dx (max (- x px) 0.0 (- px (+ x w)))
        dy (max (- y py) 0.0 (- py (+ y h)))]
    (Math/sqrt (+ (* dx dx) (* dy dy)))))

(defn- nearest-node [nodes point]
  (first (apply min-key second
                (map (fn [[id rect]] [id (point-rect-distance point rect)]) nodes))))

(defn- path-endpoints [d]
  (let [ns (numbers d)]
    [(subvec ns 0 2) (subvec ns (- (count ns) 2))]))

(defn- parse-paths [svg nodes class-name]
  (mapv (fn [[_ d]]
          (let [[start end] (path-endpoints d)]
            {:from (nearest-node nodes start)
             :to (nearest-node nodes end)}))
        (re-seq (re-pattern
                 (str "<path\\s+class=\\\"" class-name "\\\"\\s+d=\\\"([^\\\"]+)\\\""))
                svg)))

(defn- parse-control-paths [svg nodes]
  (let [[_ section] (re-find #"(?s)<!-- Main control path.*?-->(.*?)<!-- Support relationships" svg)
        tokens (re-seq #"(?s)<path\s+class=\"edge\"\s+d=\"([^\"]+)\"[^>]*/>|<text\s+class=\"edge-label\"[^>]*>(.*?)</text>" section)]
    (reduce (fn [paths [_ d label]]
              (if d
                (let [[start end] (path-endpoints d)]
                  (conj paths {:from (nearest-node nodes start)
                               :to (nearest-node nodes end)}))
                (update paths (dec (count paths)) assoc :label (visible-text label))))
            [] tokens)))

(defn- norm [s]
  (-> s str/lower-case (str/replace #"\s+" " ") str/trim))

(defn- pdf-text [pdf]
  (let [{:keys [exit out err]}
        (process/shell {:out :string :err :string :continue true}
                       "pdftotext" "-layout" pdf "-")]
    (when-not (zero? exit)
      (throw (ex-info "pdftotext failed" {:exit exit :stderr err})))
    (norm out)))

(defn check-files [{:keys [edges svg pdf]}]
  (let [data (edn/read-string (slurp edges))
        svg-text (slurp svg)
        nodes (parse-nodes svg-text)
        svg-control (parse-control-paths svg-text nodes)
        svg-support (parse-paths svg-text nodes "support-edge")
        expected-control (mapv #(select-keys % [:from :to :label])
                               (filter #(= :control (:kind %)) (:edges data)))
        expected-support (mapv #(select-keys % [:from :to])
                               (filter #(= :support (:kind %)) (:edges data)))
        expected-nodes (set (map keyword (:nodes data)))
        extracted (pdf-text pdf)
        svg-labels (keep :label svg-control)
        failures (vec
                  (concat
                   (when (not= expected-nodes (set (keys nodes)))
                     [{:check :edn-svg-nodes
                       :missing (vec (sort (remove (set (keys nodes)) expected-nodes)))
                       :extra (vec (sort (remove expected-nodes (keys nodes))))}])
                   (when (not= (frequencies expected-control) (frequencies svg-control))
                     [{:check :edn-svg-control-edges
                       :expected expected-control :actual svg-control}])
                   (when (not= (frequencies expected-support) (frequencies svg-support))
                     [{:check :edn-svg-support-edges
                       :expected expected-support :actual svg-support}])
                   (for [label svg-labels
                         :when (not (str/includes? extracted (norm label)))]
                     {:check :svg-pdf-edge-label :missing label})
                   (for [node (keys nodes)
                         :when (not (re-find (re-pattern
                                             (str "(?i)(?<![A-Z0-9])"
                                                  (java.util.regex.Pattern/quote (name node))
                                                  "(?![A-Z0-9])"))
                                            extracted))]
                     {:check :svg-pdf-node :missing node})))]
    {:pass? (empty? failures)
     :counts {:nodes (count nodes)
              :control-edges (count svg-control)
              :support-edges (count svg-support)
              :edge-labels (count svg-labels)}
     :failures failures}))

(defn- parse-args [args]
  (loop [opts defaults xs args]
    (if (empty? xs)
      opts
      (let [[arg & more] xs]
        (if (or (= "--negative-control" arg) (= "--negative" arg))
          (recur (assoc opts :negative-control true) more)
          (if-let [value (first more)]
            (recur (assoc opts (keyword (subs arg 2)) value) (rest more))
            (throw (ex-info "missing option value" {:option arg}))))))))

(defn- run-negative [opts]
  (let [source (slurp (:svg opts))
        mutated (str/replace-first source
                                   #"(<text\s+class=\"edge-label\"[^>]*>)[^<]+(</text>)"
                                   "$1BROKEN-LABEL$2")
        tmp (fs/create-temp-file {:prefix "control-map-negative-" :suffix ".svg"})]
    (try
      (spit (str tmp) mutated)
      (check-files (assoc opts :svg (str tmp)))
      (finally (fs/delete-if-exists tmp)))))

(defn -main [& args]
  (let [opts (parse-args args)
        report (try
                 (if (:negative-control opts)
                   (run-negative opts)
                   (check-files opts))
                 (catch Exception e
                   {:pass? false :counts {} :failures
                    [{:check :checker :message (.getMessage e)
                      :data (ex-data e)}]}))]
    (doseq [failure (:failures report)]
      (binding [*out* *err*] (println (pr-str failure))))
    ;; C16 convention (futon2 addeb05): 0 = pass, 1 = ordinary failure,
    ;; 2 = mutation slipped through. In --negative mode the CONTROL is what is
    ;; being scored, so a rejected mutation is a PASS of the control (0) and a
    ;; mutation that survives is the serious case (2).
    (if (:negative-control opts)
      (let [control-passed? (not (:pass? report))]
        (println (str "control-map-figure-agreement: negative-control "
                      (if control-passed? "PASS (mutation rejected)"
                          "FAIL (mutation slipped through)")
                      " exit-convention=0-pass/1-fail/2-mutation-slipped counts="
                      (pr-str (:counts report))))
        (System/exit (if control-passed? 0 2)))
      (do
        (println (str "control-map-figure-agreement: "
                      (if (:pass? report) "PASS" "FAIL")
                      " exit-convention=0-pass/1-fail counts=" (pr-str (:counts report))))
        (System/exit (if (:pass? report) 0 1))))))

(when (= *file* (System/getProperty "babashka.file"))
  (apply -main *command-line-args*))
