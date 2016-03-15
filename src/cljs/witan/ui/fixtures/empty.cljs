(ns witan.ui.fixtures.empty
  (:require [om.core :as om :include-macros true]
            [om-tools.core :refer-macros [defcomponent]]
            [venue.core :as venue])
  (:require-macros [cljs-log.core :as log]
                   [witan.ui.macros :as wm]))

(wm/create-standard-view-model! {})

(defcomponent view
  [cursor owner & opts]
  (render [_]))
