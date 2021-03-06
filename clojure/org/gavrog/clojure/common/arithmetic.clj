(ns org.gavrog.clojure.common.arithmetic
  (:use (org.gavrog.clojure.common util)))

(defn divides? [a b]
  (and (not (zero? a)) (zero? (mod b a))))

(defn gcdex [m n]
  (loop [f (abs m), fm (sign m), g (abs n), gm 0]
    (if (zero? g)
      (if (zero? n)
        [fm 0 gm 1]
        [fm (quot (- f (* fm m)) n) gm (quot (- (* gm m)) n)])
      (let [x (quot f g)]
        (recur g gm (- f (* x g)) (- fm (* x gm)))))))

(defn gcd [m n]
  (loop [f (abs m), g (abs n)]
    (if (zero? g)
      f
      (recur g (mod f g)))))

(defn lcm [m n]
  (if (and (zero? m) (zero? n))
    0
    (abs (* (quot m (gcd m n)) n))))

(defn abelian-factors [xs]
  (when (seq xs)
    (let [tmp (reductions (fn [[a _] b] [(gcd a b) (lcm a b)])
                          [(first xs)]
                          (rest xs))]
      (cons (first (last tmp))
            (abelian-factors (map second (rest tmp)))))))

(defn- combine-rows [M cols i1 i2 f11 f12 f21 f22]
  (let [stuff! (partial reduce conj!)]
    (-> (transient M)
      (stuff! (for [j cols] [[i1 j] (+ (* (M [i1 j]) f11) (* (M [i2 j]) f12))]))
      (stuff! (for [j cols] [[i2 j] (+ (* (M [i1 j]) f21) (* (M [i2 j]) f22))]))
      persistent!)))

(defn- combine-cols [M rows j1 j2 f11 f12 f21 f22]
  (let [stuff! (partial reduce conj!)]
    (-> (transient M)
      (stuff! (for [i rows] [[i j1] (+ (* (M [i j1]) f11) (* (M [i j2]) f12))]))
      (stuff! (for [i rows] [[i j2] (+ (* (M [i j1]) f21) (* (M [i j2]) f22))]))
      persistent!)))

(defn- smallest [xs]
  (when (seq xs)
    (reduce (fn [a b] (if (neg? (compare a b)) a b)) (first xs) (rest xs))))

(defn- clear-col [M rows cols over-field]
  (let [i0 (first rows), j0 (first cols)
        coeff (if over-field (fn [a b] [1 0 (- (/ b a)) 1]) gcdex)]
    (reduce (fn [M i]
              (let [a (M [i0 j0]), b (M [i j0])]
                (if (zero? b) M (apply combine-rows M cols i0 i (coeff a b)))))
            M
            (rest rows))))

(defn- try-to-clear-row [M rows cols over-field]
  (let [i0 (first rows), j0 (first cols)]
    (loop [M M, js (rest cols)]
      (if-let [j (first js)]
        (let [a (M [i0 j0]), b (M [i0 j])]
          (if (or over-field (divides? a b))
            (recur (assoc M [i0 j] 0) (rest js))
            [(apply combine-cols M rows j0 j (gcdex a b)) true]))
        [M false]))))

(defn- eliminate [M rows cols over-field]
  (let [[M dirty] (-> M
                    (clear-col rows cols over-field)
                    (try-to-clear-row rows cols over-field))]
    (if dirty (recur M rows cols over-field) M)))

(defn diagonalized [M rows cols over-field]
  (if-let [[_ i j s] (smallest (for [i rows, j cols
                                     :let [x (M [i j])]
                                     :when (not (zero? x))]
                                 [(abs x) i j (sign x)]))]
    (let [M (combine-rows M cols (first rows) i 0 1 1 0)
          M (combine-cols M rows (first cols) j 0 1 s 0)]
      (recur (eliminate M rows cols over-field)
             (rest rows) (rest cols) over-field))
    M))
