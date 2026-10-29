(ns futon2.aif.acceptance-publication-isolation-test
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.test :refer [deftest is]]
            [futon2.aif.load-identity :as identity]))

(defn- snapshot [root]
  (into (sorted-map)
        (for [f (file-seq (io/file root)) :when (.isFile f)]
          [(.getPath f) (identity/sha256 (java.nio.file.Files/readAllBytes (.toPath f)))])))

(deftest acceptance-process-publishes-only-under-its-own-roots
  (let [dir (str (java.nio.file.Files/createTempDirectory "acceptance-publication-"
                  (make-array java.nio.file.attribute.FileAttribute 0)))
        canonical "/home/joe/code/futon2"
        roots [(str canonical "/holes/tickets") (str canonical "/data/wm-repair-obligations")]
        head #(:out (sh/sh "git" "-C" canonical "rev-parse" "HEAD"))
        before [(head) (mapv snapshot roots)]
        repair-root (str dir "/repair-obligations")
        publication-root (str dir "/ticket-publication")
        env (merge (into {} (System/getenv))
                   {"FUTON_WM_REPAIR_OBLIGATION_ROOT" repair-root
                    "FUTON_WM_TICKET_PUBLICATION_ROOT" publication-root})
        form (str
               "(require '[clojure.java.io :as io] '[futon2.aif.repair-obligation :as repair] '[futon2.aif.finding-ticket :as ticket]) "
               "(assert (= repair/default-root (System/getenv \"FUTON_WM_REPAIR_OBLIGATION_ROOT\"))) "
               "(let [root repair/default-root pub (System/getenv \"FUTON_WM_TICKET_PUBLICATION_ROOT\") "
               "id \"acceptance-isolation\" f (io/file root \"findings\" (str id \".edn\"))] "
               "(io/make-parents f) (spit f (pr-str {:repair/id id :opened-at \"2026-10-29T00:00:00Z\" :failure-kind :test :failure-stage :close})) "
               ;; Even explicit destinations must not escape the acceptance process.
               "(ticket/publish! root id {:ticket-dir \"/home/joe/code/futon2/holes/tickets\" :queue-path \"/home/joe/code/futon2/resources/wm/ticket-queue.edn\"}) "
               "(assert (.isFile (io/file pub \"holes/tickets/T-acceptance-isolation.md\"))) "
               "(println :isolated-publication))")
        result (sh/sh "clojure" "-J-Xmx768m" "-M" "-e" form :env env)]
    (is (zero? (:exit result)) (pr-str result))
    (is (= before [(head) (mapv snapshot roots)]))
    (is (.isFile (io/file repair-root "findings/acceptance-isolation.edn")))
    (is (.isFile (io/file publication-root "holes/tickets/T-acceptance-isolation.md")))))
