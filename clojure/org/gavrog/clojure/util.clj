(ns org.gavrog.clojure.util)

;; General purpose definitions and entities

(def empty-queue clojure.lang.PersistentQueue/EMPTY)

(defn pop-while [pred coll]
  "Like drop-while, but with pop."
  (cond
    (empty? coll) coll
    (pred (first coll)) (recur pred (pop coll))
    :else coll))

(defn iterate-cycle [coll x]
  "Returns a lazy sequence of intermediate results, starting at x, of
   cycling through the functions in coll and applying each to the
   previous result."
  (reductions #(%2 %1) x (cycle coll)))

(defn unique
  "Returns a lazy sequence of values from coll with duplicates removed.
   If key-fun is given, it is applied to the original values before
   determining equality."
  ([key-fun coll]
    (letfn [(step [coll seen]
                  (when-let [x (first coll)]
                    (let [key (key-fun x)]
                      (if (seen key)
                        (recur (rest coll) seen)
                        (lazy-seq (cons x (step (rest coll)
                                                (conj seen key))))))))]
           (step coll #{})))
  ([coll]
    (unique identity coll)))
