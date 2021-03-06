(ns app.renderer
  (:require [reagent.core :as r]
            [cljs.spec :as s]
            [cljs.core.async :as a]
            [cljsnes.state :as state]
            [cljsnes.spec :as spec]
            [clojure.pprint :as pprint]
            [cljsnes.cpu :as cpu]
            [cljsnes.display :as display]
            [cljsnes.emulator :as emulator]
            [cljsnes.assembler :as assembler]
            [cljsnes.cartridge :as cartridge]
            [cljsnes.memory :as memory]
            [cljsnes.opcodes :as opcodes])
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]))

(enable-console-print!)

(def front (r/cursor state/state [:display :front]))

(def buffer (r/cursor state/state [:display]))

(def ppu-cycles (r/cursor state/state [:ppu :cycle]))

(def ppu-line (r/cursor state/state [:ppu :line]))

(def cpu-cycles (r/cursor state/state [:cpu :cycles]))

(def zero-flag (r/cursor state/state [:cpu :z]))

(defonce stop-chan (a/chan))

(defn swap-buffers []
  (swap! state/state update :display (fn [{:keys [front back] :as state}]
                                       (assoc state :front back
                                              :back front))))

(defn swap-button []
  [:div [:input {:type "button"
                 :value "Swap buffer!"
                 :on-click #(swap-buffers)}]])

(defn init-button []
  [:div [:input {:type "button"
                 :value "Init State!"
                 :on-click #(emulator/init "/Users/angusiguess/Downloads/Super Mario Bros. (Japan, USA).nes")}]])

(defn step-button []
  [:div [:input {:type "button"
                 :value "Step State!"
                 :on-click #(state/step!)}]])

(defn stop-button []
  [:div [:input {:type "button"
                 :value "Stop state!"
                 :on-click #(a/put! stop-chan :stop)}]])

(defn run-button []
  [:div [:input {:type "button"
                 :value "Run state!"
                 :on-click #(go-loop []
                              (let [[v ch] (a/alts! [stop-chan (go 1)])]
                                (when (not= ch stop-chan)
                                  (state/step!)
                                  (recur))))}]])

(defn save-state []
  [:div [:input {:type "button"
                 :value "Save state"
                 :on-click #(state/save-state!)}]])

(defn load-state []
  [:div [:input {:type "button"
                 :value "Load state"
                 :on-click #(state/load-state!)}]])

(defn cpu-cycle []
  [:div [:p "CPU Cycles: " @cpu-cycles]])

(defn ppu-cycle []
  [:div [:p "PPU Cycles: " @ppu-cycles]
   [:p "PPU Line: " @ppu-line]])

(defn cpu-pc []
  (let [cursor (r/cursor state/state [:cpu :pc])]
    [:div [:p "Program Counter: " (pprint/cl-format nil "~x" @cursor)]]))

(defn cpu-registers []
  (let [a (r/cursor state/state [:cpu :a])
        x (r/cursor state/state [:cpu :x])
        y (r/cursor state/state [:cpu :y])
        i (r/cursor state/state [:cpu :i])
        s (r/cursor state/state [:cpu :s])
        z (r/cursor state/state [:cpu :z])]
    [:div [:p "A: " @a " X: " @x " Y: " @y " I: " @i]
     [:p "Stack Pointer: " @s]
     [:z "Zero? " @z]]))

(defn display-component []
  @front
  (when-let [elem (.getElementById js/document "display")]
    (let [context (.getContext elem "2d")]
      (display/render-frame @front context)))
  [:div
   [:canvas {:width 256 :height 224 :id :display}]])

(defn container []
  [:div
   (display-component)
   (swap-button)
   (init-button)
   (step-button)
   (save-state)
   (load-state)
   (run-button)
   (stop-button)
   (ppu-cycle)
   (cpu-cycle)
   (cpu-pc)
   (cpu-registers)])

(defn init []
  (r/render-component [container]
                      (.getElementById js/document "container"))
  (js/console.log "Starting Application"))
