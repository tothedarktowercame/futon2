(ns futon2.aif.narrative-figures-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.edn :as edn]
            [clojure.xml :as xml]
            [clojure.string :as str]
            [futon2.aif.narrative-figures :as figures]))

;; Cut from run 1789964661's certificate, selected action and raw D-task rows.
(def recorded (edn/read-string (slurp "test/fixtures/run-narrative-1789964661.edn")))
(defn parse-svg [s] (xml/parse (java.io.ByteArrayInputStream. (.getBytes s "UTF-8"))))
(defn elements [root] (filter map? (tree-seq map? :content root)))
(defn by-class [root class-name]
  (filter #(some #{class-name} (str/split (get-in % [:attrs :class] "") #" ")) (elements root)))
(defn attr-number [element k] (Double/parseDouble (get-in element [:attrs k])))
(defn close? [a b] (< (abs (- a b)) 1.0e-10))

(deftest recorded-selection-has-proportional-deltas-and-aligned-probabilities
  (let [data (:selection recorded) svg (figures/selection-svg data) root (parse-svg svg)
        rows (:rows data) min-g (apply min (map :G rows))
        deltas (map #(- (:G %) min-g) rows)
        bars (vec (by-class root "delta-g"))
        widths (mapv #(attr-number % :width) bars)
        posteriors (by-class root "posterior")]
    (is (= :svg (:tag root)))
    (is (= 3 (count bars)))
    (is (= 0.0 (first widths)))
    ;; Compare actual SVG geometry to recorded G, not rounded labels or literals.
    (is (close? (/ (nth widths 1) (nth widths 2)) (/ (nth deltas 1) (nth deltas 2))))
    (is (= (vec deltas) (mapv #(attr-number % :data-value) bars)))
    (doseq [[row bar] (map vector rows posteriors)]
      (is (close? (* 190 (:posterior row)) (attr-number bar :width))))
    (is (close? (+ 580 (/ 190.0 3)) (attr-number (first (by-class root "uniform")) :x1)))
    (is (= 3 (count (by-class root "habit"))))
    (doseq [row rows] (is (str/includes? svg (:target row))))
    (is (= ["M-aif-policy-conditioned-eig"] (map #(get-in % [:attrs :data-target]) (by-class root "chosen"))))
    (is (str/includes? svg (str "min G = " min-g)))
    (is (str/includes? svg "decided by: not recorded in this run"))
    (is (= svg (figures/selection-svg data)))))

(deftest recorded-cascade-shows-negative-outcome-without-invented-arrows-or-shape
  (let [data (:cascade recorded) svg (figures/cascade-svg data) root (parse-svg svg)
        wanted (by-class root "want")
        updater (first (filter #(= ":hole/h6378c65a4012" (get-in % [:attrs :data-token])) wanted))]
    (is (= :svg (:tag root)))
    (is (= 1 (count (by-class root "pattern"))))
    (is (= 3 (count wanted)))
    (is (empty? (by-class root "need-edge")))
    (is (= "want observed-false" (get-in updater [:attrs :class])))
    (is (= "true" (get-in updater [:attrs :data-predicted])))
    (is (str/includes? svg "shape: not computed in this run (literal semilattice field)"))
    (is (= svg (figures/cascade-svg data)))))

(deftest declared-dependencies-and-missing-observations-are-distinct
  (let [data {:target "M" :shape :chain
              :patterns [{:id :P :produces #{["M" :q]}}
                         {:id :Q :produces #{["M" :want]} :guard {:needs #{["M" :q]}}}]
              :outcomes [{:token ["M" :want] :predicted true :observed {:status :missing}}
                         {:token ["M" :done] :predicted true :observed true}]}
        root (parse-svg (figures/cascade-svg data))]
    (is (= 1 (count (by-class root "need-edge"))))
    (is (= ":P" (get-in (first (by-class root "need-edge")) [:attrs :data-from])))
    (is (= ":Q" (get-in (first (by-class root "need-edge")) [:attrs :data-to])))
    (is (= 1 (count (by-class root "observation-missing"))))
    (is (= 1 (count (by-class root "observed-true"))))
    (is (empty? (by-class root "observed-false")))
    (is (str/includes? (figures/cascade-svg data) "shape: :chain"))
    (is (empty? (by-class (parse-svg (figures/cascade-svg (assoc data :wires []))) "need-edge")))
    (is (= 1 (count (by-class (parse-svg (figures/cascade-svg (assoc data :need-edges #{[:P :Q]} :wires []))) "need-edge"))))))

(deftest labels-are-xml-escaped-and-declines-are-retained
  (let [label "M-<&\"' >"
        data {:rows [{:target label :cascade-id :C1 :G 0 :posterior 1 :habit 1}]
              :chosen {:target label :id :C1} :decided-by #{:G :E}
              :declines [{:target "M-refused" :candidate :C2 :reason :no-new-wanted-token}]}
        svg (figures/selection-svg data) root (parse-svg svg)]
    (is (= label (get-in (first (by-class root "chosen")) [:attrs :data-target])))
    (is (str/includes? svg "M-refused :C2 — :no-new-wanted-token"))
    (is (str/includes? svg "decided by: :E, :G"))
    (is (= svg (figures/selection-svg data)))))

(deftest missing-values-and-ties-never-produce-invalid-geometry
  (doseq [data [{} {:rows [{:target "missing"}]}
                {:rows [{:target "a" :G 3 :posterior 0.5} {:target "b" :G 3 :posterior 0.5}]}
                {:rows [{:target "infinite" :G ##Inf :posterior 0}]}]]
    (let [svg (figures/selection-svg data) root (parse-svg svg)]
      (is (= :svg (:tag root)))
      (doseq [node (elements root) k [:x :y :x1 :y1 :x2 :y2 :width :height]
              :let [value (get-in node [:attrs k])] :when (and value (not (str/includes? value "%")))]
        (is (Double/isFinite (Double/parseDouble value)))))))
