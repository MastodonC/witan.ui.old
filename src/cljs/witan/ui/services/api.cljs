(ns witan.ui.services.api
  (:require [ajax.core :as ajax]
            [cljs.core.async :refer [put! take! chan <! close!]])
  (:require-macros [cljs-log.core :as log]
                   [cljs.core.async.macros :refer [go]]))

(defmulti service
  (fn [event args result-ch] event))

(defmulti api-response
  (fn [event-status response] event-status))

(defn local-endpoint
  [method]
  (str "http://localhost:3000" method))

(defn- handle-response
  [status event result-ch response]
  (if (= status :failure)
    (log/severe "An API error occurred: " event))
  (put! result-ch [status (api-response [event status] (clojure.walk/keywordize-keys response))]))

(defn POST
  [event method params result-ch]
  (ajax/POST (local-endpoint method)
             {:params params
              :handler (partial handle-response :success event result-ch)
              :error-handler (partial handle-response :failure event result-ch)
              :format :json}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod service
  :login
  [event [email pass] result-ch]
  (POST event "/login" {:username email :password pass} result-ch))

;;
(defmethod api-response
  [:login :success]
  [_ response]
  (let [token (:token response)]
    (if token
      (do
        (log/info "Login success.")
        true)
      (do
        (log/info "Login failed.")
        (log/debug "Response:" response)
        false))))

(defmethod api-response
  :default
  [_ response])
