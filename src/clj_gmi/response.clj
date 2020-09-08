(ns clj-gmi.response)

(def ^:private response-header-fmt "%02d %s\r\n") 

(defn- fmt-hd [code meta]
  (format response-header-fmt code meta))

(defn- namespaced-name
  "If `kw` is a keyword, return the stringified version of it. Works on
  namespaced keywords, e.g. `:text/gemini`"
  [kw]
  (if (keyword? kw)
    (subs (str kw) 1)
    kw))

(defn prompt [msg]
  (fmt-hd 10 msg))

(defn success [mime-type]
  (fmt-hd 20 (namespaced-name mime-type)))

(defn redirect [url]
  (fmt-hd 30 url))

(defn temp-failure
  ([] (temp-failure "Temporary error"))
  ([msg] (fmt-hd 40 msg)))

(defn perm-failure
  ([] (perm-failure "Permanent failure"))
  ([msg] (fmt-hd 50 msg)))

(defn client-cert
  ([] (client-cert "Client certificate required"))
  ([msg] (fmt-hd 60 msg)))
