(ns goatmud.websocket
  (:require
   [taoensso.sente :as sente]
   [re-frame.core :as rf]))

(defn make-socket!
  []
  (sente/make-channel-socket!
   "/ws"
   nil
   :type :ws))

(defn send! [socket & args]
  (if-let [send-fn (:send-fn socket)]
    (apply send-fn args)
    (throw (ex-info "Couldn't send message, socket isn't open!"
                    {:message (first args)}))))

(rf/reg-fx
 :ws/send!
 (fn [{:keys [socket message timeout callback-event]
       :or {timeout 30000}}]
   (if callback-event
     (send! socket message timeout #(rf/dispatch (conj callback-event %)))
     (send! socket message))))

(defmulti handle-message
  (fn [{:keys [id]} _]
    id))

(defmethod handle-message :chsk/handshake
  [_ event]
  (.log js/console "Connection established: " (pr-str event)))

(defmethod handle-message :chsk/state
  [_ event]
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
