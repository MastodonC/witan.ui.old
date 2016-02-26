(ns witan.styles.fonts
  (:require [garden.stylesheet :as gs]))

(def font-face-definitions
  [(gs/at-font-face
    {:font-family "'Fira Sans', sans-serif"})
   (gs/at-font-face
    {:font-family "'Kadwa', serif"})])

(def base-fonts  ["Helvetica Neue" "Helvetica" "Arial" "sans-serif"])
(def title-fonts ["Helvetica Neue" "Helvetica" "Arial" "sans-serif"])
