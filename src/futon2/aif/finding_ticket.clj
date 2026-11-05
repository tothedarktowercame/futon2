(ns futon2.aif.finding-ticket
  "Creation/provenance boundary only. Published tickets have ordinary fields."
  (:require [futon2.data-paths :as data-paths]
            [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.injury-ticket-join :as injury-join]
            [futon2.aif.interoceptive-store-lock :as store-lock]
            [futon2.aif.write-stamp :as write-stamp]
            [futon2.aif.ticket-publication-io :as publication]
            [futon2.aif.ticket-queue :as queue]))

(identity/register! *ns* *file*)

(def canonical-store (data-paths/path "wm-repair-obligations"))

(defn- stamp-created! [root writer path existed?]
  (when (and (not existed?) (.isFile (io/file path)))
    (write-stamp/record-in-context!
     {:root (io/file (.getParentFile (.getCanonicalFile (io/file root)))
                     "wm-write-stamps")
      :writer (str "finding-ticket/" writer)
      :path path})))

(defn destinations
  "The canonical store publishes to the primary checkout. Other store roots
   own an isolated publication tree; callers may supply explicit destinations."
  [root]
  (let [repo (if-let [isolated (System/getenv "FUTON_WM_TICKET_PUBLICATION_ROOT")]
               (io/file isolated)
               (if (= (.getCanonicalPath (io/file root)) canonical-store)
               (io/file "/home/joe/code/futon2")
               (io/file root "ticket-publication")))]
    {:ticket-dir (str (io/file repo "holes/tickets"))
     :queue-path (if (= repo (io/file "/home/joe/code/futon2"))
                   queue/live-path
                   (str (io/file repo "resources/wm/ticket-queue.edn")))}))

