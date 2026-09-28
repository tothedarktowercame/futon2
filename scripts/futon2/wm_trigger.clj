(ns futon2.wm-trigger
  "The trigger of a War Machine run, read from the environment. Requires
  nothing, so its readers load neither the tick nor the report.")

(defn trigger-from-env
  "Which clock fired this run: FUTON_WM_TRIGGER as a keyword (:wallclock-cron,
  :duree-click-*, ...), else :unspecified. The tick's version stamp and the
  flight path's selection record both read it here."
  ([] (trigger-from-env #(System/getenv %)))
  ([getenv]
   (if-let [t (getenv "FUTON_WM_TRIGGER")]
     (keyword t)
     :unspecified)))
