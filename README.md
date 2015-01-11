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

## Authorizing

This library takes your API information and will do an authorization based on a refresh token. (*Note: If your app doesn't request a refresh token, you can simply set the access-token with gmail/set-access-token!, but we can't re-authorize after that token expires without a refresh token.*)

To generate the refresh token you need to write your own interface with the Google OAuth endpoint, but for development and testing I've included an HTML file that does the basic OAuth handshake and returns the refresh token: [/resources/index.html](https://github.com/mikeflynn/gmail-clj/blob/master/resources/index.html).

## To Do

Here's a few things I haven't gotten to yet, mostly because I didn't need them in my specific use case. If you'd like to dive in an help on these, that would much appreciated!

1. Sending email is very basic at the moment (to, subject, body): no bcc, no cc, no multiple addresses bcc / cc / to.
2. All endpoints are hard-coded to the "me" authorized email identifier at this time.
3. Email attachments!!

## License

Copyright © 2014 Mike Flynn / [@thatmikeflynn](http://twitter.com/thatmikeflynn)

Distributed under the Eclipse Public License, the same as Clojure.
