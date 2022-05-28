(ns goatmud.validation
  (:require [malli.core :as m]
            [malli.error :as me]))

(defn validate-with
  [params schema]
  (when-let [results (m/explain schema params)]
    (me/humanize results)))
