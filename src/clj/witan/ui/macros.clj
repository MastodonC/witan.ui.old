(ns witan.ui.macros
  (:require [cljs.core]))

(defmacro create-standard-view-model!
  []
  `(do
     (cljs.core/declare ~'on-initialise)
     (cljs.core/declare ~'on-activate)

     (cljs.core/defmulti ~'event-handler
       (cljs.core/fn [event# args# cursor#] event#))

     (cljs.core/defmulti ~'response-handler
       (cljs.core/fn [result# response# cursor#] result#))

     (cljs.core/defn ~'view-model
        []
        (cljs.core/reify
          venue/IHandleEvent
          (~'handle-event [owner# event# args# cursor#]
                          (~'event-handler event# args# cursor#))
          venue/IHandleResponse
          (~'handle-response [owner# outcome# event# response# cursor#]
                             (~'response-handler [event# outcome#] response# cursor#))
          venue/IActivate
          (~'activate [owner# args# cursor#]
                      (~'on-activate args# cursor#))
          venue/IInitialise
          (~'initialise [owner# cursor#]
                        (~'on-initialise cursor#))))))
