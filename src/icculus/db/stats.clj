(ns icculus.db.stats
  (:require [clj-time.core :as time :refer [now days seconds minutes hours years from-now]]
            [clj-time.format :as timef]
            [clojure.tools.logging :as log]
            [hugsql.core :as hugsql]
            [clojure.string :as s]
            [clojure.java.io :as io]
            [icculus.util :as util]
            [icculus.config :refer [env]]
            [icculus.util :refer [->slug]]
            [clj-time.jdbc]))

(hugsql/def-db-fns (io/resource "sql/phishstats.sql"))

;; Adding this due to issues with `date` in PGsql, and the driver doing some bad timezone stuff.
(def simple-date (timef/formatter "yyyy-MM-dd"))

; (defn log-sqlvec [sqlvec]
;   (log/info (->> sqlvec
;                  (map #(clojure.string/replace (or % "") #"\n" ""))
;                  (clojure.string/join " ; "))))

; (defn log-command-fn [this db sqlvec options]
;   (log-sqlvec sqlvec)
;   (condp contains? (:command options)
;     #{:!} (hugsql.adapter/execute this db sqlvec options)
;     #{:? :<!} (hugsql.adapter/query this db sqlvec options)))

; (defmethod hugsql.core/hugsql-command-fn :! [sym] `log-command-fn)
; (defmethod hugsql.core/hugsql-command-fn :<! [sym] `log-command-fn)
; (defmethod hugsql.core/hugsql-command-fn :? [sym] `log-command-fn)

(def get-song nil)
;; PG returns Integers, Clojure uses Longs, we'll just use number!
(defmulti get-song #(cond (number? %) :number (string? %) :string))

(defmethod get-song :number [id]
  (let [song (first (find-song-by-id (:db env) {:id id}))]
    (if (:alias_for song) (get-song (:alias_for song)) song)))

(defmethod get-song :string [title]
  (let [song (first (find-song-by-slug (:db env) {:slug (->slug title)}))]
    (if (:alias_for song) (get-song (:alias_for song)) song)))

(defmethod get-song :default [_] nil)

(defn first-played [title]
  (if-let [song (get-song title)]
    (first (first-played-by-id (:db env) {:song_id (:id song)}))))

(defn last-played [title]
  (if-let [song (get-song title)]
    (first (last-played-by-id (:db env) {:song_id (:id song)}))))

(defn last-n-played [n title]
  (if-let [song (get-song title)]
    (last-n-played-by-id (:db env) {:song_id (:id song) :limit n})))

(defn get-set-data [date]
  (let [dstr (timef/unparse simple-date date)
        show (first (show-by-date (:db env) {:show_date dstr}))
        tracks (and show (tracks-by-show-id (:db env) {:show_id (:id show)}))]
    (when show 
      (assoc show :tracks tracks))))

(defn show-gap [title]
  (if-let [lp (last-played title)]
    (-> (shows-after-date (:db env) {:show_date (:show_date lp)}) first :count)))

(defn day-gap [title]
  (if-let [lp (last-played title)]
    (.. (time/interval (:show_date lp) (time/now)) toDuration getStandardDays)))

(defn times-played [title tptype start & [end]]
  (if-let [song (get-song title)]
    (case tptype
      :era (let [etotal   (-> (total-in-era (:db env) {:era start}) first :count)
                 et-total (track-total-by-era (:db env) {:era start :song_id (:id song)})]
             {:title (:title song) :period-total etotal :plays (transduce (map :plays) + et-total)})
      :range (let [end (or end (time/now))
                   etotal   (-> (total-in-range (:db env) {:start start :end end}) first :count)
                   et-total (track-total-by-range (:db env) {:start start :end end :song_id (:id song)})]
               {:title (:title song) :period-total etotal :plays (transduce (map :plays) + et-total)})
      nil)))

(defn duration-agg-stats [title]
  (if-let [song (get-song title)]
    (first (all-tracks-stats (:db env) {:song_id (:id song)}))))

(defn longest [title & [limit]]
  (println "longest")
  (if-let [song (get-song title)]
    (let [longest-tracks (longest-by-song-id (:db env) {:song_id (:id song) :limit (or limit 3)})]
      (map (fn [lt]
             (let [show (get-set-data (:show_date lt))]
               (merge (select-keys lt [:duration :title])
                      (select-keys show [:name :location])
                      {:date (-> show :tracks first :show_date)
                       :dow  (-> show :tracks first :dow)})))
           longest-tracks))))

(defn shortest [title & [limit]]
  (if-let [song (get-song title)]
    (let [shortest-tracks (shortest-by-song-id (:db env) {:song_id (:id song) :limit (or limit 3)})]
      (map (fn [lt]
             (let [show (get-set-data (:show_date lt))]
               (merge (select-keys lt [:duration :title])
                      (select-keys show [:name :location])
                      {:date (-> show :tracks first :show_date)
                       :dow  (-> show :tracks first :dow)})))
           shortest-tracks))))

(comment

  (longest "yem")
  (shortest "yem")

  (longest-by-song-id (:db env) {:song_id 879
                                 :limit   3})

  (-> (duration-agg-stats "yem") :avg (.longValue))

  (def xf (map :plays))
  (transduce (map :plays) + (track-total-by-era (:db env) {:era     "3.0"
                                                           :song_id 879}))

  (times-played "yem" :era "3.0")
  (times-played "yem" :range (time/date-time 2016))


  (let [sd (:show_date (last-played "yem"))]
    (time/minus (time/now) sd))


  (.. (time/interval (time/date-time 2019 04 23) (time/now)) toDuration getStandardDays)


  (type (:alias_for (first (find-song-by-slug (:db env) {:slug "yem"}))))

  (last-n-played 5 "bathtub gin" )

  (get-song "YEM")
  (first-played "YeM   ")
  (last-played "YeM   ")
  (last-n-played 3 "yem")

  (clojure.pprint/pprint  (get-set-data (util/->date "12/31/2018")))
  (clojure.pprint/pprint  (get-set-data "12/31/2018"))
  (show-by-date (:db env) {:show_date "2019-06-21"})
  (tracks-by-show-id (:db env) {:show_id 999007})

  (show-gap "strawberry fields forever")
  (day-gap "strawberry fields forever")
  (day-gap "yem"))