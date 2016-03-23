(ns witan.ui.style.app
  (:require [garden.units :refer [px percent em]]
            [witan.ui.style.colour :as colour]))

(def pc100 (percent 100))
(def switcher-padding 3)
(def switcher-icon-dx 32)

(def mock-map-ratio 1.5)

(def style [[:#app
             {:background-color colour/body-bg}

             ;; GENERAL
             [:button.pure-button
              {:align-self :center
               :box-shadow [[(px 2) (px 2) (px 4) colour/box-shadow]]}
              [:.material-icons
               {:vertical-align :middle}]
              [:i
               {:margin [[(px -3) (px 5) (px 0) (px 0)]]}]]

             [:hr
              {:height 0
               :margin [[(px 15) (px 0)]]
               :overflow :hidden
               :background :transparent
               :border 0
               :border-bottom [[(px 1) 'solid "#ddd"]]}]

             [:#create-workspace
              [:#content
               {:padding [[(em 0) (em 1)]]
                :width (px 700)}
               [:h2
                [:em
                 {:font-size (px 12)
                  :margin-left (px 5)
                  :font-style :normal
                  :color 'gray}]]
               [:button
                {:background-color colour/button-create
                 :color colour/body-bg}]]]

             [:#split
              {:height pc100
               :width  pc100
               :top (px 0)
               :position :absolute}
              [:#loading
               {:width pc100
                :height pc100
                :background-color colour/body-bg
                :position :absolute
                :top (px 0)
                :display :table
                :text-align :center}
               [:div
                {:display :table-cell
                 :vertical-align :middle}]]]

             [:#primary
              [:#container
               {:height pc100
                :position :relative}
               [:#mock
                {:height pc100
                 :width pc100}
                [:#map
                 {:background "url ('../img/london2.png') no-repeat center 30px"
                  :background-size [[(px (/ 900 mock-map-ratio)) (px (/ 695 mock-map-ratio))]]
                  :width pc100
                  :height pc100}]
                [:#title
                 {:position :absolute
                  :top (px 0)
                  :left (px 100)}
                 [:h3
                  {:line-height (px 0)}]]
                [:#left-desc
                 {:position :absolute
                  :left (px 8)
                  :top (px 80)}
                 [:i
                  {:color "#666"}]]
                [:#slider
                 {:position :absolute
                  :height (px 3)
                  :left (px 120)
                  :right (px 120)
                  :bottom (px 40)
                  :border-bottom [[(px 1) 'solid "#aaa"]]}
                 [:#text
                  {:display :flex
                   :justify-content :space-between
                   :margin-top (px -16)}]
                 [:i
                  {:color colour/gutter
                   :margin-left (percent 13)
                   :margin-top (px -10)}]]
                [:#selector
                 {:position :absolute
                  :top (px 8)
                  :right (px 8)
                  :display :inline-flex}
                 [:select
                  {:height (px 32)}]
                 [:button
                  {:background-color colour/switcher-bg
                   :width (px 42)
                   :margin-top (px -2)
                   :margin-left (px 4)}]]]
               [:#overlay
                {:position :absolute
                 :top (px 8)
                 :left (px 8)}]]]]

            [:.primary-switcher
             {:height (px (+ (* switcher-padding 2) switcher-icon-dx))
              :width (px (+ (* switcher-padding 4) (* switcher-icon-dx 2)))
              :background-color colour/switcher-bg
              :box-shadow [[(px 2) (px 2) (px 4) colour/box-shadow]]
              :border-radius (px 3)}
             [:.icons
              {:position :absolute}
              ^:prefix {:user-select :none}
              [:.icon
               {:padding (px switcher-padding)
                :cursor :pointer
                :display :table-cell
                :opacity 0.5}]
              [:.selected
               {:opacity 1
                :cursor :default}]]
             [:#indicator-container
              {:margin-left (px switcher-padding)
               :position :relative}
              [:#indicator
               {:height (px switcher-icon-dx)
                :width (px switcher-icon-dx)
                :margin-top (px switcher-padding)
                :position :absolute
                :background-color colour/body-bg
                :border-radius (px 3)
                :top (px 0)
                :transition "margin-left 0.1s"}]]
             [:.indicator-offset-1
              {:margin-left (px (+ (* switcher-padding 2) switcher-icon-dx))}]]

            [:.secondary-switcher
             [:button
              {:border-radius 0
               :background-color colour/switcher-bg
               :margin 0
               :border-left [[(px 1) 'solid colour/side-bg]]
               :box-shadow [[ (px 0) (px 2) (px 4) colour/box-shadow]]
               :font-weight :bold}]
             [:.selected
              {:background-color colour/switcher-button-selected
               :pointer-events :none}]]])
