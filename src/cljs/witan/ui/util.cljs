(ns ^:figwheel-always witan.ui.util
    (:require [cljs.core.async :refer [<! chan]]
              [venue.core :as venue]
              [cljs-time.core :as t]
              [cljs-time.format :as tf]
              [clojure.string :as str]
              [witan.ui.strings :refer [get-string]])
    (:require-macros [cljs.core.async.macros :as am :refer [go go-loop alt!]]
                     [cljs-log.core :as log]))

(def state (atom {:logged-in? false}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn contains-str
  "Performs a case-insensitive substring match"
  [source match]
  (not= -1 (.indexOf (.toLowerCase source) (.toLowerCase match))))

(defn contains-str-regex
  "Performs a case-insensitive regex match"
  [source pattern]
  (boolean (re-find (js/RegExp. pattern "i") source)))

(defn select-values
  "http://blog.jayfields.com/2011/01/clojure-select-keys-select-values-and.html"
  [m ks]
  (reduce #(if-let [v (m %2)] (conj %1 v) %1) [] ks))

(defn escape-html
  "Change special characters into HTML character entities."
  [text]
  (-> text
      (str)
      (str/replace "&"  "&amp;")
      (str/replace "<"  "&lt;")
      (str/replace ">"  "&gt;")
      (str/replace "\"" "&quot;")))

(defn str-fmt-map
  "String format with map using mustache delimiters, e.g. (str-fmt-map 'hello {{name}}' {:name 'foo'})"
  [s m]
  (let [matches (re-seq #"\{\{\s*([0-9a-zA-Z_\.\-|]+)\s*\}\}" s)
        get-key (fn [s] (vec (map keyword (str/split s "."))))
        filter-fns {"nowhitespace" (fn [s] (str/replace s #"\s+" ""))}
        run-filters (fn [s fs] ((apply comp (select-values filter-fns fs)) s))]
    (reduce
     (fn [a [match key]]
       (let [split-key (str/split key "|")
             good-key (first split-key)
             filters (rest split-key)]
         (str/replace a match (some-> m
                                      (get-in (get-key good-key))
                                      (escape-html)
                                      (run-filters filters))))) s matches)))

(defn goto-window-location!
  "Sends the window to the specified location"
  [location]
  (set! (.. js/document -location -href) location))

(defn inline-subscribe!
  "Sugar for adding a subscription to the venue message bus"
  [topic fnc]
  (let [ch (chan)]
    (venue/subscribe! topic ch)
    (go-loop []
      (let [{:keys [content]} (<! ch)]
        (fnc content))
      (recur))))

(defn add-ns
  "Adds a namespace to a keyword"
  [ns key]
  (let [sns (name ns)
        skey (name key)]
    (keyword sns skey)))

(defn remove-ns
  "Removes a namespace from a keyword"
  [key]
  (-> key name keyword))

(defn map-add-ns
  "Adjusts an entire map by adding namespace to all the keys"
  [ns m]
  (reduce (fn [a [k v]] (assoc a (add-ns ns k) v)) {} m))

(defn map-remove-ns
  "Adjusts an entire map by removing namespace to all the keys"
  [m]
  (reduce (fn [a [k v]] (assoc a (remove-ns k) v)) {} m))

(defn sanitize-filename
  "Removed slashes from a filename"
  [filename]
  (.replace filename #".*[\\\/]" ""))

(defn humanize-time
  "Converts timestamp to a human readable time"
  [time-str]
  (when (not-empty time-str)
    (let [now  (t/now)
          time (tf/parse (:date-hour-minute-second tf/formatters) time-str)
          calfn (juxt t/day t/month t/year)
          clock (tf/unparse (tf/formatter "h:mm A") time)
          front (cond
                  (= (t/day time) (t/day now)) (get-string :today)
                  (= (calfn (t/yesterday)) (calfn time)) (get-string :yesterday)
                  :default (tf/unparse (tf/formatter "MMMM dth") time))]
      (str front ", " clock))))

(defn squash-maps
  "Squashes m2 into m1, replacing maps with matching keys based on id-fn"
  [m1 m2 id-fn]
  (let [m1-vals (set (map id-fn m1))
        m2-vals (set (map id-fn m2))
        inter-vals (clojure.set/intersection m1-vals m2-vals)]
    (if (empty? inter-vals)
      (concat m1 m2)
      (let [m1-removed (remove #(contains? inter-vals (id-fn %)) m1)]
        (concat m1-removed m2)))))
