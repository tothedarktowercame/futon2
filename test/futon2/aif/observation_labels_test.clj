(ns futon2.aif.observation-labels-test
  "A-LABELS-I: real pinned C4 reads and real admission, including corrupted
   producer outputs to exercise the writer's refusal path. No admission stubs."
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is testing]]
            [futon2.aif.observation-admission :as admission]
            [futon2.aif.observation-checks :as checks]
            [futon2.aif.observation-labels :as labels]))

(def pin "3fabf0260c056c5bd09755a288cfe179b349e83b")
(def locator {:repo "futon2" :sha pin
              :path "src/futon2/aif/observation_admission.clj"
              :decl "(defn label-record"})
(def opts
  ;; The writer's code identity is an explicit input, not an inferred HEAD.
  ;; This identity names the mechanism under test; production supplies the
  ;; commit introducing observation_labels.clj (reported with this packet).
  {:mechanism-sha pin :code-sha "writer-under-test"})

(defn- real-check [] (checks/check-decl-in-file locator))

(defn- one-label [result]
  (is (= 1 (count (:written result))))
  (is (empty? (:refused result)))
  (is (= 1 (count (:store result))))
  (first (:written result)))

(deftest real-c4-result-is-admitted-with-both-verdicts
  (let [check (real-check)
        subject (labels/c4-subject check pin)
        label (one-label (labels/write-labels [check] {} opts))]
    (is (= :C4 (:check check) (:token-class subject) (:token-class label)))
    (is (= [(:evidence check)] (:evidence-pointers subject)))
    (is (= {:repo "futon2" :sha pin} (:check-cutoff subject)))
    (is (= {:author :none :enactor :none} (select-keys subject [:author :enactor])))
    (is (= true (:observed check) (:recorded label)))
    (is (= :present (:admitted label)))
    (is (= (admission/label-key subject) (:label-key label)))
    (is (= "C4/reader-form@writer-under-test" (get-in label [:admission :observer])))
    (is (= "mechanical-review@writer-under-test" (get-in label [:admission :reviewer])))))

(deftest real-disagreement-is-carried-not-overwritten
  ;; The file has `(defn label-record`, not a line starting `defn label-record`.
  ;; Splitting on opening '(' makes these the same leading token sequence;
  ;; the literal regex refuses the latter declaration head.
  (let [check (checks/check-decl-in-file (assoc locator :decl "defn label-record"))
        label (one-label (labels/write-labels [check] {} opts))]
    (is (false? (:observed check)))
    (is (false? (:recorded label)))
    (is (= :present (:admitted label)))
    (is (= false (get-in label [:admission :recorded-verdict])))))

(deftest recomputation-is-blind-and-absence-is-not-an-io-failure
  (let [subject (labels/c4-subject (real-check) pin)]
    (binding [labels/*code-sha* (:code-sha opts)]
      (is (= (labels/c4-recompute subject)
             (labels/c4-recompute (assoc subject :recorded-verdict false
                                        :author "hidden-author" :enactor "hidden-enactor"))))))
  (let [check (checks/check-decl-in-file (assoc locator :decl "(defn no-such-label-declaration"))
        label (one-label (labels/write-labels [check] {} opts))]
    (is (= :absent (:admitted label)))
    (is (false? (:recorded label))))
  (let [check (checks/check-decl-in-file (assoc locator :path "no-such-c4-file.clj"))
        result (labels/write-labels [check] {} opts)]
    (is (false? (:observed check)))
    (is (empty? (:store result)))
    (is (= :no-label (get-in result [:refused 0 :kind])))
    (is (= :insufficient (get-in result [:refused 0 :data :finding])))))

(defn- refused [result kind reason]
  (is (empty? (:store result)))
  (is (empty? (:written result)))
  (is (= 1 (count (:refused result))))
  (is (= kind (get-in result [:refused 0 :kind])))
  (when reason (is (= reason (get-in result [:refused 0 :data :reason])))))

(deftest revision-three-falsifiers-store-nothing
  (let [check (real-check)
        recompute labels/c4-recompute
        subject labels/c4-subject
        review admission/mechanical-review]
    (doseq [[falsifier corrupt kind reason]
            [[:self-truthed #(assoc % :observer (str "C4/decl-present?@" pin))
              :review-not-concur :self-truthed]
             [:cutoff-mismatch #(assoc % :cutoff {:repo "futon2" :sha "another-cutoff"})
              :review-not-concur :cutoff-mismatch]
             [:view-mismatch #(assoc % :view-digest "another-view")
              :view-digest-mismatch nil]]]
      (testing (name falsifier)
        ;; Corrupt an actual independent git recomputation, then run the real
        ;; mechanical review, admit, and label-record through write-labels.
        (with-redefs [labels/c4-recompute (fn [s] (corrupt (recompute s)))]
          (refused (labels/write-labels [check] {} opts) kind reason))))
    (testing "reviewer-missing"
      (with-redefs [admission/mechanical-review (fn [_ s a] (review nil s a))]
        (refused (labels/write-labels [check] {} opts) :reviewer-missing nil)))
    (testing "authorship-undeclared"
      (with-redefs [labels/c4-subject (fn [c sha] (dissoc (subject c sha) :author))]
        (refused (labels/write-labels [check] {} opts) :authorship-undeclared nil)))))

(deftest one-label-per-subject-and-check-mechanism
  (let [check (real-check)
        once (labels/write-labels [check check] {} opts)
        label (one-label once)
        again (labels/write-labels [check] (:store once) opts)
        changed (labels/write-labels [check] (:store once)
                                     (assoc opts :mechanism-sha
                                            "b3c4dddaed624c1a62cecc01831a8d3664e71323"))]
    (is (= [{:key (:label-key label) :already-labelled true}] (:skipped once)))
    (is (= (:skipped once) (:skipped again)))
    (is (= (:store once) (:store again)))
    (is (empty? (:written again)))
    (is (= 2 (count (:store changed))))
    (is (= 1 (count (:written changed))))
    (is (not= (:label-key label) (:label-key (first (:written changed)))))))

(deftest store-round-trips-as-identical-bytes-at-explicit-paths
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "c4-labels-" (make-array java.nio.file.attribute.FileAttribute 0)))
        first-path (io/file dir "labels.edn")
        second-path (io/file dir "again.edn")
        store (:store (labels/write-labels [(real-check)] {} opts))]
    (try
      (labels/persist! store first-path)
      (let [loaded (labels/load first-path)]
        (is (= store loaded))
        (labels/persist! loaded second-path)
        (is (= (seq (java.nio.file.Files/readAllBytes (.toPath first-path)))
               (seq (java.nio.file.Files/readAllBytes (.toPath second-path))))))
      (doseq [f [#(labels/persist! store nil) #(labels/load nil)]]
        (is (= :path-required
               (try (f) nil (catch clojure.lang.ExceptionInfo e (:kind (ex-data e)))))))
      (spit second-path "{} {}")
      (is (= :invalid-label-store
             (try (labels/load second-path) nil
                  (catch clojure.lang.ExceptionInfo e (:kind (ex-data e))))))
      (finally
        (doseq [file [first-path second-path dir]] (io/delete-file file true))))))
