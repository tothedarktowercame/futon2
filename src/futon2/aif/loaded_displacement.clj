(ns futon2.aif.loaded-displacement
  "RUNNER-DRIFT-I: which loaded futon2 namespaces hold vars whose recorded
  source line no longer holds their definition in the file on the classpath.

  Membership is every loaded namespace named futon2.aif.* or futon2.report.*
  (all-ns at check time, no list). For each, the vars with :file/:line
  metadata whose :file is the namespace's primary file (the most common one)
  are checked against that file as the classpath resolves it. A var is in
  place when the form starting at its :line reads as a list naming it among
  its top-level elements (defrecord/deftype also name ->X and map->X); when
  that form does not read, the var's name on its :line is the fallback.

  The limit: this detects DISPLACEMENT, a var whose line now holds something
  else, which is how an edited-but-not-reloaded file usually shows. It does
  not detect content drift: an edit that leaves every var's start line in
  place reads :current. Report only; nothing refuses on it."
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [clojure.lang LineNumberingPushbackReader]
           [java.io StringReader]))

(def ^:private ns-pattern #"^futon2\.(aif|report)\.")

(defn loaded-namespaces
  "The loaded namespaces the report covers, from all-ns now."
  []
  (filter #(re-find ns-pattern (str (ns-name %))) (all-ns)))

(defn- line-offsets [^String text]
  (loop [i 0 acc [0]]
    (let [j (.indexOf text "\n" (int i))]
      (if (neg? j) acc (recur (inc j) (conj acc (inc j)))))))

(defn- form-at
  "The form that starts at 1-based LINE of TEXT, read with *ns* bound to NS
  (so ::alias keywords and syntax-quote resolve), or ::unreadable."
  [ns text offsets line]
  (if-let [off (get offsets (dec line))]
    (try
      (binding [*ns* ns *read-eval* false
                *default-data-reader-fn* tagged-literal]
        (read {:eof ::unreadable :read-cond :allow}
              (LineNumberingPushbackReader. (StringReader. (subs text off)))))
      (catch Throwable _ ::unreadable))
    ::unreadable))

(defn- names-in [form]
  (when (seq? form)
    (into #{} (mapcat (fn [x]
                        (when (symbol? x)
                          (let [n (name x)] [n (str "->" n) (str "map->" n)]))))
          form)))

(defn- in-place? [ns text lines offsets v]
  (let [{:keys [line name]} (meta v)
        nm (str name)
        form (form-at ns text offsets line)]
    (if (= ::unreadable form)
      (str/includes? (get lines (dec line) "") nm)
      (contains? (or (names-in form) #{}) nm))))

(defn namespace-displacement
  "{:status :current|:displaced|:no-resource|:no-vars ...} for NS. RESOURCE
  maps a classpath-relative file to its text, or nil (default: the
  classpath)."
  ([ns] (namespace-displacement ns #(some-> (io/resource %) slurp)))
  ([ns resource]
   (let [vars (filter #(let [m (meta %)] (and (:file m) (:line m))) (vals (ns-interns ns)))
         file (some->> (seq vars) (map (comp :file meta)) frequencies (apply max-key val) key)
         text (when file (resource file))]
     (cond
       (empty? vars) {:status :no-vars}
       (nil? text) {:status :no-resource :file file}
       :else
       (let [mine (filter #(= file (:file (meta %))) vars)
             lines (vec (str/split-lines text))
             offsets (line-offsets text)
             bad (vec (for [v (sort-by (comp :line meta) mine)
                            :when (not (in-place? ns text lines offsets v))]
                        [(str (:name (meta v))) (:line (meta v))]))]
         (if (seq bad)
           {:status :displaced :file file :mismatched (count bad) :of (count mine)
            :sample (vec (take 3 bad))}
           {:status :current :of (count mine)}))))))

(defn report
  "{ns-symbol → namespace-displacement} for NAMESPACES (default: every loaded
  futon2.aif.*/futon2.report.* namespace)."
  ([] (report (loaded-namespaces)))
  ([namespaces]
   (into (sorted-map) (map (fn [ns] [(ns-name ns) (namespace-displacement ns)])) namespaces)))

(defn summary
  "The count and names of displaced namespaces in a REPORT, or a typed
  absence when there is none to read."
  [report]
  (if (map? report)
    (let [displaced (vec (sort (keep (fn [[n r]] (when (= :displaced (:status r)) n)) report)))]
      {:checked (count report) :displaced-count (count displaced) :displaced displaced})
    {:absent :no-loaded-displacement}))
