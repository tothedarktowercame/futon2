(ns futon2.aif.cascade-sources-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-problems :as cp]
            [futon2.aif.cascade-sources :as cs]))

(defn- tmp-dir []
  (doto (.toFile (java.nio.file.Files/createTempDirectory "cascade-sources" (make-array java.nio.file.attribute.FileAttribute 0)))
    (.deleteOnExit)))

(def source
  {:schema :wm/cascade-source-v1
   :target "M-fixture"
   :context :WM
   :beta {:value 1 :status :declared}
   :facts [:present-file :absent-file :unlocatable]
   :want [:goal]
   :locators {:present-file {:class :C3 :repo "futon2" :sha "b81afd97" :path "src/futon2/aif/construction.clj"}
              :absent-file {:class :C3 :repo "futon2" :sha "b81afd97" :path "src/futon2/aif/no_such.clj"}
              :unlocatable {:class :C2 :repo "futon2" :ns "futon2.aif.none-test"}
              :goal {:class :C3 :repo "futon2" :sha "b81afd97" :path "goal"}}
   :patterns {:p/act {:guard {:needs #{:present-file} :forbids #{}} :produces #{:goal}}}
   :interpretation-receipts {:p/act {:kind :hand-admitted}}
   :candidates [{:precedence [:p/act] :construction-receipt {:kind :hand-admitted}}]})

(deftest facts-are-observed-not-declared
  (let [dir (tmp-dir)]
    (spit (io/file dir "m.edn") (pr-str source))
    (let [s (cs/with-context-fn (cs/load-declared (.getPath dir)))]
      (is (= {:present-file true :absent-file false :unlocatable :unknown}
             (get-in s [:universes "M-fixture"])))
      (is (= 1 (count (:files s))))
      (is (= {:beta 1 :status :declared} (get-in s [:beta-by-context :WM])))
      (let [a (cp/assemble {:targets ["M-fixture"] :sources (assoc s :horizon-steps 2)})]
        (is (= 1 (count (:problems a))) (pr-str (:refusals a)))))))

(deftest empty-or-missing-dir-is-nil-and-malformed-throws
  (is (nil? (cs/load-declared "/nonexistent/cascade-sources")))
  (let [dir (tmp-dir)]
    (spit (io/file dir "bad.edn") (pr-str (dissoc source :beta)))
    (is (thrown? clojure.lang.ExceptionInfo (cs/load-declared (.getPath dir))))))
