(ns witan.ui.services.api
  (:require [ajax.core :as ajax]
            [cljs.core.async :refer [put! take! chan <! close!]]
            [venue.core :as venue])
  (:require-macros [cljs-log.core :as log]
                   [cljs.core.async.macros :refer [go]]))

(def ^:private api-token (atom nil))

(defmulti service-m
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

(defn GET
  [event method params result-ch]
  (ajax/GET (local-endpoint method)
   {:params params
    :handler (partial handle-response :success event result-ch)
    :error-handler (partial handle-response :failure event result-ch)
    :format :json
    :headers {"Authorization" (str "Token " @api-token)}}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn service
  [event args result-ch]
  (if (or (= event :login) @api-token)
    (service-m event args result-ch)
    (do
      (log/warn "An API request was received but there is no token so the outbound call will not be made.")
      (put! result-ch [:failure :no-token]))))

(defmethod service-m
  :login
  [event [email pass] result-ch]
  (POST event "/login" {:username email :password pass} result-ch))

(defmethod service-m
  :refresh-forecasts
  [event _ result-ch]
  (GET event "/forecasts" {:user "foobar"} result-ch))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod api-response
  [:login :success]
  [_ response]
  (if-let [token (:token response)]
    (do
      (log/info "Login success.")
      (reset! api-token token)
      (venue/publish! :api/user-logged-in {:name "foobar"})
      true)
    (do
      (log/info "Login failed.")
      (log/debug "Response:" response)
      false)))

(defmethod api-response
  [:refresh-forecasts :success]
  [_ response]
  (venue/publish! :api/forecasts-refreshed response))

(defmethod api-response
  :default
  [_ response])
