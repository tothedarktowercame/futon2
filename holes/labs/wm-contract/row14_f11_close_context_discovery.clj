(require '[futon2.aif.categorical-state-close-attachment :as attachment]
         '[futon2.aif.categorical-state-observation :as observation])

(def f11-close
  {:path "/home/joe/run4/F11-production-successor-20260912-v3/cohort/run4-f11-production-successor-20260912-v3/attempt-001/007-closed.edn"
   :sha256 "6100141ed696df07769c704dfb8d6f70eef0d0a72bb04f75adfaac270b140a26"})

(prn (assoc (attachment/inspect-close (observation/read-pinned-form! f11-close))
            :scope :read-only-pinned-f11-close
            :close/source f11-close
            :not-claimed [:categorical-label :measured-pair :kernel-count]))
