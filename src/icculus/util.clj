(ns icculus.util
  (:require [clojure.string :as s]
            [clj-time.core :as time]))

(defn safe-trim    [x] (if (nil? x) nil (s/trim x)))
(defn ->empty->nil [x] (if (nil? x) nil (if (empty? (s/trim x)) nil (s/trim x))))

(defn ->slug [song]
  (and song
       (-> song s/lower-case s/trim
           (s/replace #"\\\/" " ") (s/replace #"[()’‘”“\"\'\.?!,_&=]" "") (s/replace #"\s+" "-"))))

(defn ->date [dstr]
  (try
    (when dstr
      (cond
        ;; 2018
        (re-matches #"^\d{4}$" dstr) (time/date-time (Long/parseLong dstr))

        ;; 01/2018, 02-2018 etc
        (re-matches #"^\d{1,2}[-/]\d{4}" dstr)
        (-> (s/split dstr #"[-/]")
            ((fn [[m y]] (time/date-time (Long/parseLong y) (Long/parseLong m)))))

        ;; 01/02/2018
        (re-matches #"^\d{1,2}[-/]\d{1,2}[-/]\d{4}" dstr)
        (-> (s/split dstr #"[-/]")
            ((fn [[m d y]] (time/date-time (Long/parseLong y) (Long/parseLong m) (Long/parseLong d)))))))
    (catch Exception e
      ;; nil is fine
      )))

(comment

  (->date "1 2 e")

  (->date "1/2/2019")

  (s/split "1/2018" #"[-/]")

  )