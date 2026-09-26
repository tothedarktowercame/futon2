(ns futon2.aif.trace-strict-history-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.java.io :as io]
            [futon2.aif.trace :as trace])
  (:import [java.nio.file Files]
           [java.nio.file.attribute FileAttribute]))
(def ^:dynamic *dir* nil)
(use-fixtures :each
  (fn [f]
    (let [dir (.toFile (Files/createTempDirectory "strict-history-" (make-array FileAttribute 0)))]
      (try (binding [*dir* dir] (f))
           (finally (doseq [x (reverse (file-seq dir))] (io/delete-file x true)))))))
(defn daily [day] (io/file *dir* (str "wm-trace-2026-09-" day ".edn")))
(deftest empty-missing-and-invalid-directory
  (is (= {:status :ok :records []} (trace/read-history-strict 12 :dir (str *dir*))))
  (let [missing (io/file *dir* "missing")]
    (is (= {:status :absent :reason :trace-dir-missing :path (str missing)}
           (trace/read-history-strict 12 :dir (str missing))))))
(deftest newest-twelve-in-chronological-order
  (spit (daily "20") "{:bad") ; outside the visited window
  (spit (daily "21") (apply str (for [i (range 15)] (str (pr-str {:id i}) "\n"))))
  (is (= {:status :ok :records (mapv #(hash-map :id %) (range 3 15))}
         (trace/read-history-strict 12 :dir (str *dir*)))))
(deftest bounded-window-crosses-days
  (spit (daily "21") "{:id 1}\n{:id 2}\n")
  (spit (daily "22") "{:id 3}\n")
  (is (= [{:id 2} {:id 3}] (:records (trace/read-history-strict 2 :dir (str *dir*))))))
(deftest malformed-last-form-does-not-skip
  (spit (daily "22") "{:run/id \"good\"}\n{ :broken")
  (let [r (trace/read-history-strict 12 :dir (str *dir*))]
    (is (= :malformed-trace-record (:reason r)))
    (is (= 2 (:index r)))
    (is (= (str (daily "22")) (:path r)))
    (is (string? (get-in r [:error :class])))
    (is (not (contains? r :records))))
  (is (= [{:run/id "good"}] (trace/read-trace :dir (str *dir*) :date-str "2026-09-22"))))
(deftest unreadable-daily-path
  (.mkdir (daily "22"))
  (let [r (trace/read-history-strict 12 :dir (str *dir*))]
    (is (= :trace-read-failed (:reason r)))
    (is (= (str (daily "22")) (:path r)))
    (is (string? (get-in r [:error :message])))))
(deftest non-map-is-not-a-record
  (doseq [bad ["[]" ":futon2.aif.trace/end"]]
    (spit (daily "22") bad)
    (is (= :malformed-trace-record (:reason (trace/read-history-strict 12 :dir (str *dir*)))))))
