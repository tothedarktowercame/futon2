(ns futon2.aif.find-designation-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.find-designation :as fd]
            [futon2.aif.find-receipt :as find]
            [futon2.aif.find-receipt-test :as base])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(defn designation-fixture
  "The real occurrence binding, a valid designation artifact over a
  repository pattern the sample does NOT select, and the emitted result --
  all from the retained corpus the find tests use."
  []
  (let [{:keys [record captured id]} (base/sample)
        result (find/find record captured base/root nil)
        sources (into {} (map (juxt :id identity)) (:sources record))
        target-source (get sources (get-in record [:target :source]))
        occurrence {:target (get-in record [:target :id])
                    :target-source {:path (:path target-source)
                                    :sha256 (:sha256 target-source)}
                    :repository-sha256 (:repository-sha256 result)
                    :pinned-at (get-in record [:target :pinned-at])}
        repository (:repository (find/context record captured base/root))
        other (first (remove #{id} (sort (:patterns repository))))
        artifact {:schema :wm/find-designation-v1
                  :author {:id "joe" :role :designation-author}
                  :occurrence occurrence
                  :designated #{other}
                  :basis "These patterns address a different tension; the
                          recorded facts do not satisfy their antecedents."}]
    {:record record :captured captured :id id :result result
     :occurrence occurrence :artifact artifact :repository repository
     :other other}))

(defn refusal [reason f]
  (let [d (try (f) nil (catch clojure.lang.ExceptionInfo e (ex-data e)))]
    (is (map? d) (str "expected refusal " reason))
    (is (= reason (:reason d)) (str "expected " reason ", got " (:reason d)))
    (is (= :F4 (:law d)))
    d))

(defn spit-tmp [prefix body]
  (let [temp (.toFile (Files/createTempDirectory prefix (make-array FileAttribute 0)))
        path (.getAbsolutePath (io/file temp "designation.edn"))]
    (spit path (pr-str body))
    [path (fn [] (doseq [f (reverse (file-seq temp))] (Files/delete (.toPath f))))]))

(deftest control-1-correct-designation-is-accepted
  (let [{:keys [occurrence artifact repository other]} (designation-fixture)]
    (is (= {:designated #{other} :f4 :discriminating}
           (fd/resolve-designation occurrence artifact repository)))
    (is (= #{other} (fd/designation-for occurrence artifact repository)))
    ;; Round-trip through the EDN entry point.
    (let [[path cleanup] (spit-tmp "find-designation" artifact)]
      (try
        (is (= #{other} (fd/designation-for occurrence (fd/read-artifact path) repository)))
        (finally (cleanup))))))

(deftest control-2-self-supplied-author-is-rejected
  (let [{:keys [occurrence artifact repository]} (designation-fixture)]
    (doseq [role [:finder :interpreter :external-expectation-producer]]
      (refusal :self-supplied-designation
               #(fd/designation-for occurrence
                                    (assoc-in artifact [:author :role] role)
                                    repository)))
    ;; Declared-role control is not authentication: another id, same role.
    (is (set? (fd/designation-for occurrence
                                  (assoc-in artifact [:author :id] "someone-else")
                                  repository)))))

(deftest control-3-mismatched-occurrence-is-rejected
  (let [{:keys [occurrence artifact repository]} (designation-fixture)]
    (doseq [mutate [#(assoc-in % [:occurrence :target] "M-someone-elses-mission")
                    #(assoc-in % [:occurrence :target-source :sha256] (apply str (repeat 64 "0")))
                    #(assoc-in % [:occurrence :repository-sha256] "not-this-repository")
                    #(assoc-in % [:occurrence :pinned-at] "1999-01-01T00:00:00Z")]]
      (refusal :occurrence-binding-mismatch
               #(fd/designation-for occurrence (mutate artifact) repository)))
    (refusal :occurrence-required #(fd/designation-for nil artifact repository))))

(deftest control-4-non-canonical-id-is-rejected
  (let [{:keys [occurrence artifact repository]} (designation-fixture)]
    (doseq [bad [#{:not-namespaced} #{"snatch/have-a-temperament"} #{:ok/one "bad/two"}]]
      (refusal :invalid-designation-artifact
               #(fd/designation-for occurrence (assoc artifact :designated bad)
                                    repository)))
    (refusal :invalid-designation-artifact
             #(fd/designation-for occurrence (assoc artifact :designated [:fixture/a])
                                  repository))
    ;; Structural non-canonical ids are also refused at the EDN entry point.
    (let [[path cleanup] (spit-tmp "find-designation-bad"
                                  (assoc artifact :designated #{:not-namespaced}))]
      (try
        (refusal :invalid-designation-artifact #(fd/read-artifact path))
        (finally (cleanup))))))

(deftest control-5-missing-basis-is-rejected
  (let [{:keys [occurrence artifact repository]} (designation-fixture)]
    (doseq [mutate [#(dissoc % :basis) #(assoc % :basis "")
                    #(assoc % :basis "   \n  ")]]
      (refusal :invalid-designation-artifact
               #(fd/designation-for occurrence (mutate artifact) repository)))
    (refusal :invalid-designation-artifact
             #(fd/designation-for occurrence (dissoc artifact :schema) repository))))

(deftest no-artifact-is-honest-vacuity
  (let [{:keys [occurrence result repository record captured]} (designation-fixture)
        ctx (find/context record captured base/root)]
    ;; No artifact is the correct report, not a deficiency, and it is
    ;; recorded as such -- not silently skipped.
    (is (= {:designated nil :f4 :vacuous
            :vacuous-because :no-designation-supplied}
           (fd/resolve-designation occurrence nil repository)))
    (is (nil? (fd/designation-for occurrence nil repository)))
    ;; And it flows straight through the real validator.
    (is (= result (find/validate-result! ctx nil result)))
    (is (= :vacuous (:f4 result)))))

(deftest integration-with-validate-result
  (let [{:keys [occurrence artifact result repository record captured id other]}
        (designation-fixture)
        ctx (find/context record captured base/root)
        designated (fd/designation-for occurrence artifact repository)]
    ;; A designation intersecting the repository but not the selection.
    (is (contains? (:patterns repository) other))
    (is (not (contains? (set (:selected result)) other)))
    (is (= (assoc result :f4 :discriminating)
           (find/validate-result! ctx designated (assoc result :f4 :discriminating))))
    (is (= :discriminating
           (:f4 (find/find record captured base/root designated))))
    ;; A designation naming a pattern the finder DID select is refused by
    ;; the real F4 check -- the artifact constrains, it does not decorate.
    (is (= :designated-pattern-fired
           (:reason (try (find/validate-result!
                          ctx #{other id} (assoc result :f4 :discriminating))
                         nil (catch clojure.lang.ExceptionInfo e (ex-data e))))))
    ;; A designation entirely outside the repository is honest vacuity.
    (is (= :vacuous (:f4 (find/find record captured base/root #{:outside/pattern}))))))

;; claude-4's review, 2026-09-17: three situations reach :f4 :vacuous and a
;; reader of :f4 alone cannot tell them apart. The third — an authority naming
;; patterns that are not in this repository — is more likely a designation
;; aimed at another snapshot than a statement about this one, and it was
;; silently indistinguishable from nobody having been asked.
(deftest vacuity-says-why-it-is-vacuous
  (let [occ {:target "t" :target-source {:path "p" :sha256 "s"}
             :repository-sha256 "r" :pinned-at "2026-01-01T00:00:00Z"}
        art (fn [des] {:schema :wm/find-designation-v1
                       :author {:id "joe" :role :designation-author}
                       :occurrence occ :designated des :basis "stated reason"})
        repo {:patterns #{:a/b :c/d}}]
    (is (= :no-designation-supplied
           (:vacuous-because (fd/resolve-designation occ nil repo))))
    (is (= :designation-declared-empty
           (:vacuous-because (fd/resolve-designation occ (art #{}) repo))))
    (let [r (fd/resolve-designation occ (art #{:not/here}) repo)]
      (is (= :vacuous (:f4 r)))
      (is (= :designated-outside-repository (:vacuous-because r)))
      (is (= [:not/here] (:designated-not-in-repository r))))
    ;; a designation that bites carries no vacuity reason at all
    (is (nil? (:vacuous-because (fd/resolve-designation occ (art #{:a/b}) repo))))))
