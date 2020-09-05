(ns clj-gmi.server
  "The server part"
  (:require [clojure.java.io :as io])
  (:import [javax.net.ssl
            SSLServerSocket
            SSLServerSocketFactory
            SNIHostName]
           [java.net InetAddress]
           [java.io Closeable]))

(defonce connections (atom #{}))
(defonce server (atom nil))

(def ^:private default-config
  {:port      1965
   :backlog   0
   :addr      "127.0.0.1"
   :hostname  "localhost.localdomain"})

(defrecord GeminiServer [port backlog addr hostname socket handler conns]
  Closeable
  (close [_]
    (when-not (and (nil? socket) (.isClosed socket))
      (.close socket))))

(defn- create-server-socket 
  "Add an SSLServerSocket to the given config map"
  [{:keys [port backlog addr] :as server}]
  (let [inet-addr (InetAddress/getByName addr)]
    (-> (SSLServerSocketFactory/getDefault)
        (.createServerSocket port backlog inet-addr)
        (->> (assoc server :socket)))))

(defn- configure-sni
  "Configure an SSLServerSocket with SNI for a given hostname."
  [{:keys [hostname socket] :as server}]
  {:pre [socket]}
  (let [sni-hostname (SNIHostName. hostname)
        ssl-params (-> (.getSSLParameters socket)
                       (.setServerNames [sni-hostname]))]
    (.setSSLParameters socket ssl-params)
    server))

(defn configure
  [config]
  (-> (merge default-config config)
      (assoc :conns (atom #{}))
      (->> (reset! server))))

(defn start! 
  ([] (swap! server create-server-socket))
  ([config]
   (configure config)
   (start!)))

(defn stop! []
  (
