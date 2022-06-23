(ns goatmud.webclient.core
    (:require
      [reagent.core :as r]
      [reagent.dom :as d]
      [re-frame.core :as rf]
      [goatmud.webclient.websocket :as ws]
      [goatmud.webclient.ajax :as ajax]
      [goatmud.webclient.account :as account]))

(rf/reg-event-fx
 :app/initialize
 (fn [{:keys [db]} _]
   {:db (assoc db :screen :login)
    :dispatch [:ws/initialize]}))


(rf/reg-event-fx
 :message/send!
 (fn [{:keys [db]} [_ message]]
   {:ws/send! {:socket (:ws/socket db)
               :message [:click/button message]}}))

(rf/reg-event-db
 :change-screen
 (fn [db [_ screen]]
   (assoc db :screen screen)))

(rf/reg-sub
 :screen
 (fn [db _]
   (:screen db)))

(defn message-form []
  (let [msg (r/atom nil)]
    [:div
     [:input {:type :text
              :name "message"
              :on-change #(reset! msg (.. % -target -value))}]
     [:input {:type :button
              :value "Send!"
              :on-click #(rf/dispatch [:message/send! @msg])}]]))

;; -------------------------
;; Views


(defn login-screen
  []
  [:div
   [account/login-dialog]
   [:a {:on-click #(rf/dispatch [:change-screen :register])} "Register"]])

(defn register-screen
  []
  [:div
   [account/create-account-dialog]
   [:a {:on-click #(rf/dispatch [:change-screen :login])} "Login"]])

(defn account-home-screen
  []
  [:div "Account Home!"])

(defn show-screen []
  [:div
   [ws/connecting-dialog]
   (let [screen @(rf/subscribe [:screen])]
     (case screen
       :login [login-screen]
       :register [register-screen]
       :account-home [account-home-screen]

       [:div]))])

;; -------------------------
;; Initialize app

(defn ^:dev/after-load mount-root []
  (d/render [show-screen] (.getElementById js/document "app")))

(defn ^:export ^:dev/once init! []
  (rf/dispatch [:app/initialize])
  (mount-root))
