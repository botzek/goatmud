(ns goatmud.webclient.websocket
  (:require
   [taoensso.sente :as sente]
   [re-frame.core :as rf]))

(defn make-socket!
  []
  (sente/make-channel-socket!
   "/ws"
   nil
   :type :ws))

(defmulti handle-message
  (fn [{:keys [id]} _]
    id))

(defmethod handle-message :chsk/handshake
  [msg event]
  (rf/dispatch [:ws/update-connected!])
  (.log js/console "Connection established: " (pr-str event) "got" (pr-str msg)))

(defmethod handle-message :chsk/state
  [_ event]
  (rf/dispatch [:ws/update-connected!])
  (.log js/console "State changed: " (pr-str event)))

(defmethod handle-message :chsk/recv
  [_ event]
  (.log js/console "Recv: " (pr-str event)))

(defmethod handle-message :default
  [_ event]
  (.log js/console"Unhandled event: " (pr-str event)))

(defn receive-message!
  [{:keys [event] :as message}]
  (handle-message message event))

(defn make-router!
  [socket]
  (sente/start-chsk-router!
   (:ch-recv socket)
   #'receive-message!))

(defn send! [socket & args]
  (if-let [send-fn (:send-fn socket)]
    (apply send-fn args)
    (throw (ex-info "Couldn't send message, socket isn't open!"
                    {:message (first args)}))))

(rf/reg-event-fx
 :ws/initialize
 (fn [_ _]
   (let [{:keys [state] :as socket} (make-socket!)
         router (make-router! socket)]
     {:db {:ws/socket socket
           :ws/router router}
      :dispatch [:ws/update-connected!]})))

(rf/reg-event-db
 :ws/update-connected!
 (fn [{{:keys [state]} :ws/socket :as db} _]
   (assoc db :ws/connected? (:open? @state))))

(rf/reg-fx
 :ws/send!
 (fn [{:keys [socket message timeout callback-event]
       :or {timeout 30000}}]
   (if callback-event
     (send! socket message timeout #(rf/dispatch (conj callback-event %)))
     (send! socket message))))

(rf/reg-sub
 :ws/connected?
 (fn [db _]
   (:ws/connected? db)))

(defn connecting-dialog
  []
  (let [connected? (rf/subscribe [:ws/connected?])]
    [:div
     {:style {:position :fixed
              :z-index 1
              :left 0
              :top 0
              :width "100%"
              :height "100%"
              :overflow :auto
              :background-color "rgba(0,0,0,0.4)"
              :display (if @connected? :none :block)}}
     [:div
      {:style {:background-color "#fefefe"
               :margin "15% auto"
               :padding "20px"
               :border "1px solid #888"
               :text-align :center
               :width "80%"}}
      "Connecting"]]))
