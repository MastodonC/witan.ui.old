(ns witan.ui.controllers.user
  (:require [witan.ui.ajax :refer [GET POST endpoint]]
            [schema.core :as s]
            [om.next :as om]
            [witan.ui.data :as data])
  (:require-macros [cljs-log.core :as log]
                   [witan.ui.env :as env :refer [cljs-env]]))

(def Login
  {:user/username (s/constrained s/Str #(> (count %) 5))
   :user/password (s/constrained s/Str #(> (count %) 7))})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn kill-login-screen!
  []
  (let [login-div (.getElementById js/document "login")]
    (aset login-div "style" "visibility" "hidden")))

(defn login-success!
  [owner response]
  (om/transact! owner `[(login/complete! ~response)])
  (data/save-data!)
  (kill-login-screen!))

(defn local-endpoint
  [method]
  (str endpoint method))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti api-response
  (fn [{:keys [event status]} response] [event status]))

(defmethod api-response
  [:login :success]
  [{:keys [owner]} {:keys [session/token] :as response}]
  (if token
    (login-success! owner response)
    (om/transact! owner '[(login/set-message! {:message :string/sign-in-failure})])))

(defmethod api-response
  [:login :failure]
  [{:keys [owner]} response]
  (om/transact! owner '[(login/set-message! {:message :string/api-failure})]))

(defn route-api-response
  [event owner]
  (fn [status response]
    (api-response {:owner owner :event event :status status} response)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti handle
  (fn [event owner args] event))

(defmethod handle :login
  [event owner {:keys [email pass]}]
  (let [args {:user/username email :user/password pass}]
    (POST (local-endpoint "/login")
          {:id event
           :params (s/validate Login args)
           :result-cb (route-api-response event owner)})))

(defmethod handle :logout
  [event owner {:keys [email pass]}]
  (data/delete-data!)
  (.replace js/location "/" true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(data/subscribe-topic
 :data/app-state-restored
 #(kill-login-screen!))
