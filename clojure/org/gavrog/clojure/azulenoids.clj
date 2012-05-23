(ns org.gavrog.clojure.azulenoids
  (:import (org.gavrog.jane.numbers Whole)
           (org.gavrog.joss.dsyms.basic DSymbol DynamicDSymbol)
           (org.gavrog.joss.dsyms.generators CombineTiles DefineBranching2d))
  (:gen-class))

(defn iterate-cycle [coll x]
  (reductions #(%2 %1) x (cycle coll)))

(defn unique [coll key-fun]
  (letfn [(step [[seen _] x]
                (let [key (key-fun x)]
                  (if (seen key)
                    [seen false]
                    [(conj seen key) x])))]
         (filter identity (map second (reductions step [#{} false] coll)))))

(defn walk [ds D & idxs]
  (reduce #(.op ds %2 %1) D idxs))

(defn chain-end [ds D i j]
  (loop [E (walk ds D i)]
    (let [E* (walk ds E j)]
      (cond
        (nil? E*) E
        (= E E*) E
        (= D E*) nil
        :else (recur (walk ds E j i))))))

(defn boundary-chambers [ds D i j k]
  (iterate-cycle [#(walk ds %1 i) #(chain-end ds %1 j k)] D))

(defn max-curvature [ds]
  (let [dsx (.clone ds)]
    (do (.setVDefaultToOne dsx true) (.curvature2D dsx))))

(def template
  (DSymbol. (str "1.1:60:"
                 "2 4 6 8 10 12 14 16 18 20 22 24 26 28 30 "
                 "32 34 36 38 40 42 44 46 48 50 52 54 56 58 60,"
                 "6 3 5 12 9 11 18 15 17 24 21 23 30 27 29 36 "
                 "33 35 42 39 41 48 45 47 54 51 53 60 57 59,"
                 "0 0 12 11 28 27 0 0 18 17 36 35 24 23 58 57 30 "
                 "29 0 0 0 0 42 41 0 0 48 47 0 0 54 53 0 0 60 59 0 0:"
                 "3 3 3 3 3 3 3 3 3 3,0 0 5 0 7 0 0 0 0 0")))

(def octagon
  (DSymbol. "1.1:16 1:2 4 6 8 10 12 14 16,16 3 5 7 9 11 13 15:8"))

(def boundary-mappings
  (let [template-boundary (boundary-chambers template (Integer. 1) 0 1 2)
        octagon-boundary (cycle (map #(Integer. %1) (range 1 17)))]
    (for [p (range 1 16 2)]
      (zipmap (drop (mod (- 19 p) 16) octagon-boundary)
              (take 16 template-boundary)))))

(defn on-template [ds oct2tmp]
  (let [result (DynamicDSymbol. template)]
    (doseq [[D D*] oct2tmp :when (not (.definesOp result 2 D*))]
      (.redefineOp result 2 D* (oct2tmp (.op ds 2 D))))
    (doseq [[D D*] oct2tmp :when (not (.definesV result 1 2 D*))]
      (.redefineV result 1 2 D* (.v ds 1 2 D)))
    result))

(def octa-sets (filter #(-> %1 max-curvature .isNegative not)
                       (lazy-seq (CombineTiles. octagon))))

(def octa-syms
  (for [dset octa-sets
        dsym (lazy-seq (DefineBranching2d. dset 3 2 Whole/ZERO))
        :when (-> dsym .curvature2D .isZero)]
    dsym))

(def azul-syms
  (let [raw (for [ds octa-syms o2t boundary-mappings] (on-template ds o2t))]
    (for [ds (unique raw #(-> %1 .minimal .invariant))]
      (-> ds .dual .minimal .canonical))))

(defn -main []
  (do
    (doseq [ds azul-syms] (println (str ds)))
    (println "#Generated:")
    (println "#   " (count octa-sets) "octagonal D-sets.")
    (println "#   " (count octa-syms) "octagonal D-symbols.")
    (println "#   " (count azul-syms) "azulenoid D-symbols.")))
