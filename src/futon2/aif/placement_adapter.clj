(ns futon2.aif.placement-adapter
  "Read pinned ant placements and attach them to outer-observation facts."
  (:require [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.nio.file Files]
           [java.security MessageDigest]))

(def default-paths
  {:tsv "/home/joe/code/futon3c/holes/excursions/ant-reads/A1-placements.tsv"
   :classes "/home/joe/code/futon3c/holes/excursions/ant-reads/classes.tsv"})

(def absent-placement {:status :absent :reason :no-placement-yet})

(defn- sha256 [^bytes bytes]
  (let [digest (.digest (doto (MessageDigest/getInstance "SHA-256")
                          (.update bytes)))]
    (apply str (map #(format "%02x" (bit-and 255 %)) digest))))

(defn- read-source [path]
  (try
    (let [file (io/file path)
          bytes (Files/readAllBytes (.toPath file))]
      {:path (.getPath file) :sha256 (sha256 bytes)
       :text (String. bytes "UTF-8") :file file})
    (catch Exception _
      {:path (str path) :status :absent})))

(defn- table-lines [text]
  (let [[header & lines] (str/split-lines text)
        columns (str/split (or header "") #"\t" -1)]
    (map (fn [line]
           {:line line :fields (zipmap columns (str/split line #"\t" -1))})
         (remove str/blank? lines))))

(defn- ways [s]
  (if (or (str/blank? s) (= "none" s))
    []
    (str/split s #",\s*")))

(defn- cite [dir id]
  {:file (.getPath (io/file dir (str id ".A1.json")))})

(defn- placement-row [dir {:keys [line fields]}]
  (let [{:strs [id what status wing way action need_goes_to finish]} fields]
    {:id id :what what :status status :ways (ways way) :wing wing :action action
     :need-goes-to need_goes_to :finish finish :cite (cite dir id)
     :source :per-stem :pin (sha256 (.getBytes line "UTF-8"))}))

(defn- member-ids [classes-file member-file]
  (try
    (->> (slurp (io/file (.getParentFile classes-file) member-file))
         str/split-lines
         (remove str/blank?))
    (catch Exception _ [])))

(defn- class-placements [source]
  (let [dir (.getParentFile ^java.io.File (:file source))]
    (into {}
          (mapcat
           (fn [{:keys [line fields]}]
             (let [{:strs [class member-ids-file way wing action finish status]} fields
                   row {:what "" :status status :ways (ways way) :wing wing
                        :action (first (str/split action #"\s+")) :need-goes-to ""
                        :finish finish :source :class :class class
                        :pin (sha256 (.getBytes line "UTF-8"))}]
               (map (fn [id] [id (assoc row :id id :cite (cite dir id))])
                    (member-ids (:file source) member-ids-file))))
           (table-lines (:text source))))))

(defn- public-source [source]
  (dissoc source :text :file))

(defn load-placements
  "Load per-stem and class placements. File failures are represented as data."
  [paths]
  (let [tsv (read-source (:tsv paths))
        classes (read-source (:classes paths))
        source {:tsv (public-source tsv) :classes (public-source classes)}]
    (if-not (:text tsv)
      {:status :absent :reason :placements-unavailable :source source}
      (let [dir (.getParentFile ^java.io.File (:file tsv))
            per-rows (into {} (map (fn [entry]
                                     (let [row (placement-row dir entry)]
                                       [(:id row) row])))
                           (table-lines (:text tsv)))
            class-rows (if (:text classes) (class-placements classes) {})]
        {:status :pinned
         :source (assoc source :rows (count per-rows)
                       :class-rows (if (:text classes)
                                     (count (table-lines (:text classes))) 0))
         :rows (merge class-rows per-rows)}))))

(defn placement-fact [placements target]
  (if (= :pinned (:status placements))
    (get (:rows placements) target absent-placement)
    absent-placement))

(defn annotate
  "Attach :placement to every fact and retain placement source provenance."
  [observation placements]
  (assoc observation
         :facts (into {} (map (fn [[target fact]]
                                [target (assoc fact :placement
                                               (placement-fact placements target))]))
                      (:facts observation))
         :placement/source (cond-> (assoc (:source placements) :status (:status placements))
                             (:reason placements) (assoc :reason (:reason placements)))))
