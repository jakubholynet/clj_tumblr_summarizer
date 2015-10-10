(ns clj-tumblr-summarizer.main
  (:require [clojure.pprint :as pp]
            [clojure.data.json :as json]
            [clojure.core.async :as a :refer [chan >!! <!! close!]]
            [clj-tumblr-summarizer.core :refer [log]]
            [clj-tumblr-summarizer.output :as out]
            [clj-tumblr-summarizer.input :as in]))

(def api-key (slurp ".api-key"))
(def tumblr-max-limit 20)                                   ;; TODO mv to input

(Thread/setDefaultUncaughtExceptionHandler
  (reify Thread$UncaughtExceptionHandler
    (uncaughtException [_ thread throwable]
      (binding [*out* *err*]
        (println "UNCAUGHT ERROR ON A THREAD:" (.getMessage throwable))))))


(defn print-receiver [chan]
  (a/thread
    (loop []
      (if-let [post (<!! chan)]
        (do
          (json/pprint post)
          ;(out/output-post post)
          (recur))
        (log "print-receiver: DONE, no more input")))))

(defn -main [& args]
  (let [post-chan (chan tumblr-max-limit)]
    (in/fetch-posts {:chan post-chan, :api-key api-key})
    ;; Start the reader, wait for it to finish before shutting down
    (<!! (print-receiver post-chan))))

