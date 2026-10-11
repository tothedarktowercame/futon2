(ns futon2.aif.application-order-certificate
  "Controlled-SUT projection of ThreeHalvesBlend.ApplicationOrderCertificate."
  (:require [futon2.aif.three-halves-policy :as three-halves]))

(def schema :wm/application-order-certificate-v1)

(def ^:private implementations
  {:sut/exact-row-order-v1
   {:decode (fn [rows]
              (when (and (vector? rows)
                         (every? #(and (vector? %) (= 3 (count %))
                                       (nat-int? (first %)) (nat-int? (second %))
                                       (zero? (nth % 2))) rows)
                         (= (mapv first rows) (vec (range (count rows)))))
                (mapv second rows)))
    :accepts (fn [_construction sequence] (vector? sequence))}})

(defn- construction-edge [index-of g]
  (let [i (index-of (get-in g [:from :square-id]))
        j (index-of (get-in g [:to :square-id]))
        ri (get-in g [:from :role]) rj (get-in g [:to :role])]
    (cond (and (= :B ri) (not= :B rj)) [i j]
          (and (= :B rj) (not= :B ri)) [j i]
          :else nil)))

(defn policy-construction-order [policy]
  (let [index-of (zipmap (mapv :square-id (:squares policy)) (range))]
    (vec (distinct (keep #(construction-edge index-of %) (:gluings policy))))))

(defn- refuse [reason evidence]
  {:status :refused :kind :application-order-invalid :reason reason :evidence evidence})

(defn validate
  "Validate against an injected sealed registry. Executable decode/accepts
  results are recomputed; caller applicationSequence is evidence to compare."
  [certificate {:keys [policy policy-certificate registry captured-sources]}]
  (let [population (mapv :square-id (:squares policy))
        law-entry (some (fn [entry]
                    (when (= (select-keys entry [:identity :version :authority :receipt
                                                  :implementation-pin])
                             (select-keys certificate [:identity :version :authority :receipt
                                                        :implementation-pin]))
                      entry)) registry)
        implementation (get implementations (:implementation-pin law-entry))
        law (when (and law-entry implementation
                       (not (contains? law-entry :decode))
                       (not (contains? law-entry :accepts))
                       (three-halves/declaration-source-valid?
                        (:source law-entry) law-entry captured-sources))
              (merge law-entry implementation))
        decoded (when law ((:decode law) (:application-rows certificate)))
        expected-construction (policy-construction-order policy)
        gaps (cond-> []
               (not= schema (:schema certificate)) (conj :schema)
               (not= :valid (:status policy-certificate)) (conj :policy-not-validated)
               (not= (:policy-identity certificate) (:projection policy))
               (conj :policy-identity)
               (not= (:square-population certificate) population) (conj :square-population)
               (not= (:construction-edges certificate) expected-construction)
               (conj :construction-edges)
               (nil? law) (conj :authorized-law)
               (not (pos-int? (:version certificate))) (conj :law-version)
               (not (pos-int? (:authority certificate))) (conj :law-authority)
               (not (pos-int? (:receipt certificate))) (conj :law-receipt)
               (not (pos-int? (:selector-authority certificate))) (conj :selector-authority)
               (= (:selector-authority certificate) (:authority certificate))
               (conj :self-authorized)
               (not= decoded (:application-sequence certificate)) (conj :decoded-sequence)
               (not= (count decoded) (count (set decoded))) (conj :duplicate-square)
               (not= (set decoded) (set (range (count population))))
               (conj :exact-square-population)
               (and law (not ((:accepts law) expected-construction decoded)))
               (conj :law-refused)
               (not (three-halves/declaration-source-valid?
                     (:source certificate) certificate captured-sources))
               (conj :source-pin))]
    (if (seq gaps)
      (refuse (first gaps) gaps)
      (assoc certificate :status :valid
             :computed {:application-sequence decoded
                        :pattern-sequence
                        (mapv (fn [square-index]
                                (get-in (:squares policy)
                                        [square-index :occurrence :pattern-id]))
                              decoded)}))))
