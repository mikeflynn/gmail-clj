# `gmail-clj`

A Clojure library designed to abstract the details of the GMail REST API: [developers.google.com/gmail/api/](https://developers.google.com/gmail/api/)

## Installation

`gmail-clj` is available as a Maven artifact from [Clojars](https://clojars.org/gmail-clj):

![](https://clojars.org/gmail-clj/latest-version.svg)

## Usage

Require the library in your REPL:

```clojure
  (require '[gmail-clj.core :as gmail])
```

...or in your project.clj file

```clojure
  (ns my-app.core
    (:require [gmail-clj.core :as gmail]))
```

Be sure to set your Google API Application Key and Secret:

```clojure
  (gmail/set-client-secret! "aaaaa")
  (gmail/set-client-id! "bbbbbb")
  (gmail/set-refresh-token! "ccccc")
  (gmail/set-access-token! "dddddd") ; Optional. If you already have an access token.
```

## To Do

1. Sending email is very basic at the moment (to, subject, body): no attachments, no bcc, no cc, no multiple addresses bcc / cc / to.
2. All endpoints are hard-coded to the "me" authorized email identifier at this time.

## License

Copyright © 2014 Mike Flynn / [@mikeflynn_](http://twitter.com/mikeflynn_)

Distributed under the Eclipse Public License, the same as Clojure.
