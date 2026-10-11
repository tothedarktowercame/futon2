(ns futon2.aif.three-halves-policy
  "Controlled-SUT runtime projection of ThreeHalvesBlend.Policy coherence."
  (:require [clojure.edn :as edn]
            [futon2.aif.three-halves-square :as square])
  (:import (java.math BigInteger)
           (java.security MessageDigest)))

(def schema :wm/three-halves-policy-v1)
(def roles (set square/roles))

(defn- sha256 [s]
  (format "%064x" (BigInteger. 1 (.digest (doto (MessageDigest/getInstance "SHA-256")
                                           (.update (.getBytes (str s) "UTF-8")))))))

(defn declaration-digest [declaration]
  (sha256 (pr-str (dissoc declaration :source))))

(defn sealed-source
  "Test/adapter helper for an externally stored declaration digest. The
  returned body must itself be retained by the authority named in the pin."
  [declaration]
  (pr-str {:schema :wm/sealed-three-halves-declaration-v1
           :declaration-digest (declaration-digest declaration)}))

(defn- pin-valid? [pin declaration captured]
  (and (string? (:path pin)) (string? (:revision pin))
       (string? (:sha256 pin))
       (let [body (get captured [(:path pin) (:revision pin)])]
         (and (string? body) (= (:sha256 pin) (sha256 body))
              (try
                (= {:schema :wm/sealed-three-halves-declaration-v1
                    :declaration-digest (declaration-digest declaration)}
                   (edn/read-string body))
                (catch Exception _ false))))))

(defn- functional? [rel]
  (and (set? rel)
       (every? (fn [[_ xs]] (= 1 (count (set (map second xs)))))
               (group-by first rel))))

(defn- inverse [rel] (set (map (fn [[a b]] [b a]) rel)))
(defn- image [rel] (set (map second rel)))
(defn- domain [rel] (set (map first rel)))
(defn- lookup1 [rel x] (some (fn [[a b]] (when (= a x) b)) rel))
(defn- compose-rel [left right]
  (set (for [[a b] left [b' c] right :when (= b b')] [a c])))

(defn- carries [rel source target]
  (set (filter (fn [[a b c]]
                 (contains? (:axioms target)
                            [(lookup1 rel a) (lookup1 rel b) (lookup1 rel c)]))
               (:axioms source))))

(defn- vertex [square-index role] [square-index role])
(defn- endpoints [g id->index]
  [(vertex (get id->index (get-in g [:from :square-id])) (get-in g [:from :role]))
   (vertex (get id->index (get-in g [:to :square-id])) (get-in g [:to :role]))])

