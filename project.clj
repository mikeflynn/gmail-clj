(defproject gmail-clj "0.6"
  :description "A simple Clojure library to interface with the GMail API (read: not imap)"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [http-kit "2.1.16"]
                 [cheshire "5.3.1"]
                 [org.clojure/data.codec "0.1.0"]
                 [javax.mail/mail "1.4.3"]])
