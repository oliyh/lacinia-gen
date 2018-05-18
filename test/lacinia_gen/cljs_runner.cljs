(ns lacinia-gen.cljs-runner
  (:require [cljs.test :as test]
            [doo.runner :refer-macros [doo-all-tests]]
            [lacinia-gen.cljs-tests]))

(doo-all-tests #"^lacinia-gen.*-test$")