(defn- reachable? [forest source target]
  (loop [seen #{source} frontier [source]]
    (cond (= source target) true
          (empty? frontier) false
          :else (let [u (first frontier)
                      ns (for [g forest
                               :let [[a b] (:endpoints g)]
                               v (cond (= u a) [b] (= u b) [a] :else [])
                               :when (not (seen v))] v)
                      fresh (set ns)]
                  (if (fresh target) true
                      (recur (into seen fresh) (into (vec (rest frontier)) fresh)))))))

(defn- derive-forest [gluings]
  (reduce (fn [{:keys [forest required]} g]
            (let [[u v] (:endpoints g)]
              (if (reachable? forest u v)
                {:forest forest :required (conj required g)}
                {:forest (conj forest g) :required required})))
          {:forest [] :required []} gluings))

(defn- canonical-path [forest source target]
  (loop [queue [[source []]] seen #{source}]
    (when-let [[u path] (first queue)]
      (if (= u target) path
          (let [nexts (for [g forest
                            :let [[a b] (:endpoints g)]
                            [v reversed?] (cond (= u a) [[b false]]
                                                (= u b) [[a true]]
                                                :else [])
                            :when (not (seen v))]
                        [v (conj path {:gluing-id (:gluing-id g)
                                      :reversed? reversed?})])]
            (recur (into (vec (rest queue)) nexts)
                   (into seen (map first nexts))))))))

(defn- step-info [by-id {:keys [gluing-id reversed?]}]
  (when-let [g (get by-id gluing-id)]
    (let [[a b] (:endpoints g)]
      (if reversed?
        {:source b :target a :transport (:inverse g)}
        {:source a :target b :transport (:iso g)}))))

(defn- path-info [by-id path]
  (when (seq path)
    (let [infos (mapv #(step-info by-id %) path)]
      (when (and (every? some? infos)
                 (every? true? (map (fn [a b] (= (:target a) (:source b)))
                                    infos (rest infos))))
        {:source (:source (first infos)) :target (:target (last infos))
         :transport (reduce compose-rel (map :transport infos))}))))

(defn- gluing-result [g square-by-id id->index captured]
  (let [from-id (get-in g [:from :square-id]) to-id (get-in g [:to :square-id])
        from-role (get-in g [:from :role]) to-role (get-in g [:to :role])
        a (get-in square-by-id [from-id :receipt :objects from-role :theory])
        b (get-in square-by-id [to-id :receipt :objects to-role :theory])
        rel (:iso g) inv (:inverse g)
        ends (when (and (contains? square-by-id from-id) (contains? square-by-id to-id)
                        (roles from-role) (roles to-role))
               (endpoints g id->index))
        gaps (cond-> []
               (not (pin-valid? (:source g) g captured)) (conj :source-pin)
               (nil? ends) (conj :typed-endpoint)
               (and ends (= (first ends) (second ends))) (conj :typed-self-loop)
               (not (and (functional? rel) (functional? inv))) (conj :functional-bijection)
               (not= inv (inverse rel)) (conj :inverse)
               (and a (not= (:elems a) (domain rel))) (conj :total-domain)
               (and b (not= (:elems b) (image rel))) (conj :total-image)
               (and a b (not= (:axioms a) (carries rel a b))) (conj :forward-axiom-carriage)
               (and a b (not= (:axioms b) (carries inv b a))) (conj :inverse-axiom-carriage)
               (and a (not= (get-in g [:axiom-carriage :forward]) (carries rel a b)))
               (conj :forward-carriage-evidence)
               (and a b (not= (get-in g [:axiom-carriage :inverse]) (carries inv b a)))
               (conj :inverse-carriage-evidence))]
    (if (seq gaps) {:gaps gaps} {:value (assoc g :endpoints ends)})))

(defn validate
  "Validate a source-pinned policy and derive its typed forest/cycle basis."
  [receipt {:keys [captured-sources]}]
  (let [squares (:squares receipt) gluings (:gluings receipt)
        square-ids (mapv :square-id squares) gluing-ids (mapv :gluing-id gluings)
        unique-squares? (= (count square-ids) (count (set square-ids)))
        unique-gluings? (= (count gluing-ids) (count (set gluing-ids)))
        projection (:projection receipt)
        projected-square-ids (vec (mapcat :squares (:candidates projection)))
        occurrence-valid?
        (fn [s]
          (let [{:keys [candidate-id pattern-id position]} (:occurrence s)
                projected (some #(when (= candidate-id (:candidate-id %)) %) (:candidates projection))]
            (and projected (nat-int? position)
                 (= pattern-id (get (:precedence projected) position))
                 (= (:square-id s) (get (:squares projected) position)))))
        square-results (mapv (fn [s]
                               {:id (:square-id s)
                                :valid? (and (pin-valid? (:source s) s captured-sources)
                                             (occurrence-valid? s)
                                             (= :valid (:status (square/validate (:receipt s)))))})
                             squares)
        square-by-id (into {} (map (juxt :square-id identity)) squares)
        id->index (zipmap square-ids (range))
        gluing-results (mapv #(gluing-result % square-by-id id->index captured-sources) gluings)
        checked-gluings (mapv :value gluing-results)
        base-gaps (cond-> []
                    (not= schema (:schema receipt)) (conj :policy-schema)
                    (not (pin-valid? (:source receipt) receipt captured-sources)) (conj :policy-source-pin)
                    (not (and (map? projection) (some? (:target projection))
                              (vector? (:candidates projection)))) (conj :policy-projection)
                    (not (vector? squares)) (conj :stable-square-order)
                    (not (vector? gluings)) (conj :stable-gluing-order)
                    (not unique-squares?) (conj :duplicate-square-id)
                    (not unique-gluings?) (conj :duplicate-gluing-id)
                    (not= square-ids projected-square-ids) (conj :square-occurrence-population)
                    (some (complement :valid?) square-results) (conj :square-invalid)
                    (some :gaps gluing-results) (conj :gluing-invalid))]
    (if (seq base-gaps)
      {:status :refused :kind :three-halves-policy-invalid
       :missing-evidence base-gaps :gluing-findings (mapv :gaps gluing-results)}
      (let [{:keys [forest required]} (derive-forest checked-gluings)
            forest-ids (set (map :gluing-id forest)) required-ids (mapv :gluing-id required)
            obligations (:cycle-obligations receipt)
            obligation-ids (mapv :edge obligations)
            by-id (into {} (map (juxt :gluing-id identity)) checked-gluings)
            obligation-gaps
            (cond-> []
              (not= required-ids obligation-ids) (conj :cycle-obligation-population)
              (not= (count obligation-ids) (count (set obligation-ids)))
              (conj :duplicate-cycle-obligation)
              (some (fn [{:keys [edge forest-path]}]
                      (let [direct (get by-id edge)
                            [u v] (:endpoints direct)
                            canonical (canonical-path forest u v)
                            info (path-info by-id forest-path)]
                        (or (nil? direct) (empty? forest-path)
                            (some #(not (forest-ids (:gluing-id %))) forest-path)
                            (not= forest-path canonical)
                            (not= {:source u :target v :transport (:iso direct)} info))))
                    obligations)
              (conj :cycle-transport-or-canonical-path))]
        (if (seq obligation-gaps)
          {:status :refused :kind :three-halves-coherence-invalid
           :missing-evidence obligation-gaps
           :derived {:forest (mapv :gluing-id forest) :required-cycle-edges required-ids}}
          (assoc receipt :status :valid
                 :derived {:typed-forest (mapv :gluing-id forest)
                           :required-cycle-edges required-ids
                           :typed-components :derived-from-explicit-gluings}))))))

(defn valid? [receipt opts] (= :valid (:status (validate receipt opts))))
