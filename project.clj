(defproject clj-gmi "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.1"]
                 [lambdaisland/uri "1.4.54"]
                 [metosin/reitit-core "0.5.5"]]
  :main ^:skip-aot clj-gmi.core
  :target-path "target/%s"
  :jvm-opts ["-Djavax.net.ssl.keyStore=resources/keystore.jks"
             "-Djavax.net.ssl.keyStorePassword=changeit"]
  :profiles {:uberjar {:aot :all
                       :jvm-opts ["-Dclojure.compiler.direct-linking=true"]}})
