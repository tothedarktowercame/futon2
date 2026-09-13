(require '[futon2.aif.machine-slow-feedback-completeness :as validator]
         '[futon2.aif.machine-slow-feedback-completeness-test :as fixture]
         '[futon2.aif.machine-slow-feedback-store-v2 :as store])
(defn rebind-subject [config]
  (let [changed (#'fixture/replace-record config :completeness-subject
                 #(-> % (assoc-in [:capture :store/id] "borrowed-store")
                         (assoc-in [:capture :owner/generation] 999)))
        subject-pin (get-in changed [:roles :completeness-subject :expected-sha256])
        reviewed (#'fixture/replace-record changed :review-artifact
                   #(assoc % :subject/raw-sha256 subject-pin))
        review-pin (get-in reviewed [:roles :review-artifact :expected-sha256])]
    (reduce (fn [c role]
              (#'fixture/replace-record c role
                #(cond-> (assoc % :subject/raw-sha256 subject-pin)
                   (not= role :review-commission) (assoc :review-artifact/raw-sha256 review-pin))))
            reviewed [:review-commission :review-execution :acceptance])))
(let [{:keys [store config]} (#'fixture/fixture)]
  (try
    (let [result (validator/validate (rebind-subject config))]
      (prn {:control :coherent-borrowed-subject-capture :claimed-store "borrowed-store"
            :claimed-generation 999 :result result})
      (assert (= :join-mechanism-validated (:status result))))
    (finally (store/release! store))))
