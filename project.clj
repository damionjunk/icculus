(defproject icculus "0.1.0-SNAPSHOT"
  :description "Icculus, a Discord bot for Phish stat lookup."
  :url ""
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.suskalo/discljord "0.2.5"]
                 [cheshire "5.8.1"]
                 ;; SQL
                 [com.layerware/hugsql "0.4.9"]
                 [org.postgresql/postgresql "42.2.2"]


                 [cprop "0.1.13"]
                 [mount "0.1.12"]
                 [clj-time "0.14.4"]

                 [org.clojure/tools.logging "0.4.1"]
                 [org.clojure/tools.cli "0.3.7"]
                 [nrepl "0.6.0"]

                 ]
  :main icculus.core
  :uberjar-name "icculus-standalone.jar"
  )
