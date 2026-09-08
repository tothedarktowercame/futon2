(ns futon2.aif.calibration-admission
  "Admission boundary for returned two-layer calibration artifacts.")

(def node :R12)

(defn admit!
  "Admit RETURN only when it belongs to COMMISSION and CHECK approves that
  exact return.  The returned receipt keeps both R12 lifecycle records visible
  to the process census."
  [commission return check]
  (let [commission-id (:commission/id commission)
        return-id (:return/id return)]
    (cond
      (or (not= node (:node commission))
          (not= node (:node return))
          (nil? commission-id)
          (not= commission-id (:commission/id return)))
      {:ok false
       :error/code :r12/untied-return
       :node node}

      (or (not= node (:node check))
          (nil? return-id)
          (not= return-id (:return/id check))
          (not= commission-id (:commission/id check))
          (not= :approve (:verdict check)))
      {:ok false
       :error/code :r12/unchecked-return
       :node node}

      :else
      {:ok true
       :status :admitted
       :node node
       :returned {:node :R12
                  :commission/id commission-id
                  :return/id return-id
                  :artifact (:artifact return)}
       :checked {:node :R12
                 :commission/id commission-id
                 :return/id return-id
                 :check/id (:check/id check)
                 :verdict :approve}})))
