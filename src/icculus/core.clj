(ns icculus.core
  (:require [clojure.tools.logging :as log]
            [mount.core :as mount :refer [defstate]]
            [clojure.tools.cli :refer [parse-opts]]
            [mount.core :as mount]
            [icculus.repl :as repl]
            [icculus.config :refer [env]]
            ))

(def cli-options
  [["-p" "--port PORT"    "Port number" :parse-fn #(Integer/parseInt %)]
   ["-b" "--bind address" "Bind address"]])

(defstate ^{:on-reload :noop} repl-server
          :start
          (if-let [np (get-in env [:nrepl :port])]
            (if-let [nb (get-in env [:nrepl :bind] "localhost")]
              (repl/start {:bind nb :port np})))
          :stop
          (when repl-server
            (repl/stop repl-server)))


(defn stop-app []
  (doseq [component (:stopped (mount/stop))]
    (log/info component "stopped"))
  (shutdown-agents))

(defn start-app [args]
  (doseq [component (-> args
                        (parse-opts cli-options)
                        mount/start-with-args
                        :started)]
    (log/info component "started"))
  (.addShutdownHook (Runtime/getRuntime) (Thread. stop-app)))


(defn -main [& args]
  (start-app args))