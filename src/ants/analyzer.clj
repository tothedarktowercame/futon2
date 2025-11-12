(ns ants.analyzer)
;; ---- live analyzer ---------------------------------------------------------

(defonce ^:private live-state*
  (atom {:window 50        ;; ticks to keep
         :ticks  []        ;; sorted tick list in window
         :frame  {}}))     ;; {tick {id {metric value}}}

(defn- norm-mode [x]
  (cond
    (keyword? x) x
    (string?  x) (keyword x)
    :else        nil))

(defn- update-window [{:keys [window ticks frame] :as st}
                      {:keys [tick ids rows]}]
  (let [tick-data (into {}
                        (for [id ids]
                          [id (into {}
                                    (for [[metric m] rows]
                                      [(keyword (name metric)) (get m id)]))]))
        ticks'    (let [tks (conj (vec ticks) tick)]
                    (if (> (count tks) window) (subvec tks (- (count tks) window)) tks))
        frame'    (-> frame
                      (assoc tick tick-data)
                      (select-keys ticks'))]
    (assoc st :ticks ticks' :frame frame')))

(defn- ewma [alpha prev x] (+ (* (- 1.0 alpha) (or prev x 0.0))
                              (* alpha (or x 0.0))))

(defonce ^:private live-metrics*
  (atom {:alpha 0.15
         :G_ema  nil
         :dep_ema nil
         :ing_ema nil
         :gath_ema nil
         :phm_share_ema nil}))

(defn- summarize-tick [{:keys [frame]} t]
  (let [ids  (-> frame (get t) keys)
        getf   (fn [id k] (get-in frame [t id k]))
        acts   (map #(getf % :act) ids)
        phm%   (if (seq ids) (/ (count (filter #{"phm"} acts)) (double (count ids))) 0.0)
        Gmean  (let [xs (keep #(getf % :G) ids)] (when (seq xs) (/ (reduce + xs) (double (count xs)))))
        dep    (reduce + 0.0 (keep #(getf % :dep) ids))
        ing    (reduce + 0.0 (keep #(getf % :ing) ids))
        gath   (reduce + 0.0 (keep #(getf % :gath) ids))
        modes  (frequencies (keep #(getf % :mode) ids))
        ;; modes  (->> ids
        ;;             (map #(norm-mode (getf % :mode)))
        ;;             (remove nil?)
        ;;             frequencies)
        ]

    {:tick t
     :phm-share phm%
     :G-mean Gmean
     :dep-sum dep :ing-sum ing :gath-sum gath
     :modes modes}))

(defn- update-ewmas! [{:keys [alpha] :as st} {:keys [G-mean dep-sum ing-sum gath-sum phm-share]}]
  (swap! live-metrics*
         (fn [m]
           (-> m
               (assoc :G_ema        (ewma alpha (:G_ema m)        (or G-mean 0.0)))
               (assoc :dep_ema      (ewma alpha (:dep_ema m)      dep-sum))
               (assoc :ing_ema      (ewma alpha (:ing_ema m)      ing-sum))
               (assoc :gath_ema     (ewma alpha (:gath_ema m)     gath-sum))
               (assoc :phm_share_ema (ewma alpha (:phm_share_ema m) phm-share))))))

;; spiral detector: no (gath|dep|ing) for N ticks
(defn- starvation-spiral? [{:keys [ticks frame]} N]
  (let [ts (take-last N ticks)]
    (every?
     (fn [t]
       (let [vals (vals (get frame t))]
         (and (every? #(zero? (double (or (:gath %) 0))) vals)
              (every? #(zero? (double (or (:dep  %) 0))) vals)
              (every? #(zero? (double (or (:ing  %) 0))) vals))))
     ts)))

;; wire into your existing analyze-and-print!:
;; after you build {:ticks ... :frame ...}, do:
;;   (let [s (summarize-tick @live-state*)] (update-ewmas! {:alpha 0.15} s) (print-line s))
(defn- print-line [{:keys [tick modes] :as s}]
  (let [{:keys [G_ema dep_ema gath_ema ing_ema phm_share_ema]} @live-metrics*
        out  (long (get modes :outbound 0))
        home (long (get modes :homebound 0))
        main (long (get modes :maintain 0))]
    (println
     (format (str "Tick %3d | G_ema %.3f | dep_ema %.2f | gath_ema %.2f | "
                  "ing_ema %.2f | phm%%_ema %.2f | modes={out:%d home:%d main:%d}")
             tick (or G_ema 0.0) dep_ema gath_ema ing_ema phm_share_ema
             out home main))))

(defn- analyze-and-print! [{:keys [ticks frame] :as st} {:keys [tick] :as batch}]
  (let [t tick]
    (when (and t (contains? frame t))
      (let [ids      (-> frame (get t) keys sort)
            act-of   (fn [id] (get-in frame [t id :act]))
            cargo-of (fn [id] (get-in frame [t id :cargo]))
            mode-of  (fn [id] (norm-mode (get-in frame [t id :mode])))
            dep-of   (fn [id] (get-in frame [t id :dep]))
            white-of (fn [id] (get-in frame [t id :white]))
            ;; stalls: consecutive tail of (ret + cargo>0 + dep=0)
            tail-len (fn [id]
                       (loop [ts (reverse ticks) k 0]
                         (if (empty? ts) k
                             (let [tt (first ts)
                                   ret? (= "ret" (get-in frame [tt id :act]))
                                   c   (double (or (get-in frame [tt id :cargo]) 0))
                                   d   (double (or (get-in frame [tt id :dep])   0))]
                               (if (and ret? (> c 0) (= d 0))
                                 (recur (rest ts) (inc k))
                                 k)))))]
        ;; quick per-tick summary
        (let [prevt (when (> (count ticks) 1) (nth ticks (- (count ticks) 2)))
              transitions (when prevt
                            (let [ids-prev (-> frame (get prevt) keys set)]
                              (->> ids
                                   (keep (fn [id]
                                           (when (contains? ids-prev id)
                                             (let [m0 (norm-mode (get-in frame [prevt id :mode]))
                                                   m1 (norm-mode (get-in frame [t id :mode]))]
                                               (when (and m0 m1 (not= m0 m1))
                                                 [m0 m1])))))
                                   frequencies)))

              white-share (when (seq ids)
                            (/ (count (filter #(= 1 (white-of %)) ids))
                               (double (count ids))))
              stalls      (into {}
                                (for [id ids] [id (tail-len id)]))
              stalled     (filter (fn [[_ k]] (>= k 5)) stalls)
              stalled-by-mode (when (seq stalled)
                                (->> stalled
                                     (map (fn [[id k]] [(mode-of id) 1]))
                                     (remove (comp nil? first))
                                     frequencies))

              acts        (frequencies (map act-of ids))
              s (summarize-tick st t)]

          (update-ewmas! {:alpha 0.15} s)
          (print-line s)
          (when (and (>= (count ticks) 12)
                     (starvation-spiral? {:ticks ticks :frame frame} 12))
            (println "  ⚠ spiral: no gath/dep/ing in last 12 ticks"))

          (println (format "tick %d | white-share=%.2f | acts=%s"
                           t (or white-share 0.0) acts))
          (when (seq transitions)
            (println "  mode transitions:" transitions))

          (when (seq stalled)
            (println "  stalls (ret&cargo no-dep >=5):" (into {} stalled))
            (when (seq stalled-by-mode)
              (println "  stalls by mode:" stalled-by-mode)))

          (doseq [id ids]
            (when (and (= "ret" (act-of id))
                       (> (double (or (cargo-of id) 0)) 0)
                       (= 0.0 (double (or (dep-of id) 0))))
              (println "   ↳" id (format ": RETURNING with cargo %.2f but no deposit this tick." (cargo-of id))))))))))

;; spiral detector: no (gath|dep|ing) for N ticks

(defn start-live-aif-analysis!
  "Attach a tap listener that ingests :aif/pivot batches and prints live stats.
   Options: {:window N} (default 50). Returns a 0-arg fn to stop."
  ([]
   (start-live-aif-analysis! {}))
  ([{:keys [window] :or {window 50}}]
   (swap! live-state* assoc :window window :ticks [] :frame {})

   ;; AFTER
   (let [listener (fn [batch]
                    (when (and (map? batch) (= (:type batch) :aif/pivot))
                      (swap! live-state* update-window batch)
                      (analyze-and-print! @live-state* batch)))]
     (add-tap listener)

     (fn stop! []
       (remove-tap listener)))))
