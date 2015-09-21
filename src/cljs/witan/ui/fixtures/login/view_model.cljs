(ns ^:figwheel-always witan.ui.fixtures.login.view-model
    (:require [om.core :as om :include-macros true]
              [venue.core :as venue]
              [witan.ui.strings :as s])
    (:require-macros [cljs-log.core :as log]))

(defmulti event-handler
  (fn [event args cursor] event))

(defmulti response-handler
  (fn [result response cursor] result))

(defn view-model
  []
  (reify
    venue/IHandleEvent
    (handle-event [owner event args cursor]
      (event-handler event args cursor))
    venue/IHandleResponse
    (handle-response [owner outcome event response cursor]
      (response-handler [event outcome] response cursor))
    venue/IActivate
    (activate [owner args cursor])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

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
