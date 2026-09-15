(require '[clojure.edn :as edn] '[clojure.java.io :as io]
         '[clojure.pprint :as pp] '[futon2.aif.machine-belief :as mb])
(import '(java.security MessageDigest) '(java.math BigInteger))
(def out (first *command-line-args*))
(def trace "data/wm-trace/wm-trace-2026-09-04.edn")
(def expected "f343432d772986ddc2f7fe38107913afc997201ebae9b6240dff152e235b1120")
(def bytes-in (java.nio.file.Files/readAllBytes (.toPath (io/file trace))))
(def actual (format "%064x" (BigInteger. 1 (.digest (MessageDigest/getInstance "SHA-256") bytes-in))))
(assert (= expected actual) "trace SHA-256 mismatch")
(def entity "arxana/stack/futon-v1/leaf/2/2")
(def context {:model {:id "wm-production" :revision "2026-09-12-row-7"}
              :state-support mb/state-support :mode :single-entity
              :entity/id entity :policy-entities [entity]})
(def carried (with-open [r (java.io.PushbackReader. (io/reader trace))]
               (get-in (edn/read r) [:mu-post entity])))
(def exact (zipmap mb/state-support (map #(/ % 28) (range 1 8))))
(defn run-row [row]
  (let [result (mb/belief-state-distribution context {entity row})]
    (assert (:ok result) (pr-str result))
    (assert (= row (get-in result [:belief-input :posteriors entity])))
    result))
(def results {:context context :trace {:path trace :sha256 actual :form 0 :field :mu-post}
              :exact {:input exact :output (run-row exact)}
              :production {:input carried :output (run-row carried)}})
(assert (true? (get-in results [:exact :output :numeric-admission :exactly-normalized?])))
(assert (false? (get-in results [:production :output :numeric-admission :exactly-normalized?])))
(with-open [w (io/writer (str out "/runtime.edn"))] (binding [*out* w] (pp/pprint results)))
(with-open [w (io/writer (str out "/coordinates.tsv"))]
  (.write w "state\texact\tdouble\thex\n")
  (doseq [s mb/state-support]
    (.write w (str (name s) "\t" (get exact s) "\t" (get carried s) "\t"
                   (Double/toHexString (double (get carried s))) "\n"))))
(spit (str out "/runtime-total.txt") (str (get-in results [:production :output :numeric-admission :exact-total])))
(pp/pprint results)
