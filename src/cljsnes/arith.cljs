(ns cljsnes.arith
  (:require [cljs.spec :as s])
  (:refer-clojure :exclude [inc dec]))

(s/def ::byte (s/int-in 0 256))

(s/def ::address (s/int-in 0 65536))

(s/def ::carry-bit #{0 1})

;; Some addition fns
(defn add
  ([] [0 0])
  ([x] [x 0])
  ([x y]
   (let [sum (+ x y)
         masked-sum (bit-and 0xFF sum)
         carry (if (bit-test sum 8) 1 0)]
     [masked-sum carry]))
  ([x y z]
   (let [[sum carry] (add x y)]
     (add sum z))))

(defn inc [x]
  (add x 1))

(defn sub
  ([] [0 0])
  ([x] [x])
  ([x y]
   (let [negated-y (bit-and 0xFF (+ 1 (bit-not y)))]
     (add x negated-y)))
  ([x y z]
   (let [[diff carry] (sub x y)]
     (sub diff z))))

(defn dec [x]
  (sub x 1))

(defn make-address [lower upper]
  (+ lower (bit-shift-left upper 8)))

(defn address->bytes [address]
  (let [mask-high 0xFF00
        mask-low 0xFF]
    [(bit-and mask-low address)
     (bit-shift-right (bit-and mask-high address) 8)]))

(s/fdef make-address
        :args (s/cat :lower ::byte :upper ::byte)
        :ret ::address)

(s/fdef add
        :args (s/cat :x (s/? ::byte) :y (s/? ::byte) :z (s/? ::byte))
        :ret (s/cat :sum ::byte :carry ::carry-bit))

(s/fdef inc
        :args (s/cat :x ::byte)
        :ret (s/cat :sum ::byte :carry ::carry-bit))

;; Negative?

(defn neg-byte? [x]
  (bit-test x 7))

(s/fdef neg-byte?
        :args (s/cat :x nat-int?)
        :ret boolean?)



;; Logical Shifts

(defn asl [x]
  (let [shifted (bit-shift-left x 1)
        masked (bit-and shifted 255)
        carry (if (bit-test shifted 8) 1 0)]
    [masked carry]))

(s/fdef asl
        :args (s/cat :x ::byte)
        :fn #(or (>= (-> % :ret :shifted) (-> % :args :x))
                 (= (-> % :ret :carry) 1))
        :ret (s/cat :shifted ::byte :carry ::carry-bit))

(defn lsr [x]
  (let [shifted (bit-shift-right x 1)
        carry (if (bit-test x 0) 1 0)]
    [shifted carry]))

(s/fdef lsr
        :args (s/cat :x ::byte)
        :fn #(or (<= (-> % :ret :shifted) (-> % :args :x))
                 (= (-> % :ret :carry) 1))
        :ret (s/cat :shifted ::byte :carry ::carry-bit))

(defn unsigned->signed [x]
  (let [mask 128]
    (+ (- (bit-and x mask))
       (bit-and x (bit-not mask)))))

;; Logical ops

(defn l-and [x y]
  (bit-and x y))

(s/fdef l-and
        :args (s/cat :x ::byte :y ::byte)
        :ret ::byte)

(defn l-or [x y]
  (bit-or x y))

(s/fdef l-or
        :args (s/cat :x ::byte :y ::byte)
        :ret ::byte)

(defn l-xor [x y]
  (bit-xor x y))

(s/fdef l-xor
        :args (s/cat :x ::byte :y ::byte)
        :ret ::byte)
