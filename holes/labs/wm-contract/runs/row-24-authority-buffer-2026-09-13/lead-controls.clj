(require '[futon2.aif.authority-buffer :as a]
         '[futon2.aif.authority-buffer-test :as fixture])
(defn capture [text format]
 (let [p (fixture/temp-file text)]
  (a/capture! {:path (str p) :format format :expected-sha256 (a/sha256 (java.nio.file.Files/readAllBytes p))})))
(doseq [s [(str "{" (pr-str "a") ":1} {" (pr-str "b") ":2}")
           (str "{" (pr-str "a") ":1," (pr-str "a") ":2}")]]
 (try (prn {:control :json :input s :value (:value (capture s :json))})
  (catch Exception e (prn {:control :json :refusal (ex-data e)}))))
(prn {:control :nil-pointer :result (a/resolve-pointer! (capture "{:a 1}" :edn) [nil :missing])})
(let [c (capture (str "{:date #inst " (pr-str "2026-01-01") "}") :edn)]
 (prn {:control :mutable-before :value (:value c)})
 (.setTime ^java.util.Date (get-in c [:value :date]) 0)
 (prn {:control :mutable-after :result (a/resolve-pointer! c [:date])}))
