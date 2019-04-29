(ns icculus.config
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :as mount :refer [defstate]]
            [clojure.tools.logging :as log]))


(defstate env
          :start
          (load-config
            :merge
            [(mount/args)
             (source/from-env)]))
