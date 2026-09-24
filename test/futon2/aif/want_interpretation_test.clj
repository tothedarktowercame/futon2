(ns futon2.aif.want-interpretation-test
  "D11 part 1: which wants need an interpretation, and a request that cites
  the exact criterion and retrieves with it as the query."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [futon2.aif.interpretation-request :as ireq]
            [futon2.aif.mission-criteria :as mc]
            [futon2.aif.want-interpretation :as wi])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))

(def roots (atom []))
(use-fixtures :each
  (fn [f]
    (try (f) (finally
               (doseq [root @roots file (reverse (file-seq root))] (Files/delete (.toPath file)))
               (reset! roots [])))))

(def mission-text
  (str/join "\n" ["# M-test" "" "## MAP" ""
                  "**Exit criterion:** every MAP question has a concrete answer. **Met.**" ""
                  "## DOCUMENT" ""
                  "**Exit criterion:** someone browsing the docbook can discover what this"
                  "mission built. **Not started.**" ""
                  "## ARGUE" "" "Other text that must not be the query." ""]))

(defn- fixture []
  (let [root (.toFile (Files/createTempDirectory "want-interp" (make-array FileAttribute 0)))
        _ (swap! roots conj root)
        target (io/file root "M-test.md") code (io/file root "code.py") index (io/file root "index.json")]
    (spit target mission-text) (spit code "# retriever fixture") (spit index "[]")
    {:root root
     :opts {:resolve-fn (fn [_] {:id "M-test" :path (.getCanonicalPath target)})
            :revision-fn (constantly "fixture-revision") :library-fn (constantly [])
            :retriever-specs (mapv #(assoc % :implementation (.getCanonicalPath code)
                                            :index (.getCanonicalPath index)) ireq/retrievers)}}))

(defn- document-criterion []
  (first (filter #(= "DOCUMENT" (:phase %)) (mc/criteria "M-test" mission-text))))

(deftest unproduced-wants-are-open-and-unproduced
  (is (= [:c] (wi/unproduced-wants [:a :b :c] {:a true :b false :c false}
                                   {:p/x {:produces #{:b}}})))
  (is (= [] (wi/unproduced-wants [:a] {:a true} {}))))

(deftest request-carries-token-criterion-and-context
  (let [c (document-criterion)
        r (wi/request {:target "M-test" :want (:token c) :criterion c
                       :facts {(:token c) false} :patterns {:p/x {:guard {:needs #{} :forbids #{}}
                                                              :produces #{:y} :receipt {}}}})]
    (is (= :wm/want-interpretation-request-v1 (:schema r)))
    (is (= (:token c) (get-in r [:want :token])))
    (is (str/starts-with? (get-in r [:want :criterion :stated]) "**Exit criterion:** someone browsing"))
    (is (= {:p/x {:guard {:needs #{} :forbids #{}} :produces #{:y}}}
           (get-in r [:context :interpretations])) "receipts are not copied into the request")))

(deftest request!-retrieves-with-the-criterion-as-the-query
  (let [{:keys [root opts]} (fixture)
        c (document-criterion)
        queries (atom [])
        r (wi/request! {:target "M-test" :want (:token c) :criterion c :facts {} :patterns {}}
                       root (assoc opts :retrieve-fn (fn [q] (swap! queries conj (:query q))
                                                       [{:pattern "family/example" :score 1}])))]
    (is (= 2 (count @queries)))
    (testing "the query is the criterion at its lines, not the mission's other text"
      (is (every? #(str/starts-with? % "**Exit criterion:** someone browsing the docbook") @queries))
      (is (not-any? #(str/includes? % "must not be the query") @queries)))
    (is (= [9 10] (get-in r [:retrieval :target :citations 0 :lines])))
    (is (= :want-criterion (get-in r [:retrieval :target :tension-rule])))))

(deftest a-moved-criterion-refuses
  ;; bad case: the mission was edited after the want was read; the request
  ;; must not cite whatever now sits at the old line
  (let [{:keys [root opts]} (fixture)
        c (assoc (document-criterion) :line 5)]
    (is (= :want/criterion-moved
           (try (wi/request! {:target "M-test" :want (:token c) :criterion c :facts {} :patterns {}}
                             root (assoc opts :retrieve-fn (fn [_] [])))
                nil
                (catch clojure.lang.ExceptionInfo e
                  (:interpretation/refusal (ex-data e))))))))
