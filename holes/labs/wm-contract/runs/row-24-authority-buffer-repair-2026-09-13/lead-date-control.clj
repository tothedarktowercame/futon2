(require '[futon2.aif.authority-buffer :as a] '[futon2.aif.authority-buffer-test :as fixture])
(let [text (str "{:date #inst " (pr-str "2026-01-01") "}")
      path (fixture/temp-file text)
      c (a/capture! {:path (str path) :format :edn :expected-sha256 (a/sha256 (java.nio.file.Files/readAllBytes path))})
      first-read (a/resolve-pointer! c [:date])
      original (.getTime ^java.util.Date (:value first-read))]
  (.setTime ^java.util.Date (:value first-read) 0)
  (let [again (a/resolve-pointer! c [:date])]
    (assert (= original (.getTime ^java.util.Date (:value again))))
    (assert (= (:source-text c) text))
    (prn {:control :date-result-mutation :retained-source-unchanged true :replay-restored true})))
