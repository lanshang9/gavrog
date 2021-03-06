(ns org.gavrog.clojure.dsymbols.combineTiles
  (:use (clojure
          [set :only [difference union]])
        (org.gavrog.clojure.common
          [util :only [empty-queue]]
          [generators :only [make-backtracker results]]
          partition)
        (org.gavrog.clojure.dsymbols
          delaney)))

(defn- components-with-multiplicities [ds]
  (let [idcs (indices ds)]
    (frequencies (for [D (orbit-reps ds idcs)] (canonical (orbit ds idcs D))))))

(defn- signatures [ds, idcs]
  (into {} (for [D (orbit-reps ds idcs)
                 :let [sub (orbit ds idcs D)]
                 block (automorphism-orbits sub)
                 :let [inv (invariant sub (first block))],
                 E block]
             [E inv])))

(defn- glue-faces [ds i D E]
  (loop [ds ds, q (conj empty-queue [D E])]
    (if (empty? q)
      ds
      (let [[D E] (first q)]
        (if (s ds i D)
          (recur ds (rest q))
          (recur (glue ds i D E)
                 (into (rest q) (for [j (range (dec i))]
                                  [(s ds j D) (s ds j E)]))))))))

(defn- augmentations [d forms [symbol sig free-elements free-components]]
  (when-let [D (first free-elements)]
    (let [face (fn [D] (orbit-elements symbol (range (dec d)) D))]
      (for [E free-elements :when (= (sig D) (sig E))]
        [(glue-faces symbol d D E)
         sig
         (difference free-elements (face D) (face E))
         free-components]))))

(defn- extensions [d forms [symbol sig free-elements free-components]]
  (when-let [D (first free-elements)]
    (let [sz (size symbol)]
      (for [[part n] free-components
            :when (pos? n)
            [form s] (forms part)
            :when (= (sig D) (s 1))
            :let [face (orbit-elements symbol (range (dec d)) D)]]
        [(glue-faces (append symbol form) d D (inc sz))
         (into sig (for [D (range 1 (inc (size form)))] [(+ sz D) (s D)]))
         (difference (union free-elements
                            (set (range (inc sz) (inc (+ sz (size form))))))
                     face
                     (set (range (inc sz) (inc (+ sz (count face))))))
         (assoc free-components part (dec (free-components part)))]))))

(defn combine-tiles [ds]
  (let [d (inc (dim ds))
        face-idcs (range (dim ds))
        counts (components-with-multiplicities ds)
        parts (sort-by (comp vec invariant) (keys counts))
        forms (into {} (for [sub parts]
                         [sub (for [f (inequivalent-forms sub)]
                                [f (signatures f face-idcs)])]))]
    (make-backtracker 
      {:root (let [[sub sigs] (first (forms (first parts)))]
               [sub
                sigs
                (into #{} (elements sub))
                (assoc counts sub (dec (counts sub)))])
       :extract (fn [[symbol _ free-elements free-components]]
                  (when (and (empty? free-elements)
                             (every? (comp zero? second) free-components)
                             (canonical? symbol))
                    symbol))
       :children (fn [state]
                   (concat (augmentations d forms state)
                           (extensions d forms state)))})))
