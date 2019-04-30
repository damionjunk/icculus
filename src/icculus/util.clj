(ns icculus.util
  (:require [clojure.string :as s]
            [clj-time.core :as time])
  (:import (org.joda.time Period)
           (org.joda.time Duration)))

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

(defn- time-handle [x]
  (cond
    (= (type x) java.lang.Long) (Duration. x)
    (= (type x) java.lang.Integer) (Duration. (long x))
    :default x))

(defn duration->human [dur]
  (try
    (let [p (.normalizedStandard (Period. (time-handle dur)))
          h (when (pos? (.getHours p)) (str (.getHours p) (if (> (.getHours p) 1) " hours " " hour ")))
          m (when (pos? (.getMinutes p)) (str (.getMinutes p) (if (> (.getMinutes p) 1) " minutes " " minute ")))
          s (when (pos? (.getSeconds p)) (str (.getSeconds p) (if (> (.getSeconds p) 1) " seconds" " second")))]
      (s/trim (str h m s)))
    (catch Exception e
      (prn e))))

(comment

  (duration->human 10000000000)

  (duration->human (Period. 0 0 3 500 0 0 0 0))

  (.normalizedStandard (Period. 0 0 3 500 0 0 0 0) (org.joda.time.PeriodType/standard))
  
  )