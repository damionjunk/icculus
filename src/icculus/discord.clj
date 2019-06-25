(ns icculus.discord
  (:require [discljord.connections :as c]
            [discljord.messaging :as m]
            [discljord.events :as e]
            [clj-time.format :as timef]
            [clj-time.core :as time]
            [clojure.core.async :as a]
            [clojure.tools.logging :as log]
            [icculus.commands :as i]
            [icculus.config :refer [env]]
            [icculus.db.stats :as stats]
            [clojure.string :as s]
            [icculus.util :as util]))

;; Keeps connection information for discljord.
(defonce state (atom nil))

(def colors [0x375E97 0xFB6542 0xFFBB00 0x3F681C])
(def readable-date (timef/formatter "EEEE, MMMM dd, yyyy"))

(def build-embed nil)
(defmulti build-embed (fn [{cmd :cmd}] cmd))
(defmethod build-embed :default [_])

(defmethod build-embed :icculus [_]
  {:color  (rand-nth colors)
   :title  "Icculus bot commands"
   :fields (map (fn [[cmd {help :help ex :examples}]]
                  {:name  (str cmd " " help)
                   :value (s/join ", " ex)})
                (sort-by #(-> % second :sort) i/commands))})

(defmethod build-embed :fam [_]
  {:color       (rand-nth colors)
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
   :footer      {:text "Your name too can be written in the Helping Friendly Book."}})

(defmethod build-embed :firstplayed [{title :title}]
  (if-let [song (stats/first-played title)]
    {:color       (rand-nth colors)
     :title       (str (:title song) " was first played on " (timef/unparse readable-date (:show_date song)))
     :description (str "It was played at " (:name song) " in " (:location song))
     :footer      {:text (str (:title song) " was song " (:position song) " in set " (:set song) " and had a duration of " (util/duration->human (long (:duration song))))}
     }))

(defmethod build-embed :lastplayed [{title :title}]
  (if-let [songs (stats/last-n-played 3 title)]
    {:color       (rand-nth colors)
     :title       (str (:title (first songs)) " was last played on " (timef/unparse readable-date (:show_date (first songs))))
     :description 
     (apply str
            (str "It was played at " (:name (first songs)) " in " (:location (first songs)) "\n\n")
            "Next most recent plays 🌸:\n"
            (map (fn [song]
                   (str " 🌵 "
                        (timef/unparse readable-date (:show_date song)) 
                        " at " (:name song) " in " (:location song) "\n")) 
                 (rest songs)))
     }))

(defn gap-message [[l h] gap]
  (cond
    (<= gap l) "That seems about right."
    (<= gap h) "I'm not sure I'm okay with this..."
    :default "This is too damn long!"))

(defn jam-message [{avg :avg sd :sd mmin :min mmax :max}]
  (if (>= (- mmax mmin) 300000) ; 5 minutes
    "Those were some tasty jams!"
    "Great songs, but not much jam length variation."))

(defn gap-message-shows [gap] (gap-message [5 15] gap))
(defn gap-message-days  [gap] (gap-message [45 100] gap))

(defmethod build-embed :showgap [{title :title}]
  (if-let [gap (stats/show-gap title)]
    {:color       (rand-nth colors)
     :title       (str "The number of shows since " title " has been played ...")
     :description (str "... is " gap "!")
     :footer      {:text (gap-message-shows gap)}
     }))

(defmethod build-embed :daygap [{title :title}]
  (if-let [gap (stats/day-gap title)]
    {:color       (rand-nth colors)
     :title       (str "The number of days since " title " has been played ...")
     :description (str "... is " gap " days!")
     :footer      {:text (gap-message-days gap)}
     }))

(defmethod build-embed :timesplayed [{tp :type era :era start :start end :end title :title}]
  (if-let [tpdata (stats/times-played title (if era :era :range) (if era era start) end)]
    {:color       (rand-nth colors)
     :title       (if era (str "During the " era " era, " title " was played " (:plays tpdata) " times.")
                          (str "Between " (timef/unparse (:year-month-day timef/formatters) start) " and " (timef/unparse (:year-month-day timef/formatters) (or end (time/now))) ", " title " was played " (:plays tpdata) " times."))
     :description (str "There were a total of " (:period-total tpdata) " songs played during this period.")
     :footer      {:text (str "This represents " title " being played " (util/round-double 2 (* 100 (double (/ (:plays tpdata) (:period-total tpdata))))) "% of the time.")}}))

(defn duration-message [i {dur :duration title :title loc :name city :location d :date}]
  (str "[" (inc i) "]: " (util/duration->human (long dur))
       " on " (timef/unparse readable-date d)
       " 🐟🐟🐟 @ "
       loc " in " city
       "\n\n"))

(defn agg-stats-message [{avg :avg sd :sd mmin :min mmax :max}]
  (str "🌸 Summary stats:\navg: "
       (util/duration->hh-mm-ss (.longValue avg))
       " sd: "
       (util/duration->hh-mm-ss (.longValue sd))
       " min: "
       (util/duration->hh-mm-ss mmin)
       " max: "
       (util/duration->hh-mm-ss mmax)
       " 🌵🌵🌵"))

(defmethod build-embed :longest [{title :title}]
  (if-let [l (stats/longest title)]
    (if-let [st (stats/duration-agg-stats title)]
      {:color (rand-nth colors)
       :title (str "The top " (count l) " longest played versions of " title " 🚀")
       :footer {:text (jam-message st)}
       :description (str (apply str (map-indexed duration-message l))
                         (agg-stats-message st))
       })))

(defmethod build-embed :shortest [{title :title}]
  (if-let [l (stats/shortest title)]
    (if-let [st (stats/duration-agg-stats title)]
      {:color (rand-nth colors)
       :title (str "The " (count l) " shortest played versions of " title " 🚀")
       :footer {:text "Phish can do no wrong!"}
       :description (str (apply str (map-indexed duration-message l))
                         (agg-stats-message st))})))

(def handler nil)
(defmulti handler (fn [event-type event-data] event-type))
(defmethod handler :default [_ _])

(defmethod handler :message-create [event-type {{bot :bot} :author :keys [channel-id content author] :as mp}]
  (log/info event-type)
  (log/info content)
  (when-not bot
    (when-let [cmd (i/icculizer content)]
      (when-let [embed (build-embed cmd)]
        (log/info "Responding to valid command:" content)
        ;(log/info embed)
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
  )