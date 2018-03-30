(ns lacinia-gen.core
  (:require [clojure.test.check.generators :as gen]))

(def ^:private base-scalars
  {'String gen/string
   'Float gen/double
   'Int gen/int
   'Boolean gen/boolean})

(defn- enum [values]
  (let [g (gen/elements values)]
    (constantly g)))

(defn- field [depth width scalars all-gens type]
  (cond
    ;; sub object
    (and (keyword? type)
         (contains? all-gens type))
    ((get all-gens type) depth width scalars all-gens)

    ;; list of type
    (and (list? type)
         (= 'list (first type)))
    (let [g (gen/list (field depth width scalars all-gens (second type)))]
      (if-let [w (get width (second type))]
        (gen/resize w g)
        g))

    ;; non-nullable
    (and (list? type)
         (= 'non-null (first type)))
    (gen/such-that (complement nil?) (field depth width scalars all-gens (second type)))

    ;; scalar
    :else
    (scalars type)))

(defn- object [fields]
  (fn [depth width scalars all-gens]
    (let [fields (keep (fn [[k {:keys [type]}]]
                         (when (pos? (get depth k 1))
                           (gen/fmap (fn [v]
                                       {k v})
                                     (field (update depth k (fnil dec 1)) width scalars all-gens type))))
                       fields)]
      (gen/fmap
       (fn [kvs]
         (apply merge kvs))
       (apply gen/tuple fields)))))

(defn- make-gens [enums objects]
  (merge (reduce-kv (fn [all-gens k {:keys [fields]}]
                      (assoc all-gens k (object fields)))
                    {}
                    objects)
         (reduce-kv (fn [all-gens k {:keys [values]}]
                      (assoc all-gens k (enum values)))
                    {}
                    enums)))

(defn generator
  "Given a lacinia schema returns a function which takes an object key as an argument
  and returns a generator for that object, e.g.

  (let [g (generator my-lacinia-schema)]
    (gen/sample (g :my-object) 10))

  Options:
   - depth
     A map of object keys to the depth they should recurse to in cyclical graphs
     e.g. {:parent 1
           :child 2}

   - width
     A map of object keys to the maximum number of items that should be present in lists of that object
     e.g. {:listed-object 3}

   - scalars
     A map of custom scalar types to the generators to be used for them.
     e.g. {:DateTime (gen/...)}
  "
  [schema & [opts]]
  (let [{:keys [depth width scalars]
         :or {depth {}
              width {}
              scalars {}}} opts
        all-gens (make-gens (:enums schema) (:objects schema))
        scalars (merge base-scalars scalars)]
    (fn [type]
      ((get all-gens type) depth width scalars all-gens))))
