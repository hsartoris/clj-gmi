(ns clj-gmi.server
  "The server part - basically weavejester/tcp-server"
  (:require [clojure.java.io :as io]
            [lambdaisland.uri :refer [uri]]
            [reitit.core :as r])
  (:import [javax.net.ssl
            SSLServerSocket
            SSLServerSocketFactory
            SNIHostName]
           [java.net InetAddress SocketException]
           [java.io Closeable]))

(defn get-server-socket 
  [{:keys [port backlog addr socket hostname]}]
  (let [sock (.createServerSocket (SSLServerSocketFactory/getDefault)
                                  port
                                  backlog
                                  (InetAddress/getByName addr))
        ssl-params (.getSSLParameters sock)]
    (.setServerNames ssl-params [(SNIHostName. hostname)])
    (.setSSLParameters sock ssl-params)
    sock))

(defn running? [{:keys [socket]}]
  (when-some [socket @socket]
    (not (.isClosed socket))))

(defn close-connection 
  [{:keys [connections]} conn]
  (swap! connections disj conn)
  (println "Closing connection: " conn)
  (when-not (.isClosed conn)
    (.close conn)))

(defn- accept-connection
  [{:keys [handler connections socket] :as server}]
  (let [conn (.accept @socket)]
    (println "Connection opened: " conn)
    (swap! connections conj conn)
    (future
      (try (handler conn)
           (finally (close-connection server conn))))))

(defn gmi-server
  [config handler]
  (merge
    {:port      1965
     :backlog   0
     :addr      "127.0.0.1"
     :hostname  "localhost.localdomain"
     :handler   handler
     :connections (atom #{})
     :socket    (atom nil)}
    config))

(defn start! [server]
  (reset! (:socket server)
          (get-server-socket server))
  (future 
    (while (running? server)
      (try
        (accept-connection server)
        (catch SocketException s
          (.printStackTrace s))))))

(defn stop! 
  [{:keys [socket connections] :as server}]
  (println "closing connections")
  (doseq [conn @connections]
    (println "Closing connection " conn)
    (close-connection server conn))
  (println "shutting down server")
  (.close @socket))

(defn wrap-streams
  [handler]
  (fn [sock]
    (with-open [input   (.getInputStream sock)
                output  (.getOutputStream sock)]
      (handler input output))))

(defn error [s]
  {:error s})

(defn parse-uri [s]
  (prn s)
  (let [sub (re-find #"^[^\r]*" s)
        {:keys [scheme host port path query user password]
         :or {path "/"}} (uri s)]

    (println "Scheme: " scheme)
    (println "Host: " host)
    (println "Path: " path)
    (cond
      (and scheme (not= "gemini" scheme))
      (error "Only gemini scheme is accepted")

      (nil? host)
      (error "Hostname is required")

      (or user password)
      (error "userinfo component of URI is disallowed")

      (nil? path)
      (error "Some path must be supplied")

      ; TODO
      :else {:path path})))

(defn route-handler
  [router]
  (fn handler [sock]
    (with-open [i (io/reader sock)
                o (io/writer sock)]
      (let [request (.readLine i)
            {:keys [error path]} (parse-uri request)
            match (r/match-by-path router (or path ""))]
        (println error)
        (println path)
        (cond
          (some? error) (doto o
                          (.write error)
                          (.write "\r\n")
                          (.flush))

          (some? match) ((:result match) o)
          :else (doto o
                  (.write "Not found\r\n")
                  (.flush)))))))

(defn handler
  [sock]
  (with-open [i (io/reader sock)
              o (io/writer sock)]
    (let [request (.readLine i)]

    (println (.readLine i))
    (doto o
      (.write "hello there\r\n")
      (.flush))
    (println (.readLine i)))))
