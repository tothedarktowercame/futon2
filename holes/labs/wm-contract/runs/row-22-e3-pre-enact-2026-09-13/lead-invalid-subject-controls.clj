(require '[futon2.aif.machine-pre-enact-authorization :as e3]
         '[futon2.aif.machine-pre-enact-authorization-test :as fixture])
(def records {:pending fixture/pending :verdict fixture/verdict :review fixture/review})
(def missing-subject
  (assoc fixture/subject :candidate/occurrence-id nil :action nil :construction nil
         :field-pins [] :claim/id nil :artifact/ref nil :trace/id nil))
(def cases
  {:missing-subject
   (-> (into {} (map (fn [[k v]] [k (assoc v :subject missing-subject)]) records))
       (update :review assoc :claim/id nil :artifact/ref nil :producer-trace/id nil
               :review-trace/id nil)
       (update :verdict assoc :review-trace/id nil))
   :mixed-scope (-> records (assoc-in [:review :scope] :production)
                           (assoc-in [:verdict :scope] :production))})
(doseq [[label rs] cases]
  (let [result (e3/verify-pre-enact (#'fixture/config rs))]
    (assert (= :mechanism-authorized (:decision result)))
    (prn {:case label :decision (:decision result) :subject (:subject result)
          :scope (:scope result)})))
