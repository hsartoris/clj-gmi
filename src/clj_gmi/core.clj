(ns clj-gmi.core
  (:require [clj-gmi.server :as s]
            [clj-gmi.routes :refer [handler]])
  (:gen-class))

(defonce srv (s/gmi-server {:addr "0.0.0.0"} handler))


(defn -main
  "I don't do a whole lot ... yet."
  [& args]
  (println "Hello, World!"))
