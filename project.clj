(defproject witan.ui "0.3.13-SNAPSHOT"
  :description "FIXME: write this!"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "0.0-3297"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [sablono "0.3.4"]
                 [garden "1.2.5"]
                 [inflections "0.9.14"]
                 [datascript "0.11.6"]
                 [thi.ng/geom "0.0.881"]
                 [cljs-ajax "0.3.14"]
                 [environ "1.0.1"]
                 [venue "0.1.10"]
                 [cljs-log "0.2.2"]
                 [com.andrewmcveigh/cljs-time "0.3.14"]
                 [kixi/s3-beam "0.4.0"]
                 [markdown-clj "0.9.86"]]

  :plugins [[lein-cljsbuild "1.0.5"]
            [lein-figwheel "0.3.5"]
            [lein-garden "0.2.6"]
            [lein-cljfmt "0.3.0"]]

  :source-paths ["src/clj"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds [{:id "prod"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/js/compiled/witan-ui.js"
                                   :main witan.ui.core
                                   :optimizations :advanced
                                   :pretty-print false
                                   :externs ["../js/externs.js"]
                                   :warnings {:single-segment-namespace false}}}
                       {:id "dev"
                        :source-paths ["src/cljs"]
                        :figwheel {:websocket-host :js-client-host
                                   :on-jsload "witan.ui.core/on-js-reload" }
                        :compiler {:main witan.ui.core
                                   :asset-path "js/compiled/out"
                                   :output-to "resources/public/js/compiled/witan-ui.js"
                                   :output-dir "resources/public/js/compiled/out"
                                   :source-map-timestamp true
                                   :warnings {:single-segment-namespace false}}}]}

  :figwheel {:css-dirs ["resources/public/css"] ;; watch and update CSS
             :nrepl-port 7888}

  :garden {:builds [{;; Optional name of the build:
                     :id "ui"
                     ;; Source paths where the stylesheet source code is
                     :source-paths ["src/clj/witan/styles"]
                     ;; The var containing your stylesheet:
                     :stylesheet witan.styles.base/base
                     ;; Compiler flags passed to `garden.core/css`:
                     :compiler {;; Where to save the file:
                                :vendors [:moz :webkit :o :ms]
                                :output-to "resources/public/css/style.css"
                                ;; Compress the output?
                                :pretty-print? false}}]}
  :profiles {:uberjar {:auto-clean false}}
  :uberjar-name "witan-ui.jar"
  :jvm-opts ["-Xmx2g"]
  :release-tasks [["change" "version"
                   "leiningen.release/bump-version" "release"]
                  ["vcs" "commit"]
                  ["vcs" "tag" "release-v"]
                  ["change" "version" "leiningen.release/bump-version"]
                  ["vcs" "commit"]
                  ["vcs" "push"]])

(comment
  (do (use 'figwheel-sidecar.repl-api)
      (cljs-repl)))
