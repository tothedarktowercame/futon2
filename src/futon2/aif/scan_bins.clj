(ns futon2.aif.scan-bins
  "Versioned finite outcomes for scalar-only raw scan channels.

   The unit interval is divided into five bins.  The last interval is closed,
   so 1 belongs to bin 4.  Values outside [0,1] remain explicit underflow or
   overflow outcomes; they are never clipped.  Values that are not finite
   numbers are typed refusals rather than observations.")

(def unit-5-v1
  {:id :wm/scan-bins-unit-5-v1
   :edges [0 0.2 0.4 0.6 0.8 1]})

(defn- finite-number?
  [x]
  (and (number? x)
       (Double/isFinite (double x))))

(defn bin
  "Return the versioned categorical outcome for scalar `x` under `schema`."
  [schema x]
  (let [{:keys [id edges]} schema]
    (if-not (finite-number? x)
      {:status :refused
       :reason :not-a-finite-number
       :value (pr-str x)}
      (let [x* (double x)
            lo (double (first edges))
            hi (double (last edges))]
        {:schema id
         :bin (cond
                (< x* lo) :underflow
                (> x* hi) :overflow
                (= x* hi) (- (count edges) 2)
                :else (->> (partition 2 1 edges)
                           (keep-indexed (fn [i [a b]]
                                           (when (and (<= (double a) x*)
                                                      (< x* (double b)))
                                             i)))
                           first))
         :value x}))))
