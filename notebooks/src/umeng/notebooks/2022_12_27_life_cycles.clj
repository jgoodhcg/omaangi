(ns umeng.notebooks.2022-12-27-life-cycles
  (:import [org.shredzone.commons.suncalc MoonPhase SunTimes MoonPosition MoonIllumination])
  (:require [scicloj.kindly.v3.kind :as kind]))


;; Examples of libary interop and usage
(-> SunTimes
    (. compute)
    (. on 2020 5 1)
    (. at 40.712778 -74.005833)
    (. execute))

(-> MoonPhase
    (. compute)
    (. phase 270.0)
    (. at 40.712778 -74.005833)
    (. execute))

(-> MoonPhase
    (. compute)
    (. at 40.712778 -74.005833)
    (. execute))

(-> MoonPosition
    (. compute)
    (. on 2023 1 24)
    (. at 40.712778 -74.005833)
    (. execute))

(-> MoonIllumination
    (. compute)
    (. on 2023 1 24)
    (. at 40.712778 -74.005833)
    (. execute)
    (. getClosestPhase)
    (. toString))
