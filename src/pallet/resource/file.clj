(ns pallet.resource.file
  "File manipulation."
  (:require
   [pallet.utils :as utils]
   [pallet.stevedore :as stevedore]
   [pallet.resource.file :as file]
   [pallet.script :as script]
   [clojure.string :as string])
  (:use
   [pallet.resource :only [defresource]]
   clojure.contrib.logging))

(script/defscript rm [file & options])
(stevedore/defimpl rm :default [file & options]
  ("rm" ~(stevedore/map-to-arg-string (first options)) ~file))

(script/defscript chown [owner file & options])
(stevedore/defimpl chown :default [owner file & options]
  ("chown" ~(stevedore/map-to-arg-string (first options)) ~owner ~file))

(script/defscript chgrp [group file & options])
(stevedore/defimpl chgrp :default [group file & options]
  ("chgrp" ~(stevedore/map-to-arg-string (first options)) ~group ~file))

(script/defscript chmod [mode file & options])
(stevedore/defimpl chmod :default [mode file & options]
  ("chmod" ~(stevedore/map-to-arg-string (first options)) ~mode ~file))

(script/defscript touch [file & options])
(stevedore/defimpl touch :default [file & options]
  ("touch" ~(stevedore/map-to-arg-string (first options)) ~file))

(script/defscript sed-file [file expr-map options])

(stevedore/defimpl sed-file :default [file expr-map options]
  ("sed" "-i"
   ~(let [sep (:seperator options "/")]
      (string/join
       " "
       (map
        #(format "-e \"s%s%s%s%s%s\"" sep (first %) sep (second %) sep)
        expr-map)))
   ~file))

(script/defscript download-file [url path])

(stevedore/defimpl download-file :default [url path]
  ("curl" "-o" ~path --retry 3 --silent --show-error --fail ~url))

(script/defscript tmp-dir [])
(stevedore/defimpl tmp-dir :default []
  @TMPDIR-/tmp)

(script/defscript heredoc [path content])
(stevedore/defimpl heredoc :default [path content]
  ("{ cat" ">" ~path ~(str "<<EOFpallet\n" content "\nEOFpallet\n }")))

;; the cat is wrapped in braces so that the final newline is protected
(defn heredoc
  "Generates a heredoc. Options:
      :literal boolean  - if true, prevents shell expansion of contents"
  [path content & options]
  (let [options (apply hash-map options)]
    (stevedore/script ("{ cat" ">" ~path
             ~(str (if (options :literal) "<<'EOFpallet'\n" "<<EOFpallet\n")
                   content "\nEOFpallet\n }")))))

(defn adjust-file [path opts]
  (stevedore/chain-commands*
   (filter
    (complement nil?)
    [(when (opts :owner)
       (stevedore/script (chown ~(opts :owner) ~path)))
     (when (opts :group)
       (stevedore/script (chgrp ~(opts :group) ~path)))
     (when (opts :mode)
       (stevedore/script (chmod ~(opts :mode) ~path)))])))

(defn touch-file [path opts]
  (stevedore/chain-commands
   (stevedore/script
    (touch ~path ~(select-keys opts [:force])))
   (adjust-file path opts)))

(defn file*
  [path & options]
  (let [opts (apply hash-map options)
        opts (merge {:action :create} opts)]

    (condp = (opts :action)
      :delete
      (stevedore/checked-script
       (str "file " path)
       (rm ~path ~(select-keys opts [:force])))
      :create
      (stevedore/checked-commands
       (str "file " path)
       (touch-file path opts))
      :touch
      (stevedore/checked-commands
       (str "file " path)
       (touch-file path opts)))))

(defresource file "File management."
  file* [filename & options])

(defn sed* [path exprs options]
  (stevedore/checked-script
   (format "sed file %s" path)
   (sed-file ~path ~exprs ~options)))

(defresource sed
  sed* [path exprs options])
