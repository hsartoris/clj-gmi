(ns clj-gmi.server
  "The server part - basically weavejester/tcp-server"
  (:require [clojure.java.io :as io])
  (:import [javax.net.ssl
            SSLServerSocket
            SSLServerSocketFactory
            SNIHostName]
           [java.net InetAddress SocketException]
           [java.io Closeable]))

(defn- create-server-socket 
  "Add an SSLServerSocket to the given config map"
  [{:keys [port backlog addr socket]}]
  (let [inet-addr (InetAddress/getByName addr)]
    (-> (SSLServerSocketFactory/getDefault)
        (.createServerSocket port backlog inet-addr)
        (->> (reset! socket)))))

(defn- configure-sni
  "Configure an SSLServerSocket with SNI for a given hostname."
  [{:keys [hostname socket]}]
  {:pre [@socket]}
  (let [sni-hostname (SNIHostName. hostname)
        ssl-params (-> (.getSSLParameters @socket)
                       (.setServerNames [sni-hostname]))]
    (.setSSLParameters @socket ssl-params)))

(defn running? [{:keys [socket]}]
  (when-some [socket @socket]
    (not (.isClosed socket))))

(defn close-conn [{:keys [conns]} conn]
  (swap! conns disj conn)
  (when-not (.isClosed conn)
    (.close conn)))

(defn- accept-conn
  [{:keys [handler conns socket] :as server}]
  (let [conn (.accept @socket)]
    (swap! conns conj conn)
    (future
      (try (handler conn)
           (finally (close-conn server conn))))))

(defn gmi-server
  [config handler]
  (merge
    {:port      1965
     :backlog   0
     :addr      "127.0.0.1"
     :hostname  "localhost.localdomain"
     :handler   handler
     :conns     (atom #{})
     :socket    (atom nil)}
    config))

(defn start! [server]
  (create-server-socket server)
  (configure-sni server)
  (while (running? server)
    (try
      (accept-conn server)
      (catch SocketException _))))

(defn stop! [{:keys [socket conns] :as server}]
  (doseq [conn @conns]
    (close-conn server conn))
  (.close @socket))