(defn- description [value]
  (let [s (str/replace (str value) #"[\r\n]+" " ")]
    (if (> (count s) 2000) (str (subs s 0 2000) " … (see finding)") s)))

(defn- ticket-text [finding source sha]
  (let [environmental? (= :environmental-hold (:repair/class finding))
        behaviour (str/replace (name (or (:failure-kind finding) :unspecified-failure)) "-" " ")]
    (str "# " (if environmental? "Verify or restore " "Correct ") behaviour "\n\n"
         "**Status:** OPEN\n\n"
         (when (re-matches #"(?:M|E|T)-[A-Za-z0-9_-]+" (str (:target finding)))
           (str "Parent: " (:target finding) "\n\n"))
         "## Observed failure\n\n"
         "Target: " (or (:target finding) "not retained") "; stage: " (:failure-stage finding) ".\n\n"
         (when-let [context (:failure-context finding)]
           (str "Context: " (description context) ".\n\n"))
         (description (or (:failure-error finding) (:review-text finding) (:failure-kind finding))) "\n\n"
         "## Scoped task\n\n"
         (if environmental?
           "Verify the reported precondition and restore it if it remains unavailable. If it has cleared, retain a dated recheck showing the current condition."
           "Reproduce the reported behaviour at the named stage and correct its cause. Use the linked evidence to establish the scope; record any missing reproduction inputs explicitly.")
         "\n\n## Acceptance criteria\n\n"
         (if environmental?
           (str "- A dated precondition check with its source and result is retained. Check: dated source and result recorded in this ticket. **Not started.**\n"
                "- Restoration evidence, or a dated recheck showing the condition cleared, is retained. Check: restoration evidence or dated cleared recheck. **Not started.**\n"
                "- Ticket review accepted the scoped work. Check: Status line reads DONE. **Not started.**")
           (str "- A reproduction or regression check detects the reported failure at the named stage. Check: a test namespace/name or command recorded in this ticket. **Not started.**\n"
                "- That check passes after the change, with the scoped validation output retained. Check: commit SHA and test output. **Not started.**\n"
                "- Ticket review accepted the scoped work. Check: Status line reads DONE. **Not started.**"))
         "\n\nReview the task evidence through ordinary ticket review; mark this ticket DONE only when its scoped work is accepted.\n\n"
         "## Provenance\n\n"
         "Finding: [" (:repair/id finding) "](" source ")\n\n"
         "Finding SHA-256: `" sha "`\n")))

(defn- git [repo & args]
  (try (apply sh/sh "git" "-C" (str repo) args)
       (catch java.io.IOException e {:exit -1 :err (.getMessage e)})))

(defn- commit-ticket-once! [path ticket id]
  (let [discovery (git (.getParentFile path) "rev-parse" "--show-toplevel")]
    (if-not (zero? (:exit discovery))
      (assoc discovery :stage :repository)
      (let [repo (str/trim (:out discovery))
            relative (str (.relativize (.toPath (io/file repo)) (.toPath path)))
            present (git repo "cat-file" "-e" (str "HEAD:" relative))
            unchanged? (and (zero? (:exit present))
                            (zero? (:exit (git repo "diff" "--quiet" "HEAD" "--" relative))))
            add (when-not unchanged? (git repo "add" "--" relative))
            commit (cond unchanged? {:exit 0 :unchanged? true}
                         (not (zero? (:exit add))) (assoc add :stage :add)
                         :else (assoc (git repo "commit" "-m"
                                          (str "Publish repair ticket " ticket " (finding " id ")")
                                          "--" relative) :stage :commit))]
        (if-not (zero? (:exit commit)) commit
          (let [head (git repo "rev-parse" "HEAD")]
            (if-not (zero? (:exit head)) (assoc head :stage :head)
              {:exit 0 :status :committed :commit (str/trim (:out head))
               :repo repo :path relative :unchanged? (boolean (:unchanged? commit))})))))))

(defn- commit-ticket! [path ticket id previous]
  (loop [attempt 1]
    (let [result (commit-ticket-once! path ticket id)]
      (cond
        (zero? (:exit result))
        (if (and (:unchanged? result) (= :committed (:status previous)))
          previous
          (assoc (dissoc result :exit :unchanged?) :attempts attempt))
        (< attempt 3) (do (Thread/sleep 50) (recur (inc attempt)))
        :else {:status :failed :kind :ticket-git-publication-failed
               :attempts attempt :stage (:stage result)
               :exit (:exit result) :error (:err result)}))))

(defn publish!
  "Read the durable finding, publish without replacing ticket edits, enqueue,
   commit only the ticket under the store lock. Immutable finding provenance is
   retained alongside the latest Git publication result; a retry can repair a
   failed commit without losing the ticket or queue entry."
  ([root id] (publish! root id (destinations root)))
  ([root id {:keys [ticket-dir queue-path]}]
   (when-not (and (string? id) (re-matches #"[A-Za-z0-9][A-Za-z0-9._-]*" id))
     (throw (ex-info "Unsafe finding identity" {:kind :finding-ticket-id-invalid})))
   (let [{:keys [ticket-dir queue-path]}
         (if (System/getenv "FUTON_WM_TICKET_PUBLICATION_ROOT")
           (destinations root) {:ticket-dir ticket-dir :queue-path queue-path})
         source (publication/checked-file! (io/file root "findings" (str id ".edn")))
         bytes (java.nio.file.Files/readAllBytes (.toPath source))
         finding (edn/read-string (String. bytes "UTF-8"))
         ticket (str "T-" id)
         entry {:ticket ticket :inserted-at (:opened-at finding)}
         _ (when-not (= id (:repair/id finding))
             (throw (ex-info "Finding identity mismatch" {:kind :finding-ticket-id-mismatch})))
         _ (queue/validate! (assoc queue/empty-declaration :entries [entry]))
         path (publication/checked-file! (io/file ticket-dir (str ticket ".md")))
         receipt {:schema :wm/finding-ticket-v1 :finding/id id
                  :finding/ticket {:id ticket :path (.getPath path)
                                   :finding-path (.getPath source)
                                   :finding-sha256 (identity/sha256 bytes)
                                   :queue-path (.getAbsolutePath (io/file queue-path))}}
         receipt-file (io/file root "ticket-links" (str id ".edn"))]
     (store-lock/with-store-lock-for root
       (fn []
         (let [existed? (.exists path)]
         (publication/publish-text! path (ticket-text finding (.getPath source) (identity/sha256 bytes)) false)
           (stamp-created! root "publish!" path existed?))
         (let [resolved (injury-join/resolve-join root id)]
           (when (= :refused (:status resolved))
             (throw (ex-info "Ticket injury join refused" resolved)))
           (when (= :present (:status resolved))
             (let [marker "## Injury provenance"
                   block (str "\n" marker "\n\n```edn\n"
                              (pr-str (assoc (:join resolved)
                                             :finding-sha256 (:finding-sha256 resolved)))
                              "\n```\n")
                   previous (slurp path)]
               (cond
                 (str/includes? previous block) nil
                 (str/includes? previous marker)
                 (throw (ex-info "Ticket injury provenance conflicts"
                                 {:reason :injury-join-conflict}))
                 :else (publication/publish-text! path (str previous block) true)))))
         (let [queue-file (io/file queue-path)
               queue-lock (io/file (str (.getCanonicalPath queue-file) ".lock"))
               queue-existed? (.exists queue-file)
               lock-existed? (.exists queue-lock)]
           (queue/enqueue! queue-path entry)
           (stamp-created! root "publish!" queue-file queue-existed?)
           (stamp-created! root "publish!" queue-lock lock-existed?))
         (let [receipt-existed? (.exists receipt-file)]
           (publication/publish-text! receipt-file (pr-str receipt) false)
           (stamp-created! root "publish!" receipt-file receipt-existed?))
         ;; Retain the first publication's byte pin. The store accepts legacy
         ;; equivalent serializations on replay; that does not rewrite the
         ;; original ticket or its historical provenance receipt.
         (let [retained (edn/read-string (slurp receipt-file))]
           (when-not (= (update receipt :finding/ticket dissoc :finding-sha256)
                        (update (dissoc retained :publication/git) :finding/ticket dissoc :finding-sha256))
             (throw (ex-info "Ticket provenance conflicts" {:kind :finding-ticket-link-conflict})))
           (let [result (assoc retained :publication/git
                               (commit-ticket! path ticket id (:publication/git retained)))]
             (when-not (= retained result)
               (publication/publish-text! receipt-file (pr-str result) true))
             result)))))))
