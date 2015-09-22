(ns ^:figwheel-always witan.ui.fixtures.login.view-model
    (:require [om.core :as om :include-macros true]
              [venue.core :as venue]
              [witan.ui.strings :as s])
    (:require-macros [cljs-log.core :as log]
                     [witan.ui.macros :as wm]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(wm/create-standard-view-model!)

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn on-initialise
  [cursor])

(defn on-activate
  [args cursor])

(defmethod event-handler
  :event/reset-password
  [_ email cursor]
  (log/warn "TODO Reset instruction received in view-model"))

(defmethod event-handler
  :event/show-password-reset
  [_ show cursor]
  (if show
    (om/update! cursor :phase :reset)
    (om/update! cursor :phase :prompt)))

(defmethod event-handler
  :event/attempt-login
  [_ {:keys [email pass]} cursor]
  (om/update! cursor :phase :waiting)
  (om/update! cursor :email email)
  (venue/request! cursor :service/api :login [email pass]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmethod response-handler
  [:login :success]
  [_ response cursor]
  (if response
    (do
      (om/update! cursor :logged-in? true))
    (do
      (om/update! cursor :logged-in? true)
      ;;
      (om/update! cursor :message (s/get-string :sign-in-failure))
      (om/update! cursor :phase :prompt))))

(defmethod response-handler
  [:login :failure]
  [_ response cursor]
  (om/update! cursor :message (s/get-string :api-failure))
  (om/update! cursor :phase :prompt))
