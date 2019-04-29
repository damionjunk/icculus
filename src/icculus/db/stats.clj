(ns icculus.db.stats
  (:require [clj-time.core :as time :refer [now days seconds minutes hours years from-now]]
            [clojure.tools.logging :as log]
            [hugsql.core :as hugsql]
            [clojure.string :as s]
            [clojure.java.io :as io]
            [icculus.config :refer [env]]
            [icculus.util :refer [->slug]]
            [clj-time.jdbc]))

(hugsql/def-db-fns (io/resource "sql/phishstats.sql"))

(def get-song nil)
;; PG returns Integers, Clojure uses Longs, we'll just use number!
(defmulti get-song #(cond (number? %) :number (string? %) :string))

(defmethod get-song :number [id]
  (let [song (first (find-song-by-id (:db env) {:id id}))]
    (if (:alias_for song) (get-song-by-id (:alias_for song)) song)))

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

(defn get-set-data [date]
  (let [show (first (show-by-date (:db env) {:show_date date}))
        tracks (and show (tracks-by-show-id (:db env) {:show_id (:id show)}))]
    (assoc show :tracks tracks)))

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
  (if-let [song (get-song title)]
    (let [longest-tracks (longest-by-song-id (:db env) {:song_id (:id song) :limit (or limit 3)})]
      (map (fn [lt]
             (let [show (get-set-data (:show_date lt))]
               (merge (select-keys lt [:duration :title])
                      (select-keys show [:name :location])
                      {:date (-> show :tracks first :show_date)
                       :dow  (-> show :tracks first :dow)})))
           (sort-by :duration longest-tracks)))))

(defn shortest [title & [limit]]
  (if-let [song (get-song title)]
    (let [shortest-tracks (shortest-by-song-id (:db env) {:song_id (:id song) :limit (or limit 3)})]
      (map (fn [lt]
             (let [show (get-set-data (:show_date lt))]
               (merge (select-keys lt [:duration :title])
                      (select-keys show [:name :location])
                      {:date (-> show :tracks first :show_date)
                       :dow  (-> show :tracks first :dow)})))
           (sort-by :duration shortest-tracks)))))

(comment

  (longest "yem")
  (shortest "yem")

  (longest-by-song-id (:db env) {:song_id 879 :limit 3})

  (duration-agg-stats "yem")

  (def xf (map :plays))
  (transduce (map :plays) + (track-total-by-era (:db env) {:era "3.0" :song_id 879}))


  (times-played "yem" :era "3.0")
  (times-played "yem" :range (time/date-time 2016))


  (let [sd (:show_date (last-played "yem"))]
    (time/minus (time/now) sd)
    )


  (.. (time/interval (time/date-time 2019 04 23) (time/now)) toDuration getStandardDays)


  (type (:alias_for (first (find-song-by-slug (:db env) {:slug "yem"}))))



  (get-song "YEM")
  (first-played "YeM   ")
  (last-played "YeM   ")

  (get-set-data (time/date-time 2019 02 22))

  (show-gap "strawberry fields forever")
  (day-gap "strawberry fields forever")
  (day-gap "yem")

  )