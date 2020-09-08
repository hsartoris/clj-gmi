(ns clj-gmi.routes
  (:require [reitit.core :as r]
            [clj-gmi.response :as response]
            [clojure.java.io :as io]
            [lambdaisland.uri :refer [uri]]))

(defn respond
  [sock header & [body]]
  (println "Responding; header: " header)
  (with-open [o (io/writer sock)]
    (.write o header)
    (when body
      (.write o body)) 
    (.flush o)))

(defn text-handler
  [text]
  (fn [sock]
    (respond sock (response/success :text/gemini)
             (str text "\r\n"))))

(def router
  (r/router [["/" {:name ::root
                   :handler (text-handler "# Welcome to my page")}]
             ["/about" {:name ::about}]
             ["/test2/:id" {:name ::test2}]
             ["/test/:id" {:name ::test
                           :parameters {:path {:id int?}}}]]))

(defn parse-uri [s]
  (prn s)
  (let [sub (re-find #"^[^\r]*" s)
        {:keys [scheme host port path query user password]
         :or {path "/"}} (uri s)
        err response/perm-failure]

    (println "Scheme: " scheme)
    (println "Host: " host)
    (println "Path: " path)
    (cond
      (and scheme (not= "gemini" scheme))
      (err "Only gemini scheme is accepted")

      (nil? host)
      (err "Hostname is required")

      (or user password)
      (err "userinfo component of URI is disallowed")

      (nil? path)
      (err "Some path must be supplied")

      ; TODO
      :else {:path path})))
                       
(defn handler [sock]
  (println "Handling request")
  (with-open [i (io/reader sock)]
    (let [request (.readLine i)
          {:keys [error path]} (parse-uri request)
          match (r/match-by-path router (or path ""))]
      (cond
            (some? error) (respond sock error)
            (some? match) ((:result match) sock)
            :else (respond sock (response/temp-failure "Not found"))))))
