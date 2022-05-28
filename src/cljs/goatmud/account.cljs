(ns goatmud.account
  (:require [reagent.core :as r]
            [re-frame.core :as rf]
            [clojure.string :as string]
            [goatmud.account.web-schema :refer [create-account-schema]]
            [goatmud.validation :refer [validate-with]]))

(rf/reg-event-fx
 :account/create-account
 (fn [{:keys [db]} [_ fields errors]]
   (if-let [e (validate-with @fields create-account-schema)]
     (do
       (reset! errors e)
       nil)
     {:ajax/post {:url "/api/accounts"
                  :params @fields
                  :error-handler (fn [resp]
                                   (if-let [e (-> resp :response :errors)]
                                     (reset! errors e)
                                     (reset! errors {:? "request failed"})))
                  :success-handler (fn [_]
                                     (reset! errors nil)
                                     (reset! fields nil))}})))

(defn errors-component [errors id]
  (when-let [error (id @errors)]
    [:div.notification.is-danger (string/join error)]))

(defn create-account-dialog
  []
  (r/with-let [fields (r/atom {})
               errors (r/atom nil)]
    [:form
     {:on-submit (fn on-submit[e]
                   (rf/dispatch [:account/create-account fields errors])
                   (.preventDefault e))}
     [:div
      [:div.label "Username"]
      [:div [:input {:type :text
                     :value (:username @fields)
                     :on-change #(swap! fields assoc :username (.. % -target -value))}]]
      [errors-component errors :username]]
     [:div
      [:div.label "Email"]
      [:div [:input {:type :text
                     :value (:email @fields)
                     :on-change #(swap! fields assoc :email (.. % -target -value))}]]
      [errors-component errors :email]]
     [:div
      [:div.label "Password"]
      [:div [:input {:type :password
                     :value (:password @fields)
                     :on-change #(swap! fields assoc :password (.. % -target -value))}]]
      [errors-component errors :password]]
     [:div
      [:div.label "Confirm Password"]
      [:div [:input {:type :password
                     :value (:confirm-password @fields)
                     :on-change #(swap! fields assoc :confirm-password (.. % -target -value))}]]
      [errors-component errors :confirm-password]]
     [:input {:type :submit
              :value "Create Account"}]
     [errors-component errors :?]]))
