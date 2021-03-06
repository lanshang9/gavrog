(ns org.gavrog.clojure.common.util)

(defn abs [n] (if (neg? n) (- n) n))

(defn sign [n]
  (cond (zero? n) 0
        (neg? n) -1
        :else 1))

(def empty-queue clojure.lang.PersistentQueue/EMPTY)

(defn pop-while
  "Like drop-while, but with pop."
  ([pred coll]
    (cond
      (empty? coll) coll
      (pred (first coll)) (recur pred (pop coll))
      :else coll)))

(defn iterate-cycle
  "Returns a lazy sequence of intermediate results, starting at x, of
   cycling through the functions in coll and applying each to the
   previous result."
  ([coll x]
    (reductions #(%2 %1) x (cycle coll))))

(defn compare-lexicographically
  "Compares two sequences lexicographically. In the first form, the given
   function cmp is used for element-wise comparison. In the second form, the
   standard compare function is used, and the elements of xs must implement
   Comparable."
  ([cmp xs ys]
    (cond (empty? xs) (if (empty? ys) 0 -1)
          (empty? ys) 1
          :else (let [d (cmp (first xs) (first ys))]
                  (if (not= 0 d)
                    d
                    (recur cmp (rest xs) (rest ys))))))
  ([xs ys]
    (compare-lexicographically compare xs ys)))

(defn lexicographically-smallest
  "Returns the lexicographically smallest sequence, using cmp for element-wise
   comparison."
  ([cmp xs] xs)
  ([cmp xs ys] (if (neg? (compare-lexicographically cmp xs ys)) xs ys))
  ([cmp xs ys & more] (reduce (partial lexicographically-smallest cmp)
                          (lexicographically-smallest cmp xs ys) more)))

(defn multi-assoc [m k v]
  (assoc m k (conj (get m k #{}) v)))

(defn multi-map [pairs]
  (reduce (fn [m [k v]] (multi-assoc m k v)) {} pairs))

(defn classify
  "Given a map that assigns sequences to items, this function determines the
  shortest subsequences, if any, that characterize each item uniquely. The
  result is a map from sequences to lists of items.

  Example:
  => (classify {:a [1 2] :b [1 3] :c [2 4] :d [1 2]})
  {[1 2] (:a :d), [1 3] (:b), [2] (:c)}"

  ([items2seqs]
    (cond
      (empty? items2seqs)
      {}

      (= 1 (count items2seqs))
      { (vec (take 1 (second (first items2seqs)))) (take 1 (first items2seqs)) }

      :else
      (let [step (fn [m] (->> m
                           (map (fn [[k s]] [(first s) [k (rest s)]]))
                           (group-by first)
                           (map (fn [[k c]] [k (map second c)]))))]
        (loop [classes (sorted-map [(- (count items2seqs)) []] items2seqs)]
          (let [[[n k] c] (first classes)]
            (if (or (nil? n) (>= n -1))
              (zipmap (map (comp second first) classes)
                      (map #(map first (last %)) classes))
              (recur (into (dissoc classes [n k])
                           (for [[key cl] (step c)]
                             (if (nil? key)
                               [[0 k] cl]
                               [[(- (count cl)) (conj k key)] cl])))))))))))
