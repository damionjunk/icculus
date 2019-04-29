(ns icculus.discord
  (:require [discljord.connections :as c]
            [discljord.messaging :as m]
            [discljord.events :as e]
            [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [icculus.commands :as i]
            [icculus.config :refer [env]]
            [clojure.string :as s]))

;; Keeps connection information for discljord.
(def state (atom nil))

(def colors [0x375E97 0xFB6542 0xFFBB00 0x3F681C])

(def build-embed nil)
(defmulti build-embed (fn [{cmd :cmd}] cmd))
(defmethod build-embed :default [_])

(defmethod build-embed :icculus [_]
  {:color  (rand-nth colors)
   :author {:name "Icculus"}
   :title  "Icculus bot commands"
   :fields (map (fn [[cmd {help :help ex :examples}]]
                  {:name  (str cmd " " help)
                   :value (s/join ", " ex)})
                (sort-by #(-> % second :sort) i/commands))})

(defmethod build-embed :fam [_]
  {:color       (rand-nth colors)
   :author      {:name "Icculus"}
   :title       "Those listed here have received blessings from Icculus:"
   :description (s/join
                  "\n"
                  ["```"
                   "@Hadley - Head honcho, daily jams, otherwise great leader."
                   "@St. greyman - Saintly deeds, filling the jam needs."
                   "@el-chapo - Scribe of the records of the holy 4."
                   "@marv - Populator of bits for the population."
                   "@mocacola15, @inf4m0us - Early vessels of the knowledge of Icculus."
                   "@Dirty Harry Hood, @alflup - Prayers to Icculus were heard."
                   "```"])
   :footer      "Your name too can be written in the Helping Friendly Book."})

(def handler nil)
(defmulti handler (fn [event-type event-data] event-type))
(defmethod handler :default [_ _])

(defmethod handler :message-create [event-type {{bot :bot} :author :keys [channel-id content author] :as mp}]
  (log/info event-type)
  (log/info content)
  (when-not bot
    (when-let [cmd (i/icculizer content)]
      (when-let [embed (build-embed cmd)]
        (m/create-message! (:messaging @state) channel-id :embed embed)))))


(defn disconnect []
  (a/put! (:connection @state) [:disconnect]))


(defn connect
  "This is blocking."
  []
  (let [event-ch (a/chan 100)
        connection-ch (c/connect-bot! (get-in env [:discord :key]) event-ch)
        messaging-ch (m/start-connection! (get-in env [:discord :key]))
        init-state {:connection connection-ch
                    :event event-ch
                    :messaging messaging-ch}]
    (reset! state init-state)
    (e/message-pump! event-ch handler)
    (m/stop-connection! messaging-ch)
    (c/disconnect-bot! connection-ch)))


(comment

  (future (connect))
  (disconnect)

  (a/put! (:connection @state) [:disconnect])

  (def colors [0x375E97 0xFB6542 0xFFBB00 0x3F681C])

  (build-embed (i/icculizer "?icculus"))

  )