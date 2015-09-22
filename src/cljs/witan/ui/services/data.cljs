(ns witan.ui.services.data
  (:require [cljs.core.async :refer [put! take! chan <! close!]]
            [witan.ui.util :as util]
            [venue.core :as venue])
  (:require-macros [cljs-log.core :as log]))

(def state (atom {:logged-in? false}))

(defn logged-in? [] (:logged-in? @state))

(defmulti service
  (fn [event args result-ch] event))

;;;;;;;;;;;;;;;;;;;;;

(defn do-login!
  []
  (log/debug "LOGGED IN")
  (swap! state assoc :logged-in? true)
  (venue/reactivate!))

(util/inline-subscribe!
 (chan) :user-logged-in
 #(do-login!))
