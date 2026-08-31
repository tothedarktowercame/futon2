(ns futon2.aif.enact-read-boundary-test
  (:require [clojure.java.io :as io]
            [clojure.test :refer [deftest is]]
            [futon2.aif.enact]
            [futon2.aif.fold-llm :as fold-llm]))

(defn- private-call [symbol & args]
  (apply (ns-resolve 'futon2.aif.enact symbol) args))

(defn- with-private-dir [symbol path thunk]
  (with-redefs-fn {(ns-resolve 'futon2.aif.enact symbol) path} thunk))

(deftest legacy-escrow-absence-and-unreadability-are-distinct
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "enact-escrow-test" (make-array java.nio.file.attribute.FileAttribute 0)))
        mission "mission-x"
        file (io/file dir (str mission ".edn"))]
    (try
      (is (= :absent
             (:status (with-private-dir 'escrow-dir (.getPath dir)
                        #(private-call 'escrow-wiring mission)))))
      (spit file "[")
      (let [result (with-private-dir 'escrow-dir (.getPath dir)
                     #(private-call 'escrow-wiring mission))]
        (is (= :escrow-unreadable (:status result)))
        (is (string? (get-in result [:error :exception-class])))
        (is (string? (get-in result [:error :message]))))
      (finally (.delete file) (.delete dir)))))

(deftest prose-absence-and-unreadability-are-distinct-and-consumable
  (let [dir (.toFile (java.nio.file.Files/createTempDirectory
                      "enact-prose-test" (make-array java.nio.file.attribute.FileAttribute 0)))
        pattern "pattern-x"
        file (io/file dir (str pattern ".flexiarg"))]
    (try
      (is (= :absent
             (:status (with-private-dir 'flexiarg-dir (.getPath dir)
                        #(private-call 'prose-fn pattern)))))
      (spit file "prose")
      (let [typed-failure
            (with-redefs [slurp (fn [& _]
                                  (throw (IllegalStateException. "forced prose failure")))]
              (with-private-dir 'flexiarg-dir (.getPath dir)
                #(private-call 'prose-fn pattern)))
            fold (fold-llm/llm-fold
                  [pattern] {} {:prose-fn (constantly typed-failure)})]
        (is (= :prose-unreadable (:status typed-failure)))
        (is (= "java.lang.IllegalStateException"
               (get-in typed-failure [:error :exception-class])))
        (is (= typed-failure (get-in fold [:prose-read-results pattern])))
        (is (nil? (get-in fold [:prose-read-results pattern :prose]))))
      (finally (.delete file) (.delete dir)))))
