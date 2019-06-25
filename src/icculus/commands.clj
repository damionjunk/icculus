(ns icculus.commands
  (:require [clojure.string :as s]
            [icculus.util :as util]))

(def commands
  {"?icculus"     {:cmd  :icculus :sort 0
                   :help ""
                   :examples ["This help."]}
   "?fam"         {:cmd  :fam :sort 0
                   :help ""
                   :examples ["A list of keepers of the tenets of the Helping Friendly Book."]}
   "?timesplayed" {:cmd  :timesplayed :title true :sort 1
                   :help "[era/start date] [end date] [title]"
                   :examples ["?timesplayed 3.0 Antelope" "?timesplayed 2012 2019 Divided Sky" "?timesplayed 07-2018 YEM"]}
   "?setlist"     {:cmd :setlist :sort 2
                   :help "[date]"
                   :examples ["?setlist 12-31-2018"]}
   "?firstplayed" {:cmd  :firstplayed :title true :sort 1
                   :help "[title]"
                   :examples ["?firstplayed yem"]}
   "?lastplayed"  {:cmd  :lastplayed :title true :sort 1
                   :help "[title]"
                   :examples ["?lastplayed antelope"]}
   "?showgap"     {:cmd  :showgap :title true :sort 1
                   :help "[title]"
                   :examples ["?showgap icculus"]}
   "?daygap"      {:cmd  :daygap :title true :sort 1
                   :help "[title]"
                   :examples ["?daygap 46 days"]}
   "?shortest"    {:cmd  :shortest :title true :sort 1
                   :help "[title]"
                   :examples ["?shortest runaway jim"]}
   "?longest"    {:cmd  :longest :title true :sort 1
                  :help "[title]"
                  :examples ["?longest bbfcfm"]}}
  )

(defn tp-parser [rst]
  (when rst
    (let [tp-match
          (cond
            (re-matches #"(\d\.0)\s+(.*)" rst) {:type :era :match (re-matches #"(\d\.0)\s(.*)" rst)}
            (re-matches #"(\d{4}|\d{1,2}-\d{4})\s+(\d{4}|\d{1,2}-\d{4})\s+(.*)" rst) {:type :se :match (re-matches #"(\d{4}|\d{1,2}-\d{4})\s+(\d{4}|\d{1,2}-\d{4})\s+(.*)" rst)}
            (re-matches #"(\d{4}|\d{1,2}-\d{4})\s+(.*)" rst) {:type :s :match (re-matches #"(\d{4}|\d{1,2}-\d{4})\s+(.*)" rst)})]
      (case (:type tp-match)
        :era (let [[_ era title] (:match tp-match)] {:era era :title title :type (:type tp-match)})
        :se (let [[_ start end title] (:match tp-match)] {:start (util/->date start) :end (util/->date end) :title title :type (:type tp-match)})
        :s (let [[_ start title] (:match tp-match)] {:start (util/->date start) :title title :type (:type tp-match)})
        nil))))

(defn icculizer
  "A dispatch function to break issued commands apart into their constituent parts. Returns a map with keys:

  ```
  {:cmd   :command
   :type  :command-type
   :era   1.0 ; if set
   :start DateTime ; if set
   :end   DateTime ; if set
   :title Song Title
  }
  ```
  "
  [text]
  (let [[_ cmd rst] (re-find #"^(\?\w+)(.*)" text)
        rst         (util/->empty->nil rst)
        command     (get commands cmd)]

    ;; Chain conditional command modifications
    (some-> command
            (#(if (:title %) (assoc % :title rst) %))       ; a simple query:title command
            (#(if (= :timesplayed (:cmd %))                 ; multiple possible date/era arrangements
                (merge % (tp-parser rst))
                %))
            (#(if (= :setlist (:cmd %))
                (assoc % :start (util/->date rst))
                %)))))


(comment

  (icculizer "?timesplayed 2017 2018 46 days")
  (icculizer "?timesplayed")
  (icculizer "?setlist 2018")
  (icculizer "?setlist")
  (icculizer "?shortest yem")
  
  (icculizer "?longest yem")


  (icculizer "?fam")

  (tp-parser "1.0 46 days")
  (tp-parser "2018 2019 46 days")
  (tp-parser "1-2018 2019 46 days")
  (tp-parser "2018 46 days")
  (tp-parser "z 2018 46 days")


  )