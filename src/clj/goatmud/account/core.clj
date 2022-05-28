(ns goatmud.account.core
  (:require [malli.core :as m]
            [clojure.tools.logging :as log]
            [goatmud.account.schema :as s]))

(defonce next-account-id (atom 1))
(defonce accounts (atom []))

(defn create-account!
  [username email password]
  (cond
    (some #(= (:email %) email) @accounts)
    (throw (ex-info "Duplicate Email"
                    {:errors {:email ["Duplicate email"]}}))

    (some #(= (:username %) username) @accounts)
    (throw (ex-info "Duplicate Username"
                    {:errors {:username ["Duplicate username"]}}))

    true
    (let [account-id (swap! next-account-id inc)
          account {:id account-id :username username :email email :password password}]
      (swap! accounts conj account)
      (log/info "Created account " (select-keys account [:id :email]))
      account)))
