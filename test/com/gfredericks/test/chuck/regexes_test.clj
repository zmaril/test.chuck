(ns com.gfredericks.test.chuck.regexes-test
  (:require [clojure.test :refer [deftest are]]
            [clojure.test.check.generators :as gen]
            [clojure.test.check.properties :as prop]
            [clojure.test.check.clojure-test :refer [defspec]]
            [com.gfredericks.test.chuck.regexes :as regexes]
            [com.gfredericks.test.chuck.generators :as gen']
            [instaparse.core :as insta]))

(def gen-regexy-fragment
  (gen/elements "?*+!()[]{}^$\\:-&"))

(def gen-regexy-string
  (gen/fmap
   (fn [expr]
     (->> expr
          (tree-seq coll? seq)
          (remove coll?)
          (apply str)))
   (gen/recursive-gen
    (fn [g]
      (gen'/for [:parallel [[open closed] (gen/elements ["[]" "{}" "()"])
                            els (gen/list g)]]
        [open els closed]))
    (gen/one-of [gen/string gen-regexy-fragment]))))

(def gen-regex-parsing-attempt
  (gen'/for [s (gen/one-of [gen-regexy-string
                            gen/string-ascii
                            gen/string])]
    (try (do
           (re-pattern s)
           [:parsed s])
         (catch java.util.regex.PatternSyntaxException e
           [:not-parsed s]))))

(defn parses?
  [s]
  (try (regexes/parse s)
       true
       (catch clojure.lang.ExceptionInfo e
         (if (= ::regexes/parse-error (:type (ex-data e)))
           false
           (throw e)))))

(deftest parser-regression
  (are [s] (parses? s)
       "[]-_]" "[-x]" "[x+--y]" "[\\e]" "\\\0" "[[x]-y]")
  (are [s] (not (parses? s))
       "[b-a]" "[^]"))

(defspec the-parser-spec 1000
  (prop/for-all [[flag s] gen-regex-parsing-attempt]
    (let [parsed? (parses? s)]
      (or (= [flag parsed?] [:parsed true])
          (= [flag parsed?] [:not-parsed false])))))
