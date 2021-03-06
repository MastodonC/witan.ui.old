(ns witan.ui.services.analytics
  (:require [witan.ui.util :as util]
            [cljs.core.async :refer [close! put!]]
            [venue.core :as venue]
            [cljs-time.core :as t]
            [cljs-time.format :as tf])
  (:require-macros [cljs-log.core :as log]
                   [witan.ui.macros :as wm]))

(defn log-event
  [message]
  (log/debug "Analytics:" message))

(defn do-update
  []
  (log/debug "Intercom update")
  (.Intercom js/window "update"))

(defn on-initialise
  []
  (log-event "System initialised.")
  (.setInterval js/window do-update (* 1000 60 4))) ;; every 4 mins

(defmulti request-handler
  (fn [event args result-ch]
    (log-event (str event " - " args))
    event))

(defmethod request-handler
  :track-create-forecast
  [event args result-ch]
  (.Intercom js/window "trackEvent" "created a forecast" (clj->js args))
  (put! result-ch [:success nil]))

(defmethod request-handler
  :track-create-forecast-version
  [event args result-ch]
  (.Intercom js/window "trackEvent" "updated a forecast" (clj->js args))
  (put! result-ch [:success nil]))

(defmethod request-handler
  :track-public-download
  [event args result-ch]
  (.Intercom js/window "trackEvent" "downloaded a public file" (clj->js args))
  (put! result-ch [:success nil]))

(defmethod request-handler
  :track-output-download
  [event args result-ch]
  (.Intercom js/window "trackEvent" "downloaded a model output file" (clj->js args))
  (put! result-ch [:success nil]))

(defmethod request-handler
  :track-upload
  [event args result-ch]
  (.Intercom js/window "trackEvent" "uploaded a file" (clj->js (update args :response str)))
  (put! result-ch [:success nil]))

(defmethod request-handler
  :default
  [event args result-ch]
  (log/warn "Ignoring request to track event:" event)
  (put! result-ch [:failure nil]))

(defn service
  []
  (reify
    venue/IHandleRequest
    (handle-request [owner request args response-ch]
      (request-handler request args response-ch))
    venue/IInitialise
    (initialise [owner _]
      (on-initialise))))

;;;;;;;;;;;;;;;;;;;;;

(defmulti do-login (fn [l response] l))
(defmethod do-login true
  [_ {:keys [id username name]}]
  (log-event "Switching ON Intercom")
  (.Intercom js/window "boot"
             (clj->js {:app_id "hwl3xxh2"
                       :user_id id
                       :email username
                       :name name
                       :widget {:activator "#IntercomDefaultWidget"}})))

(defmethod do-login false
  [_ _]
  (log-event "Switching OFF Intercom")
  (.Intercom js/window "shutdown"))

;;;;;;;;;;;;;;;;;;;;;

(util/inline-subscribe!
 :api/user-identified
 #(do-login true %))

(util/inline-subscribe!
 :api/user-logged-out
 #(do-login false))
