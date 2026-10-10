(ns futon2.aif.finding-ticket
  "Creation/provenance boundary only. Published tickets have ordinary fields."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [clojure.java.shell :as sh]
            [clojure.string :as str]
            [futon2.aif.load-identity :as identity]
            [futon2.aif.interoceptive-store-lock :as store-lock]
            [futon2.aif.ticket-publication-io :as publication]
            [futon2.aif.ticket-queue :as queue]
            [futon2.data-paths :as data-paths]))

(identity/register! *ns* *file*)

(def canonical-store (data-paths/path "wm-repair-obligations"))

(defn- production-store? [root]
  (= (.getCanonicalPath (io/file root))
     (.getCanonicalPath (io/file data-paths/production-data-root
                                 "wm-repair-obligations"))))

(def live-destinations
  "Where the production store publishes: the primary checkout's tickets, and
   the live queue, which is untracked runtime state (never a tracked resource)."
  {:ticket-dir (data-paths/repo-path "holes" "tickets")
   :queue-path queue/live-path})

(defn destinations
  "The production store publishes to the primary checkout (live-destinations).
   Every other store root owns an isolated publication tree. A test JVM may
   not publish from the production store at all.

   The comparison is with the PRODUCTION store, not canonical-store: that is
   resolved through data-paths, so under a test data root it names the test
   store, and comparing with it sent the test store's tickets into the real
   checkout (offline replay, 2026-10-10: commit 79a523906 on main, D21)."
  [root]
  (cond
    (and (production-store? root) data-paths/test-mode?)
    (throw (ex-info "A test JVM may not publish tickets from the production repair store"
                    {:kind :production-store-in-test-mode :root (str root)}))

    (production-store? root) live-destinations

    :else
    (let [repo (io/file root "ticket-publication")]
      {:ticket-dir (str (io/file repo "holes/tickets"))
       :queue-path (str (io/file repo "resources/wm/ticket-queue.edn"))})))

(defn- description [value]
  (let [s (str/replace (str value) #"[\r\n]+" " ")]
    (if (> (count s) 2000) (str (subs s 0 2000) " … (see finding)") s)))

(defn- ticket-text [finding source sha]
  (let [environmental? (= :environmental-hold (:repair/class finding))
        behaviour (str/replace (name (or (:failure-kind finding) :unspecified-failure)) "-" " ")]
    (str "# " (if environmental? "Verify or restore " "Correct ") behaviour "\n\n"
         "**Status:** OPEN\n\n"
         (when (re-matches #"M-[A-Za-z0-9_-]+" (str (:target finding)))
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
         "\n\n## Acceptance evidence\n\n"
         (if environmental?
           "Retain the dated precondition check, its source and result, and evidence of any restoration."
           "Retain a reproduction or regression check that detects the reported failure and passes after the change, together with the scoped validation output.")
         " Review the task evidence through ordinary ticket review; mark this ticket DONE only when its scoped work is accepted.\n\n"
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
            _ (when (and data-paths/test-mode?
                         (= (.getCanonicalPath (io/file repo))
                            (.getCanonicalPath (io/file data-paths/production-repo-root))))
                ;; Defence in depth for D21: a test JVM never commits to the
                ;; canonical checkout, whatever path it was handed.
                (throw (ex-info "Refusing to commit a ticket to the canonical futon2 checkout from a test JVM"
                                {:kind :ticket-commit-refused-in-test-mode :repo repo})))
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
   (let [source (publication/checked-file! (io/file root "findings" (str id ".edn")))
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
         (publication/publish-text! path (ticket-text finding (.getPath source) (identity/sha256 bytes)) false)
         (queue/enqueue! queue-path entry)
         (publication/publish-text! receipt-file (pr-str receipt) false)
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
