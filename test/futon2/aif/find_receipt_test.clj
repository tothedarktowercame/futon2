(ns futon2.aif.find-receipt-test
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [clojure.test :refer [deftest is]]
            [futon2.aif.find-receipt :as find]
            [futon2.aif.interpretation-evidence :as evidence]
            [futon2.aif.interpretation-evidence-test :as fixture])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def root "/home/joe/code/futon3/library")
(defn refusal [f] (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e))))
(defn seal [x] (assoc x :sha256 (evidence/value-digest (dissoc x :sha256))))
(defn sample []
  (let [{:keys [record captured]} @fixture/fixture
        fact (get-in record [:facts 0 :id])
        sources (into {} (map (juxt :id identity)) (:sources record))
        ;; This older retained artifact predates packet 2 basename normalization.
        ;; Adapt candidates from their pinned paths, as packet 2 now does.
        record (update-in record [:retrieval :runs]
                          (fn [runs] (mapv #(update % :candidates
                                                   (fn [cs] (mapv (fn [c]
                                                                    (if-let [p (get-in sources [(:source c) :path])]
                                                                      (assoc c :pattern (str/replace (subs p (inc (count root))) #"\.flexiarg$" "")) c)) cs))) runs)))
        ;; Preserve the real nine-fact corpus and retrieval/citation shapes;
        ;; a synthetic guard gives the controls one known firing interpretation.
        record (-> record (assoc-in [:facts 0 :value] true)
                   (assoc :interpretations [(seal (assoc (first (:interpretations record)) :guard [:fact fact]))]))]
    {:record record :captured captured :fact fact :id (keyword (get-in record [:interpretations 0 :pattern]))}))

(deftest four-laws-and-independent-negative-controls
  (let [{:keys [record captured id]} (sample)
        ctx (find/context record captured root)
        r (find/find record captured root nil)]
    (is (= [id] (:selected r)))
    (is (= :vacuous (:f4 r)))
    (is (= r (find/validate-result! ctx #{} r)))
    (doseq [[law mutate] [[:F1 #(assoc % :selected [:outside/pattern])]
                          [:F2 #(assoc-in % [:receipts id :acknowledged-clause :text] "agent made this up")]
                          [:F3 #(assoc-in % [:receipts id :citation :quote] "not the captured bytes")]
                          [:F3 #(assoc-in % [:receipts id :citation] {:kind :authored-edges :tail [:outside/pattern]})]]]
      (is (= law (:law (refusal #(find/validate-result! ctx nil (mutate r)))))))
    (is (= :F4 (:law (refusal #(find/find record captured root #{id})))))
    (is (= :vacuous (:f4 (find/find record captured root #{:outside/pattern}))))
    (is (= :receipt-core-key-collision
           (:reason (refusal #(find/find record captured root nil {:receipt-extension (fn [_] {:route :invented})})))))
    (is (= r (find/validate-result! ctx nil r)))
    ;; Empty authored descent is the Lean warrant for the pattern itself.
    (is (map? (find/validate-result! ctx nil (assoc-in r [:receipts id :citation] {:kind :authored-edges :tail []}))))))

(deftest applied-interface-emits-replayable-law-witnesses
  (let [{:keys [record captured id]} (sample)
        receipt (find/applied-find record captured root nil)
        replay (find/applied-find record captured root nil)]
    (is (= :wm/find-applied-receipt-v1 (:schema receipt)))
    (is (= :runtime-validation (:mode receipt)))
    (is (= receipt replay))
    (is (= [id] (get-in receipt [:result :selected])))
    (is (every? #(= :witnessed (get-in receipt [:laws % :status]))
                [:F1 :F2 :F3 :F4]))
    (is (true? (get-in receipt [:laws :F1 :selected-within-repository])))
    (is (= #{:pattern-text} (get-in receipt [:laws :F3 :citation-kinds])))
    (is (= :vacuous (get-in receipt [:laws :F4 :designation-status])))
    (is (= (:receipt-sha256 receipt)
           (evidence/value-digest (dissoc receipt :receipt-sha256))))))

(deftest unknown-is-not-negated-into-evidence
  (doseq [g [[:fact "x"] [:not [:fact "x"]] [:not [:not [:fact "x"]]]
             [:and [:fact "x"] [:fact "yes"]] [:or [:fact "x"] [:fact "no"]]]]
    (is (= :unknown (find/guard-value {"x" :unknown "yes" true "no" false} g))))
  (is (false? (find/guard-value {"x" :unknown "no" false} [:and [:fact "x"] [:fact "no"]])))
  (let [{:keys [record captured fact id]} (sample)
        record (-> record (assoc-in [:facts 0 :value] :unknown)
                   (assoc-in [:interpretations 0 :guard] [:not [:fact fact]])
                   (update-in [:interpretations 0] seal))
        r (find/find record captured root nil) ctx (find/context record captured root)]
    (is (= [] (:selected r))) (is (= {} (:receipts r)))
    (is (= :no-pattern-addresses-this-tension (:absence r)))
    (is (some #(= id (:pattern %)) (:deferred r)))
    (is (= :F1 (:law (refusal #(find/validate-result! ctx nil (assoc r :absence nil))))))))

(deftest false-guards-are-deferred-without-receipts
  (let [{:keys [record captured id]} (sample)
        result (find/find (assoc-in record [:facts 0 :value] false) captured root nil)]
    (is (empty? (:selected result)))
    (is (nil? (get-in result [:receipts id])))
    (is (some #(and (= id (:pattern %)) (false? (:guard-value %))) (:deferred result)))))

(deftest reader-restricts-dangling-and-refuses-cycles
  (let [make-source (fn [id body] (let [path (str root "/fixture/" id ".flexiarg")
                                       bs (.getBytes body "UTF-8")]
                                   [{:id path :path path :file (str id ".source")
                                     :sha256 (evidence/sha256 bs) :revision "fixture"} bs]))
        [a ab] (make-source "a" "@flexiarg fixture/a\n@why fixture/b missing/external ;; fixture/ignored\n")
        [b bb] (make-source "b" "@flexiarg fixture/b\n")
        repository (find/read-repository root [a b] {(:file a) ab (:file b) bb})
        [cyclic cb] (make-source "b" "@flexiarg fixture/b\n@why fixture/a\n")]
    (is (= {:fixture/a #{:fixture/b}} (:stands-on repository)))
    (is (= [:missing/external] (mapv :to (:dangling repository))))
    (is (= :cyclic-repository (:reason (refusal #(find/read-repository root [a cyclic]
                                                                     {(:file a) ab (:file cyclic) cb})))))
    (is (= :source-digest-mismatch (:reason (refusal #(find/read-repository root [a b]
                                                                          {(:file a) cb (:file b) bb})))))))

(deftest ids-clause-kind-and-captured-bytes
  (let [{:keys [record captured id]} (sample)
        s (first (filter #(= (:id %) (get-in record [:interpretations 0 :source])) (:sources record)))
        original (String. ^bytes (get captured (:file s)) "UTF-8")
        changed (str/replace original #"(?m)^@flexiarg \S+" "@flexiarg wrong/directive")
        bs (.getBytes changed "UTF-8")
        record (update record :sources (fn [xs] (mapv #(if (= (:id s) (:id %)) (assoc % :sha256 (evidence/sha256 bs)) %) xs)))
        captured (assoc captured (:file s) bs)
        repository (find/read-repository root (:sources record) captured)]
    (is (contains? (:id-directive-mismatches repository) id))
    (is (some #(= :find/id-directive-mismatch (:kind %)) (:findings repository)))
    (let [membership (get-in record [:interpretations 0 :membership 0])
          index-source (first (filter #(= (:id %) (:source membership)) (:sources record)))
          old-lines (str/split-lines (String. ^bytes (get captured (:file index-source)) "UTF-8"))
          text (str (str/join "\n" old-lines) "\nwrong/directive\n")
          index-bytes (.getBytes text "UTF-8")
          altered (-> record
                      (update :sources (fn [xs] (mapv #(if (= (:id %) (:id index-source))
                                                        (assoc % :sha256 (evidence/sha256 index-bytes)) %) xs)))
                      (assoc-in [:interpretations 0 :pattern] "wrong/directive")
                      (assoc-in [:interpretations 0 :membership]
                                [{:source (:id index-source) :lines [(inc (count old-lines)) (inc (count old-lines))]
                                  :quote "wrong/directive"}])
                      (update-in [:interpretations 0] seal))]
      (is (= :directive-form-id
             (:reason (refusal #(find/find altered (assoc captured (:file index-source) index-bytes) root nil))))))
    ;; Independent clause parser refuses relabelling a genuine THEN citation as IF.
    (let [bad (-> record (assoc-in [:interpretations 0 :clauses :if] (get-in record [:interpretations 0 :clauses :then]))
                  (update-in [:interpretations 0] seal))]
      (is (= :not-authored-clause (:reason (refusal #(find/find bad captured root nil))))))))

(deftest ^:slow reference-parity-and-live-mutation-isolation
  ;; Real bb reference execution; temporary materialisation is only a parity
  ;; fixture. The production reader never creates or reads a library tree.
  (let [{:keys [record captured]} (sample)
        temp (.toFile (Files/createTempDirectory "find-reference-parity" (make-array FileAttribute 0)))
        library (.getAbsolutePath (io/file temp "library"))
        record (update record :sources
                       (fn [xs] (mapv #(if (str/starts-with? (:path %) (str root "/"))
                                        (update % :path (fn [p] (str library (subs p (count root))))) %) xs)))]
    (try
      (doseq [s (:sources record) :when (str/starts-with? (:path s) (str library "/"))]
        (io/make-parents (:path s))
        (Files/write (.toPath (io/file (:path s))) ^bytes (get captured (:file s)) (make-array java.nio.file.OpenOption 0)))
      (let [repository (find/read-repository library (:sources record) captured)
            result (find/find record captured library nil)
            script (str "(require '[find-organise :as f]) (let [r (f/read-repository " (pr-str library) " "
                        (pr-str (:sections repository)) ") result (f/find {:fires? (fn [id _] (contains? "
                        (pr-str (set (:selected result))) " id))} r)] (prn {:repository (select-keys r "
                        "[:patterns :stands-on :edges :dangling :acyclic? :id-directive-mismatches]) :result result}))")
            run (shell/sh "bb" "-cp" "/home/joe/code/futon3/checks" "-e" script)
            expected (when (zero? (:exit run)) (edn/read-string (:out run)))
            core (update result :receipts #(into (sorted-map) (map (fn [[id r]] [id (select-keys r [:if :route :warrant])])) %))]
        (is (= 0 (:exit run)) (:err run))
        (is (= (:repository expected) (select-keys repository [:patterns :stands-on :edges :dangling :acyclic? :id-directive-mismatches])))
        (is (= (:result expected) (select-keys core [:selected :receipts :absence])))
        (doseq [s (:sources record) :when (str/starts-with? (:path s) (str library "/"))] (spit (:path s) "edited live library"))
        (is (= repository (find/read-repository library (:sources record) captured)))
        (is (= result (find/find record captured library nil))))
      (finally (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f)))))))
