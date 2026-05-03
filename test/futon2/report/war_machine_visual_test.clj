(ns futon2.report.war-machine-visual-test
  "Tests for War Machine visualiser layout logic.

   Tests the pure geometry and layout functions without launching Swing.
   These are the functions that determine hex sizing, positioning,
   and hit-testing — the core of the responsive layout fix."
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.report.war-machine-visual :as vis]))

;; ---------------------------------------------------------------------------
;; hex->pixel geometry
;; ---------------------------------------------------------------------------

(deftest hex->pixel-test
  (testing "origin hex (0,0) is offset by half-hex from top-left"
    (let [[x y] (#'vis/hex->pixel 0 0 40)]
      (is (> x 0) "x should be positive (centered)")
      (is (> y 0) "y should be positive (centered)")))

  (testing "increasing q moves right"
    (let [[x1 _] (#'vis/hex->pixel 0 0 40)
          [x2 _] (#'vis/hex->pixel 1 0 40)]
      (is (> x2 x1))))

  (testing "increasing r moves down"
    (let [[_ y1] (#'vis/hex->pixel 0 0 40)
          [_ y2] (#'vis/hex->pixel 0 1 40)]
      (is (> y2 y1))))

  (testing "odd rows are offset right"
    (let [[x-even _] (#'vis/hex->pixel 0 0 40)
          [x-odd _] (#'vis/hex->pixel 0 1 40)]
      (is (> x-odd x-even) "odd row should be shifted right"))))

;; ---------------------------------------------------------------------------
;; layout-bounds
;; ---------------------------------------------------------------------------

(deftest layout-bounds-test
  (testing "empty cells produce default bounds"
    (let [b (#'vis/layout-bounds [])]
      (is (= 1 (:q-span b)))
      (is (= 1 (:r-span b)))))

  (testing "single cell has zero span"
    (let [b (#'vis/layout-bounds [{:q 3 :r 4}])]
      (is (= 3 (:q-min b)))
      (is (= 3 (:q-max b)))
      (is (= 1 (:q-span b)) "span is max(1, 0) = 1")))

  (testing "multiple cells compute correct bounds"
    (let [cells [{:q 0 :r 2} {:q 5 :r 3} {:q 8 :r 6}]
          b (#'vis/layout-bounds cells)]
      (is (= 0 (:q-min b)))
      (is (= 8 (:q-max b)))
      (is (= 8 (:q-span b)))
      (is (= 2 (:r-min b)))
      (is (= 6 (:r-max b)))
      (is (= 4 (:r-span b))))))

;; ---------------------------------------------------------------------------
;; fit-hex-size (responsive scaling)
;; ---------------------------------------------------------------------------

(deftest fit-hex-size-test
  (testing "larger panel produces larger hex size"
    (let [cells [{:q 0 :r 0} {:q 8 :r 6}]
          small (#'vis/fit-hex-size cells 400 300)
          large (#'vis/fit-hex-size cells 1200 900)]
      (is (> large small))))

  (testing "minimum size is 15"
    (let [cells [{:q 0 :r 0} {:q 100 :r 100}]
          size (#'vis/fit-hex-size cells 100 100)]
      (is (>= size 15.0))))

  (testing "empty cells still produce a valid size"
    (let [size (#'vis/fit-hex-size [] 800 600)]
      (is (pos? size)))))

;; ---------------------------------------------------------------------------
;; layout-offset (centering)
;; ---------------------------------------------------------------------------

(deftest layout-offset-test
  (testing "empty cells produce zero offset"
    (is (= [0.0 0.0] (#'vis/layout-offset [] 40 800 600))))

  (testing "offset centers the grid"
    (let [cells [{:q 0 :r 0} {:q 4 :r 4}]
          hex-size 40
          [off-x off-y] (#'vis/layout-offset cells hex-size 800 600)]
      ;; The offset should be roughly half the panel minus half the grid
      (is (pos? off-x) "should have positive x offset for centering")
      (is (pos? off-y) "should have positive y offset for centering"))))

;; ---------------------------------------------------------------------------
;; assign-layout
;; ---------------------------------------------------------------------------

(def ^:private sample-graph
  "Minimal graph data for layout testing."
  {:nodes {:repos [{:id "futon3c" :label "futon3c" :workstream :stack :commits 10 :active? true}
                   {:id "futon6" :label "futon6" :workstream :mathematics :commits 5 :active? true}
                   {:id "futon7" :label "futon7" :workstream :portfolio :commits 3 :active? true}
                   {:id "npt" :label "npt" :workstream :consulting :commits 1 :active? true}]
           :sorrys [{:id :SORRY-1 :label "SORRY-1" :severity :critical}]
           :missions [{:id "M-war-machine" :label "M-war-machine" :status "active" :repo "futon0"}
                      {:id "M-blocked" :label "M-blocked" :status "blocked" :repo "futon3c"}]
           :workstreams []}
   :dynamics {:ticks [{:id :hermit-warning :fired? true}]}
   :edges {:temporal-coupling []}})

(deftest assign-layout-test
  (testing "stack layout places repos, sorrys, and ticks"
    (let [cells (#'vis/assign-layout sample-graph)
          types (set (map #(get-in % [:node :sprite-type]) cells))]
      (is (contains? types :repo))
      (is (contains? types :sorry))
      (is (contains? types :tick))))

  (testing "no two cells share the same position"
    (let [cells (#'vis/assign-layout sample-graph)
          positions (map (juxt :q :r) cells)]
      (is (= (count positions) (count (distinct positions)))
          "all cells should have unique positions"))))

(deftest assign-mission-layout-test
  (testing "mission layout places mission cells"
    (let [cells (#'vis/assign-mission-layout {:missions (get-in sample-graph [:nodes :missions])
                                              :dependency-edges []})
          mission-cells (filter #(= :mission (get-in % [:node :sprite-type])) cells)]
      (is (seq mission-cells) "should have mission cells")
      (is (= (count mission-cells) (count (get-in sample-graph [:nodes :missions]))))))

  (testing "missions occupy unique positions"
    (let [cells (#'vis/assign-mission-layout {:missions (get-in sample-graph [:nodes :missions])
                                              :dependency-edges []})
          positions (map (juxt :q :r) cells)]
      (is (= (count positions) (count (distinct positions)))
          "all mission cells should have unique positions"))))

;; ---------------------------------------------------------------------------
;; hex-hit-test
;; ---------------------------------------------------------------------------

(deftest hex-hit-test-test
  (testing "clicking on a hex center returns that cell"
    (let [cells [{:q 3 :r 3 :node {:id "test" :sprite-type :repo}}]
          hex-size 40
          [cx cy] (#'vis/hex->pixel 3 3 hex-size)
          result (#'vis/hex-hit-test cells hex-size 0 0 cx cy)]
      (is (some? result))
      (is (= "test" (get-in result [:node :id])))))

  (testing "clicking outside all hexes returns nil"
    (let [cells [{:q 3 :r 3 :node {:id "test" :sprite-type :repo}}]
          result (#'vis/hex-hit-test cells 40 0 0 9999 9999)]
      (is (nil? result))))

  (testing "hit-test respects offset"
    (let [cells [{:q 0 :r 0 :node {:id "origin" :sprite-type :repo}}]
          hex-size 40
          [cx cy] (#'vis/hex->pixel 0 0 hex-size)
          ;; With offset 100,100, the hex center is at (cx+100, cy+100)
          result (#'vis/hex-hit-test cells hex-size 100 100 (+ cx 100) (+ cy 100))]
      (is (some? result))
      (is (= "origin" (get-in result [:node :id]))))))

;; ---------------------------------------------------------------------------
;; node-detail-text
;; ---------------------------------------------------------------------------

(deftest node-detail-text-test
  (testing "repo detail includes workstream and commits"
    (let [text (#'vis/node-detail-text {:sprite-type :repo :label "futon3c"
                                        :workstream :stack :commits 42 :active? true})]
      (is (.contains text "futon3c"))
      (is (.contains text "stack"))
      (is (.contains text "42"))))

  (testing "mission detail includes status"
    (let [text (#'vis/node-detail-text {:sprite-type :mission :label "M-war-machine"
                                        :status "blocked" :repo "futon0"})]
      (is (.contains text "M-war-machine"))
      (is (.contains text "blocked"))))

  (testing "sorry detail prefers canonical id over truncated label"
    (let [text (#'vis/node-detail-text {:sprite-type :sorry
                                        :id :SORRY-market-interface
                                        :label "market-i…"
                                        :severity :critical})]
      (is (.contains text "SORRY: 🌐1-market-interface"))
      (is (.contains text "Canonical id: SORRY-market-interface"))
      (is (not (.contains text "SORRY: market-i…")))
      (is (.contains text "critical")))))
