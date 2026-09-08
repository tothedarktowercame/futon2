(ns futon2.aif.preference-discovery
  "Read explicit preference claims into a proposed-C comparison.  The result is
   data for review; this namespace has no file-writing or registry-writing
  operation."
  (:require [clojure.edn :as edn]
            [clojure.string :as str]))

(defn- refuse! [reason data]
  (throw (ex-info (name reason) (assoc data :reason reason :refused? true))))

(defn- preference-claim? [claim]
  (= :preference (:claim/type claim)))

(defn- validate-claim! [support claim]
  (let [{:keys [disposition proposed-mass citation]} claim]
    (when-not (contains? support disposition)
      (refuse! :proposed-disposition-outside-support
               {:disposition disposition}))
    (when-not (and (number? proposed-mass)
                   (not (neg? proposed-mass))
                   (Double/isFinite (double proposed-mass)))
      (refuse! :invalid-proposed-preference-mass
               {:disposition disposition :proposed-mass proposed-mass}))
    (when-not (and (string? citation) (not (str/blank? citation)))
      (refuse! :preference-claim-without-citation
               {:disposition disposition}))
    claim))

(defn extract
  "Read an EDN rulings fragment and compare its explicit preference claims with
   SEEDED-C.  Non-preference observations do not become preference questions.

   Returns provenance-bearing proposed components and decision rows only for
   numeric disagreements.  It does not normalise, fill absent components, or
   mutate the supplied seed."
  [fragment {:keys [support mass]}]
  (when-not (and (set? support) (map? mass) (= support (set (keys mass))))
    (refuse! :seeded-c-not-complete {:field :seeded-c}))
  (let [claims (->> (edn/read-string fragment)
                    :claims
                    (filter preference-claim?)
                    (mapv #(validate-claim! support %)))
        duplicates (->> claims
                        (map :disposition)
                        frequencies
                        (keep (fn [[disposition n]] (when (> n 1) disposition)))
                        set)]
    (when (seq duplicates)
      (refuse! :duplicate-preference-claim
               {:dispositions (vec (sort duplicates))}))
    (let [components
          (mapv (fn [{:keys [disposition proposed-mass citation] :as claim}]
                  {:disposition disposition
                   :proposed-mass proposed-mass
                   :ruled-mass (get mass disposition)
                   :provenance [{:citation citation
                                 :claim (dissoc claim :citation)}]})
                claims)]
      {:support support
       :proposed-components components
       :decision-sheet
       (->> components
            (filter #(not= (:proposed-mass %) (:ruled-mass %)))
            (mapv #(assoc % :question :proposed-c-disagrees-with-ruled-c)))})))
