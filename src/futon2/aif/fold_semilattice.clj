(ns futon2.aif.fold-semilattice
  "Impl #3 of the fold interface (`futon2.aif.fold`) -- the SEMILATTICE fold.

   A cascade policy is not a flat list: selected patterns have descent edges
   (BV.seq) and co-application overlaps (BV.copar). This fold consumes that
   semilattice and emits one wiring box per selected pattern, with descent and
   co-app edges carried as wires. Delta-G uses the same coverage evaluation as the
   other fold impls, so only the build axis changes."
  (:require [clojure.string :as str]
            [futon2.aif.fold-classical :as fc]
            [futon2.aif.fold-eval :as fe]))

(def ^:private generated-by
  "semilattice-fold v1 (descent=BV.seq, co_app=BV.copar)")

(defn- pattern-id [p]
  (cond
    (string? p) p
    (map? p) (or (:pattern p) (:id p) (:pattern-id p))
    :else (str p)))

(defn- pattern-stem [p]
  (last (str/split (str p) #"/")))

(defn- leaf [p]
  (-> (pattern-stem p)
      str/lower-case
      (str/replace #"[^a-z0-9]+" "-")
      (str/replace #"^-+|-+$" "")))

(defn- normalize-edge [edge]
  (cond
    (map? edge) [(:from edge) (:to edge) (:weight edge)]
    (sequential? edge) (vec edge)
    :else []))

(defn- semilattice-edges [semilattice]
  (let [descent (mapv normalize-edge (:descent semilattice))
        co-app  (mapv normalize-edge (or (:co-app semilattice)
                                         (:co_app semilattice)))]
    {:descent (filterv #(>= (count %) 2) descent)
     :co-app  (filterv #(>= (count %) 3) co-app)}))

(defn- empty-wiring [want-sig]
  {:boxes []
   :wires []
   :terminals {:in [] :out [{:port "wiring-diagram" :type "Wiring"}]}
   :for-sorry :sorry/semilattice-fold
   :id :wiring/semilattice-generated
   :generated-by generated-by
   :want-signature want-sig
   :policy-holes []})

(defn semilattice-fold
  "Impl #3 satisfying `futon2.aif.fold`: (cascade, circumstance) ->
   {:wiring :delta-g :policy-holes}. `circumstance` carries `:semilattice`
   and may carry `:want-signature`."
  [cascade circumstance]
  (let [want-sig (or (:want-signature circumstance) fc/default-want-signature)
        semilattice (:semilattice circumstance)
        patterns (mapv pattern-id cascade)
        {:keys [descent co-app]} (semilattice-edges semilattice)
        edgeful? (or (seq descent) (seq co-app))]
    (if-not (and (seq patterns) edgeful?)
      (let [wiring (empty-wiring want-sig)]
        {:wiring wiring :delta-g nil :policy-holes []})
      (let [boxes (mapv (fn [p]
                          {:id (leaf p)
                           :pattern p
                           :produces (str (leaf p) "-out")})
                        patterns)
            seq-wires (mapv (fn [[a b]]
                              {:from (leaf a)
                               :to (leaf b)
                               :type :wire/seq
                               :carries (str (leaf a) "-out")})
                            descent)
            copar-wires (mapv (fn [[a b w]]
                                {:from (leaf a)
                                 :to (leaf b)
                                 :type :wire/copar
                                 :weight w})
                              co-app)
            touched (set (concat (map (comp leaf first) descent)
                                 (map (comp leaf second) descent)
                                 (map (comp leaf first) co-app)
                                 (map (comp leaf second) co-app)))
            isolated (filterv #(not (contains? touched (leaf %))) patterns)
            policy-holes (mapv (fn [p] {:isolated-pattern p}) isolated)
            wiring {:boxes boxes
                    :wires (vec (concat seq-wires copar-wires))
                    :terminals {:in [] :out [{:port "wiring-diagram" :type "Wiring"}]}
                    :for-sorry :sorry/semilattice-fold
                    :id :wiring/semilattice-generated
                    :generated-by generated-by
                    :want-signature want-sig
                    :policy-holes policy-holes}]
        {:wiring wiring
         :delta-g (fe/coverage-delta-g wiring)
         :policy-holes policy-holes}))))
