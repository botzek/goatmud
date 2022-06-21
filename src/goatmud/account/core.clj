(ns goatmud.account.core
  (:require [malli.core :as m]
            [clojure.tools.logging :as log]
            [clojure.edn :as edn]
            [buddy.hashers :as hashers]
            [integrant.core :as ig]
            [goatmud.account.schema :as s]))

(defonce db (atom nil))
(defonce db-file (agent nil))

(defn- save-db!
  [file-version db]
  (let [{:keys [version filename accounts]} db]
    (if (> version file-version)
      (do
        (spit filename accounts)
        version)
      file-version)))

(defmethod ig/init-key :account/core
  [_ {:keys [filename]}]
  (let [accounts-str (slurp filename)
        accounts (edn/read-string accounts-str)
        top-account-id (apply max (conj (keys accounts) 0))]
    (log/info "Init accounts found [" (count accounts) "] accounts, top account id [" top-account-id "]")
    (reset! db {:version 1 :accounts accounts :top-account-id top-account-id :filename filename})
    (send db-file (fn [_] 1))
    (add-watch db :save-db (fn save-db[_key ref _old-state _new-state]
                             (send-off db-file save-db! @ref)))))

(defmethod ig/halt-key! :account/core
  [_ _]
  (remove-watch db :save-db)
  (let [{:keys [filename accounts]} @db]
    (spit filename accounts)
    (send db-file (fn [_] nil))
    (reset! db nil)))

(defn create-account!
  [username email password]
  (let [result (atom nil)]
    (swap! db (fn create-account [{:keys [top-account-id accounts version] :as db}]
                (cond
                  (some #(= (:email %) email) (vals accounts))
                  (throw (ex-info "Duplicate Email" {:errors {:email ["Duplicate email"]}}))

                  (some #(= (:username %) username) (vals accounts))
                  (throw (ex-info "Duplicate Username" {:errors {:username ["Duplicate username"]}}))

                  true
                  (let [account-id (inc top-account-id)
                        password-hash (hashers/derive password)
                        account {:id account-id :username username :email email :password-hash password-hash}
                        accounts (assoc accounts account-id account)]
                    (reset! result account)
                    (assoc db :top-account-id account-id :accounts accounts :version (inc version))))))
    (log/info "Created account" (select-keys @result [:id :username :email]))
    @result))
